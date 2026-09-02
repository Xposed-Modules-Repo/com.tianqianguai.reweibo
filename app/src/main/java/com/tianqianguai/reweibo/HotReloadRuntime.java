package com.tianqianguai.reweibo;

import android.app.Dialog;
import android.os.Handler;
import android.os.Looper;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/** Owns every delayed callback and background executor created by one module generation. */
public final class HotReloadRuntime {
    private static final Object LOCK = new Object();
    private static long sGeneration = 1L;
    private static boolean sAccepting = true;
    private static int sActiveMainCallbacks = 0;
    private static int sActiveBackgroundTasks = 0;
    private static Handler sMainHandler;
    private static final Map<Runnable, Set<Runnable>> WRAPPERS = new IdentityHashMap<>();
    private static final List<RestartableSingleThreadExecutor> EXECUTORS = new ArrayList<>();
    private static final List<WeakReference<Dialog>> DIALOGS = new ArrayList<>();

    private HotReloadRuntime() {}

    public static ExecutorService newSingleThreadExecutor(String threadName, int priority) {
        RestartableSingleThreadExecutor executor = new RestartableSingleThreadExecutor(
            threadName,
            priority
        );
        synchronized (LOCK) {
            EXECUTORS.add(executor);
        }
        return executor;
    }

    public static boolean post(Runnable task) {
        return schedule(task, 0L);
    }

    public static boolean postDelayed(Runnable task, long delayMs) {
        return schedule(task, Math.max(0L, delayMs));
    }

    public static void removeCallbacks(Runnable task) {
        if (task == null) return;
        List<Runnable> pending;
        Handler handler;
        synchronized (LOCK) {
            Set<Runnable> wrappers = WRAPPERS.remove(task);
            pending = wrappers == null
                ? Collections.emptyList()
                : new ArrayList<>(wrappers);
            handler = sMainHandler;
        }
        if (handler != null) {
            for (Runnable wrapper : pending) handler.removeCallbacks(wrapper);
        }
    }

    private static boolean schedule(Runnable task, long delayMs) {
        if (task == null) return false;
        final long ownerGeneration;
        final Handler handler;
        synchronized (LOCK) {
            if (!sAccepting) return false;
            ownerGeneration = sGeneration;
            handler = mainHandlerLocked();
        }
        final Runnable[] holder = new Runnable[1];
        Runnable wrapper = new Runnable() {
            @Override
            public void run() {
                synchronized (LOCK) {
                    unregisterLocked(task, holder[0]);
                    if (!sAccepting || sGeneration != ownerGeneration) return;
                    sActiveMainCallbacks++;
                }
                try {
                    synchronized (LOCK) {
                        if (!sAccepting || sGeneration != ownerGeneration) return;
                    }
                    task.run();
                } finally {
                    synchronized (LOCK) {
                        sActiveMainCallbacks--;
                    }
                }
            }
        };
        holder[0] = wrapper;

        synchronized (LOCK) {
            if (!sAccepting || sGeneration != ownerGeneration) return false;
            WRAPPERS
                .computeIfAbsent(task, ignored -> Collections.newSetFromMap(new IdentityHashMap<>()))
                .add(wrapper);
            boolean posted = handler.postDelayed(wrapper, delayMs);
            if (!posted) {
                unregisterLocked(task, wrapper);
            }
            return posted;
        }
    }

    private static void unregisterLocked(Runnable task, Runnable wrapper) {
        Set<Runnable> wrappers = WRAPPERS.get(task);
        if (wrappers == null) return;
        wrappers.remove(wrapper);
        if (wrappers.isEmpty()) WRAPPERS.remove(task);
    }

    private static Handler mainHandlerLocked() {
        if (sMainHandler == null) sMainHandler = new Handler(Looper.getMainLooper());
        return sMainHandler;
    }

    public static void trackDialog(Dialog dialog) {
        if (dialog == null) return;
        boolean accepted;
        synchronized (LOCK) {
            pruneDialogsLocked();
            accepted = sAccepting;
            if (accepted) DIALOGS.add(new WeakReference<>(dialog));
        }
        if (!accepted) {
            try {
                dialog.dismiss();
            } catch (Throwable ignored) {}
        }
    }

    public static boolean hasOpenDialogs() {
        synchronized (LOCK) {
            pruneDialogsLocked();
            for (WeakReference<Dialog> reference : DIALOGS) {
                Dialog dialog = reference.get();
                try {
                    if (dialog != null && dialog.isShowing()) return true;
                } catch (Throwable ignored) {
                    return true;
                }
            }
            return false;
        }
    }

    public static void clearDialogReferences() {
        synchronized (LOCK) {
            DIALOGS.clear();
        }
    }

    private static void pruneDialogsLocked() {
        Iterator<WeakReference<Dialog>> iterator = DIALOGS.iterator();
        while (iterator.hasNext()) {
            Dialog dialog = iterator.next().get();
            if (dialog == null) {
                iterator.remove();
                continue;
            }
            try {
                if (!dialog.isShowing()) iterator.remove();
            } catch (Throwable ignored) {}
        }
    }

    public static boolean hasActiveTasks() {
        synchronized (LOCK) {
            return sActiveMainCallbacks > 0 || sActiveBackgroundTasks > 0;
        }
    }

    public static int activeTaskCount() {
        synchronized (LOCK) {
            return sActiveMainCallbacks + sActiveBackgroundTasks;
        }
    }

    public static int pendingMainTaskCount() {
        synchronized (LOCK) {
            int count = 0;
            for (Set<Runnable> wrappers : WRAPPERS.values()) count += wrappers.size();
            return count;
        }
    }

    public static boolean isAccepting() {
        synchronized (LOCK) {
            return sAccepting;
        }
    }

    /** Runs final View/WindowManager cleanup on the Android main thread without queueing new work. */
    public static boolean runMainCleanup(Runnable cleanup, long timeoutMs) {
        if (cleanup == null) return true;
        if (Looper.myLooper() == Looper.getMainLooper()) {
            try {
                cleanup.run();
                return true;
            } catch (Throwable ignored) {
                return false;
            }
        }
        final CountDownLatch done = new CountDownLatch(1);
        final boolean[] success = new boolean[] {false};
        Runnable wrapper = new Runnable() {
            @Override
            public void run() {
                try {
                    cleanup.run();
                    success[0] = true;
                } catch (Throwable ignored) {
                    success[0] = false;
                } finally {
                    done.countDown();
                }
            }
        };
        Handler handler;
        synchronized (LOCK) {
            handler = mainHandlerLocked();
        }
        if (!handler.post(wrapper)) return false;
        try {
            if (!done.await(Math.max(0L, timeoutMs), TimeUnit.MILLISECONDS)) {
                handler.removeCallbacks(wrapper);
                return false;
            }
            return success[0];
        } catch (InterruptedException interrupted) {
            handler.removeCallbacks(wrapper);
            Thread.currentThread().interrupt();
            return false;
        }
    }

    public static boolean prepareForHotReload() {
        final List<Runnable> pending;
        final Handler handler;
        final List<RestartableSingleThreadExecutor> executors;
        synchronized (LOCK) {
            if (!sAccepting || sActiveMainCallbacks > 0 || sActiveBackgroundTasks > 0) {
                return false;
            }
            sAccepting = false;
            sGeneration++;
            pending = new ArrayList<>();
            for (Set<Runnable> wrappers : WRAPPERS.values()) pending.addAll(wrappers);
            WRAPPERS.clear();
            handler = sMainHandler;
            executors = new ArrayList<>(EXECUTORS);
        }
        if (handler != null) {
            for (Runnable wrapper : pending) handler.removeCallbacks(wrapper);
        }

        for (RestartableSingleThreadExecutor executor : executors) {
            executor.retireFully();
        }
        return true;
    }

    public static void resumeAfterRejectedReload() {
        List<RestartableSingleThreadExecutor> executors;
        synchronized (LOCK) {
            executors = new ArrayList<>(EXECUTORS);
            sAccepting = true;
        }
        for (RestartableSingleThreadExecutor executor : executors) executor.resume();
    }

    private static final class RestartableSingleThreadExecutor extends AbstractExecutorService {
        private final String threadName;
        private final int priority;
        private volatile ThreadPoolExecutor delegate;

        RestartableSingleThreadExecutor(String threadName, int priority) {
            this.threadName = threadName;
            this.priority = priority;
            delegate = createDelegate();
        }

        @Override
        public void execute(Runnable command) {
            if (command == null) throw new NullPointerException("command");
            final long ownerGeneration;
            final ThreadPoolExecutor executor;
            synchronized (LOCK) {
                if (!sAccepting) throw new RejectedExecutionException("hot reload is preparing");
                ownerGeneration = sGeneration;
                executor = delegate;
                executor.execute(new Runnable() {
                    @Override
                    public void run() {
                        synchronized (LOCK) {
                            if (!sAccepting || sGeneration != ownerGeneration) return;
                            sActiveBackgroundTasks++;
                        }
                        try {
                            synchronized (LOCK) {
                                if (!sAccepting || sGeneration != ownerGeneration) return;
                            }
                            command.run();
                        } finally {
                            synchronized (LOCK) {
                                sActiveBackgroundTasks--;
                            }
                        }
                    }
                });
            }
        }

        void retireFully() {
            ThreadPoolExecutor executor = delegate;
            executor.shutdownNow();
            boolean interrupted = false;
            while (!executor.isTerminated()) {
                try {
                    executor.awaitTermination(100L, TimeUnit.MILLISECONDS);
                } catch (InterruptedException ignored) {
                    interrupted = true;
                }
            }
            if (interrupted) Thread.currentThread().interrupt();
        }

        void resume() {
            synchronized (LOCK) {
                if (!sAccepting) return;
                if (delegate.isShutdown() || delegate.isTerminated()) {
                    delegate = createDelegate();
                }
            }
        }

        @Override
        public void shutdown() {
            delegate.shutdown();
        }

        @Override
        public List<Runnable> shutdownNow() {
            return delegate.shutdownNow();
        }

        @Override
        public boolean isShutdown() {
            return delegate.isShutdown();
        }

        @Override
        public boolean isTerminated() {
            return delegate.isTerminated();
        }

        @Override
        public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
            return delegate.awaitTermination(timeout, unit);
        }

        private ThreadPoolExecutor createDelegate() {
            ThreadFactory factory = new ThreadFactory() {
                @Override
                public Thread newThread(Runnable runnable) {
                    Thread thread = new Thread(runnable, threadName);
                    thread.setPriority(priority);
                    return thread;
                }
            };
            ThreadPoolExecutor executor = new ThreadPoolExecutor(
                1,
                1,
                0L,
                TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<>(),
                factory
            );
            executor.allowCoreThreadTimeOut(false);
            return executor;
        }
    }
}

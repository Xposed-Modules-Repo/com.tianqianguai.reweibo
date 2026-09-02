package com.tianqianguai.reweibo;

/** Orders fallible cleanup before the irreversible generation quiesce commit point. */
final class HotReloadPreparation {
    interface Step {
        boolean run();
    }

    private HotReloadPreparation() {}

    static boolean run(Step uiCleanup, Step cliCleanup, Step destructiveQuiesce) {
        if (!uiCleanup.run()) return false;
        if (!cliCleanup.run()) return false;
        return destructiveQuiesce.run();
    }
}

package com.tianqianguai.reweibo;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public final class CliContract {
    public static final String ACTION_COMMAND = "com.tianqianguai.reweibo.action.CLI_COMMAND";
    public static final String EXTRA_COMMAND = "command";
    public static final String EXTRA_FULL_COMMAND = "full_command";

    public static final String PACKAGE_WEICO = "com.weico.international";

    private static final Map<String, Command> COMMANDS;

    static {
        LinkedHashMap<String, Command> commands = new LinkedHashMap<>();
        add(commands, "weico.status", PACKAGE_WEICO, "status");
        add(commands, "weico.timeline.top", PACKAGE_WEICO, "timeline.top");
        add(commands, "weico.timeline.bottom", PACKAGE_WEICO, "timeline.bottom");
        add(commands, "weico.timeline.jump", PACKAGE_WEICO, "timeline.jump");
        add(commands, "weico.cache.stats", PACKAGE_WEICO, "cache.stats");
        add(commands, "weico.cache.clear", PACKAGE_WEICO, "cache.clear");
        add(commands, "weico.preload.restart", PACKAGE_WEICO, "preload.restart");
        add(commands, "weico.settings.reload", PACKAGE_WEICO, "settings.reload");
        COMMANDS = Collections.unmodifiableMap(commands);
    }

    private CliContract() {}

    private static void add(
            LinkedHashMap<String, Command> commands,
            String fullName,
            String targetPackage,
            String localName
    ) {
        commands.put(fullName, new Command(fullName, targetPackage, localName));
    }

    public static Command commandFor(String fullName) {
        return COMMANDS.get(fullName);
    }

    public static String[] allCommandNames() {
        return COMMANDS.keySet().toArray(new String[0]);
    }

    public static final class Command {
        public final String fullName;
        public final String targetPackage;
        public final String localName;

        private Command(String fullName, String targetPackage, String localName) {
            this.fullName = fullName;
            this.targetPackage = targetPackage;
            this.localName = localName;
        }
    }
}

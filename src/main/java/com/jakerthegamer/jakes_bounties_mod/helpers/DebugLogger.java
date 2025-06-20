package com.jakerthegamer.jakes_bounties_mod.helpers;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Optional;

public class DebugLogger {
    private static final Logger LOGGER = LogManager.getLogger();
    private static boolean debugMode = false;

    public static void error(String msg){
        if (debugMode)
            LOGGER.error(msg);
    }
    public static void error(String msg, Throwable e){
        if (debugMode)
            LOGGER.error(msg, e);
    }
    public static void error(String msg, boolean debugModeOverride){
        if (debugMode || debugModeOverride)
            LOGGER.error(msg);
    }
    public static void error(String msg, Throwable e, boolean debugModeOverride){
        if (debugMode || debugModeOverride)
            LOGGER.error(msg, e);
    }
    public static void info(String msg){
        if (debugMode)
            LOGGER.error(msg);
    }
    public static void warn(String msg){
        if (debugMode)
            LOGGER.error(msg);
    }
    public static void debug(String msg){
        if (debugMode)
            LOGGER.error(msg);
    }
}

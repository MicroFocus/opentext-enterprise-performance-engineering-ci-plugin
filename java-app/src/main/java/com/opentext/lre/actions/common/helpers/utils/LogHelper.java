package com.opentext.lre.actions.common.helpers.utils;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;

public class LogHelper {

    public static Logger logger;
    private static boolean stackTraceEnabled = false;

    public static synchronized void setup(String logFilePath, boolean enableStackTrace) throws Exception {
        stackTraceEnabled = enableStackTrace;

        // Ensure directory exists
        File file = new File(logFilePath);
        File parent = file.getParentFile();
        if (!parent.exists() && !parent.mkdirs()) {
            throw new Exception("Failed to create log directory: " + parent.getAbsolutePath());
        }

        // The log.file system property is already set in Main.java before calling this method
        // log4j2.xml uses ${sys:log.file} to configure the file appender
        // Simply get the logger - log4j2 will initialize with the system property
        logger = LogManager.getLogger(LogHelper.class);

        logger.info("Log file path set to: {}", logFilePath);
    }

    public static void log(String message, boolean addDate, Object... args) {
        logger.info(String.format(message, args));
    }

    public static void error(String message) {
        logger.error(message);
    }

    public static void error(String message, Throwable throwable) {
        if (stackTraceEnabled && throwable != null) {
            logger.error(message, throwable);
        } else if (throwable != null && throwable.getMessage() != null) {
            logger.error("{} - {}", message, throwable.getMessage());
        } else {
            logger.error(message);
        }
    }

    public static void logStackTrace(Throwable throwable) {
        if (stackTraceEnabled || (throwable != null && throwable.getMessage() == null)) {
            logger.error("Error - Stack Trace: ", throwable);
        } else if (throwable != null) {
            logger.error(throwable.getMessage());
        }
    }
    public static void logStackTrace(String errorMessage, Throwable throwable) {
        if(stackTraceEnabled || (throwable != null && throwable.getMessage().trim().isEmpty())) {
            logger.error("Error: {} Stack Trace: ", errorMessage, throwable);
        } else if(throwable != null) {
            logger.error("{} - {}", errorMessage, throwable.getMessage());
        }
    }
}

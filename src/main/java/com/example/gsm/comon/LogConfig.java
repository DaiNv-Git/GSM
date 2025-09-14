package com.example.gsm.comon;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.text.MessageFormat;

public class LogConfig {

    Logger logger = null;

    Class<?> clazz;

    public LogConfig(Class<?> clazz) {
        super();
        this.clazz = clazz;
        logger = LogManager.getLogger(clazz);
    }

    public String getMessage(String string, Object... args) {
        String message = "";
        if (args.length > 0) {
            message = MessageFormat.format(string, args);
        } else {
            message = string;
        }
        return message;
    }

    public void info(String string, Object... args) {
        logger.info(getMessage(string, args));
    }

    public void error(String string, Object... args) {
        logger.error(getMessage(string, args));
    }

    public void warn(String string, Object... args) {
        logger.warn(getMessage(string, args));
    }

    public void log(Level level, String string, Object... args) {
        logger.log(level, string, args);
    }
}

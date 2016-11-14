package ar.edu.itba.pdc.chinese_whispers.application;

import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * Log manager that keeps track of loggers per class, giving the same logger instance to every call of the same class.
 */
public class LogHelper {
    private static Map<Class, org.slf4j.Logger> loggers = new HashMap<>();

    public static org.slf4j.Logger getLogger(Class klass) {
        if(!loggers.containsKey(klass)) {
            loggers.put(klass, LoggerFactory.getLogger(klass));
        }
        return loggers.get(klass);
    }
}

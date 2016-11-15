package ar.edu.itba.pdc.chinese_whispers.application;

import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * Log manager that keeps track of loggers per class, giving the same logger instance to every call of the same class.
 */
public class LogHelper {
    private static Map<Class, org.slf4j.Logger> loggers = new HashMap<>();

    /**
     * Gets a {@link org.slf4j.Logger} instance for the specified class. All instances of the same class will receive
     * the same logger instance. This balances traceability of logs (each class will prefix its messages with the class
     * name) with performance (avoid creating many logger instances).
     *
     * @param klass The class for which to log.
     * @return The corresponding logger instance.
     */
    public static org.slf4j.Logger getLogger(Class klass) {
        if(!loggers.containsKey(klass)) {
            loggers.put(klass, LoggerFactory.getLogger(klass));
        }
        return loggers.get(klass);
    }
}

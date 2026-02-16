package org.openimmunizationsoftware.cdsi.core.logic.items;

import java.util.List;

/**
 * Interface for logging functionality shared by both LogicStep and
 * LogicOutcome.
 * 
 * Provides a contract for level-based logging and event tracking across the
 * CDSI logic evaluation framework.
 */
public interface LogSink {
    /**
     * Log a message at DUMP level (most verbose).
     * 
     * @param s The message to log
     */
    void log(String s);

    /**
     * Log a message at the specified level.
     * 
     * For guidance on choosing the appropriate log level,
     * see: docs/Alerting Semantics for Step Into CDSi.md
     * 
     * @param logLevel The log level
     * @param s        The message to log
     */
    void log(LogLevel logLevel, String s);

    /**
     * Returns the list of log events with level and alert information.
     * 
     * @return Unmodifiable list of log events
     */
    List<LogEvent> getLogEventList();

    /**
     * Returns the list of log messages as strings, preserving order.
     * 
     * @return List of log messages
     */
    List<String> getLogList();
}

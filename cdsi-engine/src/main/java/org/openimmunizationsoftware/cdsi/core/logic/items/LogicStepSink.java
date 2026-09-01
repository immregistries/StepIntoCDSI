package org.openimmunizationsoftware.cdsi.core.logic.items;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * LogicStepSink handles logging functionality for LogicStep.
 * 
 * This class encapsulates the LogSink interface implementation, managing
 * log events and providing filtering/retrieval capabilities.
 */
public class LogicStepSink implements LogSink {
    private List<LogEvent> logEventList = new ArrayList<LogEvent>();
    private long nextSeq = 0;

    /**
     * Log a message at DUMP level (most verbose).
     * Maintained for backwards compatibility.
     * 
     * @param s The message to log
     */
    @Override
    public void log(String s) {
        log(LogLevel.DUMP, s);
    }

    /**
     * Log a message at the specified level.
     * 
     * For guidance on choosing the appropriate log level,
     * see: docs/Alerting Semantics for Step Into CDSi.md
     * 
     * @param logLevel The log level
     * @param s        The message to log
     */
    @Override
    public void log(LogLevel logLevel, String s) {
        logEventList.add(new LogEvent(logLevel, false, s, nextSeq++));
    }

    /**
     * Returns the list of log events with level and alert information.
     * 
     * @return Unmodifiable list of log events
     */
    @Override
    public List<LogEvent> getLogEventList() {
        return Collections.unmodifiableList(logEventList);
    }

    /**
     * Returns the list of log messages as strings, preserving order.
     * Maintained for backwards compatibility.
     * 
     * @return List of log messages
     */
    @Override
    public List<String> getLogList() {
        List<String> messages = new ArrayList<String>();
        for (LogEvent event : logEventList) {
            messages.add(event.getMessage());
        }
        return messages;
    }

    /**
     * Log an alert message at the specified level.
     * 
     * Alerts are used to flag important conditions that require attention.
     * For guidance on when to use alerts vs. regular logs,
     * see: docs/Alerting Semantics for Step Into CDSi.md
     * 
     * @param logLevel The log level
     * @param message  The alert message to log
     */
    public void alert(LogLevel logLevel, String message) {
        logEventList.add(new LogEvent(logLevel, true, message, nextSeq++));
    }
}

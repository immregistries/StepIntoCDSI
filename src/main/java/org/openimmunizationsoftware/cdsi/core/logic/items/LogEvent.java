package org.openimmunizationsoftware.cdsi.core.logic.items;

/**
 * Represents a single log event with level, alert flag, and message.
 * 
 * For detailed guidance on logging semantics, level usage, and alerting
 * patterns, see: docs/Alerting Semantics for Step Into CDSi.md
 */
public class LogEvent {
    private final LogLevel level;
    private final boolean alert;
    private final String message;
    private final long seq;

    public LogEvent(LogLevel level, boolean alert, String message, long seq) {
        this.level = level;
        this.alert = alert;
        this.message = message != null ? message : "null";
        this.seq = seq;
    }

    public LogLevel getLevel() {
        return level;
    }

    public boolean isAlert() {
        return alert;
    }

    public String getMessage() {
        return message;
    }

    public long getSeq() {
        return seq;
    }
}

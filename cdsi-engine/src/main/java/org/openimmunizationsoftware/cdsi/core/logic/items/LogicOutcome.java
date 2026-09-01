package org.openimmunizationsoftware.cdsi.core.logic.items;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public abstract class LogicOutcome implements LogSink {
  public abstract void perform();

  private List<LogEvent> logEventList = new ArrayList<LogEvent>();
  private long nextSeq = 0;
  private LogSink logicStepSink = null;

  /**
   * Log a message at DUMP level (most verbose).
   * Maintained for backwards compatibility.
   * 
   * @param s The message to log
   */
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
  public void log(LogLevel logLevel, String s) {
    if (logEventList != null) {
      logEventList.add(new LogEvent(logLevel, false, s, nextSeq++));
    }
    // Also log to parent sink if set
    if (logicStepSink != null) {
      logicStepSink.log(logLevel, s);
    }
  }

  /**
   * Log a message at the specified level (deprecated - parameter order is
   * backwards).
   * Use {@link #log(LogLevel, String)} instead.
   * 
   * @param s        The message to log
   * @param logLevel The log level
   * @deprecated Use {@link #log(LogLevel, String)} with correct parameter order
   */
  @Deprecated(since = "2026-02-16", forRemoval = false)
  protected void log(String s, LogLevel logLevel) {
    log(logLevel, s);
  }

  /**
   * Set the LogicSink for this LogicOutcome.
   * This allows LogicOutcome to log to the same sink as its parent LogicStep.
   * 
   * @param logicStepSink The LogicSink to use for logging
   */
  public void setLogicStepSink(LogSink logicStepSink) {
    this.logicStepSink = logicStepSink;
  }

  /**
   * Returns the list of log messages as strings, preserving order.
   * Maintained for backwards compatibility.
   * 
   * @return List of log messages
   */
  public List<String> getLogList() {
    List<String> messages = new ArrayList<String>();
    for (LogEvent event : logEventList) {
      messages.add(event.getMessage());
    }
    return messages;
  }

  /**
   * Returns the list of log events with level and alert information.
   * 
   * @return Unmodifiable list of log events
   */
  public List<LogEvent> getLogEventList() {
    return Collections.unmodifiableList(logEventList);
  }
}

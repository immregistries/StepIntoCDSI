package org.openimmunizationsoftware.cdsi.core.logic.items;

/**
 * Log levels ordered from least verbose (CONTROL) to most verbose (DUMP).
 * 
 * For detailed guidance on logging semantics, level usage, and alerting
 * patterns, see: docs/Alerting Semantics for Step Into CDSi.md
 */
public enum LogLevel {
    CONTROL, // Least verbose - control flow decisions
    STATE, // State changes
    REASONING, // Reasoning and business logic
    TRACE, // Detailed trace information
    DUMP // Most verbose - all details
}

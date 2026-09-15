package org.openimmunizationsoftware.cdsi.core.domain.datatypes;

/**
 * Supporting Data {@code <context>} on a {@code <conditionalSkip>} instance.
 * Table 6-4's entry condition (and 7.1 / 7.6.1's mirrors) filter by this:
 * evaluation uses Evaluation or Both; forecasting and validating use Forecast
 * or Both.
 */
public enum ConditionalSkipContext {
  EVALUATION, FORECAST, BOTH;

  public static ConditionalSkipContext fromXml(String value) {
    if (value == null) {
      return null;
    }
    String trimmed = value.trim();
    if (trimmed.isEmpty()) {
      return null;
    }
    if (trimmed.equalsIgnoreCase("Evaluation")) {
      return EVALUATION;
    }
    if (trimmed.equalsIgnoreCase("Forecast")) {
      return FORECAST;
    }
    if (trimmed.equalsIgnoreCase("Both")) {
      return BOTH;
    }
    return null;
  }
}

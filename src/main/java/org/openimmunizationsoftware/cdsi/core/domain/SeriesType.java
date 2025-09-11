package org.openimmunizationsoftware.cdsi.core.domain;

public enum SeriesType {
    RISK, STANDARD, EVALUATION_ONLY;

    public static SeriesType getSeriestType(String value) {
        if (value.equalsIgnoreCase("Risk")) {
            return RISK;
        } else if (value.equalsIgnoreCase("Standard")) {
            return STANDARD;
        } else if (value.equalsIgnoreCase("Evaluation Only")) {
            return EVALUATION_ONLY;
        } else {
            return null;
        }
    }

    public String toString() {
        switch (this) {
            case RISK:
                return "Risk";
            case STANDARD:
                return "Standard";
            case EVALUATION_ONLY:
                return "Evaluation Only";
            default:
                return null;
        }
    }
}

package org.openimmunizationsoftware.cdsi.core.logic.items;

public enum LogicResult {
    YES, NO, ANY, UNKNOWN, EXTRANEOUS, ZERO, ONE, MORE_THAN_ONE;

    public static LogicResult fromString(String result) {
        if ("YES".equalsIgnoreCase(result)) {
            return YES;
        } else if ("NO".equalsIgnoreCase(result)) {
            return NO;
        } else if ("ANY".equalsIgnoreCase(result)) {
            return ANY;
        } else if ("UNKNOWN".equalsIgnoreCase(result)) {
            return UNKNOWN;
        } else if ("EXTRANEOUS".equalsIgnoreCase(result)) {
            return EXTRANEOUS;
        } else if ("0".equalsIgnoreCase(result)) {
            return ZERO;
        } else if ("1".equalsIgnoreCase(result)) {
            return ONE;
        } else if (">1".equalsIgnoreCase(result)) {
            return MORE_THAN_ONE;
        } else {
            throw new IllegalArgumentException("Unknown LogicResult: " + result);
        }
    }
}

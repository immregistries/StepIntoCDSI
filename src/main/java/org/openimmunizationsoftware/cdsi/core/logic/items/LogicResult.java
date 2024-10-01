package org.openimmunizationsoftware.cdsi.core.logic.items;

public enum LogicResult {
  YES, NO, ANY, EXTRANEOUS;

  public static LogicResult fromString(String result) {
    if ("YES".equals(result)) {
        return YES;
    } else if ("NO".equals(result)) {
        return NO;
    } else if ("ANY".equals(result)) {
        return ANY;
    } else if ("EXTRANEOUS".equals(result)) {
        return EXTRANEOUS;
    } else {
        throw new IllegalArgumentException("Unknown LogicResult: " + result);
    }
  }
}


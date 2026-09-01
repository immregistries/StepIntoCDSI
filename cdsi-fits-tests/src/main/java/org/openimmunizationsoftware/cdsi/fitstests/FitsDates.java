package org.openimmunizationsoftware.cdsi.fitstests;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * ISO-8601 (yyyy-MM-dd) date formatting shared by the fixture DTO, the
 * runner, and the downloader. Deliberately uses the JVM default timezone
 * (not UTC), matching every other date field the engine parses (e.g.
 * ForecastServlet's "yyyyMMdd" request parsing) - mixing timezones across
 * date fields that later get subtracted from each other is how you get
 * off-by-one-day bugs.
 */
public final class FitsDates {

  private FitsDates() {
  }

  private static SimpleDateFormat newFormat() {
    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
    sdf.setLenient(false);
    return sdf;
  }

  public static Date parse(String value) {
    if (value == null || value.trim().isEmpty()) {
      return null;
    }
    try {
      return newFormat().parse(value.trim());
    } catch (ParseException e) {
      throw new IllegalArgumentException("Not a yyyy-MM-dd date: '" + value + "'", e);
    }
  }

  public static String format(Date date) {
    if (date == null) {
      return null;
    }
    return newFormat().format(date);
  }
}

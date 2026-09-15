package org.openimmunizationsoftware.cdsi.core.logic.concepts;

import java.util.Calendar;
import java.util.Date;

import org.openimmunizationsoftware.cdsi.core.domain.SeasonalRecommendation;
import org.openimmunizationsoftware.cdsi.core.domain.SeriesDose;

/**
 * Assessment-relative projection of Supporting Data seasonal recommendation
 * windows.
 *
 * <p>
 * CDC Supporting Data ships a single absolute {@code <startDate>}/{@code <endDate>}
 * pair per seasonal series dose (e.g. Influenza 2025-07-01 through 2026-06-30).
 * The Logic Specification treats those dates literally. When CDC's next-season
 * release lags, or when FITS fixtures carry an assessment date in a different
 * flu year than the bundled release, that literal window is the wrong year for
 * the patient being assessed.
 *
 * <p>
 * This helper keeps the loaded dates as a <em>template</em> (month/day span and
 * length) and shifts them by whole years so the assessment falls in an
 * analogous season - or, when the assessment lands in a gap between seasons
 * (e.g. Sep-Mar RSV-style windows), keeps the <em>upcoming</em> season so the
 * next start date can still be forecast.
 *
 * <p>
 * Documented deviation from the specification: see step packages 7.4 and 7.5.
 */
public final class SeasonalRecommendationDates {

  private SeasonalRecommendationDates() {
  }

  /**
   * Effective seasonal recommendation start date for this assessment, or
   * {@code null} if the series dose declares no season or no start.
   */
  public static Date effectiveStartDate(SeriesDose seriesDose, Date assessmentDate) {
    SeasonalRecommendation season = firstSeason(seriesDose);
    if (season == null) {
      return null;
    }
    return project(season.getSeasonalRecommendationStartDate(),
        season.getSeasonalRecommendationEndDate(), assessmentDate)[0];
  }

  /**
   * Effective seasonal recommendation end date for this assessment, or
   * {@code null} if the series dose declares no season or no end.
   */
  public static Date effectiveEndDate(SeriesDose seriesDose, Date assessmentDate) {
    SeasonalRecommendation season = firstSeason(seriesDose);
    if (season == null) {
      return null;
    }
    return project(season.getSeasonalRecommendationStartDate(),
        season.getSeasonalRecommendationEndDate(), assessmentDate)[1];
  }

  /**
   * Projects {@code [templateStart, templateEnd]} by whole-year steps relative
   * to {@code assessmentDate}. Returns {@code [start, end]} (either may be null
   * if the template was incomplete). When {@code assessmentDate} is null, the
   * template is returned unchanged.
   */
  public static Date[] project(Date templateStart, Date templateEnd, Date assessmentDate) {
    if (templateStart == null && templateEnd == null) {
      return new Date[] { null, null };
    }
    if (assessmentDate == null || templateStart == null || templateEnd == null) {
      return new Date[] { templateStart, templateEnd };
    }

    Date start = templateStart;
    Date end = templateEnd;

    // Assessment after the window: roll forward year by year until inside or past
    // a gap we cannot cover with this template's month span.
    int guard = 0;
    while (assessmentDate.after(end) && guard++ < 200) {
      start = addYears(start, 1);
      end = addYears(end, 1);
    }

    // Assessment before the window: roll back while the previous window still
    // contains the assessment. If the assessment sits in a gap between seasons
    // (after the previous end and before this start), keep the *upcoming*
    // season so FORECASTDTCAN-1 can use the next start date rather than
    // treating the patient as past end (RSV summer assessments, etc.).
    guard = 0;
    while (assessmentDate.before(start) && guard++ < 200) {
      Date previousStart = addYears(start, -1);
      Date previousEnd = addYears(end, -1);
      if (!assessmentDate.before(previousStart) && !assessmentDate.after(previousEnd)) {
        start = previousStart;
        end = previousEnd;
        break;
      }
      if (assessmentDate.after(previousEnd) && assessmentDate.before(start)) {
        break;
      }
      start = previousStart;
      end = previousEnd;
    }

    return new Date[] { start, end };
  }

  private static SeasonalRecommendation firstSeason(SeriesDose seriesDose) {
    if (seriesDose == null || seriesDose.getSeasonalRecommendationList().isEmpty()) {
      return null;
    }
    return seriesDose.getSeasonalRecommendationList().get(0);
  }

  private static Date addYears(Date date, int years) {
    Calendar calendar = Calendar.getInstance();
    calendar.setTime(date);
    calendar.add(Calendar.YEAR, years);
    return calendar.getTime();
  }
}

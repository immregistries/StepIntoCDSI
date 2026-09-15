package org.openimmunizationsoftware.cdsi.core.logic.concepts;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.openimmunizationsoftware.cdsi.core.domain.Age;
import org.openimmunizationsoftware.cdsi.core.domain.AllowableInterval;
import org.openimmunizationsoftware.cdsi.core.domain.Interval;

/**
 * Section 3.3 / Table 3-5 selection of Supporting Data logical-component
 * instances by Effective Date and Cessation Date (RELEVANT-1 / RELEVANT-2).
 *
 * <p>
 * A component is used when the anchor date (date administered for evaluation,
 * assessment date for forecasting) is on or after the effective date and on or
 * before the cessation date. Unvalued dates default to 01/01/1900 and
 * 12/31/2999 (Table 3-4).
 */
public final class RelevantSupportingData {

  private static final Date DEFAULT_EFFECTIVE = date(1900, Calendar.JANUARY, 1);
  private static final Date DEFAULT_CESSATION = date(2999, Calendar.DECEMBER, 31);

  private RelevantSupportingData() {
  }

  /**
   * Whether a logical-component instance is relevant for the given anchor date
   * (RELEVANT-1 / RELEVANT-2).
   */
  public static boolean isRelevant(Date effectiveDate, Date cessationDate, Date anchorDate) {
    if (anchorDate == null) {
      return true;
    }
    Date effective = effectiveDate != null ? effectiveDate : DEFAULT_EFFECTIVE;
    Date cessation = cessationDate != null ? cessationDate : DEFAULT_CESSATION;
    return !anchorDate.before(effective) && !anchorDate.after(cessation);
  }

  /**
   * First age row relevant for {@code anchorDate}, or {@code null} if none.
   */
  public static Age selectAge(List<Age> ages, Date anchorDate) {
    if (ages == null || ages.isEmpty()) {
      return null;
    }
    for (Age age : ages) {
      if (isRelevant(age.getEffectiveDate(), age.getCessationDate(), anchorDate)) {
        return age;
      }
    }
    return null;
  }

  /**
   * Preferable-interval rows relevant for {@code anchorDate}.
   */
  public static List<Interval> selectIntervals(List<Interval> intervals, Date anchorDate) {
    List<Interval> selected = new ArrayList<Interval>();
    if (intervals == null) {
      return selected;
    }
    for (Interval interval : intervals) {
      if (isRelevant(interval.getEffectiveDate(), interval.getCessationDate(), anchorDate)) {
        selected.add(interval);
      }
    }
    return selected;
  }

  /**
   * Allowable-interval rows relevant for {@code anchorDate}.
   */
  public static List<AllowableInterval> selectAllowableIntervals(List<AllowableInterval> intervals,
      Date anchorDate) {
    List<AllowableInterval> selected = new ArrayList<AllowableInterval>();
    if (intervals == null) {
      return selected;
    }
    for (AllowableInterval interval : intervals) {
      if (isRelevant(interval.getEffectiveDate(), interval.getCessationDate(), anchorDate)) {
        selected.add(interval);
      }
    }
    return selected;
  }

  private static Date date(int year, int month, int dayOfMonth) {
    Calendar calendar = Calendar.getInstance();
    calendar.clear();
    calendar.set(year, month, dayOfMonth);
    return calendar.getTime();
  }
}

package org.openimmunizationsoftware.cdsi.core.logic.concepts;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.junit.Test;
import org.openimmunizationsoftware.cdsi.core.domain.Age;
import org.openimmunizationsoftware.cdsi.core.domain.AllowableInterval;
import org.openimmunizationsoftware.cdsi.core.domain.Interval;
import org.openimmunizationsoftware.cdsi.core.domain.datatypes.TimePeriod;

/**
 * Table 3-5 RELEVANT-1 / RELEVANT-2 selection of Age and Preferable Interval
 * rows by Effective / Cessation Date.
 */
public class RelevantSupportingDataTest {

  @Test
  public void unvaluedDatesAreAlwaysRelevant() {
    assertTrue(RelevantSupportingData.isRelevant(null, null, date("2026-09-01")));
  }

  @Test
  public void inclusiveEffectiveAndCessationBounds() {
    Date effective = date("2009-08-07");
    Date cessation = date("2999-12-31");
    assertTrue(RelevantSupportingData.isRelevant(effective, cessation, date("2009-08-07")));
    assertTrue(RelevantSupportingData.isRelevant(effective, cessation, date("2026-09-01")));
    assertFalse(RelevantSupportingData.isRelevant(effective, cessation, date("2009-08-06")));
  }

  @Test
  public void ceasedRowEndsOnCessationDateInclusive() {
    Date effective = date("1900-01-01");
    Date cessation = date("2009-08-06");
    assertTrue(RelevantSupportingData.isRelevant(effective, cessation, date("2009-08-06")));
    assertFalse(RelevantSupportingData.isRelevant(effective, cessation, date("2009-08-07")));
  }

  @Test
  public void selectAgePicksCurrentPolioDose4RowForModernAssessment() {
    Age ceased = age(date("1900-01-01"), date("2009-08-06"), "18 weeks");
    Age current = age(date("2009-08-07"), null, "4 years");

    Age selected = RelevantSupportingData.selectAge(Arrays.asList(ceased, current), date("2026-09-01"));
    assertSame(current, selected);
  }

  @Test
  public void selectAgePicksCeasedRowOnItsLastValidDay() {
    Age ceased = age(date("1900-01-01"), date("2009-08-06"), "18 weeks");
    Age current = age(date("2009-08-07"), null, "4 years");

    Age selected = RelevantSupportingData.selectAge(Arrays.asList(ceased, current), date("2009-08-06"));
    assertSame(ceased, selected);
  }

  @Test
  public void selectIntervalsDropsCeasedPreferableInterval() {
    Interval ceased = interval(date("1900-01-01"), date("2009-08-06"), "4 weeks");
    Interval current = interval(date("2009-08-07"), null, "6 months");

    List<Interval> selected = RelevantSupportingData.selectIntervals(Arrays.asList(ceased, current),
        date("2026-09-01"));
    assertEquals(1, selected.size());
    assertSame(current, selected.get(0));
  }

  @Test
  public void selectAgeReturnsNullWhenListEmpty() {
    assertNull(RelevantSupportingData.selectAge(Collections.<Age>emptyList(), date("2026-09-01")));
  }

  @Test
  public void selectAllowableIntervalsDropsCeasedRow() {
    AllowableInterval ceased = allowableInterval(date("1900-01-01"), date("2009-08-06"));
    AllowableInterval current = allowableInterval(date("2009-08-07"), null);

    List<AllowableInterval> selected = RelevantSupportingData.selectAllowableIntervals(
        Arrays.asList(ceased, current), date("2026-09-01"));
    assertEquals(1, selected.size());
    assertSame(current, selected.get(0));
  }

  private static Age age(Date effective, Date cessation, String minAge) {
    Age age = new Age();
    age.setEffectiveDate(effective);
    age.setCessationDate(cessation);
    age.setMinimugeAge(new TimePeriod(minAge));
    return age;
  }

  private static Interval interval(Date effective, Date cessation, String minInt) {
    Interval interval = new Interval();
    interval.setEffectiveDate(effective);
    interval.setCessationDate(cessation);
    interval.setMinimumInterval(new TimePeriod(minInt));
    return interval;
  }

  private static AllowableInterval allowableInterval(Date effective, Date cessation) {
    AllowableInterval interval = new AllowableInterval();
    interval.setEffectiveDate(effective);
    interval.setCessationDate(cessation);
    return interval;
  }

  private static Date date(String iso) {
    try {
      return new SimpleDateFormat("yyyy-MM-dd").parse(iso);
    } catch (ParseException e) {
      throw new IllegalArgumentException(iso, e);
    }
  }
}

package org.openimmunizationsoftware.cdsi.core.logic.concepts;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.junit.Test;

/**
 * Assessment-relative projection of Supporting Data seasonal windows - the
 * documented deviation used by 7.4 and 7.5 when CDC's absolute season dates
 * do not match the assessment's flu year.
 */
public class SeasonalRecommendationDatesTest {

  @Test
  public void leavesTheTemplateAloneWhenAssessmentFallsInsideIt() {
    Date[] projected = SeasonalRecommendationDates.project(date("07/01/2025"), date("06/30/2026"),
        date("11/15/2025"));

    assertEquals(date("07/01/2025"), projected[0]);
    assertEquals(date("06/30/2026"), projected[1]);
  }

  @Test
  public void rollsTheTemplateBackForAHistoricalAssessmentYear() {
    // Bundled Influenza 4.65 season with a 2020 FITS evaluation date.
    Date[] projected = SeasonalRecommendationDates.project(date("07/01/2025"), date("06/30/2026"),
        date("08/01/2020"));

    assertEquals(date("07/01/2020"), projected[0]);
    assertEquals(date("06/30/2021"), projected[1]);
  }

  @Test
  public void rollsTheTemplateForwardWhenAssessmentIsPastTheEnd() {
    // CDC lag: assessment in the new flu year while SD still ends 06/30/2026.
    Date[] projected = SeasonalRecommendationDates.project(date("07/01/2025"), date("06/30/2026"),
        date("09/15/2026"));

    assertEquals(date("07/01/2026"), projected[0]);
    assertEquals(date("06/30/2027"), projected[1]);
  }

  @Test
  public void usesTheUpcomingSeasonWhenAssessmentFallsInAGap() {
    // Sep-Mar style window: assessment in June sits between seasons. Keep the
    // upcoming season so the next start date can still be forecast.
    Date[] projected = SeasonalRecommendationDates.project(date("09/01/2024"), date("03/31/2025"),
        date("06/15/2025"));

    assertEquals(date("09/01/2025"), projected[0]);
    assertEquals(date("03/31/2026"), projected[1]);
  }

  @Test
  public void returnsTheTemplateUnchangedWhenAssessmentIsNull() {
    Date[] projected = SeasonalRecommendationDates.project(date("07/01/2025"), date("06/30/2026"),
        null);

    assertEquals(date("07/01/2025"), projected[0]);
    assertEquals(date("06/30/2026"), projected[1]);
  }

  @Test
  public void returnsNullsWhenTheTemplateIsEmpty() {
    Date[] projected = SeasonalRecommendationDates.project(null, null, date("08/01/2020"));

    assertNull(projected[0]);
    assertNull(projected[1]);
  }

  @Test
  public void openEndedSeasonRollsStartBackSoAssessmentIsOnOrAfterIt() {
    // COVID-19 4.65: start 2025-08-27, empty end. A 2024 FITS assessment must
    // not wait for the literal 2025 start - roll back to the prior anniversary
    // that is already open (2023-08-27 for an Aug 2024 assessment).
    Date[] projected = SeasonalRecommendationDates.project(date("08/27/2025"), null,
        date("08/22/2024"));

    assertEquals(date("08/27/2023"), projected[0]);
    assertNull(projected[1]);
  }

  @Test
  public void openEndedSeasonLeavesStartAloneWhenAssessmentIsAlreadyOnOrAfterIt() {
    Date[] projected = SeasonalRecommendationDates.project(date("08/27/2025"), null,
        date("09/01/2025"));

    assertEquals(date("08/27/2025"), projected[0]);
    assertNull(projected[1]);
  }

  private static Date date(String monthDayYear) {
    try {
      return new SimpleDateFormat("MM/dd/yyyy").parse(monthDayYear);
    } catch (ParseException pe) {
      throw new IllegalArgumentException(pe);
    }
  }
}

package org.openimmunizationsoftware.cdsi.core.domain.datatypes;

import static org.junit.Assert.assertEquals;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.junit.Test;

public class TestTimePeriod {

  private static final SimpleDateFormat SDF = new SimpleDateFormat("MM/dd/yyyy");

  private static Date date(String mmddyyyy) {
    try {
      return SDF.parse(mmddyyyy);
    } catch (ParseException pe) {
      throw new IllegalArgumentException(pe);
    }
  }

  /**
   * CALCDT-5, verbatim: "A computed date which is not a real date must be
   * moved forward to first day of the next month," with the spec's own two
   * worked examples.
   */
  @Test
  public void calcdtFiveAComputedDateThatIsNotARealDateMovesForwardToTheFirstOfTheNextMonth() {
    assertEquals("03/31/2000 + 6 months = 10/01/2000 (September 31 does not exist)",
        date("10/01/2000"), new TimePeriod("6 months").getDateFrom(date("03/31/2000")));
    assertEquals("08/31/2000 + 6 months = 03/01/2001 (February 31 does not exist)",
        date("03/01/2001"), new TimePeriod("6 months").getDateFrom(date("08/31/2000")));
  }

  /**
   * The same rule against a FITS-observed case: 08/29/2026 + 6 months lands
   * on 02/29/2027, which does not exist (2027 is not a leap year) - CALCDT-5
   * says this must move forward to 03/01/2027.
   */
  @Test
  public void calcdtFiveAlsoCoversLandingOnAFebruaryTwentyNinthInANonLeapYear() {
    assertEquals(date("03/01/2027"), new TimePeriod("6 months").getDateFrom(date("08/29/2026")));
  }

  @Test
  public void test() {
    assertEquals("4 weeks", new TimePeriod("4 weeks").toString());
    assertEquals("4 weeks", new TimePeriod(" 4 weeks").toString());
    assertEquals("4 weeks", new TimePeriod("4 weeks ").toString());
    assertEquals("4 weeks", new TimePeriod("  4 weeks").toString());
    assertEquals("4 weeks", new TimePeriod("  4 weeks  ").toString());
    assertEquals("- 4 weeks", new TimePeriod("-4 weeks").toString());
    assertEquals("- 4 weeks", new TimePeriod(" -4 weeks").toString());
    assertEquals("- 4 weeks", new TimePeriod("-4 weeks ").toString());
    assertEquals("- 4 weeks", new TimePeriod(" - 4 weeks").toString());
    assertEquals("- 4 weeks", new TimePeriod("-  4 weeks  ").toString());
    assertEquals("4 weeks 2 days", new TimePeriod("4 weeks 2 days").toString());
    assertEquals("4 weeks 1 day", new TimePeriod("4 weeks 1 days").toString());
    assertEquals("4 weeks 1 day", new TimePeriod("4 weeks 1 day").toString());
    assertEquals("4 weeks 0 days", new TimePeriod("4 weeks 0 days").toString());
    assertEquals("4 weeks - 2 days", new TimePeriod("4 weeks - 2 days").toString());
    assertEquals("4 weeks - 2 days", new TimePeriod("4 weeks -2 days").toString());
    assertEquals("4 weeks - 2 days", new TimePeriod(" 4 weeks -2 days").toString());
    assertEquals("4 weeks - 2 days", new TimePeriod("  4 weeks -2 days").toString());
    assertEquals("4 weeks - 2 days", new TimePeriod("4 weeks -  2 days").toString());
    assertEquals("4 weeks - 2 days", new TimePeriod("4 weeks -2 days  ").toString());
    assertEquals("4 weeks - 2 days", new TimePeriod("4 weeks -2   days").toString());
    assertEquals("4 weeks - 2 days", new TimePeriod("4w-2d").toString());
    assertEquals("4 weeks 2 days", new TimePeriod("4w+2d").toString());
    assertEquals("4 weeks 2 days", new TimePeriod("+4w+2d").toString());
  }

}

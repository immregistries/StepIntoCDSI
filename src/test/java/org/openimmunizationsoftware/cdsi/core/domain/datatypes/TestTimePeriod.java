package org.openimmunizationsoftware.cdsi.core.domain.datatypes;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class TestTimePeriod
{

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

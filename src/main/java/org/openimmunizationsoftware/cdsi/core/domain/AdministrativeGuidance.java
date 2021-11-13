package org.openimmunizationsoftware.cdsi.core.domain;

public class AdministrativeGuidance {
  private Indication indication = null;
  private Schedule schedule = null;
  private Forecast forecast = null;

  public Schedule getSchedule() {
    return schedule;
  }

  public void setSchedule(Schedule schedule) {
    this.schedule = schedule;
  }

  public Indication getIndication() {
    return indication;
  }

  public void setIndication(Indication indication) {
    this.indication = indication;
  }

  public Forecast getForecast() {
    return forecast;
  }

  public void setForecast(Forecast forecast) {
    this.forecast = forecast;
  }
}

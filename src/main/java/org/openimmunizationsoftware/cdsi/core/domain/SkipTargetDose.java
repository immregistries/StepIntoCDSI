package org.openimmunizationsoftware.cdsi.core.domain;

import org.openimmunizationsoftware.cdsi.core.domain.datatypes.TimePeriod;

public class SkipTargetDose
{
  private SeriesDose seriesDose = null;
  private TimePeriod triggerAge = null;
  private TimePeriod triggerInterval = null;
  private TargetDose triggerTargetDose = null;
  private SeriesDose triggerSeriesDose = null;

  public SeriesDose getTriggerSeriesDose() {
    return triggerSeriesDose;
  }

  public void setTriggerSeriesDose(SeriesDose triggerSeriesDose) {
    this.triggerSeriesDose = triggerSeriesDose;
  }

  public TimePeriod getTriggerAge() {
    return triggerAge;
  }

  public void setTriggerAge(TimePeriod triggerAge) {
    this.triggerAge = triggerAge;
  }

  public TimePeriod getTriggerInterval() {
    return triggerInterval;
  }

  public void setTriggerInterval(TimePeriod triggerInterval) {
    this.triggerInterval = triggerInterval;
  }

  public TargetDose getTriggerTargetDose() {
    return triggerTargetDose;
  }

  public void setTriggerTargetDose(TargetDose triggerTargetDose) {
    this.triggerTargetDose = triggerTargetDose;
  }

  public SeriesDose getSeriesDose() {
    return seriesDose;
  }

  public void setSeriesDose(SeriesDose seriesDose) {
    this.seriesDose = seriesDose;
  }

}

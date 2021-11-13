package org.immregistries.step.domain;

import org.immregistries.step.core.domain.datatypes.TimePeriod;

public class LiveVirusConflict {
  private Schedule schedule = null;
  private VaccineType previousVaccineType = null;
  private VaccineType currentVaccineType = null;
  private TimePeriod conflictBeginInterval = null;
  private TimePeriod minimalConflictEndInterval = null;
  private TimePeriod conflictEndInterval = null;

  public Schedule getSchedule() {
    return schedule;
  }

  public void setSchedule(Schedule schedule) {
    this.schedule = schedule;
  }

  public VaccineType getPreviousVaccineType() {
    return previousVaccineType;
  }

  public void setPreviousVaccineType(VaccineType previousVaccineType) {
    this.previousVaccineType = previousVaccineType;
  }

  public VaccineType getCurrentVaccineType() {
    return currentVaccineType;
  }

  public void setCurrentVaccineType(VaccineType currentVaccineType) {
    this.currentVaccineType = currentVaccineType;
  }

  public TimePeriod getConflictBeginInterval() {
    return conflictBeginInterval;
  }

  public void setConflictBeginInterval(TimePeriod conflictBeginInterval) {
    this.conflictBeginInterval = conflictBeginInterval;
  }

  public TimePeriod getMinimalConflictEndInterval() {
    return minimalConflictEndInterval;
  }

  public void setMinimalConflictEndInterval(TimePeriod minimalConflictEndInterval) {
    this.minimalConflictEndInterval = minimalConflictEndInterval;
  }

  public TimePeriod getConflictEndInterval() {
    return conflictEndInterval;
  }

  public void setConflictEndInterval(TimePeriod conflictEndInterval) {
    this.conflictEndInterval = conflictEndInterval;
  }
}

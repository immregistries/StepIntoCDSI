package org.openimmunizationsoftware.cdsi.core.domain;

import java.util.ArrayList;
import java.util.List;

public class Schedule
{
  private String scheduleName = "";
  private List<Contraindication> contraindicationList = new ArrayList<Contraindication>();
  private List<LiveVirusConflict> liveVirusConflictList = new ArrayList<LiveVirusConflict>();
  private List<AntigenSeries> antigenSeriesList = new ArrayList<AntigenSeries>();
  private Immunity immunity = null;

  public Immunity getImmunity() {
    return immunity;
  }

  public void setImmunity(Immunity immunity) {
    this.immunity = immunity;
  }

  public String getScheduleName() {
    return scheduleName;
  }

  public void setScheduleName(String scheduleName) {
    this.scheduleName = scheduleName;
  }

  public List<Contraindication> getContraindicationList() {
    return contraindicationList;
  }

  public void setContraindicationList(List<Contraindication> contraindicationList) {
    this.contraindicationList = contraindicationList;
  }

  public List<LiveVirusConflict> getLiveVirusConflictList() {
    return liveVirusConflictList;
  }

  public void setLiveVirusConflictList(List<LiveVirusConflict> liveVirusConflictList) {
    this.liveVirusConflictList = liveVirusConflictList;
  }

  public List<AntigenSeries> getAntigenSeriesList() {
    return antigenSeriesList;
  }

  public void setAntigenSeriesList(List<AntigenSeries> antigenSeriesList) {
    this.antigenSeriesList = antigenSeriesList;
  }

}

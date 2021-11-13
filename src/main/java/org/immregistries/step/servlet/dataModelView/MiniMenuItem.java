package org.immregistries.step.servlet.dataModelView;

public enum MiniMenuItem {

                          DATA_MODEL("dataModelView", "Data Model"),
                          ANTIGEN("dataModelViewAntigen", "Antigen"),
                          CVX("dataModelViewCvx", "CVX"),
                          LIVE_VIRUS_CONFLICT("dataModelViewLiveVirusConflict",
                              "Live Virus Conflict"),
                          PATIENT("dataModelViewPatient", "Patient"),
                          SCHEDULE("dataModelViewSchedule", "Schedule"),
                          VACCINE_GROUP("dataModelViewVaccineGroup", "Vaccine Group"),
  ;
  private String url = "";
  private String name = "";

  private MiniMenuItem(String url, String name) {
    this.setUrl(url);
    this.setName(name);
  }

  public String getName() {
    return name;
  }

  private void setName(String name) {
    this.name = name;
  }

  public String getUrl() {
    return url;
  }

  private void setUrl(String url) {
    this.url = url;
  }

}

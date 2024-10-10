package org.openimmunizationsoftware.cdsi.core.domain;

public class Observation {

    private String observationCode = "";
    private String observationTitle = "";
    private String group = "";
    private String indicationText = "";
    private String contraindicationText = "";
    private String clarifyingText = "";
  
    public String getObservationCode() {
        return observationCode;
    }
  
    public String getObservationTitle() {
        return observationTitle;
    }
  
    public void setObservationCode(String observationCode) {
        this.observationCode = observationCode;
    }
  
    public void setObservationTitle(String observationTitle) {
        this.observationTitle = observationTitle;
    }

    public String getGroup() {
        return group;
    }
  
    public void setGroup(String group) {
        this.group = group;
    }

    public String getIndicationText() {
        return indicationText;
    }
  
    public void setIndicationText(String indicationText) {
        this.indicationText = indicationText;
    }

    public String getContraindicationText() {
        return indicationText;
    }
  
    public void setContraindicationText(String contraindicationText) {
        this.contraindicationText = contraindicationText;
    }

    public String getClarifyingText() {
        return clarifyingText;
    }
  
    public void setClarifyingText(String clarifyingText) {
        this.clarifyingText = clarifyingText;
    }
}

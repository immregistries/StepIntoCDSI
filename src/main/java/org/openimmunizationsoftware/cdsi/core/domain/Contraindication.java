package org.openimmunizationsoftware.cdsi.core.domain;

import org.openimmunizationsoftware.cdsi.core.domain.datatypes.TimePeriod;

public class Contraindication {
  
    private String observationCode = null;
    private String observationTitle = null;

    private String contraindicationTextDescription = "";
    private TimePeriod contraindicationBeginAge = null;
    private TimePeriod contraindicationEndAge = null;
    private ClinicalGuidelineObservation clinicalGuidelineObservation = null;

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

    public TimePeriod getContraindicationBeginAge() {
        return contraindicationBeginAge;
    }

    public TimePeriod getContraindicationEndAge() {
        return contraindicationEndAge;
    }

    public String getContraindicationTextDescription() {
        return contraindicationTextDescription;
    }

    public void setContraindicationBeginAge(TimePeriod contraindicationBeginAge) {
        this.contraindicationBeginAge = contraindicationBeginAge;
    }

    public void setContraindicationEndAge(TimePeriod contraindicationEndAge) {
        this.contraindicationEndAge = contraindicationEndAge;
    }

    public void setContraindicationTextDescription(String contraindicationTextDescription) {
        this.contraindicationTextDescription = contraindicationTextDescription;
    }
}

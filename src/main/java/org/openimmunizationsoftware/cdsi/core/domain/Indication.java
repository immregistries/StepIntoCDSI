package org.openimmunizationsoftware.cdsi.core.domain;

import org.openimmunizationsoftware.cdsi.core.domain.datatypes.TimePeriod;

public class Indication {
    private ObservationCode observationCode = null;
    private String description = "";
    private TimePeriod beginAge = null;
    private TimePeriod endAge = null;
    private String guidance = "";

    public ObservationCode getObservationCode() {
        return observationCode;
    }

    public void setObservationCode(ObservationCode observationCode) {
        this.observationCode = observationCode;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public TimePeriod getBeginAge() {
        return beginAge;
    }

    public void setBeginAge(TimePeriod beginAge) {
        this.beginAge = beginAge;
    }

    public TimePeriod getEndAge() {
        return endAge;
    }

    public void setEndAge(TimePeriod endAge) {
        this.endAge = endAge;
    }

    public String getGuidance() {
        return guidance;
    }

    public void setGuidance(String guidance) {
        this.guidance = guidance;
    }
}

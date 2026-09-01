package org.openimmunizationsoftware.cdsi.core.domain;

import java.util.Date;

public class PatientObservation {
    private ObservationCode observationCode = null;
    private Date observationDate = null;

    public ObservationCode getObservationCode() {
        return observationCode;
    }

    public void setObservationCode(ObservationCode observationCode) {
        this.observationCode = observationCode;
    }

    public Date getObservationDate() {
        return observationDate;
    }

    public void setObservationDate(Date observationDate) {
        this.observationDate = observationDate;
    }
}

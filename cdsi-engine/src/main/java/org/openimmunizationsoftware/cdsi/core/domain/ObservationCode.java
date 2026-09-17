package org.openimmunizationsoftware.cdsi.core.domain;

public class ObservationCode {
    private String text = "";
    private String code = "";

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    /**
     * CALCDTINT-9 (Interval.getPatientReferenceDoseDate) matches an interval's
     * declared relevant observation against the patient's own recorded
     * observations - two separately-constructed ObservationCode instances
     * (one from Supporting Data, one from the patient's medical history), so
     * this needs content equality, not object identity. The code is the
     * identity; text is a human-readable label, not part of it (the same
     * convention as VaccineType's own CVX-only equality elsewhere).
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof ObservationCode)) {
            return false;
        }
        return code.equals(((ObservationCode) obj).code);
    }

    @Override
    public int hashCode() {
        return code.hashCode();
    }
}

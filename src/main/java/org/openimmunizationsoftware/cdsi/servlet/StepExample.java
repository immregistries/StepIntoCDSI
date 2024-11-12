package org.openimmunizationsoftware.cdsi.servlet;

import java.util.Date;

public class StepExample {
    private String label = "";
    private Date receivedDate = null;
    private String requestString = "";

    public String getLabel() {
        return label;
    }

    public Date getReceivedDate() {
        return receivedDate;
    }

    public String getRequestString() {
        return requestString;
    }

    public StepExample(String label, String requestString) {
        this.label = label;
        this.requestString = requestString;
    }

    public StepExample(String requestString) {
        this.requestString = requestString;
        this.receivedDate = new Date();
    }
}

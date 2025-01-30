package org.openimmunizationsoftware.cdsi.servlet.maps;

import java.util.Date;

public class ClearEntry {

    private String iisName = null;
    private Date entryDate = new Date();
    private int countUpdate = 0;
    private int countQuery = 0;

    public int getCountUpdate() {
        return countUpdate;
    }

    public void setCountUpdate(int countUpdate) {
        this.countUpdate = countUpdate;
    }

    public int getCountQuery() {
        return countQuery;
    }

    public void setCountQuery(int countQuery) {
        this.countQuery = countQuery;
    }
}

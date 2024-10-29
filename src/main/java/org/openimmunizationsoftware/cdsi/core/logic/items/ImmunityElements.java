package org.openimmunizationsoftware.cdsi.core.logic.items;
import java.util.Date;

// Created by Nicole on 10/29/24.

public class ImmunityElements {
    private String immunityGuideline;
    private Date immunityDate;
    private String exclusionCondition;
    public ImmunityElements(String immunityGuideline, Date immunityDate, String exclusionCondition){
        this.immunityGuideline = immunityGuideline;
        this.immunityDate = immunityDate;
        this.exclusionCondition = exclusionCondition;
    }

    // Getters
    public String getImmunityGuideline(){
        return immunityGuideline;
    }
    public Date getImmunityDate(){
        return immunityDate;
    }
    public String getexclusionCondition(){
        return exclusionCondition;
    }

    // Setters
    public String setImmunityGuideline(){
        return immunityGuideline;
    }
    public Date setImmunityDate(){
        return immunityDate;
    }
    public String setexclusionCondition(){
        return exclusionCondition;
    }
}

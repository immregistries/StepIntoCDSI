package org.openimmunizationsoftware.cdsi.core.logic.items;

public class ConditionalSkipElements {
    private String doseType;
    private String doseCountLogic;
    private int doseCount;
    public ConditionalSkipElements(String doseType, String doseCountLogic, int doseCount){
        this.doseType = doseType;
        this.doseCountLogic = doseCountLogic;
        this.doseCount = doseCount;
    }
    public String getDoseType(){
        return doseType;
    }
    public String getDoseCountLogic(){
        return doseCountLogic;
    }
    public int getDoseCount(){
        return doseCount;
    }
}

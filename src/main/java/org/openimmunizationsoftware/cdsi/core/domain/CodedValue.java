package org.openimmunizationsoftware.cdsi.core.domain;

public class CodedValue {
    private String code = "";
    private String codeSystem = "";
    private String text = "";
  
    public String getCode() {
        return code;
    }
  
    public void setCode(String code) {
        this.code = code;
    }

    public String getCodeSystem() {
        return codeSystem;
    }
  
    public void setCodeSystem(String codeSystem) {
        this.codeSystem = codeSystem;
    }

    public String getText() {
        return text;
    }
  
    public void setText(String text) {
        this.text = text;
    }
}

package org.openimmunizationsoftware.cdsi.servlet.ice;

public class IceWireConformanceResult {
    public enum Verdict { PASS, FAIL, AMBER }

    public String id;
    public String description;
    public String specRef;
    public String expected;
    public String requestPretty; // truncated
    public int httpStatus; // 0 if not executed
    public String responseSnippet; // truncated for table
    public String responsePretty; // full for detail
    public Verdict verdict;
    public String actualSummary;
    public String notes;

    public IceWireConformanceResult(String id, String description, String specRef, String expected) {
        this.id = id;
        this.description = description;
        this.specRef = specRef;
        this.expected = expected;
        this.verdict = Verdict.AMBER;
        this.notes = "";
    }

    public String verdictCss() {
        switch (verdict) {
            case PASS: return "background-color:#d4edda; color:#155724;";
            case FAIL: return "background-color:#f8d7da; color:#721c24;";
            case AMBER: default: return "background-color:#fff3cd; color:#856404;";
        }
    }
    public String verdictLabel() { return verdict.name(); }
}

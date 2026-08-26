package org.openimmunizationsoftware.cdsi.servlet.ice;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Declarative wire test case. The runner interprets fields to build the HTTP request.
 */
public class IceWireTestCase {
    public String id;
    public String description;
    public String specRef;
    public String expected;

    // common patient/assessment
    public LocalDate assessmentDate = LocalDate.of(2026, 4, 4);
    public LocalDate patientDob = LocalDate.of(2010, 12, 1);
    public String patientSex = "F";

    // immunizations / observations
    public static class Imm { public LocalDate date; public String cvx; public Imm(LocalDate d, String c){date=d;cvx=c;} }
    public static class Obs { public LocalDate date; public String code; public String display; public Obs(LocalDate d, String c, String disp){date=d;code=c;display=disp;} }
    public List<Imm> immunizations = new ArrayList<>();
    public List<Obs> observations = new ArrayList<>();

    // knowledgeBase handling
    public String knowledgeBaseCanonical = "https://terminology.cdsframework.org/PlanDefinition/ice-forecast|1.0.0";
    public boolean legacyKbCodeableConcept = false; // W-11: send CodeableConcept+version instead of canonical
    public String legacyKbId = "USA-CDC-CDSI";
    public String legacyKbVersion = "4.64";
    public boolean omitKnowledgeBase = false;
    public boolean malformedCanonical = false;

    // toggles
    public boolean omitPatient = false;
    public boolean omitAssessmentDate = false;
    public boolean useDataBundle = false; // W-05: bundle instead of discrete
    public String contentType = "application/fhir+json"; // vs application/json
    public String rawMalformedJson = null; // W-14
    public String httpMethod = "POST"; // or GET
    public boolean isMetadataProbe = false;
    public boolean isSupportingDataProbe = false;
    public String supportingDataPath = null; // e.g. "/cds/supporting-data/vaccines"

    public IceWireTestCase(String id, String description, String specRef, String expected) {
        this.id = id; this.description = description; this.specRef = specRef; this.expected = expected;
    }

    public IceWireTestCase withImm(LocalDate d, String cvx){ immunizations.add(new Imm(d,cvx)); return this; }
    public IceWireTestCase withObs(LocalDate d, String code, String disp){ observations.add(new Obs(d,code,disp)); return this; }
}

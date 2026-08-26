package org.openimmunizationsoftware.cdsi.servlet.ice;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Catalogue of ~16 wire tests + supporting-data probes.
 * Base fixture: Swagger varicella example (patient 2010-12-01 F, assessment 2026-04-04, imm 111 2016-02-02, obs 024 2016-01-01)
 */
public class IceWireTestCases {

    public static List<IceWireTestCase> all() {
        List<IceWireTestCase> list = new ArrayList<>();

        LocalDate immDate = LocalDate.of(2016,2,2);
        LocalDate obsDate = LocalDate.of(2016,1,1);

        // W-01 happy JSON discrete + obs (fhir+json)
        list.add(new IceWireTestCase("W-01","Happy: discrete imm+obs, fhir+json","OD $immds-forecast in: patient 1..1, assessmentDate 1..1 valueDate, knowledgeBase 0..1 valueCanonical, immunization 0..*, observation 0..* / OpenAPI ImmDsForecastRequest",
                "200 with durationMs, engineVersion, guidanceResponse, evaluation, recommendation, operationOutcome(informational)")
                .withImm(immDate,"111").withObs(obsDate,"024","Healthcare provider verified history of or diagnosis of Varicella"));

        // W-02 same but application/json
        IceWireTestCase w02 = new IceWireTestCase("W-02","Happy: same as W-01 but Content-Type application/json","OD: requestBody content application/json + application/fhir+json",
                "200, same envelope, both content types accepted");
        w02.contentType = "application/json";
        w02.withImm(immDate,"111").withObs(obsDate,"024","Healthcare provider verified history of or diagnosis of Varicella");
        list.add(w02);

        // W-03 Accept XML (client sends fhir+json but Accept xml? Actually contentType drives Accept in runner)
        IceWireTestCase w03 = new IceWireTestCase("W-03","Happy: Accept application/fhir+xml (XML encoding)","CapabilityStatement.format includes json + fhir+json; server may also support xml","200 or 406; ICE CapabilityStatement advertises only json/fhir+json, so xml may fail – AMBER");
        w03.contentType = "application/fhir+json"; // we will set Accept header differently via runner? For now same, but note
        w03.withImm(immDate,"111");
        list.add(w03);

        // W-04 happy no obs
        IceWireTestCase w04 = new IceWireTestCase("W-04","Happy: no observation","OD observation 0..*","200");
        w04.withImm(immDate,"111");
        list.add(w04);

        // W-05 via data Bundle
        IceWireTestCase w05 = new IceWireTestCase("W-05","Happy: Bundle data alternative (single Bundle containing Patient/Immunization/Observation)","OD: data 0..1 Bundle vs discrete patient/assessmentDate/immunization/observation","200 with same recommendations (bundle parsed)");
        w05.useDataBundle = true;
        w05.withImm(immDate,"111").withObs(obsDate,"024","Healthcare provider verified history of or diagnosis of Varicella");
        list.add(w05);

        // W-06 missing patient
        IceWireTestCase w06 = new IceWireTestCase("W-06","Missing patient (1..1 required)","OD min=1 patient → business error, not HTTP 400 per OD: 200 with operationOutcome(error)","200 with operationOutcome severity error");
        w06.omitPatient = true;
        w06.withImm(immDate,"111");
        list.add(w06);

        // W-07 missing assessmentDate
        IceWireTestCase w07 = new IceWireTestCase("W-07","Missing assessmentDate","OD min=1 assessmentDate","200 with operationOutcome(error)");
        w07.omitAssessmentDate = true;
        w07.withImm(immDate,"111");
        list.add(w07);

        // W-08 missing knowledgeBase
        IceWireTestCase w08 = new IceWireTestCase("W-08","Missing knowledgeBase (0..1 but ICE requires it)","OpenAPI HandledError: Parameters must contain knowledgeBase valueCanonical → 200 + operationOutcome(error)","200 with operationOutcome error about knowledgeBase");
        w08.omitKnowledgeBase = true;
        w08.withImm(immDate,"111");
        list.add(w08);

        // W-09 malformed canonical
        IceWireTestCase w09 = new IceWireTestCase("W-09","Malformed knowledgeBase canonical (not-a-canonical)","OD type canonical → parse error or business error","200 with operationOutcome(error) or 400");
        w09.knowledgeBaseCanonical = "not-a-canonical";
        w09.withImm(immDate,"111");
        list.add(w09);

        // W-10 unknown KB version
        IceWireTestCase w10 = new IceWireTestCase("W-10","Unknown KB version |9.9.9","Supporting-data resolution → NOTSUPPORTED","200 with operationOutcome(error) / NOTSUPPORTED");
        w10.knowledgeBaseCanonical = "https://terminology.cdsframework.org/PlanDefinition/ice-forecast|9.9.9";
        w10.withImm(immDate,"111");
        list.add(w10);

        // W-11 legacy StepInto style
        IceWireTestCase w11 = new IceWireTestCase("W-11","Legacy CodeableConcept knowledgeBase + knowledgeBaseVersion (StepInto style)","Pre-R6 draft used CodeableConcept system https://ivci.org/knowledge-base; OD now canonical → should be ignored or error","200+error or ignored (divergence surface) – AMBER");
        w11.legacyKbCodeableConcept = true;
        w11.knowledgeBaseCanonical = null;
        w11.withImm(immDate,"111");
        list.add(w11);

        // W-12 invalid CVX
        IceWireTestCase w12 = new IceWireTestCase("W-12","Invalid CVX 9999","CodeSystem http://hl7.org/fhir/sid/cvx; invalid code handling","200 with operationOutcome(informational) but evaluation may be empty");
        w12.withImm(immDate,"9999");
        list.add(w12);

        // W-13 unknown condition
        IceWireTestCase w13 = new IceWireTestCase("W-13","Unknown condition 999","ivci contextual-condition 024 is only documented; unknown code handling","200 with operationOutcome");
        w13.withImm(immDate,"111").withObs(obsDate,"999","Unknown condition");
        list.add(w13);

        // W-14 malformed JSON
        IceWireTestCase w14 = new IceWireTestCase("W-14","Malformed JSON body","OD requestBody required → bean validation before controller → 400","400 with Parameters or OperationOutcome");
        w14.rawMalformedJson = "{, }";
        w14.contentType = "application/fhir+json";
        list.add(w14);

        // W-15 GET not POST
        IceWireTestCase w15 = new IceWireTestCase("W-15","GET instead of POST","OD POST only; HEAD returns 405 Allow: POST","405 Method Not Allowed, Allow: POST");
        w15.httpMethod = "GET";
        list.add(w15);

        // W-16 CapabilityStatement
        IceWireTestCase w16 = new IceWireTestCase("W-16","GET /cds/metadata CapabilityStatement","CapabilityStatement fhirVersion 6.0.0, implementation.url …/cds, rest.operation immds-forecast definition https://terminology.cdsframework.org/ice/OperationDefinition/immds-forecast","200 with CapabilityStatement containing those fields");
        w16.isMetadataProbe = true;
        w16.httpMethod = "GET";
        list.add(w16);

        return list;
    }

    public static List<IceWireTestCase> supportingDataProbes() {
        List<IceWireTestCase> list = new ArrayList<>();
        String baseSuffix = "?moduleCanonical=https://terminology.cdsframework.org/PlanDefinition/ice-forecast&moduleVersion=1.0.0";
        for (String path : new String[]{
                "/supporting-data/vaccines" + baseSuffix,
                "/supporting-data/vaccine-groups" + baseSuffix,
                "/supporting-data/series" + baseSuffix,
                "/supporting-data/seasons" + baseSuffix,
                "/supporting-data/diseases" + baseSuffix,
                "/supporting-data/module-plan-definitions",
                "/supporting-data/code-system" + baseSuffix + "&name=SUPPORTED_VACCINES"
        }) {
            IceWireTestCase tc = new IceWireTestCase("SD-" + path.replaceAll(".*/","").replaceAll("\\?.*",""),
                    "Supporting data " + path, "OpenAPI Supporting Data tag", "200 with CodeSystem/PlanDefinition");
            tc.isSupportingDataProbe = true;
            tc.supportingDataPath = path;
            tc.httpMethod = "GET";
            list.add(tc);
        }
        // explicit 404 probe for contextual-condition code-system name
        IceWireTestCase bad = new IceWireTestCase("SD-CTX-404","code-system? name=CONTEXTUAL_CONDITION (expected gap)", "Not in OpenAPI? Currently 404","404 vs CodeSystem – documents IVCI dropdown gap");
        bad.isSupportingDataProbe = true;
        bad.supportingDataPath = "/supporting-data/code-system" + baseSuffix + "&name=CONTEXTUAL_CONDITIONS";
        bad.httpMethod = "GET";
        list.add(bad);
        return list;
    }
}

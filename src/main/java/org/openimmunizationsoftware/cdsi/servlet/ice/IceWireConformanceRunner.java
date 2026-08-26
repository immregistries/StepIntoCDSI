package org.openimmunizationsoftware.cdsi.servlet.ice;

import java.io.IOException;
import java.sql.Date;
import java.util.ArrayList;
import java.util.List;

import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.CanonicalType;
import org.hl7.fhir.r4.model.CapabilityStatement;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.DateTimeType;
import org.hl7.fhir.r4.model.DateType;
import org.hl7.fhir.r4.model.Enumerations.AdministrativeGender;
import org.hl7.fhir.r4.model.Immunization;
import org.hl7.fhir.r4.model.Observation;
import org.hl7.fhir.r4.model.OperationOutcome;
import org.hl7.fhir.r4.model.Parameters;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.StringType;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.client.api.ServerValidationModeEnum;
import ca.uhn.fhir.rest.server.exceptions.BaseServerResponseException;

public class IceWireConformanceRunner {

    private final String base;
    private final FhirContext ctx;

    public IceWireConformanceRunner(String base) {
        this.base = base.endsWith("/") ? base.substring(0, base.length()-1) : base;
        this.ctx = FhirContext.forR4();
        this.ctx.getRestfulClientFactory().setServerValidationMode(ServerValidationModeEnum.NEVER);
        this.ctx.getRestfulClientFactory().setSocketTimeout(30000);
        try { this.ctx.getRestfulClientFactory().setConnectTimeout(10000);} catch(Exception ignored){}
    }

    public List<IceWireConformanceResult> runAll() {
        List<IceWireConformanceResult> out = new ArrayList<>();
        for (IceWireTestCase tc : IceWireTestCases.all()) {
            out.add(runOne(tc));
        }
        // supporting data probes as separate section but same list
        for (IceWireTestCase tc : IceWireTestCases.supportingDataProbes()) {
            IceWireConformanceResult r = new IceWireConformanceResult(tc.id, tc.description, tc.specRef, tc.expected);
            out.add(runSupportingData(tc, r));
        }
        return out;
    }

    private IceWireConformanceResult runOne(IceWireTestCase tc) {
        IceWireConformanceResult r = new IceWireConformanceResult(tc.id, tc.description, tc.specRef, tc.expected);
        try {
            if (tc.isMetadataProbe) {
                return runMetadata(tc, r);
            }
            if ("GET".equals(tc.httpMethod) && !tc.isSupportingDataProbe && !tc.isMetadataProbe) {
                // W-15: GET $immds-forecast should be 405
                return runGetImmds(tc, r);
            }
            if (tc.rawMalformedJson != null) {
                return runMalformed(tc, r);
            }

            // Build Parameters
            Parameters req = buildParameters(tc);
            String reqPretty = pretty(req, tc.contentType);
            r.requestPretty = truncate(reqPretty, 3000);

            // Execute via HAPI or raw depending
            if (tc.useDataBundle) {
                // Already built as Bundle alternative: still via HAPI operation but params contains data Bundle
                // no extra handling
            }

            // Use HAPI client
            IGenericClient client = ctx.newRestfulGenericClient(base);
            Parameters resp = null;
            int status = 200;
            String respBody = "";
            try {
                resp = client.operation().onServer().named("$immds-forecast")
                        .withParameters(req)
                        .returnResourceType(Parameters.class)
                        .execute();
                respBody = pretty(resp, tc.contentType);
                r.httpStatus = 200;
                r.responsePretty = respBody;
                r.responseSnippet = truncate(stripDivs(respBody), 2000);
            } catch (BaseServerResponseException e) {
                status = e.getStatusCode();
                r.httpStatus = status;
                String body = e.getResponseBody();
                if (body != null) {
                    r.responsePretty = body;
                    r.responseSnippet = truncate(stripDivs(body), 2000);
                    // try parse as Parameters for assertions
                    try {
                        if (body.trim().startsWith("{")) {
                            org.hl7.fhir.instance.model.api.IBaseResource parsed = ctx.newJsonParser().parseResource(body);
                            if (parsed instanceof Parameters) resp = (Parameters) parsed;
                        }
                    } catch (Exception ignored) {}
                }
                if (e.getOperationOutcome() instanceof OperationOutcome) {
                    try { r.responsePretty = pretty((OperationOutcome) e.getOperationOutcome(), tc.contentType); } catch(Exception ignored){}
                }
                // For error cases, keep responseSnippet as body
                if (r.responseSnippet == null || r.responseSnippet.isEmpty()) {
                    r.responseSnippet = truncate(e.getMessage(), 1000);
                }
                // fall through to assertions with resp maybe null
            }

            // Assertions
            evaluateWire(tc, r, resp, r.httpStatus, r.responsePretty);

        } catch (Exception e) {
            r.httpStatus = 0;
            StringBuilder sb = new StringBuilder(e.getClass().getSimpleName()+": "+e.getMessage());
            if (e.getCause()!=null) sb.append(" cause: ").append(e.getCause().toString());
            r.actualSummary = sb.toString();
            r.verdict = IceWireConformanceResult.Verdict.FAIL;
            r.notes = stackSnippet(e);
        }
        return r;
    }

    private Parameters buildParameters(IceWireTestCase tc) {
        Parameters p = new Parameters();
        if (!tc.omitAssessmentDate && tc.assessmentDate != null) {
            p.addParameter("assessmentDate", new DateType(Date.valueOf(tc.assessmentDate)));
        }
        // knowledgeBase – always discrete (even with Bundle), per spec Bundle contains patient/imm/obs but KB stays outside
        if (tc.legacyKbCodeableConcept) {
            CodeableConcept kb = new CodeableConcept();
            kb.addCoding().setSystem("https://ivci.org/knowledge-base").setCode(tc.legacyKbId).setDisplay(tc.legacyKbId);
            p.addParameter().setName("knowledgeBase").setValue(kb);
            if (tc.legacyKbVersion != null) p.addParameter("knowledgeBaseVersion", new StringType(tc.legacyKbVersion));
        } else if (!tc.omitKnowledgeBase) {
            String kbVal = tc.knowledgeBaseCanonical;
            if (tc.malformedCanonical) kbVal = "not-a-canonical";
            if (kbVal != null) p.addParameter("knowledgeBase", new CanonicalType(kbVal));
        }
        if (tc.useDataBundle) {
            Bundle b = new Bundle();
            b.setType(Bundle.BundleType.COLLECTION);
            if (!tc.omitPatient) {
                Patient pat = new Patient();
                pat.setBirthDate(Date.valueOf(tc.patientDob));
                pat.setGender(AdministrativeGender.fromCode(mapGender(tc.patientSex)));
                pat.setId("patient-1");
                b.addEntry().setResource(pat);
            }
            for (IceWireTestCase.Imm imm : tc.immunizations) {
                Immunization im = new Immunization();
                im.setOccurrence(new DateTimeType(Date.valueOf(imm.date)));
                CodeableConcept cc = new CodeableConcept();
                cc.addCoding(new Coding("http://hl7.org/fhir/sid/cvx", imm.cvx, null));
                im.setVaccineCode(cc);
                b.addEntry().setResource(im);
            }
            for (IceWireTestCase.Obs obs : tc.observations) {
                Observation o = new Observation();
                o.setStatus(Observation.ObservationStatus.FINAL);
                CodeableConcept code = new CodeableConcept();
                code.addCoding(new Coding("http://ivci.org/CodeSystem/contextual-condition", obs.code, obs.display));
                code.setText(obs.display);
                o.setCode(code);
                o.setValue(new DateTimeType(Date.valueOf(obs.date)));
                b.addEntry().setResource(o);
            }
            p.addParameter().setName("data").setResource(b);
        } else {
            if (!tc.omitPatient && tc.patientDob != null) {
                Patient pat = new Patient();
                pat.setBirthDate(Date.valueOf(tc.patientDob));
                if (tc.patientSex != null) {
                    String gc = mapGender(tc.patientSex);
                    if (gc != null) pat.setGender(AdministrativeGender.fromCode(gc));
                }
                p.addParameter().setName("patient").setResource(pat);
            }
            for (IceWireTestCase.Imm imm : tc.immunizations) {
                Immunization im = new Immunization();
                im.setOccurrence(new DateTimeType(Date.valueOf(imm.date)));
                CodeableConcept cc = new CodeableConcept();
                cc.addCoding(new Coding("http://hl7.org/fhir/sid/cvx", imm.cvx, null));
                im.setVaccineCode(cc);
                p.addParameter().setName("immunization").setResource(im);
            }
            for (IceWireTestCase.Obs obs : tc.observations) {
                Observation o = new Observation();
                o.setStatus(Observation.ObservationStatus.FINAL);
                CodeableConcept code = new CodeableConcept();
                code.addCoding(new Coding("http://ivci.org/CodeSystem/contextual-condition", obs.code, obs.display));
                code.setText(obs.display);
                o.setCode(code);
                o.setValue(new DateTimeType(Date.valueOf(obs.date)));
                p.addParameter().setName("observation").setResource(o);
            }
        }
        return p;
    }

    private void evaluateWire(IceWireTestCase tc, IceWireConformanceResult r, Parameters resp, int status, String body) {
        String bid = tc.id;
        // Default PASS, then downgrade
        // Business-error cases expect 200 + operationOutcome error
        boolean expectError = bid.equals("W-06") || bid.equals("W-07") || bid.equals("W-08") || bid.equals("W-09") || bid.equals("W-10");
        boolean expectLegacy = bid.equals("W-11");
        boolean expect400 = bid.equals("W-14");
        boolean expect405 = bid.equals("W-15");
        boolean expectHappy = bid.equals("W-01") || bid.equals("W-02") || bid.equals("W-04") || bid.equals("W-05") || bid.equals("W-12") || bid.equals("W-13");
        // W-03 is AMBER by design
        if (bid.equals("W-03")) {
            r.verdict = IceWireConformanceResult.Verdict.AMBER;
            r.actualSummary = "HTTP " + status + (body!=null && body.contains("CapabilityStatement") ? " CapabilityStatement-like" : " response "+truncate(body,200));
            r.notes = "ICE advertises only json/fhir+json; XML not required. Verdict AMBER.";
            return;
        }
        if (expect400) {
            if (status == 400) { r.verdict = IceWireConformanceResult.Verdict.PASS; r.actualSummary = "400 as expected"; }
            else { r.verdict = IceWireConformanceResult.Verdict.FAIL; r.actualSummary = "Expected 400, got "+status; }
            r.notes = "Malformed JSON should be 400, not 200. Body: "+truncate(body,400);
            return;
        }
        if (expect405) {
            if (status == 405) { r.verdict = IceWireConformanceResult.Verdict.PASS; r.actualSummary = "405 Method Not Allowed (POST only)"; }
            else { r.verdict = IceWireConformanceResult.Verdict.FAIL; r.actualSummary = "Expected 405, got "+status; }
            return;
        }
        if (expectError) {
            boolean hasErrorOutcome = body != null && (body.contains("\"severity\"") && body.toLowerCase().contains("error") || body.contains("operationOutcome"));
            // also check parsed resp
            boolean parsedError = false;
            if (resp != null) {
                for (Parameters.ParametersParameterComponent pp : resp.getParameter()) {
                    if ("operationOutcome".equals(pp.getName()) && pp.getResource() instanceof OperationOutcome) {
                        OperationOutcome oo = (OperationOutcome) pp.getResource();
                        for (OperationOutcome.OperationOutcomeIssueComponent iss : oo.getIssue()) {
                            if (iss.getSeverity() != null && iss.getSeverity().name().contains("ERROR")) parsedError = true;
                            if (iss.getDiagnostics() != null && iss.getDiagnostics().toLowerCase().contains("knowledgebase")) parsedError = true;
                        }
                    }
                }
            }
            if (status == 200 && (hasErrorOutcome || parsedError)) {
                r.verdict = IceWireConformanceResult.Verdict.PASS;
                r.actualSummary = "200 with operationOutcome(error) – per spec business errors are 200";
            } else if (status == 200) {
                r.verdict = IceWireConformanceResult.Verdict.FAIL;
                r.actualSummary = "Expected 200 + error outcome, got 200 without error. Body: "+truncate(body,300);
            } else {
                r.verdict = IceWireConformanceResult.Verdict.AMBER;
                r.actualSummary = "Got HTTP "+status+" (spec says 200+error, not "+status+")";
            }
            r.notes = status==200 ? "Correct envelope; check diagnostics mentions missing field." : "Unexpected HTTP code for business error.";
            return;
        }
        if (expectLegacy) {
            // Should be ignored or error – document divergence
            if (status == 200) {
                r.verdict = IceWireConformanceResult.Verdict.AMBER;
                r.actualSummary = "200 returned for legacy CodeableConcept style – ICE may ignore legacy params (not in R6).";
                r.notes = "Divergence: StepInto pre-R6 uses CodeableConcept+version; R6 uses valueCanonical. ICE appears tolerant. AMBER.";
            } else {
                r.verdict = IceWireConformanceResult.Verdict.AMBER;
                r.actualSummary = "HTTP "+status;
            }
            return;
        }
        if (expectHappy) {
            if (status != 200) {
                r.verdict = IceWireConformanceResult.Verdict.FAIL;
                r.actualSummary = "Expected 200, got "+status;
                return;
            }
            // check envelope contains durationMs, engineVersion, etc.
            boolean hasDuration = body != null && body.contains("durationMs");
            boolean hasEngine = body != null && body.contains("engineVersion");
            boolean hasGuidance = body != null && body.contains("guidanceResponse");
            boolean hasOpOutcomeInfo = body != null && body.contains("operationOutcome") && body.toLowerCase().contains("informational");
            if (hasDuration && hasEngine && hasGuidance && hasOpOutcomeInfo) {
                r.verdict = IceWireConformanceResult.Verdict.PASS;
                r.actualSummary = "200 with durationMs, engineVersion, guidanceResponse, opOutcome(informational)";
            } else {
                r.verdict = IceWireConformanceResult.Verdict.FAIL;
                r.actualSummary = "200 but missing envelope part: durationMs="+hasDuration+" engine="+hasEngine+" guidance="+hasGuidance+" opInfo="+hasOpOutcomeInfo;
            }
            // extra check: text.div present is expected per FHIR, not a failure
            if (body != null && body.contains("\"div\"")) {
                r.notes = "text.div present on resources – per FHIR narrative, not ICE-specific.";
            }
            return;
        }
        // fallback
        r.verdict = IceWireConformanceResult.Verdict.AMBER;
        r.actualSummary = "HTTP "+status;
    }

    private IceWireConformanceResult runMalformed(IceWireTestCase tc, IceWireConformanceResult r) {
        String url = base + "/$immds-forecast";
        r.requestPretty = tc.rawMalformedJson;
        try (CloseableHttpClient client = HttpClients.createDefault()) {
            HttpPost post = new HttpPost(url);
            post.setEntity(new StringEntity(tc.rawMalformedJson, ContentType.parse(tc.contentType)));
            post.setHeader("Accept", "application/fhir+json");
            org.apache.hc.core5.http.io.HttpClientResponseHandler<String> h = resp -> {
                int sc = resp.getCode();
                r.httpStatus = sc;
                String b = resp.getEntity() != null ? EntityUtils.toString(resp.getEntity()) : "";
                r.responsePretty = b;
                r.responseSnippet = truncate(stripDivs(b),2000);
                return b;
            };
            String body = client.execute(post, h);
            if (r.httpStatus==400) { r.verdict= IceWireConformanceResult.Verdict.PASS; r.actualSummary="400 as expected for malformed JSON"; }
            else { r.verdict= IceWireConformanceResult.Verdict.FAIL; r.actualSummary="Expected 400, got "+r.httpStatus; }
        } catch (Exception e) {
            r.verdict = IceWireConformanceResult.Verdict.FAIL;
            r.actualSummary = e.getClass().getSimpleName()+": "+e.getMessage();
            r.notes = stackSnippet(e);
        }
        return r;
    }

    private IceWireConformanceResult runGetImmds(IceWireTestCase tc, IceWireConformanceResult r) {
        String url = base + "/$immds-forecast";
        r.requestPretty = "GET " + url;
        try (CloseableHttpClient client = HttpClients.createDefault()) {
            HttpGet get = new HttpGet(url);
            get.setHeader("Accept", "application/fhir+json");
            org.apache.hc.core5.http.io.HttpClientResponseHandler<String> h = resp -> {
                r.httpStatus = resp.getCode();
                String b = resp.getEntity()!=null? EntityUtils.toString(resp.getEntity()):"";
                r.responsePretty=b;
                r.responseSnippet=truncate(stripDivs(b),2000);
                // capture Allow header
                if (resp.containsHeader("Allow")) r.actualSummary = "Allow: "+resp.getFirstHeader("Allow").getValue();
                return b;
            };
            client.execute(get,h);
            if (r.httpStatus==405) { r.verdict= IceWireConformanceResult.Verdict.PASS; r.actualSummary=(r.actualSummary!=null?r.actualSummary:"405")+" – POST only"; }
            else { r.verdict= IceWireConformanceResult.Verdict.FAIL; r.actualSummary="Expected 405, got "+r.httpStatus+(r.actualSummary!=null?" "+r.actualSummary:""); }
        } catch(Exception e){ r.verdict= IceWireConformanceResult.Verdict.FAIL; r.actualSummary=e.getMessage(); r.notes=stackSnippet(e);}
        return r;
    }

    private IceWireConformanceResult runMetadata(IceWireTestCase tc, IceWireConformanceResult r) {
        String url = base + "/metadata";
        r.requestPretty = "GET " + url;
        try (CloseableHttpClient client = HttpClients.createDefault()) {
            HttpGet get = new HttpGet(url);
            get.setHeader("Accept","application/fhir+json");
            org.apache.hc.core5.http.io.HttpClientResponseHandler<String> h = resp -> {
                r.httpStatus = resp.getCode();
                String b = resp.getEntity()!=null?EntityUtils.toString(resp.getEntity()):"";
                r.responsePretty=b;
                r.responseSnippet=truncate(stripDivs(b),2000);
                return b;
            };
            String body = client.execute(get,h);
            boolean hasFhirVersion = body.contains("fhirVersion") && body.contains("6.0.0");
            boolean hasImpl = body.contains("implementation") && body.contains("/cds");
            boolean hasOp = body.contains("immds-forecast") && body.contains("https://terminology.cdsframework.org/ice/OperationDefinition/immds-forecast");
            if (r.httpStatus==200 && hasFhirVersion && hasImpl && hasOp) {
                r.verdict= IceWireConformanceResult.Verdict.PASS;
                r.actualSummary="200 CapabilityStatement fhirVersion 6.0.0, impl …/cds, op immds-forecast";
            } else {
                r.verdict= IceWireConformanceResult.Verdict.FAIL;
                r.actualSummary="HTTP "+r.httpStatus+" hasFhir6="+hasFhirVersion+" hasImpl="+hasImpl+" hasOp="+hasOp;
            }
            r.notes = r.httpStatus==200? "CapabilityStatement returned; check R6 parse not needed due to NEVER mode." : "";
            return r;
        } catch(Exception e){ r.verdict= IceWireConformanceResult.Verdict.FAIL; r.actualSummary=e.getMessage(); r.notes=stackSnippet(e); return r;}
    }

    private IceWireConformanceResult runSupportingData(IceWireTestCase tc, IceWireConformanceResult r) {
        String url = base + tc.supportingDataPath;
        r.requestPretty = "GET " + url;
        r.specRef = tc.specRef;
        r.expected = tc.expected;
        try (CloseableHttpClient client = HttpClients.createDefault()) {
            HttpGet get = new HttpGet(url);
            get.setHeader("Accept","application/fhir+json");
            org.apache.hc.core5.http.io.HttpClientResponseHandler<String> h = resp -> {
                r.httpStatus = resp.getCode();
                String b = resp.getEntity()!=null?EntityUtils.toString(resp.getEntity()):"";
                r.responsePretty=b;
                r.responseSnippet=truncate(stripDivs(b),2000);
                return b;
            };
            String body = client.execute(get,h);
            if (tc.id.equals("SD-CTX-404")) {
                if (r.httpStatus==404) { r.verdict= IceWireConformanceResult.Verdict.AMBER; r.actualSummary="404 as observed – documents IVCI dropdown gap"; r.notes="OpenAPI lists vaccines/vaccine-groups/series/seasons/diseases/code-system, but CONTEXTUAL_CONDITIONS not discoverable via code-system endpoint."; }
                else if (r.httpStatus==200) { r.verdict= IceWireConformanceResult.Verdict.PASS; r.actualSummary="200 – code-system now supports contextual conditions"; }
                else { r.verdict= IceWireConformanceResult.Verdict.FAIL; r.actualSummary="Unexpected "+r.httpStatus; }
            } else {
                if (r.httpStatus==200) {
                    // Supporting-data returns CodeSystem/PlanDefinition JSON without resourceType wrapper in some cases; accept any 200 with content
                    boolean hasMarker = body.contains("SUPPORTED_") || body.contains("CodeSystem") || body.contains("PlanDefinition") || body.contains("\"url\"") || body.contains("\"name\"");
                    if (hasMarker || body.length()>100) {
                        r.verdict= IceWireConformanceResult.Verdict.PASS;
                        r.actualSummary="200 with "+ (body.contains("CodeSystem")?"CodeSystem": body.contains("PlanDefinition")?"PlanDefinition": hasMarker? "CodeSystem/PlanDefinition":"JSON");
                    } else {
                        r.verdict= IceWireConformanceResult.Verdict.AMBER;
                        r.actualSummary="200 but body unexpected: "+truncate(body,200);
                    }
                } else {
                    r.verdict= IceWireConformanceResult.Verdict.FAIL;
                    r.actualSummary="Expected 200 CodeSystem/PlanDefinition, got "+r.httpStatus;
                }
            }
            return r;
        } catch(Exception e){ r.verdict= IceWireConformanceResult.Verdict.FAIL; r.actualSummary=e.getMessage(); r.notes=stackSnippet(e); return r;}
    }

    private String pretty(Resource r, String contentType) {
        try {
            IParser parser = contentType!=null && contentType.contains("xml") ? ctx.newXmlParser().setPrettyPrint(true) : ctx.newJsonParser().setPrettyPrint(true);
            return parser.encodeResourceToString(r);
        } catch(Exception e){ return r.toString(); }
    }
    private String truncate(String s, int n){ if(s==null) return ""; return s.length()<=n? s : s.substring(0,n)+"...[truncated "+(s.length()-n)+" chars]"; }
    private String stripDivs(String s){ if(s==null) return ""; return s; } // keep div, just for snippet
    private String mapGender(String code){
        switch(code.toUpperCase()){
            case "M": return "male";
            case "F": return "female";
            case "O": return "other";
            case "U": return "unknown";
            default: return null;
        }
    }
    private String stackSnippet(Exception e){
        java.io.StringWriter sw = new java.io.StringWriter();
        e.printStackTrace(new java.io.PrintWriter(sw));
        String st = sw.toString();
        return truncate(st, 800);
    }
}

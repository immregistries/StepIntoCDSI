package org.openimmunizationsoftware.cdsi.servlet;

import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.hl7.fhir.r4.model.CanonicalType;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.DateTimeType;
import org.hl7.fhir.r4.model.DateType;
import org.hl7.fhir.r4.model.Enumerations.AdministrativeGender;
import org.hl7.fhir.r4.model.GuidanceResponse;
import org.hl7.fhir.r4.model.Immunization;
import org.hl7.fhir.r4.model.ImmunizationEvaluation;
import org.hl7.fhir.r4.model.ImmunizationRecommendation;
import org.hl7.fhir.r4.model.ImmunizationRecommendation.ImmunizationRecommendationRecommendationComponent;
import org.hl7.fhir.r4.model.ImmunizationRecommendation.ImmunizationRecommendationRecommendationDateCriterionComponent;
import org.hl7.fhir.r4.model.Observation;
import org.hl7.fhir.r4.model.OperationOutcome;
import org.hl7.fhir.r4.model.Parameters;
import org.hl7.fhir.r4.model.Parameters.ParametersParameterComponent;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.StringType;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.client.api.ServerValidationModeEnum;
import ca.uhn.fhir.rest.client.exceptions.FhirClientConnectionException;
import ca.uhn.fhir.rest.server.exceptions.BaseServerResponseException;

/**
 * ICE ImmDS-Forecast client harness for proposed R6 operation.
 * POST /cds/$immds-forecast with FHIR Parameters (valueCanonical knowledgeBase).
 * Mirrors SandboxServlet UI but adapted to ICE spec: canonical KB, observations.
 */
public class IceSandboxServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    private static final int NUM_IMMUNIZATION_ROWS = 20;
    private static final int NUM_OBSERVATION_ROWS = 5;
    static final String DEFAULT_ENDPOINT_BASE = "https://ice-service.cdsframework.org/opencds-decision-support-service/cds";
    private static final String SESSION_ENDPOINT_KEY = "iceSandboxEndpointBase";
    static final String DEFAULT_KNOWLEDGE_BASE_CANONICAL = "https://terminology.cdsframework.org/PlanDefinition/ice-forecast|1.0.0";

    // Known IVCI contextual-condition codes – only 024 is verified from ICE OpenAPI
    // (https://ice-service.cdsframework.org/.../api-docs ImmDsForecastRequest example).
    // Supporting-data endpoint for contextual conditions currently returns 404,
    // so no authoritative CodeSystem is discoverable. Additional codes can be
    // added once ICE publishes the CodeSystem.
    private static final Map<String, String> CONTEXTUAL_CONDITION_CODES = new LinkedHashMap<>();
    static {
        CONTEXTUAL_CONDITION_CODES.put("024", "Healthcare provider verified history of or diagnosis of Varicella");
    }

    static LocalDate parseFlexibleDateStatic(String s) {
        if (s == null || s.trim().isEmpty()) return null;
        s = s.trim();
        if (s.matches("\\d{8}")) {
            try { return LocalDate.parse(s, DateTimeFormatter.ofPattern("yyyyMMdd")); } catch (Exception e) { return null; }
        }
        if (s.matches("\\d{1,2}/\\d{1,2}/\\d{4}")) {
            try { return LocalDate.parse(s, DateTimeFormatter.ofPattern("M/d/yyyy")); } catch (Exception e) { return null; }
        }
        return null;
    }

    static String formatDateForDisplay(LocalDate d) {
        if (d == null) return "";
        return d.format(DateTimeFormatter.ofPattern("MM/dd/yyyy"));
    }

    static String normalizeDateDisplay(String dateStr) {
        if (dateStr == null || dateStr.isEmpty()) return "";
        LocalDate parsed = parseFlexibleDateStatic(dateStr);
        if (parsed != null) return formatDateForDisplay(parsed);
        return dateStr;
    }

    public static class Model {
        public String endpointBase = "";
        public String assessmentDate = "";
        public String patientDob = "";
        public String patientSex = "";
        public String encoding = "json";
        public String knowledgeBaseCanonical = "";
        public List<String> errors = new ArrayList<>();
        public String requestPretty = "";
        public String responsePretty = "";
        public List<RecommendationRow> recommendations = new ArrayList<>();
        public List<String> evaluationLines = new ArrayList<>();
        public List<String> recommendationLines = new ArrayList<>();
        public Map<Integer, String> vaccineDates = new HashMap<>();
        public Map<Integer, String> vaccineCvxs = new HashMap<>();
        public Map<Integer, String> observationDates = new HashMap<>();
        public Map<Integer, String> observationCodes = new HashMap<>();
        public String durationMs = "";
        public String engineVersion = "";
        public String operationOutcomeSummary = "";
        public String guidanceResponseSummary = "";

        public Model(HttpServletRequest req) {
            String endpointParam = req.getParameter("endpointBase");
            if (endpointParam != null && !endpointParam.isEmpty()) {
                this.endpointBase = endpointParam;
            } else {
                String sessionEndpoint = (String) req.getSession().getAttribute(SESSION_ENDPOINT_KEY);
                this.endpointBase = sessionEndpoint != null ? sessionEndpoint : DEFAULT_ENDPOINT_BASE;
            }

            String assessmentDateParam = req.getParameter("assessmentDate");
            if (assessmentDateParam == null || assessmentDateParam.isEmpty()) assessmentDateParam = req.getParameter("evalDate");
            this.assessmentDate = normalizeDateDisplay(assessmentDateParam);

            String patientDobParam = req.getParameter("patientDob");
            this.patientDob = normalizeDateDisplay(patientDobParam);

            this.patientSex = req.getParameter("patientSex") != null ? req.getParameter("patientSex") : "";
            this.encoding = req.getParameter("encoding") != null ? req.getParameter("encoding") : "json";

            String kbParam = req.getParameter("knowledgeBase");
            if (kbParam == null || kbParam.isEmpty()) kbParam = req.getParameter("knowledgeBaseCanonical");
            if (kbParam != null && !kbParam.trim().isEmpty()) {
                this.knowledgeBaseCanonical = kbParam.trim();
            } else {
                // pre-fill default if no param (first visit or missing)
                // Only pre-fill on GET without explicit empty param; check if request has any knowledgeBase param
                // If parameter was explicitly not present, use default
                if (req.getParameter("knowledgeBase") == null && req.getParameter("knowledgeBaseCanonical") == null) {
                    this.knowledgeBaseCanonical = DEFAULT_KNOWLEDGE_BASE_CANONICAL;
                } else {
                    this.knowledgeBaseCanonical = "";
                }
            }

            for (int i = 1; i <= NUM_IMMUNIZATION_ROWS; i++) {
                String dateVal = req.getParameter("vaccineDate" + i);
                String cvxVal = req.getParameter("vaccineCvx" + i);
                if (dateVal != null && !dateVal.isEmpty()) this.vaccineDates.put(i, normalizeDateDisplay(dateVal));
                if (cvxVal != null && !cvxVal.isEmpty()) this.vaccineCvxs.put(i, cvxVal.trim());
            }
            for (int i = 1; i <= NUM_OBSERVATION_ROWS; i++) {
                String dateVal = req.getParameter("observationDate" + i);
                String codeVal = req.getParameter("observationCode" + i);
                if (dateVal != null && !dateVal.isEmpty()) this.observationDates.put(i, normalizeDateDisplay(dateVal));
                if (codeVal != null && !codeVal.isEmpty()) this.observationCodes.put(i, codeVal.trim());
            }
        }
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            if ("conformance".equals(req.getParameter("view"))) {
                renderConformance(req, resp);
                return;
            }
            Model model = new Model(req);
            renderPage(req, resp, model);
        } catch (Exception e) {
            e.printStackTrace();
            throw new ServletException(e);
        }
    }

    public static class Result {
        public String requestPretty = "";
        public String responsePretty = "";
        public List<RecommendationRow> recommendationRows = new ArrayList<>();
        public String extractedEvaluations = "";
        public String extractedRecommendations = "";
        public String error = "";
        public String durationMs = "";
        public String engineVersion = "";
        public String operationOutcomeSummary = "";
        public String guidanceResponseSummary = "";
        public boolean hasError() { return !error.isEmpty(); }
    }

    public static class RecommendationRow {
        public String vaccineName;
        public String cvxCode;
        public String forecastStatus;
        public String earliestDate;
        public String recommendedDate;
        public RecommendationRow(String vaccineName, String cvxCode, String forecastStatus, String earliestDate, String recommendedDate) {
            this.vaccineName = vaccineName != null ? vaccineName : "";
            this.cvxCode = cvxCode != null ? cvxCode : "";
            this.forecastStatus = forecastStatus != null ? forecastStatus : "";
            this.earliestDate = earliestDate != null ? earliestDate : "";
            this.recommendedDate = recommendedDate != null ? recommendedDate : "";
        }
    }

    public static class ImmunizationRecord {
        public LocalDate date;
        public String cvx;
        public ImmunizationRecord(LocalDate date, String cvx) { this.date = date; this.cvx = cvx; }
    }

    public static class ObservationRecord {
        public LocalDate date;
        public String code;
        public String display;
        public ObservationRecord(LocalDate date, String code, String display) { this.date = date; this.code = code; this.display = display; }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            if ("conformance".equals(req.getParameter("view"))) {
                renderConformance(req, resp);
                return;
            }
            Model model = new Model(req);
            List<String> errors = new ArrayList<>();

            if (model.endpointBase.isEmpty()) errors.add("Endpoint Base URL is required");

            LocalDate assessmentDate = null;
            if (model.assessmentDate.isEmpty()) {
                errors.add("Assessment Date is required");
            } else {
                try { assessmentDate = parseFlexibleDate(model.assessmentDate); } catch (IllegalArgumentException e) { errors.add("Assessment Date: " + e.getMessage()); }
            }

            LocalDate patientDob = null;
            if (model.patientDob.isEmpty()) {
                errors.add("Patient DOB is required");
            } else {
                try { patientDob = parseFlexibleDate(model.patientDob); } catch (IllegalArgumentException e) { errors.add("Patient DOB: " + e.getMessage()); }
            }

            if (model.patientSex.isEmpty()) errors.add("Patient Sex is required");
            else if (!model.patientSex.matches("[MFOU]")) errors.add("Patient Sex must be one of: M, F, O, U");

            // knowledgeBase canonical is optional per spec but ICE returns HandledError if missing; allow blank to demonstrate error, but warn
            // No validation error, just let server return operationOutcome

            List<ImmunizationRecord> immunizations = new ArrayList<>();
            for (int i = 1; i <= NUM_IMMUNIZATION_ROWS; i++) {
                String dateStr = model.vaccineDates.getOrDefault(i, "").trim();
                String cvxStr = model.vaccineCvxs.getOrDefault(i, "").trim();
                if (dateStr.isEmpty() && cvxStr.isEmpty()) continue;
                if (dateStr.isEmpty() || cvxStr.isEmpty()) { errors.add("Immunization Row " + i + " requires both date and CVX"); continue; }
                LocalDate vaccineDate = null;
                try { vaccineDate = parseFlexibleDate(dateStr); } catch (IllegalArgumentException e) { errors.add("Immunization Row " + i + " date: " + e.getMessage()); continue; }
                if (!cvxStr.matches("\\d+")) { errors.add("Immunization Row " + i + " CVX must be numeric"); continue; }
                immunizations.add(new ImmunizationRecord(vaccineDate, cvxStr));
            }

            List<ObservationRecord> observations = new ArrayList<>();
            for (int i = 1; i <= NUM_OBSERVATION_ROWS; i++) {
                String dateStr = model.observationDates.getOrDefault(i, "").trim();
                String codeStr = model.observationCodes.getOrDefault(i, "").trim();
                if (dateStr.isEmpty() && codeStr.isEmpty()) continue;
                if (dateStr.isEmpty() || codeStr.isEmpty()) { errors.add("Observation Row " + i + " requires both date and code"); continue; }
                LocalDate obsDate = null;
                try { obsDate = parseFlexibleDate(dateStr); } catch (IllegalArgumentException e) { errors.add("Observation Row " + i + " date: " + e.getMessage()); continue; }
                String display = CONTEXTUAL_CONDITION_CODES.getOrDefault(codeStr, codeStr);
                observations.add(new ObservationRecord(obsDate, codeStr, display));
            }

            if (!errors.isEmpty()) {
                model.errors = errors;
                renderPage(req, resp, model);
                return;
            }

            Result result = runForecast(model.endpointBase, model.encoding, assessmentDate, patientDob, model.patientSex, immunizations, observations, model.knowledgeBaseCanonical);

            req.getSession().setAttribute(SESSION_ENDPOINT_KEY, model.endpointBase);

            model.requestPretty = result.requestPretty;
            model.responsePretty = result.responsePretty;
            model.recommendations = result.recommendationRows;
            model.durationMs = result.durationMs;
            model.engineVersion = result.engineVersion;
            model.operationOutcomeSummary = result.operationOutcomeSummary;
            model.guidanceResponseSummary = result.guidanceResponseSummary;
            if (result.hasError()) model.errors.add(result.error);

            renderPage(req, resp, model);
        } catch (Exception e) {
            e.printStackTrace();
            throw new ServletException(e);
        }
    }

    private Result runForecast(String endpointBase, String encoding, LocalDate assessmentDate,
            LocalDate patientDob, String patientSex, List<ImmunizationRecord> immunizations,
            List<ObservationRecord> observations, String knowledgeBaseCanonical) {
        Result result = new Result();
        try {
            FhirContext ctx = getFhirContext(encoding);
            // Disable eager CapabilityStatement fetch: ICE advertises FHIR 6.0.0 which HAPI R4 cannot parse (HAPI-1357)
            ctx.getRestfulClientFactory().setServerValidationMode(ServerValidationModeEnum.NEVER);
            ctx.getRestfulClientFactory().setSocketTimeout(30 * 1000);
            // connect timeout if available (HAPI 7.4.0 supports it via RestfulClientFactory)
            try { ctx.getRestfulClientFactory().setConnectTimeout(10 * 1000); } catch (Exception ignored) {}
            IGenericClient client = ctx.newRestfulGenericClient(endpointBase);

            Parameters requestParams = new Parameters();

            if (assessmentDate != null) {
                requestParams.addParameter("assessmentDate", new DateType(java.sql.Date.valueOf(assessmentDate)));
            }

            if (patientDob != null && patientSex != null && !patientSex.isEmpty()) {
                Patient patient = new Patient();
                patient.setBirthDate(java.sql.Date.valueOf(patientDob));
                String genderCode = mapGenderCode(patientSex);
                if (genderCode != null) patient.setGender(AdministrativeGender.fromCode(genderCode));
                requestParams.addParameter().setName("patient").setResource(patient);
            }

            // FHIR R6 style: knowledgeBase as valueCanonical
            if (knowledgeBaseCanonical != null && !knowledgeBaseCanonical.trim().isEmpty()) {
                String kbTrim = knowledgeBaseCanonical.trim();
                // Use CanonicalType per R6 spec (valueCanonical)
                requestParams.addParameter("knowledgeBase", new CanonicalType(kbTrim));
            }

            for (ImmunizationRecord immunRecord : immunizations) {
                Immunization immunization = new Immunization();
                immunization.setOccurrence(new DateTimeType(java.sql.Date.valueOf(immunRecord.date)));
                CodeableConcept vaccineCodeConcept = new CodeableConcept();
                vaccineCodeConcept.addCoding(new Coding("http://hl7.org/fhir/sid/cvx", immunRecord.cvx, null));
                immunization.setVaccineCode(vaccineCodeConcept);
                requestParams.addParameter().setName("immunization").setResource(immunization);
            }

            for (ObservationRecord obsRecord : observations) {
                Observation obs = new Observation();
                obs.setStatus(Observation.ObservationStatus.FINAL);
                CodeableConcept codeConcept = new CodeableConcept();
                codeConcept.addCoding(new Coding("http://ivci.org/CodeSystem/contextual-condition", obsRecord.code, obsRecord.display));
                // also set text for readability
                codeConcept.setText(obsRecord.display);
                obs.setCode(codeConcept);
                // valueDateTime per ICE example
                obs.setValue(new DateTimeType(java.sql.Date.valueOf(obsRecord.date)));
                requestParams.addParameter().setName("observation").setResource(obs);
            }

            result.requestPretty = prettyPrintFhir(requestParams, encoding);

            Parameters responseParams = null;
            try {
                responseParams = client.operation().onServer().named("$immds-forecast").withParameters(requestParams).returnResourceType(Parameters.class).execute();
            } catch (BaseServerResponseException e) {
                try {
                    StringBuilder sb = new StringBuilder();
                    sb.append("Server returned HTTP ").append(e.getStatusCode()).append(": ").append(e.getMessage());
                    String body = e.getResponseBody();
                    if (body != null && !body.isEmpty()) {
                        String snippet = body.length() > 4000 ? body.substring(0, 4000) + "...(truncated)" : body;
                        sb.append("\nResponse body:\n").append(snippet);
                        if (result.responsePretty.isEmpty()) result.responsePretty = body;
                    }
                    if (e.getOperationOutcome() != null && e.getOperationOutcome() instanceof OperationOutcome) {
                        try { result.responsePretty = prettyPrintFhir((OperationOutcome) e.getOperationOutcome(), encoding); } catch (Exception ignored) {}
                    }
                    if (e.getCause() != null) sb.append("\nCause: ").append(e.getCause().toString());
                    // Detect metadata-fetch misconfiguration and hint
                    String msgLower = e.getMessage() != null ? e.getMessage().toLowerCase() : "";
                    if (msgLower.contains("hapi-1357") || msgLower.contains("metadata")) {
                        sb.append("\nHint: HAPI tried to fetch [base]/metadata (CapabilityStatement). ICE advertises FHIR 6.0.0 which HAPI R4 cannot parse. ServerValidationMode is now NEVER; if you still see this, check that the deployed WAR contains the fix and that endpointBase is https://ice-service.cdsframework.org/opencds-decision-support-service/cds (not .../fhir).");
                    }
                    result.error = sb.toString();
                } catch (Exception ex) {
                    result.error = "Server error: " + e.getMessage() + " (while building diagnostics: " + ex.getMessage() + ")";
                }
                if (responseParams == null && result.responsePretty.isEmpty()) {
                    if (result.error == null || result.error.isEmpty()) result.error = e.getMessage();
                    return result;
                }
            }

            if (responseParams != null) {
                result.responsePretty = prettyPrintFhir(responseParams, encoding);
                result.recommendationRows = extractRecommendationRows(responseParams);
                extractMeta(responseParams, result);
            }

        } catch (FhirClientConnectionException e) {
            StringBuilder sb = new StringBuilder();
            sb.append("Failed to connect to endpoint ").append(endpointBase).append(": ").append(e.getMessage());
            if (e.getCause() != null) sb.append("\nCause: ").append(e.getCause().toString());
            sb.append("\nCheck egress firewall/proxy on the StepIntoCDSI host allows outbound HTTPS to ice-service.cdsframework.org.");
            result.error = sb.toString();
            e.printStackTrace();
        } catch (Exception e) {
            StringBuilder sb = new StringBuilder();
            sb.append(e.getClass().getSimpleName()).append(": ").append(e.getMessage() != null ? e.getMessage() : "(no message)");
            if (e.getCause() != null) sb.append("\nCause: ").append(e.getCause().toString());
            // include truncated stack for diagnostics
            java.io.StringWriter sw = new java.io.StringWriter();
            e.printStackTrace(new java.io.PrintWriter(sw));
            String stack = sw.toString();
            if (stack.length() > 6000) stack = stack.substring(0, 6000) + "...(truncated)";
            sb.append("\nStack:\n").append(stack);
            result.error = sb.toString();
            e.printStackTrace();
        }
        return result;
    }

    private void extractMeta(Parameters params, Result result) {
        for (ParametersParameterComponent p : params.getParameter()) {
            if ("durationMs".equals(p.getName()) && p.getValue() != null) {
                result.durationMs = p.getValue().toString();
                // Fhir IntegerType toString may include extra; try getValueAsString
                try {
                    if (p.getValue() instanceof org.hl7.fhir.r4.model.IntegerType) {
                        result.durationMs = String.valueOf(((org.hl7.fhir.r4.model.IntegerType) p.getValue()).getValue());
                    }
                } catch (Exception ignored) {}
            }
            if ("engineVersion".equals(p.getName()) && p.getValue() != null) {
                try {
                    if (p.getValue() instanceof StringType) result.engineVersion = ((StringType) p.getValue()).getValue();
                    else result.engineVersion = p.getValue().toString();
                } catch (Exception ex) { result.engineVersion = p.getValue().toString(); }
            }
            if ("operationOutcome".equals(p.getName()) && p.getResource() instanceof OperationOutcome) {
                OperationOutcome oo = (OperationOutcome) p.getResource();
                StringBuilder sb = new StringBuilder();
                for (OperationOutcome.OperationOutcomeIssueComponent issue : oo.getIssue()) {
                    if (sb.length() > 0) sb.append("; ");
                    sb.append(issue.getSeverity() != null ? issue.getSeverity().toString() : "").append(" ");
                    sb.append(issue.getCode() != null ? issue.getCode().toString() : "");
                    if (issue.getDetails() != null && issue.getDetails().getText() != null) sb.append(": ").append(issue.getDetails().getText());
                    else if (issue.getDiagnostics() != null) sb.append(": ").append(issue.getDiagnostics());
                }
                result.operationOutcomeSummary = sb.toString();
            }
            if ("guidanceResponse".equals(p.getName()) && p.getResource() instanceof GuidanceResponse) {
                GuidanceResponse gr = (GuidanceResponse) p.getResource();
                result.guidanceResponseSummary = gr.getStatus() != null ? gr.getStatus().toString() : "";
                if (gr.hasOccurrenceDateTime() && gr.getOccurrenceDateTime() != null) {
                    java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
                    sdf.setTimeZone(java.util.TimeZone.getTimeZone("UTC"));
                    result.guidanceResponseSummary += " @ " + sdf.format(gr.getOccurrenceDateTime());
                } else if (gr.hasOccurrenceDateTimeElement() && gr.getOccurrenceDateTimeElement().getValue() != null) {
                    result.guidanceResponseSummary += " @ " + gr.getOccurrenceDateTimeElement().getValueAsString();
                }
            }
        }
    }

    private String mapGenderCode(String code) {
        if (code == null || code.isEmpty()) return null;
        switch (code.toUpperCase()) {
            case "M": return "male";
            case "F": return "female";
            case "O": return "other";
            case "U": return "unknown";
            default: return null;
        }
    }

    private FhirContext getFhirContext(String encoding) {
        // Both encodings use R4 context (ICE payload compatible); R6 not yet in HAPI 7.4.0
        return FhirContext.forR4();
    }

    private String prettyPrintFhir(Resource resource, String encoding) {
        FhirContext ctx = getFhirContext(encoding);
        if ("xml".equalsIgnoreCase(encoding)) return ctx.newXmlParser().setPrettyPrint(true).encodeResourceToString(resource);
        else return ctx.newJsonParser().setPrettyPrint(true).encodeResourceToString(resource);
    }

    private List<RecommendationRow> extractRecommendationRows(Parameters params) {
        List<RecommendationRow> rows = new ArrayList<>();
        List<ImmunizationRecommendation> recommendations = new ArrayList<>();
        for (ParametersParameterComponent param : params.getParameter()) {
            if (param.getResource() instanceof ImmunizationRecommendation) {
                recommendations.add((ImmunizationRecommendation) param.getResource());
            }
        }
        for (ImmunizationRecommendation immRec : recommendations) {
            if (immRec.getRecommendation() == null || immRec.getRecommendation().isEmpty()) continue;
            for (ImmunizationRecommendationRecommendationComponent rec : immRec.getRecommendation()) {
                String vaccineName = "";
                String cvxCode = "";
                if (rec.getVaccineCode() != null && !rec.getVaccineCode().isEmpty()) {
                    for (CodeableConcept vaccineCode : rec.getVaccineCode()) {
                        if (vaccineCode.getCoding() != null) {
                            for (Coding coding : vaccineCode.getCoding()) {
                                if ("http://hl7.org/fhir/sid/cvx".equals(coding.getSystem())) {
                                    if (!cvxCode.isEmpty()) cvxCode += ", ";
                                    cvxCode += coding.getCode();
                                    if (coding.getDisplay() != null && !coding.getDisplay().isEmpty()) vaccineName = coding.getDisplay();
                                }
                            }
                        }
                        // also handle text if no coding
                        if (cvxCode.isEmpty() && vaccineCode.getText() != null) vaccineName = vaccineCode.getText();
                    }
                }
                // fallback to description if no vaccineCode
                if (cvxCode.isEmpty() && vaccineName.isEmpty() && rec.getDescription() != null) vaccineName = rec.getDescription();

                String forecastStatus = "";
                if (rec.getForecastStatus() != null) {
                    if (rec.getForecastStatus().getCoding() != null && !rec.getForecastStatus().getCoding().isEmpty()) {
                        Coding statusCoding = rec.getForecastStatus().getCoding().get(0);
                        if (statusCoding.getDisplay() != null && !statusCoding.getDisplay().isEmpty()) forecastStatus = statusCoding.getDisplay();
                        else if (statusCoding.getCode() != null) forecastStatus = statusCoding.getCode();
                    } else if (rec.getForecastStatus().getText() != null) {
                        forecastStatus = rec.getForecastStatus().getText();
                    }
                }

                String earliestDate = "";
                String recommendedDate = "";
                if (rec.getDateCriterion() != null && !rec.getDateCriterion().isEmpty()) {
                    for (ImmunizationRecommendationRecommendationDateCriterionComponent dateCrit : rec.getDateCriterion()) {
                        if (dateCrit.getCode() != null && dateCrit.getValue() != null) {
                            String codeDisplay = "";
                            if (dateCrit.getCode().getCoding() != null && !dateCrit.getCode().getCoding().isEmpty()) {
                                Coding codeCoding = dateCrit.getCode().getCoding().get(0);
                                codeDisplay = codeCoding.getDisplay() != null ? codeCoding.getDisplay() : codeCoding.getCode();
                                // also check code value like 30981-5
                                if (codeCoding.getCode() != null) {
                                    if ("30981-5".equals(codeCoding.getCode())) codeDisplay = "Earliest";
                                    else if ("30980-7".equals(codeCoding.getCode())) codeDisplay = "Recommend";
                                    else if ("59777-3".equals(codeCoding.getCode())) codeDisplay = "Latest";
                                    else if ("59778-1".equals(codeCoding.getCode())) codeDisplay = "Overdue";
                                }
                            } else if (dateCrit.getCode().getText() != null) {
                                codeDisplay = dateCrit.getCode().getText();
                            }
                            String dateValue = "";
                            try { dateValue = extractDateValue(dateCrit.getValue().toString()); } catch (Exception e) { dateValue = ""; }
                            if (codeDisplay != null && codeDisplay.contains("Earliest")) earliestDate = dateValue;
                            else if (codeDisplay != null && codeDisplay.contains("Recommend")) recommendedDate = dateValue;
                            else if (codeDisplay != null && codeDisplay.toLowerCase().contains("earliest")) earliestDate = dateValue;
                            else if (codeDisplay != null && codeDisplay.toLowerCase().contains("recommend")) recommendedDate = dateValue;
                        }
                    }
                }

                // ICE may return series info instead of cvx? handle series field
                if (cvxCode.isEmpty() && rec.getSeries() != null && !rec.getSeries().isEmpty()) {
                    vaccineName = rec.getSeries();
                }

                rows.add(new RecommendationRow(vaccineName, cvxCode, forecastStatus, earliestDate, recommendedDate));
            }
        }
        return rows;
    }

    private String extractDateValue(String dateTimeString) {
        if (dateTimeString == null || dateTimeString.isEmpty()) return "";
        dateTimeString = dateTimeString.trim();
        if (dateTimeString.matches("^[A-Za-z]{3}\\s+[A-Za-z]{3}\\s+.*")) {
            try {
                SimpleDateFormat javeDateFormat = new SimpleDateFormat("EEE MMM dd HH:mm:ss zzz yyyy");
                java.util.Date date = javeDateFormat.parse(dateTimeString);
                SimpleDateFormat outputFormat = new SimpleDateFormat("MM/dd/yyyy");
                return outputFormat.format(date);
            } catch (Exception e) { }
        }
        if (dateTimeString.contains("T")) {
            String datePart = dateTimeString.substring(0, dateTimeString.indexOf("T"));
            try {
                SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd");
                SimpleDateFormat outputFormat = new SimpleDateFormat("MM/dd/yyyy");
                java.util.Date date = inputFormat.parse(datePart);
                return outputFormat.format(date);
            } catch (Exception e) { }
        }
        if (dateTimeString.matches("\\d{4}-\\d{2}-\\d{2}")) {
            try {
                SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd");
                SimpleDateFormat outputFormat = new SimpleDateFormat("MM/dd/yyyy");
                java.util.Date date = inputFormat.parse(dateTimeString);
                return outputFormat.format(date);
            } catch (Exception e) { }
        }
        return "";
    }

    private void renderPage(HttpServletRequest req, HttpServletResponse resp, Model model) throws IOException {
        resp.setContentType("text/html; charset=UTF-8");
        PrintWriter out = resp.getWriter();

        out.println("<!DOCTYPE html>");
        out.println("<html>");
        out.println("<head>");
        out.println("  <meta charset=\"UTF-8\">");
        out.println("  <title>ICE ImmDS-Forecast $immds-forecast Test Harness (R6)</title>");
        out.println("  <style>");
        out.println("    body { font-family: Arial, sans-serif; margin: 20px; background-color: #f5f5f5; }");
        out.println("    h1 { color: #333; }");
        out.println("    h2 { color: #555; margin-top: 30px; border-bottom: 2px solid #007bff; padding-bottom: 10px; }");
        out.println("    h3 { color: #666; }");
        out.println("    h4 { color: #888; margin-top: 15px; }");
        out.println("    .form-container { background-color: white; padding: 20px; border-radius: 5px; box-shadow: 0 2px 5px rgba(0,0,0,0.1); }");
        out.println("    .form-group { margin-bottom: 15px; }");
        out.println("    label { display: inline-block; width: 200px; font-weight: bold; }");
        out.println("    input[type='text'], select { padding: 8px; border: 1px solid #ddd; border-radius: 4px; font-size: 14px; }");
        out.println("    input[type='text'] { width: 450px; }");
        out.println("    .short-input { width: 120px !important; }");
        out.println("    .encoding-group { margin-bottom: 15px; }");
        out.println("    .radio-label { display: inline-block; margin-right: 20px; }");
        out.println("    .immunizations-table { margin-top: 15px; }");
        out.println("    table { border-collapse: collapse; width: 100%; margin: 10px 0; }");
        out.println("    th, td { border: 1px solid #ddd; padding: 8px; text-align: left; }");
        out.println("    th { background-color: #f2f2f2; font-weight: bold; }");
        out.println("    tr:nth-child(even) { background-color: #f9f9f9; }");
        out.println("    .buttons { margin: 20px 0; }");
        out.println("    button { padding: 10px 20px; margin-right: 10px; font-size: 14px; cursor: pointer; background-color: #007bff; color: white; border: none; border-radius: 4px; }");
        out.println("    button:hover { background-color: #0056b3; }");
        out.println("    .error-container { background-color: #f8d7da; border: 1px solid #f5c6cb; color: #721c24; padding: 15px; border-radius: 5px; margin-top: 20px; }");
        out.println("    .info-container { background-color: #d1ecf1; border: 1px solid #bee5eb; color: #0c5460; padding: 15px; border-radius: 5px; margin-top: 20px; }");
        out.println("    .output-container { background-color: white; border: 1px solid #ddd; border-radius: 5px; padding: 15px; margin-top: 20px; max-height: 500px; overflow: auto; }");
        out.println("    pre { background-color: #f4f4f4; border: 1px solid #ddd; padding: 10px; border-radius: 4px; overflow-x: auto; font-family: 'Courier New', monospace; font-size: 12px; line-height: 1.4; }");
        out.println("    .copy-button { padding: 5px 10px; font-size: 12px; margin-bottom: 10px; background-color: #28a745; }");
        out.println("    .copy-button:hover { background-color: #218838; }");
        out.println("    .sidebar { float: right; width: 30%; margin-left: 20px; background-color: #e8f4f8; border: 2px solid #007bff; border-radius: 5px; padding: 15px; box-shadow: 0 2px 5px rgba(0,0,0,0.1); }");
        out.println("    .sidebar h3 { margin-top: 0; color: #0056b3; }");
        out.println("    .sidebar p { font-size: 14px; line-height: 1.6; color: #333; }");
        out.println("    .sidebar a { color: #007bff; text-decoration: none; font-weight: bold; }");
        out.println("    .sidebar a:hover { text-decoration: underline; }");
        out.println("    .main-content { overflow: auto; }");
        out.println("    .meta-box { background-color: #f0f8ff; border: 1px solid #b0d4f1; padding: 10px; border-radius: 4px; margin-top: 15px; }");
        out.println("  </style>");
        out.println("  <script>");
        out.println("    function copyText(elementId) {");
        out.println("      var elem = document.getElementById(elementId);");
        out.println("      var text = elem.textContent || elem.innerText;");
        out.println("      navigator.clipboard.writeText(text).then(function() { alert('Copied to clipboard'); }).catch(function(err) { alert('Failed to copy: ' + err); });");
        out.println("    }");
        out.println("  </script>");
        out.println("</head>");
        out.println("<body>");
        // Sidebar
        out.println("  <div class=\"sidebar\">");
        out.println("    <h3>About ICE ImmDS (R6)</h3>");
        out.println("    <p>This harness calls the external <strong>ICE</strong> (Immunization Calculation Engine) service at <code>/cds/$immds-forecast</code>, the <em>proposed R6 HLN ImmDS forecast operation</em>. Request/response are FHIR <code>Parameters</code> per <a href=\"https://ice-service.cdsframework.org/opencds-decision-support-service/cds/ice-forecast/swagger-ui/index.html#/ImmDS%20Forecast/immDsForecast\" target=\"_blank\">ICE Swagger</a>.</p>");
        out.println("    <p><strong>Request differences vs local /sandbox:</strong></p>");
        out.println("    <ul style=\"font-size: 13px; margin: 10px 0;\">");
        out.println("      <li><code>knowledgeBase</code> is <code>valueCanonical</code> (<code>url|version</code>) not <code>CodeableConcept</code>+<code>knowledgeBaseVersion</code></li>");
        out.println("      <li>Supports repeated <code>observation</code> (contextual conditions, e.g. varicella 024)</li>");
        out.println("      <li>Alternative <code>data</code> Bundle not yet exposed here</li>");
        out.println("      <li>Response includes <code>durationMs</code>, <code>engineVersion</code>, <code>GuidanceResponse</code>, <code>ImmunizationEvaluation</code></li>");
        out.println("    </ul>");
        out.println("    <p><strong>Endpoint:</strong> <code>POST [base]/$immds-forecast</code> with base <code>.../cds</code> (not <code>.../fhir</code>).</p>");
        out.println("    <p><a href=\"https://hl7.org/fhir/us/immds/index.html\" target=\"_blank\">📖 ImmDS IG</a> &nbsp; <a href=\"https://terminology.cdsframework.org/ice/OperationDefinition/immds-forecast\" target=\"_blank\">📖 OperationDefinition</a></p>");
        out.println("    <p><a href=\"/step/iceSandbox?view=conformance\" style=\"background:#007bff;color:white;padding:6px 10px;border-radius:4px;display:inline-block;\">📊 Wire Conformance Report (live)</a> <small style=\"color:#555;\">Wire-only, not clinical</small></p>");
        // Gap analysis box
        out.println("    <div class=\"info-container\" style=\"font-size:13px;\">");
        out.println("      <strong>Gap analysis vs proposed API:</strong>");
        out.println("      <ul style=\"margin:5px 0; padding-left:18px;\">");
        out.println("        <li>Canonical KB: ✅ this page; local /sandbox uses old style</li>");
        out.println("        <li>Observations: ✅ dropdown below; local /sandbox unsupported</li>");
        out.println("        <li>Bundle <code>data</code>: ⏳ not yet in UI – API supports it</li>");
        out.println("        <li>FHIR 6.0.0 vs 4.0.1 wire-compatible via HAPI R4</li>");
        out.println("      </ul>");
        out.println("    </div>");
        out.println("    <hr style=\"margin: 15px 0; border: none; border-top: 1px solid #0056b3;\">");
        out.println("    <h4 style=\"margin-top: 15px; margin-bottom: 8px; color: #0056b3;\">Quick Examples</h4>");
        List<SandboxServletExamples.ExampleCategory> examples = SandboxServletExamples.getExamples();
        String iceKbEncoded;
        try { iceKbEncoded = java.net.URLEncoder.encode(DEFAULT_KNOWLEDGE_BASE_CANONICAL, "UTF-8"); } catch (Exception e) { iceKbEncoded = DEFAULT_KNOWLEDGE_BASE_CANONICAL; }
        for (SandboxServletExamples.ExampleCategory category : examples) {
            out.println("    <div style=\"margin-bottom: 12px;\">");
            out.println("      <strong style=\"font-size: 12px; color: #0056b3;\">" + htmlEscape(category.title) + "</strong>");
            out.println("      <ul style=\"font-size: 12px; margin: 5px 0; padding-left: 20px;\">");
            for (SandboxServletExamples.ExampleLink link : category.links) {
                // link.url contains ?evalDate=...&...; append knowledgeBaseCanonical (encoded)
                String iceUrl = "/step/iceSandbox" + htmlEscape(link.url) + "&knowledgeBase=" + htmlEscape(iceKbEncoded);
                out.println("        <li><a href=\"" + iceUrl + "\" style=\"color: #007bff; text-decoration: none;\">" + htmlEscape(link.name) + "</a></li>");
            }
            out.println("      </ul>");
            out.println("    </div>");
        }
        // ICE specific example with observation
        out.println("    <div style=\"margin-bottom: 12px;\">");
        out.println("      <strong style=\"font-size: 12px; color: #0056b3;\">ICE Specific</strong>");
        out.println("      <ul style=\"font-size: 12px; margin: 5px 0; padding-left: 20px;\">");
        out.println("        <li><a href=\"/step/iceSandbox?evalDate=20260404&patientDob=20101201&patientSex=F&vaccineDate1=20160202&vaccineCvx1=111&observationDate1=20160101&observationCode1=024&knowledgeBase=" + htmlEscape(iceKbEncoded) + "\" style=\"color: #007bff; text-decoration: none;\">ICE Swagger varicella example</a></li>");
        out.println("      </ul>");
        out.println("    </div>");
        out.println("  </div>");
        out.println("  <div class=\"main-content\">");
        out.println("  <h1>ICE ImmDS-Forecast $immds-forecast Test Harness (R6)</h1>");
        out.println("  <p style=\"color:#555;\">Demonstrates calls to <code>https://ice-service.cdsframework.org/opencds-decision-support-service/cds/$immds-forecast</code> (or any compatible base). All inputs are FHIR; <code>knowledgeBase</code> is a canonical <code>url|version</code>.</p>");
        out.println("  <div class=\"form-container\">");
        out.println("    <form method=\"POST\" action=\"iceSandbox\">");
        out.println("    <h2>ImmDS-Forecast Configuration</h2>");
        out.println("    <div class=\"form-group\">");
        out.println("      <label for=\"endpointBase\">Endpoint Base URL:</label>");
        out.println("      <input type=\"text\" id=\"endpointBase\" name=\"endpointBase\" value=\"" + escapeHtml(model.endpointBase) + "\" placeholder=\"https://ice-service.cdsframework.org/opencds-decision-support-service/cds\">");
        out.println("    </div>");
        out.println("    <div class=\"encoding-group\">");
        out.println("      <label>Encoding:</label>");
        out.println("      <label class=\"radio-label\"><input type=\"radio\" name=\"encoding\" value=\"json\"" + ("json".equals(model.encoding) ? " checked" : "") + "> JSON</label>");
        out.println("      <label class=\"radio-label\"><input type=\"radio\" name=\"encoding\" value=\"xml\"" + ("xml".equals(model.encoding) ? " checked" : "") + "> XML</label>");
        out.println("    </div>");
        out.println("    <div class=\"form-group\">");
        out.println("      <label for=\"knowledgeBase\">Knowledge Base (canonical):</label>");
        out.println("      <input type=\"text\" id=\"knowledgeBase\" name=\"knowledgeBase\" value=\"" + escapeHtml(model.knowledgeBaseCanonical) + "\" placeholder=\"https://terminology.cdsframework.org/PlanDefinition/ice-forecast|1.0.0\">");
        out.println("      <small style=\"color: #666; display: block; margin-top: 4px; margin-left:200px;\">Format <code>url|version</code> – e.g. <code>" + escapeHtml(DEFAULT_KNOWLEDGE_BASE_CANONICAL) + "</code>. Leave blank to test server error handling.</small>");
        out.println("    </div>");
        out.println("    <div class=\"form-group\">");
        out.println("      <label for=\"assessmentDate\">Assessment Date:</label>");
        out.println("      <input type=\"text\" id=\"assessmentDate\" name=\"assessmentDate\" value=\"" + escapeHtml(model.assessmentDate) + "\" placeholder=\"MM/DD/YYYY\" class=\"short-input\">");
        out.println("    </div>");
        out.println("    <div class=\"form-group\">");
        out.println("      <label for=\"patientDob\">Patient DOB:</label>");
        out.println("      <input type=\"text\" id=\"patientDob\" name=\"patientDob\" value=\"" + escapeHtml(model.patientDob) + "\" placeholder=\"MM/DD/YYYY\" class=\"short-input\">");
        out.println("    </div>");
        out.println("    <div class=\"form-group\">");
        out.println("      <label for=\"patientSex\">Patient Sex:</label>");
        out.println("      <select id=\"patientSex\" name=\"patientSex\">");
        out.println("        <option value=\"\"" + ("".equals(model.patientSex) ? " selected" : "") + ">-- Select --</option>");
        out.println("        <option value=\"M\"" + ("M".equals(model.patientSex) ? " selected" : "") + ">M</option>");
        out.println("        <option value=\"F\"" + ("F".equals(model.patientSex) ? " selected" : "") + ">F</option>");
        out.println("        <option value=\"O\"" + ("O".equals(model.patientSex) ? " selected" : "") + ">O</option>");
        out.println("        <option value=\"U\"" + ("U".equals(model.patientSex) ? " selected" : "") + ">U</option>");
        out.println("      </select>");
        out.println("    </div>");
        out.println("    <h2>Immunizations</h2>");
        out.println("    <div class=\"immunizations-table\">");
        out.println("      <table>");
        out.println("        <tr><th>#</th><th>Vaccine Date (MM/DD/YYYY)</th><th>CVX Code</th></tr>");
        for (int i = 1; i <= NUM_IMMUNIZATION_ROWS; i++) {
            String dateVal = model.vaccineDates.getOrDefault(i, "");
            String cvxVal = model.vaccineCvxs.getOrDefault(i, "");
            out.println("        <tr>");
            out.println("          <td>" + i + "</td>");
            out.println("          <td><input type=\"text\" name=\"vaccineDate" + i + "\" value=\"" + escapeHtml(dateVal) + "\" placeholder=\"MM/DD/YYYY\" style=\"width: 120px;\"></td>");
            out.println("          <td><input type=\"text\" name=\"vaccineCvx" + i + "\" value=\"" + escapeHtml(cvxVal) + "\" placeholder=\"CVX code\" style=\"width: 100px;\"></td>");
            out.println("        </tr>");
        }
        out.println("      </table>");
        out.println("    </div>");
        out.println("    <h2>Observations (Contextual Conditions)</h2>");
        out.println("    <p style=\"color:#666; font-size:13px;\">Maps to <code>Observation</code> with system <code>http://ivci.org/CodeSystem/contextual-condition</code> and <code>valueDateTime</code>. Use dropdown for known IVCI codes.</p>");
        out.println("    <div class=\"immunizations-table\">");
        out.println("      <table>");
        out.println("        <tr><th>#</th><th>Observation Date (MM/DD/YYYY)</th><th>Condition Code</th></tr>");
        for (int i = 1; i <= NUM_OBSERVATION_ROWS; i++) {
            String dateVal = model.observationDates.getOrDefault(i, "");
            String codeVal = model.observationCodes.getOrDefault(i, "");
            out.println("        <tr>");
            out.println("          <td>" + i + "</td>");
            out.println("          <td><input type=\"text\" name=\"observationDate" + i + "\" value=\"" + escapeHtml(dateVal) + "\" placeholder=\"MM/DD/YYYY\" style=\"width: 120px;\"></td>");
            out.println("          <td><select name=\"observationCode" + i + "\" style=\"width: 380px;\">");
            out.println("            <option value=\"\">-- Select --</option>");
            for (Map.Entry<String, String> e : CONTEXTUAL_CONDITION_CODES.entrySet()) {
                String selected = e.getKey().equals(codeVal) ? " selected" : "";
                out.println("            <option value=\"" + escapeHtml(e.getKey()) + "\"" + selected + ">" + escapeHtml(e.getKey() + " - " + e.getValue()) + "</option>");
            }
            // If current code not in known list but non-empty, add it as custom option
            if (!codeVal.isEmpty() && !CONTEXTUAL_CONDITION_CODES.containsKey(codeVal)) {
                out.println("            <option value=\"" + escapeHtml(codeVal) + "\" selected>" + escapeHtml(codeVal + " (custom)") + "</option>");
            }
            out.println("          </select></td>");
            out.println("        </tr>");
        }
        out.println("      </table>");
        out.println("    </div>");
        out.println("    <div class=\"buttons\">");
        out.println("      <button type=\"submit\">Run ICE Forecast</button>");
        out.println("    </div>");
        out.println("    </form>");
        out.println("  </div>");

        if (!model.errors.isEmpty()) {
            out.println("  <div class=\"error-container\">");
            out.println("    <h3>Errors</h3>");
            out.println("    <ul>");
            for (String error : model.errors) {
                String escaped = htmlEscape(error).replace("\n", "<br/>");
                out.println("      <li style=\"white-space:pre-wrap; word-break:break-word;\">" + escaped + "</li>");
            }
            out.println("    </ul>");
            out.println("    <small style=\"color:#721c24;\">If you see HAPI-1357/metadata, ensure the deployed WAR includes ServerValidationMode.NEVER fix. See server logs for full stack.</small>");
            out.println("  </div>");
        }

        if (!model.requestPretty.isEmpty()) {
            out.println("  <h2>Results</h2>");
            if (!model.durationMs.isEmpty() || !model.engineVersion.isEmpty() || !model.operationOutcomeSummary.isEmpty() || !model.guidanceResponseSummary.isEmpty()) {
                out.println("  <div class=\"meta-box\">");
                if (!model.durationMs.isEmpty()) out.println("    <div><strong>durationMs:</strong> " + htmlEscape(model.durationMs) + "</div>");
                if (!model.engineVersion.isEmpty()) out.println("    <div><strong>engineVersion:</strong> " + htmlEscape(model.engineVersion) + "</div>");
                if (!model.guidanceResponseSummary.isEmpty()) out.println("    <div><strong>guidanceResponse:</strong> " + htmlEscape(model.guidanceResponseSummary) + "</div>");
                if (!model.operationOutcomeSummary.isEmpty()) out.println("    <div><strong>operationOutcome:</strong> " + htmlEscape(model.operationOutcomeSummary) + "</div>");
                out.println("  </div>");
            }
            if (!model.recommendations.isEmpty()) {
                out.println("  <div class=\"output-container\">");
                out.println("    <h3>Recommendations</h3>");
                out.println("    <table style=\"width:100%; border-collapse:collapse;\">");
                out.println("      <tr style=\"background-color:#f2f2f2;\">");
                out.println("        <th style=\"border:1px solid #ddd; padding:10px; text-align:left;\">Vaccination (CVX)</th>");
                out.println("        <th style=\"border:1px solid #ddd; padding:10px; text-align:left;\">Forecast Status</th>");
                out.println("        <th style=\"border:1px solid #ddd; padding:10px; text-align:left;\">Earliest Date</th>");
                out.println("        <th style=\"border:1px solid #ddd; padding:10px; text-align:left;\">Recommended Date</th>");
                out.println("      </tr>");
                for (RecommendationRow rec : model.recommendations) {
                    out.println("      <tr>");
                    out.println("        <td style=\"border:1px solid #ddd; padding:10px;\">" + htmlEscape(rec.vaccineName.isEmpty() ? rec.cvxCode : rec.vaccineName + (rec.cvxCode.isEmpty() ? "" : " (" + rec.cvxCode + ")")) + "</td>");
                    out.println("        <td style=\"border:1px solid #ddd; padding:10px;\">" + htmlEscape(rec.forecastStatus) + "</td>");
                    out.println("        <td style=\"border:1px solid #ddd; padding:10px;\">" + htmlEscape(rec.earliestDate) + "</td>");
                    out.println("        <td style=\"border:1px solid #ddd; padding:10px;\">" + htmlEscape(rec.recommendedDate) + "</td>");
                    out.println("      </tr>");
                }
                out.println("    </table>");
                out.println("  </div>");
            } else if (!model.responsePretty.isEmpty()) {
                out.println("  <div class=\"output-container\"><p><em>No ImmunizationRecommendation found – see Response below (check operationOutcome).</em></p></div>");
            }
            out.println("  <div class=\"output-container\">");
            out.println("    <h3>Request (FHIR Parameters)</h3>");
            out.println("    <button type=\"button\" class=\"copy-button\" onclick=\"copyText('requestOutput')\">Copy</button>");
            out.println("    <pre id=\"requestOutput\">" + htmlEscape(model.requestPretty) + "</pre>");
            out.println("  </div>");
        }

        if (!model.responsePretty.isEmpty()) {
            out.println("  <div class=\"output-container\">");
            out.println("    <h3>Response (FHIR Parameters)</h3>");
            out.println("    <button type=\"button\" class=\"copy-button\" onclick=\"copyText('responseOutput')\">Copy</button>");
            out.println("    <pre id=\"responseOutput\">" + htmlEscape(model.responsePretty) + "</pre>");
            out.println("  </div>");
        }

        out.println("  </div>"); // main-content
        FooterRenderer.render(out, getServletContext());
        out.println("</body>");
        out.println("</html>");
        out.flush();
    }

    private void renderConformance(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String endpointBase = req.getParameter("endpointBase");
        if (endpointBase == null || endpointBase.trim().isEmpty()) {
            String sess = (String) req.getSession().getAttribute(SESSION_ENDPOINT_KEY);
            endpointBase = sess != null ? sess : DEFAULT_ENDPOINT_BASE;
        } else {
            req.getSession().setAttribute(SESSION_ENDPOINT_KEY, endpointBase);
        }
        resp.setContentType("text/html; charset=UTF-8");
        PrintWriter out = resp.getWriter();
        out.println("<!DOCTYPE html><html><head><meta charset=\"UTF-8\"><title>ICE Wire Conformance Report</title>");
        out.println("<style>");
        out.println("body{font-family:Arial,sans-serif;margin:20px;background:#f5f5f5;} h1{color:#333;} h2{color:#555;border-bottom:2px solid #007bff;padding-bottom:8px;}");
        out.println(".form-container{background:white;padding:15px;border-radius:5px;box-shadow:0 2px 5px rgba(0,0,0,0.1);margin-bottom:20px;}");
        out.println("table{border-collapse:collapse;width:100%;margin:10px 0;} th,td{border:1px solid #ddd;padding:8px;text-align:left;vertical-align:top;font-size:13px;} th{background:#f2f2f2;}");
        out.println(".pass{background:#d4edda;color:#155724;} .fail{background:#f8d7da;color:#721c24;} .amber{background:#fff3cd;color:#856404;}");
        out.println("pre{background:#f4f4f4;border:1px solid #ddd;padding:8px;border-radius:4px;overflow-x:auto;font-size:11px;white-space:pre-wrap;word-break:break-word;}");
        out.println(".meta{background:#e8f4f8;border:1px solid #007bff;padding:10px;border-radius:4px;margin-top:15px;}");
        out.println("a{color:#007bff;text-decoration:none;} a:hover{text-decoration:underline;}");
        out.println("</style>");
        out.println("<script>function copyText(id){var e=document.getElementById(id);var t=e.textContent||e.innerText;navigator.clipboard.writeText(t).then(function(){alert('Copied');}).catch(function(err){alert('Failed:'+err);});}</script>");
        out.println("</head><body>");
        out.println("<p><a href=\"/step/iceSandbox\">← Back to ICE Sandbox</a> &nbsp; <a href=\"/step/home\">Home</a></p>");
        out.println("<h1>ICE Wire Conformance Report</h1>");
        out.println("<p style=\"color:#555;\">Wire-only checks against proposed R6 <code>POST /cds/$immds-forecast</code> (OD <a href=\"https://terminology.cdsframework.org/ice/OperationDefinition/immds-forecast\">immds-forecast</a> + <a href=\"https://ice-service.cdsframework.org/opencds-decision-support-service/cds/ice-forecast/swagger-ui/index.html\">Swagger</a>). Clinical correctness is out of scope. <code>text.div</code> on resources is per FHIR spec, not ICE.</p>");
        out.println("<div class=\"form-container\"><form method=\"GET\" action=\"iceSandbox\">");
        out.println("<input type=\"hidden\" name=\"view\" value=\"conformance\"/>");
        out.println("<label style=\"font-weight:bold;width:150px;display:inline-block;\">Endpoint Base:</label>");
        out.println("<input type=\"text\" name=\"endpointBase\" value=\"" + htmlEscape(endpointBase) + "\" style=\"width:520px;padding:6px;border:1px solid #ddd;border-radius:4px;\"/>");
        out.println("<button type=\"submit\" style=\"padding:8px 16px;background:#007bff;color:white;border:none;border-radius:4px;margin-left:10px;cursor:pointer;\">Re-run suite (live)</button>");
        out.println("<small style=\"color:#666;display:block;margin-top:6px;\">Defaults to session value or <code>" + htmlEscape(DEFAULT_ENDPOINT_BASE) + "</code>. Suite takes ~5-10s (16 wire + 8 supporting-data probes).</small>");
        out.println("</form></div>");

        out.println("<div class=\"meta\"><strong>Running wire suite against:</strong> <code>" + htmlEscape(endpointBase) + "</code> ...</div>");
        out.flush();

        // Run suite (blocking)
        List<org.openimmunizationsoftware.cdsi.servlet.ice.IceWireConformanceResult> results;
        long start = System.currentTimeMillis();
        try {
            org.openimmunizationsoftware.cdsi.servlet.ice.IceWireConformanceRunner runner = new org.openimmunizationsoftware.cdsi.servlet.ice.IceWireConformanceRunner(endpointBase);
            results = runner.runAll();
        } catch (Exception e) {
            out.println("<div style=\"background:#f8d7da;border:1px solid #f5c6cb;padding:15px;color:#721c24;\"><strong>Runner failed:</strong> " + htmlEscape(e.toString()) + "</div>");
            e.printStackTrace();
            out.println("</body></html>");
            return;
        }
        long dur = System.currentTimeMillis() - start;
        int pass=0, fail=0, amber=0;
        for (org.openimmunizationsoftware.cdsi.servlet.ice.IceWireConformanceResult r2 : results) {
            if (r2.verdict==org.openimmunizationsoftware.cdsi.servlet.ice.IceWireConformanceResult.Verdict.PASS) pass++;
            else if (r2.verdict==org.openimmunizationsoftware.cdsi.servlet.ice.IceWireConformanceResult.Verdict.FAIL) fail++;
            else amber++;
        }
        out.println("<p><strong>Summary:</strong> PASS <span class=\"pass\" style=\"padding:2px 6px;border-radius:3px;\">" + pass + "</span> FAIL <span class=\"fail\" style=\"padding:2px 6px;border-radius:3px;\">" + fail + "</span> AMBER <span class=\"amber\" style=\"padding:2px 6px;border-radius:3px;\">" + amber + "</span> &nbsp; total " + results.size() + " &nbsp; duration " + dur + "ms</p>");

        // Findings highlight box
        out.println("<div class=\"meta\" style=\"font-size:13px;\"><strong>Key findings (wire-only):</strong><ul>");
        out.println("<li><code>valueCanonical</code> knowledgeBase: wire-conformant (R6), unlike local <code>/sandbox</code> CodeableConcept.</li>");
        out.println("<li>Business errors → <code>200</code> with <code>operationOutcome(error)</code> (correct per OD); malformed JSON → <code>400</code> (verified).</li>");
        out.println("<li><code>text.div</code> present on all returned resources – FHIR narrative, not ICE.</li>");
        out.println("<li>Supporting-data <code>code-system? name=CONTEXTUAL_CONDITIONS</code> is 404 – explains why IVCI dropdown is limited to 024.</li>");
        out.println("</ul></div>");

        out.println("<h2>Wire Tests (16)</h2>");
        out.println("<table><tr><th>#</th><th>Test</th><th>Spec ref</th><th>Expected</th><th>Actual</th><th>HTTP</th><th>Verdict</th><th>Notes</th></tr>");
        for (org.openimmunizationsoftware.cdsi.servlet.ice.IceWireConformanceResult r2 : results) {
            // limit to first 16 then break for supporting data second table; we render all in one but split
            if (r2.id.startsWith("SD-")) continue;
            out.println("<tr>");
            out.println("<td>" + htmlEscape(r2.id) + "</td>");
            out.println("<td>" + htmlEscape(r2.description) + "</td>");
            out.println("<td style=\"font-size:11px;\">" + htmlEscape(r2.specRef) + "</td>");
            out.println("<td>" + htmlEscape(r2.expected) + "</td>");
            out.println("<td style=\"white-space:pre-wrap;word-break:break-word;\">" + htmlEscape(r2.actualSummary) + "</td>");
            out.println("<td>" + r2.httpStatus + "</td>");
            out.println("<td style=\"" + r2.verdictCss() + "font-weight:bold;text-align:center;\">" + htmlEscape(r2.verdictLabel()) + "</td>");
            out.println("<td style=\"white-space:pre-wrap;word-break:break-word;\">" + htmlEscape(r2.notes) + "</td>");
            out.println("</tr>");
        }
        out.println("</table>");

        // detail expanders
        out.println("<h3>Detail (request/response snippets)</h3>");
        for (org.openimmunizationsoftware.cdsi.servlet.ice.IceWireConformanceResult r2 : results) {
            if (r2.id.startsWith("SD-")) continue;
            out.println("<div style=\"background:white;border:1px solid #ddd;border-radius:5px;padding:10px;margin:10px 0;\">");
            out.println("<strong>" + htmlEscape(r2.id) + " – " + htmlEscape(r2.description) + " <span style=\"" + r2.verdictCss() + "padding:2px 6px;border-radius:3px;\">" + htmlEscape(r2.verdictLabel()) + "</span></strong>");
            out.println("<div style=\"display:flex;gap:10px;\"><div style=\"flex:1;\"><em>Request</em><pre>" + htmlEscape(r2.requestPretty!=null?r2.requestPretty:"") + "</pre></div>");
            out.println("<div style=\"flex:1;\"><em>Response snippet</em><pre>" + htmlEscape(r2.responseSnippet!=null?r2.responseSnippet:"") + "</pre>");
            if (r2.responsePretty!=null && !r2.responsePretty.isEmpty() && !r2.responsePretty.equals(r2.responseSnippet)) {
                String rid = "resp-" + r2.id.replaceAll("[^A-Za-z0-9]","-");
                out.println("<button onclick=\"copyText('" + rid + "')\" style=\"font-size:11px;padding:2px 6px;\">Copy full response</button><pre id=\"" + rid + "\" style=\"display:none;\">" + htmlEscape(r2.responsePretty) + "</pre>");
            }
            out.println("</div></div></div>");
        }

        out.println("<h2>Supporting-Data Probes (8)</h2>");
        out.println("<table><tr><th>#</th><th>Description</th><th>HTTP</th><th>Verdict</th><th>Actual</th><th>Notes</th></tr>");
        for (org.openimmunizationsoftware.cdsi.servlet.ice.IceWireConformanceResult r2 : results) {
            if (!r2.id.startsWith("SD-")) continue;
            out.println("<tr><td>" + htmlEscape(r2.id) + "</td><td>" + htmlEscape(r2.description) + "</td><td>" + r2.httpStatus + "</td><td style=\"" + r2.verdictCss() + "text-align:center;font-weight:bold;\">" + htmlEscape(r2.verdictLabel()) + "</td><td>" + htmlEscape(r2.actualSummary) + "</td><td>" + htmlEscape(r2.notes) + "</td></tr>");
        }
        out.println("</table>");

        out.println("<p style=\"color:#666;font-size:12px;\">Generated " + new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss z").format(new java.util.Date()) + " against " + htmlEscape(endpointBase) + ". Wire-only, clinical correctness out of scope.</p>");
        FooterRenderer.render(out, getServletContext());
        out.println("</body></html>");
    }

    private String htmlEscape(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&#39;");
    }
    private String escapeHtml(String text) { return htmlEscape(text); }

    private LocalDate parseFlexibleDate(String s) {
        if (s == null) throw new IllegalArgumentException("Date cannot be null");
        s = s.trim();
        if (s.isEmpty()) throw new IllegalArgumentException("Date cannot be blank");
        if (s.matches("\\d{8}")) {
            try { return LocalDate.parse(s, DateTimeFormatter.ofPattern("yyyyMMdd")); } catch (DateTimeParseException e) { throw new IllegalArgumentException("Invalid YYYYMMDD format: " + s, e); }
        }
        if (s.matches("\\d{1,2}/\\d{1,2}/\\d{4}")) {
            try { return LocalDate.parse(s, DateTimeFormatter.ofPattern("M/d/yyyy")); } catch (DateTimeParseException e) { throw new IllegalArgumentException("Invalid MM/DD/YYYY format: " + s, e); }
        }
        throw new IllegalArgumentException("Date must be in YYYYMMDD or MM/DD/YYYY format, got: " + s);
    }
}

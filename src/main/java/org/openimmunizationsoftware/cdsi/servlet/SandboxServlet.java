package org.openimmunizationsoftware.cdsi.servlet;

import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.DateTimeType;
import org.hl7.fhir.r4.model.DateType;
import org.hl7.fhir.r4.model.Enumerations.AdministrativeGender;
import org.hl7.fhir.r4.model.Immunization;
import org.hl7.fhir.r4.model.ImmunizationRecommendation;
import org.hl7.fhir.r4.model.ImmunizationRecommendation.ImmunizationRecommendationRecommendationComponent;
import org.hl7.fhir.r4.model.ImmunizationRecommendation.ImmunizationRecommendationRecommendationDateCriterionComponent;
import org.hl7.fhir.r4.model.Observation;
import org.hl7.fhir.r4.model.Parameters;
import org.hl7.fhir.r4.model.Parameters.ParametersParameterComponent;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Resource;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.client.exceptions.FhirClientConnectionException;

/**
 * Example link data structure for sidebar quick links.
 * Each link has a name and a URL starting with ? (to be prepended with
 * /step/sandbox).
 */
class ExampleLink {
    public String name;
    public String url; // Starts with ?

    public ExampleLink(String name, String url) {
        this.name = name;
        this.url = url;
    }
}

/**
 * Example category for organizing quick links by vaccine type.
 * Contains a title and a list of example links.
 */
class ExampleCategory {
    public String title;
    public List<ExampleLink> links;

    public ExampleCategory(String title) {
        this.title = title;
        this.links = new ArrayList<>();
    }

    public void addLink(String name, String url) {
        this.links.add(new ExampleLink(name, url));
    }
}

/**
 * Sandbox servlet examples: centralized location for managing example URLs.
 * Add new vaccine types and example links here to have them appear in the
 * sidebar.
 * URLs should start with ? and will be prepended with /step/sandbox.
 * 
 * Usage: Simply add lines like this in the getInstance() method below:
 * add("DTaP", "2024-0016 Patient receives an optional 5th dose",
 * "?evalDate=20260216&resultFormat=text&patientDob=20200716...");
 */
class SandboxServletExamples {
    private static List<ExampleCategory> examples = null;

    /**
     * Lazy-initialize and return the list of example categories.
     */
    public static List<ExampleCategory> getExamples() {
        if (examples == null) {
            examples = new ArrayList<>();
            initializeExamples();
        }
        return examples;
    }

    /**
     * Add an example link to a category (creates category if it doesn't exist).
     * This is the simple method to use for adding test cases.
     * 
     * @param categoryName The vaccine group name (e.g., "DTaP", "MMR")
     * @param exampleName  The test case ID and description (e.g., "2024-0016
     *                     Patient receives an optional 5th dose")
     * @param url          The sandbox URL starting with ? (e.g.,
     *                     "?evalDate=20260216&patientDob=...")
     */
    private static void add(String categoryName, String exampleName, String url) {
        // Find existing category or create new one
        ExampleCategory category = null;
        for (ExampleCategory cat : examples) {
            if (cat.title.equals(categoryName)) {
                category = cat;
                break;
            }
        }
        if (category == null) {
            category = new ExampleCategory(categoryName);
            examples.add(category);
        }
        category.addLink(exampleName, url);
    }

    /**
     * Initialize all example categories and links.
     * Simply add new entries here using add(categoryName, exampleName, url).
     */
    private static void initializeExamples() {
        // DTaP examples
        add("DTaP", "2024-0016 Patient receives an optional 5th dose",
                "?evalDate=20260216&resultFormat=text&patientDob=20200716&patientSex=F&vaccineDate1=20200916&vaccineCvx1=20&vaccineDate2=20210116&vaccineCvx2=20&vaccineDate3=20210819&vaccineCvx3=20&vaccineDate4=20240724&vaccineCvx4=20&vaccineDate5=20260216&vaccineCvx5=20");
        add("DTaP", "2013-0038 Invalid age for dose #3 in midst of others",
                "?evalDate=20260216&resultFormat=text&patientDob=20250215&patientSex=F&vaccineDate1=20250329&vaccineCvx1=107&vaccineDate2=20250423&vaccineCvx2=107&vaccineDate3=20250518&vaccineCvx3=107&vaccineDate4=20260216&vaccineCvx4=107");
        add("DTaP", "2020-0002 Decennial dose as Tdap",
                "?evalDate=20260216&resultFormat=text&patientDob=20040216&patientSex=F&vaccineDate1=20040416&vaccineCvx1=107&vaccineDate2=20040616&vaccineCvx2=107&vaccineDate3=20040816&vaccineCvx3=107&vaccineDate4=20050516&vaccineCvx4=107&vaccineDate5=20080216&vaccineCvx5=107&vaccineDate6=20160216&vaccineCvx6=115&vaccineDate7=20260216&vaccineCvx7=115");

        add("MMR", "2013-0565 Correctly administered single antigen M, M and R followed by MMR=series complete",
                "?evalDate=20260216&resultFormat=text&patientDob=20230207&patientSex=F&vaccineDate1=20240413&vaccineCvx1=07&vaccineMvx1=&vaccineDate2=20240613&vaccineCvx2=06&vaccineMvx2=&vaccineDate3=20250213&vaccineCvx3=05&vaccineMvx3=&vaccineDate4=20260213&vaccineCvx4=03&vaccineMvx4=");
        add("MMR", "2013-0572 Dose 2 at age 13 mo ",
                "?evalDate=20260216&resultFormat=text&patientDob=20250116&patientSex=F&vaccineDate1=20260116&vaccineCvx1=03&vaccineMvx1=&vaccineDate2=20260216&vaccineCvx2=03&vaccineMvx2=");

        add("HepB", "2013-0241 # 3 Pediarix at 24 weeks-4 days. Valid for dose 3",
                "?evalDate=20260216&resultFormat=text&patientDob=20250905&patientSex=F&vaccineDate1=20251017&vaccineCvx1=08&vaccineMvx1=&vaccineDate2=20251217&vaccineCvx2=08&vaccineMvx2=&vaccineDate3=20260216&vaccineCvx3=110&vaccineMvx3=");
        add("HepB", "2013-0202 Dose 2 to dose 3 interval 8 wks-5 days. 4th dose needed.",
                "?evalDate=20260216&resultFormat=text&patientDob=20250626&patientSex=F&vaccineDate1=20250926&vaccineCvx1=08&vaccineMvx1=&vaccineDate2=20251226&vaccineCvx2=08&vaccineMvx2=&vaccineDate3=20260215&vaccineCvx3=08&vaccineMvx3=");
        add("HepB", "2013-0209 Dose # 1 at age 0 days. ",
                "?evalDate=20260216&resultFormat=text&patientDob=20260216&patientSex=F&vaccineDate1=20260216&vaccineCvx1=45&vaccineMvx1=");
        add("HepB", "2013-0257 dose 1 to 2 Pediarix, interval 28-4 days",
                "?evalDate=20260216&resultFormat=text&patientDob=20251123&patientSex=F&vaccineDate1=20260123&vaccineCvx1=110&vaccineMvx1=&vaccineDate2=20260216&vaccineCvx2=110&vaccineMvx2=");
    }

    /**
     * Generate a code snippet that can be copied and pasted into
     * initializeExamples().
     * Used for creating Copy buttons that generate code to add test cases.
     * 
     * @param categoryName The vaccine group name (e.g., "DTaP")
     * @param exampleName  The test case ID and description (e.g., "2024-0016
     *                     Patient receives...")
     * @param url          The sandbox URL starting with ? (e.g.,
     *                     "?evalDate=20260216&...")
     * @return A code snippet ready to paste
     */
    public static String generateCodeSnippet(String categoryName, String exampleName, String url) {
        return "add(\"" + categoryName + "\", \"" + exampleName + "\", \"" + url + "\");";
    }
}

/**
 * FHIR ImmDS-Forecast client test harness.
 * Renders an HTML form to test the $immds-forecast operation.
 */
public class SandboxServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    private static final int NUM_IMMUNIZATION_ROWS = 20;
    private static final String DEFAULT_ENDPOINT_BASE = "https://florence.immregistries.org/step/fhir";
    private static final String SESSION_ENDPOINT_KEY = "sandboxEndpointBase";

    /**
     * Static utility: Parse a flexible date (YYYYMMDD or MM/DD/YYYY) to LocalDate.
     * Returns null if parsing fails.
     */
    static LocalDate parseFlexibleDateStatic(String s) {
        if (s == null || s.trim().isEmpty()) {
            return null;
        }
        s = s.trim();

        // Try YYYYMMDD format (8 digits)
        if (s.matches("\\d{8}")) {
            try {
                return LocalDate.parse(s, DateTimeFormatter.ofPattern("yyyyMMdd"));
            } catch (Exception e) {
                return null;
            }
        }

        // Try MM/DD/YYYY format
        if (s.matches("\\d{1,2}/\\d{1,2}/\\d{4}")) {
            try {
                return LocalDate.parse(s, DateTimeFormatter.ofPattern("M/d/yyyy"));
            } catch (Exception e) {
                return null;
            }
        }

        return null;
    }

    /**
     * Static utility: Format a LocalDate as MM/DD/YYYY for display.
     * Returns empty string if date is null.
     */
    static String formatDateForDisplay(LocalDate d) {
        if (d == null) {
            return "";
        }
        return d.format(DateTimeFormatter.ofPattern("MM/dd/yyyy"));
    }

    /**
     * Static utility: Normalize a date string to MM/DD/YYYY format.
     * If the string is in YYYYMMDD or MM/DD/YYYY format, returns MM/DD/YYYY.
     * Otherwise returns the string unchanged.
     */
    static String normalizeDateDisplay(String dateStr) {
        if (dateStr == null || dateStr.isEmpty()) {
            return "";
        }
        LocalDate parsed = parseFlexibleDateStatic(dateStr);
        if (parsed != null) {
            return formatDateForDisplay(parsed);
        }
        return dateStr;
    }

    /**
     * Model object to hold form data and results.
     */
    public static class Model {
        public String endpointBase = "";
        public String assessmentDate = "";
        public String patientDob = "";
        public String patientSex = "";
        public String encoding = "json";
        public List<String> errors = new ArrayList<>();
        public String requestPretty = "";
        public String responsePretty = "";
        public List<RecommendationRow> recommendations = new ArrayList<>();
        public List<String> evaluationLines = new ArrayList<>();
        public List<String> recommendationLines = new ArrayList<>();
        public Map<Integer, String> vaccineDates = new HashMap<>();
        public Map<Integer, String> vaccineCvxs = new HashMap<>();

        public Model(HttpServletRequest req) {
            // Check endpoint base: request parameter > session > default
            String endpointParam = req.getParameter("endpointBase");
            if (endpointParam != null && !endpointParam.isEmpty()) {
                this.endpointBase = endpointParam;
            } else {
                // Try session, then default
                String sessionEndpoint = (String) req.getSession().getAttribute(SESSION_ENDPOINT_KEY);
                this.endpointBase = sessionEndpoint != null ? sessionEndpoint : DEFAULT_ENDPOINT_BASE;
            }

            // Support both assessmentDate and evalDate parameter names, and format to
            // MM/DD/YYYY
            String assessmentDateParam = req.getParameter("assessmentDate");
            if (assessmentDateParam == null || assessmentDateParam.isEmpty()) {
                assessmentDateParam = req.getParameter("evalDate");
            }
            this.assessmentDate = normalizeDateDisplay(assessmentDateParam);

            // Format patient DOB to MM/DD/YYYY
            String patientDobParam = req.getParameter("patientDob");
            this.patientDob = normalizeDateDisplay(patientDobParam);

            this.patientSex = req.getParameter("patientSex") != null ? req.getParameter("patientSex") : "";
            this.encoding = req.getParameter("encoding") != null ? req.getParameter("encoding") : "json";

            // Load vaccine dates and CVX codes, formatting dates to MM/DD/YYYY
            for (int i = 1; i <= NUM_IMMUNIZATION_ROWS; i++) {
                String dateVal = req.getParameter("vaccineDate" + i);
                String cvxVal = req.getParameter("vaccineCvx" + i);
                if (dateVal != null && !dateVal.isEmpty()) {
                    // Format the date to MM/DD/YYYY
                    String formattedDate = normalizeDateDisplay(dateVal);
                    this.vaccineDates.put(i, formattedDate);
                }
                if (cvxVal != null && !cvxVal.isEmpty()) {
                    this.vaccineCvxs.put(i, cvxVal);
                }
            }
        }
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        try {
            Model model = new Model(req);
            renderPage(req, resp, model);
        } catch (Exception e) {
            e.printStackTrace();
            throw new ServletException(e);
        }
    }

    /**
     * Result object from a forecast operation.
     */
    public static class Result {
        public String requestPretty = "";
        public String responsePretty = "";
        public List<RecommendationRow> recommendationRows = new ArrayList<>();
        public String extractedEvaluations = "";
        public String extractedRecommendations = "";
        public String error = "";

        public boolean hasError() {
            return !error.isEmpty();
        }
    }

    /**
     * Recommendation row: structured data for table display.
     */
    public static class RecommendationRow {
        public String vaccineName;
        public String cvxCode;
        public String forecastStatus;
        public String earliestDate;
        public String recommendedDate;

        public RecommendationRow(String vaccineName, String cvxCode, String forecastStatus,
                String earliestDate, String recommendedDate) {
            this.vaccineName = vaccineName != null ? vaccineName : "";
            this.cvxCode = cvxCode != null ? cvxCode : "";
            this.forecastStatus = forecastStatus != null ? forecastStatus : "";
            this.earliestDate = earliestDate != null ? earliestDate : "";
            this.recommendedDate = recommendedDate != null ? recommendedDate : "";
        }
    }

    /**
     * Immunization record: parsed date and CVX code.
     */
    public static class ImmunizationRecord {
        public LocalDate date;
        public String cvx;

        public ImmunizationRecord(LocalDate date, String cvx) {
            this.date = date;
            this.cvx = cvx;
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        try {
            Model model = new Model(req);
            List<String> errors = new ArrayList<>();

            // Validate required fields
            if (model.endpointBase.isEmpty()) {
                errors.add("Endpoint Base URL is required");
            }

            // Validate assessment date
            LocalDate assessmentDate = null;
            if (model.assessmentDate.isEmpty()) {
                errors.add("Assessment Date is required");
            } else {
                try {
                    assessmentDate = parseFlexibleDate(model.assessmentDate);
                } catch (IllegalArgumentException e) {
                    errors.add("Assessment Date: " + e.getMessage());
                }
            }

            // Validate patient DOB
            LocalDate patientDob = null;
            if (model.patientDob.isEmpty()) {
                errors.add("Patient DOB is required");
            } else {
                try {
                    patientDob = parseFlexibleDate(model.patientDob);
                } catch (IllegalArgumentException e) {
                    errors.add("Patient DOB: " + e.getMessage());
                }
            }

            // Validate patient sex
            if (model.patientSex.isEmpty()) {
                errors.add("Patient Sex is required");
            } else if (!model.patientSex.matches("[MFOU]")) {
                errors.add("Patient Sex must be one of: M, F, O, U");
            }

            // Read and validate immunizations
            List<ImmunizationRecord> immunizations = new ArrayList<>();
            for (int i = 1; i <= NUM_IMMUNIZATION_ROWS; i++) {
                String dateStr = model.vaccineDates.getOrDefault(i, "").trim();
                String cvxStr = model.vaccineCvxs.getOrDefault(i, "").trim();

                // If both blank, skip
                if (dateStr.isEmpty() && cvxStr.isEmpty()) {
                    continue;
                }

                // If one blank, error
                if (dateStr.isEmpty() || cvxStr.isEmpty()) {
                    errors.add("Row " + i + " requires both date and CVX");
                    continue;
                }

                // Parse date
                LocalDate vaccineDate = null;
                try {
                    vaccineDate = parseFlexibleDate(dateStr);
                } catch (IllegalArgumentException e) {
                    errors.add("Row " + i + " date: " + e.getMessage());
                    continue;
                }

                // Validate CVX is numeric
                if (!cvxStr.matches("\\d+")) {
                    errors.add("Row " + i + " CVX must be numeric");
                    continue;
                }

                immunizations.add(new ImmunizationRecord(vaccineDate, cvxStr));
            }

            // If validation errors, render page with errors
            if (!errors.isEmpty()) {
                model.errors = errors;
                renderPage(req, resp, model);
                return;
            }

            // Call runForecast
            Result result = runForecast(model.endpointBase, model.encoding, assessmentDate, patientDob,
                    model.patientSex, immunizations);

            // Store the endpoint URL in session for future use
            req.getSession().setAttribute(SESSION_ENDPOINT_KEY, model.endpointBase);

            // Populate model with results
            model.requestPretty = result.requestPretty;
            model.responsePretty = result.responsePretty;
            model.recommendations = result.recommendationRows;
            if (result.hasError()) {
                model.errors.add(result.error);
            }
            // Note: evaluationLines and recommendationLines will be populated if needed
            // For now, results contain evaluations and recommendations as HTML strings
            // (to be refactored later to use separate lines list)

            renderPage(req, resp, model);
        } catch (Exception e) {
            e.printStackTrace();
            throw new ServletException(e);
        }
    }

    /**
     * Execute the $immds-forecast operation and return results.
     * 
     * @param endpointBase   FHIR endpoint base URL
     * @param encoding       JSON or XML
     * @param assessmentDate Assessment date (LocalDate)
     * @param patientDob     Patient date of birth (LocalDate)
     * @param patientSex     Patient sex (M/F/O/U)
     * @param immunizations  List of immunization records (LocalDate date, String
     *                       cvx)
     * @return Result object with request, response, and extracted data
     */
    private Result runForecast(String endpointBase, String encoding, LocalDate assessmentDate,
            LocalDate patientDob, String patientSex, List<ImmunizationRecord> immunizations) {
        Result result = new Result();

        try {
            // Set up FHIR context with encoding
            FhirContext ctx = getFhirContext(encoding);

            // Create generic client
            IGenericClient client = ctx.newRestfulGenericClient(endpointBase);

            // Build FHIR Parameters request
            Parameters requestParams = new Parameters();

            // Add assessmentDate parameter
            if (assessmentDate != null) {
                requestParams.addParameter("assessmentDate",
                        new DateType(java.sql.Date.valueOf(assessmentDate)));
            }

            // Add patient parameter
            if (patientDob != null && patientSex != null && !patientSex.isEmpty()) {
                Patient patient = new Patient();
                patient.setBirthDate(java.sql.Date.valueOf(patientDob));

                // Map M/F/O/U to AdministrativeGender (lowercase for FHIR)
                String genderCode = mapGenderCode(patientSex);
                if (genderCode != null) {
                    patient.setGender(AdministrativeGender.fromCode(genderCode));
                }

                requestParams.addParameter().setName("patient").setResource(patient);
            }

            // Add immunization parameters
            for (ImmunizationRecord immunRecord : immunizations) {
                Immunization immunization = new Immunization();

                // Set occurrence as date (DateTimeType with date-only)
                DateTimeType occurrenceDateTime = new DateTimeType(
                        java.sql.Date.valueOf(immunRecord.date));
                immunization.setOccurrence(occurrenceDateTime);

                // Set vaccine code using CVX coding system
                CodeableConcept vaccineCodeConcept = new CodeableConcept();
                vaccineCodeConcept.addCoding(
                        new Coding("http://hl7.org/fhir/sid/cvx", immunRecord.cvx, null));
                immunization.setVaccineCode(vaccineCodeConcept);

                requestParams.addParameter().setName("immunization").setResource(immunization);
            }

            // Pretty-print the request
            result.requestPretty = prettyPrintFhir(requestParams, encoding);

            // Invoke system-level operation POST [base]/$immds-forecast
            Parameters responseParams = client.operation()
                    .onServer()
                    .named("$immds-forecast")
                    .withParameters(requestParams)
                    .returnResourceType(Parameters.class)
                    .execute();

            // Pretty-print the response
            result.responsePretty = prettyPrintFhir(responseParams, encoding);

            // Extract evaluations and recommendations from response
            result.extractedEvaluations = extractEvaluations(responseParams);
            result.extractedRecommendations = extractRecommendations(responseParams);
            result.recommendationRows = extractRecommendationRows(responseParams);

        } catch (FhirClientConnectionException e) {
            result.error = "Failed to connect to endpoint: " + e.getMessage();
        } catch (Exception e) {
            result.error = e.getMessage();
        }

        return result;
    }

    private String mapGenderCode(String code) {
        if (code == null || code.isEmpty()) {
            return null;
        }
        switch (code.toUpperCase()) {
            case "M":
                return "male";
            case "F":
                return "female";
            case "O":
                return "other";
            case "U":
                return "unknown";
            default:
                return null;
        }
    }

    private FhirContext getFhirContext(String encoding) {
        if ("xml".equalsIgnoreCase(encoding)) {
            return FhirContext.forR4();
        } else {
            return FhirContext.forR4();
        }
    }

    private String prettyPrintFhir(Resource resource, String encoding) {
        FhirContext ctx = getFhirContext(encoding);
        if ("xml".equalsIgnoreCase(encoding)) {
            return ctx.newXmlParser().setPrettyPrint(true).encodeResourceToString(resource);
        } else {
            return ctx.newJsonParser().setPrettyPrint(true).encodeResourceToString(resource);
        }
    }

    private String extractEvaluations(Parameters params) {
        StringBuilder sb = new StringBuilder();
        List<Resource> evaluationResources = new ArrayList<>();

        // Search for "evaluation" or "evaluations" parameters
        for (ParametersParameterComponent param : params.getParameter()) {
            String paramName = param.getName();
            if ("evaluation".equals(paramName) || "evaluations".equals(paramName)) {
                if (param.getResource() != null) {
                    evaluationResources.add(param.getResource());
                }
            }
        }

        // Also search all parameters for Immunization or Observation resources
        for (ParametersParameterComponent param : params.getParameter()) {
            Resource resource = param.getResource();
            if (resource instanceof Immunization || resource instanceof Observation) {
                evaluationResources.add(resource);
            }
        }

        sb.append("<h3>Evaluations</h3>\n");

        if (evaluationResources.isEmpty()) {
            sb.append("<p><em>No evaluations found.</em></p>\n");
        } else {
            sb.append("<p><strong>Count:</strong> ").append(evaluationResources.size()).append("</p>\n");
            sb.append("<table border='1' cellpadding='8' style='width:100%; border-collapse:collapse;'>\n");
            sb.append(
                    "  <tr><th style='background-color:#f0f0f0;'>Resource Type</th><th style='background-color:#f0f0f0;'>ID</th><th style='background-color:#f0f0f0;'>Codes</th><th style='background-color:#f0f0f0;'>Status</th><th style='background-color:#f0f0f0;'>Date</th></tr>\n");

            for (Resource resource : evaluationResources) {
                String resourceType = resource.getResourceType().name();
                String id = resource.getIdElement().getIdPart();
                String codes = "";
                String status = "";
                String date = "";

                if (resource instanceof Immunization) {
                    Immunization imm = (Immunization) resource;
                    if (imm.getVaccineCode() != null && imm.getVaccineCode().getCoding() != null) {
                        StringBuilder codeSb = new StringBuilder();
                        for (Coding coding : imm.getVaccineCode().getCoding()) {
                            if (codeSb.length() > 0)
                                codeSb.append(", ");
                            if ("http://hl7.org/fhir/sid/cvx".equals(coding.getSystem())) {
                                codeSb.append("CVX:").append(coding.getCode());
                            } else {
                                codeSb.append(coding.getCode());
                            }
                        }
                        codes = codeSb.toString();
                    }
                    if (imm.getStatus() != null) {
                        status = imm.getStatus().toString();
                    }
                    // Occurrence can be DateTimeType, StringType, or Period - just get string
                    // representation
                    if (imm.getOccurrence() != null) {
                        date = imm.getOccurrence().toString();
                    }
                } else if (resource instanceof Observation) {
                    Observation obs = (Observation) resource;
                    if (obs.getCode() != null && obs.getCode().getCoding() != null) {
                        StringBuilder codeSb = new StringBuilder();
                        for (Coding coding : obs.getCode().getCoding()) {
                            if (codeSb.length() > 0)
                                codeSb.append(", ");
                            codeSb.append(coding.getCode());
                        }
                        codes = codeSb.toString();
                    }
                    if (obs.getStatus() != null) {
                        status = obs.getStatus().toString();
                    }
                    // effective can be DateTimeType or Period - just get string representation
                    if (obs.getEffective() != null) {
                        date = obs.getEffective().toString();
                    }
                }

                sb.append("  <tr><td>").append(escapeHtml(resourceType))
                        .append("</td><td>").append(escapeHtml(id))
                        .append("</td><td>").append(escapeHtml(codes))
                        .append("</td><td>").append(escapeHtml(status))
                        .append("</td><td>").append(escapeHtml(date))
                        .append("</td></tr>\n");
            }

            sb.append("</table>\n");
        }

        return sb.toString();
    }

    private String extractRecommendations(Parameters params) {
        StringBuilder sb = new StringBuilder();
        List<ImmunizationRecommendation> recommendations = new ArrayList<>();

        // Search for "recommendation" or "recommendations" parameters and
        // ImmunizationRecommendation resources
        for (ParametersParameterComponent param : params.getParameter()) {
            String paramName = param.getName();
            if ("recommendation".equals(paramName) || "recommendations".equals(paramName)) {
                if (param.getResource() instanceof ImmunizationRecommendation) {
                    recommendations.add((ImmunizationRecommendation) param.getResource());
                }
            }
            // Also search all parameters for ImmunizationRecommendation
            if (param.getResource() instanceof ImmunizationRecommendation) {
                recommendations.add((ImmunizationRecommendation) param.getResource());
            }
        }

        sb.append("<h3>Recommendations</h3>\n");

        if (recommendations.isEmpty()) {
            sb.append("<p><em>No recommendations found.</em></p>\n");
        } else {
            sb.append("<p><strong>Count:</strong> ").append(recommendations.size()).append("</p>\n");

            for (ImmunizationRecommendation immRec : recommendations) {
                if (immRec.getRecommendation() == null || immRec.getRecommendation().isEmpty()) {
                    continue;
                }

                for (ImmunizationRecommendationRecommendationComponent rec : immRec.getRecommendation()) {
                    sb.append(
                            "<table border='1' cellpadding='8' style='width:100%; border-collapse:collapse; margin-bottom:15px;'>\n");

                    // Vaccine Codes (CVX)
                    sb.append("  <tr><th style='background-color:#f0f0f0;'>Vaccine Code(s)</th><td>");
                    if (rec.getVaccineCode() != null && !rec.getVaccineCode().isEmpty()) {
                        boolean first = true;
                        for (CodeableConcept vaccineCode : rec.getVaccineCode()) {
                            if (!first)
                                sb.append(", ");
                            if (vaccineCode.getCoding() != null) {
                                boolean firstCoding = true;
                                for (Coding coding : vaccineCode.getCoding()) {
                                    if (!firstCoding)
                                        sb.append(" / ");
                                    if ("http://hl7.org/fhir/sid/cvx".equals(coding.getSystem())) {
                                        sb.append("CVX:").append(escapeHtml(coding.getCode()));
                                    } else {
                                        sb.append(escapeHtml(coding.getCode()));
                                    }
                                    firstCoding = false;
                                }
                            }
                            first = false;
                        }
                    } else {
                        sb.append("N/A");
                    }
                    sb.append("</td></tr>\n");

                    // Forecast Status
                    sb.append("  <tr><th style='background-color:#f0f0f0;'>Forecast Status</th><td>");
                    if (rec.getForecastStatus() != null) {
                        sb.append(escapeHtml(rec.getForecastStatus().toString()));
                    } else {
                        sb.append("N/A");
                    }
                    sb.append("</td></tr>\n");

                    // Date Criteria
                    if (rec.getDateCriterion() != null && !rec.getDateCriterion().isEmpty()) {
                        sb.append(
                                "  <tr><th style='background-color:#f0f0f0;' valign='top'>Date Criteria</th><td><ul style='margin:0; padding-left:20px;'>\n");
                        for (ImmunizationRecommendationRecommendationDateCriterionComponent dateCrit : rec
                                .getDateCriterion()) {
                            sb.append("    <li>");
                            if (dateCrit.getCode() != null) {
                                sb.append(escapeHtml(dateCrit.getCode().toString()));
                            }
                            sb.append(": ");
                            if (dateCrit.getValue() != null) {
                                sb.append(escapeHtml(dateCrit.getValue().toString()));
                            }
                            sb.append("</li>\n");
                        }
                        sb.append("  </ul></td></tr>\n");
                    }

                    // Series
                    String series = rec.getSeries();
                    if (series != null && !series.isEmpty()) {
                        sb.append("  <tr><th style='background-color:#f0f0f0;'>Series</th><td>");
                        sb.append(escapeHtml(series));
                        sb.append("</td></tr>\n");
                    }

                    sb.append("</table>\n");
                }
            }
        }

        return sb.toString();
    }

    /**
     * Extract recommendation rows from FHIR Parameters response for table display.
     * Returns a list of RecommendationRow objects with vaccine name/CVX, forecast
     * status, and dates.
     */
    private List<RecommendationRow> extractRecommendationRows(Parameters params) {
        List<RecommendationRow> rows = new ArrayList<>();
        List<ImmunizationRecommendation> recommendations = new ArrayList<>();

        // Search for ImmunizationRecommendation resources
        for (ParametersParameterComponent param : params.getParameter()) {
            if (param.getResource() instanceof ImmunizationRecommendation) {
                recommendations.add((ImmunizationRecommendation) param.getResource());
            }
        }

        // Extract rows from recommendations
        for (ImmunizationRecommendation immRec : recommendations) {
            if (immRec.getRecommendation() == null || immRec.getRecommendation().isEmpty()) {
                continue;
            }

            for (ImmunizationRecommendationRecommendationComponent rec : immRec.getRecommendation()) {
                // Extract vaccine name and CVX code
                String vaccineName = "";
                String cvxCode = "";
                if (rec.getVaccineCode() != null && !rec.getVaccineCode().isEmpty()) {
                    for (CodeableConcept vaccineCode : rec.getVaccineCode()) {
                        if (vaccineCode.getCoding() != null) {
                            for (Coding coding : vaccineCode.getCoding()) {
                                if ("http://hl7.org/fhir/sid/cvx".equals(coding.getSystem())) {
                                    if (!cvxCode.isEmpty())
                                        cvxCode += ", ";
                                    cvxCode += coding.getCode();
                                    if (coding.getDisplay() != null && !coding.getDisplay().isEmpty()) {
                                        vaccineName = coding.getDisplay();
                                    }
                                }
                            }
                        }
                    }
                }

                // Extract forecast status - get display text from CodeableConcept
                String forecastStatus = "";
                if (rec.getForecastStatus() != null) {
                    // Try to get display text from the first coding
                    if (rec.getForecastStatus().getCoding() != null && !rec.getForecastStatus().getCoding().isEmpty()) {
                        Coding statusCoding = rec.getForecastStatus().getCoding().get(0);
                        if (statusCoding.getDisplay() != null && !statusCoding.getDisplay().isEmpty()) {
                            forecastStatus = statusCoding.getDisplay();
                        } else if (statusCoding.getCode() != null && !statusCoding.getCode().isEmpty()) {
                            forecastStatus = statusCoding.getCode();
                        }
                    }
                }

                // Extract date criteria (earliest and recommended dates)
                String earliestDate = "";
                String recommendedDate = "";
                if (rec.getDateCriterion() != null && !rec.getDateCriterion().isEmpty()) {
                    for (ImmunizationRecommendationRecommendationDateCriterionComponent dateCrit : rec
                            .getDateCriterion()) {
                        if (dateCrit.getCode() != null && dateCrit.getValue() != null) {
                            // Get the code display text
                            String codeDisplay = "";
                            if (dateCrit.getCode().getCoding() != null && !dateCrit.getCode().getCoding().isEmpty()) {
                                Coding codeCoding = dateCrit.getCode().getCoding().get(0);
                                codeDisplay = codeCoding.getDisplay() != null ? codeCoding.getDisplay()
                                        : codeCoding.getCode();
                            }

                            // Extract and format date value properly
                            String dateValue = "";
                            try {
                                // Get the DateTimeType string representation (gives ISO format)
                                String dateTimeString = dateCrit.getValue().toString();
                                // Parse and format to MM/DD/YYYY
                                dateValue = extractDateValue(dateTimeString);
                            } catch (Exception e) {
                                // If all else fails, leave blank
                                dateValue = "";
                            }

                            // Match by display text or code
                            if (codeDisplay != null && codeDisplay.contains("Earliest")) {
                                earliestDate = dateValue;
                            } else if (codeDisplay != null && codeDisplay.contains("Recommend")) {
                                recommendedDate = dateValue;
                            }
                        }
                    }
                }

                rows.add(new RecommendationRow(vaccineName, cvxCode, forecastStatus, earliestDate, recommendedDate));
            }
        }

        return rows;
    }

    /**
     * Extract and format a date from various string representations.
     * Handles formats like:
     * - "2039-02-07T00:00:00-07:00" (ISO 8601 with time)
     * - "2039-02-07" (ISO 8601 date only)
     * - "Mon Feb 07 00:00:00 MST 2039" (Java Date toString)
     * Returns formatted as MM/DD/YYYY.
     */
    private String extractDateValue(String dateTimeString) {
        if (dateTimeString == null || dateTimeString.isEmpty()) {
            return "";
        }

        dateTimeString = dateTimeString.trim();

        // Try to handle Java Date toString format first: "Mon Feb 07 00:00:00 MST 2039"
        if (dateTimeString.matches("^[A-Za-z]{3}\\s+[A-Za-z]{3}\\s+.*")) {
            try {
                SimpleDateFormat javeDateFormat = new SimpleDateFormat("EEE MMM dd HH:mm:ss zzz yyyy");
                java.util.Date date = javeDateFormat.parse(dateTimeString);
                SimpleDateFormat outputFormat = new SimpleDateFormat("MM/dd/yyyy");
                return outputFormat.format(date);
            } catch (Exception e) {
                // Fall through to other formats
            }
        }

        // Try ISO 8601 with time: "2039-02-07T00:00:00-07:00"
        if (dateTimeString.contains("T")) {
            String datePart = dateTimeString.substring(0, dateTimeString.indexOf("T"));
            try {
                SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd");
                SimpleDateFormat outputFormat = new SimpleDateFormat("MM/dd/yyyy");
                java.util.Date date = inputFormat.parse(datePart);
                return outputFormat.format(date);
            } catch (Exception e) {
                // Fall through to next format
            }
        }

        // Try ISO 8601 date only: "2039-02-07"
        if (dateTimeString.matches("\\d{4}-\\d{2}-\\d{2}")) {
            try {
                SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd");
                SimpleDateFormat outputFormat = new SimpleDateFormat("MM/dd/yyyy");
                java.util.Date date = inputFormat.parse(dateTimeString);
                return outputFormat.format(date);
            } catch (Exception e) {
                // Fall through
            }
        }

        // If nothing worked, return empty
        return "";
    }

    private void renderPage(HttpServletRequest req, HttpServletResponse resp, Model model)
            throws IOException {
        resp.setContentType("text/html; charset=UTF-8");
        PrintWriter out = resp.getWriter();

        out.println("<!DOCTYPE html>");
        out.println("<html>");
        out.println("<head>");
        out.println("  <meta charset=\"UTF-8\">");
        out.println("  <title>FHIR ImmDS-Forecast $immds-forecast Test Harness</title>");
        out.println("  <style>");
        out.println("    body { font-family: Arial, sans-serif; margin: 20px; background-color: #f5f5f5; }");
        out.println("    h1 { color: #333; }");
        out.println(
                "    h2 { color: #555; margin-top: 30px; border-bottom: 2px solid #007bff; padding-bottom: 10px; }");
        out.println("    h3 { color: #666; }");
        out.println("    h4 { color: #888; margin-top: 15px; }");
        out.println(
                "    .form-container { background-color: white; padding: 20px; border-radius: 5px; box-shadow: 0 2px 5px rgba(0,0,0,0.1); }");
        out.println("    .form-group { margin-bottom: 15px; }");
        out.println("    label { display: inline-block; width: 150px; font-weight: bold; }");
        out.println(
                "    input[type='text'], select { padding: 8px; border: 1px solid #ddd; border-radius: 4px; font-size: 14px; }");
        out.println("    input[type='text'] { width: 350px; }");
        out.println("    .encoding-group { margin-bottom: 15px; }");
        out.println("    .radio-label { display: inline-block; margin-right: 20px; }");
        out.println("    .immunizations-table { margin-top: 15px; }");
        out.println("    table { border-collapse: collapse; width: 100%; margin: 10px 0; }");
        out.println("    th, td { border: 1px solid #ddd; padding: 8px; text-align: left; }");
        out.println("    th { background-color: #f2f2f2; font-weight: bold; }");
        out.println("    tr:nth-child(even) { background-color: #f9f9f9; }");
        out.println("    .buttons { margin: 20px 0; }");
        out.println(
                "    button { padding: 10px 20px; margin-right: 10px; font-size: 14px; cursor: pointer; background-color: #007bff; color: white; border: none; border-radius: 4px; }");
        out.println("    button:hover { background-color: #0056b3; }");
        out.println(
                "    .error-container { background-color: #f8d7da; border: 1px solid #f5c6cb; color: #721c24; padding: 15px; border-radius: 5px; margin-top: 20px; }");
        out.println(
                "    .output-container { background-color: white; border: 1px solid #ddd; border-radius: 5px; padding: 15px; margin-top: 20px; max-height: 500px; overflow: auto; }");
        out.println(
                "    pre { background-color: #f4f4f4; border: 1px solid #ddd; padding: 10px; border-radius: 4px; overflow-x: auto; font-family: 'Courier New', monospace; font-size: 12px; line-height: 1.4; }");
        out.println(
                "    .copy-button { padding: 5px 10px; font-size: 12px; margin-bottom: 10px; background-color: #28a745; }");
        out.println("    .copy-button:hover { background-color: #218838; }");
        out.println(
                "    .sidebar { float: right; width: 28%; margin-left: 20px; background-color: #e8f4f8; border: 2px solid #007bff; border-radius: 5px; padding: 15px; box-shadow: 0 2px 5px rgba(0,0,0,0.1); }");
        out.println("    .sidebar h3 { margin-top: 0; color: #0056b3; }");
        out.println("    .sidebar p { font-size: 14px; line-height: 1.6; color: #333; }");
        out.println("    .sidebar a { color: #007bff; text-decoration: none; font-weight: bold; }");
        out.println("    .sidebar a:hover { text-decoration: underline; }");
        out.println("    .main-content { overflow: auto; }");
        out.println("  </style>");
        out.println("  <script>");
        out.println("    function copyText(elementId) {");
        out.println("      var elem = document.getElementById(elementId);");
        out.println("      var text = elem.textContent || elem.innerText;");
        out.println("      navigator.clipboard.writeText(text).then(function() {");
        out.println("        alert('Copied to clipboard');");
        out.println("      }).catch(function(err) {");
        out.println("        alert('Failed to copy: ' + err);");
        out.println("      });");
        out.println("    }");
        out.println("  </script>");
        out.println("</head>");
        out.println("<body>");
        // Sidebar
        out.println("  <div class=\"sidebar\">");
        out.println("    <h3>About ImmDS</h3>");
        out.println(
                "    <p>Clinical Decision Support for Immunizations is increasingly being used in health information systems to indicate which vaccinations a patient is due for next. This FHIR implementation guide creates a standardized interface between initiating systems (EHRs, IIS, HIEs) and CDS engines.</p>");
        out.println("    <p><strong>Benefits:</strong></p>");
        out.println("    <ul style=\"font-size: 13px; margin: 10px 0;\">");
        out.println("      <li>Health systems can choose their CDS engine</li>");
        out.println("      <li>Consistent output testing and verification</li>");
        out.println("      <li>Standardized web service interface</li>");
        out.println("    </ul>");
        out.println(
                "    <p><a href=\"https://hl7.org/fhir/us/immds/index.html\" target=\"_blank\">📖 View FHIR Implementation Guide</a></p>");

        // Add example links
        out.println("    <hr style=\"margin: 15px 0; border: none; border-top: 1px solid #0056b3;\">");
        out.println("    <h4 style=\"margin-top: 15px; margin-bottom: 8px; color: #0056b3;\">Quick Examples</h4>");

        List<ExampleCategory> examples = SandboxServletExamples.getExamples();
        for (ExampleCategory category : examples) {
            out.println("    <div style=\"margin-bottom: 12px;\">");
            out.println("      <strong style=\"font-size: 12px; color: #0056b3;\">" + htmlEscape(category.title)
                    + "</strong>");
            out.println("      <ul style=\"font-size: 12px; margin: 5px 0; padding-left: 20px;\">");
            for (ExampleLink link : category.links) {
                out.println("        <li><a href=\"/step/sandbox" + htmlEscape(link.url)
                        + "\" style=\"color: #007bff; text-decoration: none;\">" +
                        htmlEscape(link.name) + "</a></li>");
            }
            out.println("      </ul>");
            out.println("    </div>");
        }

        out.println("  </div>");
        out.println("  <div class=\"main-content\">");
        out.println("  <h1>FHIR ImmDS-Forecast $immds-forecast Test Harness</h1>");
        out.println("  <div class=\"form-container\">");
        out.println("    <form method=\"POST\" action=\"sandbox\">");

        // Endpoint Base URL
        out.println("    <h2>ImmDS-Forecast Configuration</h2>");
        out.println("    <div class=\"form-group\">");
        out.println("      <label for=\"endpointBase\">Endpoint Base URL:</label>");
        out.println("      <input type=\"text\" id=\"endpointBase\" name=\"endpointBase\" value=\""
                + escapeHtml(model.endpointBase)
                + "\" placeholder=\"http://localhost:8080/fhir\">");
        out.println("    </div>");

        // Encoding radios
        out.println("    <div class=\"encoding-group\">");
        out.println("      <label>Encoding:</label>");
        out.println("      <label class=\"radio-label\"><input type=\"radio\" name=\"encoding\" value=\"json\""
                + ("json".equals(model.encoding) ? " checked" : "") + "> JSON</label>");
        out.println("      <label class=\"radio-label\"><input type=\"radio\" name=\"encoding\" value=\"xml\""
                + ("xml".equals(model.encoding) ? " checked" : "") + "> XML</label>");
        out.println("    </div>");

        // Assessment Date
        out.println("    <div class=\"form-group\">");
        out.println("      <label for=\"assessmentDate\">Assessment Date:</label>");
        out.println("      <input type=\"text\" id=\"assessmentDate\" name=\"assessmentDate\" value=\""
                + escapeHtml(model.assessmentDate)
                + "\" placeholder=\"MM/DD/YYYY\">");
        out.println("    </div>");

        // Patient DOB
        out.println("    <div class=\"form-group\">");
        out.println("      <label for=\"patientDob\">Patient DOB:</label>");
        out.println("      <input type=\"text\" id=\"patientDob\" name=\"patientDob\" value=\""
                + escapeHtml(model.patientDob)
                + "\" placeholder=\"MM/DD/YYYY\">");
        out.println("    </div>");

        // Patient Sex
        out.println("    <div class=\"form-group\">");
        out.println("      <label for=\"patientSex\">Patient Sex:</label>");
        out.println("      <select id=\"patientSex\" name=\"patientSex\">");
        out.println("        <option value=\"\"" + ("".equals(model.patientSex) ? " selected" : "")
                + ">-- Select --</option>");
        out.println("        <option value=\"M\"" + ("M".equals(model.patientSex) ? " selected" : "") + ">M</option>");
        out.println("        <option value=\"F\"" + ("F".equals(model.patientSex) ? " selected" : "") + ">F</option>");
        out.println("        <option value=\"O\"" + ("O".equals(model.patientSex) ? " selected" : "") + ">O</option>");
        out.println("        <option value=\"U\"" + ("U".equals(model.patientSex) ? " selected" : "") + ">U</option>");
        out.println("      </select>");
        out.println("    </div>");

        // Immunization table
        out.println("    <h2>Immunizations</h2>");
        out.println("    <div class=\"immunizations-table\">");
        out.println("      <table>");
        out.println("        <tr>");
        out.println("          <th>#</th>");
        out.println("          <th>Vaccine Date (MM/DD/YYYY)</th>");
        out.println("          <th>CVX Code</th>");
        out.println("        </tr>");

        for (int i = 1; i <= NUM_IMMUNIZATION_ROWS; i++) {
            String dateVal = model.vaccineDates.getOrDefault(i, "");
            String cvxVal = model.vaccineCvxs.getOrDefault(i, "");

            out.println("        <tr>");
            out.println("          <td>" + i + "</td>");
            out.println("          <td><input type=\"text\" name=\"vaccineDate" + i + "\" value=\""
                    + escapeHtml(dateVal) + "\" placeholder=\"MM/DD/YYYY\" style=\"width: 120px;\"></td>");
            out.println("          <td><input type=\"text\" name=\"vaccineCvx" + i + "\" value=\""
                    + escapeHtml(cvxVal) + "\" placeholder=\"CVX code\" style=\"width: 100px;\"></td>");
            out.println("        </tr>");
        }

        out.println("      </table>");
        out.println("    </div>");

        // Submit button
        out.println("    <div class=\"buttons\">");
        out.println("      <button type=\"submit\">Run</button>");
        out.println("    </div>");

        out.println("    </form>");
        out.println("  </div>");

        // Errors section
        if (!model.errors.isEmpty()) {
            out.println("  <div class=\"error-container\">");
            out.println("    <h3>Errors</h3>");
            out.println("    <ul>");
            for (String error : model.errors) {
                out.println("      <li>" + htmlEscape(error) + "</li>");
            }
            out.println("    </ul>");
            out.println("  </div>");
        }

        // Results sections
        if (!model.requestPretty.isEmpty()) {
            out.println("  <h2>Results</h2>");

            // Recommendations table
            if (!model.recommendations.isEmpty()) {
                out.println("  <div class=\"output-container\">");
                out.println("    <h3>Recommendations</h3>");
                out.println("    <table style=\"width:100%; border-collapse:collapse;\">");
                out.println("      <tr style=\"background-color:#f2f2f2;\">");
                out.println(
                        "        <th style=\"border:1px solid #ddd; padding:10px; text-align:left;\">Vaccination Name (CVX Code)</th>");
                out.println(
                        "        <th style=\"border:1px solid #ddd; padding:10px; text-align:left;\">Forecast Status</th>");
                out.println(
                        "        <th style=\"border:1px solid #ddd; padding:10px; text-align:left;\">Earliest Date</th>");
                out.println(
                        "        <th style=\"border:1px solid #ddd; padding:10px; text-align:left;\">Recommended Date</th>");
                out.println("      </tr>");

                for (RecommendationRow rec : model.recommendations) {
                    out.println("      <tr>");
                    out.println("        <td style=\"border:1px solid #ddd; padding:10px;\">" +
                            htmlEscape(rec.vaccineName.isEmpty() ? rec.cvxCode
                                    : rec.vaccineName + " (" + rec.cvxCode + ")")
                            +
                            "</td>");
                    out.println("        <td style=\"border:1px solid #ddd; padding:10px;\">"
                            + htmlEscape(rec.forecastStatus) + "</td>");
                    out.println("        <td style=\"border:1px solid #ddd; padding:10px;\">"
                            + htmlEscape(rec.earliestDate) + "</td>");
                    out.println("        <td style=\"border:1px solid #ddd; padding:10px;\">"
                            + htmlEscape(rec.recommendedDate) + "</td>");
                    out.println("      </tr>");
                }

                out.println("    </table>");
                out.println("  </div>");
            }

            out.println("  <div class=\"output-container\">");
            out.println("    <h3>Request</h3>");
            out.println(
                    "    <button type=\"button\" class=\"copy-button\" onclick=\"copyText('requestOutput')\">Copy</button>");
            out.println("    <pre id=\"requestOutput\">" + htmlEscape(model.requestPretty) + "</pre>");
            out.println("  </div>");
        }

        if (!model.responsePretty.isEmpty()) {
            out.println("  <div class=\"output-container\">");
            out.println("    <h3>Response</h3>");
            out.println(
                    "    <button type=\"button\" class=\"copy-button\" onclick=\"copyText('responseOutput')\">Copy</button>");
            out.println("    <pre id=\"responseOutput\">" + htmlEscape(model.responsePretty) + "</pre>");
            out.println("  </div>");
        }

        if (!model.evaluationLines.isEmpty()) {
            out.println("  <div class=\"output-container\">");
            out.println("    <h3>Evaluations</h3>");
            out.println("    <ul>");
            for (String line : model.evaluationLines) {
                out.println("      <li>" + htmlEscape(line) + "</li>");
            }
            out.println("    </ul>");
            out.println("  </div>");
        }

        if (!model.recommendationLines.isEmpty()) {
            out.println("  <div class=\"output-container\">");
            out.println("    <h3>Recommendations</h3>");
            out.println("    <ul>");
            for (String line : model.recommendationLines) {
                out.println("      <li>" + htmlEscape(line) + "</li>");
            }
            out.println("    </ul>");
            out.println("  </div>");
        }

        out.println("  </div>"); // Close main-content div
        out.println("</body>");
        out.println("</html>");

        out.flush();
    }

    /**
     * HTML-escape a string for safe display in HTML content.
     * Converts &, <, >, ", and ' to their HTML entity equivalents.
     * 
     * @param s The string to escape
     * @return Escaped string safe for HTML
     */
    private String htmlEscape(String s) {
        if (s == null)
            return "";
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    /**
     * Alias for backward compatibility. Prefer htmlEscape().
     * 
     * @param text The string to escape
     * @return Escaped string safe for HTML
     */
    private String escapeHtml(String text) {
        return htmlEscape(text);
    }

    /**
     * Safe parameter helper: returns trimmed parameter value or empty string.
     * 
     * @param req  The HTTP request
     * @param name The parameter name
     * @return Trimmed parameter value or empty string if null/blank
     */
    /**
     * Parses a flexible date format: YYYYMMDD or MM/DD/YYYY.
     * 
     * @param s The date string (trimmed automatically, blank treated as null)
     * @return Parsed LocalDate
     * @throws IllegalArgumentException if format is invalid
     */
    private LocalDate parseFlexibleDate(String s) {
        if (s == null) {
            throw new IllegalArgumentException("Date cannot be null");
        }

        s = s.trim();
        if (s.isEmpty()) {
            throw new IllegalArgumentException("Date cannot be blank");
        }

        // Try YYYYMMDD format (8 digits)
        if (s.matches("\\d{8}")) {
            try {
                return LocalDate.parse(s, DateTimeFormatter.ofPattern("yyyyMMdd"));
            } catch (DateTimeParseException e) {
                throw new IllegalArgumentException("Invalid YYYYMMDD format: " + s, e);
            }
        }

        // Try MM/DD/YYYY format
        if (s.matches("\\d{1,2}/\\d{1,2}/\\d{4}")) {
            try {
                return LocalDate.parse(s, DateTimeFormatter.ofPattern("M/d/yyyy"));
            } catch (DateTimeParseException e) {
                throw new IllegalArgumentException("Invalid MM/DD/YYYY format: " + s, e);
            }
        }

        throw new IllegalArgumentException("Date must be in YYYYMMDD or MM/DD/YYYY format, got: " + s);
    }

}

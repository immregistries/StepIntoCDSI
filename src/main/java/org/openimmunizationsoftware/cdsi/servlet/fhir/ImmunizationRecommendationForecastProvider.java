package org.openimmunizationsoftware.cdsi.servlet.fhir;

import ca.uhn.fhir.model.api.annotation.Description;
import ca.uhn.fhir.rest.annotation.Operation;
import ca.uhn.fhir.rest.annotation.OperationParam;

import org.hl7.fhir.instance.model.api.IPrimitiveType;
import org.openimmunizationsoftware.cdsi.core.data.DataModel;
import org.openimmunizationsoftware.cdsi.core.data.DataModelLoader;
import org.openimmunizationsoftware.cdsi.core.domain.VaccineGroupForecast;
import org.openimmunizationsoftware.cdsi.core.domain.VaccineGroupStatus;
import org.openimmunizationsoftware.cdsi.core.logic.LogicStep;
import org.openimmunizationsoftware.cdsi.core.logic.LogicStepFactory;
import org.openimmunizationsoftware.cdsi.core.logic.LogicStepType;
import org.openimmunizationsoftware.cdsi.servlet.fits.TestCaseRegistered;
import org.openimmunizationsoftware.cdsi.servlet.fits.TestCaseRegistered.Vaccination;
import org.openimmunizationsoftware.cdsi.servlet.SupportingDataManager;
import org.openimmunizationsoftware.cdsi.servlet.VersionComparator;
import org.hl7.fhir.r4.model.*;

import javax.servlet.ServletContext;
import java.util.*;

public class ImmunizationRecommendationForecastProvider {
	private static final String DATE_CRITERION_CODE_EARLIEST = "30981-5";
	private static final String DATE_CRITERION_CODE_DUE = "30980-7";
	private static final String[] DATE_CRITERION_CODES = { DATE_CRITERION_CODE_EARLIEST, DATE_CRITERION_CODE_DUE,
			"59777-3", "59778-1" };
	private static final String[] DATE_CRITERION_DISPLAYS = { "Earliest date to give", "Date vaccine due",
			"Latest date to give immunization", "Date when overdue for immunization" };
	private static final String IMMUNIZATION_RECOMMENDATION_DATE_CRITERION_SYSTEM = "http://hl7.org/fhir/ValueSet/immunization-recommendation-date-criterion";
	private static final String IMMUNIZATION_RECOMMENDATION_STATUS_SYSTEM = "http://hl7.org/fhir/ValueSet/immunization-recommendation-status";
	private static final String CVX = "http://hl7.org/fhir/sid/cvx";

	public static final String $_IMMDS_FORECAST = "$immds-forecast";
	public static final String ASSESSMENT_DATE = "assessmentDate";
	public static final String PATIENT = "patient";
	public static final String RECOMMENDATION = "recommendation";
	public static final String IMMUNIZATION = "immunization";
	public static final String KNOWLEDGE_BASE = "knowledgeBase";
	public static final String KNOWLEDGE_BASE_VERSION = "knowledgeBaseVersion";
	public static final String KNOWLEDGE_BASE_USED = "knowledgeBaseUsed";
	public static final String KNOWLEDGE_BASE_VERSION_USED = "knowledgeBaseVersionUsed";

	// Knowledge base system URI per ImmDS specification
	private static final String KNOWLEDGE_BASE_SYSTEM = "https://ivci.org/knowledge-base";
	// Hardcoded for now: all zips are from USA-CDC-CDSI knowledge base
	private static final String USA_CDC_CDSI = "USA-CDC-CDSI";

	private ServletContext servletContext;

	public ImmunizationRecommendationForecastProvider() {
	}

	public void setServletContext(ServletContext servletContext) {
		this.servletContext = servletContext;
	}

	@Operation(name = $_IMMDS_FORECAST)
	public Parameters immdsForecastSample(
			@Description(shortDefinition = "The date on which to assess the forecast.") @OperationParam(name = ASSESSMENT_DATE, min = 1, max = 1, typeName = "date") IPrimitiveType<Date> assessmentDate,
			@Description(shortDefinition = "Patient information.") @OperationParam(name = PATIENT, min = 1, max = 1) Patient patient,
			@Description(shortDefinition = "Patient immunization history.") @OperationParam(name = IMMUNIZATION) List<Immunization> immunization,
			@Description(shortDefinition = "Knowledge base identifier (CodeableConcept).") @OperationParam(name = KNOWLEDGE_BASE, min = 0, max = 1) CodeableConcept knowledgeBase,
			@Description(shortDefinition = "Knowledge base version string.") @OperationParam(name = KNOWLEDGE_BASE_VERSION, min = 0, max = 1, typeName = "string") StringType knowledgeBaseVersion) {
		Parameters out = new Parameters();
		TestCaseRegistered tcr = new TestCaseRegistered();
		tcr.setBirthDate(patient.getBirthDate());
		tcr.setEvalDate(assessmentDate.getValue() == null ? new Date() : assessmentDate.getValue());
		if (immunization != null) {
			for (Immunization imm : immunization) {
				Vaccination vaccination = tcr.addVaccination();
				vaccination.setVaccineDate(imm.getOccurrenceDateTimeType().getValue());
				vaccination.setVaccineCvx(imm.getVaccineCode().getCodingFirstRep().getCode());
			}
		}
		ImmunizationRecommendation recommendation = new ImmunizationRecommendation();
		String knowledgeBaseUsed = USA_CDC_CDSI; // Default to USA-CDC-CDSI
		String knowledgeBaseVersionUsed = null;

		try {
			// Parse incoming knowledge base parameter
			String requestedKnowledgeBase = null;
			if (knowledgeBase != null && knowledgeBase.hasCoding()) {
				for (Coding coding : knowledgeBase.getCoding()) {
					if (KNOWLEDGE_BASE_SYSTEM.equals(coding.getSystem())) {
						requestedKnowledgeBase = coding.getCode();
						break;
					}
				}
			}

			// Validate knowledge base (only USA-CDC-CDSI supported for now)
			if (requestedKnowledgeBase != null && !USA_CDC_CDSI.equals(requestedKnowledgeBase)) {
				// Return OperationOutcome for unsupported knowledge base
				OperationOutcome outcome = new OperationOutcome();
				outcome.addIssue()
						.setSeverity(OperationOutcome.IssueSeverity.ERROR)
						.setCode(OperationOutcome.IssueType.NOTSUPPORTED)
						.setDiagnostics("Knowledge base '" + requestedKnowledgeBase + "' is not supported. Only '"
								+ USA_CDC_CDSI + "' is available.");
				out.addParameter().setName("outcome").setResource(outcome);
				return out;
			}

			// Resolve supportingDataSet based on knowledge base version
			String versionStr = knowledgeBaseVersion != null ? knowledgeBaseVersion.getValue() : null;
			String resolvedSupportingDataSet = resolveSupportingDataSet(versionStr);
			// Extract actual version used from resolved setId
			if (resolvedSupportingDataSet != null) {
				knowledgeBaseVersionUsed = extractVersionFromSetId(resolvedSupportingDataSet);
			}

			DataModel dataModel = resolvedSupportingDataSet == null || resolvedSupportingDataSet.trim().equals("")
					? DataModelLoader.createDataModel()
					: DataModelLoader.createDataModel(resolvedSupportingDataSet.trim());
			// setup data model
			dataModel.setTestCaseRegistered(tcr);
			LogicStepFactory.createLogicStep(LogicStepType.GATHER_NECESSARY_DATA, dataModel);
			dataModel.setNextLogicStep(
					LogicStepFactory.createLogicStep(LogicStepType.GATHER_NECESSARY_DATA, dataModel));
			process(dataModel);

			recommendation.setDate(tcr.getEvalDate());
			recommendation.setAuthority(new Reference()
					.setIdentifier(new Identifier().setSystem("AIRA_TEST").setValue("step")));
			List<VaccineGroupForecast> vaccineGroupForecastList = dataModel.getVaccineGroupForecastList();
			if (vaccineGroupForecastList != null) {
				for (VaccineGroupForecast vgf : vaccineGroupForecastList) {
					String actCvx = vgf.getAntigen().getCvxForForecast();
					String actCvxDisplay = vgf.getAntigen().getName();

					// Tetanus (112)
					if (actCvx.equals("112")) {
						Calendar c = Calendar.getInstance();
						c.setTime(dataModel.getPatient().getDateOfBirth());
						c.add(Calendar.YEAR, 7);
						if (assessmentDate.getValue().before(c.getTime())) {
							actCvxDisplay = "DTaP";
							actCvx = "107";
						} else {
							actCvxDisplay = "Tdap";
							actCvx = "115";
						}
					} else if (actCvx.equals("05") || actCvx.equals("06") || actCvx.equals("07")) {
						actCvxDisplay = "MMR";
						actCvx = "03";
					}

					VaccineGroupStatus vaccineGroupStatus = vgf.getVaccineGroupStatus();
					ImmunizationRecommendation.ImmunizationRecommendationRecommendationComponent recommendationComponent = recommendation
							.addRecommendation()
							.setDescription("Step Into CDSi")
							.setForecastStatus(new CodeableConcept()
									.addCoding(new Coding(IMMUNIZATION_RECOMMENDATION_STATUS_SYSTEM,
											vaccineGroupStatus.toString(), vaccineGroupStatus.toString())));
					recommendationComponent.addVaccineCode().addCoding().setSystem(CVX).setCode(actCvx)
							.setDisplay(actCvxDisplay);
					if (vgf.getVaccineGroupStatus() == VaccineGroupStatus.NOT_COMPLETE) {
						recommendationComponent.addDateCriterion()
								.setValue(vgf.getEarliestDate())
								.setCode(new CodeableConcept()
										.addCoding(new Coding(IMMUNIZATION_RECOMMENDATION_DATE_CRITERION_SYSTEM,
												DATE_CRITERION_CODE_EARLIEST, "Earliest Date")));
						recommendationComponent.addDateCriterion()
								.setValue(vgf.getAdjustedRecommendedDate())
								.setCode(new CodeableConcept()
										.addCoding(new Coding(IMMUNIZATION_RECOMMENDATION_DATE_CRITERION_SYSTEM,
												DATE_CRITERION_CODE_DUE, "Recommended Date")));
					}
				}

			}

		} catch (Exception e) {
			e.printStackTrace();
		}

		// Add OUT parameters: recommendation, knowledgeBaseUsed,
		// knowledgeBaseVersionUsed
		out.addParameter().setName(RECOMMENDATION).setResource(recommendation);

		// knowledgeBaseUsed as CodeableConcept
		CodeableConcept kbUsed = new CodeableConcept();
		kbUsed.addCoding()
				.setSystem(KNOWLEDGE_BASE_SYSTEM)
				.setCode(knowledgeBaseUsed)
				.setDisplay(knowledgeBaseUsed);
		out.addParameter().setName(KNOWLEDGE_BASE_USED).setValue(kbUsed);

		// knowledgeBaseVersionUsed as string
		if (knowledgeBaseVersionUsed != null) {
			out.addParameter().setName(KNOWLEDGE_BASE_VERSION_USED).setValue(new StringType(knowledgeBaseVersionUsed));
		}

		return out;
	}

	/**
	 * Resolve supporting data set ID based on requested knowledge base version.
	 * If version is null/empty, select latest using numeric comparison.
	 * If version is specified, find exact match or best match.
	 * 
	 * @param requestedVersion Requested knowledge base version (e.g., "4.64")
	 * @return Supporting data set ID (e.g., "supporting-data-4.64-508")
	 */
	private String resolveSupportingDataSet(String requestedVersion) {
		if (servletContext == null) {
			return null;
		}

		List<String> allSets = SupportingDataManager.listSupportingDataSetIds(servletContext);
		if (allSets.isEmpty()) {
			return null;
		}

		// If no version requested, select latest
		if (requestedVersion == null || requestedVersion.trim().isEmpty()) {
			return selectLatestSupportingDataSet(allSets);
		}

		// Find exact match or best match for requested version
		String normalizedRequest = requestedVersion.trim();
		for (String setId : allSets) {
			String version = extractVersionFromSetId(setId);
			if (version != null && version.equals(normalizedRequest)) {
				return setId;
			}
		}

		// No exact match found - return OperationOutcome would be ideal,
		// but for now just return latest
		return selectLatestSupportingDataSet(allSets);
	}

	/**
	 * Select the latest supporting data set from available sets using numeric
	 * version comparison.
	 * 
	 * @param setIds List of supporting data set IDs
	 * @return Latest set ID, or first alphabetically if all versions unparseable
	 */
	private String selectLatestSupportingDataSet(List<String> setIds) {
		if (setIds == null || setIds.isEmpty()) {
			return null;
		}

		// Extract versions and find latest
		Map<String, String> versionToSetId = new HashMap<>();
		List<String> versions = new ArrayList<>();

		for (String setId : setIds) {
			String version = extractVersionFromSetId(setId);
			if (version != null && !version.isEmpty()) {
				versionToSetId.put(version, setId);
				versions.add(version);
			}
		}

		if (versions.isEmpty()) {
			// No parseable versions, fall back to alphabetical
			return setIds.get(0);
		}

		String latestVersion = VersionComparator.selectLatest(versions);
		return versionToSetId.get(latestVersion);
	}

	/**
	 * Extract version string from supporting data set ID.
	 * Examples:
	 * - "supporting-data-4.64-508" -> "4.64"
	 * - "supporting-data-4.10" -> "4.10"
	 * - "4.64-508" -> "4.64"
	 * 
	 * @param setId Supporting data set ID
	 * @return Version string, or null if not parseable
	 */
	private String extractVersionFromSetId(String setId) {
		if (setId == null || setId.trim().isEmpty()) {
			return null;
		}

		String s = setId.trim();

		// Remove common prefix if present
		if (s.startsWith("supporting-data-")) {
			s = s.substring("supporting-data-".length());
		}

		// Extract version as first dot-separated numeric segments
		// Examples: "4.64-508" -> "4.64", "4.10" -> "4.10"
		StringBuilder version = new StringBuilder();
		boolean foundDigit = false;

		for (int i = 0; i < s.length(); i++) {
			char c = s.charAt(i);
			if (Character.isDigit(c)) {
				version.append(c);
				foundDigit = true;
			} else if (c == '.' && foundDigit && i + 1 < s.length() && Character.isDigit(s.charAt(i + 1))) {
				version.append(c);
			} else if (foundDigit && (c == '-' || c == '_' || !Character.isLetterOrDigit(c))) {
				// Stop at first separator after digits
				break;
			}
		}

		return foundDigit ? version.toString() : null;
	}

	private void process(DataModel dataModel) throws Exception {
		int count = 0;
		while (dataModel.getLogicStep().getLogicStepType() != LogicStepType.END) {
			LogicStep nextLogicStep = dataModel.getLogicStep().process();
			dataModel.setNextLogicStep(nextLogicStep);
			count++;
			if (count > 100000) {
				System.err.println(
						"Appear to be caught in a loop at this step: " + dataModel.getLogicStep().getTitle());
				// too many steps!
				if (count > 100100) {
					throw new RuntimeException(
							"Logic steps seem to be caught in a loop, " + dataModel.getLogicStep().getTitle()
									+ ", unable to get results");
				}
			}
		}
	}

	public ImmunizationRecommendation generate(Date date, Patient patient) {
		ImmunizationRecommendation recommendation = new ImmunizationRecommendation();
		recommendation.setDate(date);

		recommendation = addGeneratedRecommendation(recommendation, randomDateAround(date));
		recommendation = addGeneratedRecommendation(recommendation, randomDateAround(date));
		recommendation = addGeneratedRecommendation(recommendation, randomDateAround(date));
		recommendation.setAuthority(new Reference()
				.setIdentifier(new Identifier().setSystem("AIRA_TEST").setValue("tester")));
		return recommendation;
	}

	public ImmunizationRecommendation addGeneratedRecommendation(ImmunizationRecommendation recommendation, Date date) {
		recommendation.setDate(date);
		ImmunizationRecommendation.ImmunizationRecommendationRecommendationComponent recommendationComponent = recommendation
				.addRecommendation()
				.setDescription("Random sample generated")
				.setForecastStatus(new CodeableConcept()
						.addCoding(new Coding(IMMUNIZATION_RECOMMENDATION_STATUS_SYSTEM, "due", "Due")));

		recommendationComponent.addVaccineCode().addCoding().setSystem(CVX).setCode("03").setDisplay("MMR");

		int randN = (int) (Math.random() * 4);
		int randDateN = (int) (1 + Math.random() * 15);
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(new java.util.Date());
		calendar.add(Calendar.DATE, randDateN);
		calendar.getTime();
		recommendationComponent.addDateCriterion()
				.setValue(calendar.getTime())
				.setCode(new CodeableConcept().addCoding(new Coding(IMMUNIZATION_RECOMMENDATION_DATE_CRITERION_SYSTEM,
						DATE_CRITERION_CODES[randN], DATE_CRITERION_DISPLAYS[randN])));
		return recommendation;
	}

	private Date randomDateAround(Date date) {
		Random random = new Random();
		return new Date((long) random.nextInt(60) * 3600 * 1000 * 24 + date.getTime());
	}
}

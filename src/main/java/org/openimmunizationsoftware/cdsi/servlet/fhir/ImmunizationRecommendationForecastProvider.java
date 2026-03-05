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
	private static final String USA_CDC_CDSI = SupportingDataManager.DEFAULT_KNOWLEDGE_BASE_ID;

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
			// Check if servletContext is available
			if (servletContext == null) {
				throw new IllegalStateException("ServletContext has not been initialized");
			}

			// Parse incoming knowledge base parameter
			String requestedKnowledgeBase = null;
			if (knowledgeBase != null && knowledgeBase.hasCoding()) {
				for (Coding coding : knowledgeBase.getCoding()) {
					if (KNOWLEDGE_BASE_SYSTEM.equals(coding.getSystem()) && coding.hasCode()) {
						requestedKnowledgeBase = coding.getCode();
						break;
					}
				}
				if ((requestedKnowledgeBase == null || requestedKnowledgeBase.trim().equals(""))
						&& knowledgeBase.getCodingFirstRep().hasCode()) {
					requestedKnowledgeBase = knowledgeBase.getCodingFirstRep().getCode();
				}
			}

			// Resolve supportingDataSet based on knowledge base and version
			String versionStr = knowledgeBaseVersion != null ? knowledgeBaseVersion.getValue() : null;
			String resolvedSupportingDataSet = SupportingDataManager.resolveSupportingDataSetForKnowledgeBase(
					servletContext, requestedKnowledgeBase, versionStr);

			if (resolvedSupportingDataSet == null) {
				OperationOutcome outcome = new OperationOutcome();
				outcome.addIssue()
						.setSeverity(OperationOutcome.IssueSeverity.ERROR)
						.setCode(OperationOutcome.IssueType.NOTSUPPORTED)
						.setDiagnostics(buildKnowledgeBaseNotFoundMessage(requestedKnowledgeBase));
				out.addParameter().setName("outcome").setResource(outcome);
				return out;
			}

			SupportingDataManager.SupportingDataDescriptor descriptor = SupportingDataManager
					.findSupportingDataDescriptor(servletContext, resolvedSupportingDataSet);
			if (descriptor != null) {
				knowledgeBaseUsed = descriptor.knowledgeBaseId;
				knowledgeBaseVersionUsed = descriptor.version;
			} else {
				knowledgeBaseUsed = requestedKnowledgeBase == null || requestedKnowledgeBase.trim().equals("")
						? USA_CDC_CDSI
						: requestedKnowledgeBase;
			}

			DataModel dataModel = DataModelLoader.createDataModel(resolvedSupportingDataSet.trim());
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
			// Log the exception with full details
			System.err.println("Error in FHIR $immds-forecast operation:");
			e.printStackTrace();

			// Return error outcome to client
			OperationOutcome outcome = new OperationOutcome();
			outcome.addIssue()
					.setSeverity(OperationOutcome.IssueSeverity.ERROR)
					.setCode(OperationOutcome.IssueType.EXCEPTION)
					.setDiagnostics("Internal error: " + e.getMessage());
			out.addParameter().setName("outcome").setResource(outcome);
			return out;
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

	private String buildKnowledgeBaseNotFoundMessage(String requestedKnowledgeBase) {
		String target = requestedKnowledgeBase == null || requestedKnowledgeBase.trim().equals("")
				? USA_CDC_CDSI
				: requestedKnowledgeBase.trim();

		// Check if servletContext is available
		if (servletContext == null) {
			return "Knowledge base '" + target + "' is not available (ServletContext not initialized).";
		}

		List<String> knowledgeBaseIdList = SupportingDataManager.listKnowledgeBaseIds(servletContext);
		if (knowledgeBaseIdList.isEmpty()) {
			return "No supporting data sets are available for knowledge base '" + target + "'.";
		}
		return "Knowledge base '" + target + "' is not available. Available knowledge bases: "
				+ String.join(", ", knowledgeBaseIdList);
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

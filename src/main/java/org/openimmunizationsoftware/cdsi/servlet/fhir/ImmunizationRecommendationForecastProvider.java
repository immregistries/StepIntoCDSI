package org.openimmunizationsoftware.cdsi.servlet.fhir;

import ca.uhn.fhir.model.api.annotation.Description;
import ca.uhn.fhir.rest.annotation.Operation;
import ca.uhn.fhir.rest.annotation.OperationParam;
import org.hl7.fhir.instance.model.api.IPrimitiveType;
import org.hl7.fhir.r4.model.Immunization;
import org.hl7.fhir.r4.model.ImmunizationRecommendation;
import org.hl7.fhir.r4.model.Parameters;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.*;

import java.io.InputStream;
import java.util.*;

public class ImmunizationRecommendationForecastProvider {
	private static final String[] DATE_CRITERION_CODES = { "30981-5", "30980-7", "59777-3", "59778-1" };
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

	public ImmunizationRecommendationForecastProvider() {

	}

	@Operation(name = $_IMMDS_FORECAST)
	public Parameters immdsForecastSample(
			@Description(shortDefinition = "The date on which to assess the forecast.") @OperationParam(name = ASSESSMENT_DATE, min = 1, max = 1, typeName = "date") IPrimitiveType<Date> assessmentDate,
			@Description(shortDefinition = "Patient information.") @OperationParam(name = PATIENT, min = 1, max = 1) Patient patient,
			@Description(shortDefinition = "Patient immunization history.") @OperationParam(name = IMMUNIZATION) List<Immunization> immunization) {
		Parameters out = new Parameters();
		ImmunizationRecommendation immunizationRecommendation = generate(assessmentDate.getValue(), patient);
		out.addParameter().setName(RECOMMENDATION).setResource(immunizationRecommendation);
		return out;
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

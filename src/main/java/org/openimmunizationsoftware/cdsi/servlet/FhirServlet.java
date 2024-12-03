package org.openimmunizationsoftware.cdsi.servlet;

import java.util.ArrayList;
import java.util.List;

import org.openimmunizationsoftware.cdsi.servlet.fhir.ImmunizationRecommendationForecastProvider;
import org.openimmunizationsoftware.cdsi.servlet.fhir.ImmunizationRecommendationProvider;
import org.openimmunizationsoftware.cdsi.servlet.fhir.PatientResourceProvider;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.narrative.DefaultThymeleafNarrativeGenerator;
import ca.uhn.fhir.narrative.INarrativeGenerator;
import ca.uhn.fhir.rest.server.IResourceProvider;
import ca.uhn.fhir.rest.server.RestfulServer;
import ca.uhn.fhir.rest.server.interceptor.ResponseHighlighterInterceptor;

/**
 * This servlet is the actual FHIR server itself
 */
public class FhirServlet extends RestfulServer {

	private static final long serialVersionUID = 1L;

	/**
	 * Constructor
	 */
	public FhirServlet() {
		super(FhirContext.forR4Cached()); // This is an R4 server
	}

	/**
	 * This method is called automatically when the
	 * servlet is initializing.
	 */
	@Override
	public void initialize() {
		/*
		 * Two resource providers are defined. Each one handles a specific
		 * type of resource.
		 */
		ImmunizationRecommendationForecastProvider immunizationRecommendationForecastProvider = new ImmunizationRecommendationForecastProvider();
		ImmunizationRecommendationProvider immunizationRecommendationProvider = new ImmunizationRecommendationProvider(
				immunizationRecommendationForecastProvider);
		List<IResourceProvider> providers = new ArrayList<>();
		providers.add(new PatientResourceProvider());
		providers.add(immunizationRecommendationProvider);
		setResourceProviders(providers);

		registerProvider(immunizationRecommendationForecastProvider);

		/*
		 * Use a narrative generator. This is a completely optional step,
		 * but can be useful as it causes HAPI to generate narratives for
		 * resources which don't otherwise have one.
		 */
		INarrativeGenerator narrativeGen = new DefaultThymeleafNarrativeGenerator();
		getFhirContext().setNarrativeGenerator(narrativeGen);

		/*
		 * Use nice coloured HTML when a browser is used to request the content
		 */
		registerInterceptor(new ResponseHighlighterInterceptor());

	}

}

package org.openimmunizationsoftware.cdsi.servlet.fhir;

import ca.uhn.fhir.rest.annotation.Search;
import ca.uhn.fhir.rest.server.IResourceProvider;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.ImmunizationRecommendation;

import java.util.Date;

public class ImmunizationRecommendationProvider implements IResourceProvider {

   private ImmunizationRecommendationForecastProvider forecastProvider;

   public ImmunizationRecommendationProvider(ImmunizationRecommendationForecastProvider forecastProvider) {
      this.forecastProvider = forecastProvider;
   }

   @Override
   public Class<ImmunizationRecommendation> getResourceType() {
      return ImmunizationRecommendation.class;
   }

   @Search
   public Bundle read() {
      Bundle bundle = new Bundle();
      bundle.addEntry().setResource(forecastProvider.generate(new Date(), null));
      return bundle;
   }
}

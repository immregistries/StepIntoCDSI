package org.openimmunizationsoftware.cdsi.servlet.fhir;

import ca.uhn.fhir.rest.annotation.IdParam;
import ca.uhn.fhir.rest.annotation.Read;
import ca.uhn.fhir.rest.annotation.RequiredParam;
import ca.uhn.fhir.rest.annotation.Search;
import ca.uhn.fhir.rest.param.StringParam;
import ca.uhn.fhir.rest.server.IResourceProvider;
import org.hl7.fhir.r4.model.*;
import org.hl7.fhir.r4.model.ValueSet.ConceptReferenceComponent;

import java.util.ArrayList;
import java.util.List;

public class ValueSetProvider implements IResourceProvider {

    public static final String VALUE_SET_ID = "contextual-conditions";
    public static final String SYSTEM_URL = CodeSystemProvider.SYSTEM_URL;

    @Override
    public Class<ValueSet> getResourceType() {
        return ValueSet.class;
    }

    @Read()
    public ValueSet readValueSet(@IdParam IdType theId) {
        if (theId == null || !VALUE_SET_ID.equals(theId.getIdPart())) {
            throw new ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException(theId);
        }
        return createValueSet();
    }

    @Search()
    public List<ValueSet> searchValueSet(@RequiredParam(name = "url") StringParam theUrl) {
        List<ValueSet> result = new ArrayList<>();
        if (VALUE_SET_ID.equals(theUrl.getValue()) || SYSTEM_URL.equals(theUrl.getValue())) {
            result.add(createValueSet());
        }
        return result;
    }

    private ValueSet createValueSet() {
        ValueSet vs = new ValueSet();
        vs.setId(VALUE_SET_ID);
        vs.setUrl(SYSTEM_URL + "/ValueSet/" + VALUE_SET_ID);
        vs.setVersion("1.0.0");
        vs.setName("ContextualConditionsSupported");
        vs.setTitle("Supported Contextual Conditions");
        vs.setStatus(Enumerations.PublicationStatus.ACTIVE);
        vs.setExperimental(false);
        vs.setPublisher("CDC CDSi");

        vs.setDescription("ValueSet of contextual conditions supported by the CDSI system. These conditions include contraindications, indications, and observations that affect immunization recommendations.");

        ValueSet.ValueSetComposeComponent compose = new ValueSet.ValueSetComposeComponent();
        ValueSet.ConceptSetComponent include = new ValueSet.ConceptSetComponent();
        include.setSystem(SYSTEM_URL);
        include.setVersion("1.0.0");

        List<ConditionService.ConditionInfo> conditions = ConditionService.getInstance().getAllConditions();
        for (ConditionService.ConditionInfo condition : conditions) {
            ConceptReferenceComponent ref = new ConceptReferenceComponent();
            ref.setCode(condition.code);
            ref.setDisplay(condition.title);
            include.addConcept(ref);
        }

        compose.addInclude(include);
        vs.setCompose(compose);

        return vs;
    }
}

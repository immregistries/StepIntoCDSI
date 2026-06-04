package org.openimmunizationsoftware.cdsi.servlet.fhir;

import ca.uhn.fhir.rest.annotation.IdParam;
import ca.uhn.fhir.rest.annotation.Read;
import ca.uhn.fhir.rest.annotation.RequiredParam;
import ca.uhn.fhir.rest.annotation.Search;
import ca.uhn.fhir.rest.param.StringParam;
import ca.uhn.fhir.rest.server.IResourceProvider;
import org.hl7.fhir.r4.model.*;
import org.hl7.fhir.r4.model.CodeSystem.ConceptDefinitionComponent;

import java.util.ArrayList;
import java.util.List;

public class CodeSystemProvider implements IResourceProvider {

    public static final String SYSTEM_URL = "https://ivci.org/conditions";
    public static final String CODE_SYSTEM_ID = "contextual-conditions";

    @Override
    public Class<CodeSystem> getResourceType() {
        return CodeSystem.class;
    }

    @Read()
    public CodeSystem readCodeSystem(@IdParam IdType theId) {
        if (theId == null || !CODE_SYSTEM_ID.equals(theId.getIdPart())) {
            throw new ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException(theId);
        }
        return createCodeSystem();
    }

    @Search()
    public List<CodeSystem> searchCodeSystem(@RequiredParam(name = "url") StringParam theUrl) {
        List<CodeSystem> result = new ArrayList<>();
        if (SYSTEM_URL.equals(theUrl.getValue()) || CODE_SYSTEM_ID.equals(theUrl.getValue())) {
            result.add(createCodeSystem());
        }
        return result;
    }

    private CodeSystem createCodeSystem() {
        CodeSystem cs = new CodeSystem();
        cs.setId(CODE_SYSTEM_ID);
        cs.setUrl(SYSTEM_URL);
        cs.setVersion("1.0.0");
        cs.setName("ContextualConditionsCDC CDSI");
        cs.setTitle("CDC CDSI Contextual Conditions");
        cs.setStatus(Enumerations.PublicationStatus.ACTIVE);
        cs.setExperimental(false);
        cs.setPublisher("Unknown");
        cs.setDescription("Contextual conditions used in Clinical Decision Support for Immunization (CDSI). These conditions include contraindications, indications, and observations that affect immunization recommendations.");

        cs.setContent(CodeSystem.CodeSystemContentMode.COMPLETE);

        List<ConditionService.ConditionInfo> conditions = ConditionService.getInstance().getAllConditions();
        cs.setCount(conditions.size());

        for (ConditionService.ConditionInfo condition : conditions) {
            ConceptDefinitionComponent concept = new ConceptDefinitionComponent();
            concept.setCode(condition.code);
            concept.setDisplay(condition.title);
            if (condition.definition != null && !condition.definition.isEmpty()) {
                concept.setDefinition(condition.definition);
            }
            cs.addConcept(concept);
        }

        return cs;
    }
}

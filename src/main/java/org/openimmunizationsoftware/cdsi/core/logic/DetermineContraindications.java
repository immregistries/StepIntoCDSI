package org.openimmunizationsoftware.cdsi.core.logic;

import static org.openimmunizationsoftware.cdsi.core.logic.items.LogicResult.ANY;
import static org.openimmunizationsoftware.cdsi.core.logic.items.LogicResult.NO;
import static org.openimmunizationsoftware.cdsi.core.logic.items.LogicResult.YES;

import java.util.Date;
import java.util.List;
import java.io.PrintWriter;

import org.openimmunizationsoftware.cdsi.core.data.DataModel;
import org.openimmunizationsoftware.cdsi.core.domain.AdverseReaction;
import org.openimmunizationsoftware.cdsi.core.domain.Contraindication;
import org.openimmunizationsoftware.cdsi.core.domain.RelevantMedicalObservation;
import org.openimmunizationsoftware.cdsi.core.logic.items.ConditionAttribute;
import org.openimmunizationsoftware.cdsi.core.logic.items.LogicCondition;
import org.openimmunizationsoftware.cdsi.core.logic.items.LogicOutcome;
import org.openimmunizationsoftware.cdsi.core.logic.items.LogicResult;
import org.openimmunizationsoftware.cdsi.core.logic.items.LogicTable;
import org.openimmunizationsoftware.cdsi.core.logic.items.ContraindicationElements;

/* TODO: Adjust logic as follows
    The null ConditionAttributes need to be declared inside of LTInnerset or something
    Then each of the logic tables that happen within 7.3 will be their own instance of LTInnerset
    Write the logic for logic tables 7-5 to 7-7, correct logicOutcomes and everything
    Afterwards make sure to point to 7.4 next
    is this true?
 */

// TODO: Log results of certain things with log()?



public class DetermineContraindications extends LogicStep{

    // ConditionAttributes to be used
    private ConditionAttribute<List<RelevantMedicalObservation>> caActivePatientObservations = null;
    private ConditionAttribute<List<AdverseReaction>> caAdverseReactions = null;
    private ConditionAttribute<ContraindicationElements> caContraindicationElements = null;
    private ConditionAttribute<Date> caAssessmentDate = null;
    private ConditionAttribute<Date> caContraindicationBeginAgeDate = null;
    private ConditionAttribute<Date> caContraindicationEndAgeDate = null;

    public DetermineContraindications(DataModel dataModel){
        super(LogicStepType.DETERMINE_CONTRAINDICATIONS, dataModel);

        // Table 7-4
        setConditionTableName("Table 7-4 Determine Contraindication Attributes");
        caActivePatientObservations = new ConditionAttribute<List<RelevantMedicalObservation>>("Patient Data", "Active Patient Observations");
        caAdverseReactions = new ConditionAttribute<List<AdverseReaction>>("Patient Data", "Adverse Reactions");
        caContraindicationElements = new ConditionAttribute<ContraindicationElements>("Supporting Data", "Contraindication Elements");
        caAssessmentDate = new ConditionAttribute<Date>("Processing Data","Assessment Date");
        caContraindicationBeginAgeDate = new ConditionAttribute<Date>("Calculated Date (CALCDTCI-1)","Contraindication Begin Age Date");
        caContraindicationEndAgeDate = new ConditionAttribute<Date>("Calculated Date (CALCDTCI-2)","Contraindication End Age Date");

        // Sets initial and assumed values
        /* TODO: 
            Create CALCDTI1 and CALCDTI2 in business rules
            Set initial values for caContraindicationBeginAgeDate, caContraindicationEndAgeDate
            Find out what ContraindicationElements is supposed to contain and adjust its initial value to match
            */
        caActivePatientObservations.setInitialValue(dataModel.getPatient().getMedicalHistory().getRelevantMedicalObservationList());
        caAdverseReactions.setInitialValue(dataModel.getPatient().getMedicalHistory().getImmunizationHistory().getAdverseReactionList());
        caContraindicationElements.setInitialValue(new ContraindicationElements()); // I don't know what the initial values are supposed to be?
        caAssessmentDate.setInitialValue(dataModel.getAssessmentDate()); 
        caAssessmentDate.setAssumedValue(new Date());
        //caContradictionBeginAgeDate.setInitialValue(CALCDTCI-1.evaluate());
        caContraindicationBeginAgeDate.setAssumedValue(FUTURE);
        //caContradictionEndAgeDate.setInitialValue(CALCDTI-2.evaluate());
        caContraindicationEndAgeDate.setAssumedValue(PAST);

        // Adds items to conditionAttributesList
        conditionAttributesList.add(caActivePatientObservations);
        conditionAttributesList.add(caAdverseReactions);
        conditionAttributesList.add(caContraindicationElements);
        conditionAttributesList.add(caAssessmentDate); // Isn't this one in there already?
        conditionAttributesList.add(caContraindicationBeginAgeDate);
        conditionAttributesList.add(caContraindicationEndAgeDate);
        
        

    }
}

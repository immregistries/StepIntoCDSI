// Created by Nicole on 10/25/24, or somewhere around that time.

package org.openimmunizationsoftware.cdsi.core.logic;

import java.util.Date;
import java.util.List;
import java.io.PrintWriter;

import org.openimmunizationsoftware.cdsi.core.data.DataModel;
import org.openimmunizationsoftware.cdsi.core.domain.AdverseReaction;
import org.openimmunizationsoftware.cdsi.core.domain.Contraindication;
import org.openimmunizationsoftware.cdsi.core.domain.RelevantMedicalObservation;
import org.openimmunizationsoftware.cdsi.core.logic.items.ConditionAttribute;

import static org.openimmunizationsoftware.cdsi.core.logic.concepts.DateRules.CALCDTCI_1;
import static org.openimmunizationsoftware.cdsi.core.logic.concepts.DateRules.CALCDTCI_2;

/* TODO: Adjust logic as follows, finish creating entire file
    The null ConditionAttributes need to be declared inside of LTInnerset or something
    Then each of the logic tables that happen within 7.3 will be their own instance of LTInnerset
    Write the logic for logic tables 7-5 to 7-7, correct logicOutcomes and everything
    Afterwards make sure to point to 7.4 next
    -is this true?
 */

//

public class DetermineContraindications extends LogicStep {

    // ConditionAttributes to be used
    private ConditionAttribute<List<RelevantMedicalObservation>> caActivePatientObservations = null;
    private ConditionAttribute<List<AdverseReaction>> caAdverseReactions = null;
    private ConditionAttribute<Contraindication> caContraindicationElements = null;
    private ConditionAttribute<Date> caAssessmentDate = null;
    private ConditionAttribute<Date> caContraindicationBeginAgeDate = null;
    private ConditionAttribute<Date> caContraindicationEndAgeDate = null;

    public DetermineContraindications(DataModel dataModel) {
        super(LogicStepType.DETERMINE_CONTRAINDICATIONS, dataModel);

        // Table 7-4
        setConditionTableName("Table 7-4 Determine Contraindication Attributes");
        caActivePatientObservations = new ConditionAttribute<List<RelevantMedicalObservation>>("Patient Data",
                "Active Patient Observations");
        caAdverseReactions = new ConditionAttribute<List<AdverseReaction>>("Patient Data", "Adverse Reactions");
        caContraindicationElements = new ConditionAttribute<Contraindication>("Supporting Data",
                "Contraindication Elements");
        caAssessmentDate = new ConditionAttribute<Date>("Processing Data", "Assessment Date");
        caContraindicationBeginAgeDate = new ConditionAttribute<Date>("Calculated Date (CALCDTCI-1)",
                "Contraindication Begin Age Date");
        caContraindicationEndAgeDate = new ConditionAttribute<Date>("Calculated Date (CALCDTCI-2)",
                "Contraindication End Age Date");

        // set assumed values, if any
        caAssessmentDate.setAssumedValue(new Date());
        caContraindicationBeginAgeDate.setAssumedValue(FUTURE);
        caContraindicationEndAgeDate.setAssumedValue(PAST);

        // set initial values
        caActivePatientObservations
                .setInitialValue(dataModel.getPatient().getMedicalHistory().getRelevantMedicalObservationList());
        caAdverseReactions
                .setInitialValue(dataModel.getPatient().getMedicalHistory().getImmunizationHistory() == null ? null
                        : dataModel.getPatient().getMedicalHistory().getImmunizationHistory().getAdverseReactionList());
        /*
         * TODO:
         * the ContraindicationElements condition attribute cannot be set correctly
         * until 'Contraindication_TO_BE_REMOVED' get replaced with 'Contraindication'
         * to allow for the DataModelLoader to work properly
         * Find out what ContraindicationElements is supposed to contain and adjust its
         * initial value to match
         */
        // caContraindicationElements.setInitialValue(dataModel.getContraindicationList().get(0));
        caAssessmentDate.setInitialValue(dataModel.getAssessmentDate());
        caContraindicationBeginAgeDate
                .setInitialValue(CALCDTCI_1.evaluate(dataModel, this, caContraindicationElements.getFinalValue()));
        caContraindicationEndAgeDate
                .setInitialValue(CALCDTCI_2.evaluate(dataModel, this, caContraindicationElements.getFinalValue()));

        // Adds items to conditionAttributesList
        conditionAttributesList.add(caActivePatientObservations);
        conditionAttributesList.add(caAdverseReactions);
        conditionAttributesList.add(caContraindicationElements);
        conditionAttributesList.add(caAssessmentDate);
        conditionAttributesList.add(caContraindicationBeginAgeDate);
        conditionAttributesList.add(caContraindicationEndAgeDate);

        setNextLogicStepType(LogicStepType.DETERMINE_FORECAST_NEED);
    }

    // TODO add Print, placeholder for now
    @Override
    public LogicStep process() throws Exception {
        setNextLogicStepType(LogicStepType.DETERMINE_FORECAST_NEED);
        evaluateLogicTables();
        return next();
    }

    @Override
    public void printPre(PrintWriter out) throws Exception {
        printStandard(out);
    }

    @Override
    public void printPost(PrintWriter out) throws Exception {
        printStandard(out);
    }

    private void printStandard(PrintWriter out) {
        out.println("<p>Placeholder text</p>");
    }

    // Logic Tables
    /*
     * setLogicCondition(0, new LogicCondition("Is X equal to Y?") {
     * 
     * @Override
     * public LogicResult evaluateInternal() {
     * if (X == Y) {
     * return LogicResult.YES;
     * }
     * return LogicResult.NO;
     * }
     * });
     */
    // Everything in the Logic Table class happens inside the constructor.

    // Logic Table 7-5
    /*
     * private class LT75 extends LogicTable {
     * public LT75(){
     * super(3, 5,
     * "Table 7-5 Does the Antigen Contraindication Apply to the Patient?");
     * 
     * // Logic Conditions
     * setLogicCondition(0, new
     * LogicCondition("Does the antigen contraindication describe any active patient observations?"
     * ){
     * 
     * @Override
     * public LogicResult evaluateInternal() {
     * if(){
     * return YES;
     * }
     * return NO;
     * }
     * });
     * }
     * }
     */
}

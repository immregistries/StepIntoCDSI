package org.openimmunizationsoftware.cdsi.core.logic;

import java.util.Date;
import java.util.List;

import org.openimmunizationsoftware.cdsi.core.data.DataModel;
import org.openimmunizationsoftware.cdsi.core.domain.AdverseReaction;
import org.openimmunizationsoftware.cdsi.core.domain.Contraindication;
import org.openimmunizationsoftware.cdsi.core.domain.PatientObservation;
import org.openimmunizationsoftware.cdsi.core.domain.PatientSeries;
import org.openimmunizationsoftware.cdsi.core.domain.PreferrableVaccine;
import org.openimmunizationsoftware.cdsi.core.domain.VaccineContraindication;
import org.openimmunizationsoftware.cdsi.core.domain.VaccineType;
import org.openimmunizationsoftware.cdsi.core.domain.datatypes.PatientSeriesStatus;
import org.openimmunizationsoftware.cdsi.core.logic.items.ConditionAttribute;
import org.openimmunizationsoftware.cdsi.core.logic.items.LogicCondition;
import org.openimmunizationsoftware.cdsi.core.logic.items.LogicOutcome;
import org.openimmunizationsoftware.cdsi.core.logic.items.LogicResult;
import org.openimmunizationsoftware.cdsi.core.logic.items.LogicTable;

import static org.openimmunizationsoftware.cdsi.core.logic.concepts.DateRules.CALCDTCI_1;
import static org.openimmunizationsoftware.cdsi.core.logic.concepts.DateRules.CALCDTCI_2;
import static org.openimmunizationsoftware.cdsi.core.logic.items.LogicResult.ANY;
import static org.openimmunizationsoftware.cdsi.core.logic.items.LogicResult.NO;
import static org.openimmunizationsoftware.cdsi.core.logic.items.LogicResult.UNKNOWN;
import static org.openimmunizationsoftware.cdsi.core.logic.items.LogicResult.YES;

public class DetermineContraindications extends LogicStep {

  // ConditionAttributes to be used
  private ConditionAttribute<List<PatientObservation>> caActivePatientObservations = null;
  private ConditionAttribute<List<AdverseReaction>> caAdverseReactions = null;
  private ConditionAttribute<Contraindication> caContraindicationElements = null;
  private ConditionAttribute<Date> caAssessmentDate = null;
  private ConditionAttribute<Date> caContraindicationBeginAgeDate = null;
  private ConditionAttribute<Date> caContraindicationEndAgeDate = null;

  private final LT75 lt75 = new LT75();
  private final LT76 lt76 = new LT76();
  private final LT77 lt77 = new LT77();

  public DetermineContraindications(DataModel dataModel) {
    super(LogicStepType.DETERMINE_CONTRAINDICATIONS, dataModel);

    // Table 7-4
    setConditionTableName("Table 7-4 Determine Contraindication Attributes");
    caActivePatientObservations = new ConditionAttribute<List<PatientObservation>>("Patient Data",
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
    // Table 7-4: the begin age date's assumed value is in the past (an
    // ageless contraindication is already in force); the end age date's is in
    // the future (it never expires).
    caContraindicationBeginAgeDate.setAssumedValue(PAST);
    caContraindicationEndAgeDate.setAssumedValue(FUTURE);

    // set initial values
    caActivePatientObservations
        .setInitialValue(dataModel.getPatient().getMedicalHistory().getPatientObservationList());
    caAdverseReactions
        .setInitialValue(dataModel
            .getPatient().getMedicalHistory().getImmunizationHistory() == null
                ? null
                : dataModel.getPatient().getMedicalHistory()
                    .getImmunizationHistory()
                    .getAdverseReactionList());

    // Table 7-4 gives "Contraindication elements" no assumed value - it has to
    // come from the Supporting Data. A representative contraindication (the
    // first one loaded, if any) is what the attribute publishes and what
    // CALCDTCI-1/2 compute from; the real per-contraindication determination
    // happens in process(), which checks every one of them.
    List<Contraindication> allContraindications = dataModel.getContraindicationList();
    Contraindication currentContraindication =
        allContraindications.isEmpty() ? null : allContraindications.get(0);
    caContraindicationElements.setInitialValue(currentContraindication);
    caAssessmentDate.setInitialValue(dataModel.getAssessmentDate());
    caContraindicationBeginAgeDate
        .setInitialValue(CALCDTCI_1.evaluate(dataModel, this, currentContraindication));
    caContraindicationEndAgeDate
        .setInitialValue(CALCDTCI_2.evaluate(dataModel, this, currentContraindication));

    // Adds items to conditionAttributesList
    conditionAttributesList.add(caActivePatientObservations);
    conditionAttributesList.add(caAdverseReactions);
    conditionAttributesList.add(caContraindicationElements);
    conditionAttributesList.add(caAssessmentDate);
    conditionAttributesList.add(caContraindicationBeginAgeDate);
    conditionAttributesList.add(caContraindicationEndAgeDate);

    logicTableList.add(lt75);
    logicTableList.add(lt76);
    logicTableList.add(lt77);

    setNextLogicStepType(LogicStepType.DETERMINE_FORECAST_NEED);
  }

  @Override
  public LogicStep process() throws Exception {
    setNextLogicStepType(LogicStepType.DETERMINE_FORECAST_NEED);

    // Table 7-7 condition 1: does any antigen contraindication apply? Vaccine
    // contraindications are the narrower, specifically-marked case (they name
    // particular vaccine types); everything else - including a plain
    // Contraindication built directly rather than through the loader - acts at
    // the antigen level.
    boolean anyAntigenContraindicationApplies = false;
    for (Contraindication contraindication : dataModel.getContraindicationList()) {
      if (!(contraindication instanceof VaccineContraindication)) {
        lt75.current = contraindication;
        lt75.evaluate();
        if (lt75.applies) {
          anyAntigenContraindicationApplies = true;
        }
      }
    }

    // Table 7-7 condition 2: does every preferable vaccine for the relevant
    // patient series have at least one applying vaccine contraindication?
    List<PreferrableVaccine> preferrableVaccineList =
        dataModel.getTargetDose().getTrackedSeriesDose().getPreferrableVaccineList();
    boolean allPreferableVaccinesContraindicated = !preferrableVaccineList.isEmpty();
    for (PreferrableVaccine preferrableVaccine : preferrableVaccineList) {
      boolean thisVaccineContraindicated = false;
      for (Contraindication contraindication : dataModel.getContraindicationList()) {
        if (contraindication instanceof VaccineContraindication) {
          lt76.current = contraindication;
          lt76.currentPreferableVaccineType = preferrableVaccine.getVaccineType();
          lt76.evaluate();
          if (lt76.applies) {
            thisVaccineContraindicated = true;
          }
        }
      }
      if (!thisVaccineContraindicated) {
        allPreferableVaccinesContraindicated = false;
      }
    }

    lt77.anyAntigenContraindicationApplies = anyAntigenContraindicationApplies;
    lt77.allPreferableVaccinesContraindicated = allPreferableVaccinesContraindicated;
    lt77.evaluate();

    return next();
  }

  /**
   * The begin/end age date window a contraindication is in force for, falling
   * back to Table 7-4's assumed values (past/future) exactly as the published
   * attribute does when a contraindication defines no age at all.
   */
  private boolean isWithinAgeWindow(Contraindication contraindication) {
    Date beginAgeDate = CALCDTCI_1.evaluate(dataModel, this, contraindication);
    if (beginAgeDate == null) {
      beginAgeDate = PAST;
    }
    Date endAgeDate = CALCDTCI_2.evaluate(dataModel, this, contraindication);
    if (endAgeDate == null) {
      endAgeDate = FUTURE;
    }
    Date assessmentDate = dataModel.getAssessmentDate();
    return !beginAgeDate.after(assessmentDate) && assessmentDate.before(endAgeDate);
  }

  /**
   * Does the patient's own history record an active observation matching this
   * contraindication's observation code?
   */
  private boolean describesActivePatientObservation(Contraindication contraindication) {
    if (contraindication.getObservationCode() == null) {
      return false;
    }
    for (PatientObservation observation : dataModel.getPatient().getMedicalHistory()
        .getPatientObservationList()) {
      if (observation.getObservationCode() != null
          && contraindication.getObservationCode().equals(observation.getObservationCode().getCode())) {
        return true;
      }
    }
    return false;
  }

  // Table 7-5
  private class LT75 extends LogicTable {
    private Contraindication current = null;
    private boolean applies = false;

    public LT75() {
      super(3, 5, "Table 7-5 Does the antigen contraindication apply to the patient?");

      setLogicCondition(0, new LogicCondition(
          "Does the antigen contraindication describe any active patient observations?") {
        @Override
        protected LogicResult evaluateInternal() {
          return describesActivePatientObservation(current) ? YES : NO;
        }
      });
      setLogicCondition(1, new LogicCondition(
          "Does the antigen contraindication describe any adverse reactions?") {
        @Override
        protected LogicResult evaluateInternal() {
          // AdverseReaction is an unimplemented domain stub (see status.yaml's
          // materiality note for this unit) - nothing ever records one, so this
          // can never be answered Yes or Unknown today.
          return NO;
        }
      });
      setLogicCondition(2, new LogicCondition(
          "Is the contraindication begin age date <= the assessment date < the contraindication "
              + "end age date?") {
        @Override
        protected LogicResult evaluateInternal() {
          return isWithinAgeWindow(current) ? YES : NO;
        }
      });

      setLogicResults(0, YES, NO, NO, UNKNOWN, ANY);
      setLogicResults(1, NO, YES, NO, UNKNOWN, ANY);
      setLogicResults(2, YES, YES, YES, YES, NO);

      setLogicOutcome(0, applyingOutcome());
      setLogicOutcome(1, applyingOutcome());
      setLogicOutcome(2, notApplyingOutcome());
      setLogicOutcome(3, notApplyingOutcome());
      setLogicOutcome(4, notApplyingOutcome());
    }

    private LogicOutcome applyingOutcome() {
      return new LogicOutcome() {
        @Override
        public void perform() {
          applies = true;
          log("Yes. The antigen contraindication applies to the patient.");
        }
      };
    }

    private LogicOutcome notApplyingOutcome() {
      return new LogicOutcome() {
        @Override
        public void perform() {
          applies = false;
          log("No. The antigen contraindication does not apply to the patient.");
        }
      };
    }
  }

  // Table 7-6
  private class LT76 extends LogicTable {
    private Contraindication current = null;
    private VaccineType currentPreferableVaccineType = null;
    private boolean applies = false;

    public LT76() {
      super(4, 7, "Table 7-6 Does the vaccine contraindication apply to the patient?");

      setLogicCondition(0, new LogicCondition(
          "Does the vaccine contraindication describe any active patient observations?") {
        @Override
        protected LogicResult evaluateInternal() {
          return describesActivePatientObservation(current) ? YES : NO;
        }
      });
      setLogicCondition(1, new LogicCondition(
          "Does the vaccine contraindication describe any adverse reactions?") {
        @Override
        protected LogicResult evaluateInternal() {
          return NO;
        }
      });
      setLogicCondition(2, new LogicCondition(
          "Is the contraindication begin age date <= the assessment date < the contraindication "
              + "end age date?") {
        @Override
        protected LogicResult evaluateInternal() {
          return isWithinAgeWindow(current) ? YES : NO;
        }
      });
      setLogicCondition(3, new LogicCondition(
          "Is the vaccine type of the preferable vaccine one of the contraindicated vaccine types "
              + "for the contraindication?") {
        @Override
        protected LogicResult evaluateInternal() {
          if (!(current instanceof VaccineContraindication)) {
            return NO;
          }
          List<VaccineType> contraindicatedVaccineTypeList =
              ((VaccineContraindication) current).getContraindicatedVaccineTypeList();
          // No named vaccine types at all means the contraindication is not
          // restricted to specific brands/types - it applies to any vaccine
          // under the observation, so nothing excludes this one.
          if (contraindicatedVaccineTypeList.isEmpty()) {
            return YES;
          }
          return contraindicatedVaccineTypeList.contains(currentPreferableVaccineType) ? YES : NO;
        }
      });

      setLogicResults(0, YES, YES, NO, NO, NO, ANY, UNKNOWN);
      setLogicResults(1, NO, NO, YES, YES, NO, ANY, UNKNOWN);
      setLogicResults(2, YES, YES, YES, YES, ANY, NO, YES);
      setLogicResults(3, YES, NO, YES, NO, ANY, ANY, YES);

      setLogicOutcome(0, applyingOutcome());
      setLogicOutcome(1, notApplyingOutcome());
      setLogicOutcome(2, applyingOutcome());
      setLogicOutcome(3, notApplyingOutcome());
      setLogicOutcome(4, notApplyingOutcome());
      setLogicOutcome(5, notApplyingOutcome());
      setLogicOutcome(6, notApplyingOutcome());
    }

    private LogicOutcome applyingOutcome() {
      return new LogicOutcome() {
        @Override
        public void perform() {
          applies = true;
          log("Yes. The vaccine contraindication applies to the patient.");
        }
      };
    }

    private LogicOutcome notApplyingOutcome() {
      return new LogicOutcome() {
        @Override
        public void perform() {
          applies = false;
          log("No. The vaccine contraindication does not apply to the patient.");
        }
      };
    }
  }

  // Table 7-7
  private class LT77 extends LogicTable {
    private boolean anyAntigenContraindicationApplies = false;
    private boolean allPreferableVaccinesContraindicated = false;

    public LT77() {
      super(2, 3, "Table 7-7 Is the relevant patient series a contraindicated patient series?");

      setLogicCondition(0, new LogicCondition(
          "Are there any antigen contraindications that apply to the patient?") {
        @Override
        protected LogicResult evaluateInternal() {
          return anyAntigenContraindicationApplies ? YES : NO;
        }
      });
      setLogicCondition(1, new LogicCondition(
          "Do all preferable vaccines for the relevant patient series have at least one vaccine "
              + "contraindication that applies to the patient?") {
        @Override
        protected LogicResult evaluateInternal() {
          return allPreferableVaccinesContraindicated ? YES : NO;
        }
      });

      setLogicResults(0, YES, NO, NO);
      setLogicResults(1, ANY, YES, NO);

      setLogicOutcome(0, new LogicOutcome() {
        @Override
        public void perform() {
          log("Yes. An applying antigen contraindication makes the relevant patient series a "
              + "contraindicated patient series.");
          setContraindicated();
        }
      });
      setLogicOutcome(1, new LogicOutcome() {
        @Override
        public void perform() {
          log("Yes. Every preferable vaccine for the relevant patient series has an applying "
              + "vaccine contraindication, so the relevant patient series is a contraindicated "
              + "patient series.");
          setContraindicated();
        }
      });
      setLogicOutcome(2, new LogicOutcome() {
        @Override
        public void perform() {
          log("No. The relevant patient series is not a contraindicated patient series.");
        }
      });
    }

    private void setContraindicated() {
      PatientSeries patientSeries = dataModel.getPatientSeriesStepper().getCurrent();
      if (patientSeries != null) {
        patientSeries.setPatientSeriesStatus(PatientSeriesStatus.CONTRAINDICATED);
      }
    }
  }
}

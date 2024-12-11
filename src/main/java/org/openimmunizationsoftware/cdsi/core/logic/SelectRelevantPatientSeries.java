package org.openimmunizationsoftware.cdsi.core.logic;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static org.openimmunizationsoftware.cdsi.core.logic.concepts.DateRules.CALCDTIND_1;
import static org.openimmunizationsoftware.cdsi.core.logic.concepts.DateRules.CALCDTIND_2;

import org.openimmunizationsoftware.cdsi.core.data.DataModel;
import org.openimmunizationsoftware.cdsi.core.domain.Antigen;
import org.openimmunizationsoftware.cdsi.core.domain.AntigenSeries;
import org.openimmunizationsoftware.cdsi.core.domain.Indication;
import org.openimmunizationsoftware.cdsi.core.domain.MedicalHistory;
import org.openimmunizationsoftware.cdsi.core.domain.ObservationCode;
import org.openimmunizationsoftware.cdsi.core.domain.PatientSeries;
import org.openimmunizationsoftware.cdsi.core.domain.SeriesType;
import org.openimmunizationsoftware.cdsi.core.logic.items.ConditionAttribute;
import org.openimmunizationsoftware.cdsi.core.logic.items.LogicCondition;
import org.openimmunizationsoftware.cdsi.core.logic.items.LogicOutcome;
import org.openimmunizationsoftware.cdsi.core.logic.items.LogicResult;
import org.openimmunizationsoftware.cdsi.core.logic.items.LogicTable;

public class SelectRelevantPatientSeries extends LogicStep {

  public SelectRelevantPatientSeries(DataModel dataModel) {
    super(LogicStepType.SELECT_RELEVANT_PATIENT_SERIES, dataModel);
    setConditionTableName("TABLE 5-2 SELECT RELEVANT PATIENT SERIES ATTRIBUTES");

    Antigen antigen = dataModel.getAntigenSelectedList().get(dataModel.getAntigenSelectedPos());
    log("Creating patient series for antigen " + antigen.getName());

    for (AntigenSeries antigenSeries : dataModel.getAntigenSeriesList()) {
      if (!antigenSeries.getTargetDisease().equals(antigen)) {
        continue;
      }
      LT55 logicTable55 = new LT55(antigenSeries);

      for (Indication indication : antigenSeries.getIndicationList()) {
        LT54 logicTable54 = new LT54();
        logicTable54.antigenSeries = antigenSeries;

        logicTable54.caGender = new ConditionAttribute<String>("Patient", "Gender");
        logicTable54.caDateOfBirth = new ConditionAttribute<Date>("Patient", "Date Of Birth");
        // TODO: 'MedicalHistory' needs to be replaced with new class 'PatientHistory'
        logicTable54.caActivePatientObservations = new ConditionAttribute<MedicalHistory>("Patient history",
            "Active Patient Observation(s)");
        logicTable54.caRequiredGender = new ConditionAttribute<List<String>>("Supporting Data (Gender)",
            "Required Gender");
        logicTable54.caSeriesType = new ConditionAttribute<String>("Supporting Data (Series Type)", "Series Type");
        logicTable54.caObservationCode = new ConditionAttribute<ObservationCode>("Supporting Data (Indication)",
            "Observation Code");
        logicTable54.caAssessmentDate = new ConditionAttribute<Date>("Runtime data", "Assessment Date");
        logicTable54.caIndicationBeginAgeDate = new ConditionAttribute<Date>("Calculated date (CALCDTIND-1)",
            "Indication Begin Age Date");
        logicTable54.caIndicationEndAgeDate = new ConditionAttribute<Date>("Calculated date (CALCDTIND-2)",
            "Indication End Age Date");

        // setting assumed values, if any
        logicTable54.caGender.setAssumedValue("Unknown");

        List<String> assumedRequiredGenderList = new ArrayList<String>();
        assumedRequiredGenderList.add(dataModel.getPatient().getGender());
        logicTable54.caRequiredGender.setAssumedValue(assumedRequiredGenderList);

        logicTable54.caAssessmentDate.setAssumedValue(new Date());
        logicTable54.caIndicationBeginAgeDate.setAssumedValue(PAST);
        logicTable54.caIndicationEndAgeDate.setAssumedValue(FUTURE);

        // setting initial values
        logicTable54.caGender.setInitialValue(dataModel.getPatient().getGender());
        logicTable54.caDateOfBirth.setInitialValue(dataModel.getPatient().getDateOfBirth());
        logicTable54.caActivePatientObservations.setInitialValue(dataModel.getPatient().getMedicalHistory());
        logicTable54.caRequiredGender.setInitialValue(antigenSeries.getRequiredGenderList());
        logicTable54.caSeriesType.setInitialValue(antigenSeries.getSeriesName());
        logicTable54.caObservationCode.setInitialValue(indication.getObservationCode());
        logicTable54.caAssessmentDate.setInitialValue(dataModel.getAssessmentDate());
        logicTable54.caIndicationBeginAgeDate.setInitialValue(CALCDTIND_1.evaluate(dataModel, this, indication));
        logicTable54.caIndicationEndAgeDate.setInitialValue(CALCDTIND_2.evaluate(dataModel, this, indication));

        logicTable55.addInnerSet(logicTable54);
        logicTableList.add(logicTable54);
      }
      logicTableList.add(logicTable55);
    }
  }

  @Override
  public LogicStep process() throws Exception {
    evaluateLogicTables();
    return LogicStepFactory.createLogicStep(LogicStepType.CREATE_RELEVANT_PATIENT_SERIES, dataModel);
  }

  public void printPre(PrintWriter out) throws Exception {
    out.println("   <h2>Antigen Series</h2>");
    out.println("     <p>Looking at antigen " + (dataModel.getAntigenSelectedPos() + 1)
        + " out of " + dataModel.getAntigenSelectedList().size() + " antigens selected. </p>");
    out.println("   <table>");
    out.println("     <tr>");
    out.println("       <th>Include</th>");
    out.println("       <th>Series Name</th>");
    out.println("       <th>Target Disease</th>");
    out.println("       <th>Vaccine Group</th>");
    out.println("     </tr>");
    Antigen antigen = dataModel.getAntigenSelectedList().get(dataModel.getAntigenSelectedPos());
    int i = 1;
    for (AntigenSeries antigenSeries : dataModel.getAntigenSeriesList()) {
      if (!antigenSeries.getTargetDisease().equals(antigen)) {
        continue;
      }
      out.println("     <tr>");
      out.println("       <td><input type=\"checkbox\" name=\"" + PARAM_ANTIGEN_SERIES_INCLUDE + i
          + "\" value=\"true\" checked></td>");
      out.println("       <td>" + antigenSeries.getSeriesName() + "</td>");
      out.println("       <td>" + antigenSeries.getTargetDisease() + "</td>");
      out.println("       <td>" + (antigenSeries.getVaccineGroup() == null ? ""
          : antigenSeries.getVaccineGroup().getName()) + "</td>");
      out.println("     </tr>");
      i++;
    }
    out.println("   </table>");
  }

  public void printPost(PrintWriter out) throws Exception {
    out.println(
        "   <p>An antigen series is one way to reach perceived immunity against a disease.  "
            + "An antigen series can be thought of as a \"path to immunity\" and is described in "
            + "relative terms.  In many cases, a single antigen may have more than one successful "
            + "path to immunity and as such may have more than one antigen series.  Antigen "
            + "series are defined through supporting data spreadsheets defined in chapter 3.</p>");

    printLogicTables(out);

    out.println("   <h2>Patient Series Included</h2>");
    out.println("   <table>");
    out.println("     <tr>");
    out.println("       <th>Series Name</th>");
    out.println("       <th>Target Disease</th>");
    out.println("       <th>Vaccine Group</th>");
    out.println("     </tr>");
    for (PatientSeries patientSeries : dataModel.getPatientSeriesList()) {
      AntigenSeries antigenSeries = patientSeries.getTrackedAntigenSeries();
      out.println("     <tr>");
      out.println("       <td>" + antigenSeries.getSeriesName() + "</td>");
      out.println("       <td>" + antigenSeries.getTargetDisease() + "</td>");
      out.println("       <td>" + (antigenSeries.getVaccineGroup() == null ? ""
          : antigenSeries.getVaccineGroup().getName()) + "</td>");
      out.println("     </tr>");
    }
    out.println("   </table>");
  }

  protected class LTInnerSet extends LogicTable {
    protected ConditionAttribute<String> caGender = null;
    protected ConditionAttribute<Date> caDateOfBirth = null;
    protected ConditionAttribute<MedicalHistory> caActivePatientObservations = null;
    protected ConditionAttribute<List<String>> caRequiredGender = null;
    protected ConditionAttribute<String> caSeriesType = null;
    protected ConditionAttribute<ObservationCode> caObservationCode = null;
    protected ConditionAttribute<Date> caAssessmentDate = null;
    protected ConditionAttribute<Date> caIndicationBeginAgeDate = null;
    protected ConditionAttribute<Date> caIndicationEndAgeDate = null;
    protected AntigenSeries antigenSeries = null;

    protected LogicResult result = LogicResult.YES;
    protected boolean applies = false;

    protected boolean isApplies() {
      return applies;
    }

    public LogicResult getResult() {
      return result;
    }

    public LTInnerSet(int conditionCount, int outcomeCount, String label) {
      super(conditionCount, outcomeCount, label);
    }
  }

  private class LT54 extends LTInnerSet {
    public LT54() {
      super(2, 4, "TABLE 5-4 DOES THE INDICATION APPLY TO THE PATIENT?");
      
      setLogicCondition(0, new LogicCondition(
          "Does the indication describe any active patient observations?") {
        @Override
        public LogicResult evaluateInternal() {
          // TODO logic condition not yet implemented
          return LogicResult.NO;
        }
      });

      setLogicCondition(1, new LogicCondition(
          "Is the indication begin age date ≤ assessment date < indication end age date?") {
        @Override
        public LogicResult evaluateInternal() {
          if (caIndicationBeginAgeDate == null || caIndicationEndAgeDate == null || caAssessmentDate == null) {
            return LogicResult.NO;
          }
          if (caIndicationBeginAgeDate.getFinalValue().after(caAssessmentDate.getFinalValue())) {
            return LogicResult.NO;
          }
          if (caAssessmentDate.getFinalValue().after(caIndicationEndAgeDate.getFinalValue())) {
            log("yes, indication begin age date is <= assesment date is < indication end age date");
            return LogicResult.YES;
          }
          return LogicResult.NO;
        }
      });

      setLogicResults(0, new LogicResult[] { LogicResult.YES, LogicResult.NO, LogicResult.ANY, LogicResult.ANY });
      setLogicResults(1, new LogicResult[] { LogicResult.YES, LogicResult.YES, LogicResult.YES, LogicResult.NO });

      setLogicOutcome(0, new LogicOutcome() {
        @Override
        public void perform() {
          applies = true;
          log("Yes. The Indication applies to the patient.");
        }
      });
      setLogicOutcome(1, new LogicOutcome() {
        @Override
        public void perform() {
          applies = false;
          log("No. The Indication does not apply to the patient.");
        }
      });
      setLogicOutcome(2, new LogicOutcome() {
        @Override
        public void perform() {
          applies = false;
          log("No. The Indication does not apply to the patient; however, the Indication Text Description should be made available to the clinician for manual determination.");
        }
      });
      setLogicOutcome(3, new LogicOutcome() {
        @Override
        public void perform() {
          applies = false;
          log("No. The Indication does not apply to the patient.");
        }
      });
    }
  }

  protected class LT55 extends LTInnerSet {
    private List<LTInnerSet> innerSetList = new ArrayList<SelectRelevantPatientSeries.LTInnerSet>();
    protected AntigenSeries antigenSeries = null;

    public void addInnerSet(LTInnerSet innerSet) {
      innerSetList.add(innerSet);
    }

    public LT55(AntigenSeries antigenSeries) {
      super(3, 4,
          "TABLE 5-5 IS AN ANTIGEN SERIES '" + antigenSeries.getSeriesName()
              + "' A RELEVANT PATIENT SERIES FOR A PATIENT?");

      setLogicCondition(0,
          new LogicCondition("Is the patient gender one of the required genders of the antigen series?") {
            @Override
            public LogicResult evaluateInternal() {
              if (caRequiredGender == null || caRequiredGender.getFinalValue() == null) {
                return LogicResult.YES;
              }
              if (caRequiredGender.getFinalValue().size() == 0) {
                return LogicResult.YES;
              }

              LogicResult result = LogicResult.NO;
              for (String requiredGender : caRequiredGender.getFinalValue()) {
                if (caGender.equals(requiredGender)) {
                  result = LogicResult.YES;
                }
              }
              return result;
            }
          });

      setLogicCondition(1,
          new LogicCondition("Is the series type of the antigen series 'Standard' or 'Evaluation Only'?") {
            @Override
            public LogicResult evaluateInternal() {
              SeriesType seriesType = antigenSeries.getSeriesType();
              if (seriesType == null) {
                return LogicResult.NO;
              }
              if (seriesType == SeriesType.STANDARD || seriesType == SeriesType.EVALUATION_ONLY) {
                return LogicResult.YES;
              }
              return LogicResult.NO;
            }
          });

      setLogicCondition(2, new LogicCondition(
          "Does at least one indication that drives the need for the antigen series apply to the patient?") {
        @Override
        public LogicResult evaluateInternal() {
          for (LTInnerSet innerSet : innerSetList) {
            if (innerSet.isApplies()) {
              return LogicResult.YES;
            }
          }
          return LogicResult.NO;
        }
      });

      setLogicResults(0, new LogicResult[] { LogicResult.YES, LogicResult.NO, LogicResult.YES, LogicResult.YES, });
      setLogicResults(1, new LogicResult[] { LogicResult.YES, LogicResult.ANY, LogicResult.NO, LogicResult.NO, });
      setLogicResults(2, new LogicResult[] { LogicResult.ANY, LogicResult.ANY, LogicResult.YES, LogicResult.NO, });

      setLogicOutcome(0, new LogicOutcome() {
        @Override
        public void perform() {
          log("Yes. The antigen series is a relevant patient series for the patient.");
          PatientSeries patientSeries = new PatientSeries(antigenSeries);
          dataModel.getPatientSeriesList().add(patientSeries);
        }
      });

      setLogicOutcome(1, new LogicOutcome() {
        @Override
        public void perform() {
          log("No. The antigen series is not a relevant patient series for the patient.");
        }
      });

      setLogicOutcome(2, new LogicOutcome() {
        @Override
        public void perform() {
          log("Yes. The antigen series is a relevant patient series for the patient.");
          PatientSeries patientSeries = new PatientSeries(antigenSeries);
          dataModel.getPatientSeriesList().add(patientSeries);
        }
      });
      setLogicOutcome(3, new LogicOutcome() {
        @Override
        public void perform() {
          log("No. The antigen series is not a relevant patient series for the patient.");
        }
      });
    }
  }
}

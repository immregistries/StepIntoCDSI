package org.openimmunizationsoftware.cdsi.core.logic;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static org.openimmunizationsoftware.cdsi.core.logic.concepts.DateRules.CALCDTIND_1;
import static org.openimmunizationsoftware.cdsi.core.logic.concepts.DateRules.CALCDTIND_2;

import org.openimmunizationsoftware.cdsi.core.data.DataModel;
import org.openimmunizationsoftware.cdsi.core.domain.AllowableVaccine;
import org.openimmunizationsoftware.cdsi.core.domain.Antigen;
import org.openimmunizationsoftware.cdsi.core.domain.AntigenSeries;
import org.openimmunizationsoftware.cdsi.core.domain.Indication;
import org.openimmunizationsoftware.cdsi.core.domain.MedicalHistory;
import org.openimmunizationsoftware.cdsi.core.domain.ObservationCode;
import org.openimmunizationsoftware.cdsi.core.domain.PatientSeries;
import org.openimmunizationsoftware.cdsi.core.domain.RelevantMedicalObservation;
import org.openimmunizationsoftware.cdsi.core.domain.VaccineType;
import org.openimmunizationsoftware.cdsi.core.domain.datatypes.YesNo;
import org.openimmunizationsoftware.cdsi.core.logic.items.ConditionAttribute;
import org.openimmunizationsoftware.cdsi.core.logic.items.LogicCondition;
import org.openimmunizationsoftware.cdsi.core.logic.items.LogicOutcome;
import org.openimmunizationsoftware.cdsi.core.logic.items.LogicResult;
import org.openimmunizationsoftware.cdsi.core.logic.items.LogicTable;

public class SelectRelevantPatientSeries extends LogicStep {

  boolean should_select = true;

  public SelectRelevantPatientSeries(DataModel dataModel) {
    super(LogicStepType.SELECT_RELEVANT_PATIENT_SERIES, dataModel);
    setConditionTableName("TABLE 5-2 SELECT RELEVANT PATIENT SERIES ATTRIBUTES");
    AntigenSeries selectedAntigenSeries = dataModel.getAntigenSeriesList().get(dataModel.getAntigenSelectedPos());

    for (Indication indication : selectedAntigenSeries.getIndicationList()) {
      LT logicTable = new LT();

      logicTable.caGender = new ConditionAttribute<String>("Patient", "Gender");
      logicTable.caDateOfBirth = new ConditionAttribute<Date>("Patient", "Date Of Birth");
      //TODO: 'MedicalHistory' needs to be replaced with new class 'PatientHistory'
      logicTable.caActivePatientObservations = new ConditionAttribute<MedicalHistory>("Patient history", "Active Patient Observation(s)");
      logicTable.caRequiredGender = new ConditionAttribute<String>("Supporting Data (Gender)", "Required Gender");
      logicTable.caSeriesType = new ConditionAttribute<String>("Supporting Data (Series Type)", "Series Type");
      logicTable.caObservationCode = new ConditionAttribute<ObservationCode>("Supporting Data (Indication)", "Observation Code");
      logicTable.caAssessmentDate = new ConditionAttribute<Date>("Runtime data", "Assessment Date");
      logicTable.caIndicationBeginAgeDate = new ConditionAttribute<Date>("Calculated date (CALCDTIND-1)", "Indication Begin Age Date");
      logicTable.caIndicationEndAgeDate = new ConditionAttribute<Date>("Calculated date (CALCDTIND-2)", "Indication End Age Date");

      //setting assumed values, if any
      logicTable.caGender.setAssumedValue("Unknown");
      logicTable.caRequiredGender.setAssumedValue(dataModel.getPatient().getGender());
      logicTable.caAssessmentDate.setAssumedValue(new Date());
      logicTable.caIndicationBeginAgeDate.setAssumedValue(PAST);
      logicTable.caIndicationEndAgeDate.setAssumedValue(FUTURE); 

      //setting initial values
      logicTable.caGender.setInitialValue(dataModel.getPatient().getGender());
      logicTable.caDateOfBirth.setInitialValue(dataModel.getPatient().getDateOfBirth());
      logicTable.caActivePatientObservations.setInitialValue(dataModel.getPatient().getMedicalHistory());
      //TODO find RequiredGender
      //logicTable.caRequiredGender.setInitialValue();
      logicTable.caSeriesType.setInitialValue(selectedAntigenSeries.getSeriesName());
      logicTable.caObservationCode.setInitialValue(indication.getObservationCode());
      logicTable.caAssessmentDate.setInitialValue(dataModel.getAssessmentDate());
      logicTable.caIndicationBeginAgeDate.setInitialValue(CALCDTIND_1.evaluate(dataModel, this, indication));
      logicTable.caIndicationEndAgeDate.setInitialValue(CALCDTIND_2.evaluate(dataModel, this, indication));

      List<ConditionAttribute<?>> caList = new ArrayList<ConditionAttribute<?>>();
      caList.add(logicTable.caGender);
      caList.add(logicTable.caDateOfBirth);
      caList.add(logicTable.caActivePatientObservations);
      caList.add(logicTable.caRequiredGender);
      caList.add(logicTable.caSeriesType);
      caList.add(logicTable.caObservationCode);
      caList.add(logicTable.caAssessmentDate);
      caList.add(logicTable.caIndicationBeginAgeDate);
      caList.add(logicTable.caIndicationEndAgeDate);

      logicTableList.add(logicTable);
    }
  }

  @Override
  public LogicStep process() throws Exception {

    Antigen antigen = dataModel.getAntigenSelectedList().get(dataModel.getAntigenSelectedPos());
    log("Creating patient series for antigen " + antigen.getName());

    for (AntigenSeries antigenSeries : dataModel.getAntigenSeriesList()) {
      if (!antigenSeries.getTargetDisease().equals(antigen)) {
        continue;
      }
      evaluateLogicTables();
      if (should_select) {
        PatientSeries patientSeries = new PatientSeries(antigenSeries);
        dataModel.getPatientSeriesList().add(patientSeries);
      }
    }

    return LogicStepFactory.createLogicStep(LogicStepType.CREATE_RELEVANT_PATIENT_SERIES, dataModel);
  }

  public void printPre(PrintWriter out) throws Exception {
    out.println("   <h2>5.1 Select Relevant Patient Series</h2>");
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
    out.println("   <h1>5.1 Select Relevant Patient Series</h2>");
    out.println(
        "   <p>An antigen series is one way to reach perceived immunity against a disease.  "
            + "An antigen series can be thought of as a \"path to immunity\" and is described in "
            + "relative terms.  In many cases, a single antigen may have more than one successful "
            + "path to immunity and as such may have more than one antigen series.  Antigen "
            + "series are defined through supporting data spreadsheets defined in chapter 3.</p>");

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

  private class LT extends LogicTable {

    private ConditionAttribute<String> caGender = null;
    private ConditionAttribute<Date> caDateOfBirth = null;
    private ConditionAttribute<MedicalHistory> caActivePatientObservations = null;
    private ConditionAttribute<String> caRequiredGender = null;
    private ConditionAttribute<String> caSeriesType = null;
    private ConditionAttribute<ObservationCode> caObservationCode = null;
    private ConditionAttribute<Date> caAssessmentDate = null;
    private ConditionAttribute<Date> caIndicationBeginAgeDate = null;
    private ConditionAttribute<Date> caIndicationEndAgeDate = null;

    public LT() {
      super(2, 4, "TABLE 5-4 DOES THE INDICATION APPLY TO THE PATIENT?");

      setLogicCondition(0, new LogicCondition(
          "Does the indication describe any active patient observations?") {
        @Override
        public LogicResult evaluateInternal() {
          //TODO add logic
          if (false) {
            return LogicResult.NO;
          }
          return LogicResult.YES;
        }
      });

      setLogicCondition(1, new LogicCondition(
          "Is the indication begin age date ≤ assessment date < indication end age date?") {
        @Override
        public LogicResult evaluateInternal() {
          if (caIndicationBeginAgeDate == null || caIndicationEndAgeDate == null || caAssessmentDate == null) {
            return LogicResult.NO;
          }
          if(caIndicationBeginAgeDate.getFinalValue().after(caAssessmentDate.getFinalValue())) {
            return LogicResult.NO;
          }
          if(caAssessmentDate.getFinalValue().after(caIndicationEndAgeDate.getFinalValue())) {
            return LogicResult.YES;
          }
          return LogicResult.NO;
        }
      });

      //TODO logic result 0, 2 asks for an 'UNKNOWN' value, should this be added to the LogicResult ENUM?
      setLogicResults(0, new LogicResult[] {LogicResult.YES, LogicResult.NO, LogicResult.ANY, LogicResult.ANY});
      setLogicResults(1, new LogicResult[] {LogicResult.YES, LogicResult.YES, LogicResult.YES, LogicResult.NO});

      setLogicOutcome(0, new LogicOutcome() {
        @Override
        public void perform() {
          should_select = true;
          log("Yes. The Indication applies to the patient.");
        }
      });
      setLogicOutcome(1, new LogicOutcome() {
        @Override
        public void perform() {
          should_select = false;
          log("No. The Indication does not apply to the patient.");
        }
      });
      setLogicOutcome(2, new LogicOutcome() {
        @Override
        public void perform() {
          should_select = false;
          log("No. The Indication does not apply to the patient; however, the Indication Text Description should be made available to the clinician for manual determination.");
        }
      });
      setLogicOutcome(3, new LogicOutcome() {
        @Override
        public void perform() {
          should_select = false;
          log("No. The Indication does not apply to the patient.");
        }
      });
      
    }
  }

}

package org.openimmunizationsoftware.cdsi.core.logic;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.openimmunizationsoftware.cdsi.core.data.DataModel;
import org.openimmunizationsoftware.cdsi.core.domain.PatientSeries;
import org.openimmunizationsoftware.cdsi.core.domain.TargetDose;
import org.openimmunizationsoftware.cdsi.core.domain.datatypes.PatientSeriesStatus;
import org.openimmunizationsoftware.cdsi.core.domain.datatypes.TargetDoseStatus;

public class CompletePatientSeries extends LogicStep {

  private List<PatientSeries> patientSeriesList = dataModel.getSelectedPatientSeriesList();

  private int numberOfValidDoses(PatientSeries patientSeries) {
    int nbOfValidDoses = 0;
    for (TargetDose target : patientSeries.getTargetDoseList()) {
      if(target.getTargetDoseStatus() != null) {
        if(target.getTargetDoseStatus().equals(TargetDoseStatus.SATISFIED)) {
          nbOfValidDoses++;
        }
      }

    }
    return nbOfValidDoses;
  }

  public CompletePatientSeries(DataModel dataModel) {
    super(LogicStepType.COMPLETE_PATIENT_SERIES, dataModel);
    setConditionTableName("Table ");
  }

  /***
   * cond1 A candidate patient series has the most valid doses
   */

  private void evaluate_ACandidatePatientSeriesHasTheMostValidDoses() {
    int mostValidDoses = 0;
    int numPatientSeriesWithMostValidDoses = 0;
    PatientSeries patientSeriesWithMostValidDoses = null;

    //set mostValidDoses to the greatest number of valid doses found in one patient series
    for (PatientSeries patientSeries : patientSeriesList) {
      if(!patientSeries.getPatientSeriesStatus().equals(PatientSeriesStatus.COMPLETE)) {
        continue;
      }
      
      int newValidDoses = numberOfValidDoses(patientSeries);
      if(newValidDoses > mostValidDoses) {
        mostValidDoses = newValidDoses;
      }
    }

    //get number of patientSeries with the most valid dose
    for (PatientSeries patientSeries : patientSeriesList) {
      if(!patientSeries.getPatientSeriesStatus().equals(PatientSeriesStatus.COMPLETE)) {
        patientSeries.descPatientScoreSeries();
        continue;
      }

      if(numberOfValidDoses(patientSeries) < mostValidDoses) {
        patientSeries.descPatientScoreSeries();
        continue;
      }
      patientSeries.incPatientScoreSeries();
      break;
    }

  }

  @Override
  public LogicStep process() throws Exception {
    setNextLogicStepType(LogicStepType.SELECT_PRIORITIZED_PATIENT_SERIES);
    evaluate_ACandidatePatientSeriesHasTheMostValidDoses();
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
    out.println(
        "<p>Complete  patient  series  provides  the  decision  table  for  determining  the  "
            + "number  of  points  to  assign  to  a complete patient series based on a specified condition. </p>");
    printTable(out);
    printBestPatientSeries(out);
  }

  private void printTable(PrintWriter out) {
    out.println("");
    out.println("<table BORDER=\"1\"> ");
    out.println("  <tr> ");
    out.println(" <th> Conditions </th> ");
    out.println(" <th> If this condition is true for the candidate patient series </th> ");
    out.println(" <th>If this condition is true for two or more candidate patient series </th> ");
    out.println(" <th>If this condition is not true for the candidate patient serie </th> ");
    out.println("  </tr> ");
    out.println("  <tr> ");
    out.println(" <td >A candidate patient series has the most valid doses.</th> ");
    out.println(" <td align=\"center\"> +1</td> ");
    out.println(" <td align=\"center\"> 0</td> ");
    out.println(" <td align=\"center\"> -1 </td> ");
    out.println("  </tr> ");
    out.println("</table>");

  }

}

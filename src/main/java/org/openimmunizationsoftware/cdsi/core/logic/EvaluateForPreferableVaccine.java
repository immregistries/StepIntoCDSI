package org.openimmunizationsoftware.cdsi.core.logic;

import static org.openimmunizationsoftware.cdsi.core.logic.concepts.DateRules.CALCDTPREF_1;
import static org.openimmunizationsoftware.cdsi.core.logic.concepts.DateRules.CALCDTPREF_2;

import java.io.PrintWriter;
import java.util.Date;

import org.openimmunizationsoftware.cdsi.core.data.DataModel;
import org.openimmunizationsoftware.cdsi.core.domain.AllowableVaccine;
import org.openimmunizationsoftware.cdsi.core.domain.AntigenAdministeredRecord;
import org.openimmunizationsoftware.cdsi.core.logic.items.ConditionAttribute;
import org.openimmunizationsoftware.cdsi.core.logic.items.LogicCondition;
import org.openimmunizationsoftware.cdsi.core.logic.items.LogicOutcome;
import org.openimmunizationsoftware.cdsi.core.logic.items.LogicResult;
import org.openimmunizationsoftware.cdsi.core.logic.items.LogicTable;

public class EvaluateForPreferableVaccine extends LogicStep
{

  private ConditionAttribute<Date> caDateAdministered = null;
  //private ConditionAttribute<>
  private ConditionAttribute<String> caTradeName = null;
  private ConditionAttribute<Date> caVaccineTypeBeginAgeDate = null;
  private ConditionAttribute<Date> caVaccineTypeEndAgeDate = null;
  private ConditionAttribute<String> caPreferableVaccineTradeName = null;
  private ConditionAttribute<String> caPreferableVaccineVolume = null;
  private ConditionAttribute<String> caPreferableVaccineType = null;
  private String caVolume = null;

  public EvaluateForPreferableVaccine(DataModel dataModel) {
    super(LogicStepType.EVALUATE_PREFERABLE_VACCINE_ADMINISTERED, dataModel);
    setConditionTableName("Table ");

    caDateAdministered = new ConditionAttribute<Date>("Vaccine dose administered", "Date Administered");
//     _____________>Logic Problem : Check logic specification
    
    AntigenAdministeredRecord aar = dataModel.getAntigenAdministeredRecord();
    caPreferableVaccineTradeName = new ConditionAttribute<String>("Supporting data (Preferable Vaccine)", "Vaccine Type") ;
    caTradeName = new ConditionAttribute<String>("Vaccine dose administered", "Vaccine Type") ;
    caVaccineTypeBeginAgeDate = new ConditionAttribute<Date>("Vaccine Type Begin Age Date", "Calculated date (CALCDTPREF-1)");
    caVaccineTypeEndAgeDate = new ConditionAttribute<Date>("Vaccine Type End Age Date", "Calculated date (CALCDTPREF-2)");
    caPreferableVaccineVolume = new ConditionAttribute<String>("Preferable Vaccine Volume ","Supporting data (Preferable Vaccine)");
    caPreferableVaccineType = new ConditionAttribute<String>("Preferable Vaccine Type ","Supporting data (Preferable Vaccine)");
    caVolume = aar.getVolume();
    caTradeName.setInitialValue(aar.getAntigen().getName());
    caPreferableVaccineTradeName.setInitialValue( dataModel.getAntigenSeriesList().get(0).getSeriesDoseList().get(0).getAllowableVaccineList().get(0).getTradeName()) ;
    caDateAdministered.setInitialValue(aar.getDateAdministered());
    
    conditionAttributesList.add(caDateAdministered);
  	conditionAttributesList.add(caTradeName);
  	conditionAttributesList.add(caVaccineTypeBeginAgeDate);
  	conditionAttributesList.add(caVaccineTypeEndAgeDate);
  	conditionAttributesList.add(caPreferableVaccineTradeName);
  	conditionAttributesList.add(caPreferableVaccineVolume);
    Date birthDate = dataModel.getPatient().getDateOfBirth();

    int i=0, j=0, k=0;
    boolean allTrue = true;
    outerloop :
    for (i=0; i<dataModel.getAntigenSeriesList().size(); i++){
    	for (j=0; j<dataModel.getAntigenSeriesList().get(i).getSeriesDoseList().size(); j++){
    		for (k=0; k<dataModel.getAntigenSeriesList().get(i).getSeriesDoseList().get(j).getPreferrableVaccineList().size() ; k++){
    		    caPreferableVaccineTradeName.setInitialValue( dataModel.getAntigenSeriesList().get(i).getSeriesDoseList().get(j).getPreferrableVaccineList().get(k).getTradeName()) ;
    		    caPreferableVaccineTradeName.setInitialValue( dataModel.getAntigenSeriesList().get(i).getSeriesDoseList().get(j).getPreferrableVaccineList().get(k).getVaccineType().getCvxCode()) ;
    		    caPreferableVaccineVolume.setInitialValue( dataModel.getAntigenSeriesList().get(i).getSeriesDoseList().get(j).getPreferrableVaccineList().get(k).getVolume()) ;

    		    if (dataModel.getAntigenSeriesList().get(i).getSeriesDoseList().get(j).getPreferrableVaccineList().get(k).getVaccineTypeBeginAge()!=null)
    		    	caVaccineTypeBeginAgeDate.setInitialValue(dataModel.getAntigenSeriesList().get(i).getSeriesDoseList().get(j).getPreferrableVaccineList().get(k).getVaccineTypeBeginAge().getDateFrom(birthDate));
    		    if (dataModel.getAntigenSeriesList().get(i).getSeriesDoseList().get(j).getPreferrableVaccineList().get(k).getVaccineTypeEndAge()!=null)
    		    	caVaccineTypeEndAgeDate.setInitialValue(dataModel.getAntigenSeriesList().get(i).getSeriesDoseList().get(j).getPreferrableVaccineList().get(k).getVaccineTypeEndAge().getDateFrom(birthDate));

    		    LT logicTable = new LT();
    		    for (int l = 0; l < logicTable.getLogicOutcomes().length; l++) {
    		        allTrue = true;
    		        for (int m = 0; m < logicTable.getLogicConditions().length; m++) {
    		          if (logicTable.getLogicResultTable()[m][l] != LogicResult.ANY && logicTable.getLogicResultTable()[m][l] != logicTable.getLogicConditions()[m].getLogicResult()) {
    		            allTrue = false;
    		          }
    		        }
    		        if (allTrue) {
    		        	
    		        	//logicTable.getLogicOutcomes()[l].perform();
    		          break outerloop;
    		        }
    		    }
    		    //if (logicTable.getLogicOutcomes().equals())
    		   
    		    logicTableList.add(logicTable);
    		   // k++;
    		}
    		//j++;
    	}
    	//i++;
    }
    
    //    // set assumed values
//    caVaccineTypeBeginAgeDate.setAssumedValue(PAST);
//    caVaccineTypeEndAgeDate.setAssumedValue(FUTURE);
//    caPreferableVaccineTradeName.setAssumedValue(aar.getTradeName());
//    caPreferableVaccineVolume.setAssumedValue(aar.getVolume());
//
//    // set actual values
//    caDateAdministered.setInitialValue(aar.getDateAdministered());
//    caTradeName.setInitialValue(aar.getTradeName());
//    caVaccineTypeBeginAgeDate.setInitialValue(CALCDTPREF_1.evaluate(dataModel, this, null));
//    caVaccineTypeEndAgeDate.setInitialValue(CALCDTPREF_2.evaluate(dataModel, this, null));
//    //caPreferableVaccineTradeName.setInitialValue(initialValue);
//
//    conditionAttributesList.add(caDateAdministered);
//    conditionAttributesList.add(caTradeName);
//    conditionAttributesList.add(caVaccineTypeBeginAgeDate);
//    conditionAttributesList.add(caVaccineTypeEndAgeDate);
//    conditionAttributesList.add(caPreferableVaccineTradeName);
//    conditionAttributesList.add(caPreferableVaccineVolume);


  }

  @Override
  public LogicStep process() throws Exception {
    setNextLogicStepType(LogicStepType.EVALUATE_ALLOWABLE_VACCINE_ADMINISTERED);
    //setNextLogicStepType(LogicStepType.EVALUATE_GENDER);
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
    out.println("<h1> " + getTitle() + "</h1>");
    out.println("<p>Evaluate  for  preferable  vaccine  validates  the  vaccine  of  a  vaccine  dose  administered  against  the  list  of preferable vaccines.</p>");

    printConditionAttributesTable(out);
    printLogicTables(out);
  }

  private class LT extends LogicTable
  {
    public LT() {
      super(4, 5, "Table 4-5 Was the supporting data defined preferrable vaccine administered?");

      setLogicCondition(0, new LogicCondition(
          "Is the vaccine type of the vaccine dose administered the same as the vaccine type of the preferable vaccine? ") {
        @Override
        public LogicResult evaluateInternal() {
          if (caPreferableVaccineType.equals(dataModel.getAntigenAdministeredRecord().getVaccineType().getCvxCode())) {
            return LogicResult.YES;
          }
          else{
          return LogicResult.NO;
          }
        }
      });
      setLogicCondition(1, new LogicCondition(
              "Is the Preferable vaccine type begin age date ≤ date administered < preferable vaccine type end age date? ") {
            @Override
            public LogicResult evaluateInternal() {
              if (caVaccineTypeBeginAgeDate.getInitialValue().before(caDateAdministered.getInitialValue()) && caDateAdministered.getInitialValue().before(caVaccineTypeEndAgeDate.getInitialValue())) {
                return LogicResult.YES;
              }
              else{
              return LogicResult.NO;
              }
            }
          });
      setLogicCondition(2, new LogicCondition(
              "Is the vaccine dose administered trade name the same as the preferable vaccine trade name? ") {
            @Override
            public LogicResult evaluateInternal() {
              if (caTradeName.equals(caPreferableVaccineTradeName)) {
                return LogicResult.YES;
              }
              else{
              return LogicResult.NO;
              }
            }
          });
      setLogicCondition(3, new LogicCondition(
              "Is the Vaccine dose administered volume >= preferable vaccine volume? ") {
            @Override
            public LogicResult evaluateInternal() {
            	if (!(caVolume.equals(new String()) || caPreferableVaccineVolume.getInitialValue().equals(new String()))){
            		if (Double.parseDouble(caPreferableVaccineVolume.getInitialValue()) <= Double.parseDouble(caVolume)) {
                        return LogicResult.YES;
                      }
                      else{
                      return LogicResult.NO;
                      }
            	}
              else{
              return LogicResult.NO;
              }
            }
          });
      setLogicResults(0, new LogicResult[] { LogicResult.YES, LogicResult.YES, LogicResult.NO, LogicResult.YES, LogicResult.YES });
      setLogicResults(1, new LogicResult[] { LogicResult.YES, LogicResult.YES, LogicResult.ANY, LogicResult.NO, LogicResult.YES });
      setLogicResults(2, new LogicResult[] { LogicResult.YES, LogicResult.YES, LogicResult.ANY, LogicResult.ANY, LogicResult.NO });
      setLogicResults(3, new LogicResult[] { LogicResult.YES, LogicResult.NO, LogicResult.ANY, LogicResult.ANY, LogicResult.ANY });

      setLogicOutcome(0, new LogicOutcome() {
          @Override
          public void perform() {
            log("Yes. A preferable vaccine was administered.");
          }
        });

      setLogicOutcome(1, new LogicOutcome() {
          @Override
          public void perform() {
            log("Yes. A preferable vaccine was administered. Evaluation Reason is volume administered is “less than recommended volume.”");
          }
        });
      setLogicOutcome(2, new LogicOutcome() {
          @Override
          public void perform() {
            log("No.  This supporting data defined preferable vaccine was not administered.");
          }
        });
      setLogicOutcome(3, new LogicOutcome() {
          @Override
          public void perform() {
            log("No.  This supporting data defined preferable vaccine was administered out of the preferred age range.");
          }
        });
      setLogicOutcome(4, new LogicOutcome() {
          @Override
          public void perform() {
            log("No. This supporting data defined preferable vaccine was of the wrong trade name. ");
          }
        });
      
      //      setLogicResults(0, LogicResult.YES, LogicResult.NO, LogicResult.NO, LogicResult.ANY);

      //      setLogicOutcome(0, new LogicOutcome() {
      //        @Override
      //        public void perform() {
      //          log("No. The target dose cannot be skipped. ");
      //          log("Setting next step: 4.3 Substitute Target Dose");
      //          setNextLogicStep(LogicStep.SUBSTITUTE_TARGET_DOSE_FOR_EVALUATION);
      //        }
      //      });
      //      
    }
  }

}

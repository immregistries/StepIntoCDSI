package org.openimmunizationsoftware.cdsi.core.logic;

import static org.openimmunizationsoftware.cdsi.core.logic.concepts.DateRules.CALCDTLIVE_1;
import static org.openimmunizationsoftware.cdsi.core.logic.concepts.DateRules.CALCDTLIVE_2;
import static org.openimmunizationsoftware.cdsi.core.logic.items.LogicResult.ANY;
import static org.openimmunizationsoftware.cdsi.core.logic.items.LogicResult.NO;
import static org.openimmunizationsoftware.cdsi.core.logic.items.LogicResult.YES;

import java.io.PrintWriter;
import java.util.Date;

import org.apache.commons.lang.time.DateUtils;
import org.openimmunizationsoftware.cdsi.core.data.DataModel;
import org.openimmunizationsoftware.cdsi.core.domain.AntigenAdministeredRecord;
import org.openimmunizationsoftware.cdsi.core.domain.LiveVirusConflict;
import org.openimmunizationsoftware.cdsi.core.domain.SeriesDose;
import org.openimmunizationsoftware.cdsi.core.domain.VaccineType;
import org.openimmunizationsoftware.cdsi.core.domain.datatypes.YesNo;
import org.openimmunizationsoftware.cdsi.core.logic.items.ConditionAttribute;
import org.openimmunizationsoftware.cdsi.core.logic.items.LogicCondition;
import org.openimmunizationsoftware.cdsi.core.logic.items.LogicOutcome;
import org.openimmunizationsoftware.cdsi.core.logic.items.LogicResult;
import org.openimmunizationsoftware.cdsi.core.logic.items.LogicTable;

public class EvaluateLiveVirusConflict extends LogicStep {

  private ConditionAttribute<Date> caDateAdministered = null;

  private ConditionAttribute<VaccineType> caCurrentVaccineType = null;
  private YesNo y = null;
  
  public EvaluateLiveVirusConflict(DataModel dataModel) {
    super(LogicStepType.EVALUATE_FOR_LIVE_VIRUS_CONFLICT, dataModel);
    setConditionTableName("Table ");

    caDateAdministered = new ConditionAttribute<Date>("Vaccine dose administered", "Date Administered");
   // caConflictBeginIntervalDate = new ConditionAttribute<Date>("Calculated date (CALCDTLIVE-1)",
     //   "Conflict Begin Interval Date");
    //caConflictEndIntervalDate = new ConditionAttribute<Date>("Calculated date(CALCDTLIVE-2 & CALCDTLIVE-3",
     //   "Conflict End Interval Date");
    caCurrentVaccineType = new ConditionAttribute<VaccineType>("Supporting Data (Live Virus Conflict)",
        "Current Vaccine Type");


    conditionAttributesList.add(caDateAdministered);

    conditionAttributesList.add(caCurrentVaccineType);
    //conditionAttributesList.add(caPreviousVaccineType);

    caDateAdministered.setInitialValue(dataModel.getAntigenAdministeredRecord().getDateAdministered());
    //if (dataModel.getAntigenAdministeredRecord().getVaccineType()!= null)
    caCurrentVaccineType.setInitialValue(dataModel.getAntigenAdministeredRecord().getVaccineType());
    
     	
    LT420 logicTable = new LT420();
    //logicTableList.add(logicTable);
    logicTable.evaluate();
    y=YesNo.NO;
    if (logicTable.getY420()==YesNo.YES){
    	for (int i=dataModel.getAntigenAdministeredRecordPos()+1; i<dataModel.getAntigenAdministeredRecordList().size(); i++){
    		AntigenAdministeredRecord vaccineAdministered=dataModel.getAntigenAdministeredRecordList().get(i);
    	    LT421 logicTab = new LT421();
    	    logicTab.caPreviousVaccineType.setInitialValue(vaccineAdministered.getVaccineType());
    		conditionAttributesList.add(logicTab.caPreviousVaccineType);
    		logicTab.evaluate();
    		if (logicTab.y421 == YesNo.YES){
        	    LT422 lt = new LT422();
        	    lt.caConflictBeginIntervalDate = new ConditionAttribute<Date>("Calculated date (CALCDTLIVE-1)",
        	            "Conflict Begin Interval Date");
        	        lt.caConflictEndIntervalDate = new ConditionAttribute<Date>("Calculated date(CALCDTLIVE-2 & CALCDTLIVE-3",
        	            "Conflict End Interval Date");

        	        conditionAttributesList.add(lt.caConflictBeginIntervalDate);
        	        conditionAttributesList.add(lt.caConflictEndIntervalDate);
        	        
        	        lt.setIntervalDate(vaccineAdministered);
        	        
        	     lt.evaluate();
        	     if (lt.y422 == YesNo.YES){
        	    	 y=YesNo.YES;
        	     }
    		}
    	}
    }
  }

  
  @Override
  public LogicStep process() throws Exception {
    setNextLogicStepType(LogicStepType.EVALUATE_PREFERABLE_VACCINE_ADMINISTERED);
   /* for(LogicTable lt : logicTableList){
    	lt.evaluate();
    }*/
    if (y == YesNo.YES){
        dataModel.getTargetDose().setStatusCause(dataModel.getTargetDose().getStatusCause()+"VirusConflict");
    }
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

    out.println(
        "<p>Evaluate  live virus conflict  validates the  date  administered  of a live virus  vaccine dose administered  against previous  live  virus  administered  vaccines  to  ensure  proper  spacing  between  administrations.  For  some  live virus vaccines and for inactivated  virus or recombinant  vaccines, this condition does not exist. Therefore, if no live  virus  supporting  data  exists  for  the  vaccine  dose  administered  being  evaluated,  the  vaccine  dose administered is not in conflict with any other vaccine dose administered.</p>");// <------------------------------------------------------

    //printConditionAttributesTable(out);
    printLogicTables(out);
  }

  private class LT420 extends LogicTable {
	  private YesNo y420 = null;
	  
    public LT420() {

      super(2, 3, "Table 4-20 Should the current vaccine dose administrated be evaluted for a live virus conflict ? ");

      setLogicCondition(0, new LogicCondition(
          "Is the current vaccine type of the vaccine dose administered one of the supporting data defined live virus conflict current vaccine types?") {
        @Override
        protected LogicResult evaluateInternal() {
          for (LiveVirusConflict liveVirusConflict : dataModel.getLiveVirusConflictList()) {
            if (liveVirusConflict.getCurrentVaccineType().equals(caCurrentVaccineType)) {
              return YES;
            }
          }
          return NO;
        }
      });

      setLogicCondition(1, new LogicCondition(
          "Is there at least one vaccine dose administered on or before the current vaccine dose administered date?") {// CHeck

        @Override
        protected LogicResult evaluateInternal() {
          for (AntigenAdministeredRecord aar : dataModel.getAntigenAdministeredRecordList()) {

            if (!aar.getDateAdministered().after(caDateAdministered.getFinalValue())) {
              return YES;
            }
          }
          return NO;
        }
      });

      setLogicResults(0, YES, NO, ANY);
      setLogicResults(1, YES, ANY, NO);

      setLogicOutcome(0, new LogicOutcome() {
        @Override
        public void perform() {
          log("Yes. The vaccine dose administered should be evaluated for a live virus conflict");
          setY420(YesNo.YES);
        }
      });

      setLogicOutcome(1, new LogicOutcome() {
        @Override
        public void perform() {
          log("No. The vaccine dose administered should not be evaluated for a live virus conflict.");
          setY420(YesNo.NO);
        }
      });

      setLogicOutcome(2, new LogicOutcome() {

        @Override
        public void perform() {
          log("No. The vaccine dose administered should not be evaluated for a live virus conflict.");
          dataModel.getTargetDose().setStatusCause(dataModel.getTargetDose().getStatusCause()+"VirusConflict");
        }
      });

    }

	public YesNo getY420() {
		return y420;
	}

	public void setY420(YesNo y420) {
		this.y420 = y420;
	}
  }

  private class LT421 extends LogicTable {
	  private ConditionAttribute<VaccineType> caPreviousVaccineType = null;
	  private YesNo y421 = null;
	  
    public LT421() {

      super(1, 2, "Table 4-21 Could the two vaccine dosesadministrated be in conflict ?");

      setLogicCondition(0, new LogicCondition(
          "Is the vaccine type of the previous vaccine dose administered the same as one of the supporting data defined live virus conflict previous vaccine types when the current vaccine dose administered type is same as the live virus conflict current vaccine type ?") {
        @Override
        protected LogicResult evaluateInternal() {
          for (LiveVirusConflict lvc : dataModel.getLiveVirusConflictList()) {
            if (lvc.getPreviousVaccineType().equals(caPreviousVaccineType)) {
              if (lvc.getCurrentVaccineType().equals(caCurrentVaccineType)) {
                return YES;
              }
            }
          }
          return NO;
        }
      });

      setLogicResults(0, YES);
      setLogicResults(1, NO);

      setLogicOutcome(0, new LogicOutcome() {

        @Override
        public void perform() {
          log("Yes. The two doses must be checked for a live virus conflict");
          y421=YesNo.YES;
        }
      });

      setLogicOutcome(1, new LogicOutcome() {

        @Override
        public void perform() {
          log("No. The two doses need not be checked for a live virus conflict");
          y421=YesNo.NO;
        }
      });

    }

  }

  private class LT422 extends LogicTable {
	  private ConditionAttribute<Date> caConflictBeginIntervalDate = null;
	  private ConditionAttribute<Date> caConflictEndIntervalDate = null;
	  private YesNo y422 = null;
	  
	  public void setIntervalDate(AntigenAdministeredRecord vaccineAdministered){
	        for (LiveVirusConflict liveVirusConflict : dataModel.getLiveVirusConflictList()) {
	            if (liveVirusConflict.getCurrentVaccineType().equals(caCurrentVaccineType)) {
	            	   //caConflictBeginIntervalDate.setInitialValue(vaccineAdministered.getDateAdministered()+liveVirusConflict.getConflictBeginInterval().getAmount());
	        	       //caConflictEndIntervalDate.setInitialValue(CALCDTLIVE_2.evaluate(dataModel, this, null));
	            		  SeriesDose referenceSeriesDose = dataModel.getTargetDose().getTrackedSeriesDose();
	            		  Date dob = vaccineAdministered.getDateAdministered();
	            		  int beginAgeAmount = liveVirusConflict.getConflictBeginInterval().getAmount();
	            		  int endAgeAmount = liveVirusConflict.getConflictBeginInterval().getAmount();
	            		  Date beginIntervalDate = new Date();
	            		  Date endIntervalDate = new Date();
	            			switch (liveVirusConflict.getConflictBeginInterval().getType()) {
	            			case DAY:
	            	  		beginIntervalDate = DateUtils.addDays(dob, beginAgeAmount);    		
	            	  		endIntervalDate = DateUtils.addDays(dob, endAgeAmount);    		
	            	  		break;
	            			case WEEK:
		            	  		beginIntervalDate = DateUtils.addWeeks(dob, beginAgeAmount);    		
		            	  		endIntervalDate = DateUtils.addWeeks(dob, endAgeAmount);
		            	  		break;
	            			case MONTH:
		            	  		beginIntervalDate = DateUtils.addMonths(dob, beginAgeAmount);    		
		            	  		endIntervalDate = DateUtils.addMonths(dob, endAgeAmount);
		            	  		break;
	            			case YEAR:
		            	  		beginIntervalDate = DateUtils.addYears(dob, beginAgeAmount);    		
		            	  		endIntervalDate = DateUtils.addYears(dob, endAgeAmount);
		            	  		break;
	            			default:
	            				break;
	            			}
	            			caConflictBeginIntervalDate.setInitialValue(beginIntervalDate);
	            			caConflictEndIntervalDate.setInitialValue(endIntervalDate);
	            }
	          }
	  }
	  
    public LT422() {
      super(1, 2,
          "Table 4-22 Is the current vaccine dose administrated in conflict with previous vaccije dose administrated ?");

      setLogicCondition(0, new LogicCondition(
          "Is the conflict begin interval date ≤ current date administered<conflict end interval date?") {

        @Override
        protected LogicResult evaluateInternal() {
          for (LiveVirusConflict lvc : dataModel.getLiveVirusConflictList()) {
            if (caDateAdministered.getFinalValue().after(caConflictBeginIntervalDate.getAssumedValue())
                && caDateAdministered.getFinalValue().before(caConflictEndIntervalDate.getAssumedValue())) {
              return YES;
            }
          }

          return NO;
        }
      });

      setLogicResults(0, YES);
      setLogicResults(1, NO);

      setLogicOutcome(0, new LogicOutcome() {

        @Override
        public void perform() {
          log("Yes. The vaccine dose administered is in conflict with a previous vaccine dose administered");
        y422=YesNo.YES;
        }
      });

      setLogicOutcome(1, new LogicOutcome() {

        @Override
        public void perform() {
          log("No. The vaccine dose administered is not in conflict with a previous vaccine dose administered.");
        y422=YesNo.NO;
        }
      });
    }
    
  }

}

package org.openimmunizationsoftware.cdsi.core.logic;

import java.io.PrintWriter;

import org.openimmunizationsoftware.cdsi.core.data.DataModel;
import org.openimmunizationsoftware.cdsi.core.logic.items.LogicTable;

public class ForecastDatesAndReasons extends LogicStep{

	public ForecastDatesAndReasons(DataModel dataModel) {
		super(LogicStepType.FORECAST_DATES_AND_REASONS, dataModel);
		// TODO Auto-generated constructor stub
		setConditionTableName("Table ");
		LT logicTable = new LT();
	    logicTableList.add(logicTable);
	}

	@Override
	public LogicStep process() throws Exception {
		setNextLogicStepType(LogicStepType.EVALUATE_CONDITIONAL_SKIP_FOR_FORECAST);
		return next();
	}

	@Override
	public void printPre(PrintWriter out) throws Exception {
		// TODO Auto-generated method stub
		printStandard(out);
	}

	@Override
	public void printPost(PrintWriter out) throws Exception {
		// TODO Auto-generated method stub
		printStandard(out);
		
	}
	
	private void printStandard(PrintWriter out){
		out.println("<h1> " + getTitle() + "</h1>");
	    out.println("<p>The CDS engine uses a patient’s medical and vaccine history to forecast immunization due dates. This chapter identifies specific business rules that are used by a CDS engine to forecast the next  target dose.  The major steps involved in this process are listed in the table below.</p>");
	    printConditionAttributesTable(out);
	    printLogicTables(out);
	}


	private class LT extends LogicTable{
		public LT() {
			super(0, 0, "Table ?-?");
		}
	}
}

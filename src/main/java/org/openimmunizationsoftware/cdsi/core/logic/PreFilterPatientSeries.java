package org.openimmunizationsoftware.cdsi.core.logic;

import java.io.PrintWriter;

import org.openimmunizationsoftware.cdsi.core.data.DataModel;

public class PreFilterPatientSeries extends LogicStep {

    // private ConditionAttribute<Date> caDateAdministered = null;

    public PreFilterPatientSeries(DataModel dataModel) {
        super(LogicStepType.PRE_FILTER_PATIENT_SERIES, dataModel);
    }

    @Override
    public LogicStep process() throws Exception {
        setNextLogicStepType(LogicStepType.ONE_BEST_PATIENT_SERIES);
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
    }

}

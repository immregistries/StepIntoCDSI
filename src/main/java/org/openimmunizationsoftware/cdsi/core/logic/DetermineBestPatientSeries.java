package org.openimmunizationsoftware.cdsi.core.logic;

import java.io.PrintWriter;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;

import org.apache.jena.sparql.function.library.leviathan.log;
import org.openimmunizationsoftware.cdsi.core.data.DataModel;
import org.openimmunizationsoftware.cdsi.core.domain.AntigenSeries;
import org.openimmunizationsoftware.cdsi.core.domain.PatientSeries;
import org.openimmunizationsoftware.cdsi.core.domain.SeriesType;
import org.openimmunizationsoftware.cdsi.core.domain.TargetDose;
import org.openimmunizationsoftware.cdsi.core.domain.VaccineDoseAdministered;
import org.openimmunizationsoftware.cdsi.core.domain.datatypes.PatientSeriesStatus;
import org.openimmunizationsoftware.cdsi.core.domain.datatypes.YesNo;
import org.openimmunizationsoftware.cdsi.core.logic.items.LogicCondition;
import org.openimmunizationsoftware.cdsi.core.logic.items.LogicOutcome;
import org.openimmunizationsoftware.cdsi.core.logic.items.LogicResult;
import org.openimmunizationsoftware.cdsi.core.logic.items.LogicTable;

public class DetermineBestPatientSeries extends LogicStep {
    private List<PatientSeries> patientSeriesList = dataModel.getSelectedPatientSeriesList();

    public DetermineBestPatientSeries(DataModel dataModel) {
        super(LogicStepType.DETERMINE_BEST_PATIENT_SERIES, dataModel);

        for(PatientSeries ps : patientSeriesList) {
            LT logicTable = new LT();
            logicTable.pps = ps;
            logicTableList.add(logicTable);
        }
    }

    private LinkedHashMap<PatientSeries, Integer> patientSeriesMap = new LinkedHashMap<PatientSeries, Integer>();
    private PatientSeries bestPatientSeries = null;


    @Override
    public LogicStep process() throws Exception {
        evaluateLogicTables();
        if(patientSeriesList != null && patientSeriesList.size() > 0) {
            bestPatientSeries = patientSeriesList.get(0);
        }
        if (bestPatientSeries != null) {
            dataModel.getBestPatientSeriesList().add(bestPatientSeries);
        }
        setNextLogicStepType(LogicStepType.SELECT_BEST_PATIENT_SERIES);
        return next();
    }

    @Override
    public void printPre(PrintWriter out) throws Exception {
        printStandard(out);
    }

    @Override
    public void printPost(PrintWriter out) throws Exception {
        printStandard(out);
        out.println("<p>Best Patient Series: " + bestPatientSeries + "</p>");
        for (Entry<PatientSeries, Integer> entry : patientSeriesMap.entrySet()) {
            out.println(
                    "<p> PatientSeries : " + entry.getKey() + " Value : " + entry.getValue() + " </p>");
        }

    }

    private void printStandard(PrintWriter out) {
        out.print("<h4> " + dataModel.getAntigen().getName() + " </h4>");
        printBestPatientSeries(out);
    }

    private class LT extends LogicTable {
        PatientSeries pps;
        public LT() {
            super(5,3, "TABLE 8-14 IS THE PRIORITIZED PATIENT SERIES THE BEST PATIENT SERIES FOR THE SERIES GROUP?");

            setLogicCondition(0, new LogicCondition("Is the prioritized patient series a complete patient series?") {
                @Override
                protected LogicResult evaluateInternal() {
                    if(pps.getPatientSeriesStatus() == null || pps.getPatientSeriesStatus() != PatientSeriesStatus.NOT_COMPLETE) {
                        return LogicResult.NO;
                    }
                    return LogicResult.YES;
                }
            });

            setLogicCondition(1, new LogicCondition("Is there a prioritized patient series that is a complete patient series in an equivalent series group?") {
                @Override
                protected LogicResult evaluateInternal() {
                    for(PatientSeries ps : patientSeriesList) {
                        if(ps.getPatientSeriesStatus() != PatientSeriesStatus.COMPLETE) {
                            return LogicResult.YES;
                        }
                    }
                    return LogicResult.NO;
                }
            });

            setLogicCondition(2, new LogicCondition("Is the series type of the prioritized patient series 'Evaluation Only'?") {
                @Override
                protected LogicResult evaluateInternal() {
                    if(pps.getTrackedAntigenSeries().getSeriesType().equals(SeriesType.EVALUATION_ONLY)) {
                        return LogicResult.YES;
                    }
                    return LogicResult.NO;
                }
            });

            setLogicCondition(3, new LogicCondition("Is the series type of the prioritized patient series 'Risk'?") {
                @Override
                protected LogicResult evaluateInternal() {
                    if(pps.getTrackedAntigenSeries().getSeriesType().equals(SeriesType.RISK)) {
                        return LogicResult.YES;
                    }
                    return LogicResult.NO;
                }
            });

            setLogicCondition(4, new LogicCondition("Is there a prioritized patient series with a series type of 'Risk' in an equivalent series group?") {
                @Override
                protected LogicResult evaluateInternal() {
                    for(PatientSeries ps : patientSeriesList) {
                        if(ps.getTrackedAntigenSeries().getSeriesType().equals(SeriesType.RISK)) {
                            return LogicResult.YES;
                        }
                    }
                    return LogicResult.NO;
                }
            });

            setLogicResults(0, LogicResult.YES, LogicResult.NO, LogicResult.NO);
            setLogicResults(1, LogicResult.ANY, LogicResult.NO, LogicResult.NO);
            setLogicResults(2, LogicResult.ANY, LogicResult.NO, LogicResult.NO);
            setLogicResults(3, LogicResult.ANY, LogicResult.YES, LogicResult.NO);
            setLogicResults(4, LogicResult.ANY, LogicResult.ANY, LogicResult.NO);

            setLogicOutcomeDefault(new LogicOutcome() {
                @Override
                public void perform() {
                    log("No. There is no best patient series for the series group.");
                }
            });

            setLogicOutcome(0, new LogicOutcome() {
                @Override
                public void perform() {
                    log("Yes. The prioritized patient series is the best patient series for the series group.");
                }
            });

            setLogicOutcome(1, new LogicOutcome() {
                @Override
                public void perform() {
                    log("Yes. The prioritized patient series is the best patient series for the series group.");
                }
            });

            setLogicOutcome(2, new LogicOutcome() {
                @Override
                public void perform() {
                    log("Yes. The prioritized patient series is the best patient series for the series group.");
                }
            });
        }
    }
}

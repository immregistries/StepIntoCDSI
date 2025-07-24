package org.openimmunizationsoftware.cdsi.core.logic;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import org.openimmunizationsoftware.cdsi.core.data.DataModel;
import org.openimmunizationsoftware.cdsi.core.domain.AntigenSeries;
import org.openimmunizationsoftware.cdsi.core.domain.Evaluation;
import org.openimmunizationsoftware.cdsi.core.domain.PatientSeries;
import org.openimmunizationsoftware.cdsi.core.domain.SeriesType;
import org.openimmunizationsoftware.cdsi.core.domain.TargetDose;
import org.openimmunizationsoftware.cdsi.core.domain.datatypes.EvaluationStatus;
import org.openimmunizationsoftware.cdsi.core.domain.datatypes.PatientSeriesStatus;
import org.openimmunizationsoftware.cdsi.core.domain.datatypes.TargetDoseStatus;
import org.openimmunizationsoftware.cdsi.core.domain.datatypes.YesNo;

public class PreFilterPatientSeries extends LogicStep {

    // private ConditionAttribute<Date> caDateAdministered = null;

    public PreFilterPatientSeries(DataModel dataModel) {
        super(LogicStepType.PRE_FILTER_PATIENT_SERIES, dataModel);
    }

    @Override
    public LogicStep process() throws Exception {

        List<PatientSeries> candidatePatientSeriesList = new ArrayList<>();
        log("Adding all non-contraindicated schedules");
        for (PatientSeries patientSeries : dataModel.getPatientSeriesList()) {
            if (!patientSeries.getPatientSeriesStatus().equals(PatientSeriesStatus.CONTRAINDICATED)) {
                log(" - Adding " + patientSeries.getTrackedAntigenSeries().getSeriesName());
                candidatePatientSeriesList.add(patientSeries);
            }
        }
        if (candidatePatientSeriesList.size() == 0) {
            log("No schedules added, adding all contraindicated schedules");
            for (PatientSeries patientSeries : dataModel.getPatientSeriesList()) {
                if (patientSeries.getPatientSeriesStatus().equals(PatientSeriesStatus.CONTRAINDICATED)) {
                    log(" - Adding " + patientSeries.getTrackedAntigenSeries().getSeriesName());
                    candidatePatientSeriesList.add(patientSeries);
                }
            }
        }
        log("Number of candidate patient series: " + candidatePatientSeriesList.size());
        // Now we will decide which ones are relevant.
        List<PatientSeries> scorablePatientSeriesList = new ArrayList<>();
        dataModel.setScorablePatientSeriesList(scorablePatientSeriesList);
        String highestRiskPriority = null;
        for (PatientSeries patientSeries : candidatePatientSeriesList) {
            AntigenSeries antigenSeries = patientSeries.getTrackedAntigenSeries();
            SeriesType seriesType = antigenSeries.getSeriesType();
            String seriesPriority = antigenSeries.getSelectPatientSeries().getSeriesPriority();
            if (seriesType != null && seriesPriority != null) {
                if (seriesType == SeriesType.RISK) {
                    if (highestRiskPriority == null || highestRiskPriority.compareTo(seriesPriority) > 0) {
                        highestRiskPriority = seriesPriority;
                    }
                }
            }
        }
        log("Highest risk priority: " + highestRiskPriority);
        int validDoseCount = 0;
        boolean addedDefault = false;
        log("Looking to add relevant patient series from candidate list");
        for (PatientSeries patientSeries : candidatePatientSeriesList) {
            boolean add = false;
            AntigenSeries antigenSeries = patientSeries.getTrackedAntigenSeries();
            SeriesType seriesType = antigenSeries.getSeriesType();
            String seriesPriority = antigenSeries.getSelectPatientSeries().getSeriesPriority();
            boolean atLeastOneValid = false;
            for (TargetDose targetDose : patientSeries.getTargetDoseList()) {
                if (targetDose.getTargetDoseStatus() == TargetDoseStatus.SATISFIED) {
                    Evaluation evaluation = targetDose.getEvaluation();
                    if (evaluation != null && evaluation.getEvaluationStatus() == EvaluationStatus.VALID) {
                        atLeastOneValid = true;
                        validDoseCount++;
                        break;
                    }
                }
            }
            String seriesName = antigenSeries.getSeriesName();
            String logString = "[" + seriesType + "] " + seriesName + " validDoseCount = " + validDoseCount;
            if (seriesType != null) {
                switch (seriesType) {
                    case RISK:
                        if (highestRiskPriority == null || highestRiskPriority.equals(seriesPriority)) {
                            add = true;
                        }
                        break;
                    case STANDARD:
                        if (atLeastOneValid) {
                            add = true;
                        }
                        break;
                    case EVALUATION_ONLY:
                        if (patientSeries.getPatientSeriesStatus() == PatientSeriesStatus.COMPLETE) {
                            add = true;
                        }
                        break;
                }
            }
            if (add) {
                scorablePatientSeriesList.add(patientSeries);
                if (antigenSeries.getSelectPatientSeries().getDefaultSeries() == YesNo.YES) {
                    addedDefault = true;
                }
                log(" + " + logString);
            } else {
                log(" - " + logString);
            }
        }
        log("Added default: " + addedDefault);
        log("Valid dose count: " + validDoseCount);
        if (!addedDefault && validDoseCount == 0) {
            log("Need to add default series");
            for (PatientSeries patientSeries : candidatePatientSeriesList) {
                AntigenSeries antigenSeries = patientSeries.getTrackedAntigenSeries();
                SeriesType seriesType = antigenSeries.getSeriesType();
                YesNo seriesDefault = antigenSeries.getSelectPatientSeries().getDefaultSeries();
                if (seriesDefault == YesNo.YES && seriesType == SeriesType.STANDARD) {
                    scorablePatientSeriesList.add(patientSeries);
                    String seriesName = antigenSeries.getSeriesName();
                    String logString = "[" + seriesType + "] " + seriesName + " validDoseCount = " + validDoseCount;
                    log(" + " + logString);
                }
            }
        }
        // log size of relevant patient series list
        log("Final number of relevant patient series: " + scorablePatientSeriesList.size());

        setNextLogicStepType(LogicStepType.IDENTIFY_ONE_PRIORITIZED_PATIENT_SERIES);
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
        printPatientSeriesList(out);
        printBestPatientSeries(out);
    }

}

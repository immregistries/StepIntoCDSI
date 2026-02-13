package org.openimmunizationsoftware.cdsi.core.logic.businessRules;

import java.text.SimpleDateFormat;
import java.util.Date;

import org.openimmunizationsoftware.cdsi.core.data.DataModel;
import org.openimmunizationsoftware.cdsi.core.domain.ConditionalSkipCondition;
import org.openimmunizationsoftware.cdsi.core.domain.DoseType;
import org.openimmunizationsoftware.cdsi.core.domain.Evaluation;
import org.openimmunizationsoftware.cdsi.core.domain.VaccineDoseAdministered;
import org.openimmunizationsoftware.cdsi.core.domain.VaccineType;
import org.openimmunizationsoftware.cdsi.core.domain.datatypes.EvaluationStatus;
import org.openimmunizationsoftware.cdsi.core.domain.datatypes.TimePeriod;

public class CONDSKIP_1
        extends org.openimmunizationsoftware.cdsi.core.logic.items.BusinessRule<Integer, ConditionalSkipCondition> {

    @Override
    public Integer evaluate(DataModel dataModel,
            ConditionalSkipCondition conditionalSkipCondition) {
        log("Evaluating CONDSKIP_1");
        log("  + " + dataModel.getImmunizationHistory()
                .getVaccineDoseAdministeredList().size() + " administered doses to evaluate");
        int count = 0;
        for (VaccineDoseAdministered vaccineDoseAdministered : dataModel.getImmunizationHistory()
                .getVaccineDoseAdministeredList()) {
            VaccineType vaccineType = vaccineDoseAdministered.getVaccine().getVaccineType();
            log("  + Looking at vaccine type: " + vaccineType);
            log("    - ConditionalSkipCondition vaccine types: "
                    + conditionalSkipCondition.getVaccineTypeSet());
            if (conditionalSkipCondition.getVaccineTypeSet().size() == 0
                    || conditionalSkipCondition.getVaccineTypeSet().contains(vaccineType)) {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
                boolean inRangeForAge = false;
                boolean inRangeForDate = false;
                Date dateAdministered = vaccineDoseAdministered.getDateAdministered();
                log("Looking at administered dose: " + vaccineType + " " + sdf.format(dateAdministered));
                {
                    TimePeriod beginAge = conditionalSkipCondition.getBeginAge();
                    TimePeriod endAge = conditionalSkipCondition.getEndAge();
                    Date beginAgeDate = null;
                    Date endAgeDate = null;
                    if (beginAge != null && beginAge.isValued()) {
                        beginAgeDate = beginAge.getDateFrom(dataModel.getPatient().getDateOfBirth());
                        log("  + beginAgeDate = " + sdf.format(beginAgeDate));
                    }
                    if (endAge != null && endAge.isValued()) {
                        endAgeDate = endAge.getDateFrom(dataModel.getPatient().getDateOfBirth());
                        log("  + endAgeDate = " + sdf.format(endAgeDate));
                    }
                    log("   --> On or after: " + onOrAfter(dateAdministered, beginAgeDate));
                    log("       +  dateAdministered  " + dateAdministered);
                    log("       +  beginAgeDate      " + beginAgeDate);
                    log("   --> Before: " + onOrAfter(dateAdministered, endAgeDate));
                    log("       +  dateAdministered  " + dateAdministered);
                    log("       +  endAgeDate      " + endAgeDate);
                    if (onOrAfter(dateAdministered, beginAgeDate)
                            && before(dateAdministered, endAgeDate)) {
                        inRangeForAge = true;
                        log("  + in range by age");
                    } else {
                        log("  + not in range by age");
                    }
                }
                {
                    Date startDate = conditionalSkipCondition.getStartDate();
                    log("  + startDate = " + (startDate == null ? "null" : sdf.format(startDate)));
                    Date endDate = conditionalSkipCondition.getEndDate();
                    log("  + endDate = " + (endDate == null ? "null" : sdf.format(endDate)));
                    if (onOrAfter(dateAdministered, startDate) && before(dateAdministered, endDate)) {
                        inRangeForDate = true;
                        log("  + in range by date");
                    } else {
                        log("  + not in range by date");
                    }
                }
                if (inRangeForAge && inRangeForDate) {
                    if (vaccineDoseAdministered.getTargetDose() == null) {
                        log("  + not counting dose with no target dose");
                        continue;
                    }
                    Evaluation evaluation = vaccineDoseAdministered.getTargetDose().getEvaluation();
                    if (evaluation != null && evaluation.getEvaluationStatus() != null) {
                        log("  + Skip dose type = " + conditionalSkipCondition.getDoseType());
                        if (conditionalSkipCondition.getDoseType() == DoseType.TOTAL) {
                            count++;
                            log("  + Counting any type of dose, regardless of evaluation status");
                        } else {
                            if (conditionalSkipCondition.getDoseType() == DoseType.VALID) {
                                log("  + dose evaluation status = " + evaluation.getEvaluationStatus());
                                if (evaluation.getEvaluationStatus() == EvaluationStatus.VALID) {
                                    count++;
                                    log("  + Counting because dose is valid");
                                }
                            }
                        }
                    }
                }
            }
        }
        log("  --> CONDSKIP_1 count = " + count);
        return count;
    }

    private static boolean onOrAfter(Date date, Date refDate) {
        return refDate == null || (date != null && !date.before(refDate));
    }

    private static boolean before(Date date, Date refDate) {
        return refDate == null || (date != null && date.before(refDate));
    }

}

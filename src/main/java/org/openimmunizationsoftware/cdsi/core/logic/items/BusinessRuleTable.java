package org.openimmunizationsoftware.cdsi.core.logic.items;

import java.util.Date;

import org.openimmunizationsoftware.cdsi.core.data.DataModel;
import org.openimmunizationsoftware.cdsi.core.domain.Antigen;
import org.openimmunizationsoftware.cdsi.core.domain.ConditionalSkipCondition;
import org.openimmunizationsoftware.cdsi.core.domain.DoseType;
import org.openimmunizationsoftware.cdsi.core.domain.Evaluation;
import org.openimmunizationsoftware.cdsi.core.domain.VaccineDoseAdministered;
import org.openimmunizationsoftware.cdsi.core.domain.datatypes.EvaluationStatus;
import org.openimmunizationsoftware.cdsi.core.domain.datatypes.TimePeriod;

/**
 * Created by Eric on 7/1/16.
 */
public class BusinessRuleTable {
  public static BusinessRule<Integer, ConditionalSkipCondition> CONDSKIP_1 = null;

  static {
    CONDSKIP_1 = new BusinessRule<Integer, ConditionalSkipCondition>() {
      @Override
      public Integer evaluate(DataModel dataModel,
          ConditionalSkipCondition conditionalSkipCondition) {
        int count = 0;
        for (VaccineDoseAdministered vaccineDoseAdministered : dataModel.getImmunizationHistory()
            .getVaccineDoseAdministeredList()) {
          if (conditionalSkipCondition.getVaccineTypeSet()
              .contains(vaccineDoseAdministered.getVaccine().getVaccineType())) {
            boolean inRange = false;
            Date dateAdministered = vaccineDoseAdministered.getDateAdministered();
            {
              TimePeriod beginAge = conditionalSkipCondition.getBeginAge();
              TimePeriod endAge = conditionalSkipCondition.getEndAge();
              if (beginAge != null && beginAge.isValued() && endAge != null && endAge.isValued()) {
                Date beginAgeDate = beginAge.getDateFrom(dataModel.getPatient().getDateOfBirth());
                Date endAgeDate = beginAge.getDateFrom(dataModel.getPatient().getDateOfBirth());
                if (onOrAfter(dateAdministered, beginAgeDate)
                    && before(dateAdministered, endAgeDate)) {
                  inRange = true;
                }
              }
            }
            {
              Date startDate = conditionalSkipCondition.getStartDate();
              Date endDate = conditionalSkipCondition.getEndDate();
              if (onOrAfter(dateAdministered, startDate) && before(dateAdministered, endDate)) {
                inRange = true;
              }
            }
            if (inRange) {
              Antigen antigen =
                  conditionalSkipCondition.getSeriesDose().getAntigenSeries().getTargetDisease();
              Evaluation evaluation = vaccineDoseAdministered.getEvaluationMap().get(antigen);
              if (evaluation != null && evaluation.getEvaluationStatus() != null) {
                if (conditionalSkipCondition.getDoseType() == DoseType.VALID
                    && evaluation.getEvaluationStatus() == EvaluationStatus.VALID
                    && conditionalSkipCondition.getDoseType() == DoseType.TOTAL) {
                  count++;
                }
              }

            }
          }
        }
        return count;
      }
    };
    CONDSKIP_1.setBusinessRuleId("CONDSKIP-1");
    CONDSKIP_1.setTerm("Number of Conditional Doses Administered");
    CONDSKIP_1.setBusinessRuleText(
        "The Number of Conditional Doses Administered must be computed as the count of vaccine doses administered where all of the following are true:"
            + "<ul><li>a. Vaccine Type is one of the supporting data defined conditional skip vaccine types.</li>"
            + "<li>b. Date Administered is:"
            + "<ul><li> on or after the conditional skip begin age date and\n"
            + "before the conditional skip end age date OR</li>"
            + "<li> on or after the conditional skip start date and before\n"
            + "conditional skip end date</li></ul> <li>c. Evaluation Status is:\n"
            + "<ul><li> \"Valid\" if the conditional skip dose type is \"Valid\" and<li>"
            + "<li> if the conditional skip dose type is \"Total\"</li></ul></li></ul>");
  }

  private static boolean onOrAfter(Date date, Date refDate) {
    return !date.before(refDate);
  }

  private static boolean before(Date date, Date refDate) {
    return date.before(refDate);
  }
}

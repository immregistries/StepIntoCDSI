package org.openimmunizationsoftware.cdsi.core.logic.items;

import org.openimmunizationsoftware.cdsi.core.data.DataModel;
import org.openimmunizationsoftware.cdsi.core.domain.ConditionalSkipCondition;
import org.openimmunizationsoftware.cdsi.core.domain.VaccineDoseAdministered;

/**
 * Created by Eric on 7/1/16.
 */
public class BusinessRuleTable {
    public static BusinessRule<Integer, ConditionalSkipCondition> CONDSKIP_1 = null;

    static {
        CONDSKIP_1 = new BusinessRule<Integer, ConditionalSkipCondition>() {
            @Override
            public Integer evaluate(DataModel dataModel, ConditionalSkipCondition conditionalSkipCondition) {
                int count = 0;
                for (VaccineDoseAdministered vaccineDoseAdministered : dataModel.getImmunizationHistory().getVaccineDoseAdministeredList()) {

                }

                return count;
            }
        };
        CONDSKIP_1.setBusinessRuleId("CONDSKIP-1");
        CONDSKIP_1.setTerm("Number of Conditional Doses Administered");
        CONDSKIP_1.setBusinessRuleText("The Number of Conditional Doses Administered must be computed as the count of vaccine doses administered where all of the following are true:" +
                "<ul><li>a. Vaccine Type is one of the supporting data defined conditional skip vaccine types.</li>" +
                "<li>b. Date Administered is:" +
                "<ul><li> on or after the conditional skip begin age date and\n" +
                "before the conditional skip end age date OR</li>" +
                "<li> on or after the conditional skip start date and before\n" +
                "conditional skip end date</li></ul> <li>c. Evaluation Status is:\n" +
                "<ul><li> \"Valid\" if the conditional skip dose type is \"Valid\" OR<li>" +
                "<li> of any status if the conditional skip dose type is \"Total\"</li></ul></li></ul>");
    }
}

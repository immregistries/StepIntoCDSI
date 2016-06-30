package org.openimmunizationsoftware.cdsi.core.domain;

import java.util.ArrayList;
import java.util.List;

public class ConditionalSkipSet {
	private int setId = 0;
	private String setDescription = "";
	private String conditionLogic = "";
	private List<ConditionalSkipCondition> conditionList = new ArrayList<ConditionalSkipCondition>();

	public int getSetId() {
		return setId;
	}

	public void setSetId(int setId) {
		this.setId = setId;
	}

	public String getSetDescription() {
		return setDescription;
	}

	public void setSetDescription(String setDescription) {
		this.setDescription = setDescription;
	}

	public String getConditionLogic() {
		return conditionLogic;
	}

	public void setConditionLogic(String conditionLogic) {
		this.conditionLogic = conditionLogic;
	}

	public List<ConditionalSkipCondition> getConditionList() {
		return conditionList;
	}

}

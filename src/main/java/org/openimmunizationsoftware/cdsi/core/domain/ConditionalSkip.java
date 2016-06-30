package org.openimmunizationsoftware.cdsi.core.domain;

import java.util.ArrayList;
import java.util.List;

public class ConditionalSkip {
	private String setLogic = "";
	private List<ConditionalSkipSet> conditionalSkipSetList = new ArrayList<ConditionalSkipSet>();

	public String getSetLogic() {
		return setLogic;
	}

	public void setSetLogic(String setLogic) {
		this.setLogic = setLogic;
	}

	public List<ConditionalSkipSet> getConditionalSkipSetList() {
		return conditionalSkipSetList;
	}
}

package org.openimmunizationsoftware.cdsi.core.logic;

import java.io.PrintWriter;
import java.util.ArrayList;

import org.openimmunizationsoftware.cdsi.core.data.DataModel;
import org.openimmunizationsoftware.cdsi.core.domain.Antigen;
import org.openimmunizationsoftware.cdsi.core.domain.AntigenSeries;

public class SelectBestPatientSeries extends LogicStep {
	public SelectBestPatientSeries(DataModel dataModel) {
		super(LogicStepType.SELECT_BEST_PATIENT_SERIES, dataModel);
	}

	@Override
	public LogicStep process() {
		dataModel.incAntigenPos();
		;
		if (dataModel.getAntigenPos() < dataModel.getAntigenList().size()) {
			Antigen antigen = dataModel.getAntigenList().get(dataModel.getAntigenPos());
			dataModel.setAntigen(dataModel.getAntigenList().get(dataModel.getAntigenPos()));
			ArrayList<AntigenSeries> antigenSeriesSelectedList = new ArrayList<AntigenSeries>();
			for (AntigenSeries antigenSeries : dataModel.getAntigenSeriesList()) {
				if (antigenSeries.getTargetDisease().equals(antigen)) {
					antigenSeriesSelectedList.add(antigenSeries);
				}
			}
			dataModel.setAntigenSeriesSelectedList(antigenSeriesSelectedList);
			setNextLogicStepType(LogicStepType.ONE_BEST_PATIENT_SERIES);
		} else {
			dataModel.setAntigen(null);
			dataModel.setAntigenSeriesSelectedList(null);
			setNextLogicStepType(LogicStepType.IDENTIFY_AND_EVALUATE_VACCINE_GROUP);
		}

		return next();
	}

	@Override
	public void printPre(PrintWriter out) throws Exception {
		// out.println("<h1>8.5 Select Best Patient Series</h1>");
		printStandard(out);

	}

	@Override
	public void printPost(PrintWriter out) {
		// out.println("<h1>8.5 Select Best Patient Series</h1>");
		printStandard(out);

	}

	private void printStandard(PrintWriter out) {
		out.println("<h1> " + getTitle() + "</h1>");
		out.println(
				"<p>Select Best  Patient Series  determines the best path to immunity (patient series) for the patient based on the evaluated immunization history and forecast. Each antigen evaluated and forecasted may contain more than one  patient series  and the goal  of  select best  patient series  is to select one of those  patient series  as being superior  to  the  others  based  on  several  factors.   </p>");
	}

}

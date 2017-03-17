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
	
	public void printTableAndFigures(PrintWriter out) {
		out.println("<p>Select best patient series involves reviewing all potential patient series which might satisfy the goals of an antigen and determining the one series which best fits the patient based on several important factors. The four steps of this process are listed in table 6-1.</p>");
		out.println("<p>TABLE 6 - 1 SELECT BEST PATIENT SERIES PROCESS STEPS</p>");
		out.println("<table>");
		out.println("	<tr>");
		out.println("		<th>Section</th>");
		out.println("		<th>Activity</th>");
		out.println("		<th>Goal</th>");
		out.println("	</tr>");
		out.println("	<tr>");
		out.println("		<td>6.2</td>");
		out.println("		<td>Identify Superior Patient Series </td>");
		out.println("		<td>The goal of this step is to determine if one patient series is superior to the other entire patient series.</td>");
		out.println("	</tr>");
		out.println("	<tr>");
		out.println("		<td>6.3</td>");
		out.println("		<td>Classify Patient Series</td>");
		out.println("		<td>The goal of this step is to classify where the patient is in the overall  path to immunity and pass those candidate patient series onto the next step. Only those patient series with the most likely chance to be considered the best are retained for further consideration.</td>");
		out.println("	</tr>");
		out.println("	<tr>");
		out.println("		<td>6.4-6.6</td>");
		out.println("		<td>Scoring Patient Series</td>");
		out.println("		<td>The goal of this step is to apply the proper scoring business rules based on results of the second step. The scoring business rules will determine the best patient series. Scoring business rules are specific to where the patient is in the overall path to immunity. The complete patient series scoring business rules look at factors important when candidate patient series are complete. Similarly in-process patient series scoring business rules and no valid doses scoring business rules look at factors important to their respective situation. For any given antigen, only one set of these scoring business rules will be applied to each candidate patient series.</td>");
		out.println("	</tr>");
		out.println("	<tr>");
		out.println("		<td>6.7</td>");
		out.println("		<td>Select Best Patient Series</td>");
		out.println("		<td>The goal of this step is to evaluate the scored candidate patient series and determine which of the candidate patient series is the one and only best patient series.</td>");
		out.println("	</tr>");
		out.println("</table>");
		out.println("<p>The process model below illustrates the major steps involved in selecting the best patient series.</p>");
		out.println("<img src=\"Figure 6.1.png\"/>");
		out.println("<p>FIGURE 6 - 1 SELECT BEST PATIENT SERIES PROCESS MODEL</p>");
		
		out.println("-->Patient series list size : "+dataModel.getPatientSeriesList().size());
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
		printTableAndFigures(out);
	}

}

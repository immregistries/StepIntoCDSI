package org.openimmunizationsoftware.cdsi.servlet;

import java.io.IOException;
import java.io.PrintWriter;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.openimmunizationsoftware.cdsi.SoftwareVersion;
import org.openimmunizationsoftware.cdsi.core.data.DataModel;
import org.openimmunizationsoftware.cdsi.core.data.DataModelLoader;
import org.openimmunizationsoftware.cdsi.core.domain.AntigenAdministeredRecord;
import org.openimmunizationsoftware.cdsi.core.domain.Forecast;
import org.openimmunizationsoftware.cdsi.core.domain.VaccineDoseAdministered;
import org.openimmunizationsoftware.cdsi.core.domain.VaccineGroupForecast;
import org.openimmunizationsoftware.cdsi.core.logic.LogicStepFactory;
import org.openimmunizationsoftware.cdsi.core.logic.LogicStepType;

public class ForecastServlet extends HttpServlet {

  public static final String RESULT_FORMAT_TEXT = "text";
  public static final String RESULT_FORMAT_HTML = "html";
  public static final String RESULT_FORMAT_COMPACT = "compact";

  public static final String PARAM_RESULT_FORMAT = "resultFormat";

  protected static final String SCHEDULE_NAME_DEFAULT = "default";

  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
    try {
      DataModel dataModel = readRequest(req);
      process(dataModel);
      PrintWriter out = new PrintWriter(resp.getOutputStream());
      String resultFormat = req.getParameter(PARAM_RESULT_FORMAT);
      if (resultFormat == null) {
        resultFormat = RESULT_FORMAT_TEXT;
      }
      if (resultFormat.equals(RESULT_FORMAT_HTML)) {
        resp.setContentType("text/plain");
        out.println("not implemented yet");
      } else if (resultFormat.equals(RESULT_FORMAT_COMPACT)) {
        resp.setContentType("text/plain");
        out.println("not implemented yet");
      } else {
        printText(resp, dataModel, out);
      }
      out.close();
    } catch (Exception e) {
      e.printStackTrace();
      throw new ServletException(e);
    }
  }

  private void printText(HttpServletResponse resp, DataModel dataModel, PrintWriter out) {
    resp.setContentType("text/plain");
    out.println("Step Into Clinical Decision Support for Immunizations - Demonstration //System");
    out.println();
    SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy");

    VaccineGroupForecast vgf = dataModel.getVaccineGroupForecast();
    List<Forecast> fl = vgf.getForecastList();
    ////System.err.println(fl.size());
    
/*    for(int i=0;i<fl.size();i++){
    	//System.out.println("AntigenName:" +fl.get(i).getAntigen());
    	//System.out.println("ForecastReason:" +fl.get(i).getForecastReason());
    }
    */
    ////System.out.println("***************************************************************************************");
    for(Forecast fln:fl){
    	    //System.out.println("Antigen :"+fln.getAntigen()+" | "+" ForecastReason: "+fln.getForecastReason());
    }

    //System.out.println("***************************************************************************************");


    List<Forecast> flNow = new ArrayList<Forecast>();
    List<Forecast> flLater = new ArrayList<Forecast>();
    List<Forecast> flDone = new ArrayList<Forecast>();
        
    Date today = new Date();
    try {
      today = sdf.parse(sdf.format(today));
      ////System.out.println(today);
    } catch (ParseException pe) {
      pe.printStackTrace();
    }
    {
      for (Forecast f : fl) {
        if (f.getForecastReason().equals("")) {
          if (f.getAdjustedRecommendedDate().after(today)) {
            flLater.add(f);
          } else {
            flNow.add(f);
          }
        } else {
          flDone.add(f);
        }
      }
    }
    
    // out.println("fl :"+fl.size());
    printList(dataModel, out, sdf, today, flNow, "VACCINATIONS RECOMMENDED");
    //out.println("flNow"+flNow.size());
    printList(dataModel, out, sdf, today, flLater, "VACCINATIONS RECOMMENDED AFTER");
    //out.println("flLater :"+flLater.size());
    printList(dataModel, out, sdf, today, flDone, "VACCINATIONS COMPLETE OR NOT RECOMMENDED");
    //out.println("flDone :"+flDone.size());

    if (dataModel.getAntigenAdministeredRecordList().size() > 0) {
      out.println("IMMUNIZATION EVALUATION");
      int count = 0;

      for (VaccineDoseAdministered vda : dataModel.getImmunizationHistory().getVaccineDoseAdministeredList()) {
        count++;
        for (AntigenAdministeredRecord aar : dataModel.getAntigenAdministeredRecordList()) {
          if (aar.getVaccineType().equals(vda.getVaccine().getVaccineType())
              && aar.getDateAdministered().equals(vda.getDateAdministered())) {
            out.print("Vaccination #" + count + ": " + aar.getVaccineType().getShortDescription() + " given "
                + sdf.format(aar.getDateAdministered()));
            // is a valid Hib dose 1. Dose 1 valid at 6 weeks of age,
            // 10/14/2012.
            out.println();
          }
        }
      }
      out.println();
    }

    out.println(
        "Forecast generated " + sdf.format(new Date()) + " using software version " + SoftwareVersion.VERSION + ".");
  }
  
  private static HashMap<String, String> NAME_MAP = new HashMap<String, String>();
  static 
  {
	  NAME_MAP.put("Hep A", "HepA");
	  NAME_MAP.put("HepB", "HepB");
	  NAME_MAP.put("Hib", "Hib");
	  NAME_MAP.put("Influenza", "");
	  NAME_MAP.put("", "");
	  NAME_MAP.put("", "");
  }
  
  private static Map<String, String> mapLabelOut = new HashMap<String, String>();
  
  private void printList(DataModel dataModel, PrintWriter out, SimpleDateFormat sdf, Date today,
      List<Forecast> forecastList, String title) {
    if (forecastList.size() > 0) {
      out.println(title + " " + sdf.format(dataModel.getAssessmentDate()));
      for (Forecast forecast : forecastList) {
        if (forecast.getAntigen() != null) {
          String name = forecast.getAntigen().getName();
          if (name.equals("Diphtheria"))
          {
            // < 7 years recommend Dtap,
            // >= 7 years recommend Tdap
            Calendar c = Calendar.getInstance();
            c.add(Calendar.YEAR, -7);
            
            if (dataModel.getPatient().getDateOfBirth().after(c.getTime()))
            {
              name = "DTaP";
            }
            else
            {
              name = "Tdap"; // could be TD
            }
          }
          out.print("Forecasting " + name + " status ");
          if (forecast.getForecastReason().equals("")) {
            if (forecast.getAdjustedRecommendedDate().after(today)) {
              out.print("due later ");
            } else {
              out.print("due ");
            }
            out.print("dose ");
            if (forecast.getTargetDose() == null) {
              out.print("? ");
            } else {
              out.print(forecast.getTargetDose().getTrackedSeriesDose().getDoseNumber() + " ");
            }
            if (forecast.getAdjustedRecommendedDate() != null) {
              out.print("due ");
              out.print(sdf.format(forecast.getAdjustedRecommendedDate()));
              out.print(" ");
              if (forecast.getEarliestDate() != null) {
                out.print("valid ");
                out.print(sdf.format(forecast.getEarliestDate()));
                out.print(" ");
                if (forecast.getAdjustedPastDueDate() != null) {
                  out.print("overdue ");
                  out.print(sdf.format(forecast.getAdjustedPastDueDate()));
                  out.print(" ");
                  if (forecast.getLatestDate() != null) {
                    out.print("finished ");
                    out.print(sdf.format(forecast.getLatestDate()));
                  }
                }
              }
            }
          } else {
            out.print(forecast.getForecastReason());
          }
          out.println();
        }
      }
      out.println();
    }
  }

  private void process(DataModel dataModel) throws Exception {
    int count = 0;
    while (dataModel.getLogicStep().getLogicStepType() != LogicStepType.END) {
      dataModel.setNextLogicStep(dataModel.getLogicStep().process());
      count++;
      if (count > 100000) {
        System.err.println("Appear to be caught in a loop at this step: " + dataModel.getLogicStep().getTitle());
        // too many steps!
        if (count > 100100) {
          throw new RuntimeException("Logic steps seem to be caught in a loop, unable to get results");
        }
      }
    }
  }

  protected DataModel readRequest(HttpServletRequest req) throws Exception {
    DataModel dataModel = DataModelLoader.createDataModel();
    dataModel.setRequest(req);
    dataModel.setNextLogicStep(LogicStepFactory.createLogicStep(LogicStepType.GATHER_NECESSARY_DATA, dataModel));
    return dataModel;
  }
}

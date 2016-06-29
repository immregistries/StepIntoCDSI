package org.openimmunizationsoftware.cdsi.servlet;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.openimmunizationsoftware.cdsi.core.data.DataModel;
import org.openimmunizationsoftware.cdsi.core.data.DataModelLoader;
import org.openimmunizationsoftware.cdsi.core.logic.LogicStepFactory;
import org.openimmunizationsoftware.cdsi.core.logic.LogicStepType;

public class ForecastServlet extends HttpServlet
{

  public static final String RESULT_FORMAT_TEXT = "text";
  public static final String RESULT_FORMAT_HTML = "html";
  public static final String RESULT_FORMAT_COMPACT = "compact";

  private static final String CONDITION_CODE_SUB_POTENT = "S";
  private static final String CONDITION_CODE_FORCE_VALID = "F";

  protected static final String SCHEDULE_NAME_DEFAULT = "default";

  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
    try {
      readRequest(req);
    } catch (Exception e) {
      e.printStackTrace();
      throw new ServletException(e);
    }
  }

  protected DataModel readRequest(HttpServletRequest req) throws Exception {
    DataModel dataModel = DataModelLoader.createDataModel();
    dataModel.setRequest(req);
    dataModel.setNextLogicStep(LogicStepFactory.createLogicStep(LogicStepType.GATHER_NECESSARY_DATA, dataModel));
    return dataModel;
  }
}

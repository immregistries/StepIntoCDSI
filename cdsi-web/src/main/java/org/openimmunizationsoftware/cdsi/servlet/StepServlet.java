package org.openimmunizationsoftware.cdsi.servlet;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.net.URLEncoder;
import java.io.UnsupportedEncodingException;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import org.openimmunizationsoftware.cdsi.SoftwareVersion;
import org.openimmunizationsoftware.cdsi.auth.AuthPageRenderer;
import org.openimmunizationsoftware.cdsi.core.data.DataModel;
import org.openimmunizationsoftware.cdsi.core.domain.AntigenAdministeredRecord;
import org.openimmunizationsoftware.cdsi.core.domain.AntigenSeries;
import org.openimmunizationsoftware.cdsi.core.domain.Evaluation;
import org.openimmunizationsoftware.cdsi.core.domain.Forecast;
import org.openimmunizationsoftware.cdsi.core.domain.PatientSeries;
import org.openimmunizationsoftware.cdsi.core.domain.TargetDose;
import org.openimmunizationsoftware.cdsi.core.domain.VaccineDoseAdministered;
import org.openimmunizationsoftware.cdsi.core.domain.VaccineGroupForecast;
import org.openimmunizationsoftware.cdsi.core.domain.VaccineType;
import org.openimmunizationsoftware.cdsi.core.logic.LogicStep;
import org.openimmunizationsoftware.cdsi.core.logic.LogicStepType;
import org.openimmunizationsoftware.cdsi.servlet.render.LogicStepRenderer;

public class StepServlet extends ForecastServlet {

  private static final String SESSION_SUPPORTING_DATA_SET = "stepSupportingDataSet";
  private static final String SESSION_STEP_SNAPSHOTS = "stepSnapshots";
  private static final String SESSION_STEP_SNAPSHOT_COUNTER = "stepSnapshotCounter";
  private static final int MAX_SNAPSHOTS_PER_SESSION = 200;

  static List<StepExample> stepExamples = null;
  static int stepExampleStartCount = 0;
  static {
    stepExamples = new ArrayList<StepExample>();
    {
      StepExample stepExample = new StepExample(
          "Original Hib",
          "evalDate=20160630&scheduleName=default&resultFormat=text&patientDob=20150630&patientSex=F&vaccineDate1=20150826&vaccineCvx1=47&vaccineMvx1=&vaccineDate2=20151027&vaccineCvx2=47&vaccineMvx2=&vaccineDate3=20151229&vaccineCvx3=47&vaccineMvx3=");
      stepExamples.add(stepExample);
    }
    {
      StepExample stepExample = new StepExample(
          "Original Complete Record",
          "evalDate=20140515&scheduleName=default&resultFormat=text&patientDob=20051215&patientSex=M&vaccineDate1=20060213&vaccineCvx1=10&vaccineMvx1=&vaccineDate2=20060214&vaccineCvx2=100&vaccineMvx2=&vaccineDate3=20060420&vaccineCvx3=10&vaccineMvx3=&vaccineDate4=20060420&vaccineCvx4=20&vaccineMvx4=&vaccineDate5=20060420&vaccineCvx5=17&vaccineMvx5=&vaccineDate6=20060616&vaccineCvx6=17&vaccineMvx6=&vaccineDate7=20060616&vaccineCvx7=10&vaccineMvx7=&vaccineDate8=20060616&vaccineCvx8=08&vaccineMvx8=&vaccineDate9=20060616&vaccineCvx9=20&vaccineMvx9=&vaccineDate10=20060616&vaccineCvx10=100&vaccineMvx10=&vaccineDate11=20060929&vaccineCvx11=08&vaccineMvx11=&vaccineDate12=20060929&vaccineCvx12=100&vaccineMvx12=&vaccineDate13=20061213&vaccineCvx13=20&vaccineMvx13=&vaccineDate14=20061215&vaccineCvx14=08&vaccineMvx14=&vaccineDate15=20061215&vaccineCvx15=85&vaccineMvx15=&vaccineDate16=20061215&vaccineCvx16=03&vaccineMvx16=&vaccineDate17=20061215&vaccineCvx17=21&vaccineMvx17=&vaccineDate18=20071105&vaccineCvx18=17&vaccineMvx18=&vaccineDate19=20071105&vaccineCvx19=20&vaccineMvx19=&vaccineDate20=20071105&vaccineCvx20=10&vaccineMvx20=&vaccineDate21=20080110&vaccineCvx21=85&vaccineMvx21=&vaccineDate22=20080110&vaccineCvx22=100&vaccineMvx22=&vaccineDate23=20140515&vaccineCvx23=21&vaccineMvx23=&vaccineDate24=20140515&vaccineCvx24=139&vaccineMvx24=&vaccineDate25=20140515&vaccineCvx25=10&vaccineMvx25=&vaccineDate26=20140515&vaccineCvx26=03&vaccineMvx26=");
      stepExamples.add(stepExample);
    }
    stepExampleStartCount = stepExamples.size();
  }

  private static final int MAX_STEP_EXAMPLES = 100;

  public static void registerRequest(HttpServletRequest req) {
    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    String label = req.getParameter("Received forecast " + sdf.format(new java.util.Date()));
    String requestString = req.getQueryString();
    StepExample stepExample = new StepExample(label, requestString);
    synchronized (stepExamples) {
      stepExamples.add(stepExampleStartCount, stepExample);
      if (stepExamples.size() > stepExampleStartCount + MAX_STEP_EXAMPLES) {
        stepExamples.remove(stepExampleStartCount + MAX_STEP_EXAMPLES);
      }
    }
  }

  @Override
  protected void doPost(HttpServletRequest req, HttpServletResponse resp)
      throws ServletException, IOException {
    doGet(req, resp);
  }

  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse resp)
      throws ServletException, IOException {

    req = new DateNormalizingRequest(req);

    HttpSession session = req.getSession(true);

    // Remove forced selection - now uses default if not specified
    String selectedSupportingDataSet = req.getParameter(PARAM_SUPPORTING_DATA_SET);
    if (selectedSupportingDataSet != null && !selectedSupportingDataSet.trim().equals("")) {
      session.setAttribute(SESSION_SUPPORTING_DATA_SET,
          SupportingDataManager.normalizeSetId(selectedSupportingDataSet));
    }

    DataModel dataModel = null;
    String action = req.getParameter("action");
    boolean nextRequested = action != null && action.equals("next");
    String viewParam = req.getParameter("view");
    boolean viewingSavedOrCurrent = viewParam != null && !viewParam.trim().equals("");
    if (nextRequested || viewingSavedOrCurrent) {
      dataModel = (DataModel) session.getAttribute("dataModel");
    }

    if (dataModel == null && isMissingRequiredStartInput(req)) {
      renderStartScreen(req, resp, nextRequested);
      return;
    }

    Exception exception = null;
    boolean processedUserAction = dataModel != null && !viewingSavedOrCurrent;

    if (dataModel == null) {
      try {
        dataModel = readRequest(req);
        session.setAttribute("dataModel", dataModel);
      } catch (Exception e) {
        e.printStackTrace();
        throw new ServletException(e);
      }
    } else if (viewingSavedOrCurrent) {
      // Read-only: inspecting a saved view (or the "current" marker) never advances or mutates
      // the live DataModel. Browser history should revisit cached report-outs, not roll back
      // the engine.
    } else {
      try {
        String submit = req.getParameter("submit");
        if (submit != null && submit.equals("Jump")) {
          String jumpTo = req.getParameter("jumpTo");
          jump(dataModel, jumpTo);
        } else if (submit != null && submit.equals("Jump4.4")) {
          String jumpTo = "Evaluate and Forecast all Patient Series";
          jump(dataModel, jumpTo);
        } else if (submit != null && submit.equals("Jump4.5")) {
          String jumpTo = "Select Best Patient Series";
          jump(dataModel, jumpTo);
        } else if (submit != null && submit.equals("Jump4.6")) {
          String jumpTo = "Identify and Evaluate Vaccine Group";
          jump(dataModel, jumpTo);
        } else if (submit != null && submit.equals("End")) {
          String jumpTo = "End";
          jump(dataModel, jumpTo);
        }
        if (dataModel.getLogicStep().getLogicStepType() != LogicStepType.END) {
          dataModel.setNextLogicStep(dataModel.getLogicStep().process());
        }
      } catch (Exception e) {
        e.printStackTrace();
        exception = e;
        dataModel.setLogicStepPrevious(dataModel.getLogicStep());
      }
    }

    StepSnapshot snapshot = null;
    if (processedUserAction && exception == null) {
      snapshot = captureSnapshot(session, dataModel, req);
    }

    StepSnapshot viewingSnapshot = null;
    if (viewingSavedOrCurrent && !"current".equals(viewParam)) {
      viewingSnapshot = findSnapshot(session, viewParam);
    }
    StepRenderState renderState = resolveRenderState(dataModel, req, viewingSnapshot);

    if ("json".equals(req.getParameter("format"))) {
      resp.setContentType("application/json;charset=UTF-8");
      PrintWriter jsonOut = new PrintWriter(resp.getOutputStream());
      jsonOut.print(buildStepJsonResponse(renderState, exception, snapshot, session));
      jsonOut.close();
      return;
    }

    resp.setContentType("text/html");

    PrintWriter out = new PrintWriter(resp.getOutputStream());
    out.println("<!DOCTYPE html PUBLIC \"-//W3C//DTD HTML 4.01//EN\">");
    out.println("<html>");
    out.println("  <head>");
    out.println("    <title>" + escapeHtml(renderState.title) + "</title>");
    out.println("    <link rel=\"stylesheet\" type=\"text/css\" href=\"indexStep.css\">");
    out.println("  </head>");
    out.println("  <body>");

    AuthPageRenderer.renderSignedInHeader(out, req);

    out.println("      <form action=\"step\" method=\"POST\" id=\"stepForm\">");
    String activeSupportingDataSet = resolveSupportingDataSet(req);
    if (activeSupportingDataSet != null && !activeSupportingDataSet.equals("")) {
      out.println("      <input type=\"hidden\" name=\"" + PARAM_SUPPORTING_DATA_SET + "\" value=\""
          + escapeHtml(activeSupportingDataSet) + "\"/>");
    }
    out.println("    <div class=\"cell\">");
    out.println(
        "  <a href=\"step\"><img src=\"Logo Large.png\" height=\"120\" align=\"left\"/></a>");
    out.println("Version " + SoftwareVersion.VERSION + "<br/>");
    out.println("  <a href=\"dataModelView\" target=\"dataModelView\">View Data Model</a><br/>");
    out.println("  <a href=\"fits\">FITS Test Cases</a>");
    out.println("<br/>Supporting Data Set: <strong>"
        + escapeHtml(activeSupportingDataSet == null ? "default" : activeSupportingDataSet)
        + "</strong>");
    out.println("<br clear=\"all\"/>");
    if (renderState.viewingSaved) {
      LogicStepType liveCurrentType = dataModel.getLogicStep() == null ? null : dataModel.getLogicStep().getLogicStepType();
      out.println("    <div id=\"savedViewBanner\" class=\"notice\">");
      out.println("      Viewing saved view #" + renderState.viewingSnapshotId
          + (renderState.transition == null ? "" : " (" + escapeHtml(renderState.transition.replace("-", " -> ")) + ")")
          + " &mdash; not the current engine position"
          + (liveCurrentType == null ? "" : " (currently at " + escapeHtml(liveCurrentType.getChapter()) + " "
              + escapeHtml(liveCurrentType.getName()) + ")")
          + ". <a href=\"#\" id=\"returnToCurrentLink\">Return to current step</a>");
      out.println("    </div>");
    }
    out.println("    <div id=\"stableSummaryPanel\">");
    if (renderState.viewingSaved) {
      out.print(renderState.stableSummaryHtml);
    } else if (exception != null) {
      StringWriter sw = new StringWriter();
      PrintWriter pw = new PrintWriter(sw);
      exception.printStackTrace(pw);

      out.println("<pre>");
      out.println(sw.toString());
      out.println("</pre>");
    } else {
      if (dataModel.getLogicStepPrevious() == null) {
        out.println("<h1>CDSi Demonstration System</h1> ");
        // make a table with two columns one with the step link and label and the other
        // with forecast
        out.println("<h2>Step Examples</h2>");
        out.println("<table>");
        out.println("  <tr>");
        out.println("    <th>Step</th>");
        out.println("    <th>Forecast</th>");
        out.println("  </tr>");
        List<StepExample> stepExamplesCopy = new ArrayList<StepExample>();
        synchronized (stepExamples) {
          stepExamplesCopy.addAll(stepExamples);
        }
        for (StepExample stepExample : stepExamples) {
          out.println("  <tr>");
          String stepLink = appendSupportingDataSet("step?" + stepExample.getRequestString(), activeSupportingDataSet);
          String forecastLink = appendSupportingDataSet("forecast?" + stepExample.getRequestString(),
              activeSupportingDataSet);
          out.println("      <td><a href=\"" + stepLink + "\">" + stepExample.getLabel() + "</a></td><td><a href=\""
              + forecastLink + "\">Forecast</a></td>");
          out.println("  </tr>");
        }
        out.println("</table>");
      } else {
        printStableView(dataModel, out);
      }
    }
    out.println("    </div>");
    out.println("    </div>");

    out.println("    <div class=\"cell\">");
    if (dataModel.getLogicStepPrevious() != null || req.getParameter(LogicStep.PARAM_EVAL_DATE) != null) {
      out.println("        <input type=\"submit\" name=\"submit\" value=\"Next Step\"/>");
      out.println("        <input type=\"submit\" name=\"submit\" value=\"End\"/>");
      out.println("        <input type=\"hidden\" name=\"action\" value=\"next\"/>");
      out.println("        <details class=\"jump-fallback\">");
      out.println("          <summary>Manual jump controls (fallback)</summary>");
      out.println("          <select id=\"jumpToSelect\" name=\"jumpTo\">");
      for (LogicStepType logicStepType : LogicStep.STEPS) {
        String display = logicStepType.getName();
        if (logicStepType.isIndent()) {
          display = " + " + display;
        }
        if (dataModel.getLogicStep().getLogicStepType() == logicStepType) {
          out.println("            <option value=\"" + logicStepType.getName() + "\" selected>"
              + display + "</option>");
        } else {
          out.println("            <option value=\"" + logicStepType.getName() + "\">" + display
              + "</option>");
        }
      }
      out.println("          </select>");
      out.println("          <input type=\"submit\" id=\"jumpSubmitBtn\" name=\"submit\" value=\"Jump\"/>");
      out.println("          <input type=\"submit\" name=\"submit\" value=\"Jump4.4\"/>");
      out.println("          <input type=\"submit\" name=\"submit\" value=\"Jump4.5\"/>");
      out.println("          <input type=\"submit\" name=\"submit\" value=\"Jump4.6\"/>");
      out.println("        </details>");
      out.println("        <br/>");
    }
    out.println("        <div class=\"process-map-wrap\">");
    try {
      out.println(renderProcessMapSvg(renderState.previousStepType, renderState.currentStepType));
    } catch (IOException e) {
      e.printStackTrace();
    }
    out.println("        </div>");
    List<StepSnapshot> traySnapshots = listSnapshots(session);
    if (!traySnapshots.isEmpty()) {
      out.println("        <div class=\"saved-views-tray\">");
      out.println("          <h3>Saved Views</h3>");
      out.println("          <ul id=\"savedViewsList\">");
      for (StepSnapshot traySnapshot : traySnapshots) {
        boolean isActive = renderState.viewingSaved && renderState.viewingSnapshotId != null
            && renderState.viewingSnapshotId == traySnapshot.getId();
        out.println("            <li><a href=\"?view=" + traySnapshot.getId() + "\" class=\"saved-view-link"
            + (isActive ? " is-active" : "") + "\" data-snapshot-id=\"" + traySnapshot.getId() + "\">"
            + escapeHtml(trayLabel(traySnapshot)) + "</a></li>");
      }
      out.println("          </ul>");
      out.println("        </div>");
    }
    if (dataModel.getLogicStepPrevious() != null || req.getParameter(LogicStep.PARAM_EVAL_DATE) != null) {
      out.println("        <script>");
      out.println("          (function() {");
      out.println("            function jumpToMapNode(stepName) {");
      out.println("              var select = document.getElementById('jumpToSelect');");
      out.println("              if (select) {");
      out.println("                var found = false;");
      out.println("                for (var i = 0; i < select.options.length; i++) {");
      out.println("                  if (select.options[i].value === stepName) { found = true; break; }");
      out.println("                }");
      out.println("                if (!found) {");
      out.println("                  var opt = document.createElement('option');");
      out.println("                  opt.value = stepName;");
      out.println("                  opt.textContent = stepName;");
      out.println("                  select.appendChild(opt);");
      out.println("                }");
      out.println("                select.value = stepName;");
      out.println("              }");
      out.println("              var jumpBtn = document.getElementById('jumpSubmitBtn');");
      out.println("              if (jumpBtn) { jumpBtn.click(); }");
      out.println("            }");
      out.println("            var wrap = document.querySelector('.process-map-wrap');");
      out.println("            if (wrap) {");
      out.println("              wrap.addEventListener('click', function(e) {");
      out.println("                var node = e.target.closest('.step-node');");
      out.println("                if (!node) { return; }");
      out.println("                var stepName = node.getAttribute('data-step-name');");
      out.println("                if (!stepName) { return; }");
      out.println("                jumpToMapNode(stepName);");
      out.println("              });");
      out.println("            }");
      out.println();
      out.println("            var form = document.getElementById('stepForm');");
      out.println("            if (!form) { return; }");
      out.println("            var bypassAjax = false;");
      out.println();
      out.println("            function setHtml(id, html) {");
      out.println("              var el = document.getElementById(id);");
      out.println("              if (el && html !== undefined && html !== null) { el.innerHTML = html; }");
      out.println("            }");
      out.println();
      out.println("            function updateMapState(mapState) {");
      out.println("              if (!mapState) { return; }");
      out.println("              var svg = document.querySelector('.process-map-wrap svg');");
      out.println("              if (!svg) { return; }");
      out.println(
          "              ['is-current', 'is-previous', 'is-active-edge'].forEach(function(cls) {");
      out.println("                svg.querySelectorAll('.' + cls).forEach(function(el) {");
      out.println("                  el.classList.remove(cls);");
      out.println("                });");
      out.println("              });");
      out.println("              [['current', 'is-current'], ['previous', 'is-previous'], ['activeEdge', 'is-active-edge']]"
          + ".forEach(function(pair) {");
      out.println("                var id = mapState[pair[0]];");
      out.println("                if (!id) { return; }");
      out.println("                var el = document.getElementById(id);");
      out.println("                if (el) { el.classList.add(pair[1]); }");
      out.println("              });");
      out.println("            }");
      out.println();
      out.println("            function buildFormData(submitter) {");
      out.println("              try {");
      out.println("                return new FormData(form, submitter || undefined);");
      out.println("              } catch (e) {");
      out.println("                var fd = new FormData(form);");
      out.println("                if (submitter && submitter.name) { fd.append(submitter.name, submitter.value); }");
      out.println("                return fd;");
      out.println("              }");
      out.println("            }");
      out.println();
      out.println("            function fallbackSubmit(submitter) {");
      out.println("              bypassAjax = true;");
      out.println("              form.requestSubmit(submitter);");
      out.println("            }");
      out.println();
      out.println("            function updateBanner(data) {");
      out.println("              var existing = document.getElementById('savedViewBanner');");
      out.println("              if (!data.viewingSaved) {");
      out.println("                if (existing) { existing.parentNode.removeChild(existing); }");
      out.println("                return;");
      out.println("              }");
      out.println("              var label = data.transition ? ' (' + data.transition.replace('-', ' -> ') + ')' : '';");
      out.println("              var html = 'Viewing saved view #' + data.viewingSnapshotId + label"
          + " + ' &mdash; not the current engine position. '"
          + " + '<a href=\"#\" id=\"returnToCurrentLink\">Return to current step</a>';");
      out.println("              if (existing) {");
      out.println("                existing.innerHTML = html;");
      out.println("              } else {");
      out.println("                var banner = document.createElement('div');");
      out.println("                banner.id = 'savedViewBanner';");
      out.println("                banner.className = 'notice';");
      out.println("                banner.innerHTML = html;");
      out.println("                var summary = document.getElementById('stableSummaryPanel');");
      out.println("                if (summary && summary.parentNode) { summary.parentNode.insertBefore(banner, summary); }");
      out.println("              }");
      out.println("            }");
      out.println();
      out.println("            function updateTrayActive(viewingSnapshotId) {");
      out.println("              document.querySelectorAll('.saved-view-link').forEach(function(link) {");
      out.println("                var isActive = viewingSnapshotId != null"
          + " && String(viewingSnapshotId) === link.getAttribute('data-snapshot-id');");
      out.println("                link.classList.toggle('is-active', isActive);");
      out.println("              });");
      out.println("            }");
      out.println();
      out.println("            function appendTrayEntry(entry) {");
      out.println("              if (!entry) { return; }");
      out.println("              var list = document.getElementById('savedViewsList');");
      out.println("              if (!list) {");
      out.println("                var wrap = document.querySelector('.process-map-wrap');");
      out.println("                if (!wrap || !wrap.parentNode) { return; }");
      out.println("                var tray = document.createElement('div');");
      out.println("                tray.className = 'saved-views-tray';");
      out.println("                tray.innerHTML = '<h3>Saved Views</h3><ul id=\"savedViewsList\"></ul>';");
      out.println("                wrap.parentNode.insertBefore(tray, wrap.nextSibling);");
      out.println("                list = document.getElementById('savedViewsList');");
      out.println("              }");
      out.println("              if (list.querySelector('[data-snapshot-id=\"' + entry.id + '\"]')) { return; }");
      out.println("              var li = document.createElement('li');");
      out.println("              var a = document.createElement('a');");
      out.println("              a.href = '?view=' + entry.id;");
      out.println("              a.className = 'saved-view-link';");
      out.println("              a.setAttribute('data-snapshot-id', entry.id);");
      out.println("              a.textContent = entry.label;");
      out.println("              li.appendChild(a);");
      out.println("              list.appendChild(li);");
      out.println("            }");
      out.println();
      out.println("            function applyState(data) {");
      out.println("              setHtml('stableSummaryPanel', data.stableSummaryHtml);");
      out.println("              setHtml('postLogPanel', (data.postHtml || '') + (data.logHtml || ''));");
      out.println("              setHtml('prePanel', data.preHtml);");
      out.println("              updateMapState(data.mapState);");
      out.println("              if (data.title) { document.title = data.title; }");
      out.println("              updateBanner(data);");
      out.println("              updateTrayActive(data.viewingSaved ? data.viewingSnapshotId : null);");
      out.println("              if (!data.viewingSaved && data.trayEntry) { appendTrayEntry(data.trayEntry); }");
      out.println("            }");
      out.println();
      out.println("            function fetchView(viewParam, pushUrl) {");
      out.println("              var url = form.getAttribute('action') + '?format=json&view=' + encodeURIComponent(viewParam);");
      out.println("              fetch(url)");
      out.println("                .then(function(resp) {");
      out.println("                  if (!resp.ok) { throw new Error('HTTP ' + resp.status); }");
      out.println("                  return resp.json();");
      out.println("                })");
      out.println("                .then(function(data) {");
      out.println("                  if (data.error) { throw new Error(data.error); }");
      out.println("                  applyState(data);");
      out.println("                  if (pushUrl) { history.pushState({ view: viewParam }, '', '?view=' + viewParam); }");
      out.println("                })");
      out.println("                .catch(function(err) {");
      out.println("                  console.error('Saved view fetch failed, reloading:', err);");
      out.println("                  location.href = '?view=' + encodeURIComponent(viewParam);");
      out.println("                });");
      out.println("            }");
      out.println();
      out.println("            document.addEventListener('click', function(e) {");
      out.println("              var link = e.target.closest('.saved-view-link');");
      out.println("              if (link) {");
      out.println("                e.preventDefault();");
      out.println("                fetchView(link.getAttribute('data-snapshot-id'), true);");
      out.println("                return;");
      out.println("              }");
      out.println("              var returnLink = e.target.closest('#returnToCurrentLink');");
      out.println("              if (returnLink) {");
      out.println("                e.preventDefault();");
      out.println("                fetchView('current', true);");
      out.println("              }");
      out.println("            });");
      out.println();
      out.println("            window.addEventListener('popstate', function() {");
      out.println("              var params = new URLSearchParams(location.search);");
      out.println("              var view = params.get('view');");
      out.println("              fetchView(view || 'current', false);");
      out.println("            });");
      out.println();
      out.println("            form.addEventListener('submit', function(e) {");
      out.println("              if (bypassAjax) { bypassAjax = false; return; }");
      out.println("              var submitter = e.submitter;");
      out.println("              try {");
      out.println("                e.preventDefault();");
      out.println("                var fd = buildFormData(submitter);");
      out.println("                fd.set('format', 'json');");
      out.println("                var params = new URLSearchParams(fd);");
      out.println(
          "                fetch(form.getAttribute('action'), { method: form.getAttribute('method') || 'POST', body: params })");
      out.println("                  .then(function(resp) {");
      out.println("                    if (!resp.ok) { throw new Error('HTTP ' + resp.status); }");
      out.println("                    return resp.json();");
      out.println("                  })");
      out.println("                  .then(function(data) {");
      out.println("                    if (data.error) { throw new Error(data.error); }");
      out.println("                    applyState(data);");
      out.println(
          "                    if (new URLSearchParams(location.search).get('view')) {");
      out.println(
          "                      history.replaceState(null, '', location.pathname);");
      out.println("                    }");
      out.println("                  })");
      out.println("                  .catch(function(err) {");
      out.println(
          "                    console.error('Step AJAX update failed, falling back to full submit:', err);");
      out.println("                    fallbackSubmit(submitter);");
      out.println("                  });");
      out.println("              } catch (err) {");
      out.println("                console.error('Step AJAX submit setup failed, falling back:', err);");
      out.println("                fallbackSubmit(submitter);");
      out.println("              }");
      out.println("            });");
      out.println("          })();");
      out.println("        </script>");
    }
    out.println("    </div>");

    out.println("    <div class=\"cell\" id=\"postLogPanel\">");
    out.print(renderState.postHtml);
    out.print(renderState.logHtml);
    out.println("    </div>");
    out.println("    <div class=\"cell\" id=\"prePanel\">");
    out.print(renderState.preHtml);
    out.println("    </div>");
    out.println("      </form>");

    out.println("  </body>");
    out.println("</html>");
    out.close();
  }

  @Override
  protected String resolveSupportingDataSet(HttpServletRequest req) {
    String supportingDataSet = req.getParameter(PARAM_SUPPORTING_DATA_SET);
    if (supportingDataSet != null && !supportingDataSet.trim().equals("")) {
      return SupportingDataManager.normalizeSetId(supportingDataSet);
    }

    HttpSession session = req.getSession(false);
    if (session != null) {
      Object value = session.getAttribute(SESSION_SUPPORTING_DATA_SET);
      if (value instanceof String && !((String) value).trim().equals("")) {
        return SupportingDataManager.normalizeSetId((String) value);
      }
    }

    return SupportingDataManager.resolveDefaultSupportingDataSet(getServletContext());
  }

  private String appendSupportingDataSet(String link, String supportingDataSet) {
    if (supportingDataSet == null || supportingDataSet.equals("")) {
      return link;
    }
    try {
      String sep = link.contains("?") ? "&" : "?";
      return link + sep + PARAM_SUPPORTING_DATA_SET + "=" + URLEncoder.encode(supportingDataSet, "UTF-8");
    } catch (UnsupportedEncodingException e) {
      return link;
    }
  }

  private String escapeHtml(String text) {
    if (text == null) {
      return "";
    }
    return text.replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
        .replace("'", "&#39;");
  }

  private boolean isMissingRequiredStartInput(HttpServletRequest req) {
    return isBlank(req.getParameter(LogicStep.PARAM_PATIENT_DOB))
        || isBlank(req.getParameter(LogicStep.PARAM_EVAL_DATE));
  }

  private boolean isBlank(String value) {
    return value == null || value.trim().equals("");
  }

  /**
   * Guided start screen shown instead of an error page when /step is reached
   * without the minimum patientDob/evalDate scenario inputs. Submitting builds
   * the same URL parameter shape already used by /forecast and /step.
   */
  private void renderStartScreen(HttpServletRequest req, HttpServletResponse resp, boolean sessionExpired)
      throws IOException {
    resp.setContentType("text/html");
    PrintWriter out = new PrintWriter(resp.getOutputStream());

    String activeSupportingDataSet = resolveSupportingDataSet(req);

    out.println("<!DOCTYPE html>");
    out.println("<html>");
    out.println("  <head>");
    out.println("    <title>CDSi - Start Stepping</title>");
    out.println("    <link rel=\"stylesheet\" type=\"text/css\" href=\"indexStep.css\">");
    out.println("    <style>");
    out.println("      #startScreenForm { display: block; max-width: 900px; margin: 20px; }");
    out.println("      #startScreenForm fieldset { margin-bottom: 16px; }");
    out.println("      #startScreenForm label { display: inline-block; min-width: 130px; }");
    out.println("      #startScreenForm .field-row { margin: 6px 0; }");
    out.println("      #startScreenForm table { margin: 8px 0; }");
    out.println("      #startScreenForm .actions { margin-top: 16px; }");
    out.println("      #startScreenForm .actions button { margin-right: 8px; }");
    out.println("      .notice { background:#FFF3CD; border:1px solid #E0C36A; padding:8px; max-width:900px; margin:20px; }");
    out.println("    </style>");
    out.println("    <script>");
    out.println("      function renumberRows(tbody, prefixes) {");
    out.println("        var rows = tbody.querySelectorAll('tr');");
    out.println("        for (var r = 0; r < rows.length; r++) {");
    out.println("          var index = r + 1;");
    out.println("          for (var p = 0; p < prefixes.length; p++) {");
    out.println("            var el = rows[r].querySelector('[name^=\"' + prefixes[p] + '\"]');");
    out.println("            if (el) { el.name = prefixes[p] + index; }");
    out.println("          }");
    out.println("        }");
    out.println("      }");
    out.println("      function addVaccinationRow() {");
    out.println("        var tbody = document.getElementById('vaccinationRows');");
    out.println("        var tr = document.createElement('tr');");
    out.println("        tr.innerHTML = '<td><input type=\"date\" name=\"vaccineDate\"></td>'");
    out.println("          + '<td><input type=\"text\" name=\"vaccineCvx\" size=\"4\"></td>'");
    out.println("          + '<td><input type=\"text\" name=\"vaccineMvx\" size=\"4\"></td>'");
    out.println("          + '<td><select name=\"vaccineConditionCode\"><option value=\"\"></option>"
        + "<option value=\"yes\">Yes</option><option value=\"no\">No</option></select></td>'");
    out.println("          + '<td><button type=\"button\" onclick=\"removeVaccinationRow(this)\">Remove</button></td>';");
    out.println("        tbody.appendChild(tr);");
    out.println(
        "        renumberRows(tbody, ['vaccineDate', 'vaccineCvx', 'vaccineMvx', 'vaccineConditionCode']);");
    out.println("      }");
    out.println("      function removeVaccinationRow(btn) {");
    out.println("        var tbody = document.getElementById('vaccinationRows');");
    out.println("        var tr = btn.closest('tr');");
    out.println("        if (tbody.children.length > 1) {");
    out.println("          tbody.removeChild(tr);");
    out.println(
        "          renumberRows(tbody, ['vaccineDate', 'vaccineCvx', 'vaccineMvx', 'vaccineConditionCode']);");
    out.println("        } else {");
    out.println("          tr.querySelectorAll('input').forEach(function(i) { i.value = ''; });");
    out.println("        }");
    out.println("      }");
    out.println("      function addObservationRow() {");
    out.println("        var tbody = document.getElementById('observationRows');");
    out.println("        var tr = document.createElement('tr');");
    out.println("        tr.innerHTML = '<td><input type=\"text\" name=\"observationCode\"></td>'");
    out.println("          + '<td><input type=\"date\" name=\"observationDate\"></td>'");
    out.println("          + '<td><button type=\"button\" onclick=\"removeObservationRow(this)\">Remove</button></td>';");
    out.println("        tbody.appendChild(tr);");
    out.println("        renumberRows(tbody, ['observationCode', 'observationDate']);");
    out.println("      }");
    out.println("      function removeObservationRow(btn) {");
    out.println("        var tbody = document.getElementById('observationRows');");
    out.println("        var tr = btn.closest('tr');");
    out.println("        if (tbody.children.length > 1) {");
    out.println("          tbody.removeChild(tr);");
    out.println("          renumberRows(tbody, ['observationCode', 'observationDate']);");
    out.println("        } else {");
    out.println("          tr.querySelectorAll('input').forEach(function(i) { i.value = ''; });");
    out.println("        }");
    out.println("      }");
    out.println("      function pruneBlankRows(tbody, prefixes) {");
    out.println("        var rows = Array.prototype.slice.call(tbody.querySelectorAll('tr'));");
    out.println("        var kept = [];");
    out.println("        rows.forEach(function(tr) {");
    out.println("          var allBlank = true;");
    out.println("          prefixes.forEach(function(prefix) {");
    out.println("            var el = tr.querySelector('[name^=\"' + prefix + '\"]');");
    out.println("            if (el && el.value && el.value.trim() !== '') { allBlank = false; }");
    out.println("          });");
    out.println("          if (allBlank) {");
    out.println("            tr.parentElement.removeChild(tr);");
    out.println("          } else {");
    out.println("            kept.push(tr);");
    out.println("          }");
    out.println("        });");
    out.println("        kept.forEach(function(tr, idx) {");
    out.println("          var index = idx + 1;");
    out.println("          prefixes.forEach(function(prefix) {");
    out.println("            var el = tr.querySelector('[name^=\"' + prefix + '\"]');");
    out.println("            if (el) { el.name = prefix + index; }");
    out.println("          });");
    out.println("        });");
    out.println("      }");
    out.println("      function pruneBlankOptionalRowsBeforeSubmit() {");
    out.println(
        "        pruneBlankRows(document.getElementById('vaccinationRows'), ['vaccineDate', 'vaccineCvx', 'vaccineMvx', 'vaccineConditionCode']);");
    out.println(
        "        pruneBlankRows(document.getElementById('observationRows'), ['observationCode', 'observationDate']);");
    out.println("      }");
    out.println("    </script>");
    out.println("  </head>");
    out.println("  <body>");

    AuthPageRenderer.renderSignedInHeader(out, req);

    out.println("    <a href=\"step\"><img src=\"Logo Large.png\" height=\"120\" align=\"left\"/></a>");
    out.println("    Version " + SoftwareVersion.VERSION + "<br clear=\"all\"/>");
    out.println("    <h1>CDSi Demonstration System</h1>");
    if (sessionExpired) {
      out.println("    <div class=\"notice\">Your step session has ended or expired. "
          + "Enter a scenario below to start a new step-through.</div>");
    }
    out.println("    <p>Provide at least a patient date of birth and an evaluation date to start stepping "
        + "through the CDSi logic, or pick an example below.</p>");

    out.println(
        "    <form id=\"startScreenForm\" action=\"step\" method=\"GET\" onsubmit=\"pruneBlankOptionalRowsBeforeSubmit()\">");

    out.println("      <fieldset>");
    out.println("        <legend>Patient</legend>");
    out.println("        <div class=\"field-row\">");
    out.println("          <label for=\"patientDob\">Patient DOB</label>");
    out.println(
        "          <input type=\"date\" id=\"patientDob\" name=\"" + LogicStep.PARAM_PATIENT_DOB + "\" required>");
    out.println("        </div>");
    out.println("        <div class=\"field-row\">");
    out.println("          <label for=\"evalDate\">Evaluation Date</label>");
    out.println(
        "          <input type=\"date\" id=\"evalDate\" name=\"" + LogicStep.PARAM_EVAL_DATE + "\" required>");
    out.println("        </div>");
    out.println("        <div class=\"field-row\">");
    out.println("          <label for=\"patientSex\">Sex</label>");
    out.println("          <select id=\"patientSex\" name=\"" + LogicStep.PARAM_PATIENT_SEX + "\">");
    out.println("            <option value=\"\"></option>");
    out.println("            <option value=\"M\">M</option>");
    out.println("            <option value=\"F\">F</option>");
    out.println("            <option value=\"O\">O</option>");
    out.println("            <option value=\"U\">U</option>");
    out.println("          </select>");
    out.println("        </div>");
    out.println("        <div class=\"field-row\">");
    out.println("          <label for=\"supportingDataSelect\">Supporting Data Set</label>");
    out.println("          <select id=\"supportingDataSelect\" name=\"" + PARAM_SUPPORTING_DATA_SET + "\">");
    out.println("            <option value=\"\">(default)</option>");
    for (SupportingDataManager.SupportingDataDescriptor descriptor : SupportingDataManager
        .listSupportingDataDescriptors(getServletContext())) {
      String displayName = descriptor.knowledgeBaseId + " v" + descriptor.version + " (" + descriptor.zipName + ")";
      boolean isSelected = descriptor.setId.equals(activeSupportingDataSet);
      out.println("            <option value=\"" + escapeHtml(descriptor.setId) + "\""
          + (isSelected ? " selected" : "") + ">" + escapeHtml(displayName) + "</option>");
    }
    out.println("          </select>");
    out.println("        </div>");
    out.println("      </fieldset>");

    out.println("      <fieldset>");
    out.println("        <legend>Vaccinations Administered (optional)</legend>");
    out.println("        <table>");
    out.println("          <tr><th>Date</th><th>CVX</th><th>MVX</th><th>Dose Condition</th><th></th></tr>");
    out.println("          <tbody id=\"vaccinationRows\">");
    out.println("            <tr>");
    out.println("              <td><input type=\"date\" name=\"vaccineDate1\"></td>");
    out.println("              <td><input type=\"text\" name=\"vaccineCvx1\" size=\"4\"></td>");
    out.println("              <td><input type=\"text\" name=\"vaccineMvx1\" size=\"4\"></td>");
    out.println("              <td>");
    out.println("                <select name=\"vaccineConditionCode1\">");
    out.println("                  <option value=\"\"></option>");
    out.println("                  <option value=\"yes\">Yes</option>");
    out.println("                  <option value=\"no\">No</option>");
    out.println("                </select>");
    out.println("              </td>");
    out.println("              <td><button type=\"button\" onclick=\"removeVaccinationRow(this)\">Remove</button></td>");
    out.println("            </tr>");
    out.println("          </tbody>");
    out.println("        </table>");
    out.println("        <button type=\"button\" onclick=\"addVaccinationRow()\">Add Row</button>");
    out.println("      </fieldset>");

    out.println("      <fieldset>");
    out.println("        <legend>Observations (optional)</legend>");
    out.println("        <table>");
    out.println("          <tr><th>Code</th><th>Date</th><th></th></tr>");
    out.println("          <tbody id=\"observationRows\">");
    out.println("            <tr>");
    out.println("              <td><input type=\"text\" name=\"observationCode1\"></td>");
    out.println("              <td><input type=\"date\" name=\"observationDate1\"></td>");
    out.println("              <td><button type=\"button\" onclick=\"removeObservationRow(this)\">Remove</button></td>");
    out.println("            </tr>");
    out.println("          </tbody>");
    out.println("        </table>");
    out.println("        <button type=\"button\" onclick=\"addObservationRow()\">Add Row</button>");
    out.println("      </fieldset>");

    out.println("      <div class=\"actions\">");
    out.println("        <button type=\"submit\" formaction=\"step\">Start Stepping</button>");
    out.println("        <button type=\"submit\" formaction=\"forecast\">Forecast</button>");
    out.println("      </div>");
    out.println("    </form>");

    out.println("    <h2>Load Example</h2>");
    out.println("    <table>");
    out.println("      <tr>");
    out.println("        <th>Step</th>");
    out.println("        <th>Forecast</th>");
    out.println("      </tr>");
    List<StepExample> stepExamplesCopy = new ArrayList<StepExample>();
    synchronized (stepExamples) {
      stepExamplesCopy.addAll(stepExamples);
    }
    for (StepExample stepExample : stepExamplesCopy) {
      String stepLink = appendSupportingDataSet("step?" + stepExample.getRequestString(), activeSupportingDataSet);
      String forecastLink = appendSupportingDataSet("forecast?" + stepExample.getRequestString(),
          activeSupportingDataSet);
      out.println("      <tr>");
      out.println("        <td><a href=\"" + stepLink + "\">" + escapeHtml(stepExample.getLabel()) + "</a></td>");
      out.println("        <td><a href=\"" + forecastLink + "\">Forecast</a></td>");
      out.println("      </tr>");
    }
    out.println("    </table>");

    out.println("  </body>");
    out.println("</html>");
    out.close();
  }

  /**
   * Normalizes HTML5 date-input values ("yyyy-MM-dd") down to the "yyyyMMdd"
   * shape the engine's forecast parameters already use, so the guided start
   * screen's date pickers work with the existing URL parameter contract.
   */
  private static final class DateNormalizingRequest extends HttpServletRequestWrapper {
    DateNormalizingRequest(HttpServletRequest request) {
      super(request);
    }

    @Override
    public String getParameter(String name) {
      String value = super.getParameter(name);
      if (value != null && isDateParam(name)) {
        return value.replace("-", "");
      }
      return value;
    }

    private boolean isDateParam(String name) {
      return name.equals(LogicStep.PARAM_PATIENT_DOB)
          || name.equals(LogicStep.PARAM_EVAL_DATE)
          || name.startsWith(LogicStep.PARAM_VACCINE_DATE)
          || name.startsWith(LogicStep.PARAM_OBSERVATION_DATE);
    }
  }

  private void printStableView(DataModel dataModel, PrintWriter out) {
    // print out patient date of birth
    if (dataModel.getPatient().getDateOfBirth() != null) {
      SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy");
      out.println("<h2>Patient DOB: " + sdf.format(dataModel.getPatient().getDateOfBirth()) + "</h2>");
    }

    // print out antigen
    if (dataModel.getPatientSeriesStepper().hasCurrent()) {
      out.println("<h2>" + dataModel.getPatientSeriesStepper().getCurrent() + "</h2>");
    } else if (dataModel.getAntigen() != null) {
      out.println("<h2>" + dataModel.getAntigen() + "</h2>");
    }

    if (dataModel.getTargetDoseList() != null) {
      List<VaccineDoseAdministered> vaccineDoseAdministeredList = new ArrayList<VaccineDoseAdministered>();
      if (dataModel.getSelectedAntigenAdministeredRecordList() != null) {
        for (AntigenAdministeredRecord aar : dataModel.getSelectedAntigenAdministeredRecordList()) {
          vaccineDoseAdministeredList.add(aar.getVaccineDoseAdministered());
        }
      }

      out.println("<table>");
      out.println("  <tr>");
      out.println("    <th colspan=\"2\">Series</th>");
      out.println("    <th colspan=\"2\">Dose Administered</th>");
      out.println("  </tr>");
      out.println("  <tr>");
      out.println("    <th>Selected</th>");
      out.println("    <th>Dose</th>");
      out.println("    <th>Status</th>");
      out.println("    <th>Vaccine Dose Admin</th>");
      out.println("  </tr>");
      for (TargetDose targetDose : dataModel.getTargetDoseList()) {
        TargetDose targetDoseSelected = dataModel.getTargetDose();
        List<Evaluation> evaluationList = targetDose.getEvaluationList();
        int rowSpan = evaluationList.size();
        // need to check if a new vaccination record is being evaluated
        AntigenAdministeredRecord aar = dataModel.getAntigenAdministeredRecord();
        boolean aarSelectedButNotYetEvaluated = false;
        if (aar != null && targetDoseSelected != null && targetDoseSelected == targetDose) {
          aarSelectedButNotYetEvaluated = true;
          for (Evaluation evaluation : evaluationList) {
            if (evaluation.getVaccineDoseAdministered() != null
                && evaluation.getVaccineDoseAdministered() == aar.getVaccineDoseAdministered()) {
              aarSelectedButNotYetEvaluated = false;
            }
          }
          if (aarSelectedButNotYetEvaluated) {
            rowSpan++;
          }
        }
        if (rowSpan == 0) {
          rowSpan = 1;
        }

        String doseNumber = targetDose.getTrackedSeriesDose().getDoseNumber();
        out.println("  <tr>");
        if (targetDoseSelected != null && targetDoseSelected == targetDose) {
          out.println("    <td rowspan=\"" + rowSpan + "\">--&gt;</td>");
        } else {
          out.println("    <td rowspan=\"" + rowSpan + "\"></td>");
        }
        out.println("    <td rowspan=\"" + rowSpan + "\">" + doseNumber + "</td>");
        if (evaluationList.size() == 0 && !aarSelectedButNotYetEvaluated) {
          out.println("    <td></td>");
          out.println("    <td></td>");
        }
        boolean first = true;
        for (Evaluation evaluation : evaluationList) {
          if (!first) {
            out.println("</tr><tr>");
          }
          out.println("    <td>" + evaluation.getEvaluationStatus() + "</td>");
          VaccineDoseAdministered vda = evaluation.getVaccineDoseAdministered();
          printVda(out, vda);
          vaccineDoseAdministeredList.remove(vda);
          first = false;
        }
        if (aarSelectedButNotYetEvaluated) {
          if (!first) {
            out.println("</tr><tr>");
          }
          out.println("    <td>--&gt;</td>");
          VaccineDoseAdministered vda = aar == null ? null : aar.getVaccineDoseAdministered();
          if (vda != null) {
            printVda(out, vda);
            vaccineDoseAdministeredList.remove(vda);
          }
        }
        out.println("  </tr>");
      }
      for (VaccineDoseAdministered vda : vaccineDoseAdministeredList) {
        out.println("  <tr>");
        out.println("    <td colspan=\"3\"></td>");
        printVda(out, vda);
        out.println("  </tr>");
      }
      out.println("</table>");

      out.println("<ul>");
      out.println("<li> Previous AAR = " + dataModel.getPreviousAntigenAdministeredRecord() + "</li>");
      out.println("<li> AAR = " + dataModel.getAntigenAdministeredRecord() + "</li>");
      if (dataModel.getPreviousTargetDose() != null) {
        out.println("<li> Previous TD = " + dataModel.getPreviousTargetDose().toString() + "</li>");
      }
      if (dataModel.getTargetDose() != null) {
        out.println("<li> TD = " + dataModel.getTargetDose().toString() + "</li>");
      }
      out.println("</ul>");
    }

    if (dataModel.getPatientSeriesStepper() != null && dataModel.getPatientSeriesStepper().getList().size() > 0) {
      out.println("psl hashcode " + dataModel.getPatientSeriesStepper().getList().hashCode() + "<br/>");
      out.println("<h2>Patient Series</h2>");
      out.println("<table>");
      out.println("  <tr>");
      out.println("    <th>Antigen</th>");
      out.println("    <th>Antigen Series</th>");
      out.println("    <th>Status</th>");
      out.println("    <th>Earliest</th>");
      out.println("    <th>Recommended</th>");
      out.println("  </tr>");
      for (PatientSeries patientSeries : dataModel.getPatientSeriesStepper().getList()) {
        AntigenSeries antigenSeries = patientSeries.getTrackedAntigenSeries();
        out.println("  <tr>");
        out.println("    <td>" + antigenSeries.getTargetDisease().getName() + "</td>");
        out.println("    <td>" + antigenSeries.getSeriesName() + "</td>");
        out.println("    <td>" + patientSeries.getPatientSeriesStatus() + "</td>");
        if (patientSeries.getForecast() == null) {
          out.println("    <td></td>");
          out.println("    <td></td>");
        } else {
          out.println("    <td>" + n(patientSeries.getForecast().getEarliestDate()) + "</td>");
          out.println("    <td>" + n(patientSeries.getForecast().getAdjustedRecommendedDate()) + "</td>");
        }
        out.println("  </tr>");
      }
      out.println("</table>");
    }

    if (dataModel.getForecastList().size() > 0) {
      out.println("<h2>Forecasts</h2>");
      out.println("<table>");
      out.println("  <tr>");
      out.println("    <th>Antigen</th>");
      out.println("    <th>VGF Status</th>");
      out.println("    <th>Earliest</th>");
      out.println("    <th>Recommended</th>");
      out.println("  </tr>");
      for (Forecast forecast : dataModel.getForecastList()) {
        out.println("  <tr>");
        out.println("    <td>" + forecast.getAntigen().getName() + "</td>");
        out.println("    <td>" + (forecast.getVaccineGroupForecast() == null ? "null"
            : forecast.getVaccineGroupForecast().getVaccineGroupStatus()) + "</td>");
        out.println("    <td>" + n(forecast.getEarliestDate()) + "</td>");
        out.println("    <td>" + n(forecast.getAdjustedRecommendedDate()) + "</td>");
        out.println("  </tr>");
      }
      out.println("</table>");
    }

    if (dataModel.getBestPatientSeriesList() != null) {
      out.println("<h3>Best Patient Series</h3>");
      out.println("<table>");
      out.println("  <tr>");
      out.println("    <th>Antigen</th>");
      out.println("    <th>Antigen Series</th>");
      out.println("    <th>Status</th>");
      out.println("    <th>Earliest</th>");
      out.println("    <th>Recommended</th>");
      out.println("  </tr>");
      for (PatientSeries patientSeries : dataModel.getBestPatientSeriesList()) {
        AntigenSeries antigenSeries = patientSeries.getTrackedAntigenSeries();
        out.println("  <tr>");
        out.println("    <td>" + antigenSeries.getTargetDisease().getName() + "</td>");
        out.println("    <td>" + antigenSeries.getSeriesName() + "</td>");
        out.println("    <td>" + patientSeries.getPatientSeriesStatus() + "</td>");
        if (patientSeries.getForecast() == null) {
          out.println("    <td>null</td>");
          out.println("    <td>null</td>");
        } else {
          out.println("    <td>" + n(patientSeries.getForecast().getEarliestDate()) + "</td>");
          out.println("    <td>" + n(patientSeries.getForecast().getAdjustedRecommendedDate()) + "</td>");
        }
        out.println("  </tr>");
      }
      out.println("</table>");
    }

    if (dataModel.getForecastList().size() > 0) {
      out.println("<h2>Vaccine Group Forecasts</h2>");
      out.println("<table>");
      out.println("  <tr>");
      out.println("    <th>Forecast Antigen</th>");
      out.println("    <th>Vaccine Group Status</th>");
      out.println("  </tr>");
      for (VaccineGroupForecast vgf : dataModel.getVaccineGroupForecastList()) {
        out.println("  <tr>");
        out.println("    <td>" + (vgf.getAntigen() == null ? "null"
            : vgf.getAntigen()) + "</td>");
        out.println("    <td>" + vgf.getVaccineGroupStatus().toString() + "</td>");
        out.println("  </tr>");
      }
      out.println("</table>");
    }

    if (dataModel.getPrioritizedPatientSeriesList() != null) {
      out.println("<h3>Prioritized Patient Series</h3>");
      out.println("<table>");
      out.println("  <tr>");
      out.println("    <th>Antigen</th>");
      out.println("    <th>Antigen Series</th>");
      out.println("    <th>Status</th>");
      out.println("    <th>Earliest</th>");
      out.println("    <th>Recommended</th>");
      out.println("  </tr>");
      for (PatientSeries patientSeries : dataModel.getPrioritizedPatientSeriesList()) {
        AntigenSeries antigenSeries = patientSeries.getTrackedAntigenSeries();
        out.println("  <tr>");
        out.println("    <td>" + antigenSeries.getTargetDisease().getName() + "</td>");
        out.println("    <td>" + antigenSeries.getSeriesName() + "</td>");
        out.println("    <td>" + patientSeries.getPatientSeriesStatus() + "</td>");
        if (patientSeries.getForecast() == null) {
          out.println("    <td>null</td>");
          out.println("    <td>null</td>");
        } else {
          out.println("    <td>" + n(patientSeries.getForecast().getEarliestDate()) + "</td>");
          out.println("    <td>" + n(patientSeries.getForecast().getAdjustedRecommendedDate()) + "</td>");
        }
        out.println("  </tr>");
      }
      out.println("</table>");
    }

    if (dataModel.getScorablePatientSeriesList() != null && dataModel.getScorablePatientSeriesList().size() > 0) {
      out.println("<h2>Scorable Patient Series</h2>");
      out.println("<table>");
      out.println("  <tr>");
      out.println("    <th>Antigen</th>");
      out.println("    <th>Antigen Series</th>");
      out.println("    <th>Status</th>");
      out.println("    <th>Earliest</th>");
      out.println("    <th>Recommended</th>");
      out.println("  </tr>");
      for (PatientSeries patientSeries : dataModel.getScorablePatientSeriesList()) {
        AntigenSeries antigenSeries = patientSeries.getTrackedAntigenSeries();
        out.println("  <tr>");
        out.println("    <td>" + antigenSeries.getTargetDisease().getName() + "</td>");
        out.println("    <td>" + antigenSeries.getSeriesName() + "</td>");
        out.println("    <td>" + patientSeries.getPatientSeriesStatus() + "</td>");
        if (patientSeries.getForecast() == null) {
          out.println("    <td>null</td>");
          out.println("    <td>null</td>");
        } else {
          out.println("    <td>" + n(patientSeries.getForecast().getEarliestDate()) + "</td>");
          out.println("    <td>" + n(patientSeries.getForecast().getAdjustedRecommendedDate()) + "</td>");
        }
        out.println("  </tr>");
      }
      out.println("</table>");
    }

  }

  private void printVda(PrintWriter out, VaccineDoseAdministered vda) {
    if (vda == null) {
      out.println("    <td></td>");
    } else {
      VaccineType vaccineType = vda.getVaccine().getVaccineType();
      String vaccineLabel = vaccineType.getShortDescription() + " (" + vaccineType.getCvxCode() + ") given "
          + n(vda.getDateAdministered());
      out.println("    <td>" + vaccineLabel + "</td>");
    }
  }

  private static volatile String processMapSvgTemplate = null;

  /**
   * Renders the curated inline process-map SVG (cdsi-web/src/main/webapp/WEB-INF/templates/step-process-model.svg)
   * with is-current/is-previous/is-active-edge classes applied for the given transition. Inline (not an
   * &lt;img&gt;) so the map's nodes and edges can be styled per request without generating a PNG per transition.
   */
  private String renderProcessMapSvg(LogicStepType previousStepType, LogicStepType currentStepType)
      throws IOException {
    String svg = loadProcessMapSvgTemplate();
    String currentNodeId = stepNodeId(currentStepType);
    String previousNodeId = stepNodeId(previousStepType);
    if (currentNodeId != null) {
      svg = addNodeClass(svg, currentNodeId, "is-current");
    }
    if (previousNodeId != null && !previousNodeId.equals(currentNodeId)) {
      svg = addNodeClass(svg, previousNodeId, "is-previous");
    }
    String edgeId = stepEdgeId(previousStepType, currentStepType);
    if (edgeId != null) {
      svg = addEdgeClass(svg, edgeId, "is-active-edge");
    }
    return svg;
  }

  private String stepNodeId(LogicStepType type) {
    return type == null ? null : "step-" + type.name();
  }

  private String stepEdgeId(LogicStepType previousStepType, LogicStepType currentStepType) {
    if (previousStepType == null || currentStepType == null) {
      return null;
    }
    return "edge-" + chapterToId(previousStepType.getChapter()) + "-" + chapterToId(currentStepType.getChapter());
  }

  private String loadProcessMapSvgTemplate() throws IOException {
    String cached = processMapSvgTemplate;
    if (cached != null) {
      return cached;
    }
    synchronized (StepServlet.class) {
      if (processMapSvgTemplate == null) {
        try (InputStream in = getServletContext().getResourceAsStream("/WEB-INF/templates/step-process-model.svg")) {
          if (in == null) {
            throw new IOException("Missing /WEB-INF/templates/step-process-model.svg");
          }
          processMapSvgTemplate = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
      }
      return processMapSvgTemplate;
    }
  }

  private String chapterToId(String chapter) {
    if (chapter == null) {
      return "";
    }
    if (chapter.equalsIgnoreCase("End")) {
      return "end";
    }
    return chapter.replace(".", "_");
  }

  private LogicStepType parseStepType(String name) {
    if (name == null) {
      return null;
    }
    try {
      return LogicStepType.valueOf(name);
    } catch (IllegalArgumentException e) {
      return null;
    }
  }

  /**
   * Holds the HTML fragments and step identity actually shown on this render: either the live
   * DataModel's current position, or a cached {@link StepSnapshot} the user is inspecting via the
   * Saved Views tray or browser history. Keeping this as one resolved value lets every render
   * call site (stable summary, map, post/log, pre, JSON) stay agnostic to which source it came
   * from.
   */
  private static final class StepRenderState {
    final String stableSummaryHtml;
    final String postHtml;
    final String logHtml;
    final String preHtml;
    final LogicStepType previousStepType;
    final LogicStepType currentStepType;
    final String transition;
    final String title;
    final boolean viewingSaved;
    final Integer viewingSnapshotId;

    StepRenderState(String stableSummaryHtml, String postHtml, String logHtml, String preHtml,
        LogicStepType previousStepType, LogicStepType currentStepType, String transition, String title,
        boolean viewingSaved, Integer viewingSnapshotId) {
      this.stableSummaryHtml = stableSummaryHtml;
      this.postHtml = postHtml;
      this.logHtml = logHtml;
      this.preHtml = preHtml;
      this.previousStepType = previousStepType;
      this.currentStepType = currentStepType;
      this.transition = transition;
      this.title = title;
      this.viewingSaved = viewingSaved;
      this.viewingSnapshotId = viewingSnapshotId;
    }
  }

  private StepRenderState resolveRenderState(DataModel dataModel, HttpServletRequest req, StepSnapshot viewingSnapshot) {
    if (viewingSnapshot != null) {
      LogicStepType previousStepType = parseStepType(viewingSnapshot.getPreviousStep());
      LogicStepType currentStepType = parseStepType(viewingSnapshot.getCurrentStep());
      String title = "CDSi - " + (currentStepType == null ? "Saved View" : currentStepType.getDisplay());
      return new StepRenderState(
          viewingSnapshot.getStableSummaryHtml(),
          viewingSnapshot.getPostHtml(),
          viewingSnapshot.getLogHtml(),
          viewingSnapshot.getPreHtml(),
          previousStepType,
          currentStepType,
          viewingSnapshot.getTransition(),
          title,
          true,
          viewingSnapshot.getId());
    }

    LogicStepType previousStepType = dataModel.getLogicStepPrevious() == null ? null
        : dataModel.getLogicStepPrevious().getLogicStepType();
    LogicStepType currentStepType = dataModel.getLogicStep() == null ? null
        : dataModel.getLogicStep().getLogicStepType();
    String transition = previousStepType == null || currentStepType == null ? null
        : previousStepType.getChapter() + "-" + currentStepType.getChapter();
    return new StepRenderState(
        renderStableSummaryHtml(dataModel),
        renderPostHtml(dataModel),
        renderLogHtml(dataModel),
        renderPreHtml(dataModel, req),
        previousStepType,
        currentStepType,
        transition,
        "CDSi - " + dataModel.getLogicStep().getTitle(),
        false,
        null);
  }

  @SuppressWarnings("unchecked")
  private StepSnapshot findSnapshot(HttpSession session, String viewParam) {
    int id;
    try {
      id = Integer.parseInt(viewParam.trim());
    } catch (NumberFormatException e) {
      return null;
    }
    List<StepSnapshot> snapshots = (List<StepSnapshot>) session.getAttribute(SESSION_STEP_SNAPSHOTS);
    if (snapshots == null) {
      return null;
    }
    for (StepSnapshot snapshot : snapshots) {
      if (snapshot.getId() == id) {
        return snapshot;
      }
    }
    return null;
  }

  @SuppressWarnings("unchecked")
  private List<StepSnapshot> listSnapshots(HttpSession session) {
    List<StepSnapshot> snapshots = (List<StepSnapshot>) session.getAttribute(SESSION_STEP_SNAPSHOTS);
    return snapshots == null ? new ArrayList<StepSnapshot>() : snapshots;
  }

  private String trayLabel(StepSnapshot snapshot) {
    if (snapshot.getTransition() != null) {
      return snapshot.getTransition().replace("-", " -> ");
    }
    return "#" + snapshot.getId();
  }

  private String addNodeClass(String svg, String nodeId, String extraClass) {
    String marker = "id=\"" + nodeId + "\" class=\"step-node ";
    int idx = svg.indexOf(marker);
    if (idx < 0) {
      return svg;
    }
    int insertAt = idx + marker.length();
    return svg.substring(0, insertAt) + extraClass + " " + svg.substring(insertAt);
  }

  private String addEdgeClass(String svg, String edgeId, String extraClass) {
    String marker = "id=\"" + edgeId + "\" class=\"edge";
    int idx = svg.indexOf(marker);
    if (idx < 0) {
      return svg;
    }
    int insertAt = idx + marker.length();
    return svg.substring(0, insertAt) + " " + extraClass + svg.substring(insertAt);
  }

  /**
   * Captures a session-scoped {@link StepSnapshot} for the transition the user just landed on
   * (a "Next Step" click, a process-map node jump, or a fallback jump control). Only the landing
   * point is captured, never the intermediate steps a jump traverses internally, since capture
   * only runs once per completed request here rather than inside {@link #jump}.
   */
  @SuppressWarnings("unchecked")
  private StepSnapshot captureSnapshot(HttpSession session, DataModel dataModel, HttpServletRequest req) {
    LogicStepType previousStepType = dataModel.getLogicStepPrevious() == null ? null
        : dataModel.getLogicStepPrevious().getLogicStepType();
    LogicStepType currentStepType = dataModel.getLogicStep() == null ? null
        : dataModel.getLogicStep().getLogicStepType();
    String transition = previousStepType == null || currentStepType == null ? null
        : previousStepType.getChapter() + "-" + currentStepType.getChapter();

    synchronized (session) {
      List<StepSnapshot> snapshots = (List<StepSnapshot>) session.getAttribute(SESSION_STEP_SNAPSHOTS);
      if (snapshots == null) {
        snapshots = new ArrayList<StepSnapshot>();
        session.setAttribute(SESSION_STEP_SNAPSHOTS, snapshots);
      }
      Integer counter = (Integer) session.getAttribute(SESSION_STEP_SNAPSHOT_COUNTER);
      int nextId = (counter == null ? 0 : counter) + 1;
      session.setAttribute(SESSION_STEP_SNAPSHOT_COUNTER, nextId);

      StepSnapshot snapshot = new StepSnapshot(
          nextId,
          previousStepType == null ? null : previousStepType.name(),
          currentStepType == null ? null : currentStepType.name(),
          transition,
          renderStableSummaryHtml(dataModel),
          renderPostHtml(dataModel),
          renderLogHtml(dataModel),
          renderPreHtml(dataModel, req));
      snapshots.add(snapshot);
      while (snapshots.size() > MAX_SNAPSHOTS_PER_SESSION) {
        snapshots.remove(0);
      }
      return snapshot;
    }
  }

  @SuppressWarnings("unchecked")
  private int countSnapshots(HttpSession session) {
    List<StepSnapshot> snapshots = (List<StepSnapshot>) session.getAttribute(SESSION_STEP_SNAPSHOTS);
    return snapshots == null ? 0 : snapshots.size();
  }

  private String renderStableSummaryHtml(DataModel dataModel) {
    StringWriter sw = new StringWriter();
    PrintWriter pw = new PrintWriter(sw);
    if (dataModel.getLogicStepPrevious() != null) {
      printStableView(dataModel, pw);
    }
    return sw.toString();
  }

  private String renderPostHtml(DataModel dataModel) {
    StringWriter sw = new StringWriter();
    PrintWriter pw = new PrintWriter(sw);
    try {
      LogicStep logicStep = dataModel.getLogicStepPrevious();
      if (logicStep != null) {
        LogicStepType logicStepType = logicStep.getLogicStepType();
        pw.println("<h1>" + logicStepType.getChapter() + " " + logicStepType.getName() + "</h1>");
        LogicStepRenderer.printPost(logicStep, pw);
      }
    } catch (Exception e) {
      e.printStackTrace();
      pw.println("<pre>");
      e.printStackTrace(pw);
      pw.println("</pre>");
    }
    return sw.toString();
  }

  private String renderLogHtml(DataModel dataModel) {
    StringWriter sw = new StringWriter();
    PrintWriter pw = new PrintWriter(sw);
    try {
      LogicStep logicStep = dataModel.getLogicStepPrevious();
      if (logicStep != null) {
        LogicStepRenderer.printLog(logicStep, pw);
      }
    } catch (Exception e) {
      e.printStackTrace();
      pw.println("<pre>");
      e.printStackTrace(pw);
      pw.println("</pre>");
    }
    return sw.toString();
  }

  private String renderPreHtml(DataModel dataModel, HttpServletRequest req) {
    StringWriter sw = new StringWriter();
    PrintWriter pw = new PrintWriter(sw);
    try {
      LogicStep logicStep = dataModel.getLogicStep();
      if (logicStep != null) {
        LogicStepType logicStepType = logicStep.getLogicStepType();
        pw.println("<h1>" + logicStepType.getChapter() + " " + logicStepType.getName() + "</h1>");
        LogicStepRenderer.printPre(logicStep, pw, req);
      }
    } catch (Exception e) {
      e.printStackTrace();
      pw.println("<pre>");
      e.printStackTrace(pw);
      pw.println("</pre>");
    }
    return sw.toString();
  }

  /**
   * AJAX response consumed by the process-map's step-form submit interception (Next Step, node-click
   * jumps, and the fallback jump controls all share this). Keeps full form submission as the fallback:
   * the client re-submits the real form on any fetch failure or on an "error" field in this payload.
   */
  private String buildStepJsonResponse(StepRenderState renderState, Exception exception, StepSnapshot snapshot,
      HttpSession session) {
    StringBuilder json = new StringBuilder();
    json.append("{");
    json.append("\"previousStep\":")
        .append(jsonStringOrNull(renderState.previousStepType == null ? null : renderState.previousStepType.name()))
        .append(",");
    json.append("\"currentStep\":")
        .append(jsonStringOrNull(renderState.currentStepType == null ? null : renderState.currentStepType.name()))
        .append(",");
    json.append("\"transition\":").append(jsonStringOrNull(renderState.transition)).append(",");
    json.append("\"title\":").append(jsonStringOrNull(renderState.title)).append(",");
    json.append("\"snapshotId\":").append(snapshot == null ? "null" : String.valueOf(snapshot.getId())).append(",");
    json.append("\"snapshotCount\":").append(countSnapshots(session)).append(",");
    json.append("\"viewingSaved\":").append(renderState.viewingSaved).append(",");
    json.append("\"viewingSnapshotId\":")
        .append(renderState.viewingSnapshotId == null ? "null" : String.valueOf(renderState.viewingSnapshotId))
        .append(",");
    if (snapshot != null) {
      json.append("\"trayEntry\":{\"id\":").append(snapshot.getId()).append(",\"label\":")
          .append(jsonStringOrNull(trayLabel(snapshot))).append("},");
    }

    if (exception != null) {
      StringWriter sw = new StringWriter();
      exception.printStackTrace(new PrintWriter(sw));
      json.append("\"error\":").append(jsonStringOrNull(sw.toString()));
    } else {
      json.append("\"stableSummaryHtml\":").append(jsonStringOrNull(renderState.stableSummaryHtml)).append(",");
      json.append("\"postHtml\":").append(jsonStringOrNull(renderState.postHtml)).append(",");
      json.append("\"logHtml\":").append(jsonStringOrNull(renderState.logHtml)).append(",");
      json.append("\"preHtml\":").append(jsonStringOrNull(renderState.preHtml)).append(",");
      json.append("\"mapState\":{");
      json.append("\"previous\":").append(jsonStringOrNull(stepNodeId(renderState.previousStepType))).append(",");
      json.append("\"current\":").append(jsonStringOrNull(stepNodeId(renderState.currentStepType))).append(",");
      json.append("\"activeEdge\":")
          .append(jsonStringOrNull(stepEdgeId(renderState.previousStepType, renderState.currentStepType)));
      json.append("}");
    }
    json.append("}");
    return json.toString();
  }

  private String jsonStringOrNull(String value) {
    if (value == null) {
      return "null";
    }
    StringBuilder sb = new StringBuilder(value.length() + 16);
    sb.append('"');
    for (int i = 0; i < value.length(); i++) {
      char c = value.charAt(i);
      switch (c) {
        case '"':
          sb.append("\\\"");
          break;
        case '\\':
          sb.append("\\\\");
          break;
        case '\n':
          sb.append("\\n");
          break;
        case '\r':
          sb.append("\\r");
          break;
        case '\t':
          sb.append("\\t");
          break;
        default:
          if (c < 0x20) {
            sb.append(String.format("\\u%04x", (int) c));
          } else {
            sb.append(c);
          }
      }
    }
    sb.append('"');
    return sb.toString();
  }

  private void jump(DataModel dataModel, String jumpTo) throws Exception {
    int count = 0;
    while (dataModel.getLogicStep().getLogicStepType() != LogicStepType.END
        && !dataModel.getLogicStep().getLogicStepType().getName().equals(jumpTo)) {
      dataModel.setNextLogicStep(dataModel.getLogicStep().process());
      if (count++ > 100000) {
        throw new Exception("Jump loop over 100000 detected");
      }
    }
  }
}


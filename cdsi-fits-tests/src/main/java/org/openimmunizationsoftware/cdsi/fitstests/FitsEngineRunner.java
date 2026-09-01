package org.openimmunizationsoftware.cdsi.fitstests;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.openimmunizationsoftware.cdsi.core.data.DataModel;
import org.openimmunizationsoftware.cdsi.core.data.DataModelLoader;
import org.openimmunizationsoftware.cdsi.core.domain.VaccineGroupForecast;
import org.openimmunizationsoftware.cdsi.core.domain.VaccineGroupStatus;
import org.openimmunizationsoftware.cdsi.core.logic.LogicStep;
import org.openimmunizationsoftware.cdsi.core.logic.LogicStepFactory;
import org.openimmunizationsoftware.cdsi.core.logic.LogicStepType;

/**
 * Runs one FitsTestCase through cdsi-engine, headlessly - no servlet, no
 * HTML, no network - and reports whether each expected forecast matched.
 * This is the entire "run a FITS case" mechanism the interactive FITS UI
 * (cdsi-web's FitsServlet) also uses internally, minus the web/HTML parts.
 */
public final class FitsEngineRunner {

  private static final int STEP_LOOP_GUARD = 100000;

  private FitsEngineRunner() {
  }

  public static FitsRunResult run(FitsTestCase testCase, String supportingDataSet) {
    try {
      DataModel dataModel = DataModelLoader.createDataModel(supportingDataSet);
      dataModel.setForecastInput(testCase.toForecastInput());
      dataModel.setNextLogicStep(LogicStepFactory.createLogicStep(LogicStepType.GATHER_NECESSARY_DATA, dataModel));

      int count = 0;
      while (dataModel.getLogicStep().getLogicStepType() != LogicStepType.END) {
        dataModel.setNextLogicStep(dataModel.getLogicStep().process());
        if (++count > STEP_LOOP_GUARD) {
          throw new IllegalStateException(
              "Logic steps stuck in a loop at " + dataModel.getLogicStep().getTitle());
        }
      }

      List<VaccineGroupForecast> vaccineGroupForecastList = dataModel.getVaccineGroupForecastList();
      List<ForecastComparison> comparisons = new ArrayList<>();
      for (FitsTestCase.ExpectedForecast expected : testCase.getExpectedForecasts()) {
        comparisons.add(compare(expected, vaccineGroupForecastList));
      }
      return new FitsRunResult(testCase, comparisons, null);
    } catch (Exception e) {
      return new FitsRunResult(testCase, List.of(), e);
    }
  }

  private static ForecastComparison compare(FitsTestCase.ExpectedForecast expected,
      List<VaccineGroupForecast> vaccineGroupForecastList) {
    for (VaccineGroupForecast vgf : vaccineGroupForecastList) {
      if (CvxEquivalence.isSameVaccine(expected.getVaccineCvx(), vgf.getAntigen().getCvxForForecast())) {
        VaccineGroupStatus actualStatus = vgf.getVaccineGroupStatus();
        String expectedStatusName = expected.getSerieStatus();
        boolean statusPass = expectedStatusName == null
            || expectedStatusName.equalsIgnoreCase(actualStatus.name());

        String actualEarliest = null;
        String actualRecommended = null;
        boolean earliestPass = true;
        boolean recommendedPass = true;
        if (actualStatus == VaccineGroupStatus.NOT_COMPLETE) {
          actualEarliest = FitsDates.format(vgf.getEarliestDate());
          actualRecommended = FitsDates.format(vgf.getAdjustedRecommendedDate());
          if (expected.getEarliestDate() != null) {
            earliestPass = expected.getEarliestDate().equals(actualEarliest);
          }
          if (expected.getRecommendedDate() != null) {
            recommendedPass = expected.getRecommendedDate().equals(actualRecommended);
          }
        }
        return new ForecastComparison(expected, actualStatus.name(), actualEarliest, actualRecommended,
            statusPass && earliestPass && recommendedPass, null);
      }
    }
    return new ForecastComparison(expected, null, null, null, false,
        "No forecasted vaccine group matched CVX " + expected.getVaccineCvx());
  }

  public static final class FitsRunResult {
    private final FitsTestCase testCase;
    private final List<ForecastComparison> comparisons;
    private final Exception exception;

    FitsRunResult(FitsTestCase testCase, List<ForecastComparison> comparisons, Exception exception) {
      this.testCase = testCase;
      this.comparisons = comparisons;
      this.exception = exception;
    }

    public FitsTestCase getTestCase() {
      return testCase;
    }

    public List<ForecastComparison> getComparisons() {
      return comparisons;
    }

    public Exception getException() {
      return exception;
    }

    public boolean isPass() {
      if (exception != null) {
        return false;
      }
      return comparisons.stream().allMatch(ForecastComparison::isPass);
    }

    public String describeFailure() {
      if (exception != null) {
        return String.format(Locale.ROOT, "%s threw %s: %s", testCase.displayName(),
            exception.getClass().getSimpleName(), exception.getMessage());
      }
      StringBuilder sb = new StringBuilder();
      for (ForecastComparison c : comparisons) {
        if (!c.isPass()) {
          sb.append(c.describe()).append("; ");
        }
      }
      return sb.length() == 0 ? "unknown failure" : sb.toString();
    }
  }

  public static final class ForecastComparison {
    private final FitsTestCase.ExpectedForecast expected;
    private final String actualStatus;
    private final String actualEarliest;
    private final String actualRecommended;
    private final boolean pass;
    private final String problem;

    ForecastComparison(FitsTestCase.ExpectedForecast expected, String actualStatus, String actualEarliest,
        String actualRecommended, boolean pass, String problem) {
      this.expected = expected;
      this.actualStatus = actualStatus;
      this.actualEarliest = actualEarliest;
      this.actualRecommended = actualRecommended;
      this.pass = pass;
      this.problem = problem;
    }

    public boolean isPass() {
      return pass;
    }

    public String describe() {
      if (problem != null) {
        return "cvx " + expected.getVaccineCvx() + ": " + problem;
      }
      return String.format(Locale.ROOT,
          "cvx %s: expected status=%s earliest=%s recommended=%s, got status=%s earliest=%s recommended=%s",
          expected.getVaccineCvx(), expected.getSerieStatus(), expected.getEarliestDate(),
          expected.getRecommendedDate(), actualStatus, actualEarliest, actualRecommended);
    }
  }
}

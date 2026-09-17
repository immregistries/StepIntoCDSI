package org.openimmunizationsoftware.cdsi.servlet;

/**
 * A captured report-out for a single user-intentional transition (a "Next Step" click, a
 * process-map node jump, or a fallback jump control) within one step session. Snapshots are
 * session-scoped and additive: only the step the user lands on is captured, never the
 * intermediate steps a jump traverses internally.
 *
 * <p>
 * Holds the same HTML fragments the AJAX step response already renders, so a saved view can be
 * redisplayed later without touching the live {@code DataModel}. A compact, structured state
 * summary for diffing between snapshots is intentionally not captured yet; that is Phase 8's
 * concern once the diffing UI exists to consume it.
 */
public class StepSnapshot {

  private final int id;
  private final String previousStep;
  private final String currentStep;
  private final String transition;
  private final String stableSummaryHtml;
  private final String postHtml;
  private final String logHtml;
  private final String preHtml;
  private final long capturedAt;

  public StepSnapshot(int id, String previousStep, String currentStep, String transition,
      String stableSummaryHtml, String postHtml, String logHtml, String preHtml) {
    this.id = id;
    this.previousStep = previousStep;
    this.currentStep = currentStep;
    this.transition = transition;
    this.stableSummaryHtml = stableSummaryHtml;
    this.postHtml = postHtml;
    this.logHtml = logHtml;
    this.preHtml = preHtml;
    this.capturedAt = System.currentTimeMillis();
  }

  public int getId() {
    return id;
  }

  public String getPreviousStep() {
    return previousStep;
  }

  public String getCurrentStep() {
    return currentStep;
  }

  public String getTransition() {
    return transition;
  }

  public String getStableSummaryHtml() {
    return stableSummaryHtml;
  }

  public String getPostHtml() {
    return postHtml;
  }

  public String getLogHtml() {
    return logHtml;
  }

  public String getPreHtml() {
    return preHtml;
  }

  public long getCapturedAt() {
    return capturedAt;
  }
}

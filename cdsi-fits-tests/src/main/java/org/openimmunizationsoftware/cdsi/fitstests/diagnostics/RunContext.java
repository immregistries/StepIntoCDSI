package org.openimmunizationsoftware.cdsi.fitstests.diagnostics;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.concurrent.TimeUnit;

import org.openimmunizationsoftware.cdsi.fitstests.ReferenceSetVerifier;

/**
 * Phase 17: everything run.json needs to say about the environment a FITS
 * run happened in. Git and Maven are both invoked as external processes
 * (ProcessBuilder) rather than pulling in a library for either - this is
 * local, read-only metadata collection, not something worth a new
 * dependency for. Every lookup fails soft ("unknown"/null), since a
 * missing git binary or a shallow checkout without history must never be
 * the reason a FITS run itself can't proceed.
 */
public final class RunContext {

  public final String startedAt;
  public final String gitCommit;
  public final String gitCommitAbbreviated;
  public final String gitBranch;
  public final boolean gitDirty;
  public final String javaVersion;
  public final String mavenVersion;
  public final String testFilter;
  public final ReferenceSetVerifier.ReferenceSet referenceSet;

  private RunContext(String startedAt, String gitCommit, String gitCommitAbbreviated, String gitBranch,
      boolean gitDirty, String javaVersion, String mavenVersion, String testFilter,
      ReferenceSetVerifier.ReferenceSet referenceSet) {
    this.startedAt = startedAt;
    this.gitCommit = gitCommit;
    this.gitCommitAbbreviated = gitCommitAbbreviated;
    this.gitBranch = gitBranch;
    this.gitDirty = gitDirty;
    this.javaVersion = javaVersion;
    this.mavenVersion = mavenVersion;
    this.testFilter = testFilter;
    this.referenceSet = referenceSet;
  }

  public static RunContext capture(ReferenceSetVerifier.ReferenceSet referenceSet) {
    String startedAt = Instant.now().toString();
    String gitCommit = runGit("rev-parse", "HEAD");
    String gitCommitAbbreviated = runGit("rev-parse", "--short", "HEAD");
    String gitBranch = runGit("rev-parse", "--abbrev-ref", "HEAD");
    boolean gitDirty = !isBlank(runGit("status", "--porcelain"));
    String javaVersion = System.getProperty("java.version", "unknown");
    String mavenVersion = detectMavenVersion();
    String testFilter = System.getProperty("test");
    return new RunContext(startedAt, gitCommit, gitCommitAbbreviated, gitBranch, gitDirty, javaVersion, mavenVersion,
        testFilter, referenceSet);
  }

  /** timestamp-commit-referenceSetId, sanitized for use as a directory
   * name on Windows and POSIX filesystems alike (both reject ':'). */
  public String runId() {
    String timestampPart = startedAt.replace(":", "").replace(".", "-");
    String commitPart = gitCommitAbbreviated == null ? "nogit" : gitCommitAbbreviated;
    String referenceSetPart = referenceSet == null ? "noreferenceset" : referenceSet.id();
    return sanitize(timestampPart + "-" + commitPart + "-" + referenceSetPart);
  }

  private static String sanitize(String value) {
    return value.replaceAll("[^A-Za-z0-9._-]", "-");
  }

  private static boolean isBlank(String s) {
    return s == null || s.isBlank();
  }

  private static String runGit(String... args) {
    String[] command = new String[args.length + 1];
    command[0] = "git";
    System.arraycopy(args, 0, command, 1, args.length);
    return runProcess(command);
  }

  private static String detectMavenVersion() {
    String fromProcess = runProcess("mvn", "-v");
    if (fromProcess != null) {
      for (String line : fromProcess.split("\\R")) {
        if (line.startsWith("Apache Maven")) {
          return line.trim();
        }
      }
    }
    return "unknown";
  }

  private static String runProcess(String... command) {
    try {
      return runProcessDirect(command);
    } catch (IOException directFailure) {
      // On Windows, tools distributed as a .cmd/.bat wrapper (Maven has no
      // native .exe) can't be launched directly by ProcessBuilder without a
      // shell to resolve them - retry through cmd.exe before giving up.
      if (!System.getProperty("os.name", "").toLowerCase(java.util.Locale.ROOT).contains("win")) {
        return null;
      }
      try {
        String[] viaShell = new String[command.length + 2];
        viaShell[0] = "cmd.exe";
        viaShell[1] = "/c";
        System.arraycopy(command, 0, viaShell, 2, command.length);
        return runProcessDirect(viaShell);
      } catch (IOException | InterruptedException shellFailure) {
        return null;
      }
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      return null;
    }
  }

  private static String runProcessDirect(String... command) throws IOException, InterruptedException {
    Process process = new ProcessBuilder(command).redirectErrorStream(true).start();
    StringBuilder output = new StringBuilder();
    try (BufferedReader reader = new BufferedReader(
        new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
      String line;
      while ((line = reader.readLine()) != null) {
        output.append(line).append('\n');
      }
    }
    boolean finished = process.waitFor(10, TimeUnit.SECONDS);
    if (!finished || process.exitValue() != 0) {
      return null;
    }
    return output.toString().strip();
  }
}

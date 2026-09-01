package org.openimmunizationsoftware.cdsi.fitstests;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.fasterxml.jackson.databind.ObjectMapper;

/** Loads every FitsTestCase fixture (written by FitsDownloader) from src/test/resources/fits/. */
public final class FitsFixtures {

  public static final String FIXTURE_ROOT = "fits";

  private static final ObjectMapper MAPPER = new ObjectMapper();

  private FitsFixtures() {
  }

  /** All fixtures under the classpath's fits/ directory, sorted for stable, reviewable test-report ordering. */
  public static List<FitsTestCase> loadAll() {
    try {
      List<FitsTestCase> testCases = new ArrayList<>();
      for (Path root : fixtureRoots()) {
        try (Stream<Path> paths = Files.walk(root)) {
          List<Path> jsonFiles = paths
              .filter(p -> p.toString().toLowerCase().endsWith(".json"))
              .sorted()
              .collect(Collectors.toList());
          for (Path jsonFile : jsonFiles) {
            testCases.add(MAPPER.readValue(jsonFile.toFile(), FitsTestCase.class));
          }
        }
      }
      testCases.sort(Comparator.comparing((FitsTestCase t) -> nullToEmpty(t.getGroupName()))
          .thenComparing(t -> nullToEmpty(t.getUid())));
      return testCases;
    } catch (IOException e) {
      throw new IllegalStateException("Failed to load FITS fixtures from classpath:" + FIXTURE_ROOT, e);
    }
  }

  private static String nullToEmpty(String s) {
    return s == null ? "" : s;
  }

  private static List<Path> fixtureRoots() throws IOException {
    List<Path> roots = new ArrayList<>();
    java.util.Enumeration<URL> resources = FitsFixtures.class.getClassLoader().getResources(FIXTURE_ROOT);
    while (resources.hasMoreElements()) {
      URL url = resources.nextElement();
      if ("file".equals(url.getProtocol())) {
        try {
          File dir = new File(url.toURI());
          if (dir.isDirectory()) {
            roots.add(dir.toPath());
          }
        } catch (Exception e) {
          throw new IOException("Could not resolve fixture root URL: " + url, e);
        }
      }
    }
    return roots;
  }
}

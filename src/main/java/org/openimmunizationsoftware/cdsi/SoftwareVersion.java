package org.openimmunizationsoftware.cdsi;

import java.io.InputStream;
import java.util.Properties;

public class SoftwareVersion {
  private static final String DEFAULT_VERSION = "5.0.0";
  public static final String VERSION = loadVersion();

  private static String loadVersion() {
    Properties properties = new Properties();
    try (InputStream inputStream = SoftwareVersion.class.getClassLoader()
        .getResourceAsStream("software-version.properties")) {
      if (inputStream == null) {
        return DEFAULT_VERSION;
      }
      properties.load(inputStream);
      String configuredVersion = properties.getProperty("software.version");
      if (configuredVersion == null || configuredVersion.trim().isEmpty()) {
        return DEFAULT_VERSION;
      }
      configuredVersion = configuredVersion.trim();
      if (configuredVersion.contains("${") || configuredVersion.contains("}")) {
        return DEFAULT_VERSION;
      }
      return configuredVersion;
    } catch (Exception e) {
      return DEFAULT_VERSION;
    }
  }
}

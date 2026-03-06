package org.openimmunizationsoftware.cdsi;

import java.io.InputStream;
import java.util.Properties;

public class SoftwareVersion {
  private static final String DEFAULT_VERSION = "5.0.0";
  private static final String DEFAULT_STEP_EXTERNAL_URL = "http://localhost:8080/step";
  private static final String DEFAULT_HUB_EXTERNAL_URL = "http://localhost:8080/hub/home";

  public static final String VERSION;
  public static final String STEP_EXTERNAL_URL;
  public static final String HUB_EXTERNAL_URL;

  static {
    LoadedProperties loaded = loadProperties();
    VERSION = loaded.version;
    STEP_EXTERNAL_URL = loaded.stepExternalUrl;
    HUB_EXTERNAL_URL = loaded.hubExternalUrl;
  }

  private static LoadedProperties loadProperties() {
    Properties properties = new Properties();
    try (InputStream inputStream = SoftwareVersion.class.getClassLoader()
        .getResourceAsStream("software-version.properties")) {
      if (inputStream == null) {
        return new LoadedProperties(DEFAULT_VERSION, DEFAULT_STEP_EXTERNAL_URL, DEFAULT_HUB_EXTERNAL_URL);
      }
      properties.load(inputStream);
      String configuredVersion = resolveProperty(properties, "software.version", DEFAULT_VERSION);
      String configuredStepExternalUrl = resolveProperty(properties, "step.external.url", DEFAULT_STEP_EXTERNAL_URL);
      String configuredHubExternalUrl = resolveProperty(properties, "hub.external.url", DEFAULT_HUB_EXTERNAL_URL);
      return new LoadedProperties(configuredVersion, configuredStepExternalUrl, configuredHubExternalUrl);
    } catch (Exception e) {
      return new LoadedProperties(DEFAULT_VERSION, DEFAULT_STEP_EXTERNAL_URL, DEFAULT_HUB_EXTERNAL_URL);
    }
  }

  private static String resolveProperty(Properties properties, String key, String defaultValue) {
    String configuredValue = properties.getProperty(key);
    if (configuredValue == null || configuredValue.trim().isEmpty()) {
      return defaultValue;
    }
    configuredValue = configuredValue.trim();
    if (configuredValue.contains("${") || configuredValue.contains("}")) {
      return defaultValue;
    }
    return configuredValue;
  }

  private static class LoadedProperties {
    private final String version;
    private final String stepExternalUrl;
    private final String hubExternalUrl;

    private LoadedProperties(String version, String stepExternalUrl, String hubExternalUrl) {
      this.version = version;
      this.stepExternalUrl = stepExternalUrl;
      this.hubExternalUrl = hubExternalUrl;
    }
  }
}

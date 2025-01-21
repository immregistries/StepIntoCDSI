package org.openimmunizationsoftware.cdsi.servlet.maps;

public class MapPlace {
  private Color fillerColor = Color.DEFAULT;
  private String link = null;
  private String tooltip = "";
  private String testParticipant = null;

  public MapPlace(String testParticipant) {
    this.testParticipant = testParticipant;
  }

  public String getTestParticipant() {
    return testParticipant;
  }

  public String getTooltip() {
    return tooltip;
  }

  public void setTooltip(String tooltip) {
    this.tooltip = tooltip;
  }

  public String getLink() {
    return link;
  }

  public void setLink(String link) {
    this.link = link;
  }

  public Color getFillerColor() {
    return fillerColor;
  }

  public void setFillerColor(Color fillerColor) {
    this.fillerColor = fillerColor;
  }

}

package org.openimmunizationsoftware.cdsi.servlet.maps;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;

public class MapEntityMaker {

  private Map<String, MapPlace> mapPlaceMap = new HashMap<>();
  private String mapTitle = "";
  private String statusTitle = "";
  private List<Color> colorList = new ArrayList<>();
  private Map<Color, String> statusColorMap = new HashMap<>();
  private double width = 1000.0;
  private double height = 680.0;

  public void addStatusColor(String status, Color color) {
    statusColorMap.put(color, status);
    colorList.add(color);
  }

  public String getStatusTitle() {
    return statusTitle;
  }

  public void setStatusTitle(String statusTitle) {
    this.statusTitle = statusTitle;
  }

  public String getMapTitle() {
    return mapTitle;
  }

  public void setMapTitle(String mapTitle) {
    this.mapTitle = mapTitle;
  }

  private Color defaultFillColor = Color.DEFAULT;

  public Color getDefaultFillColor() {
    return defaultFillColor;
  }

  public void setDefaultFillColor(Color defaultFillColor) {
    this.defaultFillColor = defaultFillColor;
  }

  public void addMapPlace(MapPlace mapPlace) {
    if (mapPlace.getTestParticipant() != null && !mapPlace.getTestParticipant().equals("")) {
      mapPlaceMap.put(mapPlace.getTestParticipant(), mapPlace);
    }
  }

  public static void printDashboardHeaderBar(String title, PrintWriter out) {
    out.println("<h1 class=\"dashboardTitle\">" + title + "</h1>");
  }

  public void printMapWithKey(PrintWriter out) {
    printDashboardHeaderBar(mapTitle, out);
    printMapOnly(out);
    Map<Color, Integer> countMap = new HashMap<>();
    Map<Color, String> iisMap = new HashMap<>();
    Map<Color, List<String>> colorNameListMap = new HashMap<>();

    for (Color color : colorList) {
      countMap.put(color, 0);
      colorNameListMap.put(color, new ArrayList<>());
    }

    for (MapPlace mapPlace : mapPlaceMap.values()) {
      Color color = mapPlace.getFillerColor();
      if (countMap.containsKey(color)) {
        int count = countMap.get(color);
        countMap.put(color, count + 1);
        colorNameListMap.get(color).add(mapPlace.getTestParticipant());
      }
    }

    for (Color color : colorList) {
      String display = getSortedTestParticipantString(colorNameListMap.get(color));
      iisMap.put(color, display);
    }

    out.println("<table width=\"1000\">");
    out.println("  <tr>");
    out.println("    <th>" + statusTitle + "</th>");
    out.println("    <th>Jurisdictions</th>");
    out.println("    <th>Count</th>");
    out.println("  </tr>");
    for (Color color : colorList) {
      out.println("  <tr>");
      out.println("    <td style=\"background-color: " + color.getRgb() + "\">"
          + statusColorMap.get(color) + "</td>");
      out.println("    <td>" + (iisMap.get(color) == null ? "" : iisMap.get(color)) + "</td>");
      out.println("    <td>" + countMap.get(color) + "</td>");
      out.println("  </tr>");
    }
    out.println("</table>");
  }

  public double getWidth() {
    return width;
  }

  public void setWidth(double width) {
    this.width = width;
  }

  public double getHeight() {
    return height;
  }

  public void setHeight(double height) {
    this.height = height;
  }

  public void printMapOnly(PrintWriter out) {
    out.println("<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"" + width + "\" height=\""
        + height + "\" viewbox=\"0 0 1000 680\">");
    out.println("<g id=\"outlines\">");
    for (MapEntity mapEntity : MapEntityManager.getMapEntityList()) {
      Color fill = defaultFillColor;
      MapPlace mapPlace = mapPlaceMap.get(mapEntity.getId());
      String mouseover = "";
      if (mapPlace != null) {
        fill = mapPlace.getFillerColor();
        if (!"".equals(mapPlace.getTooltip())) {
          out.println("<g><title>" + mapPlace.getTooltip() + "</title>");
        }
        if (mapPlace.getLink() != null) {
          out.println("<a xlink:href=\"" + mapPlace.getLink() + "\">");
          mouseover = " onmouseover=\"evt.target.setAttribute('opacity', '0.5');\" onmouseout=\"evt.target.setAttribute('opacity','1)');\"";
        }
      }
      if (mapEntity.getMapEntityType() == MapEntityType.PATH) {
        out.println(" <path id=\"" + mapEntity.getId() + "\" fill=\"" + fill.getRgb()
            + "\" d=\"" + mapEntity.getContent() + "\"" + mouseover + "/>");
      } else if (mapEntity.getMapEntityType() == MapEntityType.CIRCLE) {
        out.println(" <circle id=\"" + mapEntity.getId() + "\" fill=\"" + fill.getRgb()
            + "\" stroke=\"" + Color.DEFAULT.getRgb()
            + "\" stroke-width=\"1.5\" " + mapEntity.getContent() + "" + mouseover + "/>");
      }
      {
        double titlePosX = mapEntity.getTitlePosX();
        double titlePosY = mapEntity.getTitlePosY();
        if (titlePosX > 0 && titlePosY > 0) {
          out.println(" <text x=\"" + titlePosX + "\" y=\"" + titlePosY + "\" fill=\""
              + Color.MAP_LABEL_TEXT.getRgb() + "\" style=\"font-size: 12px;\">"
              + mapEntity.getId() + "</text>");
        }
      }
      if (mapPlace != null) {
        if (mapPlace.getLink() != null) {
          out.println("</a>");
        }
        if (!"".equals(mapPlace.getTooltip())) {
          out.println("</g>");
        }
      }
    }
    out.println("</g>");
    out.println(
        "<path id=\"frames\" fill=\"none\" stroke=\"" + Color.MAP_SEPARATING_LINES.getRgb()
            + "\" stroke-width=\"2\" d=\"M215,493v55l36,45 M0,425h147l68,68h85l54,54v46\"/>");
    out.println("</svg>");
  }

  protected String getSortedTestParticipantString(List<String> testParticipants) {
    return testParticipants.stream().map(tp -> {
      return tp;
    }).sorted().collect(Collectors.joining(", "));
  }
}

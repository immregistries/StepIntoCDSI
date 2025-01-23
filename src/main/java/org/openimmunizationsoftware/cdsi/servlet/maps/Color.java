package org.openimmunizationsoftware.cdsi.servlet.maps;

public class Color {

    public static final Color DEFAULT = new Color("#eeeeee");
    public static final Color MAP_SEPARATING_LINES = new Color("#A9A9A9");
    public static final Color MAP_LABEL_TEXT = new Color("#000000");
    public static final Color MAP_SELECTED = new Color("#00ff00");

    private String rgb = "";

    public Color(String rgb) {
        this.rgb = rgb;
    }

    public String getRgb() {
        return rgb;
    }

    public void setRgb(String rgb) {
        this.rgb = rgb;
    }
}

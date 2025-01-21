package org.openimmunizationsoftware.cdsi.servlet.maps;

public class MapEntity {
  private MapEntityType mapEntityType = null;
  private String id = "";
  private String content = "";
  private double titlePosX = 0.0;
  private double titlePosY = 0.0;

  public double getTitlePosX() {
    return titlePosX;
  }

  public MapEntity setTitlePosX(double titlePosX) {
    this.titlePosX = titlePosX;
    return this;
  }

  public double getTitlePosY() {
    return titlePosY;
  }

  public MapEntity setTitlePosY(double titlePosY) {
    this.titlePosY = titlePosY;
    return this;
  }

  public MapEntity(MapEntityType mapEntityType, String id, String content) {
    this.mapEntityType = mapEntityType;
    this.id = id;
    this.content = content;
  }

  public MapEntityType getMapEntityType() {
    return mapEntityType;
  }

  public void setMapEntityType(MapEntityType mapEntityType) {
    this.mapEntityType = mapEntityType;
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getContent() {
    return content;
  }

  public void setContent(String content) {
    this.content = content;
  }

  @Override
  public int hashCode() {
    return getId().hashCode();
  }

  @Override
  public boolean equals(Object obj) {
    if (obj instanceof MapEntity) {
      MapEntity me = (MapEntity) obj;
      return me.getId().equals(getId());
    }
    return super.equals(obj);
  }
}

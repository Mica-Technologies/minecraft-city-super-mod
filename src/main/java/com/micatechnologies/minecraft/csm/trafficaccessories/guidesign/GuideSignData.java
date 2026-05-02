package com.micatechnologies.minecraft.csm.trafficaccessories.guidesign;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.util.ArrayList;
import java.util.List;

public class GuideSignData {

  private static final int VERSION = 1;
  private static final int MAX_PANELS = 4;
  private static final Gson GSON = new GsonBuilder().create();

  private int version = VERSION;
  private int signColor = GuideSignColor.GREEN.ordinal();
  private int postType = PostType.OVERHEAD.ordinal();
  private int borderWidth = 1;
  private int cornerStyle = CornerStyle.ROUND.ordinal();
  private int minWidth = 32;
  private List<GuideSignPanel> panels = new ArrayList<>();

  public GuideSignData() {
    GuideSignPanel defaultPanel = new GuideSignPanel();
    GuideSignRow row = new GuideSignRow();
    row.addElement(GuideSignElement.createText("DESTINATION", 1.0f));
    defaultPanel.addRow(row);
    panels.add(defaultPanel);
  }

  public int getSignColorOrdinal() {
    return signColor;
  }

  public GuideSignColor getSignColor() {
    return GuideSignColor.fromNBT(signColor);
  }

  public void setSignColor(int signColor) {
    this.signColor = signColor;
  }

  public void cycleSignColor() {
    this.signColor = getSignColor().next().ordinal();
  }

  public int getPostTypeOrdinal() {
    return postType;
  }

  public PostType getPostType() {
    return PostType.fromOrdinal(postType);
  }

  public void setPostType(int postType) {
    this.postType = postType;
  }

  public void cyclePostType() {
    this.postType = getPostType().next().ordinal();
  }

  public int getBorderWidth() {
    return borderWidth;
  }

  public void setBorderWidth(int borderWidth) {
    this.borderWidth = Math.max(0, Math.min(3, borderWidth));
  }

  public int getCornerStyleOrdinal() {
    return cornerStyle;
  }

  public CornerStyle getCornerStyle() {
    return CornerStyle.fromOrdinal(cornerStyle);
  }

  public void setCornerStyle(int cornerStyle) {
    this.cornerStyle = cornerStyle;
  }

  public void cycleCornerStyle() {
    this.cornerStyle = getCornerStyle().next().ordinal();
  }

  public int getMinWidth() {
    return minWidth;
  }

  public void setMinWidth(int minWidth) {
    this.minWidth = Math.max(16, Math.min(96, minWidth));
  }

  public List<GuideSignPanel> getPanels() {
    return panels;
  }

  public void setPanels(List<GuideSignPanel> panels) {
    this.panels = panels != null ? panels : new ArrayList<>();
  }

  public boolean canAddPanel() {
    return panels.size() < MAX_PANELS;
  }

  public void addPanel() {
    if (canAddPanel()) {
      GuideSignPanel p = new GuideSignPanel();
      GuideSignRow row = new GuideSignRow();
      row.addElement(GuideSignElement.createText("", 1.0f));
      p.addRow(row);
      panels.add(p);
    }
  }

  public void removePanel(int index) {
    if (index >= 0 && index < panels.size() && panels.size() > 1) {
      panels.remove(index);
    }
  }

  public String toJson() {
    this.version = VERSION;
    return GSON.toJson(this);
  }

  public static GuideSignData fromJson(String json) {
    if (json == null || json.isEmpty()) {
      return new GuideSignData();
    }
    try {
      GuideSignData data = GSON.fromJson(json, GuideSignData.class);
      if (data == null) {
        return new GuideSignData();
      }
      if (data.panels == null) {
        data.panels = new ArrayList<>();
      }
      if (data.panels.isEmpty()) {
        GuideSignPanel p = new GuideSignPanel();
        GuideSignRow row = new GuideSignRow();
        row.addElement(GuideSignElement.createText("", 1.0f));
        p.addRow(row);
        data.panels.add(p);
      }
      for (GuideSignPanel panel : data.panels) {
        if (panel.getRows() == null) {
          panel.setRows(new ArrayList<>());
        }
        for (GuideSignRow row : panel.getRows()) {
          if (row.getElements() == null) {
            row.setElements(new ArrayList<>());
          }
        }
      }
      return data;
    } catch (Exception e) {
      return new GuideSignData();
    }
  }

  public GuideSignData copy() {
    return fromJson(toJson());
  }
}

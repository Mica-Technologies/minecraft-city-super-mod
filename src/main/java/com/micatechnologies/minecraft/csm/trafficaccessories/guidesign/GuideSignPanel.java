package com.micatechnologies.minecraft.csm.trafficaccessories.guidesign;

import java.util.ArrayList;
import java.util.List;

public class GuideSignPanel {

  private static final int MAX_ROWS = 6;

  private List<GuideSignRow> rows = new ArrayList<>();
  private ExitTabData exitTab = null;

  public GuideSignPanel() {
  }

  public List<GuideSignRow> getRows() {
    return rows;
  }

  public void setRows(List<GuideSignRow> rows) {
    this.rows = rows != null ? rows : new ArrayList<>();
  }

  public ExitTabData getExitTab() {
    return exitTab;
  }

  public void setExitTab(ExitTabData exitTab) {
    this.exitTab = exitTab;
  }

  public boolean hasExitTab() {
    return exitTab != null;
  }

  public void enableExitTab() {
    if (exitTab == null) {
      exitTab = new ExitTabData();
    }
  }

  public void disableExitTab() {
    exitTab = null;
  }

  public boolean canAddRow() {
    return rows.size() < MAX_ROWS;
  }

  public void addRow(GuideSignRow row) {
    if (canAddRow() && row != null) {
      rows.add(row);
    }
  }

  public void removeRow(int index) {
    if (index >= 0 && index < rows.size()) {
      rows.remove(index);
    }
  }

  public GuideSignPanel copy() {
    GuideSignPanel p = new GuideSignPanel();
    for (GuideSignRow r : rows) {
      p.rows.add(r.copy());
    }
    p.exitTab = exitTab != null ? exitTab.copy() : null;
    return p;
  }
}

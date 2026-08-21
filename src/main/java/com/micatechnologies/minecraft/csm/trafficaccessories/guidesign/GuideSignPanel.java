package com.micatechnologies.minecraft.csm.trafficaccessories.guidesign;

import java.util.ArrayList;
import java.util.List;

public class GuideSignPanel {

  private static final int MAX_ROWS = 6;

  /** Upper bound on arrow-per-lane lanes; wider than any realistic freeway cross-section. */
  public static final int MAX_APL_LANES = 8;

  private List<GuideSignRow> rows = new ArrayList<>();
  private ExitTabData exitTab = null;
  // Arrow-per-lane (MUTCD APL) band below this panel's rows. 0 = off. Absent in older
  // JSON, which Gson leaves at these initializers, so old signs simply have no band.
  private int aplLanes = 0;
  // The rightmost N of aplLanes lanes are EXIT ONLY (yellow patch + dark arrows).
  private int aplExitLanes = 0;

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

  public int getAplLanes() {
    return aplLanes;
  }

  public void setAplLanes(int aplLanes) {
    this.aplLanes = Math.max(0, Math.min(MAX_APL_LANES, aplLanes));
    if (aplExitLanes > this.aplLanes) {
      aplExitLanes = this.aplLanes;
    }
  }

  public boolean hasApl() {
    return aplLanes > 0;
  }

  public int getAplExitLanes() {
    return aplExitLanes;
  }

  public void setAplExitLanes(int aplExitLanes) {
    this.aplExitLanes = Math.max(0, Math.min(this.aplLanes, aplExitLanes));
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
    p.aplLanes = aplLanes;
    p.aplExitLanes = aplExitLanes;
    return p;
  }
}

package com.micatechnologies.minecraft.csm.trafficaccessories.guidesign;

import java.util.ArrayList;
import java.util.List;

public class GuideSignRow {

  private static final int MAX_ELEMENTS = 5;

  private List<GuideSignElement> elements = new ArrayList<>();
  private int verticalSpacing = 0;
  private int alignment = RowAlignment.CENTER.ordinal();

  public GuideSignRow() {
  }

  public List<GuideSignElement> getElements() {
    return elements;
  }

  public void setElements(List<GuideSignElement> elements) {
    this.elements = elements != null ? elements : new ArrayList<>();
  }

  public int getVerticalSpacing() {
    return verticalSpacing;
  }

  public void setVerticalSpacing(int verticalSpacing) {
    this.verticalSpacing = Math.max(0, Math.min(16, verticalSpacing));
  }

  public int getAlignmentOrdinal() {
    return alignment;
  }

  public RowAlignment getAlignment() {
    return RowAlignment.fromOrdinal(alignment);
  }

  public void setAlignment(int alignment) {
    this.alignment = alignment;
  }

  public void cycleAlignment() {
    this.alignment = getAlignment().next().ordinal();
  }

  public boolean canAddElement() {
    return elements.size() < MAX_ELEMENTS;
  }

  public void addElement(GuideSignElement element) {
    if (canAddElement() && element != null) {
      elements.add(element);
    }
  }

  public void removeElement(int index) {
    if (index >= 0 && index < elements.size()) {
      elements.remove(index);
    }
  }

  public void moveElementUp(int index) {
    if (index > 0 && index < elements.size()) {
      GuideSignElement e = elements.remove(index);
      elements.add(index - 1, e);
    }
  }

  public void moveElementDown(int index) {
    if (index >= 0 && index < elements.size() - 1) {
      GuideSignElement e = elements.remove(index);
      elements.add(index + 1, e);
    }
  }

  public String getSummary() {
    if (elements.isEmpty()) return "(empty)";
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < elements.size(); i++) {
      if (i > 0) sb.append("  ");
      sb.append(elements.get(i).getSummary());
    }
    return sb.toString();
  }

  public GuideSignRow copy() {
    GuideSignRow r = new GuideSignRow();
    r.verticalSpacing = verticalSpacing;
    r.alignment = alignment;
    for (GuideSignElement e : elements) {
      r.elements.add(e.copy());
    }
    return r;
  }
}

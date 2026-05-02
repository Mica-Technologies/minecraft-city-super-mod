package com.micatechnologies.minecraft.csm.trafficaccessories;

import com.micatechnologies.minecraft.csm.CsmNetwork;
import com.micatechnologies.minecraft.csm.codeutils.packets.DynamicGuideSignUpdatePacket;
import com.micatechnologies.minecraft.csm.trafficaccessories.guidesign.CornerStyle;
import com.micatechnologies.minecraft.csm.trafficaccessories.guidesign.ExitTabData;
import com.micatechnologies.minecraft.csm.trafficaccessories.guidesign.GuideSignArrowType;
import com.micatechnologies.minecraft.csm.trafficaccessories.guidesign.GuideSignBannerType;
import com.micatechnologies.minecraft.csm.trafficaccessories.guidesign.GuideSignData;
import com.micatechnologies.minecraft.csm.trafficaccessories.guidesign.GuideSignElement;
import com.micatechnologies.minecraft.csm.trafficaccessories.guidesign.GuideSignPanel;
import com.micatechnologies.minecraft.csm.trafficaccessories.guidesign.GuideSignRow;
import com.micatechnologies.minecraft.csm.trafficaccessories.guidesign.GuideSignShieldType;
import com.micatechnologies.minecraft.csm.trafficaccessories.guidesign.SignTemplates;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.ParametersAreNonnullByDefault;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.util.text.TextFormatting;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

public class DynamicGuideSignGui extends GuiScreen {

  private static final int TAB_PROPERTIES = 0;
  private static final int TAB_PANEL = 1;
  private static final int TAB_ROW = 2;
  private static final int TAB_PREVIEW = 3;

  private static final int BTN_TAB_PROPERTIES = 100;
  private static final int BTN_TAB_PANEL = 101;
  private static final int BTN_TAB_ROW = 102;
  private static final int BTN_TAB_PREVIEW = 103;
  private static final int BTN_SAVE = 200;
  private static final int BTN_CANCEL = 201;

  private static final int BTN_SIGN_COLOR = 10;
  private static final int BTN_POST_TYPE = 11;
  private static final int BTN_BORDER_DOWN = 12;
  private static final int BTN_BORDER_UP = 13;
  private static final int BTN_CORNER_STYLE = 14;
  private static final int BTN_ADD_PANEL = 15;
  private static final int BTN_REMOVE_PANEL = 16;
  private static final int BTN_TEMPLATE = 17;
  private static final int BTN_COPY = 18;
  private static final int BTN_PASTE = 19;
  private static final int BTN_MIN_WIDTH_DOWN = 51;
  private static final int BTN_MIN_WIDTH_UP = 52;

  private static final int BTN_PANEL_PREV = 20;
  private static final int BTN_PANEL_NEXT = 21;
  private static final int BTN_EXIT_TAB_TOGGLE = 22;
  private static final int BTN_EXIT_TAB_POS = 23;
  private static final int BTN_EXIT_TAB_COLOR = 24;
  private static final int BTN_EXIT_TAB_TOLL = 25;
  private static final int BTN_ADD_ROW = 26;
  private static final int BTN_REMOVE_ROW = 27;
  private static final int BTN_EDIT_ROW = 28;

  private static final int BTN_ROW_PREV = 30;
  private static final int BTN_ROW_NEXT = 31;
  private static final int BTN_ADD_ELEMENT = 32;
  private static final int BTN_REMOVE_ELEMENT = 33;
  private static final int BTN_ELEM_UP = 34;
  private static final int BTN_ELEM_DOWN = 35;

  private static final int BTN_ELEM_TYPE_CYCLE = 40;
  private static final int BTN_TEXT_SCALE_DOWN = 41;
  private static final int BTN_TEXT_SCALE_UP = 42;
  private static final int BTN_SHIELD_TYPE = 43;
  private static final int BTN_BANNER_TYPE = 44;
  private static final int BTN_ARROW_TYPE = 45;
  private static final int BTN_SPACING_DOWN = 46;
  private static final int BTN_SPACING_UP = 47;
  private static final int BTN_VSPACING_DOWN = 48;
  private static final int BTN_VSPACING_UP = 49;
  private static final int BTN_ROW_ALIGN = 50;

  private static final int FIELD_WIDTH = 240;
  private static final int BTN_HEIGHT = 18;
  private static final int SCROLL_STEP = 12;
  private static final int SCROLLBAR_WIDTH = 4;

  private final TileEntityDynamicGuideSign tileEntity;
  private GuideSignData data;
  private int currentTab = TAB_PROPERTIES;
  private int selectedPanel = 0;
  private int selectedRow = 0;
  private int selectedElement = 0;

  private int tabContentScroll = 0;
  private int tabContentMaxScroll = 0;
  private int viewportTop = 0;
  private int viewportBottom = 0;
  private int contentLeft = 0;

  private final List<GuiButton> contentButtons = new ArrayList<>();
  private final List<Integer> contentButtonNaturalY = new ArrayList<>();
  private final List<GuiTextField> contentTextFields = new ArrayList<>();
  private final List<Integer> contentTextFieldNaturalY = new ArrayList<>();
  private int customContentBottom = 0;

  private GuiTextField textField;
  private GuiTextField routeField;
  private GuiTextField exitTextField;

  // Process-wide clipboard for copy/paste between signs.
  private static String clipboardJson = null;
  // Static so the next-template selection is preserved across GUI opens.
  private static int templateIndex = 0;

  private final List<String> previewLines = new ArrayList<>();
  private static final int PREVIEW_LINE_HEIGHT = 11;

  public DynamicGuideSignGui(TileEntityDynamicGuideSign tileEntity) {
    this.tileEntity = tileEntity;
    this.data = tileEntity.getSignData().copy();
  }

  @Override
  public void initGui() {
    Keyboard.enableRepeatEvents(true);
    super.initGui();
    buttonList.clear();
    contentButtons.clear();
    contentButtonNaturalY.clear();
    contentTextFields.clear();
    contentTextFieldNaturalY.clear();

    ScaledResolution sr = new ScaledResolution(this.mc);
    int centerX = sr.getScaledWidth() / 2;
    int left = centerX - FIELD_WIDTH / 2;
    int tabWidth = FIELD_WIDTH / 4;
    int startY = 25;
    int bottomY = sr.getScaledHeight() - 30;

    contentLeft = left;
    viewportTop = startY + BTN_HEIGHT + 6;
    viewportBottom = bottomY - 4;
    customContentBottom = 0;

    buttonList.add(new GuiButton(BTN_TAB_PROPERTIES, left, startY, tabWidth, BTN_HEIGHT,
        "Properties"));
    buttonList.add(new GuiButton(BTN_TAB_PANEL, left + tabWidth, startY, tabWidth, BTN_HEIGHT,
        "Panels"));
    buttonList.add(new GuiButton(BTN_TAB_ROW, left + tabWidth * 2, startY, tabWidth, BTN_HEIGHT,
        "Elements"));
    buttonList.add(new GuiButton(BTN_TAB_PREVIEW, left + tabWidth * 3, startY, tabWidth,
        BTN_HEIGHT, "Preview"));

    int halfW = (FIELD_WIDTH - 4) / 2;

    switch (currentTab) {
      case TAB_PROPERTIES:
        buildPropertiesTab(left, viewportTop, halfW);
        break;
      case TAB_PANEL:
        buildPanelTab(left, viewportTop, halfW);
        break;
      case TAB_ROW:
        buildRowTab(left, viewportTop, halfW);
        break;
      case TAB_PREVIEW:
        buildPreviewTab(left, viewportTop);
        break;
    }

    buttonList.add(new GuiButton(BTN_SAVE, left, bottomY, halfW, BTN_HEIGHT, "Save"));
    buttonList.add(
        new GuiButton(BTN_CANCEL, left + halfW + 4, bottomY, halfW, BTN_HEIGHT, "Cancel"));

    recomputeMaxScroll();
    applyTabScroll();
    updateTabButtonStates();
  }

  private void addContentBtn(GuiButton btn) {
    contentButtons.add(btn);
    contentButtonNaturalY.add(btn.y);
    buttonList.add(btn);
  }

  private void addContentField(GuiTextField field) {
    contentTextFields.add(field);
    contentTextFieldNaturalY.add(field.y);
  }

  private void recomputeMaxScroll() {
    int maxBottom = viewportTop;
    for (int i = 0; i < contentButtons.size(); i++) {
      int b = contentButtonNaturalY.get(i) + contentButtons.get(i).height;
      if (b > maxBottom) maxBottom = b;
    }
    for (int i = 0; i < contentTextFields.size(); i++) {
      int b = contentTextFieldNaturalY.get(i) + contentTextFields.get(i).height;
      if (b > maxBottom) maxBottom = b;
    }
    if (customContentBottom > maxBottom) maxBottom = customContentBottom;
    int viewportHeight = viewportBottom - viewportTop;
    int extent = maxBottom - viewportTop;
    tabContentMaxScroll = Math.max(0, extent - viewportHeight);
    if (tabContentScroll > tabContentMaxScroll) tabContentScroll = tabContentMaxScroll;
    if (tabContentScroll < 0) tabContentScroll = 0;
  }

  private void applyTabScroll() {
    for (int i = 0; i < contentButtons.size(); i++) {
      GuiButton btn = contentButtons.get(i);
      int naturalY = contentButtonNaturalY.get(i);
      int scrolledY = naturalY - tabContentScroll;
      btn.y = scrolledY;
      btn.visible = scrolledY + btn.height > viewportTop && scrolledY < viewportBottom;
    }
    for (int i = 0; i < contentTextFields.size(); i++) {
      GuiTextField field = contentTextFields.get(i);
      int naturalY = contentTextFieldNaturalY.get(i);
      int scrolledY = naturalY - tabContentScroll;
      field.y = scrolledY;
      field.setVisible(
          scrolledY + field.height > viewportTop && scrolledY < viewportBottom);
    }
  }

  private void buildPropertiesTab(int left, int y, int halfW) {
    addContentBtn(new GuiButton(BTN_SIGN_COLOR, left, y, FIELD_WIDTH, BTN_HEIGHT, ""));
    y += BTN_HEIGHT + 3;
    addContentBtn(new GuiButton(BTN_POST_TYPE, left, y, FIELD_WIDTH, BTN_HEIGHT, ""));
    y += BTN_HEIGHT + 3;
    addContentBtn(new GuiButton(BTN_BORDER_DOWN, left, y, 30, BTN_HEIGHT, "-"));
    addContentBtn(new GuiButton(BTN_BORDER_UP, left + FIELD_WIDTH - 30, y, 30, BTN_HEIGHT, "+"));
    y += BTN_HEIGHT + 3;
    addContentBtn(new GuiButton(BTN_MIN_WIDTH_DOWN, left, y, 30, BTN_HEIGHT, "-"));
    addContentBtn(new GuiButton(BTN_MIN_WIDTH_UP, left + FIELD_WIDTH - 30, y, 30, BTN_HEIGHT, "+"));
    y += BTN_HEIGHT + 3;
    addContentBtn(new GuiButton(BTN_CORNER_STYLE, left, y, FIELD_WIDTH, BTN_HEIGHT, ""));
    y += BTN_HEIGHT + 6;
    addContentBtn(new GuiButton(BTN_ADD_PANEL, left, y, halfW, BTN_HEIGHT, "+ Add Panel"));
    GuiButton removePanel = new GuiButton(BTN_REMOVE_PANEL, left + halfW + 4, y, halfW,
        BTN_HEIGHT, "- Remove Panel");
    removePanel.enabled = data.getPanels().size() > 1;
    addContentBtn(removePanel);
    y += BTN_HEIGHT + 6;
    addContentBtn(new GuiButton(BTN_TEMPLATE, left, y, FIELD_WIDTH, BTN_HEIGHT,
        "Apply Template (cycle)"));
    y += BTN_HEIGHT + 3;
    addContentBtn(new GuiButton(BTN_COPY, left, y, halfW, BTN_HEIGHT, "Copy Sign"));
    GuiButton pasteBtn = new GuiButton(BTN_PASTE, left + halfW + 4, y, halfW, BTN_HEIGHT,
        "Paste Sign");
    pasteBtn.enabled = clipboardJson != null;
    addContentBtn(pasteBtn);
  }

  private void buildPanelTab(int left, int y, int halfW) {
    clampPanelSelection();
    GuiButton prevPnl = new GuiButton(BTN_PANEL_PREV, left, y, 40, BTN_HEIGHT, "< Prev");
    GuiButton nextPnl = new GuiButton(BTN_PANEL_NEXT, left + FIELD_WIDTH - 40, y, 40, BTN_HEIGHT,
        "Next >");
    prevPnl.enabled = selectedPanel > 0;
    nextPnl.enabled = selectedPanel < data.getPanels().size() - 1;
    addContentBtn(prevPnl);
    addContentBtn(nextPnl);
    y += BTN_HEIGHT + 4;

    GuideSignPanel panel = data.getPanels().get(selectedPanel);

    addContentBtn(new GuiButton(BTN_EXIT_TAB_TOGGLE, left, y, FIELD_WIDTH, BTN_HEIGHT, ""));
    y += BTN_HEIGHT + 2;

    if (panel.hasExitTab()) {
      exitTextField = new GuiTextField(60, fontRenderer, left, y, halfW - 2, BTN_HEIGHT);
      exitTextField.setMaxStringLength(10);
      exitTextField.setText(panel.getExitTab().getText());
      addContentField(exitTextField);
      addContentBtn(
          new GuiButton(BTN_EXIT_TAB_POS, left + halfW + 2, y, halfW, BTN_HEIGHT, ""));
      y += BTN_HEIGHT + 2;
      addContentBtn(
          new GuiButton(BTN_EXIT_TAB_COLOR, left, y, halfW - 2, BTN_HEIGHT, ""));
      addContentBtn(
          new GuiButton(BTN_EXIT_TAB_TOLL, left + halfW + 2, y, halfW, BTN_HEIGHT, ""));
      y += BTN_HEIGHT + 2;
    } else {
      exitTextField = null;
    }

    y += 4;

    List<GuideSignRow> rows = panel.getRows();
    for (int i = 0; i < rows.size(); i++) {
      int btnY = y + i * (BTN_HEIGHT + 1);
      String label = "Row " + (i + 1) + ": " + truncate(rows.get(i).getSummary(), 28);
      boolean isSelected = i == selectedRow;
      addContentBtn(new GuiButton(BTN_EDIT_ROW + 1000 + i, left, btnY,
          FIELD_WIDTH, BTN_HEIGHT, (isSelected ? "> " : "  ") + label));
    }

    y += Math.max(rows.size(), 1) * (BTN_HEIGHT + 1) + 3;
    GuiButton addRow = new GuiButton(BTN_ADD_ROW, left, y, halfW, BTN_HEIGHT, "+ Add Row");
    addRow.enabled = panel.canAddRow();
    addContentBtn(addRow);
    GuiButton removeRow = new GuiButton(BTN_REMOVE_ROW, left + halfW + 4, y, halfW, BTN_HEIGHT,
        "- Remove Row");
    removeRow.enabled = rows.size() > 0;
    addContentBtn(removeRow);
    y += BTN_HEIGHT + 2;
    addContentBtn(new GuiButton(BTN_EDIT_ROW, left, y, FIELD_WIDTH, BTN_HEIGHT,
        "Edit Selected Row >>"));
  }

  private void buildRowTab(int left, int y, int halfW) {
    clampPanelSelection();
    GuideSignPanel panel = data.getPanels().get(selectedPanel);
    if (panel.getRows().isEmpty()) {
      return;
    }
    clampRowSelection(panel);
    GuideSignRow row = panel.getRows().get(selectedRow);

    GuiButton prevRow = new GuiButton(BTN_ROW_PREV, left, y, 40, BTN_HEIGHT, "< Prev");
    GuiButton nextRow = new GuiButton(BTN_ROW_NEXT, left + FIELD_WIDTH - 40, y, 40, BTN_HEIGHT,
        "Next >");
    prevRow.enabled = selectedRow > 0;
    nextRow.enabled = selectedRow < panel.getRows().size() - 1;
    addContentBtn(prevRow);
    addContentBtn(nextRow);
    y += BTN_HEIGHT + 2;

    addContentBtn(new GuiButton(BTN_VSPACING_DOWN, left, y, 30, BTN_HEIGHT, "-"));
    addContentBtn(
        new GuiButton(BTN_VSPACING_UP, left + FIELD_WIDTH - 30, y, 30, BTN_HEIGHT, "+"));
    y += BTN_HEIGHT + 2;
    addContentBtn(new GuiButton(BTN_ROW_ALIGN, left, y, FIELD_WIDTH, BTN_HEIGHT, ""));
    y += BTN_HEIGHT + 4;

    List<GuideSignElement> elems = row.getElements();
    for (int i = 0; i < elems.size(); i++) {
      int btnY = y + i * (BTN_HEIGHT + 1);
      String label = GuideSignElement.getTypeName(elems.get(i).getType()) + ": "
          + truncate(elems.get(i).getSummary(), 24);
      boolean isSelected = i == selectedElement;
      addContentBtn(new GuiButton(BTN_ADD_ELEMENT + 1000 + i, left, btnY,
          FIELD_WIDTH, BTN_HEIGHT, (isSelected ? "> " : "  ") + label));
    }

    y += Math.max(elems.size(), 1) * (BTN_HEIGHT + 1) + 2;

    int quarterW = (FIELD_WIDTH - 12) / 4;
    GuiButton addElem = new GuiButton(BTN_ADD_ELEMENT, left, y, quarterW, BTN_HEIGHT, "+ Add");
    addElem.enabled = row.canAddElement();
    addContentBtn(addElem);
    GuiButton removeElem = new GuiButton(BTN_REMOVE_ELEMENT, left + quarterW + 4, y, quarterW,
        BTN_HEIGHT, "- Del");
    removeElem.enabled = !elems.isEmpty();
    addContentBtn(removeElem);
    GuiButton upElem = new GuiButton(BTN_ELEM_UP, left + (quarterW + 4) * 2, y, quarterW,
        BTN_HEIGHT, "Up");
    upElem.enabled = selectedElement > 0;
    addContentBtn(upElem);
    GuiButton downElem = new GuiButton(BTN_ELEM_DOWN, left + (quarterW + 4) * 3, y, quarterW,
        BTN_HEIGHT, "Down");
    downElem.enabled = selectedElement < elems.size() - 1;
    addContentBtn(downElem);

    y += BTN_HEIGHT + 6;

    if (!elems.isEmpty() && selectedElement >= 0 && selectedElement < elems.size()) {
      buildElementEditor(left, y, halfW, elems.get(selectedElement));
    }
  }

  private void buildElementEditor(int left, int y, int halfW, GuideSignElement elem) {
    addContentBtn(new GuiButton(BTN_ELEM_TYPE_CYCLE, left, y, FIELD_WIDTH, BTN_HEIGHT, ""));
    y += BTN_HEIGHT + 3;

    switch (elem.getType()) {
      case GuideSignElement.TYPE_TEXT:
        textField = new GuiTextField(70, fontRenderer, left, y, FIELD_WIDTH, BTN_HEIGHT);
        textField.setMaxStringLength(24);
        textField.setText(elem.getText());
        textField.setFocused(true);
        addContentField(textField);
        y += BTN_HEIGHT + 2;
        addContentBtn(
            new GuiButton(BTN_TEXT_SCALE_DOWN, left, y, 30, BTN_HEIGHT, "-"));
        addContentBtn(
            new GuiButton(BTN_TEXT_SCALE_UP, left + FIELD_WIDTH - 30, y, 30, BTN_HEIGHT, "+"));
        break;

      case GuideSignElement.TYPE_SHIELD:
        addContentBtn(
            new GuiButton(BTN_SHIELD_TYPE, left, y, FIELD_WIDTH, BTN_HEIGHT, ""));
        y += BTN_HEIGHT + 2;
        routeField = new GuiTextField(71, fontRenderer, left, y, halfW - 2, BTN_HEIGHT);
        routeField.setMaxStringLength(5);
        routeField.setText(elem.getRouteNumber());
        routeField.setFocused(true);
        addContentField(routeField);
        addContentBtn(
            new GuiButton(BTN_BANNER_TYPE, left + halfW + 2, y, halfW, BTN_HEIGHT, ""));
        break;

      case GuideSignElement.TYPE_ARROW:
        addContentBtn(
            new GuiButton(BTN_ARROW_TYPE, left, y, FIELD_WIDTH, BTN_HEIGHT, ""));
        break;

      case GuideSignElement.TYPE_SPACING:
        addContentBtn(
            new GuiButton(BTN_SPACING_DOWN, left, y, 30, BTN_HEIGHT, "-"));
        addContentBtn(
            new GuiButton(BTN_SPACING_UP, left + FIELD_WIDTH - 30, y, 30, BTN_HEIGHT, "+"));
        break;

      default:
        textField = null;
        routeField = null;
        break;
    }
  }

  private void buildPreviewTab(int left, int y) {
    rebuildPreviewLines();
    int totalHeight = previewLines.size() * PREVIEW_LINE_HEIGHT;
    customContentBottom = y + totalHeight + 4;
  }

  private void rebuildPreviewLines() {
    previewLines.clear();
    previewLines.add(TextFormatting.GOLD + "=== Sign ===");
    previewLines.add("Color: " + data.getSignColor().getFriendlyName());
    previewLines.add("Post: " + data.getPostType().getFriendlyName());
    previewLines.add("Border: " + data.getBorderWidth());
    previewLines.add("Corners: " + data.getCornerStyle().getFriendlyName());
    previewLines.add("");

    List<GuideSignPanel> panels = data.getPanels();
    for (int p = 0; p < panels.size(); p++) {
      GuideSignPanel panel = panels.get(p);
      previewLines.add(TextFormatting.AQUA + "Panel " + (p + 1) + " of " + panels.size());
      if (panel.hasExitTab()) {
        ExitTabData et = panel.getExitTab();
        StringBuilder sb = new StringBuilder("Exit tab: \"");
        sb.append(et.getText()).append("\" ").append(et.getPositionName());
        sb.append(", ").append(et.getGuideSignColor().getFriendlyName());
        if (et.isToll()) sb.append(", toll");
        previewLines.add(sb.toString());
      } else {
        previewLines.add(TextFormatting.GRAY + "(no exit tab)");
      }

      List<GuideSignRow> rows = panel.getRows();
      if (rows.isEmpty()) {
        previewLines.add("  " + TextFormatting.GRAY + "(no rows)");
      }
      for (int r = 0; r < rows.size(); r++) {
        GuideSignRow row = rows.get(r);
        previewLines.add("  Row " + (r + 1) + " ["
            + row.getAlignment().getFriendlyName().toLowerCase()
            + (row.getVerticalSpacing() > 0 ? ", vsp=" + row.getVerticalSpacing() : "")
            + "]");
        List<GuideSignElement> elems = row.getElements();
        if (elems.isEmpty()) {
          previewLines.add("    " + TextFormatting.GRAY + "(empty)");
        }
        for (GuideSignElement e : elems) {
          previewLines.add("    " + GuideSignElement.getTypeName(e.getType()) + ": "
              + e.getSummary());
        }
      }
      if (p < panels.size() - 1) {
        previewLines.add("");
      }
    }
  }

  private void drawPreviewLabels(int left, int y) {
    int lineY = y;
    for (String line : previewLines) {
      drawScrolledString(line, left, lineY, 0xFFFFFF);
      lineY += PREVIEW_LINE_HEIGHT;
    }
  }

  private void updateTabButtonStates() {
    for (GuiButton btn : buttonList) {
      if (btn.id == BTN_TAB_PROPERTIES) {
        btn.enabled = currentTab != TAB_PROPERTIES;
      } else if (btn.id == BTN_TAB_PANEL) {
        btn.enabled = currentTab != TAB_PANEL;
      } else if (btn.id == BTN_TAB_ROW) {
        btn.enabled = currentTab != TAB_ROW;
      } else if (btn.id == BTN_TAB_PREVIEW) {
        btn.enabled = currentTab != TAB_PREVIEW;
      }
    }
  }

  @Override
  public void drawScreen(int mouseX, int mouseY, float partialTicks) {
    drawDefaultBackground();

    ScaledResolution sr = new ScaledResolution(this.mc);
    int centerX = sr.getScaledWidth() / 2;
    int left = centerX - FIELD_WIDTH / 2;

    drawCenteredString(fontRenderer, "Dynamic Highway Guide Sign", centerX, 10, 0x00CC66);

    switch (currentTab) {
      case TAB_PROPERTIES:
        drawPropertiesLabels(left, viewportTop, centerX);
        break;
      case TAB_PANEL:
        drawPanelLabels(left, viewportTop, centerX);
        if (exitTextField != null && exitTextField.getVisible()) {
          exitTextField.drawTextBox();
        }
        break;
      case TAB_ROW:
        drawRowLabels(left, viewportTop, centerX);
        if (textField != null && textField.getVisible()) {
          textField.drawTextBox();
        }
        if (routeField != null && routeField.getVisible()) {
          routeField.drawTextBox();
        }
        break;
      case TAB_PREVIEW:
        drawPreviewLabels(left, viewportTop);
        break;
    }

    super.drawScreen(mouseX, mouseY, partialTicks);

    if (tabContentMaxScroll > 0) {
      int trackX = left + FIELD_WIDTH + 4;
      int trackHeight = viewportBottom - viewportTop;
      int viewportH = trackHeight;
      int totalContent = trackHeight + tabContentMaxScroll;
      int thumbHeight = Math.max(8, (viewportH * trackHeight) / totalContent);
      int thumbTrack = trackHeight - thumbHeight;
      int thumbY = viewportTop
          + (tabContentMaxScroll == 0 ? 0 : thumbTrack * tabContentScroll / tabContentMaxScroll);
      drawRect(trackX, viewportTop, trackX + SCROLLBAR_WIDTH, viewportTop + trackHeight,
          0x55000000);
      drawRect(trackX, thumbY, trackX + SCROLLBAR_WIDTH, thumbY + thumbHeight, 0xCCAAAAAA);
    }
  }

  private void drawScrolledCenteredString(String text, int x, int naturalY, int color) {
    int y = naturalY - tabContentScroll;
    if (y + fontRenderer.FONT_HEIGHT > viewportTop && y < viewportBottom) {
      drawCenteredString(fontRenderer, text, x, y, color);
    }
  }

  private void drawScrolledString(String text, int x, int naturalY, int color) {
    int y = naturalY - tabContentScroll;
    if (y + fontRenderer.FONT_HEIGHT > viewportTop && y < viewportBottom) {
      drawString(fontRenderer, text, x, y, color);
    }
  }

  private void drawPropertiesLabels(int left, int y, int centerX) {
    for (GuiButton btn : buttonList) {
      if (btn.id == BTN_SIGN_COLOR) {
        btn.displayString = "Color: " + data.getSignColor().getFriendlyName();
      } else if (btn.id == BTN_POST_TYPE) {
        btn.displayString = "Post: " + data.getPostType().getFriendlyName();
      } else if (btn.id == BTN_CORNER_STYLE) {
        btn.displayString = "Corners: " + data.getCornerStyle().getFriendlyName();
      } else if (btn.id == BTN_TEMPLATE) {
        btn.displayString = "Apply Template: " + SignTemplates.getName(templateIndex);
      }
    }
    drawScrolledCenteredString("Border: " + data.getBorderWidth(), centerX,
        y + (BTN_HEIGHT + 3) * 2 + 5, 0xFFFFFF);
    drawScrolledCenteredString("Min Width: " + data.getMinWidth(), centerX,
        y + (BTN_HEIGHT + 3) * 3 + 5, 0xFFFFFF);

    drawScrolledString("Panels: " + data.getPanels().size(), left,
        y + (BTN_HEIGHT + 3) * 5 - 8, 0xAAAAAA);
  }

  private void drawPanelLabels(int left, int y, int centerX) {
    clampPanelSelection();
    String panelLabel = "Panel " + (selectedPanel + 1) + " of " + data.getPanels().size();
    drawScrolledCenteredString(panelLabel, centerX, y + 5, 0xFFFFFF);

    GuideSignPanel panel = data.getPanels().get(selectedPanel);
    for (GuiButton btn : buttonList) {
      if (btn.id == BTN_EXIT_TAB_TOGGLE) {
        btn.displayString = panel.hasExitTab() ? "Exit Tab: ON" : "Exit Tab: OFF";
      } else if (btn.id == BTN_EXIT_TAB_POS && panel.hasExitTab()) {
        btn.displayString = "Pos: " + panel.getExitTab().getPositionName();
      } else if (btn.id == BTN_EXIT_TAB_COLOR && panel.hasExitTab()) {
        btn.displayString = "Color: " + panel.getExitTab().getGuideSignColor().getFriendlyName();
      } else if (btn.id == BTN_EXIT_TAB_TOLL && panel.hasExitTab()) {
        btn.displayString = "Toll: " + (panel.getExitTab().isToll() ? "Yes" : "No");
      }
    }
  }

  private void drawRowLabels(int left, int y, int centerX) {
    clampPanelSelection();
    GuideSignPanel panel = data.getPanels().get(selectedPanel);
    if (panel.getRows().isEmpty()) {
      drawScrolledCenteredString("No rows - go to Panel tab to add", centerX, y + 20,
          0xFF6666);
      return;
    }
    clampRowSelection(panel);
    GuideSignRow row = panel.getRows().get(selectedRow);

    String rowLabel =
        "Panel " + (selectedPanel + 1) + " / Row " + (selectedRow + 1) + " of " + panel.getRows()
            .size();
    drawScrolledCenteredString(rowLabel, centerX, y + 5, 0xFFFFFF);
    y += BTN_HEIGHT + 2;
    drawScrolledCenteredString("V-Spacing: " + row.getVerticalSpacing(), centerX,
        y + 5, 0xFFFFFF);
    y += BTN_HEIGHT + 2;
    for (GuiButton btn : buttonList) {
      if (btn.id == BTN_ROW_ALIGN) {
        btn.displayString = "Align: " + row.getAlignment().getFriendlyName();
      }
    }
    y += BTN_HEIGHT + 4;

    List<GuideSignElement> elems = row.getElements();
    y += Math.max(elems.size(), 1) * (BTN_HEIGHT + 1) + 2;
    y += BTN_HEIGHT + 6;

    if (!elems.isEmpty() && selectedElement >= 0 && selectedElement < elems.size()) {
      GuideSignElement elem = elems.get(selectedElement);
      for (GuiButton btn : buttonList) {
        if (btn.id == BTN_ELEM_TYPE_CYCLE) {
          btn.displayString = "Type: " + GuideSignElement.getTypeName(elem.getType());
        } else if (btn.id == BTN_SHIELD_TYPE) {
          btn.displayString = "Shield: " + elem.getGuideSignShieldType().getFriendlyName();
        } else if (btn.id == BTN_BANNER_TYPE) {
          btn.displayString = "Banner: " + elem.getGuideSignBannerType().getFriendlyName();
        } else if (btn.id == BTN_ARROW_TYPE) {
          btn.displayString = "Arrow: " + elem.getGuideSignArrowType().getFriendlyName();
        }
      }

      if (elem.getType() == GuideSignElement.TYPE_TEXT) {
        drawScrolledCenteredString("Scale: " + String.format("%.1f", elem.getTextScale()),
            left + FIELD_WIDTH / 2,
            y + BTN_HEIGHT + 2 + BTN_HEIGHT + 2 + 5, 0xFFFFFF);
      } else if (elem.getType() == GuideSignElement.TYPE_SPACING) {
        drawScrolledCenteredString("Width: " + elem.getSpacingWidth(),
            left + FIELD_WIDTH / 2, y + BTN_HEIGHT + 3 + 5, 0xFFFFFF);
      }
    }
  }

  @Override
  protected void keyTyped(char typedChar, int keyCode) throws IOException {
    if (keyCode == Keyboard.KEY_ESCAPE) {
      this.mc.displayGuiScreen(null);
      return;
    }
    if (textField != null && textField.getVisible()) {
      textField.textboxKeyTyped(typedChar, keyCode);
      syncTextFieldToElement();
    }
    if (routeField != null && routeField.getVisible()) {
      routeField.textboxKeyTyped(typedChar, keyCode);
      syncRouteFieldToElement();
    }
    if (exitTextField != null && exitTextField.getVisible()) {
      exitTextField.textboxKeyTyped(typedChar, keyCode);
      syncExitTextFieldToPanel();
    }
  }

  @Override
  protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
    if (textField != null && textField.getVisible()) {
      textField.mouseClicked(mouseX, mouseY, mouseButton);
    }
    if (routeField != null && routeField.getVisible()) {
      routeField.mouseClicked(mouseX, mouseY, mouseButton);
    }
    if (exitTextField != null && exitTextField.getVisible()) {
      exitTextField.mouseClicked(mouseX, mouseY, mouseButton);
    }

    for (GuiButton btn : buttonList) {
      int btnId = btn.id;
      if (btnId >= BTN_EDIT_ROW + 1000 && btnId < BTN_EDIT_ROW + 2000) {
        if (mouseX >= btn.x && mouseX < btn.x + btn.width
            && mouseY >= btn.y && mouseY < btn.y + btn.height) {
          selectedRow = btnId - (BTN_EDIT_ROW + 1000);
          initGui();
          return;
        }
      }
      if (btnId >= BTN_ADD_ELEMENT + 1000 && btnId < BTN_ADD_ELEMENT + 2000) {
        if (mouseX >= btn.x && mouseX < btn.x + btn.width
            && mouseY >= btn.y && mouseY < btn.y + btn.height) {
          selectedElement = btnId - (BTN_ADD_ELEMENT + 1000);
          initGui();
          return;
        }
      }
    }

    super.mouseClicked(mouseX, mouseY, mouseButton);
  }

  @Override
  public void handleMouseInput() throws IOException {
    super.handleMouseInput();
    int scroll = Mouse.getEventDWheel();
    if (scroll != 0 && tabContentMaxScroll > 0) {
      int delta = scroll > 0 ? -SCROLL_STEP : SCROLL_STEP;
      int prev = tabContentScroll;
      tabContentScroll = Math.max(0, Math.min(tabContentMaxScroll, tabContentScroll + delta));
      if (tabContentScroll != prev) {
        applyTabScroll();
      }
    }
  }

  @Override
  public void updateScreen() {
    if (textField != null) textField.updateCursorCounter();
    if (routeField != null) routeField.updateCursorCounter();
    if (exitTextField != null) exitTextField.updateCursorCounter();
  }

  @Override
  @ParametersAreNonnullByDefault
  protected void actionPerformed(GuiButton button) {
    switch (button.id) {
      case BTN_TAB_PROPERTIES:
        syncAllFields();
        currentTab = TAB_PROPERTIES;
        tabContentScroll = 0;
        initGui();
        break;
      case BTN_TAB_PANEL:
        syncAllFields();
        currentTab = TAB_PANEL;
        tabContentScroll = 0;
        initGui();
        break;
      case BTN_TAB_ROW:
        syncAllFields();
        currentTab = TAB_ROW;
        tabContentScroll = 0;
        initGui();
        break;
      case BTN_TAB_PREVIEW:
        syncAllFields();
        currentTab = TAB_PREVIEW;
        tabContentScroll = 0;
        initGui();
        break;

      case BTN_SAVE:
        syncAllFields();
        CsmNetwork.sendToServer(
            new DynamicGuideSignUpdatePacket(tileEntity.getPos(), data.toJson()));
        this.mc.displayGuiScreen(null);
        break;
      case BTN_CANCEL:
        this.mc.displayGuiScreen(null);
        break;

      case BTN_SIGN_COLOR:
        data.cycleSignColor();
        break;
      case BTN_POST_TYPE:
        data.cyclePostType();
        break;
      case BTN_BORDER_DOWN:
        data.setBorderWidth(data.getBorderWidth() - 1);
        break;
      case BTN_BORDER_UP:
        data.setBorderWidth(data.getBorderWidth() + 1);
        break;
      case BTN_MIN_WIDTH_DOWN:
        data.setMinWidth(data.getMinWidth() - 4);
        break;
      case BTN_MIN_WIDTH_UP:
        data.setMinWidth(data.getMinWidth() + 4);
        break;
      case BTN_CORNER_STYLE:
        data.cycleCornerStyle();
        break;
      case BTN_ADD_PANEL:
        data.addPanel();
        break;
      case BTN_REMOVE_PANEL:
        data.removePanel(selectedPanel);
        clampPanelSelection();
        initGui();
        break;
      case BTN_TEMPLATE:
        data = SignTemplates.get(templateIndex);
        templateIndex = (templateIndex + 1) % SignTemplates.count();
        selectedPanel = 0;
        selectedRow = 0;
        selectedElement = 0;
        initGui();
        break;
      case BTN_COPY:
        syncAllFields();
        clipboardJson = data.toJson();
        initGui();
        break;
      case BTN_PASTE:
        if (clipboardJson != null) {
          data = GuideSignData.fromJson(clipboardJson);
          selectedPanel = 0;
          selectedRow = 0;
          selectedElement = 0;
          initGui();
        }
        break;

      case BTN_PANEL_PREV:
        if (selectedPanel > 0) {
          selectedPanel--;
          tabContentScroll = 0;
          initGui();
        }
        break;
      case BTN_PANEL_NEXT:
        if (selectedPanel < data.getPanels().size() - 1) {
          selectedPanel++;
          tabContentScroll = 0;
          initGui();
        }
        break;
      case BTN_EXIT_TAB_TOGGLE: {
        GuideSignPanel panel = data.getPanels().get(selectedPanel);
        if (panel.hasExitTab()) {
          panel.disableExitTab();
        } else {
          panel.enableExitTab();
        }
        initGui();
        break;
      }
      case BTN_EXIT_TAB_POS: {
        GuideSignPanel panel = data.getPanels().get(selectedPanel);
        if (panel.hasExitTab()) {
          panel.getExitTab().cyclePosition();
        }
        break;
      }
      case BTN_EXIT_TAB_COLOR: {
        GuideSignPanel panel = data.getPanels().get(selectedPanel);
        if (panel.hasExitTab()) {
          panel.getExitTab().cycleColor();
        }
        break;
      }
      case BTN_EXIT_TAB_TOLL: {
        GuideSignPanel panel = data.getPanels().get(selectedPanel);
        if (panel.hasExitTab()) {
          panel.getExitTab().setToll(!panel.getExitTab().isToll());
        }
        break;
      }
      case BTN_ADD_ROW: {
        GuideSignPanel panel = data.getPanels().get(selectedPanel);
        GuideSignRow newRow = new GuideSignRow();
        newRow.addElement(GuideSignElement.createText("", 1.0f));
        panel.addRow(newRow);
        selectedRow = panel.getRows().size() - 1;
        initGui();
        break;
      }
      case BTN_REMOVE_ROW: {
        GuideSignPanel panel = data.getPanels().get(selectedPanel);
        panel.removeRow(selectedRow);
        clampRowSelection(panel);
        initGui();
        break;
      }
      case BTN_EDIT_ROW:
        syncAllFields();
        currentTab = TAB_ROW;
        tabContentScroll = 0;
        selectedElement = 0;
        initGui();
        break;

      case BTN_ROW_PREV:
        if (selectedRow > 0) {
          syncAllFields();
          selectedRow--;
          selectedElement = 0;
          tabContentScroll = 0;
          initGui();
        }
        break;
      case BTN_ROW_NEXT: {
        GuideSignPanel panel = data.getPanels().get(selectedPanel);
        if (selectedRow < panel.getRows().size() - 1) {
          syncAllFields();
          selectedRow++;
          selectedElement = 0;
          tabContentScroll = 0;
          initGui();
        }
        break;
      }
      case BTN_VSPACING_DOWN: {
        GuideSignPanel panel = data.getPanels().get(selectedPanel);
        if (!panel.getRows().isEmpty()) {
          clampRowSelection(panel);
          GuideSignRow row = panel.getRows().get(selectedRow);
          row.setVerticalSpacing(row.getVerticalSpacing() - 1);
        }
        break;
      }
      case BTN_VSPACING_UP: {
        GuideSignPanel panel = data.getPanels().get(selectedPanel);
        if (!panel.getRows().isEmpty()) {
          clampRowSelection(panel);
          GuideSignRow row = panel.getRows().get(selectedRow);
          row.setVerticalSpacing(row.getVerticalSpacing() + 1);
        }
        break;
      }
      case BTN_ROW_ALIGN: {
        GuideSignRow row = getSelectedRow();
        if (row != null) {
          row.cycleAlignment();
        }
        break;
      }
      case BTN_ADD_ELEMENT: {
        GuideSignRow row = getSelectedRow();
        if (row != null) {
          row.addElement(GuideSignElement.createText("", 1.0f));
          selectedElement = row.getElements().size() - 1;
          initGui();
        }
        break;
      }
      case BTN_REMOVE_ELEMENT: {
        GuideSignRow row = getSelectedRow();
        if (row != null && !row.getElements().isEmpty()) {
          row.removeElement(selectedElement);
          if (selectedElement >= row.getElements().size() && !row.getElements().isEmpty()) {
            selectedElement = row.getElements().size() - 1;
          }
          initGui();
        }
        break;
      }
      case BTN_ELEM_UP: {
        GuideSignRow row = getSelectedRow();
        if (row != null) {
          row.moveElementUp(selectedElement);
          selectedElement--;
          initGui();
        }
        break;
      }
      case BTN_ELEM_DOWN: {
        GuideSignRow row = getSelectedRow();
        if (row != null) {
          row.moveElementDown(selectedElement);
          selectedElement++;
          initGui();
        }
        break;
      }
      case BTN_ELEM_TYPE_CYCLE: {
        GuideSignElement elem = getSelectedElement();
        if (elem != null) {
          syncAllFields();
          int nextType = (elem.getType() + 1) % GuideSignElement.TYPE_COUNT;
          elem.setType(nextType);
          initGui();
        }
        break;
      }
      case BTN_TEXT_SCALE_DOWN: {
        GuideSignElement elem = getSelectedElement();
        if (elem != null) {
          elem.setTextScale(elem.getTextScale() - 0.1f);
        }
        break;
      }
      case BTN_TEXT_SCALE_UP: {
        GuideSignElement elem = getSelectedElement();
        if (elem != null) {
          elem.setTextScale(elem.getTextScale() + 0.1f);
        }
        break;
      }
      case BTN_SHIELD_TYPE: {
        GuideSignElement elem = getSelectedElement();
        if (elem != null) {
          elem.setShieldType(elem.getGuideSignShieldType().next().ordinal());
        }
        break;
      }
      case BTN_BANNER_TYPE: {
        GuideSignElement elem = getSelectedElement();
        if (elem != null) {
          elem.setBannerType(elem.getGuideSignBannerType().next().ordinal());
        }
        break;
      }
      case BTN_ARROW_TYPE: {
        GuideSignElement elem = getSelectedElement();
        if (elem != null) {
          elem.setArrowType(elem.getGuideSignArrowType().next().ordinal());
        }
        break;
      }
      case BTN_SPACING_DOWN: {
        GuideSignElement elem = getSelectedElement();
        if (elem != null) {
          elem.setSpacingWidth(elem.getSpacingWidth() - 1);
        }
        break;
      }
      case BTN_SPACING_UP: {
        GuideSignElement elem = getSelectedElement();
        if (elem != null) {
          elem.setSpacingWidth(elem.getSpacingWidth() + 1);
        }
        break;
      }
    }
  }

  private void syncTextFieldToElement() {
    GuideSignElement elem = getSelectedElement();
    if (elem != null && textField != null && elem.getType() == GuideSignElement.TYPE_TEXT) {
      elem.setText(textField.getText());
    }
  }

  private void syncRouteFieldToElement() {
    GuideSignElement elem = getSelectedElement();
    if (elem != null && routeField != null && elem.getType() == GuideSignElement.TYPE_SHIELD) {
      elem.setRouteNumber(routeField.getText());
    }
  }

  private void syncExitTextFieldToPanel() {
    clampPanelSelection();
    GuideSignPanel panel = data.getPanels().get(selectedPanel);
    if (panel.hasExitTab() && exitTextField != null) {
      panel.getExitTab().setText(exitTextField.getText());
    }
  }

  private void syncAllFields() {
    syncTextFieldToElement();
    syncRouteFieldToElement();
    syncExitTextFieldToPanel();
  }

  private GuideSignRow getSelectedRow() {
    clampPanelSelection();
    GuideSignPanel panel = data.getPanels().get(selectedPanel);
    if (panel.getRows().isEmpty()) return null;
    clampRowSelection(panel);
    return panel.getRows().get(selectedRow);
  }

  private GuideSignElement getSelectedElement() {
    GuideSignRow row = getSelectedRow();
    if (row == null || row.getElements().isEmpty()) return null;
    if (selectedElement < 0) selectedElement = 0;
    if (selectedElement >= row.getElements().size()) {
      selectedElement = row.getElements().size() - 1;
    }
    return row.getElements().get(selectedElement);
  }

  private void clampPanelSelection() {
    if (selectedPanel < 0) selectedPanel = 0;
    if (selectedPanel >= data.getPanels().size()) {
      selectedPanel = data.getPanels().size() - 1;
    }
  }

  private void clampRowSelection(GuideSignPanel panel) {
    if (selectedRow < 0) selectedRow = 0;
    if (selectedRow >= panel.getRows().size()) {
      selectedRow = Math.max(0, panel.getRows().size() - 1);
    }
  }

  private String truncate(String s, int maxLen) {
    if (s.length() > maxLen) {
      return s.substring(0, maxLen - 2) + "..";
    }
    return s;
  }

  @Override
  public boolean doesGuiPauseGame() {
    return true;
  }

  @Override
  public void onGuiClosed() {
    Keyboard.enableRepeatEvents(false);
  }
}

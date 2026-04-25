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
import java.io.IOException;
import java.util.List;
import javax.annotation.ParametersAreNonnullByDefault;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.gui.ScaledResolution;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

public class DynamicGuideSignGui extends GuiScreen {

  private static final int TAB_PROPERTIES = 0;
  private static final int TAB_PANEL = 1;
  private static final int TAB_ROW = 2;

  private static final int BTN_TAB_PROPERTIES = 100;
  private static final int BTN_TAB_PANEL = 101;
  private static final int BTN_TAB_ROW = 102;
  private static final int BTN_SAVE = 200;
  private static final int BTN_CANCEL = 201;

  private static final int BTN_SIGN_COLOR = 10;
  private static final int BTN_POST_TYPE = 11;
  private static final int BTN_BORDER_DOWN = 12;
  private static final int BTN_BORDER_UP = 13;
  private static final int BTN_CORNER_STYLE = 14;
  private static final int BTN_ADD_PANEL = 15;
  private static final int BTN_REMOVE_PANEL = 16;

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

  private static final int FIELD_WIDTH = 240;
  private static final int BTN_HEIGHT = 18;

  private final TileEntityDynamicGuideSign tileEntity;
  private GuideSignData data;
  private int currentTab = TAB_PROPERTIES;
  private int selectedPanel = 0;
  private int selectedRow = 0;
  private int selectedElement = 0;
  private int rowScrollOffset = 0;
  private int elemScrollOffset = 0;

  private GuiTextField textField;
  private GuiTextField routeField;
  private GuiTextField exitTextField;

  public DynamicGuideSignGui(TileEntityDynamicGuideSign tileEntity) {
    this.tileEntity = tileEntity;
    this.data = tileEntity.getSignData().copy();
  }

  @Override
  public void initGui() {
    Keyboard.enableRepeatEvents(true);
    super.initGui();
    buttonList.clear();

    ScaledResolution sr = new ScaledResolution(this.mc);
    int centerX = sr.getScaledWidth() / 2;
    int left = centerX - FIELD_WIDTH / 2;
    int tabWidth = FIELD_WIDTH / 3;
    int startY = 25;

    buttonList.add(new GuiButton(BTN_TAB_PROPERTIES, left, startY, tabWidth, BTN_HEIGHT,
        "Properties"));
    buttonList.add(new GuiButton(BTN_TAB_PANEL, left + tabWidth, startY, tabWidth, BTN_HEIGHT,
        "Panels"));
    buttonList.add(new GuiButton(BTN_TAB_ROW, left + tabWidth * 2, startY, tabWidth, BTN_HEIGHT,
        "Elements"));

    int contentY = startY + BTN_HEIGHT + 6;
    int halfW = (FIELD_WIDTH - 4) / 2;
    int thirdW = (FIELD_WIDTH - 8) / 3;

    switch (currentTab) {
      case TAB_PROPERTIES:
        buildPropertiesTab(left, contentY, halfW);
        break;
      case TAB_PANEL:
        buildPanelTab(left, contentY, halfW);
        break;
      case TAB_ROW:
        buildRowTab(left, contentY, halfW);
        break;
    }

    int bottomY = sr.getScaledHeight() - 30;
    buttonList.add(new GuiButton(BTN_SAVE, left, bottomY, halfW, BTN_HEIGHT, "Save"));
    buttonList.add(
        new GuiButton(BTN_CANCEL, left + halfW + 4, bottomY, halfW, BTN_HEIGHT, "Cancel"));

    updateTabButtonStates();
  }

  private void buildPropertiesTab(int left, int y, int halfW) {
    buttonList.add(new GuiButton(BTN_SIGN_COLOR, left, y, FIELD_WIDTH, BTN_HEIGHT, ""));
    y += BTN_HEIGHT + 3;
    buttonList.add(new GuiButton(BTN_POST_TYPE, left, y, FIELD_WIDTH, BTN_HEIGHT, ""));
    y += BTN_HEIGHT + 3;
    buttonList.add(new GuiButton(BTN_BORDER_DOWN, left, y, 30, BTN_HEIGHT, "-"));
    buttonList.add(new GuiButton(BTN_BORDER_UP, left + FIELD_WIDTH - 30, y, 30, BTN_HEIGHT, "+"));
    y += BTN_HEIGHT + 3;
    buttonList.add(new GuiButton(BTN_CORNER_STYLE, left, y, FIELD_WIDTH, BTN_HEIGHT, ""));
    y += BTN_HEIGHT + 6;
    buttonList.add(new GuiButton(BTN_ADD_PANEL, left, y, halfW, BTN_HEIGHT, "+ Add Panel"));
    GuiButton removePanel = new GuiButton(BTN_REMOVE_PANEL, left + halfW + 4, y, halfW,
        BTN_HEIGHT, "- Remove Panel");
    removePanel.enabled = data.getPanels().size() > 1;
    buttonList.add(removePanel);
  }

  private void buildPanelTab(int left, int y, int halfW) {
    clampPanelSelection();
    GuiButton prevPnl = new GuiButton(BTN_PANEL_PREV, left, y, 40, BTN_HEIGHT, "< Prev");
    GuiButton nextPnl = new GuiButton(BTN_PANEL_NEXT, left + FIELD_WIDTH - 40, y, 40, BTN_HEIGHT,
        "Next >");
    prevPnl.enabled = selectedPanel > 0;
    nextPnl.enabled = selectedPanel < data.getPanels().size() - 1;
    buttonList.add(prevPnl);
    buttonList.add(nextPnl);
    y += BTN_HEIGHT + 4;

    GuideSignPanel panel = data.getPanels().get(selectedPanel);

    buttonList.add(
        new GuiButton(BTN_EXIT_TAB_TOGGLE, left, y, FIELD_WIDTH, BTN_HEIGHT, ""));
    y += BTN_HEIGHT + 2;

    if (panel.hasExitTab()) {
      exitTextField = new GuiTextField(60, fontRenderer, left, y, halfW - 2, BTN_HEIGHT);
      exitTextField.setMaxStringLength(10);
      exitTextField.setText(panel.getExitTab().getText());
      buttonList.add(
          new GuiButton(BTN_EXIT_TAB_POS, left + halfW + 2, y, halfW, BTN_HEIGHT, ""));
      y += BTN_HEIGHT + 2;
      buttonList.add(
          new GuiButton(BTN_EXIT_TAB_COLOR, left, y, halfW - 2, BTN_HEIGHT, ""));
      buttonList.add(
          new GuiButton(BTN_EXIT_TAB_TOLL, left + halfW + 2, y, halfW, BTN_HEIGHT, ""));
      y += BTN_HEIGHT + 2;
    } else {
      exitTextField = null;
    }

    y += 4;

    int maxVisibleRows = 4;
    List<GuideSignRow> rows = panel.getRows();
    for (int i = rowScrollOffset; i < Math.min(rows.size(), rowScrollOffset + maxVisibleRows);
        i++) {
      int btnY = y + (i - rowScrollOffset) * (BTN_HEIGHT + 1);
      String label = "Row " + (i + 1) + ": " + truncate(rows.get(i).getSummary(), 28);
      boolean isSelected = i == selectedRow;
      GuiButton rowBtn = new GuiButton(BTN_EDIT_ROW + 1000 + i, left, btnY,
          FIELD_WIDTH, BTN_HEIGHT, (isSelected ? "> " : "  ") + label);
      buttonList.add(rowBtn);
    }

    y += maxVisibleRows * (BTN_HEIGHT + 1) + 3;
    GuiButton addRow = new GuiButton(BTN_ADD_ROW, left, y, halfW, BTN_HEIGHT, "+ Add Row");
    addRow.enabled = panel.canAddRow();
    buttonList.add(addRow);
    GuiButton removeRow = new GuiButton(BTN_REMOVE_ROW, left + halfW + 4, y, halfW, BTN_HEIGHT,
        "- Remove Row");
    removeRow.enabled = rows.size() > 0;
    buttonList.add(removeRow);
    y += BTN_HEIGHT + 2;
    buttonList.add(new GuiButton(BTN_EDIT_ROW, left, y, FIELD_WIDTH, BTN_HEIGHT,
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
    buttonList.add(prevRow);
    buttonList.add(nextRow);
    y += BTN_HEIGHT + 2;

    buttonList.add(new GuiButton(BTN_VSPACING_DOWN, left, y, 30, BTN_HEIGHT, "-"));
    buttonList.add(
        new GuiButton(BTN_VSPACING_UP, left + FIELD_WIDTH - 30, y, 30, BTN_HEIGHT, "+"));
    y += BTN_HEIGHT + 4;

    int maxVisibleElems = 3;
    List<GuideSignElement> elems = row.getElements();
    for (int i = elemScrollOffset;
        i < Math.min(elems.size(), elemScrollOffset + maxVisibleElems); i++) {
      int btnY = y + (i - elemScrollOffset) * (BTN_HEIGHT + 1);
      String label = GuideSignElement.getTypeName(elems.get(i).getType()) + ": "
          + truncate(elems.get(i).getSummary(), 24);
      boolean isSelected = i == selectedElement;
      GuiButton elemBtn = new GuiButton(BTN_ADD_ELEMENT + 1000 + i, left, btnY,
          FIELD_WIDTH, BTN_HEIGHT, (isSelected ? "> " : "  ") + label);
      buttonList.add(elemBtn);
    }

    y += maxVisibleElems * (BTN_HEIGHT + 1) + 2;

    int quarterW = (FIELD_WIDTH - 12) / 4;
    GuiButton addElem = new GuiButton(BTN_ADD_ELEMENT, left, y, quarterW, BTN_HEIGHT, "+ Add");
    addElem.enabled = row.canAddElement();
    buttonList.add(addElem);
    GuiButton removeElem = new GuiButton(BTN_REMOVE_ELEMENT, left + quarterW + 4, y, quarterW,
        BTN_HEIGHT, "- Del");
    removeElem.enabled = !elems.isEmpty();
    buttonList.add(removeElem);
    GuiButton upElem = new GuiButton(BTN_ELEM_UP, left + (quarterW + 4) * 2, y, quarterW,
        BTN_HEIGHT, "Up");
    upElem.enabled = selectedElement > 0;
    buttonList.add(upElem);
    GuiButton downElem = new GuiButton(BTN_ELEM_DOWN, left + (quarterW + 4) * 3, y, quarterW,
        BTN_HEIGHT, "Down");
    downElem.enabled = selectedElement < elems.size() - 1;
    buttonList.add(downElem);

    y += BTN_HEIGHT + 6;

    if (!elems.isEmpty() && selectedElement >= 0 && selectedElement < elems.size()) {
      buildElementEditor(left, y, halfW, elems.get(selectedElement));
    }
  }

  private void buildElementEditor(int left, int y, int halfW, GuideSignElement elem) {
    buttonList.add(new GuiButton(BTN_ELEM_TYPE_CYCLE, left, y, FIELD_WIDTH, BTN_HEIGHT, ""));
    y += BTN_HEIGHT + 3;

    switch (elem.getType()) {
      case GuideSignElement.TYPE_TEXT:
        textField = new GuiTextField(70, fontRenderer, left, y, FIELD_WIDTH, BTN_HEIGHT);
        textField.setMaxStringLength(24);
        textField.setText(elem.getText());
        textField.setFocused(true);
        y += BTN_HEIGHT + 2;
        buttonList.add(
            new GuiButton(BTN_TEXT_SCALE_DOWN, left, y, 30, BTN_HEIGHT, "-"));
        buttonList.add(
            new GuiButton(BTN_TEXT_SCALE_UP, left + FIELD_WIDTH - 30, y, 30, BTN_HEIGHT, "+"));
        break;

      case GuideSignElement.TYPE_SHIELD:
        buttonList.add(
            new GuiButton(BTN_SHIELD_TYPE, left, y, FIELD_WIDTH, BTN_HEIGHT, ""));
        y += BTN_HEIGHT + 2;
        routeField = new GuiTextField(71, fontRenderer, left, y, halfW - 2, BTN_HEIGHT);
        routeField.setMaxStringLength(5);
        routeField.setText(elem.getRouteNumber());
        routeField.setFocused(true);
        buttonList.add(
            new GuiButton(BTN_BANNER_TYPE, left + halfW + 2, y, halfW, BTN_HEIGHT, ""));
        break;

      case GuideSignElement.TYPE_ARROW:
        buttonList.add(
            new GuiButton(BTN_ARROW_TYPE, left, y, FIELD_WIDTH, BTN_HEIGHT, ""));
        break;

      case GuideSignElement.TYPE_SPACING:
        buttonList.add(
            new GuiButton(BTN_SPACING_DOWN, left, y, 30, BTN_HEIGHT, "-"));
        buttonList.add(
            new GuiButton(BTN_SPACING_UP, left + FIELD_WIDTH - 30, y, 30, BTN_HEIGHT, "+"));
        break;

      default:
        textField = null;
        routeField = null;
        break;
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
      }
    }
  }

  @Override
  public void drawScreen(int mouseX, int mouseY, float partialTicks) {
    drawDefaultBackground();

    ScaledResolution sr = new ScaledResolution(this.mc);
    int centerX = sr.getScaledWidth() / 2;
    int left = centerX - FIELD_WIDTH / 2;
    int startY = 25;

    drawCenteredString(fontRenderer, "Dynamic Highway Guide Sign", centerX, 10, 0x00CC66);

    int contentY = startY + BTN_HEIGHT + 6;

    switch (currentTab) {
      case TAB_PROPERTIES:
        drawPropertiesLabels(left, contentY, centerX);
        break;
      case TAB_PANEL:
        drawPanelLabels(left, contentY, centerX);
        if (exitTextField != null) {
          exitTextField.drawTextBox();
        }
        break;
      case TAB_ROW:
        drawRowLabels(left, contentY, centerX);
        if (textField != null) {
          textField.drawTextBox();
        }
        if (routeField != null) {
          routeField.drawTextBox();
        }
        break;
    }

    super.drawScreen(mouseX, mouseY, partialTicks);
  }

  private void drawPropertiesLabels(int left, int y, int centerX) {
    for (GuiButton btn : buttonList) {
      if (btn.id == BTN_SIGN_COLOR) {
        btn.displayString = "Color: " + data.getSignColor().getFriendlyName();
      } else if (btn.id == BTN_POST_TYPE) {
        btn.displayString = "Post: " + data.getPostType().getFriendlyName();
      } else if (btn.id == BTN_CORNER_STYLE) {
        btn.displayString = "Corners: " + data.getCornerStyle().getFriendlyName();
      }
    }
    drawCenteredString(fontRenderer, "Border: " + data.getBorderWidth(), centerX,
        y + (BTN_HEIGHT + 3) * 2 + 5, 0xFFFFFF);

    drawString(fontRenderer, "Panels: " + data.getPanels().size(), left,
        y + (BTN_HEIGHT + 3) * 4 - 8, 0xAAAAAA);
  }

  private void drawPanelLabels(int left, int y, int centerX) {
    clampPanelSelection();
    String panelLabel = "Panel " + (selectedPanel + 1) + " of " + data.getPanels().size();
    drawCenteredString(fontRenderer, panelLabel, centerX, y + 5, 0xFFFFFF);

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
      drawCenteredString(fontRenderer, "No rows - go to Panel tab to add", centerX, y + 20,
          0xFF6666);
      return;
    }
    clampRowSelection(panel);
    GuideSignRow row = panel.getRows().get(selectedRow);

    String rowLabel =
        "Panel " + (selectedPanel + 1) + " / Row " + (selectedRow + 1) + " of " + panel.getRows()
            .size();
    drawCenteredString(fontRenderer, rowLabel, centerX, y + 5, 0xFFFFFF);
    y += BTN_HEIGHT + 2;
    drawCenteredString(fontRenderer, "V-Spacing: " + row.getVerticalSpacing(), centerX,
        y + 5, 0xFFFFFF);
    y += BTN_HEIGHT + 4;

    int maxVisibleElems = 3;
    y += maxVisibleElems * (BTN_HEIGHT + 1) + 2;
    y += BTN_HEIGHT + 6;

    List<GuideSignElement> elems = row.getElements();
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
        drawCenteredString(fontRenderer, "Scale: " + String.format("%.1f", elem.getTextScale()),
            left + FIELD_WIDTH / 2,
            y + BTN_HEIGHT + 2 + BTN_HEIGHT + 2 + 5, 0xFFFFFF);
      } else if (elem.getType() == GuideSignElement.TYPE_SPACING) {
        drawCenteredString(fontRenderer, "Width: " + elem.getSpacingWidth(),
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
    if (textField != null) {
      textField.textboxKeyTyped(typedChar, keyCode);
      syncTextFieldToElement();
    }
    if (routeField != null) {
      routeField.textboxKeyTyped(typedChar, keyCode);
      syncRouteFieldToElement();
    }
    if (exitTextField != null) {
      exitTextField.textboxKeyTyped(typedChar, keyCode);
      syncExitTextFieldToPanel();
    }
  }

  @Override
  protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
    if (textField != null) {
      textField.mouseClicked(mouseX, mouseY, mouseButton);
    }
    if (routeField != null) {
      routeField.mouseClicked(mouseX, mouseY, mouseButton);
    }
    if (exitTextField != null) {
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
    if (scroll != 0) {
      if (currentTab == TAB_PANEL) {
        GuideSignPanel panel = data.getPanels().get(selectedPanel);
        int maxScroll = Math.max(0, panel.getRows().size() - 4);
        if (scroll > 0 && rowScrollOffset > 0) {
          rowScrollOffset--;
        } else if (scroll < 0 && rowScrollOffset < maxScroll) {
          rowScrollOffset++;
        }
      } else if (currentTab == TAB_ROW) {
        clampPanelSelection();
        GuideSignPanel panel = data.getPanels().get(selectedPanel);
        if (!panel.getRows().isEmpty()) {
          clampRowSelection(panel);
          GuideSignRow row = panel.getRows().get(selectedRow);
          int maxScroll = Math.max(0, row.getElements().size() - 3);
          if (scroll > 0 && elemScrollOffset > 0) {
            elemScrollOffset--;
          } else if (scroll < 0 && elemScrollOffset < maxScroll) {
            elemScrollOffset++;
          }
        }
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
        initGui();
        break;
      case BTN_TAB_PANEL:
        syncAllFields();
        currentTab = TAB_PANEL;
        initGui();
        break;
      case BTN_TAB_ROW:
        syncAllFields();
        currentTab = TAB_ROW;
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

      case BTN_PANEL_PREV:
        if (selectedPanel > 0) {
          selectedPanel--;
          rowScrollOffset = 0;
          initGui();
        }
        break;
      case BTN_PANEL_NEXT:
        if (selectedPanel < data.getPanels().size() - 1) {
          selectedPanel++;
          rowScrollOffset = 0;
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
        elemScrollOffset = 0;
        selectedElement = 0;
        initGui();
        break;

      case BTN_ROW_PREV:
        if (selectedRow > 0) {
          syncAllFields();
          selectedRow--;
          selectedElement = 0;
          elemScrollOffset = 0;
          initGui();
        }
        break;
      case BTN_ROW_NEXT: {
        GuideSignPanel panel = data.getPanels().get(selectedPanel);
        if (selectedRow < panel.getRows().size() - 1) {
          syncAllFields();
          selectedRow++;
          selectedElement = 0;
          elemScrollOffset = 0;
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

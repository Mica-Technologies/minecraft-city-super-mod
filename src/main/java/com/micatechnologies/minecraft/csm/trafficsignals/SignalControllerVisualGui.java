package com.micatechnologies.minecraft.csm.trafficsignals;

import com.micatechnologies.minecraft.csm.CsmNetwork;
import com.micatechnologies.minecraft.csm.trafficsignals.logic.TrafficSignalControllerMode;
import java.io.IOException;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * Visual timeline-based configuration GUI for traffic signal controllers. Shows colored
 * phase bars and supports direct value input. Provides a "Simple View" toggle to switch
 * back to the button-cycling GUI.
 */
@SideOnly(Side.CLIENT)
public class SignalControllerVisualGui extends GuiScreen {

  // Clipboard for copy/paste between controllers
  private static NBTTagCompound clipboard = null;

  private static final int COLOR_GREEN = 0xFF00AA00;
  private static final int COLOR_YELLOW = 0xFFCCCC00;
  private static final int COLOR_RED = 0xFFCC0000;
  private static final int COLOR_WALK = 0xFF00CC00;
  private static final int COLOR_FLASH_DW = 0xFFFF8800;
  private static final int COLOR_DONT_WALK = 0xFFCC0000;
  private static final int COLOR_ALL_RED = 0xFF880000;
  private static final int COLOR_LPI = 0xFF00DDDD;
  private static final int COLOR_BAR_BG = 0xFF333333;
  private static final int COLOR_LABEL = 0xFFCCCCCC;
  private static final int COLOR_VALUE = 0xFFFFFFFF;
  private static final int COLOR_HEADER = 0xFFFFFFFF;

  private static final int BTN_CLOSE = 100;
  private static final int BTN_SIMPLE_VIEW = 101;
  private static final int BTN_MODE_PREV = 102;
  private static final int BTN_MODE_NEXT = 103;
  private static final int BTN_COPY = 104;
  private static final int BTN_PASTE = 105;
  private static final int BTN_NIGHTLY = 106;
  private static final int BTN_POWER_LOSS = 107;
  private static final int BTN_OVERLAP_PED = 108;
  private static final int BTN_ALL_RED_FLASH = 109;
  private static final int BTN_CLEAR_FAULTS = 110;

  private final TileEntityTrafficSignalController controller;
  private final BlockPos blockPos;

  // Editable text fields for timing values (ticks displayed as seconds)
  private GuiTextField fieldYellow;
  private GuiTextField fieldAllRed;
  private GuiTextField fieldMinGreen;
  private GuiTextField fieldMaxGreen;
  private GuiTextField fieldMinGreenSec;
  private GuiTextField fieldMaxGreenSec;
  private GuiTextField fieldPedClear;
  private GuiTextField fieldPedSignal;
  private GuiTextField fieldLPI;

  private GuiTextField focusedField;

  // Layout constants
  private int guiLeft;
  private int guiTop;
  private static final int GUI_WIDTH = 370;
  private static final int GUI_HEIGHT = 235;

  public SignalControllerVisualGui(TileEntityTrafficSignalController controller) {
    this.controller = controller;
    this.blockPos = controller.getPos();
  }

  @Override
  public void initGui() {
    buttonList.clear();
    guiLeft = (width - GUI_WIDTH) / 2;
    guiTop = (height - GUI_HEIGHT) / 2;

    int y = guiTop + 4;

    // Top row: mode selector, simple view, copy/paste
    buttonList.add(new GuiButton(BTN_MODE_PREV, guiLeft + 4, y, 20, 14, "<"));
    buttonList.add(new GuiButton(BTN_MODE_NEXT, guiLeft + 190, y, 20, 14, ">"));
    buttonList.add(new GuiButton(BTN_SIMPLE_VIEW, guiLeft + 216, y, 60, 14, "Simple"));
    buttonList.add(new GuiButton(BTN_COPY, guiLeft + 280, y, 28, 14, "Copy"));
    buttonList.add(new GuiButton(BTN_PASTE, guiLeft + 310, y, 28, 14, "Paste"));

    // Create text fields for timing values
    int fieldW = 36;
    int fieldH = 12;
    int col1X = guiLeft + 80;
    int col2X = guiLeft + 210;
    int col3X = guiLeft + 290;
    int fieldY = guiTop + 128;
    int rowH = 16;

    fieldYellow = createField(col1X, fieldY, fieldW, fieldH, controller.getYellowTime());
    fieldAllRed = createField(col2X, fieldY, fieldW, fieldH, controller.getAllRedTime());
    fieldLPI = createField(col3X, fieldY, fieldW, fieldH, controller.getLeadPedestrianIntervalTime());

    fieldY += rowH;
    fieldMinGreen = createField(col1X, fieldY, fieldW, fieldH, controller.getMinGreenTime());
    fieldMaxGreen = createField(col2X, fieldY, fieldW, fieldH, controller.getMaxGreenTime());

    fieldY += rowH;
    fieldMinGreenSec = createField(col1X, fieldY, fieldW, fieldH, controller.getMinGreenTimeSecondary());
    fieldMaxGreenSec = createField(col2X, fieldY, fieldW, fieldH, controller.getMaxGreenTimeSecondary());

    fieldY += rowH;
    fieldPedClear = createField(col1X, fieldY, fieldW, fieldH, controller.getFlashDontWalkTime());
    fieldPedSignal = createField(col2X, fieldY, fieldW, fieldH, controller.getDedicatedPedSignalTime());

    // Toggle buttons row
    int toggleY = guiTop + 196;
    int toggleW = 62;
    buttonList.add(new GuiButton(BTN_NIGHTLY, guiLeft + 4, toggleY, toggleW, 14, ""));
    buttonList.add(new GuiButton(BTN_POWER_LOSS, guiLeft + 70, toggleY, toggleW, 14, ""));
    buttonList.add(new GuiButton(BTN_OVERLAP_PED, guiLeft + 136, toggleY, toggleW, 14, ""));
    buttonList.add(new GuiButton(BTN_ALL_RED_FLASH, guiLeft + 202, toggleY, toggleW, 14, ""));
    buttonList.add(new GuiButton(BTN_CLEAR_FAULTS, guiLeft + 268, toggleY, toggleW, 14, ""));

    // Close button
    buttonList.add(new GuiButton(BTN_CLOSE, guiLeft + GUI_WIDTH / 2 - 40, guiTop + GUI_HEIGHT - 18, 80, 14, "Close"));
  }

  private GuiTextField createField(int x, int y, int w, int h, long tickValue) {
    GuiTextField field = new GuiTextField(0, fontRenderer, x, y, w, h);
    field.setMaxStringLength(5);
    field.setText(ticksToSeconds(tickValue));
    field.setTextColor(0xFFFFFF);
    return field;
  }

  private static String ticksToSeconds(long ticks) {
    double seconds = ticks / 20.0;
    if (seconds == (long) seconds) {
      return String.valueOf((long) seconds);
    }
    return String.format("%.1f", seconds);
  }

  private static long secondsToTicks(String text) {
    try {
      double seconds = Double.parseDouble(text.trim());
      return Math.round(seconds * 20);
    } catch (NumberFormatException e) {
      return -1;
    }
  }

  @Override
  public void drawScreen(int mouseX, int mouseY, float partialTicks) {
    drawDefaultBackground();

    // Background panel
    drawRect(guiLeft, guiTop, guiLeft + GUI_WIDTH, guiTop + GUI_HEIGHT, 0xCC000000);
    drawRect(guiLeft, guiTop, guiLeft + GUI_WIDTH, guiTop + 1, 0xFF444444);
    drawRect(guiLeft, guiTop + GUI_HEIGHT - 1, guiLeft + GUI_WIDTH, guiTop + GUI_HEIGHT, 0xFF444444);
    drawRect(guiLeft, guiTop, guiLeft + 1, guiTop + GUI_HEIGHT, 0xFF444444);
    drawRect(guiLeft + GUI_WIDTH - 1, guiTop, guiLeft + GUI_WIDTH, guiTop + GUI_HEIGHT, 0xFF444444);

    // Mode display
    String modeName = controller.getModeName();
    drawCenteredString(fontRenderer, modeName, guiLeft + 115, guiTop + 7, COLOR_HEADER);

    // === Phase Timeline ===
    int barLeft = guiLeft + 10;
    int barRight = guiLeft + GUI_WIDTH - 10;
    int barWidth = barRight - barLeft;
    int barHeight = 14;
    int barY = guiTop + 24;

    // Calculate total cycle time for proportional bars
    long greenTime = controller.getMaxGreenTime();
    long yellowTime = controller.getYellowTime();
    long allRedTime = controller.getAllRedTime();
    long greenSec = controller.getMaxGreenTimeSecondary();
    long lpiTime = controller.getLeadPedestrianIntervalTime();
    long pedClear = controller.getFlashDontWalkTime();
    long pedSignal = controller.getDedicatedPedSignalTime();

    long totalCycle = greenTime + yellowTime + allRedTime + greenSec + yellowTime + allRedTime;
    if (totalCycle <= 0) totalCycle = 1;

    // Circuit 1 signal bar
    drawString(fontRenderer, "Circuit 1", barLeft, barY + 3, COLOR_LABEL);
    barY += 14;
    drawRect(barLeft, barY, barRight, barY + barHeight, COLOR_BAR_BG);
    int x = barLeft;
    x = drawPhaseBar(x, barY, barWidth, barHeight, greenTime, totalCycle, COLOR_GREEN, "Green");
    x = drawPhaseBar(x, barY, barWidth, barHeight, yellowTime, totalCycle, COLOR_YELLOW, "Yel");
    x = drawPhaseBar(x, barY, barWidth, barHeight, allRedTime, totalCycle, COLOR_ALL_RED, "Red");
    x = drawPhaseBar(x, barY, barWidth, barHeight, greenSec, totalCycle, COLOR_RED, "");
    x = drawPhaseBar(x, barY, barWidth, barHeight, yellowTime, totalCycle, COLOR_RED, "");
    drawPhaseBar(x, barY, barWidth, barHeight, allRedTime, totalCycle, COLOR_RED, "");

    // Circuit 2 signal bar
    barY += barHeight + 4;
    drawString(fontRenderer, "Circuit 2", barLeft, barY + 3, COLOR_LABEL);
    barY += 14;
    drawRect(barLeft, barY, barRight, barY + barHeight, COLOR_BAR_BG);
    x = barLeft;
    x = drawPhaseBar(x, barY, barWidth, barHeight, greenTime, totalCycle, COLOR_RED, "");
    x = drawPhaseBar(x, barY, barWidth, barHeight, yellowTime, totalCycle, COLOR_RED, "");
    x = drawPhaseBar(x, barY, barWidth, barHeight, allRedTime, totalCycle, COLOR_ALL_RED, "Red");
    x = drawPhaseBar(x, barY, barWidth, barHeight, greenSec, totalCycle, COLOR_GREEN, "Green");
    x = drawPhaseBar(x, barY, barWidth, barHeight, yellowTime, totalCycle, COLOR_YELLOW, "Yel");
    drawPhaseBar(x, barY, barWidth, barHeight, allRedTime, totalCycle, COLOR_ALL_RED, "");

    // Pedestrian bar
    barY += barHeight + 4;
    drawString(fontRenderer, "Pedestrian", barLeft, barY + 3, COLOR_LABEL);
    barY += 14;
    drawRect(barLeft, barY, barRight, barY + barHeight, COLOR_BAR_BG);
    long pedWalk = pedSignal;
    long pedFlash = pedClear;
    long pedDontWalk = totalCycle - pedWalk - pedFlash;
    if (pedDontWalk < 0) pedDontWalk = 0;
    x = barLeft;
    if (lpiTime > 0) {
      x = drawPhaseBar(x, barY, barWidth, barHeight, lpiTime, totalCycle, COLOR_LPI, "LPI");
      pedWalk = Math.max(0, pedWalk - lpiTime);
    }
    x = drawPhaseBar(x, barY, barWidth, barHeight, pedWalk, totalCycle, COLOR_WALK, "Walk");
    x = drawPhaseBar(x, barY, barWidth, barHeight, pedFlash, totalCycle, COLOR_FLASH_DW, "Flash");
    drawPhaseBar(x, barY, barWidth, barHeight, pedDontWalk, totalCycle, COLOR_DONT_WALK, "Don't Walk");

    // === Timing Value Fields ===
    int fieldY = guiTop + 128;
    int rowH = 16;
    int col1L = guiLeft + 8;
    int col2L = guiLeft + 138;
    int col3L = guiLeft + 248;

    drawString(fontRenderer, "Yellow:", col1L, fieldY + 2, COLOR_LABEL);
    drawString(fontRenderer, "All Red:", col2L, fieldY + 2, COLOR_LABEL);
    drawString(fontRenderer, "LPI:", col3L, fieldY + 2, COLOR_LABEL);
    fieldYellow.drawTextBox();
    fieldAllRed.drawTextBox();
    fieldLPI.drawTextBox();

    fieldY += rowH;
    drawString(fontRenderer, "Min Grn:", col1L, fieldY + 2, COLOR_LABEL);
    drawString(fontRenderer, "Max Grn:", col2L, fieldY + 2, COLOR_LABEL);
    fieldMinGreen.drawTextBox();
    fieldMaxGreen.drawTextBox();

    fieldY += rowH;
    drawString(fontRenderer, "C2 Min:", col1L, fieldY + 2, COLOR_LABEL);
    drawString(fontRenderer, "C2 Max:", col2L, fieldY + 2, COLOR_LABEL);
    fieldMinGreenSec.drawTextBox();
    fieldMaxGreenSec.drawTextBox();

    fieldY += rowH;
    drawString(fontRenderer, "Ped Clr:", col1L, fieldY + 2, COLOR_LABEL);
    drawString(fontRenderer, "Ped Sig:", col2L, fieldY + 2, COLOR_LABEL);
    fieldPedClear.drawTextBox();
    fieldPedSignal.drawTextBox();

    // "(sec)" hint on the bottom-right of the field area
    drawString(fontRenderer, "(sec)", guiLeft + GUI_WIDTH - 34, fieldY + 2, 0xFF666666);

    // Toggle button labels
    updateToggleButton(BTN_NIGHTLY, "Night: ", controller.getNightlyFallbackToFlashMode());
    updateToggleButton(BTN_POWER_LOSS, "PwrLoss: ", controller.getPowerLossFallbackToFlashMode());
    updateToggleButton(BTN_OVERLAP_PED, "OvlpPed: ", controller.getOverlapPedestrianSignals());
    updateToggleButton(BTN_ALL_RED_FLASH, "AllRed: ", controller.getAllRedFlash());
    for (GuiButton btn : buttonList) {
      if (btn.id == BTN_CLEAR_FAULTS) {
        btn.displayString = controller.isInFaultState() ? "FAULT!" : "No Faults";
      }
    }

    super.drawScreen(mouseX, mouseY, partialTicks);

    // Total cycle time display
    drawCenteredString(fontRenderer,
        "Cycle: " + ticksToSeconds(totalCycle) + "s",
        guiLeft + GUI_WIDTH / 2, guiTop + 116, 0xFFAAAAAA);
  }

  private int drawPhaseBar(int x, int y, int totalWidth, int height,
      long phaseTicks, long totalTicks, int color, String label) {
    int phaseWidth = (int) ((double) phaseTicks / totalTicks * totalWidth);
    if (phaseWidth < 1 && phaseTicks > 0) phaseWidth = 1;
    if (phaseWidth > 0) {
      drawRect(x, y, x + phaseWidth, y + height, color);
      // Draw border
      drawRect(x + phaseWidth - 1, y, x + phaseWidth, y + height, 0xFF000000);
      // Draw label if it fits
      if (!label.isEmpty() && phaseWidth > fontRenderer.getStringWidth(label) + 2) {
        drawCenteredString(fontRenderer, label, x + phaseWidth / 2, y + 3, 0xFFFFFFFF);
      }
    }
    return x + phaseWidth;
  }

  private void updateToggleButton(int id, String prefix, boolean value) {
    for (GuiButton btn : buttonList) {
      if (btn.id == id) {
        btn.displayString = prefix + (value ? "ON" : "OFF");
      }
    }
  }

  @Override
  protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
    super.mouseClicked(mouseX, mouseY, mouseButton);

    // Unfocus previous field and commit its value
    if (focusedField != null) {
      commitField(focusedField);
    }

    focusedField = null;
    GuiTextField[] fields = getAllFields();
    for (GuiTextField field : fields) {
      field.mouseClicked(mouseX, mouseY, mouseButton);
      if (field.isFocused()) {
        focusedField = field;
      }
    }
  }

  @Override
  protected void keyTyped(char typedChar, int keyCode) throws IOException {
    if (focusedField != null) {
      focusedField.textboxKeyTyped(typedChar, keyCode);
      // Enter key commits the value
      if (keyCode == 28) { // Enter
        commitField(focusedField);
        focusedField.setFocused(false);
        focusedField = null;
      }
      // Tab key moves to next field
      else if (keyCode == 15) { // Tab
        commitField(focusedField);
        focusedField.setFocused(false);
        GuiTextField[] fields = getAllFields();
        for (int i = 0; i < fields.length; i++) {
          if (fields[i] == focusedField) {
            int next = (i + 1) % fields.length;
            fields[next].setFocused(true);
            focusedField = fields[next];
            break;
          }
        }
      }
    } else {
      super.keyTyped(typedChar, keyCode);
    }
  }

  private GuiTextField[] getAllFields() {
    return new GuiTextField[]{
        fieldYellow, fieldAllRed, fieldLPI,
        fieldMinGreen, fieldMaxGreen,
        fieldMinGreenSec, fieldMaxGreenSec,
        fieldPedClear, fieldPedSignal
    };
  }

  private void commitField(GuiTextField field) {
    long ticks = secondsToTicks(field.getText());
    if (ticks < 0) return;

    String paramKey;
    if (field == fieldYellow) paramKey = "yellowTime";
    else if (field == fieldAllRed) paramKey = "allRedTime";
    else if (field == fieldLPI) paramKey = "lpi";
    else if (field == fieldMinGreen) paramKey = "minGreenTime";
    else if (field == fieldMaxGreen) paramKey = "maxGreenTime";
    else if (field == fieldMinGreenSec) paramKey = "minGreenSecondary";
    else if (field == fieldMaxGreenSec) paramKey = "maxGreenSecondary";
    else if (field == fieldPedClear) paramKey = "flashDontWalk";
    else if (field == fieldPedSignal) paramKey = "pedSignal";
    else return;

    CsmNetwork.sendToServer(new SignalControllerSetValuePacket(blockPos, paramKey, ticks));
  }

  @Override
  protected void actionPerformed(GuiButton button) throws IOException {
    switch (button.id) {
      case BTN_CLOSE:
        mc.displayGuiScreen(null);
        break;
      case BTN_SIMPLE_VIEW:
        mc.displayGuiScreen(new SignalControllerConfigGui(controller));
        break;
      case BTN_MODE_PREV: {
        int ordinal = controller.getModeOrdinal();
        int total = TrafficSignalControllerMode.values().length;
        int prev = (ordinal - 1 + total) % total;
        CsmNetwork.sendToServer(new SignalControllerSetValuePacket(blockPos, "mode", prev));
        break;
      }
      case BTN_MODE_NEXT: {
        int ordinal = controller.getModeOrdinal();
        int total = TrafficSignalControllerMode.values().length;
        int next = (ordinal + 1) % total;
        CsmNetwork.sendToServer(new SignalControllerSetValuePacket(blockPos, "mode", next));
        break;
      }
      case BTN_COPY:
        clipboard = controller.getUpdateTag();
        break;
      case BTN_PASTE:
        if (clipboard != null) {
          pasteConfig(clipboard);
        }
        break;
      case BTN_NIGHTLY:
        CsmNetwork.sendToServer(new SignalControllerConfigPacket(blockPos,
            SignalControllerConfigAction.TOGGLE_NIGHTLY_FLASH.ordinal()));
        break;
      case BTN_POWER_LOSS:
        CsmNetwork.sendToServer(new SignalControllerConfigPacket(blockPos,
            SignalControllerConfigAction.TOGGLE_POWER_LOSS_FLASH.ordinal()));
        break;
      case BTN_OVERLAP_PED:
        CsmNetwork.sendToServer(new SignalControllerConfigPacket(blockPos,
            SignalControllerConfigAction.TOGGLE_OVERLAP_PED_SIGNALS.ordinal()));
        break;
      case BTN_ALL_RED_FLASH:
        CsmNetwork.sendToServer(new SignalControllerConfigPacket(blockPos,
            SignalControllerConfigAction.TOGGLE_ALL_RED_FLASH.ordinal()));
        break;
      case BTN_CLEAR_FAULTS:
        CsmNetwork.sendToServer(new SignalControllerConfigPacket(blockPos,
            SignalControllerConfigAction.CLEAR_FAULTS.ordinal()));
        break;
    }
  }

  private void pasteConfig(NBTTagCompound source) {
    // Send each timing value from the clipboard
    sendIfPresent(source, "tcYellowTime", "yellowTime");
    sendIfPresent(source, "tcAllRedTime", "allRedTime");
    sendIfPresent(source, "tcMinGreenTime", "minGreenTime");
    sendIfPresent(source, "tcMaxGreenTime", "maxGreenTime");
    sendIfPresent(source, "tcMinGreenSecondaryTime", "minGreenSecondary");
    sendIfPresent(source, "tcMaxGreenSecondaryTime", "maxGreenSecondary");
    sendIfPresent(source, "tcFlashDontWalkTime", "flashDontWalk");
    sendIfPresent(source, "tcDedicatedPedSignalTime", "pedSignal");
    sendIfPresent(source, "tcLeadPedestrianIntervalTime", "lpi");
    if (source.hasKey("tcMode")) {
      CsmNetwork.sendToServer(new SignalControllerSetValuePacket(blockPos, "mode",
          source.getInteger("tcMode")));
    }
  }

  private void sendIfPresent(NBTTagCompound source, String nbtKey, String paramKey) {
    if (source.hasKey(nbtKey)) {
      CsmNetwork.sendToServer(new SignalControllerSetValuePacket(blockPos, paramKey,
          source.getLong(nbtKey)));
    }
  }

  @Override
  public boolean doesGuiPauseGame() {
    return false;
  }
}

package com.micatechnologies.minecraft.csm.trafficsignals;

import com.micatechnologies.minecraft.csm.roads.CsmRoads;
import com.micatechnologies.minecraft.csm.trafficsignals.logic.TrafficSignalControllerMode;
import java.io.IOException;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * Requestable-mode timing GUI for traffic signal controllers. The two service-green bounds are
 * only consulted in {@link TrafficSignalControllerMode#REQUESTABLE}, so they live on their own
 * screen rather than crowding the main visual editor, the same way the ASC-3 programming GUI
 * holds the ADVANCED-mode plan.
 *
 * <p>The other intervals a requested service uses (yellow, all-red, pedestrian clearance) are
 * shown read-only for context; they are shared with every other mode and stay editable on the
 * main screen so there is exactly one place to change them.
 */
@SideOnly(Side.CLIENT)
public class RequestableSignalControllerGui extends GuiScreen {

  private static final int COLOR_GREEN = 0xFF00AA00;
  private static final int COLOR_GREEN_EXT = 0xFF006622;
  private static final int COLOR_YELLOW = 0xFFCCCC00;
  private static final int COLOR_ALL_RED = 0xFF880000;
  private static final int COLOR_FLASH_DW = 0xFFFF8800;
  private static final int COLOR_BAR_BG = 0xFF333333;
  private static final int COLOR_LABEL = 0xFFCCCCCC;
  private static final int COLOR_MUTED = 0xFF888888;
  private static final int COLOR_HEADER = 0xFFFFFFFF;
  private static final int COLOR_WARN = 0xFFFFAA00;

  private static final int BTN_BACK = 200;
  private static final int BTN_CLOSE = 201;

  private final TileEntityTrafficSignalController controller;
  private final BlockPos blockPos;

  private GuiTextField fieldMinService;
  private GuiTextField fieldMaxService;
  private GuiTextField focusedField;

  private int guiLeft;
  private int guiTop;
  private static final int GUI_WIDTH = 340;
  private static final int GUI_HEIGHT = 186;

  public RequestableSignalControllerGui(TileEntityTrafficSignalController controller) {
    this.controller = controller;
    this.blockPos = controller.getPos();
  }

  @Override
  public void initGui() {
    buttonList.clear();
    guiLeft = (width - GUI_WIDTH) / 2;
    guiTop = (height - GUI_HEIGHT) / 2;

    int fieldW = 40;
    int fieldH = 12;
    int fieldY = guiTop + 96;

    fieldMinService = createField(guiLeft + 92, fieldY, fieldW, fieldH,
        controller.getMinRequestableServiceTime());
    fieldMaxService = createField(guiLeft + 238, fieldY, fieldW, fieldH,
        controller.getMaxRequestableServiceTime());

    buttonList.add(new GuiButton(BTN_BACK, guiLeft + 6, guiTop + GUI_HEIGHT - 20, 80, 14, "Back"));
    buttonList.add(new GuiButton(BTN_CLOSE, guiLeft + GUI_WIDTH - 86, guiTop + GUI_HEIGHT - 20, 80,
        14, "Close"));
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

    // Background panel, matching the main visual editor's frame
    drawRect(guiLeft, guiTop, guiLeft + GUI_WIDTH, guiTop + GUI_HEIGHT, 0xCC000000);
    drawRect(guiLeft, guiTop, guiLeft + GUI_WIDTH, guiTop + 1, 0xFF444444);
    drawRect(guiLeft, guiTop + GUI_HEIGHT - 1, guiLeft + GUI_WIDTH, guiTop + GUI_HEIGHT, 0xFF444444);
    drawRect(guiLeft, guiTop, guiLeft + 1, guiTop + GUI_HEIGHT, 0xFF444444);
    drawRect(guiLeft + GUI_WIDTH - 1, guiTop, guiLeft + GUI_WIDTH, guiTop + GUI_HEIGHT, 0xFF444444);

    drawCenteredString(fontRenderer, "Requestable Mode Timing",
        guiLeft + GUI_WIDTH / 2, guiTop + 7, COLOR_HEADER);

    // The screen stays reachable in any mode so the values can be set up in advance; say so
    // rather than hiding the button and leaving the setting unreachable again.
    boolean isRequestable =
        controller.getModeOrdinal() == TrafficSignalControllerMode.REQUESTABLE.ordinal();
    if (!isRequestable) {
      drawCenteredString(fontRenderer,
          "Controller is in " + controller.getModeName() + " mode - these apply in Requestable",
          guiLeft + GUI_WIDTH / 2, guiTop + 19, COLOR_WARN);
    }

    long yellowTime = controller.getYellowTime();
    long allRedTime = controller.getAllRedTime();
    long pedClear = controller.getFlashDontWalkTime();
    long minService = controller.getMinRequestableServiceTime();
    long maxService = controller.getMaxRequestableServiceTime();

    // The extension window is what separates the two settings; a non-positive span means the
    // pair has been driven into a degenerate state and there is simply nothing to draw.
    long extension = maxService - minService;
    if (extension < 0) {
      extension = 0;
    }

    long totalCycle = yellowTime + allRedTime + minService + extension + pedClear;
    if (totalCycle <= 0) {
      totalCycle = 1;
    }

    // === Service sequence timeline ===
    int barLeft = guiLeft + 10;
    int barRight = guiLeft + GUI_WIDTH - 10;
    int barWidth = barRight - barLeft;
    int barHeight = 14;
    int barY = guiTop + 34;

    drawString(fontRenderer, "Service sequence, from the call to the return to rest", barLeft,
        barY, COLOR_LABEL);
    barY += 11;
    drawRect(barLeft, barY, barRight, barY + barHeight, COLOR_BAR_BG);

    int x = barLeft;
    x = drawPhaseBar(x, barY, barWidth, barHeight, yellowTime, totalCycle, COLOR_YELLOW, "Yel");
    x = drawPhaseBar(x, barY, barWidth, barHeight, allRedTime, totalCycle, COLOR_ALL_RED, "Red");
    x = drawPhaseBar(x, barY, barWidth, barHeight, minService, totalCycle, COLOR_GREEN, "Min");
    x = drawPhaseBar(x, barY, barWidth, barHeight, extension, totalCycle, COLOR_GREEN_EXT,
        "to Max");
    drawPhaseBar(x, barY, barWidth, barHeight, pedClear, totalCycle, COLOR_FLASH_DW, "Ped Clr");

    barY += barHeight + 6;
    drawString(fontRenderer,
        "Service green holds for Min, then ends as soon as no demand remains,", barLeft, barY,
        COLOR_MUTED);
    barY += 10;
    drawString(fontRenderer,
        "or at Max regardless of demand. Longest possible service: "
            + ticksToSeconds(maxService) + "s", barLeft, barY, COLOR_MUTED);

    // === Editable fields ===
    int fieldY = guiTop + 96;
    drawString(fontRenderer, "Min Service:", guiLeft + 8, fieldY + 2, COLOR_LABEL);
    drawString(fontRenderer, "Max Service:", guiLeft + 154, fieldY + 2, COLOR_LABEL);
    fieldMinService.drawTextBox();
    fieldMaxService.drawTextBox();

    drawString(fontRenderer,
        "Seconds. " + ticksToSeconds(TileEntityTrafficSignalController.REQUESTABLE_SERVICE_FLOOR)
            + "s minimum; Min is held at or below Max.", guiLeft + 8, fieldY + 18, COLOR_MUTED);

    // === Read-only context ===
    int ctxY = guiTop + 134;
    drawString(fontRenderer, "Shared intervals, edited on the main screen:", guiLeft + 8, ctxY,
        COLOR_LABEL);
    ctxY += 11;
    drawString(fontRenderer, "Yellow: " + ticksToSeconds(yellowTime) + "s", guiLeft + 8, ctxY,
        COLOR_MUTED);
    drawString(fontRenderer, "All Red: " + ticksToSeconds(allRedTime) + "s", guiLeft + 118, ctxY,
        COLOR_MUTED);
    drawString(fontRenderer, "Ped Clear: " + ticksToSeconds(pedClear) + "s", guiLeft + 228, ctxY,
        COLOR_MUTED);

    super.drawScreen(mouseX, mouseY, partialTicks);
  }

  private int drawPhaseBar(int x, int y, int totalWidth, int height,
      long phaseTicks, long totalTicks, int color, String label) {
    int phaseWidth = (int) ((double) phaseTicks / totalTicks * totalWidth);
    if (phaseWidth < 1 && phaseTicks > 0) {
      phaseWidth = 1;
    }
    if (phaseWidth > 0) {
      drawRect(x, y, x + phaseWidth, y + height, color);
      drawRect(x + phaseWidth - 1, y, x + phaseWidth, y + height, 0xFF000000);
      if (!label.isEmpty() && phaseWidth > fontRenderer.getStringWidth(label) + 2) {
        drawCenteredString(fontRenderer, label, x + phaseWidth / 2, y + 3, 0xFFFFFFFF);
      }
    }
    return x + phaseWidth;
  }

  @Override
  protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
    super.mouseClicked(mouseX, mouseY, mouseButton);

    if (focusedField != null) {
      commitField(focusedField);
    }

    focusedField = null;
    for (GuiTextField field : getAllFields()) {
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
      if (keyCode == 28) { // Enter
        commitField(focusedField);
        focusedField.setFocused(false);
        focusedField = null;
      } else if (keyCode == 15) { // Tab
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
    return new GuiTextField[]{fieldMinService, fieldMaxService};
  }

  /**
   * Pulls each unfocused field back in line with the controller. The server clamps both bounds
   * against each other, so a typed value can legitimately come back different from what was sent;
   * without this the field would keep displaying the rejected number until the screen was
   * reopened. The focused field is left alone so it does not fight the player mid-edit.
   */
  @Override
  public void updateScreen() {
    super.updateScreen();
    syncField(fieldMinService, controller.getMinRequestableServiceTime());
    syncField(fieldMaxService, controller.getMaxRequestableServiceTime());
  }

  private void syncField(GuiTextField field, long tickValue) {
    if (field == null || field == focusedField || field.isFocused()) {
      return;
    }
    String expected = ticksToSeconds(tickValue);
    if (!expected.equals(field.getText())) {
      field.setText(expected);
    }
  }

  private void commitField(GuiTextField field) {
    long ticks = secondsToTicks(field.getText());
    if (ticks < 0) {
      return;
    }

    String paramKey;
    if (field == fieldMinService) {
      paramKey = "minRequestableService";
    } else if (field == fieldMaxService) {
      paramKey = "maxRequestableService";
    } else {
      return;
    }

    CsmRoads.NETWORK.sendToServer(new SignalControllerSetValuePacket(blockPos, paramKey, ticks));
  }

  @Override
  protected void actionPerformed(GuiButton button) throws IOException {
    switch (button.id) {
      case BTN_BACK:
        commitFocused();
        mc.displayGuiScreen(new SignalControllerVisualGui(controller));
        break;
      case BTN_CLOSE:
        commitFocused();
        mc.displayGuiScreen(null);
        break;
      default:
        break;
    }
  }

  /**
   * Commits whatever field still has focus before the screen goes away. Without this a value
   * typed and then dismissed with Back or Close would be silently discarded.
   */
  private void commitFocused() {
    if (focusedField != null) {
      commitField(focusedField);
      focusedField.setFocused(false);
      focusedField = null;
    }
  }

  @Override
  public boolean doesGuiPauseGame() {
    return false;
  }
}

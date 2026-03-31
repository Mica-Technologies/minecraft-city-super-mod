package com.micatechnologies.minecraft.csm.trafficsignals;

import com.micatechnologies.minecraft.csm.CsmNetwork;
import java.io.IOException;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * Configuration GUI for traffic signal controllers. Displays all configurable properties in a
 * two-column layout with cycle buttons that send packets to the server.
 */
@SideOnly(Side.CLIENT)
public class SignalControllerConfigGui extends GuiScreen {

  private static final int COL_WIDTH = 180;
  private static final int COL_GAP = 8;
  private static final int BUTTON_HEIGHT = 15;
  private static final int ROW_SPACING = 17;
  private static final int CLOSE_BUTTON_ID = SignalControllerConfigAction.values().length;

  private static final String[] LABELS = {
      "Mode",                         // 0  - left col
      "Yellow Time",                  // 1  - left col
      "All Red Time",                 // 2  - left col
      "Min Green Time",               // 3  - left col
      "Max Green Time",               // 4  - left col
      "Min Green (Secondary)",        // 5  - left col
      "Max Green (Secondary)",        // 6  - left col
      "Ped Clearance Time",           // 7  - left col
      "Ped Signal Time",              // 8  - right col
      "Lead Ped Interval",            // 9  - right col
      "Nightly Flash",                // 10 - right col
      "Power Loss Flash",             // 11 - right col
      "Overlap Ped Signals",          // 12 - right col
      "All Red Flash",                // 13 - right col
      "Clear Faults"                  // 14 - right col
  };

  private static final int LEFT_COL_COUNT = 8;

  // Display strings for timing values (ticks -> human-readable)
  private static final long[] LPI_OPTIONS = SignalControllerConfigPacketHandler.LPI_OPTIONS;
  private static final String[] LPI_NAMES = {"0s (Disabled)", "1s", "2s", "3s", "5s", "7s"};
  private static final long[] YELLOW_OPTIONS = SignalControllerConfigPacketHandler.YELLOW_TIME_OPTIONS;
  private static final String[] YELLOW_NAMES = {"2s", "3s", "4s", "5s", "6s"};
  private static final long[] ALL_RED_OPTIONS = SignalControllerConfigPacketHandler.ALL_RED_TIME_OPTIONS;
  private static final String[] ALL_RED_NAMES = {"0s", "1s", "2s", "3s", "4s"};
  private static final long[] FDW_OPTIONS = SignalControllerConfigPacketHandler.FLASH_DONT_WALK_OPTIONS;
  private static final String[] FDW_NAMES = {"7s", "10s", "15s", "20s", "30s"};
  private static final long[] DED_PED_OPTIONS = SignalControllerConfigPacketHandler.DEDICATED_PED_OPTIONS;
  private static final String[] DED_PED_NAMES = {"7s", "8s", "10s", "15s", "20s"};
  private static final long[] MIN_GREEN_OPTIONS = SignalControllerConfigPacketHandler.MIN_GREEN_OPTIONS;
  private static final String[] MIN_GREEN_NAMES = {"5s", "7s", "10s", "15s", "20s", "25s"};
  private static final long[] MAX_GREEN_OPTIONS = SignalControllerConfigPacketHandler.MAX_GREEN_OPTIONS;
  private static final String[] MAX_GREEN_NAMES = {"30s", "45s", "50s", "60s", "70s", "80s", "90s"};

  private final TileEntityTrafficSignalController controller;
  private final BlockPos blockPos;

  public SignalControllerConfigGui(TileEntityTrafficSignalController controller) {
    this.controller = controller;
    this.blockPos = controller.getPos();
  }

  @Override
  public void initGui() {
    buttonList.clear();
    int totalWidth = COL_WIDTH * 2 + COL_GAP;
    int leftX = width / 2 - totalWidth / 2;
    int rightX = leftX + COL_WIDTH + COL_GAP;
    int topY = height / 2 - (LEFT_COL_COUNT * ROW_SPACING + 20) / 2;

    // Left column: indices 0 through LEFT_COL_COUNT-1
    for (int i = 0; i < LEFT_COL_COUNT; i++) {
      buttonList.add(new GuiButton(i, leftX, topY + i * ROW_SPACING,
          COL_WIDTH, BUTTON_HEIGHT, ""));
    }

    // Right column: indices LEFT_COL_COUNT through LABELS.length-1
    for (int i = LEFT_COL_COUNT; i < LABELS.length; i++) {
      int row = i - LEFT_COL_COUNT;
      buttonList.add(new GuiButton(i, rightX, topY + row * ROW_SPACING,
          COL_WIDTH, BUTTON_HEIGHT, ""));
    }

    // Close button centered below both columns
    int closeY = topY + LEFT_COL_COUNT * ROW_SPACING + 4;
    buttonList.add(new GuiButton(CLOSE_BUTTON_ID, width / 2 - COL_WIDTH / 2, closeY,
        COL_WIDTH, BUTTON_HEIGHT, "Close"));
  }

  @Override
  public void drawScreen(int mouseX, int mouseY, float partialTicks) {
    drawDefaultBackground();

    for (GuiButton button : buttonList) {
      if (button.id >= 0 && button.id < LABELS.length) {
        button.displayString = LABELS[button.id] + ": " + getCurrentValue(button.id);
      }
    }

    int totalWidth = COL_WIDTH * 2 + COL_GAP;
    int topY = height / 2 - (LEFT_COL_COUNT * ROW_SPACING + 20) / 2;
    drawCenteredString(fontRenderer, "Signal Controller Configuration",
        width / 2, topY - 12, 0xFFFFFF);

    super.drawScreen(mouseX, mouseY, partialTicks);
  }

  private String getCurrentValue(int actionOrdinal) {
    switch (SignalControllerConfigAction.values()[actionOrdinal]) {
      case SWITCH_MODE:
        return controller.getModeName();
      case CYCLE_YELLOW_TIME:
        return ticksToName(controller.getYellowTime(), YELLOW_OPTIONS, YELLOW_NAMES);
      case CYCLE_ALL_RED_TIME:
        return ticksToName(controller.getAllRedTime(), ALL_RED_OPTIONS, ALL_RED_NAMES);
      case CYCLE_FLASH_DONT_WALK_TIME:
        return ticksToName(controller.getFlashDontWalkTime(), FDW_OPTIONS, FDW_NAMES);
      case CYCLE_DEDICATED_PED_SIGNAL_TIME:
        return ticksToName(controller.getDedicatedPedSignalTime(), DED_PED_OPTIONS, DED_PED_NAMES);
      case CYCLE_MIN_GREEN_TIME:
        return ticksToName(controller.getMinGreenTime(), MIN_GREEN_OPTIONS, MIN_GREEN_NAMES);
      case CYCLE_MAX_GREEN_TIME:
        return ticksToName(controller.getMaxGreenTime(), MAX_GREEN_OPTIONS, MAX_GREEN_NAMES);
      case CYCLE_MIN_GREEN_TIME_SECONDARY:
        return ticksToName(controller.getMinGreenTimeSecondary(), MIN_GREEN_OPTIONS, MIN_GREEN_NAMES);
      case CYCLE_MAX_GREEN_TIME_SECONDARY:
        return ticksToName(controller.getMaxGreenTimeSecondary(), MAX_GREEN_OPTIONS, MAX_GREEN_NAMES);
      case CYCLE_LPI_TIME:
        return ticksToName(controller.getLeadPedestrianIntervalTime(), LPI_OPTIONS, LPI_NAMES);
      case TOGGLE_NIGHTLY_FLASH:
        return controller.getNightlyFallbackToFlashMode() ? "On" : "Off";
      case TOGGLE_POWER_LOSS_FLASH:
        return controller.getPowerLossFallbackToFlashMode() ? "On" : "Off";
      case TOGGLE_OVERLAP_PED_SIGNALS:
        return controller.getOverlapPedestrianSignals() ? "On" : "Off";
      case TOGGLE_ALL_RED_FLASH:
        return controller.getAllRedFlash() ? "On" : "Off";
      case CLEAR_FAULTS:
        return controller.isInFaultState() ? "FAULT ACTIVE" : "No Faults";
      default:
        return "N/A";
    }
  }

  private static String ticksToName(long ticks, long[] options, String[] names) {
    for (int i = 0; i < options.length; i++) {
      if (options[i] == ticks) {
        return names[i];
      }
    }
    return (ticks / 20) + "s";
  }

  @Override
  protected void actionPerformed(GuiButton button) throws IOException {
    if (button.id == CLOSE_BUTTON_ID) {
      mc.displayGuiScreen(null);
    } else if (button.id >= 0 && button.id < SignalControllerConfigAction.values().length) {
      CsmNetwork.sendToServer(new SignalControllerConfigPacket(blockPos, button.id));
    }
  }

  @Override
  public boolean doesGuiPauseGame() {
    return false;
  }
}

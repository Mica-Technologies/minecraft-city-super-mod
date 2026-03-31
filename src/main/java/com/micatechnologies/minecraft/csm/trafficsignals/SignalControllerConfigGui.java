package com.micatechnologies.minecraft.csm.trafficsignals;

import com.micatechnologies.minecraft.csm.CsmNetwork;
import java.io.IOException;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * Configuration GUI for traffic signal controllers. Displays all configurable properties with
 * cycle buttons that send packets to the server. Values refresh automatically every frame.
 */
@SideOnly(Side.CLIENT)
public class SignalControllerConfigGui extends GuiScreen {

  private static final int BUTTON_WIDTH = 240;
  private static final int BUTTON_HEIGHT = 15;
  private static final int ROW_SPACING = 17;
  private static final int CLOSE_BUTTON_ID = SignalControllerConfigAction.values().length;

  private static final String[] LABELS = {
      "Operating Mode",
      "Yellow Time",
      "All Red Time",
      "Ped Clearance Time",
      "Ped Signal Time",
      "Min Green Time",
      "Max Green Time",
      "Min Green Time (Secondary)",
      "Max Green Time (Secondary)",
      "Lead Pedestrian Interval",
      "Nightly Flash",
      "Power Loss Flash",
      "Overlap Ped Signals",
      "All Red Flash",
      "Clear Faults"
  };

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
    int centerX = width / 2;
    int totalHeight = (LABELS.length + 1) * ROW_SPACING;
    int topY = Math.max(4, height / 2 - totalHeight / 2);

    for (int i = 0; i < LABELS.length; i++) {
      buttonList.add(new GuiButton(i, centerX - BUTTON_WIDTH / 2,
          topY + i * ROW_SPACING, BUTTON_WIDTH, BUTTON_HEIGHT, ""));
    }

    buttonList.add(new GuiButton(CLOSE_BUTTON_ID, centerX - BUTTON_WIDTH / 2,
        topY + LABELS.length * ROW_SPACING + 2, BUTTON_WIDTH, BUTTON_HEIGHT, "Close"));
  }

  @Override
  public void drawScreen(int mouseX, int mouseY, float partialTicks) {
    drawDefaultBackground();

    for (int i = 0; i < LABELS.length && i < buttonList.size(); i++) {
      buttonList.get(i).displayString = LABELS[i] + ": " + getCurrentValue(i);
    }

    int totalHeight = (LABELS.length + 1) * ROW_SPACING;
    int topY = Math.max(4, height / 2 - totalHeight / 2);
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

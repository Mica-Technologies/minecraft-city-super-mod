package com.micatechnologies.minecraft.csm.lifesafety;

import com.micatechnologies.minecraft.csm.CsmNetwork;
import java.io.IOException;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class FireAlarmPanelConfigGui extends GuiScreen {

  private static final int COL_WIDTH = 200;
  private static final int BUTTON_HEIGHT = 20;
  private static final int ROW_SPACING = 24;
  private static final int CLOSE_BUTTON_ID = FireAlarmPanelConfigAction.values().length;

  private final TileEntityFireAlarmControlPanel panel;
  private final BlockPos blockPos;

  public FireAlarmPanelConfigGui(TileEntityFireAlarmControlPanel panel) {
    this.panel = panel;
    this.blockPos = panel.getPos();
  }

  @Override
  public void initGui() {
    buttonList.clear();
    int leftX = width / 2 - COL_WIDTH / 2;
    int actionCount = FireAlarmPanelConfigAction.values().length;
    int totalHeight = actionCount * ROW_SPACING + ROW_SPACING + 30;
    int topY = height / 2 - totalHeight / 2 + 20;

    for (int i = 0; i < actionCount; i++) {
      buttonList.add(new GuiButton(i, leftX, topY + i * ROW_SPACING,
          COL_WIDTH, BUTTON_HEIGHT, ""));
    }

    int closeY = topY + actionCount * ROW_SPACING + 8;
    buttonList.add(new GuiButton(CLOSE_BUTTON_ID, leftX, closeY,
        COL_WIDTH, BUTTON_HEIGHT, "Close"));
  }

  @Override
  public void drawScreen(int mouseX, int mouseY, float partialTicks) {
    drawDefaultBackground();

    int actionCount = FireAlarmPanelConfigAction.values().length;
    int totalHeight = actionCount * ROW_SPACING + ROW_SPACING + 30;
    int topY = height / 2 - totalHeight / 2 + 20;

    // Title
    drawCenteredString(fontRenderer, "Fire Alarm Panel Configuration",
        width / 2, topY - 28, 0xFFFFFF);

    // Status line
    String status = panel.getStatusString();
    int statusColor;
    if (panel.getAlarmState()) {
      statusColor = panel.getAudibleSilence() ? 0xFFAA00 : 0xFF5555;
    } else {
      statusColor = 0x55FF55;
    }
    drawCenteredString(fontRenderer, "Status: " + status, width / 2, topY - 16, statusColor);

    // Update button labels
    for (GuiButton button : buttonList) {
      if (button.id >= 0 && button.id < actionCount) {
        button.displayString = getButtonLabel(button.id);
      }
    }

    super.drawScreen(mouseX, mouseY, partialTicks);
  }

  private String getButtonLabel(int actionOrdinal) {
    switch (FireAlarmPanelConfigAction.values()[actionOrdinal]) {
      case CYCLE_VOICE_EVAC_SOUND:
        return "Voice Evac: " + panel.getCurrentSoundName();
      case AUDIBLE_SILENCE:
        if (!panel.getAlarmState()) {
          return "Audible Silence (no alarm)";
        }
        return panel.getAudibleSilence() ? "Audible Silence: ON" : "Audible Silence: OFF";
      case RESET_PANEL:
        return panel.getAlarmState() ? "Reset Panel (alarm active)" : "Reset Panel";
      default:
        return "N/A";
    }
  }

  @Override
  protected void actionPerformed(GuiButton button) throws IOException {
    if (button.id == CLOSE_BUTTON_ID) {
      mc.displayGuiScreen(null);
    } else if (button.id >= 0 && button.id < FireAlarmPanelConfigAction.values().length) {
      CsmNetwork.sendToServer(new FireAlarmPanelConfigPacket(blockPos, button.id));
    }
  }

  @Override
  public boolean doesGuiPauseGame() {
    return false;
  }
}

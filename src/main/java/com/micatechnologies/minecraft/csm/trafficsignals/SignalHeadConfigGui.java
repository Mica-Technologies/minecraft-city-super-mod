package com.micatechnologies.minecraft.csm.trafficsignals;

import com.micatechnologies.minecraft.csm.CsmNetwork;
import com.micatechnologies.minecraft.csm.trafficsignals.logic.TrafficSignalSectionInfo;
import java.io.IOException;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * Configuration GUI for traffic signal heads. Two-column layout displaying all configurable
 * properties with cycle buttons that send packets to the server. The GUI reads values from
 * the client-side tile entity every frame, so changes sync automatically after the server
 * processes each packet.
 */
@SideOnly(Side.CLIENT)
public class SignalHeadConfigGui extends GuiScreen {

  private static final int BUTTON_WIDTH = 160;
  private static final int BUTTON_HEIGHT = 20;
  private static final int ROW_SPACING = 22;
  private static final int COLUMN_GAP = 6;
  private static final int CLOSE_BUTTON_ID = 100;

  private static final String[] LABELS = {
      "Body Color",
      "Door Color",
      "Visor Color",
      "Visor Type",
      "Body Tilt",
      "Bulb Style",
      "Bulb Type",
      "Alternate Flash"
  };

  private final TileEntityTrafficSignalHead tileEntity;
  private final BlockPos blockPos;

  public SignalHeadConfigGui(TileEntityTrafficSignalHead tileEntity) {
    this.tileEntity = tileEntity;
    this.blockPos = tileEntity.getPos();
  }

  @Override
  public void initGui() {
    buttonList.clear();

    int totalWidth = BUTTON_WIDTH * 2 + COLUMN_GAP;
    int leftX = width / 2 - totalWidth / 2;
    int rightX = leftX + BUTTON_WIDTH + COLUMN_GAP;
    int rows = (LABELS.length + 1) / 2; // ceiling division
    int topY = height / 2 - (rows * ROW_SPACING + ROW_SPACING) / 2;

    for (int i = 0; i < LABELS.length; i++) {
      int col = i % 2;
      int row = i / 2;
      int x = col == 0 ? leftX : rightX;
      int y = topY + row * ROW_SPACING;
      buttonList.add(new GuiButton(i, x, y, BUTTON_WIDTH, BUTTON_HEIGHT, ""));
    }

    buttonList.add(new GuiButton(CLOSE_BUTTON_ID, width / 2 - BUTTON_WIDTH / 2,
        topY + rows * ROW_SPACING + 4, BUTTON_WIDTH, BUTTON_HEIGHT, "Close"));
  }

  @Override
  public void drawScreen(int mouseX, int mouseY, float partialTicks) {
    drawDefaultBackground();

    // Update button text from current TE values
    TrafficSignalSectionInfo[] infos = tileEntity.getSectionInfos();
    for (int i = 0; i < LABELS.length && i < buttonList.size(); i++) {
      buttonList.get(i).displayString = LABELS[i] + ": " + getCurrentValue(i, infos);
    }

    // Draw title
    int rows = (LABELS.length + 1) / 2;
    int topY = height / 2 - (rows * ROW_SPACING + ROW_SPACING) / 2;
    drawCenteredString(fontRenderer, "Signal Head Configuration",
        width / 2, topY - 14, 0xFFFFFF);

    super.drawScreen(mouseX, mouseY, partialTicks);
  }

  private String getCurrentValue(int actionOrdinal, TrafficSignalSectionInfo[] infos) {
    if (actionOrdinal >= SignalHeadConfigAction.values().length) return "N/A";
    if (infos == null || infos.length == 0) return "N/A";

    switch (SignalHeadConfigAction.values()[actionOrdinal]) {
      case CYCLE_BODY_COLOR:
        return infos[0].getBodyColor().getFriendlyName();
      case CYCLE_DOOR_COLOR:
        return infos[0].getDoorColor().getFriendlyName();
      case CYCLE_VISOR_COLOR:
        return infos[0].getVisorColor().getFriendlyName();
      case CYCLE_VISOR_TYPE:
        return infos[0].getVisorType().getFriendlyName();
      case CYCLE_BODY_TILT:
        return tileEntity.getBodyTilt().getFriendlyName();
      case CYCLE_BULB_STYLE:
        return infos[0].getBulbStyle().getFriendlyName();
      case CYCLE_BULB_TYPE:
        return infos[0].getBulbType().getFriendlyName();
      case TOGGLE_ALTERNATE_FLASH:
        return tileEntity.isAlternateFlash() ? "ON (wig-wag B)" : "OFF (normal)";
      default:
        return "N/A";
    }
  }

  @Override
  protected void actionPerformed(GuiButton button) throws IOException {
    if (button.id == CLOSE_BUTTON_ID) {
      mc.displayGuiScreen(null);
    } else if (button.id >= 0 && button.id < SignalHeadConfigAction.values().length) {
      CsmNetwork.sendToServer(new SignalHeadConfigPacket(blockPos, button.id));
    }
  }

  @Override
  public boolean doesGuiPauseGame() {
    return false;
  }
}

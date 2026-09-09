package com.micatechnologies.minecraft.csm.trafficaccessories;

import com.micatechnologies.minecraft.csm.roads.CsmRoads;
import java.io.IOException;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * Configuration GUI for a radar speed feedback sign: the posted speed, the speed scale, the
 * panel size, the sign face colour, the optional SPEED LIMIT header, and the scan zone.
 *
 * <p>Every button steps forward on a plain click and backward on a shift-click. It matters
 * least here, where no list is long, and most on the schedule-style settings elsewhere, but the
 * footer says so rather than leaving it to be discovered.</p>
 */
@SideOnly(Side.CLIENT)
public class RadarSpeedSignGui extends GuiScreen {

  private static final int BUTTON_WIDTH = 150;
  private static final int BUTTON_HEIGHT = 20;
  private static final int ROW_SPACING = 22;
  private static final int COLUMN_GAP = 6;
  private static final int CLOSE_BUTTON_ID = 100;
  private static final int SETTING_ROWS = 3;

  private final TileEntityRadarSpeedSign tileEntity;
  private final BlockPos blockPos;

  public RadarSpeedSignGui(TileEntityRadarSpeedSign tileEntity) {
    this.tileEntity = tileEntity;
    this.blockPos = tileEntity.getPos();
  }

  @Override
  public void initGui() {
    buttonList.clear();

    int totalWidth = BUTTON_WIDTH * 2 + COLUMN_GAP;
    int leftX = width / 2 - totalWidth / 2;
    int rightX = leftX + BUTTON_WIDTH + COLUMN_GAP;
    int topY = topRowY();

    addSetting(RadarSpeedSignConfigAction.CYCLE_POSTED_SPEED, leftX, topY);
    addSetting(RadarSpeedSignConfigAction.CYCLE_MULTIPLIER, rightX, topY);
    addSetting(RadarSpeedSignConfigAction.CYCLE_SCALE, leftX, topY + ROW_SPACING);
    addSetting(RadarSpeedSignConfigAction.CYCLE_FACE_COLOR, rightX, topY + ROW_SPACING);
    addSetting(RadarSpeedSignConfigAction.TOGGLE_HEADER, leftX, topY + 2 * ROW_SPACING);
    addSetting(RadarSpeedSignConfigAction.CLEAR_ZONE, rightX, topY + 2 * ROW_SPACING);

    buttonList.add(new GuiButton(CLOSE_BUTTON_ID, width / 2 - BUTTON_WIDTH / 2,
        topY + SETTING_ROWS * ROW_SPACING + 6, BUTTON_WIDTH, BUTTON_HEIGHT, "Close"));
  }

  private void addSetting(RadarSpeedSignConfigAction action, int x, int y) {
    buttonList.add(new GuiButton(action.ordinal(), x, y, BUTTON_WIDTH, BUTTON_HEIGHT, ""));
  }

  /**
   * The first row's Y. The settings and the close button are centred as one block, so adding a
   * row later moves the panel rather than pushing it off the top of the screen.
   */
  private int topRowY() {
    return height / 2 - ((SETTING_ROWS + 1) * ROW_SPACING) / 2;
  }

  @Override
  public void drawScreen(int mouseX, int mouseY, float partialTicks) {
    drawDefaultBackground();

    for (GuiButton button : buttonList) {
      if (button.id == CLOSE_BUTTON_ID) {
        continue;
      }
      if (button.id >= 0 && button.id < RadarSpeedSignConfigAction.values().length) {
        button.displayString = labelFor(RadarSpeedSignConfigAction.values()[button.id]);
      }
    }

    int topY = topRowY();
    drawCenteredString(fontRenderer, "Radar Speed Sign", width / 2, topY - 26, 0xFFFFFF);

    // What the board is showing this instant, so the sign can be set up without having to run
    // past it and look.
    int reading = tileEntity.getReading();
    String status;
    if (reading <= 0) {
      status = "Reading: —";
    } else if (tileEntity.isSlowDown()) {
      status = "Reading: " + reading + " mph — SLOW DOWN";
    } else if (tileEntity.isOverLimit()) {
      status = "Reading: " + reading + " mph — over limit";
    } else {
      status = "Reading: " + reading + " mph";
    }
    drawCenteredString(fontRenderer, status, width / 2, topY - 15,
        tileEntity.isOverLimit() ? 0xFF7060 : 0xFFD070);

    drawCenteredString(fontRenderer, "Click to step forward, shift-click to step back",
        width / 2, topY + SETTING_ROWS * ROW_SPACING + 30, 0xA0A0A0);

    super.drawScreen(mouseX, mouseY, partialTicks);
  }

  private String labelFor(RadarSpeedSignConfigAction action) {
    switch (action) {
      case CYCLE_POSTED_SPEED:
        return "Posted Speed: " + tileEntity.getPostedSpeed();
      case CYCLE_MULTIPLIER:
        return "Speed Scale: "
            + TileEntityRadarSpeedSign.MULTIPLIER_NAMES[tileEntity.getMultiplierIndex()];
      case CYCLE_SCALE:
        return "Panel Size: "
            + TileEntityRadarSpeedSign.SCALE_NAMES[tileEntity.getScaleIndex()];
      case CYCLE_FACE_COLOR:
        return "Face: " + tileEntity.getFaceColor().getFriendlyName();
      case TOGGLE_HEADER:
        return "Header Panel: " + (tileEntity.isShowHeader() ? "On" : "Off");
      case CLEAR_ZONE:
        return tileEntity.hasCustomZone() ? "Zone: Custom (click to reset)" : "Zone: Default";
      default:
        return "N/A";
    }
  }

  @Override
  protected void actionPerformed(GuiButton button) throws IOException {
    if (button.id == CLOSE_BUTTON_ID) {
      mc.displayGuiScreen(null);
      return;
    }
    if (button.id >= 0 && button.id < RadarSpeedSignConfigAction.values().length) {
      int step = isShiftKeyDown() ? -1 : 1;
      CsmRoads.NETWORK.sendToServer(
          new RadarSpeedSignConfigPacket(blockPos, button.id, step));
    }
  }

  @Override
  public boolean doesGuiPauseGame() {
    return false;
  }
}

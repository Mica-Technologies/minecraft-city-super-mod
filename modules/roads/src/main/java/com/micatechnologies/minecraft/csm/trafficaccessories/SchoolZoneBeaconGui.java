package com.micatechnologies.minecraft.csm.trafficaccessories;

import com.micatechnologies.minecraft.csm.roads.CsmRoads;
import java.io.IOException;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * Configuration GUI for a school zone beacon assembly: the posted speed, the panel size, the
 * beacon arrangement, and the two windows the beacons flash in.
 *
 * <p>Every button steps forward on a plain click and backward on a shift-click, which matters
 * most for the schedule hours — cycling forward through 24 of them to move one back would be
 * tedious. The footer says so rather than leaving it to be discovered.</p>
 */
@SideOnly(Side.CLIENT)
public class SchoolZoneBeaconGui extends GuiScreen {

  private static final int BUTTON_WIDTH = 150;
  private static final int BUTTON_HEIGHT = 20;
  private static final int ROW_SPACING = 22;
  private static final int COLUMN_GAP = 6;
  private static final int CLOSE_BUTTON_ID = 100;

  /** Schedule-hour buttons get ids offset past the action ordinals so they cannot collide. */
  private static final int SCHEDULE_ID_OFFSET = 50;

  private static final String[] SCHEDULE_LABELS =
      {"AM Start", "AM End", "PM Start", "PM End"};

  private final TileEntitySchoolZoneBeacon tileEntity;
  private final BlockPos blockPos;

  public SchoolZoneBeaconGui(TileEntitySchoolZoneBeacon tileEntity) {
    this.tileEntity = tileEntity;
    this.blockPos = tileEntity.getPos();
  }

  @Override
  public void initGui() {
    buttonList.clear();

    int totalWidth = BUTTON_WIDTH * 2 + COLUMN_GAP;
    int leftX = width / 2 - totalWidth / 2;
    int rightX = leftX + BUTTON_WIDTH + COLUMN_GAP;
    // Four property rows in two columns, then two schedule rows in two columns, then close.
    int topY = height / 2 - (5 * ROW_SPACING) / 2;

    buttonList.add(new GuiButton(SchoolZoneBeaconConfigAction.CYCLE_SPEED_LIMIT.ordinal(),
        leftX, topY, BUTTON_WIDTH, BUTTON_HEIGHT, ""));
    buttonList.add(new GuiButton(SchoolZoneBeaconConfigAction.CYCLE_MODE.ordinal(),
        rightX, topY, BUTTON_WIDTH, BUTTON_HEIGHT, ""));
    buttonList.add(new GuiButton(SchoolZoneBeaconConfigAction.CYCLE_SCALE.ordinal(),
        leftX, topY + ROW_SPACING, BUTTON_WIDTH, BUTTON_HEIGHT, ""));
    buttonList.add(new GuiButton(SchoolZoneBeaconConfigAction.CYCLE_ARRANGEMENT.ordinal(),
        rightX, topY + ROW_SPACING, BUTTON_WIDTH, BUTTON_HEIGHT, ""));

    for (int i = 0; i < SCHEDULE_LABELS.length; i++) {
      int col = i % 2;
      int row = i / 2;
      buttonList.add(new GuiButton(SCHEDULE_ID_OFFSET + i,
          col == 0 ? leftX : rightX, topY + (2 + row) * ROW_SPACING,
          BUTTON_WIDTH, BUTTON_HEIGHT, ""));
    }

    buttonList.add(new GuiButton(CLOSE_BUTTON_ID, width / 2 - BUTTON_WIDTH / 2,
        topY + 4 * ROW_SPACING + 6, BUTTON_WIDTH, BUTTON_HEIGHT, "Close"));
  }

  @Override
  public void drawScreen(int mouseX, int mouseY, float partialTicks) {
    drawDefaultBackground();

    for (GuiButton button : buttonList) {
      if (button.id == CLOSE_BUTTON_ID) {
        continue;
      }
      if (button.id >= SCHEDULE_ID_OFFSET) {
        int which = button.id - SCHEDULE_ID_OFFSET;
        button.displayString =
            SCHEDULE_LABELS[which] + ": " + formatHour(tileEntity.getScheduleHour(which));
      } else if (button.id < SchoolZoneBeaconConfigAction.values().length) {
        button.displayString = labelFor(SchoolZoneBeaconConfigAction.values()[button.id]);
      }
    }

    int topY = height / 2 - (5 * ROW_SPACING) / 2;
    drawCenteredString(fontRenderer, "School Zone Beacon", width / 2, topY - 26, 0xFFFFFF);

    // Current state, so a player can see whether the zone is posted right now without having
    // to work the clock out themselves.
    int hour = tileEntity.getWorldHour();
    String status = hour < 0 ? "" : "Now " + formatHour(hour) + " — beacons "
        + (tileEntity.isFlashingNow() ? "flashing" : "dark");
    drawCenteredString(fontRenderer, status, width / 2, topY - 15, 0xFFD070);

    drawCenteredString(fontRenderer, "Click to step forward, shift-click to step back",
        width / 2, topY + 4 * ROW_SPACING + 30, 0xA0A0A0);

    super.drawScreen(mouseX, mouseY, partialTicks);
  }

  private String labelFor(SchoolZoneBeaconConfigAction action) {
    switch (action) {
      case CYCLE_SPEED_LIMIT:
        return "Speed Limit: " + tileEntity.getSpeedLimit();
      case CYCLE_SCALE:
        return "Panel Size: "
            + TileEntitySchoolZoneBeacon.SCALE_NAMES[tileEntity.getScaleIndex()];
      case CYCLE_ARRANGEMENT:
        return "Beacons: "
            + TileEntitySchoolZoneBeacon.BEACON_ARRANGEMENT_NAMES[tileEntity.getArrangement()];
      case CYCLE_MODE:
        return "Mode: " + TileEntitySchoolZoneBeacon.MODE_NAMES[tileEntity.getMode()];
      default:
        return "N/A";
    }
  }

  /** Hours read as a 12-hour clock, which is how a school zone is posted on the sign. */
  private static String formatHour(int hour) {
    int display = hour % 12;
    if (display == 0) {
      display = 12;
    }
    return display + (hour < 12 ? " AM" : " PM");
  }

  @Override
  protected void actionPerformed(GuiButton button) throws IOException {
    if (button.id == CLOSE_BUTTON_ID) {
      mc.displayGuiScreen(null);
      return;
    }
    int step = isShiftKeyDown() ? -1 : 1;
    if (button.id >= SCHEDULE_ID_OFFSET) {
      CsmRoads.NETWORK.sendToServer(new SchoolZoneBeaconConfigPacket(blockPos,
          SchoolZoneBeaconConfigAction.ADJUST_SCHEDULE_HOUR.ordinal(),
          button.id - SCHEDULE_ID_OFFSET, step));
    } else if (button.id >= 0 && button.id < SchoolZoneBeaconConfigAction.values().length) {
      CsmRoads.NETWORK.sendToServer(
          new SchoolZoneBeaconConfigPacket(blockPos, button.id, 0, step));
    }
  }

  @Override
  public boolean doesGuiPauseGame() {
    return false;
  }
}

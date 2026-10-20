package com.micatechnologies.minecraft.csm.trafficsignals;

import com.micatechnologies.minecraft.csm.roads.CsmRoads;
import com.micatechnologies.minecraft.csm.trafficsignals.TileEntityTrafficSignalSensor.ZoneType;
import com.micatechnologies.minecraft.csm.trafficsignals.logic.AbstractBlockTrafficSignalSensorAngled;
import com.micatechnologies.minecraft.csm.trafficsignals.logic.SensorAngle;
import java.io.IOException;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * Configuration GUI for traffic signal sensors, opened by sneak + right-clicking a sensor with the
 * sensor zone tool ({@code ewsignallinker}). Shows how many boxes each of the four detection zones
 * (Through / Left / Right / Protected) holds, and offers per-zone clear and remove-last-box buttons
 * and a clear-all button. For sensors
 * that support a configurable aim angle (i.e. an
 * {@link AbstractBlockTrafficSignalSensorAngled}), it also exposes None / Left / Right angle buttons.
 *
 * <p>The cardinal facing is intentionally NOT editable here: it is set on placement and is what the
 * signal controller uses to attribute the sensor to an approach. The angle is purely cosmetic.</p>
 *
 * <p>Reads live from the client-synced {@link TileEntityTrafficSignalSensor} every frame and sends
 * edits to the server via {@link SensorConfigPacket}.</p>
 */
@SideOnly(Side.CLIENT)
public class SensorConfigGui extends GuiScreen {

  private static final int BUTTON_WIDTH = 200;
  private static final int BUTTON_HEIGHT = 20;
  private static final int ROW_SPACING = 24;
  private static final int ANGLE_BUTTON_WIDTH = 64;
  private static final int ANGLE_BUTTON_GAP = 4;
  private static final int REMOVE_LAST_BUTTON_WIDTH = 64;
  private static final int ZONE_BUTTON_GAP = 4;
  private static final int ZONE_BUTTON_WIDTH =
      BUTTON_WIDTH - REMOVE_LAST_BUTTON_WIDTH - ZONE_BUTTON_GAP;

  // Button IDs.
  private static final int CLOSE_ID = 100;
  private static final int CLEAR_STANDARD_ID = 101;
  private static final int CLEAR_LEFT_ID = 102;
  private static final int CLEAR_RIGHT_ID = 103;
  private static final int CLEAR_PROTECTED_ID = 104;
  private static final int CLEAR_ALL_ID = 105;
  private static final int ANGLE_NONE_ID = 106;
  private static final int ANGLE_LEFT_ID = 107;
  private static final int ANGLE_RIGHT_ID = 108;
  private static final int REMOVE_LAST_STANDARD_ID = 109;
  private static final int REMOVE_LAST_LEFT_ID = 110;
  private static final int REMOVE_LAST_RIGHT_ID = 111;
  private static final int REMOVE_LAST_PROTECTED_ID = 112;

  private final TileEntityTrafficSignalSensor tileEntity;
  private final BlockPos blockPos;

  public SensorConfigGui(TileEntityTrafficSignalSensor tileEntity) {
    this.tileEntity = tileEntity;
    this.blockPos = tileEntity.getPos();
  }

  /** @return {@code true} if this sensor's block supports a configurable aim angle. */
  private boolean isAngled() {
    return mc.world != null
        && mc.world.getBlockState(blockPos).getBlock() instanceof AbstractBlockTrafficSignalSensorAngled;
  }

  private SensorAngle currentAngle() {
    IBlockState state = mc.world.getBlockState(blockPos);
    if (state.getBlock() instanceof AbstractBlockTrafficSignalSensorAngled) {
      return state.getValue(AbstractBlockTrafficSignalSensorAngled.ANGLE);
    }
    return SensorAngle.NONE;
  }

  @Override
  public void initGui() {
    buttonList.clear();
    boolean angled = isAngled();

    // Count rows: 4 zone-clear rows + clear-all + (optional) angle row + close.
    int rows = 4 + 1 + (angled ? 1 : 0) + 1;
    int blockHeight = rows * ROW_SPACING;
    int topY = height / 2 - blockHeight / 2;
    int centerX = width / 2;
    int leftX = centerX - BUTTON_WIDTH / 2;

    // One row per zone: clear the zone (every box), and remove its most recently drawn box.
    int y = topY;
    addZoneRow(CLEAR_STANDARD_ID, REMOVE_LAST_STANDARD_ID, leftX, y);
    y += ROW_SPACING;
    addZoneRow(CLEAR_LEFT_ID, REMOVE_LAST_LEFT_ID, leftX, y);
    y += ROW_SPACING;
    addZoneRow(CLEAR_RIGHT_ID, REMOVE_LAST_RIGHT_ID, leftX, y);
    y += ROW_SPACING;
    addZoneRow(CLEAR_PROTECTED_ID, REMOVE_LAST_PROTECTED_ID, leftX, y);
    y += ROW_SPACING;
    buttonList.add(new GuiButton(CLEAR_ALL_ID, leftX, y, BUTTON_WIDTH, BUTTON_HEIGHT, "Clear All Zones"));
    y += ROW_SPACING;

    if (angled) {
      // Three angle buttons centered on one row.
      int totalWidth = ANGLE_BUTTON_WIDTH * 3 + ANGLE_BUTTON_GAP * 2;
      int ax = centerX - totalWidth / 2;
      buttonList.add(new GuiButton(ANGLE_NONE_ID, ax, y, ANGLE_BUTTON_WIDTH, BUTTON_HEIGHT, "None"));
      buttonList.add(new GuiButton(ANGLE_LEFT_ID, ax + ANGLE_BUTTON_WIDTH + ANGLE_BUTTON_GAP, y,
          ANGLE_BUTTON_WIDTH, BUTTON_HEIGHT, "Left"));
      buttonList.add(new GuiButton(ANGLE_RIGHT_ID,
          ax + (ANGLE_BUTTON_WIDTH + ANGLE_BUTTON_GAP) * 2, y, ANGLE_BUTTON_WIDTH, BUTTON_HEIGHT,
          "Right"));
      y += ROW_SPACING;
    }

    buttonList.add(new GuiButton(CLOSE_ID, leftX, y, BUTTON_WIDTH, BUTTON_HEIGHT, "Close"));
  }

  private void addZoneRow(int clearId, int removeLastId, int leftX, int y) {
    buttonList.add(new GuiButton(clearId, leftX, y, ZONE_BUTTON_WIDTH, BUTTON_HEIGHT, ""));
    buttonList.add(new GuiButton(removeLastId, leftX + ZONE_BUTTON_WIDTH + ZONE_BUTTON_GAP, y,
        REMOVE_LAST_BUTTON_WIDTH, BUTTON_HEIGHT, "Remove Last"));
  }

  @Override
  public void drawScreen(int mouseX, int mouseY, float partialTicks) {
    drawDefaultBackground();

    boolean angled = isAngled();
    int rows = 4 + 1 + (angled ? 1 : 0) + 1;
    int topY = height / 2 - (rows * ROW_SPACING) / 2;
    drawCenteredString(fontRenderer, "Sensor Configuration", width / 2, topY - 28, 0xFFFFFF);

    // Refresh the per-zone clear button labels with the live box counts, and enable each zone's
    // buttons only while it has a box to remove.
    for (GuiButton button : buttonList) {
      ZoneType type = zoneTypeFor(button.id);
      if (type == null) {
        continue;
      }
      int boxes = tileEntity.getBoxCount(type);
      button.enabled = boxes > 0;
      if (isClearButton(button.id)) {
        button.displayString = zoneLabel(type, boxes);
      }
    }

    if (angled) {
      // Highlight the active angle button.
      SensorAngle angle = currentAngle();
      for (GuiButton button : buttonList) {
        if (button.id == ANGLE_NONE_ID) {
          button.enabled = angle != SensorAngle.NONE;
        } else if (button.id == ANGLE_LEFT_ID) {
          button.enabled = angle != SensorAngle.LEFT;
        } else if (button.id == ANGLE_RIGHT_ID) {
          button.enabled = angle != SensorAngle.RIGHT;
        }
      }
      // Caption above the angle row.
      int angleRowY = topY + 5 * ROW_SPACING;
      drawCenteredString(fontRenderer, "Aim angle:", width / 2, angleRowY - 10, 0xC0C0C0);
    }

    super.drawScreen(mouseX, mouseY, partialTicks);
  }

  private static String zoneLabel(ZoneType type, int boxes) {
    return "Clear " + type.getDisplayName() + ": "
        + (boxes == 0 ? "Not set" : boxes + (boxes == 1 ? " box" : " boxes"));
  }

  private static boolean isClearButton(int id) {
    return id == CLEAR_STANDARD_ID || id == CLEAR_LEFT_ID || id == CLEAR_RIGHT_ID
        || id == CLEAR_PROTECTED_ID;
  }

  /** @return the zone a per-zone button acts on, or {@code null} for any other button. */
  private static ZoneType zoneTypeFor(int id) {
    switch (id) {
      case CLEAR_STANDARD_ID:
      case REMOVE_LAST_STANDARD_ID:
        return ZoneType.STANDARD;
      case CLEAR_LEFT_ID:
      case REMOVE_LAST_LEFT_ID:
        return ZoneType.LEFT;
      case CLEAR_RIGHT_ID:
      case REMOVE_LAST_RIGHT_ID:
        return ZoneType.RIGHT;
      case CLEAR_PROTECTED_ID:
      case REMOVE_LAST_PROTECTED_ID:
        return ZoneType.PROTECTED;
      default:
        return null;
    }
  }

  @Override
  protected void actionPerformed(GuiButton button) throws IOException {
    SensorConfigAction action = null;
    switch (button.id) {
      case CLOSE_ID:
        mc.displayGuiScreen(null);
        return;
      case CLEAR_STANDARD_ID:
        action = SensorConfigAction.CLEAR_STANDARD;
        break;
      case CLEAR_LEFT_ID:
        action = SensorConfigAction.CLEAR_LEFT;
        break;
      case CLEAR_RIGHT_ID:
        action = SensorConfigAction.CLEAR_RIGHT;
        break;
      case CLEAR_PROTECTED_ID:
        action = SensorConfigAction.CLEAR_PROTECTED;
        break;
      case CLEAR_ALL_ID:
        action = SensorConfigAction.CLEAR_ALL;
        break;
      case ANGLE_NONE_ID:
        action = SensorConfigAction.ANGLE_NONE;
        break;
      case ANGLE_LEFT_ID:
        action = SensorConfigAction.ANGLE_LEFT;
        break;
      case ANGLE_RIGHT_ID:
        action = SensorConfigAction.ANGLE_RIGHT;
        break;
      case REMOVE_LAST_STANDARD_ID:
        action = SensorConfigAction.REMOVE_LAST_STANDARD;
        break;
      case REMOVE_LAST_LEFT_ID:
        action = SensorConfigAction.REMOVE_LAST_LEFT;
        break;
      case REMOVE_LAST_RIGHT_ID:
        action = SensorConfigAction.REMOVE_LAST_RIGHT;
        break;
      case REMOVE_LAST_PROTECTED_ID:
        action = SensorConfigAction.REMOVE_LAST_PROTECTED;
        break;
      default:
        return;
    }
    CsmRoads.NETWORK.sendToServer(new SensorConfigPacket(blockPos, action.ordinal()));
  }

  @Override
  public boolean doesGuiPauseGame() {
    return false;
  }
}

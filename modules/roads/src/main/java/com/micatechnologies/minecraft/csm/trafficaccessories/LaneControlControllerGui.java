package com.micatechnologies.minecraft.csm.trafficaccessories;

import com.micatechnologies.minecraft.csm.roads.CsmRoads;
import com.micatechnologies.minecraft.csm.trafficsignals.logic.TrafficTimeOfDaySchedule;
import java.io.IOException;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * Configuration GUI for a lane control controller: one row per group, showing that group's aspect
 * in each of the four time-of-day slots, plus its clearance interval.
 *
 * <p>Laid out as a table rather than as one setting per screen because the thing an operator
 * needs to see is the whole day at once — a reversible lane is only comprehensible when the AM
 * and PM aspects are side by side.</p>
 */
@SideOnly(Side.CLIENT)
public class LaneControlControllerGui extends GuiScreen {

  private static final int ROW_HEIGHT = 22;
  private static final int SLOT_BUTTON_WIDTH = 78;
  private static final int SLOT_GAP = 2;
  private static final int NARROW_WIDTH = 54;

  private static final int ID_ADD = 500;
  private static final int ID_CLOSE = 501;
  private static final int ID_MANUAL = 502;
  /** Group rows start here; each group takes a block of ids so the row can be recovered. */
  private static final int ID_GROUP_BASE = 0;
  private static final int IDS_PER_GROUP = 16;

  private final TileEntityLaneControlController controller;
  private final BlockPos blockPos;

  /**
   * How many groups the buttons were last built for. Adding or removing one happens on the
   * server, so the change arrives as a tile entity sync rather than as anything this screen
   * did — without noticing it, the table would keep showing the rows it opened with.
   */
  private int builtForGroups = -1;

  public LaneControlControllerGui(TileEntityLaneControlController controller) {
    this.controller = controller;
    this.blockPos = controller.getPos();
  }

  @Override
  public void initGui() {
    buttonList.clear();
    int groups = controller.getGroups().size();
    builtForGroups = groups;
    int tableWidth = SLOT_BUTTON_WIDTH * TrafficTimeOfDaySchedule.SLOT_COUNT
        + SLOT_GAP * (TrafficTimeOfDaySchedule.SLOT_COUNT - 1) + NARROW_WIDTH * 2 + SLOT_GAP * 2;
    int left = width / 2 - tableWidth / 2;
    int top = topRow();

    for (int g = 0; g < groups; g++) {
      int y = top + g * ROW_HEIGHT;
      int base = ID_GROUP_BASE + g * IDS_PER_GROUP;
      int x = left;
      // Remove, then one button per slot, then the clearance.
      buttonList.add(new GuiButton(base, x, y, NARROW_WIDTH, 20, "Remove"));
      x += NARROW_WIDTH + SLOT_GAP;
      for (int slot = 0; slot < TrafficTimeOfDaySchedule.SLOT_COUNT; slot++) {
        buttonList.add(new GuiButton(base + 1 + slot, x, y, SLOT_BUTTON_WIDTH, 20, ""));
        x += SLOT_BUTTON_WIDTH + SLOT_GAP;
      }
      buttonList.add(new GuiButton(base + 1 + TrafficTimeOfDaySchedule.SLOT_COUNT, x, y,
          NARROW_WIDTH, 20, ""));
    }

    int footer = top + groups * ROW_HEIGHT + 6;
    buttonList.add(new GuiButton(ID_ADD, left, footer, 100, 20, "Add Group"));
    buttonList.add(new GuiButton(ID_MANUAL, left + 104, footer, 150, 20, ""));
    buttonList.add(new GuiButton(ID_CLOSE, left + 258, footer, 100, 20, "Close"));
  }

  private int topRow() {
    int rows = controller.getGroups().size() + 1;
    return height / 2 - rows * ROW_HEIGHT / 2;
  }

  @Override
  public void drawScreen(int mouseX, int mouseY, float partialTicks) {
    drawDefaultBackground();

    int groups = controller.getGroups().size();
    if (groups != builtForGroups) {
      initGui();
    }
    int active = controller.getActiveSlot();

    for (GuiButton button : buttonList) {
      if (button.id == ID_MANUAL) {
        button.displayString = controller.getManualSlot()
            == TileEntityLaneControlController.MANUAL_OFF
            ? "Mode: Clock"
            : "Mode: Hold "
                + TrafficTimeOfDaySchedule.SLOT_NAMES[controller.getManualSlot()];
      } else if (button.id >= ID_GROUP_BASE && button.id < groups * IDS_PER_GROUP) {
        int group = button.id / IDS_PER_GROUP;
        int within = button.id % IDS_PER_GROUP;
        if (within >= 1 && within <= TrafficTimeOfDaySchedule.SLOT_COUNT) {
          LaneControlGroup lane = controller.getGroups().get(group);
          button.displayString = lane.getAspect(within - 1).getFriendlyName();
        } else if (within == 1 + TrafficTimeOfDaySchedule.SLOT_COUNT) {
          long ticks = controller.getGroups().get(group).getClearanceTicks();
          button.displayString = (ticks / 20L) + "s";
        }
      }
    }

    int top = topRow();
    drawCenteredString(fontRenderer, "Lane Control Controller", width / 2, top - 34, 0xFFFFFF);

    // The column headings double as the schedule: the hour each slot begins is part of what an
    // operator is reading when they look at this table.
    int tableWidth = SLOT_BUTTON_WIDTH * TrafficTimeOfDaySchedule.SLOT_COUNT
        + SLOT_GAP * (TrafficTimeOfDaySchedule.SLOT_COUNT - 1) + NARROW_WIDTH * 2 + SLOT_GAP * 2;
    int x = width / 2 - tableWidth / 2 + NARROW_WIDTH + SLOT_GAP;
    for (int slot = 0; slot < TrafficTimeOfDaySchedule.SLOT_COUNT; slot++) {
      String heading = TrafficTimeOfDaySchedule.SLOT_NAMES[slot] + " "
          + TrafficTimeOfDaySchedule.formatHour(controller.getSchedule().getStartHour(slot));
      drawCenteredString(fontRenderer, heading, x + SLOT_BUTTON_WIDTH / 2, top - 12,
          slot == active ? 0xFFD070 : 0x909090);
      x += SLOT_BUTTON_WIDTH + SLOT_GAP;
    }
    drawCenteredString(fontRenderer, "Clear", x + NARROW_WIDTH / 2, top - 12, 0x909090);

    if (groups == 0) {
      drawCenteredString(fontRenderer,
          "No groups yet — add one, then link signals with the signal link tool",
          width / 2, top + 4, 0xA0A0A0);
    }

    super.drawScreen(mouseX, mouseY, partialTicks);
  }

  @Override
  protected void actionPerformed(GuiButton button) throws IOException {
    if (button.id == ID_CLOSE) {
      mc.displayGuiScreen(null);
      return;
    }
    if (button.id == ID_ADD) {
      send(LaneControlControllerConfigAction.ADD_GROUP, 0, 0);
      return;
    }
    if (button.id == ID_MANUAL) {
      send(LaneControlControllerConfigAction.CYCLE_MANUAL, 0, 0);
      return;
    }
    int groups = controller.getGroups().size();
    if (button.id < ID_GROUP_BASE || button.id >= groups * IDS_PER_GROUP) {
      return;
    }
    int group = button.id / IDS_PER_GROUP;
    int within = button.id % IDS_PER_GROUP;
    if (within == 0) {
      send(LaneControlControllerConfigAction.REMOVE_GROUP, group, 0);
    } else if (within <= TrafficTimeOfDaySchedule.SLOT_COUNT) {
      send(LaneControlControllerConfigAction.CYCLE_ASPECT, group, within - 1);
    } else {
      // Shift-click steps the clearance down, since it is the one numeric setting here.
      send(LaneControlControllerConfigAction.ADJUST_CLEARANCE, group,
          isShiftKeyDown() ? -1 : 1);
    }
  }

  private void send(LaneControlControllerConfigAction action, int group, int slot) {
    CsmRoads.NETWORK.sendToServer(
        new LaneControlControllerConfigPacket(blockPos, action.ordinal(), group, slot));
  }

  @Override
  public boolean doesGuiPauseGame() {
    return false;
  }
}

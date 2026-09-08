package com.micatechnologies.minecraft.csm.trafficsignals;

import com.micatechnologies.minecraft.csm.roads.CsmRoads;
import java.io.IOException;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * Configuration GUI for an RRFB, opened by the signal head configuration tool the same way the
 * blankout box and crosswalk signal GUIs are. Reads from the client-side tile entity every
 * frame, so the labels catch up on their own once the server has processed each click.
 *
 * <p>Only appearance is offered. When the beacon flashes is the controller's business, and the
 * sequence itself is fixed by IA-21 rather than being a preference.</p>
 */
@SideOnly(Side.CLIENT)
public class RrfbConfigGui extends GuiScreen {

  private static final int BUTTON_WIDTH = 200;
  private static final int BUTTON_HEIGHT = 20;
  private static final int ROW_SPACING = 22;
  private static final int CLOSE_BUTTON_ID = 100;

  /** Indexes line up with {@link RrfbConfigAction} ordinals, so button.id is the action. */
  private static final String[] LABELS = {
      "Housing Color",
      "Sides",
  };

  private final TileEntityRrfb tileEntity;
  private final BlockPos blockPos;

  public RrfbConfigGui(TileEntityRrfb tileEntity) {
    this.tileEntity = tileEntity;
    this.blockPos = tileEntity.getPos();
  }

  @Override
  public void initGui() {
    buttonList.clear();
    int x = width / 2 - BUTTON_WIDTH / 2;
    int topY = height / 2 - (LABELS.length + 1) * ROW_SPACING / 2;
    for (int i = 0; i < LABELS.length; i++) {
      buttonList.add(new GuiButton(i, x, topY + i * ROW_SPACING, BUTTON_WIDTH, BUTTON_HEIGHT, ""));
    }
    buttonList.add(new GuiButton(CLOSE_BUTTON_ID, x,
        topY + LABELS.length * ROW_SPACING + 4, BUTTON_WIDTH, BUTTON_HEIGHT, "Close"));
  }

  @Override
  public void drawScreen(int mouseX, int mouseY, float partialTicks) {
    drawDefaultBackground();
    for (int i = 0; i < LABELS.length && i < buttonList.size(); i++) {
      buttonList.get(i).displayString = LABELS[i] + ": " + getCurrentValue(i);
    }
    int topY = height / 2 - (LABELS.length + 1) * ROW_SPACING / 2;
    drawCenteredString(fontRenderer, "RRFB Configuration", width / 2, topY - 14, 0xFFFFFF);
    super.drawScreen(mouseX, mouseY, partialTicks);
  }

  private String getCurrentValue(int actionOrdinal) {
    if (actionOrdinal >= RrfbConfigAction.values().length) {
      return "N/A";
    }
    switch (RrfbConfigAction.values()[actionOrdinal]) {
      case CYCLE_HOUSING_COLOR:
        return tileEntity.getHousingColor().getFriendlyName();
      case TOGGLE_DOUBLE_SIDED:
        return tileEntity.isDoubleSided() ? "Double (both faces)" : "Single (front only)";
      default:
        return "N/A";
    }
  }

  @Override
  protected void actionPerformed(GuiButton button) throws IOException {
    if (button.id == CLOSE_BUTTON_ID) {
      mc.displayGuiScreen(null);
    } else if (button.id >= 0 && button.id < RrfbConfigAction.values().length) {
      CsmRoads.NETWORK.sendToServer(new RrfbConfigPacket(blockPos, button.id));
    }
  }

  @Override
  public boolean doesGuiPauseGame() {
    return false;
  }
}

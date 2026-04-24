package com.micatechnologies.minecraft.csm.trafficaccessories;

import com.micatechnologies.minecraft.csm.CsmNetwork;
import com.micatechnologies.minecraft.csm.codeutils.packets.TileEntityVariableSpeedLimitUpdatePacket;
import javax.annotation.ParametersAreNonnullByDefault;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import org.lwjgl.input.Keyboard;

public class BlockOverheadSpeedLimitGui extends GuiScreen {

  private static final int BTN_SAVE = 0;
  private static final int BTN_CANCEL = 1;
  private static final int BTN_SPEED_DOWN = 2;
  private static final int BTN_SPEED_UP = 3;

  private static final int FIELD_WIDTH = 200;

  private final TileEntityOverheadSpeedLimit tileEntity;
  private int speedValue;

  public BlockOverheadSpeedLimitGui(TileEntityOverheadSpeedLimit tileEntity) {
    this.tileEntity = tileEntity;
    this.speedValue = tileEntity.getSpeedValue();
  }

  @Override
  public void initGui() {
    Keyboard.enableRepeatEvents(true);
    super.initGui();
    buttonList.clear();

    ScaledResolution sr = new ScaledResolution(this.mc);
    int centerX = sr.getScaledWidth() / 2;
    int fieldLeft = centerX - FIELD_WIDTH / 2;
    int startY = sr.getScaledHeight() / 2 - 40;

    int row = startY;

    buttonList.add(new GuiButton(BTN_SPEED_DOWN, fieldLeft, row, 40, 20, "- 5"));
    buttonList.add(new GuiButton(BTN_SPEED_UP, fieldLeft + FIELD_WIDTH - 40, row, 40, 20, "+ 5"));
    row += 30;

    buttonList.add(new GuiButton(BTN_SAVE, fieldLeft, row, FIELD_WIDTH, 20, "Save"));
    row += 25;
    buttonList.add(new GuiButton(BTN_CANCEL, fieldLeft, row, FIELD_WIDTH, 20, "Cancel"));
  }

  @Override
  public void drawScreen(int mouseX, int mouseY, float partialTicks) {
    drawDefaultBackground();

    ScaledResolution sr = new ScaledResolution(this.mc);
    int centerX = sr.getScaledWidth() / 2;
    int startY = sr.getScaledHeight() / 2 - 40;

    drawCenteredString(fontRenderer, "Overhead Speed Limit Sign", centerX, startY - 15,
        0xFFAA00);

    drawCenteredString(fontRenderer, "Speed Limit: " + speedValue + " MPH", centerX,
        startY + 6, 0xFFFFFF);

    super.drawScreen(mouseX, mouseY, partialTicks);
  }

  @Override
  @ParametersAreNonnullByDefault
  protected void actionPerformed(GuiButton button) {
    switch (button.id) {
      case BTN_SAVE:
        CsmNetwork.sendToServer(new TileEntityVariableSpeedLimitUpdatePacket(
            tileEntity.getPos(), speedValue,
            TileEntityVariableSpeedLimit.FLASHER_NONE, 0, 0));
        this.mc.displayGuiScreen(null);
        break;

      case BTN_CANCEL:
        this.mc.displayGuiScreen(null);
        break;

      case BTN_SPEED_DOWN:
        if (speedValue > 20) {
          speedValue -= 5;
        }
        break;

      case BTN_SPEED_UP:
        if (speedValue < 95) {
          speedValue += 5;
        }
        break;
    }
  }

  @Override
  public boolean doesGuiPauseGame() {
    return true;
  }

  @Override
  public void onGuiClosed() {
    Keyboard.enableRepeatEvents(false);
  }
}

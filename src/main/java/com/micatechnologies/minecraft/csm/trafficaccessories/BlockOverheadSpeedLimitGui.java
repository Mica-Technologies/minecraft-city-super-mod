package com.micatechnologies.minecraft.csm.trafficaccessories;

import com.micatechnologies.minecraft.csm.CsmNetwork;
import com.micatechnologies.minecraft.csm.codeutils.packets.TileEntityVariableSpeedLimitUpdatePacket;
import com.micatechnologies.minecraft.csm.trafficsignals.logic.TrafficSignalBodyColor;
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
  private static final int BTN_HOUSING_COLOR = 4;
  private static final int BTN_FULL_SCREEN = 5;

  private static final int FIELD_WIDTH = 200;

  private final TileEntityOverheadSpeedLimit tileEntity;
  private int speedValue;
  private TrafficSignalBodyColor housingColor;
  private boolean fullScreen;

  public BlockOverheadSpeedLimitGui(TileEntityOverheadSpeedLimit tileEntity) {
    this.tileEntity = tileEntity;
    this.speedValue = tileEntity.getSpeedValue();
    this.housingColor = tileEntity.getHousingColor();
    this.fullScreen = tileEntity.isFullScreen();
  }

  @Override
  public void initGui() {
    Keyboard.enableRepeatEvents(true);
    super.initGui();
    buttonList.clear();

    ScaledResolution sr = new ScaledResolution(this.mc);
    int centerX = sr.getScaledWidth() / 2;
    int fieldLeft = centerX - FIELD_WIDTH / 2;
    int halfWidth = (FIELD_WIDTH - 4) / 2;
    int startY = sr.getScaledHeight() / 2 - 50;

    int row = startY;

    buttonList.add(new GuiButton(BTN_SPEED_DOWN, fieldLeft, row, 40, 20, "- 5"));
    buttonList.add(new GuiButton(BTN_SPEED_UP, fieldLeft + FIELD_WIDTH - 40, row, 40, 20, "+ 5"));
    row += 25;

    buttonList.add(new GuiButton(BTN_HOUSING_COLOR, fieldLeft, row, FIELD_WIDTH, 20, ""));
    row += 25;

    buttonList.add(new GuiButton(BTN_FULL_SCREEN, fieldLeft, row, FIELD_WIDTH, 20, ""));
    row += 25;

    buttonList.add(new GuiButton(BTN_SAVE, fieldLeft, row, halfWidth, 20, "Save"));
    buttonList.add(
        new GuiButton(BTN_CANCEL, fieldLeft + halfWidth + 4, row, halfWidth, 20, "Cancel"));
  }

  @Override
  public void drawScreen(int mouseX, int mouseY, float partialTicks) {
    drawDefaultBackground();

    ScaledResolution sr = new ScaledResolution(this.mc);
    int centerX = sr.getScaledWidth() / 2;
    int startY = sr.getScaledHeight() / 2 - 50;

    drawCenteredString(fontRenderer, "Overhead Speed Limit Sign", centerX, startY - 15,
        0xFFAA00);

    drawCenteredString(fontRenderer, "Speed Limit: " + speedValue + " MPH", centerX,
        startY + 6, 0xFFFFFF);

    for (GuiButton btn : buttonList) {
      if (btn.id == BTN_HOUSING_COLOR) {
        btn.displayString = "Housing: " + housingColor.getFriendlyName();
      } else if (btn.id == BTN_FULL_SCREEN) {
        btn.displayString = "Full Screen: " + (fullScreen ? "Yes" : "No");
      }
    }

    super.drawScreen(mouseX, mouseY, partialTicks);
  }

  @Override
  @ParametersAreNonnullByDefault
  protected void actionPerformed(GuiButton button) {
    switch (button.id) {
      case BTN_SAVE:
        CsmNetwork.sendToServer(new TileEntityVariableSpeedLimitUpdatePacket(
            tileEntity.getPos(), speedValue,
            TileEntityVariableSpeedLimit.FLASHER_NONE, 0, 0, housingColor.toNBT(),
            fullScreen));
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

      case BTN_HOUSING_COLOR:
        housingColor = housingColor.getNextColor();
        break;

      case BTN_FULL_SCREEN:
        fullScreen = !fullScreen;
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

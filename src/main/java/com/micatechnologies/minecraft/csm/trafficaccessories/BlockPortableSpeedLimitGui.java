package com.micatechnologies.minecraft.csm.trafficaccessories;

import com.micatechnologies.minecraft.csm.CsmNetwork;
import com.micatechnologies.minecraft.csm.codeutils.packets.TileEntityVariableSpeedLimitUpdatePacket;
import javax.annotation.ParametersAreNonnullByDefault;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import org.lwjgl.input.Keyboard;

public class BlockPortableSpeedLimitGui extends GuiScreen {

  private static final int BTN_SAVE = 0;
  private static final int BTN_CANCEL = 1;
  private static final int BTN_SPEED_DOWN = 2;
  private static final int BTN_SPEED_UP = 3;
  private static final int BTN_FLASHERS = 4;
  private static final int BTN_COLOR = 5;
  private static final int BTN_ANGLE = 6;

  private static final int FIELD_WIDTH = 200;

  private final TileEntityVariableSpeedLimit tileEntity;
  private int speedValue;
  private int flasherMode;
  private int trailerColor;
  private int signAngle;

  private GuiButton flashersBtn;
  private GuiButton colorBtn;
  private GuiButton angleBtn;

  public BlockPortableSpeedLimitGui(TileEntityVariableSpeedLimit tileEntity) {
    this.tileEntity = tileEntity;
    this.speedValue = tileEntity.getSpeedValue();
    this.flasherMode = tileEntity.getFlasherMode();
    this.trailerColor = tileEntity.getTrailerColor();
    this.signAngle = tileEntity.getSignAngle();
  }

  @Override
  public void initGui() {
    Keyboard.enableRepeatEvents(true);
    super.initGui();
    buttonList.clear();

    ScaledResolution sr = new ScaledResolution(this.mc);
    int centerX = sr.getScaledWidth() / 2;
    int fieldLeft = centerX - FIELD_WIDTH / 2;
    int startY = sr.getScaledHeight() / 2 - 70;

    int row = startY;

    // Speed value selector
    buttonList.add(new GuiButton(BTN_SPEED_DOWN, fieldLeft, row, 40, 20, "- 5"));
    buttonList.add(new GuiButton(BTN_SPEED_UP, fieldLeft + FIELD_WIDTH - 40, row, 40, 20, "+ 5"));
    row += 30;

    // Flashers toggle
    flashersBtn = new GuiButton(BTN_FLASHERS, fieldLeft, row, FIELD_WIDTH, 20, "");
    buttonList.add(flashersBtn);
    row += 25;

    // Color + angle selectors
    colorBtn = new GuiButton(BTN_COLOR, fieldLeft, row, 97, 20, "");
    angleBtn = new GuiButton(BTN_ANGLE, fieldLeft + 103, row, 97, 20, "");
    buttonList.add(colorBtn);
    buttonList.add(angleBtn);
    row += 30;

    // Save / Cancel
    buttonList.add(new GuiButton(BTN_SAVE, fieldLeft, row, FIELD_WIDTH, 20, "Save"));
    row += 25;
    buttonList.add(new GuiButton(BTN_CANCEL, fieldLeft, row, FIELD_WIDTH, 20, "Cancel"));

    updateButtonStates();
  }

  private void updateButtonStates() {
    flashersBtn.displayString =
        "Flashers: " + TileEntityVariableSpeedLimit.FLASHER_MODE_NAMES[flasherMode];
    colorBtn.displayString = TileEntityVariableSpeedLimit.COLOR_NAMES[trailerColor];
    angleBtn.displayString = TileEntityVariableSpeedLimit.ANGLE_NAMES[signAngle];
  }

  @Override
  public void drawScreen(int mouseX, int mouseY, float partialTicks) {
    drawDefaultBackground();

    ScaledResolution sr = new ScaledResolution(this.mc);
    int centerX = sr.getScaledWidth() / 2;
    int startY = sr.getScaledHeight() / 2 - 70;

    drawCenteredString(fontRenderer, "Portable Speed Limit Sign", centerX, startY - 15,
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
            tileEntity.getPos(), speedValue, flasherMode, trailerColor, signAngle));
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

      case BTN_FLASHERS:
        flasherMode = (flasherMode + 1) % TileEntityVariableSpeedLimit.FLASHER_MODE_COUNT;
        updateButtonStates();
        break;

      case BTN_COLOR:
        trailerColor = (trailerColor + 1) % TileEntityVariableSpeedLimit.COLOR_COUNT;
        updateButtonStates();
        break;

      case BTN_ANGLE:
        signAngle = (signAngle + 1) % TileEntityVariableSpeedLimit.ANGLE_COUNT;
        updateButtonStates();
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

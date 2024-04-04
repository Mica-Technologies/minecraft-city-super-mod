package com.micatechnologies.minecraft.csm.technology;

import com.micatechnologies.minecraft.csm.codeutils.GuiMultiLineTextField;
import java.io.IOException;
import javax.annotation.ParametersAreNonnullByDefault;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

public class BlockRedstoneTTSGui extends GuiScreen {

  private final TileEntityRedstoneTTS tileEntityRedstoneTTS;
  private GuiMultiLineTextField ttsStringField;
  private GuiButton closeButton;
  private GuiButton cancelButton;

  public BlockRedstoneTTSGui(TileEntityRedstoneTTS tileEntityRedstoneTTS) {
    this.tileEntityRedstoneTTS = tileEntityRedstoneTTS;
  }

  @Override
  public void drawScreen(int mouseX, int mouseY, float partialTicks) {
    this.drawDefaultBackground();
    ttsStringField.drawTextBox();
    super.drawScreen(mouseX, mouseY, partialTicks);
  }

  @Override
  protected void keyTyped(char typedChar, int keyCode) throws IOException {
    this.ttsStringField.setFocused(true);
    super.keyTyped(typedChar, keyCode);
    this.ttsStringField.textboxKeyTyped(typedChar, keyCode);
  }

  @Override
  protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
    super.mouseClicked(mouseX, mouseY, mouseButton);
    this.ttsStringField.mouseClicked(mouseX, mouseY, mouseButton);
  }

  @Override
  public void handleMouseInput() throws IOException {
    super.handleMouseInput();
    int mouseX = Mouse.getEventX() * this.width / this.mc.displayWidth;
    int mouseY = this.height - Mouse.getEventY() * this.height / this.mc.displayHeight - 1;
    int scrollDelta = Mouse.getEventDWheel();

    // Call handleMouseInput method of GuiMultiLineTextField instance
    this.ttsStringField.handleMouseInput(mouseX, mouseY, scrollDelta);
  }


  @Override
  @ParametersAreNonnullByDefault
  protected void actionPerformed(GuiButton button) {
    if (button == closeButton) {
      try {
        tileEntityRedstoneTTS.setTtsStringFromGui(ttsStringField.getText());
      } catch (Exception e) {
        e.printStackTrace();
      }
      this.mc.displayGuiScreen(null);
    } else if (button == cancelButton) {
      this.mc.displayGuiScreen(null);
    }
  }

  @Override
  public void initGui() {
    Keyboard.enableRepeatEvents(true);
    super.initGui();
    ScaledResolution sr = new ScaledResolution(this.mc);
    this.ttsStringField = new GuiMultiLineTextField(0, fontRenderer, sr.getScaledWidth() / 2 - 100,
        5, 200, 150);
    this.buttonList.add(
        closeButton = new GuiButton(0, sr.getScaledWidth() / 2 - 100, sr.getScaledHeight() - 50,
            200, 20,
            "Close"));
    this.buttonList.add(
        cancelButton = new GuiButton(1, sr.getScaledWidth() / 2 - 100, sr.getScaledHeight() - 25,
            200, 20,
            "Cancel"));
    this.ttsStringField.setText(tileEntityRedstoneTTS.getTtsString());
    this.ttsStringField.setFocused(true);
  }

  @Override
  public void updateScreen() {
    super.updateScreen();
  }

  @Override
  public void onGuiClosed() {
    Keyboard.enableRepeatEvents(false);
    super.onGuiClosed();
  }

  @Override
  public boolean doesGuiPauseGame() {
    return true;
  }
}

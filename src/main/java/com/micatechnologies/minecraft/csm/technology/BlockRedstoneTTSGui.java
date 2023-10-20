package com.micatechnologies.minecraft.csm.technology;

import com.micatechnologies.minecraft.csm.codeutils.GuiMultiLineTextField;
import java.io.IOException;
import javax.annotation.ParametersAreNonnullByDefault;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.gui.ScaledResolution;
import org.lwjgl.input.Keyboard;

public class BlockRedstoneTTSGui extends GuiScreen {

  private final TileEntityRedstoneTTS tileEntityRedstoneTTS;
  private GuiMultiLineTextField ttsStringField;
  private GuiTextField ttsVoiceDurationStretchField;
  private GuiButton closeButton;
  private GuiButton cancelButton;

  public BlockRedstoneTTSGui(TileEntityRedstoneTTS tileEntityRedstoneTTS) {
    this.tileEntityRedstoneTTS = tileEntityRedstoneTTS;
  }

  @Override
  public void drawScreen(int mouseX, int mouseY, float partialTicks) {
    this.drawDefaultBackground();
    ttsStringField.drawTextBox();
    ttsVoiceDurationStretchField.drawTextBox();
    super.drawScreen(mouseX, mouseY, partialTicks);
  }

  @Override
  protected void keyTyped(char typedChar, int keyCode) throws IOException {
    this.ttsStringField.setFocused(true);
    super.keyTyped(typedChar, keyCode);
    this.ttsStringField.textboxKeyTyped(typedChar, keyCode);
    this.ttsVoiceDurationStretchField.textboxKeyTyped(typedChar, keyCode);
  }

  @Override
  protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
    super.mouseClicked(mouseX, mouseY, mouseButton);
  }

  @Override
  @ParametersAreNonnullByDefault
  protected void actionPerformed(GuiButton button) {
    if (button == closeButton) {
      try {
        tileEntityRedstoneTTS.setTtsString(ttsStringField.getText());
        tileEntityRedstoneTTS.setTtsVoiceDurationStretch(
            Float.parseFloat(ttsVoiceDurationStretchField.getText()));
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
    this.ttsVoiceDurationStretchField = new GuiTextField(1, fontRenderer,
        sr.getScaledWidth() / 2 - 100,
        sr.getScaledHeight() - 85, 200, 25);
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
    this.ttsStringField.setCanLoseFocus(false);
    this.ttsVoiceDurationStretchField.setText(
        String.valueOf(tileEntityRedstoneTTS.getTtsVoiceDurationStretch()));
  }

  @Override
  public void updateScreen() {
    super.updateScreen();
    this.ttsStringField.updateCursorCounter();
    this.ttsVoiceDurationStretchField.updateCursorCounter();
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

package com.micatechnologies.minecraft.csm.technology;

import com.micatechnologies.minecraft.csm.codeutils.CsmTts;
import com.micatechnologies.minecraft.csm.codeutils.GuiMultiLineTextField;
import java.io.IOException;
import java.util.List;
import javax.annotation.ParametersAreNonnullByDefault;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

public class BlockRedstoneTTSGui extends GuiScreen {

  private static final int BUTTON_ID_CLOSE = 0;
  private static final int BUTTON_ID_CANCEL = 1;
  private static final int BUTTON_ID_VOICE = 2;

  private final TileEntityRedstoneTTS tileEntityRedstoneTTS;
  private GuiMultiLineTextField ttsStringField;
  private GuiButton closeButton;
  private GuiButton cancelButton;
  private GuiButton voiceButton;
  private String selectedVoice;

  public BlockRedstoneTTSGui(TileEntityRedstoneTTS tileEntityRedstoneTTS) {
    this.tileEntityRedstoneTTS = tileEntityRedstoneTTS;
    this.selectedVoice = tileEntityRedstoneTTS.getTtsVoice();
    CsmTts.startInit();
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
    this.ttsStringField.handleMouseInput(mouseX, mouseY, scrollDelta);
  }


  @Override
  @ParametersAreNonnullByDefault
  protected void actionPerformed(GuiButton button) {
    if (button.id == BUTTON_ID_CLOSE) {
      try {
        tileEntityRedstoneTTS.setTtsConfigFromGui(ttsStringField.getText(), selectedVoice);
      } catch (Exception e) {
        e.printStackTrace();
      }
      this.mc.displayGuiScreen(null);
    } else if (button.id == BUTTON_ID_CANCEL) {
      this.mc.displayGuiScreen(null);
    } else if (button.id == BUTTON_ID_VOICE) {
      cycleVoice();
    }
  }

  private void cycleVoice() {
    List<String> voices = CsmTts.getAvailableVoiceIds();
    if (voices.isEmpty()) {
      return;
    }
    int idx = voices.indexOf(selectedVoice);
    idx = (idx + 1) % voices.size();
    selectedVoice = voices.get(idx);
    voiceButton.displayString = "Voice: " + CsmTts.getDisplayName(selectedVoice);
  }

  @Override
  public void initGui() {
    Keyboard.enableRepeatEvents(true);
    super.initGui();
    ScaledResolution sr = new ScaledResolution(this.mc);
    int centerX = sr.getScaledWidth() / 2 - 100;

    this.voiceButton = new GuiButton(BUTTON_ID_VOICE, centerX, 5, 200, 20,
        "Voice: " + CsmTts.getDisplayName(selectedVoice));
    this.buttonList.add(voiceButton);

    this.ttsStringField = new GuiMultiLineTextField(0, fontRenderer, centerX, 30, 200, 150);
    this.ttsStringField.setText(tileEntityRedstoneTTS.getTtsString());
    this.ttsStringField.setFocused(true);

    this.closeButton = new GuiButton(BUTTON_ID_CLOSE, centerX, sr.getScaledHeight() - 50, 200, 20,
        "Close");
    this.buttonList.add(closeButton);

    this.cancelButton = new GuiButton(BUTTON_ID_CANCEL, centerX, sr.getScaledHeight() - 25, 200,
        20, "Cancel");
    this.buttonList.add(cancelButton);
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

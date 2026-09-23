package com.micatechnologies.minecraft.csm.streetscape;

import com.micatechnologies.minecraft.csm.roads.CsmRoads;
import java.io.IOException;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.input.Keyboard;

/**
 * The editor for a utility box's ID number: one field per line of the decal. Every change is
 * sent at once, so the box in the world updates while the screen is open. Emptying every field
 * gives the box back its automatic number.
 *
 * @version 1.0
 */
@SideOnly(Side.CLIENT)
public class UtilityBoxLabelGui extends GuiScreen {

  private static final int FIELD_WIDTH = 100;
  private static final int FIELD_HEIGHT = 20;
  private static final int ROW_SPACING = 30;
  private static final int ID_DONE = 0;

  private final TileEntityUtilityBoxLabel tileEntity;
  private final BlockPos blockPos;
  private final int lines;

  private GuiTextField[] fields;

  public UtilityBoxLabelGui(TileEntityUtilityBoxLabel tileEntity, int lines) {
    this.tileEntity = tileEntity;
    this.blockPos = tileEntity.getPos();
    this.lines = Math.max(1, Math.min(2, lines));
  }

  @Override
  public void initGui() {
    buttonList.clear();
    Keyboard.enableRepeatEvents(true);
    int top = height / 2 - ROW_SPACING;
    String[] current = {tileEntity.getLine1(), tileEntity.getLine2()};
    fields = new GuiTextField[lines];
    for (int i = 0; i < lines; i++) {
      fields[i] = new GuiTextField(i, fontRenderer, width / 2 - FIELD_WIDTH / 2,
          top + i * ROW_SPACING, FIELD_WIDTH, FIELD_HEIGHT);
      fields[i].setMaxStringLength(TileEntityUtilityBoxLabel.MAX_LINE_LENGTH);
      fields[i].setText(current[i]);
    }
    fields[0].setFocused(true);
    buttonList.add(new GuiButton(ID_DONE, width / 2 - FIELD_WIDTH / 2,
        top + lines * ROW_SPACING + 8, FIELD_WIDTH, FIELD_HEIGHT, "Done"));
  }

  @Override
  public void onGuiClosed() {
    Keyboard.enableRepeatEvents(false);
  }

  @Override
  protected void actionPerformed(GuiButton button) throws IOException {
    if (button.id == ID_DONE) {
      mc.displayGuiScreen(null);
    }
  }

  @Override
  protected void keyTyped(char typedChar, int keyCode) throws IOException {
    if (keyCode == Keyboard.KEY_TAB && lines > 1) {
      boolean first = fields[0].isFocused();
      fields[0].setFocused(!first);
      fields[1].setFocused(first);
      return;
    }
    for (GuiTextField field : fields) {
      if (field.textboxKeyTyped(typedChar, keyCode)) {
        // Reflect the tile entity's rules back into the field, so the player sees exactly what
        // the decal will carry.
        String cleaned = TileEntityUtilityBoxLabel.clamp(field.getText());
        if (!cleaned.equals(field.getText())) {
          field.setText(cleaned);
        }
        send();
        return;
      }
    }
    super.keyTyped(typedChar, keyCode);
  }

  @Override
  protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
    super.mouseClicked(mouseX, mouseY, mouseButton);
    for (GuiTextField field : fields) {
      field.mouseClicked(mouseX, mouseY, mouseButton);
    }
  }

  @Override
  public void updateScreen() {
    super.updateScreen();
    for (GuiTextField field : fields) {
      field.updateCursorCounter();
    }
  }

  private void send() {
    CsmRoads.NETWORK.sendToServer(new UtilityBoxLabelPacket(blockPos, fields[0].getText(),
        lines > 1 ? fields[1].getText() : ""));
  }

  @Override
  public void drawScreen(int mouseX, int mouseY, float partialTicks) {
    drawDefaultBackground();
    int top = height / 2 - ROW_SPACING;
    drawCenteredString(fontRenderer, "Utility Box Number", width / 2, top - 30, 0xFFFFFF);
    for (int i = 0; i < lines; i++) {
      drawCenteredString(fontRenderer, lines > 1 ? "Line " + (i + 1) : "Number", width / 2,
          top + i * ROW_SPACING - 10, 0xFFD070);
      fields[i].drawTextBox();
    }
    super.drawScreen(mouseX, mouseY, partialTicks);
    drawCenteredString(fontRenderer, "Leave empty for an automatic number", width / 2,
        top + lines * ROW_SPACING + 34, 0xA0A0A0);
  }

  @Override
  public boolean doesGuiPauseGame() {
    return false;
  }
}

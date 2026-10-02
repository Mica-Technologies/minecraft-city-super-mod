package com.micatechnologies.minecraft.csm.streetscape;

import com.micatechnologies.minecraft.csm.roads.CsmRoads;
import java.io.IOException;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.input.Keyboard;

/**
 * The editor for a utility box's ID number: one field per line of the decal, and for a
 * transformer a field for the trouble phone number on its "IN CASE OF TROUBLE / CALL" sticker.
 * Every change is sent at once, so the box in the world updates while the screen is open.
 * Emptying the number fields gives the box back its automatic number; emptying the phone field
 * gives it back {@link TileEntityUtilityBoxLabel#DEFAULT_PHONE}.
 *
 * @version 1.1
 */
@SideOnly(Side.CLIENT)
public class UtilityBoxLabelGui extends GuiScreen {

  private static final int FIELD_WIDTH = 100;
  private static final int PHONE_FIELD_WIDTH = 120;
  private static final int FIELD_HEIGHT = 20;
  private static final int ROW_SPACING = 30;
  private static final int ID_DONE = 0;

  private final TileEntityUtilityBoxLabel tileEntity;
  private final BlockPos blockPos;
  private final int lines;
  private final boolean hasPhone;

  /** The number lines' fields, then the phone field if there is one. */
  private GuiTextField[] fields;

  public UtilityBoxLabelGui(TileEntityUtilityBoxLabel tileEntity, int lines, boolean hasPhone) {
    this.tileEntity = tileEntity;
    this.blockPos = tileEntity.getPos();
    this.lines = Math.max(1, Math.min(2, lines));
    this.hasPhone = hasPhone;
  }

  private int rows() {
    return lines + (hasPhone ? 1 : 0);
  }

  private int top() {
    return height / 2 - ROW_SPACING * rows() / 2 - 10;
  }

  private boolean isPhone(int index) {
    return hasPhone && index == lines;
  }

  @Override
  public void initGui() {
    buttonList.clear();
    Keyboard.enableRepeatEvents(true);
    int top = top();
    String[] current = {tileEntity.getLine1(), tileEntity.getLine2()};
    fields = new GuiTextField[rows()];
    for (int i = 0; i < fields.length; i++) {
      int w = isPhone(i) ? PHONE_FIELD_WIDTH : FIELD_WIDTH;
      fields[i] = new GuiTextField(i, fontRenderer, width / 2 - w / 2, top + i * ROW_SPACING, w,
          FIELD_HEIGHT);
      if (isPhone(i)) {
        fields[i].setMaxStringLength(TileEntityUtilityBoxLabel.MAX_PHONE_LENGTH);
        fields[i].setText(tileEntity.getPhone());
      } else {
        fields[i].setMaxStringLength(TileEntityUtilityBoxLabel.MAX_LINE_LENGTH);
        fields[i].setText(current[i]);
      }
    }
    fields[0].setFocused(true);
    buttonList.add(new GuiButton(ID_DONE, width / 2 - FIELD_WIDTH / 2,
        top + rows() * ROW_SPACING + 8, FIELD_WIDTH, FIELD_HEIGHT, I18n.format("gui.done")));
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
    if (keyCode == Keyboard.KEY_TAB && fields.length > 1) {
      int focused = 0;
      for (int i = 0; i < fields.length; i++) {
        if (fields[i].isFocused()) {
          focused = i;
        }
      }
      for (int i = 0; i < fields.length; i++) {
        fields[i].setFocused(i == (focused + 1) % fields.length);
      }
      return;
    }
    for (int i = 0; i < fields.length; i++) {
      GuiTextField field = fields[i];
      if (field.textboxKeyTyped(typedChar, keyCode)) {
        // Reflect the tile entity's rules back into the field, so the player sees exactly what
        // the sticker will carry.
        String cleaned = isPhone(i) ? TileEntityUtilityBoxLabel.clampPhone(field.getText())
            : TileEntityUtilityBoxLabel.clamp(field.getText());
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
        lines > 1 ? fields[1].getText() : "", hasPhone ? fields[lines].getText() : null));
  }

  @Override
  public void drawScreen(int mouseX, int mouseY, float partialTicks) {
    drawDefaultBackground();
    int top = top();
    drawCenteredString(fontRenderer, I18n.format("gui.csm.utility_box.title"), width / 2,
        top - 30, 0xFFFFFF);
    for (int i = 0; i < fields.length; i++) {
      String caption;
      if (isPhone(i)) {
        caption = I18n.format("gui.csm.utility_box.phone");
      } else if (lines > 1) {
        caption = I18n.format("gui.csm.utility_box.line", i + 1);
      } else {
        caption = I18n.format("gui.csm.utility_box.number");
      }
      drawCenteredString(fontRenderer, caption, width / 2, top + i * ROW_SPACING - 10, 0xFFD070);
      fields[i].drawTextBox();
    }
    super.drawScreen(mouseX, mouseY, partialTicks);
    int hintY = top + rows() * ROW_SPACING + 34;
    drawCenteredString(fontRenderer, I18n.format("gui.csm.utility_box.automatic"), width / 2,
        hintY, 0xA0A0A0);
    if (hasPhone) {
      drawCenteredString(fontRenderer, I18n.format("gui.csm.utility_box.phone_default",
          TileEntityUtilityBoxLabel.DEFAULT_PHONE), width / 2, hintY + 12, 0xA0A0A0);
    }
  }

  @Override
  public boolean doesGuiPauseGame() {
    return false;
  }
}

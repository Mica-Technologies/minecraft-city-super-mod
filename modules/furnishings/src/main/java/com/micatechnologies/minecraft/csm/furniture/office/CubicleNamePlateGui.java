package com.micatechnologies.minecraft.csm.furniture.office;

import com.micatechnologies.minecraft.csm.furnishings.CsmFurnishings;
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
 * The editor for a cubicle panel's name plate or sign: a field per line (name, title and, on the
 * sign, department). Every change is sent at once, so the plate in the world updates while the
 * screen is open; Tab moves between the fields, Enter or Done closes it.
 *
 * @since 2026.10
 */
@SideOnly(Side.CLIENT)
public class CubicleNamePlateGui extends GuiScreen {

  private static final int FIELD_WIDTH = 200;
  private static final int FIELD_HEIGHT = 20;
  private static final int ROW_SPACING = 34;
  private static final int ID_DONE = 0;
  private static final String[] CAPTIONS = {"gui.csm.cubicle_name.name",
      "gui.csm.cubicle_name.role", "gui.csm.cubicle_name.department"};

  private final TileEntityCubicleNamePlate tileEntity;
  private final BlockPos blockPos;
  private final int lines;
  private final String title;

  private GuiTextField[] fields;

  /**
   * An editor for a plate.
   *
   * @param tileEntity the plate's tile entity
   * @param style      what it carries
   * @param title      the block's name, shown at the top
   */
  public CubicleNamePlateGui(TileEntityCubicleNamePlate tileEntity, CubicleSignStyle style,
      String title) {
    this.tileEntity = tileEntity;
    this.blockPos = tileEntity.getPos();
    this.lines = Math.max(1, Math.min(TileEntityCubicleNamePlate.MAX_LINES,
        style.getLineCount()));
    this.title = title;
  }

  private int top() {
    return height / 2 - ROW_SPACING * lines / 2;
  }

  @Override
  public void initGui() {
    buttonList.clear();
    Keyboard.enableRepeatEvents(true);
    int top = top();
    fields = new GuiTextField[lines];
    for (int i = 0; i < lines; i++) {
      fields[i] = new GuiTextField(i, fontRenderer, width / 2 - FIELD_WIDTH / 2,
          top + i * ROW_SPACING, FIELD_WIDTH, FIELD_HEIGHT);
      fields[i].setMaxStringLength(TileEntityCubicleNamePlate.MAX_LINE_LENGTH);
      fields[i].setText(tileEntity.getLine(i));
    }
    fields[0].setFocused(true);
    buttonList.add(new GuiButton(ID_DONE, width / 2 - 50, top + lines * ROW_SPACING + 4, 100,
        FIELD_HEIGHT, I18n.format("gui.done")));
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
    if (keyCode == Keyboard.KEY_TAB) {
      int focused = 0;
      for (int i = 0; i < fields.length; i++) {
        if (fields[i].isFocused()) {
          focused = i;
        }
      }
      int next = (focused + (isShiftKeyDown() ? fields.length - 1 : 1)) % fields.length;
      for (int i = 0; i < fields.length; i++) {
        fields[i].setFocused(i == next);
      }
      return;
    }
    if (keyCode == Keyboard.KEY_RETURN || keyCode == Keyboard.KEY_NUMPADENTER) {
      mc.displayGuiScreen(null);
      return;
    }
    for (GuiTextField field : fields) {
      if (field.textboxKeyTyped(typedChar, keyCode)) {
        // The font has printable ASCII only, so anything else is taken out of the field at once
        // and the player sees what the plate will carry. Spaces stay while typing; the server
        // trims the ends.
        String text = field.getText();
        StringBuilder cleaned = new StringBuilder(text.length());
        for (int i = 0; i < text.length(); i++) {
          char c = text.charAt(i);
          if (c >= 32 && c < 127) {
            cleaned.append(c);
          }
        }
        if (cleaned.length() != text.length()) {
          field.setText(cleaned.toString());
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
    String[] text = new String[fields.length];
    for (int i = 0; i < fields.length; i++) {
      text[i] = fields[i].getText();
    }
    CsmFurnishings.NETWORK.sendToServer(new CubicleNamePlatePacket(blockPos, text));
  }

  @Override
  public void drawScreen(int mouseX, int mouseY, float partialTicks) {
    drawDefaultBackground();
    int top = top();
    drawCenteredString(fontRenderer, title, width / 2, top - 32, 0xFFFFFF);
    for (int i = 0; i < fields.length; i++) {
      drawString(fontRenderer, I18n.format(CAPTIONS[i]), width / 2 - FIELD_WIDTH / 2,
          top + i * ROW_SPACING - 11, 0xFFD070);
      fields[i].drawTextBox();
    }
    super.drawScreen(mouseX, mouseY, partialTicks);
    drawCenteredString(fontRenderer, I18n.format("gui.csm.cubicle_name.hint"), width / 2,
        top + lines * ROW_SPACING + 32, 0xA0A0A0);
  }

  @Override
  public boolean doesGuiPauseGame() {
    return false;
  }
}

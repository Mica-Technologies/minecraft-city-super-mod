package com.micatechnologies.minecraft.csm.venues.av;

import com.micatechnologies.minecraft.csm.venues.CsmVenues;
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
 * The editor a ribbon board's scrolling text, a banner, the marquee, the ballpark scoreboards and
 * the distance markers share: a field per line and a button per choice (a colour, a speed), each
 * click stepping it on (shift-click back). With more than three lines (the line score) the
 * captions go beside the fields and the choices into one row, so it fits a small window. Every change is
 * sent at once, so the block in the world follows while the screen is open.
 *
 * @since 2026.10
 */
@SideOnly(Side.CLIENT)
public class GuiVenueText extends GuiScreen {

  private static final int FIELD_W = 240;
  private static final int ROW = 30;
  private static final int ROW_COMPACT = 23;
  private static final int COMPACT_FIELD_W = 200;
  private static final int ID_DONE = 100;

  private final IVenueTextHolder holder;
  private final BlockPos pos;
  private final String title;
  private final int[] choices;

  private GuiTextField[] fields;
  private GuiButton[] choiceButtons;

  /**
   * An editor.
   *
   * @param holder the tile entity
   * @param pos    its position
   * @param title  the block's name
   */
  public GuiVenueText(IVenueTextHolder holder, BlockPos pos, String title) {
    this.holder = holder;
    this.pos = pos;
    this.title = title;
    this.choices = new int[holder.choiceCount()];
    for (int i = 0; i < choices.length; i++) {
      choices[i] = holder.getChoice(i);
    }
  }

  /** More than three lines: captions beside the fields, choices in one row. */
  private boolean compact() {
    return holder.lineCount() > 3;
  }

  private int row() {
    return compact() ? ROW_COMPACT : ROW;
  }

  private int fieldX() {
    return compact() ? width / 2 - COMPACT_FIELD_W / 2 + 70 : width / 2 - FIELD_W / 2;
  }

  private int top() {
    int choiceRows = compact() ? (choices.length > 0 ? 1 : 0) : choices.length;
    int h = row() * holder.lineCount() + 24 * (choiceRows + 1);
    return Math.max(compact() ? 18 : 26, height / 2 - h / 2 + (compact() ? 4 : 8));
  }

  @Override
  public void initGui() {
    buttonList.clear();
    Keyboard.enableRepeatEvents(true);
    int top = top();
    int x = fieldX();
    int fw = compact() ? COMPACT_FIELD_W : FIELD_W;
    fields = new GuiTextField[holder.lineCount()];
    for (int i = 0; i < fields.length; i++) {
      fields[i] = new GuiTextField(i, fontRenderer, x, top + i * row(), fw, 20);
      fields[i].setMaxStringLength(holder.maxLength(i));
      fields[i].setText(holder.getLine(i));
    }
    if (fields.length > 0) {
      fields[0].setFocused(true);
    }
    int y = top + fields.length * row();
    choiceButtons = new GuiButton[choices.length];
    if (compact() && choices.length > 0) {
      int total = COMPACT_FIELD_W + 140;
      int bw = (total - 4 * (choices.length - 1)) / choices.length;
      int left = width / 2 - total / 2;
      for (int i = 0; i < choices.length; i++) {
        choiceButtons[i] = new GuiButton(i, left + i * (bw + 4), y, bw, 20, "");
        buttonList.add(choiceButtons[i]);
      }
      y += 24;
    } else {
      for (int i = 0; i < choices.length; i++) {
        choiceButtons[i] = new GuiButton(i, x, y + i * 24, FIELD_W, 20, "");
        buttonList.add(choiceButtons[i]);
      }
      y += choices.length * 24;
    }
    buttonList.add(new GuiButton(ID_DONE, width / 2 - 50, y + 4, 100, 20,
        I18n.format("gui.done")));
    labels();
  }

  private void labels() {
    for (int i = 0; i < choices.length; i++) {
      choiceButtons[i].displayString = I18n.format(holder.choiceCaption(i),
          I18n.format(holder.choiceValueKey(i, choices[i])));
    }
  }

  @Override
  public void onGuiClosed() {
    Keyboard.enableRepeatEvents(false);
  }

  @Override
  protected void actionPerformed(GuiButton button) throws IOException {
    if (button.id == ID_DONE) {
      mc.displayGuiScreen(null);
      return;
    }
    int i = button.id;
    if (i >= 0 && i < choices.length) {
      int n = Math.max(1, holder.choiceRange(i));
      choices[i] = (choices[i] + (isShiftKeyDown() ? n - 1 : 1)) % n;
      labels();
      send();
    }
  }

  @Override
  protected void keyTyped(char typedChar, int keyCode) throws IOException {
    if (keyCode == Keyboard.KEY_TAB && fields.length > 0) {
      int focused = 0;
      for (int i = 0; i < fields.length; i++) {
        if (fields[i].isFocused()) {
          focused = i;
        }
      }
      int next = (focused + 1) % fields.length;
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
        // the font has printable ASCII only: anything else comes out at once
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
    String[] lines = new String[fields.length];
    for (int i = 0; i < fields.length; i++) {
      lines[i] = fields[i].getText();
    }
    CsmVenues.NETWORK.sendToServer(new VenueTextPacket(pos, lines, choices.clone()));
  }

  @Override
  public void drawScreen(int mouseX, int mouseY, float partialTicks) {
    drawDefaultBackground();
    int top = top();
    drawCenteredString(fontRenderer, title, width / 2, Math.max(2, top - (compact() ? 14 : 24)),
        0xFFFFFF);
    for (int i = 0; i < fields.length; i++) {
      String caption = I18n.format(holder.lineCaption(i));
      if (compact()) {
        drawString(fontRenderer, caption, fieldX() - 6 - fontRenderer.getStringWidth(caption),
            top + i * row() + 6, 0xFFD070);
      } else {
        drawString(fontRenderer, caption, width / 2 - FIELD_W / 2, top + i * ROW - 11, 0xFFD070);
      }
      fields[i].drawTextBox();
    }
    super.drawScreen(mouseX, mouseY, partialTicks);
  }

  @Override
  public boolean doesGuiPauseGame() {
    return false;
  }
}

package com.micatechnologies.minecraft.csm.buildingmaterials;

import java.io.IOException;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.input.Keyboard;

/**
 * A garage door keypad's PIN screen: ten digits, clear and enter, and for the keypad's owner a
 * button to set the code. Digits can be typed as well as clicked.
 *
 * <p>The screen never knows the code: it sends what was typed, and the server says whether it was
 * right. It shows only what the client is told -- whether a code is set, and whether this player
 * owns the keypad.</p>
 *
 * @version 1.0
 * @since 2026.9
 */
@SideOnly(Side.CLIENT)
public class GuiGarageKeypad extends GuiScreen {

  private static final int BTN_CLEAR = 10;
  private static final int BTN_ENTER = 11;
  private static final int BTN_SET = 12;

  private static final int KEY = 24;
  /** A vanilla button's texture is 20 px tall; a taller button shows the row below it. */
  private static final int KEY_H = 20;
  private static final int GAP = 4;
  private static final int PANEL_W = KEY * 3 + GAP * 2 + 24;

  private final TileEntityGarageDoorControl keypad;
  private final BlockPos pos;
  private final boolean manager;
  private final StringBuilder entry = new StringBuilder();

  private int left;
  private int top;
  private GuiButton setButton;

  /**
   * Constructs a {@link GuiGarageKeypad}.
   *
   * @param keypad the keypad
   * @param pos    where it is
   * @param player who is using it
   *
   * @since 1.0
   */
  public GuiGarageKeypad(TileEntityGarageDoorControl keypad, BlockPos pos, EntityPlayer player) {
    this.keypad = keypad;
    this.pos = pos;
    this.manager = keypad.mayManage(player.getUniqueID());
  }

  @Override
  public void initGui() {
    buttonList.clear();
    int panelH = 40 + 4 * (KEY_H + GAP) + (manager ? KEY_H + GAP : 0);
    left = (width - PANEL_W) / 2;
    top = (height - panelH) / 2;
    int x0 = left + 12;
    int y0 = top + 34;
    // 1 2 3 / 4 5 6 / 7 8 9 / CLR 0 ENT, as on a real keypad.
    for (int d = 1; d <= 9; d++) {
      int col = (d - 1) % 3;
      int row = (d - 1) / 3;
      buttonList.add(new GuiButton(d, x0 + col * (KEY + GAP), y0 + row * (KEY_H + GAP), KEY, KEY_H,
          Integer.toString(d)));
    }
    int y3 = y0 + 3 * (KEY_H + GAP);
    buttonList.add(new GuiButton(BTN_CLEAR, x0, y3, KEY, KEY_H,
        I18n.format("gui.csm.garage.keypad_clear")));
    buttonList.add(new GuiButton(0, x0 + KEY + GAP, y3, KEY, KEY_H, "0"));
    buttonList.add(new GuiButton(BTN_ENTER, x0 + 2 * (KEY + GAP), y3, KEY, KEY_H,
        I18n.format("gui.csm.garage.keypad_enter")));
    if (manager) {
      setButton = new GuiButton(BTN_SET, x0, y3 + KEY_H + GAP, KEY * 3 + GAP * 2, KEY_H,
          I18n.format("gui.csm.garage.keypad_set"));
      buttonList.add(setButton);
    }
    updateButtons();
  }

  private void updateButtons() {
    int n = entry.length();
    for (GuiButton b : buttonList) {
      if (b.id <= 9) {
        b.enabled = n < TileEntityGarageDoorControl.MAX_DIGITS;
      } else if (b.id == BTN_ENTER) {
        b.enabled = n > 0;
      }
    }
    if (setButton != null) {
      setButton.enabled = n >= TileEntityGarageDoorControl.MIN_DIGITS
          && n <= TileEntityGarageDoorControl.MAX_DIGITS;
    }
  }

  private void digit(int d) {
    if (entry.length() < TileEntityGarageDoorControl.MAX_DIGITS) {
      entry.append((char) ('0' + d));
      updateButtons();
    }
  }

  private void send(int action) {
    CsmBuilding.NETWORK.sendToServer(new GarageKeypadPacket(pos, action, entry.toString()));
    mc.displayGuiScreen(null);
  }

  @Override
  protected void actionPerformed(GuiButton button) throws IOException {
    if (button.id <= 9) {
      digit(button.id);
    } else if (button.id == BTN_CLEAR) {
      entry.setLength(0);
      updateButtons();
    } else if (button.id == BTN_ENTER && entry.length() > 0) {
      send(GarageKeypadPacket.ENTER);
    } else if (button.id == BTN_SET) {
      send(GarageKeypadPacket.SET);
    }
  }

  @Override
  protected void keyTyped(char typedChar, int keyCode) throws IOException {
    if (typedChar >= '0' && typedChar <= '9') {
      digit(typedChar - '0');
    } else if (keyCode == Keyboard.KEY_BACK && entry.length() > 0) {
      entry.setLength(entry.length() - 1);
      updateButtons();
    } else if ((keyCode == Keyboard.KEY_RETURN || keyCode == Keyboard.KEY_NUMPADENTER)
        && entry.length() > 0) {
      send(GarageKeypadPacket.ENTER);
    } else {
      super.keyTyped(typedChar, keyCode);
    }
  }

  @Override
  public void drawScreen(int mouseX, int mouseY, float partialTicks) {
    drawDefaultBackground();
    int panelH = 40 + 4 * (KEY_H + GAP) + (manager ? KEY_H + GAP : 0);
    // The keypad's housing and its display.
    drawRect(left, top, left + PANEL_W, top + panelH, 0xFF2A2724);
    drawRect(left + 12, top + 12, left + PANEL_W - 12, top + 28, 0xFF0E1A10);
    StringBuilder shown = new StringBuilder();
    for (int i = 0; i < entry.length(); i++) {
      shown.append('*');
    }
    drawCenteredString(fontRenderer, shown.toString(), left + PANEL_W / 2, top + 16, 0x7CFF7C);
    drawCenteredString(fontRenderer, I18n.format("gui.csm.garage.keypad"), left + PANEL_W / 2,
        top + 2, 0xB0B0B0);
    String hint = !keypad.hasCode() ? I18n.format("gui.csm.garage.keypad_hint_set")
        : manager ? I18n.format("gui.csm.garage.keypad_hint_owner") : null;
    if (hint != null) {
      drawCenteredString(fontRenderer, hint, width / 2, top + panelH + 6, 0xE0E0E0);
    }
    super.drawScreen(mouseX, mouseY, partialTicks);
  }

  @Override
  public boolean doesGuiPauseGame() {
    return false;
  }
}

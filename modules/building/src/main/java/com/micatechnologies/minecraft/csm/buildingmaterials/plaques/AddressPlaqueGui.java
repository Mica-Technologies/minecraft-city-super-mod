package com.micatechnologies.minecraft.csm.buildingmaterials.plaques;

import com.micatechnologies.minecraft.csm.buildingmaterials.CsmBuilding;
import java.io.IOException;
import javax.annotation.Nullable;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.resources.I18n;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.input.Keyboard;

/**
 * The address plaque editor: the house number and the street line, and buttons for the font,
 * text size, plate size, layout (flat or a vertical stack), numeral colour, the plaque's place in
 * its block, and (on the blocks that have them) the decal's depth and the lamp box's light. A
 * click steps a setting forward and Shift-click back. Every change is sent at once
 * ({@link AddressPlaquePacket}), so the plaque in the world changes while the screen is open;
 * Tab moves between the fields, Enter or Done closes it.
 *
 * @since 2026.11
 */
@SideOnly(Side.CLIENT)
public class AddressPlaqueGui extends GuiScreen {

  private static final int ID_DONE = 0;
  private static final int ID_FONT = 1;
  private static final int ID_SIZE = 2;
  private static final int ID_PLATE = 3;
  private static final int ID_LAYOUT = 4;
  private static final int ID_INK = 5;
  private static final int ID_DX = 6;
  private static final int ID_DY = 7;
  private static final int ID_DZ = 8;
  private static final int ID_LIT = 9;
  private static final float[] SIZES = {0.5f, 0.6f, 0.7f, 0.8f, 0.9f, 1.0f};

  private final BlockPos pos;
  private final PlaqueStyle style;
  private final String title;
  private AddressPlaque plaque;
  private GuiTextField number;
  private GuiTextField street;

  public AddressPlaqueGui(BlockPos pos, PlaqueStyle style, AddressPlaque plaque, String title) {
    this.pos = pos;
    this.style = style;
    this.plaque = plaque;
    this.title = title;
  }

  /**
   * The editor for the plaque at {@code pos}, or null if there is none there.
   *
   * @return the screen
   */
  @Nullable
  public static AddressPlaqueGui open(World world, BlockPos pos) {
    IBlockState state = world.getBlockState(pos);
    TileEntity te = world.getTileEntity(pos);
    if (!(state.getBlock() instanceof BlockAddressPlaque)
        || !(te instanceof TileEntityAddressPlaque)) {
      return null;
    }
    BlockAddressPlaque block = (BlockAddressPlaque) state.getBlock();
    return new AddressPlaqueGui(pos, block.getStyle(),
        ((TileEntityAddressPlaque) te).getPlaque(),
        I18n.format(block.getTranslationKey() + ".name"));
  }

  private int top() {
    return Math.max(4, height / 2 - 100);
  }

  @Override
  public void initGui() {
    buttonList.clear();
    Keyboard.enableRepeatEvents(true);
    int top = top();
    int x0 = width / 2 - 155;
    number = new GuiTextField(100, fontRenderer, x0, top + 34, 100, 20);
    number.setMaxStringLength(AddressPlaque.MAX_NUMBER);
    number.setText(plaque.getNumber());
    number.setFocused(true);
    street = new GuiTextField(101, fontRenderer, x0 + 110, top + 34, 200, 20);
    street.setMaxStringLength(AddressPlaque.MAX_STREET);
    street.setText(plaque.getStreet());
    int[] ids = {ID_FONT, ID_SIZE, ID_PLATE, ID_LAYOUT, ID_INK, ID_DX, ID_DY};
    int n = ids.length;
    for (int k = 0; k < n; k++) {
      buttonList.add(new GuiButton(ids[k], x0 + (k % 2) * 160, top + 64 + (k / 2) * 22, 150, 20,
          ""));
    }
    if (style.isTwoSided()) {
      buttonList.add(new GuiButton(ID_DZ, x0 + (n % 2) * 160, top + 64 + (n / 2) * 22, 150, 20,
          ""));
      n++;
    }
    if (style.isLamp()) {
      buttonList.add(new GuiButton(ID_LIT, x0 + (n % 2) * 160, top + 64 + (n / 2) * 22, 150, 20,
          ""));
      n++;
    }
    buttonList.add(new GuiButton(ID_DONE, width / 2 - 75, top + 64 + ((n + 1) / 2) * 22 + 8,
        150, 20, I18n.format("gui.done")));
    labels();
  }

  private static String on(boolean b) {
    return I18n.format(b ? "options.on" : "options.off");
  }

  private void labels() {
    for (GuiButton b : buttonList) {
      switch (b.id) {
        case ID_FONT:
          b.displayString = I18n.format("gui.csm.address_plaque.font",
              plaque.getFont() == null
                  ? I18n.format("gui.csm.address_plaque.default",
                  I18n.format("gui.csm.address_plaque.font." + style.getDefaultFont().getId()))
                  : I18n.format("gui.csm.address_plaque.font." + plaque.getFont().getId()));
          break;
        case ID_SIZE:
          b.displayString = I18n.format("gui.csm.address_plaque.size",
              Math.round(plaque.getSize() * 100));
          break;
        case ID_PLATE:
          b.displayString = I18n.format("gui.csm.address_plaque.plate",
              I18n.format("gui.csm.address_plaque.plate." + plaque.getPlate().getId()));
          break;
        case ID_LAYOUT:
          b.displayString = I18n.format("gui.csm.address_plaque.layout",
              I18n.format("gui.csm.address_plaque.layout." + (plaque.isVertical() ? "v" : "h")));
          break;
        case ID_INK:
          b.displayString = I18n.format("gui.csm.address_plaque.ink",
              plaque.getInk() == PlaqueInk.DEFAULT
                  ? I18n.format("gui.csm.address_plaque.default",
                  I18n.format("gui.csm.address_plaque.ink." + style.getDefaultInk().getId()))
                  : I18n.format("gui.csm.address_plaque.ink." + plaque.getInk().getId()));
          break;
        case ID_DX:
          b.displayString = I18n.format("gui.csm.address_plaque.dx", plaque.getDx());
          break;
        case ID_DY:
          b.displayString = I18n.format("gui.csm.address_plaque.dy", plaque.getDy());
          break;
        case ID_DZ:
          b.displayString = I18n.format("gui.csm.address_plaque.dz", plaque.getDz());
          break;
        case ID_LIT:
          b.displayString = I18n.format("gui.csm.address_plaque.lit", on(plaque.isLit()));
          break;
        default:
          break;
      }
    }
  }

  private static int stepInt(int v, int lo, int hi, boolean back) {
    int n = v + (back ? -1 : 1);
    return n > hi ? lo : n < lo ? hi : n;
  }

  @Override
  protected void actionPerformed(GuiButton button) throws IOException {
    boolean back = isShiftKeyDown();
    AddressPlaque.Builder b = plaque.toBuilder();
    switch (button.id) {
      case ID_DONE:
        mc.displayGuiScreen(null);
        return;
      case ID_FONT: {
        // default, then each font, and round again
        PlaqueFont[] fonts = PlaqueFont.values();
        int i = plaque.getFont() == null ? -1 : plaque.getFont().ordinal();
        int n = fonts.length + 1;
        int next = Math.floorMod(i + 1 + (back ? -1 : 1), n) - 1;
        b.font(next < 0 ? null : fonts[next]);
        break;
      }
      case ID_SIZE: {
        int i = 0;
        for (int k = 0; k < SIZES.length; k++) {
          if (Math.abs(SIZES[k] - plaque.getSize()) < Math.abs(SIZES[i] - plaque.getSize())) {
            i = k;
          }
        }
        b.size(SIZES[Math.floorMod(i + (back ? -1 : 1), SIZES.length)]);
        break;
      }
      case ID_PLATE: {
        PlaqueSize[] all = PlaqueSize.values();
        b.plate(all[Math.floorMod(plaque.getPlate().ordinal() + (back ? -1 : 1), all.length)]);
        break;
      }
      case ID_LAYOUT:
        b.vertical(!plaque.isVertical());
        break;
      case ID_INK: {
        PlaqueInk[] all = PlaqueInk.values();
        b.ink(all[Math.floorMod(plaque.getInk().ordinal() + (back ? -1 : 1), all.length)]);
        break;
      }
      case ID_DX:
        b.dx(stepInt(plaque.getDx(), -AddressPlaque.MAX_OFFSET, AddressPlaque.MAX_OFFSET, back));
        break;
      case ID_DY:
        b.dy(stepInt(plaque.getDy(), -AddressPlaque.MAX_OFFSET, AddressPlaque.MAX_OFFSET, back));
        break;
      case ID_DZ:
        b.dz(stepInt(plaque.getDz(), AddressPlaque.MIN_DEPTH, AddressPlaque.MAX_DEPTH, back));
        break;
      case ID_LIT:
        b.lit(!plaque.isLit());
        break;
      default:
        return;
    }
    plaque = b.build();
    labels();
    send();
  }

  @Override
  protected void keyTyped(char typedChar, int keyCode) throws IOException {
    if (keyCode == Keyboard.KEY_TAB) {
      boolean first = number.isFocused();
      number.setFocused(!first);
      street.setFocused(first);
      return;
    }
    if (keyCode == Keyboard.KEY_RETURN || keyCode == Keyboard.KEY_NUMPADENTER) {
      mc.displayGuiScreen(null);
      return;
    }
    for (GuiTextField field : new GuiTextField[]{number, street}) {
      if (field.textboxKeyTyped(typedChar, keyCode)) {
        // only what the plaque font has, so the player sees what the plaque will carry; spaces
        // stay while typing and the server trims
        String text = field.getText();
        StringBuilder cleaned = new StringBuilder(text.length());
        for (int k = 0; k < text.length(); k++) {
          if (PlaqueSheet.CHARS.indexOf(text.charAt(k)) >= 0) {
            cleaned.append(text.charAt(k));
          }
        }
        if (cleaned.length() != text.length()) {
          field.setText(cleaned.toString());
        }
        plaque = plaque.toBuilder().number(number.getText()).street(street.getText()).build();
        send();
        return;
      }
    }
    super.keyTyped(typedChar, keyCode);
  }

  @Override
  protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
    super.mouseClicked(mouseX, mouseY, mouseButton);
    number.mouseClicked(mouseX, mouseY, mouseButton);
    street.mouseClicked(mouseX, mouseY, mouseButton);
  }

  @Override
  public void updateScreen() {
    super.updateScreen();
    number.updateCursorCounter();
    street.updateCursorCounter();
  }

  @Override
  public void onGuiClosed() {
    Keyboard.enableRepeatEvents(false);
  }

  @Override
  public boolean doesGuiPauseGame() {
    return false;
  }

  private void send() {
    CsmBuilding.NETWORK.sendToServer(new AddressPlaquePacket(pos, plaque));
  }

  @Override
  public void drawScreen(int mouseX, int mouseY, float partialTicks) {
    drawDefaultBackground();
    int top = top();
    int x0 = width / 2 - 155;
    drawCenteredString(fontRenderer, title, width / 2, top, 0xFFFFFF);
    drawString(fontRenderer, I18n.format("gui.csm.address_plaque.number"), x0, top + 22,
        0xFFD070);
    drawString(fontRenderer, I18n.format("gui.csm.address_plaque.street"), x0 + 110, top + 22,
        0xFFD070);
    number.drawTextBox();
    street.drawTextBox();
    super.drawScreen(mouseX, mouseY, partialTicks);
  }
}

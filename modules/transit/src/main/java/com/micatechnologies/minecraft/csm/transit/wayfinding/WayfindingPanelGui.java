package com.micatechnologies.minecraft.csm.transit.wayfinding;

import com.micatechnologies.minecraft.csm.transit.CsmTransit;
import com.micatechnologies.minecraft.csm.transit.wayfinding.WayfindingSign.Arrow;
import com.micatechnologies.minecraft.csm.transit.wayfinding.WayfindingSign.Pictogram;
import com.micatechnologies.minecraft.csm.transit.wayfinding.WayfindingSign.Preset;
import com.micatechnologies.minecraft.csm.transit.wayfinding.WayfindingSign.Scheme;
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
 * The editor for a large hanging sign: its two lines, buttons that step through the pictograms,
 * arrows and colour schemes, a toggle for the back, and a preset button that sets the whole sign
 * at once. Every change is sent at once, so the sign in the world updates while the screen is
 * open; Tab moves between the fields, Enter or Done closes it.
 *
 * @since 2026.10
 */
@SideOnly(Side.CLIENT)
public class WayfindingPanelGui extends GuiScreen {

  private static final int FIELD_WIDTH = 304;
  private static final int BUTTON_WIDTH = 150;
  private static final int ROW = 24;
  private static final int ID_DONE = 0;
  private static final int ID_PICTOGRAM = 1;
  private static final int ID_ARROW = 2;
  private static final int ID_SCHEME = 3;
  private static final int ID_DOUBLE = 4;
  private static final int ID_PRESET = 5;

  private final BlockPos cell;
  private final TileEntityWayfindingPanel controller;
  private final String title;

  private GuiTextField line1;
  private GuiTextField line2;
  private GuiButton pictogramButton;
  private GuiButton arrowButton;
  private GuiButton schemeButton;
  private GuiButton doubleButton;
  private GuiButton presetButton;

  private Pictogram pictogram;
  private Arrow arrow;
  private Scheme scheme;
  private boolean doubleSided;
  /** The preset last applied, or -1 before any. */
  private int preset = -1;

  /**
   * An editor for a panel.
   *
   * @param cell       the cell clicked, which edits name
   * @param controller the panel's controller, whose sign is shown
   * @param title      the block's name, shown at the top
   */
  public WayfindingPanelGui(BlockPos cell, TileEntityWayfindingPanel controller, String title) {
    this.cell = cell;
    this.controller = controller;
    this.title = title;
    this.pictogram = controller.getPictogram();
    this.arrow = controller.getArrow();
    this.scheme = controller.getScheme();
    this.doubleSided = controller.isDoubleSided();
  }

  private int top() {
    return height / 2 - 100;
  }

  @Override
  public void initGui() {
    buttonList.clear();
    Keyboard.enableRepeatEvents(true);
    int top = top();
    int x = width / 2 - FIELD_WIDTH / 2;
    String text1 = line1 != null ? line1.getText() : controller.getLine1();
    String text2 = line2 != null ? line2.getText() : controller.getLine2();
    line1 = new GuiTextField(10, fontRenderer, x, top + 40, FIELD_WIDTH, 20);
    line1.setMaxStringLength(WayfindingSign.MAX_LINE_LENGTH);
    line1.setText(text1);
    line1.setFocused(true);
    line2 = new GuiTextField(11, fontRenderer, x, top + 76, FIELD_WIDTH, 20);
    line2.setMaxStringLength(WayfindingSign.MAX_LINE_LENGTH);
    line2.setText(text2);
    int bx0 = width / 2 - BUTTON_WIDTH - 2;
    int bx1 = width / 2 + 2;
    int by = top + 104;
    pictogramButton = new GuiButton(ID_PICTOGRAM, bx0, by, BUTTON_WIDTH, 20, "");
    arrowButton = new GuiButton(ID_ARROW, bx1, by, BUTTON_WIDTH, 20, "");
    schemeButton = new GuiButton(ID_SCHEME, bx0, by + ROW, BUTTON_WIDTH, 20, "");
    doubleButton = new GuiButton(ID_DOUBLE, bx1, by + ROW, BUTTON_WIDTH, 20, "");
    presetButton = new GuiButton(ID_PRESET, bx0, by + 2 * ROW, BUTTON_WIDTH * 2 + 4, 20, "");
    buttonList.add(pictogramButton);
    buttonList.add(arrowButton);
    buttonList.add(schemeButton);
    buttonList.add(doubleButton);
    buttonList.add(presetButton);
    buttonList.add(new GuiButton(ID_DONE, width / 2 - 50, by + 3 * ROW + 6, 100, 20,
        I18n.format("gui.done")));
    labels();
  }

  private void labels() {
    pictogramButton.displayString = I18n.format("gui.csm.wayfinding.pictogram",
        I18n.format("gui.csm.wayfinding.pictogram." + pictogram.getId()));
    arrowButton.displayString = I18n.format("gui.csm.wayfinding.arrow",
        I18n.format("gui.csm.wayfinding.arrow." + arrow.getId()));
    schemeButton.displayString = I18n.format("gui.csm.wayfinding.scheme",
        I18n.format("gui.csm.wayfinding.scheme." + scheme.getId()));
    doubleButton.displayString = I18n.format("gui.csm.wayfinding.double",
        I18n.format(doubleSided ? "options.on" : "options.off"));
    presetButton.displayString = preset < 0 ? I18n.format("gui.csm.wayfinding.preset.none")
        : I18n.format("gui.csm.wayfinding.preset", Preset.values()[preset].getLabel());
  }

  @Override
  public void onGuiClosed() {
    Keyboard.enableRepeatEvents(false);
  }

  @Override
  protected void actionPerformed(GuiButton button) throws IOException {
    switch (button.id) {
      case ID_DONE:
        mc.displayGuiScreen(null);
        return;
      case ID_PICTOGRAM:
        pictogram = pictogram.next();
        break;
      case ID_ARROW:
        arrow = arrow.next();
        break;
      case ID_SCHEME:
        scheme = scheme.next();
        break;
      case ID_DOUBLE:
        doubleSided = !doubleSided;
        break;
      case ID_PRESET:
        preset = (preset + 1) % Preset.values().length;
        Preset p = Preset.values()[preset];
        line1.setText(p.getLine1());
        line2.setText(p.getLine2());
        pictogram = p.getPictogram();
        scheme = p.getScheme();
        if (p.getArrow() != null) {
          arrow = p.getArrow();
        }
        break;
      default:
        return;
    }
    labels();
    send();
  }

  @Override
  protected void keyTyped(char typedChar, int keyCode) throws IOException {
    if (keyCode == Keyboard.KEY_TAB) {
      boolean first = line1.isFocused();
      line1.setFocused(!first);
      line2.setFocused(first);
      return;
    }
    if (keyCode == Keyboard.KEY_RETURN || keyCode == Keyboard.KEY_NUMPADENTER) {
      mc.displayGuiScreen(null);
      return;
    }
    for (GuiTextField field : new GuiTextField[]{line1, line2}) {
      if (field.textboxKeyTyped(typedChar, keyCode)) {
        // The font has printable ASCII only, so anything else is taken out at once and the
        // player sees what the sign will carry. Spaces stay while typing; the server trims.
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
    line1.mouseClicked(mouseX, mouseY, mouseButton);
    line2.mouseClicked(mouseX, mouseY, mouseButton);
  }

  @Override
  public void updateScreen() {
    super.updateScreen();
    line1.updateCursorCounter();
    line2.updateCursorCounter();
  }

  private void send() {
    CsmTransit.NETWORK.sendToServer(new WayfindingPanelPacket(cell, line1.getText(),
        line2.getText(), pictogram, arrow, scheme, doubleSided));
  }

  @Override
  public void drawScreen(int mouseX, int mouseY, float partialTicks) {
    drawDefaultBackground();
    int top = top();
    int x = width / 2 - FIELD_WIDTH / 2;
    drawCenteredString(fontRenderer, title, width / 2, top, 0xFFFFFF);
    controller.refreshLayout();
    drawCenteredString(fontRenderer, I18n.format("gui.csm.wayfinding.size",
        controller.getWidth(), controller.getHeight()), width / 2, top + 13, 0xA0A0A0);
    drawString(fontRenderer, I18n.format("gui.csm.wayfinding.line1"), x, top + 29, 0xFFD070);
    line1.drawTextBox();
    drawString(fontRenderer, I18n.format("gui.csm.wayfinding.line2"), x, top + 65, 0xFFD070);
    line2.drawTextBox();
    super.drawScreen(mouseX, mouseY, partialTicks);
    drawCenteredString(fontRenderer, I18n.format("gui.csm.wayfinding.hint"), width / 2,
        top + 104 + 4 * ROW + 10, 0xA0A0A0);
  }

  @Override
  public boolean doesGuiPauseGame() {
    return false;
  }
}

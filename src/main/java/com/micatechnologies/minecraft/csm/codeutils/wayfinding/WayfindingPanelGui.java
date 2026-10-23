package com.micatechnologies.minecraft.csm.codeutils.wayfinding;

import com.micatechnologies.minecraft.csm.CsmNetwork;
import com.micatechnologies.minecraft.csm.codeutils.wayfinding.WayfindingSign.Align;
import com.micatechnologies.minecraft.csm.codeutils.wayfinding.WayfindingSign.IconStyle;
import java.io.IOException;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;

/**
 * The editor for a wayfinding panel, on two pages. <b>Text</b>: the four lines, each with its
 * text size (a click steps it down by 5% from 100% to 50% and round again, Shift steps it up) and
 * the icons left and right of it (a click opens the {@link IconPicker}), the line count and the
 * alignment. <b>Style</b>: the pictogram, arrow, colour scheme and custom colours ({@code #RRGGBB},
 * empty for the scheme's), icon style, mount, drop, panel scale, border, both sides, lit, the
 * legend's text size and the block's presets. Every change is sent at once
 * ({@link WayfindingPanelPacket}), so the sign in the world updates while the screen is open; Tab
 * moves between the fields, Enter or Done closes it.
 *
 * @since 2026.10
 */
@SideOnly(Side.CLIENT)
public class WayfindingPanelGui extends GuiScreen {

  private static final int ROW = 22;
  private static final int ID_DONE = 0;
  private static final int ID_PAGE_TEXT = 1;
  private static final int ID_PAGE_STYLE = 2;
  private static final int ID_LINE_COUNT = 3;
  private static final int ID_ALIGN = 4;
  private static final int ID_PICTOGRAM = 10;
  private static final int ID_ARROW = 11;
  private static final int ID_SCHEME = 12;
  private static final int ID_ICON_STYLE = 13;
  private static final int ID_MOUNT = 14;
  private static final int ID_DROP = 15;
  private static final int ID_SCALE = 16;
  private static final int ID_BORDER = 17;
  private static final int ID_DOUBLE = 18;
  private static final int ID_LIT = 19;
  private static final int ID_TEXT_SIZE = 20;
  private static final int ID_PRESET = 21;
  /** Each line's size, left icon and right icon buttons: base + line. */
  private static final int ID_LINE_SIZE = 30;
  private static final int ID_ICON_LEFT = 40;
  private static final int ID_ICON_RIGHT = 50;
  /** The text size steps, largest first. */
  private static final float[] TEXT_SIZES = {1.0f, 0.95f, 0.9f, 0.85f, 0.8f, 0.75f, 0.7f, 0.65f,
      0.6f, 0.55f, 0.5f};
  private static final float[] PANEL_SCALES = {0.5f, 0.75f, 1f, 1.25f, 1.5f, 2f};
  private static final int[] DROPS = {0, 2, 4, 6, 8, 12, 16};

  private final BlockPos cell;
  private final AbstractTileEntityWayfindingPanel controller;
  private final WayfindingPreset[] presets;
  private final String title;

  private WayfindingSign sign;
  private boolean stylePage;
  private int preset = -1;

  private final GuiTextField[] lines = new GuiTextField[WayfindingSign.MAX_LINES];
  private GuiTextField background;
  private GuiTextField legend;

  /**
   * An editor for a panel.
   *
   * @param cell       the cell clicked, which edits name
   * @param controller the panel's controller, whose sign is shown
   * @param presets    the block's presets
   * @param title      the block's name, shown at the top
   */
  public WayfindingPanelGui(BlockPos cell, AbstractTileEntityWayfindingPanel controller,
      WayfindingPreset[] presets, String title) {
    this.cell = cell;
    this.controller = controller;
    this.presets = presets;
    this.title = title;
    this.sign = controller.getSign();
  }

  /** The top of the screen's contents: 224 px tall, so it fits a 240 px high GUI. */
  private int top() {
    return Math.max(4, height / 2 - 112);
  }

  @Override
  public void initGui() {
    buttonList.clear();
    Keyboard.enableRepeatEvents(true);
    int top = top();
    int x0 = width / 2 - 160;
    buttonList.add(new GuiButton(ID_PAGE_TEXT, x0, top + 22, 104, 20,
        I18n.format("gui.csm.wayfinding.page.text")));
    buttonList.add(new GuiButton(ID_PAGE_STYLE, x0 + 108, top + 22, 104, 20,
        I18n.format("gui.csm.wayfinding.page.style")));
    for (GuiButton b : buttonList) {
      b.enabled = b.id == ID_PAGE_TEXT ? stylePage : !stylePage;
    }
    buttonList.add(new GuiButton(ID_DONE, x0 + 216, top + 22, 104, 20,
        I18n.format("gui.done")));
    int y = top + 58;
    if (!stylePage) {
      for (int i = 0; i < WayfindingSign.MAX_LINES; i++) {
        int ry = y + i * ROW;
        GuiTextField f = new GuiTextField(100 + i, fontRenderer, x0, ry, 200, 20);
        f.setMaxStringLength(WayfindingSign.MAX_LINE_LENGTH);
        f.setText(lines[i] != null ? lines[i].getText() : sign.getLine(i));
        f.setFocused(lines[i] != null ? lines[i].isFocused() : i == 0);
        lines[i] = f;
        buttonList.add(new GuiButton(ID_LINE_SIZE + i, x0 + 202, ry, 42, 20, ""));
        buttonList.add(new IconButton(ID_ICON_LEFT + i, x0 + 246, ry, true, i));
        buttonList.add(new IconButton(ID_ICON_RIGHT + i, x0 + 284, ry, false, i));
      }
      int by = y + 4 * ROW + 4;
      buttonList.add(new GuiButton(ID_LINE_COUNT, x0, by, 157, 20, ""));
      buttonList.add(new GuiButton(ID_ALIGN, x0 + 163, by, 157, 20, ""));
      background = null;
      legend = null;
    } else {
      int[] ids = {ID_PICTOGRAM, ID_ARROW, ID_SCHEME, ID_ICON_STYLE, ID_MOUNT, ID_DROP, ID_SCALE,
          ID_BORDER, ID_DOUBLE, ID_LIT, ID_TEXT_SIZE, ID_PRESET};
      for (int k = 0; k < ids.length; k++) {
        buttonList.add(new GuiButton(ids[k], x0 + (k % 2) * 163, y + (k / 2) * ROW, 157, 20,
            ""));
      }
      int fy = y + 6 * ROW + 2;
      background = new GuiTextField(110, fontRenderer, x0 + 70, fy, 87, 18);
      background.setMaxStringLength(9);
      background.setText(colourText(sign.getCustomBackground()));
      legend = new GuiTextField(111, fontRenderer, x0 + 233, fy, 87, 18);
      legend.setMaxStringLength(9);
      legend.setText(colourText(sign.getCustomLegend()));
    }
    labels();
  }

  private static String colourText(int rgb) {
    return rgb == WayfindingSign.NO_COLOUR ? "" : WayfindingSign.toHex(rgb);
  }

  private static String on(boolean b) {
    return I18n.format(b ? "options.on" : "options.off");
  }

  private static String pct(float f) {
    return Math.round(f * 100) + "%";
  }

  private void labels() {
    for (GuiButton b : buttonList) {
      String s = null;
      if (b.id >= ID_LINE_SIZE && b.id < ID_LINE_SIZE + WayfindingSign.MAX_LINES) {
        s = pct(sign.getLineScale(b.id - ID_LINE_SIZE));
      }
      switch (b.id) {
        case ID_LINE_COUNT:
          s = I18n.format("gui.csm.wayfinding.lines", sign.getLineCount() == 0
              ? I18n.format("gui.csm.wayfinding.lines.auto") : sign.getLineCount());
          break;
        case ID_ALIGN:
          s = I18n.format("gui.csm.wayfinding.align",
              I18n.format("gui.csm.wayfinding.align." + sign.getAlign().getId()));
          break;
        case ID_PICTOGRAM:
          s = I18n.format("gui.csm.wayfinding.pictogram", iconName(sign.getPictogram()));
          break;
        case ID_ARROW:
          s = I18n.format("gui.csm.wayfinding.arrow",
              I18n.format("gui.csm.wayfinding.arrow." + sign.getArrow().getId()));
          break;
        case ID_SCHEME:
          s = I18n.format("gui.csm.wayfinding.scheme",
              I18n.format("gui.csm.wayfinding.scheme." + sign.getScheme().getId()));
          break;
        case ID_ICON_STYLE:
          s = I18n.format("gui.csm.wayfinding.icon_style",
              I18n.format("gui.csm.wayfinding.icon_style." + sign.getIconStyle().getId()));
          break;
        case ID_MOUNT:
          s = I18n.format("gui.csm.wayfinding.mount",
              I18n.format("gui.csm.wayfinding.mount." + sign.getMount().getId()));
          break;
        case ID_DROP:
          s = I18n.format("gui.csm.wayfinding.drop", sign.getDrop());
          break;
        case ID_SCALE:
          s = I18n.format("gui.csm.wayfinding.panel_scale", pct(sign.getPanelScale()));
          break;
        case ID_BORDER:
          s = I18n.format("gui.csm.wayfinding.border",
              I18n.format("gui.csm.wayfinding.border." + sign.getBorder()));
          break;
        case ID_DOUBLE:
          s = I18n.format("gui.csm.wayfinding.double", on(sign.isDoubleSided()));
          break;
        case ID_LIT:
          s = I18n.format("gui.csm.wayfinding.lit", on(sign.isLit()));
          break;
        case ID_TEXT_SIZE:
          s = I18n.format("gui.csm.wayfinding.text_size", Math.round(sign.getTextScale() * 100));
          break;
        case ID_PRESET:
          s = preset < 0 ? I18n.format("gui.csm.wayfinding.preset.none")
              : I18n.format("gui.csm.wayfinding.preset", presets[preset].getLabel());
          break;
        default:
          break;
      }
      if (s != null) {
        b.displayString = s;
      }
    }
  }

  static String iconName(WayfindingIcon icon) {
    return I18n.format("gui.csm.wayfinding.icon." + icon.getId());
  }

  /**
   * The step after a value in a list of steps, or before it with {@code back}, round from one end
   * to the other; a value between steps (set by {@code /blockdata}) goes to the next one past it.
   */
  private static float step(float[] steps, float value, boolean back, boolean descending) {
    boolean up = back == descending;
    if (up) {
      for (float s : ascending(steps, descending)) {
        if (s > value + 1e-4f) {
          return s;
        }
      }
      return ascending(steps, descending)[0];
    }
    float[] asc = ascending(steps, descending);
    for (int i = asc.length - 1; i >= 0; i--) {
      if (asc[i] < value - 1e-4f) {
        return asc[i];
      }
    }
    return asc[asc.length - 1];
  }

  private static float[] ascending(float[] steps, boolean descending) {
    if (!descending) {
      return steps;
    }
    float[] out = new float[steps.length];
    for (int i = 0; i < steps.length; i++) {
      out[i] = steps[steps.length - 1 - i];
    }
    return out;
  }

  @Override
  public void onGuiClosed() {
    Keyboard.enableRepeatEvents(false);
  }

  @Override
  protected void actionPerformed(GuiButton button) throws IOException {
    WayfindingSign.Builder b = sign.toBuilder();
    boolean shift = isShiftKeyDown();
    int id = button.id;
    if (id >= ID_LINE_SIZE && id < ID_LINE_SIZE + WayfindingSign.MAX_LINES) {
      int i = id - ID_LINE_SIZE;
      b.lineScale(i, step(TEXT_SIZES, sign.getLineScale(i), shift, true));
    } else if (id >= ID_ICON_LEFT && id < ID_ICON_RIGHT + WayfindingSign.MAX_LINES) {
      int i = id >= ID_ICON_RIGHT ? id - ID_ICON_RIGHT : id - ID_ICON_LEFT;
      boolean leftSide = id < ID_ICON_RIGHT;
      WayfindingIcon now = leftSide ? sign.getIconLeft(i) : sign.getIconRight(i);
      mc.displayGuiScreen(new IconPicker(this, now, picked -> {
        WayfindingSign.Builder nb = sign.toBuilder();
        if (leftSide) {
          nb.iconLeft(i, picked);
        } else {
          nb.iconRight(i, picked);
        }
        sign = nb.build();
        send();
      }));
      return;
    } else {
      switch (id) {
        case ID_DONE:
          mc.displayGuiScreen(null);
          return;
        case ID_PAGE_TEXT:
        case ID_PAGE_STYLE:
          stylePage = id == ID_PAGE_STYLE;
          initGui();
          return;
        case ID_LINE_COUNT:
          b.lineCount((sign.getLineCount() + (shift ? WayfindingSign.MAX_LINES : 1))
              % (WayfindingSign.MAX_LINES + 1));
          break;
        case ID_ALIGN:
          b.align(sign.getAlign().next());
          break;
        case ID_PICTOGRAM:
          mc.displayGuiScreen(new IconPicker(this, sign.getPictogram(), picked -> {
            sign = sign.toBuilder().pictogram(picked).build();
            send();
          }));
          return;
        case ID_ARROW:
          b.arrow(sign.getArrow().next());
          break;
        case ID_SCHEME:
          b.scheme(sign.getScheme().next());
          break;
        case ID_ICON_STYLE:
          b.iconStyle(sign.getIconStyle().next());
          break;
        case ID_MOUNT:
          b.mount(sign.getMount().next());
          break;
        case ID_DROP: {
          float[] drops = new float[DROPS.length];
          for (int k = 0; k < DROPS.length; k++) {
            drops[k] = DROPS[k];
          }
          b.drop(Math.round(step(drops, sign.getDrop(), shift, false)));
          break;
        }
        case ID_SCALE:
          b.panelScale(step(PANEL_SCALES, sign.getPanelScale(), shift, false));
          break;
        case ID_BORDER:
          b.border((sign.getBorder() + 1) % (WayfindingSign.MAX_BORDER + 1));
          break;
        case ID_DOUBLE:
          b.doubleSided(!sign.isDoubleSided());
          break;
        case ID_LIT:
          b.lit(!sign.isLit());
          break;
        case ID_TEXT_SIZE:
          b.textScale(step(TEXT_SIZES, sign.getTextScale(), shift, true));
          break;
        case ID_PRESET:
          if (presets.length == 0) {
            return;
          }
          preset = (preset + 1) % presets.length;
          sign = presets[preset].applyTo(sign);
          send();
          initGui();
          return;
        default:
          return;
      }
    }
    sign = b.build();
    labels();
    send();
  }

  private GuiTextField[] fields() {
    return stylePage ? new GuiTextField[]{background, legend} : lines;
  }

  @Override
  protected void keyTyped(char typedChar, int keyCode) throws IOException {
    GuiTextField[] fields = fields();
    if (keyCode == Keyboard.KEY_TAB) {
      int focused = -1;
      for (int i = 0; i < fields.length; i++) {
        if (fields[i].isFocused()) {
          focused = i;
        }
        fields[i].setFocused(false);
      }
      fields[(focused + 1) % fields.length].setFocused(true);
      return;
    }
    if (keyCode == Keyboard.KEY_RETURN || keyCode == Keyboard.KEY_NUMPADENTER) {
      mc.displayGuiScreen(null);
      return;
    }
    for (int i = 0; i < fields.length; i++) {
      GuiTextField field = fields[i];
      if (field.textboxKeyTyped(typedChar, keyCode)) {
        // The font has printable ASCII only, so anything else is taken out at once and the
        // player sees what the sign will carry. Spaces stay while typing; the server trims.
        String text = field.getText();
        StringBuilder cleaned = new StringBuilder(text.length());
        for (int k = 0; k < text.length(); k++) {
          char c = text.charAt(k);
          if (c >= 32 && c < 127) {
            cleaned.append(c);
          }
        }
        if (cleaned.length() != text.length()) {
          field.setText(cleaned.toString());
        }
        WayfindingSign.Builder b = sign.toBuilder();
        if (stylePage) {
          // a colour applies once it parses; an empty field goes back to the scheme's
          int rgb = WayfindingSign.parseColour(field.getText());
          if (rgb == WayfindingSign.NO_COLOUR && !field.getText().trim().isEmpty()) {
            return;
          }
          if (i == 0) {
            b.background(rgb);
          } else {
            b.legend(rgb);
          }
        } else {
          b.line(i, field.getText());
        }
        sign = b.build();
        send();
        return;
      }
    }
    super.keyTyped(typedChar, keyCode);
  }

  @Override
  protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
    super.mouseClicked(mouseX, mouseY, mouseButton);
    for (GuiTextField f : fields()) {
      f.mouseClicked(mouseX, mouseY, mouseButton);
    }
  }

  @Override
  public void updateScreen() {
    super.updateScreen();
    for (GuiTextField f : fields()) {
      f.updateCursorCounter();
    }
  }

  private void send() {
    CsmNetwork.CORE.sendToServer(new WayfindingPanelPacket(cell, sign));
  }

  @Override
  public void drawScreen(int mouseX, int mouseY, float partialTicks) {
    drawDefaultBackground();
    int top = top();
    int x0 = width / 2 - 160;
    drawCenteredString(fontRenderer, title, width / 2, top, 0xFFFFFF);
    drawCenteredString(fontRenderer, I18n.format("gui.csm.wayfinding.size",
        controller.getPanelWidth(), controller.getPanelHeight()), width / 2, top + 11, 0xA0A0A0);
    if (!stylePage) {
      drawString(fontRenderer, I18n.format("gui.csm.wayfinding.lines_head"), x0, top + 47,
          0xFFD070);
      drawString(fontRenderer, I18n.format("gui.csm.wayfinding.icons_head"), x0 + 246, top + 47,
          0xFFD070);
      for (GuiTextField f : lines) {
        f.drawTextBox();
      }
    } else {
      int fy = top + 58 + 6 * ROW + 2;
      drawString(fontRenderer, I18n.format("gui.csm.wayfinding.background"), x0, fy + 5,
          0xFFD070);
      drawString(fontRenderer, I18n.format("gui.csm.wayfinding.legend"), x0 + 163, fy + 5,
          0xFFD070);
      background.drawTextBox();
      legend.drawTextBox();
      drawCenteredString(fontRenderer, I18n.format("gui.csm.wayfinding.colour_hint"),
          width / 2, fy + 22, 0xA0A0A0);
    }
    super.drawScreen(mouseX, mouseY, partialTicks);
    if (!stylePage) {
      drawCenteredString(fontRenderer, I18n.format("gui.csm.wayfinding.hint"), width / 2,
          top + 184, 0xA0A0A0);
    }
  }

  @Override
  public boolean doesGuiPauseGame() {
    return false;
  }

  // --- icons ---------------------------------------------------------------------------------

  /**
   * Draws an icon in white in a square on the screen, from the block atlas: a sheet icon's mask,
   * or the arrow turned.
   */
  static void drawIcon(Minecraft mc, WayfindingIcon icon, int x, int y, int size) {
    int[] cell;
    int angle = 0;
    if (icon.isArrow()) {
      angle = icon.getArrow().getAngle();
      boolean diagonal = angle % 90 != 0;
      cell = diagonal ? WayfindingSheet.ARROW_DIAGONAL : WayfindingSheet.ARROW_STRAIGHT;
      angle -= diagonal ? 45 : 0;
    } else {
      cell = WayfindingSheet.icon(icon.getId());
    }
    if (cell == null) {
      return;
    }
    TextureAtlasSprite sprite = mc.getTextureMapBlocks()
        .getAtlasSprite(WayfindingSheet.ICON_SPRITE);
    float k = 16f / WayfindingSheet.SHEET;
    float u0 = sprite.getInterpolatedU(cell[0] * k);
    float v0 = sprite.getInterpolatedV(cell[1] * k);
    float u1 = sprite.getInterpolatedU((cell[0] + WayfindingSheet.ICON) * k);
    float v1 = sprite.getInterpolatedV((cell[1] + WayfindingSheet.ICON) * k);
    mc.getTextureManager().bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);
    GlStateManager.enableBlend();
    GlStateManager.color(1f, 1f, 1f, 1f);
    GlStateManager.pushMatrix();
    GlStateManager.translate(x + size / 2f, y + size / 2f, 0);
    // screen y runs down, so anticlockwise as read is a negative turn here
    GlStateManager.rotate(-angle, 0, 0, 1);
    float h = size / 2f;
    Tessellator tess = Tessellator.getInstance();
    BufferBuilder buf = tess.getBuffer();
    buf.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX);
    buf.pos(-h, h, 0).tex(u0, v1).endVertex();
    buf.pos(h, h, 0).tex(u1, v1).endVertex();
    buf.pos(h, -h, 0).tex(u1, v0).endVertex();
    buf.pos(-h, -h, 0).tex(u0, v0).endVertex();
    tess.draw();
    GlStateManager.popMatrix();
  }

  /** A line's left or right icon: its icon drawn on the button, or "L" / "R" for none. */
  private final class IconButton extends GuiButton {

    private final boolean leftSide;
    private final int line;

    IconButton(int id, int x, int y, boolean leftSide, int line) {
      super(id, x, y, 36, 20, "");
      this.leftSide = leftSide;
      this.line = line;
    }

    @Override
    public void drawButton(Minecraft mc, int mouseX, int mouseY, float partialTicks) {
      WayfindingIcon icon = leftSide ? sign.getIconLeft(line) : sign.getIconRight(line);
      displayString = icon == WayfindingIcon.NONE ? (leftSide ? "L -" : "- R") : "";
      super.drawButton(mc, mouseX, mouseY, partialTicks);
      if (visible && icon != WayfindingIcon.NONE) {
        drawIcon(mc, icon, x + 10, y + 2, 16);
      }
    }
  }

  /**
   * Picks an icon from a grid of all of them, each drawn; the one in use is outlined. A click
   * picks and goes back to the editor; Escape goes back with nothing changed.
   */
  static final class IconPicker extends GuiScreen {

    private static final int CELL = 24;
    private static final int COLUMNS = 12;

    private final GuiScreen parent;
    private final WayfindingIcon current;
    private final Consumer<WayfindingIcon> picked;

    IconPicker(GuiScreen parent, WayfindingIcon current, Consumer<WayfindingIcon> picked) {
      this.parent = parent;
      this.current = current;
      this.picked = picked;
    }

    private int left() {
      return width / 2 - COLUMNS * CELL / 2;
    }

    private int topEdge() {
      int rows = (WayfindingIcon.values().length + COLUMNS - 1) / COLUMNS;
      return height / 2 - rows * CELL / 2;
    }

    private WayfindingIcon at(int mouseX, int mouseY) {
      int c = Math.floorDiv(mouseX - left(), CELL);
      int r = Math.floorDiv(mouseY - topEdge(), CELL);
      int i = r * COLUMNS + c;
      if (c < 0 || c >= COLUMNS || r < 0 || i >= WayfindingIcon.values().length) {
        return null;
      }
      return WayfindingIcon.values()[i];
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
      drawDefaultBackground();
      WayfindingIcon[] all = WayfindingIcon.values();
      WayfindingIcon hover = at(mouseX, mouseY);
      drawCenteredString(fontRenderer, I18n.format("gui.csm.wayfinding.pick"), width / 2,
          topEdge() - 22, 0xFFFFFF);
      for (int i = 0; i < all.length; i++) {
        int x = left() + (i % COLUMNS) * CELL;
        int y = topEdge() + (i / COLUMNS) * CELL;
        int fill = all[i] == hover ? 0xFF505868 : 0xFF202428;
        if (all[i] == current) {
          drawRect(x, y, x + CELL - 2, y + CELL - 2, 0xFFFFD070);
          drawRect(x + 1, y + 1, x + CELL - 3, y + CELL - 3, fill);
        } else {
          drawRect(x, y, x + CELL - 2, y + CELL - 2, fill);
        }
        if (all[i] == WayfindingIcon.NONE) {
          drawCenteredString(fontRenderer, "-", x + CELL / 2 - 1, y + 7, 0xA0A0A0);
        } else {
          drawIcon(mc, all[i], x + 3, y + 3, CELL - 8);
        }
      }
      if (hover != null) {
        drawCenteredString(fontRenderer, iconName(hover), width / 2,
            topEdge() + ((all.length + COLUMNS - 1) / COLUMNS) * CELL + 6, 0xFFD070);
      }
      super.drawScreen(mouseX, mouseY, partialTicks);
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
      WayfindingIcon icon = at(mouseX, mouseY);
      if (icon != null && mouseButton == 0) {
        picked.accept(icon);
        mc.displayGuiScreen(parent);
        return;
      }
      super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
      if (keyCode == Keyboard.KEY_ESCAPE) {
        mc.displayGuiScreen(parent);
        return;
      }
      super.keyTyped(typedChar, keyCode);
    }

    @Override
    public boolean doesGuiPauseGame() {
      return false;
    }
  }
}

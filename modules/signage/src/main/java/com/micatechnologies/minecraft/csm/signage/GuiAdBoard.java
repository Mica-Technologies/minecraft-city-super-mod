package com.micatechnologies.minecraft.csm.signage;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.client.config.GuiSlider;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.opengl.GL11;

/**
 * An advertising board's screen: its size and which way it grows on the left, what it shows on
 * the right, with the ad drawn as the board would show it. While the screen is open the board's
 * new outline is drawn in the world ({@link AdBoardPreview}); Done sends it all to the server,
 * which builds the board to the new size if nothing is in the way.
 */
@SideOnly(Side.CLIENT)
public class GuiAdBoard extends GuiScreen implements GuiSlider.ISlider {

  private static final int BTN_PRESET = 0;
  private static final int SLD_WIDTH = 1;
  private static final int SLD_HEIGHT = 2;
  private static final int BTN_ALIGN = 3;
  private static final int BTN_FIT = 4;
  private static final int BTN_LIGHT = 5;
  private static final int BTN_AD_PREV = 6;
  private static final int BTN_AD = 7;
  private static final int BTN_AD_NEXT = 8;
  private static final int BTN_ROTATION = 9;
  private static final int BTN_CATEGORY = 10;
  private static final int SLD_INTERVAL = 11;
  private static final int BTN_DONE = 12;
  private static final int BTN_CANCEL = 13;
  private static final int BTN_BACK = 14;

  private static final int COL = 160;
  private static final int GAP = 10;
  private static final int H = 20;
  private static final int ROW = 22;
  private static final int PREVIEW_H = 76;

  private final TileEntityAdBoard te;
  private final BlockPos clicked;
  private final BlockPos controller;
  private final AdBoardKind kind;
  private final EnumFacing facing;
  private final List<AdEntry> ads;
  private final List<String> categories;

  private int preset = -1;
  /** Set while a preset moves the sliders, so their callback does not forget the preset. */
  private boolean applyingPreset;
  private AdBoardAlign align;
  private int adIndex;
  private AdRotation rotation;
  private int categoryIndex;
  private AdFit fit;
  private AdLight light;
  private AdBack back;

  private GuiSlider widthSlider;
  private GuiSlider heightSlider;
  private GuiSlider interval;
  private GuiButton presetButton;
  private GuiButton alignButton;
  private GuiButton fitButton;
  private GuiButton lightButton;
  private GuiButton backButton;
  private GuiButton adButton;
  private GuiButton rotationButton;
  private GuiButton categoryButton;
  private GuiButton adPrev;
  private GuiButton adNext;

  private int previewX;
  private int previewY;

  public GuiAdBoard(TileEntityAdBoard te, BlockPos clicked) {
    this.te = te;
    this.clicked = clicked;
    this.controller = te.getPos();
    IBlockState state = te.getWorld().getBlockState(controller);
    this.kind = ((AbstractBlockAdBoard) state.getBlock()).kind();
    this.facing = state.getValue(AbstractBlockAdBoard.FACING);
    AdLibrary library = AdLibrary.get();
    this.ads = library.all();
    this.categories = library.categories();
    this.align = te.getAlign();
    this.rotation = te.getRotation();
    this.fit = te.getFit();
    this.light = te.getLight();
    this.back = te.getBack();
    AdEntry current = library.resolve(te.getAdId());
    this.adIndex = Math.max(0, ads.indexOf(current));
    this.categoryIndex = Math.max(0, categories.indexOf(te.getCategory()));
  }

  @Override
  public void initGui() {
    buttonList.clear();
    int left = width / 2 - COL - GAP / 2;
    int right = width / 2 + GAP / 2;
    int top = Math.max(22, height / 2 - (PREVIEW_H + 4 + ROW * 4 + ROW + 12) / 2);

    // Left column: the board.
    int y = top;
    presetButton = add(new GuiButton(BTN_PRESET, left, y, COL, H, presetLabel()));
    y += ROW;
    int w = te.getWidth() == 1 && te.getHeight() == 1 ? defaultSize()[0] : te.getWidth();
    int h = te.getWidth() == 1 && te.getHeight() == 1 ? defaultSize()[1] : te.getHeight();
    widthSlider = add(new GuiSlider(SLD_WIDTH, left, y, COL, H,
        I18n.format("gui.csm.adboard.width") + ": ", "", 1, kind.getMaxWidth(), w, false, true,
        this));
    y += ROW;
    heightSlider = add(new GuiSlider(SLD_HEIGHT, left, y, COL, H,
        I18n.format("gui.csm.adboard.height") + ": ", "", kind.getMinHeight(),
        kind.getMaxHeight(), Math.max(h, kind.getMinHeight()), false, true, this));
    y += ROW;
    alignButton = add(new GuiButton(BTN_ALIGN, left, y, COL, H, ""));
    y += ROW;
    fitButton = add(new GuiButton(BTN_FIT, left, y, COL, H, ""));
    y += ROW;
    lightButton = add(new GuiButton(BTN_LIGHT, left, y, COL, H, ""));
    y += ROW;
    backButton = add(new GuiButton(BTN_BACK, left, y, COL, H, ""));
    backButton.visible = kind.isCabinet();

    // Right column: the ads.
    previewX = right;
    previewY = top;
    y = top + PREVIEW_H + 4;
    adPrev = add(new GuiButton(BTN_AD_PREV, right, y, 20, H, "<"));
    adButton = add(new GuiButton(BTN_AD, right + 22, y, COL - 44, H, ""));
    adNext = add(new GuiButton(BTN_AD_NEXT, right + COL - 20, y, 20, H, ">"));
    y += ROW;
    rotationButton = add(new GuiButton(BTN_ROTATION, right, y, COL, H, ""));
    y += ROW;
    categoryButton = add(new GuiButton(BTN_CATEGORY, right, y, COL, H, ""));
    y += ROW;
    interval = add(new GuiSlider(SLD_INTERVAL, right, y, COL, H,
        I18n.format("gui.csm.adboard.interval") + ": ", " s", AdRotation.MIN_INTERVAL,
        AdRotation.MAX_INTERVAL, te.getInterval(), false, true, this));

    y += ROW + 12;
    add(new GuiButton(BTN_DONE, width / 2 - 102, y, 100, H, I18n.format("gui.done")));
    add(new GuiButton(BTN_CANCEL, width / 2 + 2, y, 100, H, I18n.format("gui.cancel")));
    refresh();
  }

  private <T extends GuiButton> T add(T button) {
    buttonList.add(button);
    return button;
  }

  /** A new board's screen offers a sensible size rather than one block. */
  private int[] defaultSize() {
    int[][] presets = kind.getPresets();
    // A wall poster starts at 8 x 4; a billboard at the 14 x 48 ft bulletin, 15 x 4.
    return presets[Math.min(kind.isCabinet() ? 2 : 3, presets.length - 1)];
  }

  private String presetLabel() {
    if (preset < 0) {
      return I18n.format("gui.csm.adboard.preset");
    }
    int[] p = kind.getPresets()[preset];
    return I18n.format("gui.csm.adboard.preset") + ": " + p[0] + " x " + p[1];
  }

  private void refresh() {
    presetButton.displayString = presetLabel();
    alignButton.displayString = I18n.format("gui.csm.adboard.align") + ": "
        + I18n.format("gui.csm.adboard.align." + align.name().toLowerCase(Locale.ROOT));
    fitButton.displayString = I18n.format("gui.csm.adboard.fit") + ": "
        + I18n.format("gui.csm.adboard.fit." + fit.name().toLowerCase(Locale.ROOT));
    lightButton.displayString = I18n.format("gui.csm.adboard.light") + ": "
        + I18n.format("gui.csm.adboard.light." + light.name().toLowerCase(Locale.ROOT));
    backButton.displayString = I18n.format("gui.csm.adboard.back") + ": "
        + I18n.format("gui.csm.adboard.back." + back.name().toLowerCase(Locale.ROOT));
    rotationButton.displayString = I18n.format("gui.csm.adboard.rotation."
        + rotation.name().toLowerCase(Locale.ROOT));
    boolean single = rotation == AdRotation.SINGLE;
    adButton.displayString = fitText(ads.get(adIndex).getBrand(), adButton.width - 8);
    adPrev.enabled = single;
    adNext.enabled = single;
    adButton.enabled = single;
    categoryButton.enabled = rotation.usesCategory() && !categories.isEmpty();
    categoryButton.displayString = I18n.format("gui.csm.adboard.category") + ": "
        + (categories.isEmpty() ? "-" : I18n.format(
            "gui.csm.adboard.category." + categories.get(categoryIndex)));
    interval.enabled = !single;
    AdBoardPreview.show(controller, facing, align.controllerColumn(widthSlider.getValueInt()),
        widthSlider.getValueInt(), heightSlider.getValueInt());
  }

  private String fitText(String text, int pixels) {
    if (fontRenderer.getStringWidth(text) <= pixels) {
      return text;
    }
    return fontRenderer.trimStringToWidth(text, pixels - fontRenderer.getStringWidth("...")) + "...";
  }

  @Override
  protected void actionPerformed(GuiButton button) throws IOException {
    switch (button.id) {
      case BTN_PRESET:
        preset = (preset + 1) % kind.getPresets().length;
        int[] p = kind.getPresets()[preset];
        applyingPreset = true;
        widthSlider.setValue(p[0]);
        widthSlider.updateSlider();
        heightSlider.setValue(p[1]);
        heightSlider.updateSlider();
        applyingPreset = false;
        break;
      case BTN_ALIGN:
        align = align.next();
        break;
      case BTN_FIT:
        fit = AdFit.values()[(fit.ordinal() + 1) % AdFit.values().length];
        break;
      case BTN_LIGHT:
        light = light.next();
        break;
      case BTN_BACK:
        back = back.next();
        break;
      case BTN_AD_PREV:
        adIndex = (adIndex + ads.size() - 1) % ads.size();
        break;
      case BTN_AD:
      case BTN_AD_NEXT:
        adIndex = (adIndex + 1) % ads.size();
        break;
      case BTN_ROTATION:
        rotation = rotation.next();
        break;
      case BTN_CATEGORY:
        categoryIndex = (categoryIndex + 1) % Math.max(1, categories.size());
        break;
      case BTN_DONE:
        send();
        mc.displayGuiScreen(null);
        return;
      case BTN_CANCEL:
        mc.displayGuiScreen(null);
        return;
      default:
        break;
    }
    refresh();
  }

  private void send() {
    String category = categories.isEmpty() ? "" : categories.get(categoryIndex);
    CsmSignage.NETWORK.sendToServer(new AdBoardConfigPacket(clicked, widthSlider.getValueInt(),
        heightSlider.getValueInt(), align, ads.get(adIndex).getId(), rotation, category,
        interval.getValueInt(), fit, light, back));
  }

  @Override
  public void onChangeSliderValue(GuiSlider slider) {
    if (!applyingPreset && (slider.id == SLD_WIDTH || slider.id == SLD_HEIGHT)) {
      preset = -1;
    }
    if (interval != null) {
      // Not while initGui is still making the buttons refresh() sets.
      refresh();
    }
  }

  @Override
  public void onGuiClosed() {
    AdBoardPreview.hide();
  }

  @Override
  public boolean doesGuiPauseGame() {
    return false;
  }

  @Override
  public void drawScreen(int mouseX, int mouseY, float partialTicks) {
    // A light panel rather than the full dark background, so the board's outline shows.
    int left = width / 2 - COL - GAP / 2 - 6;
    int right = width / 2 + COL + GAP / 2 + 6;
    Gui.drawRect(left, previewY - 18, right, interval.y + ROW + 12 + H + 6, 0xB0101010);
    drawCenteredString(fontRenderer, I18n.format("tile." + kind.getRegistryName() + ".name"),
        width / 2, previewY - 13, 0xFFFFFF);
    drawPreview();
    super.drawScreen(mouseX, mouseY, partialTicks);
  }

  /** The ad in the preview box, fitted to the requested board as the board would fit it. */
  private void drawPreview() {
    AdEntry ad = rotation == AdRotation.SINGLE ? ads.get(adIndex)
        : previewPool().get(rotation.select(previewPool(), te.getWorld().getTotalWorldTime(),
            interval.getValueInt(), TileEntityAdBoard.seed(controller)));
    double faceW = widthSlider.getValueInt() - 2 * kind.getFramePx() / 16.0;
    double faceH = heightSlider.getValueInt() - kind.getServiceRows()
        - 2 * kind.getFramePx() / 16.0;
    double aspect = faceW / faceH;
    int boxW = COL;
    int boxH = PREVIEW_H;
    int pw = boxW;
    int ph = (int) Math.round(boxW / aspect);
    if (ph > boxH) {
      ph = boxH;
      pw = (int) Math.round(boxH * aspect);
    }
    int px = previewX + (boxW - pw) / 2;
    int py = previewY + (boxH - ph) / 2;
    Gui.drawRect(px - 1, py - 1, px + pw + 1, py + ph + 1, 0xFFB8BEC6);
    AdShape shape = ad.shapeFor(faceW, faceH);
    double[] place = fit.place(aspect, shape.getAspect());
    if (fit == AdFit.CONTAIN) {
      Gui.drawRect(px, py, px + pw, py + ph, 0xFF000000 | ad.getBackground());
    }
    AdTextures.bind(ad.texture(shape));
    GlStateManager.color(1F, 1F, 1F, 1F);
    double x0 = px + place[0] * pw;
    double x1 = px + place[2] * pw;
    double y0 = py + place[1] * ph;
    double y1 = py + place[3] * ph;
    Tessellator tessellator = Tessellator.getInstance();
    BufferBuilder buf = tessellator.getBuffer();
    buf.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX);
    buf.pos(x0, y1, zLevel).tex(place[4], place[7]).endVertex();
    buf.pos(x1, y1, zLevel).tex(place[6], place[7]).endVertex();
    buf.pos(x1, y0, zLevel).tex(place[6], place[5]).endVertex();
    buf.pos(x0, y0, zLevel).tex(place[4], place[5]).endVertex();
    tessellator.draw();
  }

  private List<AdEntry> previewPool() {
    AdLibrary library = AdLibrary.get();
    List<AdEntry> pool = rotation.usesCategory() && !categories.isEmpty()
        ? library.inCategory(categories.get(categoryIndex)) : library.rotation();
    return pool.isEmpty() ? Collections.singletonList(
        library.resolve(AdLibrary.HOUSE_AD)) : pool;
  }
}

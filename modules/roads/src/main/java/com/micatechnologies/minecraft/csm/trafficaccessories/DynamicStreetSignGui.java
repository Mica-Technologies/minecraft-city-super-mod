package com.micatechnologies.minecraft.csm.trafficaccessories;

import com.micatechnologies.minecraft.csm.trafficaccessories.packets.DynamicStreetSignUpdatePacket;
import com.micatechnologies.minecraft.csm.roads.CsmRoads;
import com.micatechnologies.minecraft.csm.trafficaccessories.streetsign.StreetSignData;
import com.micatechnologies.minecraft.csm.trafficaccessories.streetsign.StreetSignEmblemKind;
import com.micatechnologies.minecraft.csm.trafficaccessories.streetsign.StreetSignTemplates;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.ParametersAreNonnullByDefault;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.text.TextFormatting;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

/**
 * The editing screen for a dynamic street sign. Three tabs -- Text, Style, Preview -- sharing
 * the dynamic guide sign GUI's scrolling-viewport pattern: content between the tab strip and
 * the Save/Cancel row scrolls with the wheel, and off-viewport widgets are hidden rather than
 * moved so they cannot intercept clicks or bleed through the fixed strips.
 *
 * <p>Save sends the whole document in one {@link DynamicStreetSignUpdatePacket}; Cancel closes
 * without sending, so edits are discardable.
 */
public class DynamicStreetSignGui extends GuiScreen {

  private static final int TAB_TEXT = 0;
  private static final int TAB_STYLE = 1;
  private static final int TAB_PREVIEW = 2;

  private static final int BTN_TAB_TEXT = 100;
  private static final int BTN_TAB_STYLE = 101;
  private static final int BTN_TAB_PREVIEW = 102;
  private static final int BTN_SAVE = 200;
  private static final int BTN_CANCEL = 201;

  // Text tab
  private static final int BTN_TEXT_SCALE_DOWN = 10;
  private static final int BTN_TEXT_SCALE_UP = 11;
  private static final int BTN_BLOCK_POSITION = 12;
  private static final int BTN_BLOCK_VERTICAL = 13;
  private static final int BTN_EMBLEM_KIND = 14;
  private static final int BTN_EMBLEM_POSITION = 15;
  private static final int BTN_SHIELD_TYPE_PREV = 16;
  private static final int BTN_SHIELD_TYPE_NEXT = 17;
  private static final int BTN_LOGO_TYPE_PREV = 18;
  private static final int BTN_LOGO_TYPE_NEXT = 19;
  private static final int BTN_ARROW_POSITION = 20;
  private static final int BTN_ARROW_TYPE = 21;
  private static final int BTN_AFFIX_VERTICAL = 22;

  // Style tab
  private static final int BTN_SIGN_COLOR = 30;
  private static final int BTN_MOUNT_TYPE = 31;
  private static final int BTN_CORNER_STYLE = 32;
  private static final int BTN_DOUBLE_SIDED = 33;
  private static final int BTN_EXTRUDED_FRAME = 34;
  private static final int BTN_BORDER_DOWN = 35;
  private static final int BTN_BORDER_UP = 36;
  private static final int BTN_MIN_WIDTH_DOWN = 37;
  private static final int BTN_MIN_WIDTH_UP = 38;
  private static final int BTN_MIN_HEIGHT_DOWN = 39;
  private static final int BTN_MIN_HEIGHT_UP = 40;
  private static final int BTN_INTERNAL_LIGHT = 41;
  private static final int BTN_LIGHT_MODE = 42;
  private static final int BTN_TEMPLATE = 43;
  private static final int BTN_COPY = 44;
  private static final int BTN_PASTE = 45;

  private static final int FIELD_WIDTH = 240;
  private static final int BTN_HEIGHT = 18;
  private static final int SCROLL_STEP = 12;
  private static final int SCROLLBAR_WIDTH = 4;
  private static final int PREVIEW_VISUAL_HEIGHT = 100;
  private static final int PREVIEW_LINE_HEIGHT = 11;
  private static final int MIN_SIZE_STEP = 8;

  private final TileEntityDynamicStreetSign tileEntity;
  private StreetSignData data;
  private int currentTab = TAB_TEXT;

  private int tabContentScroll = 0;
  private int tabContentMaxScroll = 0;
  private int viewportTop = 0;
  private int viewportBottom = 0;

  private final List<GuiButton> contentButtons = new ArrayList<>();
  private final List<Integer> contentButtonNaturalY = new ArrayList<>();
  private final List<GuiTextField> contentTextFields = new ArrayList<>();
  private final List<Integer> contentTextFieldNaturalY = new ArrayList<>();
  private int customContentBottom = 0;

  // Y of each Text-tab label row, recorded while the tab is built. Recomputing these from
  // the same increments in the draw pass meant every inserted row silently desynced the
  // labels from the widgets they describe.
  private int labelYAffixRow;
  private int labelYCityRow;
  private int labelYTextScale;
  private int labelYBlockRow;
  private int labelYShieldRow;

  private GuiTextField prefixField;
  private GuiTextField nameField;
  private GuiTextField suffixField;
  private GuiTextField cityField;
  private GuiTextField blockField;
  private GuiTextField routeField;

  // Process-wide clipboard, so a player can duplicate a blade across blocks in a session.
  private static String clipboardJson = null;
  // Static so the next template in the cycle survives closing and reopening the screen.
  private static int templateIndex = 0;

  private final List<String> previewLines = new ArrayList<>();
  private final TileEntityDynamicStreetSignRenderer previewRenderer =
      new TileEntityDynamicStreetSignRenderer();

  public DynamicStreetSignGui(TileEntityDynamicStreetSign tileEntity) {
    this.tileEntity = tileEntity;
    this.data = tileEntity.getSignData().copy();
  }

  @Override
  public void initGui() {
    Keyboard.enableRepeatEvents(true);
    super.initGui();
    buttonList.clear();
    contentButtons.clear();
    contentButtonNaturalY.clear();
    contentTextFields.clear();
    contentTextFieldNaturalY.clear();
    prefixField = null;
    nameField = null;
    suffixField = null;
    cityField = null;
    blockField = null;
    routeField = null;

    ScaledResolution sr = new ScaledResolution(this.mc);
    int centerX = sr.getScaledWidth() / 2;
    int left = centerX - FIELD_WIDTH / 2;
    int tabWidth = FIELD_WIDTH / 3;
    int startY = 25;
    int bottomY = sr.getScaledHeight() - 30;

    viewportTop = startY + BTN_HEIGHT + 6;
    viewportBottom = bottomY - 4;
    customContentBottom = 0;

    buttonList.add(new GuiButton(BTN_TAB_TEXT, left, startY, tabWidth, BTN_HEIGHT, "Text"));
    buttonList.add(new GuiButton(BTN_TAB_STYLE, left + tabWidth, startY, tabWidth, BTN_HEIGHT,
        "Style"));
    buttonList.add(new GuiButton(BTN_TAB_PREVIEW, left + tabWidth * 2, startY, tabWidth,
        BTN_HEIGHT, "Preview"));

    int halfW = (FIELD_WIDTH - 4) / 2;
    switch (currentTab) {
      case TAB_TEXT:
        buildTextTab(left, viewportTop, halfW);
        break;
      case TAB_STYLE:
        buildStyleTab(left, viewportTop, halfW);
        break;
      case TAB_PREVIEW:
        buildPreviewTab(left, viewportTop);
        break;
      default:
        break;
    }

    buttonList.add(new GuiButton(BTN_SAVE, left, bottomY, halfW, BTN_HEIGHT, "Save"));
    buttonList.add(
        new GuiButton(BTN_CANCEL, left + halfW + 4, bottomY, halfW, BTN_HEIGHT, "Cancel"));

    recomputeMaxScroll();
    applyTabScroll();
    updateTabButtonStates();
  }

  // ------------------------------------------------------------------ tab building ----

  private void buildTextTab(int left, int y, int halfW) {
    // Prefix / name / suffix share a line, in the order they read on the blade.
    int prefixW = 34;
    int suffixW = 44;
    int nameW = FIELD_WIDTH - prefixW - suffixW - 8;
    labelYAffixRow = y;
    y += 10;
    prefixField = makeField(1, left, y, prefixW, StreetSignData.MAX_AFFIX_LENGTH,
        data.getPrefix());
    nameField = makeField(2, left + prefixW + 4, y, nameW, StreetSignData.MAX_NAME_LENGTH,
        data.getStreetName());
    suffixField = makeField(3, left + prefixW + nameW + 8, y, suffixW,
        StreetSignData.MAX_AFFIX_LENGTH, data.getSuffix());
    y += BTN_HEIGHT + 14;

    labelYCityRow = y;
    cityField = makeField(4, left, y, FIELD_WIDTH, StreetSignData.MAX_CITY_LENGTH,
        data.getCityText());
    y += BTN_HEIGHT + 14;

    labelYTextScale = y;
    addContentBtn(new GuiButton(BTN_TEXT_SCALE_DOWN, left, y, 30, BTN_HEIGHT, "-"));
    addContentBtn(new GuiButton(BTN_TEXT_SCALE_UP, left + FIELD_WIDTH - 30, y, 30, BTN_HEIGHT,
        "+"));
    y += BTN_HEIGHT + 3;
    addContentBtn(new GuiButton(BTN_AFFIX_VERTICAL, left, y, FIELD_WIDTH, BTN_HEIGHT, ""));
    y += BTN_HEIGHT + 12;

    // --- Block number ---
    labelYBlockRow = y;
    blockField = makeField(5, left, y, halfW, StreetSignData.MAX_BLOCK_LENGTH,
        data.getBlockNumber());
    addContentBtn(new GuiButton(BTN_BLOCK_POSITION, left + halfW + 4, y, halfW, BTN_HEIGHT, ""));
    y += BTN_HEIGHT + 3;
    GuiButton blockVertical =
        new GuiButton(BTN_BLOCK_VERTICAL, left, y, FIELD_WIDTH, BTN_HEIGHT, "");
    blockVertical.enabled = data.hasBlockNumber();
    addContentBtn(blockVertical);
    y += BTN_HEIGHT + 12;

    // --- Emblem ---
    addContentBtn(new GuiButton(BTN_EMBLEM_KIND, left, y, halfW, BTN_HEIGHT, ""));
    GuiButton emblemPosition =
        new GuiButton(BTN_EMBLEM_POSITION, left + halfW + 4, y, halfW, BTN_HEIGHT, "");
    emblemPosition.enabled = data.getEmblemKind() != StreetSignEmblemKind.NONE;
    addContentBtn(emblemPosition);
    y += BTN_HEIGHT + 3;

    labelYShieldRow = y;
    if (data.getEmblemKind() == StreetSignEmblemKind.SHIELD) {
      addContentBtn(new GuiButton(BTN_SHIELD_TYPE_PREV, left, y, 24, BTN_HEIGHT, "<"));
      addContentBtn(new GuiButton(BTN_SHIELD_TYPE_NEXT, left + FIELD_WIDTH - 24, y, 24,
          BTN_HEIGHT, ">"));
      y += BTN_HEIGHT + 3;
      routeField = makeField(6, left, y, FIELD_WIDTH, StreetSignData.MAX_ROUTE_LENGTH,
          data.getShieldRoute());
      y += BTN_HEIGHT + 12;
    } else if (data.getEmblemKind() == StreetSignEmblemKind.LOGO) {
      addContentBtn(new GuiButton(BTN_LOGO_TYPE_PREV, left, y, 24, BTN_HEIGHT, "<"));
      addContentBtn(new GuiButton(BTN_LOGO_TYPE_NEXT, left + FIELD_WIDTH - 24, y, 24,
          BTN_HEIGHT, ">"));
      y += BTN_HEIGHT + 12;
    } else {
      y += 12;
    }

    // --- Arrow ---
    addContentBtn(new GuiButton(BTN_ARROW_POSITION, left, y, halfW, BTN_HEIGHT, ""));
    GuiButton arrowType =
        new GuiButton(BTN_ARROW_TYPE, left + halfW + 4, y, halfW, BTN_HEIGHT, "");
    arrowType.enabled = data.hasArrow();
    addContentBtn(arrowType);
    y += BTN_HEIGHT + 4;
    customContentBottom = y;
  }

  private void buildStyleTab(int left, int y, int halfW) {
    addContentBtn(new GuiButton(BTN_SIGN_COLOR, left, y, halfW, BTN_HEIGHT, ""));
    addContentBtn(new GuiButton(BTN_CORNER_STYLE, left + halfW + 4, y, halfW, BTN_HEIGHT, ""));
    y += BTN_HEIGHT + 3;
    addContentBtn(new GuiButton(BTN_MOUNT_TYPE, left, y, FIELD_WIDTH, BTN_HEIGHT, ""));
    y += BTN_HEIGHT + 3;
    GuiButton doubleSided =
        new GuiButton(BTN_DOUBLE_SIDED, left, y, halfW, BTN_HEIGHT, "");
    // A flat blade has a block behind it; there is no reverse to letter.
    doubleSided.enabled = data.getMountType().canBeDoubleSided();
    addContentBtn(doubleSided);
    addContentBtn(new GuiButton(BTN_EXTRUDED_FRAME, left + halfW + 4, y, halfW, BTN_HEIGHT, ""));
    y += BTN_HEIGHT + 6;

    addContentBtn(new GuiButton(BTN_BORDER_DOWN, left, y, 30, BTN_HEIGHT, "-"));
    addContentBtn(new GuiButton(BTN_BORDER_UP, left + FIELD_WIDTH - 30, y, 30, BTN_HEIGHT, "+"));
    y += BTN_HEIGHT + 3;
    addContentBtn(new GuiButton(BTN_MIN_WIDTH_DOWN, left, y, 30, BTN_HEIGHT, "-"));
    addContentBtn(new GuiButton(BTN_MIN_WIDTH_UP, left + FIELD_WIDTH - 30, y, 30, BTN_HEIGHT,
        "+"));
    y += BTN_HEIGHT + 3;
    addContentBtn(new GuiButton(BTN_MIN_HEIGHT_DOWN, left, y, 30, BTN_HEIGHT, "-"));
    addContentBtn(new GuiButton(BTN_MIN_HEIGHT_UP, left + FIELD_WIDTH - 30, y, 30, BTN_HEIGHT,
        "+"));
    y += BTN_HEIGHT + 6;

    addContentBtn(new GuiButton(BTN_INTERNAL_LIGHT, left, y, FIELD_WIDTH, BTN_HEIGHT, ""));
    y += BTN_HEIGHT + 3;
    GuiButton lightMode = new GuiButton(BTN_LIGHT_MODE, left, y, FIELD_WIDTH, BTN_HEIGHT, "");
    lightMode.enabled = data.hasInternalLight();
    addContentBtn(lightMode);
    y += BTN_HEIGHT + 6;

    addContentBtn(new GuiButton(BTN_TEMPLATE, left, y, FIELD_WIDTH, BTN_HEIGHT, ""));
    y += BTN_HEIGHT + 3;
    addContentBtn(new GuiButton(BTN_COPY, left, y, halfW, BTN_HEIGHT, "Copy Sign"));
    GuiButton paste = new GuiButton(BTN_PASTE, left + halfW + 4, y, halfW, BTN_HEIGHT,
        "Paste Sign");
    paste.enabled = clipboardJson != null;
    addContentBtn(paste);
    y += BTN_HEIGHT + 4;
    customContentBottom = y;
  }

  private void buildPreviewTab(int left, int y) {
    rebuildPreviewLines();
    customContentBottom = y + PREVIEW_VISUAL_HEIGHT + 4
        + previewLines.size() * PREVIEW_LINE_HEIGHT;
  }

  private GuiTextField makeField(int id, int x, int y, int width, int maxLength, String text) {
    GuiTextField field = new GuiTextField(id, fontRenderer, x, y, width, BTN_HEIGHT);
    field.setMaxStringLength(maxLength);
    field.setText(text);
    addContentField(field);
    return field;
  }

  private void addContentBtn(GuiButton btn) {
    contentButtons.add(btn);
    contentButtonNaturalY.add(btn.y);
    buttonList.add(btn);
  }

  private void addContentField(GuiTextField field) {
    contentTextFields.add(field);
    contentTextFieldNaturalY.add(field.y);
  }

  private void recomputeMaxScroll() {
    int maxBottom = viewportTop;
    for (int i = 0; i < contentButtons.size(); i++) {
      int bottom = contentButtonNaturalY.get(i) + contentButtons.get(i).height;
      if (bottom > maxBottom) {
        maxBottom = bottom;
      }
    }
    for (int i = 0; i < contentTextFields.size(); i++) {
      int bottom = contentTextFieldNaturalY.get(i) + contentTextFields.get(i).height;
      if (bottom > maxBottom) {
        maxBottom = bottom;
      }
    }
    if (customContentBottom > maxBottom) {
      maxBottom = customContentBottom;
    }
    int extent = maxBottom - viewportTop;
    tabContentMaxScroll = Math.max(0, extent - (viewportBottom - viewportTop));
    tabContentScroll = Math.max(0, Math.min(tabContentMaxScroll, tabContentScroll));
  }

  private void applyTabScroll() {
    for (int i = 0; i < contentButtons.size(); i++) {
      GuiButton btn = contentButtons.get(i);
      int scrolledY = contentButtonNaturalY.get(i) - tabContentScroll;
      btn.y = scrolledY;
      btn.visible = scrolledY + btn.height > viewportTop && scrolledY < viewportBottom;
    }
    for (int i = 0; i < contentTextFields.size(); i++) {
      GuiTextField field = contentTextFields.get(i);
      int scrolledY = contentTextFieldNaturalY.get(i) - tabContentScroll;
      field.y = scrolledY;
      field.setVisible(scrolledY + field.height > viewportTop && scrolledY < viewportBottom);
    }
  }

  private void updateTabButtonStates() {
    for (GuiButton btn : buttonList) {
      if (btn.id == BTN_TAB_TEXT) {
        btn.enabled = currentTab != TAB_TEXT;
      } else if (btn.id == BTN_TAB_STYLE) {
        btn.enabled = currentTab != TAB_STYLE;
      } else if (btn.id == BTN_TAB_PREVIEW) {
        btn.enabled = currentTab != TAB_PREVIEW;
      }
    }
  }

  // ----------------------------------------------------------------------- drawing ----

  @Override
  public void drawScreen(int mouseX, int mouseY, float partialTicks) {
    drawDefaultBackground();

    ScaledResolution sr = new ScaledResolution(this.mc);
    int centerX = sr.getScaledWidth() / 2;
    int left = centerX - FIELD_WIDTH / 2;

    drawCenteredString(fontRenderer, "Dynamic Street Sign", centerX, 10, 0x66CCFF);

    switch (currentTab) {
      case TAB_TEXT:
        drawTextTabLabels(left, viewportTop, centerX);
        break;
      case TAB_STYLE:
        drawStyleTabLabels(left, viewportTop, centerX);
        break;
      case TAB_PREVIEW:
        drawPreviewTab(left, viewportTop);
        break;
      default:
        break;
    }

    for (GuiTextField field : contentTextFields) {
      if (field.getVisible()) {
        field.drawTextBox();
      }
    }

    super.drawScreen(mouseX, mouseY, partialTicks);

    if (tabContentMaxScroll > 0) {
      int trackX = left + FIELD_WIDTH + 4;
      int trackHeight = viewportBottom - viewportTop;
      int totalContent = trackHeight + tabContentMaxScroll;
      int thumbHeight = Math.max(8, (trackHeight * trackHeight) / totalContent);
      int thumbY = viewportTop
          + (trackHeight - thumbHeight) * tabContentScroll / tabContentMaxScroll;
      drawRect(trackX, viewportTop, trackX + SCROLLBAR_WIDTH, viewportTop + trackHeight,
          0x55000000);
      drawRect(trackX, thumbY, trackX + SCROLLBAR_WIDTH, thumbY + thumbHeight, 0xCCAAAAAA);
    }
  }

  private void drawTextTabLabels(int left, int y, int centerX) {
    for (GuiButton btn : buttonList) {
      switch (btn.id) {
        case BTN_BLOCK_POSITION:
          btn.displayString = "Block: " + data.getBlockPosition().getFriendlyName();
          break;
        case BTN_BLOCK_VERTICAL:
          btn.displayString = "Block Number Height: "
              + data.getBlockVertical().getFriendlyName();
          break;
        case BTN_EMBLEM_KIND:
          btn.displayString = data.getEmblemKind().getFriendlyName();
          break;
        case BTN_EMBLEM_POSITION:
          btn.displayString = "Side: " + data.getEmblemPosition().getFriendlyName();
          break;
        case BTN_ARROW_POSITION:
          btn.displayString = "Arrow: " + data.getArrowPosition().getFriendlyName();
          break;
        case BTN_ARROW_TYPE:
          btn.displayString = data.getArrowType().getFriendlyName();
          break;
        case BTN_AFFIX_VERTICAL:
          btn.displayString = "Prefix / Suffix Align: "
              + data.getAffixVertical().getFriendlyName();
          break;
        default:
          break;
      }
    }

    drawScrolledString("Prefix", left, labelYAffixRow, 0xAAAAAA);
    drawScrolledString("Street Name", left + 38, labelYAffixRow, 0xAAAAAA);
    drawScrolledString("Suffix", left + FIELD_WIDTH - 44, labelYAffixRow, 0xAAAAAA);
    drawScrolledString("City / district line (optional)", left, labelYCityRow - 10, 0xAAAAAA);
    drawScrolledCenteredString(String.format("Text Size: %.2fx", data.getTextScale()),
        centerX, labelYTextScale + 5, 0xFFFFFF);
    drawScrolledString("Block no.", left, labelYBlockRow - 9, 0xAAAAAA);

    if (data.getEmblemKind() == StreetSignEmblemKind.SHIELD) {
      drawScrolledCenteredString(data.getShieldType().getFriendlyName(), centerX,
          labelYShieldRow + 5, 0xFFFFFF);
      drawScrolledString("Route number", left, labelYShieldRow + BTN_HEIGHT + 3 - 9, 0xAAAAAA);
    } else if (data.getEmblemKind() == StreetSignEmblemKind.LOGO) {
      drawScrolledCenteredString(data.getLogoType().getFriendlyName(), centerX,
          labelYShieldRow + 5, 0xFFFFFF);
    }
  }

  private void drawStyleTabLabels(int left, int y, int centerX) {
    for (GuiButton btn : buttonList) {
      switch (btn.id) {
        case BTN_SIGN_COLOR:
          btn.displayString = data.getSignColor().getFriendlyName();
          break;
        case BTN_CORNER_STYLE:
          btn.displayString = "Corners: " + data.getCornerStyle().getFriendlyName();
          break;
        case BTN_MOUNT_TYPE:
          btn.displayString = "Mount: " + data.getMountType().getFriendlyName();
          break;
        case BTN_DOUBLE_SIDED:
          btn.displayString = "Both Sides: "
              + (data.isDoubleSidedRequested() ? "ON" : "OFF");
          break;
        case BTN_EXTRUDED_FRAME:
          btn.displayString = "Frame: " + (data.hasExtrudedFrame() ? "ON" : "OFF");
          break;
        case BTN_INTERNAL_LIGHT:
          btn.displayString = "Internal Illumination: "
              + (data.hasInternalLight() ? "ON" : "OFF");
          break;
        case BTN_LIGHT_MODE:
          btn.displayString = "Lights: " + data.getLightMode().getFriendlyName();
          break;
        case BTN_TEMPLATE:
          btn.displayString = "Apply Template: " + StreetSignTemplates.getName(templateIndex);
          break;
        default:
          break;
      }
    }

    int row = y + (BTN_HEIGHT + 3) * 2 + BTN_HEIGHT + 6;
    drawScrolledCenteredString("Border: " + data.getBorderWidth(), centerX, row + 5, 0xFFFFFF);
    row += BTN_HEIGHT + 3;
    drawScrolledCenteredString(
        String.format("Min Width: %d px (%.2f blk)", data.getMinWidth(),
            data.getMinWidth() / 16.0f), centerX, row + 5, 0xFFFFFF);
    row += BTN_HEIGHT + 3;
    drawScrolledCenteredString(
        String.format("Min Height: %d px (%.2f blk)", data.getMinHeight(),
            data.getMinHeight() / 16.0f), centerX, row + 5, 0xFFFFFF);

    if (!data.getMountType().canBeDoubleSided()) {
      drawScrolledCenteredString(
          TextFormatting.GRAY + "A flat blade has no exposed reverse", centerX,
          y + (BTN_HEIGHT + 3) * 2 + BTN_HEIGHT - 8, 0xAAAAAA);
    }
  }

  private void drawPreviewTab(int left, int y) {
    drawVisualPreview(left, y);
    int lineY = y + PREVIEW_VISUAL_HEIGHT + 4;
    for (String line : previewLines) {
      drawScrolledString(line, left, lineY, 0xFFFFFF);
      lineY += PREVIEW_LINE_HEIGHT;
    }
  }

  /**
   * Live WYSIWYG preview drawn through the TESR's own render path, scaled to fit the box and
   * scissor-clipped to it. The transform flips Y because sign pixel space is +Y up, and flips
   * Z because GUI ortho treats larger z as closer while the readable face sits at smaller z
   * than the aluminum core. No X mirror: the TESR applies its own only in world space.
   */
  private void drawVisualPreview(int left, int naturalY) {
    int boxTop = naturalY - tabContentScroll;
    if (boxTop + PREVIEW_VISUAL_HEIGHT < viewportTop || boxTop > viewportBottom) {
      return;
    }
    drawRect(left, boxTop, left + FIELD_WIDTH, boxTop + PREVIEW_VISUAL_HEIGHT, 0xFF2A2A2E);

    float[] box = previewRenderer.computePreviewBox(data);
    float scale = Math.min((FIELD_WIDTH - 8) / Math.max(1.0f, box[2]),
        (PREVIEW_VISUAL_HEIGHT - 8) / Math.max(1.0f, box[3]));
    float cx = left + FIELD_WIDTH / 2.0f;
    float cy = boxTop + PREVIEW_VISUAL_HEIGHT / 2.0f;

    ScaledResolution sr = new ScaledResolution(mc);
    int f = sr.getScaleFactor();
    GL11.glEnable(GL11.GL_SCISSOR_TEST);
    GL11.glScissor(left * f, mc.displayHeight - (boxTop + PREVIEW_VISUAL_HEIGHT) * f,
        FIELD_WIDTH * f, PREVIEW_VISUAL_HEIGHT * f);

    GlStateManager.pushMatrix();
    GlStateManager.translate(cx, cy, 120);
    GlStateManager.scale(scale, -scale, -1.0f);
    GlStateManager.translate(-box[0], -box[1], 0.0f);
    GlStateManager.enableDepth();
    previewRenderer.renderForGui(data);
    GlStateManager.popMatrix();

    GL11.glDisable(GL11.GL_SCISSOR_TEST);

    // Reset depth so the preview's z-layered quads cannot occlude later GUI drawing.
    GlStateManager.clear(GL11.GL_DEPTH_BUFFER_BIT);
    GlStateManager.disableLighting();
    GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
  }

  private void rebuildPreviewLines() {
    previewLines.clear();
    StringBuilder legend = new StringBuilder();
    if (!data.getPrefix().isEmpty()) {
      legend.append(data.getPrefix()).append(' ');
    }
    legend.append(data.getStreetName());
    if (!data.getSuffix().isEmpty()) {
      legend.append(' ').append(data.getSuffix());
    }
    previewLines.add("Legend: " + legend);
    if (data.hasCityText()) {
      previewLines.add("City line: " + data.getCityText());
    }
    previewLines.add("Mount: " + data.getMountType().getFriendlyName()
        + (data.isDoubleSided() ? " (both sides)" : " (front only)"));
    previewLines.add("Color: " + data.getSignColor().getFriendlyName()
        + ", border " + data.getBorderWidth()
        + ", " + data.getCornerStyle().getFriendlyName().toLowerCase() + " corners"
        + (data.hasExtrudedFrame() ? ", framed" : ""));
    previewLines.add("Lighting: " + (data.hasInternalLight()
        ? "internal, " + data.getLightMode().getFriendlyName() : "none"));
    if (data.hasBlockNumber()) {
      previewLines.add("Block number: " + data.getBlockNumber() + " ("
          + data.getBlockPosition().getFriendlyName().toLowerCase() + ", "
          + data.getBlockVertical().getFriendlyName().toLowerCase() + ")");
    }
    if (data.hasEmblem()) {
      String what = data.getEmblemKind() == StreetSignEmblemKind.SHIELD
          ? data.getShieldType().getFriendlyName()
          + (data.getShieldRoute().isEmpty() ? "" : " " + data.getShieldRoute())
          : data.getLogoType().getFriendlyName();
      previewLines.add("Emblem: " + what + " ("
          + data.getEmblemPosition().getFriendlyName().toLowerCase() + ")");
    }
    if (data.hasArrow()) {
      previewLines.add("Arrow: " + data.getArrowType().getFriendlyName() + " ("
          + data.getArrowPosition().getFriendlyName().toLowerCase() + ")");
    }
  }

  private void drawScrolledString(String text, int x, int naturalY, int color) {
    int y = naturalY - tabContentScroll;
    if (y + fontRenderer.FONT_HEIGHT > viewportTop && y < viewportBottom) {
      drawString(fontRenderer, text, x, y, color);
    }
  }

  private void drawScrolledCenteredString(String text, int x, int naturalY, int color) {
    int y = naturalY - tabContentScroll;
    if (y + fontRenderer.FONT_HEIGHT > viewportTop && y < viewportBottom) {
      drawCenteredString(fontRenderer, text, x, y, color);
    }
  }

  // ------------------------------------------------------------------------- input ----

  @Override
  protected void keyTyped(char typedChar, int keyCode) throws IOException {
    if (keyCode == Keyboard.KEY_ESCAPE) {
      this.mc.displayGuiScreen(null);
      return;
    }
    for (GuiTextField field : contentTextFields) {
      if (field.getVisible() && field.isFocused()) {
        field.textboxKeyTyped(typedChar, keyCode);
        syncFields();
        return;
      }
    }
  }

  @Override
  protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
    for (GuiTextField field : contentTextFields) {
      if (field.getVisible()) {
        field.mouseClicked(mouseX, mouseY, mouseButton);
      }
    }
    super.mouseClicked(mouseX, mouseY, mouseButton);
  }

  @Override
  public void handleMouseInput() throws IOException {
    super.handleMouseInput();
    int scroll = Mouse.getEventDWheel();
    if (scroll != 0 && tabContentMaxScroll > 0) {
      int delta = scroll > 0 ? -SCROLL_STEP : SCROLL_STEP;
      int previous = tabContentScroll;
      tabContentScroll = Math.max(0, Math.min(tabContentMaxScroll, tabContentScroll + delta));
      if (tabContentScroll != previous) {
        applyTabScroll();
      }
    }
  }

  @Override
  public void updateScreen() {
    for (GuiTextField field : contentTextFields) {
      field.updateCursorCounter();
    }
  }

  /** Pushes every visible text field's contents into the document. */
  private void syncFields() {
    if (prefixField != null) {
      data.setPrefix(prefixField.getText());
    }
    if (nameField != null) {
      data.setStreetName(nameField.getText());
    }
    if (suffixField != null) {
      data.setSuffix(suffixField.getText());
    }
    if (cityField != null) {
      data.setCityText(cityField.getText());
    }
    if (blockField != null) {
      data.setBlockNumber(blockField.getText());
    }
    if (routeField != null) {
      data.setShieldRoute(routeField.getText());
    }
  }

  @Override
  @ParametersAreNonnullByDefault
  protected void actionPerformed(GuiButton button) {
    // Any button press can be preceded by typing, and several rebuild the tab (dropping the
    // fields), so capture what is typed before acting on the press.
    syncFields();

    switch (button.id) {
      case BTN_TAB_TEXT:
        currentTab = TAB_TEXT;
        tabContentScroll = 0;
        initGui();
        return;
      case BTN_TAB_STYLE:
        currentTab = TAB_STYLE;
        tabContentScroll = 0;
        initGui();
        return;
      case BTN_TAB_PREVIEW:
        currentTab = TAB_PREVIEW;
        tabContentScroll = 0;
        initGui();
        return;

      case BTN_SAVE:
        CsmRoads.NETWORK.sendToServer(
            new DynamicStreetSignUpdatePacket(tileEntity.getPos(), data.toJson()));
        this.mc.displayGuiScreen(null);
        return;
      case BTN_CANCEL:
        this.mc.displayGuiScreen(null);
        return;

      // --- Text tab ---
      case BTN_TEXT_SCALE_DOWN:
        data.setTextScale(data.getTextScale() - 0.1f);
        break;
      case BTN_TEXT_SCALE_UP:
        data.setTextScale(data.getTextScale() + 0.1f);
        break;
      case BTN_BLOCK_POSITION:
        data.cycleBlockPosition();
        break;
      case BTN_BLOCK_VERTICAL:
        data.cycleBlockVertical();
        break;
      case BTN_EMBLEM_KIND:
        data.cycleEmblemKind();
        break;
      case BTN_EMBLEM_POSITION:
        data.cycleEmblemPosition();
        break;
      case BTN_SHIELD_TYPE_PREV:
        data.setShieldType(data.getShieldType().prev());
        break;
      case BTN_SHIELD_TYPE_NEXT:
        data.setShieldType(data.getShieldType().next());
        break;
      case BTN_LOGO_TYPE_PREV:
        data.setLogoType(data.getLogoType().prev());
        break;
      case BTN_LOGO_TYPE_NEXT:
        data.setLogoType(data.getLogoType().next());
        break;
      case BTN_ARROW_POSITION:
        data.cycleArrowPosition();
        break;
      case BTN_ARROW_TYPE:
        data.setArrowType(data.getArrowType().next());
        break;
      case BTN_AFFIX_VERTICAL:
        data.cycleAffixVertical();
        break;

      // --- Style tab ---
      case BTN_SIGN_COLOR:
        data.cycleSignColor();
        break;
      case BTN_CORNER_STYLE:
        data.cycleCornerStyle();
        break;
      case BTN_MOUNT_TYPE:
        data.cycleMountType();
        break;
      case BTN_DOUBLE_SIDED:
        data.toggleDoubleSided();
        break;
      case BTN_EXTRUDED_FRAME:
        data.toggleExtrudedFrame();
        break;
      case BTN_BORDER_DOWN:
        data.setBorderWidth(data.getBorderWidth() - 1);
        break;
      case BTN_BORDER_UP:
        data.setBorderWidth(data.getBorderWidth() + 1);
        break;
      case BTN_MIN_WIDTH_DOWN:
        data.setMinWidth(data.getMinWidth() - MIN_SIZE_STEP);
        break;
      case BTN_MIN_WIDTH_UP:
        data.setMinWidth(data.getMinWidth() + MIN_SIZE_STEP);
        break;
      case BTN_MIN_HEIGHT_DOWN:
        data.setMinHeight(data.getMinHeight() - MIN_SIZE_STEP);
        break;
      case BTN_MIN_HEIGHT_UP:
        data.setMinHeight(data.getMinHeight() + MIN_SIZE_STEP);
        break;
      case BTN_INTERNAL_LIGHT:
        data.toggleInternalLight();
        break;
      case BTN_LIGHT_MODE:
        data.cycleLightMode();
        break;
      case BTN_TEMPLATE:
        data = StreetSignTemplates.get(templateIndex);
        templateIndex = (templateIndex + 1) % StreetSignTemplates.count();
        break;
      case BTN_COPY:
        clipboardJson = data.toJson();
        break;
      case BTN_PASTE:
        if (clipboardJson != null) {
          data = StreetSignData.fromJson(clipboardJson);
        }
        break;
      default:
        return;
    }
    // Most controls change which widgets belong on the tab (a chosen emblem kind adds its
    // chooser, a flat mount disables the back-face toggle), so rebuild rather than trying to
    // patch individual buttons.
    initGui();
  }

  @Override
  public boolean doesGuiPauseGame() {
    return true;
  }

  @Override
  public void onGuiClosed() {
    Keyboard.enableRepeatEvents(false);
  }
}

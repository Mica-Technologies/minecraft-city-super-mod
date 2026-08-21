package com.micatechnologies.minecraft.csm.trafficaccessories;

import com.micatechnologies.minecraft.csm.codeutils.RenderHelper;
import com.micatechnologies.minecraft.csm.trafficaccessories.guidesign.CornerStyle;
import com.micatechnologies.minecraft.csm.trafficaccessories.guidesign.ExitTabData;
import com.micatechnologies.minecraft.csm.trafficaccessories.guidesign.GuideSignArrowType;
import com.micatechnologies.minecraft.csm.trafficaccessories.guidesign.GuideSignAtlas;
import com.micatechnologies.minecraft.csm.trafficaccessories.guidesign.GuideSignColor;
import com.micatechnologies.minecraft.csm.trafficaccessories.guidesign.GuideSignData;
import com.micatechnologies.minecraft.csm.trafficaccessories.guidesign.GuideSignElement;
import com.micatechnologies.minecraft.csm.trafficaccessories.guidesign.GuideSignPanel;
import com.micatechnologies.minecraft.csm.trafficaccessories.guidesign.GuideSignRow;
import com.micatechnologies.minecraft.csm.trafficaccessories.guidesign.GuideSignShieldType;
import com.micatechnologies.minecraft.csm.trafficaccessories.guidesign.PostType;
import com.micatechnologies.minecraft.csm.trafficaccessories.guidesign.RowAlignment;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.block.BlockHorizontal;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import org.lwjgl.opengl.GL11;

public class TileEntityDynamicGuideSignRenderer
    extends TileEntitySpecialRenderer<TileEntityDynamicGuideSign> {

  private static final float SIGN_DEPTH = 1.5f;
  private static final float BORDER_INSET = 0.4f;
  private static final float CX = 8.0f;
  private static final float CY = 8.0f;

  private static final float PANEL_PADDING_TOP = 2.5f;
  private static final float PANEL_PADDING_BOTTOM = 2.5f;
  private static final float PANEL_PADDING_SIDE = 3.0f;
  private static final float ROW_HEIGHT = 10.0f;
  private static final float ROW_MIN_HEIGHT = 6.0f;
  private static final float ROW_SPACING = 1.5f;
  private static final float ELEMENT_SPACING = 2.0f;
  // Sized against SignMaker/MUTCD ratios: shield and arrow are ~2.4x the destination
  // text's cap height (MC font caps at TEXT_BASE_SCALE are ~5.6px).
  private static final float SHIELD_SIZE = 12.0f;
  private static final float ARROW_SIZE = 12.0f;
  // Extra horizontal room reserved around a banner word so adjacent shields' banners
  // don't visually collide.
  private static final float BANNER_CELL_PADDING = 2.0f;
  private static final float EXIT_TAB_HEIGHT = 8.0f;
  private static final float EXIT_TAB_PADDING = 3.0f;
  private static final float PANEL_GAP = 4.0f;
  private static final float PANEL_DIVIDER_THICKNESS = 0.7f;
  private static final float DIVIDER_ELEMENT_WIDTH = 0.8f;

  private static final float POST_WIDTH = 2.5f;
  private static final float POST_DEPTH = 1.5f;

  // Legend text renders in the FHWA-style highway font (GuideSignFontRenderer); all
  // text sizes below are CAP HEIGHTS in sign pixel units.
  private static final float TEXT_CAP_HEIGHT = 5.6f;
  // Row-height factor over cap height, making room for lowercase descenders.
  private static final float TEXT_VISUAL_FACTOR = 1.3f;
  // Vertical breathing room added above+below a text element when sizing its row.
  private static final float TEXT_LEADING = 2.0f;
  private static final float EXIT_TAB_CAP_HEIGHT = 4.5f;
  // MUTCD-ish ratios against the shield: route numerals ~42% of shield height, banner
  // words ~33%.
  private static final float ROUTE_CAP_FRACTION = 0.42f;
  private static final float BANNER_CAP_FRACTION = 0.33f;
  // Fraction of the shield cell the route number may span before it shrinks to fit.
  private static final float ROUTE_TEXT_MAX_FRACTION = 0.62f;
  private static final float BANNER_AREA_HEIGHT = 5.5f;

  // Per-step chamfer for ROUND corners. Two stair steps approximate a small radius.
  private static final float CORNER_STEP = 0.6f;

  private static final int LEGEND_DARK = 0x101010;
  private static final int LEGEND_WHITE = 0xFFFFFF;

  // Cached per-frame lightmap split from the block's actual combined light, used so
  // text/shield/banner overlays respect day-night cycle and nearby light sources rather
  // than rendering at fullbright. Baked per-vertex via the BLOCK vertex format for shader
  // compatibility (shaders ignore OpenGlHelper.setLightmapTextureCoords global state).
  private int worldSkyLight;
  private int worldBlockLight;

  private static final ResourceLocation WHITE_TEXTURE =
      new ResourceLocation("csm", "textures/blocks/white1px.png");

  @Override
  public void render(TileEntityDynamicGuideSign te, double x, double y, double z,
      float partialTicks, int destroyStage, float alpha) {
    if (te == null || te.getWorld() == null) {
      return;
    }

    GuideSignData data = te.getSignData();
    if (data == null) {
      return;
    }

    int combinedLight = te.getWorld().getCombinedLight(te.getPos(), 0);
    worldSkyLight = (combinedLight >> 16) & 0xFFFF;
    worldBlockLight = combinedLight & 0xFFFF;

    EnumFacing facing = te.getWorld().getBlockState(te.getPos())
        .getValue(BlockHorizontal.FACING);

    GlStateManager.pushMatrix();
    GlStateManager.translate(x, y, z);
    GlStateManager.translate(0.5, 0.0, 0.5);

    float rotY = 0;
    switch (facing) {
      case SOUTH:
        rotY = 0;
        break;
      case WEST:
        rotY = 90;
        break;
      case NORTH:
        rotY = 180;
        break;
      case EAST:
        rotY = 270;
        break;
      default:
        break;
    }
    GlStateManager.rotate(rotY, 0, 1, 0);
    GlStateManager.translate(-0.5, 0.0, -0.5);
    GlStateManager.scale(0.0625, 0.0625, 0.0625);
    // Mirror pixel-space X about the block center so +X in the layout math below is the
    // READER's right. Without this every position (element order, row alignment, exit tab
    // and post sides) renders mirrored: the local +X axis points to the reader's left on
    // the readable face. Consequence for overlays drawn inside this space: text and atlas
    // quads map u0 to their LEFT edge and advance toward +X, exactly as unmirrored 2D
    // drawing would (see GuideSignFontRenderer).
    GlStateManager.translate(16.0f, 0.0f, 0.0f);
    GlStateManager.scale(-1.0f, 1.0f, 1.0f);

    renderSign(data);

    GlStateManager.popMatrix();
  }

  private void renderSign(GuideSignData data) {
    GuideSignColor signColor = data.getSignColor();
    int borderWidth = data.getBorderWidth();
    CornerStyle cornerStyle = data.getCornerStyle();
    List<GuideSignPanel> panels = data.getPanels();

    // Legend (border, dividers, default text) contrasts with the sign background:
    // white on green/blue/brown/black/purple, near-black on white/yellow.
    boolean lightSign = signColor.isLight();
    float legendR = lightSign ? 0.06f : 0.92f;
    float legendG = lightSign ? 0.06f : 0.92f;
    float legendB = lightSign ? 0.06f : 0.90f;
    int legendTextColor = lightSign ? LEGEND_DARK : LEGEND_WHITE;

    float totalSignHeight = computeTotalSignHeight(panels, data);
    float totalSignWidth = computeTotalSignWidth(panels, data);

    float signLeft = CX - totalSignWidth / 2.0f;
    float signTop = CY + totalSignHeight / 2.0f;
    float signBottom = CY - totalSignHeight / 2.0f;
    float faceZ = 16.0f - SIGN_DEPTH;

    GlStateManager.disableLighting();
    GL11.glDisable(GL11.GL_LIGHTING);
    GlStateManager.disableCull();
    GlStateManager.enableBlend();
    GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
    // Bind 1x1 white texture instead of disableTexture2D — shaders ignore disableTexture2D.
    Minecraft.getMinecraft().getTextureManager().bindTexture(WHITE_TEXTURE);
    GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
    GL11.glColor4f(1.0f, 1.0f, 1.0f, 1.0f);

    renderSignBackground(signLeft, signBottom, totalSignWidth, totalSignHeight,
        faceZ, signColor, borderWidth, cornerStyle, legendR, legendG, legendB);

    float borderInset = borderWidth > 0 ? borderWidth * BORDER_INSET : 0;
    float contentLeft = signLeft + PANEL_PADDING_SIDE + borderInset;
    float contentRight = signLeft + totalSignWidth - PANEL_PADDING_SIDE - borderInset;

    float panelY = signTop - PANEL_PADDING_TOP - borderInset;

    for (int pi = 0; pi < panels.size(); pi++) {
      GuideSignPanel panel = panels.get(pi);

      // Only the top panel's exit tab is rendered: the tab hangs above the sign's top
      // edge, and stacked lower panels have no free edge for one (they would all pile
      // up at the sign top and overlap).
      if (pi == 0 && panel.hasExitTab()) {
        renderExitTab(panel.getExitTab(), signLeft, signTop, totalSignWidth,
            faceZ, borderWidth, cornerStyle, legendR, legendG, legendB);
      }

      for (GuideSignRow row : panel.getRows()) {
        panelY -= row.getVerticalSpacing();

        float bannerExtra = rowHasBanner(row) ? BANNER_AREA_HEIGHT : 0;
        panelY -= bannerExtra;

        float rowHeight = computeRowHeight(row);
        float rowWidth = computeRowWidth(row, data);
        float rowX = computeRowX(row, contentLeft, contentRight, rowWidth);

        renderRow(row, rowX, panelY, rowHeight, faceZ, legendTextColor,
            legendR, legendG, legendB);

        panelY -= rowHeight + ROW_SPACING;
      }
      // The loop above adds ROW_SPACING after the panel's last row too; give it back so
      // rendering stays in lockstep with computeTotalSignHeight (which only counts
      // spacing BETWEEN rows). Without this the content drifts lower with every panel.
      if (!panel.getRows().isEmpty()) {
        panelY += ROW_SPACING;
      }

      if (pi < panels.size() - 1) {
        panelY -= PANEL_GAP;
        renderPanelDivider(signLeft, panelY + PANEL_GAP / 2, totalSignWidth,
            faceZ, borderWidth, legendR, legendG, legendB);
      }
    }

    renderPost(data.getPostType(), signLeft, signBottom, totalSignWidth, faceZ);

    GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
    GL11.glColor4f(1.0f, 1.0f, 1.0f, 1.0f);
    GlStateManager.enableLighting();
    GL11.glEnable(GL11.GL_LIGHTING);
    GlStateManager.enableCull();
    GlStateManager.disableBlend();
  }

  private void renderSignBackground(float left, float bottom, float width, float height,
      float faceZ, GuideSignColor color, int borderWidth, CornerStyle cornerStyle,
      float legendR, float legendG, float legendB) {
    Tessellator tess = Tessellator.getInstance();
    BufferBuilder buf = tess.getBuffer();

    float frontZ = faceZ + SIGN_DEPTH;

    if (borderWidth > 0) {
      float bw = borderWidth * BORDER_INSET;
      List<RenderHelper.Box> border = new ArrayList<>();
      addRectBoxes(border, left - bw, bottom - bw, left + width + bw, bottom + height + bw,
          faceZ, frontZ, cornerStyle);

      buf.begin(GL11.GL_QUADS, DefaultVertexFormats.BLOCK);
      RenderHelper.addBoxesToBufferLit(border, buf, legendR, legendG, legendB, 1.0f, 0, 0, 0,
          worldSkyLight, worldBlockLight);
      tess.draw();
    }

    float inset = borderWidth > 0 ? borderWidth * BORDER_INSET : 0;
    List<RenderHelper.Box> face = new ArrayList<>();
    addRectBoxes(face, left + inset, bottom + inset, left + width - inset, bottom + height - inset,
        faceZ - 0.1f, frontZ - 0.1f, cornerStyle);

    buf.begin(GL11.GL_QUADS, DefaultVertexFormats.BLOCK);
    RenderHelper.addBoxesToBufferLit(face, buf,
        color.getRed(), color.getGreen(), color.getBlue(), 1.0f, 0, 0, 0,
        worldSkyLight, worldBlockLight);
    tess.draw();

    // Aluminum back: real guide signs are unpainted on the reverse, so cover the back of
    // the assembly (border extent included) with a gray slab instead of the legend color.
    float bw = borderWidth > 0 ? borderWidth * BORDER_INSET : 0;
    List<RenderHelper.Box> back = new ArrayList<>();
    addRectBoxes(back, left - bw, bottom - bw, left + width + bw, bottom + height + bw,
        frontZ - 0.4f, frontZ + 0.05f, cornerStyle);
    buf.begin(GL11.GL_QUADS, DefaultVertexFormats.BLOCK);
    RenderHelper.addBoxesToBufferLit(back, buf, 0.55f, 0.56f, 0.58f, 1.0f, 0, 0, 0,
        worldSkyLight, worldBlockLight);
    tess.draw();
  }

  // Voxel-style approximation of a rounded corner: split the rect into a horizontal
  // and vertical strip that overlap in the middle, leaving a CORNER_STEP square gap
  // at each of the 4 outer corners. Falls back to a single box if the rect is too
  // small for the notch or if the style is SHARP.
  private void addRectBoxes(List<RenderHelper.Box> boxes, float l, float b, float r, float t,
      float z1, float z2, CornerStyle style) {
    boolean canNotch = style == CornerStyle.ROUND
        && (r - l) > 2 * CORNER_STEP
        && (t - b) > 2 * CORNER_STEP;
    if (!canNotch) {
      boxes.add(new RenderHelper.Box(new float[]{l, b, z1}, new float[]{r, t, z2}));
      return;
    }
    float s = CORNER_STEP;
    boxes.add(new RenderHelper.Box(
        new float[]{l, b + s, z1}, new float[]{r, t - s, z2}));
    boxes.add(new RenderHelper.Box(
        new float[]{l + s, b, z1}, new float[]{r - s, t, z2}));
  }

  private float computeRowX(GuideSignRow row, float contentLeft, float contentRight,
      float rowWidth) {
    RowAlignment align = row.getAlignment();
    switch (align) {
      case LEFT:
        return contentLeft;
      case RIGHT:
        return contentRight - rowWidth;
      case CENTER:
      default:
        return (contentLeft + contentRight - rowWidth) / 2.0f;
    }
  }

  private void renderExitTab(ExitTabData tab, float signLeft, float signTop,
      float signWidth, float faceZ, int borderWidth, CornerStyle cornerStyle,
      float legendR, float legendG, float legendB) {
    String tabText = tab.getText();
    if (tabText == null || tabText.isEmpty()) {
      tabText = "EXIT";
    }

    float tabTextW = GuideSignFontRenderer.getStringWidth(tabText, EXIT_TAB_CAP_HEIGHT);
    float tabWidth = tabTextW + EXIT_TAB_PADDING * 2;
    float tabHeight = EXIT_TAB_HEIGHT;

    float tabX;
    switch (tab.getPosition()) {
      case ExitTabData.POS_LEFT:
        tabX = signLeft + PANEL_PADDING_SIDE;
        break;
      case ExitTabData.POS_CENTER:
        tabX = signLeft + signWidth / 2.0f - tabWidth / 2.0f;
        break;
      case ExitTabData.POS_RIGHT:
      default:
        tabX = signLeft + signWidth - PANEL_PADDING_SIDE - tabWidth;
        break;
    }

    float bw = borderWidth > 0 ? borderWidth * BORDER_INSET : 0;
    // Flush-mounted: the tab background starts at the sign border's outer edge, so its
    // own border band overlaps the sign's and the two read as one attached assembly
    // (no sliver of sky between tab and sign).
    float tabBottom = signTop + bw;
    float tabTop = tabBottom + tabHeight;

    Tessellator tess = Tessellator.getInstance();
    BufferBuilder buf = tess.getBuffer();

    float tabFaceZ = faceZ - 0.2f;
    float tabFrontZ = faceZ + SIGN_DEPTH - 0.2f;

    if (borderWidth > 0) {
      List<RenderHelper.Box> tabBorder = new ArrayList<>();
      addRectBoxes(tabBorder, tabX - bw, tabBottom - bw, tabX + tabWidth + bw, tabTop + bw,
          tabFaceZ + 0.1f, tabFrontZ + 0.1f, cornerStyle);
      buf.begin(GL11.GL_QUADS, DefaultVertexFormats.BLOCK);
      RenderHelper.addBoxesToBufferLit(tabBorder, buf, legendR, legendG, legendB, 1.0f,
          0, 0, 0, worldSkyLight, worldBlockLight);
      tess.draw();
    }

    GuideSignColor tabColor = tab.getGuideSignColor();
    List<RenderHelper.Box> tabBg = new ArrayList<>();
    addRectBoxes(tabBg, tabX, tabBottom, tabX + tabWidth, tabTop, tabFaceZ, tabFrontZ,
        cornerStyle);
    buf.begin(GL11.GL_QUADS, DefaultVertexFormats.BLOCK);
    RenderHelper.addBoxesToBufferLit(tabBg, buf,
        tabColor.getRed(), tabColor.getGreen(), tabColor.getBlue(), 1.0f, 0, 0, 0,
        worldSkyLight, worldBlockLight);
    tess.draw();

    // Aluminum back for the tab, matching the sign body's unpainted reverse.
    List<RenderHelper.Box> tabBack = new ArrayList<>();
    addRectBoxes(tabBack, tabX - bw, tabBottom - bw, tabX + tabWidth + bw, tabTop + bw,
        tabFrontZ - 0.2f, tabFrontZ + 0.15f, cornerStyle);
    buf.begin(GL11.GL_QUADS, DefaultVertexFormats.BLOCK);
    RenderHelper.addBoxesToBufferLit(tabBack, buf, 0.55f, 0.56f, 0.58f, 1.0f, 0, 0, 0,
        worldSkyLight, worldBlockLight);
    tess.draw();

    float textCenterX = tabX + tabWidth / 2.0f;
    float textCenterY = tabBottom + tabHeight / 2.0f;
    int tabTextColor = tabColor.isLight() ? LEGEND_DARK : LEGEND_WHITE;
    GlStateManager.depthMask(false);
    GuideSignFontRenderer.drawString(tabText, textCenterX - tabTextW / 2.0f, textCenterY,
        tabFaceZ - 0.1f, EXIT_TAB_CAP_HEIGHT, tabTextColor, worldSkyLight, worldBlockLight);
    GlStateManager.depthMask(true);

    // Restore the white-pixel binding for subsequent untextured geometry passes.
    Minecraft.getMinecraft().getTextureManager().bindTexture(WHITE_TEXTURE);
  }

  private void renderRow(GuideSignRow row, float startX, float topY, float rowHeight,
      float faceZ, int legendTextColor, float legendR, float legendG, float legendB) {
    float curX = startX;

    // Lightmap is baked per-vertex; sub-element renderers use worldSkyLight / worldBlockLight.
    for (GuideSignElement elem : row.getElements()) {
      switch (elem.getType()) {
        case GuideSignElement.TYPE_TEXT:
          renderTextElement(elem, curX, topY, rowHeight, faceZ, legendTextColor);
          curX += getTextWidth(elem) + ELEMENT_SPACING;
          break;
        case GuideSignElement.TYPE_SHIELD:
          renderShieldElement(elem, curX, topY, rowHeight, faceZ, legendTextColor);
          curX += getShieldWidth(elem) + ELEMENT_SPACING;
          break;
        case GuideSignElement.TYPE_ARROW:
          renderArrowElement(elem, curX, topY, rowHeight, faceZ);
          curX += ARROW_SIZE + ELEMENT_SPACING;
          break;
        case GuideSignElement.TYPE_DIVIDER:
          renderDividerElement(curX, topY, rowHeight, faceZ, legendR, legendG, legendB);
          curX += DIVIDER_ELEMENT_WIDTH + ELEMENT_SPACING;
          break;
        case GuideSignElement.TYPE_SPACING:
          curX += elem.getSpacingWidth();
          break;
      }
    }
  }

  private void renderTextElement(GuideSignElement elem,
      float x, float topY, float rowHeight, float faceZ, int color) {
    String text = elem.getText();
    if (text == null || text.isEmpty()) {
      return;
    }

    float capPx = TEXT_CAP_HEIGHT * elem.getTextScale();
    float centerY = topY - rowHeight / 2.0f;

    GlStateManager.depthMask(false);
    GuideSignFontRenderer.drawString(text, x, centerY, faceZ - 0.2f, capPx, color,
        worldSkyLight, worldBlockLight);
    GlStateManager.depthMask(true);

    // Restore white-pixel binding for subsequent untextured geometry passes.
    Minecraft.getMinecraft().getTextureManager().bindTexture(WHITE_TEXTURE);
  }

  private void renderDividerElement(float x, float topY, float rowHeight, float faceZ,
      float legendR, float legendG, float legendB) {
    Tessellator tess = Tessellator.getInstance();
    BufferBuilder buf = tess.getBuffer();
    List<RenderHelper.Box> bar = new ArrayList<>();
    bar.add(new RenderHelper.Box(
        new float[]{x, topY - rowHeight, faceZ - 0.2f},
        new float[]{x + DIVIDER_ELEMENT_WIDTH, topY, faceZ - 0.1f}));
    buf.begin(GL11.GL_QUADS, DefaultVertexFormats.BLOCK);
    RenderHelper.addBoxesToBufferLit(bar, buf, legendR, legendG, legendB, 1.0f, 0, 0, 0,
        worldSkyLight, worldBlockLight);
    tess.draw();
  }

  private void renderShieldElement(GuideSignElement elem,
      float x, float topY, float rowHeight, float faceZ, int legendTextColor) {
    GuideSignShieldType shieldType = elem.getGuideSignShieldType();
    float[] uv = GuideSignAtlas.getShieldUV(shieldType);

    // The cell may be wider than the shield when a banner word above it is wider;
    // center the shield (and its banner) within the full cell width.
    float cellWidth = getShieldWidth(elem);
    float shieldCenterX = x + cellWidth / 2.0f;
    float shieldCenterY = topY - rowHeight / 2.0f;
    float halfSize = SHIELD_SIZE / 2.0f;

    Minecraft.getMinecraft().getTextureManager().bindTexture(GuideSignAtlas.ATLAS_TEXTURE);

    Tessellator tess = Tessellator.getInstance();
    BufferBuilder buf = tess.getBuffer();
    buf.begin(GL11.GL_QUADS, DefaultVertexFormats.BLOCK);

    float qLeft = shieldCenterX - halfSize;
    float qRight = shieldCenterX + halfSize;
    float qTop = shieldCenterY + halfSize;
    float qBottom = shieldCenterY - halfSize;
    float qZ = faceZ - 0.3f;

    // u0 on the left edge: the enclosing transform already un-mirrors pixel space.
    atlasVertex(buf, qRight, qTop, qZ, uv[2], uv[1]);
    atlasVertex(buf, qLeft, qTop, qZ, uv[0], uv[1]);
    atlasVertex(buf, qLeft, qBottom, qZ, uv[0], uv[3]);
    atlasVertex(buf, qRight, qBottom, qZ, uv[2], uv[3]);

    tess.draw();

    String routeNum = elem.getRouteNumber();
    if (routeNum != null && !routeNum.isEmpty()) {
      // Shrink to fit: long route numbers scale down so they stay inside the
      // shield's legend area instead of spilling over its edges.
      float capPx = SHIELD_SIZE * ROUTE_CAP_FRACTION;
      float avail = SHIELD_SIZE * ROUTE_TEXT_MAX_FRACTION;
      float w = GuideSignFontRenderer.getStringWidth(routeNum, capPx);
      if (w > avail) {
        capPx *= avail / w;
        w = avail;
      }
      GlStateManager.depthMask(false);
      GuideSignFontRenderer.drawString(routeNum, shieldCenterX - w / 2.0f, shieldCenterY,
          faceZ - 0.4f, capPx, shieldType.getRouteTextColor(),
          worldSkyLight, worldBlockLight);
      GlStateManager.depthMask(true);
    }

    String bannerText = elem.getGuideSignBannerType().getBannerText();
    if (!bannerText.isEmpty()) {
      // Banner is centered in the row's reserved banner zone, which sits ABOVE
      // the row's content band (between topY and topY + BANNER_AREA_HEIGHT).
      float bannerY = topY + BANNER_AREA_HEIGHT / 2.0f;
      float capPx = SHIELD_SIZE * BANNER_CAP_FRACTION;
      float w = GuideSignFontRenderer.getStringWidth(bannerText, capPx);
      GlStateManager.depthMask(false);
      GuideSignFontRenderer.drawString(bannerText, shieldCenterX - w / 2.0f, bannerY,
          faceZ - 0.4f, capPx, legendTextColor, worldSkyLight, worldBlockLight);
      GlStateManager.depthMask(true);
    }

    // Restore white-pixel binding for subsequent untextured geometry passes.
    Minecraft.getMinecraft().getTextureManager().bindTexture(WHITE_TEXTURE);
  }

  private void renderArrowElement(GuideSignElement elem, float x, float topY,
      float rowHeight, float faceZ) {
    GuideSignArrowType arrowType = elem.getGuideSignArrowType();
    float[] uv = GuideSignAtlas.getArrowUV(arrowType);

    float centerX = x + ARROW_SIZE / 2.0f;
    float centerY = topY - rowHeight / 2.0f;
    float halfSize = ARROW_SIZE / 2.0f;

    Minecraft.getMinecraft().getTextureManager().bindTexture(GuideSignAtlas.ATLAS_TEXTURE);

    Tessellator tess = Tessellator.getInstance();
    BufferBuilder buf = tess.getBuffer();
    buf.begin(GL11.GL_QUADS, DefaultVertexFormats.BLOCK);

    float qZ = faceZ - 0.3f;
    // u0 on the left edge: the enclosing transform already un-mirrors pixel space.
    atlasVertex(buf, centerX + halfSize, centerY + halfSize, qZ, uv[2], uv[1]);
    atlasVertex(buf, centerX - halfSize, centerY + halfSize, qZ, uv[0], uv[1]);
    atlasVertex(buf, centerX - halfSize, centerY - halfSize, qZ, uv[0], uv[3]);
    atlasVertex(buf, centerX + halfSize, centerY - halfSize, qZ, uv[2], uv[3]);

    tess.draw();
    // Restore white-pixel binding for subsequent untextured geometry passes.
    Minecraft.getMinecraft().getTextureManager().bindTexture(WHITE_TEXTURE);
  }

  private void atlasVertex(BufferBuilder buf, float x, float y, float z, float u, float v) {
    buf.pos(x, y, z).color(1.0f, 1.0f, 1.0f, 1.0f).tex(u, v)
        .lightmap(worldSkyLight, worldBlockLight).endVertex();
  }

  private void renderPanelDivider(float signLeft, float y, float signWidth,
      float faceZ, int borderWidth, float legendR, float legendG, float legendB) {
    Tessellator tess = Tessellator.getInstance();
    BufferBuilder buf = tess.getBuffer();
    List<RenderHelper.Box> divider = new ArrayList<>();
    float borderInset = borderWidth > 0 ? borderWidth * BORDER_INSET : 0;
    float inset = PANEL_PADDING_SIDE + borderInset;
    float half = PANEL_DIVIDER_THICKNESS / 2.0f;
    divider.add(new RenderHelper.Box(
        new float[]{signLeft + inset, y - half, faceZ - 0.25f},
        new float[]{signLeft + signWidth - inset, y + half, faceZ - 0.05f}));
    buf.begin(GL11.GL_QUADS, DefaultVertexFormats.BLOCK);
    RenderHelper.addBoxesToBufferLit(divider, buf, legendR, legendG, legendB, 1.0f, 0, 0, 0,
        worldSkyLight, worldBlockLight);
    tess.draw();
  }

  private void renderPost(PostType postType, float signLeft, float signBottom,
      float signWidth, float faceZ) {
    Tessellator tess = Tessellator.getInstance();
    BufferBuilder buf = tess.getBuffer();

    float postBottom = signBottom - 48.0f;
    float postTop = signBottom;
    float postFrontZ = faceZ + SIGN_DEPTH / 2.0f - POST_DEPTH / 2.0f;
    float postBackZ = postFrontZ + POST_DEPTH;

    List<RenderHelper.Box> posts = new ArrayList<>();

    switch (postType) {
      case OVERHEAD:
        break;
      case LEFT:
        posts.add(new RenderHelper.Box(
            new float[]{signLeft + 2.0f, postBottom, postFrontZ},
            new float[]{signLeft + 2.0f + POST_WIDTH, postTop, postBackZ}));
        break;
      case RIGHT:
        float rX = signLeft + signWidth - 2.0f - POST_WIDTH;
        posts.add(new RenderHelper.Box(
            new float[]{rX, postBottom, postFrontZ},
            new float[]{rX + POST_WIDTH, postTop, postBackZ}));
        break;
      case CENTER:
        float cX = signLeft + signWidth / 2.0f - POST_WIDTH / 2.0f;
        posts.add(new RenderHelper.Box(
            new float[]{cX, postBottom, postFrontZ},
            new float[]{cX + POST_WIDTH, postTop, postBackZ}));
        break;
      case RURAL:
        float r1 = signLeft + signWidth * 0.25f - POST_WIDTH / 2.0f;
        float r2 = signLeft + signWidth * 0.75f - POST_WIDTH / 2.0f;
        posts.add(new RenderHelper.Box(
            new float[]{r1, postBottom, postFrontZ},
            new float[]{r1 + POST_WIDTH, postTop, postBackZ}));
        posts.add(new RenderHelper.Box(
            new float[]{r2, postBottom, postFrontZ},
            new float[]{r2 + POST_WIDTH, postTop, postBackZ}));
        break;
    }

    if (!posts.isEmpty()) {
      buf.begin(GL11.GL_QUADS, DefaultVertexFormats.BLOCK);
      RenderHelper.addBoxesToBufferLit(posts, buf, 0.45f, 0.45f, 0.47f, 1.0f, 0, 0, 0,
          worldSkyLight, worldBlockLight);
      tess.draw();
    }
  }

  /**
   * Height of one row's content band. Text rows grow/shrink with their largest
   * text scale (plus leading) so scaled text never overflows its band; shield and
   * arrow rows are at least as tall as the graphic.
   */
  private float computeRowHeight(GuideSignRow row) {
    if (row.getElements().isEmpty()) {
      return ROW_HEIGHT;
    }
    float h = ROW_MIN_HEIGHT;
    for (GuideSignElement elem : row.getElements()) {
      switch (elem.getType()) {
        case GuideSignElement.TYPE_TEXT:
          float th = TEXT_CAP_HEIGHT * elem.getTextScale() * TEXT_VISUAL_FACTOR + TEXT_LEADING;
          if (th > h) {
            h = th;
          }
          break;
        case GuideSignElement.TYPE_SHIELD:
          if (SHIELD_SIZE > h) {
            h = SHIELD_SIZE;
          }
          break;
        case GuideSignElement.TYPE_ARROW:
          if (ARROW_SIZE > h) {
            h = ARROW_SIZE;
          }
          break;
        default:
          break;
      }
    }
    return h;
  }

  private float computeTotalSignHeight(List<GuideSignPanel> panels, GuideSignData data) {
    float h = PANEL_PADDING_TOP + PANEL_PADDING_BOTTOM;
    for (int pi = 0; pi < panels.size(); pi++) {
      GuideSignPanel panel = panels.get(pi);
      for (GuideSignRow row : panel.getRows()) {
        h += computeRowHeight(row) + ROW_SPACING + row.getVerticalSpacing();
        if (rowHasBanner(row)) {
          h += BANNER_AREA_HEIGHT;
        }
      }
      if (!panel.getRows().isEmpty()) {
        h -= ROW_SPACING;
      }
      if (pi < panels.size() - 1) {
        h += PANEL_GAP;
      }
    }
    float borderInset = data.getBorderWidth() > 0 ? data.getBorderWidth() * BORDER_INSET : 0;
    h += 2 * borderInset;
    return Math.max(16.0f, h);
  }

  private boolean rowHasBanner(GuideSignRow row) {
    for (GuideSignElement e : row.getElements()) {
      if (e.getType() == GuideSignElement.TYPE_SHIELD
          && !e.getGuideSignBannerType().getBannerText().isEmpty()) {
        return true;
      }
    }
    return false;
  }

  private float computeTotalSignWidth(List<GuideSignPanel> panels, GuideSignData data) {
    float maxRowW = 0;
    for (GuideSignPanel panel : panels) {
      for (GuideSignRow row : panel.getRows()) {
        float rw = computeRowWidth(row, data);
        if (rw > maxRowW) {
          maxRowW = rw;
        }
      }
    }
    float width = maxRowW + PANEL_PADDING_SIDE * 2;
    // Only the first panel's exit tab renders; the sign body just needs to be wide
    // enough that the tab (which sits between the sign's side edges) fits.
    if (!panels.isEmpty() && panels.get(0).hasExitTab()) {
      String tabText = panels.get(0).getExitTab().getText();
      if (tabText == null || tabText.isEmpty()) {
        tabText = "EXIT";
      }
      float tabW = GuideSignFontRenderer.getStringWidth(tabText, EXIT_TAB_CAP_HEIGHT)
          + EXIT_TAB_PADDING * 2;
      float tabNeed = tabW + PANEL_PADDING_SIDE * 2;
      if (tabNeed > width) {
        width = tabNeed;
      }
    }
    float borderInset = data.getBorderWidth() > 0 ? data.getBorderWidth() * BORDER_INSET : 0;
    return Math.max((float) data.getMinWidth(), width + 2 * borderInset);
  }

  private float computeRowWidth(GuideSignRow row, GuideSignData data) {
    float w = 0;
    for (int i = 0; i < row.getElements().size(); i++) {
      if (i > 0) {
        w += ELEMENT_SPACING;
      }
      GuideSignElement elem = row.getElements().get(i);
      switch (elem.getType()) {
        case GuideSignElement.TYPE_TEXT:
          w += getTextWidth(elem);
          break;
        case GuideSignElement.TYPE_SHIELD:
          w += getShieldWidth(elem);
          break;
        case GuideSignElement.TYPE_ARROW:
          w += ARROW_SIZE;
          break;
        case GuideSignElement.TYPE_DIVIDER:
          w += DIVIDER_ELEMENT_WIDTH;
          break;
        case GuideSignElement.TYPE_SPACING:
          w += elem.getSpacingWidth();
          break;
      }
    }
    return w;
  }

  private float getTextWidth(GuideSignElement elem) {
    String text = elem.getText();
    if (text == null || text.isEmpty()) {
      return 0;
    }
    return GuideSignFontRenderer.getStringWidth(text, TEXT_CAP_HEIGHT * elem.getTextScale());
  }

  private float getShieldWidth(GuideSignElement elem) {
    float w = SHIELD_SIZE;
    String bannerText = elem.getGuideSignBannerType().getBannerText();
    if (!bannerText.isEmpty()) {
      // Must match the cap height renderShieldElement actually draws the banner at,
      // or the reserved cell width and the drawn banner disagree.
      float bannerW = GuideSignFontRenderer.getStringWidth(bannerText,
          SHIELD_SIZE * BANNER_CAP_FRACTION) + BANNER_CELL_PADDING;
      w = Math.max(w, bannerW);
    }
    return w;
  }

  public static void cleanupDisplayList(BlockPos pos) {
    // Reserved for future display list caching optimization
  }
}

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
import java.util.ArrayList;
import java.util.List;
import net.minecraft.block.BlockHorizontal;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import org.lwjgl.opengl.GL11;

public class TileEntityDynamicGuideSignRenderer
    extends TileEntitySpecialRenderer<TileEntityDynamicGuideSign> {

  private static final float SIGN_DEPTH = 2.0f;
  private static final float BORDER_INSET = 1.5f;
  private static final float CX = 8.0f;
  private static final float CY = 8.0f;

  private static final float PANEL_PADDING_TOP = 4.0f;
  private static final float PANEL_PADDING_BOTTOM = 4.0f;
  private static final float PANEL_PADDING_SIDE = 6.0f;
  private static final float ROW_HEIGHT = 12.0f;
  private static final float ROW_SPACING = 2.0f;
  private static final float ELEMENT_SPACING = 3.0f;
  private static final float SHIELD_SIZE = 10.0f;
  private static final float ARROW_SIZE = 10.0f;
  private static final float EXIT_TAB_HEIGHT = 10.0f;
  private static final float EXIT_TAB_PADDING = 2.0f;
  private static final float PANEL_GAP = 1.0f;

  private static final float POST_WIDTH = 3.0f;
  private static final float POST_DEPTH = 2.0f;

  private static final float TEXT_BASE_SCALE = 0.8f;

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

    renderSign(data);

    GlStateManager.popMatrix();
  }

  private void renderSign(GuideSignData data) {
    GuideSignColor signColor = data.getSignColor();
    int borderWidth = data.getBorderWidth();
    List<GuideSignPanel> panels = data.getPanels();

    float totalSignHeight = computeTotalSignHeight(panels);
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
    GlStateManager.disableTexture2D();
    GL11.glDisable(GL11.GL_TEXTURE_2D);
    GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
    GL11.glColor4f(1.0f, 1.0f, 1.0f, 1.0f);

    renderSignBackground(signLeft, signBottom, totalSignWidth, totalSignHeight,
        faceZ, signColor, borderWidth);

    float panelY = signTop - PANEL_PADDING_TOP;
    if (borderWidth > 0) {
      panelY -= borderWidth * BORDER_INSET;
    }

    for (int pi = 0; pi < panels.size(); pi++) {
      GuideSignPanel panel = panels.get(pi);

      if (panel.hasExitTab()) {
        renderExitTab(panel.getExitTab(), signLeft, panelY, totalSignWidth,
            faceZ, signColor, borderWidth);
      }

      for (GuideSignRow row : panel.getRows()) {
        panelY -= row.getVerticalSpacing();

        float rowWidth = computeRowWidth(row, data);
        float rowX = CX - rowWidth / 2.0f;

        renderRow(row, rowX, panelY, faceZ, data);

        panelY -= ROW_HEIGHT + ROW_SPACING;
      }

      if (pi < panels.size() - 1) {
        panelY -= PANEL_GAP;
        renderPanelDivider(signLeft, panelY + PANEL_GAP / 2, totalSignWidth,
            faceZ, borderWidth);
      }
    }

    renderPost(data.getPostType(), signLeft, signBottom, totalSignWidth, faceZ);

    GlStateManager.enableTexture2D();
    GL11.glEnable(GL11.GL_TEXTURE_2D);
    GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
    GL11.glColor4f(1.0f, 1.0f, 1.0f, 1.0f);
    GlStateManager.enableLighting();
    GL11.glEnable(GL11.GL_LIGHTING);
    GlStateManager.enableCull();
    GlStateManager.disableBlend();
  }

  private void renderSignBackground(float left, float bottom, float width, float height,
      float faceZ, GuideSignColor color, int borderWidth) {
    Tessellator tess = Tessellator.getInstance();
    BufferBuilder buf = tess.getBuffer();

    float frontZ = faceZ + SIGN_DEPTH;

    if (borderWidth > 0) {
      float bw = borderWidth * BORDER_INSET;
      List<RenderHelper.Box> border = new ArrayList<>();
      border.add(new RenderHelper.Box(
          new float[]{left - bw, bottom - bw, faceZ},
          new float[]{left + width + bw, bottom + height + bw, frontZ}));

      buf.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
      RenderHelper.addBoxesToBuffer(border, buf, 0.92f, 0.92f, 0.90f, 1.0f, 0, 0, 0);
      tess.draw();
    }

    float inset = borderWidth > 0 ? borderWidth * BORDER_INSET : 0;
    List<RenderHelper.Box> face = new ArrayList<>();
    face.add(new RenderHelper.Box(
        new float[]{left + inset, bottom + inset, faceZ + 0.1f},
        new float[]{left + width - inset, bottom + height - inset, frontZ + 0.1f}));

    buf.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
    RenderHelper.addBoxesToBuffer(face, buf,
        color.getRed(), color.getGreen(), color.getBlue(), 1.0f, 0, 0, 0);
    tess.draw();
  }

  private void renderExitTab(ExitTabData tab, float signLeft, float panelTopY,
      float signWidth, float faceZ, GuideSignColor signColor, int borderWidth) {
    FontRenderer fr = Minecraft.getMinecraft().fontRenderer;
    String tabText = tab.getText();
    if (tabText == null || tabText.isEmpty()) {
      tabText = "EXIT";
    }

    float tabWidth = fr.getStringWidth(tabText) * TEXT_BASE_SCALE * 0.7f + EXIT_TAB_PADDING * 2;
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

    float tabBottom = panelTopY;
    float tabTop = tabBottom + tabHeight;
    float bw = borderWidth > 0 ? borderWidth * BORDER_INSET : 0;

    Tessellator tess = Tessellator.getInstance();
    BufferBuilder buf = tess.getBuffer();

    float frontZ = faceZ + SIGN_DEPTH;

    if (borderWidth > 0) {
      List<RenderHelper.Box> tabBorder = new ArrayList<>();
      tabBorder.add(new RenderHelper.Box(
          new float[]{tabX - bw, tabBottom - 0.5f, faceZ},
          new float[]{tabX + tabWidth + bw, tabTop + bw, frontZ}));
      buf.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
      RenderHelper.addBoxesToBuffer(tabBorder, buf, 0.92f, 0.92f, 0.90f, 1.0f, 0, 0, 0);
      tess.draw();
    }

    GuideSignColor tabColor = tab.getGuideSignColor();
    List<RenderHelper.Box> tabBg = new ArrayList<>();
    tabBg.add(new RenderHelper.Box(
        new float[]{tabX, tabBottom, faceZ + 0.1f},
        new float[]{tabX + tabWidth, tabTop, frontZ + 0.1f}));
    buf.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
    RenderHelper.addBoxesToBuffer(tabBg, buf,
        tabColor.getRed(), tabColor.getGreen(), tabColor.getBlue(), 1.0f, 0, 0, 0);
    tess.draw();

    GlStateManager.enableTexture2D();
    float prevBX = OpenGlHelper.lastBrightnessX;
    float prevBY = OpenGlHelper.lastBrightnessY;
    OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 240f, 240f);

    GlStateManager.pushMatrix();
    float textCenterX = tabX + tabWidth / 2.0f;
    float textCenterY = tabBottom + tabHeight / 2.0f;
    GlStateManager.translate(textCenterX, textCenterY, faceZ - 0.2f);
    GlStateManager.rotate(180, 0, 1, 0);
    float tabTextScale = TEXT_BASE_SCALE * 0.7f;
    GlStateManager.scale(tabTextScale, -tabTextScale, tabTextScale);
    GlStateManager.depthMask(false);

    int textW = fr.getStringWidth(tabText);
    fr.drawString(tabText, -textW / 2, -fr.FONT_HEIGHT / 2, 0xFFFFFF);

    GlStateManager.depthMask(true);
    GlStateManager.popMatrix();

    OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, prevBX, prevBY);
    GlStateManager.disableTexture2D();
    GL11.glDisable(GL11.GL_TEXTURE_2D);
  }

  private void renderRow(GuideSignRow row, float startX, float topY,
      float faceZ, GuideSignData data) {
    FontRenderer fr = Minecraft.getMinecraft().fontRenderer;
    float curX = startX;

    float prevBX = OpenGlHelper.lastBrightnessX;
    float prevBY = OpenGlHelper.lastBrightnessY;
    OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 240f, 240f);

    for (GuideSignElement elem : row.getElements()) {
      switch (elem.getType()) {
        case GuideSignElement.TYPE_TEXT:
          renderTextElement(fr, elem, curX, topY, faceZ);
          curX += getTextWidth(fr, elem) + ELEMENT_SPACING;
          break;
        case GuideSignElement.TYPE_SHIELD:
          renderShieldElement(fr, elem, curX, topY, faceZ);
          curX += getShieldWidth(elem) + ELEMENT_SPACING;
          break;
        case GuideSignElement.TYPE_ARROW:
          renderArrowElement(elem, curX, topY, faceZ);
          curX += ARROW_SIZE + ELEMENT_SPACING;
          break;
        case GuideSignElement.TYPE_DIVIDER:
          break;
        case GuideSignElement.TYPE_SPACING:
          curX += elem.getSpacingWidth();
          break;
      }
    }

    OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, prevBX, prevBY);
  }

  private void renderTextElement(FontRenderer fr, GuideSignElement elem,
      float x, float topY, float faceZ) {
    String text = elem.getText();
    if (text == null || text.isEmpty()) return;

    GlStateManager.enableTexture2D();
    GlStateManager.pushMatrix();

    float scale = TEXT_BASE_SCALE * elem.getTextScale();
    float textWidth = fr.getStringWidth(text) * scale;
    float centerX = x + textWidth / 2.0f;
    float centerY = topY - ROW_HEIGHT / 2.0f;

    GlStateManager.translate(centerX, centerY, faceZ - 0.2f);
    GlStateManager.rotate(180, 0, 1, 0);
    GlStateManager.scale(scale, -scale, scale);
    GlStateManager.depthMask(false);

    int textW = fr.getStringWidth(text);
    fr.drawString(text, -textW / 2, -fr.FONT_HEIGHT / 2, 0xFFFFFF);

    GlStateManager.depthMask(true);
    GlStateManager.popMatrix();
    GlStateManager.disableTexture2D();
    GL11.glDisable(GL11.GL_TEXTURE_2D);
  }

  private void renderShieldElement(FontRenderer fr, GuideSignElement elem,
      float x, float topY, float faceZ) {
    GuideSignShieldType shieldType = elem.getGuideSignShieldType();
    float[] uv = GuideSignAtlas.getShieldUV(shieldType);

    float shieldCenterX = x + SHIELD_SIZE / 2.0f;
    float shieldCenterY = topY - ROW_HEIGHT / 2.0f;
    float halfSize = SHIELD_SIZE / 2.0f;

    GlStateManager.enableTexture2D();
    Minecraft.getMinecraft().getTextureManager().bindTexture(GuideSignAtlas.ATLAS_TEXTURE);

    Tessellator tess = Tessellator.getInstance();
    BufferBuilder buf = tess.getBuffer();
    buf.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX);

    float qLeft = shieldCenterX - halfSize;
    float qRight = shieldCenterX + halfSize;
    float qTop = shieldCenterY + halfSize;
    float qBottom = shieldCenterY - halfSize;
    float qZ = faceZ - 0.3f;

    buf.pos(qRight, qTop, qZ).tex(uv[0], uv[1]).endVertex();
    buf.pos(qLeft, qTop, qZ).tex(uv[2], uv[1]).endVertex();
    buf.pos(qLeft, qBottom, qZ).tex(uv[2], uv[3]).endVertex();
    buf.pos(qRight, qBottom, qZ).tex(uv[0], uv[3]).endVertex();

    tess.draw();

    String routeNum = elem.getRouteNumber();
    if (routeNum != null && !routeNum.isEmpty()) {
      GlStateManager.pushMatrix();
      GlStateManager.translate(shieldCenterX, shieldCenterY, faceZ - 0.4f);
      GlStateManager.rotate(180, 0, 1, 0);
      float numScale = TEXT_BASE_SCALE * 0.7f;
      if (routeNum.length() > 3) {
        numScale *= 0.75f;
      }
      GlStateManager.scale(numScale, -numScale, numScale);
      GlStateManager.depthMask(false);

      int textW = fr.getStringWidth(routeNum);
      fr.drawString(routeNum, -textW / 2, -fr.FONT_HEIGHT / 2, 0xFFFFFF);

      GlStateManager.depthMask(true);
      GlStateManager.popMatrix();
    }

    String bannerText = elem.getGuideSignBannerType().getBannerText();
    if (!bannerText.isEmpty()) {
      GlStateManager.pushMatrix();
      float bannerY = shieldCenterY + halfSize + 3.0f;
      GlStateManager.translate(shieldCenterX, bannerY, faceZ - 0.4f);
      GlStateManager.rotate(180, 0, 1, 0);
      float bannerScale = TEXT_BASE_SCALE * 0.45f;
      GlStateManager.scale(bannerScale, -bannerScale, bannerScale);
      GlStateManager.depthMask(false);

      int bw = fr.getStringWidth(bannerText);
      fr.drawString(bannerText, -bw / 2, -fr.FONT_HEIGHT / 2, 0xFFFFFF);

      GlStateManager.depthMask(true);
      GlStateManager.popMatrix();
    }

    GlStateManager.disableTexture2D();
    GL11.glDisable(GL11.GL_TEXTURE_2D);
  }

  private void renderArrowElement(GuideSignElement elem, float x, float topY, float faceZ) {
    GuideSignArrowType arrowType = elem.getGuideSignArrowType();
    float[] uv = GuideSignAtlas.getArrowUV(arrowType);

    float centerX = x + ARROW_SIZE / 2.0f;
    float centerY = topY - ROW_HEIGHT / 2.0f;
    float halfSize = ARROW_SIZE / 2.0f;

    GlStateManager.enableTexture2D();
    Minecraft.getMinecraft().getTextureManager().bindTexture(GuideSignAtlas.ATLAS_TEXTURE);

    Tessellator tess = Tessellator.getInstance();
    BufferBuilder buf = tess.getBuffer();
    buf.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX);

    float qZ = faceZ - 0.3f;
    buf.pos(centerX + halfSize, centerY + halfSize, qZ).tex(uv[0], uv[1]).endVertex();
    buf.pos(centerX - halfSize, centerY + halfSize, qZ).tex(uv[2], uv[1]).endVertex();
    buf.pos(centerX - halfSize, centerY - halfSize, qZ).tex(uv[2], uv[3]).endVertex();
    buf.pos(centerX + halfSize, centerY - halfSize, qZ).tex(uv[0], uv[3]).endVertex();

    tess.draw();
    GlStateManager.disableTexture2D();
    GL11.glDisable(GL11.GL_TEXTURE_2D);
  }

  private void renderPanelDivider(float signLeft, float y, float signWidth,
      float faceZ, int borderWidth) {
    Tessellator tess = Tessellator.getInstance();
    BufferBuilder buf = tess.getBuffer();
    List<RenderHelper.Box> divider = new ArrayList<>();
    float inset = PANEL_PADDING_SIDE;
    divider.add(new RenderHelper.Box(
        new float[]{signLeft + inset, y - 0.3f, faceZ - 0.15f},
        new float[]{signLeft + signWidth - inset, y + 0.3f, faceZ - 0.05f}));
    buf.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
    RenderHelper.addBoxesToBuffer(divider, buf, 0.92f, 0.92f, 0.90f, 1.0f, 0, 0, 0);
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
      buf.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
      RenderHelper.addBoxesToBuffer(posts, buf, 0.45f, 0.45f, 0.47f, 1.0f, 0, 0, 0);
      tess.draw();
    }
  }

  private float computeTotalSignHeight(List<GuideSignPanel> panels) {
    float h = PANEL_PADDING_TOP + PANEL_PADDING_BOTTOM;
    for (int pi = 0; pi < panels.size(); pi++) {
      GuideSignPanel panel = panels.get(pi);
      for (GuideSignRow row : panel.getRows()) {
        h += ROW_HEIGHT + ROW_SPACING + row.getVerticalSpacing();
      }
      if (!panel.getRows().isEmpty()) {
        h -= ROW_SPACING;
      }
      if (pi < panels.size() - 1) {
        h += PANEL_GAP;
      }
    }
    return Math.max(16.0f, h);
  }

  private float computeTotalSignWidth(List<GuideSignPanel> panels, GuideSignData data) {
    float maxRowW = 0;
    for (GuideSignPanel panel : panels) {
      for (GuideSignRow row : panel.getRows()) {
        float rw = computeRowWidth(row, data);
        if (rw > maxRowW) maxRowW = rw;
      }
      if (panel.hasExitTab()) {
        FontRenderer fr = Minecraft.getMinecraft().fontRenderer;
        String tabText = panel.getExitTab().getText();
        if (tabText == null || tabText.isEmpty()) tabText = "EXIT";
        float tabW = fr.getStringWidth(tabText) * TEXT_BASE_SCALE * 0.7f + EXIT_TAB_PADDING * 2;
        if (tabW + PANEL_PADDING_SIDE * 2 > maxRowW) {
          maxRowW = tabW + PANEL_PADDING_SIDE * 2;
        }
      }
    }
    return Math.max(32.0f, maxRowW + PANEL_PADDING_SIDE * 2);
  }

  private float computeRowWidth(GuideSignRow row, GuideSignData data) {
    FontRenderer fr = Minecraft.getMinecraft().fontRenderer;
    float w = 0;
    for (int i = 0; i < row.getElements().size(); i++) {
      if (i > 0) w += ELEMENT_SPACING;
      GuideSignElement elem = row.getElements().get(i);
      switch (elem.getType()) {
        case GuideSignElement.TYPE_TEXT:
          w += getTextWidth(fr, elem);
          break;
        case GuideSignElement.TYPE_SHIELD:
          w += getShieldWidth(elem);
          break;
        case GuideSignElement.TYPE_ARROW:
          w += ARROW_SIZE;
          break;
        case GuideSignElement.TYPE_DIVIDER:
          break;
        case GuideSignElement.TYPE_SPACING:
          w += elem.getSpacingWidth();
          break;
      }
    }
    return w;
  }

  private float getTextWidth(FontRenderer fr, GuideSignElement elem) {
    String text = elem.getText();
    if (text == null || text.isEmpty()) return 0;
    return fr.getStringWidth(text) * TEXT_BASE_SCALE * elem.getTextScale();
  }

  private float getShieldWidth(GuideSignElement elem) {
    float w = SHIELD_SIZE;
    String bannerText = elem.getGuideSignBannerType().getBannerText();
    if (!bannerText.isEmpty()) {
      FontRenderer fr = Minecraft.getMinecraft().fontRenderer;
      float bannerW = fr.getStringWidth(bannerText) * TEXT_BASE_SCALE * 0.45f;
      w = Math.max(w, bannerW);
    }
    return w;
  }

  public static void cleanupDisplayList(BlockPos pos) {
    // Reserved for future display list caching optimization
  }
}

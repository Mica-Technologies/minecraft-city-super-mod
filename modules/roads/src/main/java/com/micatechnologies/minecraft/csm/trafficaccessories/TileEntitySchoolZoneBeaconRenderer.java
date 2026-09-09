package com.micatechnologies.minecraft.csm.trafficaccessories;

import com.micatechnologies.minecraft.csm.codeutils.CsmFontRenderer;
import com.micatechnologies.minecraft.csm.codeutils.CsmRenderUtils;
import com.micatechnologies.minecraft.csm.codeutils.RenderHelper;
import com.micatechnologies.minecraft.csm.trafficsignals.logic.TrafficSignalBulbColor;
import com.micatechnologies.minecraft.csm.trafficsignals.logic.TrafficSignalBulbStyle;
import com.micatechnologies.minecraft.csm.trafficsignals.logic.TrafficSignalBulbType;
import com.micatechnologies.minecraft.csm.trafficsignals.logic.TrafficSignalFlashPattern;
import com.micatechnologies.minecraft.csm.trafficsignals.logic.TrafficSignalTextureMap;
import com.micatechnologies.minecraft.csm.trafficsignals.logic.TrafficSignalTextureMap.TextureInfo;
import com.micatechnologies.minecraft.csm.trafficsignals.logic.TrafficSignalVertexData;
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
import org.lwjgl.opengl.GL11;

/**
 * Renderer for {@link BlockSchoolZoneBeacon}: a white SPEED LIMIT panel under a fluorescent
 * yellow-green SCHOOL banner, with one or two amber beacon bars.
 *
 * <p>Sized to the roadside sign family rather than to the electronic speed limit signs — the
 * pole-mount sign's panel is wider than a block, which reads as a gantry sign rather than
 * something on a post. This one is 12 x 22, close to {@code metal_sign_ultratall}'s
 * proportions, and draws no post of its own so it mounts on whatever pole is behind it.</p>
 *
 * <p>The beacons are real signal sections, not painted-on lamps: body, door, circle visor and
 * bulb all come from {@code TrafficSignalVertexData} and the signal bulb atlas, the way the
 * portable speed limit sign's flashers already do. That is what gives them a housing you can
 * see from the side, and it is why a beacon can be 8 inch or 12 inch — the geometry for both
 * already exists.</p>
 *
 * <p>Paired beacons alternate on {@link TrafficSignalFlashPattern#OFF} and
 * {@link TrafficSignalFlashPattern#B} — the wig-wag pair the signal system already uses, so a
 * school zone beacon blinks at the same rate as everything else in the mod rather than to its
 * own private timer.</p>
 *
 * @author Mica Technologies
 * @since 2026.9
 */
public class TileEntitySchoolZoneBeaconRenderer
    extends TileEntitySpecialRenderer<TileEntitySchoolZoneBeacon> {

  // Panel — one block wide, a little under two tall, before scale. The width is the sign
  // family's full 16: at 12 the legend did not fit, and FLASHING ran off both edges.
  private static final float PANEL_W = 16.0f;
  private static final float PANEL_H = 30.0f;
  private static final float PANEL_D = 1.0f;
  private static final float PANEL_BORDER = 0.8f;

  // The S4-3 SCHOOL plaque is its own sign sitting on top of the S5-1: fluorescent yellow-green,
  // separated from the white panel below by a strip of the black border rather than butted
  // against it. Only the plaque is coloured -- the speed limit itself is a white regulatory sign.
  private static final float BANNER_H = 5.0f;
  private static final float BANNER_DIVIDER = 0.7f;

  // Corner rounding. A sign blank is stamped with a radius, and a hard 90-degree corner is
  // the single clearest tell that one was drawn rather than cut. Approximated as stepped bands
  // rather than as a real arc: adjacent boxes cannot z-fight, overlapping ones would.
  private static final float PANEL_RADIUS = 1.5f;
  private static final int ROUND_STEPS = 3;

  // Beacons: the gap between the sign's border and the beacon, and the bracket that spans it.
  private static final float BEACON_GAP = 2.0f;
  private static final float BEACON_PAIR_GAP = 0.5f;
  private static final float BRACKET_HALF_W = 1.1f;
  private static final float BRACKET_Z1 = 12.6f;
  private static final float BRACKET_Z2 = 14.6f;

  // Signal section geometry, shared with the traffic signal renderer so a school zone beacon
  // is the same object as a signal head's section rather than a lookalike.
  private static final ResourceLocation SIGNAL_ATLAS =
      new ResourceLocation("csm", "textures/blocks/trafficsignals/lights/atlas.png");
  private static final float VISOR_TINT_SCALE = 1.04f;
  private static final float VISOR_TINT_BASE = 0.01f;
  private static final float VISOR_TILT_DEGREES = 9.0f;
  private static final float VISOR_PIVOT_Z = 11.0f;
  private static final float VISOR_CENTER_X = 8.0f;
  private static final float VISOR_CENTER_Y = 6.0f;
  private static final float SECTION_8_INCH = 8.0f;
  private static final float SECTION_12_INCH = 12.0f;
  private static final int LIGHTMAP_FULLBRIGHT_SKY = 240;
  private static final int LIGHTMAP_FULLBRIGHT_BLOCK = 240;

  /** Pivot: the middle of the cell, pushed back so the panel's rear sits on the cell's back. */
  private static final float CX = 8.0f;
  private static final float CY = 8.0f;
  private static final float CZ = 15.0f;

  // Face colours. The body is white because a speed limit is a regulatory sign; the SCHOOL
  // plaque above it is the fluorescent yellow-green the MUTCD adopted for school warnings.
  // Colouring the whole panel green, as this first did, reads as a warning sign throughout.
  private static final float[] COL_PANEL = {0.94f, 0.94f, 0.93f, 1.0f};
  private static final float[] COL_BANNER = {0.72f, 0.93f, 0.20f, 1.0f};
  private static final float[] COL_BORDER = {0.05f, 0.05f, 0.05f, 1.0f};
  private static final float[] COL_HOUSING = {0.13f, 0.13f, 0.14f, 1.0f};
  private static final float[] COL_VISOR = {
      Math.min(1.0f, COL_HOUSING[0] * VISOR_TINT_SCALE + VISOR_TINT_BASE),
      Math.min(1.0f, COL_HOUSING[1] * VISOR_TINT_SCALE + VISOR_TINT_BASE),
      Math.min(1.0f, COL_HOUSING[2] * VISOR_TINT_SCALE + VISOR_TINT_BASE)};

  private static final int TEXT_BLACK = 0x111111;
  private static final float TEXT_SCALE_LABEL = 0.42f;
  /** The plaque line is smaller than the legend because FLASHING is the longest word on it. */
  private static final float TEXT_SCALE_PLAQUE = 0.30f;
  private static final float TEXT_SCALE_SPEED = 0.85f;

  // Line centres, measured from the panel's middle. Spaced off the actual line heights rather
  // than by eye: the first pass had the speed number sitting on top of LIMIT.
  private static final float LINE_SCHOOL = 12.5f;
  private static final float LINE_SPEED = 6.7f;
  private static final float LINE_LIMIT = 2.6f;
  private static final float LINE_NUMBER = -4.1f;
  private static final float LINE_WHEN = -10.2f;
  private static final float LINE_FLASHING = -13.3f;

  private static final ResourceLocation WHITE_TEXTURE =
      new ResourceLocation("csm", "textures/blocks/white1px.png");

  @Override
  public void render(TileEntitySchoolZoneBeacon te, double x, double y, double z,
      float partialTicks, int destroyStage, float alpha) {
    if (te == null || te.getWorld() == null) {
      return;
    }

    EnumFacing facing = te.getWorld().getBlockState(te.getPos()).getValue(BlockHorizontal.FACING);

    GlStateManager.pushMatrix();
    GlStateManager.translate(x, y, z);
    GlStateManager.translate(0.5, 0.0, 0.5);
    GlStateManager.rotate(rotationFor(facing), 0, 1, 0);
    GlStateManager.translate(-0.5, 0.0, -0.5);
    GlStateManager.scale(0.0625, 0.0625, 0.0625);

    // Scale about the bottom of the assembly rather than its middle, so a bigger sign grows
    // upward instead of both ways. Scaling about the centre buried the lower half at 200% --
    // the panel sank through the ground and took WHEN FLASHING with it. At 100% the pivot
    // makes no difference, so this only ever helps.
    float scale = te.getScale();
    float pivotY = assemblyBottom(te);
    GlStateManager.translate(CX, pivotY, CZ);
    GlStateManager.scale(scale, scale, scale);
    GlStateManager.translate(-CX, -pivotY, -CZ);

    GlStateManager.disableLighting();
    GlStateManager.disableCull();
    GlStateManager.enableBlend();
    GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
    Minecraft.getMinecraft().getTextureManager().bindTexture(WHITE_TEXTURE);

    int combined = te.getWorld().getCombinedLight(te.getPos(), 0);
    int sky = (combined >> 16) & 0xFFFF;
    int block = combined & 0xFFFF;

    renderPanel(sky, block);
    renderPanelText(te);

    renderBeacons(te, sky, block);

    GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
    GlStateManager.enableLighting();
    GlStateManager.enableCull();
    GlStateManager.disableBlend();
    GlStateManager.popMatrix();
  }

  /**
   * The lowest point the assembly draws to, which is the scale pivot: the sign's bottom edge,
   * or the lower beacon's when one is fitted.
   *
   * @param te the beacon being drawn
   *
   * @return the bottom of the assembly, in model units
   */
  private static float assemblyBottom(TileEntitySchoolZoneBeacon te) {
    float borderBottom = CY - PANEL_H / 2.0f - PANEL_BORDER;
    if (te.getArrangement() != TileEntitySchoolZoneBeacon.BEACONS_ABOVE_AND_BELOW) {
      return borderBottom;
    }
    return borderBottom - BEACON_GAP - sectionSize(te);
  }

  /** The beacon head's diameter in model units, which is also the section's height. */
  private static float sectionSize(TileEntitySchoolZoneBeacon te) {
    return te.getBeaconSize() == TileEntitySchoolZoneBeacon.BEACON_SIZE_12_INCH
        ? SECTION_12_INCH
        : SECTION_8_INCH;
  }

  private static float rotationFor(EnumFacing facing) {
    switch (facing) {
      case WEST:
        return 90;
      case SOUTH:
        return 180;
      case EAST:
        return 270;
      case NORTH:
      default:
        return 0;
    }
  }

  private void renderPanel(int sky, int block) {
    Tessellator tess = Tessellator.getInstance();
    BufferBuilder buf = tess.getBuffer();

    float halfW = PANEL_W / 2.0f;
    float halfH = PANEL_H / 2.0f;
    float backZ = CZ + PANEL_D / 2.0f;
    float frontZ = CZ - PANEL_D / 2.0f;

    List<RenderHelper.Box> border = new ArrayList<>();
    addRoundedRect(border,
        CX - halfW - PANEL_BORDER, CY - halfH - PANEL_BORDER,
        CX + halfW + PANEL_BORDER, CY + halfH + PANEL_BORDER,
        frontZ, backZ, PANEL_RADIUS, true, true);
    buf.begin(GL11.GL_QUADS, DefaultVertexFormats.BLOCK);
    RenderHelper.addBoxesToBufferLit(border, buf,
        COL_BORDER[0], COL_BORDER[1], COL_BORDER[2], COL_BORDER[3], 0, 0, 0, sky, block);
    tess.draw();

    // Two faces, not one: the gap between them is left unpainted so the black border box
    // behind shows through as the divider between the plaque and the sign under it.
    float top = CY + halfH;
    float bannerBottom = top - BANNER_H;
    float bodyTop = bannerBottom - BANNER_DIVIDER;
    float faceRadius = PANEL_RADIUS - PANEL_BORDER;

    List<RenderHelper.Box> body = new ArrayList<>();
    addRoundedRect(body, CX - halfW, CY - halfH, CX + halfW, bodyTop,
        frontZ - 0.1f, frontZ + 0.2f, faceRadius, true, false);
    buf.begin(GL11.GL_QUADS, DefaultVertexFormats.BLOCK);
    RenderHelper.addBoxesToBufferLit(body, buf,
        COL_PANEL[0], COL_PANEL[1], COL_PANEL[2], COL_PANEL[3], 0, 0, 0, sky, block);
    tess.draw();

    List<RenderHelper.Box> banner = new ArrayList<>();
    addRoundedRect(banner, CX - halfW, bannerBottom, CX + halfW, top,
        frontZ - 0.1f, frontZ + 0.2f, faceRadius, false, true);
    buf.begin(GL11.GL_QUADS, DefaultVertexFormats.BLOCK);
    RenderHelper.addBoxesToBufferLit(banner, buf,
        COL_BANNER[0], COL_BANNER[1], COL_BANNER[2], COL_BANNER[3], 0, 0, 0, sky, block);
    tess.draw();
  }

  /**
   * Adds a rectangle whose corners are stepped in along a quarter circle, as a stack of boxes
   * that touch but never overlap — overlapping boxes would share a face plane and z-fight.
   *
   * <p>Each band's inset is measured at its <em>outer</em> edge, so the silhouette sits just
   * inside the arc rather than bulging past it at any step.</p>
   *
   * @param out         the list to add the boxes to
   * @param x1          left edge
   * @param y1          bottom edge
   * @param x2          right edge
   * @param y2          top edge
   * @param z1          front face
   * @param z2          back face
   * @param radius      corner radius; zero or less emits one plain box
   * @param roundBottom whether the bottom two corners are rounded
   * @param roundTop    whether the top two corners are rounded
   */
  private static void addRoundedRect(List<RenderHelper.Box> out,
      float x1, float y1, float x2, float y2, float z1, float z2,
      float radius, boolean roundBottom, boolean roundTop) {
    if (radius <= 0.0f || (!roundBottom && !roundTop)) {
      out.add(new RenderHelper.Box(new float[]{x1, y1, z1}, new float[]{x2, y2, z2}));
      return;
    }

    out.add(new RenderHelper.Box(
        new float[]{x1, roundBottom ? y1 + radius : y1, z1},
        new float[]{x2, roundTop ? y2 - radius : y2, z2}));

    for (int i = 0; i < ROUND_STEPS; i++) {
      float bandLow = radius * i / ROUND_STEPS;
      float bandHigh = radius * (i + 1) / ROUND_STEPS;
      float outerDistance = radius - bandLow;
      float inset = radius
          - (float) Math.sqrt(Math.max(0.0f, radius * radius - outerDistance * outerDistance));
      if (roundBottom) {
        out.add(new RenderHelper.Box(
            new float[]{x1 + inset, y1 + bandLow, z1},
            new float[]{x2 - inset, y1 + bandHigh, z2}));
      }
      if (roundTop) {
        out.add(new RenderHelper.Box(
            new float[]{x1 + inset, y2 - bandHigh, z1},
            new float[]{x2 - inset, y2 - bandLow, z2}));
      }
    }
  }

  /**
   * The panel legend: SCHOOL over SPEED LIMIT, the posted number, then WHEN FLASHING — the
   * S5-1 layout, which is what makes the sign readable as a school zone rather than a plain
   * speed limit.
   */
  private void renderPanelText(TileEntitySchoolZoneBeacon te) {
    CsmFontRenderer fr = CsmFontRenderer.highwayGothic();
    float faceZ = CZ - PANEL_D / 2.0f - 0.25f;

    GlStateManager.pushMatrix();
    GlStateManager.translate(CX, CY, faceZ);
    GlStateManager.rotate(180, 0, 1, 0);
    GlStateManager.depthMask(false);

    drawCentred(fr, "SCHOOL", 0, LINE_SCHOOL, TEXT_SCALE_LABEL);
    drawCentred(fr, "SPEED", 0, LINE_SPEED, TEXT_SCALE_LABEL);
    drawCentred(fr, "LIMIT", 0, LINE_LIMIT, TEXT_SCALE_LABEL);
    drawCentred(fr, String.valueOf(te.getSpeedLimit()), 0, LINE_NUMBER, TEXT_SCALE_SPEED);
    drawCentred(fr, "WHEN", 0, LINE_WHEN, TEXT_SCALE_PLAQUE);
    drawCentred(fr, "FLASHING", 0, LINE_FLASHING, TEXT_SCALE_PLAQUE);

    GlStateManager.depthMask(true);
    GlStateManager.popMatrix();
  }

  /**
   * Draws one line centred on the panel's own axis. The Y scale is negative because the font
   * renderer draws downward and this matrix has already been flipped to face the viewer.
   */
  private void drawCentred(CsmFontRenderer fr, String text, float centreX, float centreY,
      float scale) {
    GlStateManager.pushMatrix();
    GlStateManager.translate(centreX, centreY, 0);
    GlStateManager.scale(scale, -scale, scale);
    int width = fr.getStringWidth(text);
    fr.drawString(text, -width / 2, -fr.FONT_HEIGHT / 2, TEXT_BLACK);
    GlStateManager.popMatrix();
  }

  /**
   * Draws the beacons for the configured arrangement. A pair runs in opposite phases so the
   * assembly alternates rather than blinking in unison; a lone beacon simply flashes.
   */
  private void renderBeacons(TileEntitySchoolZoneBeacon te, int sky, int block) {
    boolean flashing = te.isFlashingNow();
    long millis = CsmRenderUtils.gameMillis(te.getWorld());
    boolean phaseA = flashing && TrafficSignalFlashPattern.OFF.isFlashLit(millis);
    boolean phaseB = flashing && TrafficSignalFlashPattern.B.isFlashLit(millis);

    float size = sectionSize(te);
    float borderTop = CY + PANEL_H / 2.0f + PANEL_BORDER;
    float borderBottom = CY - PANEL_H / 2.0f - PANEL_BORDER;
    float aboveY = borderTop + BEACON_GAP + size / 2.0f;

    switch (te.getArrangement()) {
      case TileEntitySchoolZoneBeacon.BEACONS_ABOVE_AND_BELOW: {
        float belowY = borderBottom - BEACON_GAP - size / 2.0f;
        renderBracket(CX, borderTop, aboveY - size / 2.0f, sky, block);
        renderBracket(CX, belowY + size / 2.0f, borderBottom, sky, block);
        renderBeacon(CX, aboveY, size, phaseA, sky, block);
        renderBeacon(CX, belowY, size, phaseB, sky, block);
        break;
      }
      case TileEntitySchoolZoneBeacon.BEACONS_TWO_ABOVE: {
        float offset = (size + BEACON_PAIR_GAP) / 2.0f;
        renderBracket(CX - offset, borderTop, aboveY - size / 2.0f, sky, block);
        renderBracket(CX + offset, borderTop, aboveY - size / 2.0f, sky, block);
        renderBeacon(CX - offset, aboveY, size, phaseA, sky, block);
        renderBeacon(CX + offset, aboveY, size, phaseB, sky, block);
        break;
      }
      case TileEntitySchoolZoneBeacon.BEACONS_ABOVE:
      default:
        renderBracket(CX, borderTop, aboveY - size / 2.0f, sky, block);
        renderBeacon(CX, aboveY, size, phaseA, sky, block);
        break;
    }
  }

  /** The stub of mast between the sign's border and a beacon, so neither one floats. */
  private void renderBracket(float centreX, float lowY, float highY, int sky, int block) {
    Tessellator tess = Tessellator.getInstance();
    BufferBuilder buf = tess.getBuffer();

    List<RenderHelper.Box> bracket = new ArrayList<>();
    bracket.add(new RenderHelper.Box(
        new float[]{centreX - BRACKET_HALF_W, lowY, BRACKET_Z1},
        new float[]{centreX + BRACKET_HALF_W, highY, BRACKET_Z2}));

    Minecraft.getMinecraft().getTextureManager().bindTexture(WHITE_TEXTURE);
    buf.begin(GL11.GL_QUADS, DefaultVertexFormats.BLOCK);
    RenderHelper.addBoxesToBufferLit(bracket, buf,
        COL_HOUSING[0], COL_HOUSING[1], COL_HOUSING[2], COL_HOUSING[3], 0, 0, 0, sky, block);
    tess.draw();
  }

  /**
   * Draws one beacon: a traffic signal section's body, door and circle visor in the housing
   * colour, then its bulb off the signal atlas.
   *
   * <p>The white swatch is bound here rather than once for the whole render because
   * {@code CsmFontRenderer.drawString} leaves the font atlas bound behind it, and untextured
   * geometry drawn after the legend would otherwise sample the font sheet — which is exactly
   * what made the first beacons invisible.</p>
   */
  private void renderBeacon(float centreX, float centreY, float size, boolean lit,
      int sky, int block) {
    boolean twelveInch = size >= SECTION_12_INCH;
    float xOffset = centreX - VISOR_CENTER_X;
    float yOffset = centreY - VISOR_CENTER_Y;

    Tessellator tess = Tessellator.getInstance();
    BufferBuilder buf = tess.getBuffer();

    Minecraft.getMinecraft().getTextureManager().bindTexture(WHITE_TEXTURE);
    buf.begin(GL11.GL_QUADS, DefaultVertexFormats.BLOCK);
    RenderHelper.addBoxesToBufferLit(twelveInch
            ? TrafficSignalVertexData.SIGNAL_BODY_VERTEX_DATA
            : TrafficSignalVertexData.SIGNAL_BODY_8INCH_VERTEX_DATA, buf,
        COL_HOUSING[0], COL_HOUSING[1], COL_HOUSING[2], COL_HOUSING[3],
        xOffset, yOffset, 0.0f, sky, block);
    RenderHelper.addBoxesToBufferLit(twelveInch
            ? TrafficSignalVertexData.SIGNAL_DOOR_VERTEX_DATA
            : TrafficSignalVertexData.SIGNAL_DOOR_8INCH_VERTEX_DATA, buf,
        COL_HOUSING[0], COL_HOUSING[1], COL_HOUSING[2], COL_HOUSING[3],
        xOffset, yOffset, 0.0f, sky, block);
    RenderHelper.addTiltedBoxesToBufferDualColorLit(twelveInch
            ? TrafficSignalVertexData.CIRCLE_VISOR_VERTEX_DATA
            : TrafficSignalVertexData.CIRCLE_VISOR_8INCH_VERTEX_DATA, buf,
        COL_VISOR[0], COL_VISOR[1], COL_VISOR[2],
        0.0f, 0.0f, 0.0f, 1.0f,
        xOffset, yOffset, 0.0f, VISOR_PIVOT_Z, VISOR_TILT_DEGREES,
        VISOR_CENTER_X, VISOR_CENTER_Y, 0.0f, sky, block);
    tess.draw();

    GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
    Minecraft.getMinecraft().getTextureManager().bindTexture(SIGNAL_ATLAS);
    buf.begin(GL11.GL_QUADS, DefaultVertexFormats.BLOCK);
    addBeaconBulb(buf, xOffset, yOffset, size, lit);
    tess.draw();
  }

  /**
   * Adds the bulb quad. The arithmetic is the signal renderer's: a section's bulb is inset
   * slightly inside its door and pushed forward toward the visor by an amount that scales with
   * the section, so an 8-inch head is not simply a shrunk 12-inch one.
   */
  private void addBeaconBulb(BufferBuilder buf, float xOffset, float yOffset, float fullSize,
      boolean lit) {
    TextureInfo texInfo = TrafficSignalTextureMap.getTextureInfoForBulb(
        TrafficSignalBulbStyle.LED, TrafficSignalBulbType.BALL,
        TrafficSignalBulbColor.YELLOW, lit);

    float sizeScale = fullSize / SECTION_12_INCH;
    float inset = fullSize * 0.02f;
    float size = fullSize - inset * 2.0f;
    float sectionOffset = (SECTION_12_INCH - fullSize) / 2.0f;
    float baseX = 2.0f + inset + xOffset + sectionOffset;
    float baseY = yOffset + inset + sectionOffset;
    float z = VISOR_PIVOT_Z + (10.4f - VISOR_PIVOT_Z) * sizeScale;

    float u1 = texInfo.getU1();
    float v1 = texInfo.getV1();
    float u2 = texInfo.getU2();
    float v2 = texInfo.getV2();

    bulbVertex(buf, baseX, baseY, z, u2, v2);
    bulbVertex(buf, baseX + size, baseY, z, u1, v2);
    bulbVertex(buf, baseX + size, baseY + size, z, u1, v1);
    bulbVertex(buf, baseX, baseY + size, z, u2, v1);
  }

  private static void bulbVertex(BufferBuilder buf, float x, float y, float z,
      float u, float v) {
    buf.pos(x, y, z).color(1.0f, 1.0f, 1.0f, 1.0f).tex(u, v)
        .lightmap(LIGHTMAP_FULLBRIGHT_SKY, LIGHTMAP_FULLBRIGHT_BLOCK).endVertex();
  }
}

package com.micatechnologies.minecraft.csm.trafficaccessories;

import com.micatechnologies.minecraft.csm.codeutils.CsmDisplayListCache;
import com.micatechnologies.minecraft.csm.codeutils.CsmFontRenderer;
import com.micatechnologies.minecraft.csm.codeutils.CsmRenderToggles;
import com.micatechnologies.minecraft.csm.codeutils.CsmRenderUtils;
import com.micatechnologies.minecraft.csm.codeutils.CsmSharedDisplayLists;
import com.micatechnologies.minecraft.csm.codeutils.RenderHelper;
import com.micatechnologies.minecraft.csm.trafficsignals.logic.TrafficSignalBulbColor;
import com.micatechnologies.minecraft.csm.codeutils.RenderHelper.Box;
import com.micatechnologies.minecraft.csm.trafficsignals.logic.TrafficSignalBodyColor;
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
 * <p>Only the bulbs change from frame to frame. Everything drawn in the white swatch -- the panel,
 * the plaque, the bracketry and the beacon housings -- is compiled once per appearance into a list
 * shared by every beacon that looks the same ({@link CsmSharedDisplayLists}), and the legend into a
 * second list keyed on the posted speed, since it is the only other texture. Both are replayed
 * under each beacon's own facing and scale. The housings' vertices carry the block's light, so the
 * light is part of the body key. {@link CsmRenderToggles#sharedBakesPerFrame} draws both per frame,
 * for comparison.</p>
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

  // Beacons: the gap between the sign's border and the beacon, and the bracketry that spans
  // it. The bracket runs the full depth of the section behind it rather than being a thin tab
  // on the sign's own plane — a head this heavy hanging off a 2-unit sliver read as floating.
  private static final float BEACON_GAP = 3.0f;
  private static final float BEACON_PAIR_GAP = 0.5f;
  private static final float BRACKET_HALF_W = 2.0f;
  private static final float BRACKET_Z1 = 11.8f;
  private static final float BRACKET_Z2 = 16.0f;
  /** How far the crossbar of a two-head assembly runs past the outside of each head. */
  private static final float CROSSBAR_OVERHANG = 2.0f;
  private static final float CROSSBAR_H = 2.2f;

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

  /**
   * Pivot: the middle of the cell, pushed back far enough that the panel's rear face lands
   * exactly on the cell's back. Anything less leaves a sliver of daylight between the sign and
   * whatever it is bolted to, which is visible from any angle off the front.
   */
  private static final float CX = 8.0f;
  private static final float CY = 8.0f;
  private static final float CZ = 16.0f - PANEL_D / 2.0f;

  // Face colours. The body is white because a speed limit is a regulatory sign; the SCHOOL
  // plaque above it is a warning sign, and so takes whichever of the two MUTCD warning
  // backgrounds the beacon is set to. Colouring the whole panel green, as this first did, reads
  // as a warning sign throughout.
  private static final float[] COL_PANEL = {0.94f, 0.94f, 0.93f, 1.0f};
  private static final float[] COL_BORDER = {0.05f, 0.05f, 0.05f, 1.0f};
  // The housing and visor shades are no longer constants: both come from the beacon's selected
  // housing colour, resolved per render in Style.

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

  /**
   * The white-swatch geometry: panel, plaque, bracketry and beacon housings. Key, see
   * {@link #bodyKey}.
   */
  private static final CsmSharedDisplayLists BODY_LISTS =
      new CsmSharedDisplayLists("school_beacon_body");

  /** The legend, in the font atlas, keyed on the posted speed limit. */
  private static final CsmSharedDisplayLists LEGEND_LISTS =
      new CsmSharedDisplayLists("school_beacon_legend");

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

    // The housings are drawn ahead of the legend now rather than after it. Every face here is
    // opaque and none of the housings overlaps the panel face the legend sits on, so the order
    // changes no pixel; the bulbs still follow their housings.
    if (CsmRenderToggles.sharedBakesPerFrame) {
      drawBody(te, sky, block);
    } else {
      long key = bodyKey(te, combined);
      int list = BODY_LISTS.get(key);
      if (list == CsmDisplayListCache.NO_LIST) {
        list = BODY_LISTS.allocate(key);
        if (list != CsmDisplayListCache.NO_LIST) {
          GL11.glNewList(list, GL11.GL_COMPILE);
          drawBody(te, sky, block);
          GL11.glEndList();
        }
      }
      if (list != CsmDisplayListCache.NO_LIST) {
        GL11.glCallList(list);
        // A direct draw resets the colour cache after its colour array; a replay does not.
        GlStateManager.resetColor();
      } else {
        // The driver refused a list name: draw directly rather than calling list 0.
        drawBody(te, sky, block);
      }
    }

    renderPanelText(te);

    renderBulbs(te);

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

  /**
   * Packs everything the white-swatch geometry depends on. Scale and facing are not in it: both
   * are the matrix the list is replayed under.
   *
   * <pre>
   *  bits  0-31  combined light, as getCombinedLight returns it (sky in the high half)
   *  bits 32-39  banner colour ordinal
   *  bits 40-47  housing colour ordinal
   *  bits 48-53  visor type ordinal
   *  bits 54-55  arrangement
   *  bit  56     beacon size
   * </pre>
   */
  private static long bodyKey(TileEntitySchoolZoneBeacon te, int combined) {
    return (combined & 0xFFFFFFFFL)
        | ((long) (te.getBannerColor().ordinal() & 0xFF) << 32)
        | ((long) (te.getHousingColor().ordinal() & 0xFF) << 40)
        | ((long) (te.getVisorType().ordinal() & 0x3F) << 48)
        | ((long) (te.getArrangement() & 0x3) << 54)
        | ((long) (te.getBeaconSize() & 0x1) << 56);
  }

  /**
   * Draws everything in the white swatch in one draw: the panel, then the bracketry and housings
   * in the order they were drawn one by one. Geometry only: the caller binds the texture and owns
   * every GL state.
   */
  private static void drawBody(TileEntitySchoolZoneBeacon te, int sky, int block) {
    Tessellator tess = Tessellator.getInstance();
    BufferBuilder buf = tess.getBuffer();
    buf.begin(GL11.GL_QUADS, DefaultVertexFormats.BLOCK);
    addPanel(buf, te, sky, block);
    addBeaconHousings(buf, te, sky, block);
    tess.draw();
  }

  private static void addPanel(BufferBuilder buf, TileEntitySchoolZoneBeacon te, int sky,
      int block) {
    float halfW = PANEL_W / 2.0f;
    float halfH = PANEL_H / 2.0f;
    float backZ = CZ + PANEL_D / 2.0f;
    float frontZ = CZ - PANEL_D / 2.0f;

    List<RenderHelper.Box> border = new ArrayList<>();
    RenderHelper.addRoundedRect(border,
        CX - halfW - PANEL_BORDER, CY - halfH - PANEL_BORDER,
        CX + halfW + PANEL_BORDER, CY + halfH + PANEL_BORDER,
        frontZ, backZ, PANEL_RADIUS, ROUND_STEPS, true, true);
    RenderHelper.addBoxesToBufferLit(border, buf,
        COL_BORDER[0], COL_BORDER[1], COL_BORDER[2], COL_BORDER[3], 0, 0, 0, sky, block);

    // Two faces, not one: the gap between them is left unpainted so the black border box
    // behind shows through as the divider between the plaque and the sign under it.
    float top = CY + halfH;
    float bannerBottom = top - BANNER_H;
    float bodyTop = bannerBottom - BANNER_DIVIDER;
    float faceRadius = PANEL_RADIUS - PANEL_BORDER;

    List<RenderHelper.Box> body = new ArrayList<>();
    RenderHelper.addRoundedRect(body, CX - halfW, CY - halfH, CX + halfW, bodyTop,
        frontZ - 0.1f, frontZ + 0.2f, faceRadius, ROUND_STEPS, true, false);
    RenderHelper.addBoxesToBufferLit(body, buf,
        COL_PANEL[0], COL_PANEL[1], COL_PANEL[2], COL_PANEL[3], 0, 0, 0, sky, block);

    MutcdSignFaceColor plaque = te.getBannerColor();
    List<RenderHelper.Box> banner = new ArrayList<>();
    RenderHelper.addRoundedRect(banner, CX - halfW, bannerBottom, CX + halfW, top,
        frontZ - 0.1f, frontZ + 0.2f, faceRadius, ROUND_STEPS, false, true);
    RenderHelper.addBoxesToBufferLit(banner, buf,
        plaque.getRed(), plaque.getGreen(), plaque.getBlue(), 1.0f, 0, 0, 0, sky, block);
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

    // What drawString set per line, set once: the atlas, texturing and the legend colour.
    fr.bindAtlas();
    GlStateManager.enableTexture2D();
    GlStateManager.color(((TEXT_BLACK >> 16) & 0xFF) / 255.0f,
        ((TEXT_BLACK >> 8) & 0xFF) / 255.0f, (TEXT_BLACK & 0xFF) / 255.0f, 1.0f);

    int speedLimit = te.getSpeedLimit();
    if (CsmRenderToggles.sharedBakesPerFrame) {
      drawLegend(fr, speedLimit);
    } else {
      int list = LEGEND_LISTS.get(speedLimit);
      if (list == CsmDisplayListCache.NO_LIST) {
        list = LEGEND_LISTS.allocate(speedLimit);
        if (list != CsmDisplayListCache.NO_LIST) {
          GL11.glNewList(list, GL11.GL_COMPILE);
          drawLegend(fr, speedLimit);
          GL11.glEndList();
        }
      }
      if (list != CsmDisplayListCache.NO_LIST) {
        GL11.glCallList(list);
      } else {
        // The driver refused a list name: draw directly rather than calling list 0.
        drawLegend(fr, speedLimit);
      }
    }

    GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
    GlStateManager.depthMask(true);
    GlStateManager.popMatrix();
  }

  /**
   * Draws the six legend lines. Geometry and matrix only: the caller binds the atlas and sets
   * the colour, so this can be compiled into a list.
   */
  private static void drawLegend(CsmFontRenderer fr, int speedLimit) {
    drawCentred(fr, "SCHOOL", 0, LINE_SCHOOL, TEXT_SCALE_LABEL);
    drawCentred(fr, "SPEED", 0, LINE_SPEED, TEXT_SCALE_LABEL);
    drawCentred(fr, "LIMIT", 0, LINE_LIMIT, TEXT_SCALE_LABEL);
    drawCentred(fr, String.valueOf(speedLimit), 0, LINE_NUMBER, TEXT_SCALE_SPEED);
    drawCentred(fr, "WHEN", 0, LINE_WHEN, TEXT_SCALE_PLAQUE);
    drawCentred(fr, "FLASHING", 0, LINE_FLASHING, TEXT_SCALE_PLAQUE);
  }

  /**
   * Draws one line centred on the panel's own axis. The Y scale is negative because the font
   * renderer draws downward and this matrix has already been flipped to face the viewer. The
   * matrix calls are not cached by {@code GlStateManager}, so they compile into a list.
   */
  private static void drawCentred(CsmFontRenderer fr, String text, float centreX, float centreY,
      float scale) {
    GlStateManager.pushMatrix();
    GlStateManager.translate(centreX, centreY, 0);
    GlStateManager.scale(scale, -scale, scale);
    int width = fr.getStringWidth(text);
    Tessellator tess = Tessellator.getInstance();
    BufferBuilder buf = tess.getBuffer();
    buf.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX);
    fr.addString(buf, text, -width / 2, -fr.FONT_HEIGHT / 2);
    tess.draw();
    GlStateManager.popMatrix();
  }

  /**
   * Draws the bulbs, the only part that changes from frame to frame. A pair runs in opposite
   * phases so the assembly alternates rather than blinking in unison; a lone beacon simply
   * flashes. Fullbright, one draw for both.
   */
  private void renderBulbs(TileEntitySchoolZoneBeacon te) {
    boolean flashing = te.isFlashingNow();
    long millis = CsmRenderUtils.gameMillis(te.getWorld());
    boolean phaseA = flashing && TrafficSignalFlashPattern.OFF.isFlashLit(millis);
    boolean phaseB = flashing && TrafficSignalFlashPattern.B.isFlashLit(millis);

    float size = sectionSize(te);
    float borderTop = CY + PANEL_H / 2.0f + PANEL_BORDER;
    float borderBottom = CY - PANEL_H / 2.0f - PANEL_BORDER;
    float aboveY = borderTop + BEACON_GAP + size / 2.0f;
    TrafficSignalBulbStyle bulbStyle = te.getBulbStyle();

    Tessellator tess = Tessellator.getInstance();
    BufferBuilder buf = tess.getBuffer();
    GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
    Minecraft.getMinecraft().getTextureManager().bindTexture(SIGNAL_ATLAS);
    buf.begin(GL11.GL_QUADS, DefaultVertexFormats.BLOCK);
    switch (te.getArrangement()) {
      case TileEntitySchoolZoneBeacon.BEACONS_ABOVE_AND_BELOW: {
        float belowY = borderBottom - BEACON_GAP - size / 2.0f;
        addBeaconBulb(buf, CX - VISOR_CENTER_X, aboveY - VISOR_CENTER_Y, size, phaseA, bulbStyle);
        addBeaconBulb(buf, CX - VISOR_CENTER_X, belowY - VISOR_CENTER_Y, size, phaseB, bulbStyle);
        break;
      }
      case TileEntitySchoolZoneBeacon.BEACONS_TWO_ABOVE: {
        float offset = (size + BEACON_PAIR_GAP) / 2.0f;
        addBeaconBulb(buf, CX - offset - VISOR_CENTER_X, aboveY - VISOR_CENTER_Y, size, phaseA,
            bulbStyle);
        addBeaconBulb(buf, CX + offset - VISOR_CENTER_X, aboveY - VISOR_CENTER_Y, size, phaseB,
            bulbStyle);
        break;
      }
      case TileEntitySchoolZoneBeacon.BEACONS_ABOVE:
      default:
        addBeaconBulb(buf, CX - VISOR_CENTER_X, aboveY - VISOR_CENTER_Y, size, phaseA, bulbStyle);
        break;
    }
    tess.draw();
  }

  /**
   * Adds the bracketry and housings for the configured arrangement, in the order they were once
   * drawn one by one: the supports, then each housing.
   */
  private static void addBeaconHousings(BufferBuilder buf, TileEntitySchoolZoneBeacon te,
      int sky, int block) {
    Style style = new Style(te);
    float size = sectionSize(te);
    float borderTop = CY + PANEL_H / 2.0f + PANEL_BORDER;
    float borderBottom = CY - PANEL_H / 2.0f - PANEL_BORDER;
    float aboveY = borderTop + BEACON_GAP + size / 2.0f;

    switch (te.getArrangement()) {
      case TileEntitySchoolZoneBeacon.BEACONS_ABOVE_AND_BELOW: {
        float belowY = borderBottom - BEACON_GAP - size / 2.0f;
        addSupport(buf, CX - BRACKET_HALF_W, borderTop,
            CX + BRACKET_HALF_W, aboveY - size / 2.0f, style.housing, sky, block);
        addSupport(buf, CX - BRACKET_HALF_W, belowY + size / 2.0f,
            CX + BRACKET_HALF_W, borderBottom, style.housing, sky, block);
        addHousing(buf, CX, aboveY, size, style, sky, block);
        addHousing(buf, CX, belowY, size, style, sky, block);
        break;
      }
      case TileEntitySchoolZoneBeacon.BEACONS_TWO_ABOVE: {
        // The pair hangs off a crossbar rather than off two separate stalks, which is both how
        // a real two-head assembly is built and the only way the outer head has anything under
        // it — the sign is not wide enough to put a stalk under each.
        float offset = (size + BEACON_PAIR_GAP) / 2.0f;
        float headBottom = aboveY - size / 2.0f;
        float crossbarBottom = headBottom - CROSSBAR_H;
        addSupport(buf, CX - BRACKET_HALF_W, borderTop,
            CX + BRACKET_HALF_W, crossbarBottom, style.housing, sky, block);
        addSupport(buf, CX - offset - size / 2.0f - CROSSBAR_OVERHANG, crossbarBottom,
            CX + offset + size / 2.0f + CROSSBAR_OVERHANG, headBottom, style.housing, sky,
            block);
        addHousing(buf, CX - offset, aboveY, size, style, sky, block);
        addHousing(buf, CX + offset, aboveY, size, style, sky, block);
        break;
      }
      case TileEntitySchoolZoneBeacon.BEACONS_ABOVE:
      default:
        addSupport(buf, CX - BRACKET_HALF_W, borderTop,
            CX + BRACKET_HALF_W, aboveY - size / 2.0f, style.housing, sky, block);
        addHousing(buf, CX, aboveY, size, style, sky, block);
        break;
    }
  }

  /**
   * Everything about how one beacon assembly looks, resolved once per render and handed down
   * rather than read from renderer state, so nothing depends on the order two assemblies draw in.
   */
  private static final class Style {
    final float[] housing;
    final float[] visor;
    final List<Box> visorData12;
    final List<Box> visorData8;

    Style(TileEntitySchoolZoneBeacon te) {
      this.housing = housingColor(te.getHousingColor());
      this.visor = visorColor(this.housing);
      this.visorData12 = TrafficSignalVertexData.resolveVisorData(te.getVisorType(), 12);
      this.visorData8 = TrafficSignalVertexData.resolveVisorData(te.getVisorType(), 8);
    }
  }

  /**
   * The housing colour as {r, g, b, a}, and the visor shade lifted off it by the same tint the
   * signal heads use so the visor reads as a separate part rather than a flat silhouette.
   */
  private static float[] housingColor(TrafficSignalBodyColor color) {
    return new float[]{color.getRed(), color.getGreen(), color.getBlue(), 1.0f};
  }

  private static float[] visorColor(float[] housing) {
    return new float[]{
        Math.min(1.0f, housing[0] * VISOR_TINT_SCALE + VISOR_TINT_BASE),
        Math.min(1.0f, housing[1] * VISOR_TINT_SCALE + VISOR_TINT_BASE),
        Math.min(1.0f, housing[2] * VISOR_TINT_SCALE + VISOR_TINT_BASE)};
  }

  /** A piece of the bracketry between the sign and a beacon, so neither one floats. */
  private static void addSupport(BufferBuilder buf, float x1, float lowY, float x2, float highY,
      float[] housing, int sky, int block) {
    if (highY <= lowY) {
      return;
    }
    List<RenderHelper.Box> bracket = new ArrayList<>();
    bracket.add(new RenderHelper.Box(
        new float[]{x1, lowY, BRACKET_Z1},
        new float[]{x2, highY, BRACKET_Z2}));

    RenderHelper.addBoxesToBufferLit(bracket, buf,
        housing[0], housing[1], housing[2], housing[3], 0, 0, 0, sky, block);
  }

  /**
   * Adds one beacon's housing: a traffic signal section's body, door and circle visor in the
   * housing colour. Its bulb is drawn live, off the signal atlas, by {@link #renderBulbs}.
   *
   * <p>All of this is in the white swatch, which the caller binds. It must be bound after the
   * legend is drawn, never before it, because {@code CsmFontRenderer} leaves its atlas bound
   * behind it and untextured geometry would sample the font sheet -- which is exactly what made
   * the first beacons invisible. The body, which holds these, is drawn ahead of the legend.</p>
   */
  private static void addHousing(BufferBuilder buf, float centreX, float centreY, float size,
      Style style, int sky, int block) {
    boolean twelveInch = size >= SECTION_12_INCH;
    float xOffset = centreX - VISOR_CENTER_X;
    float yOffset = centreY - VISOR_CENTER_Y;

    RenderHelper.addBoxesToBufferLit(twelveInch
            ? TrafficSignalVertexData.SIGNAL_BODY_VERTEX_DATA
            : TrafficSignalVertexData.SIGNAL_BODY_8INCH_VERTEX_DATA, buf,
        style.housing[0], style.housing[1], style.housing[2], style.housing[3],
        xOffset, yOffset, 0.0f, sky, block);
    RenderHelper.addBoxesToBufferLit(twelveInch
            ? TrafficSignalVertexData.SIGNAL_DOOR_VERTEX_DATA
            : TrafficSignalVertexData.SIGNAL_DOOR_8INCH_VERTEX_DATA, buf,
        style.housing[0], style.housing[1], style.housing[2], style.housing[3],
        xOffset, yOffset, 0.0f, sky, block);
    RenderHelper.addTiltedBoxesToBufferDualColorLit(
        twelveInch ? style.visorData12 : style.visorData8, buf,
        style.visor[0], style.visor[1], style.visor[2],
        0.0f, 0.0f, 0.0f, 1.0f,
        xOffset, yOffset, 0.0f, VISOR_PIVOT_Z, VISOR_TILT_DEGREES,
        VISOR_CENTER_X, VISOR_CENTER_Y, 0.0f, sky, block);
  }

  /**
   * Adds the bulb quad. The arithmetic is the signal renderer's: a section's bulb is inset
   * slightly inside its door and pushed forward toward the visor by an amount that scales with
   * the section, so an 8-inch head is not simply a shrunk 12-inch one.
   */
  private void addBeaconBulb(BufferBuilder buf, float xOffset, float yOffset, float fullSize,
      boolean lit, TrafficSignalBulbStyle bulbStyle) {
    TextureInfo texInfo = TrafficSignalTextureMap.getTextureInfoForBulb(
        bulbStyle, TrafficSignalBulbType.BALL, TrafficSignalBulbColor.YELLOW, lit);

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

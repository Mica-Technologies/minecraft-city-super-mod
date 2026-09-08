package com.micatechnologies.minecraft.csm.trafficaccessories;

import com.micatechnologies.minecraft.csm.codeutils.CsmFontRenderer;
import com.micatechnologies.minecraft.csm.codeutils.CsmRenderUtils;
import com.micatechnologies.minecraft.csm.codeutils.RenderHelper;
import com.micatechnologies.minecraft.csm.trafficsignals.logic.TrafficSignalFlashPattern;
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
 * Renderer for {@link BlockSchoolZoneBeacon}: a fluorescent yellow-green SCHOOL SPEED LIMIT
 * panel with one or two amber beacon bars.
 *
 * <p>Sized to the roadside sign family rather than to the electronic speed limit signs — the
 * pole-mount sign's panel is wider than a block, which reads as a gantry sign rather than
 * something on a post. This one is 12 x 22, close to {@code metal_sign_ultratall}'s
 * proportions, and draws no post of its own so it mounts on whatever pole is behind it.</p>
 *
 * <p>The two lamps in a bar alternate on {@link TrafficSignalFlashPattern#OFF} and
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

  // Beacon bar, above the panel and (optionally) below it.
  private static final float BAR_W = 16.0f;
  private static final float BAR_H = 6.0f;
  private static final float BAR_D = 1.6f;
  private static final float BAR_GAP = 1.0f;
  private static final float LAMP_RADIUS = 1.9f;
  private static final float LAMP_OFFSET_X = 4.0f;
  private static final int LAMP_SEGMENTS = 12;

  /** Pivot: the middle of the cell, pushed back so the panel's rear sits on the cell's back. */
  private static final float CX = 8.0f;
  private static final float CY = 8.0f;
  private static final float CZ = 15.0f;

  // Face colours. The panel is fluorescent yellow-green, which is what a school zone sign has
  // been since the MUTCD adopted it — a plain yellow sign reads as a generic warning.
  private static final float[] COL_PANEL = {0.72f, 0.93f, 0.20f, 1.0f};
  private static final float[] COL_BORDER = {0.05f, 0.05f, 0.05f, 1.0f};
  private static final float[] COL_HOUSING = {0.13f, 0.13f, 0.14f, 1.0f};
  private static final float[] COL_LAMP_DARK = {0.20f, 0.15f, 0.05f, 1.0f};
  private static final float[] COL_LAMP_LIT = {1.0f, 0.72f, 0.10f, 1.0f};

  private static final int TEXT_BLACK = 0x111111;
  private static final float TEXT_SCALE_LABEL = 0.42f;
  /** The plaque line is smaller than the legend because FLASHING is the longest word on it. */
  private static final float TEXT_SCALE_PLAQUE = 0.30f;
  private static final float TEXT_SCALE_SPEED = 1.0f;

  // Line centres, measured from the panel's middle. Spaced off the actual line heights rather
  // than by eye: the first pass had the speed number sitting on top of LIMIT.
  private static final float LINE_SCHOOL = 12.1f;
  private static final float LINE_SPEED = 8.2f;
  private static final float LINE_LIMIT = 4.3f;
  private static final float LINE_NUMBER = -2.9f;
  private static final float LINE_WHEN = -9.6f;
  private static final float LINE_FLASHING = -12.4f;

  private static final ResourceLocation WHITE_TEXTURE =
      new ResourceLocation("csm", "textures/blocks/white1px.png");
  private static final int LIGHTMAP_FULLBRIGHT = 240;

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
    float pivotY = assemblyBottom(te.getArrangement());
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

    boolean flashing = te.isFlashingNow();
    long millis = CsmRenderUtils.gameMillis(te.getWorld());
    // The two lamps are the halves of a wig-wag pair, so one is lit exactly while the other
    // is dark. Both go out together when the zone is not posted.
    boolean leftLit = flashing && TrafficSignalFlashPattern.OFF.isFlashLit(millis);
    boolean rightLit = flashing && TrafficSignalFlashPattern.B.isFlashLit(millis);

    float panelTop = CY + PANEL_H / 2.0f;
    float panelBottom = CY - PANEL_H / 2.0f;
    renderBeaconBar(panelTop + BAR_GAP, leftLit, rightLit, sky, block);
    if (te.getArrangement() == TileEntitySchoolZoneBeacon.BEACONS_ABOVE_AND_BELOW) {
      // The lower bar runs opposite the upper one, so the assembly reads as alternating
      // rather than as two bars blinking in unison.
      renderBeaconBar(panelBottom - BAR_GAP - BAR_H, rightLit, leftLit, sky, block);
    }

    GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
    GlStateManager.enableLighting();
    GlStateManager.enableCull();
    GlStateManager.disableBlend();
    GlStateManager.popMatrix();
  }

  /**
   * The lowest point the assembly draws to, which is the scale pivot: the panel's bottom edge,
   * or the lower beacon bar's when one is fitted.
   *
   * @param arrangement the beacon arrangement
   *
   * @return the bottom of the assembly, in model units
   */
  private static float assemblyBottom(int arrangement) {
    float panelBottom = CY - PANEL_H / 2.0f;
    return arrangement == TileEntitySchoolZoneBeacon.BEACONS_ABOVE_AND_BELOW
        ? panelBottom - BAR_GAP - BAR_H
        : panelBottom;
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
    border.add(new RenderHelper.Box(
        new float[]{CX - halfW - PANEL_BORDER, CY - halfH - PANEL_BORDER, frontZ},
        new float[]{CX + halfW + PANEL_BORDER, CY + halfH + PANEL_BORDER, backZ}));
    buf.begin(GL11.GL_QUADS, DefaultVertexFormats.BLOCK);
    RenderHelper.addBoxesToBufferLit(border, buf,
        COL_BORDER[0], COL_BORDER[1], COL_BORDER[2], COL_BORDER[3], 0, 0, 0, sky, block);
    tess.draw();

    List<RenderHelper.Box> face = new ArrayList<>();
    face.add(new RenderHelper.Box(
        new float[]{CX - halfW, CY - halfH, frontZ - 0.1f},
        new float[]{CX + halfW, CY + halfH, frontZ + 0.2f}));
    buf.begin(GL11.GL_QUADS, DefaultVertexFormats.BLOCK);
    RenderHelper.addBoxesToBufferLit(face, buf,
        COL_PANEL[0], COL_PANEL[1], COL_PANEL[2], COL_PANEL[3], 0, 0, 0, sky, block);
    tess.draw();
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

  private void renderBeaconBar(float bottomY, boolean leftLit, boolean rightLit,
      int sky, int block) {
    Tessellator tess = Tessellator.getInstance();
    BufferBuilder buf = tess.getBuffer();

    float halfW = BAR_W / 2.0f;
    float frontZ = CZ - BAR_D / 2.0f;
    float backZ = CZ + BAR_D / 2.0f;
    float centreY = bottomY + BAR_H / 2.0f;

    List<RenderHelper.Box> housing = new ArrayList<>();
    housing.add(new RenderHelper.Box(
        new float[]{CX - halfW, bottomY, frontZ},
        new float[]{CX + halfW, bottomY + BAR_H, backZ}));
    buf.begin(GL11.GL_QUADS, DefaultVertexFormats.BLOCK);
    RenderHelper.addBoxesToBufferLit(housing, buf,
        COL_HOUSING[0], COL_HOUSING[1], COL_HOUSING[2], COL_HOUSING[3], 0, 0, 0, sky, block);
    tess.draw();

    // A lit lamp is drawn fullbright so it reads at night, which is when a flashing beacon
    // is doing the most work.
    renderLamp(CX - LAMP_OFFSET_X, centreY, frontZ - 0.15f, leftLit);
    renderLamp(CX + LAMP_OFFSET_X, centreY, frontZ - 0.15f, rightLit);
  }

  private void renderLamp(float cx, float cy, float cz, boolean lit) {
    Tessellator tess = Tessellator.getInstance();
    BufferBuilder buf = tess.getBuffer();

    List<float[]> perimeter = new ArrayList<>(LAMP_SEGMENTS);
    for (int i = 0; i < LAMP_SEGMENTS; i++) {
      double angle = 2.0 * Math.PI * i / LAMP_SEGMENTS;
      perimeter.add(new float[]{
          cx + (float) (Math.cos(angle) * LAMP_RADIUS),
          cy + (float) (Math.sin(angle) * LAMP_RADIUS),
          cz});
    }

    float[] colour = lit ? COL_LAMP_LIT : COL_LAMP_DARK;
    if (lit) {
      GlStateManager.disableLighting();
    }
    buf.begin(GL11.GL_TRIANGLE_FAN, DefaultVertexFormats.POSITION_COLOR);
    RenderHelper.addTriangleFanToBuffer(buf, cx, cy, cz, perimeter,
        colour[0], colour[1], colour[2], colour[3]);
    tess.draw();
  }
}

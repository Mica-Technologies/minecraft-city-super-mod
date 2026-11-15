package com.micatechnologies.minecraft.csm.parks.zipline;

import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.opengl.GL11;

/**
 * Draws a zipline's cable, from its launch anchor: a steel tube along the line, a chord a block.
 * A landing anchor draws nothing; its launch anchor draws the whole span.
 *
 * <p>A few hundred quads at most, for the longest line, drawn only while the launch anchor's chunk
 * is loaded and the line is in view; lit by the light at each end.</p>
 *
 * @version 1.0
 * @since 2026.11
 */
@SideOnly(Side.CLIENT)
public class TileEntityZiplineCableRenderer
    extends TileEntitySpecialRenderer<TileEntityZiplineAnchor> {

  /** From the cable's axis to each face of the tube: a 5/8 inch wire rope is about 0.016. */
  private static final double RADIUS = 0.028;
  private static final float RED = 0.42F;
  private static final float GREEN = 0.44F;
  private static final float BLUE = 0.47F;
  /** Line lights: one every this many blocks along the cable, this half-size. */
  private static final double LIGHT_SPACING = 6.0;
  private static final double LIGHT_HALF = 0.07;

  @Override
  public void render(TileEntityZiplineAnchor te, double x, double y, double z,
      float partialTicks, int destroyStage, float alpha) {
    ZiplineLine line = te.line();
    if (line == null) {
      return;
    }
    BlockPos origin = te.getPos();
    int chords = Math.max(2, MathHelper.ceil(line.length()));
    int light = Math.max(lightAt(te, origin), lightAt(te, line.landing()));
    OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, light & 0xFFFF,
        light >> 16);

    GlStateManager.pushMatrix();
    GlStateManager.translate(x - origin.getX(), y - origin.getY(), z - origin.getZ());
    GlStateManager.disableTexture2D();
    GlStateManager.disableLighting();
    GlStateManager.disableCull();
    Tessellator tess = Tessellator.getInstance();
    BufferBuilder buf = tess.getBuffer();
    buf.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
    Vec3d prev = line.point(0);
    for (int i = 1; i <= chords; i++) {
      Vec3d p = line.point(line.length() * i / chords);
      ZiplineDraw.tube(buf, prev, p, RADIUS, RED, GREEN, BLUE);
      prev = p;
    }
    tess.draw();
    if (te.hasLights()) {
      // Warm beads along the cable, lit whatever the light around them.
      OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 240, 240);
      buf.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
      Vec3d xAxis = new Vec3d(1, 0, 0);
      Vec3d yAxis = new Vec3d(0, 1, 0);
      Vec3d zAxis = new Vec3d(0, 0, 1);
      Vec3d half = new Vec3d(LIGHT_HALF, LIGHT_HALF, LIGHT_HALF);
      for (double s = LIGHT_SPACING / 2; s < line.length(); s += LIGHT_SPACING) {
        ZiplineDraw.box(buf, line.point(s).add(0, -LIGHT_HALF, 0), xAxis, yAxis, zAxis, half,
            1.0F, 0.86F, 0.55F);
      }
      tess.draw();
    }
    GlStateManager.enableCull();
    GlStateManager.enableTexture2D();
    GlStateManager.popMatrix();
  }

  private static int lightAt(TileEntityZiplineAnchor te, BlockPos pos) {
    return te.getWorld().isBlockLoaded(pos) ? te.getWorld().getCombinedLight(pos, 0) : 0;
  }

  /** Drawn whatever the distance check says, within the tile entity's own limit. */
  @Override
  public boolean isGlobalRenderer(TileEntityZiplineAnchor te) {
    return te.getLink() != null;
  }
}

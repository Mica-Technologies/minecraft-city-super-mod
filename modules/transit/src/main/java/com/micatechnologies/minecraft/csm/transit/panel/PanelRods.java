package com.micatechnologies.minecraft.csm.transit.panel;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.opengl.GL11;

/**
 * Draws a panel's hanger rods ({@link PanelLayout}): steel, lit by the world at the panel's top,
 * from the top of the panel up to the ceiling.
 *
 * @since 2026.10
 */
@SideOnly(Side.CLIENT)
public final class PanelRods {

  /** Half a hanger rod's width, in blocks. */
  public static final float ROD = 0.55f / 16f;

  private PanelRods() {
  }

  /**
   * Draws the rods in the reader's space (x along the panel to the reader's right from its left
   * edge, y up from its bottom, in blocks), the block atlas bound and lighting off. Leaves the
   * lightmap at the panel top's light when any rod is drawn.
   *
   * @param world   the world
   * @param pos     the controller
   * @param layout  its layout
   * @param zCentre the rods' centre line, in blocks toward the reader from the cell's middle
   */
  public static void draw(World world, BlockPos pos, PanelLayout layout, float zCentre) {
    float[] rodX = layout.getRodX();
    int[] drop = layout.getRodDrop();
    int height = layout.getHeight();
    boolean any = false;
    for (int d : drop) {
      any |= d > 0;
    }
    if (!any) {
      return;
    }
    int light = world.getCombinedLight(pos.up(height), 0);
    OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, light & 0xFFFF,
        light >>> 16);
    TextureAtlasSprite steel = Minecraft.getMinecraft().getTextureMapBlocks()
        .getAtlasSprite("csm:blocks/transit/airport/steel");
    float u0 = steel.getMinU();
    float u1 = steel.getInterpolatedU(2);
    float v0 = steel.getMinV();
    float v1 = steel.getMaxV();
    float zn = zCentre - ROD;
    float zf = zCentre + ROD;
    Tessellator tess = Tessellator.getInstance();
    BufferBuilder buf = tess.getBuffer();
    buf.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX_COLOR);
    for (int i = 0; i < rodX.length && i < drop.length; i++) {
      if (drop[i] <= 0) {
        continue;
      }
      float x0 = rodX[i] - ROD;
      float x1 = rodX[i] + ROD;
      float y0 = height;
      float y1 = height + drop[i];
      // four sides, shaded by hand as the world's directional light would
      side(buf, x0, zn, x1, zn, y0, y1, u0, u1, v0, v1, 0.8f);
      side(buf, x1, zf, x0, zf, y0, y1, u0, u1, v0, v1, 0.8f);
      side(buf, x0, zf, x0, zn, y0, y1, u0, u1, v0, v1, 0.6f);
      side(buf, x1, zn, x1, zf, y0, y1, u0, u1, v0, v1, 0.6f);
    }
    tess.draw();
  }

  private static void side(BufferBuilder buf, float xa, float za, float xb, float zb, float y0,
      float y1, float u0, float u1, float v0, float v1, float shade) {
    buf.pos(xa, y0, za).tex(u0, v1).color(shade, shade, shade, 1f).endVertex();
    buf.pos(xb, y0, zb).tex(u1, v1).color(shade, shade, shade, 1f).endVertex();
    buf.pos(xb, y1, zb).tex(u1, v0).color(shade, shade, shade, 1f).endVertex();
    buf.pos(xa, y1, za).tex(u0, v0).color(shade, shade, shade, 1f).endVertex();
  }
}

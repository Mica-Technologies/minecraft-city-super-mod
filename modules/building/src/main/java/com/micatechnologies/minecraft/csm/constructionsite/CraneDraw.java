package com.micatechnologies.minecraft.csm.constructionsite;

import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * The crane renderer's drawing primitives: shaded, textured boxes and bars in block units, into a
 * {@code POSITION_TEX_COLOR} buffer, with sprites from the block atlas.
 *
 * <p>Everything the crane draws comes from the block atlas, so the whole crane is one texture --
 * which is what lets a configuration be compiled into one display list (see "Display lists: one
 * texture, no cached state" in {@code assets/docs/TRAFFIC_SIGNAL_SYSTEM.md}). Faces are shaded by
 * direction the way the world's own blocks are, since the renderer draws with lighting off.</p>
 *
 * @version 1.0
 * @since 2026.9
 */
@SideOnly(Side.CLIENT)
public final class CraneDraw {

  /** Directional shading, as vanilla's block renderer applies it. */
  private static final float TOP = 1.0F;
  private static final float BOTTOM = 0.5F;
  private static final float NORTH_SOUTH = 0.8F;
  private static final float EAST_WEST = 0.6F;

  private CraneDraw() {
  }

  /**
   * An axis-aligned box, every face textured with the sprite stretched across it.
   *
   * @since 1.0
   */
  public static void box(BufferBuilder b, TextureAtlasSprite s, double x0, double y0, double z0,
      double x1, double y1, double z1) {
    float u0 = s.getMinU();
    float u1 = s.getMaxU();
    float v0 = s.getMinV();
    float v1 = s.getMaxV();
    // top
    quad(b, TOP, x0, y1, z0, u0, v0, x0, y1, z1, u0, v1, x1, y1, z1, u1, v1, x1, y1, z0, u1, v0);
    // bottom
    quad(b, BOTTOM, x0, y0, z0, u0, v0, x1, y0, z0, u1, v0, x1, y0, z1, u1, v1, x0, y0, z1, u0, v1);
    // north (-z)
    quad(b, NORTH_SOUTH, x0, y0, z0, u1, v1, x0, y1, z0, u1, v0, x1, y1, z0, u0, v0, x1, y0, z0,
        u0, v1);
    // south (+z)
    quad(b, NORTH_SOUTH, x0, y0, z1, u0, v1, x1, y0, z1, u1, v1, x1, y1, z1, u1, v0, x0, y1, z1,
        u0, v0);
    // west (-x)
    quad(b, EAST_WEST, x0, y0, z0, u0, v1, x0, y0, z1, u1, v1, x0, y1, z1, u1, v0, x0, y1, z0,
        u0, v0);
    // east (+x)
    quad(b, EAST_WEST, x1, y0, z0, u1, v1, x1, y1, z0, u1, v0, x1, y1, z1, u0, v0, x1, y0, z1,
        u0, v1);
  }

  private static void quad(BufferBuilder b, float shade, double ax, double ay, double az,
      float au, float av, double bx, double by, double bz, float bu, float bv, double cx,
      double cy, double cz, float cu, float cv, double dx, double dy, double dz, float du,
      float dv) {
    b.pos(ax, ay, az).tex(au, av).color(shade, shade, shade, 1F).endVertex();
    b.pos(bx, by, bz).tex(bu, bv).color(shade, shade, shade, 1F).endVertex();
    b.pos(cx, cy, cz).tex(cu, cv).color(shade, shade, shade, 1F).endVertex();
    b.pos(dx, dy, dz).tex(du, dv).color(shade, shade, shade, 1F).endVertex();
  }
}

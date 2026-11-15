package com.micatechnologies.minecraft.csm.parks.zipline;

import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * Untextured shapes for the zipline's renderers, into a {@code POSITION_COLOR} quad buffer: a
 * square tube between two points (the cable, the hangers) and an oriented box (the trolley).
 * Faces are shaded by which way they point, as block faces are, so the steel reads as round
 * enough from any side.
 *
 * @version 1.0
 * @since 2026.11
 */
@SideOnly(Side.CLIENT)
final class ZiplineDraw {

  private ZiplineDraw() {
  }

  /**
   * A square tube from {@code a} to {@code b}, {@code r} from its axis to each face.
   *
   * @since 1.0
   */
  static void tube(BufferBuilder buf, Vec3d a, Vec3d b, double r, float red, float green,
      float blue) {
    Vec3d dir = b.subtract(a);
    if (dir.lengthSquared() < 1.0e-12) {
      return;
    }
    dir = dir.normalize();
    Vec3d side = Math.abs(dir.y) > 0.99 ? new Vec3d(1, 0, 0) : dir.crossProduct(new Vec3d(0, 1,
        0)).normalize();
    Vec3d up = side.crossProduct(dir).normalize();
    Vec3d[] around = {up.scale(r).add(side.scale(r)), up.scale(r).subtract(side.scale(r)),
        up.scale(-r).subtract(side.scale(r)), up.scale(-r).add(side.scale(r))};
    float[] shade = {1.0F, 0.8F, 0.55F, 0.8F};
    for (int i = 0; i < 4; i++) {
      Vec3d p = around[i];
      Vec3d q = around[(i + 1) % 4];
      float s = shade[i];
      quad(buf, a.add(p), a.add(q), b.add(q), b.add(p), red * s, green * s, blue * s);
    }
  }

  /**
   * A box centred on {@code c}, {@code half} its half-sizes along {@code x}, {@code y} and
   * {@code z} (the box's own axes, unit vectors).
   *
   * @since 1.0
   */
  static void box(BufferBuilder buf, Vec3d c, Vec3d x, Vec3d y, Vec3d z, Vec3d half, float red,
      float green, float blue) {
    Vec3d hx = x.scale(half.x);
    Vec3d hy = y.scale(half.y);
    Vec3d hz = z.scale(half.z);
    Vec3d[] v = new Vec3d[8];
    for (int i = 0; i < 8; i++) {
      v[i] = c.add((i & 1) == 0 ? hx.scale(-1) : hx).add((i & 2) == 0 ? hy.scale(-1) : hy)
          .add((i & 4) == 0 ? hz.scale(-1) : hz);
    }
    // top, bottom, then the four sides
    quad(buf, v[2], v[6], v[7], v[3], red, green, blue);
    quad(buf, v[0], v[1], v[5], v[4], red * 0.5F, green * 0.5F, blue * 0.5F);
    quad(buf, v[0], v[2], v[3], v[1], red * 0.8F, green * 0.8F, blue * 0.8F);
    quad(buf, v[4], v[5], v[7], v[6], red * 0.8F, green * 0.8F, blue * 0.8F);
    quad(buf, v[0], v[4], v[6], v[2], red * 0.65F, green * 0.65F, blue * 0.65F);
    quad(buf, v[1], v[3], v[7], v[5], red * 0.65F, green * 0.65F, blue * 0.65F);
  }

  private static void quad(BufferBuilder buf, Vec3d a, Vec3d b, Vec3d c, Vec3d d, float red,
      float green, float blue) {
    // The renderers draw with face culling off, so the winding does not matter.
    vertex(buf, a, red, green, blue);
    vertex(buf, b, red, green, blue);
    vertex(buf, c, red, green, blue);
    vertex(buf, d, red, green, blue);
  }

  private static void vertex(BufferBuilder buf, Vec3d p, float red, float green, float blue) {
    buf.pos(p.x, p.y, p.z).color(red, green, blue, 1.0F).endVertex();
  }
}

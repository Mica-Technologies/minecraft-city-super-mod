package com.micatechnologies.minecraft.csm.constructionsite;

import com.micatechnologies.minecraft.csm.codeutils.CsmRenderUtils;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GLAllocation;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.opengl.GL11;

/**
 * Draws a tower crane head's slewing unit (see {@link CraneGeometry}).
 *
 * <p>A crane is thousands of quads and changes almost never, so each head's geometry is compiled
 * once into a display list and replayed every frame, rebuilt only when its configuration or the
 * light at the head changes. The slew is a rotation applied outside the list, so turning the crane
 * never rebuilds it. The rules this follows are the ones in "Display lists: one texture, no cached
 * state" in {@code assets/docs/TRAFFIC_SIGNAL_SYSTEM.md}: the whole crane is drawn from the block
 * atlas, which is bound outside the list every frame; nothing inside the list touches cached GL
 * state; and the light is part of the cache key, since it is baked into the vertices.</p>
 *
 * <h3>Aviation lights</h3>
 *
 * <p>The red obstruction lights flash the way a real crane's do: an FAA L-864 style red flash,
 * thirty a minute, with a short rise, a hold and a fade rather than a hard blink. Every light on
 * one crane flashes together, as the lights on one structure do; each crane has its own phase,
 * taken from its position, so a skyline of cranes flashes at one rate but not in step. A flash
 * changes every frame, so it is drawn here, outside the compiled list: the lens again, bright,
 * and two halos turned to face the camera -- a tight one and a wide soft one -- added onto what
 * is behind them. The halos grow a little with distance, so a lit crane still reads as a red
 * point from far across a city, where the lens itself is less than a pixel.</p>
 *
 * @version 1.0
 * @since 2026.9
 */
@SideOnly(Side.CLIENT)
public class TileEntityCraneHeadRenderer extends TileEntitySpecialRenderer<TileEntityCraneHead> {

  /** One compiled list per head, the configuration it was compiled from, and its lamps. */
  private static final Map<BlockPos, Entry> LISTS = new HashMap<>();

  private static final ResourceLocation GLOW =
      new ResourceLocation("csm", "textures/blocks/constructionsite/crane_glow.png");

  /** Thirty flashes a minute. */
  private static final long CYCLE_MS = 2000L;
  private static final long RISE_MS = 90L;
  private static final long HOLD_MS = 700L;
  private static final long FADE_MS = 260L;

  private static final float RED = 1.0F;
  private static final float GREEN = 0.12F;
  private static final float BLUE = 0.06F;

  /** Full-bright, as the lightmap coordinates a vertex carries. */
  private static final int FULL = 240;

  /** A head's compiled list, the key it was compiled at, and its lamps' lens centres. */
  private static final class Entry {

    final int list;
    int key;
    double[][] lamps = new double[0][];

    Entry(int list, int key) {
      this.list = list;
      this.key = key;
    }
  }

  @Override
  public void render(TileEntityCraneHead te, double x, double y, double z, float partialTicks,
      int destroyStage, float alpha) {
    int light = te.getWorld().getCombinedLight(te.getPos(), 0);
    int key = te.renderKey() * 31 + light;
    BlockPos pos = te.getPos();
    Entry entry = LISTS.get(pos);
    if (entry == null) {
      entry = new Entry(GLAllocation.generateDisplayLists(1), key + 1);
      LISTS.put(pos.toImmutable(), entry);
    }
    if (entry.key != key) {
      List<double[]> lamps = new ArrayList<>();
      GlStateManager.glNewList(entry.list, GL11.GL_COMPILE);
      Tessellator tessellator = Tessellator.getInstance();
      BufferBuilder buf = tessellator.getBuffer();
      buf.begin(GL11.GL_QUADS, DefaultVertexFormats.BLOCK);
      CraneGeometry.build(buf, te, light, lamps);
      tessellator.draw();
      GlStateManager.glEndList();
      entry.key = key;
      entry.lamps = lamps.toArray(new double[0][]);
    }

    double cx = x + 0.5 + te.getCentreX();
    double cz = z + 0.5 + te.getCentreZ();
    bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);
    GlStateManager.pushMatrix();
    GlStateManager.disableLighting();
    GlStateManager.translate(cx, y, cz);
    GlStateManager.rotate(-te.getSlew(), 0F, 1F, 0F);
    GlStateManager.callList(entry.list);
    GlStateManager.enableLighting();
    GlStateManager.popMatrix();

    long phase = Math.floorMod(pos.hashCode() * 2654435761L, CYCLE_MS);
    float intensity = flash(CsmRenderUtils.gameMillis(te.getWorld(), partialTicks) + phase);
    if (intensity > 0F && entry.lamps.length > 0) {
      drawLights(entry.lamps, te, cx, y, cz, intensity);
    }
  }

  /**
   * The brightness of an obstruction light at {@code millis}: a short rise, a hold and a fade,
   * then dark for the rest of the cycle -- about half the cycle lit, as an L-864 is.
   *
   * @param millis the light's own clock
   *
   * @return 0 to 1
   *
   * @since 1.0
   */
  static float flash(long millis) {
    long t = Math.floorMod(millis, CYCLE_MS);
    if (t < RISE_MS) {
      return (float) t / RISE_MS;
    }
    t -= RISE_MS;
    if (t < HOLD_MS) {
      return 1F;
    }
    t -= HOLD_MS;
    if (t < FADE_MS) {
      float f = 1F - (float) t / FADE_MS;
      return f * f;
    }
    return 0F;
  }

  /**
   * Draws every lit lamp: the lens, then a tight and a wide halo, each added onto what is behind
   * it. {@code (cx, y, cz)} is the crane's centre relative to the camera.
   */
  private void drawLights(double[][] lamps, TileEntityCraneHead te, double cx, double y,
      double cz, float intensity) {
    double s = te.getScale();
    double a = Math.toRadians(te.getSlew());
    double cos = Math.cos(a);
    double sin = Math.sin(a);

    bindTexture(GLOW);
    GlStateManager.disableLighting();
    GlStateManager.disableCull();
    GlStateManager.disableAlpha();
    GlStateManager.enableBlend();
    GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA,
        GlStateManager.DestFactor.ONE);
    GlStateManager.depthMask(false);

    Tessellator tessellator = Tessellator.getInstance();
    BufferBuilder buf = tessellator.getBuffer();
    buf.begin(GL11.GL_QUADS, DefaultVertexFormats.BLOCK);
    for (double[] lamp : lamps) {
      // Into the camera's frame, turned to the slew the list is drawn at.
      double px = cx + lamp[0] * cos - lamp[2] * sin;
      double py = y + lamp[1];
      double pz = cz + lamp[0] * sin + lamp[2] * cos;
      double dist = Math.sqrt(px * px + py * py + pz * pz);
      double far = Math.min(dist, 256.0);
      lens(buf, px, py, pz, 0.095 * s, 0.105 * s, intensity);
      halo(buf, px, py, pz, dist, 0.75 * s + far * 0.012, intensity);
      halo(buf, px, py, pz, dist, 2.4 * s + far * 0.035, 0.6F * intensity);
    }
    tessellator.draw();

    GlStateManager.depthMask(true);
    GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA,
        GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
    GlStateManager.disableBlend();
    GlStateManager.enableAlpha();
    GlStateManager.enableCull();
    GlStateManager.enableLighting();
  }

  /**
   * The lens, lit: a box a hair bigger than the one in the list, sampling the glow texture's
   * opaque centre, so no second texture is bound.
   */
  private static void lens(BufferBuilder buf, double x, double y, double z, double r, double h,
      float a) {
    double x0 = x - r;
    double x1 = x + r;
    double y0 = y - h;
    double y1 = y + h;
    double z0 = z - r;
    double z1 = z + r;
    double[][] faces = {
        {x0, y0, z0, x1, y0, z0, x1, y1, z0, x0, y1, z0},
        {x0, y0, z1, x1, y0, z1, x1, y1, z1, x0, y1, z1},
        {x0, y0, z0, x0, y0, z1, x0, y1, z1, x0, y1, z0},
        {x1, y0, z0, x1, y0, z1, x1, y1, z1, x1, y1, z0},
        {x0, y1, z0, x1, y1, z0, x1, y1, z1, x0, y1, z1}};
    for (double[] f : faces) {
      for (int i = 0; i < 12; i += 3) {
        buf.pos(f[i], f[i + 1], f[i + 2]).color(RED, 0.18F, 0.1F, a).tex(0.5, 0.5)
            .lightmap(FULL, FULL).endVertex();
      }
    }
  }

  /**
   * A square of the glow texture, half-size {@code r}, centred on the lamp and square to the
   * line from the camera to it, so it faces the camera wherever on screen the lamp is.
   */
  private static void halo(BufferBuilder buf, double x, double y, double z, double dist,
      double r, float a) {
    if (dist < 1.0E-3) {
      return;
    }
    double nx = x / dist;
    double ny = y / dist;
    double nz = z / dist;
    // Right = n x up; for a lamp straight above or below the camera, n x east.
    double rx;
    double ry;
    double rz;
    if (Math.abs(ny) > 0.99) {
      rx = 0.0;
      ry = nz;
      rz = -ny;
    } else {
      rx = -nz;
      ry = 0.0;
      rz = nx;
    }
    double rl = Math.sqrt(rx * rx + ry * ry + rz * rz);
    rx = rx / rl * r;
    ry = ry / rl * r;
    rz = rz / rl * r;
    // Up = right x n, already of length r.
    double ux = ry * nz - rz * ny;
    double uy = rz * nx - rx * nz;
    double uz = rx * ny - ry * nx;
    corner(buf, x - rx - ux, y - ry - uy, z - rz - uz, 0, 1, a);
    corner(buf, x + rx - ux, y + ry - uy, z + rz - uz, 1, 1, a);
    corner(buf, x + rx + ux, y + ry + uy, z + rz + uz, 1, 0, a);
    corner(buf, x - rx + ux, y - ry + uy, z - rz + uz, 0, 0, a);
  }

  private static void corner(BufferBuilder buf, double x, double y, double z, double u, double v,
      float a) {
    buf.pos(x, y, z).color(RED, GREEN, BLUE, a).tex(u, v).lightmap(FULL, FULL).endVertex();
  }

  /**
   * Frees a head's display list, when the head is broken or its chunk unloads.
   *
   * @param pos the head's position
   *
   * @since 1.0
   */
  public static void release(BlockPos pos) {
    Entry entry = LISTS.remove(pos);
    if (entry != null) {
      GLAllocation.deleteDisplayLists(entry.list);
    }
  }

  @Override
  public boolean isGlobalRenderer(TileEntityCraneHead te) {
    return true;
  }
}

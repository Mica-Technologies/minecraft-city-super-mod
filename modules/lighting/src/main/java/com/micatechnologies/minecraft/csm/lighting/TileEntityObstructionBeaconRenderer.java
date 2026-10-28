package com.micatechnologies.minecraft.csm.lighting;

import com.micatechnologies.minecraft.csm.codeutils.CsmDisplayListCache;
import com.micatechnologies.minecraft.csm.codeutils.CsmRenderToggles;
import com.micatechnologies.minecraft.csm.codeutils.CsmRenderUtils;
import com.micatechnologies.minecraft.csm.codeutils.CsmSharedDisplayLists;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.EnumSkyBlock;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL14;

/**
 * Draws a tall building beacon's flash: the lens at full brightness and a glow around it, only
 * while the flash is on. Outside a flash it returns at once, so a roof of beacons costs a clock
 * read each most of the time.
 *
 * <p>The cadence follows FAA AC 150/5345-43J. The red L-864 flashes 30 times a minute: a 100 ms
 * rise, 700 ms full, a 100 ms fade, dark for the rest of the 2 s. The white L-865 strobe flashes
 * 40 times a minute, one every 1.5 s; the standard wants the flash under 100 ms by day, and a
 * single 50 ms frame was barely seen at all in game, so it is 100 ms full and a 60 ms fade: still a
 * sharp strobe, and long enough to register. Both run off the wall clock, so every beacon in view
 * flashes in step, as a lit tower's do.</p>
 *
 * <p>Three layers, all additive and fullbright: a sleeve just outside the lens, a small hot glow
 * facing the camera, and a wide soft bloom that grows as it gets darker (a beacon at noon is a
 * bright lens, not a floodlight). The glow keeps a minimum size on screen, so a beacon on a roof a
 * hundred blocks off still reads as a point of light.</p>
 *
 * <p><b>Shared lists.</b> Nothing in the geometry depends on where the beacon is or how bright the
 * flash is: the sleeves are fixed in the block, the glow and bloom are discs of unit radius scaled
 * by the matrix, and the vertex colours are the style's. So each style's four parts (sleeve, hot
 * sleeve, bloom, glow) are compiled once into lists shared by every beacon
 * ({@link CsmSharedDisplayLists}), replayed under the beacon's translation and the billboard's
 * turn and scale. The flash's brightness, which changes every frame, is applied outside the lists
 * as the blend's constant alpha ({@code glBlendColor} with {@code GL_CONSTANT_ALPHA}) rather than
 * in the vertices: a vertex colour array overrides {@code glColor}, so the colour cannot carry it.
 * The glow and bloom vertices carry their colour premultiplied by their ring's alpha, so
 * {@code src * constant + dst} is exactly the {@code src * (intensity * ringAlpha) + dst} the live
 * draw made. Drawn live, a lit red beacon tessellated 784 vertices a frame for the 900 ms of each
 * flash. {@link CsmRenderToggles#sharedBakesPerFrame} draws it live, for comparison.</p>
 *
 * @since 2026.9
 */
@SideOnly(Side.CLIENT)
public class TileEntityObstructionBeaconRenderer
    extends TileEntitySpecialRenderer<TileEntityObstructionBeacon> {

  private static final ResourceLocation WHITE =
      new ResourceLocation("csm", "textures/blocks/white1px.png");

  /** How a beacon's lens sits in its block and how it flashes. */
  static final class Style {

    /** This style's place in the list keys. */
    final int index;
    final float radius;
    final float y0;
    final float y1;
    final float r;
    final float g;
    final float b;
    final long cycle;
    final long rise;
    final long full;
    final long fade;
    final float glow;
    final float bloom;
    /** How far the glow's core is washed toward white: a red light stays red. */
    final float whiten;

    Style(int index, float radiusPx, float y0Px, float y1Px, int rgb, long cycle, long rise,
        long full, long fade, float glow, float bloom, float whiten) {
      this.index = index;
      this.radius = radiusPx / 16f;
      this.y0 = y0Px / 16f;
      this.y1 = y1Px / 16f;
      this.r = ((rgb >> 16) & 255) / 255f;
      this.g = ((rgb >> 8) & 255) / 255f;
      this.b = (rgb & 255) / 255f;
      this.cycle = cycle;
      this.rise = rise;
      this.full = full;
      this.fade = fade;
      this.glow = glow;
      this.bloom = bloom;
      this.whiten = whiten;
    }

    /** How bright the flash is at a moment, 0 to 1. */
    float intensity(long millis) {
      long t = millis % cycle;
      if (t < rise) {
        return (float) t / rise;
      }
      t -= rise;
      if (t < full) {
        return 1f;
      }
      t -= full;
      if (t < fade) {
        return 1f - (float) t / fade;
      }
      return 0f;
    }
  }

  // Lens geometry matches gen_obstruction_beacons.py: red radius 3.8 px from y 3.4 to 8.6, white
  // radius 3.2 px from y 4 to 10.6, both 3 px higher on the wall bracket.
  static final Style RED = new Style(0, 3.8f, 3.4f, 8.6f, 0xFF2A18, 2000, 100, 700, 100,
      1.2f, 4.0f, 0.2f);
  static final Style WHITE_STROBE = new Style(1, 3.2f, 4f, 10.6f, 0xF2F6FF, 1500, 0, 100,
      60, 1.5f, 6.0f, 0.6f);

  /** The glow's profile: alpha at each share of its radius, a hot core fading to nothing. */
  private static final float[] GLOW_AT = {0f, 0.2f, 0.5f, 1f};
  private static final float[] GLOW_ALPHA = {1f, 0.8f, 0.3f, 0f};
  /** The bloom's: a wide soft wash. */
  private static final float[] BLOOM_AT = {0f, 0.3f, 0.65f, 1f};
  private static final float[] BLOOM_ALPHA = {0.6f, 0.32f, 0.1f, 0f};

  /** Of the bloom, how much is left in full daylight. */
  private static final float DAYLIGHT_BLOOM = 0.3f;
  /** The glow is never smaller on screen than this share of its distance. */
  private static final float MIN_ANGULAR_SIZE = 0.012f;
  private static final int SIDES = 8;
  private static final int FAN = 20;
  /** Sky and block light for a vertex that ignores the light around it. */
  private static final int FULLBRIGHT = 240;

  /** The coloured sleeve over the lens. */
  static final int PART_SLEEVE = 0;
  /** The nearly white sleeve added on top of it. */
  static final int PART_HOT_SLEEVE = 1;
  /** The wide bloom, a disc of unit radius. */
  static final int PART_BLOOM = 2;
  /** The small hot glow, a disc of unit radius. */
  static final int PART_GLOW = 3;
  /** Core in OpenGL 1.4 with {@code glBlendColor}; LWJGL 2 names it only in its imaging class. */
  private static final int GL_CONSTANT_ALPHA = 0x8003;
  private static final int GL_ONE_MINUS_CONSTANT_ALPHA = 0x8004;
  /** How much of the hot sleeve's flash reaches the screen. */
  private static final float HOT_SLEEVE_SHARE = 0.7f;

  /** Every part of both styles: eight lists in all, whatever the number of beacons. */
  private static final CsmSharedDisplayLists LISTS =
      new CsmSharedDisplayLists("obstruction_beacon");

  /**
   * The shared list key for one part of one style.
   *
   * @param style the style's index
   * @param part  one of the {@code PART_} constants
   *
   * @return the key
   */
  static long listKey(int style, int part) {
    return ((long) style << 2) | (part & 3);
  }

  @Override
  public void render(TileEntityObstructionBeacon te, double x, double y, double z,
      float partialTicks, int destroyStage, float alpha) {
    World world = te.getWorld();
    if (world == null) {
      return;
    }
    IBlockState state = world.getBlockState(te.getPos());
    if (!(state.getBlock() instanceof BlockObstructionBeacon)
        || !state.getValue(BlockObstructionBeacon.LIT)) {
      return;
    }
    Style style = ((BlockObstructionBeacon) state.getBlock()).isStrobe() ? WHITE_STROBE : RED;
    float on = style.intensity(CsmRenderUtils.gameMillis(world, partialTicks));
    if (on <= 0f) {
      return;
    }
    float lift = state.getValue(BlockObstructionBeacon.MOUNT)
        == BlockObstructionBeacon.Mount.FLOOR ? 0f : 3f / 16f;
    float cx = (float) x + 0.5f;
    float cz = (float) z + 0.5f;
    float y0 = (float) y + style.y0 + lift;
    float y1 = (float) y + style.y1 + lift;
    float cy = (y0 + y1) / 2f;

    // Darker surroundings, bigger bloom: 0 in full light, 1 in the pitch dark.
    BlockPos p = te.getPos();
    int sky = world.getLightFor(EnumSkyBlock.SKY, p) - world.getSkylightSubtracted();
    float dark = 1f - Math.max(0, Math.min(15, sky)) / 15f;
    float bloom = style.bloom * (DAYLIGHT_BLOOM + (1 - DAYLIGHT_BLOOM) * dark);
    double dist = Math.sqrt(x * x + y * y + z * z);
    float minSize = (float) (dist * MIN_ANGULAR_SIZE);

    Minecraft.getMinecraft().getTextureManager().bindTexture(WHITE);
    GlStateManager.pushMatrix();
    GlStateManager.disableLighting();
    GlStateManager.disableCull();
    GlStateManager.enableBlend();
    GlStateManager.depthMask(false);
    // The world pass leaves the alpha test at GREATER 0.1 and the shade model FLAT. The first
    // throws away every fragment of a glow fainter than a tenth; the second colours each triangle
    // of a fan from its last vertex, a rim vertex at alpha 0. Between them the first version of
    // this renderer ran every frame and put nothing on the screen (TileEntityFireAlarmStrobeRenderer
    // found the same).
    GlStateManager.disableAlpha();
    GlStateManager.shadeModel(GL11.GL_SMOOTH);

    boolean live = CsmRenderToggles.sharedBakesPerFrame;
    Tessellator tess = Tessellator.getInstance();
    BufferBuilder buf = tess.getBuffer();

    // The lit lens: a solid fullbright sleeve just outside the baked lens, drawn over it...
    if (live) {
      GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA,
          GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
      sleeve(tess, buf, cx, cz, y0, y1, style.radius + 0.01f, style.r, style.g, style.b, on);
    } else {
      // The sleeves sit in the block: only the beacon's own offset is the matrix's.
      GlStateManager.pushMatrix();
      GlStateManager.translate(x, y + lift, z);
      GlStateManager.blendFunc(GL_CONSTANT_ALPHA, GL_ONE_MINUS_CONSTANT_ALPHA);
      GL14.glBlendColor(0f, 0f, 0f, on);
      callPart(style, PART_SLEEVE);
    }
    // ...and a hot, nearly white pass added on top of it, so it reads as a light, not a colour.
    if (live) {
      GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA,
          GlStateManager.DestFactor.ONE);
      sleeve(tess, buf, cx, cz, y0, y1, style.radius + 0.012f, 1f,
          style.g + (1 - style.g) * style.whiten, style.b + (1 - style.b) * style.whiten,
          on * HOT_SLEEVE_SHARE);
    } else {
      GlStateManager.blendFunc(GL_CONSTANT_ALPHA, GL11.GL_ONE);
      GL14.glBlendColor(0f, 0f, 0f, on * HOT_SLEEVE_SHARE);
      callPart(style, PART_HOT_SLEEVE);
      GlStateManager.popMatrix();
    }

    // The glow and the bloom face the camera. A disc through the lens would be cut in half by the
    // roof or pole under the beacon and by its own body, which the depth test puts in front of
    // everything behind them; so each is pulled toward the camera by about its own radius, clear
    // of what is round the beacon, and a wall between the viewer and the beacon still hides it.
    float bloomR = Math.max(bloom, minSize * 4f);
    float glowR = Math.max(style.glow, minSize * 1.5f);
    float len = (float) Math.sqrt(cx * cx + cy * cy + cz * cz);
    RenderManager rm = Minecraft.getMinecraft().getRenderManager();
    float bloomA = on * (0.4f + 0.6f * dark);
    billboard(cx, cy, cz, len, bloomR, rm);
    if (live) {
      radial(tess, buf, bloomR, style, bloomA, BLOOM_AT, BLOOM_ALPHA);
    } else {
      GlStateManager.scale(bloomR, bloomR, bloomR);
      GL14.glBlendColor(0f, 0f, 0f, bloomA);
      callPart(style, PART_BLOOM);
    }
    GlStateManager.popMatrix();
    billboard(cx, cy, cz, len, glowR, rm);
    if (live) {
      radial(tess, buf, glowR, style, on, GLOW_AT, GLOW_ALPHA);
    } else {
      GlStateManager.scale(glowR, glowR, glowR);
      GL14.glBlendColor(0f, 0f, 0f, on);
      callPart(style, PART_GLOW);
      // The blend colour is not GlStateManager's; leave it at GL's default for whatever follows.
      GL14.glBlendColor(0f, 0f, 0f, 0f);
    }
    GlStateManager.popMatrix();

    GlStateManager.shadeModel(GL11.GL_FLAT);
    GlStateManager.enableAlpha();
    GlStateManager.depthMask(true);
    GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA,
        GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
    GlStateManager.disableBlend();
    GlStateManager.enableCull();
    GlStateManager.enableLighting();
    GlStateManager.popMatrix();
  }

  /**
   * Replays one part's shared list, compiling it on first use, or draws it directly if the driver
   * refused a list name. The caller has set the matrix, the blend and its constant alpha.
   */
  private static void callPart(Style style, int part) {
    long key = listKey(style.index, part);
    int list = LISTS.get(key);
    if (list == CsmDisplayListCache.NO_LIST) {
      list = LISTS.allocate(key);
      if (list != CsmDisplayListCache.NO_LIST) {
        GL11.glNewList(list, GL11.GL_COMPILE);
        drawPart(style, part);
        GL11.glEndList();
      }
    }
    if (list != CsmDisplayListCache.NO_LIST) {
      GL11.glCallList(list);
    } else {
      drawPart(style, part);
    }
    // The vertex colours leave GL's current colour at the last vertex's, which GlStateManager does
    // not know about (a direct draw resets its cache afterwards; a replay does not).
    GlStateManager.resetColor();
  }

  /**
   * One part's geometry for a shared list, in the block's own frame (the sleeves) or as a disc of
   * unit radius (the bloom and glow). Every vertex alpha that the live draw scales by the flash is
   * left to the blend's constant alpha. Geometry only: the caller owns every GL state.
   */
  private static void drawPart(Style style, int part) {
    Tessellator tess = Tessellator.getInstance();
    BufferBuilder buf = tess.getBuffer();
    switch (part) {
      case PART_SLEEVE:
        sleeve(tess, buf, 0.5f, 0.5f, style.y0, style.y1, style.radius + 0.01f, style.r,
            style.g, style.b, 1f);
        break;
      case PART_HOT_SLEEVE:
        sleeve(tess, buf, 0.5f, 0.5f, style.y0, style.y1, style.radius + 0.012f, 1f,
            style.g + (1 - style.g) * style.whiten, style.b + (1 - style.b) * style.whiten, 1f);
        break;
      case PART_BLOOM:
        premultipliedRadial(tess, buf, style, BLOOM_AT, BLOOM_ALPHA);
        break;
      default:
        premultipliedRadial(tess, buf, style, GLOW_AT, GLOW_ALPHA);
        break;
    }
  }

  /** A fullbright vertex in the BLOCK format: position, colour, texture, lightmap. */
  private static void vertex(BufferBuilder buf, double x, double y, double z, float r, float g,
      float b, float a) {
    buf.pos(x, y, z).color(r, g, b, a).tex(0.5, 0.5).lightmap(FULLBRIGHT, FULLBRIGHT)
        .endVertex();
  }

  /**
   * Pushes a matrix placing a camera-facing disc at the lens, pulled toward the camera by its
   * radius (never past the camera) and shrunk to match, so it covers what it would at the lens.
   * Pop it after drawing.
   */
  private static void billboard(float cx, float cy, float cz, float len, float radius,
      RenderManager rm) {
    GlStateManager.pushMatrix();
    float pull = len > 1e-3f ? Math.min(radius, Math.max(0f, len - 0.3f)) / len : 0f;
    GlStateManager.translate(cx - cx * pull, cy - cy * pull, cz - cz * pull);
    GlStateManager.rotate(-rm.playerViewY, 0f, 1f, 0f);
    GlStateManager.rotate(rm.playerViewX, 1f, 0f, 0f);
    // Nearer the camera it would look bigger: shrink it by as much, so it looks as it would at
    // the lens.
    float keep = 1f - pull;
    GlStateManager.scale(keep, keep, keep);
  }

  /** An octagon tube round the lens, open at both ends. */
  private static void sleeve(Tessellator tess, BufferBuilder buf, float cx, float cz, float y0,
      float y1, float radius, float r, float g, float b, float a) {
    buf.begin(GL11.GL_QUADS, DefaultVertexFormats.BLOCK);
    for (int i = 0; i < SIDES; i++) {
      double a0 = Math.PI * 2 * (i + 0.5) / SIDES;
      double a1 = Math.PI * 2 * (i + 1.5) / SIDES;
      float x0 = cx + (float) Math.cos(a0) * radius;
      float z0 = cz + (float) Math.sin(a0) * radius;
      float x1 = cx + (float) Math.cos(a1) * radius;
      float z1 = cz + (float) Math.sin(a1) * radius;
      vertex(buf, x0, y0, z0, r, g, b, a);
      vertex(buf, x1, y0, z1, r, g, b, a);
      vertex(buf, x1, y1, z1, r, g, b, a);
      vertex(buf, x0, y1, z0, r, g, b, a);
    }
    tess.draw();
  }

  /**
   * A round glow in the plane facing the camera, as rings: {@code alphas[k]} at {@code at[k]} of
   * the radius, whitened toward the middle. Rings, not one fan, so the core stays bright and the
   * edge soft rather than the whole disc fading evenly from centre to rim.
   */
  private static void radial(Tessellator tess, BufferBuilder buf, float radius, Style s,
      float a, float[] at, float[] alphas) {
    buf.begin(GL11.GL_TRIANGLES, DefaultVertexFormats.BLOCK);
    for (int k = 0; k + 1 < at.length; k++) {
      float r0 = at[k] * radius;
      float r1 = at[k + 1] * radius;
      float w0 = 1f - at[k];
      float w1 = 1f - at[k + 1];
      for (int i = 0; i < FAN; i++) {
        double t0 = Math.PI * 2 * i / FAN;
        double t1 = Math.PI * 2 * (i + 1) / FAN;
        double c0 = Math.cos(t0);
        double s0 = Math.sin(t0);
        double c1 = Math.cos(t1);
        double s1 = Math.sin(t1);
        ring(buf, c0 * r0, s0 * r0, s, w0, a * alphas[k]);
        ring(buf, c0 * r1, s0 * r1, s, w1, a * alphas[k + 1]);
        ring(buf, c1 * r1, s1 * r1, s, w1, a * alphas[k + 1]);
        ring(buf, c0 * r0, s0 * r0, s, w0, a * alphas[k]);
        ring(buf, c1 * r1, s1 * r1, s, w1, a * alphas[k + 1]);
        ring(buf, c1 * r0, s1 * r0, s, w0, a * alphas[k]);
      }
    }
    tess.draw();
  }

  /** A glow vertex, its colour blended toward white by {@code white} (1 at the centre). */
  private static void ring(BufferBuilder buf, double x, double y, Style s, float white, float a) {
    float w = white * white * s.whiten;
    vertex(buf, x, y, 0, s.r + (1 - s.r) * w, s.g + (1 - s.g) * w, s.b + (1 - s.b) * w, a);
  }

  /**
   * The same rings at unit radius for a shared list, each vertex's colour premultiplied by its
   * ring's alpha. Drawn with {@code GL_CONSTANT_ALPHA, GL_ONE} and the flash as the constant,
   * {@code colour * ringAlpha * flash} is added to the screen, as the live {@code SRC_ALPHA, ONE}
   * draw added {@code colour * (flash * ringAlpha)}.
   */
  private static void premultipliedRadial(Tessellator tess, BufferBuilder buf, Style s,
      float[] at, float[] alphas) {
    buf.begin(GL11.GL_TRIANGLES, DefaultVertexFormats.BLOCK);
    for (int k = 0; k + 1 < at.length; k++) {
      float r0 = at[k];
      float r1 = at[k + 1];
      float[] c0v = premultiplied(s, 1f - at[k], alphas[k]);
      float[] c1v = premultiplied(s, 1f - at[k + 1], alphas[k + 1]);
      for (int i = 0; i < FAN; i++) {
        double t0 = Math.PI * 2 * i / FAN;
        double t1 = Math.PI * 2 * (i + 1) / FAN;
        double c0 = Math.cos(t0);
        double s0 = Math.sin(t0);
        double c1 = Math.cos(t1);
        double s1 = Math.sin(t1);
        vertex(buf, c0 * r0, s0 * r0, 0, c0v[0], c0v[1], c0v[2], c0v[3]);
        vertex(buf, c0 * r1, s0 * r1, 0, c1v[0], c1v[1], c1v[2], c1v[3]);
        vertex(buf, c1 * r1, s1 * r1, 0, c1v[0], c1v[1], c1v[2], c1v[3]);
        vertex(buf, c0 * r0, s0 * r0, 0, c0v[0], c0v[1], c0v[2], c0v[3]);
        vertex(buf, c1 * r1, s1 * r1, 0, c1v[0], c1v[1], c1v[2], c1v[3]);
        vertex(buf, c1 * r0, s1 * r0, 0, c0v[0], c0v[1], c0v[2], c0v[3]);
      }
    }
    tess.draw();
  }

  /**
   * A glow colour blended toward white by {@code white} (1 at the centre).
   *
   * @return red, green and blue
   */
  static float[] ringColour(Style s, float white) {
    float w = white * white * s.whiten;
    return new float[]{s.r + (1 - s.r) * w, s.g + (1 - s.g) * w, s.b + (1 - s.b) * w};
  }

  /**
   * A shared-list glow vertex's colour: {@link #ringColour} times the ring's alpha, which also
   * stays the vertex alpha. The blend reads only the constant alpha, never this one.
   *
   * @return red, green, blue and alpha
   */
  static float[] premultiplied(Style s, float white, float ringAlpha) {
    float[] c = ringColour(s, white);
    return new float[]{c[0] * ringAlpha, c[1] * ringAlpha, c[2] * ringAlpha, ringAlpha};
  }
}

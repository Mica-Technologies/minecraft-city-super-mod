package com.micatechnologies.minecraft.csm.constructionsite;

import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * Builds a tower crane's slewing unit -- everything above the mast -- as quads into a
 * {@code DefaultVertexFormats.BLOCK} buffer, in the head's local frame: origin at the centre of
 * the mast top, y up, the jib out along +x. The renderer turns the whole thing to its slew
 * outside, so slewing never rebuilds it.
 *
 * <p>Everything is a bar between two points or an axis box, textured with a sprite from the block
 * atlas, so a whole crane is one texture and compiles into one display list. Every vertex carries
 * the world's light at the head. The aviation lights' lenses are drawn here unlit; their flash
 * changes every frame, so the renderer draws it outside the list.</p>
 *
 * <p>The jibs are triangular lattice girders -- two bottom chords and a top chord, braced in a
 * zigzag on all three faces -- because that is the section almost every tower crane jib actually
 * is, and it is what makes a jib read as a crane rather than a beam at any distance.</p>
 *
 * @version 1.0
 * @since 2026.9
 */
@SideOnly(Side.CLIENT)
public final class CraneGeometry {

  private final BufferBuilder buf;
  private final int light;
  private final double s;

  private final TextureAtlasSprite steel;
  private final TextureAtlasSprite dark;
  private final TextureAtlasSprite ballast;
  private final TextureAtlasSprite cab;
  private final TextureAtlasSprite glass;
  private final TextureAtlasSprite lamp;
  private final TextureAtlasSprite plank;

  /** Where each aviation light's lens is, for the renderer to light it. */
  private final List<double[]> lamps;

  private CraneGeometry(BufferBuilder buf, CraneLivery livery, int light, int scale,
      List<double[]> lamps) {
    this.buf = buf;
    this.lamps = lamps;
    this.light = light;
    this.s = scale;
    TextureMap atlas = Minecraft.getMinecraft().getTextureMapBlocks();
    this.steel = atlas.getAtlasSprite("csm:blocks/constructionsite/crane_chord_"
        + livery.getName());
    this.dark = atlas.getAtlasSprite("csm:blocks/constructionsite/form_clamp");
    this.ballast = atlas.getAtlasSprite("csm:blocks/constructionsite/form_concrete");
    this.cab = atlas.getAtlasSprite("csm:blocks/constructionsite/crane_chord_white");
    this.glass = atlas.getAtlasSprite("minecraft:blocks/glass_light_blue");
    this.lamp = atlas.getAtlasSprite("minecraft:blocks/redstone_block");
    this.plank = atlas.getAtlasSprite("csm:blocks/constructionsite/scaffold_plank");
  }

  /**
   * Builds a whole crane head into {@code buf}, which must already be begun in quads with the
   * block vertex format.
   *
   * @param buf   the buffer
   * @param te    the crane's head
   * @param light the packed light at the head
   * @param lamps receives each aviation light's lens centre, in the same frame, as
   *              {@code {x, y, z}}: the lenses are drawn unlit here, and the renderer flashes
   *              them every frame outside the compiled geometry
   *
   * @since 1.0
   */
  public static void build(BufferBuilder buf, TileEntityCraneHead te, int light,
      List<double[]> lamps) {
    CraneGeometry g = new CraneGeometry(buf, te.getLivery(), light, te.getScale(), lamps);
    g.slewingUnit();
    switch (te.getModel()) {
      case HAMMERHEAD:
        g.flatTop(te, true);
        break;
      case LUFFING:
        g.luffing(te);
        break;
      default:
        g.flatTop(te, false);
        break;
    }
  }

  // --- the three models ------------------------------------------------------------------------

  /** Height of the jib's bottom chords above the mast top. */
  private double jibBase() {
    return 0.55 * s;
  }

  private double jibWidth() {
    return 0.8 * s;
  }

  private double jibDepth() {
    return 0.9 * s;
  }

  /**
   * A flat-top, or with {@code hammerhead} a hammerhead: the same jib and counter-jib, plus an
   * A-frame tower head with pendant lines down to both.
   */
  private void flatTop(TileEntityCraneHead te, boolean hammerhead) {
    double l = te.getJibLength();
    double y0 = jibBase();
    double w = jibWidth();
    double h = jibDepth();
    // The jib, from the edge of the slewing unit to its tip.
    girder(0.5 * s, y0, l, y0, w, h);
    lamp(l - 0.1 * s, y0 + h, 0.0);
    // The counter-jib, a third of the jib, with its winch and hanging ballast.
    double lc = Math.max(4.0 * s, l / 3.0);
    counterJib(lc, y0);
    cab(y0);
    trolleyAndHook(te, l, y0);
    if (hammerhead) {
      double apex = y0 + 5.0 * s;
      towerHead(apex);
      lamp(0.0, apex + 0.15 * s, 0.0);
      // Pendant lines from the apex to the jib's top chord and to the counter-jib's end.
      rope(0.0, apex, 0.0, 0.4 * l, y0 + h, 0.0);
      rope(0.0, apex, 0.0, 0.75 * l, y0 + h, 0.0);
      rope(0.0, apex, -0.2 * s, -lc + 0.5 * s, y0 + 0.4 * s, -0.4 * s);
      rope(0.0, apex, 0.2 * s, -lc + 0.5 * s, y0 + 0.4 * s, 0.4 * s);
    }
  }

  /**
   * A luffing jib: pinned at the slewing unit and raked up at the luff angle, held by luffing
   * ropes from a short A-frame behind, with the hook hanging from the tip, since a luffing crane
   * has no trolley.
   */
  private void luffing(TileEntityCraneHead te) {
    double l = te.getJibLength();
    double y0 = jibBase();
    double a = Math.toRadians(te.getLuff());
    double footX = 0.4 * s;
    double footY = y0 + 0.2 * s;
    double tipX = footX + l * Math.cos(a);
    double tipY = footY + l * Math.sin(a);
    girder(footX, footY, tipX, tipY, jibWidth(), jibDepth());
    lamp(tipX, tipY + jibDepth(), 0.0);
    double lc = Math.max(3.0 * s, l / 4.0);
    counterJib(lc, y0);
    cab(y0);
    double apex = y0 + 3.5 * s;
    towerHead(apex);
    // Luffing ropes, several falls, from the A-frame's top to the jib tip.
    for (double dz : new double[]{-0.15 * s, 0.15 * s}) {
      rope(-0.2 * s, apex, dz, tipX, tipY + jibDepth(), dz);
    }
    // Hoist rope and hook from the tip.
    double hookY = tipY - te.getHookDrop();
    hook(tipX, tipY, hookY);
  }

  // --- parts -----------------------------------------------------------------------------------

  /** The slewing ring and the platform above it. */
  private void slewingUnit() {
    box(dark, -0.45 * s, 0.0, -0.45 * s, 0.45 * s, 0.25 * s, 0.45 * s);
    box(steel, -0.55 * s, 0.25 * s, -0.55 * s, 0.55 * s, 0.45 * s, 0.55 * s);
  }

  /** The operator's cab, beside the jib root, glazed toward the jib. */
  private void cab(double y0) {
    double z0 = 0.5 * s;
    double z1 = 1.25 * s;
    box(cab, 0.05 * s, y0 - 0.35 * s, z0, 0.9 * s, y0 + 0.55 * s, z1);
    // The glazing on the front and the side away from the jib.
    box(glass, 0.9 * s, y0 - 0.1 * s, z0 + 0.08 * s, 0.93 * s, y0 + 0.45 * s, z1 - 0.08 * s);
    box(glass, 0.15 * s, y0 - 0.1 * s, z1, 0.8 * s, y0 + 0.45 * s, z1 + 0.03 * s);
  }

  /**
   * The counter-jib: two box rails back to {@code lc}, a walkway between them, the hoist winch
   * on top, and ballast blocks hanging at the end.
   */
  private void counterJib(double lc, double y0) {
    double t = 0.14 * s;
    double z = 0.4 * s;
    box(steel, -lc, y0, -z - t / 2, -0.5 * s, y0 + 0.35 * s, -z + t / 2);
    box(steel, -lc, y0, z - t / 2, -0.5 * s, y0 + 0.35 * s, z + t / 2);
    box(plank, -lc, y0 + 0.05 * s, -z + t / 2, -0.5 * s, y0 + 0.1 * s, z - t / 2);
    // Winch.
    double wx = -lc * 0.45;
    box(dark, wx - 0.45 * s, y0 + 0.1 * s, -0.3 * s, wx + 0.45 * s, y0 + 0.65 * s, 0.3 * s);
    // Ballast: a stack of concrete slabs hung at the end, below and above the rails.
    double bx0 = -lc + 0.05 * s;
    double bx1 = -lc + 1.05 * s;
    for (int i = 0; i < 4; i++) {
      double yb = y0 - 1.2 * s + i * 0.42 * s;
      box(ballast, bx0, yb, -0.5 * s, bx1, yb + 0.38 * s, 0.5 * s);
    }
    lamp(-lc + 0.1 * s, y0 + 0.35 * s, 0.0);
  }

  /**
   * The A-frame tower head: four legs from the platform's corners up to the apex, with a
   * horizontal frame halfway.
   */
  private void towerHead(double apex) {
    double b = 0.45 * s;
    double base = 0.45 * s;
    double t = 0.1 * s;
    double[][] feet = {{-b, -b}, {b, -b}, {b, b}, {-b, b}};
    for (double[] f : feet) {
      bar(steel, f[0], base, f[1], 0.0, apex, 0.0, t);
    }
    double mid = (base + apex) / 2;
    double m = b / 2;
    bar(steel, -m, mid, -m, m, mid, -m, t * 0.7);
    bar(steel, m, mid, -m, m, mid, m, t * 0.7);
    bar(steel, m, mid, m, -m, mid, m, t * 0.7);
    bar(steel, -m, mid, m, -m, mid, -m, t * 0.7);
  }

  /** The trolley riding the jib's bottom chords, and the hook hanging from it. */
  private void trolleyAndHook(TileEntityCraneHead te, double l, double y0) {
    double start = 2.0 * s;
    double tx = start + (l - 1.0 * s - start) * te.getTrolley();
    double w = jibWidth();
    box(dark, tx - 0.4 * s, y0 - 0.25 * s, -w / 2, tx + 0.4 * s, y0, w / 2);
    hook(tx, y0 - 0.25 * s, y0 - te.getHookDrop());
  }

  /** Two hoist ropes from {@code topY} down to a hook block at {@code hookY}. */
  private void hook(double x, double topY, double hookY) {
    double r = 0.18 * s;
    rope(x - r, topY, 0.0, x - r, hookY + 0.5 * s, 0.0);
    rope(x + r, topY, 0.0, x + r, hookY + 0.5 * s, 0.0);
    box(steel, x - 0.3 * s, hookY + 0.1 * s, -0.2 * s, x + 0.3 * s, hookY + 0.55 * s, 0.2 * s);
    box(dark, x - 0.06 * s, hookY - 0.25 * s, -0.06 * s, x + 0.06 * s, hookY + 0.1 * s,
        0.06 * s);
    box(dark, x - 0.06 * s, hookY - 0.3 * s, -0.06 * s, x + 0.2 * s, hookY - 0.2 * s, 0.06 * s);
  }

  /**
   * An aviation obstruction light: a dark base and a red lens, in the world's light. The flash
   * and its glow are not geometry -- see {@link TileEntityCraneHeadRenderer} -- so the lens is
   * recorded for the renderer.
   */
  private void lamp(double x, double y, double z) {
    double rb = 0.12 * s;
    double r = 0.09 * s;
    box(dark, x - rb, y, z - rb, x + rb, y + 0.06 * s, z + rb);
    box(lamp, x - r, y + 0.06 * s, z - r, x + r, y + 0.26 * s, z + r);
    lamps.add(new double[]{x, y + 0.16 * s, z});
  }

  /** A rope: a thin dark bar. */
  private void rope(double ax, double ay, double az, double bx, double by, double bz) {
    bar(dark, ax, ay, az, bx, by, bz, 0.035 * s);
  }

  /**
   * A triangular lattice girder from (x0, y0) to (x1, y1) in the xy plane: two bottom chords at
   * z = +-w/2 and a top chord at depth {@code h} above, braced in a zigzag on the two sloped
   * faces and the bottom. Panels are about as long as the girder is deep, which is the
   * proportion a real jib's bracing has.
   */
  private void girder(double x0, double y0, double x1, double y1, double w, double h) {
    double dx = x1 - x0;
    double dy = y1 - y0;
    double len = Math.sqrt(dx * dx + dy * dy);
    double ux = dx / len;
    double uy = dy / len;
    // "Up" for the girder: its axis turned a quarter in the xy plane.
    double nx = -uy;
    double ny = ux;
    double chord = 0.1 * s;
    double brace = 0.05 * s;
    double hw = w / 2;
    // The three chords.
    bar(steel, x0, y0, -hw, x1, y1, -hw, chord);
    bar(steel, x0, y0, hw, x1, y1, hw, chord);
    bar(steel, x0 + nx * h, y0 + ny * h, 0.0, x1 + nx * h, y1 + ny * h, 0.0, chord);
    int panels = Math.max(1, (int) Math.round(len / h));
    double p = len / panels;
    for (int i = 0; i < panels; i++) {
      double a = i * p;
      double m = a + p / 2;
      double b = a + p;
      double ax = x0 + ux * a;
      double ay = y0 + uy * a;
      double mx = x0 + ux * m + nx * h;
      double my = y0 + uy * m + ny * h;
      double bx = x0 + ux * b;
      double by = y0 + uy * b;
      // Zigzag up each sloped face to the top chord and back down.
      for (double side : new double[]{-hw, hw}) {
        bar(steel, ax, ay, side, mx, my, 0.0, brace);
        bar(steel, mx, my, 0.0, bx, by, side, brace);
      }
      // Zigzag across the bottom, alternating sides.
      double from = (i % 2 == 0) ? -hw : hw;
      bar(steel, ax, ay, from, bx, by, -from, brace);
    }
  }

  // --- primitives ------------------------------------------------------------------------------

  private void box(TextureAtlasSprite sp, double x0, double y0, double z0, double x1, double y1,
      double z1) {
    int lm = light;
    double[][] c = {
        {x0, y0, z0}, {x1, y0, z0}, {x1, y1, z0}, {x0, y1, z0},
        {x0, y0, z1}, {x1, y0, z1}, {x1, y1, z1}, {x0, y1, z1}};
    face(sp, c[4], c[5], c[6], c[7], 0, 0, 1, lm);
    face(sp, c[1], c[0], c[3], c[2], 0, 0, -1, lm);
    face(sp, c[5], c[1], c[2], c[6], 1, 0, 0, lm);
    face(sp, c[0], c[4], c[7], c[3], -1, 0, 0, lm);
    face(sp, c[3], c[7], c[6], c[2], 0, 1, 0, lm);
    face(sp, c[0], c[1], c[5], c[4], 0, -1, 0, lm);
  }

  /**
   * A square-section bar of thickness {@code t} from a to b, in any direction: four sides and two
   * end caps. The section is squared to the world's up unless the bar is nearly vertical.
   */
  private void bar(TextureAtlasSprite sp, double ax, double ay, double az, double bx, double by,
      double bz, double t) {
    double dx = bx - ax;
    double dy = by - ay;
    double dz = bz - az;
    double len = Math.sqrt(dx * dx + dy * dy + dz * dz);
    if (len < 1.0E-6) {
      return;
    }
    dx /= len;
    dy /= len;
    dz /= len;
    // Side vector r = d x up, or d x east for a near-vertical bar.
    double rx;
    double ry;
    double rz;
    if (Math.abs(dy) > 0.95) {
      rx = 0.0;
      ry = dz;
      rz = -dy;
    } else {
      rx = -dz;
      ry = 0.0;
      rz = dx;
    }
    double rl = Math.sqrt(rx * rx + ry * ry + rz * rz);
    rx /= rl;
    ry /= rl;
    rz /= rl;
    // u = r x d
    double ux = ry * dz - rz * dy;
    double uy = rz * dx - rx * dz;
    double uz = rx * dy - ry * dx;
    double h = t / 2;
    double[][] ca = corners(ax, ay, az, rx, ry, rz, ux, uy, uz, h);
    double[][] cb = corners(bx, by, bz, rx, ry, rz, ux, uy, uz, h);
    for (int i = 0; i < 4; i++) {
      int j = (i + 1) % 4;
      double ox = (ca[i][0] + ca[j][0]) / 2 - ax;
      double oy = (ca[i][1] + ca[j][1]) / 2 - ay;
      double oz = (ca[i][2] + ca[j][2]) / 2 - az;
      face(sp, ca[i], ca[j], cb[j], cb[i], ox, oy, oz, light);
    }
    face(sp, ca[3], ca[2], ca[1], ca[0], -dx, -dy, -dz, light);
    face(sp, cb[0], cb[1], cb[2], cb[3], dx, dy, dz, light);
  }

  private static double[][] corners(double x, double y, double z, double rx, double ry,
      double rz, double ux, double uy, double uz, double h) {
    return new double[][]{
        {x - rx * h - ux * h, y - ry * h - uy * h, z - rz * h - uz * h},
        {x + rx * h - ux * h, y + ry * h - uy * h, z + rz * h - uz * h},
        {x + rx * h + ux * h, y + ry * h + uy * h, z + rz * h + uz * h},
        {x - rx * h + ux * h, y - ry * h + uy * h, z - rz * h + uz * h}};
  }

  /**
   * One quad, wound so its front faces {@code (ox, oy, oz)} -- the outward direction -- whatever
   * order its corners came in, and shaded by that direction the way vanilla shades block faces:
   * up 1.0, down 0.5, north-south 0.8, east-west 0.6, blended for anything in between.
   */
  private void face(TextureAtlasSprite sp, double[] a, double[] b, double[] c, double[] d,
      double ox, double oy, double oz, int lm) {
    double e1x = b[0] - a[0];
    double e1y = b[1] - a[1];
    double e1z = b[2] - a[2];
    double e2x = c[0] - a[0];
    double e2y = c[1] - a[1];
    double e2z = c[2] - a[2];
    double nx = e1y * e2z - e1z * e2y;
    double ny = e1z * e2x - e1x * e2z;
    double nz = e1x * e2y - e1y * e2x;
    if (nx * ox + ny * oy + nz * oz < 0) {
      double[] tmp = b;
      b = d;
      d = tmp;
    }
    double ol = Math.sqrt(ox * ox + oy * oy + oz * oz);
    double fx = ox / ol;
    double fy = oy / ol;
    double fz = oz / ol;
    float shade = (float) (fx * fx * 0.6 + fy * fy * (fy > 0 ? 1.0 : 0.5) + fz * fz * 0.8);
    int grey = Math.min(255, (int) (shade * 255));
    int sky = (lm >> 16) & 0xFFFF;
    int block = lm & 0xFFFF;
    float u0 = sp.getMinU();
    float u1 = sp.getMaxU();
    float v0 = sp.getMinV();
    float v1 = sp.getMaxV();
    vertex(a, grey, u0, v1, sky, block);
    vertex(b, grey, u1, v1, sky, block);
    vertex(c, grey, u1, v0, sky, block);
    vertex(d, grey, u0, v0, sky, block);
  }

  private void vertex(double[] p, int grey, float u, float v, int sky, int block) {
    buf.pos(p[0], p[1], p[2]).color(grey, grey, grey, 255).tex(u, v).lightmap(sky, block)
        .endVertex();
  }
}

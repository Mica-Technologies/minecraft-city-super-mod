package com.micatechnologies.minecraft.csm.buildingmaterials.plaques;

import com.micatechnologies.minecraft.csm.buildingmaterials.plaques.PlaqueStyle.Treatment;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Where everything on an address plaque goes, in the plaque's own space: px across the block as
 * the reader sees it ({@code x}, 0 at their left), px up it ({@code y}), and px out from the wall
 * ({@code d}, 0 on the wall's face). The baked model turns this onto the block's facing. Kept free
 * of the client so it can be tested.
 *
 * <p><b>The plate</b> is a box {@link PlaqueStyle#depth} deep, centred in the block and moved by
 * {@code dx}/{@code dy}; its front is the style's nine-slice tile (the border band stays
 * {@link PlaqueStyle#border} px whatever the plate's size), its sides a flat edge colour.</p>
 *
 * <p><b>The text</b>, laid flat: the number alone fills 0.95 of the height inside the border,
 * or 0.62 with a street line (0.3) under it, measured on the glyphs' own extent (a font's digits
 * often stand taller than its capitals, and a street line has descenders); each line is shrunk to
 * fit across and the whole is scaled by the plaque's size. As a vertical stack the number's
 * characters are set one under another, 0.2 of a capital apart, each centred, the street line
 * left out. Glyph boxes are the sheet's, so nothing is
 * stretched.</p>
 *
 * <p><b>Layers</b> stand at least {@link #LIFT} px apart, the gap the JSON model generators keep
 * between faces (model_depth.py) so nothing z-fights within about 100 blocks.</p>
 *
 * @since 2026.11
 */
public final class PlaqueLayout {

  /** The least distance between two layers that overlap, px. */
  public static final float LIFT = 0.2f;
  /** Inside the border, a clear margin round the text, px. */
  static final float MARGIN = 0.25f;
  /** How far stand-off numerals stand off what is behind them, px. */
  static final float STANDOFF = 1.5f;
  /** The decal's plane, px in front of the glass it is on. */
  static final float DECAL_GAP = 0.3f;

  /** One textured rectangle facing the reader (or, {@code back}, facing the wall). */
  public static final class Piece {

    public final float x0;
    public final float y0;
    public final float x1;
    public final float y1;
    public final float d;
    public final boolean back;
    /** 0: the glyph sheet; 1: the body sheet. */
    public final int sheet;
    /** The sheet window, px; u0,v0 at (x0, y1), the piece's top left. */
    public final float u0;
    public final float v0;
    public final float u1;
    public final float v1;
    public final int rgb;
    /** Whether the piece takes the world's directional shading (a lit lens does not). */
    public final boolean shade;

    Piece(float x0, float y0, float x1, float y1, float d, boolean back, int sheet, float u0,
        float v0, float u1, float v1, int rgb, boolean shade) {
      this.x0 = x0;
      this.y0 = y0;
      this.x1 = x1;
      this.y1 = y1;
      this.d = d;
      this.back = back;
      this.sheet = sheet;
      this.u0 = u0;
      this.v0 = v0;
      this.u1 = u1;
      this.v1 = v1;
      this.rgb = rgb;
      this.shade = shade;
    }
  }

  /** The laid out plaque. */
  public static final class Result {

    /** The plate, x0, y0, x1, y1 px, or null for a style with none. */
    public final float[] plate;
    public final float depth;
    public final int edgeRgb;
    public final List<Piece> pieces;
    /** The hitbox, px: x0, y0, d0, x1, y1, d1, inside the block. */
    public final float[] box;

    Result(float[] plate, float depth, int edgeRgb, List<Piece> pieces, float[] box) {
      this.plate = plate;
      this.depth = depth;
      this.edgeRgb = edgeRgb;
      this.pieces = Collections.unmodifiableList(pieces);
      this.box = box;
    }
  }

  private PlaqueLayout() {
  }

  /** A glyph's numbers on the sheet: x, y, w, h, left, top, advance (1/16 px). */
  private static int[] glyph(PlaqueFont font, char c) {
    int i = PlaqueSheet.CHARS.indexOf(c);
    if (i < 0) {
      i = 0;
    }
    int[] t = PlaqueSheet.GLYPHS[font.sheetIndex()];
    int o = i * 7;
    return new int[]{t[o], t[o + 1], t[o + 2], t[o + 3], t[o + 4], t[o + 5], t[o + 6]};
  }

  /** A line's advance in caps. */
  static float widthInCaps(PlaqueFont font, String text) {
    float w = 0;
    for (int i = 0; i < text.length(); i++) {
      w += glyph(font, text.charAt(i))[6] / 16f;
    }
    return w / PlaqueSheet.CAP;
  }

  /** The widest of a text's characters, in caps (ink, not advance). */
  static float widestInCaps(PlaqueFont font, String text) {
    float w = 0;
    for (int i = 0; i < text.length(); i++) {
      int[] g = glyph(font, text.charAt(i));
      w = Math.max(w, Math.max(g[6] / 16f, g[2]));
    }
    return w / PlaqueSheet.CAP;
  }

  /**
   * Lays out a plaque.
   *
   * @param style  the block
   * @param plaque what it says
   * @return its plate, pieces and hitbox
   */
  public static Result layout(PlaqueStyle style, AddressPlaque plaque) {
    PlaqueSize size = plaque.getPlate();
    boolean vertical = plaque.isVertical();
    float w = vertical ? size.stackWidth : size.width;
    float h = vertical ? size.stackHeight : size.height;
    float cx = 8f + plaque.getDx();
    float cy = 8f + plaque.getDy();
    // never out of the block
    cx = Math.max(w / 2f, Math.min(16f - w / 2f, cx));
    cy = Math.max(h / 2f, Math.min(16f - h / 2f, cy));
    float x0 = cx - w / 2f;
    float y0 = cy - h / 2f;
    float x1 = cx + w / 2f;
    float y1 = cy + h / 2f;

    List<Piece> out = new ArrayList<>();
    float face;
    float[] plate = null;
    float inset;
    if (style.hasPlate()) {
      plate = new float[]{x0, y0, x1, y1};
      face = style.depth;
      float b = Math.min(style.border, Math.min(w, h) / 6f);
      String tile = style.isLamp() ? (plaque.isLit() ? "lamp_lit" : "lamp_unlit")
          : style.getTile();
      nineSlice(out, tile, x0, y0, x1, y1, b, face, !(style.isLamp() && plaque.isLit()));
      inset = b + MARGIN;
    } else if (style.isTwoSided()) {
      face = DECAL_GAP - plaque.getDz();
      inset = 0.2f;
    } else {
      face = 0f;
      inset = 0.2f;
    }

    PlaqueFont font = plaque.fontFor(style);
    int ink = plaque.inkFor(style);
    List<float[]> glyphs = new ArrayList<>();   // x0, y0, x1, y1, u0, v0, u1, v1
    if (vertical) {
      stack(glyphs, font, plaque.getNumber(), x0 + inset, y0 + inset, x1 - inset, y1 - inset,
          plaque.getSize());
    } else {
      lines(glyphs, font, plaque.getNumber(), plaque.getStreet(), x0 + inset, y0 + inset,
          x1 - inset, y1 - inset, plaque.getSize());
    }
    boolean lit = style.isLamp() && plaque.isLit();
    float top = treat(out, style.treatment, glyphs, face, ink, lit, style);

    float[] box;
    if (plate != null) {
      box = new float[]{x0, y0, 0f, x1, y1, Math.max(style.depth, 0.5f)};
    } else {
      float[] ink0 = bounds(glyphs, x0, y0, x1, y1);
      float d0 = style.isTwoSided() ? face - LIFT : 0f;
      float d1 = style.isTwoSided() ? top : top;
      box = new float[]{ink0[0], ink0[1], d0, ink0[2], ink0[3], Math.max(d1, d0 + 0.5f)};
    }
    // the hitbox stays in the block: a decal pushed into the next block keeps a sliver on the
    // face it passed through
    box[2] = Math.max(0f, Math.min(15.5f, box[2]));
    box[5] = Math.max(box[2] + 0.5f, Math.min(16f, box[5]));
    return new Result(plate, style.hasPlate() ? style.depth : 0f, style.edgeRgb, out, box);
  }

  private static float[] bounds(List<float[]> glyphs, float x0, float y0, float x1, float y1) {
    if (glyphs.isEmpty()) {
      return new float[]{x0, y0, x1, y1};
    }
    float[] b = {Float.MAX_VALUE, Float.MAX_VALUE, -Float.MAX_VALUE, -Float.MAX_VALUE};
    for (float[] g : glyphs) {
      b[0] = Math.min(b[0], g[0]);
      b[1] = Math.min(b[1], g[1]);
      b[2] = Math.max(b[2], g[2]);
      b[3] = Math.max(b[3], g[3]);
    }
    return b;
  }

  /**
   * The glyphs, drawn as the style draws them.
   *
   * @return the front layer's distance from the wall
   */
  private static float treat(List<Piece> out, Treatment treatment, List<float[]> glyphs,
      float face, int ink, boolean lit, PlaqueStyle style) {
    switch (treatment) {
      case RAISED:
        glyphLayer(out, glyphs, face + LIFT, scale(ink, 0.55f), 0, 0, false, true);
        glyphLayer(out, glyphs, face + 2 * LIFT, ink, 0, 0, false, true);
        return face + 2 * LIFT;
      case INCISED: {
        float o = cap(glyphs) * 0.035f;
        glyphLayer(out, glyphs, face + LIFT, 0xEEE8D8, o, -o, false, true);
        glyphLayer(out, glyphs, face + 2 * LIFT, ink, 0, 0, false, true);
        return face + 2 * LIFT;
      }
      case STANDOFF: {
        float front = face + STANDOFF;
        glyphLayer(out, glyphs, front - 2 * 0.45f, scale(ink, 0.45f), 0, 0, false, true);
        glyphLayer(out, glyphs, front - 0.45f, scale(ink, 0.6f), 0, 0, false, true);
        glyphLayer(out, glyphs, front, ink, 0, 0, false, true);
        return front;
      }
      case OUTLINED: {
        float o = cap(glyphs) * 0.04f;
        int outline = 0x2A1A08;
        for (float[] off : new float[][]{{o, 0}, {-o, 0}, {0, o}, {0, -o}}) {
          glyphLayer(out, glyphs, face, outline, off[0], off[1], false, true);
        }
        glyphLayer(out, glyphs, face + LIFT, ink, 0, 0, false, true);
        // read from behind the glass: the gilding's back, mirrored as the world mirrors it
        glyphLayer(out, glyphs, face - LIFT, ink, 0, 0, true, true);
        return face + LIFT;
      }
      default:
        glyphLayer(out, glyphs, face + LIFT, ink, 0, 0, false, !lit);
        return face + LIFT;
    }
  }

  /** A typical glyph's height in px, for offsets that scale with the text. */
  private static float cap(List<float[]> glyphs) {
    float h = 0;
    for (float[] g : glyphs) {
      h = Math.max(h, g[3] - g[1]);
    }
    return h;
  }

  private static void glyphLayer(List<Piece> out, List<float[]> glyphs, float d, int rgb,
      float ox, float oy, boolean back, boolean shade) {
    for (float[] g : glyphs) {
      out.add(new Piece(g[0] + ox, g[1] + oy, g[2] + ox, g[3] + oy, d, back, 0, g[4], g[5],
          g[6], g[7], rgb, shade));
    }
  }

  static int scale(int rgb, float k) {
    int r = Math.round((rgb >> 16 & 0xFF) * k);
    int g = Math.round((rgb >> 8 & 0xFF) * k);
    int b = Math.round((rgb & 0xFF) * k);
    return r << 16 | g << 8 | b;
  }

  /**
   * How far a text's glyph images reach above and below its baseline, in caps (padding
   * included): a font's digits often stand taller than its capitals, and a street line has
   * descenders.
   *
   * @return {ascent, descent}
   */
  static float[] extentInCaps(PlaqueFont font, String text) {
    float up = 0;
    float down = 0;
    for (int i = 0; i < text.length(); i++) {
      int[] g = glyph(font, text.charAt(i));
      if (g[3] > 0) {
        up = Math.max(up, -g[5]);
        down = Math.max(down, g[5] + g[3]);
      }
    }
    return new float[]{up / PlaqueSheet.CAP, down / PlaqueSheet.CAP};
  }

  /** The number over the street line, centred in the box, each line shrunk to fit across. */
  private static void lines(List<float[]> out, PlaqueFont font, String number, String street,
      float x0, float y0, float x1, float y1, float size) {
    float w = Math.max(0.1f, x1 - x0);
    float h = Math.max(0.1f, y1 - y0);
    boolean two = !street.isEmpty() && !number.isEmpty();
    float[] en = extentInCaps(font, number);
    float[] es = extentInCaps(font, street);
    float hn = Math.max(0.01f, en[0] + en[1]);
    float hs = Math.max(0.01f, es[0] + es[1]);
    // each line's share of the height, glyph images and all
    float cap1 = number.isEmpty() ? 0 : (two ? 0.62f : 0.95f) * h / hn;
    float cap2 = street.isEmpty() ? 0 : (two ? 0.3f : 0.6f) * h / hs;
    float gap = two ? 0.06f * h : 0;
    float wn = widthInCaps(font, number);
    float ws = widthInCaps(font, street);
    if (wn > 0) {
      cap1 = Math.min(cap1, w / wn);
    }
    if (ws > 0) {
      cap2 = Math.min(cap2, w / ws);
    }
    cap1 *= size;
    cap2 *= size;
    gap *= size;
    float total = cap1 * hn * (number.isEmpty() ? 0 : 1) + gap + cap2 * hs
        * (street.isEmpty() ? 0 : 1);
    float top = (y0 + y1) / 2f + total / 2f;
    float cx = (x0 + x1) / 2f;
    if (cap1 > 0) {
      line(out, font, number, cx, top - en[0] * cap1, cap1);
      top -= cap1 * hn + gap;
    }
    if (cap2 > 0) {
      line(out, font, street, cx, top - es[0] * cap2, cap2);
    }
  }

  /** One line centred on cx with its baseline at y, its capitals cap px tall. */
  private static void line(List<float[]> out, PlaqueFont font, String text, float cx, float y,
      float cap) {
    float k = cap / PlaqueSheet.CAP;
    float pen = cx - widthInCaps(font, text) * cap / 2f;
    for (int i = 0; i < text.length(); i++) {
      int[] g = glyph(font, text.charAt(i));
      put(out, g, pen, y, k);
      pen += g[6] / 16f * k;
    }
  }

  private static void put(List<float[]> out, int[] g, float pen, float baseline, float k) {
    if (g[2] == 0 || g[3] == 0) {
      return;
    }
    float gx0 = pen + g[4] * k;
    float gy1 = baseline - g[5] * k;
    out.add(new float[]{gx0, gy1 - g[3] * k, gx0 + g[2] * k, gy1, g[0], g[1], g[0] + g[2],
        g[1] + g[3]});
  }

  /** The number's characters one under another, each centred, filling the box's height. */
  private static void stack(List<float[]> out, PlaqueFont font, String number, float x0,
      float y0, float x1, float y1, float size) {
    String text = number.replace(" ", "");
    int n = text.length();
    if (n == 0) {
      return;
    }
    float w = Math.max(0.1f, x1 - x0);
    float h = Math.max(0.1f, y1 - y0);
    float[] e = extentInCaps(font, text);
    float slot = Math.max(0.01f, e[0] + e[1]);
    float gap = 0.2f;       // between characters, in caps
    float span = n * slot + (n - 1) * gap;
    float cap = Math.min(h / span, w / Math.max(0.01f, widestInCaps(font, text)));
    cap *= size;
    float cx = (x0 + x1) / 2f;
    float baseline = (y0 + y1) / 2f + cap * span / 2f - e[0] * cap;
    float k = cap / PlaqueSheet.CAP;
    for (int i = 0; i < n; i++) {
      int[] g = glyph(font, text.charAt(i));
      float inkW = g[2] * k;
      put(out, g, cx - inkW / 2f - g[4] * k, baseline, k);
      baseline -= cap * (slot + gap);
    }
  }

  /**
   * The plate's face from its tile: corners fixed, edges stretched one way, the middle both,
   * the border band {@code b} px wide whatever the plate's size.
   */
  private static void nineSlice(List<Piece> out, String tile, float x0, float y0, float x1,
      float y1, float b, float d, boolean shade) {
    int t = -1;
    for (int i = 0; i < PlaqueSheet.TILE_NAMES.length; i++) {
      if (PlaqueSheet.TILE_NAMES[i].equals(tile)) {
        t = i;
      }
    }
    if (t < 0) {
      return;
    }
    float tx = PlaqueSheet.TILE_XY[t][0];
    float ty = PlaqueSheet.TILE_XY[t][1];
    float tb = PlaqueSheet.TILE_BORDER;
    float ts = PlaqueSheet.TILE;
    float[] xs = {x0, x0 + b, x1 - b, x1};
    float[] us = {tx, tx + tb, tx + ts - tb, tx + ts};
    // y runs up, v runs down: the top row of the plate is the top of the tile
    float[] ys = {y1, y1 - b, y0 + b, y0};
    float[] vs = {ty, ty + tb, ty + ts - tb, ty + ts};
    for (int r = 0; r < 3; r++) {
      for (int c = 0; c < 3; c++) {
        out.add(new Piece(xs[c], ys[r + 1], xs[c + 1], ys[r], d, false, 1, us[c], vs[r],
            us[c + 1], vs[r + 1], 0xFFFFFF, shade));
      }
    }
  }
}

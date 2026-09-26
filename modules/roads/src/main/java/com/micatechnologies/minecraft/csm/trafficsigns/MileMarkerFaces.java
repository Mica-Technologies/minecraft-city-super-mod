package com.micatechnologies.minecraft.csm.trafficsigns;

import com.micatechnologies.minecraft.csm.codeutils.SignShift;
import com.micatechnologies.minecraft.csm.trafficaccessories.guidesign.GuideSignShieldType;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Lays out what a mile marker's tile entity says as flat pieces on its plate: one rectangle per
 * glyph, the route shield, and the route number on it, each with its sprite window and depth.
 * Pure arithmetic with no Minecraft classes, so the layout is testable on its own;
 * {@code MileMarkerBakedModel} turns the pieces into quads.
 *
 * <p>Coordinates are model units of the unturned model, whose plate faces north. The glyph
 * tables are measured in the reader's frame, where x runs to the reader's right; the reader
 * stands north of the plate, so model x is {@code 16 - readerX}. Glyphs are placed the way
 * {@code GuideSignFontRenderer} places them (pen, origin, baseline, cap), so a route number here
 * is set exactly as the route marker sign sets it.</p>
 *
 * <p>Depth: the plate's art is at {@link #artZ}; the legend 0.2 units in front of it, and a route
 * number another 0.2 in front of its shield. 0.2 is what {@code SignFaceDepthTest} asks of two
 * faces on a sign, and what a 24-bit depth buffer still tells apart a hundred blocks away: the
 * legend is in the same chunk mesh and depth pass as the plate, so anything nearer z-fights.</p>
 *
 * @since 2026.9
 */
public final class MileMarkerFaces {

  /** How far in front of what it is painted on a piece is drawn, in model units. */
  public static final float PROUD = 0.2f;

  /** Sheet pixels a glyph's piece keeps either side of its advance. */
  private static final float GLYPH_PAD = 1.0f;

  /** Sheet pixels to a sprite unit: the 256 px sheet is 16 units across. */
  private static final float SHEET_PX_PER_UV = 16.0f;

  /** Where a piece's texture comes from. */
  public enum Source {
    /** The glyph sheet, {@link MileMarkerLayout#SHEET}. */
    SHEET,
    /** The route marker face of the legend's shield. */
    SHIELD
  }

  /** One flat rectangle facing north. */
  public static final class Piece {

    public final float x0;
    public final float y0;
    public final float x1;
    public final float y1;
    public final float z;
    /** The sprite window, {@code u0, v0, u1, v1} in sprite units (0 to 16). */
    public final float[] uv;
    public final Source source;
    /** 0xRRGGBB to tint the piece, or -1 to leave the sprite's own colour. */
    public final int colour;

    Piece(float x0, float y0, float x1, float y1, float z, float[] uv, Source source,
        int colour) {
      this.x0 = x0;
      this.y0 = y0;
      this.x1 = x1;
      this.y1 = y1;
      this.z = z;
      this.uv = uv;
      this.source = source;
      this.colour = colour;
    }
  }

  private MileMarkerFaces() {
  }

  /**
   * Where a plate's art is, for each shift: the three shift models' art planes.
   *
   * @param shift the shift the sign is drawn in
   *
   * @return the art's z, in model units
   */
  public static float artZ(SignShift shift) {
    switch (shift) {
      case SETBACK:
        return 12.5f;
      case BACKTOBACK:
        return 28.49f;
      default:
        return 0.0f;
    }
  }

  /**
   * Every piece the legend puts on the plate.
   *
   * @param layout the plate
   * @param legend what it says
   * @param shift  the shift it is drawn in
   *
   * @return the pieces, in drawing order
   */
  public static List<Piece> pieces(MileMarkerLayout layout, MileMarkerLegend legend,
      SignShift shift) {
    List<Piece> out = new ArrayList<>();
    float z = artZ(shift) - PROUD;
    float[] mile = layout.getMileSlot();
    String number = legend.getMile();
    if (layout.isStacked()) {
      // One numeral a slot, the most significant at the top.
      for (int i = 0; i < number.length() && 2 * i + 1 < mile.length; i++) {
        run(out, number.substring(i, i + 1), MileMarkerLayout.SERIES_D,
            MileMarkerLayout.SERIES_D_CHARS, 8.0f, mile[2 * i], mile[2 * i + 1], Float.MAX_VALUE,
            z, -1);
      }
    } else {
      run(out, number, MileMarkerLayout.SERIES_D, MileMarkerLayout.SERIES_D_CHARS, 8.0f,
          mile[0], mile[1], mile[2], z, -1);
    }
    float[] tenth = layout.getTenthSlot();
    if (tenth != null && !legend.getTenth().isEmpty()) {
      run(out, legend.getTenth(), MileMarkerLayout.SERIES_D, MileMarkerLayout.SERIES_D_CHARS,
          8.0f, tenth[0], tenth[1], tenth[2], z, -1);
    }
    float[] direction = layout.getDirectionSlot();
    if (direction != null && legend.getDirection() >= 0) {
      float[] g = MileMarkerLayout.DIRECTION[legend.getDirection()];
      glyphs(out, new float[][]{g}, 8.0f, direction[0], direction[1], direction[2], z, -1);
    }
    float[] shieldSlot = layout.getShieldSlot();
    GuideSignShieldType shield = legend.getShield();
    if (shieldSlot != null && shield != null) {
      float cy = shieldSlot[0];
      float size = shieldSlot[1];
      float left = 8.0f - size / 2.0f;
      float top = cy + size / 2.0f;
      out.add(new Piece(16.0f - (left + size), cy - size / 2.0f, 16.0f - left, top, z,
          new float[]{0, 0, 16, 16}, Source.SHIELD, -1));
      if (!legend.getRoute().isEmpty()) {
        // The shield's own measurements, as the route marker sign reads them: centre and
        // cap as fractions of the marker's cell, its centre y measured from the cell's top.
        run(out, legend.getRoute(), MileMarkerLayout.GUIDE, "0123456789",
            left + size * shield.getRouteTextCenterX(),
            top - size * shield.getRouteTextCenterY(),
            size * shield.getRouteTextCapFraction(),
            size * shield.getRouteTextMaxFraction(), z - PROUD, shield.getRouteTextColor());
      }
    }
    return out;
  }

  /** The pieces for no legend at all: nothing. */
  public static List<Piece> none() {
    return Collections.emptyList();
  }

  private static void run(List<Piece> out, String text, float[][] table, String chars,
      float centreX, float centreY, float cap, float maxWidth, float z, int colour) {
    float[][] glyphs = new float[text.length()][];
    int n = 0;
    for (int i = 0; i < text.length(); i++) {
      int index = chars.indexOf(text.charAt(i));
      if (index >= 0) {
        glyphs[n++] = table[index];
      }
    }
    float[][] used = new float[n][];
    System.arraycopy(glyphs, 0, used, 0, n);
    glyphs(out, used, centreX, centreY, cap, maxWidth, z, colour);
  }

  /**
   * Sets a line of glyphs centred on {@code (centreX, centreY)} in the reader's frame, capitals
   * {@code cap} tall, shrunk to {@code maxWidth} if it would run wider.
   */
  private static void glyphs(List<Piece> out, float[][] glyphs, float centreX, float centreY,
      float cap, float maxWidth, float z, int colour) {
    if (glyphs.length == 0) {
      return;
    }
    // Every table's glyphs share one cap height in sheet pixels.
    float scale = cap / glyphs[0][8];
    float width = 0.0f;
    for (float[] g : glyphs) {
      width += g[9] * scale;
    }
    if (width > maxWidth && width > 0.0f) {
      scale *= maxWidth / width;
      cap = glyphs[0][8] * scale;
      width = maxWidth;
    }
    float baseline = centreY - cap / 2.0f;
    float pen = centreX - width / 2.0f;
    for (float[] g : glyphs) {
      // Only the glyph's own advance of its cell, and a pixel either side: a word's cell is
      // far wider than the word, and its empty margins would stick out past the plate.
      float from = Math.max(0.0f, g[6] - GLYPH_PAD);
      float to = Math.min(g[4], g[6] + g[9] + GLYPH_PAD);
      float cellLeft = pen - g[6] * scale;
      float left = cellLeft + from * scale;
      float right = cellLeft + to * scale;
      float top = baseline + g[7] * scale;
      float bottom = top - g[5] * scale;
      float u0 = g[0] + from / SHEET_PX_PER_UV;
      float u1 = g[0] + to / SHEET_PX_PER_UV;
      out.add(new Piece(16.0f - right, bottom, 16.0f - left, top, z,
          new float[]{u0, g[1], u1, g[3]}, Source.SHEET, colour));
      pen += g[9] * scale;
    }
  }
}

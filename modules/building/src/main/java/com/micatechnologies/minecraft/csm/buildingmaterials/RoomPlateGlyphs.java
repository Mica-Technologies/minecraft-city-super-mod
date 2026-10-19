package com.micatechnologies.minecraft.csm.buildingmaterials;

import java.util.ArrayList;
import java.util.List;

/**
 * Where a room plate's legend goes: the digits of its number and its room name, as pieces of the
 * glyph sheet ({@code textures/blocks/wallfinish/room_plate_glyphs.png}) laid on the plate's face,
 * in the plate's model space (the wall to the north, the face toward the south). Kept free of the
 * client so it can be tested.
 *
 * <p>SHARED with {@code gen_wall_finishes.py}: the sheet's layout (digits four texels apart along
 * the top, each name in a 64 x 12 slot, two columns of four from y 8, 14 apart), the names in
 * index order, and the legend's place on the plate.</p>
 *
 * @since 2026.10
 */
public final class RoomPlateGlyphs {

  /** The room names, by index; 0 is none. A {@code |} marks where the sheet breaks the line. */
  public static final String[] NAMES = {"", "EXAM", "PATIENT", "SOILED|UTILITY",
      "CLEAN|UTILITY", "NOURISHMENT", "STAFF|ONLY", "MED ROOM", "STORAGE"};
  /** The highest room number. */
  public static final int MAX_NUMBER = 9999;

  static final float SHEET_W = 128;
  static final float SHEET_H = 64;
  static final float NAME_SLOT_W = 64;
  static final float NAME_SLOT_H = 12;
  static final float NAME_TOP = 8;
  static final float NAME_ROW = 14;

  static final float DIGIT_W = 1.5f;
  static final float DIGIT_H = 2.5f;
  static final float DIGIT_ADVANCE = 2.0f;
  static final float DIGIT_TOP = 12.5f;
  static final float[] NAME_BOX = {1.75f, 6.0f, 14.25f, 8.875f};
  /** How far the legend stands off the plate's face, in px: the plate is 0.5 deep. */
  static final float LEGEND_Z = 0.55f;

  private RoomPlateGlyphs() {
  }

  /** One quad of the legend: its corners on the plate (px) and its window on the sheet (UV). */
  public static final class Piece {

    public final float x0;
    public final float y0;
    public final float x1;
    public final float y1;
    /** u0, v0, u1, v1 in the sprite's 0..16 units. */
    public final float[] uv;

    Piece(float x0, float y0, float x1, float y1, float[] uv) {
      this.x0 = x0;
      this.y0 = y0;
      this.x1 = x1;
      this.y1 = y1;
      this.uv = uv;
    }
  }

  public static int clampNumber(int number) {
    return number < 0 ? -1 : Math.min(number, MAX_NUMBER);
  }

  public static int clampName(int name) {
    return name < 0 || name >= NAMES.length ? 0 : name;
  }

  /**
   * The legend's pieces for a number (or -1 for none) and a name index.
   *
   * @param number the room number
   * @param name   the name's index
   * @return the pieces, digits first
   */
  public static List<Piece> pieces(int number, int name) {
    List<Piece> out = new ArrayList<>();
    if (number >= 0) {
      String digits = Integer.toString(clampNumber(number));
      float width = digits.length() * DIGIT_ADVANCE - (DIGIT_ADVANCE - DIGIT_W);
      float x = 8 - width / 2;
      for (int i = 0; i < digits.length(); i++) {
        int d = digits.charAt(i) - '0';
        float[] uv = {d * 4 * 16 / SHEET_W, 0, (d * 4 + 3) * 16 / SHEET_W, 5 * 16 / SHEET_H};
        out.add(new Piece(x, DIGIT_TOP - DIGIT_H, x + DIGIT_W, DIGIT_TOP, uv));
        x += DIGIT_ADVANCE;
      }
    }
    name = clampName(name);
    if (name > 0) {
      int col = (name - 1) % 2;
      int row = (name - 1) / 2;
      float sx = col * NAME_SLOT_W;
      float sy = NAME_TOP + row * NAME_ROW;
      float[] uv = {sx * 16 / SHEET_W, sy * 16 / SHEET_H, (sx + NAME_SLOT_W) * 16 / SHEET_W,
          (sy + NAME_SLOT_H) * 16 / SHEET_H};
      out.add(new Piece(NAME_BOX[0], NAME_BOX[1], NAME_BOX[2], NAME_BOX[3], uv));
    }
    return out;
  }
}

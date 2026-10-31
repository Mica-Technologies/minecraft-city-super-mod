package com.micatechnologies.minecraft.csm.venues.lines;

import java.util.Objects;

/**
 * What a court or track cell shows, found as its chunk section is built and handed to its baked
 * model as an unlisted property: the family and layout, the cell in the layout's frame, the
 * quarter turns from that frame to the world, and the four colours. Equal keys draw the same
 * quads, so the model caches by it.
 *
 * @since 2026.10
 */
public final class LineArtKey {

  /** The family's index. */
  public final int family;
  /** The layout's index. */
  public final int layout;
  /** The cell in the layout's frame. */
  public final int i;
  public final int j;
  /** Quarter turns clockwise from the layout's frame to the world's (0: its +x east). */
  public final int rot;
  /** The colours, four bits each: cc, sc, lc, oc. */
  public final int colours;

  public LineArtKey(int family, int layout, int i, int j, int rot, int colours) {
    this.family = family;
    this.layout = layout;
    this.i = i;
    this.j = j;
    this.rot = rot & 3;
    this.colours = colours;
  }

  /** Colour {@code k} (0 cc, 1 sc, 2 lc, 3 oc). */
  public int colour(int k) {
    return (colours >> (4 * k)) & 15;
  }

  /** The family. */
  public LineArtFamily family() {
    return LineArtFamily.byId(family);
  }

  @Override
  public boolean equals(Object o) {
    if (!(o instanceof LineArtKey)) {
      return false;
    }
    LineArtKey k = (LineArtKey) o;
    return family == k.family && layout == k.layout && i == k.i && j == k.j && rot == k.rot
        && colours == k.colours;
  }

  @Override
  public int hashCode() {
    return Objects.hash(family, layout, i, j, rot, colours);
  }

  /**
   * The layout-frame offset of a world offset from an anchor, for an anchor turned {@code rot}:
   * the inverse of the turn the model draws with.
   *
   * @return {u, v}
   */
  public static int[] toLayout(int dx, int dz, int rot) {
    switch (rot & 3) {
      case 1:
        return new int[]{dz, -dx};
      case 2:
        return new int[]{-dx, -dz};
      case 3:
        return new int[]{-dz, dx};
      default:
        return new int[]{dx, dz};
    }
  }

  /**
   * The world offset of a layout-frame offset (for a builder): the turn the model draws with.
   *
   * @return {dx, dz}
   */
  public static int[] toWorld(int u, int v, int rot) {
    switch (rot & 3) {
      case 1:
        return new int[]{-v, u};
      case 2:
        return new int[]{-u, -v};
      case 3:
        return new int[]{v, -u};
      default:
        return new int[]{u, v};
    }
  }
}

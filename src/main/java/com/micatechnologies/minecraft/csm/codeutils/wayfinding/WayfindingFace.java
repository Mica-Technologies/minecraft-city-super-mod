package com.micatechnologies.minecraft.csm.codeutils.wayfinding;

import java.util.Arrays;
import java.util.Objects;

/**
 * Everything one cell of a wayfinding panel draws: the panel's sign, the cell's place in the
 * panel, the mount it is drawn with and the rods or posts it carries. Immutable, and a value: the
 * baked model keeps the quads it baked for one under it, so a stairwell of identical signs bakes
 * each cell's share once.
 *
 * <p><b>The panel as drawn</b> ({@link #panelX0()} and the rest, in blocks in the cells' space:
 * x to the reader's right from the controller's left edge, y up from its bottom). Its size is the
 * cells' times the sign's scale; it is centred across the cells, and up them by its mount: its
 * top at the cells' top, less the drop, when hung, its top at the top on a ceiling, its bottom at
 * the bottom, plus the drop, on posts, and centred on a wall. The hitbox and the baked model both
 * work from these, so the two cannot disagree.</p>
 *
 * @since 2026.10
 */
public final class WayfindingFace {

  /** The slab's front and back, in the model's sixteenths (front towards the reader, z 0). */
  public static final float FREE_Z0 = 6f;
  public static final float FREE_Z1 = 10f;
  public static final float WALL_Z0 = 15f;
  public static final float WALL_Z1 = 16f;

  final WayfindingSign sign;
  final int col;
  final int row;
  final int width;
  final int height;
  final WayfindingMount mount;
  /** Rods in this cell's column: x in the cells' space and how far above the cell top, pairs. */
  final float[] rods;
  /** Posts in this cell's column: x and how far below the cell bottom, pairs. */
  final float[] posts;
  private final int hash;

  /**
   * A cell's face.
   *
   * @param sign   the panel's sign
   * @param col    the cell's column in the panel
   * @param row    its row
   * @param width  the panel's width in cells, 0 for a cell drawn plain
   * @param height its height in cells, 0 for a cell drawn plain
   * @param mount  the mount it is drawn with, never {@link WayfindingMount#AUTO}
   * @param rods   see {@link #rods}, may be null
   * @param posts  see {@link #posts}, may be null
   */
  public WayfindingFace(WayfindingSign sign, int col, int row, int width, int height,
      WayfindingMount mount, float[] rods, float[] posts) {
    this.sign = sign;
    this.col = col;
    this.row = row;
    this.width = width;
    this.height = height;
    this.mount = mount == WayfindingMount.AUTO ? WayfindingMount.HUNG : mount;
    this.rods = rods == null ? new float[0] : rods;
    this.posts = posts == null ? new float[0] : posts;
    this.hash = Objects.hash(sign, col, row, width, height, this.mount)
        * 31 * 31 + Arrays.hashCode(this.rods) * 31 + Arrays.hashCode(this.posts);
  }

  public WayfindingSign getSign() {
    return sign;
  }

  public WayfindingMount getMount() {
    return mount;
  }

  /** Whether the cell draws the sign (a cell outside its panel's rectangle is a plain slab). */
  public boolean drawsSign() {
    return width > 0 && height > 0;
  }

  /** Whether the back carries the legend too: asked for, and not against a wall. */
  public boolean drawsBack() {
    return sign.isDoubleSided() && mount.isFreeStanding();
  }

  /** The cells this face is laid out across: the panel's, or the cell's own when plain. */
  int cellsWide() {
    return drawsSign() ? width : 1;
  }

  int cellsHigh() {
    return drawsSign() ? height : 1;
  }

  int cellCol() {
    return drawsSign() ? col : 0;
  }

  int cellRow() {
    return drawsSign() ? row : 0;
  }

  public float panelWidth() {
    return cellsWide() * sign.getPanelScale();
  }

  public float panelHeight() {
    return cellsHigh() * sign.getPanelScale();
  }

  public float panelX0() {
    return (cellsWide() - panelWidth()) / 2f;
  }

  public float panelY0() {
    float free = cellsHigh() - panelHeight();
    float drop = sign.getDrop() / 16f;
    switch (mount) {
      case HUNG:
        return free - drop;
      case CEILING:
        return free;
      case POST:
        return drop;
      default:
        return free / 2f;
    }
  }

  /** The slab's front face, in the model's sixteenths. */
  public float slabZ0() {
    return mount == WayfindingMount.WALL ? WALL_Z0 : FREE_Z0;
  }

  /** The slab's back face, in the model's sixteenths. */
  public float slabZ1() {
    return mount == WayfindingMount.WALL ? WALL_Z1 : FREE_Z1;
  }

  /**
   * Where the rods hang from or the posts stand under, along the panel: two, or one for about
   * every four blocks of a wider panel; a post a narrow panel stands on is one, in the middle.
   *
   * @param posts whether these are posts
   * @return x of each, in the cells' space
   */
  public float[] supportXs(boolean posts) {
    float w = panelWidth();
    int n = posts && w <= 1.5f ? 1 : Math.max(2, Math.round(w / 4f));
    float[] out = new float[n];
    for (int i = 0; i < n; i++) {
      out[i] = panelX0() + w * (i + 0.5f) / n;
    }
    return out;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof WayfindingFace)) {
      return false;
    }
    WayfindingFace f = (WayfindingFace) o;
    return hash == f.hash && col == f.col && row == f.row && width == f.width
        && height == f.height && mount == f.mount && sign.equals(f.sign)
        && Arrays.equals(rods, f.rods) && Arrays.equals(posts, f.posts);
  }

  @Override
  public int hashCode() {
    return hash;
  }
}

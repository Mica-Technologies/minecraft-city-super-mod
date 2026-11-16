package com.micatechnologies.minecraft.csm.transit.canopy;

import net.minecraft.util.BlockRenderLayer;

/**
 * The bus station canopy decks' styles, as {@code gen_transit_canopies.py} draws them, and what
 * the code needs of each: how many tiles one profile spans, the layer it is drawn in, and where
 * across the profile the Canopy Tool stands columns and hangs lights.
 *
 * @since 2026.11
 */
public enum CanopyStyle {
  /** A flat steel deck with a white soffit. */
  FLAT(1, BlockRenderLayer.CUTOUT, new int[]{0}, new int[]{0}, false),
  /** A steel grid with glass between the beams. */
  GLASS(1, BlockRenderLayer.TRANSLUCENT, new int[]{0}, new int[]{0}, false),
  /** North-light ridges: a slope rising over three tiles, then a glazed upstand. */
  SAWTOOTH(3, BlockRenderLayer.TRANSLUCENT, new int[]{0}, new int[]{0}, true),
  /** V roofs over four tiles, on a row of columns under the valley. */
  BUTTERFLY(4, BlockRenderLayer.CUTOUT, new int[]{1}, new int[]{1, 2}, false),
  /**
   * Shallow barrel arches over six tiles, on columns at the springing: one column under each
   * gutter between two arches, and one under the canopy's far edge.
   */
  VAULT(6, BlockRenderLayer.CUTOUT, new int[]{0}, new int[]{0, 5}, true);

  private final int module;
  private final BlockRenderLayer layer;
  private final int[] columnPhases;
  private final int[] lightPhases;
  private final boolean closingColumn;

  CanopyStyle(int module, BlockRenderLayer layer, int[] columnPhases, int[] lightPhases,
      boolean closingColumn) {
    this.module = module;
    this.layer = layer;
    this.columnPhases = columnPhases;
    this.lightPhases = lightPhases;
    this.closingColumn = closingColumn;
  }

  /**
   * Whether the Canopy Tool also stands columns under the canopy's last tile across the
   * profile: where the columns stand under each module's first tile, the far edge needs its
   * own.
   */
  public boolean hasClosingColumn() {
    return closingColumn;
  }

  /** The tiles one profile spans: 1 for a flat style. */
  public int getModule() {
    return module;
  }

  /** Whether the deck changes across its module, and so stores its phase and axis. */
  public boolean isShaped() {
    return module > 1;
  }

  public BlockRenderLayer getLayer() {
    return layer;
  }

  /** Whether the Canopy Tool stands a column under a tile at this phase. */
  public boolean columnAt(int phase) {
    return contains(columnPhases, phase);
  }

  /** Whether the Canopy Tool hangs a light under a tile at this phase (its soffit is low). */
  public boolean lightAt(int phase) {
    return contains(lightPhases, phase);
  }

  private static boolean contains(int[] values, int v) {
    for (int x : values) {
      if (x == v) {
        return true;
      }
    }
    return false;
  }
}

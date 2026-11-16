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

  /** A tile turned 22.5 degrees rises this much across a block, in sixteenths. */
  private static final double R22 = 16 * Math.tan(Math.PI / 8);

  /**
   * The heights (sixteenths above the deck block's foot) a column's head or a downlight is
   * raised to under a sloped tile: every soffit height a tile's centre can have, so the
   * generator draws one model per level ({@code REACH_PX} in gen_transit_canopies.py).
   */
  public static final double[] REACH_PX = {0, R22 / 2, 8, R22 * 1.5, R22 * 2.5,
      16 + R22 / 2, 16 + R22 * 1.5};

  /**
   * How high the soffit is at the centre of a tile at this phase, above the deck block's foot,
   * in sixteenths: where a column under it has to reach, and a light hang.
   *
   * @param phase the tile's place in the module
   *
   * @return the height
   */
  public double soffitAt(int phase) {
    switch (this) {
      case SAWTOOTH:
        return R22 * (phase + 0.5);
      case BUTTERFLY:
        return phase == 0 || phase == 3 ? R22 * 1.5 : R22 / 2;
      case VAULT:
        // the tile centres of the arch: 45 degrees, then 22.5, then the crown
        return phase == 0 || phase == 5 ? 8 : phase == 1 || phase == 4 ? 16 + R22 / 2
            : 16 + R22 * 1.5;
      default:
        return 0;
    }
  }

  /**
   * The {@link #REACH_PX} level nearest the soffit at this phase.
   *
   * @param phase the tile's place in the module
   *
   * @return the level's index
   */
  public int reachAt(int phase) {
    double h = soffitAt(phase);
    int best = 0;
    for (int i = 1; i < REACH_PX.length; i++) {
      if (Math.abs(REACH_PX[i] - h) < Math.abs(REACH_PX[best] - h)) {
        best = i;
      }
    }
    return best;
  }

  /** The slope angles a light can be tilted to, after flat ({@code tilt} 1..4 along x, 5..8 z). */
  public static final double[] TILT_DEG = {22.5, -22.5, 45, -45};

  /**
   * The slope of the soffit at the centre of a tile at this phase, in degrees, rising toward the
   * profile's +x (its +z, for axis z).
   *
   * @param phase the tile's place in the module
   *
   * @return the angle: 0, 22.5 or 45, either sign
   */
  public double slopeAt(int phase) {
    switch (this) {
      case SAWTOOTH:
        return 22.5;
      case BUTTERFLY:
        return phase < 2 ? -22.5 : 22.5;
      case VAULT:
        return phase == 0 ? 45 : phase == 1 ? 22.5 : phase == 4 ? -22.5 : phase == 5 ? -45 : 0;
      default:
        return 0;
    }
  }

  /**
   * The tilt a downlight under {@code above} takes to sit flush on its soffit: 0 flat, else
   * 1 + the index in {@link #TILT_DEG}, plus 4 for a profile along z.
   *
   * @param above the state of the block above
   *
   * @return the tilt
   */
  public static int tiltUnder(net.minecraft.block.state.IBlockState above) {
    if (!(above.getBlock() instanceof BlockCanopyDeck)) {
      return 0;
    }
    CanopyStyle style = ((BlockCanopyDeck) above.getBlock()).getStyle();
    if (style == null) {
      return 0;
    }
    double a = style.slopeAt(above.getValue(BlockCanopyDeck.PHASE));
    for (int i = 0; i < TILT_DEG.length; i++) {
      if (TILT_DEG[i] == a) {
        return 1 + i
            + (above.getValue(BlockCanopyDeck.AXIS) == net.minecraft.util.EnumFacing.Axis.Z ? 4
            : 0);
      }
    }
    return 0;
  }

  /**
   * The reach level for something under {@code above}: its tile's, or 0 under anything but a
   * canopy deck.
   *
   * @param above the state of the block above
   *
   * @return the level's index
   */
  public static int reachUnder(net.minecraft.block.state.IBlockState above) {
    if (!(above.getBlock() instanceof BlockCanopyDeck)) {
      return 0;
    }
    CanopyStyle style = ((BlockCanopyDeck) above.getBlock()).getStyle();
    return style == null ? 0 : style.reachAt(above.getValue(BlockCanopyDeck.PHASE));
  }

  /**
   * Every (reach level, slope in degrees) a tile's centre can give, sorted by reach then slope,
   * flat first: a light strip's {@code seat} indexes this, so it has a model only for the pairs
   * that occur. {@code gen_transit_canopies.py} builds the same list the same way ({@code SEATS}).
   */
  public static final double[][] SEATS = seats();

  private static double[][] seats() {
    java.util.List<double[]> out = new java.util.ArrayList<>();
    out.add(new double[]{0, 0});
    for (CanopyStyle style : values()) {
      for (int p = 0; p < style.module; p++) {
        double[] seat = {style.reachAt(p), style.slopeAt(p)};
        boolean known = false;
        for (double[] s : out) {
          known |= s[0] == seat[0] && s[1] == seat[1];
        }
        if (!known) {
          out.add(seat);
        }
      }
    }
    out.sort((a, b) -> a[0] != b[0] ? Double.compare(a[0], b[0]) : Double.compare(a[1], b[1]));
    return out.toArray(new double[0][]);
  }

  /**
   * The {@link #SEATS} index for a light strip under {@code above}: its tile's reach and slope,
   * or 0 (flat, not raised) under anything but a shaped canopy deck.
   *
   * @param above the state of the block above
   *
   * @return the index
   */
  public static int seatUnder(net.minecraft.block.state.IBlockState above) {
    if (!(above.getBlock() instanceof BlockCanopyDeck)) {
      return 0;
    }
    CanopyStyle style = ((BlockCanopyDeck) above.getBlock()).getStyle();
    if (style == null) {
      return 0;
    }
    int phase = above.getValue(BlockCanopyDeck.PHASE);
    double reach = style.reachAt(phase);
    double slope = style.slopeAt(phase);
    for (int i = 0; i < SEATS.length; i++) {
      if (SEATS[i][0] == reach && SEATS[i][1] == slope) {
        return i;
      }
    }
    return 0;
  }

  /**
   * The way a light strip under {@code above} runs: across a shaped tile's profile (along its
   * ridges and valleys, where the soffit is level), whatever the strip stores; its own
   * {@code stored} axis under a flat deck or anything else.
   *
   * @param above  the state of the block above
   * @param stored the strip's stored axis
   *
   * @return the axis it is drawn along
   */
  public static net.minecraft.util.EnumFacing.Axis runAxisUnder(
      net.minecraft.block.state.IBlockState above, net.minecraft.util.EnumFacing.Axis stored) {
    if (above.getBlock() instanceof BlockCanopyDeck) {
      CanopyStyle style = ((BlockCanopyDeck) above.getBlock()).getStyle();
      if (style != null && style.isShaped()) {
        return above.getValue(BlockCanopyDeck.AXIS) == net.minecraft.util.EnumFacing.Axis.X
            ? net.minecraft.util.EnumFacing.Axis.Z : net.minecraft.util.EnumFacing.Axis.X;
      }
    }
    return stored;
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

package com.micatechnologies.minecraft.csm.powergrid.water;

/**
 * The size of one tank and which of its cells hold an invisible part: a grid of three-block
 * tiles about a vertical axis through the centre cell, the grid's bottom row at the axis cell.
 *
 * <p>Each tile's root, the cell in its middle, draws the tile's share of the tank; every other
 * cell the tank fills is a {@link BlockTankPart}, for collision. The cell map is written by
 * {@code dev-env-utils/scripts/gen_utilities_water.py} from the same profiles it lathes into the
 * tiles' models, so the collision and the drawing cannot disagree ({@link TankShapes}).</p>
 *
 * <p>A fixed tank (a water tower's bowl) is placed whole, {@code layers} tiles tall. A
 * stackable one (a ground storage tank) is one tile tall, and a layer placed on top of another
 * of the same shape carries it on up.</p>
 *
 * @since 2026.9
 */
public final class TankShape {

  /** A map character: no part in this cell. */
  public static final char NONE = '.';
  /** A map character: a solid part. */
  public static final char SOLID = '#';

  private final String registryName;
  private final int grid;
  private final int layers;
  private final boolean stackable;
  private final int bandLayer;
  private final int[] bandSlots;
  private final String[] rows;

  /**
   * @param registryName the block whose item places the tank
   * @param grid         tiles a side, odd
   * @param layers       tiles tall
   * @param stackable    one layer a unit, stacked (a ground storage tank)
   * @param bandLayer    the tile layer the name band is in, or -1 for none
   * @param bandSlots    the tiles along the north row that carry the band, by column
   * @param rows         the cell map, a string per row of cells: bottom layer first, north row
   *                     first, west cell first
   */
  public TankShape(String registryName, int grid, int layers, boolean stackable, int bandLayer,
      int[] bandSlots, String[] rows) {
    if (grid % 2 == 0 || rows.length != 3 * layers * 3 * grid) {
      throw new IllegalArgumentException("Bad tank shape " + registryName);
    }
    this.registryName = registryName;
    this.grid = grid;
    this.layers = layers;
    this.stackable = stackable;
    this.bandLayer = bandLayer;
    this.bandSlots = bandSlots.clone();
    this.rows = rows.clone();
  }

  public String getRegistryName() {
    return registryName;
  }

  /** Tiles a side. */
  public int getGrid() {
    return grid;
  }

  /** Tiles tall. */
  public int getLayers() {
    return layers;
  }

  public boolean isStackable() {
    return stackable;
  }

  /** Cells a side. */
  public int getWidth() {
    return 3 * grid;
  }

  /** Cells tall. */
  public int getHeight() {
    return 3 * layers;
  }

  /** Cells from the grid's west (or north) edge to the axis. */
  public int getHalf() {
    return (getWidth() - 1) / 2;
  }

  public int getTileCount() {
    return grid * grid * layers;
  }

  public boolean hasBand() {
    return bandLayer >= 0;
  }

  public int getBandLayer() {
    return bandLayer;
  }

  /** How many band tiles run along one side. */
  public int getBandSlotCount() {
    return bandSlots.length;
  }

  /** The column (on the north row; rows, on the east and west) of band slot {@code slot}. */
  public int getBandSlot(int slot) {
    return bandSlots[slot];
  }

  /** The slot a tile column is, or -1 if the band does not reach it. */
  public int bandSlotOf(int column) {
    for (int i = 0; i < bandSlots.length; i++) {
      if (bandSlots[i] == column) {
        return i;
      }
    }
    return -1;
  }

  /** The map character of a cell, in cells from the grid's minimum corner. */
  public char cell(int x, int y, int z) {
    int w = getWidth();
    if (x < 0 || z < 0 || y < 0 || x >= w || z >= w || y >= getHeight()) {
      return NONE;
    }
    return rows[y * w + z].charAt(x);
  }

  /** Whether a cell, from the grid's minimum corner, is the root of its tile. */
  public boolean isRootCell(int x, int y, int z) {
    return x % 3 == 1 && y % 3 == 1 && z % 3 == 1;
  }

  /**
   * The {@link BlockTankPart#KIND} for a map character: 0 for a solid part, 1 to 15 for a walkway
   * with its railing mask plus one, -1 for no part.
   */
  public static int partKind(char c) {
    if (c == SOLID) {
      return 0;
    }
    if (c >= 'a' && c <= 'o') {
      return 1 + (c - 'a');
    }
    return -1;
  }
}

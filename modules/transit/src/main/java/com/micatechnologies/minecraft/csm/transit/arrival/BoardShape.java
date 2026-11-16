package com.micatechnologies.minecraft.csm.transit.arrival;

/**
 * An arrival board's or the info totem's measurements, in sixteenths of a block, in the reader's
 * space: x to the reader's right from the master's column, y up from the master's floor, z toward
 * the reader from the middle of the block. The renderer draws from it; it is kept on the board's
 * tile entity and made again only when the board's block or mount changes.
 *
 * @since 2026.11
 */
public final class BoardShape {

  /** How far below the top of its top row a board's housing ends (the rods' stubs). */
  public static final int TOP_GAP_PX = 2;
  /** How far above the floor of its bottom row a board's housing starts. */
  public static final int BOTTOM_GAP_PX = 1;
  /** The housing's frame round the screen, and the agency band along its top. */
  public static final float FRAME = 1.25f;
  public static final float BAND = 1.0f;

  /** The totem: its body's half width and half depth, and its panels' heights. */
  public static final float TOTEM_HALF_W = 6f;
  public static final float TOTEM_HALF_D = 2.5f;
  public static final float TOTEM_INNER = 5.25f;
  public static final float TOTEM_SCREEN_Y0 = 29f;
  public static final float TOTEM_SCREEN_Y1 = 39.5f;
  public static final float TOTEM_NAME_Y0 = 41f;
  public static final float TOTEM_NAME_Y1 = 47f;
  public static final float TOTEM_MAP_Y0 = 14.5f;
  public static final float TOTEM_MAP_Y1 = 25f;

  final BlockBusArrivalBoard block;
  final boolean totem;
  final int width;
  final int height;
  final BoardMount mount;
  /** The housing. */
  final float x0;
  final float x1;
  final float y0;
  final float y1;
  final float backZ;
  final float frontZ;
  /** The screen on the front face. */
  final float screenX0;
  final float screenX1;
  final float screenY0;
  final float screenY1;
  /** The rods' and posts' x. */
  final float[] rodX;
  final float[] postX;

  BoardShape(BlockBusArrivalBoard board, BoardMount mount) {
    this.block = board;
    this.totem = board.isTotem();
    this.width = board.getWidth();
    this.height = board.getHeight();
    this.mount = mount;
    if (totem) {
      x0 = -TOTEM_HALF_W;
      x1 = TOTEM_HALF_W;
      y0 = 0;
      y1 = 48;
      backZ = -TOTEM_HALF_D;
      frontZ = TOTEM_HALF_D;
      screenX0 = -TOTEM_INNER;
      screenX1 = TOTEM_INNER;
      screenY0 = TOTEM_SCREEN_Y0;
      screenY1 = TOTEM_SCREEN_Y1;
      rodX = new float[0];
      postX = new float[0];
      return;
    }
    x0 = -width * 8 + 1;
    x1 = width * 8 - 1;
    y0 = BOTTOM_GAP_PX;
    y1 = height * 16 - TOP_GAP_PX;
    if (mount == BoardMount.WALL) {
      backZ = -8;
      frontZ = -5;
    } else {
      backZ = -2;
      frontZ = 2;
    }
    screenX0 = x0 + FRAME;
    screenX1 = x1 - FRAME;
    screenY0 = y0 + FRAME;
    screenY1 = y1 - BAND - FRAME;
    float rx = (width / 2) * 16f;
    rodX = new float[]{-rx, rx};
    int[] cols = board.postColumns();
    postX = new float[cols.length];
    for (int i = 0; i < cols.length; i++) {
      postX[i] = cols[i] * 16f;
    }
  }

  /** Whether this shape was made for a block and mount. */
  boolean isFor(BlockBusArrivalBoard board, BoardMount m) {
    return block == board && mount == m;
  }
}

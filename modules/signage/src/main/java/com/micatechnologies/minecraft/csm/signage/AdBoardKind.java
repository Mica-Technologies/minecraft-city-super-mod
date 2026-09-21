package com.micatechnologies.minecraft.csm.signage;

import javax.annotation.Nullable;

/**
 * The kinds of advertising board: what each is called, how big it may grow, the sizes its screen
 * offers, and where its face and frame sit in the block.
 *
 * <p>Geometry is in pixels (sixteenths of a block) in the frame the models are drawn in: the face
 * looks south (+z), and the back of the board is the block's north side.</p>
 *
 * <p>Two shapes of board. A <em>wall</em> board is a thin plate against a wall, its ad a little
 * in front of the plate. A <em>cabinet</em> board fills its whole block -- a solid box, so the
 * thousands of faces between the blocks of a 40 by 40 billboard are culled like a wall's -- with
 * its frame standing proud of the box on both sides and its ad just outside the box, front and,
 * if the board is double-sided, back.</p>
 *
 * <p>A board may have <em>service rows</em> at the bottom that carry no face: a printed
 * billboard's catwalk, or a kiosk's post. The face starts above them.</p>
 */
public enum AdBoardKind {
  /**
   * A framed poster on a wall (a "6-sheet" or larger): a thin backing plate and an aluminium
   * frame.
   */
  WALL_POSTER("ad_poster_board", 24, 12,
      new int[][]{{2, 3}, {4, 2}, {6, 3}, {8, 4}, {12, 6}, {15, 8}, {24, 12}},
      1.0, 1.5, 2.5, false, Service.NONE, 0, false),
  /**
   * A printed billboard, the painted bulletin of a highway: a steel cabinet over a service row --
   * hangers down to a catwalk under the face, floodlights on arms out in front, and railings only
   * at the two ends, where a real catwalk has them. The service row is the board's bottom row of
   * blocks, so the face starts a row up and nothing stands in front of it; the presets are the
   * standard US sizes to the nearest block, a row taller for it: a 12 x 25 ft poster, a 10 x 40 ft
   * junior bulletin, the 14 x 48 ft bulletin, a 20 x 60 ft spectacular, and larger wallscapes.
   */
  BILLBOARD("ad_billboard", 40, 40,
      new int[][]{{8, 5}, {12, 4}, {15, 5}, {18, 7}, {24, 9}, {32, 13}, {40, 21}, {40, 40}},
      2.0, 16.25, 16.0, true, Service.CATWALK, 1, false),
  /**
   * A digital billboard: an LED screen in a black bezel, lit by default. No catwalk; a screen is
   * serviced from behind.
   */
  DIGITAL_BILLBOARD("ad_digital_billboard", 40, 40,
      new int[][]{{8, 4}, {12, 3}, {15, 4}, {18, 6}, {24, 8}, {32, 12}, {40, 20}, {40, 40}},
      2.0, 16.25, 16.0, true, Service.NONE, 0, false),
  /**
   * A street ad kiosk at true scale: a backlit cabinet, an ad on each side, 2 x 3 blocks of face
   * on a one-block post -- about the 1.3 x 2.8 m of the real thing. Fixed size, built whole as
   * it is placed.
   */
  KIOSK("ad_kiosk", 2, 4, new int[][]{{2, 4}}, 2.0, 16.25, 16.0, true, Service.POST, 1, true),
  /** The same kiosk well over life size: 4 x 6 blocks of face on a two-block post. */
  KIOSK_LARGE("ad_kiosk_large", 4, 8, new int[][]{{4, 8}}, 3.0, 16.25, 16.0, true, Service.POST,
      2, true);

  /** What stands in a board's service rows. */
  public enum Service {
    /** No service rows. */
    NONE,
    /** A printed billboard's catwalk, hangers and floodlights, across the whole row. */
    CATWALK,
    /**
     * A kiosk's post, in the controller's column only; the rest of the service rows is empty. The
     * post stands on the controller column's right edge, which is the middle of an even-width
     * board centred on it.
     */
    POST
  }

  private final String registryName;
  private final int maxWidth;
  private final int maxHeight;
  private final int[][] presets;
  private final double framePx;
  private final double facePx;
  private final double depthPx;
  private final boolean cabinet;
  private final Service service;
  private final int serviceRows;
  private final boolean fixed;

  AdBoardKind(String registryName, int maxWidth, int maxHeight, int[][] presets, double framePx,
      double facePx, double depthPx, boolean cabinet, Service service, int serviceRows,
      boolean fixed) {
    this.registryName = registryName;
    this.maxWidth = maxWidth;
    this.maxHeight = maxHeight;
    this.presets = presets;
    this.framePx = framePx;
    this.facePx = facePx;
    this.depthPx = depthPx;
    this.cabinet = cabinet;
    this.service = service;
    this.serviceRows = serviceRows;
    this.fixed = fixed;
  }

  /** The controller's registry name: the block the player places. */
  public String getRegistryName() {
    return registryName;
  }

  /** The registry name of the blocks the controller builds around itself. */
  public String getPartRegistryName() {
    return registryName + "_part";
  }

  /** The registry name of the blocks of a board's service rows, other than its controller. */
  public String getServicePartRegistryName() {
    return registryName + "_service";
  }

  public int getMaxWidth() {
    return maxWidth;
  }

  public int getMaxHeight() {
    return maxHeight;
  }

  /** The narrowest board: a fixed-size board is only ever its one size. */
  public int getMinWidth() {
    return fixed ? maxWidth : 1;
  }

  /** The shortest board: one row of face above any service rows, or the one size if fixed. */
  public int getMinHeight() {
    return fixed ? maxHeight : serviceRows + 1;
  }

  /** Whether the board has only one size, is built whole as it is placed, and costs one item. */
  public boolean isFixedSize() {
    return fixed;
  }

  /** Sizes the screen offers, {@code {width, height}} in blocks, smallest first. */
  public int[][] getPresets() {
    return presets;
  }

  /** Width of the frame around the board's outside edge. */
  public double getFramePx() {
    return framePx;
  }

  /**
   * How far the ad's face stands off the back of the block. On a cabinet board a quarter pixel
   * outside the box: 0.05 px was tried first and z-fought with the box from a few dozen blocks
   * out, drawing streaks across the ad. The frame lip stands a whole pixel proud, so the face is
   * still inside it.
   */
  public double getFacePx() {
    return facePx;
  }

  /** Where a double-sided board's back face is: just outside the back of the block. */
  public double getBackFacePx() {
    return 16.0 - facePx;
  }

  /** How deep the board is: what is clicked and collided with. */
  public double getDepthPx() {
    return depthPx;
  }

  /** Whether the board is a solid box filling its block, and so can carry an ad on its back. */
  public boolean isCabinet() {
    return cabinet;
  }

  /** What stands in the board's service rows. */
  public Service getService() {
    return service;
  }

  /** Whether the board has service rows at all. */
  public boolean hasServiceRow() {
    return serviceRows > 0;
  }

  /** How many rows at the bottom of the board carry no face. */
  public int getServiceRows() {
    return serviceRows;
  }

  /**
   * Whether cell {@code (column, row)} of a board with its controller in {@code controllerColumn}
   * holds a block: every cell does, except the service rows of a kiosk beside its post.
   */
  public boolean hasCell(int column, int row, int controllerColumn) {
    return row >= serviceRows || service != Service.POST || column == controllerColumn;
  }

  /** The kind whose controller or part has the given registry name, or {@code null}. */
  @Nullable
  public static AdBoardKind of(String registryName) {
    for (AdBoardKind kind : values()) {
      if (kind.registryName.equals(registryName)
          || kind.getPartRegistryName().equals(registryName)
          || kind.getServicePartRegistryName().equals(registryName)) {
        return kind;
      }
    }
    return null;
  }
}

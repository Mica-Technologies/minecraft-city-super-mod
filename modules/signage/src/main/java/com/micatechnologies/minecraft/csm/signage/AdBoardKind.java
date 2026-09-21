package com.micatechnologies.minecraft.csm.signage;

import javax.annotation.Nullable;

/**
 * The kinds of advertising board built to size: what each is called, how big it may grow, the
 * sizes its screen offers, and where its face and frame sit in the block.
 *
 * <p>Geometry is in pixels (sixteenths of a block) in the frame the models are drawn in: the face
 * looks south (+z), and the back of the board is the block's north side, against the wall.</p>
 */
public enum AdBoardKind {
  /**
   * A framed poster on a wall (a "6-sheet" or larger): a thin backing plate and an aluminium
   * frame.
   */
  WALL_POSTER("ad_poster_board", 24, 12,
      new int[][]{{2, 3}, {4, 2}, {6, 3}, {8, 4}, {12, 6}, {15, 8}, {24, 12}},
      1.0, 1.5, 2.5);

  private final String registryName;
  private final int maxWidth;
  private final int maxHeight;
  private final int[][] presets;
  private final double framePx;
  private final double facePx;
  private final double depthPx;

  AdBoardKind(String registryName, int maxWidth, int maxHeight, int[][] presets, double framePx,
      double facePx, double depthPx) {
    this.registryName = registryName;
    this.maxWidth = maxWidth;
    this.maxHeight = maxHeight;
    this.presets = presets;
    this.framePx = framePx;
    this.facePx = facePx;
    this.depthPx = depthPx;
  }

  /** The controller's registry name: the block the player places. */
  public String getRegistryName() {
    return registryName;
  }

  /** The registry name of the blocks the controller builds around itself. */
  public String getPartRegistryName() {
    return registryName + "_part";
  }

  public int getMaxWidth() {
    return maxWidth;
  }

  public int getMaxHeight() {
    return maxHeight;
  }

  /** Sizes the screen offers, {@code {width, height}} in blocks, smallest first. */
  public int[][] getPresets() {
    return presets;
  }

  /** Width of the frame around the board's outside edge. */
  public double getFramePx() {
    return framePx;
  }

  /** How far the ad's face stands off the back of the block. */
  public double getFacePx() {
    return facePx;
  }

  /** How deep the board is: what is clicked and collided with. */
  public double getDepthPx() {
    return depthPx;
  }

  /** The kind whose controller or part has the given registry name, or {@code null}. */
  @Nullable
  public static AdBoardKind of(String registryName) {
    for (AdBoardKind kind : values()) {
      if (kind.registryName.equals(registryName)
          || kind.getPartRegistryName().equals(registryName)) {
        return kind;
      }
    }
    return null;
  }
}

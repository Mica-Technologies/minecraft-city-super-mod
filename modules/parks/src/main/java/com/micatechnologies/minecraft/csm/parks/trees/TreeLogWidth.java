package com.micatechnologies.minecraft.csm.parks.trees;

/**
 * How thick a log is, across, in sixteenths of a block.
 *
 * <p>The index (1-5) is what a log's connection mask records for each face neighbour, so a log
 * can taper toward a thinner one; 0 there means "not one of these logs". A vanilla log counts as
 * {@link #FULL}.</p>
 *
 * @since 2026.9
 */
public enum TreeLogWidth {
  TWIG("twig", 2),
  THIN("thin", 4),
  MEDIUM("medium", 8),
  THICK("thick", 12),
  FULL("full", 16);

  private final String id;
  private final int pixels;

  TreeLogWidth(String id, int pixels) {
    this.id = id;
    this.pixels = pixels;
  }

  public String getId() {
    return id;
  }

  /** The width across, in sixteenths of a block. */
  public int getPixels() {
    return pixels;
  }

  /** The value a connection mask stores for this width (1-5). */
  public int getMaskIndex() {
    return ordinal() + 1;
  }

  /**
   * The width a connection mask index stands for.
   *
   * @param index 1-5
   *
   * @return the width, or null for 0 (not one of these logs) or anything out of range
   */
  public static TreeLogWidth fromMaskIndex(int index) {
    TreeLogWidth[] all = values();
    return index >= 1 && index <= all.length ? all[index - 1] : null;
  }
}

package com.micatechnologies.minecraft.csm.furniture.market.shop;

/**
 * Where a shop draws the item it sells: rows of evenly spaced places on its shelves, in the
 * block's model space facing north (sixteenths, the front at low z), plus where its price label
 * goes. The numbers come from the same generator that draws the shelves
 * ({@code gen_furniture_market.py}), which prints them into the tab line, so an item stands on a
 * shelf rather than near one.
 *
 * <p>Each place shows one item. How many places are filled follows the stock: one place for
 * every quarter of a stack held ({@link #shown}).</p>
 *
 * @since 2026.10
 */
public final class ShopDisplay {

  /** Each row: {x0, x1, y, z, size, count} in sixteenths (size is the item's height). */
  private final float[][] rows;
  /** The price label: {x, y, z} in sixteenths, the label's centre on the front. */
  private final float[] label;
  private final int places;

  /**
   * A display.
   *
   * @param rows  each row's {x0, x1, y (shelf top), z (depth of the items' middle), size,
   *              count}, in sixteenths facing north
   * @param label the price label's centre {x, y, z}, in sixteenths facing north
   */
  public ShopDisplay(float[][] rows, float[] label) {
    this.rows = rows;
    this.label = label;
    int n = 0;
    for (float[] row : rows) {
      n += (int) row[5];
    }
    this.places = n;
  }

  public float[][] getRows() {
    return rows;
  }

  public float[] getLabel() {
    return label;
  }

  /** How many places it has across all its rows. */
  public int getPlaces() {
    return places;
  }

  /**
   * How many places to fill for {@code stock} items of a kind that stacks to {@code maxStack}:
   * every place when the stock is unlimited, otherwise one for each quarter stack held.
   */
  public int shown(int stock, int maxStack, boolean unlimited) {
    if (unlimited) {
      return places;
    }
    if (stock <= 0) {
      return 0;
    }
    int perPlace = Math.max(1, maxStack / 4);
    return Math.min(places, (stock + perPlace - 1) / perPlace);
  }
}

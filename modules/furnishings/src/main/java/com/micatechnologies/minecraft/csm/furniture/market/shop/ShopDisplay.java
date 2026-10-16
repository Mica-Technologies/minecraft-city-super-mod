package com.micatechnologies.minecraft.csm.furniture.market.shop;

/**
 * Where a shop draws the item it sells: rows of evenly spaced places on its shelves, in the
 * block's model space facing north (sixteenths, the front at low z), plus where its price label
 * goes. The numbers come from the same generator that draws the shelves
 * ({@code gen_furniture_market.py}), which prints them into the tab line, so an item stands on a
 * shelf rather than near one.
 *
 * <p>Each place is a box: its share of the row's width, the row's depth from {@code z0} (the
 * shelf's front edge, or the glass) to {@code z1} (its back), and {@code size} tall from the shelf
 * top. The renderer fits whatever item it draws inside that box by the item's measured extent,
 * so no item crosses a shelf, the glass or the next place.</p>
 *
 * <p>Each place shows one item. How many places are filled follows the stock: one place for
 * every quarter of a stack held ({@link #shown}).</p>
 *
 * @since 2026.10
 */
public final class ShopDisplay {

  /** A row's fields, in order. */
  public static final int X0 = 0;
  public static final int X1 = 1;
  public static final int Y = 2;
  public static final int Z0 = 3;
  public static final int Z1 = 4;
  public static final int SIZE = 5;
  public static final int COUNT = 6;

  /**
   * Each row: {x0, x1, y, z0, z1, size, count} in sixteenths (size is the most an item may stand
   * above the shelf top).
   */
  private final float[][] rows;
  /** The price label: {x, y, z} in sixteenths, the label's centre on the front. */
  private final float[] label;
  private final int places;

  /**
   * A display.
   *
   * @param rows  each row's {x0, x1, y (shelf top), z0 (front of the room for items), z1 (its
   *              back), size (height), count}, in sixteenths facing north
   * @param label the price label's centre {x, y, z}, in sixteenths facing north
   */
  public ShopDisplay(float[][] rows, float[] label) {
    this.rows = rows;
    this.label = label;
    int n = 0;
    for (float[] row : rows) {
      if (row.length != COUNT + 1) {
        throw new IllegalArgumentException("a shop display row has " + (COUNT + 1)
            + " numbers {x0, x1, y, z0, z1, size, count}, not " + row.length);
      }
      n += (int) row[COUNT];
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

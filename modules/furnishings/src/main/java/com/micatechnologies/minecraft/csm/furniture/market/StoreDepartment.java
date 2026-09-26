package com.micatechnologies.minecraft.csm.furniture.market;

import javax.annotation.Nonnull;
import net.minecraft.util.IStringSerializable;

/**
 * The departments a hanging department sign can name, in the order a click steps through them.
 * The blockstate picks the sign's band for each by its name ({@code gen_furniture_market.py}).
 *
 * <p>The ordinal is what {@link TileEntityDepartmentSign} saves, so the order is fixed: append,
 * never re-order.</p>
 *
 * @since 2026.9
 */
public enum StoreDepartment implements IStringSerializable {
  PRODUCE("produce"),
  BAKERY("bakery"),
  DELI("deli"),
  MEAT_SEAFOOD("meat_seafood"),
  PHARMACY("pharmacy"),
  FLORAL("floral"),
  DAIRY("dairy"),
  FROZEN("frozen"),
  CHECKOUT("checkout"),
  CUSTOMER_SERVICE("customer_service");

  private final String name;

  StoreDepartment(String name) {
    this.name = name;
  }

  @Override
  @Nonnull
  public String getName() {
    return name;
  }

  /**
   * The value saved as {@code ordinal}, or {@link #PRODUCE} if it is out of range.
   *
   * @param ordinal the saved ordinal
   *
   * @return the department
   */
  public static StoreDepartment byOrdinal(int ordinal) {
    StoreDepartment[] all = values();
    return ordinal >= 0 && ordinal < all.length ? all[ordinal] : PRODUCE;
  }
}

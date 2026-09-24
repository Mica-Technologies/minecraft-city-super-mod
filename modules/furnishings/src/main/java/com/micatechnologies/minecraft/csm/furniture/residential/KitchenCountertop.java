package com.micatechnologies.minecraft.csm.furniture.residential;

import javax.annotation.Nonnull;
import net.minecraft.util.IStringSerializable;

/**
 * Which countertop a built-in appliance under the counter (the dishwasher) carries over itself:
 * the one of the cabinets beside it, so the counter runs on unbroken, or its own plain top when
 * it stands alone. Light oak cabinets have dark granite, walnut and white have light quartz, as
 * {@code gen_furniture_kitchen.py} draws them.
 *
 * @since 2026.9
 */
public enum KitchenCountertop implements IStringSerializable {
  /** No cabinet beside it: the appliance's own top. */
  NONE,
  /** Dark granite, over light oak cabinets. */
  GRANITE,
  /** Light quartz, over walnut and white cabinets. */
  QUARTZ;

  /**
   * The countertop cabinets of {@code finish} have.
   *
   * @param finish the cabinets' finish ({@code oak}, {@code walnut}, {@code white})
   *
   * @return their countertop
   */
  public static KitchenCountertop of(String finish) {
    return "oak".equals(finish) ? GRANITE : QUARTZ;
  }

  @Nonnull
  @Override
  public String getName() {
    return name().toLowerCase(java.util.Locale.ROOT);
  }
}

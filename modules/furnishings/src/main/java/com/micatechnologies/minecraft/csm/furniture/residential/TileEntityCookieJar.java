package com.micatechnologies.minecraft.csm.furniture.residential;

import javax.annotation.Nonnull;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;

/**
 * A cookie jar's nine slots, which take cookies and nothing else, from a player or a hopper.
 *
 * @version 1.0
 * @since 2026.9
 */
public class TileEntityCookieJar extends TileEntityResidentialStorage {

  /** Constructs an empty jar, as the game does before loading a saved one. */
  public TileEntityCookieJar() {
    super(BlockCookieJar.SLOTS);
  }

  @Override
  public boolean accepts(@Nonnull ItemStack stack) {
    return stack.getItem() == Items.COOKIE;
  }
}

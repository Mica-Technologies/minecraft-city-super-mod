package com.micatechnologies.minecraft.csm.constructionsite;

import javax.annotation.Nonnull;
import net.minecraft.block.Block;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;

/**
 * The item for a crane part that comes in liveries: the livery is its metadata, and each livery
 * has its own name ({@code tile.<name>.<livery>.name}).
 *
 * @version 1.0
 * @since 2026.9
 */
public class ItemBlockCraneLivery extends ItemBlock {

  /**
   * Constructs an {@link ItemBlockCraneLivery}.
   *
   * @param block the crane part this is the item for
   *
   * @since 1.0
   */
  public ItemBlockCraneLivery(Block block) {
    super(block);
    setHasSubtypes(true);
    setMaxDamage(0);
  }

  @Override
  public int getMetadata(int damage) {
    return damage;
  }

  @Override
  @Nonnull
  public String getTranslationKey(@Nonnull ItemStack stack) {
    return super.getTranslationKey(stack) + "."
        + CraneLivery.fromOrdinal(stack.getMetadata()).getName();
  }
}

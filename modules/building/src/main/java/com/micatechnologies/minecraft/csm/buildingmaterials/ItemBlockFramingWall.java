package com.micatechnologies.minecraft.csm.buildingmaterials;

import javax.annotation.Nonnull;
import net.minecraft.block.Block;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;

/**
 * The item for a framed wall, which carries the wall's insulation in its metadata.
 *
 * <p>{@link FramingInsulation} is a block state, so that an insulated wall costs no registry name,
 * blockstate, model or tab class of its own. A state nothing carries, though, is a state nothing
 * can place: without this the insulated value would be reachable only through {@code /setblock}. This
 * gives each of them an item, a name and a place in the creative tab.</p>
 *
 * <p>The uninsulated wall keeps the block's own translation key, so the common case reads as
 * "Steel Stud Wall" rather than as a variant of something.</p>
 *
 * @version 1.0
 * @see BlockFramingWall#supportsInsulation()
 * @since 2026.9
 */
public class ItemBlockFramingWall extends ItemBlock {

  /**
   * Constructs an {@link ItemBlockFramingWall}.
   *
   * @param block the wall this is the item for
   *
   * @since 1.0
   */
  public ItemBlockFramingWall(Block block) {
    super(block);
    setHasSubtypes(true);
    setMaxDamage(0);
  }

  /**
   * The metadata a placed block takes from this stack. Passed through unchanged so that
   * {@link BlockFramingWall#getStateForPlacement} can read the insulation out of it.
   *
   * @param damage the stack's damage value
   *
   * @return the metadata to place with
   *
   * @since 1.0
   */
  @Override
  public int getMetadata(int damage) {
    return damage;
  }

  /**
   * The translation key, which is the block's own for an empty wall and a suffixed one for each
   * insulated variant.
   *
   * @param stack the stack to name
   *
   * @return the translation key, without the {@code .name} the language file adds
   *
   * @since 1.0
   */
  @Override
  @Nonnull
  public String getTranslationKey(@Nonnull ItemStack stack) {
    FramingInsulation insulation = BlockFramingWall.insulationFromMeta(stack.getMetadata());
    if (insulation == FramingInsulation.NONE) {
      return super.getTranslationKey(stack);
    }
    return super.getTranslationKey(stack) + "." + insulation.getName();
  }
}

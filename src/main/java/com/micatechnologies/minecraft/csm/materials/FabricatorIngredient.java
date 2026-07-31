package com.micatechnologies.minecraft.csm.materials;

import javax.annotation.Nullable;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.oredict.OreDictionary;

/**
 * One ingredient of a Fabricator recipe: an item, an optional subtype, and how many are needed.
 *
 * <p>Fabricator costs are not limited to {@link CsmParts}. Mixing in vanilla items lets a recipe
 * say something true about what the block is made of — coloured metal takes the matching dye,
 * wooden furniture takes planks, utility crossarms take timber — instead of expressing everything
 * as generic sheet metal.</p>
 *
 * <p>Metadata may be {@link OreDictionary#WILDCARD_VALUE} to accept any subtype, which is how
 * "any planks" or "any dye" is expressed. Where the subtype is the point, as with the dye that
 * colours a metal block, an exact value is used instead.</p>
 *
 * @author Mica Technologies
 * @since 2026.7
 */
public final class FabricatorIngredient {

  /** Full registry name, e.g. {@code csm:part_sheet_metal} or {@code minecraft:dye}. */
  private final String itemId;

  /** Required subtype, or {@link OreDictionary#WILDCARD_VALUE} for any. */
  private final int metadata;

  /** How many are consumed per unit fabricated. */
  private final int count;

  private FabricatorIngredient(String itemId, int metadata, int count) {
    this.itemId = itemId;
    this.metadata = metadata;
    this.count = count;
  }

  /**
   * An ingredient drawn from the mod's own crafting parts.
   *
   * @param partRegistryName a constant from {@link CsmParts}
   * @param count            how many are needed
   */
  public static FabricatorIngredient part(String partRegistryName, int count) {
    return new FabricatorIngredient("csm:" + partRegistryName, 0, count);
  }

  /**
   * A vanilla ingredient of any subtype.
   *
   * @param itemId full registry name, e.g. {@code minecraft:planks}
   * @param count  how many are needed
   */
  public static FabricatorIngredient any(String itemId, int count) {
    return new FabricatorIngredient(itemId, OreDictionary.WILDCARD_VALUE, count);
  }

  /**
   * A vanilla ingredient of one specific subtype.
   *
   * @param itemId   full registry name, e.g. {@code minecraft:dye}
   * @param metadata the required subtype
   * @param count    how many are needed
   */
  public static FabricatorIngredient exact(String itemId, int metadata, int count) {
    return new FabricatorIngredient(itemId, metadata, count);
  }

  public String getItemId() {
    return itemId;
  }

  public int getMetadata() {
    return metadata;
  }

  public int getCount() {
    return count;
  }

  /**
   * Resolves the ingredient to its {@link Item}, or {@code null} if it is not registered.
   *
   * @since 2026.7
   */
  @Nullable
  public Item resolve() {
    return Item.getByNameOrId(itemId);
  }

  /**
   * Whether the given stack satisfies this ingredient's item and subtype. Count is not
   * considered — callers total up matching stacks themselves.
   *
   * @since 2026.7
   */
  public boolean matches(ItemStack stack) {
    if (stack == null || stack.isEmpty()) {
      return false;
    }
    Item item = resolve();
    if (item == null || stack.getItem() != item) {
      return false;
    }
    return metadata == OreDictionary.WILDCARD_VALUE || stack.getMetadata() == metadata;
  }

  /**
   * Builds a display stack for this ingredient. Wildcard subtypes render as subtype 0, which is
   * the plain variant of every vanilla item used here.
   *
   * @param quantity batch multiplier applied to the per-unit count
   *
   * @since 2026.7
   */
  public ItemStack toDisplayStack(int quantity) {
    Item item = resolve();
    if (item == null) {
      return ItemStack.EMPTY;
    }
    int meta = metadata == OreDictionary.WILDCARD_VALUE ? 0 : metadata;
    return new ItemStack(item, Math.max(1, count * quantity), meta);
  }
}

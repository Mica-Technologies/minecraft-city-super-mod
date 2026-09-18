package com.micatechnologies.minecraft.csm.constructionsite;

import com.micatechnologies.minecraft.csm.CsmRegistry;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

/**
 * The add-ons a player can fit to a placed scaffold bay with an item, and take off again.
 *
 * <p>Each is one stored bit on the bay. Declared in the order they come off: a sneaking
 * right-click with an empty hand removes the first one fitted in this order, so the netting, which
 * covers the rest, comes off before the casters and the ladder frame behind it.</p>
 *
 * @version 1.0
 * @since 2026.9
 */
public enum ScaffoldAddon {

  /** Green debris netting on every open face. */
  NETTING(BlockScaffoldFrame.NETTED, "scaffold_netting"),

  /** Casters in place of screw jacks, where the bay stands on the ground. */
  CASTERS(BlockScaffoldFrame.CASTERS, "scaffold_casters"),

  /** Ladder frames, with rungs, in place of walk-through frames. */
  LADDER_FRAME(BlockScaffoldFrame.LADDER, "scaffold_ladder_frame");

  private final PropertyBool property;
  private final String itemRegistryName;

  ScaffoldAddon(PropertyBool property, String itemRegistryName) {
    this.property = property;
    this.itemRegistryName = itemRegistryName;
  }

  /**
   * The bay's stored flag for this add-on.
   *
   * @return the property
   *
   * @since 1.0
   */
  public PropertyBool getProperty() {
    return property;
  }

  /**
   * The registry name of the item that fits this add-on.
   *
   * @return the item's registry name, without the namespace
   *
   * @since 1.0
   */
  public String getItemRegistryName() {
    return itemRegistryName;
  }

  /**
   * One of the item that fits this add-on, as given back when it is taken off or the bay is
   * broken.
   *
   * @return the stack, or an empty stack if the item is somehow not registered
   *
   * @since 1.0
   */
  public ItemStack toStack() {
    Item item = CsmRegistry.getItem(itemRegistryName);
    return item == null ? ItemStack.EMPTY : new ItemStack(item);
  }
}

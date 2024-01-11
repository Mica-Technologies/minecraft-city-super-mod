package com.micatechnologies.minecraft.csm.codeutils;

import com.micatechnologies.minecraft.csm.Csm;
import com.micatechnologies.minecraft.csm.CsmConstants;
import com.micatechnologies.minecraft.csm.CsmRegistry;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.item.Item;

/**
 * Abstract item class which provides common methods and properties for all items in this mod.
 *
 * @version 1.0
 * @see Item
 * @since 2023.3
 */
@MethodsReturnNonnullByDefault
public abstract class AbstractItem extends Item implements IHasModel, ICsmItem {

  /**
   * Constructs an AbstractItem using default values (0 damage, 64 stack size).
   *
   * @since 1.0
   */
  public AbstractItem() {
    this(0, 64);
  }

  /**
   * Constructs an AbstractItem.
   *
   * @param maxDamage    The maximum damage of the item.
   * @param maxStackSize The maximum stack size of the item.
   *
   * @since 1.0
   */
  public AbstractItem(int maxDamage, int maxStackSize) {
    setTranslationKey(getItemRegistryName());
    setRegistryName(CsmConstants.MOD_NAMESPACE, getItemRegistryName());
    setMaxDamage(maxDamage);
    setMaxStackSize(maxStackSize);
    CsmRegistry.registerItem(this);
  }

  /**
   * Registers the block's model.
   *
   * @since 1.0
   */
  @Override
  public void registerModels() {
    Csm.proxy.setCustomModelResourceLocation(this, 0, "inventory");
  }
}

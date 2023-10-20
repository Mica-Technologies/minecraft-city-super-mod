package com.micatechnologies.minecraft.csm.codeutils;

import com.micatechnologies.minecraft.csm.Csm;
import com.micatechnologies.minecraft.csm.CsmConstants;
import com.micatechnologies.minecraft.csm.CsmRegistry;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.item.Item;
import net.minecraft.item.ItemSpade;

/**
 * Abstract item spade class which provides common methods and properties for all item spades in
 * this mod.
 *
 * @version 1.0
 * @see Item
 * @since 2023.3
 */
@MethodsReturnNonnullByDefault
public abstract class AbstractItemSpade extends ItemSpade implements IHasModel, ICsmItem {

  /**
   * Constructs an AbstractItemSpade using default values (0 damage, 64 stack size).
   *
   * @since 1.0
   */
  public AbstractItemSpade(ToolMaterial material) {
    this(0, 64, material);
  }

  /**
   * Constructs an AbstractItemSpade.
   *
   * @param maxDamage    The maximum damage of the item spade.
   * @param maxStackSize The maximum stack size of the item spade.
   *
   * @since 1.0
   */
  public AbstractItemSpade(int maxDamage, int maxStackSize, ToolMaterial material) {
    super(material);
    setUnlocalizedName(getItemRegistryName());
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

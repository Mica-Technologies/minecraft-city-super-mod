package com.micatechnologies.minecraft.csm.codeutils;

import java.util.List;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

/**
 * Factory for creating decorative {@link AbstractItem} instances that only differ in registry name
 * and tooltip text. Eliminates the need for a separate class file per decorative item.
 *
 * @since 2026.4
 */
public class ItemDecorativeFactory extends AbstractItem {

  /**
   * ThreadLocal used to pass the registry name to the superclass constructor. The AbstractItem
   * constructor calls getItemRegistryName() before subclass fields are initialized, so we
   * store the name here before calling super() and read it in getItemRegistryName().
   */
  private static final ThreadLocal<String> PENDING_REGISTRY_NAME = new ThreadLocal<>();

  private final String registryName;
  private final String tooltip;

  public ItemDecorativeFactory(String registryName, String tooltip) {
    this(initRegistryName(registryName), registryName, tooltip);
  }

  private ItemDecorativeFactory(Void ignored, String registryName, String tooltip) {
    this.registryName = registryName;
    this.tooltip = tooltip;
  }

  private static Void initRegistryName(String name) {
    PENDING_REGISTRY_NAME.set(name);
    return null;
  }

  @Override
  public String getItemRegistryName() {
    if (registryName != null) {
      return registryName;
    }
    return PENDING_REGISTRY_NAME.get();
  }

  @Override
  public void addInformation(ItemStack itemstack, World world, List<String> list,
      ITooltipFlag flag) {
    super.addInformation(itemstack, world, list, flag);
    if (tooltip != null && !tooltip.isEmpty()) {
      list.add(tooltip);
    }
  }
}

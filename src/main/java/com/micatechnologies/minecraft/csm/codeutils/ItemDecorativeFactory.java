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

  private final String registryName;
  private final String tooltip;

  public ItemDecorativeFactory(String registryName, String tooltip) {
    this.registryName = registryName;
    this.tooltip = tooltip;
  }

  @Override
  public String getItemRegistryName() {
    return registryName;
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

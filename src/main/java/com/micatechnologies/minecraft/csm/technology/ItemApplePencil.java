package com.micatechnologies.minecraft.csm.technology;

import com.micatechnologies.minecraft.csm.codeutils.AbstractItemSpade;
import java.util.HashMap;
import java.util.Set;
import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;
import net.minecraft.item.ItemStack;
import net.minecraftforge.common.util.EnumHelper;

public class ItemApplePencil extends AbstractItemSpade {

  public ItemApplePencil() {
    super(0, 64, EnumHelper.addToolMaterial("APPLEPENCIL", 1, 100, 4f, 1f, 2));
    this.attackSpeed = -1.2f;
  }

  /**
   * Retrieves the registry name of the item.
   *
   * @return The registry name of the item.
   *
   * @since 1.0
   */
  @Override
  public String getItemRegistryName() {
    return "applepencil";
  }

  @Override
  @Nonnull
  @ParametersAreNonnullByDefault
  public Set<String> getToolClasses(ItemStack stack) {
    HashMap<String, Integer> ret = new HashMap<>();
    ret.put("spade", 1);
    return ret.keySet();
  }
}

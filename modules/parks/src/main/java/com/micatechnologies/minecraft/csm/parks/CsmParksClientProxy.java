package com.micatechnologies.minecraft.csm.parks;

import com.micatechnologies.minecraft.csm.CsmRegistry;
import com.micatechnologies.minecraft.csm.parks.tools.ItemChainsaw;
import com.micatechnologies.minecraft.csm.parks.trees.TreeModels;
import net.minecraft.item.Item;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;

/**
 * The Parks &amp; Greenery module's client proxy: puts the tree kit's baked models in place, and
 * lets the chainsaw's item model show it running.
 *
 * @since 2026.9
 */
public class CsmParksClientProxy extends CsmParksCommonProxy {

  @Override
  public void preInit(FMLPreInitializationEvent event) {
    TreeModels.register();
    // The item exists by now: Core's preInit builds every tab before a module's preInit runs.
    Item saw = CsmRegistry.getItem("chainsaw");
    if (saw != null) {
      saw.addPropertyOverride(new ResourceLocation("csm", "running"),
          (stack, world, entity) -> ItemChainsaw.isRunning(stack) ? 1.0F : 0.0F);
    }
  }
}

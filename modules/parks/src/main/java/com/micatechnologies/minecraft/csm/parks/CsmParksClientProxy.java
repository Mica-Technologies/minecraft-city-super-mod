package com.micatechnologies.minecraft.csm.parks;

import com.micatechnologies.minecraft.csm.CsmRegistry;
import com.micatechnologies.minecraft.csm.parks.planters.PlanterModels;
import com.micatechnologies.minecraft.csm.parks.tools.ItemFuelledTool;
import com.micatechnologies.minecraft.csm.parks.trees.TreeModels;
import net.minecraft.item.Item;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;

/**
 * The Parks &amp; Greenery module's client proxy: puts the tree kit's and the planters' baked
 * models in place, gives a planter its plant's colour, and lets the chainsaw's and the stump
 * grinder's item models show them running.
 *
 * @since 2026.9
 */
public class CsmParksClientProxy extends CsmParksCommonProxy {

  @Override
  public void preInit(FMLPreInitializationEvent event) {
    TreeModels.register();
    PlanterModels.register();
    // The items exist by now: Core's preInit builds every tab before a module's preInit runs.
    for (String name : new String[]{"chainsaw", "stump_grinder"}) {
      Item tool = CsmRegistry.getItem(name);
      if (tool != null) {
        tool.addPropertyOverride(new ResourceLocation("csm", "running"),
            (stack, world, entity) -> ItemFuelledTool.isRunning(stack) ? 1.0F : 0.0F);
      }
    }
  }

  @Override
  public void init(FMLInitializationEvent event) {
    super.init(event);
    PlanterModels.registerColours();
  }
}

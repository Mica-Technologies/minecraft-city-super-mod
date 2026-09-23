package com.micatechnologies.minecraft.csm.parks;

import com.micatechnologies.minecraft.csm.parks.trees.TreeModels;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;

/**
 * The Parks &amp; Greenery module's client proxy: puts the tree kit's baked models in place.
 *
 * @since 2026.9
 */
public class CsmParksClientProxy extends CsmParksCommonProxy {

  @Override
  public void preInit(FMLPreInitializationEvent event) {
    TreeModels.register();
  }
}

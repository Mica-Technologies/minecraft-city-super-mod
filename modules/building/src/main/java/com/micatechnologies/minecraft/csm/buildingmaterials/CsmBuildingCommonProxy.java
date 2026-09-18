package com.micatechnologies.minecraft.csm.buildingmaterials;

import com.micatechnologies.minecraft.csm.codeutils.ICsmProxy;
import net.minecraft.item.Item;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;

/**
 * The building module's proxy on a dedicated server, where it has nothing to do.
 *
 * @version 1.0
 * @since 2026.9
 */
public class CsmBuildingCommonProxy implements ICsmProxy {

  @Override
  public void preInit(FMLPreInitializationEvent event) {
    // Nothing to do on the server side
  }

  @Override
  public void init(FMLInitializationEvent event) {
    // Nothing to do on the server side
  }

  @Override
  public void postInit(FMLPostInitializationEvent event) {
    // Nothing to do on the server side
  }

  @Override
  public void serverLoad(FMLServerStartingEvent event) {
    // Nothing to do on the server side
  }

  @Override
  public void setCustomModelResourceLocation(Item item, int meta, String id) {
    // Does nothing on the server side
  }
}

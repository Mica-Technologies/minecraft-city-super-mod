package com.micatechnologies.minecraft.csm.roads;

import com.micatechnologies.minecraft.csm.codeutils.ICsmProxy;
import net.minecraft.item.Item;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;

/**
 * Server-side (and common) proxy for the Roads & Traffic module. Everything this module does per side is
 * rendering, so the common proxy has nothing to do; the client proxy extends it.
 *
 * @see ICsmProxy
 */
public class CsmRoadsCommonProxy implements ICsmProxy {

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

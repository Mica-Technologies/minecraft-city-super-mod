package com.micatechnologies.minecraft.csm.parks;

import com.micatechnologies.minecraft.csm.codeutils.ICsmProxy;
import net.minecraft.item.Item;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;

/**
 * The Parks &amp; Greenery module's server-side proxy: nothing to do.
 *
 * @since 2026.9
 */
public class CsmParksCommonProxy implements ICsmProxy {

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

package com.micatechnologies.minecraft.csm.tts;

import com.micatechnologies.minecraft.csm.codeutils.ICsmProxy;
import net.minecraft.item.Item;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;

/**
 * Sided proxy for the Text to Speech module on the server/common side. Speech synthesis is
 * client work — a dedicated server has no audio device and never plays a message — so this proxy
 * does nothing at all.
 *
 * @version 1.0
 * @see ICsmProxy
 * @since 2026.9
 */
public class CsmTextToSpeechCommonProxy implements ICsmProxy {

  @Override
  public void preInit(FMLPreInitializationEvent event) {
    // Nothing to do: the speech engine is client-only
  }

  @Override
  public void init(FMLInitializationEvent event) {
    // Nothing to do: the speech engine is client-only
  }

  @Override
  public void postInit(FMLPostInitializationEvent event) {
    // Nothing to do: the speech engine is client-only
  }

  @Override
  public void serverLoad(FMLServerStartingEvent event) {
    // Nothing to do: the speech engine is client-only
  }

  @Override
  public void setCustomModelResourceLocation(Item item, int meta, String id) {
    // Does nothing on the server side
  }
}

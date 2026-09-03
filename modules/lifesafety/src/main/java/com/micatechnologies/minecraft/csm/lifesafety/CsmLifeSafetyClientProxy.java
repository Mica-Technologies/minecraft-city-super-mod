package com.micatechnologies.minecraft.csm.lifesafety;

import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;

/**
 * Client proxy for the Life Safety module: binds this module's tile-entity special renderers (and any
 * client-only overlays) once Forge reaches initialization. These bindings used to live in Core's
 * client proxy; they are here so Core never names a class that belongs to a module.
 */
public class CsmLifeSafetyClientProxy extends CsmLifeSafetyCommonProxy {

  @Override
  public void init(FMLInitializationEvent event) {
    ClientRegistry.bindTileEntitySpecialRenderer(
        com.micatechnologies.minecraft.csm.lifesafety.TileEntityFireAlarmStrobe.class,
        new com.micatechnologies.minecraft.csm.lifesafety.TileEntityFireAlarmStrobeRenderer());
    // Also bind to TileEntityFireAlarmSoundIndex so Gentex Commander 3 (which uses that TE
    // for sound selection) can also render strobe effects — the renderer checks IStrobeBlock
    ClientRegistry.bindTileEntitySpecialRenderer(
        com.micatechnologies.minecraft.csm.lifesafety.TileEntityFireAlarmSoundIndex.class,
        new com.micatechnologies.minecraft.csm.lifesafety.TileEntityFireAlarmStrobeRenderer());

    ClientRegistry.bindTileEntitySpecialRenderer(
        com.micatechnologies.minecraft.csm.lifesafety.TileEntityEmergencyLight.class,
        new com.micatechnologies.minecraft.csm.lifesafety.TileEntityEmergencyLightRenderer());
  }
}

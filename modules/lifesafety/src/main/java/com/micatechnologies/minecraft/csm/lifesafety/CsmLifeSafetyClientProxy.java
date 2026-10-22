package com.micatechnologies.minecraft.csm.lifesafety;

import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;

/**
 * Client proxy for the Life Safety module: binds this module's tile-entity special renderers (and any
 * client-only overlays) once Forge reaches initialization. These bindings used to live in Core's
 * client proxy; they are here so Core never names a class that belongs to a module.
 */
public class CsmLifeSafetyClientProxy extends CsmLifeSafetyCommonProxy {

  @Override
  public void preInit(FMLPreInitializationEvent event) {
    // A dressed standpipe pass-through draws the block it copies, baked into the chunk mesh
    com.micatechnologies.minecraft.csm.lifesafety.fireprotection.StandpipePassthroughModels
        .register();
  }

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
    // And to detectors with a strobe built in, whose tile entity is a sensor's
    ClientRegistry.bindTileEntitySpecialRenderer(
        com.micatechnologies.minecraft.csm.lifesafety.TileEntityFireAlarmSensorStrobe.class,
        new com.micatechnologies.minecraft.csm.lifesafety.TileEntityFireAlarmStrobeRenderer());

    ClientRegistry.bindTileEntitySpecialRenderer(
        com.micatechnologies.minecraft.csm.lifesafety.TileEntityEmergencyLight.class,
        new com.micatechnologies.minecraft.csm.lifesafety.TileEntityEmergencyLightRenderer());
    // Exit signs with emergency heads glow like the emergency lights on battery
    ClientRegistry.bindTileEntitySpecialRenderer(
        com.micatechnologies.minecraft.csm.lifesafety.exitsign.TileEntityExitSign.class,
        new com.micatechnologies.minecraft.csm.lifesafety.TileEntityEmergencyLightRenderer());
    // A rotating warning siren's horn, drawn only while it sounds
    ClientRegistry.bindTileEntitySpecialRenderer(
        com.micatechnologies.minecraft.csm.lifesafety.stations.TileEntityWarningSiren.class,
        new com.micatechnologies.minecraft.csm.lifesafety.stations.TileEntityWarningSirenRenderer());
  }
}

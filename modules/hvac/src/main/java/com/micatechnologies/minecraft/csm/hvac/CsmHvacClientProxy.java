package com.micatechnologies.minecraft.csm.hvac;

import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;

/**
 * Client proxy for the HVAC module: binds this module's tile-entity special renderers (and any
 * client-only overlays) once Forge reaches initialization. These bindings used to live in Core's
 * client proxy; they are here so Core never names a class that belongs to a module.
 */
public class CsmHvacClientProxy extends CsmHvacCommonProxy {

  @Override
  public void init(FMLInitializationEvent event) {
    // Register HVAC thermostat TESRs (primary and zone share the same renderer)
    ClientRegistry.bindTileEntitySpecialRenderer(
        com.micatechnologies.minecraft.csm.hvac.TileEntityHvacThermostat.class,
        new com.micatechnologies.minecraft.csm.hvac.TileEntityHvacThermostatRenderer());
    ClientRegistry.bindTileEntitySpecialRenderer(
        com.micatechnologies.minecraft.csm.hvac.TileEntityHvacZoneThermostat.class,
        new com.micatechnologies.minecraft.csm.hvac.TileEntityHvacThermostatRenderer());

    // Register HVAC temperature HUD overlay
    com.micatechnologies.minecraft.csm.hvac.HvacHudOverlay.register();
  }
}

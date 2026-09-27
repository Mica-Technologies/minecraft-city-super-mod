package com.micatechnologies.minecraft.csm.lighting;

import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;

/**
 * Client proxy for the Lighting module: binds its tile-entity renderers once Forge reaches
 * initialization.
 *
 * @since 2026.9
 */
public class CsmLightingClientProxy extends CsmLightingCommonProxy {

  @Override
  public void init(FMLInitializationEvent event) {
    // The tall building beacons' flash and glow
    ClientRegistry.bindTileEntitySpecialRenderer(TileEntityObstructionBeacon.class,
        new TileEntityObstructionBeaconRenderer());
  }
}

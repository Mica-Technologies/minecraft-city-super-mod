package com.micatechnologies.minecraft.csm.furnishings;

import com.micatechnologies.minecraft.csm.furniture.residential.TileEntityCeilingFan;
import com.micatechnologies.minecraft.csm.furniture.residential.TileEntityCeilingFanRenderer;
import com.micatechnologies.minecraft.csm.furniture.residential.TileEntityResidentialClock;
import com.micatechnologies.minecraft.csm.furniture.residential.TileEntityResidentialClockRenderer;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;

/**
 * Client proxy for the Furniture &amp; Novelties module: binds this module's tile-entity special
 * renderers once Forge reaches initialization -- the Residential clocks' time and the ceiling
 * fan's turning blades.
 *
 * @since 2026.9
 */
public class CsmFurnishingsClientProxy extends CsmFurnishingsCommonProxy {

  @Override
  public void init(FMLInitializationEvent event) {
    ClientRegistry.bindTileEntitySpecialRenderer(TileEntityResidentialClock.class,
        new TileEntityResidentialClockRenderer());
    ClientRegistry.bindTileEntitySpecialRenderer(TileEntityCeilingFan.class,
        new TileEntityCeilingFanRenderer());
  }
}

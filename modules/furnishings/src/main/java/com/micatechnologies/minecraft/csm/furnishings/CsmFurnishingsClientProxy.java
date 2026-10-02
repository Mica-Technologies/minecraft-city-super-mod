package com.micatechnologies.minecraft.csm.furnishings;

import com.micatechnologies.minecraft.csm.furniture.office.TileEntityCubicleNamePlate;
import com.micatechnologies.minecraft.csm.furniture.office.TileEntityCubicleNamePlateRenderer;
import com.micatechnologies.minecraft.csm.furniture.residential.TileEntityCeilingFan;
import com.micatechnologies.minecraft.csm.furniture.residential.TileEntityCeilingFanRenderer;
import com.micatechnologies.minecraft.csm.furniture.residential.TileEntityResidentialClock;
import com.micatechnologies.minecraft.csm.furniture.residential.TileEntityResidentialClockRenderer;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;

/**
 * Client proxy for the Furniture &amp; Novelties module: binds this module's tile-entity special
 * renderers once Forge reaches initialization -- the Residential clocks' time, the ceiling
 * fan's turning blades and the words on a cubicle panel's name plate.
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
    ClientRegistry.bindTileEntitySpecialRenderer(TileEntityCubicleNamePlate.class,
        new TileEntityCubicleNamePlateRenderer());
  }
}

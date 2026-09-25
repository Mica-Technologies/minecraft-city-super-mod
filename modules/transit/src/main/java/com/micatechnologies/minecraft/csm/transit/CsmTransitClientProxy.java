package com.micatechnologies.minecraft.csm.transit;

import com.micatechnologies.minecraft.csm.transit.airport.TileEntityFlightBoard;
import com.micatechnologies.minecraft.csm.transit.airport.TileEntityFlightBoardRenderer;
import com.micatechnologies.minecraft.csm.transit.platform.TileEntityPlatformClock;
import com.micatechnologies.minecraft.csm.transit.platform.TileEntityPlatformClockRenderer;
import com.micatechnologies.minecraft.csm.transit.stop.TileEntityBusArrivalDisplay;
import com.micatechnologies.minecraft.csm.transit.stop.TileEntityBusArrivalDisplayRenderer;
import com.micatechnologies.minecraft.csm.transit.stop.TileEntityBusStopFlag;
import com.micatechnologies.minecraft.csm.transit.stop.TileEntityBusStopFlagRenderer;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;

/**
 * The transit module's proxy on the client: the renderers for the bus stop flag's route numbers,
 * the arrival display's text, the platform clock's hands and the flight information boards'
 * screens. Everything else is drawn from
 * baked models.
 *
 * @since 2026.9
 */
public class CsmTransitClientProxy extends CsmTransitCommonProxy {

  @Override
  public void init(FMLInitializationEvent event) {
    ClientRegistry.bindTileEntitySpecialRenderer(TileEntityBusStopFlag.class,
        new TileEntityBusStopFlagRenderer());
    ClientRegistry.bindTileEntitySpecialRenderer(TileEntityBusArrivalDisplay.class,
        new TileEntityBusArrivalDisplayRenderer());
    ClientRegistry.bindTileEntitySpecialRenderer(TileEntityPlatformClock.class,
        new TileEntityPlatformClockRenderer());
    ClientRegistry.bindTileEntitySpecialRenderer(TileEntityFlightBoard.class,
        new TileEntityFlightBoardRenderer());
  }
}

package com.micatechnologies.minecraft.csm.transit;

import com.micatechnologies.minecraft.csm.codeutils.CsmLifecycleHooks;
import com.micatechnologies.minecraft.csm.transit.airport.TileEntityFlightBoard;
import com.micatechnologies.minecraft.csm.transit.airport.TileEntityFlightBoardLarge;
import com.micatechnologies.minecraft.csm.transit.airport.TileEntityFlightBoardLargeRenderer;
import com.micatechnologies.minecraft.csm.transit.airport.TileEntityFlightBoardRenderer;
import com.micatechnologies.minecraft.csm.transit.board.BusBoardAnnouncer;
import com.micatechnologies.minecraft.csm.transit.board.BusStation;
import com.micatechnologies.minecraft.csm.transit.board.TileEntityBusBayDisplay;
import com.micatechnologies.minecraft.csm.transit.board.TileEntityBusBayDisplayRenderer;
import com.micatechnologies.minecraft.csm.transit.board.TileEntityBusDepartureBoard;
import com.micatechnologies.minecraft.csm.transit.board.TileEntityBusDepartureBoardRenderer;
import com.micatechnologies.minecraft.csm.transit.platform.TileEntityPlatformClock;
import com.micatechnologies.minecraft.csm.transit.platform.TileEntityPlatformClockRenderer;
import com.micatechnologies.minecraft.csm.transit.stop.TileEntityBusArrivalDisplay;
import com.micatechnologies.minecraft.csm.transit.stop.TileEntityBusArrivalDisplayRenderer;
import com.micatechnologies.minecraft.csm.transit.stop.TileEntityBusStopFlag;
import com.micatechnologies.minecraft.csm.transit.stop.TileEntityBusStopFlagRenderer;
import com.micatechnologies.minecraft.csm.transit.wayfinding.TileEntityWayfindingPanel;
import com.micatechnologies.minecraft.csm.transit.wayfinding.TileEntityWayfindingPanelRenderer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;

/**
 * The transit module's proxy on the client: the renderers for the bus stop flag's route numbers,
 * the arrival display's text, the platform clock's hands, the flight information boards' screens
 * and the bus departure boards' and bay displays' screens, the large hanging signs' legends,
 * and the departure boards' spoken announcements (only when Text to Speech is installed).
 * Everything else is drawn from baked models.
 *
 * @since 2026.9
 */
public class CsmTransitClientProxy extends CsmTransitCommonProxy {

  /**
   * The large hanging sign's sprites go on the block atlas at its first stitch, which comes
   * before initialization.
   */
  @Override
  public void preInit(FMLPreInitializationEvent event) {
    MinecraftForge.EVENT_BUS.register(new TileEntityWayfindingPanelRenderer.Sprites());
  }

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
    ClientRegistry.bindTileEntitySpecialRenderer(TileEntityFlightBoardLarge.class,
        new TileEntityFlightBoardLargeRenderer());
    ClientRegistry.bindTileEntitySpecialRenderer(TileEntityBusDepartureBoard.class,
        new TileEntityBusDepartureBoardRenderer());
    ClientRegistry.bindTileEntitySpecialRenderer(TileEntityBusBayDisplay.class,
        new TileEntityBusBayDisplayRenderer());
    ClientRegistry.bindTileEntitySpecialRenderer(TileEntityWayfindingPanel.class,
        new TileEntityWayfindingPanelRenderer());
    BusBoardAnnouncer announcer = new BusBoardAnnouncer();
    MinecraftForge.EVENT_BUS.register(announcer);
    CsmLifecycleHooks.onClientDisconnect(() -> {
      BusStation.clear();
      announcer.clear();
    });
  }
}

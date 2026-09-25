package com.micatechnologies.minecraft.csm.tabs;

import com.micatechnologies.minecraft.csm.CsmRegistry;
import com.micatechnologies.minecraft.csm.codeutils.CsmTab;
import com.micatechnologies.minecraft.csm.transit.fare.BlockFareGate;
import com.micatechnologies.minecraft.csm.transit.fare.BlockFareGateAda2;
import com.micatechnologies.minecraft.csm.transit.fare.BlockFareGateAda3;
import com.micatechnologies.minecraft.csm.transit.fare.BlockFareVendingMachine;
import com.micatechnologies.minecraft.csm.transit.fare.ItemFareTicket;
import com.micatechnologies.minecraft.csm.transit.fare.ItemTransitCard;
import com.micatechnologies.minecraft.csm.transit.shelter.BlockBusShelter;
import com.micatechnologies.minecraft.csm.transit.shelter.BusShelterStyle;
import com.micatechnologies.minecraft.csm.transit.stop.BlockBusArrivalDisplay;
import com.micatechnologies.minecraft.csm.transit.stop.BlockBusStopFitting;
import com.micatechnologies.minecraft.csm.transit.stop.BlockBusStopFlag;
import com.micatechnologies.minecraft.csm.transit.stop.BlockBusStopPlaque;
import com.micatechnologies.minecraft.csm.transit.stop.BlockBusStopPole;
import com.micatechnologies.minecraft.csm.transit.stop.BusStopPoleStyle;
import net.minecraft.block.Block;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;

/**
 * The tab for public transit: the fare gates, the fare vending machine and the tickets and cards
 * they take, and the bus stops (poles, agency flags, poster cases, the arrival display and the
 * curb plaque).
 *
 * @version 1.0
 * @since 2026.9
 */
@CsmTab.Load(order = 27)
public class CsmTabTransit extends CsmTab {

  /**
   * Gets the ID (unique identifier) of the tab.
   *
   * @return the ID of the tab
   *
   * @since 1.0
   */
  @Override
  public String getTabId() {
    return "tabtransit";
  }

  /**
   * Gets the block to use as the icon of the tab.
   *
   * @return the block to use as the icon of the tab
   *
   * @since 1.0
   */
  @Override
  public Block getTabIcon() {
    return CsmRegistry.getBlock("fare_gate");
  }

  /**
   * Gets a boolean indicating if the tab is searchable (has its own search bar).
   *
   * @return {@code true} if the tab is searchable, otherwise {@code false}
   *
   * @since 1.0
   */
  @Override
  public boolean getTabSearchable() {
    return false;
  }

  /**
   * Gets a boolean indicating if the tab is hidden (not displayed in the inventory).
   *
   * @return {@code true} if the tab is hidden, otherwise {@code false}
   *
   * @since 1.0
   */
  @Override
  public boolean getTabHidden() {
    return false;
  }

  /**
   * Initializes all the elements belonging to the tab.
   *
   * @param fmlPreInitializationEvent the {@link FMLPreInitializationEvent} that is being processed
   *
   * @since 1.0
   */
  @Override
  public void initTabElements(FMLPreInitializationEvent fmlPreInitializationEvent) {
    initTabBlock(BlockFareVendingMachine.class, fmlPreInitializationEvent);
    initTabBlock(BlockFareGate.class, fmlPreInitializationEvent);
    initTabBlock(BlockFareGateAda2.class, fmlPreInitializationEvent);
    initTabBlock(BlockFareGateAda3.class, fmlPreInitializationEvent);
    initTabItem(ItemFareTicket.class, fmlPreInitializationEvent);
    initTabItem(ItemTransitCard.class, fmlPreInitializationEvent);

    // Bus stops (gen_transit_stops.py --fragments)
    initTabBlock(new BlockBusStopPole("bus_stop_pole_round_galvanized", BusStopPoleStyle.ROUND_GALVANIZED));
    initTabBlock(new BlockBusStopPole("bus_stop_pole_square_galvanized", BusStopPoleStyle.SQUARE_GALVANIZED));
    initTabBlock(new BlockBusStopPole("bus_stop_pole_round_teal", BusStopPoleStyle.ROUND_TEAL));
    initTabBlock(new BlockBusStopPole("bus_stop_pole_square_navy", BusStopPoleStyle.SQUARE_NAVY));
    initTabBlock(new BlockBusStopPole("bus_stop_pole_round_green", BusStopPoleStyle.ROUND_GREEN));
    initTabBlock(new BlockBusStopPole("bus_stop_pole_square_red", BusStopPoleStyle.SQUARE_RED));
    initTabBlock(new BlockBusStopFlag("bus_stop_flag_cityline", new double[]{6.7, 0, 6.7, 16, 16, 9.3}));
    initTabBlock(new BlockBusStopFlag("bus_stop_flag_riverway", new double[]{6.7, 0, 6.7, 16, 16, 9.3}));
    initTabBlock(new BlockBusStopFlag("bus_stop_flag_verdant", new double[]{6.7, 0, 6.7, 16, 16, 9.3}));
    initTabBlock(new BlockBusStopFlag("bus_stop_flag_emberline", new double[]{6.7, 0, 6.7, 16, 16, 9.3}));
    initTabBlock(new BlockBusStopFitting("bus_stop_timetable_case", new double[]{4, 0, 5.5, 12, 16, 9.3}));
    initTabBlock(new BlockBusStopFitting("bus_stop_route_map_case", new double[]{3.5, 0, 5.5, 12.5, 16, 9.3}));
    initTabBlock(new BlockBusArrivalDisplay("bus_stop_arrival_display", new double[]{0.7, 0, 3.7, 15.3, 16, 9.3}));
    initTabBlock(new BlockBusStopPlaque("bus_stop_curb_plaque"));

    // Bus shelters (gen_transit_shelters.py --fragments)
    initTabBlock(new BlockBusShelter("bus_shelter_glass_cityline", BusShelterStyle.GLASS));
    initTabBlock(new BlockBusShelter("bus_shelter_glass_riverway", BusShelterStyle.GLASS));
    initTabBlock(new BlockBusShelter("bus_shelter_glass_verdant", BusShelterStyle.GLASS));
    initTabBlock(new BlockBusShelter("bus_shelter_glass_emberline", BusShelterStyle.GLASS));
    initTabBlock(new BlockBusShelter("bus_shelter_cantilever_cityline", BusShelterStyle.CANTILEVER));
    initTabBlock(new BlockBusShelter("bus_shelter_cantilever_riverway", BusShelterStyle.CANTILEVER));
    initTabBlock(new BlockBusShelter("bus_shelter_cantilever_verdant", BusShelterStyle.CANTILEVER));
    initTabBlock(new BlockBusShelter("bus_shelter_cantilever_emberline", BusShelterStyle.CANTILEVER));
    initTabBlock(new BlockBusShelter("bus_shelter_flat_cityline", BusShelterStyle.FLAT));
    initTabBlock(new BlockBusShelter("bus_shelter_flat_riverway", BusShelterStyle.FLAT));
    initTabBlock(new BlockBusShelter("bus_shelter_flat_verdant", BusShelterStyle.FLAT));
    initTabBlock(new BlockBusShelter("bus_shelter_flat_emberline", BusShelterStyle.FLAT));
  }
}

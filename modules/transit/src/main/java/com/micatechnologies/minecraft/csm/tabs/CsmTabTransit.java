package com.micatechnologies.minecraft.csm.tabs;

import com.micatechnologies.minecraft.csm.CsmRegistry;
import com.micatechnologies.minecraft.csm.codeutils.CsmTab;
import com.micatechnologies.minecraft.csm.transit.airport.BlockAirportCounter;
import com.micatechnologies.minecraft.csm.transit.airport.BlockBaggageCarousel;
import com.micatechnologies.minecraft.csm.transit.airport.BlockBoardingPassScanner;
import com.micatechnologies.minecraft.csm.transit.airport.BlockCheckinDesk;
import com.micatechnologies.minecraft.csm.transit.airport.BlockFlightBoard;
import com.micatechnologies.minecraft.csm.transit.airport.BlockGateSign;
import com.micatechnologies.minecraft.csm.transit.airport.BlockQueueStanchion;
import com.micatechnologies.minecraft.csm.transit.airport.BlockSecurityLine;
import com.micatechnologies.minecraft.csm.transit.airport.BlockSecurityTray;
import com.micatechnologies.minecraft.csm.transit.airport.BlockSelfCheckinKiosk;
import com.micatechnologies.minecraft.csm.transit.airport.ItemBoardingPass;
import com.micatechnologies.minecraft.csm.transit.fare.BlockFareGate;
import com.micatechnologies.minecraft.csm.transit.fare.BlockFareGateAda2;
import com.micatechnologies.minecraft.csm.transit.fare.BlockFareGateAda3;
import com.micatechnologies.minecraft.csm.transit.fare.BlockFareVendingMachine;
import com.micatechnologies.minecraft.csm.transit.fare.ItemFareTicket;
import com.micatechnologies.minecraft.csm.transit.fare.ItemTransitCard;
import com.micatechnologies.minecraft.csm.transit.platform.BlockPlatformBench;
import com.micatechnologies.minecraft.csm.transit.platform.BlockPlatformCanopy;
import com.micatechnologies.minecraft.csm.transit.platform.BlockPlatformClock;
import com.micatechnologies.minecraft.csm.transit.platform.BlockPlatformColumn;
import com.micatechnologies.minecraft.csm.transit.platform.BlockPlatformColumnNumber;
import com.micatechnologies.minecraft.csm.transit.platform.BlockPlatformFixture;
import com.micatechnologies.minecraft.csm.transit.platform.BlockPlatformHelpPoint;
import com.micatechnologies.minecraft.csm.transit.platform.BlockPlatformNumberSign;
import com.micatechnologies.minecraft.csm.transit.platform.BlockPlatformRun;
import com.micatechnologies.minecraft.csm.transit.platform.BlockPlatformValidator;
import com.micatechnologies.minecraft.csm.transit.platform.BlockStationNameSign;
import com.micatechnologies.minecraft.csm.transit.platform.BlockStationTile;
import com.micatechnologies.minecraft.csm.transit.platform.BlockTactilePaving;
import com.micatechnologies.minecraft.csm.transit.shelter.BlockBusShelter;
import com.micatechnologies.minecraft.csm.transit.shelter.BusShelterStyle;
import com.micatechnologies.minecraft.csm.trafficsigns.BlockTrafficSign;
import com.micatechnologies.minecraft.csm.transit.stop.BlockBusArrivalDisplay;
import com.micatechnologies.minecraft.csm.transit.stop.BlockBusStopFlag;
import com.micatechnologies.minecraft.csm.transit.stop.BlockBusStopPlaque;
import net.minecraft.block.Block;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;

/**
 * The tab for public transit: the fare gates, the fare vending machine and the tickets and cards
 * they take, the bus stops (agency flags, poster cases and the arrival display, all road signs on
 * Roads' sign posts, and the curb plaque), the bus shelters, and the station and platform fit-out
 * (tactile paving, platform furniture, station signs, tile, columns, the canopy and the ticket
 * validator), and the airport terminal pieces (check-in, the queue, security, the gate, the flight
 * information boards, baggage claim, luggage carts and wayfinding) with the boarding pass.
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

    // Bus stops (gen_transit_stops.py --fragments): road signs, stood on Roads' sign posts
    initTabBlock(new BlockBusStopFlag("bus_stop_flag_cityline"));
    initTabBlock(new BlockBusStopFlag("bus_stop_flag_riverway"));
    initTabBlock(new BlockBusStopFlag("bus_stop_flag_verdant"));
    initTabBlock(new BlockBusStopFlag("bus_stop_flag_emberline"));
    initTabBlock(new BlockTrafficSign("bus_stop_timetable_case"));
    initTabBlock(new BlockTrafficSign("bus_stop_route_map_case"));
    initTabBlock(new BlockBusArrivalDisplay("bus_stop_arrival_display"));
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

    // Station and platform fit-out (gen_transit_platforms.py --fragments), made to complement
    // RCMC's stations: tactile paving, furniture, signs, tile, columns, canopy, validator
    initTabBlock(new BlockTactilePaving("tactile_warning_yellow"));
    initTabBlock(new BlockTactilePaving("tactile_warning_grey"));
    initTabBlock(new BlockTactilePaving("tactile_guidance_yellow"));
    initTabBlock(new BlockTactilePaving("tactile_guidance_grey"));
    initTabBlock(new BlockPlatformBench("platform_bench", new double[]{0, 0, 3, 16, 15, 11}));
    initTabBlock(new BlockPlatformRun("platform_perch", new double[]{0, 0, 6, 16, 12, 12}));
    initTabBlock(new BlockPlatformHelpPoint("platform_help_point", new double[]{4, 0, 5, 12, 26, 11}, false));
    initTabBlock(new BlockPlatformHelpPoint("station_emergency_point", new double[]{3, 1, 12, 13, 14, 16}, true));
    initTabBlock(new BlockPlatformFixture("platform_cctv_dome", new double[]{5, 7, 5, 11, 16, 11}));
    initTabBlock(new BlockPlatformFixture("platform_cctv_camera", new double[]{6, 8, 0, 10, 14, 16}));
    initTabBlock(new BlockPlatformClock("platform_clock", new double[]{3, 3, 6, 13, 16, 10}));
    initTabBlock(new BlockPlatformFixture("platform_litter_bin", new double[]{4, 0, 4, 12, 14, 15}, true));
    initTabBlock(new BlockPlatformNumberSign("platform_number_sign", new double[]{2, 1, 7, 14, 16, 9}));
    initTabBlock(new BlockPlatformFixture("platform_sign_to_trains", new double[]{0, 5, 7, 16, 16, 9}));
    initTabBlock(new BlockPlatformFixture("platform_sign_exit", new double[]{0, 5, 7, 16, 16, 9}));
    initTabBlock(new BlockPlatformFixture("platform_sign_lines", new double[]{0, 5, 7, 16, 16, 9}));
    initTabBlock(new BlockPlatformFixture("platform_gap_sign", new double[]{1, 4, 15, 15, 12, 16}));
    initTabBlock(new BlockStationNameSign("station_name_sign", new double[]{0, 5, 15, 16, 12, 16}));
    initTabBlock(new BlockPlatformFixture("station_network_map", new double[]{0, 1, 14, 16, 15, 16}));
    initTabBlock(new BlockStationTile("station_tile_white"));
    initTabBlock(new BlockStationTile("station_tile_band_cityline"));
    initTabBlock(new BlockStationTile("station_tile_band_riverway"));
    initTabBlock(new BlockStationTile("station_tile_band_verdant"));
    initTabBlock(new BlockStationTile("station_tile_band_emberline"));
    initTabBlock(new BlockPlatformColumn("platform_column_tile", new double[]{2.5, 0, 2.5, 13.5, 16, 13.5}));
    initTabBlock(new BlockPlatformColumn("platform_column_steel", new double[]{4, 0, 4, 12, 16, 12}));
    initTabBlock(new BlockPlatformColumnNumber("platform_column_number", new double[]{2.5, 0, 2.5, 13.5, 16, 13.5}));
    initTabBlock(new BlockPlatformCanopy("platform_canopy"));
    initTabBlock(new BlockPlatformValidator("platform_validator", new double[]{5, 0, 5, 11, 15, 11}));

    // Airport terminal (gen_transit_airport.py --fragments): check-in, the queue, security, the
    // gate, the flight information boards, baggage claim, carts and wayfinding
    initTabBlock(new BlockCheckinDesk("airport_checkin_desk", new double[]{0, 0, 1, 16, 16, 15.6}));
    initTabBlock(new BlockAirportCounter("airport_checkin_scale", "checkin", new double[]{0.4, 0, 1, 15.6, 12.2, 16}));
    initTabBlock(new BlockAirportCounter("airport_gate_desk", "gate", new double[]{0, 0, 1, 16, 16, 13.2}));
    initTabBlock(new BlockSelfCheckinKiosk("airport_self_checkin_kiosk", new double[]{3, 0, 4, 13, 16, 13}));
    initTabBlock(new BlockQueueStanchion("airport_queue_stanchion_black"));
    initTabBlock(new BlockQueueStanchion("airport_queue_stanchion_blue"));
    initTabBlock(new BlockSecurityLine("airport_xray_scanner", new double[]{0, 0, 2, 16, 16, 14}, 22.0));
    initTabBlock(new BlockSecurityLine("airport_security_roller", new double[]{0, 0, 2.8, 16, 13.2, 13.2}, 0));
    initTabBlock(new BlockSecurityLine("airport_divest_table", new double[]{0, 0, 2.8, 16, 13.2, 13.2}, 0));
    initTabBlock(new BlockSecurityTray("airport_security_trays"));
    initTabBlock(new BlockSecurityTray("airport_security_tray_items"));
    initTabBlock(new BlockBoardingPassScanner("airport_boarding_pass_scanner", new double[]{4.5, 0, 4.5, 11.5, 14, 11}));
    initTabBlock(new BlockPlatformBench("airport_seating_black", new double[]{0, 0, 2.8, 16, 15, 11}));
    initTabBlock(new BlockPlatformBench("airport_seating_blue", new double[]{0, 0, 2.8, 16, 15, 11}));
    initTabBlock(new BlockFlightBoard("airport_flight_board_departures", new double[]{0, 3, 14.8, 16, 13, 16}, false));
    initTabBlock(new BlockFlightBoard("airport_flight_board_arrivals", new double[]{0, 3, 14.8, 16, 13, 16}, true));
    initTabBlock(new BlockBaggageCarousel("airport_baggage_carousel"));
    initTabBlock(new BlockGateSign("airport_gate_sign", new double[]{2, 3, 7, 14, 16, 9}));
    initTabBlock(new BlockPlatformFixture("airport_sign_gates", new double[]{0, 5, 7, 16, 16, 9}));
    initTabBlock(new BlockPlatformFixture("airport_sign_arrivals", new double[]{0, 5, 7, 16, 16, 9}));
    initTabBlock(new BlockPlatformFixture("airport_sign_check_in", new double[]{0, 5, 7, 16, 16, 9}));
    initTabBlock(new BlockPlatformFixture("airport_sign_baggage_claim", new double[]{0, 5, 7, 16, 16, 9}));
    initTabBlock(new BlockPlatformFixture("airport_sign_ground_transport", new double[]{0, 5, 7, 16, 16, 9}));
    initTabBlock(new BlockPlatformFixture("airport_luggage_cart", new double[]{3, 0, 1, 13, 15.4, 14.8}));
    initTabBlock(new BlockPlatformRun("airport_cart_rack", new double[]{0, 0, 2.4, 16, 15.4, 13.6}));
    initTabItem(new ItemBoardingPass());
  }
}

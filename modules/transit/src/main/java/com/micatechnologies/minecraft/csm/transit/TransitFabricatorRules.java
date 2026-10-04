package com.micatechnologies.minecraft.csm.transit;

import com.micatechnologies.minecraft.csm.materials.CsmFabricatorCosts;
import com.micatechnologies.minecraft.csm.materials.CsmParts;
import com.micatechnologies.minecraft.csm.materials.FabricatorIngredient;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.block.Block;

/**
 * What the Fabricator charges for the Transit tab.
 *
 * <p>The fare equipment is a powered cabinet with a reader, a board and its wiring. It costs what
 * it cost in the Technology tab it came from (a control board, sheet metal and a wiring harness),
 * so moving it changed no price. The bus stops are priced by what they are made of: a flag a sign
 * blank and its fixings, a poster case a sign blank in sheet metal, the arrival display an LED
 * panel with its board, the curb plaque a casting of sheet metal; the sign posts they stand on
 * are Roads', priced by Roads. The bus departure board and bay display are priced as the arrival
 * display is: an LED module, a control board and sheet metal. A shelter is a pole section's worth of posts, sheet metal for its
 * roof and frame and an LED module for its roof light; the glass shelter adds glass panes and a
 * second sheet, the cantilever's canopy a second sheet. The station fit-out: tactile paving and
 * wall tile a concrete mix; the help and emergency points a board, a sounder and sheet metal;
 * CCTV an optical sensor; the validator a board, an LED module and sheet metal; the clock a board;
 * the canopy sheet metal and an LED module; a column a pole section and a concrete mix; the
 * signs a sign blank and fixings; the bench, the perch and the bin sheet metal and fixings.
 * {@code audit_fabricator_costs.py} mirrors these branches.</p>
 *
 * @since 2026.9
 */
public final class TransitFabricatorRules {

  /** The Transit tab, priced by {@link #price}. */
  public static final String TAB_ID = "tabtransit";

  private static final String MC_GLASS_PANE = "minecraft:glass_pane";

  private TransitFabricatorRules() {
  }

  /**
   * Prices a block in the Transit tab.
   *
   * @param block        the block
   * @param registryName its registry name
   *
   * @return the cost, or null for the generic cost
   */
  @Nullable
  public static List<FabricatorIngredient> price(Block block, String registryName) {
    if (registryName.startsWith("bus_stop_flag_")) {
      return CsmFabricatorCosts.cost(FabricatorIngredient.part(CsmParts.SIGN_BLANK, 1),
          FabricatorIngredient.part(CsmParts.FASTENER_KIT, 1));
    }
    if (registryName.startsWith("bus_stop_") && registryName.endsWith("_case")) {
      return CsmFabricatorCosts.cost(FabricatorIngredient.part(CsmParts.SIGN_BLANK, 1),
          FabricatorIngredient.part(CsmParts.SHEET_METAL, 1));
    }
    if (registryName.equals("bus_stop_arrival_display")) {
      return CsmFabricatorCosts.cost(FabricatorIngredient.part(CsmParts.LED_MODULE, 1),
          FabricatorIngredient.part(CsmParts.CONTROL_BOARD, 1),
          FabricatorIngredient.part(CsmParts.SHEET_METAL, 1));
    }
    if (registryName.equals("bus_departure_board") || registryName.equals("bus_bay_display")) {
      return CsmFabricatorCosts.cost(FabricatorIngredient.part(CsmParts.LED_MODULE, 1),
          FabricatorIngredient.part(CsmParts.CONTROL_BOARD, 1),
          FabricatorIngredient.part(CsmParts.SHEET_METAL, 1));
    }
    if (registryName.startsWith("bus_shelter_glass_")) {
      return CsmFabricatorCosts.cost(FabricatorIngredient.part(CsmParts.POLE_SECTION, 1),
          FabricatorIngredient.part(CsmParts.SHEET_METAL, 2),
          FabricatorIngredient.any(MC_GLASS_PANE, 4),
          FabricatorIngredient.part(CsmParts.LED_MODULE, 1));
    }
    if (registryName.startsWith("bus_shelter_cantilever_")) {
      return CsmFabricatorCosts.cost(FabricatorIngredient.part(CsmParts.POLE_SECTION, 1),
          FabricatorIngredient.part(CsmParts.SHEET_METAL, 2),
          FabricatorIngredient.part(CsmParts.LED_MODULE, 1));
    }
    if (registryName.startsWith("bus_shelter_")) {
      return CsmFabricatorCosts.cost(FabricatorIngredient.part(CsmParts.POLE_SECTION, 1),
          FabricatorIngredient.part(CsmParts.SHEET_METAL, 1),
          FabricatorIngredient.part(CsmParts.LED_MODULE, 1));
    }
    if (registryName.equals("bus_stop_curb_plaque")) {
      return CsmFabricatorCosts.cost(FabricatorIngredient.part(CsmParts.SHEET_METAL, 1));
    }
    // The station and platform fit-out
    if (registryName.startsWith("tactile_") || registryName.startsWith("station_tile_")) {
      return CsmFabricatorCosts.cost(FabricatorIngredient.part(CsmParts.CONCRETE_MIX, 1));
    }
    if (registryName.equals("platform_help_point")
        || registryName.equals("station_emergency_point")) {
      return CsmFabricatorCosts.cost(FabricatorIngredient.part(CsmParts.CONTROL_BOARD, 1),
          FabricatorIngredient.part(CsmParts.SOUNDER_DRIVER, 1),
          FabricatorIngredient.part(CsmParts.SHEET_METAL, 1));
    }
    if (registryName.startsWith("platform_cctv_")) {
      return CsmFabricatorCosts.cost(FabricatorIngredient.part(CsmParts.OPTICAL_SENSOR, 1),
          FabricatorIngredient.part(CsmParts.SHEET_METAL, 1));
    }
    if (registryName.equals("platform_validator")) {
      return CsmFabricatorCosts.cost(FabricatorIngredient.part(CsmParts.CONTROL_BOARD, 1),
          FabricatorIngredient.part(CsmParts.LED_MODULE, 1),
          FabricatorIngredient.part(CsmParts.SHEET_METAL, 1));
    }
    if (registryName.equals("platform_clock")) {
      return CsmFabricatorCosts.cost(FabricatorIngredient.part(CsmParts.CONTROL_BOARD, 1),
          FabricatorIngredient.part(CsmParts.SHEET_METAL, 1));
    }
    if (registryName.equals("platform_canopy")) {
      return CsmFabricatorCosts.cost(FabricatorIngredient.part(CsmParts.SHEET_METAL, 2),
          FabricatorIngredient.part(CsmParts.LED_MODULE, 1));
    }
    if (registryName.startsWith("platform_column_")) {
      return CsmFabricatorCosts.cost(FabricatorIngredient.part(CsmParts.POLE_SECTION, 1),
          FabricatorIngredient.part(CsmParts.CONCRETE_MIX, 1));
    }
    if (registryName.startsWith("platform_sign_") || registryName.startsWith("station_name_")
        || registryName.equals("platform_number_sign") || registryName.equals("platform_gap_sign")
        || registryName.equals("station_network_map")) {
      return CsmFabricatorCosts.cost(FabricatorIngredient.part(CsmParts.SIGN_BLANK, 1),
          FabricatorIngredient.part(CsmParts.FASTENER_KIT, 1));
    }
    List<FabricatorIngredient> station = station(registryName);
    if (station != null) {
      return station;
    }
    if (registryName.startsWith("airport_")) {
      return airport(registryName);
    }
    if (registryName.startsWith("platform_")) {
      // the bench, the perch and the litter bin
      return CsmFabricatorCosts.cost(FabricatorIngredient.part(CsmParts.SHEET_METAL, 2),
          FabricatorIngredient.part(CsmParts.FASTENER_KIT, 1));
    }
    return CsmFabricatorCosts.cost(FabricatorIngredient.part(CsmParts.CONTROL_BOARD, 1),
        FabricatorIngredient.part(CsmParts.SHEET_METAL, 1),
        FabricatorIngredient.part(CsmParts.WIRING_HARNESS, 1));
  }

  /**
   * Prices a station piece: the kiosk's glass a sheet of steel and panes, its roof steel and a
   * light, the globe lamp a post, a lens and an LED, a railing steel and fittings (a sign blank
   * more with a plate), the service gate twice the steel, a line bullet a sign blank, the booth
   * counter steel, panes and fittings.
   *
   * @return the cost, or null if the block is not a station piece
   */
  @Nullable
  private static List<FabricatorIngredient> station(String registryName) {
    switch (registryName) {
      case "station_entrance_glass":
        return CsmFabricatorCosts.cost(FabricatorIngredient.part(CsmParts.SHEET_METAL, 1),
            FabricatorIngredient.any(MC_GLASS_PANE, 2));
      case "station_entrance_globe":
        return CsmFabricatorCosts.cost(FabricatorIngredient.part(CsmParts.POLE_SECTION, 1),
            FabricatorIngredient.part(CsmParts.LENS_ASSEMBLY, 1),
            FabricatorIngredient.part(CsmParts.LED_MODULE, 1));
      case "station_entrance_railing":
      case "station_fare_railing":
        return CsmFabricatorCosts.cost(FabricatorIngredient.part(CsmParts.SHEET_METAL, 1),
            FabricatorIngredient.part(CsmParts.FASTENER_KIT, 1));
      case "station_entrance_railing_sign":
      case "station_fare_railing_sign":
        return CsmFabricatorCosts.cost(FabricatorIngredient.part(CsmParts.SHEET_METAL, 1),
            FabricatorIngredient.part(CsmParts.FASTENER_KIT, 1),
            FabricatorIngredient.part(CsmParts.SIGN_BLANK, 1));
      case "station_service_gate":
        return CsmFabricatorCosts.cost(FabricatorIngredient.part(CsmParts.SHEET_METAL, 2),
            FabricatorIngredient.part(CsmParts.FASTENER_KIT, 1));
      case "station_line_bullet":
        return CsmFabricatorCosts.cost(FabricatorIngredient.part(CsmParts.SIGN_BLANK, 1));
      case "station_booth_counter":
        return CsmFabricatorCosts.cost(FabricatorIngredient.part(CsmParts.SHEET_METAL, 2),
            FabricatorIngredient.any(MC_GLASS_PANE, 2),
            FabricatorIngredient.part(CsmParts.FASTENER_KIT, 1));
      default:
        break;
    }
    if (registryName.startsWith("station_entrance_roof_")) {
      return CsmFabricatorCosts.cost(FabricatorIngredient.part(CsmParts.SHEET_METAL, 2),
          FabricatorIngredient.part(CsmParts.LED_MODULE, 1));
    }
    return null;
  }

  /**
   * Prices an airport terminal piece by what it is made of: electronics where it has a screen,
   * a reader or a sensor, sheet metal and fittings for the furniture, a sign blank for a sign.
   */
  private static List<FabricatorIngredient> airport(String registryName) {
    List<FabricatorIngredient> airside = airside(registryName);
    if (airside != null) {
      return airside;
    }
    switch (registryName) {
      case "airport_checkin_desk":
      case "airport_gate_desk":
        return CsmFabricatorCosts.cost(FabricatorIngredient.part(CsmParts.SHEET_METAL, 2),
            FabricatorIngredient.part(CsmParts.CONTROL_BOARD, 1),
            FabricatorIngredient.part(CsmParts.FASTENER_KIT, 1));
      case "airport_checkin_scale":
        return CsmFabricatorCosts.cost(FabricatorIngredient.part(CsmParts.SHEET_METAL, 1),
            FabricatorIngredient.part(CsmParts.CONTROL_BOARD, 1),
            FabricatorIngredient.part(CsmParts.WIRING_HARNESS, 1));
      case "airport_self_checkin_kiosk":
        return CsmFabricatorCosts.cost(FabricatorIngredient.part(CsmParts.CONTROL_BOARD, 1),
            FabricatorIngredient.part(CsmParts.LED_MODULE, 1),
            FabricatorIngredient.part(CsmParts.SHEET_METAL, 1));
      case "airport_self_checkin_kiosk_large":
        // the same kiosk, a body twice the height
        return CsmFabricatorCosts.cost(FabricatorIngredient.part(CsmParts.CONTROL_BOARD, 1),
            FabricatorIngredient.part(CsmParts.LED_MODULE, 1),
            FabricatorIngredient.part(CsmParts.SHEET_METAL, 2));
      case "airport_xray_scanner":
        return CsmFabricatorCosts.cost(FabricatorIngredient.part(CsmParts.ENCLOSURE_SHELL, 1),
            FabricatorIngredient.part(CsmParts.CONTROL_BOARD, 1),
            FabricatorIngredient.part(CsmParts.OPTICAL_SENSOR, 1),
            FabricatorIngredient.part(CsmParts.WIRING_HARNESS, 1));
      case "airport_boarding_pass_scanner":
        return CsmFabricatorCosts.cost(FabricatorIngredient.part(CsmParts.CONTROL_BOARD, 1),
            FabricatorIngredient.part(CsmParts.OPTICAL_SENSOR, 1),
            FabricatorIngredient.part(CsmParts.SHEET_METAL, 1));
      case "airport_flight_board_departures":
      case "airport_flight_board_arrivals":
        return CsmFabricatorCosts.cost(FabricatorIngredient.part(CsmParts.LED_MODULE, 1),
            FabricatorIngredient.part(CsmParts.CONTROL_BOARD, 1),
            FabricatorIngredient.part(CsmParts.SHEET_METAL, 1));
      case "airport_flight_board_large_departures":
      case "airport_flight_board_large_arrivals":
        // a cell of a large screen: its share of the panel and the bezel round it
        return CsmFabricatorCosts.cost(FabricatorIngredient.part(CsmParts.LED_MODULE, 1),
            FabricatorIngredient.part(CsmParts.SHEET_METAL, 1));
      case "airport_wayfinding_panel":
        // a backlit sign cell: its face and the lamp behind it
        return CsmFabricatorCosts.cost(FabricatorIngredient.part(CsmParts.SIGN_BLANK, 1),
            FabricatorIngredient.part(CsmParts.LED_MODULE, 1));
      case "airport_baggage_carousel":
        return CsmFabricatorCosts.cost(FabricatorIngredient.part(CsmParts.SHEET_METAL, 2),
            FabricatorIngredient.part(CsmParts.WIRING_HARNESS, 1),
            FabricatorIngredient.part(CsmParts.FASTENER_KIT, 1));
      case "airport_queue_stanchion_black":
      case "airport_queue_stanchion_blue":
        return CsmFabricatorCosts.cost(FabricatorIngredient.part(CsmParts.POLE_SECTION, 1),
            FabricatorIngredient.part(CsmParts.FASTENER_KIT, 1));
      case "airport_security_trays":
      case "airport_security_tray_items":
      case "airport_luggage_cart":
        return CsmFabricatorCosts.cost(FabricatorIngredient.part(CsmParts.SHEET_METAL, 1),
            FabricatorIngredient.part(CsmParts.FASTENER_KIT, 1));
      default:
        if (registryName.startsWith("airport_sign_") || registryName.equals("airport_gate_sign")) {
          return CsmFabricatorCosts.cost(FabricatorIngredient.part(CsmParts.SIGN_BLANK, 1),
              FabricatorIngredient.part(CsmParts.FASTENER_KIT, 1));
        }
        // the security rollers and divesting table, the seating and the cart rack
        return CsmFabricatorCosts.cost(FabricatorIngredient.part(CsmParts.SHEET_METAL, 2),
            FabricatorIngredient.part(CsmParts.FASTENER_KIT, 1));
    }
  }

  /**
   * Prices an airside piece: a lens and an LED for each light, a sign blank and an LED for a lit
   * sign, pole sections for the masts and legs, and sheet metal (with a control board where it
   * has a motor or a panel) for the ground equipment and the jet bridge. Null for a terminal
   * piece.
   */
  private static List<FabricatorIngredient> airside(String registryName) {
    switch (registryName) {
      case "airport_runway_edge_light":
      case "airport_taxiway_edge_light":
      case "airport_runway_threshold_light":
      case "airport_runway_centreline_light":
      case "airport_taxiway_centreline_light":
      case "airport_stop_bar_light":
        return CsmFabricatorCosts.cost(FabricatorIngredient.part(CsmParts.LENS_ASSEMBLY, 1),
            FabricatorIngredient.part(CsmParts.LED_MODULE, 1),
            FabricatorIngredient.part(CsmParts.FASTENER_KIT, 1));
      case "airport_approach_light_bar":
        return CsmFabricatorCosts.cost(FabricatorIngredient.part(CsmParts.LENS_ASSEMBLY, 2),
            FabricatorIngredient.part(CsmParts.LED_MODULE, 2),
            FabricatorIngredient.part(CsmParts.SHEET_METAL, 1));
      case "airport_beacon":
        return CsmFabricatorCosts.cost(FabricatorIngredient.part(CsmParts.LENS_ASSEMBLY, 2),
            FabricatorIngredient.part(CsmParts.LED_MODULE, 1),
            FabricatorIngredient.part(CsmParts.CONTROL_BOARD, 1));
      case "airport_wind_sock":
        return CsmFabricatorCosts.cost(FabricatorIngredient.part(CsmParts.POLE_SECTION, 1),
            FabricatorIngredient.part(CsmParts.LENS_ASSEMBLY, 1),
            FabricatorIngredient.part(CsmParts.FASTENER_KIT, 1));
      case "airport_antenna_mast":
        return CsmFabricatorCosts.cost(FabricatorIngredient.part(CsmParts.POLE_SECTION, 1),
            FabricatorIngredient.part(CsmParts.WIRING_HARNESS, 1),
            FabricatorIngredient.part(CsmParts.LENS_ASSEMBLY, 1));
      case "airport_taxiway_location_sign":
      case "airport_taxiway_direction_sign":
      case "airport_runway_holding_sign":
      case "airport_runway_distance_sign":
        return CsmFabricatorCosts.cost(FabricatorIngredient.part(CsmParts.SIGN_BLANK, 1),
            FabricatorIngredient.part(CsmParts.LED_MODULE, 1),
            FabricatorIngredient.part(CsmParts.FASTENER_KIT, 1));
      case "airport_stand_sign":
        return CsmFabricatorCosts.cost(FabricatorIngredient.part(CsmParts.SIGN_BLANK, 1),
            FabricatorIngredient.part(CsmParts.FASTENER_KIT, 1));
      case "airport_airfield_mast":
        return CsmFabricatorCosts.cost(FabricatorIngredient.part(CsmParts.POLE_SECTION, 1),
            FabricatorIngredient.part(CsmParts.FASTENER_KIT, 1));
      case "airport_wheel_chocks":
        return CsmFabricatorCosts.cost(FabricatorIngredient.part(CsmParts.SHEET_METAL, 1));
      case "airport_ground_power_unit":
        return CsmFabricatorCosts.cost(FabricatorIngredient.part(CsmParts.ENCLOSURE_SHELL, 1),
            FabricatorIngredient.part(CsmParts.CONTROL_BOARD, 1),
            FabricatorIngredient.part(CsmParts.WIRING_HARNESS, 2));
      case "airport_baggage_tug":
        return CsmFabricatorCosts.cost(FabricatorIngredient.part(CsmParts.SHEET_METAL, 3),
            FabricatorIngredient.part(CsmParts.CONTROL_BOARD, 1),
            FabricatorIngredient.part(CsmParts.WIRING_HARNESS, 1));
      case "airport_baggage_cart":
        return CsmFabricatorCosts.cost(FabricatorIngredient.part(CsmParts.SHEET_METAL, 2),
            FabricatorIngredient.part(CsmParts.FASTENER_KIT, 1));
      case "airport_air_stairs":
      case "airport_jet_bridge_tunnel":
        return CsmFabricatorCosts.cost(FabricatorIngredient.part(CsmParts.SHEET_METAL, 3),
            FabricatorIngredient.part(CsmParts.FASTENER_KIT, 1));
      case "airport_jet_bridge_cab":
        return CsmFabricatorCosts.cost(FabricatorIngredient.part(CsmParts.SHEET_METAL, 3),
            FabricatorIngredient.part(CsmParts.CONTROL_BOARD, 1),
            FabricatorIngredient.part(CsmParts.FASTENER_KIT, 1));
      case "airport_jet_bridge_rotunda":
        return CsmFabricatorCosts.cost(FabricatorIngredient.part(CsmParts.SHEET_METAL, 4),
            FabricatorIngredient.part(CsmParts.FASTENER_KIT, 2));
      case "airport_jet_bridge_drive":
        return CsmFabricatorCosts.cost(FabricatorIngredient.part(CsmParts.POLE_SECTION, 2),
            FabricatorIngredient.part(CsmParts.WIRING_HARNESS, 1));
      case "airport_jet_bridge_column":
        return CsmFabricatorCosts.cost(FabricatorIngredient.part(CsmParts.CONCRETE_MIX, 2));
      default:
        return null;
    }
  }
}

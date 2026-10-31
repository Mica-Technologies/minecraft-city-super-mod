package com.micatechnologies.minecraft.csm.venues;

import com.micatechnologies.minecraft.csm.materials.CsmFabricatorCosts;
import com.micatechnologies.minecraft.csm.materials.CsmParts;
import com.micatechnologies.minecraft.csm.materials.FabricatorIngredient;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.block.Block;

/**
 * What the Fabricator charges for the Sports & Venues tabs, by what a piece is made of: a moulded
 * seat on a cast standard is sheet metal and fittings; a light is an LED and its housing; a glass
 * guard is glass on a metal shoe; a sign is a sign blank; a painted floor marking is dye. An arena
 * holds thousands of seats, so a seat stays cheap. On the event floor: ice is ice and a court is
 * planks (a painted cell or a line adds dye), a board is sheet metal, its glass is glass, and the
 * machines (the stanchion, the resurfacer, the ice plant) are priced as the machines they are.
 * Over the floor: a scoreboard is enclosures and LED modules, a ribbon board an LED module on
 * sheet metal, a banner wool and dye, the speakers drivers, the camera a lens and a sensor.
 * On the concourse: counters are sheet metal (a suite's bar and rail wood), a concession machine
 * sheet metal and wiring behind its glass, a menu board an enclosure of LED modules, the
 * turnstile and scanner sheet metal round a control board (the scanner a sensor), the team store's
 * garments wool. Outside: a marquee cell is an LED module on sheet metal (its controller adds a
 * control board), the pylon stone, the blades sign blanks, the box office window glass on
 * sheet metal; a bleacher is sheet metal, the press box sheet metal and glass, the ticket booth
 * enclosures and glass, a light tower's shaft iron, its head LED modules. At the ballpark: turf
 * is dirt and seed, clay is clay, an edge both, a chalk line or a painted marking adds dye; home
 * plate and the rubber are quartz, a base wool; the dugout's pieces sheet metal (the bat rack
 * planks, the cooler a bucket); a net is string; the outfield wall concrete under wool padding
 * (its marker adds dye); the foul pole iron; the scoreboards enclosures and LED modules. On a
 * football or soccer field: turf is dirt and seed, a painted cell, an anchor or the end zone adds
 * dye; the goal posts and goals iron (a soccer goal's net string); a pylon or a corner flag wool;
 * the benches, the heater, the fan, the shelter (with its glass) sheet metal; the chain iron bars;
 * the boards enclosures and LED modules.
 * {@code audit_fabricator_costs.py} mirrors these branches.
 *
 * @since 2026.10
 */
public final class VenuesFabricatorRules {

  /** The Seating tab. */
  public static final String TAB_SEATING = "tabvenuesseating";
  /** The Arena tab. */
  public static final String TAB_ARENA = "tabvenuesarena";
  /** The Concourse tab. */
  public static final String TAB_CONCOURSE = "tabvenuesconcourse";
  /** The Ballpark tab. */
  public static final String TAB_BALLPARK = "tabvenuesballpark";
  /** The Football & Soccer tab. */
  public static final String TAB_FOOTBALL = "tabvenuesfootball";

  private VenuesFabricatorRules() {
  }

  /**
   * Prices a block in a Sports & Venues tab.
   *
   * @param block        the block
   * @param registryName its registry name
   *
   * @return the cost, or null for the generic cost
   */
  @Nullable
  public static List<FabricatorIngredient> price(Block block, String registryName) {
    List<FabricatorIngredient> arena = arena(registryName);
    if (arena != null) {
      return arena;
    }
    List<FabricatorIngredient> concourse = concourse(registryName);
    if (concourse != null) {
      return concourse;
    }
    List<FabricatorIngredient> outside = exterior(registryName);
    if (outside != null) {
      return outside;
    }
    List<FabricatorIngredient> park = ballpark(registryName);
    if (park != null) {
      return park;
    }
    List<FabricatorIngredient> field = football(registryName);
    if (field != null) {
      return field;
    }
    if (registryName.endsWith("_sign")) {
      return CsmFabricatorCosts.cost(FabricatorIngredient.part(CsmParts.SIGN_BLANK, 1));
    }
    if (registryName.endsWith("_light")) {
      return CsmFabricatorCosts.cost(FabricatorIngredient.part(CsmParts.LED_MODULE, 1),
          FabricatorIngredient.part(CsmParts.SHEET_METAL, 1));
    }
    if (registryName.startsWith("stadium_glass_guard")) {
      return CsmFabricatorCosts.cost(FabricatorIngredient.any("minecraft:glass_pane", 2),
          FabricatorIngredient.part(CsmParts.SHEET_METAL, 1));
    }
    if (registryName.equals("stadium_wheelchair_space")) {
      return CsmFabricatorCosts.cost(FabricatorIngredient.any("minecraft:dye", 1));
    }
    if (registryName.startsWith("stadium_seat") || registryName.endsWith("_seat")) {
      return CsmFabricatorCosts.cost(FabricatorIngredient.part(CsmParts.SHEET_METAL, 1));
    }
    return CsmFabricatorCosts.cost(FabricatorIngredient.part(CsmParts.SHEET_METAL, 1),
        FabricatorIngredient.part(CsmParts.FASTENER_KIT, 1));
  }

  /** The Arena tab's pieces, or null for a registry name that is not one of them. */
  @Nullable
  private static List<FabricatorIngredient> arena(String registryName) {
    switch (registryName) {
      case "rink_ice":
        return CsmFabricatorCosts.cost(FabricatorIngredient.any("minecraft:ice", 1));
      case "rink_ice_line":
      case "rink_ice_spot":
      case "rink_ice_painted":
        return CsmFabricatorCosts.cost(FabricatorIngredient.any("minecraft:ice", 1),
            FabricatorIngredient.any("minecraft:dye", 1));
      case "rink_rubber_floor":
        return CsmFabricatorCosts.cost(FabricatorIngredient.any("minecraft:carpet", 1));
      case "court_maple":
        return CsmFabricatorCosts.cost(FabricatorIngredient.any("minecraft:planks", 1));
      case "court_line":
      case "court_floor_anchor":
      case "court_painted":
        return CsmFabricatorCosts.cost(FabricatorIngredient.any("minecraft:planks", 1),
            FabricatorIngredient.any("minecraft:dye", 1));
      case "hockey_goal":
        return CsmFabricatorCosts.cost(FabricatorIngredient.any("minecraft:iron_ingot", 2),
            FabricatorIngredient.any("minecraft:string", 2));
      case "basketball_stanchion":
        return CsmFabricatorCosts.cost(FabricatorIngredient.part(CsmParts.SHEET_METAL, 4),
            FabricatorIngredient.any("minecraft:glass_pane", 2),
            FabricatorIngredient.part(CsmParts.LED_MODULE, 1),
            FabricatorIngredient.part(CsmParts.CONTROL_BOARD, 1));
      case "ice_resurfacer":
        return CsmFabricatorCosts.cost(FabricatorIngredient.part(CsmParts.SHEET_METAL, 6),
            FabricatorIngredient.part(CsmParts.WIRING_HARNESS, 2),
            FabricatorIngredient.part(CsmParts.CONTROL_BOARD, 1));
      case "ice_plant":
        return CsmFabricatorCosts.cost(FabricatorIngredient.part(CsmParts.ENCLOSURE_SHELL, 2),
            FabricatorIngredient.part(CsmParts.DUCTING, 1),
            FabricatorIngredient.part(CsmParts.CONTROL_BOARD, 1));
      case "scorers_table":
        return CsmFabricatorCosts.cost(FabricatorIngredient.part(CsmParts.SHEET_METAL, 2),
            FabricatorIngredient.part(CsmParts.LED_MODULE, 2),
            FabricatorIngredient.part(CsmParts.CONTROL_BOARD, 1));
      case "player_bench":
      case "penalty_bench":
        return CsmFabricatorCosts.cost(FabricatorIngredient.part(CsmParts.SHEET_METAL, 1),
            FabricatorIngredient.any("minecraft:wool", 1));
      case "centre_hung_scoreboard":
        return CsmFabricatorCosts.cost(FabricatorIngredient.part(CsmParts.ENCLOSURE_SHELL, 6),
            FabricatorIngredient.part(CsmParts.LED_MODULE, 8),
            FabricatorIngredient.part(CsmParts.CONTROL_BOARD, 2),
            FabricatorIngredient.part(CsmParts.WIRING_HARNESS, 2));
      case "centre_hung_scoreboard_small":
        return CsmFabricatorCosts.cost(FabricatorIngredient.part(CsmParts.ENCLOSURE_SHELL, 3),
            FabricatorIngredient.part(CsmParts.LED_MODULE, 4),
            FabricatorIngredient.part(CsmParts.CONTROL_BOARD, 1),
            FabricatorIngredient.part(CsmParts.WIRING_HARNESS, 1));
      case "led_ribbon_board":
      case "led_ribbon_board_corner":
        return CsmFabricatorCosts.cost(FabricatorIngredient.part(CsmParts.LED_MODULE, 1),
            FabricatorIngredient.part(CsmParts.SHEET_METAL, 1));
      case "led_ribbon_board_text":
        return CsmFabricatorCosts.cost(FabricatorIngredient.part(CsmParts.LED_MODULE, 1),
            FabricatorIngredient.part(CsmParts.SHEET_METAL, 1),
            FabricatorIngredient.part(CsmParts.CONTROL_BOARD, 1));
      case "championship_banner":
      case "retired_number_banner":
        return CsmFabricatorCosts.cost(FabricatorIngredient.any("minecraft:wool", 3),
            FabricatorIngredient.any("minecraft:dye", 1));
      case "championship_banner_small":
      case "retired_number_banner_small":
        return CsmFabricatorCosts.cost(FabricatorIngredient.any("minecraft:wool", 1),
            FabricatorIngredient.any("minecraft:dye", 1));
      case "line_array_speaker":
        return CsmFabricatorCosts.cost(FabricatorIngredient.part(CsmParts.ENCLOSURE_SHELL, 2),
            FabricatorIngredient.part(CsmParts.SOUNDER_DRIVER, 2),
            FabricatorIngredient.part(CsmParts.WIRING_HARNESS, 1));
      case "broadcast_camera_platform":
        return CsmFabricatorCosts.cost(FabricatorIngredient.part(CsmParts.SHEET_METAL, 2),
            FabricatorIngredient.part(CsmParts.LENS_ASSEMBLY, 1),
            FabricatorIngredient.part(CsmParts.OPTICAL_SENSOR, 1));
      case "venue_catwalk":
        return CsmFabricatorCosts.cost(FabricatorIngredient.part(CsmParts.SHEET_METAL, 1),
            FabricatorIngredient.any("minecraft:iron_bars", 1));
      default:
        break;
    }
    if (registryName.startsWith("dasher_glass")) {
      return CsmFabricatorCosts.cost(FabricatorIngredient.any("minecraft:glass_pane", 2),
          FabricatorIngredient.part(CsmParts.SHEET_METAL, 1));
    }
    if (registryName.equals("dasher_penalty_door")) {
      return CsmFabricatorCosts.cost(FabricatorIngredient.part(CsmParts.SHEET_METAL, 1),
          FabricatorIngredient.any("minecraft:glass_pane", 2),
          FabricatorIngredient.part(CsmParts.FASTENER_KIT, 1));
    }
    if (registryName.startsWith("dasher_")) {
      return CsmFabricatorCosts.cost(FabricatorIngredient.part(CsmParts.SHEET_METAL, 1),
          FabricatorIngredient.part(CsmParts.FASTENER_KIT, 1));
    }
    return null;
  }

  /** The Concourse tab's pieces, or null for a registry name that is not one of them. */
  @Nullable
  private static List<FabricatorIngredient> concourse(String registryName) {
    switch (registryName) {
      case "concession_counter":
      case "concession_counter_corner":
      case "concession_back_counter":
        return CsmFabricatorCosts.cost(FabricatorIngredient.part(CsmParts.SHEET_METAL, 2),
            FabricatorIngredient.part(CsmParts.FASTENER_KIT, 1));
      case "concession_sign":
        return CsmFabricatorCosts.cost(FabricatorIngredient.part(CsmParts.SIGN_BLANK, 1),
            FabricatorIngredient.part(CsmParts.LED_MODULE, 1));
      case "concession_menu_board":
        return CsmFabricatorCosts.cost(FabricatorIngredient.part(CsmParts.ENCLOSURE_SHELL, 1),
            FabricatorIngredient.part(CsmParts.LED_MODULE, 2),
            FabricatorIngredient.part(CsmParts.CONTROL_BOARD, 1));
      case "hot_dog_roller_grill":
      case "popcorn_machine":
      case "pretzel_warmer":
      case "nacho_cheese_dispenser":
        return CsmFabricatorCosts.cost(FabricatorIngredient.part(CsmParts.SHEET_METAL, 2),
            FabricatorIngredient.part(CsmParts.WIRING_HARNESS, 1),
            FabricatorIngredient.any("minecraft:glass_pane", 1));
      case "soft_serve_machine":
        return CsmFabricatorCosts.cost(FabricatorIngredient.part(CsmParts.SHEET_METAL, 2),
            FabricatorIngredient.part(CsmParts.WIRING_HARNESS, 1),
            FabricatorIngredient.part(CsmParts.CONTROL_BOARD, 1));
      case "beer_kiosk":
        return CsmFabricatorCosts.cost(FabricatorIngredient.part(CsmParts.SHEET_METAL, 3),
            FabricatorIngredient.part(CsmParts.LED_MODULE, 1),
            FabricatorIngredient.part(CsmParts.FASTENER_KIT, 1));
      case "stadium_turnstile":
        return CsmFabricatorCosts.cost(FabricatorIngredient.part(CsmParts.SHEET_METAL, 2),
            FabricatorIngredient.part(CsmParts.CONTROL_BOARD, 1),
            FabricatorIngredient.part(CsmParts.FASTENER_KIT, 1));
      case "stadium_ticket_scanner":
        return CsmFabricatorCosts.cost(FabricatorIngredient.part(CsmParts.OPTICAL_SENSOR, 1),
            FabricatorIngredient.part(CsmParts.CONTROL_BOARD, 1),
            FabricatorIngredient.part(CsmParts.SHEET_METAL, 1));
      case "suite_bar_counter":
        return CsmFabricatorCosts.cost(FabricatorIngredient.any("minecraft:planks", 2),
            FabricatorIngredient.part(CsmParts.FASTENER_KIT, 1));
      case "suite_drink_rail":
        return CsmFabricatorCosts.cost(FabricatorIngredient.any("minecraft:planks", 1));
      case "jersey_wall_rack":
      case "jersey_mannequin":
        return CsmFabricatorCosts.cost(FabricatorIngredient.any("minecraft:wool", 2),
            FabricatorIngredient.part(CsmParts.SHEET_METAL, 1));
      case "hat_wall":
      case "team_apparel_table":
        return CsmFabricatorCosts.cost(FabricatorIngredient.any("minecraft:wool", 2),
            FabricatorIngredient.any("minecraft:planks", 1));
      default:
        return null;
    }
  }

  /** The Ballpark tab's pieces, or null for a registry name that is not one of them. */
  @Nullable
  private static List<FabricatorIngredient> ballpark(String registryName) {
    switch (registryName) {
      case "ballpark_turf":
        return CsmFabricatorCosts.cost(FabricatorIngredient.any("minecraft:dirt", 1),
            FabricatorIngredient.any("minecraft:wheat_seeds", 1));
      case "ballpark_clay":
      case "pitchers_mound":
      case "pitchers_mound_slope":
        return CsmFabricatorCosts.cost(FabricatorIngredient.any("minecraft:clay_ball", 2));
      case "ballpark_field_edge":
      case "ballpark_track_edge":
        return CsmFabricatorCosts.cost(FabricatorIngredient.any("minecraft:dirt", 1),
            FabricatorIngredient.any("minecraft:clay_ball", 1));
      case "ballpark_chalk_turf":
        return CsmFabricatorCosts.cost(FabricatorIngredient.any("minecraft:dirt", 1),
            FabricatorIngredient.any("minecraft:dye", 1));
      case "ballpark_chalk_clay":
      case "ballpark_painted":
        return CsmFabricatorCosts.cost(FabricatorIngredient.any("minecraft:clay_ball", 1),
            FabricatorIngredient.any("minecraft:dye", 1));
      case "home_plate":
      case "pitchers_rubber":
        return CsmFabricatorCosts.cost(FabricatorIngredient.any("minecraft:quartz", 1));
      case "baseball_base":
      case "softball_base":
        return CsmFabricatorCosts.cost(FabricatorIngredient.any("minecraft:wool", 1));
      case "dugout_bench":
      case "dugout_canopy":
        return CsmFabricatorCosts.cost(FabricatorIngredient.part(CsmParts.SHEET_METAL, 2));
      case "dugout_rail":
        return CsmFabricatorCosts.cost(FabricatorIngredient.part(CsmParts.SHEET_METAL, 1),
            FabricatorIngredient.any("minecraft:wool", 1));
      case "dugout_helmet_rack":
        return CsmFabricatorCosts.cost(FabricatorIngredient.part(CsmParts.SHEET_METAL, 1),
            FabricatorIngredient.any("minecraft:dye", 1));
      case "dugout_bat_rack":
        return CsmFabricatorCosts.cost(FabricatorIngredient.any("minecraft:planks", 2));
      case "dugout_water_cooler":
        return CsmFabricatorCosts.cost(FabricatorIngredient.part(CsmParts.SHEET_METAL, 1),
            FabricatorIngredient.any("minecraft:bucket", 1));
      case "ballpark_net":
      case "ballpark_net_diagonal":
      case "ballpark_net_roof":
        return CsmFabricatorCosts.cost(FabricatorIngredient.any("minecraft:string", 2));
      case "outfield_wall":
      case "outfield_wall_diagonal":
        return CsmFabricatorCosts.cost(FabricatorIngredient.part(CsmParts.CONCRETE_MIX, 1),
            FabricatorIngredient.any("minecraft:wool", 1));
      case "outfield_wall_marker":
        return CsmFabricatorCosts.cost(FabricatorIngredient.part(CsmParts.CONCRETE_MIX, 1),
            FabricatorIngredient.any("minecraft:wool", 1),
            FabricatorIngredient.any("minecraft:dye", 1));
      case "foul_pole":
        return CsmFabricatorCosts.cost(FabricatorIngredient.any("minecraft:iron_ingot", 2),
            FabricatorIngredient.any("minecraft:dye", 1));
      case "ballpark_scoreboard":
        return CsmFabricatorCosts.cost(FabricatorIngredient.part(CsmParts.ENCLOSURE_SHELL, 4),
            FabricatorIngredient.part(CsmParts.LED_MODULE, 4),
            FabricatorIngredient.part(CsmParts.CONTROL_BOARD, 1));
      case "ballpark_count_board":
        return CsmFabricatorCosts.cost(FabricatorIngredient.part(CsmParts.ENCLOSURE_SHELL, 1),
            FabricatorIngredient.part(CsmParts.LED_MODULE, 2),
            FabricatorIngredient.part(CsmParts.CONTROL_BOARD, 1));
      case "pitching_machine":
        return CsmFabricatorCosts.cost(FabricatorIngredient.part(CsmParts.SHEET_METAL, 2),
            FabricatorIngredient.part(CsmParts.WIRING_HARNESS, 1));
      default:
        break;
    }
    if (registryName.endsWith("_anchor") || registryName.endsWith("_anchor_third")) {
      return CsmFabricatorCosts.cost(FabricatorIngredient.any("minecraft:clay_ball", 1),
          FabricatorIngredient.any("minecraft:dye", 1));
    }
    return null;
  }

  /** The Football & Soccer tab's pieces, or null for a registry name that is not one of them. */
  @Nullable
  private static List<FabricatorIngredient> football(String registryName) {
    switch (registryName) {
      case "field_turf":
        return CsmFabricatorCosts.cost(FabricatorIngredient.any("minecraft:dirt", 1),
            FabricatorIngredient.any("minecraft:wheat_seeds", 1));
      case "football_end_zone":
      case "football_marking_anchor":
      case "football_marking_anchor_2":
      case "football_painted":
      case "soccer_marking_anchor":
      case "soccer_marking_anchor_2":
      case "soccer_painted":
        return CsmFabricatorCosts.cost(FabricatorIngredient.any("minecraft:dirt", 1),
            FabricatorIngredient.any("minecraft:dye", 1));
      case "football_end_zone_lettering":
        return CsmFabricatorCosts.cost(FabricatorIngredient.any("minecraft:dye", 2));
      case "football_goal_post":
        return CsmFabricatorCosts.cost(FabricatorIngredient.any("minecraft:iron_ingot", 4),
            FabricatorIngredient.any("minecraft:dye", 1));
      case "soccer_goal":
        return CsmFabricatorCosts.cost(FabricatorIngredient.any("minecraft:iron_ingot", 3),
            FabricatorIngredient.any("minecraft:string", 4));
      case "soccer_goal_small":
        return CsmFabricatorCosts.cost(FabricatorIngredient.any("minecraft:iron_ingot", 1),
            FabricatorIngredient.any("minecraft:string", 2));
      case "end_zone_pylon":
      case "soccer_corner_flag":
        return CsmFabricatorCosts.cost(FabricatorIngredient.any("minecraft:wool", 1));
      case "football_down_box":
      case "football_chain_stake":
        return CsmFabricatorCosts.cost(FabricatorIngredient.part(CsmParts.SHEET_METAL, 1),
            FabricatorIngredient.any("minecraft:dye", 1));
      case "football_chain":
        return CsmFabricatorCosts.cost(FabricatorIngredient.any("minecraft:iron_bars", 1));
      case "football_team_bench":
        return CsmFabricatorCosts.cost(FabricatorIngredient.part(CsmParts.SHEET_METAL, 2));
      case "sideline_heater":
        return CsmFabricatorCosts.cost(FabricatorIngredient.part(CsmParts.SHEET_METAL, 2),
            FabricatorIngredient.part(CsmParts.WIRING_HARNESS, 1));
      case "sideline_fan":
        return CsmFabricatorCosts.cost(FabricatorIngredient.part(CsmParts.SHEET_METAL, 1),
            FabricatorIngredient.part(CsmParts.WIRING_HARNESS, 1));
      case "soccer_substitutes_shelter":
        return CsmFabricatorCosts.cost(FabricatorIngredient.part(CsmParts.SHEET_METAL, 2),
            FabricatorIngredient.any("minecraft:glass_pane", 2));
      case "fourth_official_board":
        return CsmFabricatorCosts.cost(FabricatorIngredient.part(CsmParts.LED_MODULE, 1),
            FabricatorIngredient.part(CsmParts.SHEET_METAL, 1));
      case "football_scoreboard":
      case "soccer_scoreboard":
        return CsmFabricatorCosts.cost(FabricatorIngredient.part(CsmParts.ENCLOSURE_SHELL, 4),
            FabricatorIngredient.part(CsmParts.LED_MODULE, 4),
            FabricatorIngredient.part(CsmParts.CONTROL_BOARD, 1));
      default:
        return null;
    }
  }

  /** The exterior and the shared outdoor pieces, or null for a name that is not one of them. */
  @Nullable
  private static List<FabricatorIngredient> exterior(String registryName) {
    switch (registryName) {
      case "marquee_board":
        return CsmFabricatorCosts.cost(FabricatorIngredient.part(CsmParts.LED_MODULE, 1),
            FabricatorIngredient.part(CsmParts.SHEET_METAL, 1));
      case "marquee_board_controller":
        return CsmFabricatorCosts.cost(FabricatorIngredient.part(CsmParts.LED_MODULE, 1),
            FabricatorIngredient.part(CsmParts.SHEET_METAL, 1),
            FabricatorIngredient.part(CsmParts.CONTROL_BOARD, 1));
      case "marquee_pylon":
        return CsmFabricatorCosts.cost(FabricatorIngredient.any("minecraft:stone", 2),
            FabricatorIngredient.part(CsmParts.SHEET_METAL, 1));
      case "venue_gate_blade":
      case "venue_section_blade":
        return CsmFabricatorCosts.cost(FabricatorIngredient.part(CsmParts.SIGN_BLANK, 1),
            FabricatorIngredient.part(CsmParts.LED_MODULE, 1));
      case "venue_blade_bracket":
        return CsmFabricatorCosts.cost(FabricatorIngredient.any("minecraft:iron_ingot", 1),
            FabricatorIngredient.part(CsmParts.FASTENER_KIT, 1));
      case "box_office_window":
        return CsmFabricatorCosts.cost(FabricatorIngredient.part(CsmParts.SHEET_METAL, 2),
            FabricatorIngredient.any("minecraft:glass_pane", 2),
            FabricatorIngredient.part(CsmParts.LED_MODULE, 1));
      case "aluminium_bleacher":
      case "aluminium_bleacher_aisle":
        return CsmFabricatorCosts.cost(FabricatorIngredient.part(CsmParts.SHEET_METAL, 2));
      case "aluminium_bleacher_frame":
        return CsmFabricatorCosts.cost(FabricatorIngredient.part(CsmParts.SHEET_METAL, 1));
      case "press_box_module":
        return CsmFabricatorCosts.cost(FabricatorIngredient.part(CsmParts.SHEET_METAL, 2),
            FabricatorIngredient.any("minecraft:glass_pane", 2));
      case "ticket_booth":
        return CsmFabricatorCosts.cost(FabricatorIngredient.part(CsmParts.ENCLOSURE_SHELL, 2),
            FabricatorIngredient.any("minecraft:glass_pane", 4),
            FabricatorIngredient.part(CsmParts.FASTENER_KIT, 1));
      case "stadium_light_pole":
      case "stadium_light_lattice":
        return CsmFabricatorCosts.cost(FabricatorIngredient.any("minecraft:iron_ingot", 2));
      case "stadium_light_head":
        return CsmFabricatorCosts.cost(FabricatorIngredient.part(CsmParts.LED_MODULE, 6),
            FabricatorIngredient.part(CsmParts.SHEET_METAL, 2),
            FabricatorIngredient.part(CsmParts.WIRING_HARNESS, 1));
      case "stadium_light_head_small":
        return CsmFabricatorCosts.cost(FabricatorIngredient.part(CsmParts.LED_MODULE, 3),
            FabricatorIngredient.part(CsmParts.SHEET_METAL, 1),
            FabricatorIngredient.part(CsmParts.WIRING_HARNESS, 1));
      default:
        return null;
    }
  }
}

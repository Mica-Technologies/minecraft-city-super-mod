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
 * the boards enclosures and LED modules. At a bowling centre: a maple lane or approach block is
 * planks (a synthetic one planks and dye), the pins planks and dye, the pinsetter enclosures,
 * wiring, a control board and LED modules for its masking graphic, the ball return sheet metal
 * and its blower's wiring, a settee wool on sheet metal, the console a control board behind
 * glass, the monitors enclosures and LED modules, the ball rack sheet metal and dye, the shoe
 * counter planks, the cubbies planks and leather, the shoe spray sheet metal. On a court or a
 * track: acrylic is stone and dye, polyurethane slime and dye (a painted cell or an anchor adds
 * dye), sand is sand; a net string on iron posts; the umpire chair and the bench sheet metal; the
 * chain-link fence iron bars, its curtain wool; the ball machine sheet metal and wiring; the
 * scoreboard and the lap counter enclosures and LED modules (the lap counter's bell gold); the
 * hoop iron and glass; hurdles, blocks, the toe board and the barrier iron or planks; the camera
 * tower a lens and a sensor; the landing mats wool; the cage iron and string.
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
  /** The Bowling tab. */
  public static final String TAB_BOWLING = "tabvenuesbowling";
  /** The Courts, Track &amp; Pool tab. */
  public static final String TAB_COURTS = "tabvenuescourts";
  /** The Golf tab. */
  public static final String TAB_GOLF = "tabvenuesgolf";
  /** The Skate Park tab. */
  public static final String TAB_SKATE = "tabvenuesskate";

  /**
   * The pool's, golf's and the skate park's pieces: a registry name, then its ingredients, each
   * a vanilla item name (lower case) or a CSM part (upper case, {@code part_} and the name in
   * lower case), with {@code " xN"} for more than one. {@code audit_fabricator_costs.py} reads
   * this table.
   */
  private static final String[][] POOL_GOLF_SKATE = {
      {"pool_tile", "hardened_clay"},
      {"pool_deck", "hardened_clay"},
      {"pool_gutter", "hardened_clay", "iron_bars"},
      {"pool_floor_anchor", "hardened_clay", "dye x2"},
      {"pool_floor_painted", "hardened_clay", "dye x2"},
      {"pool_wall_target", "hardened_clay", "dye"},
      {"pool_touchpad", "hardened_clay", "WIRING_HARNESS"},
      {"pool_lane_rope", "string x2", "dye"},
      {"pool_backstroke_flags", "string", "wool", "iron_ingot"},
      {"pool_starting_block", "iron_ingot x2", "dye"},
      {"pool_ladder", "iron_ingot x3"},
      {"pool_lifeguard_chair", "SHEET_METAL x3", "wool"},
      {"pool_springboard_1m", "planks x4", "iron_ingot x2"},
      {"pool_springboard_3m", "CONCRETE_MIX x4", "planks x4", "iron_ingot x4"},
      {"pool_dive_platform", "CONCRETE_MIX x12", "iron_ingot x6"},
      {"pool_pace_clock", "ENCLOSURE_SHELL", "CONTROL_BOARD"},
      {"pool_scoreboard", "ENCLOSURE_SHELL x2", "LED_MODULE x2", "CONTROL_BOARD"},
      {"golf_turf", "dirt", "wheat_seeds"},
      {"golf_bunker_sand", "sand"},
      {"golf_cart_path", "CONCRETE_MIX"},
      {"golf_cart_path_curb", "CONCRETE_MIX"},
      {"golf_flagstick", "stick x2", "wool"},
      {"golf_tee_marker", "planks", "dye"},
      {"golf_yardage_marker", "planks", "dye"},
      {"golf_hole_sign", "SIGN_BLANK", "planks"},
      {"golf_ball_washer", "SHEET_METAL", "FASTENER_KIT"},
      {"golf_bench", "planks x2", "iron_ingot"},
      {"driving_range_mat", "wool x2", "slime_ball"},
      {"range_tee_divider", "planks x2", "iron_ingot"},
      {"range_ball_bucket", "iron_ingot"},
      {"range_target_flag", "stick x2", "wool", "planks"},
      {"range_ball_dispenser", "SHEET_METAL x3", "CONTROL_BOARD", "WIRING_HARNESS"},
      {"minigolf_carpet", "wool", "dye"},
      {"minigolf_cup", "wool", "dye"},
      {"minigolf_border", "planks"},
      {"minigolf_ramp", "wool", "planks"},
      {"minigolf_scorecard_stand", "planks", "paper x2"},
      {"minigolf_windmill", "brick x4", "planks x2", "wool x2"},
      {"minigolf_loop", "iron_ingot x3", "wool"},
      {"skate_concrete", "CONCRETE_MIX"},
      {"skate_quarter_pipe", "CONCRETE_MIX x2", "iron_ingot"},
      {"skate_bowl_corner", "CONCRETE_MIX x2", "iron_ingot"},
      {"skate_ramp", "CONCRETE_MIX"},
      {"skate_grind_rail", "iron_ingot x2"},
      {"skate_ledge", "CONCRETE_MIX x2", "iron_ingot"}};

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
    List<FabricatorIngredient> bowl = bowling(registryName);
    if (bowl != null) {
      return bowl;
    }
    List<FabricatorIngredient> court = courts(registryName);
    if (court != null) {
      return court;
    }
    List<FabricatorIngredient> table = tabled(registryName);
    if (table != null) {
      return table;
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
      case "court_acrylic_painted":
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

  /** The Bowling tab's pieces, or null for a registry name that is not one of them. */
  @Nullable
  private static List<FabricatorIngredient> bowling(String registryName) {
    switch (registryName) {
      case "bowling_lane":
      case "bowling_approach":
        return CsmFabricatorCosts.cost(FabricatorIngredient.any("minecraft:planks", 2));
      case "bowling_lane_synthetic":
      case "bowling_approach_synthetic":
        return CsmFabricatorCosts.cost(FabricatorIngredient.any("minecraft:planks", 1),
            FabricatorIngredient.any("minecraft:dye", 1));
      case "bowling_pins":
        return CsmFabricatorCosts.cost(FabricatorIngredient.any("minecraft:planks", 2),
            FabricatorIngredient.any("minecraft:dye", 1));
      case "bowling_pinsetter":
        return CsmFabricatorCosts.cost(FabricatorIngredient.part(CsmParts.ENCLOSURE_SHELL, 4),
            FabricatorIngredient.part(CsmParts.WIRING_HARNESS, 2),
            FabricatorIngredient.part(CsmParts.CONTROL_BOARD, 1),
            FabricatorIngredient.part(CsmParts.LED_MODULE, 2));
      case "bowling_ball_return":
        return CsmFabricatorCosts.cost(FabricatorIngredient.part(CsmParts.SHEET_METAL, 2),
            FabricatorIngredient.part(CsmParts.WIRING_HARNESS, 1));
      case "bowling_settee":
      case "bowling_settee_corner":
        return CsmFabricatorCosts.cost(FabricatorIngredient.any("minecraft:wool", 2),
            FabricatorIngredient.part(CsmParts.SHEET_METAL, 1));
      case "bowling_scorer_console":
        return CsmFabricatorCosts.cost(FabricatorIngredient.part(CsmParts.CONTROL_BOARD, 1),
            FabricatorIngredient.part(CsmParts.SHEET_METAL, 1),
            FabricatorIngredient.any("minecraft:glass_pane", 1));
      case "bowling_scoring_monitor":
        return CsmFabricatorCosts.cost(FabricatorIngredient.part(CsmParts.ENCLOSURE_SHELL, 2),
            FabricatorIngredient.part(CsmParts.LED_MODULE, 4),
            FabricatorIngredient.part(CsmParts.CONTROL_BOARD, 1));
      case "bowling_ball_rack":
        return CsmFabricatorCosts.cost(FabricatorIngredient.part(CsmParts.SHEET_METAL, 2),
            FabricatorIngredient.any("minecraft:dye", 1));
      case "bowling_shoe_counter":
        return CsmFabricatorCosts.cost(FabricatorIngredient.any("minecraft:planks", 2),
            FabricatorIngredient.part(CsmParts.FASTENER_KIT, 1));
      case "bowling_shoe_cubbies":
        return CsmFabricatorCosts.cost(FabricatorIngredient.any("minecraft:planks", 2),
            FabricatorIngredient.any("minecraft:leather", 2));
      case "bowling_shoe_spray":
        return CsmFabricatorCosts.cost(FabricatorIngredient.part(CsmParts.SHEET_METAL, 1));
      default:
        return null;
    }
  }

  /** A piece in {@link #POOL_GOLF_SKATE}, or null for a name that is not one of them. */
  @Nullable
  private static List<FabricatorIngredient> tabled(String registryName) {
    for (String[] row : POOL_GOLF_SKATE) {
      if (!row[0].equals(registryName)) {
        continue;
      }
      FabricatorIngredient[] in = new FabricatorIngredient[row.length - 1];
      for (int i = 1; i < row.length; i++) {
        String spec = row[i];
        int n = 1;
        int x = spec.indexOf(" x");
        if (x > 0) {
          n = Integer.parseInt(spec.substring(x + 2));
          spec = spec.substring(0, x);
        }
        in[i - 1] = spec.equals(spec.toUpperCase(java.util.Locale.ROOT))
            ? FabricatorIngredient.part("part_" + spec.toLowerCase(java.util.Locale.ROOT), n)
            : FabricatorIngredient.any("minecraft:" + spec, n);
      }
      return CsmFabricatorCosts.cost(in);
    }
    return null;
  }

  /** The Courts, Track &amp; Pool tab's pieces, or null for a name that is not one of them. */
  @Nullable
  private static List<FabricatorIngredient> courts(String registryName) {
    switch (registryName) {
      case "court_acrylic":
        return CsmFabricatorCosts.cost(FabricatorIngredient.any("minecraft:stone", 1),
            FabricatorIngredient.any("minecraft:dye", 1));
      case "court_acrylic_anchor":
      case "court_acrylic_painted":
        return CsmFabricatorCosts.cost(FabricatorIngredient.any("minecraft:stone", 1),
            FabricatorIngredient.any("minecraft:dye", 2));
      case "track_surface":
        return CsmFabricatorCosts.cost(FabricatorIngredient.any("minecraft:slime_ball", 1),
            FabricatorIngredient.any("minecraft:dye", 1));
      case "track_marking_anchor":
      case "track_painted":
        return CsmFabricatorCosts.cost(FabricatorIngredient.any("minecraft:slime_ball", 1),
            FabricatorIngredient.any("minecraft:dye", 2));
      case "sports_sand":
        return CsmFabricatorCosts.cost(FabricatorIngredient.any("minecraft:sand", 1));
      case "tennis_net":
      case "pickleball_net":
      case "volleyball_net":
        return CsmFabricatorCosts.cost(FabricatorIngredient.any("minecraft:string", 2),
            FabricatorIngredient.any("minecraft:iron_ingot", 1));
      case "tennis_umpire_chair":
        return CsmFabricatorCosts.cost(FabricatorIngredient.part(CsmParts.SHEET_METAL, 3));
      case "court_players_bench":
        return CsmFabricatorCosts.cost(FabricatorIngredient.part(CsmParts.SHEET_METAL, 1));
      case "court_fence":
        return CsmFabricatorCosts.cost(FabricatorIngredient.any("minecraft:iron_bars", 2));
      case "court_divider_curtain":
        return CsmFabricatorCosts.cost(FabricatorIngredient.any("minecraft:wool", 2));
      case "tennis_ball_machine":
        return CsmFabricatorCosts.cost(FabricatorIngredient.part(CsmParts.SHEET_METAL, 2),
            FabricatorIngredient.part(CsmParts.WIRING_HARNESS, 1));
      case "court_scoreboard":
        return CsmFabricatorCosts.cost(FabricatorIngredient.part(CsmParts.ENCLOSURE_SHELL, 2),
            FabricatorIngredient.part(CsmParts.LED_MODULE, 2),
            FabricatorIngredient.part(CsmParts.CONTROL_BOARD, 1));
      case "track_lap_counter":
        return CsmFabricatorCosts.cost(FabricatorIngredient.part(CsmParts.ENCLOSURE_SHELL, 1),
            FabricatorIngredient.part(CsmParts.LED_MODULE, 1),
            FabricatorIngredient.any("minecraft:gold_ingot", 1));
      case "basketball_hoop_pole":
        return CsmFabricatorCosts.cost(FabricatorIngredient.any("minecraft:iron_ingot", 4),
            FabricatorIngredient.any("minecraft:glass_pane", 2),
            FabricatorIngredient.any("minecraft:string", 1));
      case "track_hurdle":
      case "starting_block":
        return CsmFabricatorCosts.cost(FabricatorIngredient.any("minecraft:iron_ingot", 1),
            FabricatorIngredient.any("minecraft:dye", 1));
      case "shot_put_toe_board":
      case "steeplechase_barrier":
        return CsmFabricatorCosts.cost(FabricatorIngredient.any("minecraft:planks", 2),
            FabricatorIngredient.any("minecraft:iron_ingot", 1));
      case "track_timing_tower":
        return CsmFabricatorCosts.cost(FabricatorIngredient.any("minecraft:iron_ingot", 2),
            FabricatorIngredient.part(CsmParts.LENS_ASSEMBLY, 1),
            FabricatorIngredient.part(CsmParts.OPTICAL_SENSOR, 1));
      case "high_jump_mat":
      case "pole_vault_pit":
        return CsmFabricatorCosts.cost(FabricatorIngredient.any("minecraft:wool", 6),
            FabricatorIngredient.any("minecraft:iron_ingot", 2));
      case "throwing_cage":
        return CsmFabricatorCosts.cost(FabricatorIngredient.any("minecraft:iron_ingot", 4),
            FabricatorIngredient.any("minecraft:string", 6));
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

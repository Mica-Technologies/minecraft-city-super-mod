package com.micatechnologies.minecraft.csm.furniture.residential;

import com.micatechnologies.minecraft.csm.materials.CsmFabricatorCosts;
import com.micatechnologies.minecraft.csm.materials.CsmParts;
import com.micatechnologies.minecraft.csm.materials.FabricatorIngredient;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.block.Block;

/**
 * What the Fabricator charges for the Residential tab: furniture is timber and fittings, as the
 * Furniture tab's is, in proportion to how much of it there is; upholstery adds wool, a
 * bookcase its books, and the cafe table's cast iron base an iron ingot. The kitchen adds stone
 * for its countertops and prices its appliances as sheet metal and electrics, its tableware as
 * clay or glass. The bedroom's beds are wool and planks, as a vanilla bed is. The bathroom's
 * porcelain is clay, its rails iron, its laundry appliances priced as the dishwasher. The living
 * room's extras are priced by what they are made of: electronics as an enclosure and a control
 * board, a speaker's driver, a piano's timber and strings, a painting as a painting, a plant as
 * a flower pot and what grows in it, a fireplace as stone and a mantel. Outdoors, a frame is teak
 * or, powder-coated, iron; cushions, canopies and pet beds wool; grills and the fire pit's bowl
 * sheet metal; plastic (a cooler, a kiddie pool) an enclosure shell; a chimney bricks; and what
 * bounces a slime ball.
 *
 * <p>Priced by registry name, whose piece is its first words and whose finish is its last
 * ({@code sofa_corner_navy}). {@code audit_fabricator_costs.py} mirrors this.</p>
 *
 * @since 2026.9
 */
public final class ResidentialFabricatorRules {

  /** The Residential tab. */
  public static final String TAB_ID = "tabresidential";

  private static final String MC_PLANKS = "minecraft:planks";
  private static final String MC_WOOL = "minecraft:wool";
  private static final String MC_BOOK = "minecraft:book";
  private static final String MC_IRON_INGOT = "minecraft:iron_ingot";
  private static final String MC_STONE = "minecraft:stone";
  private static final String MC_GLASS = "minecraft:glass";
  private static final String MC_CLAY = "minecraft:clay_ball";
  private static final String MC_CAKE = "minecraft:cake";
  private static final String MC_GLASS_PANE = "minecraft:glass_pane";
  private static final String MC_PAPER = "minecraft:paper";
  private static final String MC_CLOCK = "minecraft:clock";
  private static final String MC_PAINTING = "minecraft:painting";
  private static final String MC_FLOWER_POT = "minecraft:flower_pot";
  private static final String MC_SAPLING = "minecraft:sapling";
  private static final String MC_CACTUS = "minecraft:cactus";
  private static final String MC_VINE = "minecraft:vine";
  private static final String MC_STRING = "minecraft:string";
  private static final String MC_TORCH = "minecraft:torch";
  private static final String MC_GOLD_NUGGET = "minecraft:gold_nugget";
  private static final String MC_LEVER = "minecraft:lever";
  private static final String MC_STONE_BUTTON = "minecraft:stone_button";
  private static final String MC_BRICK = "minecraft:brick";
  private static final String MC_SLIME_BALL = "minecraft:slime_ball";
  private static final String MC_SAND = "minecraft:sand";

  private ResidentialFabricatorRules() {
  }

  /**
   * Prices a block in the Residential tab.
   *
   * @param block        the block
   * @param registryName its registry name
   *
   * @return the cost, or null for the generic cost
   */
  @Nullable
  public static List<FabricatorIngredient> price(Block block, String registryName) {
    List<FabricatorIngredient> outdoor = outdoor(registryName);
    if (outdoor != null) {
      return outdoor;
    }
    List<FabricatorIngredient> living = living(registryName);
    if (living != null) {
      return living;
    }
    List<FabricatorIngredient> bathroom = bathroom(registryName);
    if (bathroom != null) {
      return bathroom;
    }
    List<FabricatorIngredient> kitchen = kitchen(registryName);
    if (kitchen != null) {
      return kitchen;
    }
    List<FabricatorIngredient> bedroom = bedroom(registryName);
    if (bedroom != null) {
      return bedroom;
    }
    if (registryName.startsWith("sofa_")) {
      return CsmFabricatorCosts.cost(FabricatorIngredient.any(MC_PLANKS, 2),
          FabricatorIngredient.any(MC_WOOL, 3));
    }
    if (registryName.startsWith("armchair_")) {
      return CsmFabricatorCosts.cost(FabricatorIngredient.any(MC_PLANKS, 2),
          FabricatorIngredient.any(MC_WOOL, 2));
    }
    if (registryName.startsWith("bookcase_")) {
      return CsmFabricatorCosts.cost(FabricatorIngredient.any(MC_PLANKS, 3),
          FabricatorIngredient.any(MC_BOOK, 2));
    }
    if (registryName.startsWith("sideboard_")) {
      return CsmFabricatorCosts.cost(FabricatorIngredient.any(MC_PLANKS, 6),
          FabricatorIngredient.part(CsmParts.FASTENER_KIT, 1));
    }
    if (registryName.startsWith("tv_stand_")) {
      return CsmFabricatorCosts.cost(FabricatorIngredient.any(MC_PLANKS, 4),
          FabricatorIngredient.part(CsmParts.FASTENER_KIT, 1));
    }
    if (registryName.startsWith("dining_table_")) {
      return CsmFabricatorCosts.cost(FabricatorIngredient.any(MC_PLANKS, 3),
          FabricatorIngredient.part(CsmParts.FASTENER_KIT, 1));
    }
    if (registryName.startsWith("cafe_table_")) {
      return CsmFabricatorCosts.cost(FabricatorIngredient.any(MC_PLANKS, 1),
          FabricatorIngredient.any(MC_IRON_INGOT, 1));
    }
    // Chairs, stools and the smaller tables.
    return CsmFabricatorCosts.cost(FabricatorIngredient.any(MC_PLANKS, 2),
        FabricatorIngredient.part(CsmParts.FASTENER_KIT, 1));
  }

  /**
   * The bathroom, restroom and laundry: porcelain is fired clay (a toilet or a bath a good deal
   * of it), with a steel tap or valve where it has one; rails, holders and grab bars are iron;
   * the vanity and mirror cabinet are timber like the kitchen's, with clay for the basin and
   * glass for the mirror; the shower is glass in a steel frame on a porcelain tray; the washing
   * machine and dryer are priced as the dishwasher is.
   *
   * @param registryName the registry name
   *
   * @return the cost, or null if it is none of these
   */
  @Nullable
  private static List<FabricatorIngredient> bathroom(String registryName) {
    if (registryName.startsWith("toilet_paper_holder_")) {
      return CsmFabricatorCosts.cost(FabricatorIngredient.any(MC_IRON_INGOT, 1),
          FabricatorIngredient.any(MC_PAPER, 1));
    }
    if (registryName.startsWith("toilet_brush_")) {
      return CsmFabricatorCosts.cost(FabricatorIngredient.any(MC_IRON_INGOT, 1));
    }
    if (registryName.startsWith("toilet_") || registryName.startsWith("urinal_")) {
      return CsmFabricatorCosts.cost(FabricatorIngredient.any(MC_CLAY, 4),
          FabricatorIngredient.part(CsmParts.FASTENER_KIT, 1));
    }
    if (registryName.startsWith("pedestal_sink_") || registryName.startsWith("laundry_tub_")) {
      return CsmFabricatorCosts.cost(FabricatorIngredient.any(MC_CLAY, 3),
          FabricatorIngredient.part(CsmParts.SHEET_METAL, 1));
    }
    if (registryName.startsWith("bathroom_vanity_")) {
      return CsmFabricatorCosts.cost(FabricatorIngredient.any(MC_PLANKS, 3),
          FabricatorIngredient.any(MC_CLAY, 2), FabricatorIngredient.part(CsmParts.SHEET_METAL, 1));
    }
    if (registryName.startsWith("mirror_cabinet_")) {
      return CsmFabricatorCosts.cost(FabricatorIngredient.any(MC_PLANKS, 2),
          FabricatorIngredient.any(MC_GLASS_PANE, 2),
          FabricatorIngredient.part(CsmParts.FASTENER_KIT, 1));
    }
    if (registryName.startsWith("bathtub_")) {
      return CsmFabricatorCosts.cost(FabricatorIngredient.any(MC_CLAY, 6),
          FabricatorIngredient.part(CsmParts.SHEET_METAL, 1));
    }
    if (registryName.startsWith("shower_head_")) {
      return CsmFabricatorCosts.cost(FabricatorIngredient.part(CsmParts.SHEET_METAL, 1));
    }
    if (registryName.startsWith("shower_")) {
      return CsmFabricatorCosts.cost(FabricatorIngredient.any(MC_GLASS_PANE, 4),
          FabricatorIngredient.any(MC_CLAY, 2), FabricatorIngredient.part(CsmParts.SHEET_METAL, 1));
    }
    if (registryName.startsWith("towel_rail_")) {
      return CsmFabricatorCosts.cost(FabricatorIngredient.any(MC_IRON_INGOT, 1),
          FabricatorIngredient.any(MC_WOOL, 1));
    }
    if (registryName.startsWith("heated_towel_rail_")) {
      return CsmFabricatorCosts.cost(FabricatorIngredient.part(CsmParts.SHEET_METAL, 1),
          FabricatorIngredient.part(CsmParts.WIRING_HARNESS, 1));
    }
    if (registryName.startsWith("bathroom_radiator_")) {
      return CsmFabricatorCosts.cost(FabricatorIngredient.part(CsmParts.SHEET_METAL, 2));
    }
    if (registryName.startsWith("wastebasket_") || registryName.startsWith("soap_dispenser_")
        || registryName.startsWith("paper_towel_dispenser_")) {
      return CsmFabricatorCosts.cost(FabricatorIngredient.part(CsmParts.SHEET_METAL, 1));
    }
    if (registryName.startsWith("toiletries_tray_")) {
      return CsmFabricatorCosts.cost(FabricatorIngredient.any(MC_CLAY, 1),
          FabricatorIngredient.any(MC_GLASS, 1));
    }
    if (registryName.startsWith("bath_mat_")) {
      return CsmFabricatorCosts.cost(FabricatorIngredient.any(MC_WOOL, 1));
    }
    if (registryName.startsWith("grab_bar_")) {
      return CsmFabricatorCosts.cost(FabricatorIngredient.any(MC_IRON_INGOT, 2));
    }
    if (registryName.startsWith("baby_changing_station_")) {
      return CsmFabricatorCosts.cost(FabricatorIngredient.part(CsmParts.SHEET_METAL, 1),
          FabricatorIngredient.part(CsmParts.FASTENER_KIT, 1));
    }
    if (registryName.startsWith("washing_machine_") || registryName.startsWith("dryer_")) {
      return CsmFabricatorCosts.cost(FabricatorIngredient.part(CsmParts.SHEET_METAL, 3),
          FabricatorIngredient.part(CsmParts.CONTROL_BOARD, 1),
          FabricatorIngredient.part(CsmParts.WIRING_HARNESS, 1));
    }
    if (registryName.startsWith("steam_iron_")) {
      return CsmFabricatorCosts.cost(FabricatorIngredient.part(CsmParts.SHEET_METAL, 1),
          FabricatorIngredient.part(CsmParts.WIRING_HARNESS, 1));
    }
    if (registryName.startsWith("ironing_board_")) {
      return CsmFabricatorCosts.cost(FabricatorIngredient.any(MC_IRON_INGOT, 2),
          FabricatorIngredient.any(MC_WOOL, 1));
    }
    if (registryName.startsWith("laundry_basket_")) {
      return CsmFabricatorCosts.cost(FabricatorIngredient.any(MC_PLANKS, 2));
    }
    return null;
  }

  /**
   * The kitchen: cabinets are timber and fittings, with stone for a countertop; the sink adds its
   * steel bowl; the appliances are sheet metal with a lamp, a duct, or a compressor's controls.
   *
   * @param registryName the registry name
   *
   * @return the cost, or null if it is not a kitchen piece
   */
  @Nullable
  private static List<FabricatorIngredient> kitchen(String registryName) {
    List<FabricatorIngredient> appliance = appliances(registryName);
    if (appliance != null) {
      return appliance;
    }
    if (registryName.startsWith("kitchen_corner_cabinet_")
        || registryName.startsWith("kitchen_island_")) {
      return CsmFabricatorCosts.cost(FabricatorIngredient.any(MC_PLANKS, 5),
          FabricatorIngredient.any(MC_STONE, 2), FabricatorIngredient.part(CsmParts.FASTENER_KIT, 1));
    }
    if (registryName.startsWith("kitchen_cooktop_cabinet_")) {
      return CsmFabricatorCosts.cost(FabricatorIngredient.any(MC_PLANKS, 4),
          FabricatorIngredient.any(MC_STONE, 1),
          FabricatorIngredient.part(CsmParts.WIRING_HARNESS, 1));
    }
    if (registryName.startsWith("kitchen_sink_cabinet_")) {
      return CsmFabricatorCosts.cost(FabricatorIngredient.any(MC_PLANKS, 3),
          FabricatorIngredient.any(MC_STONE, 1), FabricatorIngredient.part(CsmParts.SHEET_METAL, 1));
    }
    if (registryName.startsWith("kitchen_wall_shelf_")) {
      return CsmFabricatorCosts.cost(FabricatorIngredient.any(MC_PLANKS, 2));
    }
    if (registryName.startsWith("kitchen_wall_cabinet_")) {
      return CsmFabricatorCosts.cost(FabricatorIngredient.any(MC_PLANKS, 3),
          FabricatorIngredient.part(CsmParts.FASTENER_KIT, 1));
    }
    if (registryName.startsWith("kitchen_")) {
      // The base cabinets: two doors, drawers, door and drawer.
      return CsmFabricatorCosts.cost(FabricatorIngredient.any(MC_PLANKS, 4),
          FabricatorIngredient.any(MC_STONE, 1), FabricatorIngredient.part(CsmParts.FASTENER_KIT, 1));
    }
    if (registryName.startsWith("range_hood_")) {
      return CsmFabricatorCosts.cost(FabricatorIngredient.part(CsmParts.SHEET_METAL, 2),
          FabricatorIngredient.part(CsmParts.DUCTING, 1),
          FabricatorIngredient.part(CsmParts.LED_MODULE, 1));
    }
    if (registryName.startsWith("under_cabinet_light_")) {
      return CsmFabricatorCosts.cost(FabricatorIngredient.part(CsmParts.LED_MODULE, 1),
          FabricatorIngredient.part(CsmParts.SHEET_METAL, 1));
    }
    if (registryName.startsWith("refrigerator_")) {
      return CsmFabricatorCosts.cost(FabricatorIngredient.part(CsmParts.SHEET_METAL, 4),
          FabricatorIngredient.part(CsmParts.CONTROL_BOARD, 1),
          FabricatorIngredient.part(CsmParts.WIRING_HARNESS, 1));
    }
    if (registryName.startsWith("chest_freezer_")) {
      return CsmFabricatorCosts.cost(FabricatorIngredient.part(CsmParts.SHEET_METAL, 3),
          FabricatorIngredient.part(CsmParts.CONTROL_BOARD, 1),
          FabricatorIngredient.part(CsmParts.WIRING_HARNESS, 1));
    }
    return null;
  }

  /**
   * The bedroom and nursery: a bed is wool and planks as a vanilla bed is (three of each for a
   * single, twice that for a double, a king or a bunk); the case pieces are timber and fittings
   * in proportion to their size, with glass for a mirror; a rug is wool.
   *
   * @param registryName the registry name
   *
   * @return the cost, or null if it is not a bedroom piece
   */
  @Nullable
  private static List<FabricatorIngredient> bedroom(String registryName) {
    if (registryName.startsWith("bed_single_")) {
      return CsmFabricatorCosts.cost(FabricatorIngredient.any(MC_PLANKS, 3),
          FabricatorIngredient.any(MC_WOOL, 3));
    }
    if (registryName.startsWith("bed_double_") || registryName.startsWith("bed_king_")
        || registryName.startsWith("bunk_bed_")) {
      return CsmFabricatorCosts.cost(FabricatorIngredient.any(MC_PLANKS, 6),
          FabricatorIngredient.any(MC_WOOL, 6));
    }
    if (registryName.startsWith("day_bed_")) {
      return CsmFabricatorCosts.cost(FabricatorIngredient.any(MC_PLANKS, 4),
          FabricatorIngredient.any(MC_WOOL, 3));
    }
    if (registryName.startsWith("wardrobe_") || registryName.startsWith("closet_")) {
      return CsmFabricatorCosts.cost(FabricatorIngredient.any(MC_PLANKS, 8),
          FabricatorIngredient.part(CsmParts.FASTENER_KIT, 1));
    }
    if (registryName.startsWith("dresser_mirror_")) {
      return CsmFabricatorCosts.cost(FabricatorIngredient.any(MC_PLANKS, 6),
          FabricatorIngredient.any(MC_GLASS_PANE, 2),
          FabricatorIngredient.part(CsmParts.FASTENER_KIT, 1));
    }
    if (registryName.startsWith("dresser_")) {
      return CsmFabricatorCosts.cost(FabricatorIngredient.any(MC_PLANKS, 6),
          FabricatorIngredient.part(CsmParts.FASTENER_KIT, 1));
    }
    if (registryName.startsWith("vanity_stool_")) {
      return CsmFabricatorCosts.cost(FabricatorIngredient.any(MC_PLANKS, 1),
          FabricatorIngredient.any(MC_WOOL, 1));
    }
    if (registryName.startsWith("vanity_") || registryName.startsWith("standing_mirror_")) {
      return CsmFabricatorCosts.cost(FabricatorIngredient.any(MC_PLANKS, 3),
          FabricatorIngredient.any(MC_GLASS_PANE, 3),
          FabricatorIngredient.part(CsmParts.FASTENER_KIT, 1));
    }
    if (registryName.startsWith("desk_chair_")) {
      return CsmFabricatorCosts.cost(FabricatorIngredient.any(MC_IRON_INGOT, 1),
          FabricatorIngredient.any(MC_WOOL, 2));
    }
    if (registryName.startsWith("desk_") || registryName.startsWith("blanket_chest_")) {
      return CsmFabricatorCosts.cost(FabricatorIngredient.any(MC_PLANKS, 4),
          FabricatorIngredient.part(CsmParts.FASTENER_KIT, 1));
    }
    if (registryName.startsWith("crib_") || registryName.startsWith("cradle_with_drawers_")
        || registryName.startsWith("changing_table_")) {
      return CsmFabricatorCosts.cost(FabricatorIngredient.any(MC_PLANKS, 4),
          FabricatorIngredient.any(MC_WOOL, 1), FabricatorIngredient.part(CsmParts.FASTENER_KIT, 1));
    }
    if (registryName.startsWith("rocking_chair_")) {
      return CsmFabricatorCosts.cost(FabricatorIngredient.any(MC_PLANKS, 3));
    }
    if (registryName.startsWith("rug_")) {
      return CsmFabricatorCosts.cost(FabricatorIngredient.any(MC_WOOL, 2));
    }
    // The nightstand falls through to the chairs' and small tables' cost.
    return null;
  }

  /**
   * The working appliances and the small things for the counter: the big appliances are sheet
   * metal with a control board and wiring, as the refrigerator is; the countertop ones a sheet
   * and wiring, with a control board where they have a timer or a display; tableware is fired
   * clay or glass.
   *
   * @param registryName the registry name
   *
   * @return the cost, or null if it is none of these
   */
  @Nullable
  private static List<FabricatorIngredient> appliances(String registryName) {
    if (registryName.startsWith("kitchen_range_")) {
      return CsmFabricatorCosts.cost(FabricatorIngredient.part(CsmParts.SHEET_METAL, 4),
          FabricatorIngredient.part(CsmParts.CONTROL_BOARD, 1),
          FabricatorIngredient.part(CsmParts.WIRING_HARNESS, 1));
    }
    if (registryName.startsWith("wall_oven_") || registryName.startsWith("dishwasher_")) {
      return CsmFabricatorCosts.cost(FabricatorIngredient.part(CsmParts.SHEET_METAL, 3),
          FabricatorIngredient.part(CsmParts.CONTROL_BOARD, 1),
          FabricatorIngredient.part(CsmParts.WIRING_HARNESS, 1));
    }
    if (registryName.startsWith("microwave_")) {
      return CsmFabricatorCosts.cost(FabricatorIngredient.part(CsmParts.SHEET_METAL, 2),
          FabricatorIngredient.part(CsmParts.CONTROL_BOARD, 1),
          FabricatorIngredient.part(CsmParts.WIRING_HARNESS, 1));
    }
    if (registryName.startsWith("air_fryer_") || registryName.startsWith("coffee_machine_")) {
      return CsmFabricatorCosts.cost(FabricatorIngredient.part(CsmParts.SHEET_METAL, 1),
          FabricatorIngredient.part(CsmParts.CONTROL_BOARD, 1),
          FabricatorIngredient.part(CsmParts.WIRING_HARNESS, 1));
    }
    if (registryName.startsWith("blender_")) {
      return CsmFabricatorCosts.cost(FabricatorIngredient.part(CsmParts.SHEET_METAL, 1),
          FabricatorIngredient.part(CsmParts.WIRING_HARNESS, 1),
          FabricatorIngredient.any(MC_GLASS, 1));
    }
    if (registryName.startsWith("toaster_") || registryName.startsWith("kettle_")
        || registryName.startsWith("stand_mixer_")) {
      return CsmFabricatorCosts.cost(FabricatorIngredient.part(CsmParts.SHEET_METAL, 1),
          FabricatorIngredient.part(CsmParts.WIRING_HARNESS, 1));
    }
    if (registryName.startsWith("cookie_jar_") || registryName.startsWith("plate_stack_")) {
      return CsmFabricatorCosts.cost(FabricatorIngredient.any(MC_CLAY, 3));
    }
    if (registryName.startsWith("dinner_plate_") || registryName.startsWith("coffee_mug_")) {
      return CsmFabricatorCosts.cost(FabricatorIngredient.any(MC_CLAY, 1));
    }
    if (registryName.startsWith("drinking_glass_")) {
      return CsmFabricatorCosts.cost(FabricatorIngredient.any(MC_GLASS, 1));
    }
    if (registryName.startsWith("cake_stand_")) {
      return CsmFabricatorCosts.cost(FabricatorIngredient.any(MC_GLASS, 1),
          FabricatorIngredient.any(MC_CAKE, 1));
    }
    if (registryName.startsWith("chopping_board_")) {
      return CsmFabricatorCosts.cost(FabricatorIngredient.any(MC_PLANKS, 1));
    }
    return null;
  }

  /**
   * The living room's extras: TVs, a stereo and speakers are an enclosure with a control board
   * and a panel or a driver; the piano timber with iron for its strings; clocks a clock;
   * pictures a painting or paper in a frame; house plants a flower pot and what grows in it;
   * the fireplace stone under a timber mantel; the ceiling fan and lamps sheet metal and wiring
   * with an LED; candles a torch; the switch a lever and the doorbell a button with a sounder;
   * the crate a chest's timber.
   *
   * @param registryName the registry name
   *
   * @return the cost, or null if it is none of these
   */
  @Nullable
  private static List<FabricatorIngredient> living(String registryName) {
    if (registryName.startsWith("large_flat_screen_tv_")
        || registryName.startsWith("large_wall_tv_")) {
      return CsmFabricatorCosts.cost(FabricatorIngredient.part(CsmParts.ENCLOSURE_SHELL, 2),
          FabricatorIngredient.part(CsmParts.CONTROL_BOARD, 1),
          FabricatorIngredient.part(CsmParts.LED_MODULE, 2));
    }
    if (registryName.startsWith("flat_screen_tv_") || registryName.startsWith("wall_tv_")) {
      return CsmFabricatorCosts.cost(FabricatorIngredient.part(CsmParts.ENCLOSURE_SHELL, 1),
          FabricatorIngredient.part(CsmParts.CONTROL_BOARD, 1),
          FabricatorIngredient.part(CsmParts.LED_MODULE, 1));
    }
    if (registryName.startsWith("crt_tv_")) {
      return CsmFabricatorCosts.cost(FabricatorIngredient.part(CsmParts.ENCLOSURE_SHELL, 1),
          FabricatorIngredient.part(CsmParts.CONTROL_BOARD, 1),
          FabricatorIngredient.any(MC_GLASS, 1));
    }
    if (registryName.startsWith("stereo_")) {
      return CsmFabricatorCosts.cost(FabricatorIngredient.part(CsmParts.ENCLOSURE_SHELL, 1),
          FabricatorIngredient.part(CsmParts.CONTROL_BOARD, 1),
          FabricatorIngredient.part(CsmParts.SOUNDER_DRIVER, 1));
    }
    if (registryName.startsWith("bookshelf_speaker_")) {
      return CsmFabricatorCosts.cost(FabricatorIngredient.any(MC_PLANKS, 1),
          FabricatorIngredient.part(CsmParts.SOUNDER_DRIVER, 1));
    }
    if (registryName.startsWith("subwoofer_")) {
      return CsmFabricatorCosts.cost(FabricatorIngredient.any(MC_PLANKS, 2),
          FabricatorIngredient.part(CsmParts.SOUNDER_DRIVER, 1));
    }
    if (registryName.startsWith("upright_piano_")) {
      return CsmFabricatorCosts.cost(FabricatorIngredient.any(MC_PLANKS, 6),
          FabricatorIngredient.any(MC_IRON_INGOT, 2),
          FabricatorIngredient.part(CsmParts.FASTENER_KIT, 1));
    }
    if (registryName.startsWith("piano_bench_")) {
      return CsmFabricatorCosts.cost(FabricatorIngredient.any(MC_PLANKS, 2),
          FabricatorIngredient.any(MC_WOOL, 1));
    }
    if (registryName.startsWith("digital_clock_")) {
      return CsmFabricatorCosts.cost(FabricatorIngredient.part(CsmParts.ENCLOSURE_SHELL, 1),
          FabricatorIngredient.part(CsmParts.CONTROL_BOARD, 1));
    }
    if (registryName.startsWith("wall_clock_")) {
      return CsmFabricatorCosts.cost(FabricatorIngredient.any(MC_PLANKS, 1),
          FabricatorIngredient.any(MC_CLOCK, 1));
    }
    if (registryName.startsWith("photo_frame_")) {
      return CsmFabricatorCosts.cost(FabricatorIngredient.any(MC_PLANKS, 1),
          FabricatorIngredient.any(MC_PAPER, 1));
    }
    if (registryName.startsWith("wall_photo_frames_")) {
      return CsmFabricatorCosts.cost(FabricatorIngredient.any(MC_PLANKS, 2),
          FabricatorIngredient.any(MC_PAPER, 3));
    }
    if (registryName.startsWith("wide_wall_art_")) {
      return CsmFabricatorCosts.cost(FabricatorIngredient.any(MC_PAINTING, 2));
    }
    if (registryName.startsWith("wall_art_")) {
      return CsmFabricatorCosts.cost(FabricatorIngredient.any(MC_PAINTING, 1));
    }
    if (registryName.startsWith("monstera_plant_") || registryName.startsWith("snake_plant_")
        || registryName.startsWith("fiddle_leaf_fig_")) {
      return CsmFabricatorCosts.cost(FabricatorIngredient.any(MC_FLOWER_POT, 1),
          FabricatorIngredient.any(MC_SAPLING, 1));
    }
    if (registryName.startsWith("succulent_pots_")) {
      return CsmFabricatorCosts.cost(FabricatorIngredient.any(MC_FLOWER_POT, 1),
          FabricatorIngredient.any(MC_CACTUS, 1));
    }
    if (registryName.startsWith("hanging_plant_")) {
      return CsmFabricatorCosts.cost(FabricatorIngredient.any(MC_FLOWER_POT, 1),
          FabricatorIngredient.any(MC_VINE, 1), FabricatorIngredient.any(MC_STRING, 1));
    }
    if (registryName.startsWith("fireplace_")) {
      return CsmFabricatorCosts.cost(FabricatorIngredient.any(MC_STONE, 4),
          FabricatorIngredient.any(MC_PLANKS, 2), FabricatorIngredient.any(MC_IRON_INGOT, 1));
    }
    if (registryName.startsWith("ceiling_fan_")) {
      return CsmFabricatorCosts.cost(FabricatorIngredient.part(CsmParts.SHEET_METAL, 1),
          FabricatorIngredient.part(CsmParts.WIRING_HARNESS, 1),
          FabricatorIngredient.part(CsmParts.LED_MODULE, 1),
          FabricatorIngredient.any(MC_PLANKS, 1));
    }
    if (registryName.startsWith("floor_lamp_") || registryName.startsWith("table_lamp_")) {
      return CsmFabricatorCosts.cost(FabricatorIngredient.part(CsmParts.LED_MODULE, 1),
          FabricatorIngredient.any(MC_IRON_INGOT, 1), FabricatorIngredient.any(MC_WOOL, 1));
    }
    if (registryName.startsWith("pillar_candles_")) {
      return CsmFabricatorCosts.cost(FabricatorIngredient.any(MC_TORCH, 3));
    }
    if (registryName.startsWith("candlestick_")) {
      return CsmFabricatorCosts.cost(FabricatorIngredient.any(MC_TORCH, 1),
          FabricatorIngredient.any(MC_GOLD_NUGGET, 2));
    }
    if (registryName.startsWith("door_mat_")) {
      return CsmFabricatorCosts.cost(FabricatorIngredient.any(MC_WOOL, 1));
    }
    if (registryName.startsWith("light_switch_")) {
      return CsmFabricatorCosts.cost(FabricatorIngredient.any(MC_LEVER, 1),
          FabricatorIngredient.part(CsmParts.WIRING_HARNESS, 1));
    }
    if (registryName.startsWith("doorbell_")) {
      return CsmFabricatorCosts.cost(FabricatorIngredient.any(MC_STONE_BUTTON, 1),
          FabricatorIngredient.part(CsmParts.SOUNDER_DRIVER, 1));
    }
    if (registryName.startsWith("storage_crate_")) {
      return CsmFabricatorCosts.cost(FabricatorIngredient.any(MC_PLANKS, 4),
          FabricatorIngredient.part(CsmParts.FASTENER_KIT, 1));
    }
    return null;
  }

  /**
   * The outdoor and backyard section: frames are timber, or iron where they are powder-coated
   * steel; upholstery, canopies and pet beds wool; the grills sheet metal (the gas grill with its
   * igniter's wiring); a fire pit stone or a steel bowl; plastic things an enclosure shell; the
   * chimney bricks; the trampoline, bounce castle and diving board a slime ball for their bounce.
   *
   * @param registryName the registry name
   *
   * @return the cost, or null if it is none of these
   */
  @Nullable
  private static List<FabricatorIngredient> outdoor(String registryName) {
    boolean black = registryName.endsWith("_black");
    String frame = black ? MC_IRON_INGOT : MC_PLANKS;
    if (registryName.startsWith("patio_table_")) {
      return CsmFabricatorCosts.cost(FabricatorIngredient.any(frame, 3),
          FabricatorIngredient.part(CsmParts.FASTENER_KIT, 1));
    }
    if (registryName.startsWith("patio_chair_")) {
      return CsmFabricatorCosts.cost(FabricatorIngredient.any(frame, 2),
          FabricatorIngredient.part(CsmParts.FASTENER_KIT, 1));
    }
    if (registryName.startsWith("patio_umbrella_")) {
      return CsmFabricatorCosts.cost(FabricatorIngredient.any(MC_WOOL, 3),
          FabricatorIngredient.any(MC_IRON_INGOT, 2));
    }
    if (registryName.startsWith("table_umbrella_")) {
      return CsmFabricatorCosts.cost(FabricatorIngredient.any(MC_WOOL, 3),
          FabricatorIngredient.any(MC_IRON_INGOT, 1));
    }
    if (registryName.startsWith("sun_lounger_")) {
      return CsmFabricatorCosts.cost(FabricatorIngredient.any(frame, 3),
          FabricatorIngredient.any(MC_WOOL, 2));
    }
    if (registryName.startsWith("adirondack_chair_")) {
      return CsmFabricatorCosts.cost(FabricatorIngredient.any(MC_PLANKS, 3));
    }
    if (registryName.startsWith("outdoor_sofa_")) {
      return CsmFabricatorCosts.cost(FabricatorIngredient.any(MC_PLANKS, 2),
          FabricatorIngredient.any(MC_WOOL, 3));
    }
    if (registryName.startsWith("outdoor_rug_")) {
      return CsmFabricatorCosts.cost(FabricatorIngredient.any(MC_WOOL, 2));
    }
    if (registryName.startsWith("gas_grill_")) {
      return CsmFabricatorCosts.cost(FabricatorIngredient.part(CsmParts.SHEET_METAL, 3),
          FabricatorIngredient.part(CsmParts.WIRING_HARNESS, 1));
    }
    if (registryName.startsWith("kettle_grill_")) {
      return CsmFabricatorCosts.cost(FabricatorIngredient.part(CsmParts.SHEET_METAL, 2),
          FabricatorIngredient.any(MC_IRON_INGOT, 1));
    }
    if (registryName.startsWith("fire_pit_")) {
      return registryName.endsWith("_steel")
          ? CsmFabricatorCosts.cost(FabricatorIngredient.part(CsmParts.SHEET_METAL, 2))
          : CsmFabricatorCosts.cost(FabricatorIngredient.any(MC_STONE, 4));
    }
    if (registryName.startsWith("cooler_") || registryName.startsWith("kiddie_pool_")) {
      return CsmFabricatorCosts.cost(FabricatorIngredient.part(CsmParts.ENCLOSURE_SHELL, 2));
    }
    if (registryName.startsWith("pool_float_")) {
      return CsmFabricatorCosts.cost(FabricatorIngredient.part(CsmParts.ENCLOSURE_SHELL, 1));
    }
    if (registryName.startsWith("chimney_")) {
      return CsmFabricatorCosts.cost(FabricatorIngredient.any(MC_BRICK, 6));
    }
    if (registryName.startsWith("picket_fence_")) {
      return CsmFabricatorCosts.cost(FabricatorIngredient.any(MC_PLANKS, 2));
    }
    if (registryName.startsWith("picket_gate_")) {
      return CsmFabricatorCosts.cost(FabricatorIngredient.any(MC_PLANKS, 3),
          FabricatorIngredient.part(CsmParts.FASTENER_KIT, 1));
    }
    if (registryName.startsWith("stepping_stones_")) {
      return CsmFabricatorCosts.cost(FabricatorIngredient.any(MC_STONE, 2));
    }
    if (registryName.startsWith("string_lights_")) {
      return CsmFabricatorCosts.cost(FabricatorIngredient.part(CsmParts.LED_MODULE, 1),
          FabricatorIngredient.any(MC_STRING, 2));
    }
    if (registryName.startsWith("trampoline_")) {
      return CsmFabricatorCosts.cost(FabricatorIngredient.any(MC_IRON_INGOT, 2),
          FabricatorIngredient.any(MC_WOOL, 2), FabricatorIngredient.any(MC_SLIME_BALL, 1));
    }
    if (registryName.startsWith("bounce_castle_")) {
      return CsmFabricatorCosts.cost(FabricatorIngredient.any(MC_WOOL, 8),
          FabricatorIngredient.any(MC_SLIME_BALL, 2));
    }
    if (registryName.startsWith("diving_board_")) {
      return CsmFabricatorCosts.cost(FabricatorIngredient.any(MC_PLANKS, 2),
          FabricatorIngredient.any(MC_IRON_INGOT, 1), FabricatorIngredient.any(MC_SLIME_BALL, 1));
    }
    if (registryName.startsWith("pet_bed_")) {
      return CsmFabricatorCosts.cost(FabricatorIngredient.any(MC_WOOL, 2));
    }
    if (registryName.startsWith("pet_bowls_")) {
      return registryName.endsWith("_red")
          ? CsmFabricatorCosts.cost(FabricatorIngredient.any(MC_CLAY, 2))
          : CsmFabricatorCosts.cost(FabricatorIngredient.any(MC_IRON_INGOT, 1));
    }
    if (registryName.startsWith("litter_box_")) {
      return CsmFabricatorCosts.cost(FabricatorIngredient.part(CsmParts.ENCLOSURE_SHELL, 1),
          FabricatorIngredient.any(MC_SAND, 1));
    }
    if (registryName.startsWith("cat_tree_")) {
      return CsmFabricatorCosts.cost(FabricatorIngredient.any(MC_PLANKS, 3),
          FabricatorIngredient.any(MC_WOOL, 3), FabricatorIngredient.any(MC_STRING, 2));
    }
    if (registryName.startsWith("hose_reel_")) {
      return CsmFabricatorCosts.cost(FabricatorIngredient.part(CsmParts.SHEET_METAL, 1),
          FabricatorIngredient.part(CsmParts.FASTENER_KIT, 1));
    }
    if (registryName.startsWith("outdoor_wall_light_")) {
      return CsmFabricatorCosts.cost(FabricatorIngredient.part(CsmParts.LED_MODULE, 1),
          FabricatorIngredient.part(CsmParts.SHEET_METAL, 1));
    }
    return null;
  }
}

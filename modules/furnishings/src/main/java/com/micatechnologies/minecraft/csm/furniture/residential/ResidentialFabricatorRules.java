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
 * porcelain is clay, its rails iron, its laundry appliances priced as the dishwasher.
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
}

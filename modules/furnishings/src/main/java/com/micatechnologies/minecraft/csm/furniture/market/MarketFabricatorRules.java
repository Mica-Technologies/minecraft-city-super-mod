package com.micatechnologies.minecraft.csm.furniture.market;

import com.micatechnologies.minecraft.csm.materials.CsmFabricatorCosts;
import com.micatechnologies.minecraft.csm.materials.CsmParts;
import com.micatechnologies.minecraft.csm.materials.FabricatorIngredient;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.annotation.Nullable;
import net.minecraft.block.Block;

/**
 * What the Fabricator charges for the Market &amp; Store tab: refrigerated displays are sheet
 * steel and glass with their controls and lights; gondola shelving is steel; the produce stand,
 * bulk bins and counters are timber, the checkout's belt with its motor and its scanner with an
 * optical sensor; the terminals and registers are electronics (the old register is brass); carts,
 * the corral and racks are steel wire; the aisle sign is a sign blank. The fresh departments
 * follow the same lines: the butcher, seafood and hot food cases and the floral cooler as the
 * displays, the bread rack, pastry case and coffee station counters as timber, the brewer,
 * fountain and rotisserie as sheet steel with their controls, and the flower stand as iron and
 * the flowers in it. The front of the store likewise: the pharmacy counters as the service desk
 * with a sign, its shelf wall as the gondola, the tobacco case as timber and glass, the lottery
 * pieces as a dispenser of paper and a terminal of electronics, the ice chest as the island
 * freezer without its glass, the propane cage and firewood rack as iron (the rack with its logs),
 * the kiosks as sheet steel with their electronics. The merchandising: end caps as the gondola
 * with a sign blank for the header, pallet stacks as planks and paper, the bins as iron or
 * board, seasonal tables as timber and cloth with a sign, the candy strip as the belt counter
 * less its motor, department signs as two sign blanks, the standing signs as one on a pole, the
 * clothes rails, racks and tables as iron or timber and cloth, mannequins and dress forms as
 * their shells and cloth, and fitting rooms as timber, cloth and a mirror.
 *
 * <p>The blocks that moved in keep what they cost before: the produce crates as the Furniture
 * tab's woodwork, the Verifone as the Technology tab's electronics. Priced by registry name,
 * whose piece is its first words ({@code reach_in_cooler_black}). Written from one table with
 * {@code audit_fabricator_costs.py}'s mirror of it, so the two agree.</p>
 *
 * @since 2026.9
 */
public final class MarketFabricatorRules {

  /** The Market &amp; Store tab. */
  public static final String TAB_ID = "tabmarketstore";

  private static final String MC_PLANKS = "minecraft:planks";
  private static final String MC_GLASS_PANE = "minecraft:glass_pane";
  private static final String MC_IRON_INGOT = "minecraft:iron_ingot";
  private static final String MC_PAPER = "minecraft:paper";
  private static final String MC_GOLD_NUGGET = "minecraft:gold_nugget";
  private static final String MC_DYE = "minecraft:dye";
  private static final String MC_FLOWER = "minecraft:red_flower";
  private static final String MC_LOG = "minecraft:log";
  private static final String MC_WOOL = "minecraft:wool";

  /** The produce crates that moved in from the Furniture tab. */
  private static final Set<String> MOVED_CRATES = new HashSet<>(Arrays.asList(
      "applecrate", "bananacrate", "beetcrate", "carrotbarrel", "carrotcrate", "corncrate",
      "goldenapples", "greenapplecrate", "largecrate", "lettucecrate", "onioncrate",
      "orangecrate", "pearcrate", "potatoecrate", "tomatoecrate"));

  private MarketFabricatorRules() {
  }

  /**
   * Prices a block in the Market &amp; Store tab.
   *
   * @param block        the block
   * @param registryName its registry name
   *
   * @return the cost
   */
  @Nullable
  public static List<FabricatorIngredient> price(Block block, String registryName) {
    if (MOVED_CRATES.contains(registryName)) {
      return CsmFabricatorCosts.cost(FabricatorIngredient.any(MC_PLANKS, 2),
          FabricatorIngredient.part(CsmParts.FASTENER_KIT, 1));
    }
    if ("vf915".equals(registryName)) {
      return CsmFabricatorCosts.cost(FabricatorIngredient.part(CsmParts.CONTROL_BOARD, 1),
          FabricatorIngredient.part(CsmParts.SHEET_METAL, 1),
          FabricatorIngredient.part(CsmParts.WIRING_HARNESS, 1));
    }
    if (registryName.startsWith("reach_in_cooler_")
        || registryName.startsWith("reach_in_freezer_")) {
      return CsmFabricatorCosts.cost(FabricatorIngredient.part(CsmParts.SHEET_METAL, 3),
          FabricatorIngredient.any(MC_GLASS_PANE, 3),
          FabricatorIngredient.part(CsmParts.CONTROL_BOARD, 1),
          FabricatorIngredient.part(CsmParts.LED_MODULE, 1));
    }
    if (registryName.startsWith("dairy_case_")) {
      return CsmFabricatorCosts.cost(FabricatorIngredient.part(CsmParts.SHEET_METAL, 4),
          FabricatorIngredient.part(CsmParts.CONTROL_BOARD, 1),
          FabricatorIngredient.part(CsmParts.LED_MODULE, 1));
    }
    if (registryName.startsWith("island_freezer_")) {
      return CsmFabricatorCosts.cost(FabricatorIngredient.part(CsmParts.SHEET_METAL, 3),
          FabricatorIngredient.any(MC_GLASS_PANE, 2),
          FabricatorIngredient.part(CsmParts.CONTROL_BOARD, 1));
    }
    if (registryName.startsWith("ice_cream_case_")
        || registryName.startsWith("deli_case_")) {
      return CsmFabricatorCosts.cost(FabricatorIngredient.part(CsmParts.SHEET_METAL, 2),
          FabricatorIngredient.any(MC_GLASS_PANE, 3),
          FabricatorIngredient.part(CsmParts.CONTROL_BOARD, 1));
    }
    if (registryName.startsWith("butcher_case_") || registryName.startsWith("seafood_case_")) {
      return CsmFabricatorCosts.cost(FabricatorIngredient.part(CsmParts.SHEET_METAL, 2),
          FabricatorIngredient.any(MC_GLASS_PANE, 3),
          FabricatorIngredient.part(CsmParts.CONTROL_BOARD, 1));
    }
    if (registryName.startsWith("hot_food_case_")) {
      return CsmFabricatorCosts.cost(FabricatorIngredient.part(CsmParts.SHEET_METAL, 2),
          FabricatorIngredient.any(MC_GLASS_PANE, 2),
          FabricatorIngredient.part(CsmParts.WIRING_HARNESS, 1),
          FabricatorIngredient.part(CsmParts.LED_MODULE, 1));
    }
    if (registryName.startsWith("floral_cooler_")) {
      return CsmFabricatorCosts.cost(FabricatorIngredient.part(CsmParts.SHEET_METAL, 3),
          FabricatorIngredient.any(MC_GLASS_PANE, 3),
          FabricatorIngredient.part(CsmParts.CONTROL_BOARD, 1),
          FabricatorIngredient.part(CsmParts.LED_MODULE, 1));
    }
    if (registryName.startsWith("bread_rack_")) {
      return CsmFabricatorCosts.cost(FabricatorIngredient.any(MC_PLANKS, 3),
          FabricatorIngredient.part(CsmParts.FASTENER_KIT, 1));
    }
    if (registryName.startsWith("pastry_case_")) {
      return CsmFabricatorCosts.cost(FabricatorIngredient.any(MC_PLANKS, 2),
          FabricatorIngredient.any(MC_GLASS_PANE, 2),
          FabricatorIngredient.part(CsmParts.FASTENER_KIT, 1));
    }
    if (registryName.startsWith("rotisserie_oven_")) {
      return CsmFabricatorCosts.cost(FabricatorIngredient.part(CsmParts.SHEET_METAL, 2),
          FabricatorIngredient.any(MC_GLASS_PANE, 1),
          FabricatorIngredient.part(CsmParts.WIRING_HARNESS, 1));
    }
    if (registryName.startsWith("flower_stand_")) {
      return CsmFabricatorCosts.cost(FabricatorIngredient.any(MC_IRON_INGOT, 2),
          FabricatorIngredient.any(MC_FLOWER, 3));
    }
    if (registryName.startsWith("coffee_bar_") || registryName.startsWith("cup_counter_")) {
      return CsmFabricatorCosts.cost(FabricatorIngredient.any(MC_PLANKS, 3),
          FabricatorIngredient.part(CsmParts.FASTENER_KIT, 1));
    }
    if (registryName.startsWith("coffee_brewer_")) {
      return CsmFabricatorCosts.cost(FabricatorIngredient.part(CsmParts.SHEET_METAL, 2),
          FabricatorIngredient.part(CsmParts.CONTROL_BOARD, 1),
          FabricatorIngredient.part(CsmParts.WIRING_HARNESS, 1));
    }
    if (registryName.startsWith("fountain_machine_")) {
      return CsmFabricatorCosts.cost(FabricatorIngredient.part(CsmParts.SHEET_METAL, 2),
          FabricatorIngredient.part(CsmParts.CONTROL_BOARD, 1),
          FabricatorIngredient.part(CsmParts.LED_MODULE, 1));
    }
    if (registryName.startsWith("bakery_case_")) {
      return CsmFabricatorCosts.cost(FabricatorIngredient.any(MC_PLANKS, 2),
          FabricatorIngredient.any(MC_GLASS_PANE, 3),
          FabricatorIngredient.part(CsmParts.LED_MODULE, 1));
    }
    if (registryName.startsWith("gondola_shelf_")) {
      return CsmFabricatorCosts.cost(FabricatorIngredient.part(CsmParts.SHEET_METAL, 2),
          FabricatorIngredient.part(CsmParts.FASTENER_KIT, 1));
    }
    if (registryName.startsWith("produce_stand_")) {
      return CsmFabricatorCosts.cost(FabricatorIngredient.any(MC_PLANKS, 3),
          FabricatorIngredient.part(CsmParts.FASTENER_KIT, 1));
    }
    if (registryName.startsWith("produce_scale_")) {
      return CsmFabricatorCosts.cost(FabricatorIngredient.part(CsmParts.SHEET_METAL, 1),
          FabricatorIngredient.any(MC_IRON_INGOT, 1));
    }
    if (registryName.startsWith("bulk_bins_")) {
      return CsmFabricatorCosts.cost(FabricatorIngredient.any(MC_PLANKS, 2),
          FabricatorIngredient.any(MC_GLASS_PANE, 2));
    }
    if (registryName.startsWith("checkout_belt_")) {
      return CsmFabricatorCosts.cost(FabricatorIngredient.any(MC_PLANKS, 3),
          FabricatorIngredient.part(CsmParts.SHEET_METAL, 1),
          FabricatorIngredient.part(CsmParts.WIRING_HARNESS, 1));
    }
    if (registryName.startsWith("checkout_scanner_")) {
      return CsmFabricatorCosts.cost(FabricatorIngredient.any(MC_PLANKS, 3),
          FabricatorIngredient.part(CsmParts.SHEET_METAL, 1),
          FabricatorIngredient.part(CsmParts.OPTICAL_SENSOR, 1));
    }
    if (registryName.startsWith("checkout_bagging_")) {
      return CsmFabricatorCosts.cost(FabricatorIngredient.any(MC_PLANKS, 3),
          FabricatorIngredient.part(CsmParts.SHEET_METAL, 1));
    }
    if (registryName.startsWith("pos_terminal_")) {
      return CsmFabricatorCosts.cost(FabricatorIngredient.part(CsmParts.CONTROL_BOARD, 1),
          FabricatorIngredient.part(CsmParts.LED_MODULE, 1),
          FabricatorIngredient.part(CsmParts.SHEET_METAL, 1));
    }
    if (registryName.startsWith("cash_register_")) {
      return CsmFabricatorCosts.cost(FabricatorIngredient.part(CsmParts.SHEET_METAL, 2),
          FabricatorIngredient.any(MC_GOLD_NUGGET, 2),
          FabricatorIngredient.part(CsmParts.FASTENER_KIT, 1));
    }
    if (registryName.startsWith("receipt_printer_")) {
      return CsmFabricatorCosts.cost(FabricatorIngredient.part(CsmParts.CONTROL_BOARD, 1),
          FabricatorIngredient.any(MC_PAPER, 1));
    }
    if (registryName.startsWith("card_terminal_stand_")) {
      return CsmFabricatorCosts.cost(FabricatorIngredient.part(CsmParts.CONTROL_BOARD, 1),
          FabricatorIngredient.part(CsmParts.LED_MODULE, 1));
    }
    if (registryName.startsWith("bag_carousel_")) {
      return CsmFabricatorCosts.cost(FabricatorIngredient.any(MC_IRON_INGOT, 1),
          FabricatorIngredient.any(MC_PAPER, 2));
    }
    if (registryName.startsWith("self_checkout_")) {
      return CsmFabricatorCosts.cost(FabricatorIngredient.part(CsmParts.SHEET_METAL, 2),
          FabricatorIngredient.part(CsmParts.CONTROL_BOARD, 1),
          FabricatorIngredient.part(CsmParts.LED_MODULE, 1),
          FabricatorIngredient.part(CsmParts.OPTICAL_SENSOR, 1));
    }
    if (registryName.startsWith("service_desk_")) {
      return CsmFabricatorCosts.cost(FabricatorIngredient.any(MC_PLANKS, 5),
          FabricatorIngredient.part(CsmParts.FASTENER_KIT, 1));
    }
    if (registryName.startsWith("impulse_rack_")) {
      return CsmFabricatorCosts.cost(FabricatorIngredient.any(MC_IRON_INGOT, 2),
          FabricatorIngredient.any(MC_PAPER, 1));
    }
    if (registryName.startsWith("shopping_cart_")) {
      return CsmFabricatorCosts.cost(FabricatorIngredient.any(MC_IRON_INGOT, 3));
    }
    if (registryName.startsWith("cart_corral_")) {
      return CsmFabricatorCosts.cost(FabricatorIngredient.any(MC_IRON_INGOT, 3));
    }
    if (registryName.startsWith("basket_stack_")) {
      return CsmFabricatorCosts.cost(FabricatorIngredient.any(MC_PAPER, 2),
          FabricatorIngredient.any(MC_DYE, 1), FabricatorIngredient.any(MC_IRON_INGOT, 1));
    }
    if (registryName.startsWith("security_gate_")) {
      return CsmFabricatorCosts.cost(FabricatorIngredient.part(CsmParts.SHEET_METAL, 1),
          FabricatorIngredient.part(CsmParts.WIRING_HARNESS, 1));
    }
    if (registryName.startsWith("aisle_sign_")) {
      return CsmFabricatorCosts.cost(FabricatorIngredient.part(CsmParts.SIGN_BLANK, 1));
    }
    if (registryName.startsWith("magazine_rack_")) {
      return CsmFabricatorCosts.cost(FabricatorIngredient.any(MC_IRON_INGOT, 1),
          FabricatorIngredient.any(MC_PAPER, 3));
    }
    if (registryName.startsWith("pharmacy_dropoff_")
        || registryName.startsWith("pharmacy_pickup_")) {
      return CsmFabricatorCosts.cost(FabricatorIngredient.any(MC_PLANKS, 5),
          FabricatorIngredient.part(CsmParts.FASTENER_KIT, 1),
          FabricatorIngredient.part(CsmParts.SIGN_BLANK, 1));
    }
    if (registryName.startsWith("pharmacy_shelf_")) {
      return CsmFabricatorCosts.cost(FabricatorIngredient.part(CsmParts.SHEET_METAL, 2),
          FabricatorIngredient.part(CsmParts.FASTENER_KIT, 1));
    }
    if (registryName.startsWith("pharmacy_sign_")
        || registryName.startsWith("consultation_sign_")) {
      return CsmFabricatorCosts.cost(FabricatorIngredient.part(CsmParts.SIGN_BLANK, 1));
    }
    if (registryName.startsWith("tobacco_case_")) {
      return CsmFabricatorCosts.cost(FabricatorIngredient.any(MC_PLANKS, 2),
          FabricatorIngredient.any(MC_GLASS_PANE, 2),
          FabricatorIngredient.part(CsmParts.FASTENER_KIT, 1));
    }
    if (registryName.startsWith("lottery_dispenser_")) {
      return CsmFabricatorCosts.cost(FabricatorIngredient.any(MC_GLASS_PANE, 1),
          FabricatorIngredient.any(MC_PAPER, 2));
    }
    if (registryName.startsWith("lottery_terminal_")) {
      return CsmFabricatorCosts.cost(FabricatorIngredient.part(CsmParts.CONTROL_BOARD, 1),
          FabricatorIngredient.part(CsmParts.LED_MODULE, 1),
          FabricatorIngredient.any(MC_PAPER, 1));
    }
    if (registryName.startsWith("ice_merchandiser_")) {
      return CsmFabricatorCosts.cost(FabricatorIngredient.part(CsmParts.SHEET_METAL, 3),
          FabricatorIngredient.part(CsmParts.CONTROL_BOARD, 1));
    }
    if (registryName.startsWith("propane_cage_")) {
      return CsmFabricatorCosts.cost(FabricatorIngredient.any(MC_IRON_INGOT, 3),
          FabricatorIngredient.part(CsmParts.SHEET_METAL, 2));
    }
    if (registryName.startsWith("firewood_rack_")) {
      return CsmFabricatorCosts.cost(FabricatorIngredient.any(MC_IRON_INGOT, 2),
          FabricatorIngredient.any(MC_LOG, 2));
    }
    if (registryName.startsWith("coin_kiosk_") || registryName.startsWith("dvd_kiosk_")) {
      return CsmFabricatorCosts.cost(FabricatorIngredient.part(CsmParts.SHEET_METAL, 3),
          FabricatorIngredient.part(CsmParts.CONTROL_BOARD, 1),
          FabricatorIngredient.part(CsmParts.LED_MODULE, 1));
    }
    if (registryName.startsWith("photo_kiosk_")) {
      return CsmFabricatorCosts.cost(FabricatorIngredient.part(CsmParts.SHEET_METAL, 2),
          FabricatorIngredient.part(CsmParts.CONTROL_BOARD, 1),
          FabricatorIngredient.part(CsmParts.LED_MODULE, 1),
          FabricatorIngredient.any(MC_PAPER, 1));
    }
    if (registryName.startsWith("bottle_return_machine_")) {
      return CsmFabricatorCosts.cost(FabricatorIngredient.part(CsmParts.SHEET_METAL, 3),
          FabricatorIngredient.part(CsmParts.CONTROL_BOARD, 1),
          FabricatorIngredient.part(CsmParts.OPTICAL_SENSOR, 1));
    }
    if (registryName.startsWith("end_cap_")) {
      return CsmFabricatorCosts.cost(FabricatorIngredient.part(CsmParts.SHEET_METAL, 2),
          FabricatorIngredient.part(CsmParts.FASTENER_KIT, 1),
          FabricatorIngredient.part(CsmParts.SIGN_BLANK, 1));
    }
    if (registryName.startsWith("pallet_stack_")) {
      return CsmFabricatorCosts.cost(FabricatorIngredient.any(MC_PLANKS, 2),
          FabricatorIngredient.any(MC_PAPER, 2));
    }
    if (registryName.startsWith("bargain_bin_")) {
      return CsmFabricatorCosts.cost(FabricatorIngredient.any(MC_IRON_INGOT, 3),
          FabricatorIngredient.part(CsmParts.SIGN_BLANK, 1));
    }
    if (registryName.startsWith("dump_bin_")) {
      return CsmFabricatorCosts.cost(FabricatorIngredient.any(MC_PAPER, 4));
    }
    if (registryName.startsWith("seasonal_table_")) {
      return CsmFabricatorCosts.cost(FabricatorIngredient.any(MC_PLANKS, 2),
          FabricatorIngredient.any(MC_WOOL, 1), FabricatorIngredient.part(CsmParts.SIGN_BLANK, 1));
    }
    if (registryName.startsWith("checkout_candy_")) {
      return CsmFabricatorCosts.cost(FabricatorIngredient.any(MC_PLANKS, 3),
          FabricatorIngredient.part(CsmParts.SHEET_METAL, 1),
          FabricatorIngredient.any(MC_IRON_INGOT, 1));
    }
    if (registryName.startsWith("department_sign_")) {
      return CsmFabricatorCosts.cost(FabricatorIngredient.part(CsmParts.SIGN_BLANK, 2));
    }
    if (registryName.startsWith("sale_sign_") || registryName.startsWith("deal_sign_")) {
      return CsmFabricatorCosts.cost(FabricatorIngredient.part(CsmParts.SIGN_BLANK, 1),
          FabricatorIngredient.any(MC_IRON_INGOT, 1));
    }
    if (registryName.startsWith("clothing_rail_") || registryName.startsWith("round_rack_")) {
      return CsmFabricatorCosts.cost(FabricatorIngredient.any(MC_IRON_INGOT, 2),
          FabricatorIngredient.any(MC_WOOL, 2));
    }
    if (registryName.startsWith("apparel_table_")) {
      return CsmFabricatorCosts.cost(FabricatorIngredient.any(MC_PLANKS, 3),
          FabricatorIngredient.any(MC_WOOL, 1));
    }
    if (registryName.startsWith("mannequin_")) {
      return CsmFabricatorCosts.cost(FabricatorIngredient.part(CsmParts.SHEET_METAL, 1),
          FabricatorIngredient.any(MC_WOOL, 1));
    }
    if (registryName.startsWith("dress_form_")) {
      return CsmFabricatorCosts.cost(FabricatorIngredient.any(MC_WOOL, 2),
          FabricatorIngredient.any(MC_PLANKS, 1));
    }
    if (registryName.startsWith("fitting_room_")) {
      return CsmFabricatorCosts.cost(FabricatorIngredient.any(MC_PLANKS, 4),
          FabricatorIngredient.any(MC_WOOL, 2), FabricatorIngredient.any(MC_GLASS_PANE, 1));
    }
    return CsmFabricatorCosts.cost(FabricatorIngredient.any(MC_PLANKS, 2),
        FabricatorIngredient.part(CsmParts.FASTENER_KIT, 1));
  }
}

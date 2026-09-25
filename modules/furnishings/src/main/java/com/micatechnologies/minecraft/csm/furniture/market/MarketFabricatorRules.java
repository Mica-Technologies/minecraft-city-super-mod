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
 * the corral and racks are steel wire; the aisle sign is a sign blank.
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
    if (registryName.startsWith("bottle_return_machine_")) {
      return CsmFabricatorCosts.cost(FabricatorIngredient.part(CsmParts.SHEET_METAL, 3),
          FabricatorIngredient.part(CsmParts.CONTROL_BOARD, 1),
          FabricatorIngredient.part(CsmParts.OPTICAL_SENSOR, 1));
    }
    return CsmFabricatorCosts.cost(FabricatorIngredient.any(MC_PLANKS, 2),
        FabricatorIngredient.part(CsmParts.FASTENER_KIT, 1));
  }
}

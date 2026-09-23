package com.micatechnologies.minecraft.csm.parks;

import com.micatechnologies.minecraft.csm.materials.CsmFabricatorCosts;
import com.micatechnologies.minecraft.csm.materials.CsmParts;
import com.micatechnologies.minecraft.csm.materials.FabricatorIngredient;
import com.micatechnologies.minecraft.csm.parks.trees.BlockHangingMoss;
import com.micatechnologies.minecraft.csm.parks.trees.BlockTreeLeaves;
import com.micatechnologies.minecraft.csm.parks.trees.BlockTreeLog;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.block.Block;

/**
 * What the Fabricator charges for the Parks &amp; Greenery tabs.
 *
 * <p>Trees &amp; Plants: wood for wood, and a planting costs what it is planted with.</p>
 *
 * <p>A log costs planks in proportion to how much wood it is -- a twig one plank, a full-width
 * log four -- so building a tree in survival costs about what the wood in it would. Leaves cost
 * vanilla leaves (a palm crown, a whole head of fronds, two), and hanging moss a vine.</p>
 *
 * @since 2026.9
 */
public final class ParksFabricatorRules {

  /** The Trees &amp; Plants tab, priced by {@link #price}. */
  public static final String TAB_ID = "tabtreesplants";
  /** The Parks tab, priced by {@link #priceParks}. */
  public static final String PARKS_TAB_ID = "tabparks";

  private static final String MC_PLANKS = "minecraft:planks";
  private static final String MC_LEAVES = "minecraft:leaves";
  private static final String MC_VINE = "minecraft:vine";
  private static final String MC_CLAY_BALL = "minecraft:clay_ball";
  private static final String MC_STONE = "minecraft:stone";
  private static final String MC_SAPLING = "minecraft:sapling";
  private static final String MC_TALLGRASS = "minecraft:tallgrass";
  private static final String MC_FLOWER = "minecraft:red_flower";
  private static final String MC_GRAVEL = "minecraft:gravel";
  private static final String MC_SAND = "minecraft:sand";
  private static final String MC_WOOL = "minecraft:wool";
  private static final String MC_STICK = "minecraft:stick";

  private ParksFabricatorRules() {
  }

  /**
   * Prices a block in the Trees &amp; Plants tab.
   *
   * @param block        the block
   * @param registryName its registry name
   *
   * @return the cost, or null for the generic cost
   */
  @Nullable
  public static List<FabricatorIngredient> price(Block block, String registryName) {
    if (block instanceof BlockTreeLog) {
      int planks;
      switch (((BlockTreeLog) block).getWidth()) {
        case TWIG:
        case THIN:
          planks = 1;
          break;
        case MEDIUM:
          planks = 2;
          break;
        case THICK:
          planks = 3;
          break;
        default:
          planks = 4;
          break;
      }
      return CsmFabricatorCosts.cost(FabricatorIngredient.any(MC_PLANKS, planks));
    }
    if (block instanceof BlockTreeLeaves) {
      int leaves = ((BlockTreeLeaves) block).getLeafType().isPalm() ? 2 : 1;
      return CsmFabricatorCosts.cost(FabricatorIngredient.any(MC_LEAVES, leaves));
    }
    if (block instanceof BlockHangingMoss) {
      return CsmFabricatorCosts.cost(FabricatorIngredient.any(MC_VINE, 1));
    }
    return plantingCost(registryName);
  }

  /**
   * The street tree accessories and plantings, by name. Grates and the pit fence are cast and
   * bent steel, the generic cost (null).
   */
  @Nullable
  private static List<FabricatorIngredient> plantingCost(String name) {
    if (name.startsWith("hedge_")) {
      return CsmFabricatorCosts.cost(FabricatorIngredient.any(MC_LEAVES, 2));
    }
    if (name.startsWith("shrub_")) {
      return CsmFabricatorCosts.cost(FabricatorIngredient.any(MC_SAPLING, 1));
    }
    if (name.startsWith("grass_")) {
      return CsmFabricatorCosts.cost(FabricatorIngredient.any(MC_TALLGRASS, 1));
    }
    if (name.startsWith("flower_bed_") || name.startsWith("hanging_basket_")) {
      return CsmFabricatorCosts.cost(FabricatorIngredient.any(MC_FLOWER, 2));
    }
    if (name.equals("tree_stake")) {
      return CsmFabricatorCosts.cost(FabricatorIngredient.any(MC_STICK, 2));
    }
    if (name.equals("ground_pea_gravel")) {
      return CsmFabricatorCosts.cost(FabricatorIngredient.any(MC_GRAVEL, 1));
    }
    if (name.equals("ground_decomposed_granite")) {
      return CsmFabricatorCosts.cost(FabricatorIngredient.any(MC_SAND, 1));
    }
    if (name.equals("ground_turf")) {
      return CsmFabricatorCosts.cost(FabricatorIngredient.any(MC_WOOL, 1));
    }
    if (name.equals("ground_mulch") || name.equals("tree_pit_mulch")) {
      return CsmFabricatorCosts.cost(FabricatorIngredient.any(MC_PLANKS, 1));
    }
    if (name.endsWith("_wood") && (name.startsWith("planter_") || name.startsWith("raised_bed_"))) {
      return CsmFabricatorCosts.cost(FabricatorIngredient.any(MC_PLANKS, 3));
    }
    if (name.endsWith("_concrete")) {
      return CsmFabricatorCosts.cost(FabricatorIngredient.any(MC_STONE, 3));
    }
    return null;
  }

  /**
   * Prices a block in the Parks tab. Planters are fired clay, a bird bath and the fountains cast
   * stone, wooden benches, tables and the pergola timber, the irrigation controller a control
   * board; the playground, bins, steel furniture and drinking fountains are steel, the generic
   * cost.
   *
   * @param block        the block
   * @param registryName its registry name
   *
   * @return the cost, or null for the generic cost
   */
  @Nullable
  public static List<FabricatorIngredient> priceParks(Block block, String registryName) {
    if (registryName.endsWith("flowerpot")) {
      return CsmFabricatorCosts.cost(FabricatorIngredient.any(MC_CLAY_BALL, 3));
    }
    if (registryName.equals("birdbath") || registryName.startsWith("fountain_")) {
      return CsmFabricatorCosts.cost(FabricatorIngredient.any(MC_STONE, 3));
    }
    if (registryName.startsWith("gazebo_roof_")) {
      return CsmFabricatorCosts.cost(FabricatorIngredient.any(MC_PLANKS, 8));
    }
    if (registryName.endsWith("_wood") || registryName.startsWith("pergola_")
        || registryName.startsWith("gazebo_")) {
      return CsmFabricatorCosts.cost(FabricatorIngredient.any(MC_PLANKS, 3));
    }
    if (registryName.equals("irrigation_controller")) {
      return CsmFabricatorCosts.cost(FabricatorIngredient.part(CsmParts.CONTROL_BOARD, 1),
          FabricatorIngredient.part(CsmParts.SHEET_METAL, 1));
    }
    if (registryName.equals("ground_rubber_safety")) {
      return CsmFabricatorCosts.cost(FabricatorIngredient.any(MC_CLAY_BALL, 1));
    }
    return null;
  }
}

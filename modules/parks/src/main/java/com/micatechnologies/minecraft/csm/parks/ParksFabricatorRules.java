package com.micatechnologies.minecraft.csm.parks;

import com.micatechnologies.minecraft.csm.materials.CsmFabricatorCosts;
import com.micatechnologies.minecraft.csm.materials.FabricatorIngredient;
import com.micatechnologies.minecraft.csm.parks.trees.BlockHangingMoss;
import com.micatechnologies.minecraft.csm.parks.trees.BlockTreeLeaves;
import com.micatechnologies.minecraft.csm.parks.trees.BlockTreeLog;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.block.Block;

/**
 * What the Fabricator charges for the Trees &amp; Plants tab: wood for wood.
 *
 * <p>A log costs planks in proportion to how much wood it is -- a twig one plank, a full-width
 * log four -- so building a tree in survival costs about what the wood in it would. Leaves cost
 * vanilla leaves (a palm crown, a whole head of fronds, two), and hanging moss a vine.</p>
 *
 * @since 2026.9
 */
public final class ParksFabricatorRules {

  /** The tab these rules price. */
  public static final String TAB_ID = "tabtreesplants";

  private static final String MC_PLANKS = "minecraft:planks";
  private static final String MC_LEAVES = "minecraft:leaves";
  private static final String MC_VINE = "minecraft:vine";

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
    return null;
  }
}

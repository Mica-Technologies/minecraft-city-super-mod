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
 * <p>Everything in the tab today is fare equipment: a powered cabinet with a reader, a board and
 * its wiring. It costs what it cost in the Technology tab it came from (a control board, sheet
 * metal and a wiring harness), so moving it changed no price. Add branches here as the tab grows
 * things that are not electronics -- a shelter is glass and steel, a platform edge concrete.</p>
 *
 * @since 2026.9
 */
public final class TransitFabricatorRules {

  /** The Transit tab, priced by {@link #price}. */
  public static final String TAB_ID = "tabtransit";

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
    return CsmFabricatorCosts.cost(FabricatorIngredient.part(CsmParts.CONTROL_BOARD, 1),
        FabricatorIngredient.part(CsmParts.SHEET_METAL, 1),
        FabricatorIngredient.part(CsmParts.WIRING_HARNESS, 1));
  }
}

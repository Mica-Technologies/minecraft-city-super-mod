package com.micatechnologies.minecraft.csm.streetscape;

import com.micatechnologies.minecraft.csm.materials.CsmFabricatorCosts;
import com.micatechnologies.minecraft.csm.materials.CsmParts;
import com.micatechnologies.minecraft.csm.materials.FabricatorIngredient;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.block.Block;

/**
 * Fabricator costs for the Streetscape tab.
 *
 * <p>Covers are cast iron, so they cost iron rather than the sheet metal and fasteners a bracket
 * does; the ones set in a concrete box add concrete, and the storm drain marker is a printed
 * plaque. A pad-mount transformer is sheet metal, wiring and its concrete pad. Anything this does
 * not know (the hydrant, the delineators, the other utility boxes) returns {@code null} and takes
 * the generic cost, which is what the hydrant and delineators cost before they moved here.</p>
 *
 * @version 1.0
 */
public final class StreetscapeFabricatorRules {

  /**
   * The Streetscape tab's id, as {@code CsmTabStreetscape.getTabId()} returns it.
   */
  public static final String TAB_ID = "tabstreetscape";

  private static final String MC_IRON_INGOT = "minecraft:iron_ingot";

  private StreetscapeFabricatorRules() {
  }

  /**
   * Prices a block in the Streetscape tab.
   *
   * @param block        the block
   * @param registryName its registry name
   *
   * @return the ingredients, or {@code null} for the generic cost
   */
  @Nullable
  public static List<FabricatorIngredient> price(Block block, String registryName) {
    if (block instanceof BlockParkingMeter) {
      // A digital meter or pay station is electronics in a steel case; a mechanical one is
      // clockwork, and takes the generic cost.
      if (((BlockParkingMeter) block).getKind() != BlockParkingMeter.Kind.MECHANICAL) {
        return CsmFabricatorCosts.cost(FabricatorIngredient.part(CsmParts.CONTROL_BOARD, 1),
            FabricatorIngredient.part(CsmParts.SHEET_METAL, 1));
      }
      return null;
    }
    if (registryName.equals("parking_pay_by_phone_sign")) {
      return CsmFabricatorCosts.cost(FabricatorIngredient.part(CsmParts.SIGN_BLANK, 1));
    }
    if (block instanceof BlockUtilityBox) {
      // A transformer is a steel cabinet full of windings on a concrete pad; every other box
      // is an enclosure, and takes the generic sheet metal and fasteners.
      if (registryName.startsWith("transformer")) {
        return CsmFabricatorCosts.cost(FabricatorIngredient.part(CsmParts.SHEET_METAL, 2),
            FabricatorIngredient.part(CsmParts.WIRING_HARNESS, 1),
            FabricatorIngredient.part(CsmParts.CONCRETE_MIX, 1));
      }
      // A cluster box unit is two blocks of steel cabinet and fourteen locks.
      if (registryName.startsWith("mailbox_cluster")) {
        return CsmFabricatorCosts.cost(FabricatorIngredient.part(CsmParts.SHEET_METAL, 2),
            FabricatorIngredient.part(CsmParts.FASTENER_KIT, 1));
      }
      // The concrete sphere is cast concrete through and through.
      if (registryName.startsWith("bollard_sphere")) {
        return CsmFabricatorCosts.cost(FabricatorIngredient.part(CsmParts.CONCRETE_MIX, 2));
      }
      return null;
    }
    if (!(block instanceof BlockStreetCover)) {
      return null;
    }
    if (registryName.startsWith("storm_drain_marker")) {
      return CsmFabricatorCosts.cost(FabricatorIngredient.part(CsmParts.SIGN_BLANK, 1));
    }
    if (registryName.startsWith("vault_lid") || registryName.startsWith("valve_box")
        || registryName.startsWith("sewer_cleanout")) {
      return CsmFabricatorCosts.cost(FabricatorIngredient.any(MC_IRON_INGOT, 1),
          FabricatorIngredient.part(CsmParts.CONCRETE_MIX, 1));
    }
    return CsmFabricatorCosts.cost(FabricatorIngredient.any(MC_IRON_INGOT, 2));
  }
}

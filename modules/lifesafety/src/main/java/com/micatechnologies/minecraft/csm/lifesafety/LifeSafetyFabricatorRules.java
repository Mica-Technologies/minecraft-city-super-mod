package com.micatechnologies.minecraft.csm.lifesafety;

import com.micatechnologies.minecraft.csm.materials.CsmBlockDisplayNames;
import com.micatechnologies.minecraft.csm.materials.CsmFabricatorCosts;
import com.micatechnologies.minecraft.csm.materials.CsmParts;
import com.micatechnologies.minecraft.csm.materials.FabricatorIngredient;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.block.Block;

/**
 * The Fabricator cost rule for the Life Safety tab, registered with
 * {@link CsmFabricatorCosts} from {@link CsmLifeSafety}'s pre-initialization.
 *
 * <p>The rule lives here rather than in Core because it is decided from this subsystem's own
 * class hierarchy. With this module absent, its blocks are absent too, and any block that somehow
 * reached the tab would take Core's generic equipment cost — the same cost this rule ends on.
 *
 * @see CsmFabricatorCosts
 * @since 2026.9
 */
public final class LifeSafetyFabricatorRules {

  /**
   * The creative tab this rule prices. Must match {@code CsmTabLifeSafety.getTabId()}.
   *
   * @since 2026.9
   */
  public static final String TAB_ID = "tablifesafety";

  /**
   * Private constructor: this class is a static rule holder and is never instantiated.
   *
   * @since 2026.9
   */
  private LifeSafetyFabricatorRules() {
    throw new UnsupportedOperationException("LifeSafetyFabricatorRules is a utility class.");
  }

  /**
   * Prices fire alarm and life safety equipment by appliance type.
   *
   * <p><b>Order matters, most specific first.</b> {@code AbstractBlockFireAlarmDetector} extends
   * {@code AbstractBlockFireAlarmActivator}, and {@code AbstractBlockFireAlarmSounderVoiceEvac}
   * extends {@code AbstractBlockFireAlarmSounder}, so testing the parent first would swallow the
   * subclass and leave its branch unreachable. Detectors were previously priced as pull stations
   * for exactly that reason.</p>
   *
   * @param block        the block to price
   * @param registryName the block's registry name, for display-name lookups
   *
   * @return the ingredients, or {@code null} for anything else in the tab, which takes Core's
   *     generic equipment cost
   *
   * @since 2026.9
   */
  @Nullable
  public static List<FabricatorIngredient> price(Block block, String registryName) {
    // Sprinklers are classed as detectors but are a glass bulb on a brass body, not an
    // electronic sensor.
    if (CsmBlockDisplayNames.hasWord(registryName, "sprinkler")) {
      return CsmFabricatorCosts.cost(FabricatorIngredient.part(CsmParts.SHEET_METAL, 1),
          FabricatorIngredient.part(CsmParts.LENS_ASSEMBLY, 1));
    }
    // Detectors sense; check before activators, which they extend.
    if (block instanceof AbstractBlockFireAlarmDetector) {
      return CsmFabricatorCosts.cost(FabricatorIngredient.part(CsmParts.OPTICAL_SENSOR, 1),
          FabricatorIngredient.part(CsmParts.CONTROL_BOARD, 1));
    }
    if (block instanceof AbstractBlockFireAlarmActivator) {
      return CsmFabricatorCosts.cost(FabricatorIngredient.part(CsmParts.CONTROL_BOARD, 1),
          FabricatorIngredient.part(CsmParts.SHEET_METAL, 1));
    }
    // Voice evac speakers are a kind of sounder and cost the same, so one check covers both.
    if (block instanceof AbstractBlockFireAlarmSounder) {
      return CsmFabricatorCosts.cost(FabricatorIngredient.part(CsmParts.SOUNDER_DRIVER, 1),
          FabricatorIngredient.part(CsmParts.ENCLOSURE_SHELL, 1));
    }
    // Panels, exit signs and the rest: Core's generic equipment cost, which is what this branch
    // returned directly before the rule moved out of Core.
    return null;
  }
}

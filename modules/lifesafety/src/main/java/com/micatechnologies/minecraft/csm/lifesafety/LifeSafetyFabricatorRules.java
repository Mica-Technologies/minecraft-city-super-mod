package com.micatechnologies.minecraft.csm.lifesafety;

import com.micatechnologies.minecraft.csm.materials.CsmBlockDisplayNames;
import com.micatechnologies.minecraft.csm.materials.CsmFabricatorCosts;
import com.micatechnologies.minecraft.csm.materials.CsmParts;
import com.micatechnologies.minecraft.csm.materials.FabricatorIngredient;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.block.Block;

/**
 * The Fabricator cost rules for the Life Safety tabs, registered with
 * {@link CsmFabricatorCosts} from {@link CsmLifeSafety}'s pre-initialization: {@link #price} for
 * the fire alarm tab, {@link #priceExits} and {@link #priceFireProtection} for the two tabs split
 * out of it.
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

  /** The Exits &amp; Emergency Lighting tab, priced by {@link #priceExits}. */
  public static final String EXITS_TAB_ID = "tabexitsemergency";

  /** The Fire Protection tab, priced by {@link #priceFireProtection}. */
  public static final String FIRE_PROTECTION_TAB_ID = "tabfireprotection";

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

  /**
   * Prices the Exits &amp; Emergency Lighting tab. Exit signs and emergency lights cost what they
   * did in the one Life Safety tab: steel and a wiring harness, Core's equipment cost.
   *
   * @param block        the block to price
   * @param registryName the block's registry name
   *
   * @return the ingredients
   */
  public static List<FabricatorIngredient> priceExits(Block block, String registryName) {
    return CsmFabricatorCosts.cost(FabricatorIngredient.part(CsmParts.SHEET_METAL, 1),
        FabricatorIngredient.part(CsmParts.WIRING_HARNESS, 1));
  }

  /**
   * Prices the Fire Protection tab. Sprinklers, the gong (a sounder) and anything else of the
   * fire alarm's own kinds keep their price from {@link #price}. The rest by name: a sign plate is
   * a sign blank, a cabinet steel and fixings (an AED's also its electronics), an extinguisher two
   * sheets of steel, a valve, pipe or connection steel and fixings. Anything else, such as the door
   * holders, takes steel and a wiring harness, as it would have in the one Life Safety tab.
   *
   * @param block        the block to price
   * @param registryName the block's registry name
   *
   * @return the ingredients
   */
  public static List<FabricatorIngredient> priceFireProtection(Block block, String registryName) {
    List<FabricatorIngredient> priced = price(block, registryName);
    if (priced != null) {
      return priced;
    }
    if (CsmBlockDisplayNames.hasWord(registryName, "sign")) {
      return CsmFabricatorCosts.cost(FabricatorIngredient.part(CsmParts.SIGN_BLANK, 1));
    }
    if (CsmBlockDisplayNames.hasWord(registryName, "aed")) {
      return CsmFabricatorCosts.cost(FabricatorIngredient.part(CsmParts.SHEET_METAL, 1),
          FabricatorIngredient.part(CsmParts.CONTROL_BOARD, 1),
          FabricatorIngredient.part(CsmParts.WIRING_HARNESS, 1));
    }
    if (CsmBlockDisplayNames.hasWord(registryName, "cabinet")) {
      return CsmFabricatorCosts.cost(FabricatorIngredient.part(CsmParts.SHEET_METAL, 2),
          FabricatorIngredient.part(CsmParts.FASTENER_KIT, 1));
    }
    if (CsmBlockDisplayNames.hasWord(registryName, "extinguisher")) {
      return CsmFabricatorCosts.cost(FabricatorIngredient.part(CsmParts.SHEET_METAL, 2));
    }
    if (CsmBlockDisplayNames.hasWord(registryName, "valve")
        || CsmBlockDisplayNames.hasWord(registryName, "connection")
        || CsmBlockDisplayNames.hasWord(registryName, "riser")
        || CsmBlockDisplayNames.hasWord(registryName, "preventer")
        || CsmBlockDisplayNames.hasWord(registryName, "box")) {
      return CsmFabricatorCosts.cost(FabricatorIngredient.part(CsmParts.SHEET_METAL, 1),
          FabricatorIngredient.part(CsmParts.FASTENER_KIT, 1));
    }
    return CsmFabricatorCosts.cost(FabricatorIngredient.part(CsmParts.SHEET_METAL, 1),
        FabricatorIngredient.part(CsmParts.WIRING_HARNESS, 1));
  }
}

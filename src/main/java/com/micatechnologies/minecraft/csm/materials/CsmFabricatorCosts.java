package com.micatechnologies.minecraft.csm.materials;

import com.micatechnologies.minecraft.csm.codeutils.AbstractBlockFence;
import com.micatechnologies.minecraft.csm.codeutils.AbstractBlockSlab;
import com.micatechnologies.minecraft.csm.codeutils.AbstractBlockStairs;
import com.micatechnologies.minecraft.csm.codeutils.CsmTab;
import com.micatechnologies.minecraft.csm.codeutils.ICsmBlock;
import com.micatechnologies.minecraft.csm.lifesafety.AbstractBlockFireAlarmActivator;
import com.micatechnologies.minecraft.csm.lifesafety.AbstractBlockFireAlarmDetector;
import com.micatechnologies.minecraft.csm.lifesafety.AbstractBlockFireAlarmSounder;
import com.micatechnologies.minecraft.csm.trafficsignals.BlockTrafficSignalController;
import com.micatechnologies.minecraft.csm.trafficsignals.logic.AbstractBlockControllableSignal;
import com.micatechnologies.minecraft.csm.trafficsignals.logic.AbstractBlockTrafficSignalSensor;
import com.micatechnologies.minecraft.csm.trafficsignals.logic.AbstractBlockTrafficSignalSensorHZEight;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.block.Block;
import net.minecraft.creativetab.CreativeTabs;

/**
 * The part cost of every City Super Mod block, for the {@link BlockCsmFabricator}.
 *
 * <p><b>Why this is computed rather than tabulated.</b> The mod has over 1,500 blocks but only
 * fourteen {@link CsmParts}. Grouping blocks by every attribute a cost table can distinguish —
 * creative tab plus base class — yields only 47 groups, so 846 of the 852 non-sign blocks would
 * have shared ingredients with at least one other block. Minecraft resolves identical recipes to
 * whichever loaded first, so a per-block recipe file would have silently left the great majority
 * of the mod uncraftable. The Fabricator selects its output explicitly instead of inferring it
 * from ingredients, so a cost only has to be <i>fair</i>, not <i>unique</i>.</p>
 *
 * <h3>What identifies a block</h3>
 *
 * <p>Costs are decided from the block's <b>English display name</b> (via
 * {@link CsmBlockDisplayNames}) and its base class — never from its registry name. Registry names
 * in this mod are not descriptive and must not be treated as if they were: the lighting subsystem
 * uses opaque abbreviations like {@code novtm}, {@code rcpb} and {@code ocpm} for a tapered mast
 * and two concrete poles, {@code pcc} is a popcorn ceiling rather than anything concrete, and ids
 * have historically been <i>reused</i> for different blocks to avoid bloating existing worlds. The
 * display name is the only text that reliably says what a block is.</p>
 *
 * <p>The specific signal used is the <b>last word of the display name</b>, which in this mod's
 * naming is reliably the noun naming the thing: "NOV Tapered Mast" is a mast, "Post Light" is a
 * light, "Pole-Mount Variable Speed Limit Sign" is a sign, and "Christmas Tree" is a tree.
 * Parenthetical qualifiers are stripped first, so "NOV Round Concrete Pole (Base 1)" is a pole
 * rather than a base.</p>
 *
 * <h3>Rule order</h3>
 * <ol>
 *   <li>Poles, masts and crossarms — structural, whatever subsystem they are filed under. This is
 *       what stops street-light masts costing an LED module.</li>
 *   <li>Road signs take a sign blank.</li>
 *   <li>Building materials, priced from base class, with coloured metal taking its matching dye.</li>
 *   <li>Mounting hardware — mounts, brackets, backplates, covers — takes sheet metal and
 *       fasteners rather than the electronics of its subsystem.</li>
 *   <li>Optical devices take a sensing element.</li>
 *   <li>Equipment with a dedicated base class: fire alarm appliances, signal heads, detection
 *       sensors, the controller cabinet.</li>
 *   <li>Everything else takes its subsystem default.</li>
 * </ol>
 *
 * <p>A {@code null} return means "not fabricable". That covers non-CSM blocks and every block
 * registered to {@code CsmTabNone}: hidden and retiring blocks are given a {@code null} creative
 * tab by {@code CsmTab}, so they fall out here without a separate exclusion list.</p>
 *
 * @author Mica Technologies
 * @since 2026.7
 */
public final class CsmFabricatorCosts {

  private static final String TAB_BUILDING_MATERIALS = "tabbuildingmaterials";
  private static final String TAB_FURNITURE = "tabfurniture";
  private static final String TAB_GAMING = "tabgaming";
  private static final String TAB_HVAC = "tabhvac";
  private static final String TAB_LIFE_SAFETY = "tablifesafety";
  private static final String TAB_LIGHTING = "tablighting";
  private static final String TAB_MATERIALS = "tabmaterials";
  private static final String TAB_NOVELTIES = "tabnovelties";
  private static final String TAB_POWER_GRID = "tabpowergrid";
  private static final String TAB_ROAD_SIGNS = "tabroadsigns";
  private static final String TAB_TECHNOLOGY = "tabtechnology";
  private static final String TAB_TRAFFIC_ACCESSORIES = "tabtrafficaccessories";
  private static final String TAB_TRAFFIC_SIGNALS = "tabtrafficsignals";

  private static final String MC_PLANKS = "minecraft:planks";
  private static final String MC_DYE = "minecraft:dye";
  private static final String MC_CLAY_BALL = "minecraft:clay_ball";
  private static final String MC_PAPER = "minecraft:paper";
  private static final String MC_PRISMARINE_CRYSTALS = "minecraft:prismarine_crystals";

  /** Display-name nouns meaning the block is a vertical structural member. */
  private static final String[] POLE_NOUNS = {"pole", "mast", "crossarm", "standard", "post"};

  /** Display-name nouns meaning the block is mounting or enclosure hardware, not a device. */
  private static final String[] MOUNT_NOUNS = {
      "mount", "bracket", "backplate", "cover", "visor", "clamp", "hanger", "adapter", "coupler",
      "cap", "top", "base", "plate", "arm",
  };

  /** Display-name words meaning the block senses optically. */
  private static final String[] OPTICAL_WORDS = {"camera", "alpr", "radar", "lidar"};

  /** Display-name words for furniture that is plainly metal rather than timber. */
  private static final String[] METAL_FURNITURE_WORDS = {
      "hydrant", "anchor", "chain", "chains", "barbed", "radiator", "grill", "grate", "rail",
  };

  /** Display-name words for novelties that are actually electronics. */
  private static final String[] ELECTRONIC_NOVELTY_WORDS = {
      "record", "player", "jukebox", "radio", "television", "tv",
  };

  private CsmFabricatorCosts() {
    throw new UnsupportedOperationException("CsmFabricatorCosts is a utility class.");
  }

  /**
   * Returns the ingredients needed to fabricate one of the given block, or {@code null} if the
   * block cannot be fabricated.
   *
   * @param block the block to price
   *
   * @return an immutable ingredient list, or {@code null}
   *
   * @since 2026.7
   */
  @Nullable
  public static List<FabricatorIngredient> getCost(Block block) {
    if (!(block instanceof ICsmBlock)) {
      return null;
    }
    CreativeTabs tab = block.getCreativeTab();
    if (tab == null) {
      // Hidden / retiring blocks (CsmTabNone) get a null tab and are deliberately not fabricable.
      // The IDE flags this condition as always false because the MCP mappings do not mark
      // Block.getCreativeTab() nullable; it very much can be null here, and removing the check
      // would make all 112 hidden blocks fabricable.
      return null;
    }
    // Resolved via CsmTab rather than CreativeTabs.getTabLabel(), which is client-only and is
    // stripped from the dedicated server by FML.
    String tabId = CsmTab.getTabId(tab);
    if (tabId == null || TAB_MATERIALS.equals(tabId)) {
      // The parts themselves and the Fabricator are crafted at a bench, not fabricated.
      return null;
    }

    // The registry name is used only as the key to look up the display name. Nothing is inferred
    // from the id itself — see the class javadoc.
    String registryName = ((ICsmBlock) block).getBlockRegistryName();
    String noun = CsmBlockDisplayNames.lastWord(registryName);

    // 1. Structural members, regardless of which subsystem files them.
    if (matches(noun, POLE_NOUNS)) {
      if (CsmBlockDisplayNames.hasWord(registryName, "concrete")) {
        return cost(FabricatorIngredient.part(CsmParts.POLE_SECTION, 1),
            FabricatorIngredient.part(CsmParts.CONCRETE_MIX, 1));
      }
      if (CsmBlockDisplayNames.hasWord(registryName, "wood")
          || CsmBlockDisplayNames.hasWord(registryName, "wooden")
          || "crossarm".equals(noun)) {
        return cost(FabricatorIngredient.any(MC_PLANKS, 2),
            FabricatorIngredient.part(CsmParts.FASTENER_KIT, 1));
      }
      return cost(FabricatorIngredient.part(CsmParts.POLE_SECTION, 2));
    }

    // 2. Road signs.
    if (TAB_ROAD_SIGNS.equals(tabId)) {
      return cost(FabricatorIngredient.part(CsmParts.SIGN_BLANK, 1));
    }

    // 3. Building materials.
    if (TAB_BUILDING_MATERIALS.equals(tabId)) {
      return buildingMaterialCost(block, registryName);
    }

    // 4. Mounting hardware.
    if (matches(noun, MOUNT_NOUNS)) {
      return cost(FabricatorIngredient.part(CsmParts.SHEET_METAL, 1),
          FabricatorIngredient.part(CsmParts.FASTENER_KIT, 1));
    }

    // 5. Optical devices.
    if (hasAnyWord(registryName, OPTICAL_WORDS)) {
      return cost(FabricatorIngredient.part(CsmParts.OPTICAL_SENSOR, 1),
          FabricatorIngredient.part(CsmParts.CONTROL_BOARD, 1));
    }

    // 6 and 7. Equipment base classes, then subsystem defaults.
    switch (tabId) {
      case TAB_HVAC:
        return cost(FabricatorIngredient.part(CsmParts.DUCTING, 1),
            FabricatorIngredient.part(CsmParts.SHEET_METAL, 1));

      case TAB_LIFE_SAFETY:
        return lifeSafetyCost(block, registryName);

      case TAB_LIGHTING:
        return cost(FabricatorIngredient.part(CsmParts.LED_MODULE, 1),
            FabricatorIngredient.part(CsmParts.SHEET_METAL, 1),
            FabricatorIngredient.part(CsmParts.WIRING_HARNESS, 1));

      case TAB_NOVELTIES:
        if (hasAnyWord(registryName, ELECTRONIC_NOVELTY_WORDS)) {
          return cost(FabricatorIngredient.part(CsmParts.CONTROL_BOARD, 1),
              FabricatorIngredient.part(CsmParts.SHEET_METAL, 1));
        }
        // Ornaments and knick-knacks: modelled and painted, not fabricated from steel.
        return cost(FabricatorIngredient.any(MC_CLAY_BALL, 2),
            FabricatorIngredient.any(MC_DYE, 1));

      case TAB_GAMING:
        return gamingCost(registryName, noun);

      case TAB_POWER_GRID:
        return cost(FabricatorIngredient.part(CsmParts.POLE_SECTION, 1),
            FabricatorIngredient.part(CsmParts.WIRING_HARNESS, 1));

      case TAB_TECHNOLOGY:
        return cost(FabricatorIngredient.part(CsmParts.CONTROL_BOARD, 1),
            FabricatorIngredient.part(CsmParts.SHEET_METAL, 1),
            FabricatorIngredient.part(CsmParts.WIRING_HARNESS, 1));

      case TAB_TRAFFIC_ACCESSORIES:
        return cost(FabricatorIngredient.part(CsmParts.SHEET_METAL, 1),
            FabricatorIngredient.part(CsmParts.FASTENER_KIT, 1));

      case TAB_TRAFFIC_SIGNALS:
        return trafficSignalCost(block);

      case TAB_FURNITURE:
        if (hasAnyWord(registryName, METAL_FURNITURE_WORDS)) {
          return cost(FabricatorIngredient.part(CsmParts.SHEET_METAL, 1),
              FabricatorIngredient.part(CsmParts.FASTENER_KIT, 1));
        }
        return cost(FabricatorIngredient.any(MC_PLANKS, 2),
            FabricatorIngredient.part(CsmParts.FASTENER_KIT, 1));

      default:
        // An unrecognised tab means a tab was added without pricing its contents. Fall back to a
        // sane generic cost rather than silently making the whole tab unfabricable.
        return cost(FabricatorIngredient.part(CsmParts.SHEET_METAL, 1),
            FabricatorIngredient.part(CsmParts.FASTENER_KIT, 1));
    }
  }

  /** Prices the Gaming tab, which mixes powered arcade cabinets with plain toys and tables. */
  private static List<FabricatorIngredient> gamingCost(String registryName, String noun) {
    if (CsmBlockDisplayNames.hasWord(registryName, "arcade")) {
      return cost(FabricatorIngredient.part(CsmParts.SHEET_METAL, 2),
          FabricatorIngredient.part(CsmParts.CONTROL_BOARD, 1),
          FabricatorIngredient.part(CsmParts.LED_MODULE, 1));
    }
    if ("cards".equals(noun) || "deck".equals(noun)
        || CsmBlockDisplayNames.hasWord(registryName, "card")) {
      return cost(FabricatorIngredient.any(MC_PAPER, 3));
    }
    // Dollhouses, toyboxes, dart boards and the games tables are all woodwork.
    return cost(FabricatorIngredient.any(MC_PLANKS, 2),
        FabricatorIngredient.part(CsmParts.FASTENER_KIT, 1));
  }

  /**
   * Prices building materials. Coloured metal takes the dye matching its colour, so the fifteen
   * colour sets are made from their own colour. Slab, stairs and fence variants are priced
   * relative to the full block, mirroring vanilla's ratios.
   */
  private static List<FabricatorIngredient> buildingMaterialCost(Block block, String registryName) {
    if (CsmBlockDisplayNames.hasWord(registryName, "metal")) {
      FabricatorIngredient dye = dyeForMetalColour(registryName);
      if (block instanceof AbstractBlockSlab) {
        return cost(FabricatorIngredient.part(CsmParts.SHEET_METAL, 1), dye);
      }
      if (block instanceof AbstractBlockStairs) {
        return cost(FabricatorIngredient.part(CsmParts.SHEET_METAL, 2), dye);
      }
      if (block instanceof AbstractBlockFence) {
        return cost(FabricatorIngredient.part(CsmParts.SHEET_METAL, 1),
            FabricatorIngredient.part(CsmParts.FASTENER_KIT, 1), dye);
      }
      return cost(FabricatorIngredient.part(CsmParts.SHEET_METAL, 2), dye);
    }
    // The rest of this tab is ceiling finishes — popcorn ceiling and ceiling tiles — despite
    // registry ids such as "pcc" and "dct1" suggesting concrete.
    return cost(FabricatorIngredient.part(CsmParts.CONCRETE_MIX, 1),
        FabricatorIngredient.any(MC_CLAY_BALL, 1));
  }

  /**
   * Maps a coloured-metal block to the vanilla dye that colours it, by display name. Iridescent
   * metal has no dye equivalent and uses prismarine crystals for its shimmer.
   */
  private static FabricatorIngredient dyeForMetalColour(String registryName) {
    if (CsmBlockDisplayNames.hasWord(registryName, "iridescent")) {
      return FabricatorIngredient.any(MC_PRISMARINE_CRYSTALS, 1);
    }
    int meta = 15; // white, and the fallback
    if (CsmBlockDisplayNames.hasWord(registryName, "light")
        && CsmBlockDisplayNames.hasWord(registryName, "blue")) {
      meta = 12;
    } else if (CsmBlockDisplayNames.hasWord(registryName, "black")) {
      meta = 0;
    } else if (CsmBlockDisplayNames.hasWord(registryName, "red")) {
      meta = 1;
    } else if (CsmBlockDisplayNames.hasWord(registryName, "green")) {
      meta = 2;
    } else if (CsmBlockDisplayNames.hasWord(registryName, "blue")) {
      meta = 4;
    } else if (CsmBlockDisplayNames.hasWord(registryName, "purple")) {
      meta = 5;
    } else if (CsmBlockDisplayNames.hasWord(registryName, "silver")) {
      meta = 7; // vanilla light gray
    } else if (CsmBlockDisplayNames.hasWord(registryName, "pink")) {
      meta = 9;
    } else if (CsmBlockDisplayNames.hasWord(registryName, "lime")) {
      meta = 10;
    } else if (CsmBlockDisplayNames.hasWord(registryName, "yellow")) {
      meta = 11;
    } else if (CsmBlockDisplayNames.hasWord(registryName, "magenta")) {
      meta = 13;
    } else if (CsmBlockDisplayNames.hasWord(registryName, "orange")
        || CsmBlockDisplayNames.hasWord(registryName, "copper")) {
      meta = 14; // copper is the closest vanilla dye to orange
    }
    return FabricatorIngredient.exact(MC_DYE, meta, 1);
  }

  /**
   * Prices fire alarm and life safety equipment by appliance type.
   *
   * <p><b>Order matters, most specific first.</b> {@code AbstractBlockFireAlarmDetector} extends
   * {@code AbstractBlockFireAlarmActivator}, and {@code AbstractBlockFireAlarmSounderVoiceEvac}
   * extends {@code AbstractBlockFireAlarmSounder}, so testing the parent first would swallow the
   * subclass and leave its branch unreachable. Detectors were previously priced as pull stations
   * for exactly that reason.</p>
   */
  private static List<FabricatorIngredient> lifeSafetyCost(Block block, String registryName) {
    // Sprinklers are classed as detectors but are a glass bulb on a brass body, not an
    // electronic sensor.
    if (CsmBlockDisplayNames.hasWord(registryName, "sprinkler")) {
      return cost(FabricatorIngredient.part(CsmParts.SHEET_METAL, 1),
          FabricatorIngredient.part(CsmParts.LENS_ASSEMBLY, 1));
    }
    // Detectors sense; check before activators, which they extend.
    if (block instanceof AbstractBlockFireAlarmDetector) {
      return cost(FabricatorIngredient.part(CsmParts.OPTICAL_SENSOR, 1),
          FabricatorIngredient.part(CsmParts.CONTROL_BOARD, 1));
    }
    if (block instanceof AbstractBlockFireAlarmActivator) {
      return cost(FabricatorIngredient.part(CsmParts.CONTROL_BOARD, 1),
          FabricatorIngredient.part(CsmParts.SHEET_METAL, 1));
    }
    // Voice evac speakers are a kind of sounder and cost the same, so one check covers both.
    if (block instanceof AbstractBlockFireAlarmSounder) {
      return cost(FabricatorIngredient.part(CsmParts.SOUNDER_DRIVER, 1),
          FabricatorIngredient.part(CsmParts.ENCLOSURE_SHELL, 1));
    }
    return cost(FabricatorIngredient.part(CsmParts.SHEET_METAL, 1),
        FabricatorIngredient.part(CsmParts.WIRING_HARNESS, 1));
  }

  /** Prices traffic signal equipment by role: detection, control, or signal head. */
  private static List<FabricatorIngredient> trafficSignalCost(Block block) {
    if (block instanceof AbstractBlockTrafficSignalSensor
        || block instanceof AbstractBlockTrafficSignalSensorHZEight) {
      return cost(FabricatorIngredient.part(CsmParts.OPTICAL_SENSOR, 1),
          FabricatorIngredient.part(CsmParts.CONTROL_BOARD, 1));
    }
    if (block instanceof BlockTrafficSignalController) {
      return cost(FabricatorIngredient.part(CsmParts.ENCLOSURE_SHELL, 1),
          FabricatorIngredient.part(CsmParts.CONTROL_BOARD, 2),
          FabricatorIngredient.part(CsmParts.WIRING_HARNESS, 1));
    }
    if (block instanceof AbstractBlockControllableSignal) {
      return cost(FabricatorIngredient.part(CsmParts.LED_MODULE, 1),
          FabricatorIngredient.part(CsmParts.LENS_ASSEMBLY, 1),
          FabricatorIngredient.part(CsmParts.SHEET_METAL, 1));
    }
    return cost(FabricatorIngredient.part(CsmParts.SHEET_METAL, 1),
        FabricatorIngredient.part(CsmParts.WIRING_HARNESS, 1));
  }

  private static boolean matches(String noun, String[] candidates) {
    for (String candidate : candidates) {
      if (candidate.equals(noun)) {
        return true;
      }
    }
    return false;
  }

  private static boolean hasAnyWord(String registryName, String[] words) {
    for (String word : words) {
      if (CsmBlockDisplayNames.hasWord(registryName, word)) {
        return true;
      }
    }
    return false;
  }

  private static List<FabricatorIngredient> cost(FabricatorIngredient... ingredients) {
    return Collections.unmodifiableList(Arrays.asList(ingredients));
  }
}

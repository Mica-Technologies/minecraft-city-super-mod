package com.micatechnologies.minecraft.csm.materials;

import com.micatechnologies.minecraft.csm.codeutils.AbstractBlockFence;
import com.micatechnologies.minecraft.csm.codeutils.AbstractBlockSetBasic;
import com.micatechnologies.minecraft.csm.codeutils.AbstractBlockSlab;
import com.micatechnologies.minecraft.csm.codeutils.AbstractBlockStairs;
import com.micatechnologies.minecraft.csm.codeutils.AbstractBlockTrafficPole;
import com.micatechnologies.minecraft.csm.codeutils.AbstractBlockTrafficPoleDiagonal;
import com.micatechnologies.minecraft.csm.codeutils.CsmTab;
import com.micatechnologies.minecraft.csm.codeutils.ICsmBlock;
import com.micatechnologies.minecraft.csm.lifesafety.AbstractBlockFireAlarmActivator;
import com.micatechnologies.minecraft.csm.lifesafety.AbstractBlockFireAlarmDetector;
import com.micatechnologies.minecraft.csm.lifesafety.AbstractBlockFireAlarmSounder;
import com.micatechnologies.minecraft.csm.lifesafety.AbstractBlockFireAlarmSounderVoiceEvac;
import com.micatechnologies.minecraft.csm.trafficsignals.BlockTrafficSignalController;
import com.micatechnologies.minecraft.csm.trafficsignals.logic.AbstractBlockControllableSignal;
import com.micatechnologies.minecraft.csm.trafficsignals.logic.AbstractBlockTrafficSignalSensor;
import com.micatechnologies.minecraft.csm.trafficsignals.logic.AbstractBlockTrafficSignalSensorHZEight;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
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
 * <p>Costs are derived at runtime from what a block actually is. Nothing is generated and nothing
 * needs regenerating: a newly added block picks up a sensible cost automatically.</p>
 *
 * <h3>How a cost is chosen</h3>
 * <ol>
 *   <li>Road signs take a sign blank.</li>
 *   <li>Building materials are priced from their base class, with coloured metal taking the dye
 *       that matches its colour.</li>
 *   <li><b>Structural hardware is recognised by registry name and priced before any subsystem
 *       default.</b> This is what stops a bare mounting bracket in the Lighting tab from costing
 *       an LED module, and makes poles cost pole sections rather than generic sheet metal.</li>
 *   <li>Equipment types with a dedicated base class (fire alarm appliances, signal heads,
 *       detection sensors, the controller cabinet) are priced from that class.</li>
 *   <li>Everything else takes its subsystem's default.</li>
 * </ol>
 *
 * <p>Step 3 is deliberately name-based. The mod's registry names are descriptive and consistent,
 * and there is no base class distinguishing "pole" from "bracket" from "cabinet" within a
 * subsystem — those all share {@code AbstractBlockRotatableNSEWUD}. Class checks are used wherever
 * a class actually carries the meaning; names are used only where they do not.</p>
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
  private static final String MC_PRISMARINE_CRYSTALS = "minecraft:prismarine_crystals";

  /**
   * Name fragments meaning "this thing is powered or optical". A block whose name contains one of
   * these is never treated as plain structural hardware, so an illuminated or electronic item does
   * not get demoted to sheet metal just because it is also named after its mounting.
   */
  private static final String[] ELECTRONIC_MARKERS = {
      "signal", "light", "lamp", "sign", "beacon", "strobe", "horn", "speaker", "camera",
      "detector", "meter", "display", "message", "screen", "monitor", "radar",
  };

  /** Name endings that mean the block is a mounting or enclosure part rather than a device. */
  private static final String[] MOUNTING_SUFFIXES = {
      "mount", "mounts", "mountkit", "bracket", "base", "backplate", "cover", "visor", "hanger",
      "plate", "clamp",
  };

  /** Name fragments meaning the block is made of an optical sensing element. */
  private static final String[] OPTICAL_MARKERS = {"camera", "alpr", "radar", "lidar"};

  /** Name fragments for furniture that is plainly metal rather than timber. */
  private static final String[] METAL_FURNITURE_MARKERS = {
      "hydrant", "anchor", "chain", "barbedwire", "radiator", "grill", "grate", "rail",
  };

  /** Name fragments for novelties that are actually electronics. */
  private static final String[] ELECTRONIC_NOVELTY_MARKERS = {
      "record", "player", "jukebox", "radio", "arcade", "tv",
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
      return null;
    }
    // Resolved via CsmTab rather than CreativeTabs.getTabLabel(), which is client-only and is
    // stripped from the dedicated server by FML.
    String tabId = CsmTab.getTabId(tab);
    if (tabId == null || TAB_MATERIALS.equals(tabId)) {
      // The parts themselves and the Fabricator are crafted at a bench, not fabricated.
      return null;
    }

    String name = ((ICsmBlock) block).getBlockRegistryName();
    name = name == null ? "" : name.toLowerCase(Locale.ROOT);

    if (TAB_ROAD_SIGNS.equals(tabId)) {
      // Signposts and signpost mounts live in the Road Signs tab but are the hardware a sign
      // bolts onto, not a sign face, so they take a pole rather than a blank.
      if (name.contains("signpost")) {
        return cost(FabricatorIngredient.part(CsmParts.POLE_SECTION, 1),
            FabricatorIngredient.part(CsmParts.FASTENER_KIT, 1));
      }
      return cost(FabricatorIngredient.part(CsmParts.SIGN_BLANK, 1));
    }

    if (TAB_BUILDING_MATERIALS.equals(tabId)) {
      return buildingMaterialCost(block, name);
    }

    List<FabricatorIngredient> structural = structuralCost(block, name);
    if (structural != null) {
      return structural;
    }

    if (containsAny(name, OPTICAL_MARKERS)) {
      return cost(FabricatorIngredient.part(CsmParts.OPTICAL_SENSOR, 1),
          FabricatorIngredient.part(CsmParts.CONTROL_BOARD, 1));
    }

    switch (tabId) {
      case TAB_HVAC:
        return cost(FabricatorIngredient.part(CsmParts.DUCTING, 1),
            FabricatorIngredient.part(CsmParts.SHEET_METAL, 1));

      case TAB_LIFE_SAFETY:
        return lifeSafetyCost(block);

      case TAB_LIGHTING:
        return cost(FabricatorIngredient.part(CsmParts.LED_MODULE, 1),
            FabricatorIngredient.part(CsmParts.SHEET_METAL, 1),
            FabricatorIngredient.part(CsmParts.WIRING_HARNESS, 1));

      case TAB_NOVELTIES:
        if (containsAny(name, ELECTRONIC_NOVELTY_MARKERS)) {
          return cost(FabricatorIngredient.part(CsmParts.CONTROL_BOARD, 1),
              FabricatorIngredient.part(CsmParts.SHEET_METAL, 1));
        }
        // Ornaments and knick-knacks: modelled and painted, not fabricated from steel.
        return cost(FabricatorIngredient.any(MC_CLAY_BALL, 2),
            FabricatorIngredient.any(MC_DYE, 1));

      case TAB_GAMING:
        return cost(FabricatorIngredient.part(CsmParts.SHEET_METAL, 2),
            FabricatorIngredient.part(CsmParts.CONTROL_BOARD, 1),
            FabricatorIngredient.part(CsmParts.LED_MODULE, 1));

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
        if (containsAny(name, METAL_FURNITURE_MARKERS)) {
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

  /**
   * Prices structural hardware — poles, crossarms, signposts and mounting parts — from the block's
   * registry name, or returns {@code null} if the block is not structural.
   *
   * <p>Runs before the subsystem defaults so hardware inside an electronic subsystem is not priced
   * as if it were a device. Blocks whose names carry an {@link #ELECTRONIC_MARKERS electronic
   * marker} are excluded, so an illuminated bollard or a variable message sign on a pole keeps its
   * electronics.</p>
   */
  @Nullable
  private static List<FabricatorIngredient> structuralCost(Block block, String name) {
    if (block instanceof AbstractBlockTrafficPole
        || block instanceof AbstractBlockTrafficPoleDiagonal) {
      return cost(FabricatorIngredient.part(CsmParts.POLE_SECTION, 1),
          FabricatorIngredient.part(CsmParts.FASTENER_KIT, 1));
    }
    // Checked ahead of the electronic-marker guard: a signpost is literally a post, but the word
    // contains "sign", so the guard would otherwise veto it and leave signposts priced as
    // generic sheet metal.
    if (name.contains("signpost")) {
      return cost(FabricatorIngredient.part(CsmParts.POLE_SECTION, 1),
          FabricatorIngredient.part(CsmParts.FASTENER_KIT, 1));
    }
    if (containsAny(name, ELECTRONIC_MARKERS)) {
      return null;
    }
    if (name.contains("xarm")) {
      // Utility crossarms are timber.
      return cost(FabricatorIngredient.any(MC_PLANKS, 2),
          FabricatorIngredient.part(CsmParts.FASTENER_KIT, 1));
    }
    if (name.contains("pole")) {
      if (name.contains("concrete")) {
        return cost(FabricatorIngredient.part(CsmParts.POLE_SECTION, 1),
            FabricatorIngredient.part(CsmParts.CONCRETE_MIX, 1));
      }
      return cost(FabricatorIngredient.part(CsmParts.POLE_SECTION, 2));
    }
    if (endsWithAny(name, MOUNTING_SUFFIXES)) {
      return cost(FabricatorIngredient.part(CsmParts.SHEET_METAL, 1),
          FabricatorIngredient.part(CsmParts.FASTENER_KIT, 1));
    }
    return null;
  }

  /**
   * Prices building materials. Coloured metal takes the dye matching its colour, so the fifteen
   * colour sets are made from their own colour rather than all being interchangeable. Slab, stairs
   * and fence variants are priced relative to the full block, mirroring vanilla's ratios.
   */
  private static List<FabricatorIngredient> buildingMaterialCost(Block block, String name) {
    if (name.contains("metal")) {
      FabricatorIngredient dye = dyeForMetalColour(name);
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
    if (block instanceof AbstractBlockSlab) {
      return cost(FabricatorIngredient.part(CsmParts.CONCRETE_MIX, 1));
    }
    if (block instanceof AbstractBlockStairs || block instanceof AbstractBlockSetBasic) {
      return cost(FabricatorIngredient.part(CsmParts.CONCRETE_MIX, 2));
    }
    // The concrete-family blocks (PCC, CTF, CT/DCT variants).
    return cost(FabricatorIngredient.part(CsmParts.CONCRETE_MIX, 2));
  }

  /**
   * Maps a coloured-metal registry name to the vanilla dye that colours it. Iridescent metal has
   * no dye equivalent and uses prismarine crystals for its shimmer.
   */
  private static FabricatorIngredient dyeForMetalColour(String name) {
    if (name.startsWith("iridescent")) {
      return FabricatorIngredient.any(MC_PRISMARINE_CRYSTALS, 1);
    }
    int meta;
    if (name.startsWith("black")) {
      meta = 0;
    } else if (name.startsWith("red")) {
      meta = 1;
    } else if (name.startsWith("green")) {
      meta = 2;
    } else if (name.startsWith("blue")) {
      meta = 4;
    } else if (name.startsWith("purple")) {
      meta = 5;
    } else if (name.startsWith("silver")) {
      meta = 7; // vanilla light gray
    } else if (name.startsWith("pink")) {
      meta = 9;
    } else if (name.startsWith("lime")) {
      meta = 10;
    } else if (name.startsWith("yellow")) {
      meta = 11;
    } else if (name.startsWith("lightblue")) {
      meta = 12;
    } else if (name.startsWith("magenta")) {
      meta = 13;
    } else if (name.startsWith("orange") || name.startsWith("copper")) {
      meta = 14; // copper is the closest vanilla dye to orange
    } else {
      meta = 15; // white, and the fallback
    }
    return FabricatorIngredient.exact(MC_DYE, meta, 1);
  }

  /** Prices fire alarm and life safety equipment by appliance type. */
  private static List<FabricatorIngredient> lifeSafetyCost(Block block) {
    if (block instanceof AbstractBlockFireAlarmSounder
        || block instanceof AbstractBlockFireAlarmSounderVoiceEvac) {
      return cost(FabricatorIngredient.part(CsmParts.SOUNDER_DRIVER, 1),
          FabricatorIngredient.part(CsmParts.ENCLOSURE_SHELL, 1));
    }
    if (block instanceof AbstractBlockFireAlarmActivator) {
      return cost(FabricatorIngredient.part(CsmParts.CONTROL_BOARD, 1),
          FabricatorIngredient.part(CsmParts.SHEET_METAL, 1));
    }
    if (block instanceof AbstractBlockFireAlarmDetector) {
      return cost(FabricatorIngredient.part(CsmParts.OPTICAL_SENSOR, 1),
          FabricatorIngredient.part(CsmParts.CONTROL_BOARD, 1));
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

  private static boolean containsAny(String name, String[] fragments) {
    for (String fragment : fragments) {
      if (name.contains(fragment)) {
        return true;
      }
    }
    return false;
  }

  private static boolean endsWithAny(String name, String[] suffixes) {
    for (String suffix : suffixes) {
      if (name.endsWith(suffix)) {
        return true;
      }
    }
    return false;
  }

  private static List<FabricatorIngredient> cost(FabricatorIngredient... ingredients) {
    return Collections.unmodifiableList(Arrays.asList(ingredients));
  }
}

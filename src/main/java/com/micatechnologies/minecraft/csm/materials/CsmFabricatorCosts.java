package com.micatechnologies.minecraft.csm.materials;

import com.micatechnologies.minecraft.csm.codeutils.AbstractBlockFence;
import com.micatechnologies.minecraft.csm.codeutils.AbstractBlockSlab;
import com.micatechnologies.minecraft.csm.codeutils.AbstractBlockStairs;
import com.micatechnologies.minecraft.csm.codeutils.CsmTab;
import com.micatechnologies.minecraft.csm.codeutils.ICsmBlock;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
 *       sensors, the controller cabinet. Those live with their subsystem, as an
 *       {@link ICsmFabricatorCostRule} registered for the tab.</li>
 *   <li>Everything else takes its subsystem default.</li>
 * </ol>
 *
 * <p>A {@code null} return means "not fabricable". That covers non-CSM blocks and every block
 * registered to a hidden tab: hidden and retiring blocks are given a {@code null} creative
 * tab by {@code CsmTab}, so they fall out here without a separate exclusion list.</p>
 *
 * @author Mica Technologies
 * @since 2026.7
 */
public final class CsmFabricatorCosts {

  private static final String TAB_BUILDING_MATERIALS = "tabbuildingmaterials";
  private static final String TAB_CONSTRUCTION_SITE = "tabconstructionsite";
  private static final String TAB_FURNITURE = "tabfurniture";
  private static final String TAB_GAMING = "tabgaming";
  private static final String TAB_HVAC = "tabhvac";
  private static final String TAB_INTERIOR_FINISHES = "tabinteriorfinishes";
  private static final String TAB_LIFE_SAFETY = "tablifesafety";
  private static final String TAB_LIGHTING = "tablighting";
  private static final String TAB_MATERIALS = "tabmaterials";
  private static final String TAB_NOVELTIES = "tabnovelties";
  private static final String TAB_POWER_GRID = "tabpowergrid";
  private static final String TAB_ROAD_SIGNS = "tabroadsigns";
  private static final String TAB_STRUCTURE_FRAMING = "tabstructureframing";
  private static final String TAB_TECHNOLOGY = "tabtechnology";
  private static final String TAB_TRAFFIC_ACCESSORIES = "tabtrafficaccessories";
  private static final String TAB_TRAFFIC_SIGNALS = "tabtrafficsignals";

  private static final String MC_PLANKS = "minecraft:planks";
  private static final String MC_DYE = "minecraft:dye";
  private static final String MC_CLAY_BALL = "minecraft:clay_ball";
  private static final String MC_COBBLESTONE = "minecraft:cobblestone";
  private static final String MC_IRON_INGOT = "minecraft:iron_ingot";
  private static final String MC_PAPER = "minecraft:paper";
  private static final String MC_PRISMARINE_CRYSTALS = "minecraft:prismarine_crystals";
  private static final String MC_DIRT = "minecraft:dirt";
  private static final String MC_GRAVEL = "minecraft:gravel";
  private static final String MC_SAND = "minecraft:sand";

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

  /**
   * The subsystem cost rules, by creative tab id. At most one rule per tab.
   *
   * @since 2026.9
   */
  private static final Map<String, ICsmFabricatorCostRule> RULES = new HashMap<>();

  private CsmFabricatorCosts() {
    throw new UnsupportedOperationException("CsmFabricatorCosts is a utility class.");
  }

  /**
   * Registers a subsystem's pricing rule for one creative tab. Call this from the owning module's
   * pre-initialization.
   *
   * <p>Pre-initialization is early enough: costs are first read at post-initialization, for the
   * "Fabricator coverage" log line, and thereafter only when a Fabricator GUI is opened. Forge
   * runs every mod's pre-initialization before any of that, so no rule can be registered too
   * late to be seen.
   *
   * @param tabId the creative tab id the rule prices, as returned by {@code CsmTab.getTabId}
   * @param rule  the rule to ask for blocks in that tab
   *
   * @throws IllegalStateException if a rule is already registered for the tab; two rules for one
   *                               tab means one of them would silently never be asked
   * @since 2026.9
   */
  public static void registerRule(String tabId, ICsmFabricatorCostRule rule) {
    if (tabId == null || rule == null) {
      throw new IllegalArgumentException("A Fabricator cost rule and its tab id are required.");
    }
    if (RULES.containsKey(tabId)) {
      throw new IllegalStateException(
          "A Fabricator cost rule is already registered for tab " + tabId);
    }
    RULES.put(tabId, rule);
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
      // Hidden / retiring blocks get a null tab and are deliberately not fabricable.
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

    // 3a. The construction site. Priced here, before the mounting-hardware rule, because a site
    // is full of things whose names end in "base" or "plate" -- a screw-jack base, a trench
    // plate -- that are not brackets.
    if (TAB_CONSTRUCTION_SITE.equals(tabId)) {
      return constructionSiteCost(registryName);
    }

    // 3b. Interior finishes. Priced here rather than in the tab switch below because
    // these are plain materials, like the building materials above, and because the
    // ceiling blocks moved to this tab from Building Materials and must keep the cost
    // they already had.
    if (TAB_INTERIOR_FINISHES.equals(tabId)) {
      return interiorFinishCost();
    }

    // 4. Mounting hardware.
    //
    // Never in the framing tab. This list holds "plate" and "base", and it is consulted before
    // any tab is looked at, so a structural base plate and a timber sole plate were both being
    // priced as brackets — and the timber one in sheet metal, which is not even the right
    // material. A plate that a building stands on is structure, not hardware.
    if (!TAB_STRUCTURE_FRAMING.equals(tabId) && matches(noun, MOUNT_NOUNS)) {
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
        return equipmentCost(tabId, block, registryName);

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

      case TAB_STRUCTURE_FRAMING:
        // Timber framing is lumber and nails; everything else here is steel sections and
        // fasteners. Refine further per member type — joist, deck, structural steel — as those
        // blocks are built.
        if (CsmBlockDisplayNames.hasWord(registryName, "wood")) {
          return cost(FabricatorIngredient.any(MC_PLANKS, 2),
              FabricatorIngredient.part(CsmParts.FASTENER_KIT, 1));
        }
        return cost(FabricatorIngredient.part(CsmParts.SHEET_METAL, 2),
            FabricatorIngredient.part(CsmParts.FASTENER_KIT, 1));

      case TAB_TECHNOLOGY:
        return cost(FabricatorIngredient.part(CsmParts.CONTROL_BOARD, 1),
            FabricatorIngredient.part(CsmParts.SHEET_METAL, 1),
            FabricatorIngredient.part(CsmParts.WIRING_HARNESS, 1));

      case TAB_TRAFFIC_ACCESSORIES:
        return cost(FabricatorIngredient.part(CsmParts.SHEET_METAL, 1),
            FabricatorIngredient.part(CsmParts.FASTENER_KIT, 1));

      case TAB_TRAFFIC_SIGNALS:
        return equipmentCost(tabId, block, registryName);

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
    // Metal cladding is sheet steel or aluminium screwed to girts. Its names avoid the word
    // "metal", which would take it into the colour-set rule above and charge it a dye.
    if (CsmBlockDisplayNames.hasWord(registryName, "cladding")) {
      return cost(FabricatorIngredient.part(CsmParts.SHEET_METAL, 2),
          FabricatorIngredient.part(CsmParts.FASTENER_KIT, 1));
    }
    // Natural stone veneer is quarried stone laid in mortar. Cast stone is concrete and falls
    // through to the masonry cost below.
    if (CsmBlockDisplayNames.hasWord(registryName, "veneer")
        && !CsmBlockDisplayNames.hasWord(registryName, "cast")) {
      return cost(FabricatorIngredient.any(MC_COBBLESTONE, 1),
          FabricatorIngredient.part(CsmParts.CONCRETE_MIX, 1));
    }
    // Siding is not masonry, and the fallback below would make a cedar shingle out of concrete.
    // Fiber cement is cement and cellulose fibre; vinyl is a thin coloured sheet; the rest is
    // timber.
    if (CsmBlockDisplayNames.hasWord(registryName, "siding")) {
      if (CsmBlockDisplayNames.hasWord(registryName, "cement")) {
        return cost(FabricatorIngredient.part(CsmParts.CONCRETE_MIX, 1),
            FabricatorIngredient.any(MC_PAPER, 1));
      }
      if (CsmBlockDisplayNames.hasWord(registryName, "vinyl")) {
        return cost(FabricatorIngredient.any(MC_PAPER, 2), FabricatorIngredient.any(MC_DYE, 1));
      }
      return cost(FabricatorIngredient.any(MC_PLANKS, 2));
    }
    // Chain-link is galvanized or coated steel wire on steel pipe, and its barbed-wire top is the
    // same wire; the masonry fallback below would make it out of concrete.
    if (CsmBlockDisplayNames.hasWord(registryName, "chain")) {
      return cost(FabricatorIngredient.any(MC_IRON_INGOT, 1),
          FabricatorIngredient.part(CsmParts.FASTENER_KIT, 1));
    }
    // Everything else in this tab is masonry and cast material.
    return cost(FabricatorIngredient.part(CsmParts.CONCRETE_MIX, 1),
        FabricatorIngredient.any(MC_CLAY_BALL, 1));
  }

  /**
   * Prices the construction site. Scaffolding is steel tube and a plank deck; formwork is plywood
   * and timber held with ties; a shore is a steel post and its fittings; rebar is steel bar.
   * Anything else in the tab takes steel and fixings until its phase gives it a rule of its own.
   */
  private static List<FabricatorIngredient> constructionSiteCost(String registryName) {
    if (CsmBlockDisplayNames.hasWord(registryName, "scaffold")) {
      return cost(FabricatorIngredient.part(CsmParts.POLE_SECTION, 1),
          FabricatorIngredient.any(MC_PLANKS, 1));
    }
    if (CsmBlockDisplayNames.hasWord(registryName, "formwork")) {
      return cost(FabricatorIngredient.any(MC_PLANKS, 2),
          FabricatorIngredient.part(CsmParts.FASTENER_KIT, 1));
    }
    if (CsmBlockDisplayNames.hasWord(registryName, "shore")) {
      return cost(FabricatorIngredient.part(CsmParts.POLE_SECTION, 1),
          FabricatorIngredient.part(CsmParts.FASTENER_KIT, 1));
    }
    if (CsmBlockDisplayNames.hasWord(registryName, "rebar")) {
      return cost(FabricatorIngredient.any(MC_IRON_INGOT, 1));
    }
    // Trench plates are thick steel plate; a trench box is two plated panels and its spreader
    // pipes.
    if (CsmBlockDisplayNames.hasWord(registryName, "trench")) {
      if (CsmBlockDisplayNames.hasWord(registryName, "box")) {
        return cost(FabricatorIngredient.part(CsmParts.SHEET_METAL, 2),
            FabricatorIngredient.part(CsmParts.POLE_SECTION, 1));
      }
      return cost(FabricatorIngredient.part(CsmParts.SHEET_METAL, 2));
    }
    // A stockpile item is one layer of the material itself.
    if (CsmBlockDisplayNames.hasWord(registryName, "stockpile")) {
      if (CsmBlockDisplayNames.hasWord(registryName, "gravel")) {
        return cost(FabricatorIngredient.any(MC_GRAVEL, 1));
      }
      if (CsmBlockDisplayNames.hasWord(registryName, "sand")) {
        return cost(FabricatorIngredient.any(MC_SAND, 1));
      }
      return cost(FabricatorIngredient.any(MC_DIRT, 1));
    }
    // A silt fence is geotextile stapled to wooden stakes.
    if (CsmBlockDisplayNames.hasWord(registryName, "silt")) {
      return cost(FabricatorIngredient.any(MC_PLANKS, 1), FabricatorIngredient.any(MC_PAPER, 1));
    }
    // Site logistics: what the load is, and the pallet or dunnage under it.
    if (CsmBlockDisplayNames.hasWord(registryName, "pallet")) {
      if (CsmBlockDisplayNames.hasWord(registryName, "brick")) {
        return cost(FabricatorIngredient.any(MC_PLANKS, 1),
            FabricatorIngredient.any(MC_CLAY_BALL, 2));
      }
      if (CsmBlockDisplayNames.hasWord(registryName, "drywall")) {
        return cost(FabricatorIngredient.any(MC_PLANKS, 1), FabricatorIngredient.any(MC_PAPER, 2));
      }
      if (CsmBlockDisplayNames.hasWord(registryName, "block")) {
        return cost(FabricatorIngredient.any(MC_PLANKS, 1),
            FabricatorIngredient.part(CsmParts.CONCRETE_MIX, 1));
      }
      return cost(FabricatorIngredient.any(MC_PLANKS, 1),
          FabricatorIngredient.part(CsmParts.CONCRETE_MIX, 2));
    }
    if (CsmBlockDisplayNames.hasWord(registryName, "lumber")) {
      return cost(FabricatorIngredient.any(MC_PLANKS, 3));
    }
    if (CsmBlockDisplayNames.hasWord(registryName, "insulation")
        || CsmBlockDisplayNames.hasWord(registryName, "pvc")) {
      return cost(FabricatorIngredient.any(MC_PAPER, 2), FabricatorIngredient.any(MC_DYE, 1));
    }
    if (CsmBlockDisplayNames.hasWord(registryName, "conduit")) {
      return cost(FabricatorIngredient.part(CsmParts.POLE_SECTION, 1));
    }
    if (CsmBlockDisplayNames.hasWord(registryName, "spool")) {
      return cost(FabricatorIngredient.any(MC_PLANKS, 1),
          FabricatorIngredient.part(CsmParts.WIRING_HARNESS, 1));
    }
    // Site facilities. A container or dumpster is priced per block of it, as it is built.
    if (CsmBlockDisplayNames.hasWord(registryName, "container")
        || CsmBlockDisplayNames.hasWord(registryName, "dumpster")) {
      return cost(FabricatorIngredient.part(CsmParts.SHEET_METAL, 2));
    }
    if (CsmBlockDisplayNames.hasWord(registryName, "trailer")) {
      return cost(FabricatorIngredient.part(CsmParts.SHEET_METAL, 1),
          FabricatorIngredient.any(MC_PLANKS, 1));
    }
    if (CsmBlockDisplayNames.hasWord(registryName, "toilet")) {
      return cost(FabricatorIngredient.part(CsmParts.SHEET_METAL, 1),
          FabricatorIngredient.any(MC_DYE, 1));
    }
    if (CsmBlockDisplayNames.hasWord(registryName, "gang")) {
      return cost(FabricatorIngredient.part(CsmParts.SHEET_METAL, 2),
          FabricatorIngredient.part(CsmParts.FASTENER_KIT, 1));
    }
    if (CsmBlockDisplayNames.hasWord(registryName, "washout")) {
      return cost(FabricatorIngredient.part(CsmParts.SHEET_METAL, 1),
          FabricatorIngredient.any(MC_PAPER, 1));
    }
    // A crane head is the whole slewing unit: the jib's steelwork, and the cab's controls and
    // wiring. Crane masts never reach here -- the pole rule takes anything named a mast first.
    if (CsmBlockDisplayNames.hasWord(registryName, "crane")) {
      return cost(FabricatorIngredient.part(CsmParts.POLE_SECTION, 4),
          FabricatorIngredient.part(CsmParts.CONTROL_BOARD, 1),
          FabricatorIngredient.part(CsmParts.WIRING_HARNESS, 1));
    }
    return cost(FabricatorIngredient.part(CsmParts.SHEET_METAL, 1),
        FabricatorIngredient.part(CsmParts.FASTENER_KIT, 1));
  }

  /**
   * Prices interior finishes. Today that is the ceiling finishes — popcorn ceiling and
   * ceiling tiles — which moved here from the Building Materials tab and keep the cost
   * they had there, despite registry ids such as {@code pcc} and {@code dct1}
   * suggesting concrete.
   */
  private static List<FabricatorIngredient> interiorFinishCost() {
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
   * Prices a block in a tab whose equipment rules belong to the subsystem that owns them.
   *
   * <p>Asks the {@link ICsmFabricatorCostRule} registered for the tab, if any. A rule that is
   * absent — its module is not installed — or silent (a {@code null} answer, meaning the block is
   * none of the equipment types it knows) leaves the block on the generic equipment cost below,
   * which is what the tab's branch returned directly before the rules moved out of Core. The
   * fallback stays here because it is generic: it names no subsystem, and a tab with no rule
   * still needs it.
   *
   * @param tabId        the block's creative tab id
   * @param block        the block to price
   * @param registryName the block's registry name, for display-name lookups
   *
   * @return the ingredients for the block
   */
  private static List<FabricatorIngredient> equipmentCost(String tabId, Block block,
      String registryName) {
    ICsmFabricatorCostRule rule = RULES.get(tabId);
    if (rule != null) {
      List<FabricatorIngredient> priced = rule.price(block, registryName);
      if (priced != null) {
        return priced;
      }
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

  /**
   * Builds an immutable ingredient list. Public so that an {@link ICsmFabricatorCostRule} in
   * another package builds its answers the same way Core does.
   *
   * @param ingredients the ingredients, in the order they should be shown
   *
   * @return an immutable ingredient list
   *
   * @since 2026.9
   */
  public static List<FabricatorIngredient> cost(FabricatorIngredient... ingredients) {
    return Collections.unmodifiableList(Arrays.asList(ingredients));
  }
}

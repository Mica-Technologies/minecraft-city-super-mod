package com.micatechnologies.minecraft.csm.tabs;

import com.micatechnologies.minecraft.csm.CsmRegistry;
import com.micatechnologies.minecraft.csm.codeutils.CsmTab;
import com.micatechnologies.minecraft.csm.parks.landscape.BlockParkCrop;
import com.micatechnologies.minecraft.csm.parks.landscape.BlockParkFacing;
import com.micatechnologies.minecraft.csm.parks.landscape.BlockParkJoining;
import com.micatechnologies.minecraft.csm.parks.landscape.BlockParkProp;
import com.micatechnologies.minecraft.csm.parks.landscape.BlockParkSaguaro;
import com.micatechnologies.minecraft.csm.parks.planting.ItemTreePlantingTool;
import com.micatechnologies.minecraft.csm.parks.tools.BlockBrushPile;
import com.micatechnologies.minecraft.csm.parks.tools.ItemChainsaw;
import com.micatechnologies.minecraft.csm.parks.tools.ItemPoleTrimmer;
import com.micatechnologies.minecraft.csm.parks.tools.ItemTreeShears;
import com.micatechnologies.minecraft.csm.parks.trees.BlockHangingMoss;
import com.micatechnologies.minecraft.csm.parks.trees.BlockTreeLeaves;
import com.micatechnologies.minecraft.csm.parks.trees.BlockTreeLog;
import com.micatechnologies.minecraft.csm.parks.trees.TreeLeafType;
import com.micatechnologies.minecraft.csm.parks.trees.TreeLogWidth;
import com.micatechnologies.minecraft.csm.parks.trees.TreeWood;
import net.minecraft.block.Block;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;

/**
 * The Trees &amp; Plants tab of the Parks &amp; Greenery module: tree logs and leaves by species,
 * the Tree Planting Tool and the tree tools (chainsaw, pole trimmer, shears and the brush pile a
 * felling leaves), street tree accessories and plantings.
 *
 * @since 2026.9
 */
@CsmTab.Load(order = 19)
public class CsmTabTreesPlants extends CsmTab {

  @Override
  public String getTabId() {
    return "tabtreesplants";
  }

  @Override
  public Block getTabIcon() {
    return CsmRegistry.getBlock(BlockTreeLog.name(TreeWood.LIVE_OAK, TreeLogWidth.MEDIUM));
  }

  @Override
  public boolean getTabSearchable() {
    return false;
  }

  @Override
  public boolean getTabHidden() {
    return false;
  }

  @Override
  public void initTabElements(FMLPreInitializationEvent fmlPreInitializationEvent) {
    initTabItem(ItemTreePlantingTool.class, fmlPreInitializationEvent);
    initTabItem(ItemChainsaw.class, fmlPreInitializationEvent);
    initTabItem(ItemPoleTrimmer.class, fmlPreInitializationEvent);
    initTabItem(ItemTreeShears.class, fmlPreInitializationEvent);
    initTabBlock(BlockBrushPile.class, fmlPreInitializationEvent);

    // Logs: every wood in every width, a wood's widths together. Written by gen_trees.py
    // (--fragments); one line a block, so the tools that read tab sources find each one.
    initTabBlock(new BlockTreeLog("tree_log_liveoak_twig", TreeWood.LIVE_OAK, TreeLogWidth.TWIG));
    initTabBlock(new BlockTreeLog("tree_log_liveoak_thin", TreeWood.LIVE_OAK, TreeLogWidth.THIN));
    initTabBlock(new BlockTreeLog("tree_log_liveoak_medium", TreeWood.LIVE_OAK, TreeLogWidth.MEDIUM));
    initTabBlock(new BlockTreeLog("tree_log_liveoak_thick", TreeWood.LIVE_OAK, TreeLogWidth.THICK));
    initTabBlock(new BlockTreeLog("tree_log_liveoak_full", TreeWood.LIVE_OAK, TreeLogWidth.FULL));
    initTabBlock(new BlockTreeLog("tree_log_elm_twig", TreeWood.ELM, TreeLogWidth.TWIG));
    initTabBlock(new BlockTreeLog("tree_log_elm_thin", TreeWood.ELM, TreeLogWidth.THIN));
    initTabBlock(new BlockTreeLog("tree_log_elm_medium", TreeWood.ELM, TreeLogWidth.MEDIUM));
    initTabBlock(new BlockTreeLog("tree_log_elm_thick", TreeWood.ELM, TreeLogWidth.THICK));
    initTabBlock(new BlockTreeLog("tree_log_elm_full", TreeWood.ELM, TreeLogWidth.FULL));
    initTabBlock(new BlockTreeLog("tree_log_plane_twig", TreeWood.PLANE, TreeLogWidth.TWIG));
    initTabBlock(new BlockTreeLog("tree_log_plane_thin", TreeWood.PLANE, TreeLogWidth.THIN));
    initTabBlock(new BlockTreeLog("tree_log_plane_medium", TreeWood.PLANE, TreeLogWidth.MEDIUM));
    initTabBlock(new BlockTreeLog("tree_log_plane_thick", TreeWood.PLANE, TreeLogWidth.THICK));
    initTabBlock(new BlockTreeLog("tree_log_plane_full", TreeWood.PLANE, TreeLogWidth.FULL));
    initTabBlock(new BlockTreeLog("tree_log_honeylocust_twig", TreeWood.HONEY_LOCUST, TreeLogWidth.TWIG));
    initTabBlock(new BlockTreeLog("tree_log_honeylocust_thin", TreeWood.HONEY_LOCUST, TreeLogWidth.THIN));
    initTabBlock(new BlockTreeLog("tree_log_honeylocust_medium", TreeWood.HONEY_LOCUST, TreeLogWidth.MEDIUM));
    initTabBlock(new BlockTreeLog("tree_log_honeylocust_thick", TreeWood.HONEY_LOCUST, TreeLogWidth.THICK));
    initTabBlock(new BlockTreeLog("tree_log_honeylocust_full", TreeWood.HONEY_LOCUST, TreeLogWidth.FULL));
    initTabBlock(new BlockTreeLog("tree_log_cypress_twig", TreeWood.CYPRESS, TreeLogWidth.TWIG));
    initTabBlock(new BlockTreeLog("tree_log_cypress_thin", TreeWood.CYPRESS, TreeLogWidth.THIN));
    initTabBlock(new BlockTreeLog("tree_log_cypress_medium", TreeWood.CYPRESS, TreeLogWidth.MEDIUM));
    initTabBlock(new BlockTreeLog("tree_log_cypress_thick", TreeWood.CYPRESS, TreeLogWidth.THICK));
    initTabBlock(new BlockTreeLog("tree_log_cypress_full", TreeWood.CYPRESS, TreeLogWidth.FULL));
    initTabBlock(new BlockTreeLog("tree_log_ginkgo_twig", TreeWood.GINKGO, TreeLogWidth.TWIG));
    initTabBlock(new BlockTreeLog("tree_log_ginkgo_thin", TreeWood.GINKGO, TreeLogWidth.THIN));
    initTabBlock(new BlockTreeLog("tree_log_ginkgo_medium", TreeWood.GINKGO, TreeLogWidth.MEDIUM));
    initTabBlock(new BlockTreeLog("tree_log_ginkgo_thick", TreeWood.GINKGO, TreeLogWidth.THICK));
    initTabBlock(new BlockTreeLog("tree_log_ginkgo_full", TreeWood.GINKGO, TreeLogWidth.FULL));
    initTabBlock(new BlockTreeLog("tree_log_palm_twig", TreeWood.PALM, TreeLogWidth.TWIG));
    initTabBlock(new BlockTreeLog("tree_log_palm_thin", TreeWood.PALM, TreeLogWidth.THIN));
    initTabBlock(new BlockTreeLog("tree_log_palm_medium", TreeWood.PALM, TreeLogWidth.MEDIUM));
    initTabBlock(new BlockTreeLog("tree_log_palm_thick", TreeWood.PALM, TreeLogWidth.THICK));
    initTabBlock(new BlockTreeLog("tree_log_palm_full", TreeWood.PALM, TreeLogWidth.FULL));
    initTabBlock(new BlockTreeLog("tree_log_jacaranda_twig", TreeWood.JACARANDA, TreeLogWidth.TWIG));
    initTabBlock(new BlockTreeLog("tree_log_jacaranda_thin", TreeWood.JACARANDA, TreeLogWidth.THIN));
    initTabBlock(new BlockTreeLog("tree_log_jacaranda_medium", TreeWood.JACARANDA, TreeLogWidth.MEDIUM));
    initTabBlock(new BlockTreeLog("tree_log_jacaranda_thick", TreeWood.JACARANDA, TreeLogWidth.THICK));
    initTabBlock(new BlockTreeLog("tree_log_jacaranda_full", TreeWood.JACARANDA, TreeLogWidth.FULL));
    initTabBlock(new BlockTreeLog("tree_log_pepper_twig", TreeWood.PEPPER, TreeLogWidth.TWIG));
    initTabBlock(new BlockTreeLog("tree_log_pepper_thin", TreeWood.PEPPER, TreeLogWidth.THIN));
    initTabBlock(new BlockTreeLog("tree_log_pepper_medium", TreeWood.PEPPER, TreeLogWidth.MEDIUM));
    initTabBlock(new BlockTreeLog("tree_log_pepper_thick", TreeWood.PEPPER, TreeLogWidth.THICK));
    initTabBlock(new BlockTreeLog("tree_log_pepper_full", TreeWood.PEPPER, TreeLogWidth.FULL));
    initTabBlock(new BlockTreeLog("tree_log_poplar_twig", TreeWood.POPLAR, TreeLogWidth.TWIG));
    initTabBlock(new BlockTreeLog("tree_log_poplar_thin", TreeWood.POPLAR, TreeLogWidth.THIN));
    initTabBlock(new BlockTreeLog("tree_log_poplar_medium", TreeWood.POPLAR, TreeLogWidth.MEDIUM));
    initTabBlock(new BlockTreeLog("tree_log_poplar_thick", TreeWood.POPLAR, TreeLogWidth.THICK));
    initTabBlock(new BlockTreeLog("tree_log_poplar_full", TreeWood.POPLAR, TreeLogWidth.FULL));
    initTabBlock(new BlockTreeLog("tree_log_sweetgum_twig", TreeWood.SWEETGUM, TreeLogWidth.TWIG));
    initTabBlock(new BlockTreeLog("tree_log_sweetgum_thin", TreeWood.SWEETGUM, TreeLogWidth.THIN));
    initTabBlock(new BlockTreeLog("tree_log_sweetgum_medium", TreeWood.SWEETGUM, TreeLogWidth.MEDIUM));
    initTabBlock(new BlockTreeLog("tree_log_sweetgum_thick", TreeWood.SWEETGUM, TreeLogWidth.THICK));
    initTabBlock(new BlockTreeLog("tree_log_sweetgum_full", TreeWood.SWEETGUM, TreeLogWidth.FULL));
    initTabBlock(new BlockTreeLog("tree_log_hornbeam_twig", TreeWood.HORNBEAM, TreeLogWidth.TWIG));
    initTabBlock(new BlockTreeLog("tree_log_hornbeam_thin", TreeWood.HORNBEAM, TreeLogWidth.THIN));
    initTabBlock(new BlockTreeLog("tree_log_hornbeam_medium", TreeWood.HORNBEAM, TreeLogWidth.MEDIUM));
    initTabBlock(new BlockTreeLog("tree_log_hornbeam_thick", TreeWood.HORNBEAM, TreeLogWidth.THICK));
    initTabBlock(new BlockTreeLog("tree_log_hornbeam_full", TreeWood.HORNBEAM, TreeLogWidth.FULL));
    initTabBlock(new BlockTreeLog("tree_log_gum_twig", TreeWood.GUM, TreeLogWidth.TWIG));
    initTabBlock(new BlockTreeLog("tree_log_gum_thin", TreeWood.GUM, TreeLogWidth.THIN));
    initTabBlock(new BlockTreeLog("tree_log_gum_medium", TreeWood.GUM, TreeLogWidth.MEDIUM));
    initTabBlock(new BlockTreeLog("tree_log_gum_thick", TreeWood.GUM, TreeLogWidth.THICK));
    initTabBlock(new BlockTreeLog("tree_log_gum_full", TreeWood.GUM, TreeLogWidth.FULL));
    initTabBlock(new BlockTreeLog("tree_log_willow_twig", TreeWood.WILLOW, TreeLogWidth.TWIG));
    initTabBlock(new BlockTreeLog("tree_log_willow_thin", TreeWood.WILLOW, TreeLogWidth.THIN));
    initTabBlock(new BlockTreeLog("tree_log_willow_medium", TreeWood.WILLOW, TreeLogWidth.MEDIUM));
    initTabBlock(new BlockTreeLog("tree_log_willow_thick", TreeWood.WILLOW, TreeLogWidth.THICK));
    initTabBlock(new BlockTreeLog("tree_log_willow_full", TreeWood.WILLOW, TreeLogWidth.FULL));
    initTabBlock(new BlockTreeLog("tree_log_linden_twig", TreeWood.LINDEN, TreeLogWidth.TWIG));
    initTabBlock(new BlockTreeLog("tree_log_linden_thin", TreeWood.LINDEN, TreeLogWidth.THIN));
    initTabBlock(new BlockTreeLog("tree_log_linden_medium", TreeWood.LINDEN, TreeLogWidth.MEDIUM));
    initTabBlock(new BlockTreeLog("tree_log_linden_thick", TreeWood.LINDEN, TreeLogWidth.THICK));
    initTabBlock(new BlockTreeLog("tree_log_linden_full", TreeWood.LINDEN, TreeLogWidth.FULL));
    initTabBlock(new BlockTreeLog("tree_log_birch_twig", TreeWood.BIRCH, TreeLogWidth.TWIG));
    initTabBlock(new BlockTreeLog("tree_log_birch_thin", TreeWood.BIRCH, TreeLogWidth.THIN));
    initTabBlock(new BlockTreeLog("tree_log_birch_medium", TreeWood.BIRCH, TreeLogWidth.MEDIUM));
    initTabBlock(new BlockTreeLog("tree_log_birch_thick", TreeWood.BIRCH, TreeLogWidth.THICK));
    initTabBlock(new BlockTreeLog("tree_log_birch_full", TreeWood.BIRCH, TreeLogWidth.FULL));
    initTabBlock(new BlockTreeLog("tree_log_maple_twig", TreeWood.MAPLE, TreeLogWidth.TWIG));
    initTabBlock(new BlockTreeLog("tree_log_maple_thin", TreeWood.MAPLE, TreeLogWidth.THIN));
    initTabBlock(new BlockTreeLog("tree_log_maple_medium", TreeWood.MAPLE, TreeLogWidth.MEDIUM));
    initTabBlock(new BlockTreeLog("tree_log_maple_thick", TreeWood.MAPLE, TreeLogWidth.THICK));
    initTabBlock(new BlockTreeLog("tree_log_maple_full", TreeWood.MAPLE, TreeLogWidth.FULL));
    initTabBlock(new BlockTreeLog("tree_log_spruce_twig", TreeWood.SPRUCE, TreeLogWidth.TWIG));
    initTabBlock(new BlockTreeLog("tree_log_spruce_thin", TreeWood.SPRUCE, TreeLogWidth.THIN));
    initTabBlock(new BlockTreeLog("tree_log_spruce_medium", TreeWood.SPRUCE, TreeLogWidth.MEDIUM));
    initTabBlock(new BlockTreeLog("tree_log_spruce_thick", TreeWood.SPRUCE, TreeLogWidth.THICK));
    initTabBlock(new BlockTreeLog("tree_log_spruce_full", TreeWood.SPRUCE, TreeLogWidth.FULL));
    initTabBlock(new BlockTreeLog("tree_log_pine_twig", TreeWood.PINE, TreeLogWidth.TWIG));
    initTabBlock(new BlockTreeLog("tree_log_pine_thin", TreeWood.PINE, TreeLogWidth.THIN));
    initTabBlock(new BlockTreeLog("tree_log_pine_medium", TreeWood.PINE, TreeLogWidth.MEDIUM));
    initTabBlock(new BlockTreeLog("tree_log_pine_thick", TreeWood.PINE, TreeLogWidth.THICK));
    initTabBlock(new BlockTreeLog("tree_log_pine_full", TreeWood.PINE, TreeLogWidth.FULL));
    initTabBlock(new BlockTreeLog("tree_log_beech_twig", TreeWood.BEECH, TreeLogWidth.TWIG));
    initTabBlock(new BlockTreeLog("tree_log_beech_thin", TreeWood.BEECH, TreeLogWidth.THIN));
    initTabBlock(new BlockTreeLog("tree_log_beech_medium", TreeWood.BEECH, TreeLogWidth.MEDIUM));
    initTabBlock(new BlockTreeLog("tree_log_beech_thick", TreeWood.BEECH, TreeLogWidth.THICK));
    initTabBlock(new BlockTreeLog("tree_log_beech_full", TreeWood.BEECH, TreeLogWidth.FULL));
    initTabBlock(new BlockTreeLog("tree_log_sabal_twig", TreeWood.SABAL, TreeLogWidth.TWIG));
    initTabBlock(new BlockTreeLog("tree_log_sabal_thin", TreeWood.SABAL, TreeLogWidth.THIN));
    initTabBlock(new BlockTreeLog("tree_log_sabal_medium", TreeWood.SABAL, TreeLogWidth.MEDIUM));
    initTabBlock(new BlockTreeLog("tree_log_sabal_thick", TreeWood.SABAL, TreeLogWidth.THICK));
    initTabBlock(new BlockTreeLog("tree_log_sabal_full", TreeWood.SABAL, TreeLogWidth.FULL));
    initTabBlock(new BlockTreeLog("tree_log_redwood_twig", TreeWood.REDWOOD, TreeLogWidth.TWIG));
    initTabBlock(new BlockTreeLog("tree_log_redwood_thin", TreeWood.REDWOOD, TreeLogWidth.THIN));
    initTabBlock(new BlockTreeLog("tree_log_redwood_medium", TreeWood.REDWOOD, TreeLogWidth.MEDIUM));
    initTabBlock(new BlockTreeLog("tree_log_redwood_thick", TreeWood.REDWOOD, TreeLogWidth.THICK));
    initTabBlock(new BlockTreeLog("tree_log_redwood_full", TreeWood.REDWOOD, TreeLogWidth.FULL));
    initTabBlock(new BlockTreeLog("tree_log_whitepine_twig", TreeWood.WHITE_PINE, TreeLogWidth.TWIG));
    initTabBlock(new BlockTreeLog("tree_log_whitepine_thin", TreeWood.WHITE_PINE, TreeLogWidth.THIN));
    initTabBlock(new BlockTreeLog("tree_log_whitepine_medium", TreeWood.WHITE_PINE, TreeLogWidth.MEDIUM));
    initTabBlock(new BlockTreeLog("tree_log_whitepine_thick", TreeWood.WHITE_PINE, TreeLogWidth.THICK));
    initTabBlock(new BlockTreeLog("tree_log_whitepine_full", TreeWood.WHITE_PINE, TreeLogWidth.FULL));
    initTabBlock(new BlockTreeLog("tree_log_oak_twig", TreeWood.OAK, TreeLogWidth.TWIG));
    initTabBlock(new BlockTreeLog("tree_log_oak_thin", TreeWood.OAK, TreeLogWidth.THIN));
    initTabBlock(new BlockTreeLog("tree_log_oak_medium", TreeWood.OAK, TreeLogWidth.MEDIUM));
    initTabBlock(new BlockTreeLog("tree_log_oak_thick", TreeWood.OAK, TreeLogWidth.THICK));
    initTabBlock(new BlockTreeLog("tree_log_oak_full", TreeWood.OAK, TreeLogWidth.FULL));
    initTabBlock(new BlockTreeLog("tree_log_camphor_twig", TreeWood.CAMPHOR, TreeLogWidth.TWIG));
    initTabBlock(new BlockTreeLog("tree_log_camphor_thin", TreeWood.CAMPHOR, TreeLogWidth.THIN));
    initTabBlock(new BlockTreeLog("tree_log_camphor_medium", TreeWood.CAMPHOR, TreeLogWidth.MEDIUM));
    initTabBlock(new BlockTreeLog("tree_log_camphor_thick", TreeWood.CAMPHOR, TreeLogWidth.THICK));
    initTabBlock(new BlockTreeLog("tree_log_camphor_full", TreeWood.CAMPHOR, TreeLogWidth.FULL));
    initTabBlock(new BlockTreeLog("tree_log_sequoia_twig", TreeWood.SEQUOIA, TreeLogWidth.TWIG));
    initTabBlock(new BlockTreeLog("tree_log_sequoia_thin", TreeWood.SEQUOIA, TreeLogWidth.THIN));
    initTabBlock(new BlockTreeLog("tree_log_sequoia_medium", TreeWood.SEQUOIA, TreeLogWidth.MEDIUM));
    initTabBlock(new BlockTreeLog("tree_log_sequoia_thick", TreeWood.SEQUOIA, TreeLogWidth.THICK));
    initTabBlock(new BlockTreeLog("tree_log_sequoia_full", TreeWood.SEQUOIA, TreeLogWidth.FULL));
    initTabBlock(new BlockTreeLog("tree_log_joshua_twig", TreeWood.JOSHUA, TreeLogWidth.TWIG));
    initTabBlock(new BlockTreeLog("tree_log_joshua_thin", TreeWood.JOSHUA, TreeLogWidth.THIN));
    initTabBlock(new BlockTreeLog("tree_log_joshua_medium", TreeWood.JOSHUA, TreeLogWidth.MEDIUM));
    initTabBlock(new BlockTreeLog("tree_log_joshua_thick", TreeWood.JOSHUA, TreeLogWidth.THICK));
    initTabBlock(new BlockTreeLog("tree_log_joshua_full", TreeWood.JOSHUA, TreeLogWidth.FULL));
    initTabBlock(new BlockTreeLog("tree_log_canary_twig", TreeWood.CANARY, TreeLogWidth.TWIG));
    initTabBlock(new BlockTreeLog("tree_log_canary_thin", TreeWood.CANARY, TreeLogWidth.THIN));
    initTabBlock(new BlockTreeLog("tree_log_canary_medium", TreeWood.CANARY, TreeLogWidth.MEDIUM));
    initTabBlock(new BlockTreeLog("tree_log_canary_thick", TreeWood.CANARY, TreeLogWidth.THICK));
    initTabBlock(new BlockTreeLog("tree_log_canary_full", TreeWood.CANARY, TreeLogWidth.FULL));
    initTabBlock(new BlockTreeLog("tree_log_palmgrey_twig", TreeWood.PALM_GREY, TreeLogWidth.TWIG));
    initTabBlock(new BlockTreeLog("tree_log_palmgrey_thin", TreeWood.PALM_GREY, TreeLogWidth.THIN));
    initTabBlock(new BlockTreeLog("tree_log_palmgrey_medium", TreeWood.PALM_GREY, TreeLogWidth.MEDIUM));
    initTabBlock(new BlockTreeLog("tree_log_palmgrey_thick", TreeWood.PALM_GREY, TreeLogWidth.THICK));
    initTabBlock(new BlockTreeLog("tree_log_palmgrey_full", TreeWood.PALM_GREY, TreeLogWidth.FULL));
    initTabBlock(new BlockTreeLog("tree_log_douglasfir_twig", TreeWood.DOUGLAS_FIR, TreeLogWidth.TWIG));
    initTabBlock(new BlockTreeLog("tree_log_douglasfir_thin", TreeWood.DOUGLAS_FIR, TreeLogWidth.THIN));
    initTabBlock(new BlockTreeLog("tree_log_douglasfir_medium", TreeWood.DOUGLAS_FIR, TreeLogWidth.MEDIUM));
    initTabBlock(new BlockTreeLog("tree_log_douglasfir_thick", TreeWood.DOUGLAS_FIR, TreeLogWidth.THICK));
    initTabBlock(new BlockTreeLog("tree_log_douglasfir_full", TreeWood.DOUGLAS_FIR, TreeLogWidth.FULL));
    initTabBlock(new BlockTreeLog("tree_log_bristlecone_twig", TreeWood.BRISTLECONE, TreeLogWidth.TWIG));
    initTabBlock(new BlockTreeLog("tree_log_bristlecone_thin", TreeWood.BRISTLECONE, TreeLogWidth.THIN));
    initTabBlock(new BlockTreeLog("tree_log_bristlecone_medium", TreeWood.BRISTLECONE, TreeLogWidth.MEDIUM));
    initTabBlock(new BlockTreeLog("tree_log_bristlecone_thick", TreeWood.BRISTLECONE, TreeLogWidth.THICK));
    initTabBlock(new BlockTreeLog("tree_log_bristlecone_full", TreeWood.BRISTLECONE, TreeLogWidth.FULL));
    initTabBlock(new BlockTreeLog("tree_log_sycamore_twig", TreeWood.SYCAMORE, TreeLogWidth.TWIG));
    initTabBlock(new BlockTreeLog("tree_log_sycamore_thin", TreeWood.SYCAMORE, TreeLogWidth.THIN));
    initTabBlock(new BlockTreeLog("tree_log_sycamore_medium", TreeWood.SYCAMORE, TreeLogWidth.MEDIUM));
    initTabBlock(new BlockTreeLog("tree_log_sycamore_thick", TreeWood.SYCAMORE, TreeLogWidth.THICK));
    initTabBlock(new BlockTreeLog("tree_log_sycamore_full", TreeWood.SYCAMORE, TreeLogWidth.FULL));
    initTabBlock(new BlockTreeLog("tree_log_bluegum_twig", TreeWood.BLUE_GUM, TreeLogWidth.TWIG));
    initTabBlock(new BlockTreeLog("tree_log_bluegum_thin", TreeWood.BLUE_GUM, TreeLogWidth.THIN));
    initTabBlock(new BlockTreeLog("tree_log_bluegum_medium", TreeWood.BLUE_GUM, TreeLogWidth.MEDIUM));
    initTabBlock(new BlockTreeLog("tree_log_bluegum_thick", TreeWood.BLUE_GUM, TreeLogWidth.THICK));
    initTabBlock(new BlockTreeLog("tree_log_bluegum_full", TreeWood.BLUE_GUM, TreeLogWidth.FULL));
    initTabBlock(new BlockTreeLog("tree_log_citrus_twig", TreeWood.CITRUS, TreeLogWidth.TWIG));
    initTabBlock(new BlockTreeLog("tree_log_citrus_thin", TreeWood.CITRUS, TreeLogWidth.THIN));
    initTabBlock(new BlockTreeLog("tree_log_citrus_medium", TreeWood.CITRUS, TreeLogWidth.MEDIUM));
    initTabBlock(new BlockTreeLog("tree_log_citrus_thick", TreeWood.CITRUS, TreeLogWidth.THICK));
    initTabBlock(new BlockTreeLog("tree_log_citrus_full", TreeWood.CITRUS, TreeLogWidth.FULL));
    initTabBlock(new BlockTreeLog("tree_log_avocado_twig", TreeWood.AVOCADO, TreeLogWidth.TWIG));
    initTabBlock(new BlockTreeLog("tree_log_avocado_thin", TreeWood.AVOCADO, TreeLogWidth.THIN));
    initTabBlock(new BlockTreeLog("tree_log_avocado_medium", TreeWood.AVOCADO, TreeLogWidth.MEDIUM));
    initTabBlock(new BlockTreeLog("tree_log_avocado_thick", TreeWood.AVOCADO, TreeLogWidth.THICK));
    initTabBlock(new BlockTreeLog("tree_log_avocado_full", TreeWood.AVOCADO, TreeLogWidth.FULL));
    initTabBlock(new BlockTreeLog("tree_log_olive_twig", TreeWood.OLIVE, TreeLogWidth.TWIG));
    initTabBlock(new BlockTreeLog("tree_log_olive_thin", TreeWood.OLIVE, TreeLogWidth.THIN));
    initTabBlock(new BlockTreeLog("tree_log_olive_medium", TreeWood.OLIVE, TreeLogWidth.MEDIUM));
    initTabBlock(new BlockTreeLog("tree_log_olive_thick", TreeWood.OLIVE, TreeLogWidth.THICK));
    initTabBlock(new BlockTreeLog("tree_log_olive_full", TreeWood.OLIVE, TreeLogWidth.FULL));
    initTabBlock(new BlockTreeLog("tree_log_apple_twig", TreeWood.APPLE, TreeLogWidth.TWIG));
    initTabBlock(new BlockTreeLog("tree_log_apple_thin", TreeWood.APPLE, TreeLogWidth.THIN));
    initTabBlock(new BlockTreeLog("tree_log_apple_medium", TreeWood.APPLE, TreeLogWidth.MEDIUM));
    initTabBlock(new BlockTreeLog("tree_log_apple_thick", TreeWood.APPLE, TreeLogWidth.THICK));
    initTabBlock(new BlockTreeLog("tree_log_apple_full", TreeWood.APPLE, TreeLogWidth.FULL));
    initTabBlock(new BlockTreeLog("tree_log_mulberry_twig", TreeWood.MULBERRY, TreeLogWidth.TWIG));
    initTabBlock(new BlockTreeLog("tree_log_mulberry_thin", TreeWood.MULBERRY, TreeLogWidth.THIN));
    initTabBlock(new BlockTreeLog("tree_log_mulberry_medium", TreeWood.MULBERRY, TreeLogWidth.MEDIUM));
    initTabBlock(new BlockTreeLog("tree_log_mulberry_thick", TreeWood.MULBERRY, TreeLogWidth.THICK));
    initTabBlock(new BlockTreeLog("tree_log_mulberry_full", TreeWood.MULBERRY, TreeLogWidth.FULL));
    initTabBlock(new BlockTreeLog("tree_log_banana_twig", TreeWood.BANANA, TreeLogWidth.TWIG));
    initTabBlock(new BlockTreeLog("tree_log_banana_thin", TreeWood.BANANA, TreeLogWidth.THIN));
    initTabBlock(new BlockTreeLog("tree_log_banana_medium", TreeWood.BANANA, TreeLogWidth.MEDIUM));
    initTabBlock(new BlockTreeLog("tree_log_banana_thick", TreeWood.BANANA, TreeLogWidth.THICK));
    initTabBlock(new BlockTreeLog("tree_log_banana_full", TreeWood.BANANA, TreeLogWidth.FULL));

    // Leaves: one block a species and season, then the palm crowns and the hanging moss. Also
    // written by gen_trees.py.
    initTabBlock(new BlockTreeLeaves("tree_leaves_liveoak", TreeLeafType.BROADLEAF,
        "csm:blocks/parks/leaves_liveoak"));
    initTabBlock(new BlockTreeLeaves("tree_leaves_elm", TreeLeafType.BROADLEAF,
        "csm:blocks/parks/leaves_elm"));
    initTabBlock(new BlockTreeLeaves("tree_leaves_plane", TreeLeafType.BROADLEAF,
        "csm:blocks/parks/leaves_plane"));
    initTabBlock(new BlockTreeLeaves("tree_leaves_honeylocust", TreeLeafType.AIRY,
        "csm:blocks/parks/leaves_honeylocust"));
    initTabBlock(new BlockTreeLeaves("tree_leaves_ginkgo", TreeLeafType.BROADLEAF,
        "csm:blocks/parks/leaves_ginkgo"));
    initTabBlock(new BlockTreeLeaves("tree_leaves_cypress", TreeLeafType.NEEDLE,
        "csm:blocks/parks/leaves_cypress"));
    initTabBlock(new BlockTreeLeaves("tree_leaves_elm_autumn", TreeLeafType.BROADLEAF,
        "csm:blocks/parks/leaves_elm_autumn"));
    initTabBlock(new BlockTreeLeaves("tree_leaves_plane_autumn", TreeLeafType.BROADLEAF,
        "csm:blocks/parks/leaves_plane_autumn"));
    initTabBlock(new BlockTreeLeaves("tree_leaves_honeylocust_autumn", TreeLeafType.AIRY,
        "csm:blocks/parks/leaves_honeylocust_autumn"));
    initTabBlock(new BlockTreeLeaves("tree_leaves_ginkgo_autumn", TreeLeafType.BROADLEAF,
        "csm:blocks/parks/leaves_ginkgo_autumn"));
    initTabBlock(new BlockTreeLeaves("tree_leaves_jacaranda", TreeLeafType.AIRY,
        "csm:blocks/parks/leaves_jacaranda"));
    initTabBlock(new BlockTreeLeaves("tree_leaves_jacaranda_blossom", TreeLeafType.AIRY,
        "csm:blocks/parks/leaves_jacaranda_blossom"));
    initTabBlock(new BlockTreeLeaves("tree_leaves_pepper", TreeLeafType.WEEPING,
        "csm:blocks/parks/leaves_pepper"));
    initTabBlock(new BlockTreeLeaves("tree_leaves_poplar", TreeLeafType.BROADLEAF,
        "csm:blocks/parks/leaves_poplar"));
    initTabBlock(new BlockTreeLeaves("tree_leaves_poplar_autumn", TreeLeafType.BROADLEAF,
        "csm:blocks/parks/leaves_poplar_autumn"));
    initTabBlock(new BlockTreeLeaves("tree_leaves_sweetgum", TreeLeafType.BROADLEAF,
        "csm:blocks/parks/leaves_sweetgum"));
    initTabBlock(new BlockTreeLeaves("tree_leaves_sweetgum_autumn", TreeLeafType.BROADLEAF,
        "csm:blocks/parks/leaves_sweetgum_autumn"));
    initTabBlock(new BlockTreeLeaves("tree_leaves_hornbeam", TreeLeafType.BROADLEAF,
        "csm:blocks/parks/leaves_hornbeam"));
    initTabBlock(new BlockTreeLeaves("tree_leaves_gum", TreeLeafType.AIRY,
        "csm:blocks/parks/leaves_gum"));
    initTabBlock(new BlockTreeLeaves("tree_leaves_willow", TreeLeafType.WEEPING,
        "csm:blocks/parks/leaves_willow"));
    initTabBlock(new BlockTreeLeaves("tree_leaves_arborvitae", TreeLeafType.NEEDLE,
        "csm:blocks/parks/leaves_arborvitae"));
    initTabBlock(new BlockTreeLeaves("tree_leaves_linden", TreeLeafType.BROADLEAF,
        "csm:blocks/parks/leaves_linden"));
    initTabBlock(new BlockTreeLeaves("tree_leaves_linden_clipped", TreeLeafType.CLIPPED,
        "csm:blocks/parks/leaves_linden_clipped"));
    initTabBlock(new BlockTreeLeaves("tree_leaves_birch", TreeLeafType.BROADLEAF,
        "csm:blocks/parks/leaves_birch"));
    initTabBlock(new BlockTreeLeaves("tree_leaves_birch_autumn", TreeLeafType.BROADLEAF,
        "csm:blocks/parks/leaves_birch_autumn"));
    initTabBlock(new BlockTreeLeaves("tree_leaves_maple_japanese", TreeLeafType.BROADLEAF,
        "csm:blocks/parks/leaves_maple_japanese"));
    initTabBlock(new BlockTreeLeaves("tree_leaves_maple_japanese_autumn", TreeLeafType.BROADLEAF,
        "csm:blocks/parks/leaves_maple_japanese_autumn"));
    initTabBlock(new BlockTreeLeaves("tree_leaves_spruce_blue", TreeLeafType.BROADLEAF,
        "csm:blocks/parks/leaves_spruce_blue"));
    initTabBlock(new BlockTreeLeaves("tree_leaves_pine", TreeLeafType.BROADLEAF,
        "csm:blocks/parks/leaves_pine"));
    initTabBlock(new BlockTreeLeaves("tree_leaves_beech", TreeLeafType.BROADLEAF,
        "csm:blocks/parks/leaves_beech"));
    initTabBlock(new BlockTreeLeaves("tree_leaves_beech_autumn", TreeLeafType.BROADLEAF,
        "csm:blocks/parks/leaves_beech_autumn"));
    initTabBlock(new BlockTreeLeaves("tree_leaves_redwood", TreeLeafType.BROADLEAF,
        "csm:blocks/parks/leaves_redwood"));
    initTabBlock(new BlockTreeLeaves("tree_leaves_pine_white", TreeLeafType.BROADLEAF,
        "csm:blocks/parks/leaves_pine_white"));
    initTabBlock(new BlockTreeLeaves("tree_leaves_oak", TreeLeafType.BROADLEAF,
        "csm:blocks/parks/leaves_oak"));
    initTabBlock(new BlockTreeLeaves("tree_leaves_oak_autumn", TreeLeafType.BROADLEAF,
        "csm:blocks/parks/leaves_oak_autumn"));
    initTabBlock(new BlockTreeLeaves("tree_leaves_camphor", TreeLeafType.BROADLEAF,
        "csm:blocks/parks/leaves_camphor"));
    initTabBlock(new BlockTreeLeaves("tree_leaves_sequoia", TreeLeafType.BROADLEAF,
        "csm:blocks/parks/leaves_sequoia"));
    initTabBlock(new BlockTreeLeaves("tree_leaves_douglasfir", TreeLeafType.BROADLEAF,
        "csm:blocks/parks/leaves_douglasfir"));
    initTabBlock(new BlockTreeLeaves("tree_leaves_bristlecone", TreeLeafType.BROADLEAF,
        "csm:blocks/parks/leaves_bristlecone"));
    initTabBlock(new BlockTreeLeaves("tree_leaves_bluegum", TreeLeafType.AIRY,
        "csm:blocks/parks/leaves_bluegum"));
    initTabBlock(new BlockTreeLeaves("tree_leaves_citrus", TreeLeafType.BROADLEAF,
        "csm:blocks/parks/leaves_citrus"));
    initTabBlock(new BlockTreeLeaves("tree_leaves_citrus_orange", TreeLeafType.BROADLEAF,
        "csm:blocks/parks/leaves_citrus_orange"));
    initTabBlock(new BlockTreeLeaves("tree_leaves_citrus_lemon", TreeLeafType.BROADLEAF,
        "csm:blocks/parks/leaves_citrus_lemon"));
    initTabBlock(new BlockTreeLeaves("tree_leaves_citrus_lime", TreeLeafType.BROADLEAF,
        "csm:blocks/parks/leaves_citrus_lime"));
    initTabBlock(new BlockTreeLeaves("tree_leaves_citrus_grapefruit", TreeLeafType.BROADLEAF,
        "csm:blocks/parks/leaves_citrus_grapefruit"));
    initTabBlock(new BlockTreeLeaves("tree_leaves_avocado", TreeLeafType.BROADLEAF,
        "csm:blocks/parks/leaves_avocado"));
    initTabBlock(new BlockTreeLeaves("tree_leaves_olive", TreeLeafType.AIRY,
        "csm:blocks/parks/leaves_olive"));
    initTabBlock(new BlockTreeLeaves("tree_leaves_apple", TreeLeafType.BROADLEAF,
        "csm:blocks/parks/leaves_apple"));
    initTabBlock(new BlockTreeLeaves("tree_leaves_apple_honeycrisp", TreeLeafType.BROADLEAF,
        "csm:blocks/parks/leaves_apple_honeycrisp"));
    initTabBlock(new BlockTreeLeaves("tree_leaves_apple_granny", TreeLeafType.BROADLEAF,
        "csm:blocks/parks/leaves_apple_granny"));
    initTabBlock(new BlockTreeLeaves("tree_leaves_apple_golden", TreeLeafType.BROADLEAF,
        "csm:blocks/parks/leaves_apple_golden"));
    initTabBlock(new BlockTreeLeaves("tree_leaves_mulberry", TreeLeafType.BROADLEAF,
        "csm:blocks/parks/leaves_mulberry"));
    initTabBlock(new BlockTreeLeaves("tree_crown_palm_fan", TreeLeafType.PALM_FAN,
        "csm:blocks/parks/palm_crown_fan"));
    initTabBlock(new BlockTreeLeaves("tree_crown_palm_fan_skirt", TreeLeafType.PALM_FAN_SKIRT,
        "csm:blocks/parks/palm_crown_fan"));
    initTabBlock(new BlockTreeLeaves("tree_crown_palm_feather", TreeLeafType.PALM_FEATHER,
        "csm:blocks/parks/palm_crown_feather"));
    initTabBlock(new BlockTreeLeaves("tree_crown_palm_cabbage", TreeLeafType.PALM_CABBAGE,
        "csm:blocks/parks/palm_crown_cabbage"));
    initTabBlock(new BlockTreeLeaves("tree_crown_palm_cabbage_skirt", TreeLeafType.PALM_CABBAGE_SKIRT,
        "csm:blocks/parks/palm_crown_cabbage"));
    initTabBlock(new BlockTreeLeaves("tree_crown_palm_canary", TreeLeafType.PALM_CANARY,
        "csm:blocks/parks/palm_crown_canary"));
    initTabBlock(new BlockTreeLeaves("tree_crown_palm_coconut", TreeLeafType.PALM_COCONUT,
        "csm:blocks/parks/palm_crown_coconut"));
    initTabBlock(new BlockTreeLeaves("tree_crown_palm_king", TreeLeafType.PALM_KING,
        "csm:blocks/parks/palm_crown_king"));
    initTabBlock(new BlockTreeLeaves("tree_crown_joshua", TreeLeafType.ROSETTE,
        "csm:blocks/parks/palm_crown_joshua"));
    initTabBlock(new BlockTreeLeaves("tree_crown_banana", TreeLeafType.PALM_BANANA,
        "csm:blocks/parks/palm_crown_banana"));
    initTabBlock(new BlockTreeLeaves("tree_crown_banana_fruit", TreeLeafType.PALM_BANANA_FRUIT,
        "csm:blocks/parks/palm_crown_banana"));
    initTabBlock(new BlockHangingMoss("spanish_moss"));
    initTabBlock(new BlockHangingMoss("willow_strands"));

    // Street tree accessories and plantings, written by gen_park_plantings.py (--fragments).
    initTabBlock(new BlockParkProp("tree_grate_square", BlockParkProp.Kind.GROUND, 16, 0));
    initTabBlock(new BlockParkProp("tree_grate_round", BlockParkProp.Kind.GROUND, 16, 0));
    initTabBlock(new BlockParkProp("tree_pit_mulch", BlockParkProp.Kind.GROUND, 16, 0));
    initTabBlock(new BlockParkJoining("hedge_boxwood_low", BlockParkJoining.Kind.HEDGE, 10, 14));
    initTabBlock(new BlockParkJoining("hedge_boxwood_tall", BlockParkJoining.Kind.HEDGE, 16, 14));
    initTabBlock(new BlockParkJoining("hedge_privet_low", BlockParkJoining.Kind.HEDGE, 10, 14));
    initTabBlock(new BlockParkJoining("hedge_privet_tall", BlockParkJoining.Kind.HEDGE, 16, 14));
    initTabBlock(new BlockParkJoining("tree_pit_fence", BlockParkJoining.Kind.FENCE, 9, 2));
    initTabBlock(new BlockParkJoining("raised_bed_concrete", BlockParkJoining.Kind.BED, 12, 16));
    initTabBlock(new BlockParkProp("planter_concrete", BlockParkProp.Kind.PLANTER, 14, 1));
    initTabBlock(new BlockParkJoining("raised_bed_wood", BlockParkJoining.Kind.BED, 12, 16));
    initTabBlock(new BlockParkProp("planter_wood", BlockParkProp.Kind.PLANTER, 14, 1));
    initTabBlock(new BlockParkJoining("raised_bed_corten", BlockParkJoining.Kind.BED, 12, 16));
    initTabBlock(new BlockParkProp("planter_corten", BlockParkProp.Kind.PLANTER, 14, 1));
    initTabBlock(new BlockParkProp("shrub_boxwood", BlockParkProp.Kind.SHRUB, 12, 2));
    initTabBlock(new BlockParkProp("shrub_hydrangea", BlockParkProp.Kind.SHRUB, 14, 1));
    initTabBlock(new BlockParkProp("shrub_juniper", BlockParkProp.Kind.SHRUB, 16, 2));
    initTabBlock(new BlockParkProp("grass_fountain", BlockParkProp.Kind.PLANT, 12, 2));
    initTabBlock(new BlockParkProp("grass_feather_reed", BlockParkProp.Kind.PLANT, 12, 2));
    initTabBlock(new BlockParkProp("grass_blue_fescue", BlockParkProp.Kind.PLANT, 12, 2));
    initTabBlock(new BlockParkProp("flower_bed_red", BlockParkProp.Kind.PLANT, 6, 0));
    initTabBlock(new BlockParkProp("flower_bed_yellow", BlockParkProp.Kind.PLANT, 6, 0));
    initTabBlock(new BlockParkProp("flower_bed_purple", BlockParkProp.Kind.PLANT, 6, 0));
    initTabBlock(new BlockParkProp("flower_bed_mixed", BlockParkProp.Kind.PLANT, 6, 0));
    initTabBlock(new BlockParkProp("ground_mulch", BlockParkProp.Kind.COVER, 1, 0));
    initTabBlock(new BlockParkProp("ground_pea_gravel", BlockParkProp.Kind.COVER, 1, 0));
    initTabBlock(new BlockParkProp("ground_decomposed_granite", BlockParkProp.Kind.COVER, 1, 0));
    initTabBlock(new BlockParkProp("ground_turf", BlockParkProp.Kind.COVER, 1, 0));
    initTabBlock(new BlockParkFacing.TreeStake("tree_stake", new int[]{7, 0, 11, 9, 24, 13}, true));
    initTabBlock(new BlockParkFacing.PoleFitted("hanging_basket_petunia", new int[]{2, 0, 0, 14, 16, 12}, false));
    initTabBlock(new BlockParkFacing.PoleFitted("hanging_basket_mixed", new int[]{2, 0, 0, 14, 16, 12}, false));

    // Native plants by region, each region's plants followed by the same plants in nursery
    // pots, so a garden centre can be stocked in rows. Also written by gen_park_plantings.py.
    // California
    initTabBlock(new BlockParkProp("flower_california_poppy", BlockParkProp.Kind.PLANT, 8, 2));
    initTabBlock(new BlockParkProp("shrub_manzanita", BlockParkProp.Kind.SHRUB, 14, 1));
    initTabBlock(new BlockParkProp("shrub_ceanothus", BlockParkProp.Kind.SHRUB, 12, 0));
    initTabBlock(new BlockParkProp("shrub_white_sage", BlockParkProp.Kind.SHRUB, 8, 1));
    initTabBlock(new BlockParkProp("grass_deergrass", BlockParkProp.Kind.PLANT, 14, 1));
    initTabBlock(new BlockParkProp("potted_flower_california_poppy", BlockParkProp.Kind.SHRUB, 11, 3));
    initTabBlock(new BlockParkProp("potted_shrub_manzanita", BlockParkProp.Kind.SHRUB, 15, 3));
    initTabBlock(new BlockParkProp("potted_shrub_ceanothus", BlockParkProp.Kind.SHRUB, 14, 3));
    initTabBlock(new BlockParkProp("potted_shrub_white_sage", BlockParkProp.Kind.SHRUB, 11, 3));
    initTabBlock(new BlockParkProp("potted_grass_deergrass", BlockParkProp.Kind.SHRUB, 15, 3));
    // New Hampshire
    initTabBlock(new BlockParkProp("shrub_mountain_laurel", BlockParkProp.Kind.SHRUB, 14, 1));
    initTabBlock(new BlockParkProp("shrub_highbush_blueberry", BlockParkProp.Kind.SHRUB, 16, 1));
    initTabBlock(new BlockParkProp("shrub_winterberry", BlockParkProp.Kind.SHRUB, 15, 1));
    initTabBlock(new BlockParkProp("flower_lupine", BlockParkProp.Kind.PLANT, 14, 2));
    initTabBlock(new BlockParkProp("flower_ladys_slipper", BlockParkProp.Kind.PLANT, 11, 3));
    initTabBlock(new BlockParkProp("potted_shrub_mountain_laurel", BlockParkProp.Kind.SHRUB, 15, 3));
    initTabBlock(new BlockParkProp("potted_shrub_highbush_blueberry", BlockParkProp.Kind.SHRUB, 16, 3));
    initTabBlock(new BlockParkProp("potted_shrub_winterberry", BlockParkProp.Kind.SHRUB, 15, 3));
    initTabBlock(new BlockParkProp("potted_flower_lupine", BlockParkProp.Kind.SHRUB, 15, 3));
    initTabBlock(new BlockParkProp("potted_flower_ladys_slipper", BlockParkProp.Kind.SHRUB, 13, 3));
    // Colorado
    initTabBlock(new BlockParkProp("flower_columbine", BlockParkProp.Kind.PLANT, 12, 2));
    initTabBlock(new BlockParkProp("flower_penstemon", BlockParkProp.Kind.PLANT, 14, 2));
    initTabBlock(new BlockParkProp("shrub_rabbitbrush", BlockParkProp.Kind.SHRUB, 12, 0));
    initTabBlock(new BlockParkProp("shrub_sagebrush", BlockParkProp.Kind.SHRUB, 13, 1));
    initTabBlock(new BlockParkProp("grass_blue_grama", BlockParkProp.Kind.PLANT, 10, 1));
    initTabBlock(new BlockParkProp("plant_yucca", BlockParkProp.Kind.PLANT, 15, 1));
    initTabBlock(new BlockParkProp("potted_flower_columbine", BlockParkProp.Kind.SHRUB, 14, 3));
    initTabBlock(new BlockParkProp("potted_flower_penstemon", BlockParkProp.Kind.SHRUB, 15, 3));
    initTabBlock(new BlockParkProp("potted_shrub_rabbitbrush", BlockParkProp.Kind.SHRUB, 14, 3));
    initTabBlock(new BlockParkProp("potted_shrub_sagebrush", BlockParkProp.Kind.SHRUB, 14, 3));
    initTabBlock(new BlockParkProp("potted_grass_blue_grama", BlockParkProp.Kind.SHRUB, 12, 3));
    initTabBlock(new BlockParkProp("potted_plant_yucca", BlockParkProp.Kind.SHRUB, 15, 3));
    // Florida
    initTabBlock(new BlockParkProp("plant_saw_palmetto", BlockParkProp.Kind.PLANT, 14, 0));
    initTabBlock(new BlockParkProp("plant_coontie", BlockParkProp.Kind.PLANT, 9, 1));
    initTabBlock(new BlockParkProp("shrub_beautyberry", BlockParkProp.Kind.SHRUB, 13, 0));
    initTabBlock(new BlockParkProp("shrub_firebush", BlockParkProp.Kind.SHRUB, 16, 1));
    initTabBlock(new BlockParkProp("flower_coreopsis", BlockParkProp.Kind.PLANT, 10, 2));
    initTabBlock(new BlockParkProp("grass_pink_muhly", BlockParkProp.Kind.PLANT, 14, 0));
    initTabBlock(new BlockParkProp("potted_plant_saw_palmetto", BlockParkProp.Kind.SHRUB, 15, 3));
    initTabBlock(new BlockParkProp("potted_plant_coontie", BlockParkProp.Kind.SHRUB, 12, 3));
    initTabBlock(new BlockParkProp("potted_shrub_beautyberry", BlockParkProp.Kind.SHRUB, 14, 3));
    initTabBlock(new BlockParkProp("potted_shrub_firebush", BlockParkProp.Kind.SHRUB, 16, 3));
    initTabBlock(new BlockParkProp("potted_flower_coreopsis", BlockParkProp.Kind.SHRUB, 12, 3));
    initTabBlock(new BlockParkProp("potted_grass_pink_muhly", BlockParkProp.Kind.SHRUB, 15, 3));
    // Japan
    initTabBlock(new BlockParkProp("shrub_satsuki_azalea", BlockParkProp.Kind.SHRUB, 10, 0));
    initTabBlock(new BlockParkProp("shrub_camellia", BlockParkProp.Kind.SHRUB, 16, 1));
    initTabBlock(new BlockParkProp("flower_japanese_iris", BlockParkProp.Kind.PLANT, 13, 1));
    initTabBlock(new BlockParkProp("plant_bamboo", BlockParkProp.Kind.PLANT, 16, 0));
    initTabBlock(new BlockParkProp("grass_hakone", BlockParkProp.Kind.PLANT, 7, 0));
    initTabBlock(new BlockParkProp("potted_shrub_satsuki_azalea", BlockParkProp.Kind.SHRUB, 12, 3));
    initTabBlock(new BlockParkProp("potted_shrub_camellia", BlockParkProp.Kind.SHRUB, 16, 3));
    initTabBlock(new BlockParkProp("potted_flower_japanese_iris", BlockParkProp.Kind.SHRUB, 14, 3));
    initTabBlock(new BlockParkProp("potted_plant_bamboo", BlockParkProp.Kind.SHRUB, 16, 3));
    initTabBlock(new BlockParkProp("potted_grass_hakone", BlockParkProp.Kind.SHRUB, 11, 3));
    // Sweden and Denmark
    initTabBlock(new BlockParkProp("shrub_heather", BlockParkProp.Kind.SHRUB, 8, 0));
    initTabBlock(new BlockParkProp("shrub_lingonberry", BlockParkProp.Kind.PLANT, 4, 0));
    initTabBlock(new BlockParkProp("flower_wood_anemone", BlockParkProp.Kind.PLANT, 6, 1));
    initTabBlock(new BlockParkProp("flower_harebell", BlockParkProp.Kind.PLANT, 10, 2));
    initTabBlock(new BlockParkProp("flower_marguerite", BlockParkProp.Kind.PLANT, 12, 2));
    initTabBlock(new BlockParkProp("potted_shrub_heather", BlockParkProp.Kind.SHRUB, 11, 3));
    initTabBlock(new BlockParkProp("potted_shrub_lingonberry", BlockParkProp.Kind.SHRUB, 9, 3));
    initTabBlock(new BlockParkProp("potted_flower_wood_anemone", BlockParkProp.Kind.SHRUB, 10, 3));
    initTabBlock(new BlockParkProp("potted_flower_harebell", BlockParkProp.Kind.SHRUB, 12, 3));
    initTabBlock(new BlockParkProp("potted_flower_marguerite", BlockParkProp.Kind.SHRUB, 14, 3));
    // Herbs and garden plants
    initTabBlock(new BlockParkProp("shrub_lavender", BlockParkProp.Kind.SHRUB, 11, 2));
    initTabBlock(new BlockParkProp("shrub_rosemary", BlockParkProp.Kind.SHRUB, 15, 2));
    initTabBlock(new BlockParkProp("shrub_mexican_bush_sage", BlockParkProp.Kind.SHRUB, 16, 0));
    initTabBlock(new BlockParkProp("shrub_russian_sage", BlockParkProp.Kind.PLANT, 16, 1));
    initTabBlock(new BlockParkProp("shrub_star_jasmine", BlockParkProp.Kind.SHRUB, 9, 0));
    initTabBlock(new BlockParkProp("plant_bird_of_paradise", BlockParkProp.Kind.PLANT, 16, 1));
    initTabBlock(new BlockParkProp("plant_foxtail_fern", BlockParkProp.Kind.PLANT, 10, 2));
    initTabBlock(new BlockParkProp("flower_wild_mustard", BlockParkProp.Kind.PLANT, 16, 2));
    initTabBlock(new BlockParkProp("potted_shrub_lavender", BlockParkProp.Kind.SHRUB, 13, 3));
    initTabBlock(new BlockParkProp("potted_shrub_rosemary", BlockParkProp.Kind.SHRUB, 15, 3));
    initTabBlock(new BlockParkProp("potted_shrub_mexican_bush_sage", BlockParkProp.Kind.SHRUB, 16, 3));
    initTabBlock(new BlockParkProp("potted_shrub_russian_sage", BlockParkProp.Kind.SHRUB, 16, 3));
    initTabBlock(new BlockParkProp("potted_shrub_star_jasmine", BlockParkProp.Kind.SHRUB, 12, 3));
    initTabBlock(new BlockParkProp("potted_plant_bird_of_paradise", BlockParkProp.Kind.SHRUB, 16, 3));
    initTabBlock(new BlockParkProp("potted_plant_foxtail_fern", BlockParkProp.Kind.SHRUB, 12, 3));
    initTabBlock(new BlockParkProp("potted_flower_wild_mustard", BlockParkProp.Kind.SHRUB, 16, 3));
    // Desert: cacti and succulents
    initTabBlock(new BlockParkSaguaro("cactus_saguaro", 4));
    initTabBlock(new BlockParkProp("cactus_golden_barrel", BlockParkProp.Kind.CACTUS, 10, 3));
    initTabBlock(new BlockParkProp("cactus_prickly_pear", BlockParkProp.Kind.CACTUS, 16, 1));
    initTabBlock(new BlockParkProp("plant_agave", BlockParkProp.Kind.SHRUB, 12, 1));
    initTabBlock(new BlockParkProp("plant_aloe_vera", BlockParkProp.Kind.PLANT, 11, 3));
    initTabBlock(new BlockParkProp("plant_echeveria", BlockParkProp.Kind.PLANT, 5, 0));
    initTabBlock(new BlockParkProp("plant_jade", BlockParkProp.Kind.SHRUB, 12, 2));
    initTabBlock(new BlockParkProp("potted_cactus_saguaro", BlockParkProp.Kind.SHRUB, 15, 3));
    initTabBlock(new BlockParkProp("potted_cactus_golden_barrel", BlockParkProp.Kind.SHRUB, 12, 3));
    initTabBlock(new BlockParkProp("potted_cactus_prickly_pear", BlockParkProp.Kind.SHRUB, 16, 3));
    initTabBlock(new BlockParkProp("potted_plant_agave", BlockParkProp.Kind.SHRUB, 14, 3));
    initTabBlock(new BlockParkProp("potted_plant_aloe_vera", BlockParkProp.Kind.SHRUB, 13, 3));
    initTabBlock(new BlockParkProp("potted_plant_echeveria", BlockParkProp.Kind.SHRUB, 9, 3));
    initTabBlock(new BlockParkProp("potted_plant_jade", BlockParkProp.Kind.SHRUB, 14, 3));
    // Garden flowers
    initTabBlock(new BlockParkProp("flower_tulips", BlockParkProp.Kind.PLANT, 9, 2));
    initTabBlock(new BlockParkProp("flower_daffodils", BlockParkProp.Kind.PLANT, 8, 2));
    initTabBlock(new BlockParkProp("shrub_rose", BlockParkProp.Kind.SHRUB, 15, 1));
    initTabBlock(new BlockParkProp("flower_sunflower", BlockParkProp.Kind.PLANT, 28, 3));
    initTabBlock(new BlockParkProp("flower_marigolds", BlockParkProp.Kind.PLANT, 7, 1));
    initTabBlock(new BlockParkProp("flower_zinnias", BlockParkProp.Kind.PLANT, 12, 2));
    initTabBlock(new BlockParkProp("shrub_hibiscus", BlockParkProp.Kind.SHRUB, 16, 1));
    initTabBlock(new BlockParkProp("shrub_bougainvillea", BlockParkProp.Kind.SHRUB, 14, 0));
    initTabBlock(new BlockParkProp("potted_flower_tulips", BlockParkProp.Kind.SHRUB, 12, 3));
    initTabBlock(new BlockParkProp("potted_flower_daffodils", BlockParkProp.Kind.SHRUB, 11, 3));
    initTabBlock(new BlockParkProp("potted_shrub_rose", BlockParkProp.Kind.SHRUB, 15, 3));
    initTabBlock(new BlockParkProp("potted_flower_sunflower", BlockParkProp.Kind.SHRUB, 16, 3));
    initTabBlock(new BlockParkProp("potted_flower_marigolds", BlockParkProp.Kind.SHRUB, 11, 3));
    initTabBlock(new BlockParkProp("potted_flower_zinnias", BlockParkProp.Kind.SHRUB, 14, 3));
    initTabBlock(new BlockParkProp("potted_shrub_hibiscus", BlockParkProp.Kind.SHRUB, 16, 3));
    initTabBlock(new BlockParkProp("potted_shrub_bougainvillea", BlockParkProp.Kind.SHRUB, 15, 3));

    // The nursery, garden centre and farm: more planters, nursery benches and stock, crop rows.
    initTabBlock(new BlockParkProp("planter_terracotta_small", BlockParkProp.Kind.PLANTER, 8, 4));
    initTabBlock(new BlockParkProp("planter_terracotta_large", BlockParkProp.Kind.PLANTER, 13, 2));
    initTabBlock(new BlockParkProp("planter_glazed_urn", BlockParkProp.Kind.PLANTER, 14, 2));
    initTabBlock(new BlockParkProp("planter_concrete_bowl", BlockParkProp.Kind.PLANTER, 8, 1));
    initTabBlock(new BlockParkProp("planter_half_barrel", BlockParkProp.Kind.PLANTER, 10, 1));
    initTabBlock(new BlockParkFacing("window_box_flowers", new int[]{1, 0, 10, 15, 10, 16}, true));
    initTabBlock(new BlockParkJoining("nursery_bench", BlockParkJoining.Kind.TABLE, 16, 16));
    initTabBlock(new BlockParkFacing("potting_bench", new int[]{0, 0, 3, 16, 16, 16}, true));
    initTabBlock(new BlockParkProp("nursery_pot_stack", BlockParkProp.Kind.PLANTER, 12, 1));
    initTabBlock(new BlockParkProp("seedling_flats", BlockParkProp.Kind.PLANTER, 3, 0));
    initTabBlock(new BlockParkProp("compost_bin", BlockParkProp.Kind.PLANTER, 14, 0));
    initTabBlock(new BlockParkFacing("wheelbarrow", new int[]{2, 0, 0, 14, 10, 16}, true));
    initTabBlock(new BlockParkCrop("crop_row_lettuce", new int[]{0, 0, 2, 16, 5, 14}));
    initTabBlock(new BlockParkCrop("crop_row_tomato", new int[]{0, 0, 2, 16, 16, 14}));
    initTabBlock(new BlockParkFacing("trellis_clematis", new int[]{0, 0, 7, 16, 16, 9}, false));
    initTabBlock(new BlockParkCrop("crop_row_lima_bean", new int[]{0, 0, 2, 16, 16, 14}));
    initTabBlock(new BlockParkCrop("crop_row_pumpkin", new int[]{0, 0, 2, 16, 10, 14}));
    initTabBlock(new BlockParkCrop("crop_row_watermelon", new int[]{0, 0, 2, 16, 8, 14}));
    initTabBlock(new BlockParkCrop("crop_row_boysenberry", new int[]{0, 0, 2, 16, 15, 14}));
  }
}

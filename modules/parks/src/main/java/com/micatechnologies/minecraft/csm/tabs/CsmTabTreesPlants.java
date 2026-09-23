package com.micatechnologies.minecraft.csm.tabs;

import com.micatechnologies.minecraft.csm.CsmRegistry;
import com.micatechnologies.minecraft.csm.codeutils.CsmTab;
import com.micatechnologies.minecraft.csm.parks.landscape.BlockParkFacing;
import com.micatechnologies.minecraft.csm.parks.landscape.BlockParkJoining;
import com.micatechnologies.minecraft.csm.parks.landscape.BlockParkProp;
import com.micatechnologies.minecraft.csm.parks.planting.ItemTreePlantingTool;
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
 * the Tree Planting Tool, street tree accessories and plantings.
 *
 * @since 2026.9
 */
@CsmTab.Load(order = 18)
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
    initTabBlock(new BlockTreeLeaves("tree_crown_palm_fan", TreeLeafType.PALM_FAN,
        "csm:blocks/parks/palm_crown_fan"));
    initTabBlock(new BlockTreeLeaves("tree_crown_palm_fan_skirt", TreeLeafType.PALM_FAN_SKIRT,
        "csm:blocks/parks/palm_crown_fan"));
    initTabBlock(new BlockTreeLeaves("tree_crown_palm_feather", TreeLeafType.PALM_FEATHER,
        "csm:blocks/parks/palm_crown_feather"));
    initTabBlock(new BlockHangingMoss("spanish_moss"));

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
    initTabBlock(new BlockParkFacing("tree_stake", new int[]{7, 0, 11, 9, 24, 13}, true));
    initTabBlock(new BlockParkFacing.PoleFitted("hanging_basket_petunia", new int[]{2, 0, 0, 14, 16, 12}, false));
    initTabBlock(new BlockParkFacing.PoleFitted("hanging_basket_mixed", new int[]{2, 0, 0, 14, 16, 12}, false));
  }
}

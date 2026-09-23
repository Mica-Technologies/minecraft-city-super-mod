package com.micatechnologies.minecraft.csm.tabs;

import com.micatechnologies.minecraft.csm.CsmRegistry;
import com.micatechnologies.minecraft.csm.codeutils.CsmTab;
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

    // Leaves: one block a species (and later a season). Also written by gen_trees.py.
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
  }
}

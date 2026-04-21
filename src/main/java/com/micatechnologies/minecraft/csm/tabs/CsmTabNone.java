package com.micatechnologies.minecraft.csm.tabs;

import com.micatechnologies.minecraft.csm.codeutils.CsmTab;
import com.micatechnologies.minecraft.csm.lighting.BlockLightupAir;
import com.micatechnologies.minecraft.csm.trafficaccessories.BlockTrafficAccessoryBackplate;
import com.micatechnologies.minecraft.csm.trafficaccessories.BlockTrafficAccessoryNSEWUD;
import com.micatechnologies.minecraft.csm.trafficaccessories.BlockTrafficPoleHorizontalMountDouble;
import com.micatechnologies.minecraft.csm.trafficaccessories.BlockTrafficPoleHorizontalMountDoubleBlack;
import com.micatechnologies.minecraft.csm.trafficaccessories.BlockTrafficPoleHorizontalMountDoubleTan;
import com.micatechnologies.minecraft.csm.trafficaccessories.BlockTrafficPoleHorizontalMountDoubleWhite;
import com.micatechnologies.minecraft.csm.trafficaccessories.BlockTrafficPoleHorizontalSingleMount;
import com.micatechnologies.minecraft.csm.trafficaccessories.BlockTrafficPoleHorizontalSingleMountBlack;
import com.micatechnologies.minecraft.csm.trafficaccessories.BlockTrafficPoleHorizontalSingleMountTan;
import com.micatechnologies.minecraft.csm.trafficaccessories.BlockTrafficPoleHorizontalSingleMountWhite;
import com.micatechnologies.minecraft.csm.trafficsignals.TrafficSignalBlocks;
import net.minecraft.block.Block;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;

/**
 * The "none" tab for blocks which don't belong to a tab.
 *
 * @version 1.0
 */
@CsmTab.Load(order = 0)
public class CsmTabNone extends CsmTab {

  /**
   * Gets the ID (unique identifier) of the tab.
   *
   * @return the ID of the tab
   *
   * @since 1.0
   */
  @Override
  public String getTabId() {
    return null;
  }

  /**
   * Gets the block to use as the icon of the tab
   *
   * @return the block to use as the icon of the tab
   *
   * @since 1.0
   */
  @Override
  public Block getTabIcon() {
    return null;
  }

  /**
   * Gets a boolean indicating if the tab is searchable (has its own search bar).
   *
   * @return {@code true} if the tab is searchable, otherwise {@code false}
   *
   * @since 1.0
   */
  @Override
  public boolean getTabSearchable() {
    return false;
  }

  /**
   * Gets a boolean indicating if the tab is hidden (not displayed in the inventory).
   *
   * @return {@code true} if the tab is hidden, otherwise {@code false}
   *
   * @since 1.0
   */
  @Override
  public boolean getTabHidden() {
    return true;
  }

  /**
   * Initializes all the elements belonging to the tab.
   *
   * @param fmlPreInitializationEvent the {@link FMLPreInitializationEvent} that is being processed
   *
   * @since 1.0
   */
  @Override
  public void initTabElements(FMLPreInitializationEvent fmlPreInitializationEvent) {
    initTabBlock(BlockLightupAir.class, fmlPreInitializationEvent); // Lightup Air

    // Deprecated angled signal blocks (auto-convert to non-angled equivalents via ICsmRetiringBlock)
    initTabBlock(TrafficSignalBlocks.VERTICAL_ANGLE_AHEAD_SIGNAL);
    initTabBlock(TrafficSignalBlocks.VERTICAL_ANGLE_BIKE_SIGNAL);
    initTabBlock(TrafficSignalBlocks.VERTICAL_ANGLE_LEFT_SIGNAL);
    initTabBlock(TrafficSignalBlocks.VERTICAL_ANGLE_RAIL_SIGNAL);
    initTabBlock(TrafficSignalBlocks.VERTICAL_ANGLE_RIGHT2_SIGNAL);
    initTabBlock(TrafficSignalBlocks.VERTICAL_ANGLE_RIGHT_SIGNAL);
    initTabBlock(TrafficSignalBlocks.VERTICAL_ANGLE_SOLID_SIGNAL);
    initTabBlock(TrafficSignalBlocks.VERTICAL_ANGLE_U_TURN_SIGNAL);
    initTabBlock(TrafficSignalBlocks.VERTICAL_ANGLE_UP_LEFT_SIGNAL);
    initTabBlock(TrafficSignalBlocks.VERTICAL_ANGLE2_AHEAD_SIGNAL);
    initTabBlock(TrafficSignalBlocks.VERTICAL_ANGLE2_BIKE_SIGNAL);
    initTabBlock(TrafficSignalBlocks.VERTICAL_ANGLE2_LEFT_SIGNAL);
    initTabBlock(TrafficSignalBlocks.VERTICAL_ANGLE2_RAIL_SIGNAL);
    initTabBlock(TrafficSignalBlocks.VERTICAL_ANGLE2_RIGHT2_SIGNAL);
    initTabBlock(TrafficSignalBlocks.VERTICAL_ANGLE2_RIGHT_SIGNAL);
    initTabBlock(TrafficSignalBlocks.VERTICAL_ANGLE2_SOLID_SIGNAL);
    initTabBlock(TrafficSignalBlocks.VERTICAL_ANGLE2_U_TURN_SIGNAL);
    initTabBlock(TrafficSignalBlocks.VERTICAL_ANGLE2_UP_LEFT_SIGNAL);

    // Deprecated gray signal head blocks (auto-convert to non-gray equivalents via ICsmRetiringBlock)
    initTabBlock(TrafficSignalBlocks.VERTICAL_SOLID_SIGNAL_GRAY);
    initTabBlock(TrafficSignalBlocks.VERTICAL_SOLID_FLASH_GREEN_SIGNAL_GRAY);
    initTabBlock(TrafficSignalBlocks.VERTICAL_SOLID_FLASH_YELLOW_SIGNAL_GRAY);
    initTabBlock(TrafficSignalBlocks.VERTICAL_SOLID_FLASH_RED_SIGNAL_GRAY);
    initTabBlock(TrafficSignalBlocks.VERTICAL_LEFT_SIGNAL_GRAY);
    initTabBlock(TrafficSignalBlocks.VERTICAL_RIGHT_SIGNAL_GRAY);
    initTabBlock(TrafficSignalBlocks.VERTICAL_AHEAD_SIGNAL_GRAY);
    initTabBlock(TrafficSignalBlocks.VERTICAL_BIKE_SIGNAL_GRAY);
    initTabBlock(TrafficSignalBlocks.VERTICAL_RAIL_SIGNAL_GRAY);
    initTabBlock(TrafficSignalBlocks.VERTICAL_HYBRID_LEFT_SIGNAL_GRAY);
    initTabBlock(TrafficSignalBlocks.VERTICAL_HYBRID_LEFT_ADD_ON_SIGNAL_GRAY);
    initTabBlock(TrafficSignalBlocks.VERTICAL_LEFT_ADD_ON_SIGNAL_GRAY);
    initTabBlock(TrafficSignalBlocks.VERTICAL_RIGHT_ADD_ON_SIGNAL_GRAY);
    initTabBlock(TrafficSignalBlocks.VERTICAL_LEFT_ADD_ON_FYA_SIGNAL_GRAY);
    initTabBlock(TrafficSignalBlocks.VERTICAL_RIGHT_ADD_ON_FYA_SIGNAL_GRAY);
    initTabBlock(TrafficSignalBlocks.VERTICAL_LEFT_DOUBLE_ADD_ON_SIGNAL_GRAY);
    initTabBlock(TrafficSignalBlocks.VERTICAL_RIGHT_DOUBLE_ADD_ON_SIGNAL_GRAY);
    initTabBlock(TrafficSignalBlocks.VERTICAL_UP_LEFT_ADD_ON_SIGNAL_GRAY);
    initTabBlock(TrafficSignalBlocks.VERTICAL_UP_LEFT_SIGNAL_GRAY);
    initTabBlock(TrafficSignalBlocks.SINGLE_SOLID_SIGNAL_RED_GRAY);
    initTabBlock(TrafficSignalBlocks.SINGLE_SOLID_SIGNAL_YELLOW_GRAY);
    initTabBlock(TrafficSignalBlocks.SINGLE_SOLID_SIGNAL_GREEN_GRAY);
    initTabBlock(TrafficSignalBlocks.SINGLE_SOLID_SIGNAL_YELLOW_ADVANCE_FLASH_GRAY_A);
    initTabBlock(TrafficSignalBlocks.SINGLE_SOLID_SIGNAL_YELLOW_ADVANCE_FLASH_GRAY_B);

    // Deprecated single-section angled signal blocks
    initTabBlock(TrafficSignalBlocks.SINGLE_SOLID_SIGNAL_RED_LEFT_ANGLE);
    initTabBlock(TrafficSignalBlocks.SINGLE_SOLID_SIGNAL_RED_RIGHT_ANGLE);
    initTabBlock(TrafficSignalBlocks.SINGLE_SOLID_SIGNAL_YELLOW_LEFT_ANGLE);
    initTabBlock(TrafficSignalBlocks.SINGLE_SOLID_SIGNAL_YELLOW_RIGHT_ANGLE);
    initTabBlock(TrafficSignalBlocks.SINGLE_SOLID_SIGNAL_GREEN_LEFT_ANGLE);
    initTabBlock(TrafficSignalBlocks.SINGLE_SOLID_SIGNAL_GREEN_RIGHT_ANGLE);

    // Deprecated horizontal angled signal blocks
    initTabBlock(TrafficSignalBlocks.HORIZONTAL_ANGLE_AHEAD_SIGNAL);
    initTabBlock(TrafficSignalBlocks.HORIZONTAL_ANGLE_BIKE_SIGNAL);
    initTabBlock(TrafficSignalBlocks.HORIZONTAL_ANGLE_LEFT_SIGNAL);
    initTabBlock(TrafficSignalBlocks.HORIZONTAL_ANGLE_RAIL_SIGNAL);
    initTabBlock(TrafficSignalBlocks.HORIZONTAL_ANGLE_RIGHT2_SIGNAL);
    initTabBlock(TrafficSignalBlocks.HORIZONTAL_ANGLE_RIGHT_SIGNAL);
    initTabBlock(TrafficSignalBlocks.HORIZONTAL_ANGLE_SOLID_SIGNAL);
    initTabBlock(TrafficSignalBlocks.HORIZONTAL_ANGLE_U_TURN_SIGNAL);
    initTabBlock(TrafficSignalBlocks.HORIZONTAL_ANGLE_UP_LEFT_SIGNAL);

    // Deprecated Barlo strobe signal (now a visor type option on any signal)
    initTabBlock(TrafficSignalBlocks.VERTICAL_SOLID_SIGNAL_BARLO);

    // Deprecated crosswalk signal blocks (auto-convert to new custom-rendered equivalents)
    initTabBlock(com.micatechnologies.minecraft.csm.trafficsignals.BlockControllableCrosswalk.class, fmlPreInitializationEvent);
    initTabBlock(com.micatechnologies.minecraft.csm.trafficsignals.BlockControllableCrosswalkMount.class, fmlPreInitializationEvent);
    initTabBlock(com.micatechnologies.minecraft.csm.trafficsignals.BlockControllableCrosswalkLeftMount.class, fmlPreInitializationEvent);
    initTabBlock(com.micatechnologies.minecraft.csm.trafficsignals.BlockControllableCrosswalkRightMount.class, fmlPreInitializationEvent);
    initTabBlock(com.micatechnologies.minecraft.csm.trafficsignals.BlockControllableCrosswalkMountGray.class, fmlPreInitializationEvent);
    initTabBlock(com.micatechnologies.minecraft.csm.trafficsignals.BlockControllableCrosswalkMount90Deg.class, fmlPreInitializationEvent);
    initTabBlock(com.micatechnologies.minecraft.csm.trafficsignals.BlockControllableCrosswalkLeftMount90Deg.class, fmlPreInitializationEvent);
    initTabBlock(com.micatechnologies.minecraft.csm.trafficsignals.BlockControllableCrosswalkRightMount90Deg.class, fmlPreInitializationEvent);
    initTabBlock(com.micatechnologies.minecraft.csm.trafficsignals.BlockControllableCrosswalkDoubleWordedBaseMount.class, fmlPreInitializationEvent);
    initTabBlock(com.micatechnologies.minecraft.csm.trafficsignals.BlockControllableCrosswalkDoubleWordedLeftMount.class, fmlPreInitializationEvent);
    initTabBlock(com.micatechnologies.minecraft.csm.trafficsignals.BlockControllableCrosswalkDoubleWordedRearMount.class, fmlPreInitializationEvent);
    initTabBlock(com.micatechnologies.minecraft.csm.trafficsignals.BlockControllableCrosswalkDoubleWordedRightMount.class, fmlPreInitializationEvent);

    // Deprecated static horizontal backplates — the vertical tlborder*/tlborderaddon* blocks
    // now auto-adjust to horizontal mode when placed next to a horizontal signal, so these
    // fixed-horizontal variants are redundant. Retire each to its closest vertical analog.
    initTabBlock(new BlockTrafficAccessoryBackplate("tlhborderblack", "tlborderblackblack"));
    initTabBlock(new BlockTrafficAccessoryBackplate("tlhbordertan", "tlborderlargegray"));
    initTabBlock(new BlockTrafficAccessoryBackplate("tlhborderwhite", "tlborderwhiteblack"));
    initTabBlock(new BlockTrafficAccessoryBackplate("tlhborderyellow", "tlborderyellowblack"));

    // Deprecated horizontal angle pole + pre-attached mount variants. The base
    // trafficpolehorizontalangle{color} blocks auto-connect to adjacent mount kits now, so
    // the ..mount1/2/3 variants are redundant. Retire each color family back to its base.
    initTabBlock(new BlockTrafficAccessoryNSEWUD("trafficpolehorizontalanglemount1black", BB_DEPRECATED, BlockRenderLayer.CUTOUT_MIPPED, 2F, true, "trafficpolehorizontalangleblack"));
    initTabBlock(new BlockTrafficAccessoryNSEWUD("trafficpolehorizontalanglemount1silver", BB_DEPRECATED, BlockRenderLayer.CUTOUT_MIPPED, 2F, true, "trafficpolehorizontalanglesilver"));
    initTabBlock(new BlockTrafficAccessoryNSEWUD("trafficpolehorizontalanglemount1tan", BB_DEPRECATED, BlockRenderLayer.CUTOUT_MIPPED, 2F, true, "trafficpolehorizontalangletan"));
    initTabBlock(new BlockTrafficAccessoryNSEWUD("trafficpolehorizontalanglemount1unpainted", BB_DEPRECATED, BlockRenderLayer.CUTOUT_MIPPED, 2F, true, "trafficpolehorizontalangleunpainted"));
    initTabBlock(new BlockTrafficAccessoryNSEWUD("trafficpolehorizontalanglemount1white", BB_DEPRECATED, BlockRenderLayer.CUTOUT_MIPPED, 2F, true, "trafficpolehorizontalanglewhite"));
    initTabBlock(new BlockTrafficAccessoryNSEWUD("trafficpolehorizontalanglemount2black", BB_DEPRECATED, BlockRenderLayer.CUTOUT_MIPPED, 2F, true, "trafficpolehorizontalangleblack"));
    initTabBlock(new BlockTrafficAccessoryNSEWUD("trafficpolehorizontalanglemount2silver", BB_DEPRECATED, BlockRenderLayer.CUTOUT_MIPPED, 2F, true, "trafficpolehorizontalanglesilver"));
    initTabBlock(new BlockTrafficAccessoryNSEWUD("trafficpolehorizontalanglemount2tan", BB_DEPRECATED, BlockRenderLayer.CUTOUT_MIPPED, 2F, true, "trafficpolehorizontalangletan"));
    initTabBlock(new BlockTrafficAccessoryNSEWUD("trafficpolehorizontalanglemount2unpainted", BB_DEPRECATED, BlockRenderLayer.CUTOUT_MIPPED, 2F, true, "trafficpolehorizontalangleunpainted"));
    initTabBlock(new BlockTrafficAccessoryNSEWUD("trafficpolehorizontalanglemount2white", BB_DEPRECATED, BlockRenderLayer.CUTOUT_MIPPED, 2F, true, "trafficpolehorizontalanglewhite"));
    initTabBlock(new BlockTrafficAccessoryNSEWUD("trafficpolehorizontalanglemount3black", BB_DEPRECATED, BlockRenderLayer.CUTOUT_MIPPED, 2F, true, "trafficpolehorizontalangleblack"));
    initTabBlock(new BlockTrafficAccessoryNSEWUD("trafficpolehorizontalanglemount3silver", BB_DEPRECATED, BlockRenderLayer.CUTOUT_MIPPED, 2F, true, "trafficpolehorizontalanglesilver"));
    initTabBlock(new BlockTrafficAccessoryNSEWUD("trafficpolehorizontalanglemount3tan", BB_DEPRECATED, BlockRenderLayer.CUTOUT_MIPPED, 2F, true, "trafficpolehorizontalangletan"));
    initTabBlock(new BlockTrafficAccessoryNSEWUD("trafficpolehorizontalanglemount3unpainted", BB_DEPRECATED, BlockRenderLayer.CUTOUT_MIPPED, 2F, true, "trafficpolehorizontalangleunpainted"));
    initTabBlock(new BlockTrafficAccessoryNSEWUD("trafficpolehorizontalanglemount3white", BB_DEPRECATED, BlockRenderLayer.CUTOUT_MIPPED, 2F, true, "trafficpolehorizontalanglewhite"));

    // Deprecated straight horizontal pole w/ pre-attached single/double mount variants.
    // These classes were already ICsmRetiringBlock; just relocating them out of the visible
    // traffic accessories tab so the creative inventory isn't cluttered with redundancies.
    initTabBlock(BlockTrafficPoleHorizontalMountDouble.class, fmlPreInitializationEvent);
    initTabBlock(BlockTrafficPoleHorizontalMountDoubleBlack.class, fmlPreInitializationEvent);
    initTabBlock(BlockTrafficPoleHorizontalMountDoubleTan.class, fmlPreInitializationEvent);
    initTabBlock(BlockTrafficPoleHorizontalMountDoubleWhite.class, fmlPreInitializationEvent);
    initTabBlock(BlockTrafficPoleHorizontalSingleMount.class, fmlPreInitializationEvent);
    initTabBlock(BlockTrafficPoleHorizontalSingleMountBlack.class, fmlPreInitializationEvent);
    initTabBlock(BlockTrafficPoleHorizontalSingleMountTan.class, fmlPreInitializationEvent);
    initTabBlock(BlockTrafficPoleHorizontalSingleMountWhite.class, fmlPreInitializationEvent);
  }

  /**
   * Shared placeholder bounding box for deprecated NSEWUD factory-registered blocks in this
   * hidden tab. These blocks exist only to satisfy world-load of legacy placements; they auto-
   * retire on random tick to their live replacements, so their collision shape is only ever
   * seen briefly (if at all). Full-cube keeps selection/hit-testing sane in the meantime.
   */
  private static final AxisAlignedBB BB_DEPRECATED =
      new AxisAlignedBB(0.0, 0.0, 0.0, 1.0, 1.0, 1.0);
}

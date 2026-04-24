package com.micatechnologies.minecraft.csm.tabs;

import com.micatechnologies.minecraft.csm.CsmRegistry;
import com.micatechnologies.minecraft.csm.codeutils.CsmTab;
import com.micatechnologies.minecraft.csm.trafficaccessories.BlockMiniSolarPanel;
import com.micatechnologies.minecraft.csm.trafficaccessories.BlockTrafficAccessoryBackplate;
import com.micatechnologies.minecraft.csm.trafficaccessories.BlockTrafficAccessoryBackplateFitted;
import com.micatechnologies.minecraft.csm.trafficaccessories.BlockTrafficAccessoryNSEW;
import com.micatechnologies.minecraft.csm.trafficaccessories.BlockTrafficAccessoryNSEWUD;
import com.micatechnologies.minecraft.csm.trafficaccessories.BlockTrafficLightMountKit;
import com.micatechnologies.minecraft.csm.trafficaccessories.BlockTrafficPoleHorizontalAngleBlack;
import com.micatechnologies.minecraft.csm.trafficaccessories.BlockTrafficPoleHorizontalAngleSilver;
import com.micatechnologies.minecraft.csm.trafficaccessories.BlockTrafficPoleHorizontalAngleTan;
import com.micatechnologies.minecraft.csm.trafficaccessories.BlockTrafficPoleHorizontalAngleUnpainted;
import com.micatechnologies.minecraft.csm.trafficaccessories.BlockTrafficPoleHorizontalAngleWhite;
import com.micatechnologies.minecraft.csm.trafficaccessories.BlockTrafficPoleLargeBlack;
import com.micatechnologies.minecraft.csm.trafficaccessories.BlockTrafficPoleLargeGray;
import com.micatechnologies.minecraft.csm.trafficaccessories.BlockTrafficPoleLargeTan;
import com.micatechnologies.minecraft.csm.trafficaccessories.BlockTrafficPoleLargeUnpainted;
import com.micatechnologies.minecraft.csm.trafficaccessories.BlockTrafficPoleLargeWhite;
import com.micatechnologies.minecraft.csm.trafficaccessories.BlockTrafficPoleSmallBlack;
import com.micatechnologies.minecraft.csm.trafficaccessories.BlockTrafficPoleSmallGray;
import com.micatechnologies.minecraft.csm.trafficaccessories.BlockTrafficPoleSmallTan;
import com.micatechnologies.minecraft.csm.trafficaccessories.BlockTrafficPoleSmallUnpainted;
import com.micatechnologies.minecraft.csm.trafficaccessories.BlockTrafficPoleSmallWhite;
import com.micatechnologies.minecraft.csm.trafficaccessories.BlockTrafficSignalFatigueMitigator1;
import com.micatechnologies.minecraft.csm.trafficaccessories.BlockTrafficSignalFatigueMitigator2;
import com.micatechnologies.minecraft.csm.trafficaccessories.BlockTrafficSignalFatigueMitigator3;
import com.micatechnologies.minecraft.csm.trafficaccessories.BlockTrafficSignalFatigueMitigator4;
import com.micatechnologies.minecraft.csm.trafficaccessories.BlockTrafficStreetNameSign;
import com.micatechnologies.minecraft.csm.trafficaccessories.BlockTrafficStreetNameSignDouble;
import com.micatechnologies.minecraft.csm.trafficaccessories.BlockTrafficStreetNameSignMount;
import com.micatechnologies.minecraft.csm.trafficaccessories.BlockTrafficpolehorzdoublemountunpainted;
import com.micatechnologies.minecraft.csm.trafficaccessories.BlockTrafficpolehorzsinglemountunpainted;
import net.minecraft.block.Block;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;

/**
 * The tab for traffic accessory blocks.
 *
 * @version 2.0
 */
@CsmTab.Load(order = 9)
public class CsmTabTrafficAccessories extends CsmTab {

  /**
   * Gets the ID (unique identifier) of the tab.
   *
   * @return the ID of the tab
   *
   * @since 1.0
   */
  @Override
  public String getTabId() {
    return "tabtrafficaccessories";
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
    return CsmRegistry.getBlock("tlborderyellowblack");
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
    return false;
  }

  // Shared bounding boxes for NSEWUD factory blocks
  private static final AxisAlignedBB BB_CONTROL_BOX_LARGE =
      new AxisAlignedBB(-0.312500, -0.562500, 0.312500, 1.312500, 1.625000, 1.375000);
  private static final AxisAlignedBB BB_CONTROL_BOX_SMALL =
      new AxisAlignedBB(0.125000, -0.437500, 0.437500, 0.875000, 1.375000, 1.375000);
  private static final AxisAlignedBB BB_DMPT =
      new AxisAlignedBB(0.125000, 0.125000, -0.062500, 0.875000, 0.875000, 1.000000);
  private static final AxisAlignedBB BB_FREEWAY_CALL_BOX =
      new AxisAlignedBB(0.125000, 0.062500, 0.937500, 0.875000, 0.937500, 1.000000);
  private static final AxisAlignedBB BB_METAL_WIRE_CENTER =
      new AxisAlignedBB(0.000000, 0.000000, 0.500000, 1.000000, 0.100000, 0.600000);
  private static final AxisAlignedBB BB_METAL_WIRE_CENTER_TOP =
      new AxisAlignedBB(0.000000, 0.900000, 0.500000, 1.000000, 1.000000, 0.600000);
  private static final AxisAlignedBB BB_METAL_WIRE_OFFSET =
      new AxisAlignedBB(0.000000, 0.000000, 0.851563, 1.000000, 0.100000, 0.898438);
  private static final AxisAlignedBB BB_METAL_WIRE_OFFSET_TOP =
      new AxisAlignedBB(0.000000, 0.900000, 0.851563, 1.000000, 1.000000, 0.898438);
  private static final AxisAlignedBB BB_SIGNAL_POLE_MOUNT2 =
      new AxisAlignedBB(0.437500, -1.000000, -0.250000, 0.562500, 1.562500, 0.750000);
  private static final AxisAlignedBB BB_TL_CONTROLLER =
      new AxisAlignedBB(-0.984375, -1.000000, -0.484375, 1.984375, 2.000000, 1.343750);
  private static final AxisAlignedBB BB_TL_D_COVER =
      new AxisAlignedBB(-0.312500, -0.875000, 0.875000, 1.312500, 1.625000, 1.750000);
  private static final AxisAlignedBB BB_TL_H_COVER =
      new AxisAlignedBB(-0.687500, 0.000000, 1.062500, 1.687500, 0.875000, 1.875000);
  private static final AxisAlignedBB BB_TL_H_MOUNT_KIT =
      new AxisAlignedBB(-0.687500, 0.312500, 0.000000, 1.687500, 0.437500, 1.000000);
  private static final AxisAlignedBB BB_TL_PM =
      new AxisAlignedBB(0.062500, -0.356250, 0.062500, 0.937500, 0.581250, 0.937500);
  private static final AxisAlignedBB BB_TL_V_COVER =
      new AxisAlignedBB(0.062500, -0.812500, 0.937500, 0.937500, 1.562500, 1.750000);
  private static final AxisAlignedBB BB_TL_V_MOUNT_KIT =
      new AxisAlignedBB(0.437500, -0.812500, 0.000000, 0.562500, 1.562500, 1.000000);
  private static final AxisAlignedBB BB_TL_V_MOUNT_KIT_8_INCH =
      new AxisAlignedBB(0.437500, -0.812500, 0.000000, 0.562500, 0.812500, 1.000000);
  private static final AxisAlignedBB BB_TL_V_MOUNT_KIT_8812_INCH =
      new AxisAlignedBB(0.437500, -0.812500, 0.000000, 0.562500, 1.062500, 1.000000);
  private static final AxisAlignedBB BB_TL_V_TALL_MOUNT_KIT =
      new AxisAlignedBB(0.437500, -0.812500, 0.000000, 0.562500, 2.000000, 1.000000);
  private static final AxisAlignedBB BB_TL_V_TALL_90R_MOUNT_KIT =
      new AxisAlignedBB(-0.218750, -0.812500, 0.000000, 0.562500, 2.000000, 1.000000);
  private static final AxisAlignedBB BB_TL_V_TALL_90L_MOUNT_KIT =
      new AxisAlignedBB(0.437500, -0.812500, 0.000000, 1.218750, 2.000000, 1.000000);
  private static final AxisAlignedBB BB_TLITE_HORZ_WIRE_MOUNT =
      new AxisAlignedBB(1.437500, -0.250000, 0.500000, 1.562500, 0.000000, 0.875000);
  private static final AxisAlignedBB BB_PLUMBIZER_SIGNAL_MOUNT =
      new AxisAlignedBB(0.375000, 0.250000, 0.000000, 0.625000, 0.500000, 1.000000);
  private static final AxisAlignedBB BB_TLITE_VERT_WIRE_MOUNT =
      new AxisAlignedBB(0.437500, 0.500000, 0.437500, 0.562500, 1.000000, 0.937500);
  private static final AxisAlignedBB BB_TRAFFIC_POLE_BASE =
      new AxisAlignedBB(0.000000, 0.000000, 0.000000, 1.000000, 1.000000, 1.000000);
  private static final AxisAlignedBB BB_TRAFFIC_POLE_HORIZ_SIGN_MOUNT =
      new AxisAlignedBB(0.000000, 0.000000, 0.000000, 0.750000, 1.000000, 1.000000);
  private static final AxisAlignedBB BB_TRAFFIC_POLE_HORIZ_ANGLE_MOUNT_1 =
      new AxisAlignedBB(0.250000, 0.250000, -0.212500, 0.750000, 0.750000, 1.212500);
  private static final AxisAlignedBB BB_TRAFFIC_POLE_HORIZ_ANGLE_MOUNT_2 =
      new AxisAlignedBB(0.250000, 0.250000, -0.212500, 0.750000, 0.750000, 1.212500);
  private static final AxisAlignedBB BB_TRAFFIC_POLE_HORIZ_ANGLE_MOUNT_3 =
      new AxisAlignedBB(0.000000, 0.250000, -0.212500, 1.000000, 0.750000, 1.212500);
  private static final AxisAlignedBB BB_TRAFFIC_POLE_VERT_CONNECTOR =
      new AxisAlignedBB(0.125000, 0.000000, 0.000000, 0.875000, 1.000000, 0.875000);
  private static final AxisAlignedBB BB_TRAFFIC_POLE_VERT_CONNECTOR_ANGLED =
      new AxisAlignedBB(0.125000, 0.000000, -0.625000, 0.875000, 1.000000, 0.875000);
  private static final AxisAlignedBB BB_TRAFFIC_POLE_VERT_CONNECTOR_DOUBLE =
      new AxisAlignedBB(-0.062649, 0.000000, -0.062649, 1.062649, 1.000000, 1.062649);
  private static final AxisAlignedBB BB_TRAFFIC_POLE_VERT_CURVE_CONNECTOR =
      new AxisAlignedBB(0.250000, -0.750000, -1.000000, 0.750000, 0.750000, 2.000000);
  private static final AxisAlignedBB BB_TRAFFIC_POLE_VERT_DOUBLE_GUY_MOUNT =
      new AxisAlignedBB(-0.437500, 0.000000, 0.000000, 1.437500, 1.000000, 0.875000);
  private static final AxisAlignedBB BB_TRAFFIC_POLE_VERT_LIGHT_MOUNT =
      new AxisAlignedBB(0.312500, 0.187500, -1.000000, 0.687500, 1.187500, 1.187500);
  private static final AxisAlignedBB BB_TRAFFIC_POLE_VERT_QUAD_MOUNT =
      new AxisAlignedBB(0.000000, 0.000000, 0.000000, 1.000000, 1.000000, 1.000000);
  private static final AxisAlignedBB BB_TRAFFIC_POLE_VERT_SIGNAL_MOUNT =
      new AxisAlignedBB(0.125000, -1.000000, 0.125000, 1.562500, 2.000000, 0.875000);
  private static final AxisAlignedBB BB_TRAFFIC_POLE_HORZ_DBL =
      new AxisAlignedBB(0.250000, 0.250000, -1.000000, 0.750000, 0.750000, 1.000000);
  private static final AxisAlignedBB BB_TRAFFIC_POLE_VERT_DBL =
      new AxisAlignedBB(0.125000, 0.125000, -1.000000, 0.875000, 0.875000, 1.000000);
  private static final AxisAlignedBB BB_TL_SNOW_BEACON =
      new AxisAlignedBB(-0.500000, -1.000000, 0.250000, 1.375000, 0.631250, 0.750000);
  private static final AxisAlignedBB BB_TL_PREEMPT_BEACON =
      new AxisAlignedBB(0.000000, -0.421875, 0.406250, 0.562500, 0.156250, 0.593750);
  private static final AxisAlignedBB BB_TL_INTERCONNECT_MODULE_1 =
      new AxisAlignedBB(0.325000, 0.125000, 1.100000, 0.700000, 0.500000, 1.515625);
  private static final AxisAlignedBB BB_TL_INTERCONNECT_MODULE_2 =
      new AxisAlignedBB(0.250000, -0.375000, 0.100000, 0.656250, 1.328125, 0.587500);
  private static final AxisAlignedBB BB_TL_HANG_MOUNT =
      new AxisAlignedBB(0.500000, -0.750000, 0.750000, 0.600000, 1.609375, 0.850000);
  private static final AxisAlignedBB BB_TRAFFIC_POLE_HORIZ_SINGLE_MOUNT =
      new AxisAlignedBB(0.000000, 0.250000, 0.000000, 0.750000, 0.750000, 1.000000);
  private static final AxisAlignedBB BB_TRAFFIC_POLE_HORIZ_MOUNT_DOUBLE =
      new AxisAlignedBB(0.000000, 0.250000, 0.000000, 1.000000, 0.750000, 1.000000);

  /**
   * Initializes all the elements belonging to the tab.
   *
   * @param fmlPreInitializationEvent the {@link FMLPreInitializationEvent} that is being processed
   *
   * @since 1.0
   */
  @Override
  public void initTabElements(FMLPreInitializationEvent fmlPreInitializationEvent) {
    // --- NSEWUD factory blocks: Control Boxes ---
    initTabBlock(new BlockTrafficAccessoryNSEWUD("controlboxlarge", BB_CONTROL_BOX_LARGE, BlockRenderLayer.CUTOUT_MIPPED, 2F, false));
    initTabBlock(new BlockTrafficAccessoryNSEWUD("controlboxlargeblack", BB_CONTROL_BOX_LARGE, BlockRenderLayer.CUTOUT_MIPPED, 2F, false));
    initTabBlock(new BlockTrafficAccessoryNSEWUD("controlboxlargemattewhite", BB_CONTROL_BOX_LARGE, BlockRenderLayer.CUTOUT_MIPPED, 2F, false));
    initTabBlock(new BlockTrafficAccessoryNSEWUD("controlboxlargesilver", BB_CONTROL_BOX_LARGE, BlockRenderLayer.CUTOUT_MIPPED, 2F, false));
    initTabBlock(new BlockTrafficAccessoryNSEWUD("controlboxlargetan", BB_CONTROL_BOX_LARGE, BlockRenderLayer.CUTOUT_MIPPED, 2F, false));
    initTabBlock(new BlockTrafficAccessoryNSEWUD("controlboxlargewhite", BB_CONTROL_BOX_LARGE, BlockRenderLayer.CUTOUT_MIPPED, 2F, false));
    initTabBlock(new BlockTrafficAccessoryNSEWUD("controlboxsmallblack", BB_CONTROL_BOX_SMALL, BlockRenderLayer.CUTOUT_MIPPED, 2F, false));
    initTabBlock(new BlockTrafficAccessoryNSEWUD("controlboxsmallmattewhite", BB_CONTROL_BOX_SMALL, BlockRenderLayer.CUTOUT_MIPPED, 2F, false));
    initTabBlock(new BlockTrafficAccessoryNSEWUD("controlboxsmallmetal", BB_CONTROL_BOX_SMALL, BlockRenderLayer.CUTOUT_MIPPED, 2F, false));
    initTabBlock(new BlockTrafficAccessoryNSEWUD("controlboxsmallsilver", BB_CONTROL_BOX_SMALL, BlockRenderLayer.CUTOUT_MIPPED, 2F, false));
    initTabBlock(new BlockTrafficAccessoryNSEWUD("controlboxsmalltan", BB_CONTROL_BOX_SMALL, BlockRenderLayer.CUTOUT_MIPPED, 2F, false));
    initTabBlock(new BlockTrafficAccessoryNSEWUD("controlboxsmallwhite", BB_CONTROL_BOX_SMALL, BlockRenderLayer.CUTOUT_MIPPED, 2F, false));

    // --- NSEWUD factory blocks: DMPT ---
    initTabBlock(new BlockTrafficAccessoryNSEWUD("dmptblack", BB_DMPT, BlockRenderLayer.SOLID, 1F, true));
    initTabBlock(new BlockTrafficAccessoryNSEWUD("dmpt", BB_DMPT, BlockRenderLayer.SOLID, 1F, true));
    initTabBlock(new BlockTrafficAccessoryNSEWUD("dmpttan", BB_DMPT, BlockRenderLayer.SOLID, 1F, true));
    initTabBlock(new BlockTrafficAccessoryNSEWUD("dmptunpainted", BB_DMPT, BlockRenderLayer.SOLID, 1F, true));
    initTabBlock(new BlockTrafficAccessoryNSEWUD("dmptwhite", BB_DMPT, BlockRenderLayer.SOLID, 1F, true));

    // --- NSEWUD factory blocks: Misc ---
    initTabBlock(new BlockTrafficAccessoryNSEWUD("freewaycallbox", BB_FREEWAY_CALL_BOX, BlockRenderLayer.SOLID, 2F, false));
    initTabBlock(new BlockTrafficAccessoryNSEWUD("metalwirecenter", BB_METAL_WIRE_CENTER, BlockRenderLayer.SOLID, 2F, true));
    initTabBlock(new BlockTrafficAccessoryNSEWUD("metalwirecentertop", BB_METAL_WIRE_CENTER_TOP, BlockRenderLayer.SOLID, 2F, true));
    initTabBlock(new BlockTrafficAccessoryNSEWUD("metalwireoffset", BB_METAL_WIRE_OFFSET, BlockRenderLayer.SOLID, 2F, true));
    initTabBlock(new BlockTrafficAccessoryNSEWUD("metalwireoffsettop", BB_METAL_WIRE_OFFSET_TOP, BlockRenderLayer.SOLID, 2F, true));
    initTabBlock(new BlockTrafficAccessoryNSEW("signalpolemount2", BB_SIGNAL_POLE_MOUNT2, BlockRenderLayer.CUTOUT_MIPPED, 2F, false));

    // --- Backplate factory blocks: TLBorder5AddOn ---
    initTabBlock(new BlockTrafficAccessoryBackplate("tlborder5addonblackblack"));
    initTabBlock(new BlockTrafficAccessoryBackplate("tlborder5addonblackwhite"));
    initTabBlock(new BlockTrafficAccessoryBackplate("tlborder5addonblackyellow"));
    initTabBlock(new BlockTrafficAccessoryBackplate("tlborder5addonlargegray"));
    initTabBlock(new BlockTrafficAccessoryBackplate("tlborder5addonblueblack"));
    initTabBlock(new BlockTrafficAccessoryBackplate("tlborder5addonpinkblack"));
    initTabBlock(new BlockTrafficAccessoryBackplate("tlborder5addonblackblue"));
    initTabBlock(new BlockTrafficAccessoryBackplate("tlborder5addonblackpink"));
    initTabBlock(new BlockTrafficAccessoryBackplate("tlborder5addonwhiteblack"));
    initTabBlock(new BlockTrafficAccessoryBackplate("tlborder5addonyellowblack"));

    // --- Backplate factory blocks: TLBorderAddOn ---
    initTabBlock(new BlockTrafficAccessoryBackplate("tlborderaddonblackblack"));
    initTabBlock(new BlockTrafficAccessoryBackplate("tlborderaddonblackwhite"));
    initTabBlock(new BlockTrafficAccessoryBackplate("tlborderaddonblackyellow"));
    initTabBlock(new BlockTrafficAccessoryBackplate("tlborderaddonlargegray"));
    initTabBlock(new BlockTrafficAccessoryBackplate("tlborderaddonblueblack"));
    initTabBlock(new BlockTrafficAccessoryBackplate("tlborderaddonpinkblack"));
    initTabBlock(new BlockTrafficAccessoryBackplate("tlborderaddonblackblue"));
    initTabBlock(new BlockTrafficAccessoryBackplate("tlborderaddonblackpink"));
    initTabBlock(new BlockTrafficAccessoryBackplate("tlborderaddonwhiteblack"));
    initTabBlock(new BlockTrafficAccessoryBackplate("tlborderaddonyellowblack"));

    // --- Backplate factory blocks: TLBorder (standard 3-section) ---
    initTabBlock(new BlockTrafficAccessoryBackplate("tlborderblackblack"));
    initTabBlock(new BlockTrafficAccessoryBackplate("tlborderblackblack8812inch"));
    initTabBlock(new BlockTrafficAccessoryBackplate("tlborderblackblack8inch"));
    initTabBlock(new BlockTrafficAccessoryBackplate("tlborderblackwhite"));
    initTabBlock(new BlockTrafficAccessoryBackplate("tlborderblackyellow"));
    initTabBlock(new BlockTrafficAccessoryBackplate("tlborderlargegray"));
    initTabBlock(new BlockTrafficAccessoryBackplate("tlborderblueblack"));
    initTabBlock(new BlockTrafficAccessoryBackplate("tlborderpinkblack"));
    initTabBlock(new BlockTrafficAccessoryBackplate("tlborderblackblue"));
    initTabBlock(new BlockTrafficAccessoryBackplate("tlborderblackpink"));

    // --- Backplate factory blocks: TLBorderSingle ---
    initTabBlock(new BlockTrafficAccessoryBackplate("tlbordersingleblackblack"));
    initTabBlock(new BlockTrafficAccessoryBackplate("tlbordersingleblackwhite"));
    initTabBlock(new BlockTrafficAccessoryBackplate("tlbordersingleblackyellow"));
    initTabBlock(new BlockTrafficAccessoryBackplate("tlbordersinglelargegray"));
    initTabBlock(new BlockTrafficAccessoryBackplate("tlbordersinglewhiteblack"));
    initTabBlock(new BlockTrafficAccessoryBackplate("tlbordersingleyellowblack"));
    initTabBlock(new BlockTrafficAccessoryBackplate("tlbordersingleblackblue"));
    initTabBlock(new BlockTrafficAccessoryBackplate("tlbordersingleblackpink"));
    initTabBlock(new BlockTrafficAccessoryBackplate("tlbordersingleblueblack"));
    initTabBlock(new BlockTrafficAccessoryBackplate("tlbordersinglepinkblack"));

    // --- Backplate factory blocks: TLBorder remaining color combos ---
    initTabBlock(new BlockTrafficAccessoryBackplate("tlborderwhiteblack"));
    initTabBlock(new BlockTrafficAccessoryBackplate("tlborderwhiteblack8812inch"));
    initTabBlock(new BlockTrafficAccessoryBackplate("tlborderwhiteblack8inch"));
    initTabBlock(new BlockTrafficAccessoryBackplate("tlborderyellowblack"));
    initTabBlock(new BlockTrafficAccessoryBackplate("tlborderyellowblack8812inch"));
    initTabBlock(new BlockTrafficAccessoryBackplate("tlborderyellowblack8inch"));
    initTabBlock(new BlockTrafficAccessoryBackplate("tlborderblueblack8inch"));
    initTabBlock(new BlockTrafficAccessoryBackplate("tlborderpinkblack8inch"));
    initTabBlock(new BlockTrafficAccessoryBackplate("tlbordergraygray8inch"));
    initTabBlock(new BlockTrafficAccessoryBackplate("tlborderblueblack8812inch"));
    initTabBlock(new BlockTrafficAccessoryBackplate("tlborderpinkblack8812inch"));
    initTabBlock(new BlockTrafficAccessoryBackplate("tlbordergraygray8812inch"));

    // --- NSEWUD factory blocks: TL Controllers ---
    initTabBlock(new BlockTrafficAccessoryNSEWUD("tlcontroller", BB_TL_CONTROLLER, BlockRenderLayer.CUTOUT_MIPPED, 2F, false));
    initTabBlock(new BlockTrafficAccessoryNSEWUD("tlcontrollerblack", BB_TL_CONTROLLER, BlockRenderLayer.CUTOUT_MIPPED, 2F, false));
    initTabBlock(new BlockTrafficAccessoryNSEWUD("tlcontrollermattewhite", BB_TL_CONTROLLER, BlockRenderLayer.CUTOUT_MIPPED, 2F, false));
    initTabBlock(new BlockTrafficAccessoryNSEWUD("tlcontrollersilver", BB_TL_CONTROLLER, BlockRenderLayer.CUTOUT_MIPPED, 2F, false));
    initTabBlock(new BlockTrafficAccessoryNSEWUD("tlcontrollertan", BB_TL_CONTROLLER, BlockRenderLayer.CUTOUT_MIPPED, 2F, false));
    initTabBlock(new BlockTrafficAccessoryNSEWUD("tlcontrollerwhite", BB_TL_CONTROLLER, BlockRenderLayer.CUTOUT_MIPPED, 2F, false));

    // --- NSEWUD factory blocks: TL Covers ---
    initTabBlock(new BlockTrafficAccessoryNSEWUD("tldcover", BB_TL_D_COVER, BlockRenderLayer.SOLID, 2F, true));

    // --- Backplate fitted factory blocks: Doghouse ---
    initTabBlock(new BlockTrafficAccessoryBackplateFitted("tldoghouseborderblackblack"));
    initTabBlock(new BlockTrafficAccessoryBackplateFitted("tldoghouseborderblackwhite"));
    initTabBlock(new BlockTrafficAccessoryBackplateFitted("tldoghouseborderblackyellow"));
    initTabBlock(new BlockTrafficAccessoryBackplateFitted("tldoghousebordergraygray"));
    initTabBlock(new BlockTrafficAccessoryBackplateFitted("tldoghouseborderblueblack"));
    initTabBlock(new BlockTrafficAccessoryBackplateFitted("tldoghouseborderpinkblack"));
    initTabBlock(new BlockTrafficAccessoryBackplateFitted("tldoghouseborderblackblue"));
    initTabBlock(new BlockTrafficAccessoryBackplateFitted("tldoghouseborderblackpink"));
    initTabBlock(new BlockTrafficAccessoryBackplateFitted("tldoghouseborderwhiteblack"));
    initTabBlock(new BlockTrafficAccessoryBackplateFitted("tldoghouseborderyellowblack"));

    // --- Backplate fitted factory blocks: Hawk ---
    initTabBlock(new BlockTrafficAccessoryBackplateFitted("tlhawkborderblackblack"));
    initTabBlock(new BlockTrafficAccessoryBackplateFitted("tlhawkborderblackwhite"));
    initTabBlock(new BlockTrafficAccessoryBackplateFitted("tlhawkborderblackyellow"));
    initTabBlock(new BlockTrafficAccessoryBackplateFitted("tlhawkbordergraygray"));
    initTabBlock(new BlockTrafficAccessoryBackplateFitted("tlhawkborderblueblack"));
    initTabBlock(new BlockTrafficAccessoryBackplateFitted("tlhawkborderpinkblack"));
    initTabBlock(new BlockTrafficAccessoryBackplateFitted("tlhawkborderblackblue"));
    initTabBlock(new BlockTrafficAccessoryBackplateFitted("tlhawkborderblackpink"));
    initTabBlock(new BlockTrafficAccessoryBackplateFitted("tlhawkborderwhiteblack"));
    initTabBlock(new BlockTrafficAccessoryBackplateFitted("tlhawkborderyellowblack"));

    // --- Backplate factory blocks: TLHBorder ---

    // --- NSEWUD factory blocks: TL Covers and Mount Kits ---
    initTabBlock(new BlockTrafficAccessoryNSEWUD("tlhcover", BB_TL_H_COVER, BlockRenderLayer.SOLID, 2F, true));
    initTabBlock(new BlockTrafficAccessoryNSEW("tlhmountkit", BB_TL_H_MOUNT_KIT, BlockRenderLayer.CUTOUT_MIPPED, 2F, false));
    initTabBlock(new BlockTrafficAccessoryNSEWUD("tlpmblack", BB_TL_PM, BlockRenderLayer.CUTOUT_MIPPED, 2F, true));
    initTabBlock(new BlockTrafficAccessoryNSEWUD("tlpmsilver", BB_TL_PM, BlockRenderLayer.CUTOUT_MIPPED, 2F, true));
    initTabBlock(new BlockTrafficAccessoryNSEWUD("tlpmtan", BB_TL_PM, BlockRenderLayer.CUTOUT_MIPPED, 2F, true));
    initTabBlock(new BlockTrafficAccessoryNSEWUD("tlpmwhite", BB_TL_PM, BlockRenderLayer.CUTOUT_MIPPED, 2F, true));
    initTabBlock(new BlockTrafficAccessoryNSEWUD("tlvcover", BB_TL_V_COVER, BlockRenderLayer.SOLID, 2F, true));
    initTabBlock(new BlockTrafficAccessoryNSEW("tlvmountkit", BB_TL_V_MOUNT_KIT, BlockRenderLayer.CUTOUT_MIPPED, 2F, false));
    initTabBlock(new BlockTrafficAccessoryNSEW("tlvmountkit8812inch", BB_TL_V_MOUNT_KIT_8812_INCH, BlockRenderLayer.CUTOUT_MIPPED, 2F, false));
    initTabBlock(new BlockTrafficAccessoryNSEW("tlvmountkit8inch", BB_TL_V_MOUNT_KIT_8_INCH, BlockRenderLayer.CUTOUT_MIPPED, 2F, false));
    initTabBlock(new BlockTrafficAccessoryNSEW("tlvtall90lmountkit", BB_TL_V_TALL_90L_MOUNT_KIT, BlockRenderLayer.CUTOUT_MIPPED, 2F, false));
    initTabBlock(new BlockTrafficAccessoryNSEW("tlvtall90rmountkit", BB_TL_V_TALL_90R_MOUNT_KIT, BlockRenderLayer.CUTOUT_MIPPED, 2F, false));
    initTabBlock(new BlockTrafficAccessoryNSEW("tlvtallmountkit", BB_TL_V_TALL_MOUNT_KIT, BlockRenderLayer.CUTOUT_MIPPED, 2F, false));
    initTabBlock(new BlockTrafficLightMountKit());
    initTabBlock(new BlockTrafficAccessoryNSEW("plumbizer_signal_mount", BB_PLUMBIZER_SIGNAL_MOUNT,
        BlockRenderLayer.CUTOUT_MIPPED, 2F, false));
    initTabBlock(new BlockTrafficAccessoryNSEWUD("tlitehorzwiremount", BB_TLITE_HORZ_WIRE_MOUNT, BlockRenderLayer.SOLID, 1F, true));
    initTabBlock(new BlockTrafficAccessoryNSEWUD("tlitevertwiremount", BB_TLITE_VERT_WIRE_MOUNT, BlockRenderLayer.SOLID, 1F, true));
    initTabBlock(new BlockTrafficAccessoryNSEWUD("tlpmunpainted", BB_TL_PM, BlockRenderLayer.CUTOUT_MIPPED, 2F, true));

    // --- NSEWUD factory blocks: Traffic Pole Bases ---
    initTabBlock(new BlockTrafficAccessoryNSEWUD("trafficpolebaseblack", BB_TRAFFIC_POLE_BASE, BlockRenderLayer.CUTOUT_MIPPED, 2F, true));
    initTabBlock(new BlockTrafficAccessoryNSEWUD("trafficpolebasesilver", BB_TRAFFIC_POLE_BASE, BlockRenderLayer.CUTOUT_MIPPED, 2F, true));
    initTabBlock(new BlockTrafficAccessoryNSEWUD("trafficpolebasetan", BB_TRAFFIC_POLE_BASE, BlockRenderLayer.CUTOUT_MIPPED, 2F, true));
    initTabBlock(new BlockTrafficAccessoryNSEWUD("trafficpolebaseunpainted", BB_TRAFFIC_POLE_BASE, BlockRenderLayer.CUTOUT_MIPPED, 2F, true));
    initTabBlock(new BlockTrafficAccessoryNSEWUD("trafficpolebasewhite", BB_TRAFFIC_POLE_BASE, BlockRenderLayer.CUTOUT_MIPPED, 2F, true));

    // --- NSEWUD factory blocks: Horizontal Sign Mounts ---
    initTabBlock(new BlockTrafficAccessoryNSEWUD("trafficpolehorizsignmountblack", BB_TRAFFIC_POLE_HORIZ_SIGN_MOUNT, BlockRenderLayer.CUTOUT_MIPPED, 2F, true));
    initTabBlock(new BlockTrafficAccessoryNSEWUD("trafficpolehorizsignmountsilver", BB_TRAFFIC_POLE_HORIZ_SIGN_MOUNT, BlockRenderLayer.CUTOUT_MIPPED, 2F, true));
    initTabBlock(new BlockTrafficAccessoryNSEWUD("trafficpolehorizsignmounttan", BB_TRAFFIC_POLE_HORIZ_SIGN_MOUNT, BlockRenderLayer.CUTOUT_MIPPED, 2F, true));
    initTabBlock(new BlockTrafficAccessoryNSEWUD("trafficpolehorizsignmountwhite", BB_TRAFFIC_POLE_HORIZ_SIGN_MOUNT, BlockRenderLayer.CUTOUT_MIPPED, 2F, true));

    // --- Class-based: Traffic Poles (TrafficPole/Diagonal/HZEight) ---
    initTabBlock(BlockTrafficPoleSmallGray.class, fmlPreInitializationEvent);
    initTabBlock(BlockTrafficPoleHorizontalAngleBlack.class, fmlPreInitializationEvent);

    // --- NSEWUD factory blocks: Horizontal Angle Mounts ---

    // --- Class-based: Traffic Pole Angles (HZEight) ---
    initTabBlock(BlockTrafficPoleHorizontalAngleSilver.class, fmlPreInitializationEvent);
    initTabBlock(BlockTrafficPoleHorizontalAngleTan.class, fmlPreInitializationEvent);
    initTabBlock(BlockTrafficPoleHorizontalAngleUnpainted.class, fmlPreInitializationEvent);
    initTabBlock(BlockTrafficPoleHorizontalAngleWhite.class, fmlPreInitializationEvent);
    initTabBlock(BlockTrafficPoleSmallBlack.class, fmlPreInitializationEvent);


    // --- Class-based: Traffic Poles (SmallTan, SmallWhite) ---
    initTabBlock(BlockTrafficPoleSmallTan.class, fmlPreInitializationEvent);
    initTabBlock(BlockTrafficPoleSmallWhite.class, fmlPreInitializationEvent);

    // --- Class-based: Traffic Pole Large (vertical) ---
    initTabBlock(BlockTrafficPoleLargeGray.class, fmlPreInitializationEvent);
    initTabBlock(BlockTrafficPoleLargeBlack.class, fmlPreInitializationEvent);

    // --- NSEWUD factory blocks: Vertical Connectors ---
    initTabBlock(new BlockTrafficAccessoryNSEWUD("trafficpoleverticalconnector", BB_TRAFFIC_POLE_VERT_CONNECTOR, BlockRenderLayer.CUTOUT_MIPPED, 2F, true));
    initTabBlock(new BlockTrafficAccessoryNSEWUD("trafficpoleverticalconnectorangledblack", BB_TRAFFIC_POLE_VERT_CONNECTOR_ANGLED, BlockRenderLayer.CUTOUT_MIPPED, 2F, true));
    initTabBlock(new BlockTrafficAccessoryNSEWUD("trafficpoleverticalconnectorangledsilver", BB_TRAFFIC_POLE_VERT_CONNECTOR_ANGLED, BlockRenderLayer.CUTOUT_MIPPED, 2F, true));
    initTabBlock(new BlockTrafficAccessoryNSEWUD("trafficpoleverticalconnectorangledtan", BB_TRAFFIC_POLE_VERT_CONNECTOR_ANGLED, BlockRenderLayer.CUTOUT_MIPPED, 2F, true));
    initTabBlock(new BlockTrafficAccessoryNSEWUD("trafficpoleverticalconnectorangledunpainted", BB_TRAFFIC_POLE_VERT_CONNECTOR_ANGLED, BlockRenderLayer.CUTOUT_MIPPED, 2F, true));
    initTabBlock(new BlockTrafficAccessoryNSEWUD("trafficpoleverticalconnectorangledwhite", BB_TRAFFIC_POLE_VERT_CONNECTOR_ANGLED, BlockRenderLayer.CUTOUT_MIPPED, 2F, true));
    initTabBlock(new BlockTrafficAccessoryNSEWUD("trafficpoleverticalconnectorblack", BB_TRAFFIC_POLE_VERT_CONNECTOR, BlockRenderLayer.CUTOUT_MIPPED, 2F, true));
    initTabBlock(new BlockTrafficAccessoryNSEWUD("trafficpoleverticalconnectordoubleblack", BB_TRAFFIC_POLE_VERT_CONNECTOR_DOUBLE, BlockRenderLayer.CUTOUT_MIPPED, 2F, true));
    initTabBlock(new BlockTrafficAccessoryNSEWUD("trafficpoleverticalconnectordoublesilver", BB_TRAFFIC_POLE_VERT_CONNECTOR_DOUBLE, BlockRenderLayer.CUTOUT_MIPPED, 2F, true));
    initTabBlock(new BlockTrafficAccessoryNSEWUD("trafficpoleverticalconnectordoubletan", BB_TRAFFIC_POLE_VERT_CONNECTOR_DOUBLE, BlockRenderLayer.CUTOUT_MIPPED, 2F, true));
    initTabBlock(new BlockTrafficAccessoryNSEWUD("trafficpoleverticalconnectordoubleunpainted", BB_TRAFFIC_POLE_VERT_CONNECTOR_DOUBLE, BlockRenderLayer.CUTOUT_MIPPED, 2F, true));
    initTabBlock(new BlockTrafficAccessoryNSEWUD("trafficpoleverticalconnectordoublewhite", BB_TRAFFIC_POLE_VERT_CONNECTOR_DOUBLE, BlockRenderLayer.CUTOUT_MIPPED, 2F, true));
    initTabBlock(new BlockTrafficAccessoryNSEWUD("trafficpoleverticalconnectortan", BB_TRAFFIC_POLE_VERT_CONNECTOR, BlockRenderLayer.CUTOUT_MIPPED, 2F, true));
    initTabBlock(new BlockTrafficAccessoryNSEWUD("trafficpoleverticalconnectorwhite", BB_TRAFFIC_POLE_VERT_CONNECTOR, BlockRenderLayer.CUTOUT_MIPPED, 2F, true));

    // --- NSEWUD factory blocks: Vertical Curve Connectors ---
    initTabBlock(new BlockTrafficAccessoryNSEWUD("trafficpoleverticalcurveconnector", BB_TRAFFIC_POLE_VERT_CURVE_CONNECTOR, BlockRenderLayer.CUTOUT_MIPPED, 2F, true));
    initTabBlock(new BlockTrafficAccessoryNSEWUD("trafficpoleverticalcurveconnectorblack", BB_TRAFFIC_POLE_VERT_CURVE_CONNECTOR, BlockRenderLayer.CUTOUT_MIPPED, 2F, true));
    initTabBlock(new BlockTrafficAccessoryNSEWUD("trafficpoleverticalcurveconnectordoubleguyblack", BB_TRAFFIC_POLE_VERT_CURVE_CONNECTOR, BlockRenderLayer.CUTOUT_MIPPED, 2F, true));
    initTabBlock(new BlockTrafficAccessoryNSEWUD("trafficpoleverticalcurveconnectordoubleguysilver", BB_TRAFFIC_POLE_VERT_CURVE_CONNECTOR, BlockRenderLayer.CUTOUT_MIPPED, 2F, true));
    initTabBlock(new BlockTrafficAccessoryNSEWUD("trafficpoleverticalcurveconnectordoubleguytan", BB_TRAFFIC_POLE_VERT_CURVE_CONNECTOR, BlockRenderLayer.CUTOUT_MIPPED, 2F, true));
    initTabBlock(new BlockTrafficAccessoryNSEWUD("trafficpoleverticalcurveconnectordoubleguyunpainted", BB_TRAFFIC_POLE_VERT_CURVE_CONNECTOR, BlockRenderLayer.CUTOUT_MIPPED, 2F, true));
    initTabBlock(new BlockTrafficAccessoryNSEWUD("trafficpoleverticalcurveconnectordoubleguywhite", BB_TRAFFIC_POLE_VERT_CURVE_CONNECTOR, BlockRenderLayer.CUTOUT_MIPPED, 2F, true));
    initTabBlock(new BlockTrafficAccessoryNSEWUD("trafficpoleverticalcurveconnectortan", BB_TRAFFIC_POLE_VERT_CURVE_CONNECTOR, BlockRenderLayer.CUTOUT_MIPPED, 2F, true));
    initTabBlock(new BlockTrafficAccessoryNSEWUD("trafficpoleverticalcurveconnectorwhite", BB_TRAFFIC_POLE_VERT_CURVE_CONNECTOR, BlockRenderLayer.CUTOUT_MIPPED, 2F, true));

    // --- NSEWUD factory blocks: Vertical Double Guy Mounts ---
    initTabBlock(new BlockTrafficAccessoryNSEWUD("trafficpoleverticaldoubleguymountblack", BB_TRAFFIC_POLE_VERT_DOUBLE_GUY_MOUNT, BlockRenderLayer.CUTOUT_MIPPED, 2F, true));
    initTabBlock(new BlockTrafficAccessoryNSEWUD("trafficpoleverticaldoubleguymountsilver", BB_TRAFFIC_POLE_VERT_DOUBLE_GUY_MOUNT, BlockRenderLayer.CUTOUT_MIPPED, 2F, true));
    initTabBlock(new BlockTrafficAccessoryNSEWUD("trafficpoleverticaldoubleguymounttan", BB_TRAFFIC_POLE_VERT_DOUBLE_GUY_MOUNT, BlockRenderLayer.CUTOUT_MIPPED, 2F, true));
    initTabBlock(new BlockTrafficAccessoryNSEWUD("trafficpoleverticaldoubleguymountunpainted", BB_TRAFFIC_POLE_VERT_DOUBLE_GUY_MOUNT, BlockRenderLayer.CUTOUT_MIPPED, 2F, true));
    initTabBlock(new BlockTrafficAccessoryNSEWUD("trafficpoleverticaldoubleguymountwhite", BB_TRAFFIC_POLE_VERT_DOUBLE_GUY_MOUNT, BlockRenderLayer.CUTOUT_MIPPED, 2F, true));

    // --- NSEWUD factory blocks: Vertical Light Mounts ---
    initTabBlock(new BlockTrafficAccessoryNSEWUD("trafficpoleverticallightmount", BB_TRAFFIC_POLE_VERT_LIGHT_MOUNT, BlockRenderLayer.CUTOUT_MIPPED, 2F, true));
    initTabBlock(new BlockTrafficAccessoryNSEWUD("trafficpoleverticallightmountblack", BB_TRAFFIC_POLE_VERT_LIGHT_MOUNT, BlockRenderLayer.CUTOUT_MIPPED, 2F, true));
    initTabBlock(new BlockTrafficAccessoryNSEWUD("trafficpoleverticallightmounttan", BB_TRAFFIC_POLE_VERT_LIGHT_MOUNT, BlockRenderLayer.CUTOUT_MIPPED, 2F, true));
    initTabBlock(new BlockTrafficAccessoryNSEWUD("trafficpoleverticallightmountunpainted", BB_TRAFFIC_POLE_VERT_LIGHT_MOUNT, BlockRenderLayer.CUTOUT_MIPPED, 2F, false));
    initTabBlock(new BlockTrafficAccessoryNSEWUD("trafficpoleverticallightmountwhite", BB_TRAFFIC_POLE_VERT_LIGHT_MOUNT, BlockRenderLayer.CUTOUT_MIPPED, 2F, true));

    // --- NSEWUD factory blocks: Vertical Quad Mounts ---
    initTabBlock(new BlockTrafficAccessoryNSEWUD("trafficpoleverticalquadmount", BB_TRAFFIC_POLE_VERT_QUAD_MOUNT, BlockRenderLayer.CUTOUT_MIPPED, 2F, true));
    initTabBlock(new BlockTrafficAccessoryNSEWUD("trafficpoleverticalquadmountblack", BB_TRAFFIC_POLE_VERT_QUAD_MOUNT, BlockRenderLayer.CUTOUT_MIPPED, 2F, true));
    initTabBlock(new BlockTrafficAccessoryNSEWUD("trafficpoleverticalquadmounttan", BB_TRAFFIC_POLE_VERT_QUAD_MOUNT, BlockRenderLayer.CUTOUT_MIPPED, 2F, true));
    initTabBlock(new BlockTrafficAccessoryNSEWUD("trafficpoleverticalquadmountwhite", BB_TRAFFIC_POLE_VERT_QUAD_MOUNT, BlockRenderLayer.CUTOUT_MIPPED, 2F, true));

    // --- NSEWUD factory blocks: Vertical Signal Mounts ---
    initTabBlock(new BlockTrafficAccessoryNSEWUD("trafficpoleverticalsignalmount", BB_TRAFFIC_POLE_VERT_SIGNAL_MOUNT, BlockRenderLayer.CUTOUT_MIPPED, 2F, true));
    initTabBlock(new BlockTrafficAccessoryNSEWUD("trafficpoleverticalsignalmountblack", BB_TRAFFIC_POLE_VERT_SIGNAL_MOUNT, BlockRenderLayer.CUTOUT_MIPPED, 2F, true));
    initTabBlock(new BlockTrafficAccessoryNSEWUD("trafficpoleverticalsignalmounttan", BB_TRAFFIC_POLE_VERT_SIGNAL_MOUNT, BlockRenderLayer.CUTOUT_MIPPED, 2F, true));
    initTabBlock(new BlockTrafficAccessoryNSEWUD("trafficpoleverticalsignalmountwhite", BB_TRAFFIC_POLE_VERT_SIGNAL_MOUNT, BlockRenderLayer.CUTOUT_MIPPED, 2F, true));

    // --- Class-based: Traffic Pole Large (Tan, White) ---
    initTabBlock(BlockTrafficPoleLargeTan.class, fmlPreInitializationEvent);
    initTabBlock(BlockTrafficPoleLargeWhite.class, fmlPreInitializationEvent);

    // --- Class-based: Fatigue Mitigators ---
    initTabBlock(BlockTrafficSignalFatigueMitigator1.class, fmlPreInitializationEvent);
    initTabBlock(BlockTrafficSignalFatigueMitigator2.class, fmlPreInitializationEvent);
    initTabBlock(BlockTrafficSignalFatigueMitigator3.class, fmlPreInitializationEvent);
    initTabBlock(BlockTrafficSignalFatigueMitigator4.class, fmlPreInitializationEvent);

    // --- NSEWUD factory blocks: Signal Accessories ---
    initTabBlock(new BlockTrafficAccessoryNSEWUD("tlhangmount", BB_TL_HANG_MOUNT, BlockRenderLayer.CUTOUT_MIPPED, 2F, false));
    initTabBlock(new BlockTrafficAccessoryNSEWUD("tlinterconnectmodule1", BB_TL_INTERCONNECT_MODULE_1, BlockRenderLayer.CUTOUT_MIPPED, 2F, false));
    initTabBlock(new BlockTrafficAccessoryNSEWUD("tlinterconnectmodule2", BB_TL_INTERCONNECT_MODULE_2, BlockRenderLayer.CUTOUT_MIPPED, 2F, false));
    initTabBlock(new BlockTrafficAccessoryNSEWUD("tlpreemptbeacon", BB_TL_PREEMPT_BEACON, BlockRenderLayer.CUTOUT_MIPPED, 2F, false));
    initTabBlock(new BlockTrafficAccessoryNSEWUD("tlsnowbeacon", BB_TL_SNOW_BEACON, BlockRenderLayer.CUTOUT_MIPPED, 2F, false));

    // --- Class-based: Street Name Signs (NSEW, not NSEWUD) ---
    initTabBlock(BlockTrafficStreetNameSign.class, fmlPreInitializationEvent);
    initTabBlock(BlockTrafficStreetNameSignDouble.class, fmlPreInitializationEvent);
    initTabBlock(BlockTrafficStreetNameSignMount.class, fmlPreInitializationEvent);

    // --- NSEWUD factory blocks: Horizontal Double Poles ---
    initTabBlock(new BlockTrafficAccessoryNSEWUD("trafficpolehorzdblblack", BB_TRAFFIC_POLE_HORZ_DBL, BlockRenderLayer.CUTOUT_MIPPED, 2F, true));
    initTabBlock(new BlockTrafficAccessoryNSEWUD("trafficpolehorzdblconcrete", BB_TRAFFIC_POLE_HORZ_DBL, BlockRenderLayer.CUTOUT_MIPPED, 2F, true));
    initTabBlock(new BlockTrafficAccessoryNSEWUD("trafficpolehorzdblsilver", BB_TRAFFIC_POLE_HORZ_DBL, BlockRenderLayer.CUTOUT_MIPPED, 2F, true));
    initTabBlock(new BlockTrafficAccessoryNSEWUD("trafficpolehorzdbltan", BB_TRAFFIC_POLE_HORZ_DBL, BlockRenderLayer.CUTOUT_MIPPED, 2F, true));
    initTabBlock(new BlockTrafficAccessoryNSEWUD("trafficpolehorzdblunpainted", BB_TRAFFIC_POLE_HORZ_DBL, BlockRenderLayer.CUTOUT_MIPPED, 2F, true));
    initTabBlock(new BlockTrafficAccessoryNSEWUD("trafficpolehorzdblwhite", BB_TRAFFIC_POLE_HORZ_DBL, BlockRenderLayer.CUTOUT_MIPPED, 2F, true));

    // --- Class-based: ICsmRetiringBlock (7 overrides, unpainted variants) ---
    initTabBlock(BlockTrafficpolehorzdoublemountunpainted.class, fmlPreInitializationEvent);
    initTabBlock(new BlockTrafficAccessoryNSEWUD("trafficpolehorzsignmountunpainted", BB_TRAFFIC_POLE_HORIZ_SIGN_MOUNT, BlockRenderLayer.CUTOUT_MIPPED, 2F, true));
    initTabBlock(BlockTrafficpolehorzsinglemountunpainted.class, fmlPreInitializationEvent);

    // --- Class-based: Traffic Pole Small (Unpainted) ---
    initTabBlock(BlockTrafficPoleSmallUnpainted.class, fmlPreInitializationEvent);

    // --- NSEWUD factory blocks: Vertical Double Poles ---
    initTabBlock(new BlockTrafficAccessoryNSEWUD("trafficpolevertdblblack", BB_TRAFFIC_POLE_VERT_DBL, BlockRenderLayer.CUTOUT_MIPPED, 2F, true));
    initTabBlock(new BlockTrafficAccessoryNSEWUD("trafficpolevertdblconcrete", BB_TRAFFIC_POLE_VERT_DBL, BlockRenderLayer.CUTOUT_MIPPED, 2F, true));
    initTabBlock(new BlockTrafficAccessoryNSEWUD("trafficpolevertdblsilver", BB_TRAFFIC_POLE_VERT_DBL, BlockRenderLayer.CUTOUT_MIPPED, 2F, true));
    initTabBlock(new BlockTrafficAccessoryNSEWUD("trafficpolevertdbltan", BB_TRAFFIC_POLE_VERT_DBL, BlockRenderLayer.CUTOUT_MIPPED, 2F, true));
    initTabBlock(new BlockTrafficAccessoryNSEWUD("trafficpolevertdblunpainted", BB_TRAFFIC_POLE_VERT_DBL, BlockRenderLayer.CUTOUT_MIPPED, 2F, true));
    initTabBlock(new BlockTrafficAccessoryNSEWUD("trafficpolevertdblwhite", BB_TRAFFIC_POLE_VERT_DBL, BlockRenderLayer.CUTOUT_MIPPED, 2F, true));

    // --- NSEWUD factory blocks: Legacy lowercase connectors/mounts ---
    initTabBlock(new BlockTrafficAccessoryNSEWUD("trafficpoleverticalconnectorconcrete", BB_TRAFFIC_POLE_VERT_CONNECTOR, BlockRenderLayer.CUTOUT_MIPPED, 2F, true));
    initTabBlock(new BlockTrafficAccessoryNSEWUD("trafficpoleverticalconnectorunpainted", BB_TRAFFIC_POLE_VERT_CONNECTOR, BlockRenderLayer.CUTOUT_MIPPED, 2F, true));
    initTabBlock(new BlockTrafficAccessoryNSEWUD("trafficpoleverticalcurveconnectorunpainted", BB_TRAFFIC_POLE_VERT_CURVE_CONNECTOR, BlockRenderLayer.CUTOUT_MIPPED, 2F, true));
    initTabBlock(new BlockTrafficAccessoryNSEWUD("trafficpoleverticalquadmountconcrete", BB_TRAFFIC_POLE_VERT_QUAD_MOUNT, BlockRenderLayer.CUTOUT_MIPPED, 2F, true));
    initTabBlock(new BlockTrafficAccessoryNSEWUD("trafficpoleverticalquadmountunpainted", BB_TRAFFIC_POLE_VERT_QUAD_MOUNT, BlockRenderLayer.CUTOUT_MIPPED, 2F, true));
    initTabBlock(new BlockTrafficAccessoryNSEWUD("trafficpoleverticalsignalmountconcrete", BB_TRAFFIC_POLE_VERT_SIGNAL_MOUNT, BlockRenderLayer.CUTOUT_MIPPED, 2F, true));
    initTabBlock(new BlockTrafficAccessoryNSEWUD("trafficpoleverticalsignalmountunpainted", BB_TRAFFIC_POLE_VERT_SIGNAL_MOUNT, BlockRenderLayer.CUTOUT_MIPPED, 2F, true));

    // --- Class-based: Traffic Pole Large (Unpainted) ---
    initTabBlock(BlockTrafficPoleLargeUnpainted.class, fmlPreInitializationEvent);

    // --- Class-based: Mini Solar Panel ---
    initTabBlock(BlockMiniSolarPanel.class, fmlPreInitializationEvent);

    // --- Class-based: Portable Message Sign ---
    initTabBlock(com.micatechnologies.minecraft.csm.trafficaccessories.BlockPortableMessageSign.class,
        fmlPreInitializationEvent);
  }
}

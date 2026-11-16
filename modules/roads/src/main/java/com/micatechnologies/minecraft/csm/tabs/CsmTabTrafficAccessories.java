package com.micatechnologies.minecraft.csm.tabs;

import com.micatechnologies.minecraft.csm.CsmRegistry;
import com.micatechnologies.minecraft.csm.codeutils.CsmTab;
import com.micatechnologies.minecraft.csm.trafficaccessories.BlockAlprCameraSolar;
import com.micatechnologies.minecraft.csm.trafficaccessories.BlockAlprCameraSolarWall;
import com.micatechnologies.minecraft.csm.trafficaccessories.BlockEnforcementCamera;
import com.micatechnologies.minecraft.csm.trafficaccessories.BlockMiniSolarPanel;
import com.micatechnologies.minecraft.csm.trafficaccessories.BlockPreemptBeacon;
import com.micatechnologies.minecraft.csm.trafficaccessories.BlockPreemptConfirmDomeRed;
import com.micatechnologies.minecraft.csm.trafficaccessories.BlockPreemptConfirmDomeRedPole;
import com.micatechnologies.minecraft.csm.trafficaccessories.BlockPreemptConfirmDomeRedSpan;
import com.micatechnologies.minecraft.csm.trafficaccessories.BlockPreemptConfirmParBlue;
import com.micatechnologies.minecraft.csm.trafficaccessories.BlockPreemptConfirmParBluePole;
import com.micatechnologies.minecraft.csm.trafficaccessories.BlockPreemptConfirmParBlueSpan;
import com.micatechnologies.minecraft.csm.trafficaccessories.BlockPreemptConfirmParWhite;
import com.micatechnologies.minecraft.csm.trafficaccessories.BlockPreemptConfirmParWhitePole;
import com.micatechnologies.minecraft.csm.trafficaccessories.BlockPreemptConfirmParWhiteSpan;
import com.micatechnologies.minecraft.csm.trafficaccessories.BlockSnowBeacon;
import com.micatechnologies.minecraft.csm.trafficaccessories.BlockTrafficAccessoryBackplate;
import com.micatechnologies.minecraft.csm.trafficaccessories.BlockTrafficAccessoryBackplateFitted;
import com.micatechnologies.minecraft.csm.trafficaccessories.BlockTrafficAccessoryNSEW;
import com.micatechnologies.minecraft.csm.trafficaccessories.BlockSignalMountKit;
import com.micatechnologies.minecraft.csm.trafficaccessories.BlockTrafficAccessoryNSEWUD;
import com.micatechnologies.minecraft.csm.trafficaccessories.BlockTrafficLightCover;
import com.micatechnologies.minecraft.csm.trafficaccessories.BlockTrafficLightMountKit;
import com.micatechnologies.minecraft.csm.trafficaccessories.spanwire.BlockSignalClusterHanger;
import com.micatechnologies.minecraft.csm.trafficaccessories.spanwire.BlockSpanWireAnchor;
import com.micatechnologies.minecraft.csm.trafficaccessories.spanwire.BlockSpanWireDisconnectBox;
import com.micatechnologies.minecraft.csm.trafficaccessories.spanwire.BlockSpanWireGuyAnchor;
import com.micatechnologies.minecraft.csm.trafficaccessories.spanwire.BlockSpanWireHangerMount;
import com.micatechnologies.minecraft.csm.trafficaccessories.spanwire.ItemSpanWireTool;
import com.micatechnologies.minecraft.csm.trafficaccessories.BlockTrafficSignalFatigueMitigator1;
import com.micatechnologies.minecraft.csm.trafficaccessories.BlockTrafficSignalFatigueMitigator2;
import com.micatechnologies.minecraft.csm.trafficaccessories.BlockTrafficSignalFatigueMitigator3;
import com.micatechnologies.minecraft.csm.trafficaccessories.BlockTrafficSignalFatigueMitigator4;
import com.micatechnologies.minecraft.csm.trafficaccessories.BlockTrafficStreetNameSign;
import com.micatechnologies.minecraft.csm.trafficaccessories.BlockTrafficStreetNameSignDouble;
import com.micatechnologies.minecraft.csm.trafficaccessories.BlockTrafficStreetNameSignMount;
import com.micatechnologies.minecraft.csm.trafficaccessories.ItemStreetLightConfigTool;
import net.minecraft.block.Block;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;

/**
 * The tab for traffic signal hardware: control boxes, backplates, covers and mount kits, span
 * wire and bridge cables, cameras, message and speed signs, beacons and the other devices that
 * hang from or stand beside a signalised road. The poles they mount on are in
 * {@link CsmTabTrafficPoles}; work zone and roadside safety devices are in {@link CsmTabWorkZone}.
 *
 * @version 3.0
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
  private static final AxisAlignedBB BB_TL_H_MOUNT_KIT =
      new AxisAlignedBB(-0.687500, 0.312500, 0.000000, 1.687500, 0.437500, 1.000000);
  private static final AxisAlignedBB BB_TL_PM =
      new AxisAlignedBB(0.062500, -0.356250, 0.062500, 0.937500, 0.581250, 0.937500);
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
  /** Package-visible: the retired span wire cluster mount in {@link CsmTabRoadsHidden} keeps it. */
  static final AxisAlignedBB BB_TLITE_VERT_WIRE_MOUNT =
      new AxisAlignedBB(0.437500, 0.500000, 0.437500, 0.562500, 1.000000, 0.937500);
  private static final AxisAlignedBB BB_TL_INTERCONNECT_MODULE_1 =
      new AxisAlignedBB(0.325000, 0.125000, 1.100000, 0.700000, 0.500000, 1.515625);
  private static final AxisAlignedBB BB_TL_INTERCONNECT_MODULE_2 =
      new AxisAlignedBB(0.250000, -0.375000, 0.100000, 0.656250, 1.328125, 0.587500);
  private static final AxisAlignedBB BB_TL_HANG_MOUNT =
      new AxisAlignedBB(0.500000, -0.750000, 0.750000, 0.600000, 1.609375, 0.850000);

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
    initTabBlock(new BlockTrafficAccessoryBackplate("tlborder5addonblackgreen"));
    initTabBlock(new BlockTrafficAccessoryBackplate("tlborder5addongreenblack"));
    initTabBlock(new BlockTrafficAccessoryBackplate("tlborder5addonyellowyellow"));

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
    initTabBlock(new BlockTrafficAccessoryBackplate("tlborderaddonblackgreen"));
    initTabBlock(new BlockTrafficAccessoryBackplate("tlborderaddongreenblack"));
    initTabBlock(new BlockTrafficAccessoryBackplate("tlborderaddonyellowyellow"));

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
    initTabBlock(new BlockTrafficAccessoryBackplate("tlborderblackgreen"));
    initTabBlock(new BlockTrafficAccessoryBackplate("tlbordergreenblack"));
    initTabBlock(new BlockTrafficAccessoryBackplate("tlborderyellowyellow"));

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
    initTabBlock(new BlockTrafficAccessoryBackplate("tlbordersingleblackgreen"));
    initTabBlock(new BlockTrafficAccessoryBackplate("tlbordersinglegreenblack"));
    initTabBlock(new BlockTrafficAccessoryBackplate("tlbordersingleyellowyellow"));

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
    initTabBlock(new BlockTrafficAccessoryBackplate("tlbordergreenblack8inch"));
    initTabBlock(new BlockTrafficAccessoryBackplate("tlbordergreenblack8812inch"));
    initTabBlock(new BlockTrafficAccessoryBackplate("tlborderyellowyellow8inch"));
    initTabBlock(new BlockTrafficAccessoryBackplate("tlborderyellowyellow8812inch"));

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
    initTabBlock(new BlockTrafficAccessoryBackplateFitted("tldoghouseborderblackgreen"));
    initTabBlock(new BlockTrafficAccessoryBackplateFitted("tldoghousebordergreenblack"));
    initTabBlock(new BlockTrafficAccessoryBackplateFitted("tldoghouseborderyellowyellow"));

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
    initTabBlock(new BlockTrafficAccessoryBackplateFitted("tlhawkborderblackgreen"));
    initTabBlock(new BlockTrafficAccessoryBackplateFitted("tlhawkbordergreenblack"));
    initTabBlock(new BlockTrafficAccessoryBackplateFitted("tlhawkborderyellowyellow"));

    // --- Backplate factory blocks: TLHBorder ---

    // --- NSEWUD factory blocks: TL Covers and Mount Kits ---
    // (tlhcover retired to the dynamic tlvcover — see CsmTabRoadsHidden)
    initTabBlock(new BlockSignalMountKit("tlhmountkit", BB_TL_H_MOUNT_KIT, BlockRenderLayer.CUTOUT_MIPPED, 2F, false));
    initTabBlock(new BlockTrafficAccessoryNSEWUD("tlpmblack", BB_TL_PM, BlockRenderLayer.CUTOUT_MIPPED, 2F, true));
    initTabBlock(new BlockTrafficAccessoryNSEWUD("tlpmsilver", BB_TL_PM, BlockRenderLayer.CUTOUT_MIPPED, 2F, true));
    initTabBlock(new BlockTrafficAccessoryNSEWUD("tlpmtan", BB_TL_PM, BlockRenderLayer.CUTOUT_MIPPED, 2F, true));
    initTabBlock(new BlockTrafficAccessoryNSEWUD("tlpmwhite", BB_TL_PM, BlockRenderLayer.CUTOUT_MIPPED, 2F, true));
    // Dynamic TESR-based cover: adapts to the adjacent signal's orientation, size, tilt,
    // and add-on signals (keeps the legacy "tlvcover" registry name for world compatibility)
    initTabBlock(new BlockTrafficLightCover());
    initTabBlock(new BlockSignalMountKit("tlvmountkit", BB_TL_V_MOUNT_KIT, BlockRenderLayer.CUTOUT_MIPPED, 2F, false));
    initTabBlock(new BlockSignalMountKit("tlvmountkit8812inch", BB_TL_V_MOUNT_KIT_8812_INCH, BlockRenderLayer.CUTOUT_MIPPED, 2F, false));
    initTabBlock(new BlockSignalMountKit("tlvmountkit8inch", BB_TL_V_MOUNT_KIT_8_INCH, BlockRenderLayer.CUTOUT_MIPPED, 2F, false));
    initTabBlock(new BlockSignalMountKit("tlvtall90lmountkit", BB_TL_V_TALL_90L_MOUNT_KIT, BlockRenderLayer.CUTOUT_MIPPED, 2F, false));
    initTabBlock(new BlockSignalMountKit("tlvtall90rmountkit", BB_TL_V_TALL_90R_MOUNT_KIT, BlockRenderLayer.CUTOUT_MIPPED, 2F, false));
    initTabBlock(new BlockSignalMountKit("tlvtallmountkit", BB_TL_V_TALL_MOUNT_KIT, BlockRenderLayer.CUTOUT_MIPPED, 2F, false));
    initTabBlock(new BlockTrafficLightMountKit());
    initTabBlock(new BlockTrafficAccessoryNSEW("plumbizer_signal_mount", BB_PLUMBIZER_SIGNAL_MOUNT,
        BlockRenderLayer.CUTOUT_MIPPED, 2F, false));
    // Wire mounts carry a span wire hanger tile entity so they can hang from a strung span.
    // Unlinked they are the same decorative mounts they have always been.
    initTabBlock(new BlockSpanWireHangerMount("tlitehorzwiremount", BB_TLITE_HORZ_WIRE_MOUNT, BlockRenderLayer.SOLID, 1F, true));
    initTabBlock(new BlockSpanWireHangerMount("tlitevertwiremount", BB_TLITE_VERT_WIRE_MOUNT, BlockRenderLayer.SOLID, 1F, true));
    // The old span wire cluster mount is retired into this hanger (CsmTabRoadsHidden).
    initTabBlock(new BlockSignalClusterHanger("signal_cluster_hanger", BB_TLITE_VERT_WIRE_MOUNT, BlockRenderLayer.SOLID, 1F, true));
    initTabBlock(new BlockSpanWireDisconnectBox("spanwiredisconnectbox", BB_TLITE_VERT_WIRE_MOUNT, BlockRenderLayer.CUTOUT_MIPPED, 1F, false));
    initTabBlock(new BlockSpanWireAnchor());
    initTabBlock(new BlockSpanWireGuyAnchor());
    initTabItem(ItemSpanWireTool.class, fmlPreInitializationEvent); // Span Wire Tool
    // Bridge cables: an anchor owns and draws cables to other anchors or blocks
    // (assets/docs/BRIDGE_CABLE_SYSTEM.md)
    initTabBlock(new com.micatechnologies.minecraft.csm.trafficaccessories.bridgecable.BlockBridgeCableAnchor("bridge_cable_socket"));
    initTabBlock(new com.micatechnologies.minecraft.csm.trafficaccessories.bridgecable.BlockBridgeCableAnchor("bridge_cable_clevis"));
    initTabBlock(new com.micatechnologies.minecraft.csm.trafficaccessories.bridgecable.BlockBridgeCableAnchor("bridge_cable_saddle"));
    initTabBlock(new com.micatechnologies.minecraft.csm.trafficaccessories.bridgecable.BlockBridgeCableAnchor("bridge_cable_deck_anchor"));
    initTabItem(com.micatechnologies.minecraft.csm.trafficaccessories.bridgecable.ItemBridgeCableTool.class, fmlPreInitializationEvent);
    initTabBlock(new BlockTrafficAccessoryNSEWUD("tlpmunpainted", BB_TL_PM, BlockRenderLayer.CUTOUT_MIPPED, 2F, true));

    // --- Street Light Configuration Tool: finials and caps on pedestal and concrete poles ---
    initTabItem(ItemStreetLightConfigTool.class, fmlPreInitializationEvent); // Street Light Configuration Tool

    // --- Class-based: Fatigue Mitigators ---
    initTabBlock(BlockTrafficSignalFatigueMitigator1.class, fmlPreInitializationEvent);
    initTabBlock(BlockTrafficSignalFatigueMitigator2.class, fmlPreInitializationEvent);
    initTabBlock(BlockTrafficSignalFatigueMitigator3.class, fmlPreInitializationEvent);
    initTabBlock(BlockTrafficSignalFatigueMitigator4.class, fmlPreInitializationEvent);

    // --- NSEWUD factory blocks: Signal Accessories ---
    initTabBlock(new BlockTrafficAccessoryNSEWUD("tlhangmount", BB_TL_HANG_MOUNT, BlockRenderLayer.CUTOUT_MIPPED, 2F, false));
    initTabBlock(new BlockTrafficAccessoryNSEWUD("tlinterconnectmodule1", BB_TL_INTERCONNECT_MODULE_1, BlockRenderLayer.CUTOUT_MIPPED, 2F, false));
    initTabBlock(new BlockTrafficAccessoryNSEWUD("tlinterconnectmodule2", BB_TL_INTERCONNECT_MODULE_2, BlockRenderLayer.CUTOUT_MIPPED, 2F, false));
    initTabBlock(BlockPreemptBeacon.class, fmlPreInitializationEvent);
    initTabBlock(BlockPreemptConfirmParWhite.class, fmlPreInitializationEvent);
    initTabBlock(BlockPreemptConfirmParBlue.class, fmlPreInitializationEvent);
    initTabBlock(BlockPreemptConfirmDomeRed.class, fmlPreInitializationEvent);
    initTabBlock(BlockPreemptConfirmParWhiteSpan.class, fmlPreInitializationEvent);
    initTabBlock(BlockPreemptConfirmParBlueSpan.class, fmlPreInitializationEvent);
    initTabBlock(BlockPreemptConfirmDomeRedSpan.class, fmlPreInitializationEvent);
    initTabBlock(BlockPreemptConfirmParWhitePole.class, fmlPreInitializationEvent);
    initTabBlock(BlockPreemptConfirmParBluePole.class, fmlPreInitializationEvent);
    initTabBlock(BlockPreemptConfirmDomeRedPole.class, fmlPreInitializationEvent);
    initTabBlock(BlockSnowBeacon.class, fmlPreInitializationEvent);
    initTabBlock(com.micatechnologies.minecraft.csm.trafficaccessories.BlockRailroadCrossingFlasher.class,
        fmlPreInitializationEvent);
    initTabBlock(com.micatechnologies.minecraft.csm.trafficaccessories.BlockRailroadCrossingGate1.class,
        fmlPreInitializationEvent);
    initTabBlock(com.micatechnologies.minecraft.csm.trafficaccessories.BlockRailroadCrossingGate2.class,
        fmlPreInitializationEvent);
    initTabBlock(com.micatechnologies.minecraft.csm.trafficaccessories.BlockRailroadCrossingGate3.class,
        fmlPreInitializationEvent);

    // --- Class-based: Street Name Signs (NSEW, not NSEWUD) ---
    initTabBlock(BlockTrafficStreetNameSign.class, fmlPreInitializationEvent);
    initTabBlock(BlockTrafficStreetNameSignDouble.class, fmlPreInitializationEvent);
    initTabBlock(BlockTrafficStreetNameSignMount.class, fmlPreInitializationEvent);

    // --- Class-based: Mini Solar Panel ---
    initTabBlock(BlockMiniSolarPanel.class, fmlPreInitializationEvent);

    // --- Class-based: Solar ALPR Cameras (OBJ models) ---
    initTabBlock(BlockAlprCameraSolar.class, fmlPreInitializationEvent);
    initTabBlock(BlockAlprCameraSolarWall.class, fmlPreInitializationEvent);

    // --- Factory: Enforcement Cameras (OBJ models, gen_enforcement_cameras.py) ---
    // Pole-top devices sleeve over a pole's top; side-arm devices fit the pole behind them.
    initTabBlock(new BlockEnforcementCamera.PoleTop("enforcement_red_light_camera_top_white",
        BlockEnforcementCamera.BB_RED_LIGHT_CAMERA_TOP));
    initTabBlock(new BlockEnforcementCamera.PoleTop("enforcement_red_light_camera_top_black",
        BlockEnforcementCamera.BB_RED_LIGHT_CAMERA_TOP));
    initTabBlock(new BlockEnforcementCamera.SideArm("enforcement_red_light_camera_side_white",
        BlockEnforcementCamera.BB_RED_LIGHT_CAMERA_SIDE));
    initTabBlock(new BlockEnforcementCamera.SideArm("enforcement_red_light_camera_side_black",
        BlockEnforcementCamera.BB_RED_LIGHT_CAMERA_SIDE));
    initTabBlock(new BlockEnforcementCamera.SideArm("enforcement_flash_unit_side_white",
        BlockEnforcementCamera.BB_FLASH_UNIT_SIDE));
    initTabBlock(new BlockEnforcementCamera.SideArm("enforcement_flash_unit_side_black",
        BlockEnforcementCamera.BB_FLASH_UNIT_SIDE));
    initTabBlock(new BlockEnforcementCamera.PoleTop("enforcement_speed_camera_top_white",
        BlockEnforcementCamera.BB_SPEED_CAMERA_TOP));
    initTabBlock(new BlockEnforcementCamera.PoleTop("enforcement_speed_camera_top_black",
        BlockEnforcementCamera.BB_SPEED_CAMERA_TOP));
    initTabBlock(new BlockEnforcementCamera.SideArm("enforcement_speed_camera_side_white",
        BlockEnforcementCamera.BB_SPEED_CAMERA_SIDE));
    initTabBlock(new BlockEnforcementCamera.SideArm("enforcement_speed_camera_side_black",
        BlockEnforcementCamera.BB_SPEED_CAMERA_SIDE));

    // --- Class-based: Portable Message Sign ---
    initTabBlock(com.micatechnologies.minecraft.csm.trafficaccessories.BlockPortableMessageSign.class,
        fmlPreInitializationEvent);

    // --- Class-based: Overhead Message Sign ---
    initTabBlock(com.micatechnologies.minecraft.csm.trafficaccessories.BlockOverheadMessageSign.class,
        fmlPreInitializationEvent);

    // --- Class-based: Portable Speed Limit Sign ---
    initTabBlock(com.micatechnologies.minecraft.csm.trafficaccessories.BlockPortableSpeedLimitSign.class,
        fmlPreInitializationEvent);

    // --- Class-based: Overhead Speed Limit Sign ---
    initTabBlock(com.micatechnologies.minecraft.csm.trafficaccessories.BlockOverheadSpeedLimitSign.class,
        fmlPreInitializationEvent);

    // --- Class-based: Pole-Mount Speed Limit Sign ---
    initTabBlock(com.micatechnologies.minecraft.csm.trafficaccessories.BlockPoleMountSpeedLimitSign.class,
        fmlPreInitializationEvent);

    // --- Class-based: School Zone Beacon Assembly ---
    initTabBlock(com.micatechnologies.minecraft.csm.trafficaccessories.BlockSchoolZoneBeacon.class,
        fmlPreInitializationEvent);

    // --- Class-based: Radar Speed Feedback Sign ---
    initTabBlock(com.micatechnologies.minecraft.csm.trafficaccessories.BlockRadarSpeedSign.class,
        fmlPreInitializationEvent);

    // --- Class-based: Lane Control Controller ---
    initTabBlock(
        com.micatechnologies.minecraft.csm.trafficaccessories.BlockLaneControlController.class,
        fmlPreInitializationEvent);

    // --- Class-based: Lane Control Signal ---
    initTabBlock(com.micatechnologies.minecraft.csm.trafficaccessories.BlockLaneControlSignal.class,
        fmlPreInitializationEvent);

    // --- Class-based: Dynamic Guide Sign ---
    initTabBlock(com.micatechnologies.minecraft.csm.trafficaccessories.BlockDynamicGuideSign.class,
        fmlPreInitializationEvent);
    initTabBlock(new com.micatechnologies.minecraft.csm.trafficaccessories.truss.BlockSignTruss("sign_truss", false));
    initTabBlock(new com.micatechnologies.minecraft.csm.trafficaccessories.truss.BlockSignTruss("sign_truss_large", true));
    initTabBlock(new com.micatechnologies.minecraft.csm.trafficaccessories.truss.BlockSignTrussLight("sign_truss_light"));
    initTabBlock(new com.micatechnologies.minecraft.csm.trafficaccessories.truss.BlockTrussCatwalk("sign_truss_catwalk"));

    // --- Class-based: Dynamic Street Sign ---
    initTabBlock(com.micatechnologies.minecraft.csm.trafficaccessories.BlockDynamicStreetSign.class,
        fmlPreInitializationEvent);
  }
}

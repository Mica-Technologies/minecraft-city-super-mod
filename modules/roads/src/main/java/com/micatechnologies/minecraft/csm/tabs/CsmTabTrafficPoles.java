package com.micatechnologies.minecraft.csm.tabs;

import com.micatechnologies.minecraft.csm.CsmRegistry;
import com.micatechnologies.minecraft.csm.codeutils.AbstractBlockTrafficPole.TRAFFIC_POLE_COLOR;
import com.micatechnologies.minecraft.csm.codeutils.CsmTab;
import com.micatechnologies.minecraft.csm.trafficaccessories.BlockTrafficAccessoryNSEWUD;
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
import com.micatechnologies.minecraft.csm.trafficaccessories.BlockTrafficPoleMastArmCurve;
import com.micatechnologies.minecraft.csm.trafficaccessories.BlockTrafficPoleFinial;
import com.micatechnologies.minecraft.csm.trafficaccessories.BlockTrafficPoleImpact;
import com.micatechnologies.minecraft.csm.trafficaccessories.BlockTrafficPoleConcrete;
import com.micatechnologies.minecraft.csm.trafficaccessories.BlockTrafficPoleDecorative;
import com.micatechnologies.minecraft.csm.trafficaccessories.BlockTrafficPoleHorizontalAngleConcrete;
import com.micatechnologies.minecraft.csm.trafficaccessories.BlockTrafficPolePedestal;
import com.micatechnologies.minecraft.csm.trafficaccessories.LuminaireArmProfile;
import com.micatechnologies.minecraft.csm.trafficaccessories.NycSignalArmProfile;
import com.micatechnologies.minecraft.csm.trafficaccessories.TrussSignalArmProfile;
import com.micatechnologies.minecraft.csm.trafficaccessories.MastArmCurveProfile;
import com.micatechnologies.minecraft.csm.trafficaccessories.BlockTrafficPoleSmallBlack;
import com.micatechnologies.minecraft.csm.trafficaccessories.BlockTrafficPoleSmallGray;
import com.micatechnologies.minecraft.csm.trafficaccessories.BlockTrafficPoleSmallTan;
import com.micatechnologies.minecraft.csm.trafficaccessories.BlockTrafficPoleSmallUnpainted;
import com.micatechnologies.minecraft.csm.trafficaccessories.BlockTrafficPoleSmallWhite;
import com.micatechnologies.minecraft.csm.trafficaccessories.BlockTrafficpolehorzdoublemountunpainted;
import com.micatechnologies.minecraft.csm.trafficaccessories.BlockTrafficpolehorzsinglemountunpainted;
import com.micatechnologies.minecraft.csm.trafficaccessories.ItemStreetLightConfigTool;
import net.minecraft.block.Block;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;

/**
 * The tab for traffic poles and every pole part: painted, concrete, pedestal, impact and
 * decorative poles, their bases, connectors, mounts and finials, and the mast arms, truss and
 * luminaire arms. Split out of {@link CsmTabTrafficAccessories}, which keeps the signal hardware.
 *
 * @version 1.0
 */
@CsmTab.Load(order = 9, sub = -1)
public class CsmTabTrafficPoles extends CsmTab {

  /**
   * Gets the ID (unique identifier) of the tab.
   *
   * @return the ID of the tab
   *
   * @since 1.0
   */
  @Override
  public String getTabId() {
    return "tabtrafficpoles";
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
    return CsmRegistry.getBlock("trafficpolevertical");
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

  // Shared bounding boxes for the pole and pole-part factory blocks
  private static final AxisAlignedBB BB_TRAFFIC_POLE_BASE =
      new AxisAlignedBB(0.000000, 0.000000, 0.000000, 1.000000, 1.000000, 1.000000);
  private static final AxisAlignedBB BB_TRAFFIC_POLE_HORIZ_SIGN_MOUNT =
      new AxisAlignedBB(0.000000, 0.000000, 0.000000, 0.750000, 1.000000, 1.000000);
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

  /**
   * Initializes all the elements belonging to the tab.
   *
   * @param fmlPreInitializationEvent the {@link FMLPreInitializationEvent} that is being processed
   *
   * @since 1.0
   */
  @Override
  public void initTabElements(FMLPreInitializationEvent fmlPreInitializationEvent) {
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

    // --- Pedestal poles (OBJ; one block, base and cap decided per block from its neighbours) ---
    initTabBlock(new BlockTrafficPolePedestal("trafficpolepedestalsilver", TRAFFIC_POLE_COLOR.SILVER));
    initTabBlock(new BlockTrafficPolePedestal("trafficpolepedestalblack", TRAFFIC_POLE_COLOR.BLACK));
    initTabBlock(new BlockTrafficPolePedestal("trafficpolepedestaltan", TRAFFIC_POLE_COLOR.TAN));
    initTabBlock(new BlockTrafficPolePedestal("trafficpolepedestalwhite", TRAFFIC_POLE_COLOR.WHITE));
    initTabBlock(new BlockTrafficPolePedestal("trafficpolepedestalunpainted", TRAFFIC_POLE_COLOR.UNPAINTED));
    initTabBlock(BlockTrafficPoleFinial.class, fmlPreInitializationEvent); // Pole Finial

    // --- Impact resistant poles (GitHub #347): a tube the full width of its block, standing on
    // the painted poles' bolted bases -- see BlockTrafficPoleImpact and gen_pole_bases.py ---
    initTabBlock(new BlockTrafficPoleImpact("trafficpoleimpactsilver", TRAFFIC_POLE_COLOR.SILVER));
    initTabBlock(new BlockTrafficPoleImpact("trafficpoleimpactblack", TRAFFIC_POLE_COLOR.BLACK));
    initTabBlock(new BlockTrafficPoleImpact("trafficpoleimpacttan", TRAFFIC_POLE_COLOR.TAN));
    initTabBlock(new BlockTrafficPoleImpact("trafficpoleimpactwhite", TRAFFIC_POLE_COLOR.WHITE));
    initTabBlock(new BlockTrafficPoleImpact("trafficpoleimpactunpainted", TRAFFIC_POLE_COLOR.UNPAINTED));

    // --- Concrete poles (round and octagon, thick and thin; each block draws its own base, cap
    // and tenon from its neighbours -- see BlockTrafficPoleConcrete and gen_concrete_poles.py) ---
    initTabBlock(new BlockTrafficPoleConcrete("trafficpoleverticalconcrete", 6.0, 8.0));
    initTabBlock(new BlockTrafficPoleConcrete("trafficpoleverticalconcreteoctagon", 6.0, 8.0));
    initTabBlock(new BlockTrafficPoleConcrete("trafficpolehorizontalconcrete", 4.0, 6.0));
    initTabBlock(new BlockTrafficPoleConcrete("trafficpolehorizontalconcreteoctagon", 4.0, 6.0));
    initTabBlock(BlockTrafficPoleHorizontalAngleConcrete.class, fmlPreInitializationEvent);

    // Sets pedestal finials and concrete pole caps: a pole tool, so it sits with the poles.
    initTabItem(ItemStreetLightConfigTool.class, fmlPreInitializationEvent); // Street Light Configuration Tool

    // --- Concrete pole accessories: every live family in round concrete, and the straight
    // vertical sections again in octagon concrete (curves and arms stay round) ---
    initTabBlock(new BlockTrafficAccessoryNSEWUD("trafficpoleverticalconnectorconcrete", BB_TRAFFIC_POLE_VERT_CONNECTOR, BlockRenderLayer.CUTOUT_MIPPED, 2F, true));
    initTabBlock(new BlockTrafficAccessoryNSEWUD("trafficpoleverticalconnectorconcreteoctagon", BB_TRAFFIC_POLE_VERT_CONNECTOR, BlockRenderLayer.CUTOUT_MIPPED, 2F, true));
    initTabBlock(new BlockTrafficAccessoryNSEWUD("trafficpoleverticalconnectorangledconcrete", BB_TRAFFIC_POLE_VERT_CONNECTOR_ANGLED, BlockRenderLayer.CUTOUT_MIPPED, 2F, true));
    initTabBlock(new BlockTrafficAccessoryNSEWUD("trafficpoleverticalconnectorangledconcreteoctagon", BB_TRAFFIC_POLE_VERT_CONNECTOR_ANGLED, BlockRenderLayer.CUTOUT_MIPPED, 2F, true));
    initTabBlock(new BlockTrafficAccessoryNSEWUD("trafficpoleverticalconnectordoubleconcrete", BB_TRAFFIC_POLE_VERT_CONNECTOR_DOUBLE, BlockRenderLayer.CUTOUT_MIPPED, 2F, true));
    initTabBlock(new BlockTrafficAccessoryNSEWUD("trafficpoleverticalconnectordoubleconcreteoctagon", BB_TRAFFIC_POLE_VERT_CONNECTOR_DOUBLE, BlockRenderLayer.CUTOUT_MIPPED, 2F, true));
    initTabBlock(new BlockTrafficAccessoryNSEWUD("trafficpoleverticalquadmountconcrete", BB_TRAFFIC_POLE_VERT_QUAD_MOUNT, BlockRenderLayer.CUTOUT_MIPPED, 2F, true));
    initTabBlock(new BlockTrafficAccessoryNSEWUD("trafficpoleverticalquadmountconcreteoctagon", BB_TRAFFIC_POLE_VERT_QUAD_MOUNT, BlockRenderLayer.CUTOUT_MIPPED, 2F, true));
    initTabBlock(new BlockTrafficAccessoryNSEWUD("trafficpoleverticalsignalmountconcrete", BB_TRAFFIC_POLE_VERT_SIGNAL_MOUNT, BlockRenderLayer.CUTOUT_MIPPED, 2F, true));
    initTabBlock(new BlockTrafficAccessoryNSEWUD("trafficpoleverticalsignalmountconcreteoctagon", BB_TRAFFIC_POLE_VERT_SIGNAL_MOUNT, BlockRenderLayer.CUTOUT_MIPPED, 2F, true));
    initTabBlock(new BlockTrafficAccessoryNSEWUD("trafficpoleverticaldoubleguymountconcrete", BB_TRAFFIC_POLE_VERT_DOUBLE_GUY_MOUNT, BlockRenderLayer.CUTOUT_MIPPED, 2F, true));
    initTabBlock(new BlockTrafficAccessoryNSEWUD("trafficpoleverticaldoubleguymountconcreteoctagon", BB_TRAFFIC_POLE_VERT_DOUBLE_GUY_MOUNT, BlockRenderLayer.CUTOUT_MIPPED, 2F, true));
    initTabBlock(new BlockTrafficAccessoryNSEWUD("trafficpolevertdblconcrete", BB_TRAFFIC_POLE_VERT_DBL, BlockRenderLayer.CUTOUT_MIPPED, 2F, true));
    initTabBlock(new BlockTrafficAccessoryNSEWUD("trafficpolevertdblconcreteoctagon", BB_TRAFFIC_POLE_VERT_DBL, BlockRenderLayer.CUTOUT_MIPPED, 2F, true));
    initTabBlock(new BlockTrafficAccessoryNSEWUD("trafficpolehorzdblconcrete", BB_TRAFFIC_POLE_HORZ_DBL, BlockRenderLayer.CUTOUT_MIPPED, 2F, true));
    initTabBlock(new BlockTrafficAccessoryNSEWUD("trafficpolehorizsignmountconcrete", BB_TRAFFIC_POLE_HORIZ_SIGN_MOUNT, BlockRenderLayer.CUTOUT_MIPPED, 2F, true));
    initTabBlock(new BlockTrafficAccessoryNSEWUD("trafficpoleverticalcurveconnectorconcrete", BB_TRAFFIC_POLE_VERT_CURVE_CONNECTOR, BlockRenderLayer.CUTOUT_MIPPED, 2F, true));
    initTabBlock(new BlockTrafficAccessoryNSEWUD("trafficpoleverticalcurveconnectordoubleguyconcrete", BB_TRAFFIC_POLE_VERT_CURVE_CONNECTOR, BlockRenderLayer.CUTOUT_MIPPED, 2F, true));
    initTabBlock(new BlockTrafficAccessoryNSEWUD.PoleFitted("trafficpoleverticallightmountconcrete", BB_TRAFFIC_POLE_VERT_LIGHT_MOUNT, BlockRenderLayer.CUTOUT_MIPPED, 2F, true));
    initTabBlock(new BlockTrafficPoleMastArmCurve("trafficpolemastarmcurve4x1concrete", MastArmCurveProfile.P4X1));
    initTabBlock(new BlockTrafficPoleMastArmCurve("trafficpolemastarmcurve5x1concrete", MastArmCurveProfile.P5X1));
    initTabBlock(new BlockTrafficPoleMastArmCurve("trafficpolemastarmcurve6x2concrete", MastArmCurveProfile.P6X2));
    initTabBlock(new BlockTrafficPoleMastArmCurve("trafficpolemastarmcurve8x2concrete", MastArmCurveProfile.P8X2));
    initTabBlock(new BlockTrafficPoleMastArmCurve("trafficpolemastarmcurve10x2concrete", MastArmCurveProfile.P10X2));

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

    // --- Realistic multi-block mast arm curves (OBJ; each places several cells) ---
    initTabBlock(new BlockTrafficPoleMastArmCurve("trafficpolemastarmcurve4x1silver", MastArmCurveProfile.P4X1));
    initTabBlock(new BlockTrafficPoleMastArmCurve("trafficpolemastarmcurve4x1black", MastArmCurveProfile.P4X1));
    initTabBlock(new BlockTrafficPoleMastArmCurve("trafficpolemastarmcurve4x1tan", MastArmCurveProfile.P4X1));
    initTabBlock(new BlockTrafficPoleMastArmCurve("trafficpolemastarmcurve4x1white", MastArmCurveProfile.P4X1));
    initTabBlock(new BlockTrafficPoleMastArmCurve("trafficpolemastarmcurve4x1unpainted", MastArmCurveProfile.P4X1));
    initTabBlock(new BlockTrafficPoleMastArmCurve("trafficpolemastarmcurve5x1silver", MastArmCurveProfile.P5X1));
    initTabBlock(new BlockTrafficPoleMastArmCurve("trafficpolemastarmcurve5x1black", MastArmCurveProfile.P5X1));
    initTabBlock(new BlockTrafficPoleMastArmCurve("trafficpolemastarmcurve5x1tan", MastArmCurveProfile.P5X1));
    initTabBlock(new BlockTrafficPoleMastArmCurve("trafficpolemastarmcurve5x1white", MastArmCurveProfile.P5X1));
    initTabBlock(new BlockTrafficPoleMastArmCurve("trafficpolemastarmcurve5x1unpainted", MastArmCurveProfile.P5X1));
    initTabBlock(new BlockTrafficPoleMastArmCurve("trafficpolemastarmcurve6x2silver", MastArmCurveProfile.P6X2));
    initTabBlock(new BlockTrafficPoleMastArmCurve("trafficpolemastarmcurve6x2black", MastArmCurveProfile.P6X2));
    initTabBlock(new BlockTrafficPoleMastArmCurve("trafficpolemastarmcurve6x2tan", MastArmCurveProfile.P6X2));
    initTabBlock(new BlockTrafficPoleMastArmCurve("trafficpolemastarmcurve6x2white", MastArmCurveProfile.P6X2));
    initTabBlock(new BlockTrafficPoleMastArmCurve("trafficpolemastarmcurve6x2unpainted", MastArmCurveProfile.P6X2));
    initTabBlock(new BlockTrafficPoleMastArmCurve("trafficpolemastarmcurve8x2silver", MastArmCurveProfile.P8X2));
    initTabBlock(new BlockTrafficPoleMastArmCurve("trafficpolemastarmcurve8x2black", MastArmCurveProfile.P8X2));
    initTabBlock(new BlockTrafficPoleMastArmCurve("trafficpolemastarmcurve8x2tan", MastArmCurveProfile.P8X2));
    initTabBlock(new BlockTrafficPoleMastArmCurve("trafficpolemastarmcurve8x2white", MastArmCurveProfile.P8X2));
    initTabBlock(new BlockTrafficPoleMastArmCurve("trafficpolemastarmcurve8x2unpainted", MastArmCurveProfile.P8X2));
    initTabBlock(new BlockTrafficPoleMastArmCurve("trafficpolemastarmcurve10x2silver", MastArmCurveProfile.P10X2));
    initTabBlock(new BlockTrafficPoleMastArmCurve("trafficpolemastarmcurve10x2black", MastArmCurveProfile.P10X2));
    initTabBlock(new BlockTrafficPoleMastArmCurve("trafficpolemastarmcurve10x2tan", MastArmCurveProfile.P10X2));
    initTabBlock(new BlockTrafficPoleMastArmCurve("trafficpolemastarmcurve10x2white", MastArmCurveProfile.P10X2));
    initTabBlock(new BlockTrafficPoleMastArmCurve("trafficpolemastarmcurve10x2unpainted", MastArmCurveProfile.P10X2));

    // --- Truss signal mast arms (level top, sloped, diagonal): gen_truss_signal_arms.py ---
    initTabBlock(new BlockTrafficPoleMastArmCurve("trusssignalarmlevel6galvanized", TrussSignalArmProfile.LEVEL6));
    initTabBlock(new BlockTrafficPoleMastArmCurve("trusssignalarmlevel6silver", TrussSignalArmProfile.LEVEL6));
    initTabBlock(new BlockTrafficPoleMastArmCurve("trusssignalarmlevel6black", TrussSignalArmProfile.LEVEL6));
    initTabBlock(new BlockTrafficPoleMastArmCurve("trusssignalarmlevel8galvanized", TrussSignalArmProfile.LEVEL8));
    initTabBlock(new BlockTrafficPoleMastArmCurve("trusssignalarmlevel8silver", TrussSignalArmProfile.LEVEL8));
    initTabBlock(new BlockTrafficPoleMastArmCurve("trusssignalarmlevel8black", TrussSignalArmProfile.LEVEL8));
    initTabBlock(new BlockTrafficPoleMastArmCurve("trusssignalarmlevel10galvanized", TrussSignalArmProfile.LEVEL10));
    initTabBlock(new BlockTrafficPoleMastArmCurve("trusssignalarmlevel10silver", TrussSignalArmProfile.LEVEL10));
    initTabBlock(new BlockTrafficPoleMastArmCurve("trusssignalarmlevel10black", TrussSignalArmProfile.LEVEL10));
    initTabBlock(new BlockTrafficPoleMastArmCurve("trusssignalarmlevel12galvanized", TrussSignalArmProfile.LEVEL12));
    initTabBlock(new BlockTrafficPoleMastArmCurve("trusssignalarmlevel12silver", TrussSignalArmProfile.LEVEL12));
    initTabBlock(new BlockTrafficPoleMastArmCurve("trusssignalarmlevel12black", TrussSignalArmProfile.LEVEL12));
    initTabBlock(new BlockTrafficPoleMastArmCurve("trusssignalarmsloped8galvanized", TrussSignalArmProfile.SLOPED8));
    initTabBlock(new BlockTrafficPoleMastArmCurve("trusssignalarmsloped8silver", TrussSignalArmProfile.SLOPED8));
    initTabBlock(new BlockTrafficPoleMastArmCurve("trusssignalarmsloped8black", TrussSignalArmProfile.SLOPED8));
    initTabBlock(new BlockTrafficPoleMastArmCurve("trusssignalarmsloped12galvanized", TrussSignalArmProfile.SLOPED12));
    initTabBlock(new BlockTrafficPoleMastArmCurve("trusssignalarmsloped12silver", TrussSignalArmProfile.SLOPED12));
    initTabBlock(new BlockTrafficPoleMastArmCurve("trusssignalarmsloped12black", TrussSignalArmProfile.SLOPED12));
    initTabBlock(new BlockTrafficPoleMastArmCurve("trusssignalarmdiagonal5galvanized", TrussSignalArmProfile.DIAGONAL5));
    initTabBlock(new BlockTrafficPoleMastArmCurve("trusssignalarmdiagonal5silver", TrussSignalArmProfile.DIAGONAL5));
    initTabBlock(new BlockTrafficPoleMastArmCurve("trusssignalarmdiagonal5black", TrussSignalArmProfile.DIAGONAL5));
    initTabBlock(new BlockTrafficPoleMastArmCurve("trusssignalarmdiagonal8galvanized", TrussSignalArmProfile.DIAGONAL8));
    initTabBlock(new BlockTrafficPoleMastArmCurve("trusssignalarmdiagonal8silver", TrussSignalArmProfile.DIAGONAL8));
    initTabBlock(new BlockTrafficPoleMastArmCurve("trusssignalarmdiagonal8black", TrussSignalArmProfile.DIAGONAL8));

    // --- New York City guyed signal mast arms (regular, diagonal): gen_truss_signal_arms.py ---
    initTabBlock(new BlockTrafficPoleMastArmCurve("nycsignalarmregular4brown", NycSignalArmProfile.REGULAR4));
    initTabBlock(new BlockTrafficPoleMastArmCurve("nycsignalarmregular4galvanized", NycSignalArmProfile.REGULAR4));
    initTabBlock(new BlockTrafficPoleMastArmCurve("nycsignalarmregular4black", NycSignalArmProfile.REGULAR4));
    initTabBlock(new BlockTrafficPoleMastArmCurve("nycsignalarmregular6brown", NycSignalArmProfile.REGULAR6));
    initTabBlock(new BlockTrafficPoleMastArmCurve("nycsignalarmregular6galvanized", NycSignalArmProfile.REGULAR6));
    initTabBlock(new BlockTrafficPoleMastArmCurve("nycsignalarmregular6black", NycSignalArmProfile.REGULAR6));
    initTabBlock(new BlockTrafficPoleMastArmCurve("nycsignalarmregular8brown", NycSignalArmProfile.REGULAR8));
    initTabBlock(new BlockTrafficPoleMastArmCurve("nycsignalarmregular8galvanized", NycSignalArmProfile.REGULAR8));
    initTabBlock(new BlockTrafficPoleMastArmCurve("nycsignalarmregular8black", NycSignalArmProfile.REGULAR8));
    initTabBlock(new BlockTrafficPoleMastArmCurve("nycsignalarmdiagonal4brown", NycSignalArmProfile.DIAGONAL4));
    initTabBlock(new BlockTrafficPoleMastArmCurve("nycsignalarmdiagonal4galvanized", NycSignalArmProfile.DIAGONAL4));
    initTabBlock(new BlockTrafficPoleMastArmCurve("nycsignalarmdiagonal4black", NycSignalArmProfile.DIAGONAL4));
    initTabBlock(new BlockTrafficPoleMastArmCurve("nycsignalarmdiagonal6brown", NycSignalArmProfile.DIAGONAL6));
    initTabBlock(new BlockTrafficPoleMastArmCurve("nycsignalarmdiagonal6galvanized", NycSignalArmProfile.DIAGONAL6));
    initTabBlock(new BlockTrafficPoleMastArmCurve("nycsignalarmdiagonal6black", NycSignalArmProfile.DIAGONAL6));
    initTabBlock(new BlockTrafficPoleMastArmCurve("nycsignalarmdiagonal8brown", NycSignalArmProfile.DIAGONAL8));
    initTabBlock(new BlockTrafficPoleMastArmCurve("nycsignalarmdiagonal8galvanized", NycSignalArmProfile.DIAGONAL8));
    initTabBlock(new BlockTrafficPoleMastArmCurve("nycsignalarmdiagonal8black", NycSignalArmProfile.DIAGONAL8));
    initTabBlock(new BlockTrafficPoleMastArmCurve("nycsignalarmdiagonal12brown", NycSignalArmProfile.DIAGONAL12));
    initTabBlock(new BlockTrafficPoleMastArmCurve("nycsignalarmdiagonal12galvanized", NycSignalArmProfile.DIAGONAL12));
    initTabBlock(new BlockTrafficPoleMastArmCurve("nycsignalarmdiagonal12black", NycSignalArmProfile.DIAGONAL12));

    // --- Luminaire arms (gen_luminaire_arms.py; multi-block, a street light at the tip) ---
    initTabBlock(new BlockTrafficPoleMastArmCurve("luminairearmcaltrans6ftgalvanized", LuminaireArmProfile.CALTRANS6FT));
    initTabBlock(new BlockTrafficPoleMastArmCurve("luminairearmcaltrans6ftsilver", LuminaireArmProfile.CALTRANS6FT));
    initTabBlock(new BlockTrafficPoleMastArmCurve("luminairearmcaltrans6ftblack", LuminaireArmProfile.CALTRANS6FT));
    initTabBlock(new BlockTrafficPoleMastArmCurve("luminairearmcaltrans6ftwhite", LuminaireArmProfile.CALTRANS6FT));
    initTabBlock(new BlockTrafficPoleMastArmCurve("luminairearmcaltrans6fttan", LuminaireArmProfile.CALTRANS6FT));
    initTabBlock(new BlockTrafficPoleMastArmCurve("luminairearmcaltrans6ftconcrete", LuminaireArmProfile.CALTRANS6FT));
    initTabBlock(new BlockTrafficPoleMastArmCurve("luminairearmcaltrans6ftdoublegalvanized", LuminaireArmProfile.CALTRANS6FTDOUBLE));
    initTabBlock(new BlockTrafficPoleMastArmCurve("luminairearmcaltrans6ftdoublesilver", LuminaireArmProfile.CALTRANS6FTDOUBLE));
    initTabBlock(new BlockTrafficPoleMastArmCurve("luminairearmcaltrans6ftdoubleblack", LuminaireArmProfile.CALTRANS6FTDOUBLE));
    initTabBlock(new BlockTrafficPoleMastArmCurve("luminairearmcaltrans6ftdoublewhite", LuminaireArmProfile.CALTRANS6FTDOUBLE));
    initTabBlock(new BlockTrafficPoleMastArmCurve("luminairearmcaltrans6ftdoubletan", LuminaireArmProfile.CALTRANS6FTDOUBLE));
    initTabBlock(new BlockTrafficPoleMastArmCurve("luminairearmcaltrans6ftdoubleconcrete", LuminaireArmProfile.CALTRANS6FTDOUBLE));
    initTabBlock(new BlockTrafficPoleMastArmCurve("luminairearmcaltrans6ftstraightgalvanized", LuminaireArmProfile.CALTRANS6FTSTRAIGHT));
    initTabBlock(new BlockTrafficPoleMastArmCurve("luminairearmcaltrans6ftstraightsilver", LuminaireArmProfile.CALTRANS6FTSTRAIGHT));
    initTabBlock(new BlockTrafficPoleMastArmCurve("luminairearmcaltrans6ftstraightblack", LuminaireArmProfile.CALTRANS6FTSTRAIGHT));
    initTabBlock(new BlockTrafficPoleMastArmCurve("luminairearmcaltrans6ftstraightwhite", LuminaireArmProfile.CALTRANS6FTSTRAIGHT));
    initTabBlock(new BlockTrafficPoleMastArmCurve("luminairearmcaltrans6ftstraighttan", LuminaireArmProfile.CALTRANS6FTSTRAIGHT));
    initTabBlock(new BlockTrafficPoleMastArmCurve("luminairearmcaltrans6ftstraightconcrete", LuminaireArmProfile.CALTRANS6FTSTRAIGHT));
    initTabBlock(new BlockTrafficPoleMastArmCurve("luminairearmcaltrans6ftstraightdoublegalvanized", LuminaireArmProfile.CALTRANS6FTSTRAIGHTDOUBLE));
    initTabBlock(new BlockTrafficPoleMastArmCurve("luminairearmcaltrans6ftstraightdoublesilver", LuminaireArmProfile.CALTRANS6FTSTRAIGHTDOUBLE));
    initTabBlock(new BlockTrafficPoleMastArmCurve("luminairearmcaltrans6ftstraightdoubleblack", LuminaireArmProfile.CALTRANS6FTSTRAIGHTDOUBLE));
    initTabBlock(new BlockTrafficPoleMastArmCurve("luminairearmcaltrans6ftstraightdoublewhite", LuminaireArmProfile.CALTRANS6FTSTRAIGHTDOUBLE));
    initTabBlock(new BlockTrafficPoleMastArmCurve("luminairearmcaltrans6ftstraightdoubletan", LuminaireArmProfile.CALTRANS6FTSTRAIGHTDOUBLE));
    initTabBlock(new BlockTrafficPoleMastArmCurve("luminairearmcaltrans6ftstraightdoubleconcrete", LuminaireArmProfile.CALTRANS6FTSTRAIGHTDOUBLE));
    initTabBlock(new BlockTrafficPoleMastArmCurve("luminairearmcaltrans8ftgalvanized", LuminaireArmProfile.CALTRANS8FT));
    initTabBlock(new BlockTrafficPoleMastArmCurve("luminairearmcaltrans8ftsilver", LuminaireArmProfile.CALTRANS8FT));
    initTabBlock(new BlockTrafficPoleMastArmCurve("luminairearmcaltrans8ftblack", LuminaireArmProfile.CALTRANS8FT));
    initTabBlock(new BlockTrafficPoleMastArmCurve("luminairearmcaltrans8ftwhite", LuminaireArmProfile.CALTRANS8FT));
    initTabBlock(new BlockTrafficPoleMastArmCurve("luminairearmcaltrans8fttan", LuminaireArmProfile.CALTRANS8FT));
    initTabBlock(new BlockTrafficPoleMastArmCurve("luminairearmcaltrans8ftconcrete", LuminaireArmProfile.CALTRANS8FT));
    initTabBlock(new BlockTrafficPoleMastArmCurve("luminairearmcaltrans8ftdoublegalvanized", LuminaireArmProfile.CALTRANS8FTDOUBLE));
    initTabBlock(new BlockTrafficPoleMastArmCurve("luminairearmcaltrans8ftdoublesilver", LuminaireArmProfile.CALTRANS8FTDOUBLE));
    initTabBlock(new BlockTrafficPoleMastArmCurve("luminairearmcaltrans8ftdoubleblack", LuminaireArmProfile.CALTRANS8FTDOUBLE));
    initTabBlock(new BlockTrafficPoleMastArmCurve("luminairearmcaltrans8ftdoublewhite", LuminaireArmProfile.CALTRANS8FTDOUBLE));
    initTabBlock(new BlockTrafficPoleMastArmCurve("luminairearmcaltrans8ftdoubletan", LuminaireArmProfile.CALTRANS8FTDOUBLE));
    initTabBlock(new BlockTrafficPoleMastArmCurve("luminairearmcaltrans8ftdoubleconcrete", LuminaireArmProfile.CALTRANS8FTDOUBLE));
    initTabBlock(new BlockTrafficPoleMastArmCurve("luminairearmcaltrans8ftstraightgalvanized", LuminaireArmProfile.CALTRANS8FTSTRAIGHT));
    initTabBlock(new BlockTrafficPoleMastArmCurve("luminairearmcaltrans8ftstraightsilver", LuminaireArmProfile.CALTRANS8FTSTRAIGHT));
    initTabBlock(new BlockTrafficPoleMastArmCurve("luminairearmcaltrans8ftstraightblack", LuminaireArmProfile.CALTRANS8FTSTRAIGHT));
    initTabBlock(new BlockTrafficPoleMastArmCurve("luminairearmcaltrans8ftstraightwhite", LuminaireArmProfile.CALTRANS8FTSTRAIGHT));
    initTabBlock(new BlockTrafficPoleMastArmCurve("luminairearmcaltrans8ftstraighttan", LuminaireArmProfile.CALTRANS8FTSTRAIGHT));
    initTabBlock(new BlockTrafficPoleMastArmCurve("luminairearmcaltrans8ftstraightconcrete", LuminaireArmProfile.CALTRANS8FTSTRAIGHT));
    initTabBlock(new BlockTrafficPoleMastArmCurve("luminairearmcaltrans8ftstraightdoublegalvanized", LuminaireArmProfile.CALTRANS8FTSTRAIGHTDOUBLE));
    initTabBlock(new BlockTrafficPoleMastArmCurve("luminairearmcaltrans8ftstraightdoublesilver", LuminaireArmProfile.CALTRANS8FTSTRAIGHTDOUBLE));
    initTabBlock(new BlockTrafficPoleMastArmCurve("luminairearmcaltrans8ftstraightdoubleblack", LuminaireArmProfile.CALTRANS8FTSTRAIGHTDOUBLE));
    initTabBlock(new BlockTrafficPoleMastArmCurve("luminairearmcaltrans8ftstraightdoublewhite", LuminaireArmProfile.CALTRANS8FTSTRAIGHTDOUBLE));
    initTabBlock(new BlockTrafficPoleMastArmCurve("luminairearmcaltrans8ftstraightdoubletan", LuminaireArmProfile.CALTRANS8FTSTRAIGHTDOUBLE));
    initTabBlock(new BlockTrafficPoleMastArmCurve("luminairearmcaltrans8ftstraightdoubleconcrete", LuminaireArmProfile.CALTRANS8FTSTRAIGHTDOUBLE));
    initTabBlock(new BlockTrafficPoleMastArmCurve("luminairearmcaltrans10ftgalvanized", LuminaireArmProfile.CALTRANS10FT));
    initTabBlock(new BlockTrafficPoleMastArmCurve("luminairearmcaltrans10ftsilver", LuminaireArmProfile.CALTRANS10FT));
    initTabBlock(new BlockTrafficPoleMastArmCurve("luminairearmcaltrans10ftblack", LuminaireArmProfile.CALTRANS10FT));
    initTabBlock(new BlockTrafficPoleMastArmCurve("luminairearmcaltrans10ftwhite", LuminaireArmProfile.CALTRANS10FT));
    initTabBlock(new BlockTrafficPoleMastArmCurve("luminairearmcaltrans10fttan", LuminaireArmProfile.CALTRANS10FT));
    initTabBlock(new BlockTrafficPoleMastArmCurve("luminairearmcaltrans10ftconcrete", LuminaireArmProfile.CALTRANS10FT));
    initTabBlock(new BlockTrafficPoleMastArmCurve("luminairearmcaltrans10ftdoublegalvanized", LuminaireArmProfile.CALTRANS10FTDOUBLE));
    initTabBlock(new BlockTrafficPoleMastArmCurve("luminairearmcaltrans10ftdoublesilver", LuminaireArmProfile.CALTRANS10FTDOUBLE));
    initTabBlock(new BlockTrafficPoleMastArmCurve("luminairearmcaltrans10ftdoubleblack", LuminaireArmProfile.CALTRANS10FTDOUBLE));
    initTabBlock(new BlockTrafficPoleMastArmCurve("luminairearmcaltrans10ftdoublewhite", LuminaireArmProfile.CALTRANS10FTDOUBLE));
    initTabBlock(new BlockTrafficPoleMastArmCurve("luminairearmcaltrans10ftdoubletan", LuminaireArmProfile.CALTRANS10FTDOUBLE));
    initTabBlock(new BlockTrafficPoleMastArmCurve("luminairearmcaltrans10ftdoubleconcrete", LuminaireArmProfile.CALTRANS10FTDOUBLE));
    initTabBlock(new BlockTrafficPoleMastArmCurve("luminairearmcaltrans10ftstraightgalvanized", LuminaireArmProfile.CALTRANS10FTSTRAIGHT));
    initTabBlock(new BlockTrafficPoleMastArmCurve("luminairearmcaltrans10ftstraightsilver", LuminaireArmProfile.CALTRANS10FTSTRAIGHT));
    initTabBlock(new BlockTrafficPoleMastArmCurve("luminairearmcaltrans10ftstraightblack", LuminaireArmProfile.CALTRANS10FTSTRAIGHT));
    initTabBlock(new BlockTrafficPoleMastArmCurve("luminairearmcaltrans10ftstraightwhite", LuminaireArmProfile.CALTRANS10FTSTRAIGHT));
    initTabBlock(new BlockTrafficPoleMastArmCurve("luminairearmcaltrans10ftstraighttan", LuminaireArmProfile.CALTRANS10FTSTRAIGHT));
    initTabBlock(new BlockTrafficPoleMastArmCurve("luminairearmcaltrans10ftstraightconcrete", LuminaireArmProfile.CALTRANS10FTSTRAIGHT));
    initTabBlock(new BlockTrafficPoleMastArmCurve("luminairearmcaltrans10ftstraightdoublegalvanized", LuminaireArmProfile.CALTRANS10FTSTRAIGHTDOUBLE));
    initTabBlock(new BlockTrafficPoleMastArmCurve("luminairearmcaltrans10ftstraightdoublesilver", LuminaireArmProfile.CALTRANS10FTSTRAIGHTDOUBLE));
    initTabBlock(new BlockTrafficPoleMastArmCurve("luminairearmcaltrans10ftstraightdoubleblack", LuminaireArmProfile.CALTRANS10FTSTRAIGHTDOUBLE));
    initTabBlock(new BlockTrafficPoleMastArmCurve("luminairearmcaltrans10ftstraightdoublewhite", LuminaireArmProfile.CALTRANS10FTSTRAIGHTDOUBLE));
    initTabBlock(new BlockTrafficPoleMastArmCurve("luminairearmcaltrans10ftstraightdoubletan", LuminaireArmProfile.CALTRANS10FTSTRAIGHTDOUBLE));
    initTabBlock(new BlockTrafficPoleMastArmCurve("luminairearmcaltrans10ftstraightdoubleconcrete", LuminaireArmProfile.CALTRANS10FTSTRAIGHTDOUBLE));
    initTabBlock(new BlockTrafficPoleMastArmCurve("luminairearmcaltrans12ftgalvanized", LuminaireArmProfile.CALTRANS12FT));
    initTabBlock(new BlockTrafficPoleMastArmCurve("luminairearmcaltrans12ftsilver", LuminaireArmProfile.CALTRANS12FT));
    initTabBlock(new BlockTrafficPoleMastArmCurve("luminairearmcaltrans12ftblack", LuminaireArmProfile.CALTRANS12FT));
    initTabBlock(new BlockTrafficPoleMastArmCurve("luminairearmcaltrans12ftwhite", LuminaireArmProfile.CALTRANS12FT));
    initTabBlock(new BlockTrafficPoleMastArmCurve("luminairearmcaltrans12fttan", LuminaireArmProfile.CALTRANS12FT));
    initTabBlock(new BlockTrafficPoleMastArmCurve("luminairearmcaltrans12ftconcrete", LuminaireArmProfile.CALTRANS12FT));
    initTabBlock(new BlockTrafficPoleMastArmCurve("luminairearmcaltrans12ftdoublegalvanized", LuminaireArmProfile.CALTRANS12FTDOUBLE));
    initTabBlock(new BlockTrafficPoleMastArmCurve("luminairearmcaltrans12ftdoublesilver", LuminaireArmProfile.CALTRANS12FTDOUBLE));
    initTabBlock(new BlockTrafficPoleMastArmCurve("luminairearmcaltrans12ftdoubleblack", LuminaireArmProfile.CALTRANS12FTDOUBLE));
    initTabBlock(new BlockTrafficPoleMastArmCurve("luminairearmcaltrans12ftdoublewhite", LuminaireArmProfile.CALTRANS12FTDOUBLE));
    initTabBlock(new BlockTrafficPoleMastArmCurve("luminairearmcaltrans12ftdoubletan", LuminaireArmProfile.CALTRANS12FTDOUBLE));
    initTabBlock(new BlockTrafficPoleMastArmCurve("luminairearmcaltrans12ftdoubleconcrete", LuminaireArmProfile.CALTRANS12FTDOUBLE));
    initTabBlock(new BlockTrafficPoleMastArmCurve("luminairearmcaltrans12ftstraightgalvanized", LuminaireArmProfile.CALTRANS12FTSTRAIGHT));
    initTabBlock(new BlockTrafficPoleMastArmCurve("luminairearmcaltrans12ftstraightsilver", LuminaireArmProfile.CALTRANS12FTSTRAIGHT));
    initTabBlock(new BlockTrafficPoleMastArmCurve("luminairearmcaltrans12ftstraightblack", LuminaireArmProfile.CALTRANS12FTSTRAIGHT));
    initTabBlock(new BlockTrafficPoleMastArmCurve("luminairearmcaltrans12ftstraightwhite", LuminaireArmProfile.CALTRANS12FTSTRAIGHT));
    initTabBlock(new BlockTrafficPoleMastArmCurve("luminairearmcaltrans12ftstraighttan", LuminaireArmProfile.CALTRANS12FTSTRAIGHT));
    initTabBlock(new BlockTrafficPoleMastArmCurve("luminairearmcaltrans12ftstraightconcrete", LuminaireArmProfile.CALTRANS12FTSTRAIGHT));
    initTabBlock(new BlockTrafficPoleMastArmCurve("luminairearmcaltrans12ftstraightdoublegalvanized", LuminaireArmProfile.CALTRANS12FTSTRAIGHTDOUBLE));
    initTabBlock(new BlockTrafficPoleMastArmCurve("luminairearmcaltrans12ftstraightdoublesilver", LuminaireArmProfile.CALTRANS12FTSTRAIGHTDOUBLE));
    initTabBlock(new BlockTrafficPoleMastArmCurve("luminairearmcaltrans12ftstraightdoubleblack", LuminaireArmProfile.CALTRANS12FTSTRAIGHTDOUBLE));
    initTabBlock(new BlockTrafficPoleMastArmCurve("luminairearmcaltrans12ftstraightdoublewhite", LuminaireArmProfile.CALTRANS12FTSTRAIGHTDOUBLE));
    initTabBlock(new BlockTrafficPoleMastArmCurve("luminairearmcaltrans12ftstraightdoubletan", LuminaireArmProfile.CALTRANS12FTSTRAIGHTDOUBLE));
    initTabBlock(new BlockTrafficPoleMastArmCurve("luminairearmcaltrans12ftstraightdoubleconcrete", LuminaireArmProfile.CALTRANS12FTSTRAIGHTDOUBLE));
    initTabBlock(new BlockTrafficPoleMastArmCurve("luminairearmcaltrans15ftgalvanized", LuminaireArmProfile.CALTRANS15FT));
    initTabBlock(new BlockTrafficPoleMastArmCurve("luminairearmcaltrans15ftsilver", LuminaireArmProfile.CALTRANS15FT));
    initTabBlock(new BlockTrafficPoleMastArmCurve("luminairearmcaltrans15ftblack", LuminaireArmProfile.CALTRANS15FT));
    initTabBlock(new BlockTrafficPoleMastArmCurve("luminairearmcaltrans15ftwhite", LuminaireArmProfile.CALTRANS15FT));
    initTabBlock(new BlockTrafficPoleMastArmCurve("luminairearmcaltrans15fttan", LuminaireArmProfile.CALTRANS15FT));
    initTabBlock(new BlockTrafficPoleMastArmCurve("luminairearmcaltrans15ftconcrete", LuminaireArmProfile.CALTRANS15FT));
    initTabBlock(new BlockTrafficPoleMastArmCurve("luminairearmcaltrans15ftdoublegalvanized", LuminaireArmProfile.CALTRANS15FTDOUBLE));
    initTabBlock(new BlockTrafficPoleMastArmCurve("luminairearmcaltrans15ftdoublesilver", LuminaireArmProfile.CALTRANS15FTDOUBLE));
    initTabBlock(new BlockTrafficPoleMastArmCurve("luminairearmcaltrans15ftdoubleblack", LuminaireArmProfile.CALTRANS15FTDOUBLE));
    initTabBlock(new BlockTrafficPoleMastArmCurve("luminairearmcaltrans15ftdoublewhite", LuminaireArmProfile.CALTRANS15FTDOUBLE));
    initTabBlock(new BlockTrafficPoleMastArmCurve("luminairearmcaltrans15ftdoubletan", LuminaireArmProfile.CALTRANS15FTDOUBLE));
    initTabBlock(new BlockTrafficPoleMastArmCurve("luminairearmcaltrans15ftdoubleconcrete", LuminaireArmProfile.CALTRANS15FTDOUBLE));
    initTabBlock(new BlockTrafficPoleMastArmCurve("luminairearmcaltrans15ftstraightgalvanized", LuminaireArmProfile.CALTRANS15FTSTRAIGHT));
    initTabBlock(new BlockTrafficPoleMastArmCurve("luminairearmcaltrans15ftstraightsilver", LuminaireArmProfile.CALTRANS15FTSTRAIGHT));
    initTabBlock(new BlockTrafficPoleMastArmCurve("luminairearmcaltrans15ftstraightblack", LuminaireArmProfile.CALTRANS15FTSTRAIGHT));
    initTabBlock(new BlockTrafficPoleMastArmCurve("luminairearmcaltrans15ftstraightwhite", LuminaireArmProfile.CALTRANS15FTSTRAIGHT));
    initTabBlock(new BlockTrafficPoleMastArmCurve("luminairearmcaltrans15ftstraighttan", LuminaireArmProfile.CALTRANS15FTSTRAIGHT));
    initTabBlock(new BlockTrafficPoleMastArmCurve("luminairearmcaltrans15ftstraightconcrete", LuminaireArmProfile.CALTRANS15FTSTRAIGHT));
    initTabBlock(new BlockTrafficPoleMastArmCurve("luminairearmcaltrans15ftstraightdoublegalvanized", LuminaireArmProfile.CALTRANS15FTSTRAIGHTDOUBLE));
    initTabBlock(new BlockTrafficPoleMastArmCurve("luminairearmcaltrans15ftstraightdoublesilver", LuminaireArmProfile.CALTRANS15FTSTRAIGHTDOUBLE));
    initTabBlock(new BlockTrafficPoleMastArmCurve("luminairearmcaltrans15ftstraightdoubleblack", LuminaireArmProfile.CALTRANS15FTSTRAIGHTDOUBLE));
    initTabBlock(new BlockTrafficPoleMastArmCurve("luminairearmcaltrans15ftstraightdoublewhite", LuminaireArmProfile.CALTRANS15FTSTRAIGHTDOUBLE));
    initTabBlock(new BlockTrafficPoleMastArmCurve("luminairearmcaltrans15ftstraightdoubletan", LuminaireArmProfile.CALTRANS15FTSTRAIGHTDOUBLE));
    initTabBlock(new BlockTrafficPoleMastArmCurve("luminairearmcaltrans15ftstraightdoubleconcrete", LuminaireArmProfile.CALTRANS15FTSTRAIGHTDOUBLE));
    initTabBlock(new BlockTrafficPoleMastArmCurve("luminairearmtruss8ftgalvanized", LuminaireArmProfile.TRUSS8FT));
    initTabBlock(new BlockTrafficPoleMastArmCurve("luminairearmtruss8ftsilver", LuminaireArmProfile.TRUSS8FT));
    initTabBlock(new BlockTrafficPoleMastArmCurve("luminairearmtruss8ftblack", LuminaireArmProfile.TRUSS8FT));
    initTabBlock(new BlockTrafficPoleMastArmCurve("luminairearmtruss8ftwhite", LuminaireArmProfile.TRUSS8FT));
    initTabBlock(new BlockTrafficPoleMastArmCurve("luminairearmtruss8fttan", LuminaireArmProfile.TRUSS8FT));
    initTabBlock(new BlockTrafficPoleMastArmCurve("luminairearmtruss8ftconcrete", LuminaireArmProfile.TRUSS8FT));
    initTabBlock(new BlockTrafficPoleMastArmCurve("luminairearmtruss8ftdoublegalvanized", LuminaireArmProfile.TRUSS8FTDOUBLE));
    initTabBlock(new BlockTrafficPoleMastArmCurve("luminairearmtruss8ftdoublesilver", LuminaireArmProfile.TRUSS8FTDOUBLE));
    initTabBlock(new BlockTrafficPoleMastArmCurve("luminairearmtruss8ftdoubleblack", LuminaireArmProfile.TRUSS8FTDOUBLE));
    initTabBlock(new BlockTrafficPoleMastArmCurve("luminairearmtruss8ftdoublewhite", LuminaireArmProfile.TRUSS8FTDOUBLE));
    initTabBlock(new BlockTrafficPoleMastArmCurve("luminairearmtruss8ftdoubletan", LuminaireArmProfile.TRUSS8FTDOUBLE));
    initTabBlock(new BlockTrafficPoleMastArmCurve("luminairearmtruss8ftdoubleconcrete", LuminaireArmProfile.TRUSS8FTDOUBLE));
    initTabBlock(new BlockTrafficPoleMastArmCurve("luminairearmtruss8ftstraightgalvanized", LuminaireArmProfile.TRUSS8FTSTRAIGHT));
    initTabBlock(new BlockTrafficPoleMastArmCurve("luminairearmtruss8ftstraightsilver", LuminaireArmProfile.TRUSS8FTSTRAIGHT));
    initTabBlock(new BlockTrafficPoleMastArmCurve("luminairearmtruss8ftstraightblack", LuminaireArmProfile.TRUSS8FTSTRAIGHT));
    initTabBlock(new BlockTrafficPoleMastArmCurve("luminairearmtruss8ftstraightwhite", LuminaireArmProfile.TRUSS8FTSTRAIGHT));
    initTabBlock(new BlockTrafficPoleMastArmCurve("luminairearmtruss8ftstraighttan", LuminaireArmProfile.TRUSS8FTSTRAIGHT));
    initTabBlock(new BlockTrafficPoleMastArmCurve("luminairearmtruss8ftstraightconcrete", LuminaireArmProfile.TRUSS8FTSTRAIGHT));
    initTabBlock(new BlockTrafficPoleMastArmCurve("luminairearmtruss8ftstraightdoublegalvanized", LuminaireArmProfile.TRUSS8FTSTRAIGHTDOUBLE));
    initTabBlock(new BlockTrafficPoleMastArmCurve("luminairearmtruss8ftstraightdoublesilver", LuminaireArmProfile.TRUSS8FTSTRAIGHTDOUBLE));
    initTabBlock(new BlockTrafficPoleMastArmCurve("luminairearmtruss8ftstraightdoubleblack", LuminaireArmProfile.TRUSS8FTSTRAIGHTDOUBLE));
    initTabBlock(new BlockTrafficPoleMastArmCurve("luminairearmtruss8ftstraightdoublewhite", LuminaireArmProfile.TRUSS8FTSTRAIGHTDOUBLE));
    initTabBlock(new BlockTrafficPoleMastArmCurve("luminairearmtruss8ftstraightdoubletan", LuminaireArmProfile.TRUSS8FTSTRAIGHTDOUBLE));
    initTabBlock(new BlockTrafficPoleMastArmCurve("luminairearmtruss8ftstraightdoubleconcrete", LuminaireArmProfile.TRUSS8FTSTRAIGHTDOUBLE));
    initTabBlock(new BlockTrafficPoleMastArmCurve("luminairearmtruss10ftgalvanized", LuminaireArmProfile.TRUSS10FT));
    initTabBlock(new BlockTrafficPoleMastArmCurve("luminairearmtruss10ftsilver", LuminaireArmProfile.TRUSS10FT));
    initTabBlock(new BlockTrafficPoleMastArmCurve("luminairearmtruss10ftblack", LuminaireArmProfile.TRUSS10FT));
    initTabBlock(new BlockTrafficPoleMastArmCurve("luminairearmtruss10ftwhite", LuminaireArmProfile.TRUSS10FT));
    initTabBlock(new BlockTrafficPoleMastArmCurve("luminairearmtruss10fttan", LuminaireArmProfile.TRUSS10FT));
    initTabBlock(new BlockTrafficPoleMastArmCurve("luminairearmtruss10ftconcrete", LuminaireArmProfile.TRUSS10FT));
    initTabBlock(new BlockTrafficPoleMastArmCurve("luminairearmtruss10ftdoublegalvanized", LuminaireArmProfile.TRUSS10FTDOUBLE));
    initTabBlock(new BlockTrafficPoleMastArmCurve("luminairearmtruss10ftdoublesilver", LuminaireArmProfile.TRUSS10FTDOUBLE));
    initTabBlock(new BlockTrafficPoleMastArmCurve("luminairearmtruss10ftdoubleblack", LuminaireArmProfile.TRUSS10FTDOUBLE));
    initTabBlock(new BlockTrafficPoleMastArmCurve("luminairearmtruss10ftdoublewhite", LuminaireArmProfile.TRUSS10FTDOUBLE));
    initTabBlock(new BlockTrafficPoleMastArmCurve("luminairearmtruss10ftdoubletan", LuminaireArmProfile.TRUSS10FTDOUBLE));
    initTabBlock(new BlockTrafficPoleMastArmCurve("luminairearmtruss10ftdoubleconcrete", LuminaireArmProfile.TRUSS10FTDOUBLE));
    initTabBlock(new BlockTrafficPoleMastArmCurve("luminairearmtruss10ftstraightgalvanized", LuminaireArmProfile.TRUSS10FTSTRAIGHT));
    initTabBlock(new BlockTrafficPoleMastArmCurve("luminairearmtruss10ftstraightsilver", LuminaireArmProfile.TRUSS10FTSTRAIGHT));
    initTabBlock(new BlockTrafficPoleMastArmCurve("luminairearmtruss10ftstraightblack", LuminaireArmProfile.TRUSS10FTSTRAIGHT));
    initTabBlock(new BlockTrafficPoleMastArmCurve("luminairearmtruss10ftstraightwhite", LuminaireArmProfile.TRUSS10FTSTRAIGHT));
    initTabBlock(new BlockTrafficPoleMastArmCurve("luminairearmtruss10ftstraighttan", LuminaireArmProfile.TRUSS10FTSTRAIGHT));
    initTabBlock(new BlockTrafficPoleMastArmCurve("luminairearmtruss10ftstraightconcrete", LuminaireArmProfile.TRUSS10FTSTRAIGHT));
    initTabBlock(new BlockTrafficPoleMastArmCurve("luminairearmtruss10ftstraightdoublegalvanized", LuminaireArmProfile.TRUSS10FTSTRAIGHTDOUBLE));
    initTabBlock(new BlockTrafficPoleMastArmCurve("luminairearmtruss10ftstraightdoublesilver", LuminaireArmProfile.TRUSS10FTSTRAIGHTDOUBLE));
    initTabBlock(new BlockTrafficPoleMastArmCurve("luminairearmtruss10ftstraightdoubleblack", LuminaireArmProfile.TRUSS10FTSTRAIGHTDOUBLE));
    initTabBlock(new BlockTrafficPoleMastArmCurve("luminairearmtruss10ftstraightdoublewhite", LuminaireArmProfile.TRUSS10FTSTRAIGHTDOUBLE));
    initTabBlock(new BlockTrafficPoleMastArmCurve("luminairearmtruss10ftstraightdoubletan", LuminaireArmProfile.TRUSS10FTSTRAIGHTDOUBLE));
    initTabBlock(new BlockTrafficPoleMastArmCurve("luminairearmtruss10ftstraightdoubleconcrete", LuminaireArmProfile.TRUSS10FTSTRAIGHTDOUBLE));
    initTabBlock(new BlockTrafficPoleMastArmCurve("luminairearmtruss12ftgalvanized", LuminaireArmProfile.TRUSS12FT));
    initTabBlock(new BlockTrafficPoleMastArmCurve("luminairearmtruss12ftsilver", LuminaireArmProfile.TRUSS12FT));
    initTabBlock(new BlockTrafficPoleMastArmCurve("luminairearmtruss12ftblack", LuminaireArmProfile.TRUSS12FT));
    initTabBlock(new BlockTrafficPoleMastArmCurve("luminairearmtruss12ftwhite", LuminaireArmProfile.TRUSS12FT));
    initTabBlock(new BlockTrafficPoleMastArmCurve("luminairearmtruss12fttan", LuminaireArmProfile.TRUSS12FT));
    initTabBlock(new BlockTrafficPoleMastArmCurve("luminairearmtruss12ftconcrete", LuminaireArmProfile.TRUSS12FT));
    initTabBlock(new BlockTrafficPoleMastArmCurve("luminairearmtruss12ftdoublegalvanized", LuminaireArmProfile.TRUSS12FTDOUBLE));
    initTabBlock(new BlockTrafficPoleMastArmCurve("luminairearmtruss12ftdoublesilver", LuminaireArmProfile.TRUSS12FTDOUBLE));
    initTabBlock(new BlockTrafficPoleMastArmCurve("luminairearmtruss12ftdoubleblack", LuminaireArmProfile.TRUSS12FTDOUBLE));
    initTabBlock(new BlockTrafficPoleMastArmCurve("luminairearmtruss12ftdoublewhite", LuminaireArmProfile.TRUSS12FTDOUBLE));
    initTabBlock(new BlockTrafficPoleMastArmCurve("luminairearmtruss12ftdoubletan", LuminaireArmProfile.TRUSS12FTDOUBLE));
    initTabBlock(new BlockTrafficPoleMastArmCurve("luminairearmtruss12ftdoubleconcrete", LuminaireArmProfile.TRUSS12FTDOUBLE));
    initTabBlock(new BlockTrafficPoleMastArmCurve("luminairearmtruss12ftstraightgalvanized", LuminaireArmProfile.TRUSS12FTSTRAIGHT));
    initTabBlock(new BlockTrafficPoleMastArmCurve("luminairearmtruss12ftstraightsilver", LuminaireArmProfile.TRUSS12FTSTRAIGHT));
    initTabBlock(new BlockTrafficPoleMastArmCurve("luminairearmtruss12ftstraightblack", LuminaireArmProfile.TRUSS12FTSTRAIGHT));
    initTabBlock(new BlockTrafficPoleMastArmCurve("luminairearmtruss12ftstraightwhite", LuminaireArmProfile.TRUSS12FTSTRAIGHT));
    initTabBlock(new BlockTrafficPoleMastArmCurve("luminairearmtruss12ftstraighttan", LuminaireArmProfile.TRUSS12FTSTRAIGHT));
    initTabBlock(new BlockTrafficPoleMastArmCurve("luminairearmtruss12ftstraightconcrete", LuminaireArmProfile.TRUSS12FTSTRAIGHT));
    initTabBlock(new BlockTrafficPoleMastArmCurve("luminairearmtruss12ftstraightdoublegalvanized", LuminaireArmProfile.TRUSS12FTSTRAIGHTDOUBLE));
    initTabBlock(new BlockTrafficPoleMastArmCurve("luminairearmtruss12ftstraightdoublesilver", LuminaireArmProfile.TRUSS12FTSTRAIGHTDOUBLE));
    initTabBlock(new BlockTrafficPoleMastArmCurve("luminairearmtruss12ftstraightdoubleblack", LuminaireArmProfile.TRUSS12FTSTRAIGHTDOUBLE));
    initTabBlock(new BlockTrafficPoleMastArmCurve("luminairearmtruss12ftstraightdoublewhite", LuminaireArmProfile.TRUSS12FTSTRAIGHTDOUBLE));
    initTabBlock(new BlockTrafficPoleMastArmCurve("luminairearmtruss12ftstraightdoubletan", LuminaireArmProfile.TRUSS12FTSTRAIGHTDOUBLE));
    initTabBlock(new BlockTrafficPoleMastArmCurve("luminairearmtruss12ftstraightdoubleconcrete", LuminaireArmProfile.TRUSS12FTSTRAIGHTDOUBLE));
    initTabBlock(new BlockTrafficPoleMastArmCurve("luminairearmtruss15ftgalvanized", LuminaireArmProfile.TRUSS15FT));
    initTabBlock(new BlockTrafficPoleMastArmCurve("luminairearmtruss15ftsilver", LuminaireArmProfile.TRUSS15FT));
    initTabBlock(new BlockTrafficPoleMastArmCurve("luminairearmtruss15ftblack", LuminaireArmProfile.TRUSS15FT));
    initTabBlock(new BlockTrafficPoleMastArmCurve("luminairearmtruss15ftwhite", LuminaireArmProfile.TRUSS15FT));
    initTabBlock(new BlockTrafficPoleMastArmCurve("luminairearmtruss15fttan", LuminaireArmProfile.TRUSS15FT));
    initTabBlock(new BlockTrafficPoleMastArmCurve("luminairearmtruss15ftconcrete", LuminaireArmProfile.TRUSS15FT));
    initTabBlock(new BlockTrafficPoleMastArmCurve("luminairearmtruss15ftdoublegalvanized", LuminaireArmProfile.TRUSS15FTDOUBLE));
    initTabBlock(new BlockTrafficPoleMastArmCurve("luminairearmtruss15ftdoublesilver", LuminaireArmProfile.TRUSS15FTDOUBLE));
    initTabBlock(new BlockTrafficPoleMastArmCurve("luminairearmtruss15ftdoubleblack", LuminaireArmProfile.TRUSS15FTDOUBLE));
    initTabBlock(new BlockTrafficPoleMastArmCurve("luminairearmtruss15ftdoublewhite", LuminaireArmProfile.TRUSS15FTDOUBLE));
    initTabBlock(new BlockTrafficPoleMastArmCurve("luminairearmtruss15ftdoubletan", LuminaireArmProfile.TRUSS15FTDOUBLE));
    initTabBlock(new BlockTrafficPoleMastArmCurve("luminairearmtruss15ftdoubleconcrete", LuminaireArmProfile.TRUSS15FTDOUBLE));
    initTabBlock(new BlockTrafficPoleMastArmCurve("luminairearmtruss15ftstraightgalvanized", LuminaireArmProfile.TRUSS15FTSTRAIGHT));
    initTabBlock(new BlockTrafficPoleMastArmCurve("luminairearmtruss15ftstraightsilver", LuminaireArmProfile.TRUSS15FTSTRAIGHT));
    initTabBlock(new BlockTrafficPoleMastArmCurve("luminairearmtruss15ftstraightblack", LuminaireArmProfile.TRUSS15FTSTRAIGHT));
    initTabBlock(new BlockTrafficPoleMastArmCurve("luminairearmtruss15ftstraightwhite", LuminaireArmProfile.TRUSS15FTSTRAIGHT));
    initTabBlock(new BlockTrafficPoleMastArmCurve("luminairearmtruss15ftstraighttan", LuminaireArmProfile.TRUSS15FTSTRAIGHT));
    initTabBlock(new BlockTrafficPoleMastArmCurve("luminairearmtruss15ftstraightconcrete", LuminaireArmProfile.TRUSS15FTSTRAIGHT));
    initTabBlock(new BlockTrafficPoleMastArmCurve("luminairearmtruss15ftstraightdoublegalvanized", LuminaireArmProfile.TRUSS15FTSTRAIGHTDOUBLE));
    initTabBlock(new BlockTrafficPoleMastArmCurve("luminairearmtruss15ftstraightdoublesilver", LuminaireArmProfile.TRUSS15FTSTRAIGHTDOUBLE));
    initTabBlock(new BlockTrafficPoleMastArmCurve("luminairearmtruss15ftstraightdoubleblack", LuminaireArmProfile.TRUSS15FTSTRAIGHTDOUBLE));
    initTabBlock(new BlockTrafficPoleMastArmCurve("luminairearmtruss15ftstraightdoublewhite", LuminaireArmProfile.TRUSS15FTSTRAIGHTDOUBLE));
    initTabBlock(new BlockTrafficPoleMastArmCurve("luminairearmtruss15ftstraightdoubletan", LuminaireArmProfile.TRUSS15FTSTRAIGHTDOUBLE));
    initTabBlock(new BlockTrafficPoleMastArmCurve("luminairearmtruss15ftstraightdoubleconcrete", LuminaireArmProfile.TRUSS15FTSTRAIGHTDOUBLE));

    // --- Decorative and historic street light poles (GitHub #99; OBJ, one block stacked, base,
    // cap and capital decided per block from its neighbours -- gen_decorative_poles.py) ---
    initTabBlock(new BlockTrafficPoleDecorative("decopolelafluted", 4.0, 4.0, 7.2, TRAFFIC_POLE_COLOR.BLACK));
    initTabBlock(new BlockTrafficPoleDecorative("decopolelabroadwayrose", 4.0, 4.0, 7.0, TRAFFIC_POLE_COLOR.BLACK));
    initTabBlock(new BlockTrafficPoleDecorative("decopolelaconcrete", 4.0, 4.5, 7.0, TRAFFIC_POLE_COLOR.SILVER));
    initTabBlock(new BlockTrafficPoleDecorative("decopolelawestwood", 4.0, 4.0, 6.8, TRAFFIC_POLE_COLOR.BLACK));
    initTabBlock(new BlockTrafficPoleDecorative("decopolelahollywood", 3.0, 3.0, 5.2, TRAFFIC_POLE_COLOR.SILVER));
    initTabBlock(new BlockTrafficPoleMastArmCurve("luminairearmlaupsweep8ftgalvanized", LuminaireArmProfile.LAUPSWEEP8FT));
    initTabBlock(new BlockTrafficPoleMastArmCurve("luminairearmlaupsweep8ftsilver", LuminaireArmProfile.LAUPSWEEP8FT));
    initTabBlock(new BlockTrafficPoleMastArmCurve("luminairearmlaupsweep8ftblack", LuminaireArmProfile.LAUPSWEEP8FT));
    initTabBlock(new BlockTrafficPoleMastArmCurve("luminairearmlaupsweep10ftgalvanized", LuminaireArmProfile.LAUPSWEEP10FT));
    initTabBlock(new BlockTrafficPoleMastArmCurve("luminairearmlaupsweep10ftsilver", LuminaireArmProfile.LAUPSWEEP10FT));
    initTabBlock(new BlockTrafficPoleMastArmCurve("luminairearmlaupsweep10ftblack", LuminaireArmProfile.LAUPSWEEP10FT));
    initTabBlock(new BlockTrafficPoleMastArmCurve("luminairearmlaupsweep12ftgalvanized", LuminaireArmProfile.LAUPSWEEP12FT));
    initTabBlock(new BlockTrafficPoleMastArmCurve("luminairearmlaupsweep12ftsilver", LuminaireArmProfile.LAUPSWEEP12FT));
    initTabBlock(new BlockTrafficPoleMastArmCurve("luminairearmlaupsweep12ftblack", LuminaireArmProfile.LAUPSWEEP12FT));
    initTabBlock(new BlockTrafficPoleMastArmCurve("luminairearmvannuystriplegalvanized", LuminaireArmProfile.VANNUYSTRIPLE));
    initTabBlock(new BlockTrafficPoleMastArmCurve("luminairearmvannuystriplesilver", LuminaireArmProfile.VANNUYSTRIPLE));
    initTabBlock(new BlockTrafficPoleDecorative("decopoleanaheim", 3.0, 3.0, 5.0, TRAFFIC_POLE_COLOR.BLACK));
    initTabBlock(new BlockTrafficPoleMastArmCurve("luminairearmanaheim8ftgreen", LuminaireArmProfile.ANAHEIM8FT));
    initTabBlock(new BlockTrafficPoleDecorative("decopolereprofluted", 3.0, 3.0, 6.0, TRAFFIC_POLE_COLOR.BLACK));
    initTabBlock(new BlockTrafficPoleDecorative("decopolecalheights", 3.0, 2.6, 4.6, TRAFFIC_POLE_COLOR.BLACK));

    // --- NSEWUD factory blocks: Vertical Double Guy Mounts ---
    initTabBlock(new BlockTrafficAccessoryNSEWUD("trafficpoleverticaldoubleguymountblack", BB_TRAFFIC_POLE_VERT_DOUBLE_GUY_MOUNT, BlockRenderLayer.CUTOUT_MIPPED, 2F, true));
    initTabBlock(new BlockTrafficAccessoryNSEWUD("trafficpoleverticaldoubleguymountsilver", BB_TRAFFIC_POLE_VERT_DOUBLE_GUY_MOUNT, BlockRenderLayer.CUTOUT_MIPPED, 2F, true));
    initTabBlock(new BlockTrafficAccessoryNSEWUD("trafficpoleverticaldoubleguymounttan", BB_TRAFFIC_POLE_VERT_DOUBLE_GUY_MOUNT, BlockRenderLayer.CUTOUT_MIPPED, 2F, true));
    initTabBlock(new BlockTrafficAccessoryNSEWUD("trafficpoleverticaldoubleguymountunpainted", BB_TRAFFIC_POLE_VERT_DOUBLE_GUY_MOUNT, BlockRenderLayer.CUTOUT_MIPPED, 2F, true));
    initTabBlock(new BlockTrafficAccessoryNSEWUD("trafficpoleverticaldoubleguymountwhite", BB_TRAFFIC_POLE_VERT_DOUBLE_GUY_MOUNT, BlockRenderLayer.CUTOUT_MIPPED, 2F, true));

    // --- NSEWUD factory blocks: Vertical Light Mounts ---
    initTabBlock(new BlockTrafficAccessoryNSEWUD.PoleFitted("trafficpoleverticallightmount", BB_TRAFFIC_POLE_VERT_LIGHT_MOUNT, BlockRenderLayer.CUTOUT_MIPPED, 2F, true));
    initTabBlock(new BlockTrafficAccessoryNSEWUD.PoleFitted("trafficpoleverticallightmountblack", BB_TRAFFIC_POLE_VERT_LIGHT_MOUNT, BlockRenderLayer.CUTOUT_MIPPED, 2F, true));
    initTabBlock(new BlockTrafficAccessoryNSEWUD.PoleFitted("trafficpoleverticallightmounttan", BB_TRAFFIC_POLE_VERT_LIGHT_MOUNT, BlockRenderLayer.CUTOUT_MIPPED, 2F, true));
    initTabBlock(new BlockTrafficAccessoryNSEWUD.PoleFitted("trafficpoleverticallightmountunpainted", BB_TRAFFIC_POLE_VERT_LIGHT_MOUNT, BlockRenderLayer.CUTOUT_MIPPED, 2F, false));
    initTabBlock(new BlockTrafficAccessoryNSEWUD.PoleFitted("trafficpoleverticallightmountwhite", BB_TRAFFIC_POLE_VERT_LIGHT_MOUNT, BlockRenderLayer.CUTOUT_MIPPED, 2F, true));

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

    // --- NSEWUD factory blocks: Horizontal Double Poles ---
    initTabBlock(new BlockTrafficAccessoryNSEWUD("trafficpolehorzdblblack", BB_TRAFFIC_POLE_HORZ_DBL, BlockRenderLayer.CUTOUT_MIPPED, 2F, true));
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
    initTabBlock(new BlockTrafficAccessoryNSEWUD("trafficpolevertdblsilver", BB_TRAFFIC_POLE_VERT_DBL, BlockRenderLayer.CUTOUT_MIPPED, 2F, true));
    initTabBlock(new BlockTrafficAccessoryNSEWUD("trafficpolevertdbltan", BB_TRAFFIC_POLE_VERT_DBL, BlockRenderLayer.CUTOUT_MIPPED, 2F, true));
    initTabBlock(new BlockTrafficAccessoryNSEWUD("trafficpolevertdblunpainted", BB_TRAFFIC_POLE_VERT_DBL, BlockRenderLayer.CUTOUT_MIPPED, 2F, true));
    initTabBlock(new BlockTrafficAccessoryNSEWUD("trafficpolevertdblwhite", BB_TRAFFIC_POLE_VERT_DBL, BlockRenderLayer.CUTOUT_MIPPED, 2F, true));

    // --- NSEWUD factory blocks: Legacy lowercase connectors/mounts ---
    initTabBlock(new BlockTrafficAccessoryNSEWUD("trafficpoleverticalconnectorunpainted", BB_TRAFFIC_POLE_VERT_CONNECTOR, BlockRenderLayer.CUTOUT_MIPPED, 2F, true));
    initTabBlock(new BlockTrafficAccessoryNSEWUD("trafficpoleverticalcurveconnectorunpainted", BB_TRAFFIC_POLE_VERT_CURVE_CONNECTOR, BlockRenderLayer.CUTOUT_MIPPED, 2F, true));
    initTabBlock(new BlockTrafficAccessoryNSEWUD("trafficpoleverticalquadmountunpainted", BB_TRAFFIC_POLE_VERT_QUAD_MOUNT, BlockRenderLayer.CUTOUT_MIPPED, 2F, true));
    initTabBlock(new BlockTrafficAccessoryNSEWUD("trafficpoleverticalsignalmountunpainted", BB_TRAFFIC_POLE_VERT_SIGNAL_MOUNT, BlockRenderLayer.CUTOUT_MIPPED, 2F, true));

    // --- Class-based: Traffic Pole Large (Unpainted) ---
    initTabBlock(BlockTrafficPoleLargeUnpainted.class, fmlPreInitializationEvent);
  }
}

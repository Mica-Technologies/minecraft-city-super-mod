package com.micatechnologies.minecraft.csm.tabs;

import com.micatechnologies.minecraft.csm.CsmRegistry;
import com.micatechnologies.minecraft.csm.codeutils.CsmTab;
import com.micatechnologies.minecraft.csm.trafficaccessories.BarricadeGeometry;
import com.micatechnologies.minecraft.csm.trafficaccessories.BlockCrashCushion;
import com.micatechnologies.minecraft.csm.trafficaccessories.BlockGuardrail;
import com.micatechnologies.minecraft.csm.trafficaccessories.BlockGuardrailBoxBeam;
import com.micatechnologies.minecraft.csm.trafficaccessories.BlockGuardrailEnd;
import com.micatechnologies.minecraft.csm.trafficaccessories.BlockGuardrailTransition;
import com.micatechnologies.minecraft.csm.trafficaccessories.ItemGuardrailTool;
import com.micatechnologies.minecraft.csm.trafficaccessories.BlockWorkZoneArrowBoard;
import com.micatechnologies.minecraft.csm.trafficaccessories.BlockWorkZoneBarricade;
import com.micatechnologies.minecraft.csm.trafficaccessories.BlockWorkZoneBarricadeFolding;
import com.micatechnologies.minecraft.csm.trafficaccessories.BlockWorkZoneDevice;
import com.micatechnologies.minecraft.csm.trafficaccessories.BlockWorkZoneDeviceDiagonal;
import com.micatechnologies.minecraft.csm.trafficaccessories.BlockWorkZoneDeviceFlashing;
import com.micatechnologies.minecraft.csm.trafficaccessories.BlockWorkZoneDeviceRotatable;
import com.micatechnologies.minecraft.csm.trafficaccessories.BlockWorkZoneFence;
import com.micatechnologies.minecraft.csm.trafficaccessories.BlockWorkZonePlate;
import com.micatechnologies.minecraft.csm.trafficaccessories.BlockWorkZoneWall;
import net.minecraft.block.Block;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;

/**
 * The tab for work zone and roadside safety devices: channelizing devices, barricades, arrow
 * boards, guardrails and their ends and transitions, box beam, cable barrier and crash
 * cushions, with the Guardrail Tool. Split out of {@link CsmTabTrafficAccessories}.
 *
 * @version 1.0
 */
@CsmTab.Load(order = 9, sub = 1)
public class CsmTabWorkZone extends CsmTab {

  /**
   * Gets the ID (unique identifier) of the tab.
   *
   * @return the ID of the tab
   *
   * @since 1.0
   */
  @Override
  public String getTabId() {
    return "tabworkzone";
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
    return CsmRegistry.getBlock("traffic_cone");
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


  /**
   * Initializes all the elements belonging to the tab.
   *
   * @param fmlPreInitializationEvent the {@link FMLPreInitializationEvent} that is being processed
   *
   * @since 1.0
   */
  @Override
  public void initTabElements(FMLPreInitializationEvent fmlPreInitializationEvent) {
    // --- Work zone channelizing devices ---
    // Bounding boxes come from the models' own extents, emitted by
    // dev-env-utils/scripts/gen_work_zone_devices.py alongside the geometry, so they follow the
    // shape rather than being eyeballed. Regenerate rather than hand-editing them.
    initTabBlock(new BlockWorkZoneDevice("traffic_cone",
        new AxisAlignedBB(0.297187, 0.000000, 0.297187, 0.702813, 0.770000, 0.702813)));
    initTabBlock(new BlockWorkZoneDevice("traffic_cone_lime",
        new AxisAlignedBB(0.297187, 0.000000, 0.297187, 0.702813, 0.770000, 0.702813)));
    initTabBlock(new BlockWorkZoneDeviceDiagonal("traffic_cone_knocked",
        new AxisAlignedBB(0.297187, 0.000000, 0.115000, 0.702813, 0.405625, 0.885000)));
    initTabBlock(new BlockWorkZoneDeviceFlashing("traffic_drum",
        new AxisAlignedBB(0.064777, 0.000000, 0.064777, 0.935223, 1.077462, 0.935223)));
    initTabBlock(new BlockWorkZoneDevice("traffic_drum_unlit",
        new AxisAlignedBB(0.064777, 0.000000, 0.064777, 0.935223, 0.959375, 0.935223)));
    initTabBlock(new BlockWorkZoneDevice("channelizer_tube",
        new AxisAlignedBB(0.213129, 0.000000, 0.168750, 0.786871, 1.183469, 0.831250)));
    initTabBlock(new BlockWorkZoneDevice("channelizer_tube_lime",
        new AxisAlignedBB(0.213129, 0.000000, 0.168750, 0.786871, 1.183469, 0.831250)));
    initTabBlock(new BlockWorkZoneDeviceDiagonal("channelizer_cade_left",
        new AxisAlignedBB(0.217062, 0.000000, 0.217062, 0.782938, 0.975000, 0.782938)));
    initTabBlock(new BlockWorkZoneDeviceDiagonal("channelizer_cade_right",
        new AxisAlignedBB(0.217062, 0.000000, 0.217062, 0.782938, 0.975000, 0.782938)));
    initTabBlock(new BlockWorkZoneBarricade("barricade_type_1_left", BarricadeGeometry.TYPE1_TOP,
        new AxisAlignedBB(-0.257075, 0.000000, 0.204250, 1.257075, 1.092000, 0.795750)));
    initTabBlock(new BlockWorkZoneBarricade("barricade_type_1_right", BarricadeGeometry.TYPE1_TOP,
        new AxisAlignedBB(-0.257075, 0.000000, 0.204250, 1.257075, 1.092000, 0.795750)));
    initTabBlock(new BlockWorkZoneBarricadeFolding("barricade_type_2_left",
        BarricadeGeometry.FOLDING_TOP,
        new AxisAlignedBB(0.109375, 0.000000, 0.290625, 0.890625, 0.928125, 0.709375)));
    initTabBlock(new BlockWorkZoneBarricadeFolding("barricade_type_2_right",
        BarricadeGeometry.FOLDING_TOP,
        new AxisAlignedBB(0.109375, 0.000000, 0.290625, 0.890625, 0.928125, 0.709375)));
    initTabBlock(new BlockWorkZoneBarricade("barricade_type_3_left", BarricadeGeometry.TYPE3_TOP,
        new AxisAlignedBB(-0.257075, 0.000000, 0.204250, 1.257075, 1.501500, 0.795750)));
    initTabBlock(new BlockWorkZoneBarricade("barricade_type_3_right", BarricadeGeometry.TYPE3_TOP,
        new AxisAlignedBB(-0.257075, 0.000000, 0.204250, 1.257075, 1.501500, 0.795750)));
    initTabBlock(new BlockWorkZoneDeviceDiagonal("pavement_marker_white",
        new AxisAlignedBB(0.342500, 0.000000, 0.395000, 0.657500, 0.137812, 0.552500)));
    initTabBlock(new BlockWorkZoneDeviceDiagonal("pavement_marker_yellow",
        new AxisAlignedBB(0.342500, 0.000000, 0.395000, 0.657500, 0.137812, 0.552500)));
    initTabBlock(new BlockWorkZoneDeviceDiagonal("pavement_marker_blue",
        new AxisAlignedBB(0.342500, 0.000000, 0.395000, 0.657500, 0.137812, 0.552500)));
    initTabBlock(new BlockWorkZoneDeviceDiagonal("pavement_marker_green",
        new AxisAlignedBB(0.342500, 0.000000, 0.395000, 0.657500, 0.137812, 0.552500)));
    initTabBlock(new BlockWorkZoneDeviceDiagonal("pavement_marker_orange",
        new AxisAlignedBB(0.342500, 0.000000, 0.395000, 0.657500, 0.137812, 0.552500)));
    initTabBlock(new BlockWorkZoneDeviceDiagonal("pavement_marker_red",
        new AxisAlignedBB(0.342500, 0.000000, 0.395000, 0.657500, 0.137812, 0.552500)));
    initTabBlock(new BlockWorkZoneWall("channelizing_wall_orange",
        new AxisAlignedBB(0.000000, 0.000000, 0.312500, 1.000000, 0.687500, 0.687500)));
    initTabBlock(new BlockWorkZoneWall("channelizing_wall_white",
        new AxisAlignedBB(0.000000, 0.000000, 0.312500, 1.000000, 0.687500, 0.687500)));
    initTabBlock(new BlockWorkZoneWall("concrete_barrier",
        new AxisAlignedBB(0.000000, 0.000000, 0.275000, 1.000000, 0.812500, 0.725000)));
    initTabBlock(new BlockWorkZoneDeviceRotatable("portable_signal_trailer_arm",
        new AxisAlignedBB(-1.875000, 0.000000, -0.312500, 2.125000, 1.400000, 1.312500)));
    initTabBlock(new BlockWorkZoneDeviceDiagonal("portable_signal_trailer",
        new AxisAlignedBB(-1.125000, 0.000000, 0.000000, 1.625000, 1.350000, 1.000000)));
    initTabBlock(new BlockWorkZoneDeviceDiagonal("portable_ped_signal_trailer",
        new AxisAlignedBB(-1.125000, 0.000000, 0.000000, 1.625000, 1.350000, 1.000000)));
    initTabBlock(new BlockWorkZoneDeviceDiagonal("vertical_panel_left",
        new AxisAlignedBB(0.368750, 0.000000, 0.397500, 0.631250, 0.887500, 0.685000)));
    initTabBlock(new BlockWorkZoneDeviceDiagonal("vertical_panel_right",
        new AxisAlignedBB(0.368750, 0.000000, 0.397500, 0.631250, 0.887500, 0.685000)));
    initTabBlock(new BlockWorkZoneDeviceDiagonal("flagger_paddle",
        new AxisAlignedBB(0.350000, 0.000000, 0.428125, 0.650000, 0.800000, 0.571875)));
    initTabBlock(new BlockWorkZonePlate("road_plate",
        new AxisAlignedBB(0.000000, 0.000000, 0.000000, 1.000000, 0.053125, 1.000000)));
    initTabBlock(new BlockWorkZoneFence("safety_fence",
        new AxisAlignedBB(0.000000, 0.000000, 0.473750, 1.000000, 0.887500, 0.526250)));
    initTabBlock(new BlockWorkZoneArrowBoard("arrow_board",
        new AxisAlignedBB(-1.388350, 0.000000, -0.379375, 2.573750, 0.603750, 1.379375)));
    initTabBlock(new BlockWorkZoneDevice("sand_barrel_array",
        new AxisAlignedBB(0.064777, 0.000000, 0.064777, 0.935223, 0.862500, 0.935223)));

    // --- Guardrails -------------------------------------------------------------------------
    // Generated by gen_guardrails.py and gen_guardrail_ends.py; the bounding boxes are the
    // inventory models' own extents, which leaves the diagonal filler out on purpose -- a lone
    // rail must not claim the cell beside it.
    //
    // Post material and sidedness are separate blocks rather than state on one, because a
    // guardrail run is long and a tile entity per cell would put thousands along a highway. The
    // Guardrail Tool swaps a block for its sibling, which costs nothing to store.
    initTabBlock(new BlockGuardrail("w_beam_guardrail",
        new AxisAlignedBB(0.000000, 0.000000, 0.050000, 1.000000, 1.000000, 0.600000)));
    initTabBlock(new BlockGuardrail("w_beam_guardrail_wood",
        new AxisAlignedBB(0.000000, 0.000000, 0.050000, 1.000000, 1.000000, 0.600000)));
    initTabBlock(new BlockGuardrail("w_beam_guardrail_double",
        new AxisAlignedBB(0.000000, 0.000000, 0.050000, 1.000000, 1.000000, 0.950000)));
    initTabBlock(new BlockGuardrail("w_beam_guardrail_wood_double",
        new AxisAlignedBB(0.000000, 0.000000, 0.050000, 1.000000, 1.000000, 0.950000)));
    initTabBlock(new BlockGuardrailEnd("guardrail_end_flared",
        new AxisAlignedBB(0.000000, 0.000000, 0.050000, 0.827622, 1.000000, 0.730165)));
    initTabBlock(new BlockGuardrailEnd("guardrail_end_boxing_glove",
        new AxisAlignedBB(0.000000, 0.000000, 0.050000, 0.862500, 1.000000, 0.525000)));
    initTabBlock(new BlockGuardrailEnd("guardrail_end_terminal",
        new AxisAlignedBB(0.000000, 0.000000, 0.025000, 1.000000, 1.000000, 0.607322)));
    initTabBlock(new BlockGuardrailEnd("guardrail_end_turndown",
        new AxisAlignedBB(0.000000, 0.000000, 0.018750, 1.000000, 1.000000, 0.600000)));
    // The RAIL KIND is what a run joins on, and it is the one thing the generators cannot know.
    // Left off, every family would default to w_beam and a box beam would happily join a W-beam
    // run — two rails of different depths meeting with a step and calling themselves one run.
    initTabBlock(new BlockGuardrail("thrie_beam_guardrail",
        new AxisAlignedBB(0.000000, 0.000000, 0.050000, 1.000000, 1.000000, 0.600000), THRIE));
    initTabBlock(new BlockGuardrail("thrie_beam_guardrail_wood",
        new AxisAlignedBB(0.000000, 0.000000, 0.050000, 1.000000, 1.000000, 0.600000), THRIE));
    initTabBlock(new BlockGuardrail("thrie_beam_guardrail_double",
        new AxisAlignedBB(0.000000, 0.000000, 0.050000, 1.000000, 1.000000, 0.950000), THRIE));
    initTabBlock(new BlockGuardrail("thrie_beam_guardrail_wood_double",
        new AxisAlignedBB(0.000000, 0.000000, 0.050000, 1.000000, 1.000000, 0.950000), THRIE));
    initTabBlock(new BlockGuardrailEnd("guardrail_end_flared_thrie",
        new AxisAlignedBB(0.000000, 0.000000, 0.050000, 0.862829, 1.000000, 0.730165), THRIE));
    initTabBlock(new BlockGuardrailEnd("guardrail_end_turndown_thrie",
        new AxisAlignedBB(0.000000, 0.000000, 0.018750, 1.000000, 1.000000, 0.600000), THRIE));
    // W-beam at one end and thrie at the other, which is why it needs a class of its own: a block
    // that is two rails matches neither neighbour under the ordinary rule.
    initTabBlock(new BlockGuardrailTransition("w_beam_thrie_transition",
        new AxisAlignedBB(0.000000, 0.000000, 0.050000, 1.000000, 1.000000, 0.600000),
        W_BEAM, THRIE));

    initTabBlock(new BlockGuardrailBoxBeam("box_beam_guardrail",
        new AxisAlignedBB(0.000000, 0.000000, 0.050000, 1.000000, 1.000000, 0.712500), BOX_BEAM));
    initTabBlock(new BlockGuardrailBoxBeam("box_beam_guardrail_double",
        new AxisAlignedBB(0.000000, 0.000000, 0.050000, 1.000000, 1.000000, 0.950000), BOX_BEAM));
    // Two tubes half a block apart, for bridges (#191): a stacked block on a single one makes
    // three evenly spaced tubes, and two stacked blocks make four.
    initTabBlock(new BlockGuardrailBoxBeam("box_beam_guardrail_stacked",
        new AxisAlignedBB(0.000000, 0.000000, 0.050000, 1.000000, 1.000000, 0.712500), BOX_BEAM));
    initTabBlock(new BlockGuardrailBoxBeam("box_beam_guardrail_stacked_double",
        new AxisAlignedBB(0.000000, 0.000000, 0.050000, 1.000000, 1.000000, 0.950000), BOX_BEAM));
    initTabBlock(new BlockGuardrail("cable_barrier",
        new AxisAlignedBB(0.000000, 0.000000, 0.096875, 1.000000, 1.000000, 0.306250), CABLE));
    initTabBlock(new BlockGuardrail("cable_barrier_double",
        new AxisAlignedBB(0.000000, 0.000000, 0.096875, 1.000000, 1.000000, 0.403125), CABLE));
    initTabBlock(new BlockGuardrailEnd("guardrail_end_bullnose_box",
        new AxisAlignedBB(0.000000, 0.000000, 0.012500, 0.631250, 1.000000, 0.800000), BOX_BEAM));
    initTabBlock(new BlockGuardrailEnd("cable_barrier_anchor",
        new AxisAlignedBB(0.000000, 0.000000, 0.012500, 0.537500, 1.000000, 0.287500), CABLE));

    initTabBlock(new BlockCrashCushion("crash_cushion_nose",
        new AxisAlignedBB(0.000000, 0.000000, 0.050000, 1.000000, 0.843750, 0.500000), true));
    initTabBlock(new BlockCrashCushion("crash_cushion_bay",
        new AxisAlignedBB(0.000000, 0.000000, 0.028125, 1.000000, 0.868750, 0.521875)));

    initTabItem(ItemGuardrailTool.class, fmlPreInitializationEvent); // Guardrail Tool
  }

  /** Rail sections. Two guardrails join only if the rails at the ends that meet are the same. */
  private static final String W_BEAM = "w_beam";
  private static final String THRIE = "thrie_beam";
  private static final String BOX_BEAM = "box_beam";
  private static final String CABLE = "cable";
}

package com.micatechnologies.minecraft.csm.tabs;

import com.micatechnologies.minecraft.csm.CsmRegistry;
import com.micatechnologies.minecraft.csm.codeutils.CsmTab;
import com.micatechnologies.minecraft.csm.powergrid.gas.BlockEquipmentSkid;
import com.micatechnologies.minecraft.csm.powergrid.gas.BlockVentStack;
import com.micatechnologies.minecraft.csm.powergrid.services.BlockUtilityFixture;
import com.micatechnologies.minecraft.csm.powergrid.services.BlockUtilityPanel;
import com.micatechnologies.minecraft.csm.powergrid.services.BlockUtilityRun;
import com.micatechnologies.minecraft.csm.powergrid.services.BlockUtilitySign;
import com.micatechnologies.minecraft.csm.powergrid.sewer.BlockAccessHatch;
import com.micatechnologies.minecraft.csm.powergrid.sewer.BlockHeadwall;
import com.micatechnologies.minecraft.csm.powergrid.sewer.BlockManholeCone;
import com.micatechnologies.minecraft.csm.powergrid.sewer.BlockPrecastRun;
import com.micatechnologies.minecraft.csm.powergrid.sewer.BlockRiprap;
import com.micatechnologies.minecraft.csm.powergrid.sewer.BlockStackedSection;
import com.micatechnologies.minecraft.csm.powergrid.sewer.BlockSwitchedUnit;
import com.micatechnologies.minecraft.csm.powergrid.sewer.BlockWingwall;
import com.micatechnologies.minecraft.csm.powergrid.telecom.BlockAntennaArray;
import com.micatechnologies.minecraft.csm.powergrid.telecom.BlockCabinet;
import com.micatechnologies.minecraft.csm.powergrid.telecom.BlockIceBridge;
import com.micatechnologies.minecraft.csm.powergrid.telecom.BlockPoleRadio;
import com.micatechnologies.minecraft.csm.powergrid.telecom.BlockSmallCell;
import com.micatechnologies.minecraft.csm.powergrid.water.BlockCagedLadder;
import com.micatechnologies.minecraft.csm.powergrid.water.BlockGroundTank;
import com.micatechnologies.minecraft.csm.powergrid.water.BlockPedestalSection;
import com.micatechnologies.minecraft.csm.powergrid.water.BlockPipeFitting;
import com.micatechnologies.minecraft.csm.powergrid.water.BlockPumpUnit;
import com.micatechnologies.minecraft.csm.powergrid.water.BlockTankTile;
import com.micatechnologies.minecraft.csm.powergrid.water.BlockTowerBrace;
import com.micatechnologies.minecraft.csm.powergrid.water.BlockTowerColumn;
import com.micatechnologies.minecraft.csm.powergrid.water.BlockTowerStrut;
import com.micatechnologies.minecraft.csm.powergrid.water.BlockWaterPipe;
import com.micatechnologies.minecraft.csm.powergrid.water.TankShapes;
import com.micatechnologies.minecraft.csm.streetscape.BlockUtilityBox;
import com.micatechnologies.minecraft.csm.streetscape.UtilityBoxSpec;
import net.minecraft.block.Block;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;

/**
 * The Utilities tab: the services a city runs to its buildings, from the meters on the wall back
 * to the plant. The overhead line's poles and hardware stay in the Power Grid tab, in the same
 * module; see {@code assets/docs/UTILITIES_SYSTEM.md} for why there are two.
 *
 * <p>The block lines are written by {@code dev-env-utils/scripts/gen_utilities_meters.py
 * --fragments}, {@code gen_utilities_water.py --fragments}, {@code gen_utilities_sewer.py
 * --fragments} and {@code gen_utilities_gas_telecom.py --fragments}, which measure every box from
 * the model they write.</p>
 *
 * @since 2026.9
 */
@CsmTab.Load(order = 28)
public class CsmTabUtilities extends CsmTab {

  @Override
  public String getTabId() {
    return "tabutilities";
  }

  @Override
  public Block getTabIcon() {
    return CsmRegistry.getBlock("electric_meter_digital");
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
    // --- Building service meters (gen_utilities_meters.py --fragments) ---
    initTabBlock(new BlockUtilityFixture("electric_meter_digital", new double[]{4.25, 0, 10.29, 11.75, 13, 16}));
    initTabBlock(new BlockUtilityFixture("electric_meter_analog", new double[]{4.25, 0, 10.29, 11.75, 13, 16}));
    initTabBlock(new BlockUtilityFixture("electric_meter_socket", new double[]{4.25, 0, 12.49, 11.75, 13, 16}));
    initTabBlock(new BlockUtilityRun("electric_meter_bank", new double[]{0, 0.6, 10.29, 16, 15.9, 16}));
    initTabBlock(new BlockUtilityFixture("service_disconnect", new double[]{3.2, 0, 11.5, 11.5, 16, 16}));
    initTabBlock(new BlockUtilityPanel("main_panel", new double[]{2.5, 1, 13.2, 13.5, 15.5, 16}, new double[]{2.5, 1, 3, 13.7, 15.5, 16}));
    initTabBlock(new BlockUtilityBox("electric_switchboard", new UtilityBoxSpec(1, 1, 2, new AxisAlignedBB(0.019, 0, 0.049, 0.981, 2, 1), null)));
    initTabBlock(new BlockUtilityFixture("gas_meter", new double[]{4.5, 0, 10.95, 15, 15.8, 16}));
    initTabBlock(new BlockUtilityRun("gas_meter_bank", new double[]{0, 0, 10.95, 16, 16, 16}));
    initTabBlock(new BlockUtilityFixture("water_meter_setter", new double[]{0.6, 0, 11.1, 14.3, 16, 16}));
    initTabBlock(new BlockUtilityFixture("utility_label_electric", new double[]{1, 9, 15.5, 15, 12.5, 16}));
    initTabBlock(new BlockUtilityFixture("utility_label_gas", new double[]{1, 9, 15.5, 15, 12.5, 16}));
    initTabBlock(new BlockUtilityFixture("utility_label_water", new double[]{1, 9, 15.5, 15, 12.5, 16}));
    initTabBlock(new BlockUtilityFixture("utility_label_disconnect", new double[]{1, 9, 15.5, 15, 12.5, 16}));

    // --- Water system (gen_utilities_water.py --fragments) ---
    initTabBlock(new BlockTowerColumn("water_tower_leg", 5.5));
    initTabBlock(new BlockTowerColumn("water_tower_riser", 6.8));
    initTabBlock(new BlockTowerBrace("water_tower_brace"));
    initTabBlock(new BlockTowerStrut("water_tower_strut"));
    initTabBlock(new BlockCagedLadder("caged_ladder", new double[]{1.2, 0, 1.6, 14.8, 16, 16}));
    initTabBlock(new BlockPedestalSection("water_tower_pedestal"));
    initTabBlock(new BlockTankTile("water_tower_bowl_small", TankShapes.BOWL_SMALL));
    initTabBlock(new BlockTankTile("water_tower_bowl_medium", TankShapes.BOWL_MEDIUM));
    initTabBlock(new BlockTankTile("water_tower_spheroid_small", TankShapes.SPHEROID_SMALL));
    initTabBlock(new BlockTankTile("water_tower_spheroid_medium", TankShapes.SPHEROID_MEDIUM));
    initTabBlock(new BlockGroundTank("ground_tank_small", TankShapes.TANK_SMALL));
    initTabBlock(new BlockGroundTank("ground_tank_large", TankShapes.TANK_LARGE));
    initTabBlock(new BlockWaterPipe("water_pipe"));
    initTabBlock(new BlockUtilityFixture("water_pipe_support", new double[]{3, 0, 2.5, 13, 16, 13.5}));
    initTabBlock(new BlockPipeFitting("water_gate_valve", new double[]{2.6, 2.6, 0, 13.4, 16, 16}));
    initTabBlock(new BlockPipeFitting("water_butterfly_valve", new double[]{2.2, 2.2, 0, 13.8, 16, 16}));
    initTabBlock(new BlockPipeFitting("water_check_valve", new double[]{3, 2.6, 0, 14.4, 13.61, 16}));
    initTabBlock(new BlockPipeFitting("water_flow_meter", new double[]{2.1, 2.1, 0, 13.9, 16, 16}));
    initTabBlock(new BlockPipeFitting("water_air_release_valve", new double[]{3, 3, 0, 13, 16, 16}));
    initTabBlock(new BlockPumpUnit("pump_split_case", new UtilityBoxSpec(2, 1, 1, new AxisAlignedBB(-0.938, 0, -0.001, 0.938, 0.912, 1.001), null), BlockPumpUnit.Nozzles.FRONT_BACK));
    initTabBlock(new BlockPumpUnit("pump_vertical_inline", new UtilityBoxSpec(1, 1, 2, new AxisAlignedBB(-0.001, 0, 0.175, 1.001, 1.876, 0.825), null), BlockPumpUnit.Nozzles.LEFT_RIGHT));
    initTabBlock(new BlockPumpUnit("water_hydropneumatic_tank", new UtilityBoxSpec(1, 1, 2, new AxisAlignedBB(0.1, 0, 0.049, 0.9, 1.838, 1.001), null), BlockPumpUnit.Nozzles.BACK));
    initTabBlock(new BlockUtilityBox("pump_control_panel", new UtilityBoxSpec(1, 1, 2, new AxisAlignedBB(0.037, 0, 0.225, 0.963, 2, 0.994), null)));
    initTabBlock(new BlockUtilityBox("air_release_enclosure", new UtilityBoxSpec(1, 1, 1, new AxisAlignedBB(0.031, 0, 0.031, 0.969, 0.925, 0.969), null)));
    initTabBlock(new BlockUtilityBox("air_release_vault", new UtilityBoxSpec(1, 1, 1, new AxisAlignedBB(0, 0, 0, 1, 0.85, 1), null)));
    initTabBlock(new BlockUtilityBox("backflow_enclosure", new UtilityBoxSpec(2, 1, 1, new AxisAlignedBB(-0.969, 0, 0.062, 0.969, 0.963, 0.938), null)));
    initTabBlock(new BlockUtilityBox("chemical_feed_skid", new UtilityBoxSpec(2, 1, 2, new AxisAlignedBB(-0.969, 0, 0.062, 0.969, 1.625, 0.963), null)));
    initTabBlock(new BlockUtilityBox("chlorine_cylinder_scale", new UtilityBoxSpec(1, 1, 2, new AxisAlignedBB(0.031, 0, 0.062, 0.969, 1.613, 0.938), null)));

    // --- Sewer and stormwater (gen_utilities_sewer.py --fragments) ---
    initTabBlock(new BlockAccessHatch("wet_well_hatch", true));
    initTabBlock(new BlockAccessHatch("valve_vault_hatch", false));
    initTabBlock(new BlockSwitchedUnit("lift_station_control_panel", new UtilityBoxSpec(1, 1, 2, new AxisAlignedBB(0.031, 0, 0.325, 0.969, 1.75, 0.938), null), false, 12));
    initTabBlock(new BlockSwitchedUnit("lift_station_generator", new UtilityBoxSpec(2, 1, 2, new AxisAlignedBB(-0.975, 0, 0.075, 0.975, 1.95, 0.925), null), true, 0));
    initTabBlock(new BlockPrecastRun("curb_inlet", new double[]{0, 0, 0, 16, 16, 16}));
    initTabBlock(new BlockHeadwall("outfall_headwall", new double[]{0, 0, 9.4, 16, 17.2, 16}));
    initTabBlock(new BlockHeadwall("outfall_headwall_pipe", new double[]{0, 0, 8, 16, 17.2, 16}));
    initTabBlock(new BlockHeadwall("outfall_headwall_flap_gate", new double[]{0, 0, 7.1, 16, 17.2, 16}));
    initTabBlock(new BlockWingwall("outfall_wingwall", 12.429, 6.714));
    initTabBlock(new BlockRiprap("riprap", 5));
    initTabBlock(new BlockStackedSection("outlet_riser", "outlet", false, false));
    initTabBlock(new BlockPrecastRun("emergency_spillway", new double[]{0, 0, 0, 16, 4.2, 16}));
    initTabBlock(new BlockStackedSection("manhole_riser", "manhole", true, false));
    initTabBlock(new BlockStackedSection("manhole_riser_cutaway", "manhole", true, true));
    initTabBlock(new BlockManholeCone("manhole_cone", false));
    initTabBlock(new BlockManholeCone("manhole_cone_cutaway", true));

    // --- Gas yard and telecom (gen_utilities_gas_telecom.py --fragments) ---
    initTabBlock(new BlockWaterPipe("gas_pipe", BlockWaterPipe.GAS, 3.7));
    initTabBlock(new BlockPipeFitting("gas_ball_valve", new double[]{3.4, 3.4, 0, 12.6, 15.3, 16}, BlockWaterPipe.GAS));
    initTabBlock(new BlockPipeFitting("gas_pressure_regulator", new double[]{1.9, 3.4, 0, 14.1, 16, 16}, BlockWaterPipe.GAS));
    initTabBlock(new BlockPipeFitting("gas_turbine_meter", new double[]{3.1, 3.1, 0, 12.9, 16, 16}, BlockWaterPipe.GAS));
    initTabBlock(new BlockVentStack("gas_vent_stack", new double[]{4.2, 0, 4.2, 11.8, 14.4, 12.4}));
    initTabBlock(new BlockEquipmentSkid("gas_station_skid"));
    initTabBlock(new BlockPumpUnit("gas_line_heater", new UtilityBoxSpec(2, 1, 2, new AxisAlignedBB(-0.994, 0, -0.001, 0.775, 1.951, 1.001), null), BlockPumpUnit.Nozzles.FRONT_BACK, BlockWaterPipe.GAS));
    initTabBlock(new BlockUtilityBox("gas_odorant_tank", new UtilityBoxSpec(2, 1, 2, new AxisAlignedBB(-0.969, 0, 0.062, 0.969, 1.101, 0.938), null)));
    initTabBlock(new BlockUtilitySign("gas_sign_warning", new double[]{2, 5, 15.4, 14, 13, 16}));
    initTabBlock(new BlockUtilitySign("gas_sign_no_smoking", new double[]{2, 5, 15.4, 14, 13, 16}));
    initTabBlock(new BlockUtilitySign("gas_sign_emergency", new double[]{2, 5, 15.4, 14, 13, 16}));
    initTabBlock(new BlockUtilitySign("gas_sign_station", new double[]{2, 5, 15.4, 14, 13, 16}));
    initTabBlock(new BlockUtilitySign("gas_sign_authorized", new double[]{2, 5, 15.4, 14, 13, 16}));
    initTabBlock(new BlockUtilitySign("cell_site_sign", new double[]{2, 5, 15.4, 14, 13, 16}));
    initTabBlock(new BlockCabinet("fiber_distribution_cabinet", new UtilityBoxSpec(2, 1, 2, new AxisAlignedBB(-0.969, 0, 0.062, 0.969, 1.438, 0.938), null)));
    initTabBlock(new BlockUtilityBox("cell_equipment_cabinet", new UtilityBoxSpec(1, 1, 2, new AxisAlignedBB(0.031, 0, 0.031, 0.969, 1.812, 0.975), null)));
    initTabBlock(new BlockUtilityBox("cell_battery_cabinet", new UtilityBoxSpec(1, 1, 2, new AxisAlignedBB(0.031, 0, 0.031, 0.969, 1.812, 0.975), null)));
    initTabBlock(new BlockIceBridge("ice_bridge"));
    initTabBlock(new BlockTowerColumn("ice_bridge_stanchion", 1.4));
    initTabBlock(new BlockUtilityFixture("gps_antenna", new double[]{6.1, 2, 8.1, 9.9, 12.71, 16}));
    initTabBlock(new BlockTowerColumn("monopole_section", 6));
    initTabBlock(new BlockAntennaArray("monopole_antenna_array", new double[]{2, 0, 2, 14, 16, 14}));
    initTabBlock(new BlockSmallCell("small_cell_antenna", new double[]{3.4, 0, 3.4, 12.6, 16, 12.6}));
    initTabBlock(new BlockPoleRadio("small_cell_radio", new double[]{4.2, 1.8, 6.2, 11.8, 14, 11.4}));
  }
}

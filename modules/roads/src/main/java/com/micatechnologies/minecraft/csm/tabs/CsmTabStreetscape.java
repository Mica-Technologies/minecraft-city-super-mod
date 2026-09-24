package com.micatechnologies.minecraft.csm.tabs;

import com.micatechnologies.minecraft.csm.CsmRegistry;
import com.micatechnologies.minecraft.csm.codeutils.CsmTab;
import com.micatechnologies.minecraft.csm.streetscape.BlockBollardFlexible;
import com.micatechnologies.minecraft.csm.streetscape.BlockFireHydrant;
import com.micatechnologies.minecraft.csm.streetscape.BlockMailbox;
import com.micatechnologies.minecraft.csm.streetscape.BlockMailboxCurbside;
import com.micatechnologies.minecraft.csm.streetscape.BlockNewsRack;
import com.micatechnologies.minecraft.csm.streetscape.BlockParkingMeter;
import com.micatechnologies.minecraft.csm.streetscape.BlockStreetCover;
import com.micatechnologies.minecraft.csm.streetscape.BlockUtilityBox;
import com.micatechnologies.minecraft.csm.streetscape.BlockUtilityBoxLabelled;
import com.micatechnologies.minecraft.csm.streetscape.UtilityBoxSpec;
import com.micatechnologies.minecraft.csm.trafficaccessories.BlockWorkZoneDeviceDiagonal;
import net.minecraft.block.Block;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;

/**
 * The tab for the street fixtures between the curb and the building line: hydrants, delineators,
 * and the covers, bollards, mailboxes and parking meters that join them.
 *
 * <p>Curbs, sidewalks, tactile paving and the common bollards are left to external road mods,
 * which already supply them; every block here settles onto their sloped and partial-height
 * surfaces.</p>
 *
 * @version 1.0
 */
@CsmTab.Load(order = 11)
public class CsmTabStreetscape extends CsmTab {

  /**
   * Gets the ID (unique identifier) of the tab.
   *
   * @return the ID of the tab
   */
  @Override
  public String getTabId() {
    return "tabstreetscape";
  }

  /**
   * Gets the block to use as the icon of the tab
   *
   * @return the block to use as the icon of the tab
   */
  @Override
  public Block getTabIcon() {
    return CsmRegistry.getBlock("firehydrant");
  }

  /**
   * Gets a boolean indicating if the tab is searchable (has its own search bar).
   *
   * @return {@code true} if the tab is searchable, otherwise {@code false}
   */
  @Override
  public boolean getTabSearchable() {
    return false;
  }

  /**
   * Gets a boolean indicating if the tab is hidden (not displayed in the inventory).
   *
   * @return {@code true} if the tab is hidden, otherwise {@code false}
   */
  @Override
  public boolean getTabHidden() {
    return false;
  }

  /**
   * Initializes all the elements belonging to the tab.
   *
   * @param fmlPreInitializationEvent the {@link FMLPreInitializationEvent} that is being processed
   */
  @Override
  public void initTabElements(FMLPreInitializationEvent fmlPreInitializationEvent) {
    initTabBlock(BlockFireHydrant.class, fmlPreInitializationEvent); // Fire Hydrant
    // Hydrants and standpipes (gen_streetscape_street_furniture.py --fragments).
    initTabBlock(new BlockUtilityBox("hydrant_yellow_blue_cap", new UtilityBoxSpec(1, 1, 1,
        new AxisAlignedBB(0.137500, 0.000000, 0.137500, 0.862500, 0.812500, 0.750000),
        null)));
    initTabBlock(new BlockUtilityBox("hydrant_yellow_green_cap", new UtilityBoxSpec(1, 1, 1,
        new AxisAlignedBB(0.137500, 0.000000, 0.137500, 0.862500, 0.812500, 0.750000),
        null)));
    initTabBlock(new BlockUtilityBox("hydrant_yellow_orange_cap", new UtilityBoxSpec(1, 1, 1,
        new AxisAlignedBB(0.137500, 0.000000, 0.137500, 0.862500, 0.812500, 0.750000),
        null)));
    initTabBlock(new BlockUtilityBox("hydrant_yellow_red_cap", new UtilityBoxSpec(1, 1, 1,
        new AxisAlignedBB(0.137500, 0.000000, 0.137500, 0.862500, 0.812500, 0.750000),
        null)));
    initTabBlock(new BlockUtilityBox("hydrant_red_white_cap", new UtilityBoxSpec(1, 1, 1,
        new AxisAlignedBB(0.137500, 0.000000, 0.137500, 0.862500, 0.812500, 0.750000),
        null)));
    initTabBlock(new BlockUtilityBox("hydrant_red_silver_cap", new UtilityBoxSpec(1, 1, 1,
        new AxisAlignedBB(0.137500, 0.000000, 0.137500, 0.862500, 0.812500, 0.750000),
        null)));
    initTabBlock(new BlockUtilityBox("hydrant_wall", new UtilityBoxSpec(1, 1, 1,
        new AxisAlignedBB(0.281250, 0.281250, 0.636750, 0.718750, 0.718750, 1.000000),
        null)));
    initTabBlock(new BlockUtilityBox("standpipe_sidewalk", new UtilityBoxSpec(1, 1, 1,
        new AxisAlignedBB(0.259358, 0.000000, 0.286750, 0.740642, 0.712511, 0.600011),
        null)));

    // Delineators, moved here from Traffic Accessories.
    initTabBlock(new BlockWorkZoneDeviceDiagonal("delineator_post",
        new AxisAlignedBB(0.356250, 0.000000, 0.356250, 0.643750, 0.937500, 0.643750)));
    initTabBlock(new BlockWorkZoneDeviceDiagonal("delineator_post_yellow",
        new AxisAlignedBB(0.356250, 0.000000, 0.356250, 0.643750, 0.937500, 0.643750)));
    initTabBlock(new BlockWorkZoneDeviceDiagonal("delineator_zebra",
        new AxisAlignedBB(0.068750, 0.000000, 0.340625, 0.931250, 0.190625, 0.659375)));

    // Bollards (gen_streetscape_bollards.py --fragments).
    initTabBlock(new BlockUtilityBox("bollard_cast_iron_black", new UtilityBoxSpec(1, 1, 1,
        new AxisAlignedBB(0.337498, 0.000000, 0.337498, 0.662502, 0.925750, 0.662502),
        null)));
    initTabBlock(new BlockUtilityBox("bollard_cast_iron_green", new UtilityBoxSpec(1, 1, 1,
        new AxisAlignedBB(0.337498, 0.000000, 0.337498, 0.662502, 0.925750, 0.662502),
        null)));
    initTabBlock(new BlockUtilityBox("bollard_stainless", new UtilityBoxSpec(1, 1, 1,
        new AxisAlignedBB(0.356236, 0.000000, 0.356236, 0.643764, 0.907000, 0.643764),
        null)));
    initTabBlock(new BlockUtilityBox("bollard_crash_rated", new UtilityBoxSpec(1, 1, 1,
        new AxisAlignedBB(0.187500, 0.000000, 0.187500, 0.812500, 0.969500, 0.812500),
        null)));
    initTabBlock(new BlockUtilityBox("bollard_pipe_sleeve", new UtilityBoxSpec(1, 1, 1,
        new AxisAlignedBB(0.321853, 0.000000, 0.321853, 0.678147, 0.938250, 0.678147),
        null)));
    initTabBlock(new BlockBollardFlexible("bollard_flexible_white", new UtilityBoxSpec(1, 1, 1,
        new AxisAlignedBB(0.362500, 0.000000, 0.362500, 0.637500, 0.957000, 0.637500),
        null)));
    initTabBlock(new BlockBollardFlexible("bollard_flexible_yellow", new UtilityBoxSpec(1, 1, 1,
        new AxisAlignedBB(0.362500, 0.000000, 0.362500, 0.637500, 0.957000, 0.637500),
        null)));
    initTabBlock(new BlockUtilityBox("bollard_sphere_red", new UtilityBoxSpec(1, 1, 1,
        new AxisAlignedBB(0.075000, 0.000000, 0.075000, 0.925000, 0.837500, 0.925000),
        null)));

    // Covers (gen_streetscape_covers.py --fragments).
    initTabBlock(new BlockStreetCover("manhole_sewer",
        new AxisAlignedBB(0.125000, 0.000000, 0.125000, 0.875000, 0.062500, 0.875000)));
    initTabBlock(new BlockStreetCover("manhole_sewer_rusted",
        new AxisAlignedBB(0.125000, 0.000000, 0.125000, 0.875000, 0.062500, 0.875000)));
    initTabBlock(new BlockStreetCover("manhole_storm",
        new AxisAlignedBB(0.125000, 0.000000, 0.125000, 0.875000, 0.062500, 0.875000)));
    initTabBlock(new BlockStreetCover("manhole_storm_rusted",
        new AxisAlignedBB(0.125000, 0.000000, 0.125000, 0.875000, 0.062500, 0.875000)));
    initTabBlock(new BlockStreetCover("manhole_water",
        new AxisAlignedBB(0.125000, 0.000000, 0.125000, 0.875000, 0.062500, 0.875000)));
    initTabBlock(new BlockStreetCover("manhole_water_rusted",
        new AxisAlignedBB(0.125000, 0.000000, 0.125000, 0.875000, 0.062500, 0.875000)));
    initTabBlock(new BlockStreetCover("manhole_electric",
        new AxisAlignedBB(0.125000, 0.000000, 0.125000, 0.875000, 0.062500, 0.875000)));
    initTabBlock(new BlockStreetCover("manhole_electric_rusted",
        new AxisAlignedBB(0.125000, 0.000000, 0.125000, 0.875000, 0.062500, 0.875000)));
    initTabBlock(new BlockStreetCover("manhole_telecom",
        new AxisAlignedBB(0.125000, 0.000000, 0.125000, 0.875000, 0.062500, 0.875000)));
    initTabBlock(new BlockStreetCover("manhole_telecom_rusted",
        new AxisAlignedBB(0.125000, 0.000000, 0.125000, 0.875000, 0.062500, 0.875000)));
    initTabBlock(new BlockStreetCover("manhole_gas",
        new AxisAlignedBB(0.125000, 0.000000, 0.125000, 0.875000, 0.062500, 0.875000)));
    initTabBlock(new BlockStreetCover("manhole_gas_rusted",
        new AxisAlignedBB(0.125000, 0.000000, 0.125000, 0.875000, 0.062500, 0.875000)));
    initTabBlock(new BlockStreetCover("manhole_plain",
        new AxisAlignedBB(0.125000, 0.000000, 0.125000, 0.875000, 0.062500, 0.875000)));
    initTabBlock(new BlockStreetCover("manhole_plain_rusted",
        new AxisAlignedBB(0.125000, 0.000000, 0.125000, 0.875000, 0.062500, 0.875000)));
    initTabBlock(new BlockStreetCover("vault_lid_electric",
        new AxisAlignedBB(0.140625, 0.000000, 0.281250, 0.859375, 0.062500, 0.718750)));
    initTabBlock(new BlockStreetCover("vault_lid_electric_rusted",
        new AxisAlignedBB(0.140625, 0.000000, 0.281250, 0.859375, 0.062500, 0.718750)));
    initTabBlock(new BlockStreetCover("vault_lid_traffic_signal",
        new AxisAlignedBB(0.109375, 0.000000, 0.234375, 0.890625, 0.062500, 0.765625)));
    initTabBlock(new BlockStreetCover("vault_lid_traffic_signal_rusted",
        new AxisAlignedBB(0.109375, 0.000000, 0.234375, 0.890625, 0.062500, 0.765625)));
    initTabBlock(new BlockStreetCover("vault_lid_fiber_optic",
        new AxisAlignedBB(0.078125, 0.000000, 0.171875, 0.921875, 0.062500, 0.828125)));
    initTabBlock(new BlockStreetCover("vault_lid_water_meter",
        new AxisAlignedBB(0.109375, 0.000000, 0.234375, 0.890625, 0.062500, 0.765625)));
    initTabBlock(new BlockStreetCover("vault_lid_water_meter_rusted",
        new AxisAlignedBB(0.109375, 0.000000, 0.234375, 0.890625, 0.062500, 0.765625)));
    initTabBlock(new BlockStreetCover("valve_box_water",
        new AxisAlignedBB(0.312500, 0.000000, 0.312500, 0.687500, 0.062500, 0.687500)));
    initTabBlock(new BlockStreetCover("valve_box_water_rusted",
        new AxisAlignedBB(0.312500, 0.000000, 0.312500, 0.687500, 0.062500, 0.687500)));
    initTabBlock(new BlockStreetCover("valve_box_gas",
        new AxisAlignedBB(0.312500, 0.000000, 0.312500, 0.687500, 0.062500, 0.687500)));
    initTabBlock(new BlockStreetCover("valve_box_gas_rusted",
        new AxisAlignedBB(0.312500, 0.000000, 0.312500, 0.687500, 0.062500, 0.687500)));
    initTabBlock(new BlockStreetCover("sewer_cleanout",
        new AxisAlignedBB(0.343750, 0.000000, 0.343750, 0.656250, 0.062500, 0.656250)));
    initTabBlock(new BlockStreetCover("sewer_cleanout_rusted",
        new AxisAlignedBB(0.343750, 0.000000, 0.343750, 0.656250, 0.062500, 0.656250)));
    initTabBlock(new BlockStreetCover("catch_basin_grate",
        new AxisAlignedBB(0.187500, 0.000000, 0.187500, 0.812500, 0.062500, 0.812500)));
    initTabBlock(new BlockStreetCover("catch_basin_grate_rusted",
        new AxisAlignedBB(0.187500, 0.000000, 0.187500, 0.812500, 0.062500, 0.812500)));
    initTabBlock(new BlockStreetCover("trench_drain",
        new AxisAlignedBB(0.000000, 0.000000, 0.343750, 1.000000, 0.062500, 0.656250)));
    initTabBlock(new BlockStreetCover("trench_drain_rusted",
        new AxisAlignedBB(0.000000, 0.000000, 0.343750, 1.000000, 0.062500, 0.656250)));
    initTabBlock(new BlockStreetCover("gutter_inlet_grate",
        new AxisAlignedBB(0.062500, 0.000000, 0.562500, 0.937500, 0.062500, 0.968750)));
    initTabBlock(new BlockStreetCover("gutter_inlet_grate_rusted",
        new AxisAlignedBB(0.062500, 0.000000, 0.562500, 0.937500, 0.062500, 0.968750)));
    initTabBlock(new BlockStreetCover("storm_drain_marker",
        new AxisAlignedBB(0.171875, 0.000000, 0.171875, 0.828125, 0.062500, 0.828125)));

    // Utility boxes (gen_streetscape_utility.py --fragments).
    initTabBlock(new BlockUtilityBoxLabelled("transformer_padmount_small",
        new UtilityBoxSpec(1, 1, 1,
        new AxisAlignedBB(0.018750, 0.000000, 0.062500, 0.981250, 0.781250, 0.981250),
        new UtilityBoxSpec.Label(8.0f, 8.4f, 2.5f, 2, 1.1f, false))));
    initTabBlock(new BlockUtilityBoxLabelled("transformer_padmount_small_rusted",
        new UtilityBoxSpec(1, 1, 1,
        new AxisAlignedBB(0.018750, 0.000000, 0.062500, 0.981250, 0.781250, 0.981250),
        new UtilityBoxSpec.Label(8.0f, 8.4f, 2.5f, 2, 1.1f, false))));
    initTabBlock(new BlockUtilityBoxLabelled("transformer_padmount_medium",
        new UtilityBoxSpec(2, 1, 1,
        new AxisAlignedBB(-0.762500, 0.000000, 0.031250, 0.762500, 0.843750, 0.981250),
        new UtilityBoxSpec.Label(5.0f, 9.0f, 2.0f, 2, 1.4f, false))));
    initTabBlock(new BlockUtilityBoxLabelled("transformer_padmount_medium_rusted",
        new UtilityBoxSpec(2, 1, 1,
        new AxisAlignedBB(-0.762500, 0.000000, 0.031250, 0.762500, 0.843750, 0.981250),
        new UtilityBoxSpec.Label(5.0f, 9.0f, 2.0f, 2, 1.4f, false))));
    initTabBlock(new BlockUtilityBoxLabelled("transformer_padmount_large",
        new UtilityBoxSpec(2, 2, 1,
        new AxisAlignedBB(-0.825000, 0.000000, 0.031250, 0.825000, 0.968750, 1.575000),
        new UtilityBoxSpec.Label(5.0f, 10.6f, 2.0f, 2, 1.6f, false))));
    initTabBlock(new BlockUtilityBoxLabelled("transformer_padmount_large_rusted",
        new UtilityBoxSpec(2, 2, 1,
        new AxisAlignedBB(-0.825000, 0.000000, 0.031250, 0.825000, 0.968750, 1.575000),
        new UtilityBoxSpec.Label(5.0f, 10.6f, 2.0f, 2, 1.6f, false))));
    initTabBlock(new BlockUtilityBoxLabelled("transformer_padmount_three_phase",
        new UtilityBoxSpec(2, 2, 2,
        new AxisAlignedBB(-0.937500, 0.000000, 0.000000, 0.937500, 1.725000, 1.687500),
        new UtilityBoxSpec.Label(-6.5f, 23.5f, 2.0f, 1, 2.2f, false))));
    initTabBlock(new BlockUtilityBoxLabelled("transformer_padmount_three_phase_rusted",
        new UtilityBoxSpec(2, 2, 2,
        new AxisAlignedBB(-0.937500, 0.000000, 0.000000, 0.937500, 1.725000, 1.687500),
        new UtilityBoxSpec.Label(-6.5f, 23.5f, 2.0f, 1, 2.2f, false))));
    initTabBlock(new BlockUtilityBox("utility_pedestal_square_tall", new UtilityBoxSpec(1, 1, 2,
        new AxisAlignedBB(0.262500, 0.000000, 0.262500, 0.737500, 1.262500, 0.737500),
        null)));
    initTabBlock(new BlockUtilityBox("utility_pedestal_square_tall_rusted",
        new UtilityBoxSpec(1, 1, 2,
        new AxisAlignedBB(0.262500, 0.000000, 0.262500, 0.737500, 1.262500, 0.737500),
        null)));
    initTabBlock(new BlockUtilityBox("utility_pedestal_square_short", new UtilityBoxSpec(1, 1, 1,
        new AxisAlignedBB(0.262500, 0.000000, 0.262500, 0.737500, 0.825000, 0.737500),
        null)));
    initTabBlock(new BlockUtilityBox("utility_pedestal_square_short_rusted",
        new UtilityBoxSpec(1, 1, 1,
        new AxisAlignedBB(0.262500, 0.000000, 0.262500, 0.737500, 0.825000, 0.737500),
        null)));
    initTabBlock(new BlockUtilityBox("utility_pedestal_round_tall", new UtilityBoxSpec(1, 1, 2,
        new AxisAlignedBB(0.287500, 0.000000, 0.287500, 0.712500, 1.157000, 0.712500),
        null)));
    initTabBlock(new BlockUtilityBox("utility_pedestal_round_tall_rusted",
        new UtilityBoxSpec(1, 1, 2,
        new AxisAlignedBB(0.287500, 0.000000, 0.287500, 0.712500, 1.157000, 0.712500),
        null)));
    initTabBlock(new BlockUtilityBox("utility_pedestal_round_short", new UtilityBoxSpec(1, 1, 1,
        new AxisAlignedBB(0.287500, 0.000000, 0.287500, 0.712500, 0.782000, 0.712500),
        null)));
    initTabBlock(new BlockUtilityBox("utility_pedestal_round_short_rusted",
        new UtilityBoxSpec(1, 1, 1,
        new AxisAlignedBB(0.287500, 0.000000, 0.287500, 0.712500, 0.782000, 0.712500),
        null)));
    initTabBlock(new BlockUtilityBoxLabelled("telecom_pedestal_ribbed", new UtilityBoxSpec(1, 1, 2,
        new AxisAlignedBB(0.250000, 0.000000, 0.212500, 0.750000, 1.125000, 0.750000),
        new UtilityBoxSpec.Label(10.3f, 14.0f, 4.0f, 1, 1.2f, true))));
    initTabBlock(new BlockUtilityBox("telecom_enclosure_low", new UtilityBoxSpec(1, 1, 1,
        new AxisAlignedBB(0.062500, 0.000000, 0.281250, 0.937500, 0.531250, 0.781250),
        null)));
    initTabBlock(new BlockUtilityBox("utility_marker_electric", new UtilityBoxSpec(1, 1, 2,
        new AxisAlignedBB(0.456250, 0.000000, 0.475000, 0.543750, 1.250000, 0.525000),
        null)));
    initTabBlock(new BlockUtilityBox("utility_marker_gas", new UtilityBoxSpec(1, 1, 2,
        new AxisAlignedBB(0.456250, 0.000000, 0.475000, 0.543750, 1.250000, 0.525000),
        null)));
    initTabBlock(new BlockUtilityBox("utility_marker_telecom", new UtilityBoxSpec(1, 1, 2,
        new AxisAlignedBB(0.456250, 0.000000, 0.475000, 0.543750, 1.250000, 0.525000),
        null)));

    // Parking meters (gen_streetscape_meters.py --fragments).
    initTabBlock(new BlockParkingMeter("parking_meter_mechanical", new UtilityBoxSpec(1, 1, 2,
        new AxisAlignedBB(0.350000, 0.000000, 0.356250, 0.650000, 1.306250, 0.612500),
        null),
        BlockParkingMeter.Kind.MECHANICAL, new float[][]{{8f, 17.6f, 6.15f, 3.2f, 2.6f}}));
    initTabBlock(new BlockParkingMeter("parking_meter_mechanical_double",
        new UtilityBoxSpec(1, 1, 2,
        new AxisAlignedBB(0.162500, 0.000000, 0.356250, 0.837500, 1.331250, 0.612500),
        null),
        BlockParkingMeter.Kind.MECHANICAL, new float[][]{
            {11.0f, 18.0f, 6.15f, 3.0f, 2.6f},
            {5.0f, 18.0f, 6.15f, 3.0f, 2.6f}}));
    initTabBlock(new BlockParkingMeter("parking_meter_digital", new UtilityBoxSpec(1, 1, 2,
        new AxisAlignedBB(0.343750, 0.000000, 0.371875, 0.656250, 1.412500, 0.625000),
        null),
        BlockParkingMeter.Kind.DIGITAL, new float[][]{{8f, 20.2f, 5.95f, 4.0f, 1.8f}}));
    initTabBlock(new BlockParkingMeter("parking_meter_digital_double", new UtilityBoxSpec(1, 1, 2,
        new AxisAlignedBB(0.162500, 0.000000, 0.371875, 0.837500, 1.437500, 0.625000),
        null),
        BlockParkingMeter.Kind.DIGITAL, new float[][]{
            {11.0f, 20.6f, 5.95f, 3.6f, 1.8f},
            {5.0f, 20.6f, 5.95f, 3.6f, 1.8f}}));
    initTabBlock(new BlockParkingMeter("parking_pay_station", new UtilityBoxSpec(1, 1, 2,
        new AxisAlignedBB(0.218750, 0.000000, 0.281250, 0.781250, 1.587500, 0.718750),
        null),
        BlockParkingMeter.Kind.STATION, new float[][]{{8f, 18.8f, 4.95f, 5.8f, 3.6f}}));
    initTabBlock(new BlockUtilityBoxLabelled("parking_pay_by_phone_sign",
        new UtilityBoxSpec(1, 1, 2,
        new AxisAlignedBB(0.218750, 0.000000, 0.462500, 0.781250, 1.625000, 0.562500),
        new UtilityBoxSpec.Label(8.0f, 16.3f, 7.4f, 1, 1.3f, false,
            0xF0F0EC, 0x16683E))));

    // News racks (gen_streetscape_street_furniture.py --fragments).
    initTabBlock(new BlockNewsRack("news_rack_blue", new UtilityBoxSpec(1, 1, 2,
        new AxisAlignedBB(0.000000, 0.000000, 0.162500, 1.000000, 1.500000, 0.837500),
        null)));
    initTabBlock(new BlockNewsRack("news_rack_red", new UtilityBoxSpec(1, 1, 2,
        new AxisAlignedBB(0.000000, 0.000000, 0.162500, 1.000000, 1.500000, 0.837500),
        null)));
    initTabBlock(new BlockNewsRack("news_rack_green", new UtilityBoxSpec(1, 1, 2,
        new AxisAlignedBB(0.000000, 0.000000, 0.162500, 1.000000, 1.500000, 0.837500),
        null)));
    initTabBlock(new BlockNewsRack("news_rack_yellow", new UtilityBoxSpec(1, 1, 2,
        new AxisAlignedBB(0.000000, 0.000000, 0.162500, 1.000000, 1.500000, 0.837500),
        null)));
    initTabBlock(new BlockNewsRack("news_rack_free", new UtilityBoxSpec(1, 1, 2,
        new AxisAlignedBB(0.000000, 0.000000, 0.162500, 1.000000, 1.500000, 0.837500),
        null)));

    // Mailboxes (gen_streetscape_mailboxes.py --fragments).
    initTabBlock(new BlockMailbox("mailbox_collection_blue", new UtilityBoxSpec(1, 1, 2,
        new AxisAlignedBB(0.187500, 0.000000, 0.112500, 0.812500, 1.562500, 0.812500),
        null),
        new float[][]{{3f, 3f, 13f, 20f}},
        new int[]{27},
        new String[]{"Mailbox"}));
    initTabBlock(new BlockMailbox("mailbox_cluster_gray", new UtilityBoxSpec(2, 1, 2,
        new AxisAlignedBB(-0.781250, 0.000000, 0.156250, 0.781250, 1.937500, 0.843750),
        null),
        new float[][]{{5.75f, 25.02f, 10.81f, 28.87f}, {0.23f, 25.02f, 5.29f, 28.87f},
            {-5.29f, 25.02f, -0.23f, 28.87f}, {-10.81f, 25.02f, -5.75f, 28.87f},
            {5.75f, 20.75f, 10.81f, 24.6f}, {0.23f, 20.75f, 5.29f, 24.6f},
            {-5.29f, 20.75f, -0.23f, 24.6f}, {-10.81f, 20.75f, -5.75f, 24.6f},
            {5.75f, 16.48f, 10.81f, 20.33f}, {0.23f, 16.48f, 5.29f, 20.33f},
            {-5.29f, 16.48f, -0.23f, 20.33f}, {-10.81f, 16.48f, -5.75f, 20.33f},
            {0.23f, 9.13f, 10.81f, 15.85f}, {-10.81f, 9.13f, -0.23f, 15.85f}},
        new int[]{9, 9, 9, 9, 9, 9, 9, 9, 9, 9, 9, 9, 27, 27},
        new String[]{"Box 1", "Box 2", "Box 3", "Box 4", "Box 5", "Box 6", "Box 7", "Box 8",
            "Box 9", "Box 10", "Box 11", "Box 12", "Parcel Locker 1", "Parcel Locker 2"}));
    initTabBlock(new BlockMailbox("mailbox_cluster_bronze", new UtilityBoxSpec(2, 1, 2,
        new AxisAlignedBB(-0.781250, 0.000000, 0.156250, 0.781250, 1.937500, 0.843750),
        null),
        new float[][]{{5.75f, 25.02f, 10.81f, 28.87f}, {0.23f, 25.02f, 5.29f, 28.87f},
            {-5.29f, 25.02f, -0.23f, 28.87f}, {-10.81f, 25.02f, -5.75f, 28.87f},
            {5.75f, 20.75f, 10.81f, 24.6f}, {0.23f, 20.75f, 5.29f, 24.6f},
            {-5.29f, 20.75f, -0.23f, 24.6f}, {-10.81f, 20.75f, -5.75f, 24.6f},
            {5.75f, 16.48f, 10.81f, 20.33f}, {0.23f, 16.48f, 5.29f, 20.33f},
            {-5.29f, 16.48f, -0.23f, 20.33f}, {-10.81f, 16.48f, -5.75f, 20.33f},
            {0.23f, 9.13f, 10.81f, 15.85f}, {-10.81f, 9.13f, -0.23f, 15.85f}},
        new int[]{9, 9, 9, 9, 9, 9, 9, 9, 9, 9, 9, 9, 27, 27},
        new String[]{"Box 1", "Box 2", "Box 3", "Box 4", "Box 5", "Box 6", "Box 7", "Box 8",
            "Box 9", "Box 10", "Box 11", "Box 12", "Parcel Locker 1", "Parcel Locker 2"}));
    initTabBlock(new BlockMailboxCurbside("mailbox_curbside_black", new UtilityBoxSpec(1, 1, 2,
        new AxisAlignedBB(0.262500, 0.000000, 0.075000, 0.687516, 1.500016, 0.875000),
        null),
        new float[][]{{5f, 17.5f, 11f, 24.0f}},
        new int[]{9},
        new String[]{"Mailbox"}));
    initTabBlock(new BlockMailboxCurbside("mailbox_curbside_green", new UtilityBoxSpec(1, 1, 2,
        new AxisAlignedBB(0.262500, 0.000000, 0.075000, 0.687516, 1.500016, 0.875000),
        null),
        new float[][]{{5f, 17.5f, 11f, 24.0f}},
        new int[]{9},
        new String[]{"Mailbox"}));
    initTabBlock(new BlockMailboxCurbside("mailbox_curbside_white", new UtilityBoxSpec(1, 1, 2,
        new AxisAlignedBB(0.262500, 0.000000, 0.075000, 0.687516, 1.500016, 0.875000),
        null),
        new float[][]{{5f, 17.5f, 11f, 24.0f}},
        new int[]{9},
        new String[]{"Mailbox"}));
    initTabBlock(new BlockMailbox("mailbox_wall_bank_aluminum", new UtilityBoxSpec(1, 1, 1,
        new AxisAlignedBB(0.062500, 0.093750, 0.778125, 0.937500, 0.937500, 1.000000),
        null),
        new float[][]{{8.26f, 10.54f, 14.07f, 14.09f}, {1.93f, 10.54f, 7.74f, 14.09f},
            {8.26f, 6.47f, 14.07f, 10.03f}, {1.93f, 6.47f, 7.74f, 10.03f},
            {8.26f, 2.41f, 14.07f, 5.96f}, {1.93f, 2.41f, 7.74f, 5.96f}},
        new int[]{9, 9, 9, 9, 9, 9},
        new String[]{"Box 1", "Box 2", "Box 3", "Box 4", "Box 5", "Box 6"}));
    initTabBlock(new BlockMailbox("mailbox_wall_bank_brass", new UtilityBoxSpec(1, 1, 1,
        new AxisAlignedBB(0.062500, 0.093750, 0.778125, 0.937500, 0.937500, 1.000000),
        null),
        new float[][]{{8.26f, 10.54f, 14.07f, 14.09f}, {1.93f, 10.54f, 7.74f, 14.09f},
            {8.26f, 6.47f, 14.07f, 10.03f}, {1.93f, 6.47f, 7.74f, 10.03f},
            {8.26f, 2.41f, 14.07f, 5.96f}, {1.93f, 2.41f, 7.74f, 5.96f}},
        new int[]{9, 9, 9, 9, 9, 9},
        new String[]{"Box 1", "Box 2", "Box 3", "Box 4", "Box 5", "Box 6"}));
  }
}

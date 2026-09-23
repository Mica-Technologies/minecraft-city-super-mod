package com.micatechnologies.minecraft.csm.tabs;

import com.micatechnologies.minecraft.csm.CsmRegistry;
import com.micatechnologies.minecraft.csm.codeutils.CsmTab;
import com.micatechnologies.minecraft.csm.streetscape.BlockFireHydrant;
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

    // Delineators, moved here from Traffic Accessories.
    initTabBlock(new BlockWorkZoneDeviceDiagonal("delineator_post",
        new AxisAlignedBB(0.356250, 0.000000, 0.356250, 0.643750, 0.937500, 0.643750)));
    initTabBlock(new BlockWorkZoneDeviceDiagonal("delineator_post_yellow",
        new AxisAlignedBB(0.356250, 0.000000, 0.356250, 0.643750, 0.937500, 0.643750)));
    initTabBlock(new BlockWorkZoneDeviceDiagonal("delineator_zebra",
        new AxisAlignedBB(0.068750, 0.000000, 0.340625, 0.931250, 0.190625, 0.659375)));

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
  }
}

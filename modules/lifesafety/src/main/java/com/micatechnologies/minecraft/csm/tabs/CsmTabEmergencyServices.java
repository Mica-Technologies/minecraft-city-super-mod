package com.micatechnologies.minecraft.csm.tabs;

import com.micatechnologies.minecraft.csm.CsmRegistry;
import com.micatechnologies.minecraft.csm.codeutils.CsmTab;
import com.micatechnologies.minecraft.csm.lifesafety.fireprotection.BlockFireProtectionProp;
import com.micatechnologies.minecraft.csm.lifesafety.stations.BlockCellDoor;
import com.micatechnologies.minecraft.csm.lifesafety.stations.BlockFirePole;
import com.micatechnologies.minecraft.csm.lifesafety.stations.BlockFirePoleHole;
import com.micatechnologies.minecraft.csm.lifesafety.stations.BlockLitProp;
import com.micatechnologies.minecraft.csm.lifesafety.stations.BlockMetalDetector;
import com.micatechnologies.minecraft.csm.lifesafety.stations.BlockSceneTape;
import com.micatechnologies.minecraft.csm.lifesafety.stations.BlockStationAlertController;
import com.micatechnologies.minecraft.csm.lifesafety.stations.BlockStationAlertDevice;
import com.micatechnologies.minecraft.csm.lifesafety.stations.BlockStationBell;
import com.micatechnologies.minecraft.csm.lifesafety.stations.BlockStationNumberPlaque;
import com.micatechnologies.minecraft.csm.lifesafety.stations.BlockTapeStanchion;
import net.minecraft.block.Block;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;

/**
 * The Emergency Services tab of the Life Safety module: what goes in a fire station, a police
 * station and an ambulance station, and the community warning and dispatch equipment around them.
 * No vehicles. Written by {@code gen_emergency_services.py} ({@code --fragments}).
 *
 * @since 2026.9
 */
@CsmTab.Load(order = 22)
public class CsmTabEmergencyServices extends CsmTab {

  @Override
  public String getTabId() {
    return "tabemergencyservices";
  }

  @Override
  public Block getTabIcon() {
    return CsmRegistry.getBlock("maltese_cross_emblem");
  }

  @Override
  public boolean getTabSearchable() {
    return true;
  }

  @Override
  public boolean getTabHidden() {
    return false;
  }

  @Override
  public void initTabElements(FMLPreInitializationEvent fmlPreInitializationEvent) {
    // --- Emergency Services (gen_emergency_services.py --fragments) ---
    initTabBlock(new BlockFireProtectionProp("turnout_gear_locker", new int[]{0, 0, 2, 16, 16, 16}, true));
    initTabBlock(new BlockFireProtectionProp("turnout_gear_locker_empty", new int[]{0, 0, 2, 16, 16, 16}, true));
    initTabBlock(new BlockFireProtectionProp("scba_wall_rack", new int[]{1, 1, 9, 15, 15, 16}, true));
    initTabBlock(new BlockFireProtectionProp("scba_cylinder_cascade", new int[]{0, 0, 3, 16, 16, 13}, true));
    initTabBlock(new BlockFireProtectionProp("scba_fill_station", new int[]{1, 0, 3, 15, 16, 16}, true));
    initTabBlock(new BlockFireProtectionProp("hose_rack_wall", new int[]{2, 2, 9, 14, 14, 16}, false));
    initTabBlock(new BlockFireProtectionProp("hose_rolls", new int[]{1, 0, 3, 15, 9, 13}, true));
    initTabBlock(new BlockFireProtectionProp("hose_drying_rack", new int[]{0, 0, 5, 16, 16, 11}, true));
    initTabBlock(new BlockFireProtectionProp("nozzle_rack", new int[]{1, 3, 11, 15, 13, 16}, false));
    initTabBlock(new BlockFireProtectionProp("fire_tool_board", new int[]{0, 0, 13, 16, 16, 16}, false));
    initTabBlock(new BlockFireProtectionProp("gear_extractor", new int[]{1, 0, 2, 15, 15, 16}, true));
    initTabBlock(new BlockFireProtectionProp("air_compressor", new int[]{0, 0, 3, 16, 13, 13}, true));
    initTabBlock(new BlockStationBell("station_alarm_gong", new int[]{3, 3, 10, 13, 13, 16}));
    initTabBlock(new BlockFirePole());
    initTabBlock(new BlockFirePoleHole());
    initTabBlock(new BlockFireProtectionProp("maltese_cross_emblem", new int[]{1, 1, 15, 15, 15, 16}, false));
    initTabBlock(new BlockStationNumberPlaque("station_number_plaque", new int[]{2, 2, 15, 14, 14, 16}));
    initTabBlock(new BlockStationAlertController("station_alert_controller", new int[]{2, 1, 11, 14, 15, 16}));
    initTabBlock(new BlockStationAlertDevice("station_alert_speaker", BlockStationAlertDevice.Kind.SPEAKER, new int[]{3, 3, 13, 13, 13, 16}));
    initTabBlock(new BlockStationAlertDevice("station_alert_light_red", BlockStationAlertDevice.Kind.LIGHT, new int[]{2, 5, 12, 14, 11, 16}));
    initTabBlock(new BlockStationAlertDevice("station_alert_light_white", BlockStationAlertDevice.Kind.LIGHT, new int[]{2, 5, 12, 14, 11, 16}));
    initTabBlock(new BlockStationAlertDevice("station_alert_relay", BlockStationAlertDevice.Kind.RELAY, new int[]{5, 4, 13, 11, 12, 16}));
    initTabBlock(new BlockStationAlertDevice("bay_clearance_light", BlockStationAlertDevice.Kind.CLEARANCE, new int[]{4, 1, 12, 12, 15, 16}));
    initTabBlock(new BlockFireProtectionProp("front_desk_counter", new int[]{0, 0, 3, 16, 16, 16}, true));
    initTabBlock(new BlockFireProtectionProp("pass_through_tray", new int[]{3, 0, 2, 13, 3, 14}, false));
    initTabBlock(new BlockFireProtectionProp("lobby_phone", new int[]{5, 3, 12, 11, 13, 16}, false));
    initTabBlock(new BlockMetalDetector("metal_detector", new int[]{0, 0, 5, 16, 16, 11}));
    initTabBlock(new BlockCellDoor("holding_cell_door", new int[]{0, 0, 7, 16, 16, 9}));
    initTabBlock(new BlockFireProtectionProp("holding_cell_bench", new int[]{0, 5, 8, 16, 8, 16}, true));
    initTabBlock(new BlockFireProtectionProp("holding_cell_toilet", new int[]{3, 0, 8, 13, 15, 16}, true));
    initTabBlock(new BlockFireProtectionProp("height_chart", new int[]{0, 0, 15, 16, 16, 16}, false));
    initTabBlock(new BlockFireProtectionProp("fingerprint_scanner", new int[]{4, 0, 5, 12, 4, 12}, false));
    initTabBlock(new BlockFireProtectionProp("booking_camera", new int[]{5, 0, 5, 11, 16, 11}, true));
    initTabBlock(new BlockFireProtectionProp("property_bins", new int[]{0, 0, 6, 16, 16, 16}, true));
    initTabBlock(new BlockFireProtectionProp("evidence_locker", new int[]{0, 0, 2, 16, 16, 16}, true));
    initTabBlock(new BlockFireProtectionProp("equipment_locker", new int[]{0, 0, 3, 16, 16, 16}, true));
    initTabBlock(new BlockFireProtectionProp("k9_kennel", new int[]{0, 0, 0, 16, 12, 16}, true));
    initTabBlock(new BlockLitProp("police_lamp", new int[]{4, 2, 4, 12, 15, 16}, 14));
    initTabBlock(new BlockFireProtectionProp("police_star_emblem", new int[]{1, 1, 15, 15, 15, 16}, false));
    initTabBlock(new BlockSceneTape("police_line_tape"));
    initTabBlock(new BlockSceneTape("fire_line_tape"));
    initTabBlock(new BlockTapeStanchion("tape_stanchion", new int[]{5, 0, 5, 11, 14, 11}));
    initTabBlock(new BlockFireProtectionProp("ems_stretcher", new int[]{1, 0, 1, 15, 14, 15}, true));
    initTabBlock(new BlockFireProtectionProp("ems_stretcher_lowered", new int[]{1, 0, 1, 15, 9, 15}, true));
    initTabBlock(new BlockFireProtectionProp("ems_stair_chair", new int[]{4, 0, 4, 12, 16, 14}, true));
    initTabBlock(new BlockFireProtectionProp("ems_backboard_rack", new int[]{1, 0, 10, 15, 16, 16}, true));
    initTabBlock(new BlockFireProtectionProp("ems_supply_shelving", new int[]{0, 0, 6, 16, 16, 16}, true));
    initTabBlock(new BlockFireProtectionProp("oxygen_cylinder_rack", new int[]{1, 0, 4, 15, 15, 14}, true));
    initTabBlock(new BlockFireProtectionProp("medication_safe", new int[]{3, 2, 9, 13, 13, 16}, true));
    initTabBlock(new BlockFireProtectionProp("decon_sink", new int[]{1, 0, 5, 15, 16, 16}, true));
    initTabBlock(new BlockFireProtectionProp("eyewash_station", new int[]{2, 1, 9, 14, 15, 16}, true));
    initTabBlock(new BlockFireProtectionProp("star_of_life_emblem", new int[]{1, 1, 15, 15, 15, 16}, false));
  }
}

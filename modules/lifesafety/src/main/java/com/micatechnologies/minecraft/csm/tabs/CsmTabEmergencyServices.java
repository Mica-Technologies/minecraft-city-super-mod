package com.micatechnologies.minecraft.csm.tabs;

import com.micatechnologies.minecraft.csm.CsmRegistry;
import com.micatechnologies.minecraft.csm.codeutils.CsmTab;
import com.micatechnologies.minecraft.csm.lifesafety.fireprotection.BlockFireProtectionProp;
import com.micatechnologies.minecraft.csm.lifesafety.stations.BlockFirePole;
import com.micatechnologies.minecraft.csm.lifesafety.stations.BlockFirePoleHole;
import com.micatechnologies.minecraft.csm.lifesafety.stations.BlockStationAlertController;
import com.micatechnologies.minecraft.csm.lifesafety.stations.BlockStationAlertDevice;
import com.micatechnologies.minecraft.csm.lifesafety.stations.BlockStationBell;
import com.micatechnologies.minecraft.csm.lifesafety.stations.BlockStationNumberPlaque;
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
  }
}

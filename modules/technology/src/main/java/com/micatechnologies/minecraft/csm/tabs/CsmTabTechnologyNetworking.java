package com.micatechnologies.minecraft.csm.tabs;

import com.micatechnologies.minecraft.csm.CsmRegistry;
import com.micatechnologies.minecraft.csm.codeutils.CsmTab;
import com.micatechnologies.minecraft.csm.technology.BlockDeskDeviceFactory;
import com.micatechnologies.minecraft.csm.technology.network.BlockCableTray;
import com.micatechnologies.minecraft.csm.technology.rack.BlockNetworkCabinet;
import com.micatechnologies.minecraft.csm.technology.rack.BlockServerRack;
import com.micatechnologies.minecraft.csm.technology.rack.ItemRackUnit;
import com.micatechnologies.minecraft.csm.technology.rack.RackFrame;
import com.micatechnologies.minecraft.csm.technology.rack.RackUnitType;
import net.minecraft.block.Block;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;

/**
 * The Networking &amp; IT tab of the Technology module: Micafi switches, gateways and wireless
 * gear, the server racks and what goes in them, and the cabling a building's network runs on.
 * The rest of the Technology module (AV, phones, the school set) stays in the Technology tab.
 *
 * @since 2026.10
 */
@CsmTab.Load(order = 32)
public class CsmTabTechnologyNetworking extends CsmTab {

  @Override
  public String getTabId() {
    return "tabtechnetworking";
  }

  @Override
  public Block getTabIcon() {
    return CsmRegistry.getBlock("micafi_switch_8_port");
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
    // --- Technology - Networking & IT (gen_technology_networking.py --fragments) ---
    initTabBlock(new BlockDeskDeviceFactory("micafi_switch_5_port", new double[]{5.0, 0.0, 5.75, 11.0, 2.0, 10.25}));
    initTabBlock(new BlockDeskDeviceFactory("micafi_switch_8_port", new double[]{4.0, 0.0, 5.0, 12.0, 2.0, 11.0}));
    initTabBlock(new BlockDeskDeviceFactory("micafi_switch_16_port", new double[]{1.0, 0.0, 4.5, 15.0, 2.5, 11.5}));
    initTabBlock(new BlockDeskDeviceFactory("micafi_switch_flex_mini", new double[]{5.75, 0.0, 6.25, 10.25, 1.25, 9.75}));
    initTabBlock(new BlockDeskDeviceFactory("micafi_gateway", new double[]{4.5, 0.0, 4.5, 11.5, 3.0, 11.5}));
    initTabBlock(new BlockDeskDeviceFactory("micafi_control_hub", new double[]{6.0, 0.0, 5.0, 10.0, 1.6, 11.0}));
    initTabBlock(new BlockDeskDeviceFactory("corvane_cable_modem", new double[]{6.75, 0.0, 5.0, 9.25, 9.0, 11.0}));
    initTabBlock(new BlockDeskDeviceFactory("micafi_poe_injector", new double[]{6.5, 0.0, 5.25, 9.5, 1.5, 10.75}));
    initTabBlock(new BlockDeskDeviceFactory("network_faceplate_1_port", new double[]{5.75, 4.5, 15.5, 10.25, 11.5, 16.0}));
    initTabBlock(new BlockDeskDeviceFactory("network_faceplate_2_port", new double[]{5.75, 4.5, 15.5, 10.25, 11.5, 16.0}));
    initTabBlock(new BlockDeskDeviceFactory("network_faceplate_4_port", new double[]{5.75, 4.5, 15.5, 10.25, 11.5, 16.0}));
    initTabBlock(new BlockDeskDeviceFactory("floor_box_network", new double[]{3.0, 0.0, 3.0, 13.0, 0.6, 13.0}));
    initTabBlock(new BlockCableTray("cable_tray_basket", new double[]{0.0, 3.0}, new double[]{11.0, 16.0}));
    initTabBlock(new BlockCableTray("cable_tray_ladder", new double[]{0.0, 2.0}, new double[]{12.0, 16.0}));
    // --- Technology - Networking & IT (gen_technology_rack.py --fragments) ---
    initTabBlock(new BlockServerRack("server_rack_42u_open", RackFrame.OPEN_42U));
    initTabBlock(new BlockServerRack("server_rack_42u_mesh", RackFrame.CABINET_42U));
    initTabBlock(new BlockServerRack("server_rack_42u_glass", RackFrame.CABINET_42U));
    initTabBlock(new BlockServerRack("server_rack_42u_filled_compute", RackFrame.CABINET_42U, true));
    initTabBlock(new BlockServerRack("server_rack_42u_filled_mixed", RackFrame.CABINET_42U, true));
    initTabBlock(new BlockServerRack("server_rack_42u_filled_storage", RackFrame.CABINET_42U, true));
    initTabBlock(new BlockServerRack("server_rack_42u_filled_network", RackFrame.CABINET_42U, true));
    initTabBlock(new BlockServerRack("server_rack_42u_filled_gpu", RackFrame.CABINET_42U, true));
    initTabBlock(new BlockServerRack("server_rack_42u_filled_sparse", RackFrame.CABINET_42U, true));
    initTabBlock(new BlockServerRack("server_rack_42u_filled_network_open", RackFrame.OPEN_42U, true));
    initTabBlock(new BlockNetworkCabinet("network_cabinet_12u_wall", RackFrame.WALL_12U));
    initTabItem(new ItemRackUnit(RackUnitType.SERVER_1U));
    initTabItem(new ItemRackUnit(RackUnitType.SERVER_2U));
    initTabItem(new ItemRackUnit(RackUnitType.STORAGE_4U));
    initTabItem(new ItemRackUnit(RackUnitType.SWITCH_24_PORT));
    initTabItem(new ItemRackUnit(RackUnitType.SWITCH_48_PORT));
    initTabItem(new ItemRackUnit(RackUnitType.PATCH_PANEL));
    initTabItem(new ItemRackUnit(RackUnitType.FIREWALL));
    initTabItem(new ItemRackUnit(RackUnitType.UPS_2U));
    initTabItem(new ItemRackUnit(RackUnitType.PDU));
    initTabItem(new ItemRackUnit(RackUnitType.KVM_DRAWER));
    initTabItem(new ItemRackUnit(RackUnitType.CABLE_MANAGER));
    initTabItem(new ItemRackUnit(RackUnitType.BLANKING_PLATE));
    initTabItem(new ItemRackUnit(RackUnitType.MICAFI_GATEWAY));
    initTabItem(new ItemRackUnit(RackUnitType.GPU_SERVER_2U));
  }
}

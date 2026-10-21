package com.micatechnologies.minecraft.csm.tabs;

import com.micatechnologies.minecraft.csm.CsmRegistry;
import com.micatechnologies.minecraft.csm.codeutils.CsmTab;
import com.micatechnologies.minecraft.csm.technology.BlockDeskDeviceFactory;
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
    // --- Networking & IT (gen_technology_networking.py --fragments) ---
    initTabBlock(new BlockDeskDeviceFactory("micafi_switch_8_port", new double[]{4.0, 0.0, 5.0, 12.0, 2.0, 11.0}));
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

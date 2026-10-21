package com.micatechnologies.minecraft.csm.tabs;

import com.micatechnologies.minecraft.csm.CsmRegistry;
import com.micatechnologies.minecraft.csm.codeutils.CsmTab;
import com.micatechnologies.minecraft.csm.codeutils.ItemDecorativeFactory;
import com.micatechnologies.minecraft.csm.technology.BlockDeskDeviceFactory;
import com.micatechnologies.minecraft.csm.technology.BlockDeskScreenFactory;
import net.minecraft.block.Block;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;

/**
 * The Computers &amp; Peripherals tab of the Technology module: gaming rigs and workstations,
 * monitors, keyboards of every kind, mice and the rest of what sits on a desk around a computer.
 * The rest of the Technology module (AV, phones, the school set) stays in the Technology tab.
 *
 * @since 2026.10
 */
@CsmTab.Load(order = 31)
public class CsmTabTechnologyComputers extends CsmTab {

  @Override
  public String getTabId() {
    return "tabtechcomputers";
  }

  @Override
  public Block getTabIcon() {
    return CsmRegistry.getBlock("keyboard_mech_full_black");
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
    // --- Technology - Computers & Peripherals (gen_technology_computers.py --fragments) ---
    initTabBlock(new BlockDeskDeviceFactory("pc_tower_gaming_mid_black", new double[]{4.5, 0.0, 1.0, 11.5, 15.0, 15.0}));
    initTabBlock(new BlockDeskDeviceFactory("pc_tower_gaming_mid_white", new double[]{4.5, 0.0, 1.0, 11.5, 15.0, 15.0}));
    initTabBlock(new BlockDeskDeviceFactory("pc_tower_gaming_full_black", new double[]{4.0, 0.0, 0.5, 12.0, 16.0, 15.5}));
    initTabBlock(new BlockDeskDeviceFactory("pc_mini_itx_black", new double[]{4.0, 0.0, 3.0, 12.0, 9.5, 14.0}));
    initTabBlock(new BlockDeskDeviceFactory("pc_workstation_silver", new double[]{4.0, 0.0, 1.0, 12.0, 16.0, 15.0}));
    initTabBlock(new BlockDeskDeviceFactory("pc_small_form_factor_black", new double[]{2.5, 0.0, 3.0, 13.5, 4.0, 13.0}));
    initTabBlock(new BlockDeskDeviceFactory("pc_mini_desktop_grey", new double[]{5.5, 0.0, 5.5, 10.5, 2.0, 10.5}));
    initTabBlock(new BlockDeskDeviceFactory("pc_test_bench_open", new double[]{1.0, 0.0, 2.0, 15.0, 7.0, 14.0}));
    initTabBlock(new BlockDeskScreenFactory("laptop_gaming_open_black", new double[]{1.0, 0.0, 3.25, 15.0, 9.5, 16.0}));
    initTabBlock(new BlockDeskDeviceFactory("laptop_gaming_closed_black", new double[]{1.0, 0.0, 3.0, 15.0, 1.5, 12.75}));
    initTabBlock(new BlockDeskScreenFactory("monitor_24_office_black", new double[]{1.75, 0.0, 8.0, 14.25, 10.75, 12.75}));
    initTabBlock(new BlockDeskScreenFactory("monitor_24_office_webcam_black", new double[]{1.75, 0.0, 8.0, 14.25, 11.75, 12.75}));
    initTabBlock(new BlockDeskScreenFactory("monitor_27_gaming_black", new double[]{1.0, 0.0, 5.0, 15.0, 11.94, 13.0}));
    initTabBlock(new BlockDeskScreenFactory("monitor_27_gaming_white", new double[]{1.0, 0.0, 5.0, 15.0, 11.94, 13.0}));
    initTabBlock(new BlockDeskScreenFactory("monitor_34_ultrawide_curved_black", new double[]{0.0, 0.0, 6.5, 16.0, 13.0, 13.75}));
    initTabBlock(new BlockDeskScreenFactory("monitor_dual_arm_black", new double[]{0.0, 0.0, 7.5, 16.0, 12.5, 15.75}));
    initTabBlock(new BlockDeskScreenFactory("monitor_portrait_black", new double[]{4.25, 0.0, 8.0, 11.75, 15.5, 12.75}));
    initTabBlock(new BlockDeskDeviceFactory("keyboard_mech_full_black", new double[]{0.5, 0.0, 5.5, 15.5, 1.5, 10.5}));
    initTabBlock(new BlockDeskDeviceFactory("keyboard_mech_full_white", new double[]{0.5, 0.0, 5.5, 15.5, 1.5, 10.5}));
    initTabBlock(new BlockDeskDeviceFactory("keyboard_mech_tkl_black", new double[]{1.75, 0.0, 5.5, 14.25, 1.5, 10.5}));
    initTabBlock(new BlockDeskDeviceFactory("keyboard_mech_tkl_pink", new double[]{1.75, 0.0, 5.5, 14.25, 1.5, 10.5}));
    initTabBlock(new BlockDeskDeviceFactory("keyboard_mech_60_white", new double[]{2.75, 0.0, 6.0, 13.25, 1.5, 10.0}));
    initTabBlock(new BlockDeskDeviceFactory("keyboard_mech_60_retro", new double[]{2.75, 0.0, 6.0, 13.25, 1.5, 10.0}));
    initTabBlock(new BlockDeskDeviceFactory("keyboard_mech_rgb_black", new double[]{0.5, 0.0, 5.5, 15.5, 1.75, 10.5}));
    initTabBlock(new BlockDeskDeviceFactory("keyboard_slim_silver", new double[]{0.5, 0.0, 5.5, 15.5, 0.75, 10.5}));
    initTabBlock(new BlockDeskDeviceFactory("keyboard_retro_beige", new double[]{0.5, 0.0, 4.75, 15.5, 2.25, 11.25}));
    initTabBlock(new BlockDeskDeviceFactory("keyboard_wireless_compact_white", new double[]{2.5, 0.0, 5.0, 13.5, 1.0, 11.0}));
    initTabBlock(new BlockDeskDeviceFactory("keyboard_split_ergonomic_black", new double[]{1.0, 0.0, 2.0, 15.0, 1.5, 9.75}));
    initTabBlock(new BlockDeskDeviceFactory("numpad_black", new double[]{6.0, 0.0, 5.5, 10.0, 1.5, 10.5}));
    initTabBlock(new BlockDeskDeviceFactory("numpad_white", new double[]{6.0, 0.0, 5.5, 10.0, 1.5, 10.5}));
    initTabBlock(new BlockDeskDeviceFactory("mouse_gaming_black", new double[]{6.5, 0.0, 5.75, 9.5, 1.75, 10.25}));
    initTabBlock(new BlockDeskDeviceFactory("mouse_vertical_ergonomic_grey", new double[]{6.75, 0.0, 5.5, 9.75, 3.0, 10.25}));
    initTabBlock(new BlockDeskDeviceFactory("mouse_trackball_black", new double[]{5.75, 0.0, 5.25, 10.25, 3.25, 10.75}));
    initTabBlock(new BlockDeskDeviceFactory("mouse_travel_white", new double[]{7.0, 0.0, 6.25, 9.0, 1.25, 9.75}));
    initTabBlock(new BlockDeskDeviceFactory("mouse_pad_black", new double[]{4.5, 0.0, 5.0, 11.5, 0.25, 11.0}));
    initTabBlock(new BlockDeskDeviceFactory("desk_mat_xxl_black", new double[]{0.0, 0.0, 3.5, 16.0, 0.25, 12.5}));
    initTabBlock(new BlockDeskDeviceFactory("desk_mat_xxl_rgb", new double[]{0.0, 0.0, 3.5, 16.0, 0.25, 12.5}));
    initTabBlock(new BlockDeskDeviceFactory("headset_on_stand_black", new double[]{3.0, 0.0, 5.5, 12.75, 10.75, 10.5}));
    initTabBlock(new BlockDeskDeviceFactory("webcam_tripod_black", new double[]{6.0, 0.0, 6.0, 10.0, 5.5, 10.75}));
    initTabBlock(new BlockDeskDeviceFactory("microphone_boom_arm_black", new double[]{6.0, 0.0, 6.0, 16.0, 11.0, 16.0}));
    initTabBlock(new BlockDeskDeviceFactory("speakers_desktop_pair_black", new double[]{0.5, 0.0, 6.0, 15.5, 5.5, 9.75}));
    initTabBlock(new BlockDeskDeviceFactory("subwoofer_desktop_black", new double[]{4.5, 0.0, 4.5, 11.5, 8.0, 11.5}));
    initTabBlock(new BlockDeskDeviceFactory("usb_hub_grey", new double[]{5.0, 0.0, 7.0, 11.0, 1.0, 12.0}));
    initTabBlock(new BlockDeskDeviceFactory("external_drive_black", new double[]{6.5, 0.0, 6.0, 9.5, 1.0, 13.0}));
    initTabBlock(new BlockDeskDeviceFactory("docking_station_vertical_silver", new double[]{6.0, 0.0, 3.0, 9.75, 11.25, 13.0}));
    initTabBlock(new BlockDeskDeviceFactory("kvm_switch_grey", new double[]{5.0, 0.0, 6.0, 11.0, 1.5, 10.0}));
    initTabBlock(new BlockDeskDeviceFactory("macro_keypad_black", new double[]{5.5, 0.0, 6.25, 10.5, 2.0, 10.5}));
    initTabBlock(new BlockDeskDeviceFactory("console_orbix_white", new double[]{5.75, 0.0, 3.0, 10.25, 12.75, 13.0}));
    initTabBlock(new BlockDeskDeviceFactory("console_vertex_black", new double[]{2.5, 0.0, 4.0, 13.5, 3.0, 12.0}));
    initTabBlock(new BlockDeskDeviceFactory("console_pocketeer_docked", new double[]{3.5, 0.0, 7.0, 12.5, 5.5, 9.25}));
    initTabBlock(new BlockDeskDeviceFactory("console_retro_grey", new double[]{4.0, 0.0, 5.0, 12.0, 5.0, 11.0}));
    initTabItem(new ItemDecorativeFactory("controller_orbix_white", "This controller does nothing and is only for looks!"));
    initTabItem(new ItemDecorativeFactory("controller_vertex_black", "This controller does nothing and is only for looks!"));
    initTabItem(new ItemDecorativeFactory("controller_pocketeer_grips", "This controller does nothing and is only for looks!"));
    initTabItem(new ItemDecorativeFactory("controller_retro_grey", "This controller does nothing and is only for looks!"));
    initTabItem(new ItemDecorativeFactory("vr_headset_black", "This VR headset does nothing and is only for looks!"));
  }
}

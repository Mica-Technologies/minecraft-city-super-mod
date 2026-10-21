package com.micatechnologies.minecraft.csm.tabs;

import com.micatechnologies.minecraft.csm.CsmRegistry;
import com.micatechnologies.minecraft.csm.codeutils.CsmTab;
import com.micatechnologies.minecraft.csm.technology.BlockDeskDeviceFactory;
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
  }
}

package com.micatechnologies.minecraft.csm.tabs;

import com.micatechnologies.minecraft.csm.CsmRegistry;
import com.micatechnologies.minecraft.csm.codeutils.CsmTab;
import com.micatechnologies.minecraft.csm.technology.BlockDeskDeviceFactory;
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
    // --- Computers & Peripherals (gen_technology_computers.py --fragments) ---
    initTabBlock(new BlockDeskDeviceFactory("keyboard_mech_full_black", new double[]{0.6, 0.0, 5.6, 15.4, 1.35, 10.4}));
  }
}

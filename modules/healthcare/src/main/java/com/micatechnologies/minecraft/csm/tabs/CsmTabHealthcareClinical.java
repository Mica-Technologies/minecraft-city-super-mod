package com.micatechnologies.minecraft.csm.tabs;

import com.micatechnologies.minecraft.csm.CsmRegistry;
import com.micatechnologies.minecraft.csm.codeutils.CsmTab;
import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;

/**
 * The Healthcare module's clinical tab: nursing (the nurse call master panel, infection control,
 * linen), and later the emergency department, surgery support, imaging, the lab, pharmacy, the
 * morgue and the building systems.
 *
 * @version 1.0
 * @since 2026.10
 */
@CsmTab.Load(order = 30)
public class CsmTabHealthcareClinical extends CsmTab {

  @Override
  public String getTabId() {
    return "tabhealthcareclinical";
  }

  @Override
  public Block getTabIcon() {
    Block icon = CsmRegistry.getBlock("nurse_call_master_panel");
    return icon != null ? icon : Blocks.QUARTZ_BLOCK;
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
  }
}

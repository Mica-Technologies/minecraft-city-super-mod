package com.micatechnologies.minecraft.csm.tabs;

import com.micatechnologies.minecraft.csm.codeutils.CsmTab;
import com.micatechnologies.minecraft.csm.hvac.BlockHvacExhaustFanPart;
import net.minecraft.block.Block;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;

/**
 * The HVAC module's hidden tab: the cells a multi-block unit places for itself (the lab exhaust
 * fan's), which no player picks from the inventory.
 *
 * @version 1.0
 * @since 2026.10
 */
@CsmTab.Load(order = -15)
public class CsmTabHvacHidden extends CsmTab {

  @Override
  public String getTabId() {
    return null;
  }

  @Override
  public Block getTabIcon() {
    return null;
  }

  @Override
  public boolean getTabSearchable() {
    return false;
  }

  @Override
  public boolean getTabHidden() {
    return true;
  }

  @Override
  public void initTabElements(FMLPreInitializationEvent fmlPreInitializationEvent) {
    // --- gen_hvac_exhaust.py --fragments ---
    initTabBlock(new BlockHvacExhaustFanPart());
  }
}

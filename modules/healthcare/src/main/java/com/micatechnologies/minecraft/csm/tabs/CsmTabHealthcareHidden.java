package com.micatechnologies.minecraft.csm.tabs;

import com.micatechnologies.minecraft.csm.codeutils.CsmTab;
import net.minecraft.block.Block;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;

/**
 * The healthcare module's hidden tab: the cells a multi-block piece places for itself (a hospital
 * bed's foot), which no player picks from the inventory.
 *
 * @version 1.0
 * @since 2026.10
 */
@CsmTab.Load(order = -11)
public class CsmTabHealthcareHidden extends CsmTab {

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
  }
}

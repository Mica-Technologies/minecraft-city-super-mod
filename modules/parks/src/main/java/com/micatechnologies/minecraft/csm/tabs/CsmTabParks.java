package com.micatechnologies.minecraft.csm.tabs;

import com.micatechnologies.minecraft.csm.codeutils.CsmTab;
import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;

/**
 * The Parks tab of the Parks &amp; Greenery module: benches, tables, bins, playground pieces,
 * fountains, irrigation and the other amenities of a park.
 *
 * @since 2026.9
 */
@CsmTab.Load(order = 19)
public class CsmTabParks extends CsmTab {

  @Override
  public String getTabId() {
    return "tabparks";
  }

  @Override
  public Block getTabIcon() {
    return Blocks.RED_FLOWER;
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

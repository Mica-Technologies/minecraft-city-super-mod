package com.micatechnologies.minecraft.csm.tabs;

import com.micatechnologies.minecraft.csm.codeutils.CsmTab;
import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;

/**
 * The Trees &amp; Plants tab of the Parks &amp; Greenery module: tree logs and leaves by species,
 * the Tree Planting Tool, street tree accessories and plantings.
 *
 * @since 2026.9
 */
@CsmTab.Load(order = 18)
public class CsmTabTreesPlants extends CsmTab {

  @Override
  public String getTabId() {
    return "tabtreesplants";
  }

  @Override
  public Block getTabIcon() {
    return Blocks.SAPLING;
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

package com.micatechnologies.minecraft.csm.tabs;

import com.micatechnologies.minecraft.csm.codeutils.CsmTab;
import com.micatechnologies.minecraft.csm.furniture.outdoor.BlockBounceCastlePart;
import com.micatechnologies.minecraft.csm.furniture.residential.BlockSwitchRelay;
import net.minecraft.block.Block;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;

/**
 * The Furniture &amp; Novelties module's hidden tab: the blocks its other blocks place for
 * themselves, which no player places -- the relay a linked light switch puts beside the block it
 * powers.
 *
 * @version 1.0
 * @since 2026.9
 */
@CsmTab.Load(order = -7)
public class CsmTabFurnishingsHidden extends CsmTab {

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
    initTabBlock(new BlockSwitchRelay()); // Light Switch Relay
    initTabBlock(new BlockBounceCastlePart()); // Bounce Castle Part
  }
}

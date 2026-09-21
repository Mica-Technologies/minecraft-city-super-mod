package com.micatechnologies.minecraft.csm.tabs;

import com.micatechnologies.minecraft.csm.codeutils.CsmTab;
import com.micatechnologies.minecraft.csm.signage.BlockAdBoardPart;
import net.minecraft.block.Block;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;

/**
 * The signage module's hidden tab: the blocks an advertising board builds around its controller,
 * which no player places.
 *
 * @version 1.0
 */
@CsmTab.Load(order = -8)
public class CsmTabSignageHidden extends CsmTab {

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
    initTabBlock(new BlockAdBoardPart("ad_poster_board_part")); // Poster Board
    initTabBlock(new BlockAdBoardPart("ad_billboard_part")); // Billboard
    initTabBlock(new BlockAdBoardPart("ad_digital_billboard_part")); // Digital Billboard
  }
}

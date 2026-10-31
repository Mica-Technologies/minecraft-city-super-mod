package com.micatechnologies.minecraft.csm.tabs;

import com.micatechnologies.minecraft.csm.codeutils.CsmTab;
import com.micatechnologies.minecraft.csm.venues.BlockVenueUnitPart;
import net.minecraft.block.Block;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;

/**
 * The venues module's hidden tab: the cells a multi-block piece places for itself and any
 * retiring block, which no player picks from the inventory. So far the one invisible unit part
 * every multi-block unit (the hockey goal, the basketball stanchion, the ice resurfacer, the ice
 * plant) fills its footprint with.
 *
 * @version 1.0
 * @since 2026.10
 */
@CsmTab.Load(order = -12)
public class CsmTabVenuesHidden extends CsmTab {

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
    // --- hidden (gen_venues_arena.py --fragments) ---
    initTabBlock(new BlockVenueUnitPart());
  }
}

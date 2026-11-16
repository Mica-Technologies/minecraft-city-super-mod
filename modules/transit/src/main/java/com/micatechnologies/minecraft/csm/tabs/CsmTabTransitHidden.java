package com.micatechnologies.minecraft.csm.tabs;

import com.micatechnologies.minecraft.csm.codeutils.CsmTab;
import com.micatechnologies.minecraft.csm.transit.arrival.BlockBusArrivalBoardPart;
import net.minecraft.block.Block;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;

/**
 * The transit module's hidden tab: the cells a multi-block piece places for itself, which no
 * player picks from the inventory. So far the one invisible part every arrival board and info
 * totem fills its cells with.
 *
 * <p>Hidden tabs have no creative-inventory presence at all: {@link CsmTab} gives them a
 * {@code null} {@code CreativeTabs}, which is also what keeps their blocks out of the
 * Fabricator.</p>
 *
 * @version 1.0
 * @since 2026.11
 */
@CsmTab.Load(order = -14)
public class CsmTabTransitHidden extends CsmTab {

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
    // --- hidden (gen_transit_arrival_boards.py --fragments) ---
    initTabBlock(new BlockBusArrivalBoardPart());
  }
}

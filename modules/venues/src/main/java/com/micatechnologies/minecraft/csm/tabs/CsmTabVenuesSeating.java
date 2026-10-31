package com.micatechnologies.minecraft.csm.tabs;

import com.micatechnologies.minecraft.csm.CsmRegistry;
import com.micatechnologies.minecraft.csm.codeutils.CsmTab;
import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;

/**
 * The Sports & Venues module's seating tab: the seating bowl of an arena or stadium. Stadium
 * seats in ten colours, the aisle-end standards with their row letters, the companion seat and
 * wheelchair space, aisle step lights and handrails, glass guards for tier and suite fronts, and
 * the section and portal signs over the vomitories.
 *
 * <p>The module's later tabs follow it: the event floor (rink boards and glass, goals, the
 * court, benches) and then the scoreboard, AV and concourse.</p>
 *
 * @version 1.0
 * @since 2026.10
 */
@CsmTab.Load(order = 34)
public class CsmTabVenuesSeating extends CsmTab {

  @Override
  public String getTabId() {
    return "tabvenuesseating";
  }

  @Override
  public Block getTabIcon() {
    Block icon = CsmRegistry.getBlock("stadium_seat_navy");
    return icon != null ? icon : Blocks.WOOL;
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

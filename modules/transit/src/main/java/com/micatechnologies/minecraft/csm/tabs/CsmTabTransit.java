package com.micatechnologies.minecraft.csm.tabs;

import com.micatechnologies.minecraft.csm.CsmRegistry;
import com.micatechnologies.minecraft.csm.codeutils.CsmTab;
import com.micatechnologies.minecraft.csm.transit.fare.BlockFareGate;
import com.micatechnologies.minecraft.csm.transit.fare.BlockFareGateAda2;
import com.micatechnologies.minecraft.csm.transit.fare.BlockFareGateAda3;
import com.micatechnologies.minecraft.csm.transit.fare.BlockFareVendingMachine;
import com.micatechnologies.minecraft.csm.transit.fare.ItemFareTicket;
import com.micatechnologies.minecraft.csm.transit.fare.ItemTransitCard;
import net.minecraft.block.Block;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;

/**
 * The tab for public transit: the fare gates, the fare vending machine and the tickets and cards
 * they take.
 *
 * @version 1.0
 * @since 2026.9
 */
@CsmTab.Load(order = 27)
public class CsmTabTransit extends CsmTab {

  /**
   * Gets the ID (unique identifier) of the tab.
   *
   * @return the ID of the tab
   *
   * @since 1.0
   */
  @Override
  public String getTabId() {
    return "tabtransit";
  }

  /**
   * Gets the block to use as the icon of the tab.
   *
   * @return the block to use as the icon of the tab
   *
   * @since 1.0
   */
  @Override
  public Block getTabIcon() {
    return CsmRegistry.getBlock("fare_gate");
  }

  /**
   * Gets a boolean indicating if the tab is searchable (has its own search bar).
   *
   * @return {@code true} if the tab is searchable, otherwise {@code false}
   *
   * @since 1.0
   */
  @Override
  public boolean getTabSearchable() {
    return false;
  }

  /**
   * Gets a boolean indicating if the tab is hidden (not displayed in the inventory).
   *
   * @return {@code true} if the tab is hidden, otherwise {@code false}
   *
   * @since 1.0
   */
  @Override
  public boolean getTabHidden() {
    return false;
  }

  /**
   * Initializes all the elements belonging to the tab.
   *
   * @param fmlPreInitializationEvent the {@link FMLPreInitializationEvent} that is being processed
   *
   * @since 1.0
   */
  @Override
  public void initTabElements(FMLPreInitializationEvent fmlPreInitializationEvent) {
    initTabBlock(BlockFareVendingMachine.class, fmlPreInitializationEvent);
    initTabBlock(BlockFareGate.class, fmlPreInitializationEvent);
    initTabBlock(BlockFareGateAda2.class, fmlPreInitializationEvent);
    initTabBlock(BlockFareGateAda3.class, fmlPreInitializationEvent);
    initTabItem(ItemFareTicket.class, fmlPreInitializationEvent);
    initTabItem(ItemTransitCard.class, fmlPreInitializationEvent);
  }
}

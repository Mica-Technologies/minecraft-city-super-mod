package com.micatechnologies.minecraft.csm.tabs;

import com.micatechnologies.minecraft.csm.CsmRegistry;
import com.micatechnologies.minecraft.csm.codeutils.CsmTab;
import com.micatechnologies.minecraft.csm.streetscape.BlockFireHydrant;
import com.micatechnologies.minecraft.csm.trafficaccessories.BlockWorkZoneDeviceDiagonal;
import net.minecraft.block.Block;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;

/**
 * The tab for the street fixtures between the curb and the building line: hydrants, delineators,
 * and the covers, bollards, mailboxes and parking meters that join them.
 *
 * <p>Curbs, sidewalks, tactile paving and the common bollards are left to external road mods,
 * which already supply them; every block here settles onto their sloped and partial-height
 * surfaces.</p>
 *
 * @version 1.0
 */
@CsmTab.Load(order = 11)
public class CsmTabStreetscape extends CsmTab {

  /**
   * Gets the ID (unique identifier) of the tab.
   *
   * @return the ID of the tab
   */
  @Override
  public String getTabId() {
    return "tabstreetscape";
  }

  /**
   * Gets the block to use as the icon of the tab
   *
   * @return the block to use as the icon of the tab
   */
  @Override
  public Block getTabIcon() {
    return CsmRegistry.getBlock("firehydrant");
  }

  /**
   * Gets a boolean indicating if the tab is searchable (has its own search bar).
   *
   * @return {@code true} if the tab is searchable, otherwise {@code false}
   */
  @Override
  public boolean getTabSearchable() {
    return false;
  }

  /**
   * Gets a boolean indicating if the tab is hidden (not displayed in the inventory).
   *
   * @return {@code true} if the tab is hidden, otherwise {@code false}
   */
  @Override
  public boolean getTabHidden() {
    return false;
  }

  /**
   * Initializes all the elements belonging to the tab.
   *
   * @param fmlPreInitializationEvent the {@link FMLPreInitializationEvent} that is being processed
   */
  @Override
  public void initTabElements(FMLPreInitializationEvent fmlPreInitializationEvent) {
    initTabBlock(BlockFireHydrant.class, fmlPreInitializationEvent); // Fire Hydrant

    // Delineators, moved here from Traffic Accessories.
    initTabBlock(new BlockWorkZoneDeviceDiagonal("delineator_post",
        new AxisAlignedBB(0.356250, 0.000000, 0.356250, 0.643750, 0.937500, 0.643750)));
    initTabBlock(new BlockWorkZoneDeviceDiagonal("delineator_post_yellow",
        new AxisAlignedBB(0.356250, 0.000000, 0.356250, 0.643750, 0.937500, 0.643750)));
    initTabBlock(new BlockWorkZoneDeviceDiagonal("delineator_zebra",
        new AxisAlignedBB(0.068750, 0.000000, 0.340625, 0.931250, 0.190625, 0.659375)));
  }
}

package com.micatechnologies.minecraft.csm.tabs;

import com.micatechnologies.minecraft.csm.CsmRegistry;
import com.micatechnologies.minecraft.csm.codeutils.CsmTab;
import net.minecraft.block.Block;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;

/**
 * The tab for structural and framing blocks: steel and wood stud walls, the horizontal structure
 * that spans between them, and structural steel.
 *
 * <p>This tab is deliberately empty until the framing blocks themselves land. It is created ahead
 * of them so that the tab order, the Fabricator pricing branch and the guidebook page all exist
 * before there is content to put in them, rather than being retrofitted around blocks that are
 * already registered.</p>
 *
 * @version 1.0
 */
@CsmTab.Load(order = 14)
public class CsmTabStructureFraming extends CsmTab {

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
   * Gets the ID (unique identifier) of the tab.
   *
   * @return the ID of the tab
   *
   * @since 1.0
   */
  @Override
  public String getTabId() {
    return "tabstructureframing";
  }

  /**
   * Gets the block to use as the icon of the tab
   *
   * <p>Placeholder: this tab has no blocks of its own yet, and a tab must supply an icon or
   * {@link CsmTab#getTabIconStack()} throws. Swap this for the steel stud wall as soon as that
   * block exists.</p>
   *
   * @return the block to use as the icon of the tab
   *
   * @since 1.0
   */
  @Override
  public Block getTabIcon() {
    return CsmRegistry.getBlock("silvermetal");
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
   * Initializes all the items belonging to the tab.
   *
   * @since 1.0
   */
  @Override
  public void initTabElements(FMLPreInitializationEvent fmlPreInitializationEvent) {
    // Intentionally empty. The framing blocks register here as they are built.
  }
}

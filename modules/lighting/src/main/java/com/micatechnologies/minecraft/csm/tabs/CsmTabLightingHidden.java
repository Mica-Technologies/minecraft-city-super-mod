package com.micatechnologies.minecraft.csm.tabs;

import com.micatechnologies.minecraft.csm.codeutils.CsmTab;
import com.micatechnologies.minecraft.csm.lighting.BlockLightupAir;
import com.micatechnologies.minecraft.csm.lighting.BlockOCPB;
import com.micatechnologies.minecraft.csm.lighting.BlockOCPM;
import com.micatechnologies.minecraft.csm.lighting.BlockOCPT;
import com.micatechnologies.minecraft.csm.lighting.BlockRCPB;
import com.micatechnologies.minecraft.csm.lighting.BlockRCPB2;
import com.micatechnologies.minecraft.csm.lighting.BlockRCPM;
import com.micatechnologies.minecraft.csm.lighting.BlockRCPT;
import net.minecraft.block.Block;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;

/**
 * The hidden tab holding the Lighting blocks that never belong in the creative inventory:
 * the light-up air block a lit fixture projects into the space in front of it, and the legacy
 * concrete pole pieces while they retire.
 *
 * <p>Hidden tabs have no creative-inventory presence at all: {@link CsmTab} gives them a
 * {@code null} {@code CreativeTabs}, which is also what keeps their blocks out of the
 * Fabricator. They exist only so a retiring block stays registered and can convert an old
 * placement to its replacement.</p>
 *
 * @version 1.0
 */
@CsmTab.Load(order = -9)
public class CsmTabLightingHidden extends CsmTab {

  /**
   * Gets the ID (unique identifier) of the tab.
   *
   * @return the ID of the tab
   *
   * @since 1.0
   */
  @Override
  public String getTabId() {
    return null;
  }

  /**
   * Gets the block to use as the icon of the tab
   *
   * @return the block to use as the icon of the tab
   *
   * @since 1.0
   */
  @Override
  public Block getTabIcon() {
    return null;
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
    return true;
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
    initTabBlock(BlockLightupAir.class, fmlPreInitializationEvent); // Lightup Air

    // The hand-placed concrete pole pieces, retiring into the concrete poles in the Traffic
    // Accessories tab (BlockTrafficPoleConcrete), which draw their own base and top.
    initTabBlock(BlockOCPB.class, fmlPreInitializationEvent);
    initTabBlock(BlockOCPM.class, fmlPreInitializationEvent);
    initTabBlock(BlockOCPT.class, fmlPreInitializationEvent);
    initTabBlock(BlockRCPB.class, fmlPreInitializationEvent);
    initTabBlock(BlockRCPB2.class, fmlPreInitializationEvent);
    initTabBlock(BlockRCPM.class, fmlPreInitializationEvent);
    initTabBlock(BlockRCPT.class, fmlPreInitializationEvent);
  }
}

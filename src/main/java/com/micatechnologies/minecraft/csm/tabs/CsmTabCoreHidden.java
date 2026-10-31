package com.micatechnologies.minecraft.csm.tabs;

import com.micatechnologies.minecraft.csm.codeutils.CsmTab;
import com.micatechnologies.minecraft.csm.codeutils.light.BlockLightupAir;
import net.minecraft.block.Block;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;

/**
 * Core's hidden tab: the blocks every module may place but no player should pick, registered
 * whichever modules are installed. Today that is the light-up air ({@code csm:lightupair}), which
 * Lighting's fixtures and Sports &amp; Venues' stadium light heads both place.
 *
 * <p>Like every hidden tab it has no creative-inventory presence ({@link CsmTab} gives it a
 * {@code null} {@code CreativeTabs}, which also keeps its blocks out of the Fabricator), and it
 * takes the lowest order of all, so its blocks register before every module's.</p>
 */
@CsmTab.Load(order = -16)
public class CsmTabCoreHidden extends CsmTab {

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
    initTabBlock(BlockLightupAir.class, fmlPreInitializationEvent); // Lightup Air
  }
}

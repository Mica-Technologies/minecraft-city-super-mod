package com.micatechnologies.minecraft.csm.tabs;

import com.micatechnologies.minecraft.csm.CsmRegistry;
import com.micatechnologies.minecraft.csm.codeutils.CsmTab;
import com.micatechnologies.minecraft.csm.venues.arena.BlockArenaFloorBase;
import com.micatechnologies.minecraft.csm.venues.arena.BlockArenaRun;
import com.micatechnologies.minecraft.csm.venues.lines.BlockSportSurface;
import com.micatechnologies.minecraft.csm.venues.skate.BlockSkateElement;
import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;

/**
 * The Sports &amp; Venues module's Skate Park tab: smooth concrete, quarter pipes (1 m in one
 * block, 2 m in three), the bowl corner that joins two round an inside corner, a kicker, a bank
 * and a box top, a grind rail and a ledge that join into runs. Curves drawn true, collision a
 * staircase under them (see {@code VENUES_SYSTEM.md}).
 *
 * @version 1.0
 * @since 2026.10
 */
@CsmTab.Load(order = 42)
public class CsmTabVenuesSkate extends CsmTab {

  @Override
  public String getTabId() {
    return "tabvenuesskate";
  }

  @Override
  public Block getTabIcon() {
    Block icon = CsmRegistry.getBlock("skate_quarter_pipe");
    if (icon == null) {
      icon = CsmRegistry.getBlock("skate_concrete");
    }
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
    // --- Sports & Venues - Skate Park (gen_venues_skate.py --fragments) ---
    initTabBlock(new BlockSkateElement("skate_quarter_pipe", new int[][]{new int[]{0, 0, 4, 16, 1, 6, 0, 0, 6, 16, 2, 8, 0, 0, 8, 16, 3, 10, 0, 0, 10, 16, 4, 12, 0, 0, 12, 16, 7, 14, 0, 0, 14, 16, 10, 16}, new int[]{0, 0, 6, 16, 1, 8, 0, 0, 8, 16, 1, 10, 0, 0, 10, 16, 2, 12, 0, 0, 12, 16, 3, 14, 0, 0, 14, 16, 4, 16}, new int[]{0, 0, 0, 16, 5, 2, 0, 0, 2, 16, 6, 4, 0, 0, 4, 16, 8, 6, 0, 0, 6, 16, 10, 8, 0, 0, 8, 16, 12, 10, 0, 0, 10, 16, 15, 12, 0, 0, 12, 16, 16, 14, 0, 0, 14, 16, 16, 16}, new int[]{0, 0, 12, 16, 2, 14, 0, 0, 14, 16, 8, 16}}));
    initTabBlock(new BlockSkateElement("skate_bowl_corner", new int[][]{new int[]{0, 0, 4, 4, 1, 8, 0, 0, 8, 4, 4, 12, 0, 0, 12, 4, 9, 16, 4, 0, 0, 8, 1, 4, 4, 0, 4, 8, 2, 8, 4, 0, 8, 8, 5, 12, 4, 0, 12, 8, 11, 16, 8, 0, 0, 12, 4, 4, 8, 0, 4, 12, 5, 8, 8, 0, 8, 12, 9, 12, 8, 0, 12, 12, 16, 16, 12, 0, 0, 16, 9, 4, 12, 0, 4, 16, 11, 8, 12, 0, 8, 16, 16, 12, 12, 0, 12, 16, 16, 16}}));
    initTabBlock(new BlockSkateElement("skate_ramp", new int[][]{new int[]{0, 0, 2, 16, 1, 4, 0, 0, 4, 16, 2, 6, 0, 0, 6, 16, 3, 8, 0, 0, 8, 16, 3, 10, 0, 0, 10, 16, 4, 12, 0, 0, 12, 16, 5, 14, 0, 0, 14, 16, 6, 16}, new int[]{0, 0, 2, 16, 2, 4, 0, 0, 4, 16, 2, 6, 0, 0, 6, 16, 4, 8, 0, 0, 8, 16, 4, 10, 0, 0, 10, 16, 6, 12, 0, 0, 12, 16, 6, 14, 0, 0, 14, 16, 8, 16}, new int[]{0, 0, 0, 16, 8, 2, 0, 0, 2, 16, 10, 4, 0, 0, 4, 16, 10, 6, 0, 0, 6, 16, 12, 8, 0, 0, 8, 16, 12, 10, 0, 0, 10, 16, 14, 12, 0, 0, 12, 16, 14, 14, 0, 0, 14, 16, 16, 16}, new int[]{0, 0, 0, 16, 8, 16}}));
    initTabBlock(new BlockSportSurface("skate_concrete", BlockArenaFloorBase.Surface.CONCRETE, false));
    initTabBlock(new BlockArenaRun("skate_grind_rail", BlockArenaRun.Kind.GRIND_RAIL));
    initTabBlock(new BlockArenaRun("skate_ledge", BlockArenaRun.Kind.SKATE_LEDGE));
  }
}

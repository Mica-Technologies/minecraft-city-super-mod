package com.micatechnologies.minecraft.csm.tabs;

import com.micatechnologies.minecraft.csm.CsmRegistry;
import com.micatechnologies.minecraft.csm.codeutils.CsmTab;
import com.micatechnologies.minecraft.csm.venues.BlockVenueUnit;
import com.micatechnologies.minecraft.csm.venues.arena.BlockArenaRun;
import com.micatechnologies.minecraft.csm.venues.bowling.BlockBowlingFixture;
import com.micatechnologies.minecraft.csm.venues.bowling.BlockBowlingLane;
import com.micatechnologies.minecraft.csm.venues.bowling.BlockBowlingMonitor;
import com.micatechnologies.minecraft.csm.venues.bowling.BlockBowlingPins;
import com.micatechnologies.minecraft.csm.venues.bowling.BlockBowlingPinsetter;
import com.micatechnologies.minecraft.csm.venues.bowling.BlockBowlingSettee;
import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;

/**
 * The Sports &amp; Venues module's Bowling tab: a ten-pin bowling centre at one block a metre.
 * Lanes and approaches in maple or synthetic, laid end to end to any length, two blocks a lane,
 * that mark themselves from their place along it (the foul line, dots, arrows, range finders,
 * pin deck and pit) and raise bumpers on a sneak-click; the pins, knocked down by a click; the
 * pinsetter and masking unit; the ball return; the settee and its curved corner; the scorer's
 * console; the overhead scoring monitors; the house ball rack; the shoe counter, cubbies and
 * shoe spray (see {@code VENUES_SYSTEM.md}).
 *
 * @version 1.0
 * @since 2026.10
 */
@CsmTab.Load(order = 39)
public class CsmTabVenuesBowling extends CsmTab {

  @Override
  public String getTabId() {
    return "tabvenuesbowling";
  }

  @Override
  public Block getTabIcon() {
    Block icon = CsmRegistry.getBlock("bowling_pins");
    if (icon == null) {
      icon = CsmRegistry.getBlock("bowling_lane");
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
    // --- Sports & Venues - Bowling (gen_venues_bowling.py --fragments) ---
    initTabBlock(new BlockBowlingLane("bowling_lane", BlockBowlingLane.Kind.LANE, false));
    initTabBlock(new BlockBowlingLane("bowling_lane_synthetic", BlockBowlingLane.Kind.LANE, true));
    initTabBlock(new BlockBowlingLane("bowling_approach", BlockBowlingLane.Kind.APPROACH, false));
    initTabBlock(new BlockBowlingLane("bowling_approach_synthetic", BlockBowlingLane.Kind.APPROACH, true));
    initTabBlock(new BlockBowlingPins("bowling_pins"));
    initTabBlock(new BlockBowlingPinsetter("bowling_pinsetter", new int[]{0, 0, 1, 1, 0, 1, 2, 0, 1, 3, 0, 1, 0, 0, 2, 1, 0, 2, 2, 0, 2, 3, 0, 2, 0, 1, -1, 1, 1, -1, 2, 1, -1, 3, 1, -1, 0, 1, 0, 1, 1, 0, 2, 1, 0, 3, 1, 0, 0, 1, 1, 1, 1, 1, 2, 1, 1, 3, 1, 1, 0, 1, 2, 1, 1, 2, 2, 1, 2, 3, 1, 2}, new int[]{0, 0, 12, 64, 35, 44, 0, 16, -13, 64, 33, 12}));
    initTabBlock(new BlockVenueUnit("bowling_ball_return", new int[]{1, 0, 0}, new int[]{10, 0, 0, 22, 13, 16}));
    initTabBlock(new BlockBowlingSettee("bowling_settee", false));
    initTabBlock(new BlockBowlingSettee("bowling_settee_corner", true));
    initTabBlock(new BlockBowlingFixture("bowling_scorer_console", BlockBowlingFixture.Kind.CONSOLE));
    initTabBlock(new BlockBowlingMonitor("bowling_scoring_monitor", new int[]{1, 0, 0, 2, 0, 0, 3, 0, 0, 0, 1, 0, 1, 1, 0, 2, 1, 0, 3, 1, 0}, new int[]{1, 6, 6, 63, 32, 9}));
    initTabBlock(new BlockArenaRun("bowling_ball_rack", BlockArenaRun.Kind.BALL_RACK));
    initTabBlock(new BlockArenaRun("bowling_shoe_counter", BlockArenaRun.Kind.SHOE_COUNTER));
    initTabBlock(new BlockBowlingFixture("bowling_shoe_cubbies", BlockBowlingFixture.Kind.CUBBIES));
    initTabBlock(new BlockBowlingFixture("bowling_shoe_spray", BlockBowlingFixture.Kind.SPRAY));
  }
}

package com.micatechnologies.minecraft.csm.tabs;

import com.micatechnologies.minecraft.csm.CsmRegistry;
import com.micatechnologies.minecraft.csm.codeutils.CsmTab;
import com.micatechnologies.minecraft.csm.venues.arena.BlockArenaFloor;
import com.micatechnologies.minecraft.csm.venues.arena.BlockArenaRun;
import com.micatechnologies.minecraft.csm.venues.ballpark.BallparkLayouts;
import com.micatechnologies.minecraft.csm.venues.ballpark.BlockBallparkAnchor;
import com.micatechnologies.minecraft.csm.venues.ballpark.BlockBallparkFixture;
import com.micatechnologies.minecraft.csm.venues.ballpark.BlockBallparkNet;
import com.micatechnologies.minecraft.csm.venues.ballpark.BlockBallparkPainted;
import com.micatechnologies.minecraft.csm.venues.ballpark.BlockBallparkScoreboard;
import com.micatechnologies.minecraft.csm.venues.ballpark.BlockFoulPole;
import com.micatechnologies.minecraft.csm.venues.ballpark.BlockMoundSlope;
import com.micatechnologies.minecraft.csm.venues.ballpark.BlockOutfieldWall;
import com.micatechnologies.minecraft.csm.venues.ballpark.BlockOutfieldWallMarker;
import com.micatechnologies.minecraft.csm.venues.ballpark.BlockPitchersMound;
import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;

/**
 * The Sports &amp; Venues module's Ballpark tab: baseball and softball. The field's surfaces
 * (turf in two mowing tones, infield clay, packing clay, the warning track), the edges and chalk
 * lines that run at 45 degrees across a diamond laid along the grid, the marking anchors and the
 * painted cells that draw the home plate area, base cutouts, coach's boxes, on-deck circles,
 * the infield arc and softball's circle round them, the pitcher's mound, home plate, the bases
 * and the rubber; the dugout's bench, rail, canopy, racks and water cooler; backstop and batting
 * cage nets; the outfield wall with its distance markers and the foul pole; the line score
 * scoreboard and the count board; and a pitching machine. Bleachers, the press box and light
 * towers are in Seating &amp; Venue (see {@code VENUES_SYSTEM.md}).
 *
 * @version 1.0
 * @since 2026.10
 */
@CsmTab.Load(order = 37)
public class CsmTabVenuesBallpark extends CsmTab {

  @Override
  public String getTabId() {
    return "tabvenuesballpark";
  }

  @Override
  public Block getTabIcon() {
    Block icon = CsmRegistry.getBlock("baseball_base");
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
    // --- Sports & Venues - Ballpark (gen_venues_ballpark.py --fragments) ---
    initTabBlock(new BlockArenaFloor("ballpark_turf", 3, BlockArenaFloor.Orient.FACING, BlockArenaFloor.Surface.TURF));
    initTabBlock(new BlockArenaFloor("ballpark_clay", 4, BlockArenaFloor.Orient.FACING, BlockArenaFloor.Surface.CLAY));
    initTabBlock(new BlockArenaFloor("ballpark_field_edge", 4, BlockArenaFloor.Orient.FACING, BlockArenaFloor.Surface.TURF));
    initTabBlock(new BlockArenaFloor("ballpark_track_edge", 4, BlockArenaFloor.Orient.FACING, BlockArenaFloor.Surface.TURF));
    initTabBlock(new BlockArenaFloor("ballpark_chalk_turf", 4, BlockArenaFloor.Orient.FACING, BlockArenaFloor.Surface.TURF));
    initTabBlock(new BlockArenaFloor("ballpark_chalk_clay", 4, BlockArenaFloor.Orient.FACING, BlockArenaFloor.Surface.CLAY));
    initTabBlock(new BlockBallparkAnchor("ballpark_marking_anchor", BallparkLayouts.FIELD, 0, false, 4));
    initTabBlock(new BlockBallparkAnchor("ballpark_marking_anchor_third", BallparkLayouts.FIELD, 0, true, 4));
    initTabBlock(new BlockBallparkAnchor("ballpark_arc_anchor", BallparkLayouts.FIELD, 1, false, 3));
    initTabBlock(new BlockBallparkAnchor("ballpark_arc_anchor_third", BallparkLayouts.FIELD, 1, true, 3));
    initTabBlock(new BlockBallparkAnchor("softball_marking_anchor", BallparkLayouts.FIELD, 2, false, 4));
    initTabBlock(new BlockBallparkAnchor("softball_marking_anchor_third", BallparkLayouts.FIELD, 2, true, 4));
    initTabBlock(new BlockBallparkPainted("ballpark_painted", BallparkLayouts.FIELD));
    initTabBlock(new BlockPitchersMound("pitchers_mound"));
    initTabBlock(new BlockMoundSlope("pitchers_mound_slope", BallparkLayouts.MOUND, BallparkLayouts.MOUND_HEIGHT));
    initTabBlock(new BlockBallparkFixture("home_plate", BlockBallparkFixture.Kind.HOME_PLATE));
    initTabBlock(new BlockBallparkFixture("baseball_base", BlockBallparkFixture.Kind.BASE));
    initTabBlock(new BlockBallparkFixture("softball_base", BlockBallparkFixture.Kind.BASE));
    initTabBlock(new BlockBallparkFixture("pitchers_rubber", BlockBallparkFixture.Kind.RUBBER));
    initTabBlock(new BlockArenaRun("dugout_bench", BlockArenaRun.Kind.DUGOUT_BENCH));
    initTabBlock(new BlockArenaRun("dugout_rail", BlockArenaRun.Kind.DUGOUT_RAIL));
    initTabBlock(new BlockArenaRun("dugout_canopy", BlockArenaRun.Kind.DUGOUT_CANOPY));
    initTabBlock(new BlockBallparkFixture("dugout_helmet_rack", BlockBallparkFixture.Kind.HELMET_RACK));
    initTabBlock(new BlockBallparkFixture("dugout_bat_rack", BlockBallparkFixture.Kind.BAT_RACK));
    initTabBlock(new BlockBallparkFixture("dugout_water_cooler", BlockBallparkFixture.Kind.WATER_COOLER));
    initTabBlock(new BlockBallparkNet("ballpark_net", false));
    initTabBlock(new BlockBallparkNet("ballpark_net_diagonal", true));
    initTabBlock(new BlockBallparkFixture("ballpark_net_roof", BlockBallparkFixture.Kind.NET_ROOF));
    initTabBlock(new BlockOutfieldWall("outfield_wall", false));
    initTabBlock(new BlockOutfieldWall("outfield_wall_diagonal", true));
    initTabBlock(new BlockOutfieldWallMarker("outfield_wall_marker"));
    initTabBlock(new BlockFoulPole("foul_pole"));
    initTabBlock(new BlockBallparkScoreboard("ballpark_scoreboard", false, new int[]{9, 0, 0, 0, 1, 0, 9, 1, 0, 0, 2, 0, 9, 2, 0, -1, 3, 0, 0, 3, 0, 1, 3, 0, 2, 3, 0, 3, 3, 0, 4, 3, 0, 5, 3, 0, 6, 3, 0, 7, 3, 0, 8, 3, 0, 9, 3, 0, 10, 3, 0, -1, 4, 0, 0, 4, 0, 1, 4, 0, 2, 4, 0, 3, 4, 0, 4, 4, 0, 5, 4, 0, 6, 4, 0, 7, 4, 0, 8, 4, 0, 9, 4, 0, 10, 4, 0, -1, 5, 0, 0, 5, 0, 1, 5, 0, 2, 5, 0, 3, 5, 0, 4, 5, 0, 5, 5, 0, 6, 5, 0, 7, 5, 0, 8, 5, 0, 9, 5, 0, 10, 5, 0, -1, 6, 0, 0, 6, 0, 1, 6, 0, 2, 6, 0, 3, 6, 0, 4, 6, 0, 5, 6, 0, 6, 6, 0, 7, 6, 0, 8, 6, 0, 9, 6, 0, 10, 6, 0, -1, 7, 0, 0, 7, 0, 1, 7, 0, 2, 7, 0, 3, 7, 0, 4, 7, 0, 5, 7, 0, 6, 7, 0, 7, 7, 0, 8, 7, 0, 9, 7, 0, 10, 7, 0}, new int[]{-16, 48, 2, 176, 128, 14, 5, 0, 5, 11, 48, 11, 149, 0, 5, 155, 48, 11}));
    initTabBlock(new BlockBallparkScoreboard("ballpark_count_board", true, new int[]{1, 0, 0, 2, 0, 0, 0, 1, 0, 1, 1, 0, 2, 1, 0}, new int[]{0, 0, 10, 48, 32, 16}));
    initTabBlock(new BlockBallparkFixture("pitching_machine", BlockBallparkFixture.Kind.PITCHING_MACHINE));
  }
}

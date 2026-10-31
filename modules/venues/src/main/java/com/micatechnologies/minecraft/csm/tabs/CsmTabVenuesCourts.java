package com.micatechnologies.minecraft.csm.tabs;

import com.micatechnologies.minecraft.csm.CsmRegistry;
import com.micatechnologies.minecraft.csm.codeutils.CsmTab;
import com.micatechnologies.minecraft.csm.venues.BlockVenueUnit;
import com.micatechnologies.minecraft.csm.venues.arena.BlockArenaFloorBase;
import com.micatechnologies.minecraft.csm.venues.arena.BlockArenaRun;
import com.micatechnologies.minecraft.csm.venues.courts.BlockCourtBoard;
import com.micatechnologies.minecraft.csm.venues.courts.BlockCourtFence;
import com.micatechnologies.minecraft.csm.venues.courts.BlockCourtNet;
import com.micatechnologies.minecraft.csm.venues.courts.BlockSportFixture;
import com.micatechnologies.minecraft.csm.venues.lines.BlockLineAnchor;
import com.micatechnologies.minecraft.csm.venues.lines.BlockLinePainted;
import com.micatechnologies.minecraft.csm.venues.lines.BlockSportSurface;
import com.micatechnologies.minecraft.csm.venues.lines.LineArtFamily;
import com.micatechnologies.minecraft.csm.venues.pool.BlockVenueSign;
import com.micatechnologies.minecraft.csm.venues.pool.BlockPoolStartingBlock;
import com.micatechnologies.minecraft.csm.venues.pool.BlockPaceClock;
import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;

/**
 * The Sports &amp; Venues module's Courts, Track &amp; Pool tab. Outdoor hard courts in acrylic
 * (tennis, pickleball on its own or over tennis, basketball full and half court, volleyball
 * indoors and on sand) and a World Athletics 400 m track with its field events, every line drawn
 * where it really falls by the line art engine ({@code venues.lines}): an anchor and painted
 * cells that bake their piece of the court as quads, plain surfaces in sixteen colours, sand;
 * the nets, umpire chair, players' bench, chain-link fence and windscreen, divider curtain,
 * ball machine, courtside scoreboard and pole-mounted hoop; hurdles, starting blocks, the finish
 * camera tower, the lap counter and bell, the high jump and pole vault landing areas with their
 * standards, the shot put toe board, the discus and hammer cage and the steeplechase barrier.
 * A competition pool: tile, deck and gutter, the floor's lane lines by the same engine (a third
 * family), the end walls' targets and touch pads, lane ropes, backstroke flags, numbered starting
 * blocks, a lifeguard chair, ladders, springboards and a platform, a pace clock and the
 * scoreboard. Golf and the skate park have tabs of their own (see {@code VENUES_SYSTEM.md}).
 *
 * @version 1.0
 * @since 2026.10
 */
@CsmTab.Load(order = 40)
public class CsmTabVenuesCourts extends CsmTab {

  @Override
  public String getTabId() {
    return "tabvenuescourts";
  }

  @Override
  public Block getTabIcon() {
    Block icon = CsmRegistry.getBlock("tennis_net");
    if (icon == null) {
      icon = CsmRegistry.getBlock("court_acrylic");
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
    // --- Sports & Venues - Courts, Track & Pool (gen_venues_courts.py --fragments) ---
    initTabBlock(new BlockSportSurface("court_acrylic", BlockArenaFloorBase.Surface.ACRYLIC, true));
    initTabBlock(new BlockLineAnchor("court_acrylic_anchor", BlockArenaFloorBase.Surface.ACRYLIC, LineArtFamily.COURTS));
    initTabBlock(new BlockLinePainted("court_acrylic_painted", BlockArenaFloorBase.Surface.ACRYLIC, LineArtFamily.COURTS));
    initTabBlock(new BlockSportSurface("sports_sand", BlockArenaFloorBase.Surface.SAND, false));
    initTabBlock(new BlockCourtNet("tennis_net", BlockCourtNet.Kind.TENNIS));
    initTabBlock(new BlockCourtNet("pickleball_net", BlockCourtNet.Kind.PICKLEBALL));
    initTabBlock(new BlockCourtNet("volleyball_net", BlockCourtNet.Kind.VOLLEYBALL));
    initTabBlock(new BlockVenueUnit("tennis_umpire_chair", new int[]{0, 1, 0}, new int[]{1, 0, 2, 15, 31, 15}));
    initTabBlock(new BlockArenaRun("court_players_bench", BlockArenaRun.Kind.COURT_BENCH));
    initTabBlock(new BlockSportFixture("tennis_ball_machine", BlockSportFixture.Kind.BALL_MACHINE));
    initTabBlock(new BlockCourtFence("court_fence", false));
    initTabBlock(new BlockCourtFence("court_divider_curtain", true));
    initTabBlock(new BlockCourtBoard("court_scoreboard", false, new int[]{-2, 0, 0, -2, 1, 0, -1, 1, 0, 0, 1, 0, -2, 2, 0, -1, 2, 0, 0, 2, 0}, new int[]{9, 0, 7, 12, 16, 10, -28, 0, 7, -25, 16, 10, -33, 16, 6, 17, 40, 10}));
    initTabBlock(new BlockVenueUnit("basketball_hoop_pole", new int[]{0, 1, 0, 0, 2, -2, 0, 2, -1, 0, 2, 0, -1, 3, -2, 0, 3, -2, 1, 3, -2, 0, 3, -1, 0, 3, 0}, new int[]{5, 0, 5, 11, 57, 11, 6, 40, -27, 10, 57, 8, -6, 46, -28, 22, 64, -25}));
    // --- (gen_venues_track.py --fragments) ---
    initTabBlock(new BlockSportSurface("track_surface", BlockArenaFloorBase.Surface.POLYURETHANE, true));
    initTabBlock(new BlockLineAnchor("track_marking_anchor", BlockArenaFloorBase.Surface.POLYURETHANE, LineArtFamily.TRACK));
    initTabBlock(new BlockLinePainted("track_painted", BlockArenaFloorBase.Surface.POLYURETHANE, LineArtFamily.TRACK));
    initTabBlock(new BlockSportFixture("track_hurdle", BlockSportFixture.Kind.HURDLE));
    initTabBlock(new BlockSportFixture("starting_block", BlockSportFixture.Kind.STARTING_BLOCK));
    initTabBlock(new BlockSportFixture("shot_put_toe_board", BlockSportFixture.Kind.TOE_BOARD));
    initTabBlock(new BlockVenueUnit("track_timing_tower", new int[]{0, 1, 0, 0, 2, 0, 0, 3, 0}, new int[]{6, 0, 6, 10, 63, 10}));
    initTabBlock(new BlockCourtBoard("track_lap_counter", true, new int[]{0, 1, 0}, new int[]{7, 0, 7, 9, 20, 9, 0, 20, 6, 16, 32, 11}));
    initTabBlock(new BlockVenueUnit("high_jump_mat", new int[]{-2, 0, -1, 2, 0, -1, -3, 0, 0, -2, 0, 0, -1, 0, 0, 1, 0, 0, 2, 0, 0, 3, 0, 0, -3, 0, 1, -2, 0, 1, -1, 0, 1, 0, 0, 1, 1, 0, 1, 2, 0, 1, 3, 0, 1, -3, 0, 2, -2, 0, 2, -1, 0, 2, 0, 0, 2, 1, 0, 2, 2, 0, 2, 3, 0, 2, -3, 0, 3, -2, 0, 3, -1, 0, 3, 0, 0, 3, 1, 0, 3, 2, 0, 3, 3, 0, 3, -2, 1, -1, 2, 1, -1, -2, 2, -1, 2, 2, -1}, new int[]{-40, 0, 0, 56, 11, 64, -26, 0, -6, -22, 42, -2, 38, 0, -6, 42, 42, -2}));
    initTabBlock(new BlockVenueUnit("pole_vault_pit", new int[]{-2, 0, -2, -1, 0, -2, 1, 0, -2, 2, 0, -2, -2, 0, -1, -1, 0, -1, 1, 0, -1, 2, 0, -1, -2, 0, 0, -1, 0, 0, 1, 0, 0, 2, 0, 0, -2, 0, 1, -1, 0, 1, 0, 0, 1, 1, 0, 1, 2, 0, 1, -2, 0, 2, -1, 0, 2, 0, 0, 2, 1, 0, 2, 2, 0, 2, -2, 0, 3, -1, 0, 3, 0, 0, 3, 1, 0, 3, 2, 0, 3, -2, 0, 4, -1, 0, 4, 0, 0, 4, 1, 0, 4, 2, 0, 4, -2, 0, 5, -1, 0, 5, 0, 0, 5, 1, 0, 5, 2, 0, 5, -2, 1, 0, 2, 1, 0, -2, 2, 0, 2, 2, 0, -2, 3, 0, 2, 3, 0, -2, 4, 0, 2, 4, 0, -2, 5, 0, 2, 5, 0, -2, 6, 0, 2, 6, 0}, new int[]{-24, 0, 0, 40, 13, 96, -24, 0, -32, -2, 13, 0, 18, 0, -32, 40, 13, 0, -28, 0, 6, -24, 104, 10, 40, 0, 6, 44, 104, 10}));
    initTabBlock(new BlockVenueUnit("throwing_cage", new int[]{-3, 0, -7, 3, 0, -7, -4, 0, -6, -3, 0, -6, 3, 0, -6, 4, 0, -6, -4, 0, -5, 4, 0, -5, -4, 0, -4, 4, 0, -4, -4, 0, -3, 4, 0, -3, -4, 0, -2, -3, 0, -2, 3, 0, -2, 4, 0, -2, -3, 0, -1, -2, 0, -1, 2, 0, -1, 3, 0, -1, -2, 0, 0, -1, 0, 0, 1, 0, 0, 2, 0, 0, -3, 1, -7, 3, 1, -7, -4, 1, -6, -3, 1, -6, 3, 1, -6, 4, 1, -6, -4, 1, -5, 4, 1, -5, -4, 1, -4, 4, 1, -4, -4, 1, -3, 4, 1, -3, -4, 1, -2, -3, 1, -2, 3, 1, -2, 4, 1, -2, -3, 1, -1, -2, 1, -1, 2, 1, -1, 3, 1, -1, -2, 1, 0, -1, 1, 0, 0, 1, 0, 1, 1, 0, 2, 1, 0, -3, 2, -7, 3, 2, -7, -4, 2, -6, -3, 2, -6, 3, 2, -6, 4, 2, -6, -4, 2, -5, 4, 2, -5, -4, 2, -4, 4, 2, -4, -4, 2, -3, 4, 2, -3, -4, 2, -2, -3, 2, -2, 3, 2, -2, 4, 2, -2, -3, 2, -1, -2, 2, -1, 2, 2, -1, 3, 2, -1, -2, 2, 0, -1, 2, 0, 0, 2, 0, 1, 2, 0, 2, 2, 0, -3, 3, -7, 3, 3, -7, -4, 3, -6, -3, 3, -6, 3, 3, -6, 4, 3, -6, -4, 3, -5, 4, 3, -5, -4, 3, -4, 4, 3, -4, -4, 3, -3, 4, 3, -3, -4, 3, -2, -3, 3, -2, 3, 3, -2, 4, 3, -2, -3, 3, -1, -2, 3, -1, 2, 3, -1, 3, 3, -1, -2, 3, 0, -1, 3, 0, 0, 3, 0, 1, 3, 0, 2, 3, 0, -3, 4, -7, 3, 4, -7, -4, 4, -6, -3, 4, -6, 3, 4, -6, 4, 4, -6, -4, 4, -5, 4, 4, -5, -4, 4, -4, 4, 4, -4, -4, 4, -3, 4, 4, -3, -4, 4, -2, -3, 4, -2, 3, 4, -2, 4, 4, -2, -3, 4, -1, -2, 4, -1, 2, 4, -1, 3, 4, -1, -2, 4, 0, -1, 4, 0, 0, 4, 0, 1, 4, 0, 2, 4, 0, -3, 5, -7, 3, 5, -7, -4, 5, -6, -3, 5, -6, 3, 5, -6, 4, 5, -6, -4, 5, -5, 4, 5, -5, -4, 5, -4, 4, 5, -4, -4, 5, -3, 4, 5, -3, -4, 5, -2, -3, 5, -2, 3, 5, -2, 4, 5, -2, -3, 5, -1, -2, 5, -1, 2, 5, -1, 3, 5, -1, -2, 5, 0, -1, 5, 0, 0, 5, 0, 1, 5, 0, 2, 5, 0, -3, 6, -7, 3, 6, -7, -4, 6, -6, -3, 6, -6, 3, 6, -6, 4, 6, -6, -4, 6, -5, 4, 6, -5, -4, 6, -4, 4, 6, -4, -4, 6, -3, 4, 6, -3, -4, 6, -2, -3, 6, -2, 3, 6, -2, 4, 6, -2, -3, 6, -1, -2, 6, -1, 2, 6, -1, 3, 6, -1, -2, 6, 0, -1, 6, 0, 0, 6, 0, 1, 6, 0, 2, 6, 0}, new int[]{54, 0, -106, 58, 112, -102, 54, 0, -90, 58, 112, -86, 70, 0, -90, 74, 112, -86, 70, 0, -74, 74, 112, -70, 70, 0, -58, 74, 112, -54, 70, 0, -42, 74, 112, -38, 70, 0, -26, 74, 112, -22, 54, 0, -26, 58, 112, -22, 54, 0, -10, 58, 112, -6, 38, 0, -10, 42, 112, -6, 38, 0, 6, 42, 112, 10, 22, 0, 6, 26, 112, 10, 6, 0, 6, 10, 112, 10, -10, 0, 6, -6, 112, 10, -26, 0, 6, -22, 112, 10, -26, 0, -10, -22, 112, -6, -42, 0, -10, -38, 112, -6, -42, 0, -26, -38, 112, -22, -58, 0, -26, -54, 112, -22, -58, 0, -42, -54, 112, -38, -58, 0, -58, -54, 112, -54, -58, 0, -74, -54, 112, -70, -58, 0, -90, -54, 112, -86, -42, 0, -90, -38, 112, -86, -42, 0, -106, -38, 112, -102}));
    initTabBlock(new BlockVenueUnit("steeplechase_barrier", new int[]{1, 0, 0, 2, 0, 0, 3, 0, 0}, new int[]{0, 0, 6, 64, 15, 10}));
    // --- Sports & Venues - Courts, Track & Pool (gen_venues_pool.py --fragments) ---
    initTabBlock(new BlockSportSurface("pool_tile", BlockArenaFloorBase.Surface.TILE, true, 15));
    initTabBlock(new BlockSportSurface("pool_deck", BlockArenaFloorBase.Surface.TILE, true, 8));
    initTabBlock(new BlockSportSurface("pool_gutter", BlockArenaFloorBase.Surface.TILE, false));
    initTabBlock(new BlockLineAnchor("pool_floor_anchor", BlockArenaFloorBase.Surface.TILE, LineArtFamily.POOL));
    initTabBlock(new BlockLinePainted("pool_floor_painted", BlockArenaFloorBase.Surface.TILE, LineArtFamily.POOL));
    initTabBlock(new BlockSportFixture("pool_wall_target", BlockSportFixture.Kind.WALL_TARGET));
    initTabBlock(new BlockSportFixture("pool_touchpad", BlockSportFixture.Kind.TOUCHPAD));
    initTabBlock(new BlockSportFixture("pool_lane_rope", BlockSportFixture.Kind.LANE_ROPE));
    initTabBlock(new BlockArenaRun("pool_backstroke_flags", BlockArenaRun.Kind.BACKSTROKE_FLAGS));
    initTabBlock(new BlockPoolStartingBlock("pool_starting_block"));
    initTabBlock(new BlockSportFixture("pool_ladder", BlockSportFixture.Kind.POOL_LADDER));
    initTabBlock(new BlockVenueUnit("pool_lifeguard_chair", new int[]{0, 1, 0, 0, 2, 0, 0, 3, 0}, new int[]{1, 0, 0, 15, 31, 14, 1, 31, 10, 15, 46, 14, 7, 46, 7, 9, 62, 9}));
    initTabBlock(new BlockVenueUnit("pool_springboard_1m", new int[]{0, 0, -2, 0, 0, -1, 0, 0, 1, 0, 0, 2, 0, 0, 3}, new int[]{4, 12, -24, 12, 14, 54, 3, 0, 10, 13, 12, 18, 4, 0, 46, 12, 12, 52}));
    initTabBlock(new BlockVenueUnit("pool_springboard_3m", new int[]{0, 0, 1, 0, 0, 2, 0, 0, 3, 0, 1, 1, 0, 1, 2, 0, 1, 3, 0, 2, -2, 0, 2, -1, 0, 2, 0, 0, 2, 1, 0, 2, 2, 0, 2, 3, 0, 3, 3}, new int[]{4, 44, -24, 12, 46, 54, 2, 0, 18, 14, 44, 54, 3, 0, 54, 13, 60, 58}));
    initTabBlock(new BlockVenueUnit("pool_dive_platform", new int[]{0, 0, 1, 0, 1, 0, 0, 1, 1, 0, 2, 0, 0, 2, 1, 0, 3, 0, 0, 3, 1, -1, 4, -2, 0, 4, -2, 1, 4, -2, -1, 4, -1, 0, 4, -1, 1, 4, -1, -1, 4, 0, 0, 4, 0, 1, 4, 0, -1, 4, 1, 0, 4, 1, 1, 4, 1, -1, 4, 2, 0, 4, 2, 1, 4, 2, -1, 4, 3, 0, 4, 3, 1, 4, 3}, new int[]{-8, 75, -36, 24, 78, 60, -4, 0, -2, 20, 75, 36, 3, 0, 60, 13, 92, 65}));
    initTabBlock(new BlockPaceClock("pool_pace_clock"));
    initTabBlock(new BlockVenueSign("pool_scoreboard", BlockVenueSign.Style.POOL_BOARD, new int[]{3, 0, 0, 0, 1, 0, 1, 1, 0, 2, 1, 0, 3, 1, 0, 0, 2, 0, 1, 2, 0, 2, 2, 0, 3, 2, 0}, new int[]{7, 0, 7, 9, 16, 9, 55, 0, 7, 57, 16, 9, 0, 16, 5, 64, 48, 10}));
  }
}

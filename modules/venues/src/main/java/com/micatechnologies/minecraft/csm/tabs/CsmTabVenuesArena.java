package com.micatechnologies.minecraft.csm.tabs;

import com.micatechnologies.minecraft.csm.CsmRegistry;
import com.micatechnologies.minecraft.csm.codeutils.CsmTab;
import com.micatechnologies.minecraft.csm.venues.BlockVenueUnit;
import com.micatechnologies.minecraft.csm.venues.arena.ArenaFloorLayouts;
import com.micatechnologies.minecraft.csm.venues.arena.BlockArenaFloor;
import com.micatechnologies.minecraft.csm.venues.arena.BlockArenaFloorAnchor;
import com.micatechnologies.minecraft.csm.venues.arena.BlockArenaFloorPainted;
import com.micatechnologies.minecraft.csm.venues.arena.BlockArenaRun;
import com.micatechnologies.minecraft.csm.venues.arena.BlockDasherCorner;
import com.micatechnologies.minecraft.csm.venues.arena.BlockDasherCurve;
import com.micatechnologies.minecraft.csm.venues.arena.BlockDasherDoor;
import com.micatechnologies.minecraft.csm.venues.arena.BlockDasherStraight;
import com.micatechnologies.minecraft.csm.venues.av.BlockLedRibbon;
import com.micatechnologies.minecraft.csm.venues.av.BlockLedRibbonCorner;
import com.micatechnologies.minecraft.csm.venues.av.BlockLedRibbonText;
import com.micatechnologies.minecraft.csm.venues.av.BlockScoreboard;
import com.micatechnologies.minecraft.csm.venues.av.BlockVenueBanner;
import com.micatechnologies.minecraft.csm.venues.av.BlockVenueCatwalk;
import com.micatechnologies.minecraft.csm.venues.av.ScoreboardSize;
import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;

/**
 * The Sports &amp; Venues module's Arena tab: an arena's event floor. The rink (ice and its
 * paints, the ice lines, the painted circles, crease and spots round their anchors, rubber
 * matting), the dasher boards and glass (straights, 45 degree corners, the true curve of a
 * 61 x 26 rink's corner, the player bench gate and penalty box door), the hockey goal, the
 * benches and the scorer's table, the ice resurfacer and the ice plant; the basketball court
 * (maple in four finishes, its lines, the keys and centre circle round their anchors) and the
 * portable basketball stanchion; then the arena's scoreboard and AV: the centre-hung scoreboard
 * in two sizes, the LED ribbon board (straights, 45 degree corners and the scrolling-text piece),
 * championship and retired-number banners in two sizes, the line-array speaker cluster, the
 * broadcast camera platform and a catwalk grating.
 *
 * @version 1.0
 * @since 2026.10
 */
@CsmTab.Load(order = 35)
public class CsmTabVenuesArena extends CsmTab {

  @Override
  public String getTabId() {
    return "tabvenuesarena";
  }

  @Override
  public Block getTabIcon() {
    Block icon = CsmRegistry.getBlock("hockey_goal");
    return icon != null ? icon : Blocks.PACKED_ICE;
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
    // --- Sports & Venues - Arena (gen_venues_arena.py --fragments) ---
    initTabBlock(new BlockArenaFloor("rink_ice", 4, BlockArenaFloor.Orient.NONE, BlockArenaFloor.Surface.ICE));
    initTabBlock(new BlockArenaFloor("rink_rubber_floor", 1, BlockArenaFloor.Orient.NONE, BlockArenaFloor.Surface.RUBBER));
    initTabBlock(new BlockArenaFloor("rink_ice_line", 3, BlockArenaFloor.Orient.AXIS, BlockArenaFloor.Surface.ICE));
    initTabBlock(new BlockArenaFloor("court_maple", 4, BlockArenaFloor.Orient.AXIS, BlockArenaFloor.Surface.WOOD));
    initTabBlock(new BlockArenaFloor("court_line", 3, BlockArenaFloor.Orient.FACING, BlockArenaFloor.Surface.WOOD));
    initTabBlock(new BlockArenaFloorAnchor("rink_ice_spot", ArenaFloorLayouts.ICE));
    initTabBlock(new BlockArenaFloorPainted("rink_ice_painted", ArenaFloorLayouts.ICE));
    initTabBlock(new BlockArenaFloorAnchor("court_floor_anchor", ArenaFloorLayouts.COURT));
    initTabBlock(new BlockArenaFloorPainted("court_painted", ArenaFloorLayouts.COURT));
    initTabBlock(new BlockDasherStraight("dasher_board", false));
    initTabBlock(new BlockDasherStraight("dasher_glass", true));
    initTabBlock(new BlockDasherCorner("dasher_board_corner", false));
    initTabBlock(new BlockDasherCorner("dasher_glass_corner", true));
    initTabBlock(new BlockDasherCurve("dasher_board_curve_nw", false, EnumFacing.NORTH));
    initTabBlock(new BlockDasherCurve("dasher_board_curve_ne", false, EnumFacing.EAST));
    initTabBlock(new BlockDasherCurve("dasher_board_curve_se", false, EnumFacing.SOUTH));
    initTabBlock(new BlockDasherCurve("dasher_board_curve_sw", false, EnumFacing.WEST));
    initTabBlock(new BlockDasherCurve("dasher_glass_curve_nw", true, EnumFacing.NORTH));
    initTabBlock(new BlockDasherCurve("dasher_glass_curve_ne", true, EnumFacing.EAST));
    initTabBlock(new BlockDasherCurve("dasher_glass_curve_se", true, EnumFacing.SOUTH));
    initTabBlock(new BlockDasherCurve("dasher_glass_curve_sw", true, EnumFacing.WEST));
    initTabBlock(new BlockDasherDoor("dasher_bench_gate", false));
    initTabBlock(new BlockDasherDoor("dasher_penalty_door", true));
    initTabBlock(new BlockVenueUnit("hockey_goal", new int[]{1, 0, 0, 0, 0, 1, 1, 0, 1}, new int[]{0, 0, 7, 32, 20, 23}));
    initTabBlock(new BlockVenueUnit("basketball_stanchion", new int[]{0, 0, 1, 0, 0, 2, 0, 1, -1, 0, 1, 0, 0, 2, -2, 0, 2, -1, -1, 3, -3, 0, 3, -3, 1, 3, -3, 0, 3, -2, 0, 4, -3}, new int[]{0, 0, -2, 16, 16, 38, 6, 16, -8, 10, 32, 8, 6, 32, -24, 10, 51, -8, 7, 48, -35, 9, 53, -24, -7, 45, -36, 23, 64, -34, 4, 48, -44, 12, 49, -36, 3, 64, -36, 13, 70, -34}));
    initTabBlock(new BlockVenueUnit("ice_resurfacer", new int[]{0, 0, -1, 1, 0, -1, 1, 0, 0, 0, 0, 1, 1, 0, 1, 0, 1, -1, 1, 1, -1, 0, 1, 0, 1, 1, 0, 0, 1, 1, 1, 1, 1}, new int[]{0, 0, -16, 32, 29, 32}));
    initTabBlock(new BlockVenueUnit("ice_plant", new int[]{1, 0, 0, 0, 1, 0, 1, 1, 0}, new int[]{0, 0, 3, 32, 32, 16}));
    initTabBlock(new BlockArenaRun("player_bench", BlockArenaRun.Kind.PLAYER_BENCH));
    initTabBlock(new BlockArenaRun("penalty_bench", BlockArenaRun.Kind.PENALTY_BENCH));
    initTabBlock(new BlockArenaRun("scorers_table", BlockArenaRun.Kind.SCORERS_TABLE));
    // --- Sports & Venues - Arena (gen_venues_av.py --fragments) ---
    initTabBlock(new BlockScoreboard("centre_hung_scoreboard", ScoreboardSize.LARGE));
    initTabBlock(new BlockScoreboard("centre_hung_scoreboard_small", ScoreboardSize.SMALL));
    initTabBlock(new BlockLedRibbon("led_ribbon_board"));
    initTabBlock(new BlockLedRibbonCorner("led_ribbon_board_corner"));
    initTabBlock(new BlockLedRibbonText("led_ribbon_board_text"));
    initTabBlock(new BlockVenueBanner("championship_banner", true, false, new int[]{0, -1, 0, 1, -1, 0, 1, 0, 0, 0, 1, 0, 1, 1, 0}, new int[]{0, -15, 7, 32, 32, 9}));
    initTabBlock(new BlockVenueBanner("championship_banner_small", false, false, new int[]{0, 1, 0}, new int[]{0, 1, 7, 16, 32, 9}));
    initTabBlock(new BlockVenueBanner("retired_number_banner", true, true, new int[]{0, -1, 0, 1, -1, 0, 1, 0, 0, 0, 1, 0, 1, 1, 0}, new int[]{0, -15, 7, 32, 32, 9}));
    initTabBlock(new BlockVenueBanner("retired_number_banner_small", false, true, new int[]{0, 1, 0}, new int[]{0, 1, 7, 16, 32, 9}));
    initTabBlock(new BlockVenueUnit("line_array_speaker", true, new int[]{0, -1, 0, 0, 1, 0}, new int[]{1, -15, 2, 15, 32, 14}));
    initTabBlock(new BlockVenueUnit("broadcast_camera_platform", new int[]{1, 0, 0, 0, 0, 1, 1, 0, 1, 0, 1, 0, 1, 1, 0, 0, 1, 1, 1, 1, 1}, new int[]{0, 0, 0, 32, 14, 32, 8, 14, 6, 24, 30, 26}));
    initTabBlock(new BlockVenueCatwalk());
  }
}

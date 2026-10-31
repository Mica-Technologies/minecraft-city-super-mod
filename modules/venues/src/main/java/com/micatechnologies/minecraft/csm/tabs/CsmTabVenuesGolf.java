package com.micatechnologies.minecraft.csm.tabs;

import com.micatechnologies.minecraft.csm.CsmRegistry;
import com.micatechnologies.minecraft.csm.codeutils.CsmTab;
import com.micatechnologies.minecraft.csm.venues.BlockVenueUnit;
import com.micatechnologies.minecraft.csm.venues.arena.BlockArenaFloorBase;
import com.micatechnologies.minecraft.csm.venues.arena.BlockArenaRun;
import com.micatechnologies.minecraft.csm.venues.courts.BlockSportFixture;
import com.micatechnologies.minecraft.csm.venues.golf.BlockEdgeRail;
import com.micatechnologies.minecraft.csm.venues.golf.BlockGolfTurf;
import com.micatechnologies.minecraft.csm.venues.lines.BlockSportSurface;
import com.micatechnologies.minecraft.csm.venues.pool.BlockVenueSign;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;

/**
 * The Sports &amp; Venues module's Golf tab: the course (turf mown in five cuts, its stripes from
 * each block's position; the cup, flagstick, bunker, tee markers, yardage markers, the tee sign,
 * ball washer, bench, cart path and curb), the driving range (mat, bay divider, ball dispenser,
 * bucket, distance flags) and mini-golf (carpet, cup, border, ramp, windmill, loop, scorecard
 * stand). Decor: no ball physics (see {@code VENUES_SYSTEM.md}).
 *
 * @version 1.0
 * @since 2026.10
 */
@CsmTab.Load(order = 41)
public class CsmTabVenuesGolf extends CsmTab {

  @Override
  public String getTabId() {
    return "tabvenuesgolf";
  }

  @Override
  public Block getTabIcon() {
    Block icon = CsmRegistry.getBlock("golf_flagstick");
    if (icon == null) {
      icon = CsmRegistry.getBlock("golf_turf");
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
    // --- Sports & Venues - Golf (gen_venues_golf.py --fragments) ---
    initTabBlock(new BlockGolfTurf("golf_turf"));
    initTabBlock(new BlockSportSurface("golf_bunker_sand", BlockArenaFloorBase.Surface.SAND, false));
    initTabBlock(new BlockSportSurface("golf_cart_path", BlockArenaFloorBase.Surface.CONCRETE, false));
    initTabBlock(new BlockSportFixture("golf_flagstick", BlockSportFixture.Kind.FLAGSTICK));
    initTabBlock(new BlockSportFixture("golf_tee_marker", BlockSportFixture.Kind.TEE_MARKER));
    initTabBlock(new BlockSportFixture("golf_yardage_marker", BlockSportFixture.Kind.YARDAGE_MARKER));
    initTabBlock(new BlockVenueSign("golf_hole_sign", BlockVenueSign.Style.GOLF_HOLE, new int[]{0, 1, 0}, new int[]{7, 0, 8, 9, 14, 10, 2, 14, 6, 14, 31, 9}));
    initTabBlock(new BlockSportFixture("golf_ball_washer", BlockSportFixture.Kind.BALL_WASHER));
    initTabBlock(new BlockArenaRun("golf_bench", BlockArenaRun.Kind.GOLF_BENCH));
    initTabBlock(new BlockEdgeRail("minigolf_border", Material.WOOD, SoundType.WOOD, 2, 3));
    initTabBlock(new BlockEdgeRail("golf_cart_path_curb", Material.ROCK, SoundType.STONE, 3, 2.5));
    initTabBlock(new BlockSportFixture("driving_range_mat", BlockSportFixture.Kind.RANGE_MAT));
    initTabBlock(new BlockSportFixture("range_tee_divider", BlockSportFixture.Kind.RANGE_DIVIDER));
    initTabBlock(new BlockSportFixture("range_ball_bucket", BlockSportFixture.Kind.RANGE_BUCKET));
    initTabBlock(new BlockSportFixture("range_target_flag", BlockSportFixture.Kind.RANGE_FLAG));
    initTabBlock(new BlockVenueUnit("range_ball_dispenser", new int[]{0, 1, 0}, new int[]{2, 0, 3, 14, 29, 14}));
    initTabBlock(new BlockSportSurface("minigolf_carpet", BlockArenaFloorBase.Surface.CARPET, true, 0));
    initTabBlock(new BlockSportSurface("minigolf_cup", BlockArenaFloorBase.Surface.CARPET, true, 0));
    initTabBlock(new BlockSportFixture("minigolf_ramp", BlockSportFixture.Kind.MINIGOLF_HUMP));
    initTabBlock(new BlockSportFixture("minigolf_scorecard_stand", BlockSportFixture.Kind.SCORECARD_STAND));
    initTabBlock(new BlockVenueUnit("minigolf_windmill", new int[]{0, 1, 0}, new int[]{1, 0, 1, 15, 36, 15}));
    initTabBlock(new BlockVenueUnit("minigolf_loop", new int[]{0, 1, 0}, new int[]{1, 0, 1, 15, 2, 15, 3, 20, 2, 13, 24, 14}));
  }
}

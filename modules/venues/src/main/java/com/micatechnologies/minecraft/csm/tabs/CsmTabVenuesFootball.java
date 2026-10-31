package com.micatechnologies.minecraft.csm.tabs;

import com.micatechnologies.minecraft.csm.CsmRegistry;
import com.micatechnologies.minecraft.csm.codeutils.CsmTab;
import com.micatechnologies.minecraft.csm.venues.BlockVenueUnit;
import com.micatechnologies.minecraft.csm.venues.arena.BlockArenaRun;
import com.micatechnologies.minecraft.csm.venues.field.BlockEndZoneLettering;
import com.micatechnologies.minecraft.csm.venues.field.BlockFieldAnchor;
import com.micatechnologies.minecraft.csm.venues.field.BlockFieldFixture;
import com.micatechnologies.minecraft.csm.venues.field.BlockFieldPainted;
import com.micatechnologies.minecraft.csm.venues.field.BlockFieldScoreboard;
import com.micatechnologies.minecraft.csm.venues.field.BlockFieldTurf;
import com.micatechnologies.minecraft.csm.venues.field.FieldLayouts;
import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;

/**
 * The Sports &amp; Venues module's Football &amp; Soccer tab: an American football field and a
 * soccer pitch at full size, one block a metre. The field's turf in two mowing tones, white
 * paint, the surround and the end zone's colour; the marking anchors and painted cells that draw
 * every line, hash mark, number and arc where it really falls, to the texel; the goals; the
 * sideline's pylons, benches, heater and fan, down box and chain set, corner flags, substitutes'
 * shelter and fourth official's board; the end zone lettering; and the football and soccer
 * scoreboards. Light towers and bleachers are in Seating &amp; Venue (see
 * {@code VENUES_SYSTEM.md}).
 *
 * @version 1.0
 * @since 2026.10
 */
@CsmTab.Load(order = 38)
public class CsmTabVenuesFootball extends CsmTab {

  @Override
  public String getTabId() {
    return "tabvenuesfootball";
  }

  @Override
  public Block getTabIcon() {
    Block icon = CsmRegistry.getBlock("football_goal_post");
    if (icon == null) {
      icon = CsmRegistry.getBlock("field_turf");
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
    // --- Sports & Venues - Football & Soccer (gen_venues_football.py --fragments) ---
    initTabBlock(new BlockFieldTurf("field_turf", BlockFieldTurf.Kind.TURF));
    initTabBlock(new BlockFieldTurf("football_end_zone", BlockFieldTurf.Kind.END_ZONE));
    initTabBlock(new BlockFieldAnchor("football_marking_anchor", FieldLayouts.FOOTBALL, 0, 8));
    initTabBlock(new BlockFieldAnchor("football_marking_anchor_2", FieldLayouts.FOOTBALL, 8, 7));
    initTabBlock(new BlockFieldPainted("football_painted", FieldLayouts.FOOTBALL));
    initTabBlock(new BlockFieldAnchor("soccer_marking_anchor", FieldLayouts.SOCCER, 0, 8));
    initTabBlock(new BlockFieldAnchor("soccer_marking_anchor_2", FieldLayouts.SOCCER, 8, 7));
    initTabBlock(new BlockFieldPainted("soccer_painted", FieldLayouts.SOCCER));
    initTabBlock(new BlockVenueUnit("football_goal_post", new int[]{0, 1, 0, 0, 2, -2, 0, 2, -1, 0, 2, 0, -3, 3, -2, -2, 3, -2, -1, 3, -2, 0, 3, -2, 1, 3, -2, 2, 3, -2, 3, 3, -2, 0, 3, -1, -3, 4, -2, 3, 4, -2, -3, 5, -2, 3, 5, -2, -3, 6, -2, 3, 6, -2, -3, 7, -2, 3, 7, -2, -3, 8, -2, 3, 8, -2, -3, 9, -2, 3, 9, -2, -3, 10, -2, 3, 10, -2, -3, 11, -2, 3, 11, -2, -3, 12, -2, 3, 12, -2, -3, 13, -2, 3, 13, -2}, new int[]{6, 0, 1, 10, 49, 5, 6, 32, -27, 10, 54, 5, -40, 48, -27, 56, 54, -23, -40, 49, -26, -37, 219, -23, 53, 49, -26, 56, 219, -23}));
    initTabBlock(new BlockVenueUnit("soccer_goal", new int[]{-4, 0, 0, -3, 0, 0, -2, 0, 0, -1, 0, 0, 1, 0, 0, 2, 0, 0, 3, 0, 0, -4, 0, 1, 3, 0, 1, -4, 0, 2, 3, 0, 2, -4, 1, 0, 3, 1, 0, -4, 1, 1, 3, 1, 1, -4, 1, 2, 3, 1, 2, -4, 2, 0, -3, 2, 0, -2, 2, 0, -1, 2, 0, 0, 2, 0, 1, 2, 0, 2, 2, 0, 3, 2, 0}, new int[]{-60, 0, 14, -59, 41, 16, 59, 0, 14, 60, 41, 16, -59, 39, 14, 59, 41, 16, -59, 0, 45, 59, 35, 48, -61, 0, 16, -59, 35, 45, 59, 0, 16, 61, 35, 45, -59, 0, 14, 59, 1, 16}));
    initTabBlock(new BlockVenueUnit("soccer_goal_small", new int[]{-2, 0, 0, 2, 0, 0, -2, 1, 0, -1, 1, 0, 0, 1, 0, 1, 1, 0, 2, 1, 0}, new int[]{-23, 0, 0, -21, 31, 2, 37, 0, 0, 39, 31, 2, -21, 29, 0, 37, 31, 2}));
    initTabBlock(new BlockFieldFixture("end_zone_pylon", BlockFieldFixture.Kind.PYLON));
    initTabBlock(new BlockFieldFixture("football_down_box", BlockFieldFixture.Kind.DOWN_BOX));
    initTabBlock(new BlockFieldFixture("football_chain_stake", BlockFieldFixture.Kind.CHAIN_STAKE));
    initTabBlock(new BlockFieldFixture("football_chain", BlockFieldFixture.Kind.CHAIN));
    initTabBlock(new BlockFieldFixture("sideline_heater", BlockFieldFixture.Kind.HEATER));
    initTabBlock(new BlockFieldFixture("sideline_fan", BlockFieldFixture.Kind.FAN));
    initTabBlock(new BlockFieldFixture("soccer_corner_flag", BlockFieldFixture.Kind.CORNER_FLAG));
    initTabBlock(new BlockFieldFixture("fourth_official_board", BlockFieldFixture.Kind.BOARD));
    initTabBlock(new BlockArenaRun("football_team_bench", BlockArenaRun.Kind.TEAM_BENCH));
    initTabBlock(new BlockArenaRun("soccer_substitutes_shelter", BlockArenaRun.Kind.SUB_SHELTER));
    initTabBlock(new BlockEndZoneLettering("football_end_zone_lettering"));
    initTabBlock(new BlockFieldScoreboard("football_scoreboard", false, new int[]{9, 0, 0, 0, 1, 0, 9, 1, 0, 0, 2, 0, 9, 2, 0, -1, 3, 0, 0, 3, 0, 1, 3, 0, 2, 3, 0, 3, 3, 0, 4, 3, 0, 5, 3, 0, 6, 3, 0, 7, 3, 0, 8, 3, 0, 9, 3, 0, 10, 3, 0, -1, 4, 0, 0, 4, 0, 1, 4, 0, 2, 4, 0, 3, 4, 0, 4, 4, 0, 5, 4, 0, 6, 4, 0, 7, 4, 0, 8, 4, 0, 9, 4, 0, 10, 4, 0, -1, 5, 0, 0, 5, 0, 1, 5, 0, 2, 5, 0, 3, 5, 0, 4, 5, 0, 5, 5, 0, 6, 5, 0, 7, 5, 0, 8, 5, 0, 9, 5, 0, 10, 5, 0, -1, 6, 0, 0, 6, 0, 1, 6, 0, 2, 6, 0, 3, 6, 0, 4, 6, 0, 5, 6, 0, 6, 6, 0, 7, 6, 0, 8, 6, 0, 9, 6, 0, 10, 6, 0, -1, 7, 0, 0, 7, 0, 1, 7, 0, 2, 7, 0, 3, 7, 0, 4, 7, 0, 5, 7, 0, 6, 7, 0, 7, 7, 0, 8, 7, 0, 9, 7, 0, 10, 7, 0}, new int[]{-16, 48, 2, 176, 128, 14, 5, 0, 5, 11, 48, 11, 149, 0, 5, 155, 48, 11}));
    initTabBlock(new BlockFieldScoreboard("soccer_scoreboard", true, new int[]{9, 0, 0, 0, 1, 0, 9, 1, 0, 0, 2, 0, 9, 2, 0, -1, 3, 0, 0, 3, 0, 1, 3, 0, 2, 3, 0, 3, 3, 0, 4, 3, 0, 5, 3, 0, 6, 3, 0, 7, 3, 0, 8, 3, 0, 9, 3, 0, 10, 3, 0, -1, 4, 0, 0, 4, 0, 1, 4, 0, 2, 4, 0, 3, 4, 0, 4, 4, 0, 5, 4, 0, 6, 4, 0, 7, 4, 0, 8, 4, 0, 9, 4, 0, 10, 4, 0, -1, 5, 0, 0, 5, 0, 1, 5, 0, 2, 5, 0, 3, 5, 0, 4, 5, 0, 5, 5, 0, 6, 5, 0, 7, 5, 0, 8, 5, 0, 9, 5, 0, 10, 5, 0, -1, 6, 0, 0, 6, 0, 1, 6, 0, 2, 6, 0, 3, 6, 0, 4, 6, 0, 5, 6, 0, 6, 6, 0, 7, 6, 0, 8, 6, 0, 9, 6, 0, 10, 6, 0, -1, 7, 0, 0, 7, 0, 1, 7, 0, 2, 7, 0, 3, 7, 0, 4, 7, 0, 5, 7, 0, 6, 7, 0, 7, 7, 0, 8, 7, 0, 9, 7, 0, 10, 7, 0}, new int[]{-16, 48, 2, 176, 128, 14, 5, 0, 5, 11, 48, 11, 149, 0, 5, 155, 48, 11}));
  }
}

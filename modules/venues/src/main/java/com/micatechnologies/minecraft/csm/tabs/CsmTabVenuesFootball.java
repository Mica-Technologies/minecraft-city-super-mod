package com.micatechnologies.minecraft.csm.tabs;

import com.micatechnologies.minecraft.csm.CsmRegistry;
import com.micatechnologies.minecraft.csm.codeutils.CsmTab;
import com.micatechnologies.minecraft.csm.venues.field.BlockFieldAnchor;
import com.micatechnologies.minecraft.csm.venues.field.BlockFieldPainted;
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
  }
}

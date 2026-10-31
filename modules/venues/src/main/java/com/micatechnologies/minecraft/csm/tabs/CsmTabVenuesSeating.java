package com.micatechnologies.minecraft.csm.tabs;

import com.micatechnologies.minecraft.csm.CsmRegistry;
import com.micatechnologies.minecraft.csm.codeutils.CsmTab;
import com.micatechnologies.minecraft.csm.venues.outdoor.BlockBleacher;
import com.micatechnologies.minecraft.csm.venues.outdoor.BlockBleacherFrame;
import com.micatechnologies.minecraft.csm.venues.outdoor.BlockLightHead;
import com.micatechnologies.minecraft.csm.venues.outdoor.BlockLightPole;
import com.micatechnologies.minecraft.csm.venues.outdoor.BlockPressBox;
import com.micatechnologies.minecraft.csm.venues.outdoor.BlockTicketBooth;
import com.micatechnologies.minecraft.csm.venues.seating.BlockStadiumAisleEnd;
import com.micatechnologies.minecraft.csm.venues.seating.BlockStadiumCompanionSeat;
import com.micatechnologies.minecraft.csm.venues.seating.BlockStadiumGlassGuard;
import com.micatechnologies.minecraft.csm.venues.seating.BlockStadiumHandrail;
import com.micatechnologies.minecraft.csm.venues.seating.BlockStadiumPortalSign;
import com.micatechnologies.minecraft.csm.venues.seating.BlockStadiumSeat;
import com.micatechnologies.minecraft.csm.venues.seating.BlockStadiumSectionSign;
import com.micatechnologies.minecraft.csm.venues.seating.BlockStadiumStepLight;
import com.micatechnologies.minecraft.csm.venues.seating.BlockStadiumWheelchairSpace;
import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;

/**
 * The Sports & Venues module's shared tab, Seating & Venue: what every venue has whatever its
 * sport. So far the seating bowl: stadium seats in ten colours, the aisle-end standards with
 * their row letters, the companion seat and wheelchair space, aisle step lights and handrails,
 * glass guards for tier and suite fronts, and the section and portal signs over the vomitories;
 * then the pieces every outdoor venue shares: aluminium bleachers and their understructure, the
 * press box, the ticket booth and the stadium light towers.
 *
 * <p>The module's other tabs follow it, one per kind of venue rather than one per piece:
 * Arena (ice, court, scoreboard and AV), Concourse, and then Ballpark, Football &amp; Soccer,
 * Bowling, and Courts, Track &amp; Pool (see {@code VENUES_SYSTEM.md}).</p>
 *
 * @version 1.0
 * @since 2026.10
 */
@CsmTab.Load(order = 34)
public class CsmTabVenuesSeating extends CsmTab {

  @Override
  public String getTabId() {
    return "tabvenuesseating";
  }

  @Override
  public Block getTabIcon() {
    Block icon = CsmRegistry.getBlock("stadium_seat_navy");
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
    // --- Sports & Venues - Seating & Venue (gen_venues_seating.py --fragments) ---
    initTabBlock(new BlockStadiumSeat("stadium_seat_charcoal", 0x3A3C40));
    initTabBlock(new BlockStadiumSeat("stadium_seat_navy", 0x1C2A56));
    initTabBlock(new BlockStadiumSeat("stadium_seat_ice_blue", 0x9CC8E6));
    initTabBlock(new BlockStadiumSeat("stadium_seat_crimson", 0x961C2C));
    initTabBlock(new BlockStadiumSeat("stadium_seat_gold", 0xD6A82A));
    initTabBlock(new BlockStadiumSeat("stadium_seat_silver", 0xB0B4BA));
    initTabBlock(new BlockStadiumSeat("stadium_seat_black", 0x1C1C1E));
    initTabBlock(new BlockStadiumSeat("stadium_seat_red", 0xC42424));
    initTabBlock(new BlockStadiumSeat("stadium_seat_green", 0x1E6E38));
    initTabBlock(new BlockStadiumSeat("stadium_seat_white", 0xEEEEEA));
    initTabBlock(new BlockStadiumAisleEnd("stadium_seat_aisle_end", 0));
    initTabBlock(new BlockStadiumAisleEnd("stadium_seat_aisle_end_2", 16));
    initTabBlock(new BlockStadiumCompanionSeat("stadium_companion_seat"));
    initTabBlock(new BlockStadiumWheelchairSpace("stadium_wheelchair_space"));
    initTabBlock(new BlockStadiumStepLight("stadium_aisle_step_light"));
    initTabBlock(new BlockStadiumHandrail("stadium_aisle_handrail"));
    initTabBlock(new BlockStadiumGlassGuard("stadium_glass_guard", BlockStadiumGlassGuard.Kind.LEVEL));
    initTabBlock(new BlockStadiumGlassGuard("stadium_glass_guard_slope", BlockStadiumGlassGuard.Kind.SLOPE));
    initTabBlock(new BlockStadiumGlassGuard("stadium_glass_guard_steep", BlockStadiumGlassGuard.Kind.STEEP));
    initTabBlock(new BlockStadiumSectionSign("stadium_section_sign"));
    initTabBlock(new BlockStadiumPortalSign("stadium_portal_sign"));
    // --- Sports & Venues - Seating & Venue (gen_venues_exterior.py --fragments) ---
    initTabBlock(new BlockBleacher("aluminium_bleacher", false));
    initTabBlock(new BlockBleacher("aluminium_bleacher_aisle", true));
    initTabBlock(new BlockBleacherFrame("aluminium_bleacher_frame"));
    initTabBlock(new BlockPressBox("press_box_module", new int[]{0, 1, 0}, new int[]{0, 0, 0, 16, 32, 3}));
    initTabBlock(new BlockTicketBooth("ticket_booth", new int[]{1, 0, 0, 0, 0, 1, 1, 0, 1, 0, 1, 0, 1, 1, 0, 0, 1, 1, 1, 1, 1, 0, 2, 0, 1, 2, 0, 0, 2, 1, 1, 2, 1}, new int[]{0, 0, 0, 32, 38, 32, -2, 38, -2, 34, 42, 34}));
    initTabBlock(new BlockLightPole("stadium_light_pole", false));
    initTabBlock(new BlockLightPole("stadium_light_lattice", true));
    initTabBlock(new BlockLightHead("stadium_light_head", new int[]{-2, 0, 0, -1, 0, 0, 1, 0, 0, 2, 0, 0, -2, 1, 0, -1, 1, 0, 0, 1, 0, 1, 1, 0, 2, 1, 0, -2, 2, 0, -1, 2, 0, 0, 2, 0, 1, 2, 0, 2, 2, 0}, new int[]{-32, 8, 2, 48, 46, 12, 5, 0, 7, 11, 10, 13}));
    initTabBlock(new BlockLightHead("stadium_light_head_small", new int[]{-1, 0, 0, 1, 0, 0, -1, 1, 0, 0, 1, 0, 1, 1, 0}, new int[]{-16, 8, 2, 32, 30, 12, 5, 0, 7, 11, 10, 13}));
  }
}

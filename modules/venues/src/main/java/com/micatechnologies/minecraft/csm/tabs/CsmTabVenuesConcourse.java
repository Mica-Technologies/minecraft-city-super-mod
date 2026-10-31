package com.micatechnologies.minecraft.csm.tabs;

import com.micatechnologies.minecraft.csm.CsmRegistry;
import com.micatechnologies.minecraft.csm.codeutils.CsmTab;
import com.micatechnologies.minecraft.csm.venues.concourse.BlockBeerKiosk;
import com.micatechnologies.minecraft.csm.venues.concourse.BlockConcessionAppliance;
import com.micatechnologies.minecraft.csm.venues.concourse.BlockConcessionCounter;
import com.micatechnologies.minecraft.csm.venues.concourse.BlockConcessionCounterCorner;
import com.micatechnologies.minecraft.csm.venues.concourse.BlockConcessionSign;
import com.micatechnologies.minecraft.csm.venues.concourse.BlockConcourseRun;
import com.micatechnologies.minecraft.csm.venues.concourse.BlockJerseyRack;
import com.micatechnologies.minecraft.csm.venues.concourse.BlockMenuBoard;
import com.micatechnologies.minecraft.csm.venues.concourse.BlockStadiumTurnstile;
import com.micatechnologies.minecraft.csm.venues.concourse.BlockTeamStoreFixture;
import com.micatechnologies.minecraft.csm.venues.concourse.BlockTicketScanner;
import com.micatechnologies.minecraft.csm.venues.concourse.ItemVenueTicket;
import com.micatechnologies.minecraft.csm.venues.exterior.BlockBladeBracket;
import com.micatechnologies.minecraft.csm.venues.exterior.BlockBoxOfficeWindow;
import com.micatechnologies.minecraft.csm.venues.exterior.BlockMarqueeBoard;
import com.micatechnologies.minecraft.csm.venues.exterior.BlockMarqueeController;
import com.micatechnologies.minecraft.csm.venues.exterior.BlockMarqueePylon;
import com.micatechnologies.minecraft.csm.venues.exterior.BlockVenueBlade;
import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;

/**
 * The Sports &amp; Venues module's Concourse tab: what an arena's concourse and premium spaces
 * have that the rest of CSM does not. Concession counters (straight, outside corner, the back
 * counter), the lit concession sign and the digital menu board, the hot dog roller grill, popcorn
 * machine, pretzel warmer, nacho station and soft-serve machine, the portable beer kiosk, the
 * stadium turnstile with its ticket scanner and the Venue Ticket, the suite bar counter and drink
 * rail, and the team store's jersey rack, cap wall, apparel table and jersey mannequin; and the
 * arena's outside and entry: the exterior marquee board built to size, its pylon, the gate and
 * section wayfinding blades with their bracket, and the box office window. The coolers, registers, fountain drinks, bar stools, TVs, metal detector and queue stanchions a
 * concourse also uses are Furnishings', Life Safety's and Transit's (see
 * {@code VENUES_SYSTEM.md}).
 *
 * @version 1.0
 * @since 2026.10
 */
@CsmTab.Load(order = 36)
public class CsmTabVenuesConcourse extends CsmTab {

  @Override
  public String getTabId() {
    return "tabvenuesconcourse";
  }

  @Override
  public Block getTabIcon() {
    Block icon = CsmRegistry.getBlock("popcorn_machine");
    return icon != null ? icon : Blocks.CAKE;
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
    // --- Sports & Venues - Concourse (gen_venues_concourse.py --fragments) ---
    initTabBlock(new BlockConcessionCounter("concession_counter"));
    initTabBlock(new BlockConcessionCounterCorner("concession_counter_corner"));
    initTabBlock(new BlockConcourseRun("concession_back_counter", BlockConcourseRun.Kind.BACK_COUNTER));
    initTabBlock(new BlockConcessionSign("concession_sign"));
    initTabBlock(new BlockMenuBoard("concession_menu_board", new int[]{-1, 0, 0, 1, 0, 0, -1, 1, 0, 0, 1, 0, 1, 1, 0}, new int[]{-16, 0, 13, 32, 32, 16}));
    initTabBlock(new BlockConcessionAppliance("hot_dog_roller_grill", BlockConcessionAppliance.Kind.HOT_DOG_ROLLER));
    initTabBlock(new BlockConcessionAppliance("popcorn_machine", BlockConcessionAppliance.Kind.POPCORN));
    initTabBlock(new BlockConcessionAppliance("pretzel_warmer", BlockConcessionAppliance.Kind.PRETZEL_WARMER));
    initTabBlock(new BlockConcessionAppliance("nacho_cheese_dispenser", BlockConcessionAppliance.Kind.NACHO));
    initTabBlock(new BlockConcessionAppliance("soft_serve_machine", BlockConcessionAppliance.Kind.SOFT_SERVE));
    initTabBlock(new BlockBeerKiosk("beer_kiosk", new int[]{1, 0, 0, 0, 1, 0, 1, 1, 0, 0, 2, 0, 1, 2, 0}, new int[]{1, 0, 3, 31, 17, 14, 0, 17, 13, 32, 40, 15, 0, 40, 0, 32, 46, 16}));
    initTabBlock(new BlockStadiumTurnstile("stadium_turnstile"));
    initTabBlock(new BlockTicketScanner("stadium_ticket_scanner"));
    initTabBlock(new BlockConcourseRun("suite_bar_counter", BlockConcourseRun.Kind.SUITE_BAR));
    initTabBlock(new BlockConcourseRun("suite_drink_rail", BlockConcourseRun.Kind.DRINK_RAIL));
    initTabBlock(new BlockJerseyRack("jersey_wall_rack"));
    initTabBlock(new BlockTeamStoreFixture("hat_wall", BlockTeamStoreFixture.Kind.HAT_WALL));
    initTabBlock(new BlockTeamStoreFixture("team_apparel_table", BlockTeamStoreFixture.Kind.APPAREL_TABLE));
    initTabBlock(new BlockTeamStoreFixture("jersey_mannequin", BlockTeamStoreFixture.Kind.MANNEQUIN));
    // --- Sports & Venues - Concourse (gen_venues_exterior.py --fragments) ---
    initTabBlock(new BlockMarqueeBoard("marquee_board"));
    initTabBlock(new BlockMarqueeController("marquee_board_controller"));
    initTabBlock(new BlockMarqueePylon("marquee_pylon"));
    initTabBlock(new BlockVenueBlade("venue_gate_blade", true));
    initTabBlock(new BlockVenueBlade("venue_section_blade", false));
    initTabBlock(new BlockBladeBracket("venue_blade_bracket"));
    initTabBlock(new BlockBoxOfficeWindow("box_office_window", new int[]{0, 1, 0}, new int[]{0, 0, 6, 16, 32, 16, 0, 13, 1, 16, 15, 6}));
    initTabItem(new ItemVenueTicket());
  }
}

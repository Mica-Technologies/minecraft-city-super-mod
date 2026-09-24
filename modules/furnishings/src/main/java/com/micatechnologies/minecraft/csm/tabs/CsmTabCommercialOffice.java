package com.micatechnologies.minecraft.csm.tabs;

import com.micatechnologies.minecraft.csm.CsmRegistry;
import com.micatechnologies.minecraft.csm.codeutils.CsmTab;
import com.micatechnologies.minecraft.csm.furniture.office.BlockCubiclePanel;
import com.micatechnologies.minecraft.csm.furniture.office.OfficeAppliances;
import com.micatechnologies.minecraft.csm.furniture.residential.BlockBathroomFixture;
import com.micatechnologies.minecraft.csm.furniture.residential.BlockBookcase;
import com.micatechnologies.minecraft.csm.furniture.residential.BlockBuiltInAppliance;
import com.micatechnologies.minecraft.csm.furniture.residential.BlockCloset;
import com.micatechnologies.minecraft.csm.furniture.residential.BlockCounterLight;
import com.micatechnologies.minecraft.csm.furniture.residential.BlockCounterPiece;
import com.micatechnologies.minecraft.csm.furniture.residential.BlockDiningTable;
import com.micatechnologies.minecraft.csm.furniture.residential.BlockFoldingFixture;
import com.micatechnologies.minecraft.csm.furniture.residential.BlockKitchenCabinet;
import com.micatechnologies.minecraft.csm.furniture.residential.BlockKitchenCorner;
import com.micatechnologies.minecraft.csm.furniture.residential.BlockKitchenLight;
import com.micatechnologies.minecraft.csm.furniture.residential.BlockResidentialFurniture;
import com.micatechnologies.minecraft.csm.furniture.residential.BlockResidentialRun;
import com.micatechnologies.minecraft.csm.furniture.residential.BlockResidentialStorage;
import com.micatechnologies.minecraft.csm.furniture.residential.BlockResidentialTall;
import com.micatechnologies.minecraft.csm.furniture.residential.FixtureMaterial;
import com.micatechnologies.minecraft.csm.furniture.residential.KitchenFront;
import com.micatechnologies.minecraft.csm.furniture.residential.KitchenLine;
import com.micatechnologies.minecraft.csm.novelties.FurnishingsSounds;
import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.util.BlockRenderLayer;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;

/**
 * The tab for the furniture of offices, schools and studios: office desks that join into long
 * desks with their pedestals and L corners, the reception desk, filing cabinets, office shelving
 * and the conference table, each in three laminates; cubicle panels in two heights and three
 * fabrics; office seating and the waiting-room bench; the whiteboard, chalkboard, cork board and
 * projector screen; the school desk, the teacher's desk and lockers; the things on a desk and
 * the copier that copies written books; and a streamer's green screen, ring light and camera.
 * The lines below are printed by {@code gen_furniture_office.py --fragments}.
 *
 * @version 1.0
 * @since 2026.9
 */
@CsmTab.Load(order = 25)
public class CsmTabCommercialOffice extends CsmTab {

  @Override
  public String getTabId() {
    return "tabcommercialoffice";
  }

  @Override
  public Block getTabIcon() {
    return CsmRegistry.getBlock("task_chair_navy");
  }

  @Override
  public boolean getTabSearchable() {
    return true;
  }

  @Override
  public boolean getTabHidden() {
    return false;
  }

  @Override
  public void initTabElements(FMLPreInitializationEvent fmlPreInitializationEvent) {
    // ---- Office ----
    // Office Desk
    initTabBlock(new BlockKitchenCabinet("office_desk_white", new int[]{0, 0, 0, 16, 12, 16}, KitchenLine.DESK, 0, KitchenFront.OPEN));
    initTabBlock(new BlockKitchenCabinet("office_desk_grey", new int[]{0, 0, 0, 16, 12, 16}, KitchenLine.DESK, 0, KitchenFront.OPEN));
    initTabBlock(new BlockKitchenCabinet("office_desk_walnut", new int[]{0, 0, 0, 16, 12, 16}, KitchenLine.DESK, 0, KitchenFront.OPEN));

    // Office Desk with Pedestal
    initTabBlock(new BlockKitchenCabinet("office_desk_pedestal_white", new int[]{0, 0, 0, 16, 12, 16}, KitchenLine.DESK, 18, KitchenFront.DRAWERS));
    initTabBlock(new BlockKitchenCabinet("office_desk_pedestal_grey", new int[]{0, 0, 0, 16, 12, 16}, KitchenLine.DESK, 18, KitchenFront.DRAWERS));
    initTabBlock(new BlockKitchenCabinet("office_desk_pedestal_walnut", new int[]{0, 0, 0, 16, 12, 16}, KitchenLine.DESK, 18, KitchenFront.DRAWERS));

    // L-Desk Corner
    initTabBlock(new BlockKitchenCorner("office_desk_corner_white", new int[]{0, 0, 0, 16, 12, 16}, KitchenLine.DESK, 0, KitchenFront.OPEN));
    initTabBlock(new BlockKitchenCorner("office_desk_corner_grey", new int[]{0, 0, 0, 16, 12, 16}, KitchenLine.DESK, 0, KitchenFront.OPEN));
    initTabBlock(new BlockKitchenCorner("office_desk_corner_walnut", new int[]{0, 0, 0, 16, 12, 16}, KitchenLine.DESK, 0, KitchenFront.OPEN));

    // Reception Desk
    initTabBlock(new BlockKitchenCabinet("reception_desk_white", new int[]{0, 0, 0, 16, 16, 15}, KitchenLine.RECEPTION, 9, KitchenFront.DRAWERS));
    initTabBlock(new BlockKitchenCabinet("reception_desk_grey", new int[]{0, 0, 0, 16, 16, 15}, KitchenLine.RECEPTION, 9, KitchenFront.DRAWERS));
    initTabBlock(new BlockKitchenCabinet("reception_desk_walnut", new int[]{0, 0, 0, 16, 16, 15}, KitchenLine.RECEPTION, 9, KitchenFront.DRAWERS));

    // Filing Cabinet
    initTabBlock(new BlockResidentialTall("filing_cabinet_white", new int[]{0, 0, 2, 16, 21, 16}, false, 27, FurnishingsSounds.DRAWER_OPEN, FurnishingsSounds.DRAWER_CLOSE));
    initTabBlock(new BlockResidentialTall("filing_cabinet_grey", new int[]{0, 0, 2, 16, 21, 16}, false, 27, FurnishingsSounds.DRAWER_OPEN, FurnishingsSounds.DRAWER_CLOSE));
    initTabBlock(new BlockResidentialTall("filing_cabinet_walnut", new int[]{0, 0, 2, 16, 21, 16}, false, 27, FurnishingsSounds.DRAWER_OPEN, FurnishingsSounds.DRAWER_CLOSE));

    // Office Shelving
    initTabBlock(new BlockBookcase("office_shelving_white", new int[]{0, 0, 10, 16, 16, 16}));
    initTabBlock(new BlockBookcase("office_shelving_grey", new int[]{0, 0, 10, 16, 16, 16}));
    initTabBlock(new BlockBookcase("office_shelving_walnut", new int[]{0, 0, 10, 16, 16, 16}));

    // Conference Table
    initTabBlock(new BlockDiningTable("conference_table_white"));
    initTabBlock(new BlockDiningTable("conference_table_grey"));
    initTabBlock(new BlockDiningTable("conference_table_walnut"));

    // Teacher's Desk
    initTabBlock(new BlockResidentialStorage("teacher_desk_white", new int[]{0, 0, 0, 16, 14, 16}, 18, FurnishingsSounds.DRAWER_OPEN, FurnishingsSounds.DRAWER_CLOSE));
    initTabBlock(new BlockResidentialStorage("teacher_desk_grey", new int[]{0, 0, 0, 16, 14, 16}, 18, FurnishingsSounds.DRAWER_OPEN, FurnishingsSounds.DRAWER_CLOSE));
    initTabBlock(new BlockResidentialStorage("teacher_desk_walnut", new int[]{0, 0, 0, 16, 14, 16}, 18, FurnishingsSounds.DRAWER_OPEN, FurnishingsSounds.DRAWER_CLOSE));

    // ---- Cubicles ----
    // Cubicle Panel
    initTabBlock(new BlockCubiclePanel("cubicle_panel_charcoal", 16));
    initTabBlock(new BlockCubiclePanel("cubicle_panel_navy", 16));
    initTabBlock(new BlockCubiclePanel("cubicle_panel_oatmeal", 16));

    // Half-Height Cubicle Panel
    initTabBlock(new BlockCubiclePanel("cubicle_panel_half_charcoal", 8));
    initTabBlock(new BlockCubiclePanel("cubicle_panel_half_navy", 8));
    initTabBlock(new BlockCubiclePanel("cubicle_panel_half_oatmeal", 8));

    // ---- Seating ----
    // Task Chair
    initTabBlock(new BlockResidentialFurniture("task_chair_charcoal", new int[]{1, 0, 2, 15, 17, 14}, true, 8, 0.5, 0));
    initTabBlock(new BlockResidentialFurniture("task_chair_navy", new int[]{1, 0, 2, 15, 17, 14}, true, 8, 0.5, 0));
    initTabBlock(new BlockResidentialFurniture("task_chair_red", new int[]{1, 0, 2, 15, 17, 14}, true, 8, 0.5, 0));

    // Guest Chair
    initTabBlock(new BlockResidentialFurniture("guest_chair_charcoal", new int[]{2, 0, 3, 14, 15, 14}, true, 8, 0.25, 0));
    initTabBlock(new BlockResidentialFurniture("guest_chair_navy", new int[]{2, 0, 3, 14, 15, 14}, true, 8, 0.25, 0));
    initTabBlock(new BlockResidentialFurniture("guest_chair_red", new int[]{2, 0, 3, 14, 15, 14}, true, 8, 0.25, 0));

    // Conference Chair
    initTabBlock(new BlockResidentialFurniture("conference_chair_charcoal", new int[]{2, 0, 2, 14, 19, 15}, true, 8.25, 0.5, 0));
    initTabBlock(new BlockResidentialFurniture("conference_chair_navy", new int[]{2, 0, 2, 14, 19, 15}, true, 8.25, 0.5, 0));
    initTabBlock(new BlockResidentialFurniture("conference_chair_red", new int[]{2, 0, 2, 14, 19, 15}, true, 8.25, 0.5, 0));

    // Gaming Chair
    initTabBlock(new BlockResidentialFurniture("gaming_chair_charcoal", new int[]{1, 0, 2, 15, 21, 14}, true, 8, 0.5, 0));
    initTabBlock(new BlockResidentialFurniture("gaming_chair_navy", new int[]{1, 0, 2, 15, 21, 14}, true, 8, 0.5, 0));
    initTabBlock(new BlockResidentialFurniture("gaming_chair_red", new int[]{1, 0, 2, 15, 21, 14}, true, 8, 0.5, 0));

    // Waiting Room Bench
    initTabBlock(new BlockResidentialRun("waiting_bench_charcoal", new int[]{0, 0, 3, 16, 14, 13}, true, 7.25, 0.75));
    initTabBlock(new BlockResidentialRun("waiting_bench_navy", new int[]{0, 0, 3, 16, 14, 13}, true, 7.25, 0.75));
    initTabBlock(new BlockResidentialRun("waiting_bench_red", new int[]{0, 0, 3, 16, 14, 13}, true, 7.25, 0.75));

    // ---- Boards ----
    // Whiteboard
    initTabBlock(new BlockResidentialRun("whiteboard_aluminium", new int[]{0, 2, 13, 16, 15, 16}, false));

    // Chalkboard
    initTabBlock(new BlockResidentialRun("chalkboard_oak", new int[]{0, 2, 13, 16, 15, 16}, false));

    // Cork Notice Board
    initTabBlock(new BlockResidentialRun("cork_board_oak", new int[]{0, 2, 14, 16, 15, 16}, false));

    // Projector Screen
    initTabBlock(new BlockFoldingFixture("projector_screen_white", new int[]{0, 13, 13, 16, 16, 16}, new int[]{0, 0, 13, 16, 16, 16}, FixtureMaterial.PLASTIC));

    // ---- School ----
    // School Desk and Chair
    initTabBlock(new BlockResidentialFurniture("school_desk_blue", new int[]{2, 0, 0, 14, 14, 15}, false, 7.5, -2.75, 0));
    initTabBlock(new BlockResidentialFurniture("school_desk_red", new int[]{2, 0, 0, 14, 14, 15}, false, 7.5, -2.75, 0));

    // Locker
    initTabBlock(new BlockCloset("locker_blue", new int[]{0, 0, 7, 16, 29, 16}, 9, FurnishingsSounds.LOCKER_DOOR_OPEN, FurnishingsSounds.LOCKER_DOOR_CLOSE));
    initTabBlock(new BlockCloset("locker_grey", new int[]{0, 0, 7, 16, 29, 16}, 9, FurnishingsSounds.LOCKER_DOOR_OPEN, FurnishingsSounds.LOCKER_DOOR_CLOSE));
    initTabBlock(new BlockCloset("locker_red", new int[]{0, 0, 7, 16, 29, 16}, 9, FurnishingsSounds.LOCKER_DOOR_OPEN, FurnishingsSounds.LOCKER_DOOR_CLOSE));

    // ---- On the desk ----
    // Desktop Computer
    initTabBlock(new BlockCounterLight("desktop_computer_black", new int[]{2, 0, 3, 14, 11, 12}, Material.WOOD, SoundType.METAL, BlockRenderLayer.SOLID, 3));

    // Computer Tower
    initTabBlock(new BlockCounterPiece("computer_tower_black", new int[]{5, 0, 3, 11, 11, 13}, Material.WOOD, SoundType.METAL, BlockRenderLayer.SOLID));

    // Retro Computer
    initTabBlock(new BlockCounterLight("retro_computer_beige", new int[]{2, 0, 1, 14, 10, 15}, Material.WOOD, SoundType.METAL, BlockRenderLayer.SOLID, 3));

    // Laptop
    initTabBlock(new BlockCounterLight("laptop_silver", new int[]{3, 0, 3, 13, 7, 10}, Material.WOOD, SoundType.METAL, BlockRenderLayer.SOLID, 2));

    // Desk Phone
    initTabBlock(new BlockCounterPiece("desk_phone_black", new int[]{4, 0, 4, 12, 4, 12}, Material.WOOD, SoundType.METAL, BlockRenderLayer.SOLID, FurnishingsSounds.APPLIANCE_BEEP, 1.6F, null));

    // Fax Machine
    initTabBlock(new BlockCounterPiece("fax_machine_grey", new int[]{3, 0, 3, 13, 7, 12}, Material.WOOD, SoundType.METAL, BlockRenderLayer.SOLID, FurnishingsSounds.PRINTER_RUN, 1.3F, null));

    // Pen Holder
    initTabBlock(new BlockCounterPiece("pen_holder_black", new int[]{6, 0, 6, 10, 6, 10}, Material.WOOD, SoundType.METAL, BlockRenderLayer.SOLID));

    // Paper Tray
    initTabBlock(new BlockCounterPiece("paper_tray_black", new int[]{3, 0, 4, 13, 4, 12}, Material.WOOD, SoundType.METAL, BlockRenderLayer.SOLID));

    // Desk Lamp
    initTabBlock(new BlockCounterLight("desk_lamp_black", new int[]{5, 0, 2, 11, 11, 14}, Material.WOOD, SoundType.METAL, BlockRenderLayer.SOLID, 12));
    initTabBlock(new BlockCounterLight("desk_lamp_white", new int[]{5, 0, 2, 11, 11, 14}, Material.WOOD, SoundType.METAL, BlockRenderLayer.SOLID, 12));

    // ---- Copier ----
    // Copier
    initTabBlock(new BlockBuiltInAppliance("copier_grey", new int[]{0, 0, 1, 16, 16, 15}, OfficeAppliances.COPIER, null));

    // ---- Streaming ----
    // Green Screen
    initTabBlock(new BlockFoldingFixture("green_screen_chroma", new int[]{1, 0, 6, 15, 2, 10}, new int[]{1, 0, 6, 15, 31, 10}, FixtureMaterial.PLASTIC));

    // Ring Light
    initTabBlock(new BlockKitchenLight("ring_light_black", new int[]{4, 0, 4, 12, 31, 12}, 14));

    // Studio Camera
    initTabBlock(new BlockBathroomFixture("studio_camera_black", new int[]{4, 0, 1, 12, 21, 16}, FixtureMaterial.METAL));
  }
}

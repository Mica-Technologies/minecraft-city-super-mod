package com.micatechnologies.minecraft.csm.tabs;

import com.micatechnologies.minecraft.csm.CsmRegistry;
import com.micatechnologies.minecraft.csm.codeutils.CsmTab;
import com.micatechnologies.minecraft.csm.furniture.residential.BedLayout;
import com.micatechnologies.minecraft.csm.furniture.residential.BlockBasin;
import com.micatechnologies.minecraft.csm.furniture.residential.BlockBathroomFixture;
import com.micatechnologies.minecraft.csm.furniture.residential.BlockBathroomVanity;
import com.micatechnologies.minecraft.csm.furniture.residential.BlockBathtub;
import com.micatechnologies.minecraft.csm.furniture.residential.BlockBookcase;
import com.micatechnologies.minecraft.csm.furniture.residential.BlockBuiltInAppliance;
import com.micatechnologies.minecraft.csm.furniture.residential.BlockChestFreezer;
import com.micatechnologies.minecraft.csm.furniture.residential.BlockCloset;
import com.micatechnologies.minecraft.csm.furniture.residential.BlockCookieJar;
import com.micatechnologies.minecraft.csm.furniture.residential.BlockCounterAppliance;
import com.micatechnologies.minecraft.csm.furniture.residential.BlockCounterPiece;
import com.micatechnologies.minecraft.csm.furniture.residential.BlockDiningTable;
import com.micatechnologies.minecraft.csm.furniture.residential.BlockDishwasher;
import com.micatechnologies.minecraft.csm.furniture.residential.BlockFoldingFixture;
import com.micatechnologies.minecraft.csm.furniture.residential.BlockKitchenCabinet;
import com.micatechnologies.minecraft.csm.furniture.residential.BlockKitchenCorner;
import com.micatechnologies.minecraft.csm.furniture.residential.BlockKitchenLight;
import com.micatechnologies.minecraft.csm.furniture.residential.BlockKitchenSink;
import com.micatechnologies.minecraft.csm.furniture.residential.BlockLaundryAppliance;
import com.micatechnologies.minecraft.csm.furniture.residential.BlockRefrigerator;
import com.micatechnologies.minecraft.csm.furniture.residential.BlockResidentialBed;
import com.micatechnologies.minecraft.csm.furniture.residential.BlockResidentialFurniture;
import com.micatechnologies.minecraft.csm.furniture.residential.BlockResidentialStorage;
import com.micatechnologies.minecraft.csm.furniture.residential.BlockResidentialTall;
import com.micatechnologies.minecraft.csm.furniture.residential.BlockRug;
import com.micatechnologies.minecraft.csm.furniture.residential.BlockShower;
import com.micatechnologies.minecraft.csm.furniture.residential.BlockShowerHead;
import com.micatechnologies.minecraft.csm.furniture.residential.BlockSofa;
import com.micatechnologies.minecraft.csm.furniture.residential.BlockSofaCorner;
import com.micatechnologies.minecraft.csm.furniture.residential.BlockToilet;
import com.micatechnologies.minecraft.csm.furniture.residential.FixtureMaterial;
import com.micatechnologies.minecraft.csm.furniture.residential.ItemResidentialFood;
import com.micatechnologies.minecraft.csm.furniture.residential.KitchenAppliances;
import com.micatechnologies.minecraft.csm.furniture.residential.KitchenFront;
import com.micatechnologies.minecraft.csm.furniture.residential.KitchenLine;
import com.micatechnologies.minecraft.csm.furniture.residential.LaundryAppliances;
import com.micatechnologies.minecraft.csm.novelties.FurnishingsSounds;
import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.init.MobEffects;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.BlockRenderLayer;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;

/**
 * The tab for the furniture of homes: living and dining (tables, chairs, stools, bookcases, TV
 * stands, sideboards, armchairs and sofas), each in three wood finishes or four fabrics, then the
 * kitchen (cabinets, sink, island, hoods, lights, refrigerators, freezer), then its working
 * appliances, the things on its counters and the tableware, and the food and drink the
 * appliances make; then the bedroom, study and nursery; then the bathroom, a commercial
 * restroom's fittings and the laundry. The lines below are printed by
 * {@code gen_furniture_residential.py --fragments}, {@code gen_furniture_kitchen.py --fragments},
 * {@code gen_furniture_appliances.py --fragments}, {@code gen_furniture_bedroom.py --fragments}
 * and {@code gen_furniture_bathroom.py --fragments}.
 *
 * @version 1.0
 * @since 2026.9
 */
@CsmTab.Load(order = 24)
public class CsmTabResidential extends CsmTab {

  @Override
  public String getTabId() {
    return "tabresidential";
  }

  @Override
  public Block getTabIcon() {
    return CsmRegistry.getBlock("armchair_navy");
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
    // Dining Table
    initTabBlock(new BlockDiningTable("dining_table_oak"));
    initTabBlock(new BlockDiningTable("dining_table_walnut"));
    initTabBlock(new BlockDiningTable("dining_table_white"));

    // Coffee Table
    initTabBlock(new BlockResidentialFurniture("coffee_table_oak", new int[]{0, 0, 3, 16, 7, 13}, false));
    initTabBlock(new BlockResidentialFurniture("coffee_table_walnut", new int[]{0, 0, 3, 16, 7, 13}, false));
    initTabBlock(new BlockResidentialFurniture("coffee_table_white", new int[]{0, 0, 3, 16, 7, 13}, false));

    // Side Table
    initTabBlock(new BlockResidentialFurniture("side_table_oak", new int[]{4, 0, 4, 12, 9, 12}, false));
    initTabBlock(new BlockResidentialFurniture("side_table_walnut", new int[]{4, 0, 4, 12, 9, 12}, false));
    initTabBlock(new BlockResidentialFurniture("side_table_white", new int[]{4, 0, 4, 12, 9, 12}, false));

    // Cafe Table
    initTabBlock(new BlockResidentialFurniture("cafe_table_oak", new int[]{2, 0, 2, 14, 12, 14}, false));
    initTabBlock(new BlockResidentialFurniture("cafe_table_walnut", new int[]{2, 0, 2, 14, 12, 14}, false));
    initTabBlock(new BlockResidentialFurniture("cafe_table_white", new int[]{2, 0, 2, 14, 12, 14}, false));

    // Dining Chair
    initTabBlock(new BlockResidentialFurniture("dining_chair_oak", new int[]{4, 0, 4, 12, 15, 12}, false, 7.5, 0.25, 0));
    initTabBlock(new BlockResidentialFurniture("dining_chair_walnut", new int[]{4, 0, 4, 12, 15, 12}, false, 7.5, 0.25, 0));
    initTabBlock(new BlockResidentialFurniture("dining_chair_white", new int[]{4, 0, 4, 12, 15, 12}, false, 7.5, 0.25, 0));

    // Bar Stool
    initTabBlock(new BlockResidentialFurniture("bar_stool_oak", new int[]{4, 0, 4, 12, 12, 12}, false, 12, 0, 0));
    initTabBlock(new BlockResidentialFurniture("bar_stool_walnut", new int[]{4, 0, 4, 12, 12, 12}, false, 12, 0, 0));
    initTabBlock(new BlockResidentialFurniture("bar_stool_white", new int[]{4, 0, 4, 12, 12, 12}, false, 12, 0, 0));

    // Bookcase
    initTabBlock(new BlockBookcase("bookcase_oak", new int[]{0, 0, 10, 16, 16, 16}));
    initTabBlock(new BlockBookcase("bookcase_walnut", new int[]{0, 0, 10, 16, 16, 16}));
    initTabBlock(new BlockBookcase("bookcase_white", new int[]{0, 0, 10, 16, 16, 16}));

    // TV Stand
    initTabBlock(new BlockResidentialStorage("tv_stand_oak", new int[]{0, 0, 9, 16, 8, 16}, 9));
    initTabBlock(new BlockResidentialStorage("tv_stand_walnut", new int[]{0, 0, 9, 16, 8, 16}, 9));
    initTabBlock(new BlockResidentialStorage("tv_stand_white", new int[]{0, 0, 9, 16, 8, 16}, 9));

    // Sideboard
    initTabBlock(new BlockResidentialStorage("sideboard_oak", new int[]{0, 0, 8, 16, 14, 16}, 18));
    initTabBlock(new BlockResidentialStorage("sideboard_walnut", new int[]{0, 0, 8, 16, 14, 16}, 18));
    initTabBlock(new BlockResidentialStorage("sideboard_white", new int[]{0, 0, 8, 16, 14, 16}, 18));

    // Armchair
    initTabBlock(new BlockResidentialFurniture("armchair_charcoal", new int[]{1, 0, 1, 15, 14, 15}, true, 7.25, 1.9, 0));
    initTabBlock(new BlockResidentialFurniture("armchair_navy", new int[]{1, 0, 1, 15, 14, 15}, true, 7.25, 1.9, 0));
    initTabBlock(new BlockResidentialFurniture("armchair_oatmeal", new int[]{1, 0, 1, 15, 14, 15}, true, 7.25, 1.9, 0));
    initTabBlock(new BlockResidentialFurniture("armchair_red", new int[]{1, 0, 1, 15, 14, 15}, true, 7.25, 1.9, 0));

    // Sofa
    initTabBlock(new BlockSofa("sofa_charcoal", new int[]{0, 0, 1, 16, 14, 16}));
    initTabBlock(new BlockSofa("sofa_navy", new int[]{0, 0, 1, 16, 14, 16}));
    initTabBlock(new BlockSofa("sofa_oatmeal", new int[]{0, 0, 1, 16, 14, 16}));
    initTabBlock(new BlockSofa("sofa_red", new int[]{0, 0, 1, 16, 14, 16}));

    // Sofa Corner
    initTabBlock(new BlockSofaCorner("sofa_corner_charcoal"));
    initTabBlock(new BlockSofaCorner("sofa_corner_navy"));
    initTabBlock(new BlockSofaCorner("sofa_corner_oatmeal"));
    initTabBlock(new BlockSofaCorner("sofa_corner_red"));

    // ---- Kitchen ----
    // Kitchen Base Cabinet
    initTabBlock(new BlockKitchenCabinet("kitchen_base_cabinet_oak", new int[]{0, 0, 0, 16, 15, 16}, KitchenLine.BASE, 18, KitchenFront.DOORS));
    initTabBlock(new BlockKitchenCabinet("kitchen_base_cabinet_walnut", new int[]{0, 0, 0, 16, 15, 16}, KitchenLine.BASE, 18, KitchenFront.DOORS));
    initTabBlock(new BlockKitchenCabinet("kitchen_base_cabinet_white", new int[]{0, 0, 0, 16, 15, 16}, KitchenLine.BASE, 18, KitchenFront.DOORS));

    // Kitchen Drawer Cabinet
    initTabBlock(new BlockKitchenCabinet("kitchen_drawer_cabinet_oak", new int[]{0, 0, 0, 16, 15, 16}, KitchenLine.BASE, 18, KitchenFront.DRAWERS));
    initTabBlock(new BlockKitchenCabinet("kitchen_drawer_cabinet_walnut", new int[]{0, 0, 0, 16, 15, 16}, KitchenLine.BASE, 18, KitchenFront.DRAWERS));
    initTabBlock(new BlockKitchenCabinet("kitchen_drawer_cabinet_white", new int[]{0, 0, 0, 16, 15, 16}, KitchenLine.BASE, 18, KitchenFront.DRAWERS));

    // Kitchen Door and Drawer Cabinet
    initTabBlock(new BlockKitchenCabinet("kitchen_door_drawer_cabinet_oak", new int[]{0, 0, 0, 16, 15, 16}, KitchenLine.BASE, 18, KitchenFront.DOORS));
    initTabBlock(new BlockKitchenCabinet("kitchen_door_drawer_cabinet_walnut", new int[]{0, 0, 0, 16, 15, 16}, KitchenLine.BASE, 18, KitchenFront.DOORS));
    initTabBlock(new BlockKitchenCabinet("kitchen_door_drawer_cabinet_white", new int[]{0, 0, 0, 16, 15, 16}, KitchenLine.BASE, 18, KitchenFront.DOORS));

    // Kitchen Corner Cabinet
    initTabBlock(new BlockKitchenCorner("kitchen_corner_cabinet_oak"));
    initTabBlock(new BlockKitchenCorner("kitchen_corner_cabinet_walnut"));
    initTabBlock(new BlockKitchenCorner("kitchen_corner_cabinet_white"));

    // Kitchen Sink Cabinet
    initTabBlock(new BlockKitchenSink("kitchen_sink_cabinet_oak"));
    initTabBlock(new BlockKitchenSink("kitchen_sink_cabinet_walnut"));
    initTabBlock(new BlockKitchenSink("kitchen_sink_cabinet_white"));

    // Kitchen Island
    initTabBlock(new BlockKitchenCabinet("kitchen_island_oak", new int[]{0, 0, 0, 16, 15, 16}, KitchenLine.ISLAND, 18, KitchenFront.DOORS));
    initTabBlock(new BlockKitchenCabinet("kitchen_island_walnut", new int[]{0, 0, 0, 16, 15, 16}, KitchenLine.ISLAND, 18, KitchenFront.DOORS));
    initTabBlock(new BlockKitchenCabinet("kitchen_island_white", new int[]{0, 0, 0, 16, 15, 16}, KitchenLine.ISLAND, 18, KitchenFront.DOORS));

    // Kitchen Wall Cabinet
    initTabBlock(new BlockKitchenCabinet("kitchen_wall_cabinet_oak", new int[]{0, 0, 8, 16, 16, 16}, KitchenLine.WALL, 9, KitchenFront.DOORS));
    initTabBlock(new BlockKitchenCabinet("kitchen_wall_cabinet_walnut", new int[]{0, 0, 8, 16, 16, 16}, KitchenLine.WALL, 9, KitchenFront.DOORS));
    initTabBlock(new BlockKitchenCabinet("kitchen_wall_cabinet_white", new int[]{0, 0, 8, 16, 16, 16}, KitchenLine.WALL, 9, KitchenFront.DOORS));

    // Open Kitchen Shelf
    initTabBlock(new BlockKitchenCabinet("kitchen_wall_shelf_oak", new int[]{0, 0, 8, 16, 16, 16}, KitchenLine.WALL, 0, KitchenFront.OPEN));
    initTabBlock(new BlockKitchenCabinet("kitchen_wall_shelf_walnut", new int[]{0, 0, 8, 16, 16, 16}, KitchenLine.WALL, 0, KitchenFront.OPEN));
    initTabBlock(new BlockKitchenCabinet("kitchen_wall_shelf_white", new int[]{0, 0, 8, 16, 16, 16}, KitchenLine.WALL, 0, KitchenFront.OPEN));

    // Range Hood
    initTabBlock(new BlockKitchenLight("range_hood_stainless", new int[]{0, 0, 6, 16, 16, 16}, 13));

    // Under-Cabinet Range Hood
    initTabBlock(new BlockKitchenLight("range_hood_under_cabinet_stainless", new int[]{0, 13, 7, 16, 16, 16}, 13));

    // Under-Cabinet Light
    initTabBlock(new BlockKitchenLight("under_cabinet_light_stainless", new int[]{1, 15, 9, 15, 16, 11}, 10));

    // Refrigerator
    initTabBlock(new BlockRefrigerator("refrigerator_stainless"));
    initTabBlock(new BlockRefrigerator("refrigerator_white"));

    // Chest Freezer
    initTabBlock(new BlockChestFreezer("chest_freezer_white", new int[]{0, 0, 2, 16, 15, 15}));

    // ---- Kitchen appliances, cooking and tableware ----
    // Electric Range
    initTabBlock(new BlockBuiltInAppliance("kitchen_range_stainless", new int[]{0, 0, 0, 16, 16, 16}, KitchenAppliances.OVEN, KitchenLine.BASE));
    initTabBlock(new BlockBuiltInAppliance("kitchen_range_white", new int[]{0, 0, 0, 16, 16, 16}, KitchenAppliances.OVEN, KitchenLine.BASE));

    // Kitchen Cooktop Cabinet
    initTabBlock(new BlockKitchenCabinet("kitchen_cooktop_cabinet_oak", new int[]{0, 0, 0, 16, 15, 16}, KitchenLine.BASE, 18, KitchenFront.DRAWERS));
    initTabBlock(new BlockKitchenCabinet("kitchen_cooktop_cabinet_walnut", new int[]{0, 0, 0, 16, 15, 16}, KitchenLine.BASE, 18, KitchenFront.DRAWERS));
    initTabBlock(new BlockKitchenCabinet("kitchen_cooktop_cabinet_white", new int[]{0, 0, 0, 16, 15, 16}, KitchenLine.BASE, 18, KitchenFront.DRAWERS));

    // Wall Oven
    initTabBlock(new BlockBuiltInAppliance("wall_oven_stainless", new int[]{0, 0, 0, 16, 16, 16}, KitchenAppliances.OVEN, null));
    initTabBlock(new BlockBuiltInAppliance("wall_oven_white", new int[]{0, 0, 0, 16, 16, 16}, KitchenAppliances.OVEN, null));

    // Dishwasher
    initTabBlock(new BlockDishwasher("dishwasher_stainless", KitchenAppliances.DISHWASHER));
    initTabBlock(new BlockDishwasher("dishwasher_white", KitchenAppliances.DISHWASHER));

    // Microwave
    initTabBlock(new BlockCounterAppliance("microwave_stainless", new int[]{2, 0, 3, 14, 7, 14}, KitchenAppliances.MICROWAVE));
    initTabBlock(new BlockCounterAppliance("microwave_white", new int[]{2, 0, 3, 14, 7, 14}, KitchenAppliances.MICROWAVE));
    initTabBlock(new BlockCounterAppliance("microwave_black", new int[]{2, 0, 3, 14, 7, 14}, KitchenAppliances.MICROWAVE));

    // Toaster
    initTabBlock(new BlockCounterAppliance("toaster_stainless", new int[]{4, 0, 5, 13, 6, 11}, KitchenAppliances.TOASTER));
    initTabBlock(new BlockCounterAppliance("toaster_white", new int[]{4, 0, 5, 13, 6, 11}, KitchenAppliances.TOASTER));
    initTabBlock(new BlockCounterAppliance("toaster_red", new int[]{4, 0, 5, 13, 6, 11}, KitchenAppliances.TOASTER));

    // Air Fryer
    initTabBlock(new BlockCounterAppliance("air_fryer_black", new int[]{4, 0, 3, 12, 8, 13}, KitchenAppliances.AIR_FRYER));
    initTabBlock(new BlockCounterAppliance("air_fryer_white", new int[]{4, 0, 3, 12, 8, 13}, KitchenAppliances.AIR_FRYER));

    // Blender
    initTabBlock(new BlockCounterAppliance("blender_stainless", new int[]{5, 0, 6, 12, 11, 12}, KitchenAppliances.BLENDER));
    initTabBlock(new BlockCounterAppliance("blender_black", new int[]{5, 0, 6, 12, 11, 12}, KitchenAppliances.BLENDER));

    // Coffee Machine
    initTabBlock(new BlockCounterAppliance("coffee_machine_stainless", new int[]{4, 0, 4, 12, 10, 13}, KitchenAppliances.COFFEE_MACHINE));
    initTabBlock(new BlockCounterAppliance("coffee_machine_black", new int[]{4, 0, 4, 12, 10, 13}, KitchenAppliances.COFFEE_MACHINE));

    // Electric Kettle
    initTabBlock(new BlockCounterPiece("kettle_stainless", new int[]{5, 0, 5, 11, 6, 13}, Material.WOOD, SoundType.METAL, BlockRenderLayer.SOLID, FurnishingsSounds.KETTLE_WHISTLE, 1.0F, new double[]{8, 4.6, 4.8}));
    initTabBlock(new BlockCounterPiece("kettle_white", new int[]{5, 0, 5, 11, 6, 13}, Material.WOOD, SoundType.METAL, BlockRenderLayer.SOLID, FurnishingsSounds.KETTLE_WHISTLE, 1.0F, new double[]{8, 4.6, 4.8}));
    initTabBlock(new BlockCounterPiece("kettle_red", new int[]{5, 0, 5, 11, 6, 13}, Material.WOOD, SoundType.METAL, BlockRenderLayer.SOLID, FurnishingsSounds.KETTLE_WHISTLE, 1.0F, new double[]{8, 4.6, 4.8}));

    // Stand Mixer
    initTabBlock(new BlockCounterPiece("stand_mixer_red", new int[]{5, 0, 5, 11, 9, 13}, Material.WOOD, SoundType.METAL, BlockRenderLayer.SOLID, FurnishingsSounds.BLENDER_WHIRR, 0.7F, null));
    initTabBlock(new BlockCounterPiece("stand_mixer_white", new int[]{5, 0, 5, 11, 9, 13}, Material.WOOD, SoundType.METAL, BlockRenderLayer.SOLID, FurnishingsSounds.BLENDER_WHIRR, 0.7F, null));
    initTabBlock(new BlockCounterPiece("stand_mixer_stainless", new int[]{5, 0, 5, 11, 9, 13}, Material.WOOD, SoundType.METAL, BlockRenderLayer.SOLID, FurnishingsSounds.BLENDER_WHIRR, 0.7F, null));

    // Cookie Jar
    initTabBlock(new BlockCookieJar("cookie_jar_ceramic", new int[]{4, 0, 4, 12, 8, 12}));

    // Chopping Board
    initTabBlock(new BlockCounterPiece("chopping_board_oak", new int[]{3, 0, 4, 13, 2, 12}, Material.WOOD, SoundType.WOOD, BlockRenderLayer.SOLID));
    initTabBlock(new BlockCounterPiece("chopping_board_walnut", new int[]{3, 0, 4, 13, 2, 12}, Material.WOOD, SoundType.WOOD, BlockRenderLayer.SOLID));
    initTabBlock(new BlockCounterPiece("chopping_board_white", new int[]{3, 0, 4, 13, 2, 12}, Material.WOOD, SoundType.WOOD, BlockRenderLayer.SOLID));

    // Dinner Plate
    initTabBlock(new BlockCounterPiece("dinner_plate_plain", new int[]{4, 0, 4, 12, 1, 12}, Material.GLASS, SoundType.GLASS, BlockRenderLayer.CUTOUT));
    initTabBlock(new BlockCounterPiece("dinner_plate_patterned", new int[]{4, 0, 4, 12, 1, 12}, Material.GLASS, SoundType.GLASS, BlockRenderLayer.CUTOUT));

    // Stack of Plates
    initTabBlock(new BlockCounterPiece("plate_stack_plain", new int[]{4, 0, 4, 12, 3, 12}, Material.GLASS, SoundType.GLASS, BlockRenderLayer.CUTOUT));
    initTabBlock(new BlockCounterPiece("plate_stack_patterned", new int[]{4, 0, 4, 12, 3, 12}, Material.GLASS, SoundType.GLASS, BlockRenderLayer.CUTOUT));

    // Coffee Mug
    initTabBlock(new BlockCounterPiece("coffee_mug_white", new int[]{6, 0, 6, 11, 4, 10}, Material.GLASS, SoundType.GLASS, BlockRenderLayer.CUTOUT));
    initTabBlock(new BlockCounterPiece("coffee_mug_red", new int[]{6, 0, 6, 11, 4, 10}, Material.GLASS, SoundType.GLASS, BlockRenderLayer.CUTOUT));

    // Drinking Glass
    initTabBlock(new BlockCounterPiece("drinking_glass_water", new int[]{7, 0, 7, 9, 4, 9}, Material.GLASS, SoundType.GLASS, BlockRenderLayer.TRANSLUCENT));

    // Cake Stand
    initTabBlock(new BlockCounterPiece("cake_stand_chocolate", new int[]{4, 0, 4, 12, 7, 12}, Material.GLASS, SoundType.GLASS, BlockRenderLayer.SOLID));
    initTabBlock(new BlockCounterPiece("cake_stand_strawberry", new int[]{4, 0, 4, 12, 7, 12}, Material.GLASS, SoundType.GLASS, BlockRenderLayer.SOLID));

    // Food and drink the appliances make
    initTabItem(new ItemResidentialFood("toast", 6, 0.75F, false, null));
    initTabItem(new ItemResidentialFood("smoothie", 4, 0.6F, true, null));
    initTabItem(new ItemResidentialFood("coffee", 1, 0.2F, true, new PotionEffect(MobEffects.SPEED, 600, 0)));

    // ---- Bedroom, study and nursery ----
    // Single Bed
    initTabBlock(new BlockResidentialBed("bed_single_charcoal", BedLayout.SINGLE, new int[][]{{0, 0, 0, 16, 9, 16}, {0, 0, 0, 16, 9, 16}}, true));
    initTabBlock(new BlockResidentialBed("bed_single_navy", BedLayout.SINGLE, new int[][]{{0, 0, 0, 16, 9, 16}, {0, 0, 0, 16, 9, 16}}, true));
    initTabBlock(new BlockResidentialBed("bed_single_oatmeal", BedLayout.SINGLE, new int[][]{{0, 0, 0, 16, 9, 16}, {0, 0, 0, 16, 9, 16}}, true));
    initTabBlock(new BlockResidentialBed("bed_single_red", BedLayout.SINGLE, new int[][]{{0, 0, 0, 16, 9, 16}, {0, 0, 0, 16, 9, 16}}, true));

    // Double Bed
    initTabBlock(new BlockResidentialBed("bed_double_charcoal", BedLayout.WIDE, new int[][]{{0, 0, 0, 12, 9, 16}, {0, 0, 0, 12, 9, 16}, {4, 0, 0, 16, 9, 16}, {4, 0, 0, 16, 9, 16}}, true));
    initTabBlock(new BlockResidentialBed("bed_double_navy", BedLayout.WIDE, new int[][]{{0, 0, 0, 12, 9, 16}, {0, 0, 0, 12, 9, 16}, {4, 0, 0, 16, 9, 16}, {4, 0, 0, 16, 9, 16}}, true));
    initTabBlock(new BlockResidentialBed("bed_double_oatmeal", BedLayout.WIDE, new int[][]{{0, 0, 0, 12, 9, 16}, {0, 0, 0, 12, 9, 16}, {4, 0, 0, 16, 9, 16}, {4, 0, 0, 16, 9, 16}}, true));
    initTabBlock(new BlockResidentialBed("bed_double_red", BedLayout.WIDE, new int[][]{{0, 0, 0, 12, 9, 16}, {0, 0, 0, 12, 9, 16}, {4, 0, 0, 16, 9, 16}, {4, 0, 0, 16, 9, 16}}, true));

    // King Bed
    initTabBlock(new BlockResidentialBed("bed_king_charcoal", BedLayout.WIDE, new int[][]{{0, 0, 0, 16, 9, 16}, {0, 0, 0, 16, 9, 16}, {0, 0, 0, 16, 9, 16}, {0, 0, 0, 16, 9, 16}}, true));
    initTabBlock(new BlockResidentialBed("bed_king_navy", BedLayout.WIDE, new int[][]{{0, 0, 0, 16, 9, 16}, {0, 0, 0, 16, 9, 16}, {0, 0, 0, 16, 9, 16}, {0, 0, 0, 16, 9, 16}}, true));
    initTabBlock(new BlockResidentialBed("bed_king_oatmeal", BedLayout.WIDE, new int[][]{{0, 0, 0, 16, 9, 16}, {0, 0, 0, 16, 9, 16}, {0, 0, 0, 16, 9, 16}, {0, 0, 0, 16, 9, 16}}, true));
    initTabBlock(new BlockResidentialBed("bed_king_red", BedLayout.WIDE, new int[][]{{0, 0, 0, 16, 9, 16}, {0, 0, 0, 16, 9, 16}, {0, 0, 0, 16, 9, 16}, {0, 0, 0, 16, 9, 16}}, true));

    // Day Bed
    initTabBlock(new BlockResidentialBed("day_bed_oak", BedLayout.DAY, new int[][]{{0, 0, 1, 16, 7, 15}, {0, 0, 1, 16, 7, 15}}, false));
    initTabBlock(new BlockResidentialBed("day_bed_walnut", BedLayout.DAY, new int[][]{{0, 0, 1, 16, 7, 15}, {0, 0, 1, 16, 7, 15}}, false));
    initTabBlock(new BlockResidentialBed("day_bed_white", BedLayout.DAY, new int[][]{{0, 0, 1, 16, 7, 15}, {0, 0, 1, 16, 7, 15}}, false));

    // Bunk Bed
    initTabBlock(new BlockResidentialBed("bunk_bed_oak", BedLayout.BUNK, new int[][]{{0, 0, 0, 16, 6, 16}, {0, 0, 0, 16, 6, 16}, {0, 1, 0, 16, 5, 16}, {0, 1, 0, 16, 5, 16}}, false));
    initTabBlock(new BlockResidentialBed("bunk_bed_walnut", BedLayout.BUNK, new int[][]{{0, 0, 0, 16, 6, 16}, {0, 0, 0, 16, 6, 16}, {0, 1, 0, 16, 5, 16}, {0, 1, 0, 16, 5, 16}}, false));
    initTabBlock(new BlockResidentialBed("bunk_bed_white", BedLayout.BUNK, new int[][]{{0, 0, 0, 16, 6, 16}, {0, 0, 0, 16, 6, 16}, {0, 1, 0, 16, 5, 16}, {0, 1, 0, 16, 5, 16}}, false));

    // Nightstand
    initTabBlock(new BlockResidentialStorage("nightstand_oak", new int[]{2, 0, 3, 14, 9, 16}, 9, FurnishingsSounds.DRAWER_OPEN, FurnishingsSounds.DRAWER_CLOSE));
    initTabBlock(new BlockResidentialStorage("nightstand_walnut", new int[]{2, 0, 3, 14, 9, 16}, 9, FurnishingsSounds.DRAWER_OPEN, FurnishingsSounds.DRAWER_CLOSE));
    initTabBlock(new BlockResidentialStorage("nightstand_white", new int[]{2, 0, 3, 14, 9, 16}, 9, FurnishingsSounds.DRAWER_OPEN, FurnishingsSounds.DRAWER_CLOSE));

    // Dresser
    initTabBlock(new BlockResidentialStorage("dresser_oak", new int[]{0, 0, 4, 16, 14, 16}, 27, FurnishingsSounds.DRAWER_OPEN, FurnishingsSounds.DRAWER_CLOSE));
    initTabBlock(new BlockResidentialStorage("dresser_walnut", new int[]{0, 0, 4, 16, 14, 16}, 27, FurnishingsSounds.DRAWER_OPEN, FurnishingsSounds.DRAWER_CLOSE));
    initTabBlock(new BlockResidentialStorage("dresser_white", new int[]{0, 0, 4, 16, 14, 16}, 27, FurnishingsSounds.DRAWER_OPEN, FurnishingsSounds.DRAWER_CLOSE));

    // Dresser with Mirror
    initTabBlock(new BlockResidentialTall("dresser_mirror_oak", new int[]{0, 0, 4, 16, 29, 16}, false, 27, FurnishingsSounds.DRAWER_OPEN, FurnishingsSounds.DRAWER_CLOSE));
    initTabBlock(new BlockResidentialTall("dresser_mirror_walnut", new int[]{0, 0, 4, 16, 29, 16}, false, 27, FurnishingsSounds.DRAWER_OPEN, FurnishingsSounds.DRAWER_CLOSE));
    initTabBlock(new BlockResidentialTall("dresser_mirror_white", new int[]{0, 0, 4, 16, 29, 16}, false, 27, FurnishingsSounds.DRAWER_OPEN, FurnishingsSounds.DRAWER_CLOSE));

    // Wardrobe
    initTabBlock(new BlockResidentialTall("wardrobe_oak", new int[]{0, 0, 2, 16, 30, 16}, false, 27, FurnishingsSounds.CABINET_OPEN, FurnishingsSounds.CABINET_CLOSE));
    initTabBlock(new BlockResidentialTall("wardrobe_walnut", new int[]{0, 0, 2, 16, 30, 16}, false, 27, FurnishingsSounds.CABINET_OPEN, FurnishingsSounds.CABINET_CLOSE));
    initTabBlock(new BlockResidentialTall("wardrobe_white", new int[]{0, 0, 2, 16, 30, 16}, false, 27, FurnishingsSounds.CABINET_OPEN, FurnishingsSounds.CABINET_CLOSE));

    // Closet
    initTabBlock(new BlockCloset("closet_white", new int[]{0, 0, 3, 16, 32, 16}, 27, FurnishingsSounds.CABINET_OPEN, FurnishingsSounds.CABINET_CLOSE));

    // Blanket Chest
    initTabBlock(new BlockResidentialStorage("blanket_chest_oak", new int[]{1, 0, 4, 15, 7, 13}, 18, FurnishingsSounds.CABINET_OPEN, FurnishingsSounds.CABINET_CLOSE));
    initTabBlock(new BlockResidentialStorage("blanket_chest_walnut", new int[]{1, 0, 4, 15, 7, 13}, 18, FurnishingsSounds.CABINET_OPEN, FurnishingsSounds.CABINET_CLOSE));
    initTabBlock(new BlockResidentialStorage("blanket_chest_white", new int[]{1, 0, 4, 15, 7, 13}, 18, FurnishingsSounds.CABINET_OPEN, FurnishingsSounds.CABINET_CLOSE));

    // Desk
    initTabBlock(new BlockResidentialStorage("desk_oak", new int[]{0, 0, 3, 16, 12, 16}, 9, FurnishingsSounds.DRAWER_OPEN, FurnishingsSounds.DRAWER_CLOSE));
    initTabBlock(new BlockResidentialStorage("desk_walnut", new int[]{0, 0, 3, 16, 12, 16}, 9, FurnishingsSounds.DRAWER_OPEN, FurnishingsSounds.DRAWER_CLOSE));
    initTabBlock(new BlockResidentialStorage("desk_white", new int[]{0, 0, 3, 16, 12, 16}, 9, FurnishingsSounds.DRAWER_OPEN, FurnishingsSounds.DRAWER_CLOSE));

    // Desk Chair
    initTabBlock(new BlockResidentialFurniture("desk_chair_charcoal", new int[]{3, 0, 3, 13, 16, 14}, true, 8, 0.5, 0));
    initTabBlock(new BlockResidentialFurniture("desk_chair_navy", new int[]{3, 0, 3, 13, 16, 14}, true, 8, 0.5, 0));
    initTabBlock(new BlockResidentialFurniture("desk_chair_oatmeal", new int[]{3, 0, 3, 13, 16, 14}, true, 8, 0.5, 0));
    initTabBlock(new BlockResidentialFurniture("desk_chair_red", new int[]{3, 0, 3, 13, 16, 14}, true, 8, 0.5, 0));

    // Standing Mirror
    initTabBlock(new BlockResidentialTall("standing_mirror_oak", new int[]{3, 0, 5, 13, 28, 11}, false, 0, null, null));
    initTabBlock(new BlockResidentialTall("standing_mirror_walnut", new int[]{3, 0, 5, 13, 28, 11}, false, 0, null, null));
    initTabBlock(new BlockResidentialTall("standing_mirror_white", new int[]{3, 0, 5, 13, 28, 11}, false, 0, null, null));

    // Vanity Table
    initTabBlock(new BlockResidentialTall("vanity_oak", new int[]{0, 0, 5, 16, 26, 16}, false, 9, FurnishingsSounds.DRAWER_OPEN, FurnishingsSounds.DRAWER_CLOSE));
    initTabBlock(new BlockResidentialTall("vanity_walnut", new int[]{0, 0, 5, 16, 26, 16}, false, 9, FurnishingsSounds.DRAWER_OPEN, FurnishingsSounds.DRAWER_CLOSE));
    initTabBlock(new BlockResidentialTall("vanity_white", new int[]{0, 0, 5, 16, 26, 16}, false, 9, FurnishingsSounds.DRAWER_OPEN, FurnishingsSounds.DRAWER_CLOSE));

    // Vanity Stool
    initTabBlock(new BlockResidentialFurniture("vanity_stool_oak", new int[]{4, 0, 4, 12, 8, 12}, false, 8.25, 0, 0));
    initTabBlock(new BlockResidentialFurniture("vanity_stool_walnut", new int[]{4, 0, 4, 12, 8, 12}, false, 8.25, 0, 0));
    initTabBlock(new BlockResidentialFurniture("vanity_stool_white", new int[]{4, 0, 4, 12, 8, 12}, false, 8.25, 0, 0));

    // Crib
    initTabBlock(new BlockResidentialFurniture("crib_oak", new int[]{0, 0, 3, 16, 14, 14}, false));
    initTabBlock(new BlockResidentialFurniture("crib_walnut", new int[]{0, 0, 3, 16, 14, 14}, false));
    initTabBlock(new BlockResidentialFurniture("crib_white", new int[]{0, 0, 3, 16, 14, 14}, false));

    // Cradle with Drawers
    initTabBlock(new BlockResidentialStorage("cradle_with_drawers_oak", new int[]{1, 0, 3, 15, 13, 14}, 9, FurnishingsSounds.DRAWER_OPEN, FurnishingsSounds.DRAWER_CLOSE));
    initTabBlock(new BlockResidentialStorage("cradle_with_drawers_walnut", new int[]{1, 0, 3, 15, 13, 14}, 9, FurnishingsSounds.DRAWER_OPEN, FurnishingsSounds.DRAWER_CLOSE));
    initTabBlock(new BlockResidentialStorage("cradle_with_drawers_white", new int[]{1, 0, 3, 15, 13, 14}, 9, FurnishingsSounds.DRAWER_OPEN, FurnishingsSounds.DRAWER_CLOSE));

    // Changing Table
    initTabBlock(new BlockResidentialStorage("changing_table_oak", new int[]{0, 0, 3, 16, 15, 15}, 9, FurnishingsSounds.DRAWER_OPEN, FurnishingsSounds.DRAWER_CLOSE));
    initTabBlock(new BlockResidentialStorage("changing_table_walnut", new int[]{0, 0, 3, 16, 15, 15}, 9, FurnishingsSounds.DRAWER_OPEN, FurnishingsSounds.DRAWER_CLOSE));
    initTabBlock(new BlockResidentialStorage("changing_table_white", new int[]{0, 0, 3, 16, 15, 15}, 9, FurnishingsSounds.DRAWER_OPEN, FurnishingsSounds.DRAWER_CLOSE));

    // Rocking Chair
    initTabBlock(new BlockResidentialFurniture("rocking_chair_oak", new int[]{2, 0, 1, 14, 15, 15}, false, 7.5, 0.5, 0));
    initTabBlock(new BlockResidentialFurniture("rocking_chair_walnut", new int[]{2, 0, 1, 14, 15, 15}, false, 7.5, 0.5, 0));
    initTabBlock(new BlockResidentialFurniture("rocking_chair_white", new int[]{2, 0, 1, 14, 15, 15}, false, 7.5, 0.5, 0));

    // Rug
    initTabBlock(new BlockRug("rug_charcoal"));
    initTabBlock(new BlockRug("rug_navy"));
    initTabBlock(new BlockRug("rug_oatmeal"));
    initTabBlock(new BlockRug("rug_red"));

    // ---- Bathroom ----
    // Toilet
    initTabBlock(new BlockToilet("toilet_white", new int[]{3, 0, 2, 13, 16, 16}));

    // Toilet Paper Holder
    initTabBlock(new BlockBathroomFixture("toilet_paper_holder_chrome", new int[]{4, 8, 11, 12, 13, 16}, FixtureMaterial.METAL));

    // Pedestal Sink
    initTabBlock(new BlockBasin("pedestal_sink_white", new int[]{2, 0, 3, 14, 16, 16}, FixtureMaterial.PORCELAIN));

    // Bathroom Vanity
    initTabBlock(new BlockBathroomVanity("bathroom_vanity_oak"));
    initTabBlock(new BlockBathroomVanity("bathroom_vanity_walnut"));
    initTabBlock(new BlockBathroomVanity("bathroom_vanity_white"));

    // Mirror Cabinet
    initTabBlock(new BlockResidentialStorage("mirror_cabinet_oak", new int[]{1, 1, 12, 15, 15, 16}, 9, FurnishingsSounds.CABINET_OPEN, FurnishingsSounds.CABINET_CLOSE));
    initTabBlock(new BlockResidentialStorage("mirror_cabinet_walnut", new int[]{1, 1, 12, 15, 15, 16}, 9, FurnishingsSounds.CABINET_OPEN, FurnishingsSounds.CABINET_CLOSE));
    initTabBlock(new BlockResidentialStorage("mirror_cabinet_white", new int[]{1, 1, 12, 15, 15, 16}, 9, FurnishingsSounds.CABINET_OPEN, FurnishingsSounds.CABINET_CLOSE));

    // Bathtub
    initTabBlock(new BlockBathtub("bathtub_white"));

    // Shower Enclosure
    initTabBlock(new BlockShower("shower_enclosure_chrome", new int[]{0, 0, 0, 16, 31, 16}));

    // Shower Head
    initTabBlock(new BlockShowerHead("shower_head_chrome", new int[]{5, 3, 8, 11, 14, 16}));

    // Towel Rail
    initTabBlock(new BlockBathroomFixture("towel_rail_chrome", new int[]{2, 6, 13, 14, 14, 16}, FixtureMaterial.METAL));

    // Heated Towel Rail
    initTabBlock(new BlockBathroomFixture("heated_towel_rail_chrome", new int[]{1, 1, 13, 15, 16, 16}, FixtureMaterial.METAL));

    // Bathroom Radiator
    initTabBlock(new BlockBathroomFixture("bathroom_radiator_white", new int[]{0, 0, 13, 16, 14, 16}, FixtureMaterial.METAL));

    // Wastebasket
    initTabBlock(new BlockResidentialStorage("wastebasket_stainless", new int[]{4, 0, 4, 12, 9, 13}, 9, FurnishingsSounds.JAR_LID, null));
    initTabBlock(new BlockResidentialStorage("wastebasket_white", new int[]{4, 0, 4, 12, 9, 13}, 9, FurnishingsSounds.JAR_LID, null));

    // Toiletries Tray
    initTabBlock(new BlockCounterPiece("toiletries_tray_white", new int[]{3, 0, 5, 13, 6, 11}, Material.GLASS, SoundType.GLASS, BlockRenderLayer.SOLID));

    // Toilet Brush
    initTabBlock(new BlockCounterPiece("toilet_brush_stainless", new int[]{6, 0, 6, 10, 11, 10}, Material.WOOD, SoundType.METAL, BlockRenderLayer.SOLID));

    // Bath Mat
    initTabBlock(new BlockRug("bath_mat_white"));
    initTabBlock(new BlockRug("bath_mat_blue"));
    initTabBlock(new BlockRug("bath_mat_grey"));

    // ---- Commercial restroom ----
    // Urinal
    initTabBlock(new BlockBathroomFixture("urinal_white", new int[]{4, 3, 7, 12, 16, 16}, FixtureMaterial.PORCELAIN, FurnishingsSounds.TOILET_FLUSH, 1.3F));

    // Soap Dispenser
    initTabBlock(new BlockBathroomFixture("soap_dispenser_white", new int[]{6, 7, 12, 10, 14, 16}, FixtureMaterial.PLASTIC));

    // Paper Towel Dispenser
    initTabBlock(new BlockBathroomFixture("paper_towel_dispenser_stainless", new int[]{3, 4, 11, 13, 15, 16}, FixtureMaterial.METAL));

    // Grab Bar
    initTabBlock(new BlockBathroomFixture("grab_bar_stainless", new int[]{1, 12, 13, 15, 15, 16}, FixtureMaterial.METAL));

    // Baby Changing Station
    initTabBlock(new BlockFoldingFixture("baby_changing_station_grey", new int[]{1, 0, 12, 15, 16, 16}, new int[]{1, 0, 2, 15, 4, 16}, FixtureMaterial.PLASTIC));

    // ---- Laundry ----
    // Washing Machine
    initTabBlock(new BlockLaundryAppliance("washing_machine_white", new int[]{0, 0, 0, 16, 15, 16}, LaundryAppliances.WASHING_MACHINE));
    initTabBlock(new BlockLaundryAppliance("washing_machine_stainless", new int[]{0, 0, 0, 16, 15, 16}, LaundryAppliances.WASHING_MACHINE));

    // Tumble Dryer
    initTabBlock(new BlockLaundryAppliance("dryer_white", new int[]{0, 0, 0, 16, 15, 16}, LaundryAppliances.DRYER));
    initTabBlock(new BlockLaundryAppliance("dryer_stainless", new int[]{0, 0, 0, 16, 15, 16}, LaundryAppliances.DRYER));

    // Steam Iron
    initTabBlock(new BlockCounterPiece("steam_iron_blue", new int[]{5, 0, 2, 11, 4, 12}, Material.WOOD, SoundType.METAL, BlockRenderLayer.SOLID, FurnishingsSounds.IRON_STEAM, 1.0F, new double[]{8, 0.5, 3.25}));

    // Ironing Board
    initTabBlock(new BlockBathroomFixture("ironing_board_blue", new int[]{0, 0, 5, 16, 15, 11}, FixtureMaterial.METAL));
    initTabBlock(new BlockBathroomFixture("ironing_board_grey", new int[]{0, 0, 5, 16, 15, 11}, FixtureMaterial.METAL));

    // Laundry Basket
    initTabBlock(new BlockResidentialStorage("laundry_basket_wicker", new int[]{2, 0, 3, 14, 9, 13}, 9));
    initTabBlock(new BlockResidentialStorage("laundry_basket_white", new int[]{2, 0, 3, 14, 9, 13}, 9));

    // Laundry Tub
    initTabBlock(new BlockBasin("laundry_tub_white", new int[]{1, 0, 3, 15, 16, 16}, FixtureMaterial.PLASTIC));
  }
}

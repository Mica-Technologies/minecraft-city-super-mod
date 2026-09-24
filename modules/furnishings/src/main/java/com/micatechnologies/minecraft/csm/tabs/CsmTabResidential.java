package com.micatechnologies.minecraft.csm.tabs;

import com.micatechnologies.minecraft.csm.CsmRegistry;
import com.micatechnologies.minecraft.csm.codeutils.CsmTab;
import com.micatechnologies.minecraft.csm.furniture.residential.BlockBookcase;
import com.micatechnologies.minecraft.csm.furniture.residential.BlockDiningTable;
import com.micatechnologies.minecraft.csm.furniture.residential.BlockResidentialFurniture;
import com.micatechnologies.minecraft.csm.furniture.residential.BlockResidentialStorage;
import com.micatechnologies.minecraft.csm.furniture.residential.BlockSofa;
import com.micatechnologies.minecraft.csm.furniture.residential.BlockSofaCorner;
import net.minecraft.block.Block;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;

/**
 * The tab for the furniture of homes: living and dining so far (tables, chairs, stools,
 * bookcases, TV stands, sideboards, armchairs and sofas), each in three wood finishes or three
 * fabrics. The lines below are printed by {@code gen_furniture_residential.py --fragments}.
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
  }
}

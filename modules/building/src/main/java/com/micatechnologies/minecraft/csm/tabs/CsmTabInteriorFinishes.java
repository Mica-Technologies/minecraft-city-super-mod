package com.micatechnologies.minecraft.csm.tabs;

import com.micatechnologies.minecraft.csm.CsmRegistry;
import com.micatechnologies.minecraft.csm.buildingmaterials.BlockCT50s1;
import com.micatechnologies.minecraft.csm.buildingmaterials.BlockCT50s2;
import com.micatechnologies.minecraft.csm.buildingmaterials.BlockCT50s3;
import com.micatechnologies.minecraft.csm.buildingmaterials.BlockCTF;
import com.micatechnologies.minecraft.csm.buildingmaterials.BlockCTFD;
import com.micatechnologies.minecraft.csm.buildingmaterials.BlockCTS1;
import com.micatechnologies.minecraft.csm.buildingmaterials.BlockCTS2;
import com.micatechnologies.minecraft.csm.buildingmaterials.BlockCTS3;
import com.micatechnologies.minecraft.csm.buildingmaterials.BlockDCT1;
import com.micatechnologies.minecraft.csm.buildingmaterials.BlockDCT2;
import com.micatechnologies.minecraft.csm.buildingmaterials.BlockDCT3;
import com.micatechnologies.minecraft.csm.buildingmaterials.BlockFloorFinish;
import com.micatechnologies.minecraft.csm.buildingmaterials.BlockSetPolishedConcrete;
import com.micatechnologies.minecraft.csm.buildingmaterials.BlockSetHardwoodOak;
import com.micatechnologies.minecraft.csm.buildingmaterials.BlockSetHardwoodWalnut;
import com.micatechnologies.minecraft.csm.buildingmaterials.BlockCornerGuard;
import com.micatechnologies.minecraft.csm.buildingmaterials.BlockPCC;
import com.micatechnologies.minecraft.csm.buildingmaterials.BlockWallFinish;
import com.micatechnologies.minecraft.csm.buildingmaterials.BlockWindowTreatment;
import com.micatechnologies.minecraft.csm.codeutils.CsmTab;
import net.minecraft.block.Block;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;

/**
 * The tab for interior finish blocks: the surfaces that go on last.
 *
 * <p>The ceiling finishes moved here from {@link CsmTabBuildingMaterials}, which had been holding
 * them because it was the only tab this module owned. They are registered here in the order they
 * appeared there, so their order relative to one another is unchanged; their position in the
 * overall registry moves, because this tab is visited later.</p>
 *
 * @version 1.0
 */
@CsmTab.Load(order = 15)
public class CsmTabInteriorFinishes extends CsmTab {

  /**
   * Gets a boolean indicating if the tab is hidden (not displayed in the inventory).
   *
   * @return {@code true} if the tab is hidden, otherwise {@code false}
   *
   * @since 1.0
   */
  @Override
  public boolean getTabHidden() {
    return false;
  }

  /**
   * Gets the ID (unique identifier) of the tab.
   *
   * @return the ID of the tab
   *
   * @since 1.0
   */
  @Override
  public String getTabId() {
    return "tabinteriorfinishes";
  }

  /**
   * Gets the block to use as the icon of the tab
   *
   * @return the block to use as the icon of the tab
   *
   * @since 1.0
   */
  @Override
  public Block getTabIcon() {
    return CsmRegistry.getBlock("pcc");
  }

  /**
   * Gets a boolean indicating if the tab is searchable (has its own search bar).
   *
   * @return {@code true} if the tab is searchable, otherwise {@code false}
   *
   * @since 1.0
   */
  @Override
  public boolean getTabSearchable() {
    return false;
  }

  /**
   * Initializes all the items belonging to the tab.
   *
   * @since 1.0
   */
  @Override
  public void initTabElements(FMLPreInitializationEvent fmlPreInitializationEvent) {
    initTabBlock(BlockPCC.class, fmlPreInitializationEvent); // PCC
    initTabBlock(BlockCTF.class, fmlPreInitializationEvent); // CTF
    initTabBlock(BlockCTFD.class, fmlPreInitializationEvent); // CTFD
    initTabBlock(BlockCT50s1.class, fmlPreInitializationEvent); // CT50s1
    initTabBlock(BlockCT50s2.class, fmlPreInitializationEvent); // CT50s2
    initTabBlock(BlockCT50s3.class, fmlPreInitializationEvent); // CT50s3
    initTabBlock(BlockCTS1.class, fmlPreInitializationEvent); // CTS1
    initTabBlock(BlockCTS2.class, fmlPreInitializationEvent); // CTS2
    initTabBlock(BlockCTS3.class, fmlPreInitializationEvent); // CTS3
    initTabBlock(BlockDCT1.class, fmlPreInitializationEvent); // DCT1
    initTabBlock(BlockDCT2.class, fmlPreInitializationEvent); // DCT2
    initTabBlock(BlockDCT3.class, fmlPreInitializationEvent); // DCT3

    // Window treatments: blinds, shades and curtains, hung against a window.
    initTabBlock(new BlockWindowTreatment("blind_venetian_white")); // Venetian Blind (White)
    initTabBlock(new BlockWindowTreatment("shade_roller_white")); // Roller Shade (White)
    initTabBlock(new BlockWindowTreatment("shade_roller_grey")); // Roller Shade (Grey)
    initTabBlock(new BlockWindowTreatment("shade_roller_blackout")); // Roller Shade (Blackout)
    initTabBlock(new BlockWindowTreatment("blind_vertical_white")); // Vertical Blind (White)
    initTabBlock(new BlockWindowTreatment("curtain_beige")); // Curtain (Beige)
    initTabBlock(new BlockWindowTreatment("curtain_grey")); // Curtain (Grey)
    initTabBlock(new BlockWindowTreatment("curtain_navy")); // Curtain (Navy)
    initTabBlock(new BlockWindowTreatment("curtain_sheer")); // Sheer Curtain

    // Flooring: finishes laid over any floor, and the sets built of them.
    initTabBlock(new BlockFloorFinish("floor_carpet_grey")); // Carpet Tile (Grey)
    initTabBlock(new BlockFloorFinish("floor_carpet_blue")); // Carpet Tile (Blue)
    initTabBlock(new BlockFloorFinish("floor_carpet_charcoal")); // Carpet Tile (Charcoal)
    initTabBlock(new BlockFloorFinish("floor_vct_white")); // Vinyl Composition Tile (White)
    initTabBlock(new BlockFloorFinish("floor_vct_beige")); // Vinyl Composition Tile (Beige)
    initTabBlock(new BlockFloorFinish("floor_ceramic_white")); // Ceramic Floor Tile (White)
    initTabBlock(new BlockFloorFinish("floor_ceramic_grey")); // Ceramic Floor Tile (Grey)
    initTabBlock(new BlockFloorFinish("floor_hardwood_oak")); // Hardwood Floor (Oak)
    initTabBlock(new BlockFloorFinish("floor_hardwood_walnut")); // Hardwood Floor (Walnut)
    initTabBlock(new BlockFloorFinish("floor_polished_concrete")); // Polished Concrete Floor
    initTabBlock(new BlockFloorFinish("floor_rubber_studded")); // Rubber Floor (Studded)
    initTabBlock(BlockSetPolishedConcrete.class,
        fmlPreInitializationEvent); // Polished Concrete Set (Block, Fence, Slab, Stairs)
    initTabBlock(BlockSetHardwoodOak.class,
        fmlPreInitializationEvent); // Oak Hardwood Set (Block, Fence, Slab, Stairs)
    initTabBlock(BlockSetHardwoodWalnut.class,
        fmlPreInitializationEvent); // Walnut Hardwood Set (Block, Fence, Slab, Stairs)

    // Wall finishes hung on any wall, and corner guards.
    initTabBlock(new BlockWallFinish("wall_paint_white")); // Painted Drywall (White)
    initTabBlock(new BlockWallFinish("wall_paint_offwhite")); // Painted Drywall (Off-White)
    initTabBlock(new BlockWallFinish("wall_paint_greige")); // Painted Drywall (Greige)
    initTabBlock(new BlockWallFinish("wall_paint_grey")); // Painted Drywall (Light Grey)
    initTabBlock(new BlockWallFinish("wall_paint_blue")); // Painted Drywall (Pale Blue)
    initTabBlock(new BlockWallFinish("wall_paint_sage")); // Painted Drywall (Sage)
    initTabBlock(new BlockWallFinish("wall_tile_subway_white")); // Ceramic Wall Tile (White Subway)
    initTabBlock(new BlockWallFinish("wall_tile_subway_green")); // Ceramic Wall Tile (Green Subway)
    initTabBlock(new BlockWallFinish("wall_tile_square_white")); // Ceramic Wall Tile (White Square)
    initTabBlock(new BlockWallFinish("wall_acoustic_grey")); // Acoustic Wall Panel (Grey)
    initTabBlock(new BlockWallFinish("wall_acoustic_blue")); // Acoustic Wall Panel (Blue)
    initTabBlock(new BlockWallFinish("wall_acoustic_charcoal")); // Acoustic Wall Panel (Charcoal)
    initTabBlock(new BlockWallFinish("wall_beadboard_white")); // Beadboard (White)
    initTabBlock(new BlockWallFinish("wall_slatwall_oak")); // Wood Slat Wall (Oak)
    initTabBlock(new BlockCornerGuard("corner_guard_steel")); // Corner Guard (Stainless)
    initTabBlock(new BlockCornerGuard("corner_guard_white")); // Corner Guard (White Vinyl)
  }
}

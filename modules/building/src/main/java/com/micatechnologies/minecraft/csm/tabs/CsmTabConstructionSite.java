package com.micatechnologies.minecraft.csm.tabs;

import com.micatechnologies.minecraft.csm.CsmRegistry;
import com.micatechnologies.minecraft.csm.buildingmaterials.BlockBuildingDoor;
import com.micatechnologies.minecraft.csm.codeutils.CsmTab;
import com.micatechnologies.minecraft.csm.constructionsite.BlockCraneHead;
import com.micatechnologies.minecraft.csm.constructionsite.BlockCraneMast;
import com.micatechnologies.minecraft.csm.constructionsite.BlockCraneMastLarge;
import com.micatechnologies.minecraft.csm.constructionsite.BlockFormworkWall;
import com.micatechnologies.minecraft.csm.constructionsite.BlockJobTrailer;
import com.micatechnologies.minecraft.csm.constructionsite.BlockPortableToilet;
import com.micatechnologies.minecraft.csm.constructionsite.BlockPostShore;
import com.micatechnologies.minecraft.csm.constructionsite.BlockRebarBundle;
import com.micatechnologies.minecraft.csm.constructionsite.BlockScaffoldFrame;
import com.micatechnologies.minecraft.csm.constructionsite.BlockSiteAxialProp;
import com.micatechnologies.minecraft.csm.constructionsite.BlockSiteFacingProp;
import com.micatechnologies.minecraft.csm.constructionsite.BlockSiteFence;
import com.micatechnologies.minecraft.csm.constructionsite.BlockSiteProp;
import com.micatechnologies.minecraft.csm.constructionsite.BlockSiteShell;
import com.micatechnologies.minecraft.csm.constructionsite.BlockStockpile;
import com.micatechnologies.minecraft.csm.constructionsite.BlockTrenchBox;
import com.micatechnologies.minecraft.csm.constructionsite.BlockTrenchPlate;
import com.micatechnologies.minecraft.csm.constructionsite.ItemScaffoldCasters;
import com.micatechnologies.minecraft.csm.constructionsite.ItemScaffoldLadderFrame;
import com.micatechnologies.minecraft.csm.constructionsite.ItemScaffoldNetting;
import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;

/**
 * The tab for the construction site: what stands around a building while it goes up.
 *
 * <p>Stops at the road edge. Cones, drums, barricades, the safety fence and the temporary barriers
 * belong to {@code csm_roads}' work zone and are not repeated here.</p>
 *
 * @version 1.0
 * @since 2026.9
 */
@CsmTab.Load(order = 16)
public class CsmTabConstructionSite extends CsmTab {

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
    return "tabconstructionsite";
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
    return CsmRegistry.getBlock("scaffold_frame");
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
    initTabBlock(BlockScaffoldFrame.class, fmlPreInitializationEvent); // Frame Scaffold
    initTabItem(ItemScaffoldLadderFrame.class, fmlPreInitializationEvent); // Scaffold Ladder Frame
    initTabItem(ItemScaffoldNetting.class, fmlPreInitializationEvent); // Scaffold Debris Netting
    initTabItem(ItemScaffoldCasters.class, fmlPreInitializationEvent); // Scaffold Casters

    // Formwork, shoring and rebar: the concrete half of the site.
    initTabBlock(BlockFormworkWall.class, fmlPreInitializationEvent); // Wall Formwork
    initTabBlock(new BlockSiteProp("formwork_column", Material.WOOD, SoundType.WOOD, "axe",
        BlockSiteProp.box16(2, 0, 2, 14, 16, 14))); // Column Formwork
    initTabBlock(BlockPostShore.class, fmlPreInitializationEvent); // Post Shore
    initTabBlock(new BlockSiteProp("rebar_mat", Material.IRON, SoundType.METAL, "pickaxe",
        BlockSiteProp.box16(0, 0, 0, 16, 2.5, 16))); // Rebar Mat
    initTabBlock(new BlockSiteProp("rebar_dowels", Material.IRON, SoundType.METAL, "pickaxe",
        BlockSiteProp.box16(3, 0, 3, 13, 14.5, 13))); // Rebar Dowels
    initTabBlock(new BlockSiteProp("rebar_cage", Material.IRON, SoundType.METAL, "pickaxe",
        BlockSiteProp.box16(3.4, 0, 3.4, 12.6, 16, 12.6))); // Rebar Column Cage
    initTabBlock(BlockRebarBundle.class, fmlPreInitializationEvent); // Rebar Bundle

    // Fencing and erosion control.
    initTabBlock(new BlockSiteFence("temp_fence")); // Temporary Fence
    initTabBlock(new BlockSiteFence("temp_fence_screened")); // Temporary Fence (Privacy Screen)
    initTabBlock(new BlockSiteFence("silt_fence")); // Silt Fence

    // Earthworks.
    initTabBlock(BlockTrenchPlate.class, fmlPreInitializationEvent); // Trench Plate
    initTabBlock(BlockTrenchBox.class, fmlPreInitializationEvent); // Trench Box
    initTabBlock(new BlockStockpile("stockpile_soil", Material.GROUND)); // Soil Stockpile
    initTabBlock(new BlockStockpile("stockpile_gravel",
        Material.GROUND)); // Gravel Stockpile
    initTabBlock(new BlockStockpile("stockpile_sand", Material.SAND)); // Sand Stockpile

    // Site logistics.
    initTabBlock(new BlockSiteAxialProp("pallet_brick", Material.WOOD, SoundType.WOOD, "axe",
        BlockSiteProp.box16(0.5, 0, 0.5, 15.5, 12.6, 15.5))); // Brick Pallet
    initTabBlock(new BlockSiteAxialProp("pallet_cmu", Material.WOOD, SoundType.WOOD, "axe",
        BlockSiteProp.box16(0.5, 0, 0.5, 15.5, 13.5, 15.5))); // Concrete Block Pallet
    initTabBlock(new BlockSiteAxialProp("pallet_drywall", Material.WOOD, SoundType.WOOD, "axe",
        BlockSiteProp.box16(0.5, 0, 0.5, 15.5, 10, 15.5))); // Drywall Pallet
    initTabBlock(new BlockSiteAxialProp("pallet_concrete_bags",
        Material.WOOD, SoundType.WOOD, "axe",
        BlockSiteProp.box16(0.5, 0, 0.5, 15.5, 11.5, 15.5))); // Bagged Concrete Pallet
    initTabBlock(new BlockSiteAxialProp("lumber_stack", Material.WOOD, SoundType.WOOD, "axe",
        BlockSiteProp.box16(0, 0, 1, 16, 11.5, 15))); // Lumber Stack
    initTabBlock(new BlockSiteAxialProp("insulation_rolls", Material.CLOTH, SoundType.CLOTH, "axe",
        BlockSiteProp.box16(0, 0, 0, 16, 16, 16))); // Insulation Rolls
    initTabBlock(new BlockSiteAxialProp("pipe_bundle", Material.WOOD, SoundType.WOOD, "axe",
        BlockSiteProp.box16(0, 0, 2, 16, 9, 14))); // PVC Pipe Bundle
    initTabBlock(new BlockSiteAxialProp("conduit_bundle", Material.IRON, SoundType.METAL, "pickaxe",
        BlockSiteProp.box16(0, 0, 4, 16, 6, 12))); // Conduit Bundle
    initTabBlock(new BlockSiteAxialProp("wire_spool", Material.WOOD, SoundType.WOOD, "axe",
        BlockSiteProp.box16(1, 0, 1, 15, 15, 15))); // Wire Spool

    // Site facilities.
    initTabBlock(new BlockSiteShell("container_blue")); // Shipping Container (Blue)
    initTabBlock(new BlockSiteShell("container_maroon")); // Shipping Container (Maroon)
    initTabBlock(new BlockSiteShell("container_green")); // Shipping Container (Green)
    initTabBlock(new BlockSiteShell("container_grey")); // Shipping Container (Grey)
    initTabBlock(new BlockSiteShell("dumpster_blue")); // Roll-Off Dumpster (Blue)
    initTabBlock(new BlockSiteShell("dumpster_maroon")); // Roll-Off Dumpster (Maroon)
    initTabBlock(new BlockSiteShell("dumpster_green")); // Roll-Off Dumpster (Green)
    initTabBlock(new BlockSiteShell("dumpster_grey")); // Roll-Off Dumpster (Grey)
    initTabBlock(new BlockJobTrailer("job_trailer_wall")); // Job Trailer Wall
    initTabBlock(new BlockJobTrailer("job_trailer_window")); // Job Trailer Window
    initTabBlock(new BlockBuildingDoor("job_trailer_door")); // Job Trailer Door
    initTabBlock(new BlockPortableToilet()); // Portable Toilet
    initTabBlock(new BlockSiteFacingProp("gang_box", Material.IRON, SoundType.METAL, "pickaxe",
        BlockSiteProp.box16(0, 0, 2.5, 16, 10.5, 13.5))); // Gang Box
    initTabBlock(new BlockSiteFacingProp("concrete_washout",
        Material.IRON, SoundType.METAL, "pickaxe",
        BlockSiteProp.box16(0, 0, 0, 16, 5, 16))); // Concrete Washout

    // The tower crane.
    initTabBlock(BlockCraneMast.class, fmlPreInitializationEvent); // Tower Crane Mast
    initTabBlock(BlockCraneMastLarge.class, fmlPreInitializationEvent); // Tower Crane Mast (2x2)
    initTabBlock(BlockCraneHead.class, fmlPreInitializationEvent); // Tower Crane Head
  }
}

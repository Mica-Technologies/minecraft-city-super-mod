package com.micatechnologies.minecraft.csm.tabs;

import com.micatechnologies.minecraft.csm.CsmRegistry;
import com.micatechnologies.minecraft.csm.buildingmaterials.BlockSetCmuGlazed;
import com.micatechnologies.minecraft.csm.buildingmaterials.BlockSetCmuGroundface;
import com.micatechnologies.minecraft.csm.buildingmaterials.BlockSetCmuSplitface;
import com.micatechnologies.minecraft.csm.buildingmaterials.BlockSetCmuStandard;
import com.micatechnologies.minecraft.csm.buildingmaterials.BlockGarageDoor;
import com.micatechnologies.minecraft.csm.buildingmaterials.BlockGarageDoorControl;
import com.micatechnologies.minecraft.csm.buildingmaterials.BlockGarageDoorHanger;
import com.micatechnologies.minecraft.csm.buildingmaterials.BlockGarageDoorOpener;
import com.micatechnologies.minecraft.csm.buildingmaterials.BlockGarageDoor;
import com.micatechnologies.minecraft.csm.buildingmaterials.BlockGlazing;
import com.micatechnologies.minecraft.csm.buildingmaterials.BlockGlazingPane;
import com.micatechnologies.minecraft.csm.buildingmaterials.BlockSetBlackMetal;
import com.micatechnologies.minecraft.csm.buildingmaterials.BlockSetBlueMetal;
import com.micatechnologies.minecraft.csm.buildingmaterials.BlockSetCopperMetal;
import com.micatechnologies.minecraft.csm.buildingmaterials.BlockSetGreenMetal;
import com.micatechnologies.minecraft.csm.buildingmaterials.BlockSetIridescentMetal;
import com.micatechnologies.minecraft.csm.buildingmaterials.BlockSetLightBlueMetal;
import com.micatechnologies.minecraft.csm.buildingmaterials.BlockSetLimeMetal;
import com.micatechnologies.minecraft.csm.buildingmaterials.BlockSetMagentaMetal;
import com.micatechnologies.minecraft.csm.buildingmaterials.BlockSetOrangeMetal;
import com.micatechnologies.minecraft.csm.buildingmaterials.BlockSetPinkMetal;
import com.micatechnologies.minecraft.csm.buildingmaterials.BlockSetPurpleMetal;
import com.micatechnologies.minecraft.csm.buildingmaterials.BlockSetRedMetal;
import com.micatechnologies.minecraft.csm.buildingmaterials.BlockSetSilverMetal;
import com.micatechnologies.minecraft.csm.buildingmaterials.BlockSetWhiteMetal;
import com.micatechnologies.minecraft.csm.buildingmaterials.BlockSetYellowMetal;
import com.micatechnologies.minecraft.csm.buildingmaterials.BlockBrickTrim;
import com.micatechnologies.minecraft.csm.buildingmaterials.BlockSetBrickBrown;
import com.micatechnologies.minecraft.csm.buildingmaterials.BlockSetBrickBuff;
import com.micatechnologies.minecraft.csm.buildingmaterials.BlockSetBrickGrey;
import com.micatechnologies.minecraft.csm.buildingmaterials.BlockSetBrickRed;
import com.micatechnologies.minecraft.csm.buildingmaterials.BlockSetStuccoSmooth;
import com.micatechnologies.minecraft.csm.buildingmaterials.BlockSetStuccoSandfloat;
import com.micatechnologies.minecraft.csm.buildingmaterials.BlockSetStuccoKnockdown;
import com.micatechnologies.minecraft.csm.buildingmaterials.BlockSetSidingLap;
import com.micatechnologies.minecraft.csm.buildingmaterials.BlockSetSidingBoardbatten;
import com.micatechnologies.minecraft.csm.buildingmaterials.BlockSetSidingShingle;
import com.micatechnologies.minecraft.csm.buildingmaterials.BlockSetSidingVinyl;
import com.micatechnologies.minecraft.csm.buildingmaterials.BlockSetCladdingCorrugated;
import com.micatechnologies.minecraft.csm.buildingmaterials.BlockSetCladdingStandingseam;
import com.micatechnologies.minecraft.csm.buildingmaterials.BlockSetCladdingInsulated;
import com.micatechnologies.minecraft.csm.buildingmaterials.BlockSetCladdingComposite;
import com.micatechnologies.minecraft.csm.buildingmaterials.BlockSetStoneAshlar;
import com.micatechnologies.minecraft.csm.buildingmaterials.BlockSetStoneFieldstone;
import com.micatechnologies.minecraft.csm.buildingmaterials.BlockSetStoneCast;
import com.micatechnologies.minecraft.csm.codeutils.CsmTab;
import com.micatechnologies.minecraft.csm.constructionsite.BlockSiteFence;
import net.minecraft.block.Block;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;

/**
 * The tab for building material blocks.
 *
 * @version 1.0
 */
@CsmTab.Load(order = 1)
public class CsmTabBuildingMaterials extends CsmTab {

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
    return "tabbuildingmaterials";
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
    return CsmRegistry.getBlock("silvermetal");
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
    initTabBlock(BlockSetBlackMetal.class,
        fmlPreInitializationEvent); // Black Metal Set (Block, Fence, Slab, Stairs)
    initTabBlock(BlockSetSilverMetal.class,
        fmlPreInitializationEvent); // Silver Metal Set (Block, Fence, Slab, Stairs)
    initTabBlock(BlockSetWhiteMetal.class,
        fmlPreInitializationEvent); // White Metal Set (Block, Fence, Slab, Stairs)
    initTabBlock(BlockSetGreenMetal.class,
        fmlPreInitializationEvent); // Green Metal Set (Block, Fence, Slab, Stairs)
    initTabBlock(BlockSetBlueMetal.class,
        fmlPreInitializationEvent); // Blue Metal Set (Block, Fence, Slab, Stairs)
    initTabBlock(BlockSetRedMetal.class,
        fmlPreInitializationEvent); // Red Metal Set (Block, Fence, Slab, Stairs)
    initTabBlock(BlockSetCopperMetal.class,
        fmlPreInitializationEvent); // Copper Metal Set (Block, Fence, Slab, Stairs)
    initTabBlock(BlockSetLightBlueMetal.class,
        fmlPreInitializationEvent); // Light Blue Metal Set (Block, Fence, Slab, Stairs)
    initTabBlock(BlockSetLimeMetal.class,
        fmlPreInitializationEvent); // Lime Metal Set (Block, Fence, Slab, Stairs)
    initTabBlock(BlockSetMagentaMetal.class,
        fmlPreInitializationEvent); // Magenta Metal Set (Block, Fence, Slab, Stairs)
    initTabBlock(BlockSetOrangeMetal.class,
        fmlPreInitializationEvent); // Orange Metal Set (Block, Fence, Slab, Stairs)
    initTabBlock(BlockSetPinkMetal.class,
        fmlPreInitializationEvent); // Pink Metal Set (Block, Fence, Slab, Stairs)
    initTabBlock(BlockSetPurpleMetal.class,
        fmlPreInitializationEvent); // Purple Metal Set (Block, Fence, Slab, Stairs)
    initTabBlock(BlockSetYellowMetal.class,
        fmlPreInitializationEvent); // Yellow Metal Set (Block, Fence, Slab, Stairs)
    initTabBlock(BlockSetIridescentMetal.class,
        fmlPreInitializationEvent); // Iridescent Metal Set (Block, Fence, Slab, Stairs)
    initTabBlock(BlockSetCmuStandard.class,
        fmlPreInitializationEvent); // Concrete Block Set (Block, Fence, Slab, Stairs)
    initTabBlock(BlockSetCmuSplitface.class,
        fmlPreInitializationEvent); // Split-Face Block Set (Block, Fence, Slab, Stairs)
    initTabBlock(BlockSetCmuGroundface.class,
        fmlPreInitializationEvent); // Ground-Face Block Set (Block, Fence, Slab, Stairs)
    initTabBlock(BlockSetCmuGlazed.class,
        fmlPreInitializationEvent); // Glazed Block Set (Block, Fence, Slab, Stairs)
    initTabBlock(BlockSetBrickRed.class,
        fmlPreInitializationEvent); // Red Brick Set (Block, Fence, Slab, Stairs)
    initTabBlock(new BlockBrickTrim("brick_red_soldier")); // Red Brick (Soldier Course)
    initTabBlock(new BlockBrickTrim("brick_red_header")); // Red Brick (Header Course)
    initTabBlock(new BlockBrickTrim("brick_red_weep")); // Red Brick (Weep Holes)
    initTabBlock(BlockSetBrickBrown.class,
        fmlPreInitializationEvent); // Brown Brick Set (Block, Fence, Slab, Stairs)
    initTabBlock(new BlockBrickTrim("brick_brown_soldier")); // Brown Brick (Soldier Course)
    initTabBlock(new BlockBrickTrim("brick_brown_header")); // Brown Brick (Header Course)
    initTabBlock(new BlockBrickTrim("brick_brown_weep")); // Brown Brick (Weep Holes)
    initTabBlock(BlockSetBrickBuff.class,
        fmlPreInitializationEvent); // Buff Brick Set (Block, Fence, Slab, Stairs)
    initTabBlock(new BlockBrickTrim("brick_buff_soldier")); // Buff Brick (Soldier Course)
    initTabBlock(new BlockBrickTrim("brick_buff_header")); // Buff Brick (Header Course)
    initTabBlock(new BlockBrickTrim("brick_buff_weep")); // Buff Brick (Weep Holes)
    initTabBlock(BlockSetBrickGrey.class,
        fmlPreInitializationEvent); // Grey Brick Set (Block, Fence, Slab, Stairs)
    initTabBlock(new BlockBrickTrim("brick_grey_soldier")); // Grey Brick (Soldier Course)
    initTabBlock(new BlockBrickTrim("brick_grey_header")); // Grey Brick (Header Course)
    initTabBlock(new BlockBrickTrim("brick_grey_weep")); // Grey Brick (Weep Holes)
    initTabBlock(BlockSetStuccoSmooth.class,
        fmlPreInitializationEvent); // Smooth Stucco Set (Block, Fence, Slab, Stairs)
    initTabBlock(BlockSetStuccoSandfloat.class,
        fmlPreInitializationEvent); // Sand Float Stucco Set (Block, Fence, Slab, Stairs)
    initTabBlock(BlockSetStuccoKnockdown.class,
        fmlPreInitializationEvent); // Knockdown Stucco Set (Block, Fence, Slab, Stairs)
    initTabBlock(BlockSetSidingLap.class,
        fmlPreInitializationEvent); // Fiber Cement Lap Siding Set (Block, Fence, Slab, Stairs)
    initTabBlock(BlockSetSidingBoardbatten.class,
        fmlPreInitializationEvent); // Board and Batten Siding Set (Block, Fence, Slab, Stairs)
    initTabBlock(BlockSetSidingShingle.class,
        fmlPreInitializationEvent); // Cedar Shingle Siding Set (Block, Fence, Slab, Stairs)
    initTabBlock(BlockSetSidingVinyl.class,
        fmlPreInitializationEvent); // Vinyl Siding Set (Block, Fence, Slab, Stairs)
    initTabBlock(BlockSetCladdingCorrugated.class,
        fmlPreInitializationEvent); // Corrugated Steel Cladding Set (Block, Fence, Slab, Stairs)
    initTabBlock(BlockSetCladdingStandingseam.class,
        fmlPreInitializationEvent); // Standing Seam Cladding Set (Block, Fence, Slab, Stairs)
    initTabBlock(BlockSetCladdingInsulated.class,
        fmlPreInitializationEvent); // Insulated Panel Cladding Set (Block, Fence, Slab, Stairs)
    initTabBlock(BlockSetCladdingComposite.class,
        fmlPreInitializationEvent); // Composite Panel Cladding Set (Block, Fence, Slab, Stairs)
    initTabBlock(BlockSetStoneAshlar.class,
        fmlPreInitializationEvent); // Ashlar Stone Veneer Set (Block, Fence, Slab, Stairs)
    initTabBlock(BlockSetStoneFieldstone.class,
        fmlPreInitializationEvent); // Fieldstone Veneer Set (Block, Fence, Slab, Stairs)
    initTabBlock(BlockSetStoneCast.class,
        fmlPreInitializationEvent); // Cast Stone Veneer Set (Block, Fence, Slab, Stairs)
    initTabBlock(new BlockSiteFence("chainlink_fence")); // Chain-Link Fence
    initTabBlock(new BlockSiteFence("chainlink_fence_black")); // Chain-Link Fence (Black)
    initTabBlock(new BlockSiteFence("chainlink_barbed_top")); // Chain-Link Barbed Wire Top

    // Glazing: each kind of glass as a block and a pane.
    initTabBlock(new BlockGlazing("glass_clear")); // Clear Glass
    initTabBlock(new BlockGlazingPane("glass_pane_clear")); // Clear Glass Pane
    initTabBlock(new BlockGlazing("glass_grey")); // Grey Tinted Glass
    initTabBlock(new BlockGlazingPane("glass_pane_grey")); // Grey Tinted Glass Pane
    initTabBlock(new BlockGlazing("glass_bronze")); // Bronze Tinted Glass
    initTabBlock(new BlockGlazingPane("glass_pane_bronze")); // Bronze Tinted Glass Pane
    initTabBlock(new BlockGlazing("glass_blue")); // Blue Tinted Glass
    initTabBlock(new BlockGlazingPane("glass_pane_blue")); // Blue Tinted Glass Pane
    initTabBlock(new BlockGlazing("glass_oneway")); // One-Way Glass
    initTabBlock(new BlockGlazingPane("glass_pane_oneway")); // One-Way Glass Pane
    initTabBlock(new BlockGlazing("glass_wired")); // Wired Glass
    initTabBlock(new BlockGlazingPane("glass_pane_wired")); // Wired Glass Pane
    initTabBlock(new BlockGlazing("glass_bullet")); // Bullet-Resistant Glass
    initTabBlock(new BlockGlazingPane("glass_pane_bullet")); // Bullet-Resistant Glass Pane
    initTabBlock(new BlockGlazing("glass_frosted")); // Frosted Glass
    initTabBlock(new BlockGlazingPane("glass_pane_frosted")); // Frosted Glass Pane

    // Garage doors: built to the size of the opening, animated only while they move.
    initTabBlock(new BlockGarageDoor("garage_door_sectional_white")); // Garage Door (White Raised Panel)
    initTabBlock(new BlockGarageDoor("garage_door_sectional_windowed")); // Garage Door (Windowed)
    initTabBlock(new BlockGarageDoor("garage_door_sectional_commercial")); // Sectional Door (Commercial Steel)
    initTabBlock(new BlockGarageDoor("garage_door_rollup_galvanized")); // Roll-Up Door (Galvanized)
    initTabBlock(new BlockGarageDoor("garage_door_grille")); // Security Grille
    initTabBlock(BlockGarageDoorOpener.class, fmlPreInitializationEvent); // Garage Door Opener
    initTabBlock(BlockGarageDoorHanger.class, fmlPreInitializationEvent); // Garage Door Hanger
    initTabBlock(new BlockGarageDoorControl("garage_door_button")); // Garage Door Button
    initTabBlock(new BlockGarageDoorControl("garage_door_station")); // Garage Door Control Station
    initTabBlock(new BlockGarageDoorControl("garage_door_keypad")); // Garage Door Keypad
  }
}

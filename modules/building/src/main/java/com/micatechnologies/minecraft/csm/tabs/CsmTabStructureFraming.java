package com.micatechnologies.minecraft.csm.tabs;

import com.micatechnologies.minecraft.csm.CsmRegistry;
import com.micatechnologies.minecraft.csm.buildingmaterials.BlockHollowMetalDoorFrame;
import com.micatechnologies.minecraft.csm.buildingmaterials.BlockSteelStudWall;
import com.micatechnologies.minecraft.csm.buildingmaterials.BlockSteelStudWallBraced;
import com.micatechnologies.minecraft.csm.buildingmaterials.BlockSteelStudWallDoor;
import com.micatechnologies.minecraft.csm.buildingmaterials.BlockSteelStudWallNarrow;
import com.micatechnologies.minecraft.csm.buildingmaterials.BlockSteelStudWallWindow;
import com.micatechnologies.minecraft.csm.buildingmaterials.BlockSteelTrack;
import com.micatechnologies.minecraft.csm.buildingmaterials.BlockWoodPlate;
import com.micatechnologies.minecraft.csm.buildingmaterials.BlockWoodStudWall;
import com.micatechnologies.minecraft.csm.buildingmaterials.BlockWoodStudWallBlocking;
import com.micatechnologies.minecraft.csm.buildingmaterials.BlockWoodStudWallBraced;
import com.micatechnologies.minecraft.csm.buildingmaterials.BlockWoodStudWallDoor;
import com.micatechnologies.minecraft.csm.buildingmaterials.BlockWoodStudWallNarrow;
import com.micatechnologies.minecraft.csm.buildingmaterials.BlockWoodStudWallWindow;
import com.micatechnologies.minecraft.csm.codeutils.CsmTab;
import net.minecraft.block.Block;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;

/**
 * The tab for structural and framing blocks: steel and wood stud walls, the horizontal structure
 * that spans between them, and structural steel.
 *
 * <p>The families arrive one at a time: steel studs first, then wood, then the joists, trusses
 * and deck, then structural steel.</p>
 *
 * @version 1.0
 */
@CsmTab.Load(order = 14)
public class CsmTabStructureFraming extends CsmTab {

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
    return "tabstructureframing";
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
    return CsmRegistry.getBlock("steel_stud_wall");
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
    initTabBlock(BlockSteelStudWall.class, fmlPreInitializationEvent); // Steel Stud Wall
    initTabBlock(BlockSteelStudWallNarrow.class, fmlPreInitializationEvent); // Steel Stud Wall (Narrow)
    initTabBlock(BlockSteelStudWallBraced.class, fmlPreInitializationEvent); // Steel Stud Wall (Braced)
    initTabBlock(BlockSteelStudWallDoor.class, fmlPreInitializationEvent); // Steel Stud Wall (Door Opening)
    initTabBlock(BlockSteelStudWallWindow.class, fmlPreInitializationEvent); // Steel Stud Wall (Window Opening)
    initTabBlock(BlockSteelTrack.class, fmlPreInitializationEvent); // Steel Track
    initTabBlock(BlockHollowMetalDoorFrame.class, fmlPreInitializationEvent); // Hollow Metal Door Frame
    initTabBlock(BlockWoodStudWall.class, fmlPreInitializationEvent); // Wood Stud Wall
    initTabBlock(BlockWoodStudWallNarrow.class, fmlPreInitializationEvent); // Wood Stud Wall (Narrow)
    initTabBlock(BlockWoodStudWallBraced.class, fmlPreInitializationEvent); // Wood Stud Wall (Braced)
    initTabBlock(BlockWoodStudWallBlocking.class, fmlPreInitializationEvent); // Wood Stud Wall (Fire Blocking)
    initTabBlock(BlockWoodStudWallDoor.class, fmlPreInitializationEvent); // Wood Stud Wall (Door Opening)
    initTabBlock(BlockWoodStudWallWindow.class, fmlPreInitializationEvent); // Wood Stud Wall (Window Opening)
    initTabBlock(BlockWoodPlate.class, fmlPreInitializationEvent); // Wood Sole Plate
  }
}

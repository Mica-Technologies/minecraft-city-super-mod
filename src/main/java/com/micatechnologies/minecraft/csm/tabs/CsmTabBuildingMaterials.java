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
import com.micatechnologies.minecraft.csm.buildingmaterials.BlockPCC;
import com.micatechnologies.minecraft.csm.buildingmaterials.BlockSetBlackMetal;
import com.micatechnologies.minecraft.csm.buildingmaterials.BlockSetBlueMetal;
import com.micatechnologies.minecraft.csm.buildingmaterials.BlockSetCopperMetal;
import com.micatechnologies.minecraft.csm.buildingmaterials.BlockSetGreenMetal;
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
import com.micatechnologies.minecraft.csm.codeutils.CsmTab;
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

  }
}

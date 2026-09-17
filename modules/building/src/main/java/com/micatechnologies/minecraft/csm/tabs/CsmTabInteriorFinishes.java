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
  }
}

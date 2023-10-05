package com.micatechnologies.minecraft.csm.tabs;

import com.micatechnologies.minecraft.csm.CsmRegistry;
import com.micatechnologies.minecraft.csm.buildingmaterials.*;
import com.micatechnologies.minecraft.csm.codeutils.CsmTab;
import net.minecraft.block.Block;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;

/**
 * The tab for building material blocks.
 *
 * @version 1.0
 */
@CsmTab.Load
public class CsmTabBuildingMaterials extends CsmTab
{
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
        return CsmRegistry.getBlock( "pcc" );
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
     * Initializes all the items belonging to the tab.
     *
     * @since 1.0
     */
    @Override
    public void initTabElements( FMLPreInitializationEvent fmlPreInitializationEvent ) {
        initTabBlock( BlockSetBlackMetal.class,
                      fmlPreInitializationEvent ); // Black Metal Set (Block, Fence, Slab, Stairs)
        initTabBlock( BlockSetSilverMetal.class,
                      fmlPreInitializationEvent ); // Silver Metal Set (Block, Fence, Slab, Stairs)
        initTabBlock( BlockSetWhiteMetal.class,
                      fmlPreInitializationEvent ); // White Metal Set (Block, Fence, Slab, Stairs)
        initTabBlock( BlockPCC.class, fmlPreInitializationEvent ); // PCC
        initTabBlock( BlockCTF.class, fmlPreInitializationEvent ); // CTF
        initTabBlock( BlockCT50s1.class, fmlPreInitializationEvent ); // CT50s1
        initTabBlock( BlockCT50s2.class, fmlPreInitializationEvent ); // CT50s2
        initTabBlock( BlockCT50s3.class, fmlPreInitializationEvent ); // CT50s3
        initTabBlock( BlockCTS1.class, fmlPreInitializationEvent ); // CTS1
        initTabBlock( BlockCTS2.class, fmlPreInitializationEvent ); // CTS2
        initTabBlock( BlockCTS3.class, fmlPreInitializationEvent ); // CTS3
        initTabBlock( BlockDCT1.class, fmlPreInitializationEvent ); // DCT1
        initTabBlock( BlockDCT2.class, fmlPreInitializationEvent ); // DCT2
        initTabBlock( BlockDCT3.class, fmlPreInitializationEvent ); // DCT3

    }
}

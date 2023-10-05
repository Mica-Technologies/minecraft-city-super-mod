package com.micatechnologies.minecraft.csm.tabs;

import com.micatechnologies.minecraft.csm.CsmRegistry;
import com.micatechnologies.minecraft.csm.buildingmaterials.BlockPCC;
import com.micatechnologies.minecraft.csm.codeutils.CsmTab;
import com.micatechnologies.minecraft.csm.hvac.*;
import net.minecraft.block.Block;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;

/**
 * The tab for HVAC blocks.
 *
 * @version 1.0
 */
@CsmTab.Load
public class CsmTabHvac extends CsmTab
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
        return "tabhvac";
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
        return CsmRegistry.getBlock( "sv4" );
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
        initTabBlock( BlockART1.class, fmlPreInitializationEvent ); // ART1
        initTabBlock( BlockART2.class, fmlPreInitializationEvent ); // ART2
        initTabBlock( BlockARTD1.class, fmlPreInitializationEvent ); // ARTD1
        initTabBlock( BlockARTD2.class, fmlPreInitializationEvent ); // ARTD2
        initTabBlock( BlockDFV1.class, fmlPreInitializationEvent ); // DFV1
        initTabBlock( BlockDFV2.class, fmlPreInitializationEvent ); // DFV2
        initTabBlock( BlockDFVD1.class, fmlPreInitializationEvent ); // DFVD1
        initTabBlock( BlockDFVD2.class, fmlPreInitializationEvent ); // DFVD2
        initTabBlock( BlockLCV.class, fmlPreInitializationEvent ); // LCV
        initTabBlock( BlockMV1.class, fmlPreInitializationEvent ); // MV1
        initTabBlock( BlockMV2.class, fmlPreInitializationEvent ); // MV2
        initTabBlock( BlockMV3.class, fmlPreInitializationEvent ); // MV3
        initTabBlock( BlockMVD1.class, fmlPreInitializationEvent ); // MVD1
        initTabBlock( BlockMVD2.class, fmlPreInitializationEvent ); // MVD2
        initTabBlock( BlockPBF.class, fmlPreInitializationEvent ); // PBF
        initTabBlock( BlockPV.class, fmlPreInitializationEvent ); // PV
        initTabBlock( BlockPVD.class, fmlPreInitializationEvent ); // PVD
        initTabBlock( BlockRV1.class, fmlPreInitializationEvent ); // RV1
        initTabBlock( BlockRV2.class, fmlPreInitializationEvent ); // RV2
        initTabBlock( BlockSCV.class, fmlPreInitializationEvent ); // SCV
        initTabBlock( BlockSV1.class, fmlPreInitializationEvent ); // SV1
        initTabBlock( BlockSV2.class, fmlPreInitializationEvent ); // SV2
        initTabBlock( BlockSV3.class, fmlPreInitializationEvent ); // SV3
        initTabBlock( BlockSV4.class, fmlPreInitializationEvent ); // SV4
        initTabBlock( BlockSV5.class, fmlPreInitializationEvent ); // SV5
        initTabBlock( BlockSVD1.class, fmlPreInitializationEvent ); // SVD1
        initTabBlock( BlockSVD2.class, fmlPreInitializationEvent ); // SVD2
        initTabBlock( BlockSVD3.class, fmlPreInitializationEvent ); // SVD3
        initTabBlock( BlockSVD4.class, fmlPreInitializationEvent ); // SVD4
        initTabBlock( BlockSVD5.class, fmlPreInitializationEvent ); // SVD5
    }
}

package com.micatechnologies.minecraft.csm.tabs;

import com.micatechnologies.minecraft.csm.CsmRegistry;
import com.micatechnologies.minecraft.csm.codeutils.CsmTab;
import com.micatechnologies.minecraft.csm.powergrid.fe.BlockForgeEnergyToRedstone;
import com.micatechnologies.minecraft.csm.technology.*;
import com.micatechnologies.minecraft.csm.trafficaccessories.BlockTLBorderYellowBlack;
import net.minecraft.block.Block;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;

/**
 * The tab for technology blocks.
 *
 * @version 1.0
 */
@CsmTab.Load
public class CsmTabTechnology extends CsmTab
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
        return "tabtechnology";
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
        return CsmRegistry.getBlock( BlockImacpro.class );
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
        initTabBlock( BlockATLS1.class, fmlPreInitializationEvent ); // ATLS1
        initTabBlock( BlockATLS2.class, fmlPreInitializationEvent ); // ATLS2
        initTabBlock( BlockATLS3.class, fmlPreInitializationEvent ); // ATLS3
        initTabBlock( BlockATLS4.class, fmlPreInitializationEvent ); // ATLS4
        initTabBlock( BlockATLS5.class, fmlPreInitializationEvent ); // ATLS5
        initTabBlock( BlockAppleTV.class, fmlPreInitializationEvent ); // AppleTV
        initTabBlock( BlockBose1.class, fmlPreInitializationEvent ); // Bose1
        initTabBlock( BlockBose2.class, fmlPreInitializationEvent ); // Bose2
        initTabBlock( BlockFJS1.class, fmlPreInitializationEvent ); // FJS1
        initTabBlock( BlockFJS2.class, fmlPreInitializationEvent ); // FJS2
        initTabBlock( BlockFarevend.class, fmlPreInitializationEvent ); // Farevend
        initTabBlock( BlockImac.class, fmlPreInitializationEvent ); // Imac
        initTabBlock( BlockImacpro.class, fmlPreInitializationEvent ); // Imacpro
        initTabBlock( BlockJBLC1.class, fmlPreInitializationEvent ); // JBLC1
        initTabBlock( BlockJBLC2.class, fmlPreInitializationEvent ); // JBLC2
        initTabBlock( BlockMbp.class, fmlPreInitializationEvent ); // Mbp
        initTabBlock( BlockRedstoneTTS.class, fmlPreInitializationEvent ); // RedstoneTTS
        initTabBlock( BlockRedstoneTTSGui.class, fmlPreInitializationEvent ); // RedstoneTTSGui
        initTabBlock( BlockStbox.class, fmlPreInitializationEvent ); // Stbox
        initTabBlock( BlockTvdish.class, fmlPreInitializationEvent ); // Tvdish
        initTabBlock( BlockTvdishside.class, fmlPreInitializationEvent ); // Tvdishside
        initTabBlock( BlockVCS1.class, fmlPreInitializationEvent ); // VCS1
        initTabBlock( BlockVCS2.class, fmlPreInitializationEvent ); // VCS2
        initTabBlock( BlockVCS3.class, fmlPreInitializationEvent ); // VCS3
        initTabBlock( BlockVCS4.class, fmlPreInitializationEvent ); // VCS4
        initTabBlock( BlockVCS5.class, fmlPreInitializationEvent ); // VCS5
        initTabBlock( BlockVCS6.class, fmlPreInitializationEvent ); // VCS6
        initTabBlock( BlockVCS7.class, fmlPreInitializationEvent ); // VCS7
        initTabBlock( BlockVCS8.class, fmlPreInitializationEvent ); // VCS8
        initTabBlock( BlockVCS9.class, fmlPreInitializationEvent ); // VCS9
        initTabBlock( BlockVf915.class, fmlPreInitializationEvent ); // Vf915
        initTabBlock( BlockWAPTPL225.class, fmlPreInitializationEvent ); // WAPTPL225
        initTabBlock( BlockWapac.class, fmlPreInitializationEvent ); // Wapac
        initTabBlock( BlockWapn.class, fmlPreInitializationEvent ); // Wapn
        initTabBlock( BlockWg.class, fmlPreInitializationEvent ); // Wg
    }
}

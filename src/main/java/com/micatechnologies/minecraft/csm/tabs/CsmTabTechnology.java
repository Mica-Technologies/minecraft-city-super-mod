package com.micatechnologies.minecraft.csm.tabs;

import com.micatechnologies.minecraft.csm.codeutils.CsmTab;
import com.micatechnologies.minecraft.csm.powergrid.fe.BlockForgeEnergyToRedstone;
import com.micatechnologies.minecraft.csm.technology.BlockImacpro;
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
        return BlockImacpro.block;
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

    }
}

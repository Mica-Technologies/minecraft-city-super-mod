package com.micatechnologies.minecraft.csm.tabs;

import com.micatechnologies.minecraft.csm.codeutils.CsmTab;
import com.micatechnologies.minecraft.csm.lifesafety.BlockMCLACodeApprovedExitSignISA;
import net.minecraft.block.Block;
import net.minecraft.creativetab.CreativeTabs;

/**
 * The tab for life safety blocks.
 *
 * @version 1.0
 */
@CsmTab.Load
public class CsmTabLifeSafety extends CsmTab
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
        return "tablifesafety";
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
        return BlockMCLACodeApprovedExitSignISA.block;
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
        return true;
    }

    /**
     * Gets the {@link CreativeTabs} instance for the {@link CsmTab} implementation.
     *
     * @return the {@link CreativeTabs} instance for the {@link CsmTab} implementation class
     *
     * @since 1.0
     */
    public static CreativeTabs get() {
        return get( CsmTabLifeSafety.class );
    }
}

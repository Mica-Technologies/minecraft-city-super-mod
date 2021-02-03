package com.micatechnologies.minecraft.csm.NEEDSWORK;

import com.micatechnologies.minecraft.csm.ElementsCitySuperMod;
import com.micatechnologies.minecraft.csm.NEEDSWORK.block.BlockWbs;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.fml.relauncher.Side;

import net.minecraft.item.ItemStack;
import net.minecraft.creativetab.CreativeTabs;

@ElementsCitySuperMod.ModElement.Tag
public class TabMCLAOtherTab extends ElementsCitySuperMod.ModElement
{
    private final static String       ID_NAME   = "tabmclaothertab";
    public static        CreativeTabs tab;

    public TabMCLAOtherTab( ElementsCitySuperMod instance ) {
        super( instance, 1015 );
    }

    @Override
    public void initElements() {
        tab = new CreativeTabs( ID_NAME )
        {
            @SideOnly( Side.CLIENT )
            @Override
            public ItemStack getTabIconItem() {
                return new ItemStack( BlockWbs.block, 1 );
            }

            @SideOnly( Side.CLIENT )
            public boolean hasSearchBar() {
                return true;
            }
        };
    }
}

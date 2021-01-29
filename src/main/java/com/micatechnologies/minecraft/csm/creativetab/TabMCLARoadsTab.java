package com.micatechnologies.minecraft.csm.creativetab;

import com.micatechnologies.minecraft.csm.ElementsCitySuperMod;
import com.micatechnologies.minecraft.csm.block.BlockTrafficPoleVerticalCurveConnector;
import com.micatechnologies.minecraft.csm.block.BlockWbs;
import net.minecraft.block.Block;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.fml.relauncher.Side;

import net.minecraft.item.ItemStack;
import net.minecraft.creativetab.CreativeTabs;

@ElementsCitySuperMod.ModElement.Tag
public class TabMCLARoadsTab extends ElementsCitySuperMod.ModElement
{
    private final static String       ID_NAME   = "tabmclaroadstab";
    public static        CreativeTabs tab;

    public TabMCLARoadsTab( ElementsCitySuperMod instance ) {
        super( instance, 1012 );
    }

    @Override
    public void initElements() {
        tab = new CreativeTabs( ID_NAME )
        {
            @SideOnly( Side.CLIENT )
            @Override
            public ItemStack getTabIconItem() {
                return new ItemStack( BlockTrafficPoleVerticalCurveConnector.block, 1 );
            }

            @SideOnly( Side.CLIENT )
            public boolean hasSearchBar() {
                return true;
            }
        };
    }
}

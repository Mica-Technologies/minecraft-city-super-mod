package com.micatechnologies.minecraft.csm.creativetab;

import com.micatechnologies.minecraft.csm.ElementsCitySuperMod;
import com.micatechnologies.minecraft.csm.block.BlockImacpro;
import com.micatechnologies.minecraft.csm.block.BlockSignpoststopsign;
import net.minecraft.block.Block;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.fml.relauncher.Side;

import net.minecraft.item.ItemStack;
import net.minecraft.creativetab.CreativeTabs;

@ElementsCitySuperMod.ModElement.Tag
public class TabRoadSignTab extends ElementsCitySuperMod.ModElement
{
    private final static String       ID_NAME   = "tabroadsigntab";
    public static        CreativeTabs tab;

    public TabRoadSignTab( ElementsCitySuperMod instance ) {
        super( instance, 1066 );
    }

    @Override
    public void initElements() {
        tab = new CreativeTabs( ID_NAME )
        {
            @SideOnly( Side.CLIENT )
            @Override
            public ItemStack getTabIconItem() {
                return new ItemStack( BlockSignpoststopsign.block, 1 );
            }

            @SideOnly( Side.CLIENT )
            public boolean hasSearchBar() {
                return true;
            }
        };
    }
}

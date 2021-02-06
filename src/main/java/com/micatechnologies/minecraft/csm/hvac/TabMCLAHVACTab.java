package com.micatechnologies.minecraft.csm.hvac;

import com.micatechnologies.minecraft.csm.ElementsCitySuperMod;
import com.micatechnologies.minecraft.csm.hvac.BlockSV4;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.fml.relauncher.Side;

import net.minecraft.item.ItemStack;
import net.minecraft.creativetab.CreativeTabs;

@ElementsCitySuperMod.ModElement.Tag
public class TabMCLAHVACTab extends ElementsCitySuperMod.ModElement
{
    private final static String       ID_NAME   = "tabmclahvactab";
    public static        CreativeTabs tab;

    public TabMCLAHVACTab( ElementsCitySuperMod instance ) {
        super( instance, 1028 );
    }

    @Override
    public void initElements() {
        tab = new CreativeTabs( ID_NAME )
        {
            @SideOnly( Side.CLIENT )
            @Override
            public ItemStack getTabIconItem() {
                return new ItemStack( BlockSV4.block, 1 );
            }

            @SideOnly( Side.CLIENT )
            public boolean hasSearchBar() {
                return false;
            }
        };
    }
}

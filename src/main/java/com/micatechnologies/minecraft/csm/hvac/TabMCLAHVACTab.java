package com.micatechnologies.minecraft.csm.hvac;

import com.micatechnologies.minecraft.csm.ElementsCitySuperMod;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@ElementsCitySuperMod.ModElement.Tag
public class TabMCLAHVACTab extends ElementsCitySuperMod.ModElement
{
    private final static String       ID_NAME = "tabmclahvactab";
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

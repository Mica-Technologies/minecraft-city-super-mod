package com.micatechnologies.minecraft.csm.trafficaccessories;

import com.micatechnologies.minecraft.csm.ElementsCitySuperMod;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@ElementsCitySuperMod.ModElement.Tag
public class TabTrafficSignalAccessories extends ElementsCitySuperMod.ModElement
{
    private final static String       ID_NAME   = "tabtrafficsignalaccessories";
    public static        CreativeTabs tab;

    public TabTrafficSignalAccessories( ElementsCitySuperMod instance ) {
        super( instance, 2334 );
    }

    @Override
    public void initElements() {
        tab = new CreativeTabs( ID_NAME )
        {
            @SideOnly( Side.CLIENT )
            @Override
            public ItemStack getTabIconItem() {
                return new ItemStack( BlockTLBorderYellowBlack.block, 1 );
            }

            @SideOnly( Side.CLIENT )
            public boolean hasSearchBar() {
                return true;
            }
        };
    }
}

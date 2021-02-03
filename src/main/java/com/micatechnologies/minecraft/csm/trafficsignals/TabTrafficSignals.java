package com.micatechnologies.minecraft.csm.trafficsignals;

import com.micatechnologies.minecraft.csm.ElementsCitySuperMod;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@ElementsCitySuperMod.ModElement.Tag
public class TabTrafficSignals extends ElementsCitySuperMod.ModElement
{
    private final static String       ID_NAME = "tabtrafficsignals";
    public static        CreativeTabs tab;

    public TabTrafficSignals( ElementsCitySuperMod instance ) {
        super( instance, 1563 );
    }

    @Override
    public void initElements() {
        tab = new CreativeTabs( ID_NAME )
        {
            @SideOnly( Side.CLIENT )
            @Override
            public ItemStack getTabIconItem() {
                return new ItemStack( BlockControllableVerticalSolidSignal.block, 1 );
            }

            @SideOnly( Side.CLIENT )
            public boolean hasSearchBar() {
                return true;
            }
        };
    }
}

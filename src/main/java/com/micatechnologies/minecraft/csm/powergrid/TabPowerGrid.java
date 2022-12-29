package com.micatechnologies.minecraft.csm.powergrid;

import com.micatechnologies.minecraft.csm.ElementsCitySuperMod;
import com.micatechnologies.minecraft.csm.powergrid.fe.BlockForgeEnergyToRedstone;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@ElementsCitySuperMod.ModElement.Tag
public class TabPowerGrid extends ElementsCitySuperMod.ModElement
{
    private final static String       ID_NAME = "tabpowergrid";
    public static        CreativeTabs tab;

    public TabPowerGrid( ElementsCitySuperMod instance ) {
        super( instance, 1014 );
    }

    @Override
    public void initElements() {
        tab = new CreativeTabs( ID_NAME )
        {
            @SideOnly( Side.CLIENT )
            @Override
            public ItemStack getTabIconItem() {
                return new ItemStack( BlockForgeEnergyToRedstone.block, 1 );
            }

            @SideOnly( Side.CLIENT )
            public boolean hasSearchBar() {
                return false;
            }
        };
    }
}

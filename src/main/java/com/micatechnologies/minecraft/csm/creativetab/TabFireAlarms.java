package com.micatechnologies.minecraft.csm.creativetab;

import com.micatechnologies.minecraft.csm.ElementsCitySuperMod;
import com.micatechnologies.minecraft.csm.block.BlockFireAlarmWheelockASRed;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@ElementsCitySuperMod.ModElement.Tag
public class TabFireAlarms extends ElementsCitySuperMod.ModElement
{
    public TabFireAlarms( ElementsCitySuperMod instance ) {
        super( instance, 1599 );
    }

    @Override
    public void initElements() {
        tab = new CreativeTabs( "tabfirealarms" )
        {
            @SideOnly( Side.CLIENT )
            @Override
            public ItemStack getTabIconItem() {
                return new ItemStack( BlockFireAlarmWheelockASRed.block, 1 );
            }

            @SideOnly( Side.CLIENT )
            public boolean hasSearchBar() {
                return false;
            }
        };
    }

    public static CreativeTabs tab;
}

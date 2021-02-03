package com.micatechnologies.minecraft.csm.NEEDSWORK;

import com.micatechnologies.minecraft.csm.ElementsCitySuperMod;
import com.micatechnologies.minecraft.csm.NEEDSWORK.block.BlockMCLACodeApprovedExitSignDual;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.fml.relauncher.Side;

import net.minecraft.item.ItemStack;
import net.minecraft.creativetab.CreativeTabs;

@ElementsCitySuperMod.ModElement.Tag
public class TabMCLAElectricTab extends ElementsCitySuperMod.ModElement
{
    private final static String       ID_NAME   = "tabmclaelectrictab";
    public static        CreativeTabs tab;

    public TabMCLAElectricTab( ElementsCitySuperMod instance ) {
        super( instance, 1014 );
    }

    @Override
    public void initElements() {
        tab = new CreativeTabs( ID_NAME )
        {
            @SideOnly( Side.CLIENT )
            @Override
            public ItemStack getTabIconItem() {
                return new ItemStack( BlockMCLACodeApprovedExitSignDual.block, 1 );
            }

            @SideOnly( Side.CLIENT )
            public boolean hasSearchBar() {
                return true;
            }
        };
    }
}

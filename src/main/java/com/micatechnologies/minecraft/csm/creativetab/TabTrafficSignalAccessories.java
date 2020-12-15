
package com.micatechnologies.minecraft.csm.creativetab;

import com.micatechnologies.minecraft.csm.ElementsCitySuperMod;
import com.micatechnologies.minecraft.csm.block.BlockTLBorderYellowBlack;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@ElementsCitySuperMod.ModElement.Tag
public class TabTrafficSignalAccessories extends ElementsCitySuperMod.ModElement {
	public TabTrafficSignalAccessories( ElementsCitySuperMod instance) {
		super(instance, 2334);
	}

	@Override
	public void initElements() {
		tab = new CreativeTabs("tabtrafficsignalaccessories") {
			@SideOnly(Side.CLIENT)
			@Override
			public ItemStack getTabIconItem() {
				return new ItemStack( BlockTLBorderYellowBlack.block, 1 );
			}

			@SideOnly(Side.CLIENT)
			public boolean hasSearchBar() {
				return false;
			}
		};
	}
	public static CreativeTabs tab;
}

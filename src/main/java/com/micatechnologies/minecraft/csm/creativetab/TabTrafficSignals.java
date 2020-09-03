
package com.micatechnologies.minecraft.csm.creativetab;

import com.micatechnologies.minecraft.csm.ElementsCitySuperMod;
import com.micatechnologies.minecraft.csm.block.BlockControllableVerticalSolidSignal;
import com.micatechnologies.minecraft.csm.block.BlockTLVSolidRed;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@ElementsCitySuperMod.ModElement.Tag
public class TabTrafficSignals extends ElementsCitySuperMod.ModElement {
	public TabTrafficSignals( ElementsCitySuperMod instance) {
		super(instance, 1563);
	}

	@Override
	public void initElements() {
		tab = new CreativeTabs("tabtrafficsignals") {
			@SideOnly(Side.CLIENT)
			@Override
			public ItemStack getTabIconItem() {
				return new ItemStack( BlockControllableVerticalSolidSignal.block, 1 );
			}

			@SideOnly(Side.CLIENT)
			public boolean hasSearchBar() {
				return false;
			}
		};
	}
	public static CreativeTabs tab;
}

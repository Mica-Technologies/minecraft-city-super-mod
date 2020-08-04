
package com.micatechnologies.minecraft.csm.creativetab;

import com.micatechnologies.minecraft.csm.ElementsCitySuperMod;
import com.micatechnologies.minecraft.csm.block.BlockImacpro;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.fml.relauncher.Side;

import net.minecraft.item.ItemStack;
import net.minecraft.creativetab.CreativeTabs;

@ElementsCitySuperMod.ModElement.Tag
public class TabMCLATechTab extends ElementsCitySuperMod.ModElement {
	public TabMCLATechTab(ElementsCitySuperMod instance) {
		super(instance, 1016);
	}

	@Override
	public void initElements() {
		tab = new CreativeTabs("tabmclatechtab") {
			@SideOnly(Side.CLIENT)
			@Override
			public ItemStack getTabIconItem() {
				return new ItemStack( BlockImacpro.block, (int) (1));
			}

			@SideOnly(Side.CLIENT)
			public boolean hasSearchBar() {
				return false;
			}
		};
	}
	public static CreativeTabs tab;
}

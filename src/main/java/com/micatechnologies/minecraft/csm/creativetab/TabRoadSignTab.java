
package com.micatechnologies.minecraft.csm.creativetab;

import com.micatechnologies.minecraft.csm.ElementsCitySuperMod;
import com.micatechnologies.minecraft.csm.block.BlockSignpoststopsign;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.fml.relauncher.Side;

import net.minecraft.item.ItemStack;
import net.minecraft.creativetab.CreativeTabs;

@ElementsCitySuperMod.ModElement.Tag
public class TabRoadSignTab extends ElementsCitySuperMod.ModElement {
	public TabRoadSignTab(ElementsCitySuperMod instance) {
		super(instance, 1066);
	}

	@Override
	public void initElements() {
		tab = new CreativeTabs("tabroadsigntab") {
			@SideOnly(Side.CLIENT)
			@Override
			public ItemStack getTabIconItem() {
				return new ItemStack( BlockSignpoststopsign.block, (int) (1));
			}

			@SideOnly(Side.CLIENT)
			public boolean hasSearchBar() {
				return false;
			}
		};
	}
	public static CreativeTabs tab;
}

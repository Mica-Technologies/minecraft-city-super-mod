
package com.micatechnologies.minecraft.csm.creativetab;

import com.micatechnologies.minecraft.csm.ElementsCitySuperMod;
import com.micatechnologies.minecraft.csm.block.BlockWasr;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.fml.relauncher.Side;

import net.minecraft.item.ItemStack;
import net.minecraft.creativetab.CreativeTabs;

@ElementsCitySuperMod.ModElement.Tag
public class TabFireAlarmHorn extends ElementsCitySuperMod.ModElement {
	public TabFireAlarmHorn(ElementsCitySuperMod instance) {
		super(instance, 1050);
	}

	@Override
	public void initElements() {
		tab = new CreativeTabs("tabfirealarmhorn") {
			@SideOnly(Side.CLIENT)
			@Override
			public ItemStack getTabIconItem() {
				return new ItemStack( BlockWasr.block, (int) (1));
			}

			@SideOnly(Side.CLIENT)
			public boolean hasSearchBar() {
				return false;
			}
		};
	}
	public static CreativeTabs tab;
}

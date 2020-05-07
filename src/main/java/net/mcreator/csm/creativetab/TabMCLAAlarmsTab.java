
package net.mcreator.csm.creativetab;

import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.fml.relauncher.Side;

import net.minecraft.item.ItemStack;
import net.minecraft.creativetab.CreativeTabs;

import net.mcreator.csm.ElementsCitySuperMod;

@ElementsCitySuperMod.ModElement.Tag
public class TabMCLAAlarmsTab extends ElementsCitySuperMod.ModElement {
	public TabMCLAAlarmsTab(ElementsCitySuperMod instance) {
		super(instance, 1013);
	}

	@Override
	public void initElements() {
		tab = new CreativeTabs("tabmclaalarmstab") {
			@SideOnly(Side.CLIENT)
			@Override
			public ItemStack getTabIconItem() {
				return new ItemStack(BlockPs3.block, (int) (1));
			}

			@SideOnly(Side.CLIENT)
			public boolean hasSearchBar() {
				return false;
			}
		};
	}
	public static CreativeTabs tab;
}

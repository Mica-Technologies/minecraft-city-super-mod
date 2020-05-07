
package net.mcreator.csm.creativetab;

import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.fml.relauncher.Side;

import net.minecraft.item.ItemStack;
import net.minecraft.creativetab.CreativeTabs;

import net.mcreator.csm.ElementsCitySuperMod;

@ElementsCitySuperMod.ModElement.Tag
public class TabMCLARoadsTab extends ElementsCitySuperMod.ModElement {
	public TabMCLARoadsTab(ElementsCitySuperMod instance) {
		super(instance, 1012);
	}

	@Override
	public void initElements() {
		tab = new CreativeTabs("tabmclaroadstab") {
			@SideOnly(Side.CLIENT)
			@Override
			public ItemStack getTabIconItem() {
				return new ItemStack(BlockCWOnRight.block, (int) (1));
			}

			@SideOnly(Side.CLIENT)
			public boolean hasSearchBar() {
				return false;
			}
		};
	}
	public static CreativeTabs tab;
}

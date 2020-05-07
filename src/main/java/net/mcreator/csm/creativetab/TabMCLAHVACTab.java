
package net.mcreator.csm.creativetab;

import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.fml.relauncher.Side;

import net.minecraft.item.ItemStack;
import net.minecraft.creativetab.CreativeTabs;

import net.mcreator.csm.block.BlockSV4;
import net.mcreator.csm.ElementsCitySuperMod;

@ElementsCitySuperMod.ModElement.Tag
public class TabMCLAHVACTab extends ElementsCitySuperMod.ModElement {
	public TabMCLAHVACTab(ElementsCitySuperMod instance) {
		super(instance, 1028);
	}

	@Override
	public void initElements() {
		tab = new CreativeTabs("tabmclahvactab") {
			@SideOnly(Side.CLIENT)
			@Override
			public ItemStack getTabIconItem() {
				return new ItemStack(BlockSV4.block, (int) (1));
			}

			@SideOnly(Side.CLIENT)
			public boolean hasSearchBar() {
				return false;
			}
		};
	}
	public static CreativeTabs tab;
}

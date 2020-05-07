
package net.mcreator.csm.creativetab;

import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.fml.relauncher.Side;

import net.minecraft.item.ItemStack;
import net.minecraft.creativetab.CreativeTabs;

import net.mcreator.csm.block.BlockTLDSolidRed;
import net.mcreator.csm.ElementsCitySuperMod;

@ElementsCitySuperMod.ModElement.Tag
public class TabTrafficSignalsDoghouse extends ElementsCitySuperMod.ModElement {
	public TabTrafficSignalsDoghouse(ElementsCitySuperMod instance) {
		super(instance, 1055);
	}

	@Override
	public void initElements() {
		tab = new CreativeTabs("tabtrafficsignalsdoghouse") {
			@SideOnly(Side.CLIENT)
			@Override
			public ItemStack getTabIconItem() {
				return new ItemStack(BlockTLDSolidRed.block, (int) (1));
			}

			@SideOnly(Side.CLIENT)
			public boolean hasSearchBar() {
				return false;
			}
		};
	}
	public static CreativeTabs tab;
}

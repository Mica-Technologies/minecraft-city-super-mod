
package net.mcreator.csm.creativetab;

import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.fml.relauncher.Side;

import net.minecraft.item.ItemStack;
import net.minecraft.creativetab.CreativeTabs;

import net.mcreator.csm.block.BlockAirHockeyTable;
import net.mcreator.csm.ElementsCitySuperMod;

@ElementsCitySuperMod.ModElement.Tag
public class TabCSMGaming extends ElementsCitySuperMod.ModElement {
	public TabCSMGaming(ElementsCitySuperMod instance) {
		super(instance, 1117);
	}

	@Override
	public void initElements() {
		tab = new CreativeTabs("tabcsmgaming") {
			@SideOnly(Side.CLIENT)
			@Override
			public ItemStack getTabIconItem() {
				return new ItemStack(BlockAirHockeyTable.block, (int) (1));
			}

			@SideOnly(Side.CLIENT)
			public boolean hasSearchBar() {
				return false;
			}
		};
	}
	public static CreativeTabs tab;
}


package net.mcreator.csm.creativetab;

import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.fml.relauncher.Side;

import net.minecraft.item.ItemStack;
import net.minecraft.creativetab.CreativeTabs;

import net.mcreator.csm.block.BlockMCLACodeApprovedExitSignDual;
import net.mcreator.csm.ElementsCitySuperMod;

@ElementsCitySuperMod.ModElement.Tag
public class TabMCLAElectricTab extends ElementsCitySuperMod.ModElement {
	public TabMCLAElectricTab(ElementsCitySuperMod instance) {
		super(instance, 1014);
	}

	@Override
	public void initElements() {
		tab = new CreativeTabs("tabmclaelectrictab") {
			@SideOnly(Side.CLIENT)
			@Override
			public ItemStack getTabIconItem() {
				return new ItemStack(BlockMCLACodeApprovedExitSignDual.block, (int) (1));
			}

			@SideOnly(Side.CLIENT)
			public boolean hasSearchBar() {
				return false;
			}
		};
	}
	public static CreativeTabs tab;
}

package net.mcreator.csm;

import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.fml.relauncher.Side;

import net.minecraft.item.ItemStack;
import net.minecraft.creativetab.CreativeTabs;

@Elementscsm.ModElement.Tag
public class MCreatorMCLAElectricTab extends Elementscsm.ModElement {
	public MCreatorMCLAElectricTab(Elementscsm instance) {
		super(instance, 1014);
	}

	@Override
	public void initElements() {
		tab = new CreativeTabs("tabmclaelectrictab") {
			@SideOnly(Side.CLIENT)
			@Override
			public ItemStack getTabIconItem() {
				return new ItemStack(MCreatorMCLACodeApprovedExitSignDual.block, (int) (1));
			}

			@SideOnly(Side.CLIENT)
			public boolean hasSearchBar() {
				return false;
			}
		};
	}
	public static CreativeTabs tab;
}

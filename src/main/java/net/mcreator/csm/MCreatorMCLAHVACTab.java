package net.mcreator.csm;

import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.fml.relauncher.Side;

import net.minecraft.item.ItemStack;
import net.minecraft.creativetab.CreativeTabs;

@Elementscsm.ModElement.Tag
public class MCreatorMCLAHVACTab extends Elementscsm.ModElement {
	public MCreatorMCLAHVACTab(Elementscsm instance) {
		super(instance, 1028);
	}

	@Override
	public void initElements() {
		tab = new CreativeTabs("tabmclahvactab") {
			@SideOnly(Side.CLIENT)
			@Override
			public ItemStack getTabIconItem() {
				return new ItemStack(MCreatorSV4.block, (int) (1));
			}

			@SideOnly(Side.CLIENT)
			public boolean hasSearchBar() {
				return false;
			}
		};
	}
	public static CreativeTabs tab;
}

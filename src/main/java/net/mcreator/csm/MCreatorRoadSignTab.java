package net.mcreator.csm;

import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.fml.relauncher.Side;

import net.minecraft.item.ItemStack;
import net.minecraft.creativetab.CreativeTabs;

@Elementscsm.ModElement.Tag
public class MCreatorRoadSignTab extends Elementscsm.ModElement {
	public MCreatorRoadSignTab(Elementscsm instance) {
		super(instance, 1066);
	}

	@Override
	public void initElements() {
		tab = new CreativeTabs("tabroadsigntab") {
			@SideOnly(Side.CLIENT)
			@Override
			public ItemStack getTabIconItem() {
				return new ItemStack(MCreatorSignpoststopsign.block, (int) (1));
			}

			@SideOnly(Side.CLIENT)
			public boolean hasSearchBar() {
				return false;
			}
		};
	}
	public static CreativeTabs tab;
}

package net.mcreator.csm;

import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.fml.relauncher.Side;

import net.minecraft.item.ItemStack;
import net.minecraft.creativetab.CreativeTabs;

@Elementscsm.ModElement.Tag
public class MCreatorTrafficSignalsVertical extends Elementscsm.ModElement {
	public MCreatorTrafficSignalsVertical(Elementscsm instance) {
		super(instance, 1052);
	}

	@Override
	public void initElements() {
		tab = new CreativeTabs("tabtrafficsignalsvertical") {
			@SideOnly(Side.CLIENT)
			@Override
			public ItemStack getTabIconItem() {
				return new ItemStack(MCreatorTLVSolidRed.block, (int) (1));
			}

			@SideOnly(Side.CLIENT)
			public boolean hasSearchBar() {
				return false;
			}
		};
	}
	public static CreativeTabs tab;
}

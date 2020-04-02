package net.mcreator.csm;

import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.fml.relauncher.Side;

import net.minecraft.item.ItemStack;
import net.minecraft.creativetab.CreativeTabs;

@Elementscsm.ModElement.Tag
public class MCreatorTrafficSignalsHorizontal extends Elementscsm.ModElement {
	public MCreatorTrafficSignalsHorizontal(Elementscsm instance) {
		super(instance, 1053);
	}

	@Override
	public void initElements() {
		tab = new CreativeTabs("tabtrafficsignalshorizontal") {
			@SideOnly(Side.CLIENT)
			@Override
			public ItemStack getTabIconItem() {
				return new ItemStack(MCreatorTLHSolidRed.block, (int) (1));
			}

			@SideOnly(Side.CLIENT)
			public boolean hasSearchBar() {
				return false;
			}
		};
	}
	public static CreativeTabs tab;
}

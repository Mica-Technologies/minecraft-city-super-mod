package net.mcreator.csm;

import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.fml.relauncher.Side;

import net.minecraft.item.ItemStack;
import net.minecraft.creativetab.CreativeTabs;

@Elementscsm.ModElement.Tag
public class MCreatorFireAlarmSpeaker extends Elementscsm.ModElement {
	public MCreatorFireAlarmSpeaker(Elementscsm instance) {
		super(instance, 1049);
	}

	@Override
	public void initElements() {
		tab = new CreativeTabs("tabfirealarmspeaker") {
			@SideOnly(Side.CLIENT)
			@Override
			public ItemStack getTabIconItem() {
				return new ItemStack(MCreatorWheelockWhiteET70SS.block, (int) (1));
			}

			@SideOnly(Side.CLIENT)
			public boolean hasSearchBar() {
				return false;
			}
		};
	}
	public static CreativeTabs tab;
}

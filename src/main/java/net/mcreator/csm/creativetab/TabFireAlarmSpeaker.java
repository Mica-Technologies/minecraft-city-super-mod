
package net.mcreator.csm.creativetab;

import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.fml.relauncher.Side;

import net.minecraft.item.ItemStack;
import net.minecraft.creativetab.CreativeTabs;

import net.mcreator.csm.block.BlockWheelockWhiteET70SS;
import net.mcreator.csm.ElementsCitySuperMod;

@ElementsCitySuperMod.ModElement.Tag
public class TabFireAlarmSpeaker extends ElementsCitySuperMod.ModElement {
	public TabFireAlarmSpeaker(ElementsCitySuperMod instance) {
		super(instance, 1049);
	}

	@Override
	public void initElements() {
		tab = new CreativeTabs("tabfirealarmspeaker") {
			@SideOnly(Side.CLIENT)
			@Override
			public ItemStack getTabIconItem() {
				return new ItemStack(BlockWheelockWhiteET70SS.block, (int) (1));
			}

			@SideOnly(Side.CLIENT)
			public boolean hasSearchBar() {
				return false;
			}
		};
	}
	public static CreativeTabs tab;
}

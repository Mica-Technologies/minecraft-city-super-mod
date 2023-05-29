
package com.micatechnologies.minecraft.csm.trafficsigns;

import com.micatechnologies.minecraft.csm.ElementsCitySuperMod;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.client.event.ModelRegistryEvent;

import net.minecraft.item.ItemBlock;
import net.minecraft.item.Item;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.block.Block;

@ElementsCitySuperMod.ModElement.Tag
public class BlockStreetsweepwed extends ElementsCitySuperMod.ModElement {
	@GameRegistry.ObjectHolder("csm:streetsweepwed")
	public static final Block block = null;
	public BlockStreetsweepwed(ElementsCitySuperMod instance) {
		super(instance, 1081);
	}

	@Override
	public void initElements() {
		elements.blocks.add(() -> new BlockCustom().setRegistryName("streetsweepwed"));
		elements.items.add(() -> new ItemBlock(block).setRegistryName(block.getRegistryName()));
	}

	@SideOnly(Side.CLIENT)
	@Override
	public void registerModels(ModelRegistryEvent event) {
		ModelLoader.setCustomModelResourceLocation(Item.getItemFromBlock(block), 0, new ModelResourceLocation("csm:streetsweepwed", "inventory"));
	}
	public static class BlockCustom extends AbstractBlockSign
    {
        @Override
        public String getBlockRegistryName() {
            return "streetsweepwed";
        }
	}
}

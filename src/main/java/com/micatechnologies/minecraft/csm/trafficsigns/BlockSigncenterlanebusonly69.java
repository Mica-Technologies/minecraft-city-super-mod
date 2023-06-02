
package com.micatechnologies.minecraft.csm.trafficsigns;

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
public class BlockSigncenterlanebusonly69 extends ElementsCitySuperMod.ModElement {
	@GameRegistry.ObjectHolder("csm:signcenterlanebusonly69")
	public static final Block block = null;
	public BlockSigncenterlanebusonly69(ElementsCitySuperMod instance) {
		super(instance, 570);
	}

	@Override
	public void initElements() {
		elements.blocks.add(() -> new BlockCustom().setRegistryName("signcenterlanebusonly69"));
		elements.items.add(() -> new ItemBlock(block).setRegistryName(block.getRegistryName()));
	}

	@SideOnly(Side.CLIENT)
	@Override
	public void registerModels(ModelRegistryEvent event) {
		ModelLoader.setCustomModelResourceLocation(Item.getItemFromBlock(block), 0,
				new ModelResourceLocation("csm:signcenterlanebusonly69", "inventory"));
	}
	public static class BlockCustom extends AbstractBlockSign
    {
        @Override
        public String getBlockRegistryName() {
            return "signcenterlanebusonly69";
        }
	}
}

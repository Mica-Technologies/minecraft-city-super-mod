package net.mcreator.csm;

import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.client.event.ModelRegistryEvent;

import net.minecraft.item.ItemBlock;
import net.minecraft.item.Item;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.block.material.Material;
import net.minecraft.block.SoundType;
import net.minecraft.block.Block;

@Elementscsm.ModElement.Tag
public class MCreatorCTFD extends Elementscsm.ModElement {
	@GameRegistry.ObjectHolder("csm:ctfd")
	public static final Block block = null;

	public MCreatorCTFD(Elementscsm instance) {
		super(instance, 740);
	}

	@Override
	public void initElements() {
		elements.blocks.add(() -> new BlockCustom());
		elements.items.add(() -> new ItemBlock(block).setRegistryName(block.getRegistryName()));
	}

	@SideOnly(Side.CLIENT)
	@Override
	public void registerModels(ModelRegistryEvent event) {
		ModelLoader.setCustomModelResourceLocation(Item.getItemFromBlock(block), 0, new ModelResourceLocation("csm:ctfd", "inventory"));
	}

	public static class BlockCustom extends Block {
		public BlockCustom() {
			super(Material.ROCK);
			setRegistryName("ctfd");
			setUnlocalizedName("ctfd");
			setSoundType(SoundType.STONE);
			setHarvestLevel("pickaxe", 1);
			setHardness(2F);
			setResistance(10F);
			setLightLevel(0F);
			setLightOpacity(255);
			setCreativeTab(MCreatorMCLABuildingMaterialsTab.tab);
		}
	}
}

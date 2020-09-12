
package com.micatechnologies.minecraft.csm.block;

import com.micatechnologies.minecraft.csm.ElementsCitySuperMod;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.client.event.ModelRegistryEvent;

import net.minecraft.item.ItemBlock;
import net.minecraft.item.Item;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.block.state.IBlockState;
import net.minecraft.block.material.Material;
import net.minecraft.block.SoundType;
import net.minecraft.block.BlockFence;
import net.minecraft.block.Block;

import com.micatechnologies.minecraft.csm.creativetab.TabMCLABuildingMaterialsTab;

@ElementsCitySuperMod.ModElement.Tag
public class BlockBlackMetalFence extends ElementsCitySuperMod.ModElement {
	@GameRegistry.ObjectHolder("csm:blackmetalfence")
	public static final Block block = null;
	public BlockBlackMetalFence(ElementsCitySuperMod instance) {
		super(instance, 748);
	}

	@Override
	public void initElements() {
		elements.blocks.add(() -> new BlockCustom().setRegistryName("blackmetalfence"));
		elements.items.add(() -> new ItemBlock(block).setRegistryName(block.getRegistryName()));
	}

	@SideOnly(Side.CLIENT)
	@Override
	public void registerModels(ModelRegistryEvent event) {
		ModelLoader.setCustomModelResourceLocation(Item.getItemFromBlock(block), 0, new ModelResourceLocation("csm:blackmetalfence", "inventory"));
	}
	public static class BlockCustom extends BlockFence {
		public BlockCustom() {
			super(Material.ROCK, Material.ROCK.getMaterialMapColor());
			setUnlocalizedName("blackmetalfence");
			setSoundType(SoundType.GROUND);
			setHarvestLevel("pickaxe", 1);
			setHardness(2F);
			setResistance(10F);
			setLightLevel(0F);
			setLightOpacity(0);
			setCreativeTab(TabMCLABuildingMaterialsTab.tab);
		}

		@Override
		public boolean isOpaqueCube(IBlockState state) {
			return false;
		}
	}
}
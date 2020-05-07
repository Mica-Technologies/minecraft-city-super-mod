package net.mcreator.csm.block;

import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.client.event.ModelRegistryEvent;

import net.minecraft.world.World;
import net.minecraft.world.IBlockAccess;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.Item;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.block.state.IBlockState;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.properties.PropertyDirection;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.material.Material;
import net.minecraft.block.SoundType;
import net.minecraft.block.BlockDirectional;
import net.minecraft.block.Block;

import net.mcreator.csm.creativetab.TabTrafficSignalsVertical;
import net.mcreator.csm.ElementsCitySuperMod;

@ElementsCitySuperMod.ModElement.Tag
public class BlockTLVRightYellowFlash extends ElementsCitySuperMod.ModElement {
	@GameRegistry.ObjectHolder("csm:tlvrightyellowflash")
	public static final Block block = null;
	public BlockTLVRightYellowFlash(ElementsCitySuperMod instance) {
		super(instance, 436);
	}

	@Override
	public void initElements() {
		elements.blocks.add(() -> new BlockCustom());
		elements.items.add(() -> new ItemBlock(block).setRegistryName(block.getRegistryName()));
	}

	@SideOnly(Side.CLIENT)
	@Override
	public void registerModels(ModelRegistryEvent event) {
		ModelLoader.setCustomModelResourceLocation(Item.getItemFromBlock(block), 0,
				new ModelResourceLocation("csm:tlvrightyellowflash", "inventory"));
	}
	public static class BlockCustom extends Block {
		public static final PropertyDirection FACING = BlockDirectional.FACING;
		public BlockCustom() {
			super(Material.ROCK);
			setRegistryName("tlvrightyellowflash");
			setUnlocalizedName("tlvrightyellowflash");
			setSoundType(SoundType.GROUND);
			setHarvestLevel("pickaxe", 1);
			setHardness(2F);
			setResistance(10F);
			setLightLevel(0F);
			setLightOpacity(0);
			setCreativeTab(TabTrafficSignalsVertical.tab);
			this.setDefaultState(this.blockState.getBaseState().withProperty(FACING, EnumFacing.NORTH));
		}

		@SideOnly(Side.CLIENT)
		@Override
		public BlockRenderLayer getBlockLayer() {
			return BlockRenderLayer.CUTOUT_MIPPED;
		}
		public static final PropertyBool POWERED = PropertyBool.create("powered");
		@Override
		public void neighborChanged(IBlockState state, World world, BlockPos pos, Block blockIn, BlockPos p_189540_5_) {
			int powered = world.isBlockIndirectlyGettingPowered(pos);
			world.setBlockState(pos, state.withProperty(POWERED, powered > 0), 3);
		}

		@Override
		public void onBlockPlacedBy(World world, BlockPos pos, IBlockState state, EntityLivingBase placer, ItemStack stack) {
			world.setBlockState(pos, state.withProperty(FACING, getFacingFromEntity(pos, placer)), 2);
		}

		public static EnumFacing getFacingFromEntity(BlockPos clickedBlock, EntityLivingBase entity) {
			return EnumFacing.getFacingFromVector((float) (entity.posX - clickedBlock.getX()), (float) (entity.posY - clickedBlock.getY()),
					(float) (entity.posZ - clickedBlock.getZ()));
		}

		@Override
		public IBlockState getStateFromMeta(int meta) {
			return getDefaultState().withProperty(FACING, EnumFacing.getFront(meta & 7)).withProperty(POWERED, (meta & 8) != 0);
		}

		@Override
		public int getMetaFromState(IBlockState state) {
			return state.getValue(FACING).getIndex() + (state.getValue(POWERED) ? 8 : 0);
		}

		@Override
		protected BlockStateContainer createBlockState() {
			return new BlockStateContainer(this, FACING, POWERED);
		}

		@Override
		public int getLightValue(IBlockState state, IBlockAccess world, BlockPos pos) {
			return state.getValue(POWERED) == true ? 15 : 0;
		}

		@Override
		public boolean isOpaqueCube(IBlockState state) {
			return false;
		}
	}
}

package com.micatechnologies.minecraft.csm.lighting;

import com.micatechnologies.minecraft.csm.tabs.CsmTabLighting;
import net.minecraft.block.Block;
import net.minecraft.block.BlockHorizontal;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyDirection;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@ElementsCitySuperMod.ModElement.Tag
public class BlockNOVTM3 extends ElementsCitySuperMod.ModElement
{
    @GameRegistry.ObjectHolder( "csm:novtm3" )
    public static final Block block = null;

    public BlockNOVTM3( ElementsCitySuperMod instance ) {
        super( instance, 618 );
    }

    @Override
    public void initElements() {
        elements.blocks.add( () -> new BlockCustom().setRegistryName( "novtm3" ) );
        elements.items.add( () -> new ItemBlock( block ).setRegistryName( block.getRegistryName() ) );
    }

    @SideOnly( Side.CLIENT )
    @Override
    public void registerModels( ModelRegistryEvent event ) {
        ModelLoader.setCustomModelResourceLocation( Item.getItemFromBlock( block ), 0,
                                                    new ModelResourceLocation( "csm:novtm3", "inventory" ) );
    }

    public static class BlockCustom extends Block
    {
        public static final PropertyDirection FACING = BlockHorizontal.FACING;

        public BlockCustom() {
            super( Material.ROCK );
            setUnlocalizedName( "novtm3" );
            setSoundType( SoundType.STONE );
            setHarvestLevel( "pickaxe", 1 );
            setHardness( 2F );
            setResistance( 10F );
            setLightLevel( 0F );
            setLightOpacity( 0 );
            setCreativeTab( CsmTabLighting.get() );
            this.setDefaultState( this.blockState.getBaseState().withProperty( FACING, EnumFacing.NORTH ) );
        }

        @Override
        public IBlockState getStateFromMeta( int meta ) {
            return getDefaultState().withProperty( FACING, EnumFacing.getHorizontal( meta ) );
        }

        @Override
        public int getMetaFromState( IBlockState state ) {
            return state.getValue( FACING ).getHorizontalIndex();
        }

        @Override
        public boolean isFullCube( IBlockState state ) {
            return false;
        }

        @Override
        public boolean isOpaqueCube( IBlockState state ) {
            return false;
        }

        @SideOnly( Side.CLIENT )
        @Override
        public BlockRenderLayer getBlockLayer() {
            return BlockRenderLayer.CUTOUT_MIPPED;
        }

        @Override
        public void onBlockPlacedBy( World world,
                                     BlockPos pos,
                                     IBlockState state,
                                     EntityLivingBase placer,
                                     ItemStack stack )
        {
            world.setBlockState( pos, state.withProperty( FACING, placer.getHorizontalFacing().getOpposite() ), 2 );
        }

        @Override
        protected net.minecraft.block.state.BlockStateContainer createBlockState() {
            return new net.minecraft.block.state.BlockStateContainer( this, FACING );
        }
    }
}

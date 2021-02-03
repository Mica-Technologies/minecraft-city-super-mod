package com.micatechnologies.minecraft.csm.NEEDSWORK.block;

import com.micatechnologies.minecraft.csm.ElementsCitySuperMod;
import com.micatechnologies.minecraft.csm.technology.TabMCLATechTab;
import net.minecraft.block.Block;
import net.minecraft.block.BlockDirectional;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.properties.PropertyDirection;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.Mirror;
import net.minecraft.util.Rotation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@ElementsCitySuperMod.ModElement.Tag
public class BlockFJS1 extends ElementsCitySuperMod.ModElement
{
    @GameRegistry.ObjectHolder( "csm:fjs1" )
    public static final Block block = null;

    public BlockFJS1( ElementsCitySuperMod instance ) {
        super( instance, 790 );
    }

    @Override
    public void initElements() {
        elements.blocks.add( () -> new BlockCustom().setRegistryName( "fjs1" ) );
        elements.items.add( () -> new ItemBlock( block ).setRegistryName( block.getRegistryName() ) );
    }

    @SideOnly( Side.CLIENT )
    @Override
    public void registerModels( ModelRegistryEvent event ) {
        ModelLoader.setCustomModelResourceLocation( Item.getItemFromBlock( block ), 0,
                                                    new ModelResourceLocation( "csm:fjs1", "inventory" ) );
    }

    public static class BlockCustom extends Block
    {
        public static final PropertyDirection FACING = BlockDirectional.FACING;

        public BlockCustom() {
            super( Material.ROCK );
            setUnlocalizedName( "fjs1" );
            setSoundType( SoundType.STONE );
            setHarvestLevel( "pickaxe", 1 );
            setHardness( 2F );
            setResistance( 10F );
            setLightLevel( 0F );
            setLightOpacity( 0 );
            setCreativeTab( TabMCLATechTab.tab );
            this.setDefaultState( this.blockState.getBaseState().withProperty( FACING, EnumFacing.NORTH ) );
        }

        @Override
        public IBlockState getStateFromMeta( int meta ) {
            return this.getDefaultState().withProperty( FACING, EnumFacing.getFront( meta ) );
        }

        @Override
        public int getMetaFromState( IBlockState state ) {
            return ( ( EnumFacing ) state.getValue( FACING ) ).getIndex();
        }

        @Override
        public IBlockState withRotation( IBlockState state, Rotation rot ) {
            return state.withProperty( FACING, rot.rotate( ( EnumFacing ) state.getValue( FACING ) ) );
        }

        @Override
        public IBlockState withMirror( IBlockState state, Mirror mirrorIn ) {
            return state.withRotation( mirrorIn.toRotation( ( EnumFacing ) state.getValue( FACING ) ) );
        }

        @Override
        public boolean isFullCube( IBlockState state ) {
            return false;
        }

        @Override
        public boolean isPassable( IBlockAccess worldIn, BlockPos pos ) {
            return true;
        }

        @Override
        public AxisAlignedBB getBoundingBox( IBlockState state, IBlockAccess source, BlockPos pos ) {
            switch ( ( EnumFacing ) state.getValue( BlockDirectional.FACING ) ) {
                case SOUTH:
                default:
                    return new AxisAlignedBB( 1D, 0.9D, 1D, 0D, 1D, 0D );
                case NORTH:
                    return new AxisAlignedBB( 0D, 0.9D, 0D, 1D, 1D, 1D );
                case WEST:
                    return new AxisAlignedBB( 0D, 0.9D, 1D, 1D, 1D, 0D );
                case EAST:
                    return new AxisAlignedBB( 1D, 0.9D, 0D, 0D, 1D, 1D );
                case UP:
                    return new AxisAlignedBB( 0D, 1D, 0.9D, 1D, 0D, 1D );
                case DOWN:
                    return new AxisAlignedBB( 0D, 0D, 0.1D, 1D, 1D, 0D );
            }
        }

        @Override
        @javax.annotation.Nullable
        public AxisAlignedBB getCollisionBoundingBox( IBlockState blockState, IBlockAccess worldIn, BlockPos pos ) {
            return NULL_AABB;
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
        public IBlockState getStateForPlacement( World worldIn,
                                                 BlockPos pos,
                                                 EnumFacing facing,
                                                 float hitX,
                                                 float hitY,
                                                 float hitZ,
                                                 int meta,
                                                 EntityLivingBase placer )
        {
            return this.getDefaultState()
                       .withProperty( FACING, EnumFacing.getDirectionFromEntityLiving( pos, placer ) );
        }

        @Override
        protected net.minecraft.block.state.BlockStateContainer createBlockState() {
            return new net.minecraft.block.state.BlockStateContainer( this, new IProperty[]{ FACING } );
        }
    }
}

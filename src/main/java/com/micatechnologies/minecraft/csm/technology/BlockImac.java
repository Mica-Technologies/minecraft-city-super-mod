package com.micatechnologies.minecraft.csm.technology;

import com.micatechnologies.minecraft.csm.tabs.CsmTabTechnology;
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
public class BlockImac extends ElementsCitySuperMod.ModElement
{
    @GameRegistry.ObjectHolder( "csm:imac" )
    public static final Block block = null;

    public BlockImac( ElementsCitySuperMod instance ) {
        super( instance, 69 );
    }

    @Override
    public void initElements() {
        elements.blocks.add( () -> new BlockCustom().setRegistryName( "imac" ) );
        elements.items.add( () -> new ItemBlock( block ).setRegistryName( block.getRegistryName() ) );
    }

    @SideOnly( Side.CLIENT )
    @Override
    public void registerModels( ModelRegistryEvent event ) {
        ModelLoader.setCustomModelResourceLocation( Item.getItemFromBlock( block ), 0,
                                                    new ModelResourceLocation( "csm:imac", "inventory" ) );
    }

    public static class BlockCustom extends Block
    {
        public static final PropertyDirection FACING = BlockDirectional.FACING;

        public BlockCustom() {
            super( Material.ROCK );
            setUnlocalizedName( "imac" );
            setSoundType( SoundType.STONE );
            setHarvestLevel( "pickaxe", 1 );
            setHardness( 2F );
            setResistance( 10F );
            setLightLevel( 0.6F );
            setLightOpacity( 0 );
            setCreativeTab( CsmTabTechnology.get() );
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
        public AxisAlignedBB getBoundingBox( IBlockState state, IBlockAccess source, BlockPos pos ) {
            switch ( ( EnumFacing ) state.getValue( BlockDirectional.FACING ) ) {
                case SOUTH:
                default:
                    return new AxisAlignedBB( 1D, 0D, 1D, 0D, 0.75D, 0.25D );
                case NORTH:
                    return new AxisAlignedBB( 0D, 0D, 0D, 1D, 0.75D, 0.75D );
                case WEST:
                    return new AxisAlignedBB( 0D, 0D, 1D, 0.75D, 0.75D, 0D );
                case EAST:
                    return new AxisAlignedBB( 1D, 0D, 0D, 0.25D, 0.75D, 1D );
                case UP:
                    return new AxisAlignedBB( 0D, 1D, 0D, 1D, 0.25D, 0.75D );
                case DOWN:
                    return new AxisAlignedBB( 0D, 0D, 1D, 1D, 0.75D, 0.25D );
            }
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

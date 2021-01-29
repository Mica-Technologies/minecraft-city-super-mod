package com.micatechnologies.minecraft.csm.lighting;

import com.micatechnologies.minecraft.csm.creativetab.TabLighting;
import net.minecraft.block.Block;
import net.minecraft.block.BlockDirectional;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyDirection;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.Random;

public abstract class AbstractBrightLight extends Block
{

    public static final PropertyDirection FACING = BlockDirectional.FACING;

    public AbstractBrightLight() {
        super( Material.ROCK );
        setUnlocalizedName( getBlockRegistryName() );
        setSoundType( SoundType.GROUND );
        setHarvestLevel( "pickaxe", 1 );
        setHardness( 2F );
        setResistance( 10F );
        setLightLevel( 0F );
        setLightOpacity( 0 );
        setCreativeTab( TabLighting.tab );
        this.setDefaultState( this.blockState.getBaseState().withProperty( FACING, EnumFacing.NORTH ) );
    }

    @SideOnly( Side.CLIENT )
    @Override
    public BlockRenderLayer getBlockLayer() {
        return BlockRenderLayer.CUTOUT_MIPPED;
    }

    @Override
    protected BlockStateContainer createBlockState() {
        return new BlockStateContainer( this, FACING );
    }

    @Override
    public IBlockState getStateFromMeta( int meta ) {
        return this.getDefaultState().withProperty( FACING, EnumFacing.getFront( meta ) );
    }

    @Override
    public int getMetaFromState( IBlockState state ) {
        return state.getValue( FACING ).getIndex();
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
        return this.getDefaultState().withProperty( FACING, EnumFacing.getDirectionFromEntityLiving( pos, placer ) );
    }

    @Override
    public boolean isFullCube( IBlockState state ) {
        return false;
    }

    @Override
    public AxisAlignedBB getBoundingBox( IBlockState state, IBlockAccess source, BlockPos pos ) {
        switch ( state.getValue( FACING ) ) {
            case SOUTH:
            default:
                return new AxisAlignedBB( 1D, 0D, 0.2D, 0D, 1D, 0D );
            case NORTH:
                return new AxisAlignedBB( 0D, 0D, 0.8D, 1D, 1D, 1D );
            case WEST:
                return new AxisAlignedBB( 0.8D, 0D, 1D, 1D, 1D, 0D );
            case EAST:
                return new AxisAlignedBB( 0.2D, 0D, 0D, 0D, 1D, 1D );
            case UP:
                return new AxisAlignedBB( 0D, 0.2D, 0D, 1D, 0D, 1D );
            case DOWN:
                return new AxisAlignedBB( 0D, 0.8D, 1D, 1D, 1D, 0D );
        }
    }

    @Override
    public int getLightValue( IBlockState state, IBlockAccess world, BlockPos pos ) {
        return 0;
    }

    @Override
    public boolean isOpaqueCube( IBlockState state ) {
        return false;
    }

    @Override
    public int tickRate( World p_tickRate_1_ ) {
        return 180;
    }

    @Override
    public void onBlockAdded( World p_onBlockAdded_1_, BlockPos p_onBlockAdded_2_, IBlockState p_onBlockAdded_3_ ) {
        p_onBlockAdded_1_.scheduleUpdate( p_onBlockAdded_2_, this, this.tickRate( p_onBlockAdded_1_ ) );
        super.onBlockAdded( p_onBlockAdded_1_, p_onBlockAdded_2_, p_onBlockAdded_3_ );
    }

    @Override
    public void updateTick( World p_updateTick_1_,
                            BlockPos p_updateTick_2_,
                            IBlockState p_updateTick_3_,
                            Random p_updateTick_4_ )
    {
        try {
            onTick( p_updateTick_1_, p_updateTick_2_, p_updateTick_3_ );
        }
        catch ( Exception ignored ) {

        }
        p_updateTick_1_.scheduleUpdate( p_updateTick_2_, this, this.tickRate( p_updateTick_1_ ) );
    }

    abstract public String getBlockRegistryName();

    abstract public void onTick( World world, BlockPos blockPos, IBlockState blockState );
}

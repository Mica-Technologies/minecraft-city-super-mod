package com.micatechnologies.minecraft.csm.block;

import net.minecraft.block.Block;
import net.minecraft.block.BlockHorizontal;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyDirection;
import net.minecraft.block.properties.PropertyInteger;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public abstract class AbstractBlockControllableSignal extends Block
{
    public final        int               SIGNAL_RED    = 0;
    public final        int               SIGNAL_YELLOW = 1;
    public final        int               SIGNAL_GREEN  = 2;
    public final        int               SIGNAL_OFF    = 3;
    public static final PropertyDirection FACING        = BlockHorizontal.FACING;
    public static final PropertyInteger   COLOR         = PropertyInteger.create( "color", 0, 3 );

    public AbstractBlockControllableSignal( Material p_i46399_1_, MapColor p_i46399_2_ )
    {
        super( p_i46399_1_, p_i46399_2_ );
    }

    public AbstractBlockControllableSignal( Material p_i45394_1_ ) {
        super( p_i45394_1_ );
    }

    public enum SIGNAL_SIDE
    {
        LEFT, AHEAD, RIGHT, CROSSWALK
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
    public IBlockState getStateFromMeta( int meta ) {
        int colorVal = meta % 4;
        int facingVal = ( meta - colorVal ) / 4;

        return getDefaultState().withProperty( FACING, EnumFacing.getHorizontal( facingVal ) )
                                .withProperty( COLOR, colorVal );
    }

    @Override
    public int getMetaFromState( IBlockState state ) {
        int facingVal = state.getValue( FACING ).getHorizontalIndex() * 4;
        int colorVal = state.getValue( COLOR );
        return facingVal + colorVal;
    }

    @Override
    protected BlockStateContainer createBlockState() {
        return new BlockStateContainer( this, FACING, COLOR );
    }

    @Override
    public int getLightValue( IBlockState state, IBlockAccess world, BlockPos pos ) {
        return state.getValue( COLOR ) != SIGNAL_OFF ? 15 : 0;
    }

    @Override
    public boolean isOpaqueCube( IBlockState state ) {
        return false;
    }

    public abstract SIGNAL_SIDE getSignalSide();

    public static void changeSignalColor( World world, BlockPos blockPos, int signalColor ) {
        IBlockState blockState = world.getBlockState( blockPos );
        world.setBlockState( blockPos, blockState.withProperty( COLOR, signalColor ) );
    }
}
package com.micatechnologies.minecraft.csm.trafficsignals.logic;

import com.micatechnologies.minecraft.csm.trafficsignals.TileEntityTrafficSignalSensor;
import net.minecraft.block.Block;
import net.minecraft.block.BlockHorizontal;
import net.minecraft.block.ITileEntityProvider;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyDirection;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import javax.annotation.ParametersAreNonnullByDefault;

public class AbstractBlockTrafficSignalSensor extends Block implements ITileEntityProvider
{

    public static final PropertyDirection FACING = BlockHorizontal.FACING;

    public AbstractBlockTrafficSignalSensor( Material blockMaterialIn, MapColor blockMapColorIn )
    {
        super( blockMaterialIn, blockMapColorIn );
    }

    public AbstractBlockTrafficSignalSensor( Material materialIn ) {
        super( materialIn );
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
        return getDefaultState().withProperty( FACING, EnumFacing.getHorizontal( meta ) );
    }

    @Override
    public int getMetaFromState( IBlockState state ) {
        return state.getValue( FACING ).getHorizontalIndex();
    }

    @Override
    protected BlockStateContainer createBlockState() {
        return new BlockStateContainer( this, FACING );
    }

    @Override
    public boolean isOpaqueCube( IBlockState state ) {
        return false;
    }

    @Override
    @ParametersAreNonnullByDefault
    public TileEntity createNewTileEntity( World world, int i ) {
        return new TileEntityTrafficSignalSensor();
    }
}

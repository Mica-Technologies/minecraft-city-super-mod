package com.micatechnologies.minecraft.csm.trafficsignals.logic;

import com.micatechnologies.minecraft.csm.trafficsignals.TileEntityTrafficSignalController;
import com.micatechnologies.minecraft.csm.trafficsignals.TileEntityTrafficSignalSensor;
import net.minecraft.block.Block;
import net.minecraft.block.BlockDirectional;
import net.minecraft.block.ITileEntityProvider;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.properties.PropertyDirection;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.Mirror;
import net.minecraft.util.Rotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import javax.annotation.ParametersAreNonnullByDefault;

public class AbstractBlockTrafficSignalSensor extends Block implements ITileEntityProvider
{

    public static final PropertyDirection FACING = BlockDirectional.FACING;

    public AbstractBlockTrafficSignalSensor( Material blockMaterialIn, MapColor blockMapColorIn )
    {
        super( blockMaterialIn, blockMapColorIn );
    }

    public AbstractBlockTrafficSignalSensor( Material materialIn ) {
        super( materialIn );
    }

    @Override
    protected net.minecraft.block.state.BlockStateContainer createBlockState() {
        return new net.minecraft.block.state.BlockStateContainer( this, new IProperty[]{ FACING } );
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
    public IBlockState getStateFromMeta( int meta ) {
        return this.getDefaultState().withProperty( FACING, EnumFacing.getFront( meta ) );
    }

    @Override
    public int getMetaFromState( IBlockState state ) {
        return ( ( EnumFacing ) state.getValue( FACING ) ).getIndex();
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
    public boolean isOpaqueCube( IBlockState state ) {
        return false;
    }

    @Override
    @ParametersAreNonnullByDefault
    public TileEntity createNewTileEntity( World world, int i ) {
        return new TileEntityTrafficSignalSensor();
    }
}

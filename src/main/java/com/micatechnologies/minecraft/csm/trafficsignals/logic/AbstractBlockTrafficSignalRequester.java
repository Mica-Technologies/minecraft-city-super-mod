package com.micatechnologies.minecraft.csm.trafficsignals.logic;

import com.micatechnologies.minecraft.csm.codeutils.ICsmTileEntityProvider;
import com.micatechnologies.minecraft.csm.trafficsignals.ItemNSSignalLinker;
import com.micatechnologies.minecraft.csm.trafficsignals.TileEntityTrafficSignalRequester;
import com.micatechnologies.minecraft.csm.trafficsignals.TileEntityTrafficSignalTickableRequester;
import net.minecraft.block.ITileEntityProvider;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.*;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import javax.annotation.ParametersAreNonnullByDefault;

public abstract class AbstractBlockTrafficSignalRequester extends AbstractBlockControllableCrosswalkAccessory
        implements ICsmTileEntityProvider
{

    public AbstractBlockTrafficSignalRequester( Material p_i45394_1_ ) {
        super( p_i45394_1_ );
    }

    @Override
    public boolean onBlockActivated( World p_180639_1_,
                                     BlockPos p_180639_2_,
                                     IBlockState p_180639_3_,
                                     EntityPlayer p_180639_4_,
                                     EnumHand p_180639_5_,
                                     EnumFacing p_180639_6_,
                                     float p_180639_7_,
                                     float p_180639_8_,
                                     float p_180639_9_ )
    {
        if ( p_180639_4_.inventory.getCurrentItem() != null &&
                p_180639_4_.inventory.getCurrentItem().getItem() instanceof ItemNSSignalLinker.ItemCustom ) {
            return super.onBlockActivated( p_180639_1_, p_180639_2_, p_180639_3_, p_180639_4_, p_180639_5_, p_180639_6_,
                                           p_180639_7_, p_180639_8_, p_180639_9_ );
        }

        try {
            TileEntity rawTileEntity = p_180639_1_.getTileEntity( p_180639_2_ );
            if ( rawTileEntity instanceof TileEntityTrafficSignalTickableRequester ) {
                TileEntityTrafficSignalTickableRequester tileEntity
                        = ( TileEntityTrafficSignalTickableRequester ) rawTileEntity;
                tileEntity.incrementRequestCount();
            }
            else {
                System.err.println( "Unable to send a traffic signal request due to tile entity missing error!" );
            }
        }
        catch ( Exception e ) {
            System.err.println( "An error occurred while activating a traffic signal request!" );
            e.printStackTrace();
        }

        return true;
    }

    public static void resetRequestCount( World world, BlockPos blockPos ) {
        try {
            TileEntity rawTileEntity = world.getTileEntity( blockPos );
            if ( rawTileEntity instanceof TileEntityTrafficSignalTickableRequester ) {
                TileEntityTrafficSignalTickableRequester tileEntity
                        = ( TileEntityTrafficSignalTickableRequester ) rawTileEntity;
                tileEntity.resetRequestCount();
            }
            else {
                System.err.println(
                        "Unable to reset the traffic signal's request count due to tile entity missing error!" );
            }
        }
        catch ( Exception e ) {
            System.err.println( "An error occurred while resetting the traffic signal's request count!" );
            e.printStackTrace();
        }
    }

    /**
     * Gets the tile entity class for the block.
     *
     * @return the tile entity class for the block
     *
     * @since 1.0
     */
    @Override
    public Class< ? extends TileEntity > getTileEntityClass() {
        return TileEntityTrafficSignalRequester.class;
    }
}

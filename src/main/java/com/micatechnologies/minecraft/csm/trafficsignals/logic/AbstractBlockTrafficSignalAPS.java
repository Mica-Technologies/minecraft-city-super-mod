package com.micatechnologies.minecraft.csm.trafficsignals.logic;

import com.micatechnologies.minecraft.csm.trafficsignals.TileEntityTrafficSignalAPS;
import com.micatechnologies.minecraft.csm.trafficsignals.TileEntityTrafficSignalRequester;
import com.micatechnologies.minecraft.csm.trafficsignals.TileEntityTrafficSignalTickableRequester;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

public abstract class AbstractBlockTrafficSignalAPS extends AbstractBlockTrafficSignalTickableRequester
{
    public AbstractBlockTrafficSignalAPS( Material p_i45394_1_ ) {
        super( p_i45394_1_ );
    }

    @Override
    public int getLightValue( IBlockState state, IBlockAccess world, BlockPos pos ) {
        return 0;
    }

    /**
     * Retrieves the bounding box of the block.
     *
     * @param state  the block state
     * @param source the block access
     * @param pos    the block position
     *
     * @return The bounding box of the block.
     *
     * @since 1.0
     */
    public AxisAlignedBB getBlockBoundingBox( IBlockState state, IBlockAccess source, BlockPos pos ) {
        return new AxisAlignedBB(0D, 0D, 0.8D, 1D, 1D, 1D );
    }

    @Override
    public boolean onBlockActivated( World p_onBlockActivated_1_,
                                     BlockPos p_onBlockActivated_2_,
                                     IBlockState p_onBlockActivated_3_,
                                     EntityPlayer p_onBlockActivated_4_,
                                     EnumHand p_onBlockActivated_5_,
                                     EnumFacing p_onBlockActivated_6_,
                                     float p_onBlockActivated_7_,
                                     float p_onBlockActivated_8_,
                                     float p_onBlockActivated_9_ )
    {
        // Switch to next sound if sneak-clicked
        if ( p_onBlockActivated_4_.isSneaking() ) {
            TileEntity rawTileEntity = p_onBlockActivated_1_.getTileEntity( p_onBlockActivated_2_ );
            if ( rawTileEntity instanceof TileEntityTrafficSignalAPS ) {
                TileEntityTrafficSignalAPS tileEntity = ( TileEntityTrafficSignalAPS ) rawTileEntity;
                String newSoundName = tileEntity.switchSound();
                if ( !p_onBlockActivated_1_.isRemote ) {
                    p_onBlockActivated_4_.sendMessage(
                            new TextComponentString( "APS has switched voice mode to: " + newSoundName ) );
                }
            }
        }

        // Play onPress from tile entity
        if ( p_onBlockActivated_3_.getValue( COLOR ) == SIGNAL_RED ||
                p_onBlockActivated_3_.getValue( COLOR ) == SIGNAL_YELLOW ) {
            TileEntity rawTileEntity = p_onBlockActivated_1_.getTileEntity( p_onBlockActivated_2_ );
            if ( rawTileEntity instanceof TileEntityTrafficSignalAPS ) {
                TileEntityTrafficSignalAPS tileEntity = ( TileEntityTrafficSignalAPS ) rawTileEntity;
                tileEntity.onPress();
            }
        }

        return super.onBlockActivated( p_onBlockActivated_1_, p_onBlockActivated_2_, p_onBlockActivated_3_,
                                       p_onBlockActivated_4_, p_onBlockActivated_5_, p_onBlockActivated_6_,
                                       p_onBlockActivated_7_, p_onBlockActivated_8_, p_onBlockActivated_9_ );
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
        return TileEntityTrafficSignalAPS.class;
    }

    /**
     * Gets the tile entity name for the block.
     *
     * @return the tile entity name for the block
     *
     * @since 1.0
     */
    @Override
    public String getTileEntityName() {
        return "tileentitytrafficsignalaps";
    }
}

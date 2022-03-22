package com.micatechnologies.minecraft.csm.trafficsignals;

import com.micatechnologies.minecraft.csm.ElementsCitySuperMod;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.util.List;

@ElementsCitySuperMod.ModElement.Tag
public class TileEntityTrafficSignalRequester extends TileEntity
{
    private static final String REQUEST_COUNT_KEY = "requestCount";
    private              int    requestCount      = 0;

    @Override
    public void readFromNBT( NBTTagCompound p_readFromNBT_1_ ) {
        super.readFromNBT( p_readFromNBT_1_ );

        if ( p_readFromNBT_1_.hasKey( REQUEST_COUNT_KEY ) ) {
            requestCount = p_readFromNBT_1_.getInteger( REQUEST_COUNT_KEY );
        }
    }

    @Override
    public boolean shouldRefresh( World p_shouldRefresh_1_,
                                  BlockPos p_shouldRefresh_2_,
                                  IBlockState p_shouldRefresh_3_,
                                  IBlockState p_shouldRefresh_4_ )
    {
        return false;
    }

    @Override
    public NBTTagCompound writeToNBT( NBTTagCompound p_writeToNBT_1_ ) {
        p_writeToNBT_1_.setInteger( REQUEST_COUNT_KEY, requestCount );

        return super.writeToNBT( p_writeToNBT_1_ );
    }

    public void resetRequestCount() {
        requestCount = 0;
    }

    public int getRequestCount() {
        return requestCount;
    }

    public void incrementRequestCount() {
        requestCount++;
    }

    @Override
    @Nullable
    public SPacketUpdateTileEntity getUpdatePacket()
    {
        NBTTagCompound nbtTagCompound = new NBTTagCompound();
        writeToNBT( nbtTagCompound );
        int metadata = getBlockMetadata();
        return new SPacketUpdateTileEntity( this.pos, metadata, nbtTagCompound );
    }

    @Override
    public void onDataPacket( NetworkManager networkManager, SPacketUpdateTileEntity pkt ) {
        readFromNBT( pkt.getNbtCompound() );
    }

    @Override
    public NBTTagCompound getUpdateTag()
    {
        NBTTagCompound nbtTagCompound = new NBTTagCompound();
        writeToNBT( nbtTagCompound );
        return nbtTagCompound;
    }

    @Override
    public void handleUpdateTag( NBTTagCompound nbtTagCompound )
    {
        this.readFromNBT( nbtTagCompound );
    }
}

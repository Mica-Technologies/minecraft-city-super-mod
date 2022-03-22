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
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.util.List;

@ElementsCitySuperMod.ModElement.Tag
public class TileEntityTrafficSignalSensor extends TileEntity
{
    private static final String CORNER_1_KEY = "blockPos1";
    private static final String CORNER_2_KEY = "blockPos2";

    private BlockPos corner1;
    private BlockPos corner2;

    @Override
    public void readFromNBT( NBTTagCompound p_readFromNBT_1_ ) {
        super.readFromNBT( p_readFromNBT_1_ );

        if ( p_readFromNBT_1_.hasKey( CORNER_1_KEY ) ) {
            corner1 = BlockPos.fromLong( p_readFromNBT_1_.getLong( CORNER_1_KEY ) );
        }
        else {
            corner1 = null;
        }

        if ( p_readFromNBT_1_.hasKey( CORNER_2_KEY ) ) {
            corner2 = BlockPos.fromLong( p_readFromNBT_1_.getLong( CORNER_2_KEY ) );
        }
        else {
            corner2 = null;
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
        if ( corner1 != null ) {
            p_writeToNBT_1_.setLong( CORNER_1_KEY, corner1.toLong() );
        }
        else {
            p_writeToNBT_1_.removeTag( CORNER_1_KEY );
        }

        if ( corner2 != null ) {
            p_writeToNBT_1_.setLong( CORNER_2_KEY, corner2.toLong() );
        }
        else {
            p_writeToNBT_1_.removeTag( CORNER_2_KEY );
        }

        return super.writeToNBT( p_writeToNBT_1_ );
    }

    public boolean setScanCorners( BlockPos blockPos1, BlockPos blockPos2 ) {
        boolean overwroteExisting = corner1 != null && corner2 != null;
        corner1 = blockPos1;
        corner2 = blockPos2;
        markDirty();
        return overwroteExisting;
    }

    private int scanEntities( World world ) {
        int count = 0;
        if ( world != null && corner1 != null && corner2 != null ) {
            AxisAlignedBB scanRange = new AxisAlignedBB( corner1, corner2 );
            List< Entity > entitiesWithinAABBExcludingEntity = world.getEntitiesWithinAABBExcludingEntity( null,
                                                                                                           scanRange );
            for ( Entity entity : entitiesWithinAABBExcludingEntity ) {
                if ( entity instanceof EntityVillager || entity instanceof EntityPlayer ) {
                    count++;
                }
            }
        }
        return count;
    }

    public int getWaitingCount( World world ) {
        return scanEntities( world );
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

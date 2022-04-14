package com.micatechnologies.minecraft.csm.codeutils;

import net.minecraft.block.state.IBlockState;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ITickable;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import javax.annotation.Nullable;

/**
 * Abstract tile entity implementation with tick handler and customizable tick rate. This class is based on the
 * {@link TileEntity} class and implements the {@link ITickable} interface.
 *
 * @author Mica Technologies
 * @since 2022.1.0
 */
public abstract class AbstractTickableTileEntity extends TileEntity implements ITickable
{

    /**
     * Abstract method which must be implemented to return the NBT tag compound with the tile entity's NBT data.
     *
     * @param compound the NBT tag compound to write the tile entity's NBT data to
     *
     * @return the NBT tag compound with the tile entity's NBT data
     */
    public abstract NBTTagCompound writeNBT( NBTTagCompound compound );

    /**
     * Abstract method which must be implemented to process the reading of the tile entity's NBT data from the supplied
     * NBT tag compound.
     *
     * @param compound the NBT tag compound to read the tile entity's NBT data from
     */
    public abstract void readNBT( NBTTagCompound compound );

    /**
     * Abstract method which must be implemented to return the tick rate of the tile entity.
     *
     * @return the tick rate of the tile entity
     */
    public abstract long getTickRate();

    /**
     * Abstract method which must be implemented to handle the tick event of the tile entity.
     */
    public abstract void onTick();

    /**
     * Abstract method which must be implemented to return a boolean indicating if the tile entity should also tick on
     * the client side. By default, the tile entity will always tick on the server side, and in the event of
     * singleplayer/local mode, the host client is considered the server.
     *
     * @return a boolean indicating if the tile entity should also tick on the client side
     */
    public abstract boolean doClientTick();

    /**
     * Handler for when the tile entity ticks. This method is called every tick, and a comparison is made to see if the
     * tile entity's tick rate has been reached. If so, the onTick() method is called. The tick rate is determined by
     * the value returned by the {@link #getTickRate()} method.
     */
    @Override
    public void update() {
        if ( getWorld().getTotalWorldTime() % getTickRate() == 0L ) {
            if ( doClientTick() || !getWorld().isRemote ) {
                try {
                    onTick();
                }
                catch ( Exception e ) {
                    System.err.println( "An error occurred while ticking [remote: " +
                                                world.isRemote +
                                                "] a tile entity [" +
                                                this.getClass().getCanonicalName() +
                                                "]: " );
                    e.printStackTrace( System.err );
                }
            }
        }
    }

    /**
     * Method which processes the reading of the tile entity's NBT data from the supplied NBT tag compound.
     *
     * @param compound the NBT tag compound to read the tile entity's NBT data from
     */
    @Override
    public void readFromNBT( NBTTagCompound compound ) {
        readNBT( compound );
        super.readFromNBT( compound );
    }

    /**
     * Method which processes the writing of the tile entity's NBT data to the supplied NBT tag compound.
     *
     * @param compound the NBT tag compound to write the tile entity's NBT data to
     *
     * @return the NBT tag compound with the tile entity's NBT data
     */
    @Override
    public NBTTagCompound writeToNBT( NBTTagCompound compound ) {
        return super.writeToNBT( writeNBT( compound ) );
    }

    /**
     * Overridden handler which returns whether the tile entity should be refreshed or not. In this case, we always
     * return false, as we don't want to refresh the tile entity.
     *
     * @param world    the block/tile entity's world
     * @param pos      the block/tile entity's position
     * @param oldState the old state of the block/tile entity
     * @param newState the new state of the block/tile entity
     *
     * @return false, always
     */
    @Override
    public boolean shouldRefresh( World world, BlockPos pos, IBlockState oldState, IBlockState newState )
    {
        return false;
    }

    /**
     * Helper method which marks the tile entity as dirty, and schedules a world update for the block/tile entity.
     *
     * @param world the block/tile entity's world
     * @param pos   the block/tile entity's position
     * @param state the block/tile entity's state
     */
    public void markDirtySync( World world, BlockPos pos, IBlockState state ) {
        markDirty();
        world.markBlockRangeForRenderUpdate( pos, pos );
        world.notifyBlockUpdate( pos, state, state, 3 );
        world.scheduleBlockUpdate( pos, this.getBlockType(), 0, 0 );
    }

    /**
     * Prepares a tile entity update packet for syncing the tile entity to the client. The tile entity update packet is
     * populated with the tile entity's NBT data. This method is similar to {@link #getUpdateTag()}, but is used when
     * the tile entity is being synced to the client.
     *
     * @return the tile entity update packet for syncing the tile entity to the client
     */
    @Override
    @Nullable
    public SPacketUpdateTileEntity getUpdatePacket()
    {
        // Create a new NBT tag compound and write the tile entity NBT data to it
        NBTTagCompound nbtTagCompound = new NBTTagCompound();
        this.writeToNBT( nbtTagCompound );

        // Create a new update packet with the new NBT tag compound
        return new SPacketUpdateTileEntity( getPos(), getBlockMetadata(), getUpdateTag() );
    }

    /**
     * Handler for when a tile entity update packet is received from the server. This method applies the tile entity's
     * update packet data to the tile entity NBT data. This method is similar to
     * {@link #handleUpdateTag(NBTTagCompound)}, but is used when the tile entity is being synced to the client.
     *
     * @param networkManager the network manager
     * @param pkt            the tile entity update packet for reading the tile entity NBT data
     */
    @Override
    public void onDataPacket( NetworkManager networkManager, SPacketUpdateTileEntity pkt ) {
        // Read the update packet data NBT tag compound in to the tile entity NBT data
        this.readFromNBT( pkt.getNbtCompound() );
    }

    /**
     * Prepares a tile entity update tag for syncing the tile entity to the client. The tile entity update tag is
     * populated with the tile entity's NBT data. This method is similar to {@link #getUpdatePacket()}, but is used when
     * chunk data is sent to the client.
     *
     * @return the tile entity update tag for syncing the tile entity to the client
     */
    @Override
    public NBTTagCompound getUpdateTag()
    {
        // Create a new NBT tag compound and write the tile entity NBT data to it
        NBTTagCompound nbtTagCompound = new NBTTagCompound();
        return this.writeToNBT( nbtTagCompound );
    }

    /**
     * Handler for when a tile entity update tag is received from the server. This method applies the tile entity's
     * update tag data to the tile entity NBT data. This method is similar to
     * {@link #onDataPacket(NetworkManager, SPacketUpdateTileEntity)}, but is used when chunk data is sent to the
     * client.
     *
     * @param nbtTagCompound the tile entity update tag for reading the tile entity NBT data
     */
    @Override
    public void handleUpdateTag( NBTTagCompound nbtTagCompound )
    {
        // Read the update tag data NBT tag compound in to the tile entity NBT data
        this.readFromNBT( nbtTagCompound );
    }
}

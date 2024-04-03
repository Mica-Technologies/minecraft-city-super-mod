package com.micatechnologies.minecraft.csm.codeutils;

import javax.annotation.Nullable;
import net.minecraft.block.state.IBlockState;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.util.Constants;

/**
 * Abstract tile entity implementation. This class is based on the {@link TileEntity} class.
 *
 * @author Mica Technologies
 * @since 2023.2.0
 */
public abstract class AbstractTileEntity extends TileEntity {

  /**
   * Helper method which marks the tile entity as dirty, and schedules a world update for the
   * block/tile entity. This method does not trigger re-rendering of the block.
   *
   * @param world the block/tile entity's world
   * @param pos   the block/tile entity's position
   */
  public void markDirtySync(World world, BlockPos pos) {
    boolean defaultSendClientUpdate = false;
    markDirtySync(world, pos, defaultSendClientUpdate);
  }

  /**
   * Helper method which marks the tile entity as dirty, and schedules a world update for the
   * block/tile entity. This method does not trigger re-rendering of the block.
   *
   * @param world            the block/tile entity's world
   * @param pos              the block/tile entity's position
   * @param sendClientUpdate whether to send a client update packet or not
   */
  public void markDirtySync(World world, BlockPos pos, boolean sendClientUpdate) {
    world.scheduleBlockUpdate(pos, this.getBlockType(), 0, 0);
    markDirty();
    if (sendClientUpdate) {
      syncServerToClient(world);
    }
  }

  /**
   * Abstract method which must be implemented to process the reading of the tile entity's NBT data
   * from the supplied NBT tag compound.
   *
   * @param compound the NBT tag compound to read the tile entity's NBT data from
   */
  public abstract void readNBT(NBTTagCompound compound);

  /**
   * Helper method which sends a data packet to the client to update the tile entity's NBT data.
   * This method is similar to {@link #markDirtySync(World, BlockPos)}, but is used when it is
   * necessary to send the tile entity's NBT data to the client.
   *
   * @param world the block/tile entity's world
   */
  public void syncServerToClient(World world) {
    if (!world.isRemote) {
      // Send an update packet to the client
      SPacketUpdateTileEntity packet = getUpdatePacket();
      if (packet != null) {
        MinecraftServer minecraftServer = world.getMinecraftServer();
        if (minecraftServer != null) {
          minecraftServer.getPlayerList().sendPacketToAllPlayers(packet);
        }
      }
    }
  }

  /**
   * Method which processes the reading of the tile entity's NBT data from the supplied NBT tag
   * compound.
   *
   * @param compound the NBT tag compound to read the tile entity's NBT data from
   */
  @Override
  public void readFromNBT(NBTTagCompound compound) {
    super.readFromNBT(compound);
    readNBT(compound);
  }

  /**
   * Abstract method which must be implemented to return the NBT tag compound with the tile entity's
   * NBT data.
   *
   * @param compound the NBT tag compound to write the tile entity's NBT data to
   *
   * @return the NBT tag compound with the tile entity's NBT data
   */
  public abstract NBTTagCompound writeNBT(NBTTagCompound compound);

  /**
   * Method which processes the writing of the tile entity's NBT data to the supplied NBT tag
   * compound.
   *
   * @param compound the NBT tag compound to write the tile entity's NBT data to
   *
   * @return the NBT tag compound with the tile entity's NBT data
   */
  @Override
  public NBTTagCompound writeToNBT(NBTTagCompound compound) {
    return writeNBT(super.writeToNBT(compound));
  }

  /**
   * Helper method which marks the tile entity as dirty, and schedules a world update for the
   * block/tile entity. This method triggers re-rendering of the block.
   *
   * @param world the block/tile entity's world
   * @param pos   the block/tile entity's position
   * @param state the block/tile entity's state
   */
  public void markDirtySync(World world, BlockPos pos, IBlockState state) {
    boolean defaultSendClientUpdate = false;
    markDirtySync(world, pos, state, defaultSendClientUpdate);
  }

  /**
   * Overridden handler which returns whether the tile entity should be refreshed or not. In this
   * case, we return false, as we don't want to refresh the tile entity, unless the block has
   * changed.
   *
   * @param world    the block/tile entity's world
   * @param pos      the block/tile entity's position
   * @param oldState the old state of the block/tile entity
   * @param newState the new state of the block/tile entity
   *
   * @return false, always
   */
  @Override
  public boolean shouldRefresh(World world, BlockPos pos, IBlockState oldState,
      IBlockState newState) {
    return oldState.getBlock() != newState.getBlock();
  }

  /**
   * Helper method which marks the tile entity as dirty, and schedules a world update for the
   * block/tile entity. This method triggers re-rendering of the block.
   *
   * @param world            the block/tile entity's world
   * @param pos              the block/tile entity's position
   * @param state            the block/tile entity's state
   * @param sendClientUpdate whether to send a client update packet or not
   */
  public void markDirtySync(World world, BlockPos pos, IBlockState state,
      boolean sendClientUpdate) {
    world.markBlockRangeForRenderUpdate(pos, pos);
    world.notifyBlockUpdate(pos, state, state, Constants.BlockFlags.DEFAULT);
    markDirtySync(world, pos, sendClientUpdate);
  }


  /**
   * Prepares a tile entity update packet for syncing the tile entity to the client. The tile entity
   * update packet is populated with the tile entity's NBT data. This method is similar to
   * {@link #getUpdateTag()}, but is used when the tile entity is being synced to the client.
   *
   * @return the tile entity update packet for syncing the tile entity to the client
   */
  @Override
  @Nullable
  public SPacketUpdateTileEntity getUpdatePacket() {
    // Create a new update packet with the new NBT tag compound
    return new SPacketUpdateTileEntity(getPos(), getBlockMetadata(), getUpdateTag());
  }

  /**
   * Handler for when a tile entity update packet is received from the server. This method applies
   * the tile entity's update packet data to the tile entity NBT data. This method is similar to
   * {@link #handleUpdateTag(NBTTagCompound)}, but is used when the tile entity is being synced to
   * the client.
   *
   * @param networkManager the network manager
   * @param pkt            the tile entity update packet for reading the tile entity NBT data
   */
  @Override
  public void onDataPacket(NetworkManager networkManager, SPacketUpdateTileEntity pkt) {
    // Read the update packet data NBT tag compound in to the tile entity NBT data
    this.readFromNBT(pkt.getNbtCompound());
  }

  /**
   * Prepares a tile entity update tag for syncing the tile entity to the client. The tile entity
   * update tag is populated with the tile entity's NBT data. This method is similar to
   * {@link #getUpdatePacket()}, but is used when chunk data is sent to the client.
   *
   * @return the tile entity update tag for syncing the tile entity to the client
   */
  @Override
  public NBTTagCompound getUpdateTag() {
    // Create a new NBT tag compound and write the tile entity NBT data to it
    return this.writeToNBT(new NBTTagCompound());
  }

  /**
   * Handler for when a tile entity update tag is received from the server. This method applies the
   * tile entity's update tag data to the tile entity NBT data. This method is similar to
   * {@link #onDataPacket(NetworkManager, SPacketUpdateTileEntity)}, but is used when chunk data is
   * sent to the client.
   *
   * @param nbtTagCompound the tile entity update tag for reading the tile entity NBT data
   */
  @Override
  public void handleUpdateTag(NBTTagCompound nbtTagCompound) {
    // Read the update tag data NBT tag compound in to the tile entity NBT data
    this.readFromNBT(nbtTagCompound);
  }
}

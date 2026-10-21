package com.micatechnologies.minecraft.csm.technology.rack;

import com.micatechnologies.minecraft.csm.codeutils.AbstractTileEntity;
import net.minecraft.block.state.IBlockState;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;

/**
 * What a fillable rack holds ({@link RackContents}). It lives in the rack's lowest cell; the
 * upper cell of a two-block rack reads it from there.
 *
 * <p>Everything here is drawn into the chunk mesh by {@code RackBakedModel}, so there is no
 * renderer and nothing ticks. A change is sent to the clients once, with a block update that
 * rebuilds the sections holding both cells; the chunk's own data carries it to a client that
 * loads the rack later ({@link #getUpdateTag()}).</p>
 *
 * <p>One short key, {@code u}: a byte array of {@code [unit ordinal, top U]} pairs, absent when
 * the rack is empty.</p>
 *
 * @since 2026.10
 */
public class TileEntityServerRack extends AbstractTileEntity {

  /** The units, as {@link RackContents#toBytes()}. */
  static final String NBT_UNITS = "u";

  private RackContents contents = RackContents.EMPTY;

  @Override
  public void readNBT(NBTTagCompound compound) {
    contents = RackContents.fromBytes(compound.getByteArray(NBT_UNITS));
  }

  @Override
  public NBTTagCompound writeNBT(NBTTagCompound compound) {
    if (!contents.isEmpty()) {
      compound.setByteArray(NBT_UNITS, contents.toBytes());
    }
    return compound;
  }

  /** What is mounted. */
  public RackContents getContents() {
    return contents;
  }

  /**
   * Replaces what is mounted, saves it and tells the clients, whose rebuilt sections then draw
   * it.
   *
   * @param contents the new contents
   */
  public void setContents(RackContents contents) {
    this.contents = contents;
    if (world != null && !world.isRemote) {
      IBlockState state = world.getBlockState(pos);
      markDirtySync(world, pos, state, true);
      world.markBlockRangeForRenderUpdate(pos, pos.up());
    }
  }

  /** Only the contents reach a baked model, so a sync that leaves them alone redraws nothing. */
  @Override
  protected long getBakedModelKey() {
    return contents.hashCode();
  }

  /** Redraws the upper cell too, which reads these contents from here. */
  @Override
  public void onDataPacket(NetworkManager net, SPacketUpdateTileEntity pkt) {
    RackContents before = contents;
    super.onDataPacket(net, pkt);
    if (world != null && world.isRemote && !before.equals(contents)) {
      world.markBlockRangeForRenderUpdate(pos, pos.up());
    }
  }
}

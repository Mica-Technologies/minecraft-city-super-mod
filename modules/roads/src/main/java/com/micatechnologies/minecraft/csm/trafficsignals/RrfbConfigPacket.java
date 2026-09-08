package com.micatechnologies.minecraft.csm.trafficsignals;

import io.netty.buffer.ByteBuf;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;

/**
 * Client-to-server packet for a click in the RRFB configuration GUI. Carries the beacon's
 * position and which {@link RrfbConfigAction} to apply to it.
 */
public class RrfbConfigPacket implements IMessage {

  private BlockPos pos;
  private int actionOrdinal;

  public RrfbConfigPacket() {
    // Required by Forge
  }

  public RrfbConfigPacket(BlockPos pos, int actionOrdinal) {
    this.pos = pos;
    this.actionOrdinal = actionOrdinal;
  }

  @Override
  public void fromBytes(ByteBuf buf) {
    this.pos = BlockPos.fromLong(buf.readLong());
    this.actionOrdinal = buf.readInt();
  }

  @Override
  public void toBytes(ByteBuf buf) {
    buf.writeLong(this.pos.toLong());
    buf.writeInt(this.actionOrdinal);
  }

  public BlockPos getPos() {
    return pos;
  }

  public int getActionOrdinal() {
    return actionOrdinal;
  }
}

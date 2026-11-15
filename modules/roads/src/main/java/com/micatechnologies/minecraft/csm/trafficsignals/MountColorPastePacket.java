package com.micatechnologies.minecraft.csm.trafficsignals;

import io.netty.buffer.ByteBuf;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;

/**
 * Sent from client to server when a copied colour is pasted onto the hardware that holds a head:
 * a signal cluster hanger's colour, or a signal head's mount colour. Carries the target and the
 * colour as its {@code TrafficSignalBodyColor.toNBT()} ordinal; see
 * {@link SignalAppearanceClipboard} for what a paste does where.
 *
 * @see MountColorPastePacketHandler
 */
public class MountColorPastePacket implements IMessage {

  private BlockPos pos;
  private int color;

  public MountColorPastePacket() {
    // Required by Forge
  }

  public MountColorPastePacket(BlockPos pos, int color) {
    this.pos = pos;
    this.color = color;
  }

  @Override
  public void fromBytes(ByteBuf buf) {
    this.pos = BlockPos.fromLong(buf.readLong());
    this.color = buf.readInt();
  }

  @Override
  public void toBytes(ByteBuf buf) {
    buf.writeLong(this.pos.toLong());
    buf.writeInt(this.color);
  }

  public BlockPos getPos() {
    return pos;
  }

  public int getColor() {
    return color;
  }
}

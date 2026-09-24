package com.micatechnologies.minecraft.csm.streetscape;

import io.netty.buffer.ByteBuf;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;

/**
 * Something a player did on a meter's screen: pay for time on a space, or (the owner) collect
 * the takings. The server decides what the player may do; see
 * {@link ParkingMeterActionPacketHandler}.
 *
 * @version 1.0
 */
public class ParkingMeterActionPacket implements IMessage {

  public static final int PAY = 0;
  public static final int COLLECT = 1;

  private BlockPos pos;
  private int action;
  private int space;
  private int blocks;

  public ParkingMeterActionPacket() {
  }

  public ParkingMeterActionPacket(BlockPos pos, int action, int space, int blocks) {
    this.pos = pos;
    this.action = action;
    this.space = space;
    this.blocks = blocks;
  }

  @Override
  public void fromBytes(ByteBuf buf) {
    pos = BlockPos.fromLong(buf.readLong());
    action = buf.readByte();
    space = buf.readByte();
    blocks = buf.readShort();
  }

  @Override
  public void toBytes(ByteBuf buf) {
    buf.writeLong(pos.toLong());
    buf.writeByte(action);
    buf.writeByte(space);
    buf.writeShort(blocks);
  }

  public BlockPos getPos() {
    return pos;
  }

  public int getAction() {
    return action;
  }

  public int getSpace() {
    return space;
  }

  public int getBlocks() {
    return blocks;
  }
}

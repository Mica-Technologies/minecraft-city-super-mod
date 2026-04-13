package com.micatechnologies.minecraft.csm.trafficsignals;

import io.netty.buffer.ByteBuf;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;

/**
 * Network packet sent from client to server when a player directly sets a parameter value in the
 * signal controller configuration GUI, as opposed to cycling through preset options.
 */
public class SignalControllerSetValuePacket implements IMessage {

  private BlockPos pos;
  private String paramKey;
  private long tickValue;

  public SignalControllerSetValuePacket() {
  }

  public SignalControllerSetValuePacket(BlockPos pos, String paramKey, long tickValue) {
    this.pos = pos;
    this.paramKey = paramKey;
    this.tickValue = tickValue;
  }

  @Override
  public void fromBytes(ByteBuf buf) {
    this.pos = BlockPos.fromLong(buf.readLong());
    this.paramKey = ByteBufUtils.readUTF8String(buf);
    this.tickValue = buf.readLong();
  }

  @Override
  public void toBytes(ByteBuf buf) {
    buf.writeLong(this.pos.toLong());
    ByteBufUtils.writeUTF8String(buf, this.paramKey);
    buf.writeLong(this.tickValue);
  }

  public BlockPos getPos() {
    return pos;
  }

  public String getParamKey() {
    return paramKey;
  }

  public long getTickValue() {
    return tickValue;
  }
}

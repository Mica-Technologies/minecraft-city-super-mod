package com.micatechnologies.minecraft.csm.trafficaccessories;

import io.netty.buffer.ByteBuf;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;

/**
 * Client-to-server packet for a click in the radar speed feedback sign's configuration GUI.
 * Carries the sign's position, the action and a step, so one packet covers both stepping a
 * setting forward and stepping it back.
 */
public class RadarSpeedSignConfigPacket implements IMessage {

  private BlockPos pos;
  private int actionOrdinal;
  private int step;

  public RadarSpeedSignConfigPacket() {
    // Required by Forge
  }

  public RadarSpeedSignConfigPacket(BlockPos pos, int actionOrdinal, int step) {
    this.pos = pos;
    this.actionOrdinal = actionOrdinal;
    this.step = step;
  }

  @Override
  public void fromBytes(ByteBuf buf) {
    this.pos = BlockPos.fromLong(buf.readLong());
    this.actionOrdinal = buf.readInt();
    this.step = buf.readInt();
  }

  @Override
  public void toBytes(ByteBuf buf) {
    buf.writeLong(this.pos.toLong());
    buf.writeInt(this.actionOrdinal);
    buf.writeInt(this.step);
  }

  public BlockPos getPos() {
    return pos;
  }

  public int getActionOrdinal() {
    return actionOrdinal;
  }

  public int getStep() {
    return step;
  }
}

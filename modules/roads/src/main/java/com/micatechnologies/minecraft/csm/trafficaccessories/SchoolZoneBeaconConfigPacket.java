package com.micatechnologies.minecraft.csm.trafficaccessories;

import io.netty.buffer.ByteBuf;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;

/**
 * Client-to-server packet for a click in the school zone beacon configuration GUI. Carries the
 * beacon's position, the action, an index (which schedule hour, for
 * {@link SchoolZoneBeaconConfigAction#ADJUST_SCHEDULE_HOUR}) and a step, so one packet covers
 * both incrementing and decrementing a value.
 */
public class SchoolZoneBeaconConfigPacket implements IMessage {

  private BlockPos pos;
  private int actionOrdinal;
  private int index;
  private int step;

  public SchoolZoneBeaconConfigPacket() {
    // Required by Forge
  }

  public SchoolZoneBeaconConfigPacket(BlockPos pos, int actionOrdinal, int index, int step) {
    this.pos = pos;
    this.actionOrdinal = actionOrdinal;
    this.index = index;
    this.step = step;
  }

  @Override
  public void fromBytes(ByteBuf buf) {
    this.pos = BlockPos.fromLong(buf.readLong());
    this.actionOrdinal = buf.readInt();
    this.index = buf.readInt();
    this.step = buf.readInt();
  }

  @Override
  public void toBytes(ByteBuf buf) {
    buf.writeLong(this.pos.toLong());
    buf.writeInt(this.actionOrdinal);
    buf.writeInt(this.index);
    buf.writeInt(this.step);
  }

  public BlockPos getPos() {
    return pos;
  }

  public int getActionOrdinal() {
    return actionOrdinal;
  }

  public int getIndex() {
    return index;
  }

  public int getStep() {
    return step;
  }
}

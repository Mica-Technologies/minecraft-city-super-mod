package com.micatechnologies.minecraft.csm.trafficaccessories;

import io.netty.buffer.ByteBuf;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;

/**
 * Client-to-server packet for a click in the lane control controller's configuration GUI.
 * Carries the controller's position, the action, the group it applies to and the time-of-day
 * slot, since most of the settings are per group per slot.
 */
public class LaneControlControllerConfigPacket implements IMessage {

  private BlockPos pos;
  private int actionOrdinal;
  private int group;
  private int slot;

  public LaneControlControllerConfigPacket() {
    // Required by Forge
  }

  public LaneControlControllerConfigPacket(BlockPos pos, int actionOrdinal, int group, int slot) {
    this.pos = pos;
    this.actionOrdinal = actionOrdinal;
    this.group = group;
    this.slot = slot;
  }

  @Override
  public void fromBytes(ByteBuf buf) {
    this.pos = BlockPos.fromLong(buf.readLong());
    this.actionOrdinal = buf.readInt();
    this.group = buf.readInt();
    this.slot = buf.readInt();
  }

  @Override
  public void toBytes(ByteBuf buf) {
    buf.writeLong(this.pos.toLong());
    buf.writeInt(this.actionOrdinal);
    buf.writeInt(this.group);
    buf.writeInt(this.slot);
  }

  public BlockPos getPos() {
    return pos;
  }

  public int getActionOrdinal() {
    return actionOrdinal;
  }

  public int getGroup() {
    return group;
  }

  public int getSlot() {
    return slot;
  }
}

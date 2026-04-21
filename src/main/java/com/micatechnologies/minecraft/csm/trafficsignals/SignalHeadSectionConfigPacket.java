package com.micatechnologies.minecraft.csm.trafficsignals;

import io.netty.buffer.ByteBuf;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;

/**
 * Network packet sent from client to server when a player clicks a property button in the
 * per-section page of the signal head configuration GUI. Carries the block position, the
 * target section index, and the action to perform on that section.
 */
public class SignalHeadSectionConfigPacket implements IMessage {

  private BlockPos pos;
  private int sectionIndex;
  private int actionOrdinal;

  public SignalHeadSectionConfigPacket() {
    // Required by Forge
  }

  public SignalHeadSectionConfigPacket(BlockPos pos, int sectionIndex, int actionOrdinal) {
    this.pos = pos;
    this.sectionIndex = sectionIndex;
    this.actionOrdinal = actionOrdinal;
  }

  @Override
  public void fromBytes(ByteBuf buf) {
    this.pos = BlockPos.fromLong(buf.readLong());
    this.sectionIndex = buf.readInt();
    this.actionOrdinal = buf.readInt();
  }

  @Override
  public void toBytes(ByteBuf buf) {
    buf.writeLong(this.pos.toLong());
    buf.writeInt(this.sectionIndex);
    buf.writeInt(this.actionOrdinal);
  }

  public BlockPos getPos() {
    return pos;
  }

  public int getSectionIndex() {
    return sectionIndex;
  }

  public int getActionOrdinal() {
    return actionOrdinal;
  }
}

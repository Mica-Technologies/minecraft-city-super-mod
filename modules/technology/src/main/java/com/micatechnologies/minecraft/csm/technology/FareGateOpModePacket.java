package com.micatechnologies.minecraft.csm.technology;

import io.netty.buffer.ByteBuf;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;

/**
 * Client → server packet that sets the operator override mode on a fare gate.
 *
 * @author Mica Technologies
 * @since 2026.5
 */
public class FareGateOpModePacket implements IMessage {

  private BlockPos pos;
  private int modeOrdinal;

  public FareGateOpModePacket() {
    // Required by Forge
  }

  public FareGateOpModePacket(BlockPos pos, FareGateOpMode mode) {
    this.pos = pos;
    this.modeOrdinal = mode.ordinal();
  }

  @Override
  public void fromBytes(ByteBuf buf) {
    this.pos = BlockPos.fromLong(buf.readLong());
    this.modeOrdinal = buf.readInt();
  }

  @Override
  public void toBytes(ByteBuf buf) {
    buf.writeLong(pos.toLong());
    buf.writeInt(modeOrdinal);
  }

  public BlockPos getPos() {
    return pos;
  }

  public int getModeOrdinal() {
    return modeOrdinal;
  }
}

package com.micatechnologies.minecraft.csm.trafficsignals;

import io.netty.buffer.ByteBuf;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;

/**
 * Client&rarr;server packet carrying a single edit to a controller's ADVANCED (NEMA) program from
 * the CSM ASC-3 programming GUI. A compact generic shape — {@code action} names the field/operation,
 * {@code index} addresses a phase number or preempt index where relevant, and {@code value} carries
 * the new value (ticks, enum ordinal, circuit index, phase number to toggle, or 0/1 boolean).
 *
 * @author Mica Technologies
 * @since 2026.6
 */
public class AdvancedSignalControllerConfigPacket implements IMessage {

  private BlockPos pos;
  private String action;
  private int index;
  private long value;

  public AdvancedSignalControllerConfigPacket() {
  }

  public AdvancedSignalControllerConfigPacket(BlockPos pos, String action, int index, long value) {
    this.pos = pos;
    this.action = action;
    this.index = index;
    this.value = value;
  }

  @Override
  public void fromBytes(ByteBuf buf) {
    this.pos = BlockPos.fromLong(buf.readLong());
    this.action = ByteBufUtils.readUTF8String(buf);
    this.index = buf.readInt();
    this.value = buf.readLong();
  }

  @Override
  public void toBytes(ByteBuf buf) {
    buf.writeLong(this.pos.toLong());
    ByteBufUtils.writeUTF8String(buf, this.action);
    buf.writeInt(this.index);
    buf.writeLong(this.value);
  }

  public BlockPos getPos() {
    return pos;
  }

  public String getAction() {
    return action;
  }

  public int getIndex() {
    return index;
  }

  public long getValue() {
    return value;
  }
}

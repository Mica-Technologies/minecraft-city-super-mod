package com.micatechnologies.minecraft.csm.streetscape;

import io.netty.buffer.ByteBuf;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;

/**
 * A utility box's ID number, sent from its editor. The server clamps both lines again
 * ({@link TileEntityUtilityBoxLabel#clamp}); what arrives here is only trusted to be short.
 *
 * @version 1.0
 */
public class UtilityBoxLabelPacket implements IMessage {

  /** A line longer than this is cut on arrival, before anything else looks at it. */
  private static final int MAX_WIRE_LENGTH = 32;

  private BlockPos pos;
  private String line1;
  private String line2;

  public UtilityBoxLabelPacket() {
  }

  public UtilityBoxLabelPacket(BlockPos pos, String line1, String line2) {
    this.pos = pos;
    this.line1 = line1 == null ? "" : line1;
    this.line2 = line2 == null ? "" : line2;
  }

  @Override
  public void fromBytes(ByteBuf buf) {
    this.pos = BlockPos.fromLong(buf.readLong());
    this.line1 = cut(ByteBufUtils.readUTF8String(buf));
    this.line2 = cut(ByteBufUtils.readUTF8String(buf));
  }

  private static String cut(String read) {
    return read.length() > MAX_WIRE_LENGTH ? read.substring(0, MAX_WIRE_LENGTH) : read;
  }

  @Override
  public void toBytes(ByteBuf buf) {
    buf.writeLong(this.pos.toLong());
    ByteBufUtils.writeUTF8String(buf, this.line1);
    ByteBufUtils.writeUTF8String(buf, this.line2);
  }

  public BlockPos getPos() {
    return pos;
  }

  public String getLine1() {
    return line1;
  }

  public String getLine2() {
    return line2;
  }
}

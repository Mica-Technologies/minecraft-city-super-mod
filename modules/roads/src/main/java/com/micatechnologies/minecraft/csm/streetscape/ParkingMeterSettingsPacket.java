package com.micatechnologies.minecraft.csm.streetscape;

import io.netty.buffer.ByteBuf;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;

/**
 * A meter's settings from its owner's screen. The server checks the sender may change them and
 * clamps every value to its caps ({@link TileEntityParkingMeter#applySettings}).
 *
 * @version 1.0
 */
public class ParkingMeterSettingsPacket implements IMessage {

  private BlockPos pos;
  private int emeraldsPerBlock;
  private int minutesPerBlock;
  private int maxMinutes;
  private double moneyPerBlock;
  private boolean collect;
  private int spaces;

  public ParkingMeterSettingsPacket() {
  }

  public ParkingMeterSettingsPacket(BlockPos pos, int emeraldsPerBlock, int minutesPerBlock,
      int maxMinutes, double moneyPerBlock, boolean collect, int spaces) {
    this.pos = pos;
    this.emeraldsPerBlock = emeraldsPerBlock;
    this.minutesPerBlock = minutesPerBlock;
    this.maxMinutes = maxMinutes;
    this.moneyPerBlock = moneyPerBlock;
    this.collect = collect;
    this.spaces = spaces;
  }

  @Override
  public void fromBytes(ByteBuf buf) {
    pos = BlockPos.fromLong(buf.readLong());
    emeraldsPerBlock = buf.readInt();
    minutesPerBlock = buf.readInt();
    maxMinutes = buf.readInt();
    moneyPerBlock = buf.readDouble();
    collect = buf.readBoolean();
    spaces = buf.readByte();
  }

  @Override
  public void toBytes(ByteBuf buf) {
    buf.writeLong(pos.toLong());
    buf.writeInt(emeraldsPerBlock);
    buf.writeInt(minutesPerBlock);
    buf.writeInt(maxMinutes);
    buf.writeDouble(moneyPerBlock);
    buf.writeBoolean(collect);
    buf.writeByte(spaces);
  }

  public BlockPos getPos() {
    return pos;
  }

  public int getEmeraldsPerBlock() {
    return emeraldsPerBlock;
  }

  public int getMinutesPerBlock() {
    return minutesPerBlock;
  }

  public int getMaxMinutes() {
    return maxMinutes;
  }

  public double getMoneyPerBlock() {
    return moneyPerBlock;
  }

  public boolean isCollect() {
    return collect;
  }

  public int getSpaces() {
    return spaces;
  }
}

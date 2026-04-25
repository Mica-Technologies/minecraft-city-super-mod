package com.micatechnologies.minecraft.csm.codeutils.packets;

import io.netty.buffer.ByteBuf;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;

public class TileEntityVariableSpeedLimitUpdatePacket implements IMessage {

  private BlockPos pos;
  private int speedValue;
  private int flasherMode;
  private int trailerColor;
  private int signAngle;
  private int housingColor;

  public TileEntityVariableSpeedLimitUpdatePacket() {
  }

  public TileEntityVariableSpeedLimitUpdatePacket(BlockPos pos, int speedValue,
      int flasherMode, int trailerColor, int signAngle) {
    this.pos = pos;
    this.speedValue = speedValue;
    this.flasherMode = flasherMode;
    this.trailerColor = trailerColor;
    this.signAngle = signAngle;
    this.housingColor = 0;
  }

  public TileEntityVariableSpeedLimitUpdatePacket(BlockPos pos, int speedValue,
      int flasherMode, int trailerColor, int signAngle, int housingColor) {
    this.pos = pos;
    this.speedValue = speedValue;
    this.flasherMode = flasherMode;
    this.trailerColor = trailerColor;
    this.signAngle = signAngle;
    this.housingColor = housingColor;
  }

  @Override
  public void fromBytes(ByteBuf buf) {
    this.pos = BlockPos.fromLong(buf.readLong());
    this.speedValue = buf.readInt();
    this.flasherMode = buf.readInt();
    this.trailerColor = buf.readInt();
    this.signAngle = buf.readInt();
    if (buf.isReadable()) {
      this.housingColor = buf.readInt();
    }
  }

  @Override
  public void toBytes(ByteBuf buf) {
    buf.writeLong(this.pos.toLong());
    buf.writeInt(this.speedValue);
    buf.writeInt(this.flasherMode);
    buf.writeInt(this.trailerColor);
    buf.writeInt(this.signAngle);
    buf.writeInt(this.housingColor);
  }

  public BlockPos getPos() {
    return pos;
  }

  public int getSpeedValue() {
    return speedValue;
  }

  public int getFlasherMode() {
    return flasherMode;
  }

  public int getTrailerColor() {
    return trailerColor;
  }

  public int getSignAngle() {
    return signAngle;
  }

  public int getHousingColor() {
    return housingColor;
  }
}

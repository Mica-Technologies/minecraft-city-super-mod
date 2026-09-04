package com.micatechnologies.minecraft.csm.hvac;

import io.netty.buffer.ByteBuf;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;

/**
 * Network packet sent from client to server when a player adjusts thermostat settings in the GUI.
 */
public class HvacThermostatConfigPacket implements IMessage {

  private BlockPos pos;
  private int targetTempLow;
  private int targetTempHigh;

  public HvacThermostatConfigPacket() {
  }

  public HvacThermostatConfigPacket(BlockPos pos, int targetTempLow, int targetTempHigh) {
    this.pos = pos;
    this.targetTempLow = targetTempLow;
    this.targetTempHigh = targetTempHigh;
  }

  @Override
  public void fromBytes(ByteBuf buf) {
    this.pos = BlockPos.fromLong(buf.readLong());
    this.targetTempLow = buf.readInt();
    this.targetTempHigh = buf.readInt();
  }

  @Override
  public void toBytes(ByteBuf buf) {
    buf.writeLong(this.pos.toLong());
    buf.writeInt(this.targetTempLow);
    buf.writeInt(this.targetTempHigh);
  }

  public BlockPos getPos() {
    return pos;
  }

  public int getTargetTempLow() {
    return targetTempLow;
  }

  public int getTargetTempHigh() {
    return targetTempHigh;
  }
}

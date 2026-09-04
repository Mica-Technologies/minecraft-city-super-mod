package com.micatechnologies.minecraft.csm.lifesafety;

import io.netty.buffer.ByteBuf;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;

/**
 * Network packet that carries a fire alarm panel configuration action from client to server.
 * Encodes the target panel position, the {@link FireAlarmPanelConfigAction} ordinal, and an
 * optional integer value for the actions that need an argument (the voice evacuation message
 * index, for instance).
 *
 * @author Mica Technologies
 * @since 2026.4
 */

public class FireAlarmPanelConfigPacket implements IMessage {

  private BlockPos pos;
  private int actionOrdinal;
  private int value;

  public FireAlarmPanelConfigPacket() {
  }

  public FireAlarmPanelConfigPacket(BlockPos pos, int actionOrdinal) {
    this(pos, actionOrdinal, 0);
  }

  public FireAlarmPanelConfigPacket(BlockPos pos, int actionOrdinal, int value) {
    this.pos = pos;
    this.actionOrdinal = actionOrdinal;
    this.value = value;
  }

  @Override
  public void fromBytes(ByteBuf buf) {
    this.pos = BlockPos.fromLong(buf.readLong());
    this.actionOrdinal = buf.readInt();
    this.value = buf.readInt();
  }

  @Override
  public void toBytes(ByteBuf buf) {
    buf.writeLong(this.pos.toLong());
    buf.writeInt(this.actionOrdinal);
    buf.writeInt(this.value);
  }

  public BlockPos getPos() {
    return pos;
  }

  public int getActionOrdinal() {
    return actionOrdinal;
  }

  public int getValue() {
    return value;
  }
}

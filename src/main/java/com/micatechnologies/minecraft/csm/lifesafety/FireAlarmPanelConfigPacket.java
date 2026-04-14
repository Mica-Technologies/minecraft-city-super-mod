package com.micatechnologies.minecraft.csm.lifesafety;

import io.netty.buffer.ByteBuf;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;

/**
 * Network packet that carries a fire alarm panel configuration action from client to server.
 * Encodes the target panel position and the {@link FireAlarmPanelConfigAction} ordinal.
 *
 * @author Mica Technologies
 * @since 2026.4
 */

public class FireAlarmPanelConfigPacket implements IMessage {

  private BlockPos pos;
  private int actionOrdinal;

  public FireAlarmPanelConfigPacket() {
  }

  public FireAlarmPanelConfigPacket(BlockPos pos, int actionOrdinal) {
    this.pos = pos;
    this.actionOrdinal = actionOrdinal;
  }

  @Override
  public void fromBytes(ByteBuf buf) {
    this.pos = BlockPos.fromLong(buf.readLong());
    this.actionOrdinal = buf.readInt();
  }

  @Override
  public void toBytes(ByteBuf buf) {
    buf.writeLong(this.pos.toLong());
    buf.writeInt(this.actionOrdinal);
  }

  public BlockPos getPos() {
    return pos;
  }

  public int getActionOrdinal() {
    return actionOrdinal;
  }
}

package com.micatechnologies.minecraft.csm.codeutils.packets;

import io.netty.buffer.ByteBuf;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;

public class TileEntityRedstoneTTSUpdatePacket implements IMessage {

  private BlockPos pos;
  private String ttsString;
  private String ttsVoice;

  public TileEntityRedstoneTTSUpdatePacket() {
    // Required by Forge
  }

  public TileEntityRedstoneTTSUpdatePacket(BlockPos pos, String ttsString, String ttsVoice) {
    this.pos = pos;
    this.ttsString = ttsString;
    this.ttsVoice = ttsVoice;
  }

  @Override
  public void fromBytes(ByteBuf buf) {
    this.pos = BlockPos.fromLong(buf.readLong());
    this.ttsString = ByteBufUtils.readUTF8String(buf);
    this.ttsVoice = ByteBufUtils.readUTF8String(buf);
  }

  @Override
  public void toBytes(ByteBuf buf) {
    buf.writeLong(this.pos.toLong());
    ByteBufUtils.writeUTF8String(buf, this.ttsString);
    ByteBufUtils.writeUTF8String(buf, this.ttsVoice);
  }

  public BlockPos getPos() {
    return pos;
  }

  public String getTtsString() {
    return ttsString;
  }

  public String getTtsVoice() {
    return ttsVoice;
  }
}

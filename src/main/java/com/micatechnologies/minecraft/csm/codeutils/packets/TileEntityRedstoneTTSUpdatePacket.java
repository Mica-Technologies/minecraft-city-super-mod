package com.micatechnologies.minecraft.csm.codeutils.packets;

import io.netty.buffer.ByteBuf;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;

public class TileEntityRedstoneTTSUpdatePacket implements IMessage {

  private BlockPos pos;
  private String ttsString;

  public TileEntityRedstoneTTSUpdatePacket() {
    // Required by Forge
  }

  public TileEntityRedstoneTTSUpdatePacket(BlockPos pos, String ttsString) {
    this.pos = pos;
    this.ttsString = ttsString;
  }

  @Override
  public void fromBytes(ByteBuf buf) {
    // Read the position of the tile entity
    this.pos = BlockPos.fromLong(buf.readLong());

    // Read the TTS string
    this.ttsString = ByteBufUtils.readUTF8String(buf);
  }

  @Override
  public void toBytes(ByteBuf buf) {
    // Write the position of the tile entity
    buf.writeLong(this.pos.toLong());

    // Write the TTS string
    ByteBufUtils.writeUTF8String(buf, this.ttsString);
  }

  public BlockPos getPos() {
    return pos;
  }

  public String getTtsString() {
    return ttsString;
  }
}

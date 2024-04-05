package com.micatechnologies.minecraft.csm.codeutils.packets;

import io.netty.buffer.ByteBuf;
import java.nio.charset.StandardCharsets;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;

public class TileEntityRedstoneTTSInvokePacket implements IMessage {

  private String ttsString;

  public TileEntityRedstoneTTSInvokePacket() {
    // Required by Forge
  }

  public TileEntityRedstoneTTSInvokePacket(String ttsString) {
    this.ttsString = ttsString;
  }

  @Override
  public void fromBytes(ByteBuf buf) {
    int length = buf.readInt();
    byte[] bytes = new byte[length];
    buf.readBytes(bytes);
    this.ttsString = new String(bytes, StandardCharsets.UTF_8);
  }

  @Override
  public void toBytes(ByteBuf buf) {
    byte[] bytes = this.ttsString.getBytes(StandardCharsets.UTF_8);
    buf.writeInt(bytes.length);
    buf.writeBytes(bytes);
  }

  public String getTtsString() {
    return ttsString;
  }
}

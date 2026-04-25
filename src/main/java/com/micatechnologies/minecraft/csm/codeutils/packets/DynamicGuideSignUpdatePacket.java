package com.micatechnologies.minecraft.csm.codeutils.packets;

import io.netty.buffer.ByteBuf;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;

public class DynamicGuideSignUpdatePacket implements IMessage {

  private BlockPos pos;
  private String signDataJson;

  public DynamicGuideSignUpdatePacket() {
  }

  public DynamicGuideSignUpdatePacket(BlockPos pos, String signDataJson) {
    this.pos = pos;
    this.signDataJson = signDataJson;
  }

  @Override
  public void fromBytes(ByteBuf buf) {
    this.pos = BlockPos.fromLong(buf.readLong());
    this.signDataJson = ByteBufUtils.readUTF8String(buf);
  }

  @Override
  public void toBytes(ByteBuf buf) {
    buf.writeLong(this.pos.toLong());
    ByteBufUtils.writeUTF8String(buf, this.signDataJson);
  }

  public BlockPos getPos() {
    return pos;
  }

  public String getSignDataJson() {
    return signDataJson;
  }
}

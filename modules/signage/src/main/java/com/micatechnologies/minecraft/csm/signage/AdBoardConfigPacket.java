package com.micatechnologies.minecraft.csm.signage;

import com.micatechnologies.minecraft.csm.codeutils.CsmPacketUtils;
import io.netty.buffer.ByteBuf;
import java.nio.charset.StandardCharsets;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;

/**
 * A board's screen, sent to the server when Done is pressed: the size wanted and what to show.
 * {@code clicked} is the block the screen was opened from, which the server checks the player
 * can reach, and finds the controller from itself.
 */
public class AdBoardConfigPacket implements IMessage {

  BlockPos clicked;
  int width;
  int height;
  int align;
  String adId;
  int rotation;
  String category;
  int interval;
  int fit;
  int light;

  public AdBoardConfigPacket() {
  }

  public AdBoardConfigPacket(BlockPos clicked, int width, int height, AdBoardAlign align,
      String adId, AdRotation rotation, String category, int interval, AdFit fit,
      AdLight light) {
    this.clicked = clicked;
    this.width = width;
    this.height = height;
    this.align = align.ordinal();
    this.adId = adId;
    this.rotation = rotation.ordinal();
    this.category = category;
    this.interval = interval;
    this.fit = fit.ordinal();
    this.light = light.ordinal();
  }

  @Override
  public void fromBytes(ByteBuf buf) {
    clicked = BlockPos.fromLong(buf.readLong());
    width = buf.readUnsignedByte();
    height = buf.readUnsignedByte();
    align = buf.readUnsignedByte();
    adId = CsmPacketUtils.readBoundedString(buf, TileEntityAdBoard.MAX_ID_LENGTH);
    rotation = buf.readUnsignedByte();
    category = CsmPacketUtils.readBoundedString(buf, TileEntityAdBoard.MAX_ID_LENGTH);
    interval = buf.readUnsignedShort();
    fit = buf.readUnsignedByte();
    light = buf.readUnsignedByte();
  }

  @Override
  public void toBytes(ByteBuf buf) {
    buf.writeLong(clicked.toLong());
    buf.writeByte(width);
    buf.writeByte(height);
    buf.writeByte(align);
    writeString(buf, adId);
    buf.writeByte(rotation);
    writeString(buf, category);
    buf.writeShort(interval);
    buf.writeByte(fit);
    buf.writeByte(light);
  }

  private static void writeString(ByteBuf buf, String s) {
    byte[] bytes = s.getBytes(StandardCharsets.UTF_8);
    buf.writeInt(bytes.length);
    buf.writeBytes(bytes);
  }
}

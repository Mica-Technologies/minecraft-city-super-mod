package com.micatechnologies.minecraft.csm.codeutils.packets;

import io.netty.buffer.ByteBuf;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;

public class TileEntityPortableMessageSignUpdatePacket implements IMessage {

  private BlockPos pos;
  private List<String[]> pages = new ArrayList<>();
  private int flasherMode;
  private int cycleSpeed;
  private int trailerColor;
  private int signAngle;
  private int housingColor;

  public TileEntityPortableMessageSignUpdatePacket() {
  }

  public TileEntityPortableMessageSignUpdatePacket(BlockPos pos, List<String[]> pages,
      int flasherMode, int cycleSpeed, int trailerColor, int signAngle) {
    this.pos = pos;
    this.pages = pages;
    this.flasherMode = flasherMode;
    this.cycleSpeed = cycleSpeed;
    this.trailerColor = trailerColor;
    this.signAngle = signAngle;
    this.housingColor = 0;
  }

  public TileEntityPortableMessageSignUpdatePacket(BlockPos pos, List<String[]> pages,
      int flasherMode, int cycleSpeed, int trailerColor, int signAngle, int housingColor) {
    this.pos = pos;
    this.pages = pages;
    this.flasherMode = flasherMode;
    this.cycleSpeed = cycleSpeed;
    this.trailerColor = trailerColor;
    this.signAngle = signAngle;
    this.housingColor = housingColor;
  }

  @Override
  public void fromBytes(ByteBuf buf) {
    this.pos = BlockPos.fromLong(buf.readLong());
    this.flasherMode = buf.readInt();
    this.cycleSpeed = buf.readInt();
    this.trailerColor = buf.readInt();
    this.signAngle = buf.readInt();
    int count = buf.readInt();
    pages.clear();
    for (int i = 0; i < count; i++) {
      pages.add(new String[]{
          ByteBufUtils.readUTF8String(buf),
          ByteBufUtils.readUTF8String(buf),
          ByteBufUtils.readUTF8String(buf)
      });
    }
    if (buf.isReadable()) {
      this.housingColor = buf.readInt();
    }
  }

  @Override
  public void toBytes(ByteBuf buf) {
    buf.writeLong(this.pos.toLong());
    buf.writeInt(this.flasherMode);
    buf.writeInt(this.cycleSpeed);
    buf.writeInt(this.trailerColor);
    buf.writeInt(this.signAngle);
    buf.writeInt(pages.size());
    for (String[] page : pages) {
      ByteBufUtils.writeUTF8String(buf, page[0]);
      ByteBufUtils.writeUTF8String(buf, page[1]);
      ByteBufUtils.writeUTF8String(buf, page[2]);
    }
    buf.writeInt(this.housingColor);
  }

  public BlockPos getPos() {
    return pos;
  }

  public List<String[]> getPages() {
    return pages;
  }

  public int getFlasherMode() {
    return flasherMode;
  }

  public int getCycleSpeed() {
    return cycleSpeed;
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

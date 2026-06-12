package com.micatechnologies.minecraft.csm.trafficsignals;

import io.netty.buffer.ByteBuf;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;

/**
 * Network packet sent from client to server when a player clicks "Paste Appearance" in the signal
 * head configuration GUI. Carries a bundle of copied appearance settings — body/door/visor colors,
 * visor type, bulb style and mount color (each as its enum's {@code toNBT()} ordinal) plus the
 * bulb-aging and horizontal-flip flags — to apply to the target signal head.
 *
 * @see SignalHeadConfigGui
 * @see TileEntityTrafficSignalHead#applyCopiedAppearance
 */
public class SignalHeadAppearancePacket implements IMessage {

  private BlockPos pos;
  private int bodyColor;
  private int doorColor;
  private int visorColor;
  private int visorType;
  private int bulbStyle;
  private int mountColor;
  private boolean agingEnabled;
  private boolean horizontalFlip;

  public SignalHeadAppearancePacket() {
    // Required by Forge
  }

  public SignalHeadAppearancePacket(BlockPos pos, int bodyColor, int doorColor, int visorColor,
      int visorType, int bulbStyle, int mountColor, boolean agingEnabled, boolean horizontalFlip) {
    this.pos = pos;
    this.bodyColor = bodyColor;
    this.doorColor = doorColor;
    this.visorColor = visorColor;
    this.visorType = visorType;
    this.bulbStyle = bulbStyle;
    this.mountColor = mountColor;
    this.agingEnabled = agingEnabled;
    this.horizontalFlip = horizontalFlip;
  }

  @Override
  public void fromBytes(ByteBuf buf) {
    this.pos = BlockPos.fromLong(buf.readLong());
    this.bodyColor = buf.readInt();
    this.doorColor = buf.readInt();
    this.visorColor = buf.readInt();
    this.visorType = buf.readInt();
    this.bulbStyle = buf.readInt();
    this.mountColor = buf.readInt();
    this.agingEnabled = buf.readBoolean();
    this.horizontalFlip = buf.readBoolean();
  }

  @Override
  public void toBytes(ByteBuf buf) {
    buf.writeLong(this.pos.toLong());
    buf.writeInt(this.bodyColor);
    buf.writeInt(this.doorColor);
    buf.writeInt(this.visorColor);
    buf.writeInt(this.visorType);
    buf.writeInt(this.bulbStyle);
    buf.writeInt(this.mountColor);
    buf.writeBoolean(this.agingEnabled);
    buf.writeBoolean(this.horizontalFlip);
  }

  public BlockPos getPos() {
    return pos;
  }

  public int getBodyColor() {
    return bodyColor;
  }

  public int getDoorColor() {
    return doorColor;
  }

  public int getVisorColor() {
    return visorColor;
  }

  public int getVisorType() {
    return visorType;
  }

  public int getBulbStyle() {
    return bulbStyle;
  }

  public int getMountColor() {
    return mountColor;
  }

  public boolean isAgingEnabled() {
    return agingEnabled;
  }

  public boolean isHorizontalFlip() {
    return horizontalFlip;
  }
}

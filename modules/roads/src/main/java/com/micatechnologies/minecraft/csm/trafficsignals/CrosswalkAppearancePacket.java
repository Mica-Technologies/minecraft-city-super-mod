package com.micatechnologies.minecraft.csm.trafficsignals;

import io.netty.buffer.ByteBuf;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;

/**
 * Network packet sent from client to server when a player clicks "Paste Appearance" in the
 * crosswalk signal configuration GUI. Carries the copied appearance settings — body and visor
 * colors, visor type and display format, each as its enum's {@code toNBT()} ordinal — to apply
 * to the target crosswalk signal.
 *
 * <p>Mount type and body tilt are not carried: they place the signal rather than style it. See
 * {@link TileEntityCrosswalkSignalNew#applyCopiedAppearance}.</p>
 *
 * @see CrosswalkConfigGui
 * @see TileEntityCrosswalkSignalNew#applyCopiedAppearance
 */
public class CrosswalkAppearancePacket implements IMessage {

  private BlockPos pos;
  private int bodyColor;
  private int visorColor;
  private int visorType;
  private int bulbType;

  public CrosswalkAppearancePacket() {
    // Required by Forge
  }

  public CrosswalkAppearancePacket(BlockPos pos, int bodyColor, int visorColor, int visorType,
      int bulbType) {
    this.pos = pos;
    this.bodyColor = bodyColor;
    this.visorColor = visorColor;
    this.visorType = visorType;
    this.bulbType = bulbType;
  }

  @Override
  public void fromBytes(ByteBuf buf) {
    this.pos = BlockPos.fromLong(buf.readLong());
    this.bodyColor = buf.readInt();
    this.visorColor = buf.readInt();
    this.visorType = buf.readInt();
    this.bulbType = buf.readInt();
  }

  @Override
  public void toBytes(ByteBuf buf) {
    buf.writeLong(this.pos.toLong());
    buf.writeInt(this.bodyColor);
    buf.writeInt(this.visorColor);
    buf.writeInt(this.visorType);
    buf.writeInt(this.bulbType);
  }

  public BlockPos getPos() {
    return pos;
  }

  public int getBodyColor() {
    return bodyColor;
  }

  public int getVisorColor() {
    return visorColor;
  }

  public int getVisorType() {
    return visorType;
  }

  public int getBulbType() {
    return bulbType;
  }
}

package com.micatechnologies.minecraft.csm.trafficsigns;

import io.netty.buffer.ByteBuf;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;

/**
 * Client-to-server packet for the dynamic route marker sign's editor: the whole configuration,
 * which is a marker and a route number. One packet for both, because the editor's text field has
 * no natural stepped form and a marker change usually arrives with a number change.
 *
 * @version 1.0
 * @since 2026.9.20
 */
public class RouteMarkerConfigPacket implements IMessage {

  /** Cap on what is read off the wire at all, before the tile entity clamps it properly. */
  private static final int MAX_WIRE_LENGTH = 16;

  private BlockPos pos;
  private int shieldOrdinal;
  private String routeNumber = "";

  public RouteMarkerConfigPacket() {
    // Required by Forge
  }

  public RouteMarkerConfigPacket(BlockPos pos, int shieldOrdinal, String routeNumber) {
    this.pos = pos;
    this.shieldOrdinal = shieldOrdinal;
    this.routeNumber = routeNumber == null ? "" : routeNumber;
  }

  @Override
  public void fromBytes(ByteBuf buf) {
    this.pos = BlockPos.fromLong(buf.readLong());
    this.shieldOrdinal = buf.readInt();
    String read = ByteBufUtils.readUTF8String(buf);
    this.routeNumber = read.length() > MAX_WIRE_LENGTH
        ? read.substring(0, MAX_WIRE_LENGTH) : read;
  }

  @Override
  public void toBytes(ByteBuf buf) {
    buf.writeLong(this.pos.toLong());
    buf.writeInt(this.shieldOrdinal);
    ByteBufUtils.writeUTF8String(buf, this.routeNumber);
  }

  public BlockPos getPos() {
    return pos;
  }

  public int getShieldOrdinal() {
    return shieldOrdinal;
  }

  public String getRouteNumber() {
    return routeNumber;
  }
}

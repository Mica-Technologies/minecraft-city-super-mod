package com.micatechnologies.minecraft.csm.trafficsigns;

import io.netty.buffer.ByteBuf;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;

/**
 * Client-to-server packet for the mile marker's editor: every setting at once. The server clamps
 * each to the plate it lands on ({@link TileEntityMileMarkerSign#configure}).
 *
 * @since 2026.9
 */
public class MileMarkerConfigPacket implements IMessage {

  /** Cap on what is read off the wire at all, before the tile entity clamps it properly. */
  private static final int MAX_WIRE_LENGTH = 8;

  private BlockPos pos;
  private int mile;
  private int tenth;
  private int shieldOrdinal;
  private String route = "";
  private int direction;

  public MileMarkerConfigPacket() {
    // Required by Forge
  }

  public MileMarkerConfigPacket(BlockPos pos, int mile, int tenth, int shieldOrdinal,
      String route, int direction) {
    this.pos = pos;
    this.mile = mile;
    this.tenth = tenth;
    this.shieldOrdinal = shieldOrdinal;
    this.route = route == null ? "" : route;
    this.direction = direction;
  }

  @Override
  public void fromBytes(ByteBuf buf) {
    pos = BlockPos.fromLong(buf.readLong());
    mile = buf.readInt();
    tenth = buf.readInt();
    shieldOrdinal = buf.readInt();
    String read = ByteBufUtils.readUTF8String(buf);
    route = read.length() > MAX_WIRE_LENGTH ? read.substring(0, MAX_WIRE_LENGTH) : read;
    direction = buf.readInt();
  }

  @Override
  public void toBytes(ByteBuf buf) {
    buf.writeLong(pos.toLong());
    buf.writeInt(mile);
    buf.writeInt(tenth);
    buf.writeInt(shieldOrdinal);
    ByteBufUtils.writeUTF8String(buf, route);
    buf.writeInt(direction);
  }

  public BlockPos getPos() {
    return pos;
  }

  public int getMile() {
    return mile;
  }

  public int getTenth() {
    return tenth;
  }

  public int getShieldOrdinal() {
    return shieldOrdinal;
  }

  public String getRoute() {
    return route;
  }

  public int getDirection() {
    return direction;
  }
}

package com.micatechnologies.minecraft.csm.furniture.office;

import io.netty.buffer.ByteBuf;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;

/**
 * The words on a cubicle panel's name plate or sign, sent from its editor. The server cleans
 * every line again ({@link TileEntityCubicleNamePlate#clamp}); what arrives here is only trusted
 * to be short and to be at most {@link TileEntityCubicleNamePlate#MAX_LINES} lines.
 *
 * @since 2026.10
 */
public class CubicleNamePlatePacket implements IMessage {

  /** A line longer than this is cut on arrival, before anything else looks at it. */
  private static final int MAX_WIRE_LENGTH = 64;

  private BlockPos pos;
  private String[] lines;

  public CubicleNamePlatePacket() {
  }

  /**
   * A plate's words.
   *
   * @param pos   the panel
   * @param lines its lines, from the top
   */
  public CubicleNamePlatePacket(BlockPos pos, String[] lines) {
    this.pos = pos;
    int n = Math.min(lines.length, TileEntityCubicleNamePlate.MAX_LINES);
    this.lines = new String[n];
    for (int i = 0; i < n; i++) {
      this.lines[i] = lines[i] == null ? "" : lines[i];
    }
  }

  @Override
  public void fromBytes(ByteBuf buf) {
    pos = BlockPos.fromLong(buf.readLong());
    int n = Math.max(0, Math.min(buf.readByte(), TileEntityCubicleNamePlate.MAX_LINES));
    lines = new String[n];
    for (int i = 0; i < n; i++) {
      String read = ByteBufUtils.readUTF8String(buf);
      lines[i] = read.length() > MAX_WIRE_LENGTH ? read.substring(0, MAX_WIRE_LENGTH) : read;
    }
  }

  @Override
  public void toBytes(ByteBuf buf) {
    buf.writeLong(pos.toLong());
    buf.writeByte(lines.length);
    for (String line : lines) {
      ByteBufUtils.writeUTF8String(buf, line);
    }
  }

  public BlockPos getPos() {
    return pos;
  }

  public String[] getLines() {
    return lines;
  }
}

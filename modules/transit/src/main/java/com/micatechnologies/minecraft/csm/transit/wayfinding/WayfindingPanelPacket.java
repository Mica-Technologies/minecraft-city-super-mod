package com.micatechnologies.minecraft.csm.transit.wayfinding;

import io.netty.buffer.ByteBuf;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;

/**
 * A large hanging sign's whole sign, sent from its editor on every change. The server cleans the
 * lines again and reads the enums by id; what arrives here is only trusted to be short.
 *
 * @since 2026.10
 */
public class WayfindingPanelPacket implements IMessage {

  /** A string longer than this is cut on arrival, before anything else looks at it. */
  private static final int MAX_WIRE_LENGTH = 64;

  private BlockPos pos;
  private String line1;
  private String line2;
  private String pictogram;
  private String arrow;
  private String scheme;
  private boolean doubleSided;

  public WayfindingPanelPacket() {
  }

  /**
   * A sign.
   *
   * @param pos         any cell of the panel (the editor sends its controller's)
   * @param line1       the top line
   * @param line2       the bottom line, empty for one line
   * @param pictogram   the pictogram
   * @param arrow       the arrow
   * @param scheme      the colours
   * @param doubleSided whether the back carries the legend too
   */
  public WayfindingPanelPacket(BlockPos pos, String line1, String line2,
      WayfindingSign.Pictogram pictogram, WayfindingSign.Arrow arrow,
      WayfindingSign.Scheme scheme, boolean doubleSided) {
    this.pos = pos;
    this.line1 = line1 == null ? "" : line1;
    this.line2 = line2 == null ? "" : line2;
    this.pictogram = pictogram.getId();
    this.arrow = arrow.getId();
    this.scheme = scheme.getId();
    this.doubleSided = doubleSided;
  }

  private static String read(ByteBuf buf) {
    String s = ByteBufUtils.readUTF8String(buf);
    return s.length() > MAX_WIRE_LENGTH ? s.substring(0, MAX_WIRE_LENGTH) : s;
  }

  @Override
  public void fromBytes(ByteBuf buf) {
    pos = BlockPos.fromLong(buf.readLong());
    line1 = read(buf);
    line2 = read(buf);
    pictogram = read(buf);
    arrow = read(buf);
    scheme = read(buf);
    doubleSided = buf.readBoolean();
  }

  @Override
  public void toBytes(ByteBuf buf) {
    buf.writeLong(pos.toLong());
    ByteBufUtils.writeUTF8String(buf, line1);
    ByteBufUtils.writeUTF8String(buf, line2);
    ByteBufUtils.writeUTF8String(buf, pictogram);
    ByteBufUtils.writeUTF8String(buf, arrow);
    ByteBufUtils.writeUTF8String(buf, scheme);
    buf.writeBoolean(doubleSided);
  }

  public BlockPos getPos() {
    return pos;
  }

  public String getLine1() {
    return line1;
  }

  public String getLine2() {
    return line2;
  }

  public WayfindingSign.Pictogram getPictogram() {
    return WayfindingSign.Pictogram.byId(pictogram);
  }

  public WayfindingSign.Arrow getArrow() {
    return WayfindingSign.Arrow.byId(arrow);
  }

  public WayfindingSign.Scheme getScheme() {
    return WayfindingSign.Scheme.byId(scheme);
  }

  public boolean isDoubleSided() {
    return doubleSided;
  }
}

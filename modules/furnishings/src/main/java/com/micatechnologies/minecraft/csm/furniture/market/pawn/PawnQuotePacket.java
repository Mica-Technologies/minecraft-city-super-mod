package com.micatechnologies.minecraft.csm.furniture.market.pawn;

import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.inventory.Container;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * What an open pawn screen shows, worked out on the server (only it has the values): a line per
 * slot (what it pays or why it is refused, or a resale price), a header (the total, or the
 * currency), and whether the counter resells and the viewer may manage it. Text travels as JSON
 * text components, so the client shows it in its own language.
 *
 * @since 2026.10
 */
public class PawnQuotePacket implements IMessage {

  /** The counter resells (show the buy-back button). */
  static final int FLAG_RESALE = 1;
  /** The viewer may manage the counter (show the resale switch). */
  static final int FLAG_ADMIN = 2;
  /** Pawn shops are buying and this player may pawn (enable the pawn button). */
  static final int FLAG_CAN_PAWN = 4;

  int windowId;
  int flags;
  String header = "";
  String[] lines = new String[0];
  boolean[] bad = new boolean[0];

  public PawnQuotePacket() {
  }

  PawnQuotePacket(int windowId, int flags, String header, String[] lines, boolean[] bad) {
    this.windowId = windowId;
    this.flags = flags;
    this.header = header;
    this.lines = lines;
    this.bad = bad;
  }

  @Override
  public void fromBytes(ByteBuf buf) {
    windowId = buf.readInt();
    flags = buf.readByte();
    header = ByteBufUtils.readUTF8String(buf);
    int n = Math.min(buf.readByte(), 54);
    lines = new String[n];
    bad = new boolean[n];
    for (int i = 0; i < n; i++) {
      bad[i] = buf.readBoolean();
      lines[i] = ByteBufUtils.readUTF8String(buf);
    }
  }

  @Override
  public void toBytes(ByteBuf buf) {
    buf.writeInt(windowId);
    buf.writeByte(flags);
    ByteBufUtils.writeUTF8String(buf, header);
    buf.writeByte(lines.length);
    for (int i = 0; i < lines.length; i++) {
      buf.writeBoolean(bad[i]);
      ByteBufUtils.writeUTF8String(buf, lines[i] == null ? "" : lines[i]);
    }
  }

  /** Client side: hands it to the open pawn screen's container, if it is still that one. */
  public static class Handler implements IMessageHandler<PawnQuotePacket, IMessage> {

    @Override
    public IMessage onMessage(PawnQuotePacket message, MessageContext ctx) {
      Minecraft.getMinecraft().addScheduledTask(() -> receive(message));
      return null;
    }

    @SideOnly(Side.CLIENT)
    private static void receive(PawnQuotePacket message) {
      Minecraft mc = Minecraft.getMinecraft();
      if (mc.player == null) {
        return;
      }
      Container open = mc.player.openContainer;
      if (open != null && open.windowId == message.windowId && open instanceof IPawnQuoted) {
        ((IPawnQuoted) open).setQuote(message);
      }
    }
  }
}

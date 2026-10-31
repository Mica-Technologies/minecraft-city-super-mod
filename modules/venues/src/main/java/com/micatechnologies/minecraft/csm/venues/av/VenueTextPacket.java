package com.micatechnologies.minecraft.csm.venues.av;

import com.micatechnologies.minecraft.csm.codeutils.CsmPacketUtils;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

/**
 * A ribbon board's or a banner's words and colours from {@link GuiVenueText}, to the server. At
 * most {@link IVenueTextHolder#MAX_LINES} lines of at most 128 bytes and
 * {@link IVenueTextHolder#MAX_CHOICES} choices are read; the holder cleans and clamps them.
 *
 * @since 2026.10
 */
public class VenueTextPacket implements IMessage {

  private static final int MAX_WIRE = 128;

  BlockPos pos;
  String[] lines;
  int[] choices;

  public VenueTextPacket() {
  }

  public VenueTextPacket(BlockPos pos, String[] lines, int[] choices) {
    this.pos = pos;
    this.lines = lines;
    this.choices = choices;
  }

  @Override
  public void fromBytes(ByteBuf buf) {
    pos = BlockPos.fromLong(buf.readLong());
    int n = buf.readUnsignedByte();
    if (n > IVenueTextHolder.MAX_LINES) {
      throw new IndexOutOfBoundsException("too many lines: " + n);
    }
    lines = new String[n];
    for (int i = 0; i < n; i++) {
      lines[i] = CsmPacketUtils.readBoundedString(buf, MAX_WIRE);
    }
    int c = buf.readUnsignedByte();
    if (c > IVenueTextHolder.MAX_CHOICES) {
      throw new IndexOutOfBoundsException("too many choices: " + c);
    }
    choices = new int[c];
    for (int i = 0; i < c; i++) {
      choices[i] = buf.readUnsignedByte();
    }
  }

  @Override
  public void toBytes(ByteBuf buf) {
    buf.writeLong(pos.toLong());
    buf.writeByte(lines.length);
    for (String line : lines) {
      VenuePackets.writeString(buf, line);
    }
    buf.writeByte(choices.length);
    for (int c : choices) {
      buf.writeByte(c);
    }
  }

  /** Applies it on the server's thread, if the player can reach the block. */
  public static class Handler implements IMessageHandler<VenueTextPacket, IMessage> {

    @Override
    public IMessage onMessage(VenueTextPacket m, MessageContext ctx) {
      EntityPlayerMP player = ctx.getServerHandler().player;
      player.server.addScheduledTask(() -> {
        if (!VenuePackets.canReach(player, m.pos)) {
          return;
        }
        TileEntity te = player.world.getTileEntity(m.pos);
        if (te instanceof IVenueTextHolder) {
          ((IVenueTextHolder) te).applyText(m.lines, m.choices);
        }
      });
      return null;
    }
  }
}

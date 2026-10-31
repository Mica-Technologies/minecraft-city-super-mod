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
 * The scoreboard editor, to the server: everything it edits ({@link #APPLY}), or start/stop
 * ({@link #TOGGLE}) or reset ({@link #RESET}) the clock. Strings are bounded on decode; the tile
 * entity cleans, clamps and range-checks everything again.
 *
 * @since 2026.10
 */
public class ScoreboardPacket implements IMessage {

  public static final int APPLY = 0;
  public static final int TOGGLE = 1;
  public static final int RESET = 2;

  /** Longest string on the wire, bytes; the tile entity cuts names to 12 characters. */
  private static final int MAX_WIRE = 64;

  BlockPos pos;
  int action;
  String home = "";
  String away = "";
  int homeScore;
  int awayScore;
  int homeShots;
  int awayShots;
  String period = "";
  int clock = -1;
  int length = -1;
  int mode;
  int homeColour;
  int awayColour;

  public ScoreboardPacket() {
  }

  /** A clock action alone. */
  public static ScoreboardPacket action(BlockPos pos, int action) {
    ScoreboardPacket p = new ScoreboardPacket();
    p.pos = pos;
    p.action = action;
    return p;
  }

  /** Everything the editor holds. A clock or length of -1 leaves it as it is. */
  public static ScoreboardPacket apply(BlockPos pos, String home, String away, int homeScore,
      int awayScore, int homeShots, int awayShots, String period, int clock, int length,
      int mode, int homeColour, int awayColour) {
    ScoreboardPacket p = new ScoreboardPacket();
    p.pos = pos;
    p.action = APPLY;
    p.home = home;
    p.away = away;
    p.homeScore = homeScore;
    p.awayScore = awayScore;
    p.homeShots = homeShots;
    p.awayShots = awayShots;
    p.period = period;
    p.clock = clock;
    p.length = length;
    p.mode = mode;
    p.homeColour = homeColour;
    p.awayColour = awayColour;
    return p;
  }

  @Override
  public void fromBytes(ByteBuf buf) {
    pos = BlockPos.fromLong(buf.readLong());
    action = buf.readUnsignedByte();
    if (action != APPLY) {
      return;
    }
    home = CsmPacketUtils.readBoundedString(buf, MAX_WIRE);
    away = CsmPacketUtils.readBoundedString(buf, MAX_WIRE);
    homeScore = buf.readUnsignedShort();
    awayScore = buf.readUnsignedShort();
    homeShots = buf.readUnsignedShort();
    awayShots = buf.readUnsignedShort();
    period = CsmPacketUtils.readBoundedString(buf, MAX_WIRE);
    clock = buf.readInt();
    length = buf.readInt();
    mode = buf.readUnsignedByte();
    homeColour = buf.readUnsignedByte();
    awayColour = buf.readUnsignedByte();
  }

  @Override
  public void toBytes(ByteBuf buf) {
    buf.writeLong(pos.toLong());
    buf.writeByte(action);
    if (action != APPLY) {
      return;
    }
    VenuePackets.writeString(buf, home);
    VenuePackets.writeString(buf, away);
    buf.writeShort(homeScore);
    buf.writeShort(awayScore);
    buf.writeShort(homeShots);
    buf.writeShort(awayShots);
    VenuePackets.writeString(buf, period);
    buf.writeInt(clock);
    buf.writeInt(length);
    buf.writeByte(mode);
    buf.writeByte(homeColour);
    buf.writeByte(awayColour);
  }

  /** Applies it on the server's thread, if the player can reach the board. */
  public static class Handler implements IMessageHandler<ScoreboardPacket, IMessage> {

    @Override
    public IMessage onMessage(ScoreboardPacket m, MessageContext ctx) {
      EntityPlayerMP player = ctx.getServerHandler().player;
      player.server.addScheduledTask(() -> {
        if (!VenuePackets.canReach(player, m.pos)) {
          return;
        }
        TileEntity te = player.world.getTileEntity(m.pos);
        if (!(te instanceof TileEntityScoreboard)) {
          return;
        }
        TileEntityScoreboard board = (TileEntityScoreboard) te;
        switch (m.action) {
          case APPLY:
            board.apply(m.home, m.away, m.homeScore, m.awayScore, m.homeShots, m.awayShots,
                m.period, Math.min(m.clock, TileEntityScoreboard.MAX_CLOCK),
                Math.min(m.length, TileEntityScoreboard.MAX_CLOCK), m.mode, m.homeColour,
                m.awayColour);
            break;
          case TOGGLE:
            board.toggleClock();
            break;
          case RESET:
            board.resetClock();
            break;
          default:
            break;
        }
      });
      return null;
    }
  }
}

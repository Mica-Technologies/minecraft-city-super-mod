package com.micatechnologies.minecraft.csm.transit.arrival;

import com.micatechnologies.minecraft.csm.codeutils.CsmPacketUtils;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

/**
 * An arrival board's or info totem's data from its editor ({@link GuiBusStopInfo}), to the
 * server. Client to server only.
 *
 * <p>Every string is a bounded read ({@link CsmPacketUtils#readBoundedString}, at most
 * {@link BusStopInfo#MAX_WIRE_BYTES}) and an agency no ordinal names is rejected, both while
 * decoding, which disconnects a lying sender ({@link BusStopInfo#readFrom(ByteBuf)}). The handler
 * runs on the server's thread, checks the player can reach the board first
 * ({@link CsmPacketUtils#canPlayerReach}: loaded chunk, within 8 blocks), and copies the values in
 * through the board's setters, so the caps are applied again on the server. Reach is the only
 * permission, as for the other sign editors.</p>
 *
 * @since 2026.11
 */
public class BusStopInfoPacket implements IMessage {

  private BlockPos pos;
  private BusStopInfo info;

  public BusStopInfoPacket() {
    // Required by Forge
  }

  /**
   * A packet setting a board's data.
   *
   * @param pos  the board's master
   * @param info its new data
   */
  public BusStopInfoPacket(BlockPos pos, BusStopInfo info) {
    this.pos = pos;
    this.info = info;
  }

  @Override
  public void fromBytes(ByteBuf buf) {
    pos = BlockPos.fromLong(buf.readLong());
    info = BusStopInfo.readFrom(buf);
  }

  @Override
  public void toBytes(ByteBuf buf) {
    buf.writeLong(pos.toLong());
    info.writeTo(buf);
  }

  /** The board's master. */
  public BlockPos getPos() {
    return pos;
  }

  /** The data as decoded. */
  public BusStopInfo getInfo() {
    return info;
  }

  /** Applies a packet on the server's thread, if the player can reach the board. */
  public static class Handler implements IMessageHandler<BusStopInfoPacket, IMessage> {

    @Override
    public IMessage onMessage(BusStopInfoPacket message, MessageContext ctx) {
      EntityPlayerMP player = ctx.getServerHandler().player;
      player.server.addScheduledTask(() -> apply(message, player));
      return null;
    }

    private static void apply(BusStopInfoPacket message, EntityPlayerMP player) {
      if (!CsmPacketUtils.canPlayerReach(player, message.pos)) {
        return;
      }
      TileEntity te = player.world.getTileEntity(message.pos);
      if (!(te instanceof TileEntityBusArrivalBoard)) {
        return;
      }
      TileEntityBusArrivalBoard board = (TileEntityBusArrivalBoard) te;
      board.getInfo().copyFrom(message.info);
      board.markChanged();
    }
  }
}

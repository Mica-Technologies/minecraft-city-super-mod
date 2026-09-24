package com.micatechnologies.minecraft.csm.streetscape;

import com.micatechnologies.minecraft.csm.codeutils.CsmPacketUtils;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

/**
 * Pays for time or collects takings on the server, for a player who can reach the meter.
 * Collecting is its owner's or an operator's alone ({@link ParkingPayments#collect} checks).
 *
 * @version 1.0
 */
public class ParkingMeterActionPacketHandler
    implements IMessageHandler<ParkingMeterActionPacket, IMessage> {

  /** A single payment buys at most this many blocks, whatever the packet says. */
  private static final int MAX_BLOCKS = 1440;

  @Override
  public IMessage onMessage(ParkingMeterActionPacket message, MessageContext ctx) {
    EntityPlayerMP player = ctx.getServerHandler().player;
    player.server.addScheduledTask(() -> {
      if (!CsmPacketUtils.canPlayerReach(player, message.getPos())) {
        return;
      }
      TileEntity te = player.world.getTileEntity(message.getPos());
      if (!(te instanceof TileEntityParkingMeter)) {
        return;
      }
      TileEntityParkingMeter meter = (TileEntityParkingMeter) te;
      if (message.getAction() == ParkingMeterActionPacket.PAY) {
        int blocks = Math.max(1, Math.min(MAX_BLOCKS, message.getBlocks()));
        ParkingPayments.pay(player, player.world, message.getPos(), meter, message.getSpace(),
            blocks);
      } else if (message.getAction() == ParkingMeterActionPacket.COLLECT) {
        ParkingPayments.collect(player, player.world, message.getPos(), meter);
      }
    });
    return null;
  }
}

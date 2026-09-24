package com.micatechnologies.minecraft.csm.streetscape;

import com.micatechnologies.minecraft.csm.codeutils.CsmPacketUtils;
import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

/**
 * Applies a meter's settings, if the sender is its owner or an operator and can reach it. Only
 * a pay station's number of spaces can be changed; a meter has as many as it has heads.
 *
 * @version 1.0
 */
public class ParkingMeterSettingsPacketHandler
    implements IMessageHandler<ParkingMeterSettingsPacket, IMessage> {

  @Override
  public IMessage onMessage(ParkingMeterSettingsPacket message, MessageContext ctx) {
    EntityPlayerMP player = ctx.getServerHandler().player;
    player.server.addScheduledTask(() -> {
      if (!CsmPacketUtils.canPlayerReach(player, message.getPos())) {
        return;
      }
      TileEntity te = player.world.getTileEntity(message.getPos());
      Block block = player.world.getBlockState(message.getPos()).getBlock();
      if (!(te instanceof TileEntityParkingMeter) || !(block instanceof BlockParkingMeter)) {
        return;
      }
      TileEntityParkingMeter meter = (TileEntityParkingMeter) te;
      if (!ParkingPayments.mayConfigure(player, meter)) {
        return;
      }
      boolean station = ((BlockParkingMeter) block).getKind() == BlockParkingMeter.Kind.STATION;
      meter.applySettings(message.getEmeraldsPerBlock(), message.getMinutesPerBlock(),
          message.getMaxMinutes(), message.getMoneyPerBlock(), message.isCollect(),
          station ? message.getSpaces() : meter.getSpaces());
      meter.markDirtySync(player.world, message.getPos(), true);
      BlockParkingMeter.refresh(player.world, message.getPos());
    });
    return null;
  }
}

package com.micatechnologies.minecraft.csm.hvac;

import com.micatechnologies.minecraft.csm.codeutils.CsmPacketUtils;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

/**
 * Server-side handler for HVAC thermostat configuration packets. Validates the tile entity and
 * applies the requested target temperature range. Supports both primary and zone thermostats.
 */
public class HvacThermostatConfigPacketHandler implements
    IMessageHandler<HvacThermostatConfigPacket, IMessage> {

  @Override
  public IMessage onMessage(HvacThermostatConfigPacket message, MessageContext ctx) {
    EntityPlayerMP player = ctx.getServerHandler().player;
    player.server.addScheduledTask(() -> {
      if (!CsmPacketUtils.canPlayerReach(player, message.getPos())) {
        return;
      }
      World world = player.world;
      TileEntity te = world.getTileEntity(message.getPos());

      if (te instanceof TileEntityHvacThermostatBase) {
        int low = Math.max(0, Math.min(115, message.getTargetTempLow()));
        int high = Math.max(low + 5, Math.min(120, message.getTargetTempHigh()));
        TileEntityHvacThermostatBase thermostat = (TileEntityHvacThermostatBase) te;
        thermostat.setTargetTempLow(low);
        thermostat.setTargetTempHigh(high);
      }
    });
    return null;
  }
}

package com.micatechnologies.minecraft.csm.hvac;

import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

/**
 * Server-side handler for HVAC thermostat configuration packets. Validates the tile entity and
 * applies the requested target temperature range.
 */
public class HvacThermostatConfigPacketHandler implements
    IMessageHandler<HvacThermostatConfigPacket, IMessage> {

  @Override
  public IMessage onMessage(HvacThermostatConfigPacket message, MessageContext ctx) {
    ctx.getServerHandler().player.server.addScheduledTask(() -> {
      World world = ctx.getServerHandler().player.world;
      TileEntity te = world.getTileEntity(message.getPos());
      if (!(te instanceof TileEntityHvacThermostat)) {
        return;
      }

      TileEntityHvacThermostat thermostat = (TileEntityHvacThermostat) te;
      thermostat.setTargetTempLow(message.getTargetTempLow());
      thermostat.setTargetTempHigh(message.getTargetTempHigh());
      thermostat.syncServerToClient(world);
    });
    return null;
  }
}

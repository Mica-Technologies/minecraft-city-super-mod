package com.micatechnologies.minecraft.csm.trafficsignals;

import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

/**
 * Server-side handler for signal head configuration packets. Validates the tile entity and
 * applies the requested property change by calling the corresponding getNext*() method.
 */
public class SignalHeadConfigPacketHandler implements
    IMessageHandler<SignalHeadConfigPacket, IMessage> {

  @Override
  public IMessage onMessage(SignalHeadConfigPacket message, MessageContext ctx) {
    ctx.getServerHandler().player.server.addScheduledTask(() -> {
      World world = ctx.getServerHandler().player.world;
      TileEntity te = world.getTileEntity(message.getPos());
      if (!(te instanceof TileEntityTrafficSignalHead)) {
        return;
      }

      int ordinal = message.getActionOrdinal();
      if (ordinal < 0 || ordinal >= SignalHeadConfigAction.values().length) {
        return;
      }

      TileEntityTrafficSignalHead signalHead = (TileEntityTrafficSignalHead) te;
      switch (SignalHeadConfigAction.values()[ordinal]) {
        case CYCLE_BODY_COLOR:
          signalHead.getNextBodyPaintColor();
          break;
        case CYCLE_DOOR_COLOR:
          signalHead.getNextDoorPaintColor();
          break;
        case CYCLE_VISOR_COLOR:
          signalHead.getNextVisorPaintColor();
          break;
        case CYCLE_VISOR_TYPE:
          signalHead.getNextVisorType();
          break;
        case CYCLE_BODY_TILT:
          signalHead.getNextBodyTilt();
          break;
        case CYCLE_BULB_STYLE:
          signalHead.getNextBulbStyle();
          break;
        case CYCLE_BULB_TYPE:
          signalHead.getNextBulbType();
          break;
      }
    });
    return null;
  }
}

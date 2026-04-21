package com.micatechnologies.minecraft.csm.trafficsignals;

import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

/**
 * Server-side handler for {@link SignalHeadSectionConfigPacket}. Validates the tile entity and
 * the requested section index, then dispatches to the matching per-section mutator on the
 * signal head tile entity.
 */
public class SignalHeadSectionConfigPacketHandler implements
    IMessageHandler<SignalHeadSectionConfigPacket, IMessage> {

  @Override
  public IMessage onMessage(SignalHeadSectionConfigPacket message, MessageContext ctx) {
    ctx.getServerHandler().player.server.addScheduledTask(() -> {
      World world = ctx.getServerHandler().player.world;
      TileEntity te = world.getTileEntity(message.getPos());
      if (!(te instanceof TileEntityTrafficSignalHead)) {
        return;
      }

      int ordinal = message.getActionOrdinal();
      if (ordinal < 0 || ordinal >= SignalHeadSectionConfigAction.values().length) {
        return;
      }

      TileEntityTrafficSignalHead signalHead = (TileEntityTrafficSignalHead) te;
      int sectionIndex = message.getSectionIndex();
      if (sectionIndex < 0 || sectionIndex >= signalHead.getSectionCount()) {
        return;
      }

      switch (SignalHeadSectionConfigAction.values()[ordinal]) {
        case CYCLE_BODY_COLOR:
          signalHead.getNextBodyPaintColor(sectionIndex);
          break;
        case CYCLE_DOOR_COLOR:
          signalHead.getNextDoorPaintColor(sectionIndex);
          break;
        case CYCLE_VISOR_COLOR:
          signalHead.getNextVisorPaintColor(sectionIndex);
          break;
        case CYCLE_VISOR_TYPE:
          signalHead.getNextVisorType(sectionIndex);
          break;
        case CYCLE_BULB_STYLE:
          signalHead.getNextBulbStyle(sectionIndex);
          break;
        case CYCLE_BULB_TYPE:
          signalHead.getNextBulbType(sectionIndex);
          break;
        case CYCLE_BULB_AGING_STATE:
          signalHead.getNextBulbAgingState(sectionIndex);
          break;
      }
    });
    return null;
  }
}

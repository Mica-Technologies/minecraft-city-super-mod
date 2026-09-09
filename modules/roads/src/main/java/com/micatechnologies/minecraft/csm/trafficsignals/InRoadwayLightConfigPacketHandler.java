package com.micatechnologies.minecraft.csm.trafficsignals;

import com.micatechnologies.minecraft.csm.codeutils.CsmPacketUtils;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

/**
 * Server-side handler for {@link InRoadwayLightConfigPacket}.
 */
public class InRoadwayLightConfigPacketHandler
    implements IMessageHandler<InRoadwayLightConfigPacket, IMessage> {

  @Override
  public IMessage onMessage(InRoadwayLightConfigPacket message, MessageContext ctx) {
    EntityPlayerMP player = ctx.getServerHandler().player;
    player.server.addScheduledTask(() -> {
      if (!CsmPacketUtils.canPlayerReach(player, message.getPos())) {
        return;
      }
      World world = player.world;
      TileEntity te = world.getTileEntity(message.getPos());
      if (!(te instanceof TileEntityInRoadwayWarningLight)) {
        return;
      }
      int ordinal = message.getActionOrdinal();
      if (ordinal < 0 || ordinal >= InRoadwayLightConfigAction.values().length) {
        return;
      }
      TileEntityInRoadwayWarningLight light = (TileEntityInRoadwayWarningLight) te;
      switch (InRoadwayLightConfigAction.values()[ordinal]) {
        case CYCLE_LINK_MODE:
          light.getNextLinkMode();
          break;
        case CYCLE_PATTERN:
          light.getNextPattern();
          break;
        default:
          break;
      }
    });
    return null;
  }
}

package com.micatechnologies.minecraft.csm.trafficsignals;

import com.micatechnologies.minecraft.csm.codeutils.CsmPacketUtils;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

/**
 * Server-side handler for {@link RrfbConfigPacket}. Validates the tile entity and the action
 * ordinal, then applies the change to the beacon.
 */
public class RrfbConfigPacketHandler implements IMessageHandler<RrfbConfigPacket, IMessage> {

  @Override
  public IMessage onMessage(RrfbConfigPacket message, MessageContext ctx) {
    EntityPlayerMP player = ctx.getServerHandler().player;
    player.server.addScheduledTask(() -> {
      if (!CsmPacketUtils.canPlayerReach(player, message.getPos())) {
        return;
      }
      World world = player.world;
      TileEntity te = world.getTileEntity(message.getPos());
      if (!(te instanceof TileEntityRrfb)) {
        return;
      }
      int ordinal = message.getActionOrdinal();
      if (ordinal < 0 || ordinal >= RrfbConfigAction.values().length) {
        return;
      }
      TileEntityRrfb rrfb = (TileEntityRrfb) te;
      switch (RrfbConfigAction.values()[ordinal]) {
        case CYCLE_HOUSING_COLOR:
          rrfb.getNextHousingColor();
          break;
        case TOGGLE_DOUBLE_SIDED:
          rrfb.toggleDoubleSided();
          break;
      }
    });
    return null;
  }
}

package com.micatechnologies.minecraft.csm.trafficsigns;

import com.micatechnologies.minecraft.csm.codeutils.CsmPacketUtils;
import com.micatechnologies.minecraft.csm.trafficaccessories.guidesign.GuideSignShieldType;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

/**
 * Server-side handler for {@link RouteMarkerConfigPacket}. Both values are clamped where they
 * land -- {@code fromOrdinal} falls back to the default marker and the tile entity trims the
 * route number -- so a crafted packet can only ever produce a legal marker.
 *
 * @version 1.0
 * @since 2026.9.20
 */
public class RouteMarkerConfigPacketHandler
    implements IMessageHandler<RouteMarkerConfigPacket, IMessage> {

  @Override
  public IMessage onMessage(RouteMarkerConfigPacket message, MessageContext ctx) {
    EntityPlayerMP player = ctx.getServerHandler().player;
    player.server.addScheduledTask(() -> {
      if (!CsmPacketUtils.canPlayerReach(player, message.getPos())) {
        return;
      }
      TileEntity te = player.world.getTileEntity(message.getPos());
      if (!(te instanceof TileEntityDynamicRouteMarkerSign)) {
        return;
      }
      TileEntityDynamicRouteMarkerSign sign = (TileEntityDynamicRouteMarkerSign) te;
      sign.setShield(GuideSignShieldType.fromOrdinal(message.getShieldOrdinal()));
      sign.setRouteNumber(message.getRouteNumber());
    });
    return null;
  }
}

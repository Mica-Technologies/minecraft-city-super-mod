package com.micatechnologies.minecraft.csm.trafficsignals;

import com.micatechnologies.minecraft.csm.codeutils.CsmPacketUtils;
import com.micatechnologies.minecraft.csm.trafficaccessories.spanwire.TileEntitySignalClusterHanger;
import com.micatechnologies.minecraft.csm.trafficsignals.logic.TrafficSignalBodyColor;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

/**
 * Server-side handler for {@link MountColorPastePacket}: paints a signal cluster hanger, or a
 * signal head's mount, in the pasted colour, and says so on the action bar.
 */
public class MountColorPastePacketHandler implements
    IMessageHandler<MountColorPastePacket, IMessage> {

  @Override
  public IMessage onMessage(MountColorPastePacket message, MessageContext ctx) {
    EntityPlayerMP player = ctx.getServerHandler().player;
    player.server.addScheduledTask(() -> {
      if (!CsmPacketUtils.canPlayerReach(player, message.getPos())) {
        return;
      }
      TileEntity te = player.world.getTileEntity(message.getPos());
      TrafficSignalBodyColor color = TrafficSignalBodyColor.fromNBT(message.getColor());
      final String key;
      if (te instanceof TileEntitySignalClusterHanger) {
        ((TileEntitySignalClusterHanger) te).setColor(color);
        key = "csm.signalappearance.pasted_hanger";
      } else if (te instanceof TileEntityTrafficSignalHead) {
        ((TileEntityTrafficSignalHead) te).setMountColor(color);
        key = "csm.signalappearance.pasted_mount";
      } else {
        return;
      }
      player.sendStatusMessage(new TextComponentTranslation(key, color.getFriendlyName()), true);
    });
    return null;
  }
}

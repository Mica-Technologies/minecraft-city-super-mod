package com.micatechnologies.minecraft.csm.trafficaccessories.spanwire;

import com.micatechnologies.minecraft.csm.codeutils.CsmPacketUtils;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

/**
 * Applies a mount configuration change on the server.
 *
 * <p>Reach-checked and bounds-checked like every other config packet in the mod: the position and
 * the action both arrive from a client and neither can be trusted.
 */
public class SpanWireMountConfigPacketHandler
    implements IMessageHandler<SpanWireMountConfigPacket, IMessage> {

  @Override
  public IMessage onMessage(SpanWireMountConfigPacket message, MessageContext ctx) {
    final EntityPlayerMP player = ctx.getServerHandler().player;
    player.server.addScheduledTask(() -> {
      if (!CsmPacketUtils.canPlayerReach(player, message.getPos())) {
        return;
      }
      final TileEntity te = player.world.getTileEntity(message.getPos());
      if (!(te instanceof TileEntitySpanWireHanger)) {
        return;
      }
      final int ordinal = message.getActionOrdinal();
      if (ordinal < 0 || ordinal >= SpanWireMountConfigAction.values().length) {
        return;
      }

      final TileEntitySpanWireHanger hanger = (TileEntitySpanWireHanger) te;
      switch (SpanWireMountConfigAction.values()[ordinal]) {
        case CYCLE_MOUNT_STYLE:
          hanger.cycleMountStyle();
          break;
        case CYCLE_COIL_STYLE:
          hanger.cycleCoilStyle();
          break;
        case CYCLE_SIGNAL_SIDE:
          hanger.cycleSignalSide();
          break;
        case TOGGLE_BOX_SPAN:
          hanger.toggleBoxSpan();
          break;
        case CYCLE_SAG:
          hanger.cycleSag();
          break;
        case CYCLE_CLUSTER_WIDTH:
          if (hanger instanceof TileEntitySpanWireClusterMount) {
            ((TileEntitySpanWireClusterMount) hanger).cycleClusterWidth();
          }
          break;
        default:
          break;
      }
    });
    return null;
  }
}

package com.micatechnologies.minecraft.csm.trafficsigns;

import com.micatechnologies.minecraft.csm.codeutils.CsmPacketUtils;
import com.micatechnologies.minecraft.csm.trafficaccessories.guidesign.GuideSignShieldType;
import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

/**
 * Server-side handler for {@link MileMarkerConfigPacket}: checks reach, then hands every value to
 * the tile entity, which clamps each to the plate the block is -- so a crafted packet can only
 * ever produce a legend the plate has room for.
 *
 * @since 2026.9
 */
public class MileMarkerConfigPacketHandler
    implements IMessageHandler<MileMarkerConfigPacket, IMessage> {

  @Override
  public IMessage onMessage(MileMarkerConfigPacket message, MessageContext ctx) {
    EntityPlayerMP player = ctx.getServerHandler().player;
    player.server.addScheduledTask(() -> {
      if (!CsmPacketUtils.canPlayerReach(player, message.getPos())) {
        return;
      }
      Block block = player.world.getBlockState(message.getPos()).getBlock();
      TileEntity te = player.world.getTileEntity(message.getPos());
      if (!(block instanceof BlockMileMarkerSign) || !(te instanceof TileEntityMileMarkerSign)) {
        return;
      }
      ((TileEntityMileMarkerSign) te).configure(((BlockMileMarkerSign) block).getLayout(),
          message.getMile(), message.getTenth(),
          GuideSignShieldType.fromOrdinal(message.getShieldOrdinal()), message.getRoute(),
          message.getDirection());
    });
    return null;
  }
}

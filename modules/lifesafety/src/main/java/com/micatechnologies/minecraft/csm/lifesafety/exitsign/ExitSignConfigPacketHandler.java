package com.micatechnologies.minecraft.csm.lifesafety.exitsign;

import com.micatechnologies.minecraft.csm.codeutils.CsmPacketUtils;
import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

/**
 * Applies an {@link ExitSignConfigPacket} on the server. Reach only, as for every other config
 * screen: the sign's screen is open to every player who can right-click it. A setup with an
 * out-of-range option is dropped; one with a value the sign does not offer is clamped to it.
 *
 * @since 2026.9
 */
public class ExitSignConfigPacketHandler implements
    IMessageHandler<ExitSignConfigPacket, IMessage> {

  @Override
  public IMessage onMessage(ExitSignConfigPacket message, MessageContext ctx) {
    EntityPlayerMP player = ctx.getServerHandler().player;
    player.server.addScheduledTask(() -> {
      if (!CsmPacketUtils.canPlayerReach(player, message.getPos())) {
        return;
      }
      World world = player.world;
      Block block = world.getBlockState(message.getPos()).getBlock();
      TileEntity te = world.getTileEntity(message.getPos());
      if (!(block instanceof AbstractBlockExitSign) || !(te instanceof TileEntityExitSign)) {
        return;
      }
      ExitSignConfig config = ExitSignConfig.unpack(message.getPackedConfig());
      if (config == null) {
        return;
      }
      ((TileEntityExitSign) te).setConfig(((AbstractBlockExitSign) block).getSpec().clamp(config));
    });
    return null;
  }
}

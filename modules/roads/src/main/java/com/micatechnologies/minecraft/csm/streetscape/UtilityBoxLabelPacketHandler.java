package com.micatechnologies.minecraft.csm.streetscape;

import com.micatechnologies.minecraft.csm.codeutils.CsmPacketUtils;
import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

/**
 * Applies a utility box's ID number on the server, if the player can reach the box.
 *
 * @version 1.0
 */
public class UtilityBoxLabelPacketHandler
    implements IMessageHandler<UtilityBoxLabelPacket, IMessage> {

  @Override
  public IMessage onMessage(UtilityBoxLabelPacket message, MessageContext ctx) {
    EntityPlayerMP player = ctx.getServerHandler().player;
    player.server.addScheduledTask(() -> {
      if (!CsmPacketUtils.canPlayerReach(player, message.getPos())) {
        return;
      }
      World world = player.world;
      TileEntity te = world.getTileEntity(message.getPos());
      if (!(te instanceof TileEntityUtilityBoxLabel)) {
        return;
      }
      TileEntityUtilityBoxLabel label = (TileEntityUtilityBoxLabel) te;
      label.setLines(message.getLine1(), message.getLine2());
      Block block = world.getBlockState(message.getPos()).getBlock();
      if (message.getPhone() != null && block instanceof BlockUtilityBoxLabelled
          && ((BlockUtilityBoxLabelled) block).getSpec().getPhone() != null) {
        label.setPhone(message.getPhone());
      }
      label.markDirtySync(world, message.getPos(), true);
    });
    return null;
  }
}

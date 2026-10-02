package com.micatechnologies.minecraft.csm.furniture.office;

import com.micatechnologies.minecraft.csm.codeutils.CsmPacketUtils;
import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

/**
 * Applies a cubicle name plate's words on the server, if the player can reach the panel and may
 * change blocks there (not in adventure mode, not inside spawn protection). Only as many lines as
 * the panel's plate carries are kept.
 *
 * @since 2026.10
 */
public class CubicleNamePlatePacketHandler
    implements IMessageHandler<CubicleNamePlatePacket, IMessage> {

  @Override
  public IMessage onMessage(CubicleNamePlatePacket message, MessageContext ctx) {
    EntityPlayerMP player = ctx.getServerHandler().player;
    player.server.addScheduledTask(() -> {
      if (!CsmPacketUtils.canPlayerReach(player, message.getPos())
          || !player.capabilities.allowEdit) {
        return;
      }
      World world = player.world;
      if (!world.isBlockModifiable(player, message.getPos())) {
        return;
      }
      Block block = world.getBlockState(message.getPos()).getBlock();
      TileEntity te = world.getTileEntity(message.getPos());
      if (!(block instanceof BlockCubiclePanelNamed)
          || !(te instanceof TileEntityCubicleNamePlate)) {
        return;
      }
      int count = ((BlockCubiclePanelNamed) block).getStyle().getLineCount();
      TileEntityCubicleNamePlate plate = (TileEntityCubicleNamePlate) te;
      String[] lines = message.getLines();
      for (int i = 0; i < TileEntityCubicleNamePlate.MAX_LINES; i++) {
        plate.setLine(i, i < count && i < lines.length ? lines[i] : "");
      }
      plate.markDirtySync(world, message.getPos(), true);
    });
    return null;
  }
}

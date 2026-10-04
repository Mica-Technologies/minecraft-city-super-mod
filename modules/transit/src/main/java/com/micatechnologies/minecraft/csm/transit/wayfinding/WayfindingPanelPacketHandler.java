package com.micatechnologies.minecraft.csm.transit.wayfinding;

import com.micatechnologies.minecraft.csm.codeutils.CsmPacketUtils;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

/**
 * Applies a large hanging sign's edit on the server, if the player can reach the cell named and
 * may change blocks there (not in adventure mode, not inside spawn protection). The sign is
 * written to every cell of the panel, at most {@value WayfindingSign#MAX_WIDTH} x
 * {@value WayfindingSign#MAX_HEIGHT}, so that whichever cell becomes the controller later already
 * has it ({@link TileEntityWayfindingPanel}).
 *
 * @since 2026.10
 */
public class WayfindingPanelPacketHandler
    implements IMessageHandler<WayfindingPanelPacket, IMessage> {

  @Override
  public IMessage onMessage(WayfindingPanelPacket message, MessageContext ctx) {
    EntityPlayerMP player = ctx.getServerHandler().player;
    player.server.addScheduledTask(() -> {
      BlockPos target = message.getPos();
      if (!CsmPacketUtils.canPlayerReach(player, target) || !player.capabilities.allowEdit) {
        return;
      }
      World world = player.world;
      if (!world.isBlockModifiable(player, target)) {
        return;
      }
      IBlockState state = world.getBlockState(target);
      if (!(state.getBlock() instanceof BlockWayfindingPanel)) {
        return;
      }
      String line1 = WayfindingSign.clamp(message.getLine1());
      String line2 = WayfindingSign.clamp(message.getLine2());
      BlockPos controller = WayfindingSign.controllerOf(world, target, state);
      EnumFacing facing = state.getValue(BlockWayfindingPanel.FACING);
      EnumFacing right = WayfindingSign.leftOf(facing).getOpposite();
      int width = WayfindingSign.widthFrom(world, controller, state.getBlock(), facing);
      int height = WayfindingSign.heightFrom(world, controller, state.getBlock(), facing);
      for (int col = 0; col < width; col++) {
        for (int row = 0; row < height; row++) {
          BlockPos at = controller.offset(right, col).up(row);
          if (!WayfindingSign.joins(world, at, state.getBlock(), facing)) {
            continue;
          }
          TileEntity te = world.getTileEntity(at);
          if (te instanceof TileEntityWayfindingPanel) {
            TileEntityWayfindingPanel cell = (TileEntityWayfindingPanel) te;
            cell.setSign(line1, line2, message.getPictogram(), message.getArrow(),
                message.getScheme(), message.isDoubleSided());
            cell.markDirtySync(world, at, true);
          }
        }
      }
    });
    return null;
  }
}

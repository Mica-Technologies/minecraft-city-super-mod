package com.micatechnologies.minecraft.csm.trafficsignals;

import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

/**
 * Server-side handler for {@link AdvancedSignalControllerConfigPacket}. Applies the requested
 * ADVANCED-program edit to the controller tile entity, which validates, mutates the plan, and
 * re-syncs to clients.
 *
 * @author Mica Technologies
 * @since 2026.6
 */
public class AdvancedSignalControllerConfigPacketHandler implements
    IMessageHandler<AdvancedSignalControllerConfigPacket, IMessage> {

  @Override
  public IMessage onMessage(AdvancedSignalControllerConfigPacket message, MessageContext ctx) {
    ctx.getServerHandler().player.server.addScheduledTask(() -> {
      World world = ctx.getServerHandler().player.world;
      TileEntity te = world.getTileEntity(message.getPos());
      if (!(te instanceof TileEntityTrafficSignalController)) {
        return;
      }
      // Gate advanced programming behind op/creative, consistent with opening the GUI from the
      // block. The config tool path already restricts who can reach this GUI.
      if (!ctx.getServerHandler().player.canUseCommand(2, "")
          && !ctx.getServerHandler().player.isCreative()) {
        return;
      }
      ((TileEntityTrafficSignalController) te).applyAdvancedConfig(
          message.getAction(), message.getIndex(), message.getValue());
    });
    return null;
  }
}

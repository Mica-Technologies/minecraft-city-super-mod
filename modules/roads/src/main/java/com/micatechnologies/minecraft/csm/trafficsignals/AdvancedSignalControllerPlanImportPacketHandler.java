package com.micatechnologies.minecraft.csm.trafficsignals;

import com.micatechnologies.minecraft.csm.codeutils.CsmPacketUtils;
import com.micatechnologies.minecraft.csm.trafficsignals.logic.AdvancedPlanJson;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

/**
 * Server-side handler for {@link AdvancedSignalControllerPlanImportPacket}: loads the pasted plan
 * into the controller, under the same reach and op/creative checks as every other ADVANCED panel
 * edit, and tells the player what happened. A plan that does not parse changes nothing.
 *
 * @author Mica Technologies
 * @since 2026.11
 */
public class AdvancedSignalControllerPlanImportPacketHandler implements
    IMessageHandler<AdvancedSignalControllerPlanImportPacket, IMessage> {

  @Override
  public IMessage onMessage(AdvancedSignalControllerPlanImportPacket message,
      MessageContext ctx) {
    EntityPlayerMP player = ctx.getServerHandler().player;
    player.server.addScheduledTask(() -> {
      if (message.getPos() == null || !CsmPacketUtils.canPlayerReach(player, message.getPos())
          || !CsmPacketUtils.isOperatorOrCreative(player)) {
        return;
      }
      TileEntity te = player.world.getTileEntity(message.getPos());
      if (!(te instanceof TileEntityTrafficSignalController)) {
        return;
      }
      String json = message.inflate();
      if (json == null) {
        tell(player, TextFormatting.RED, "Plan not loaded: the pasted plan was damaged or too "
            + "large.");
        return;
      }
      try {
        tell(player, TextFormatting.GREEN,
            ((TileEntityTrafficSignalController) te).importAdvancedPlanJson(json));
      } catch (AdvancedPlanJson.PlanFormatException e) {
        tell(player, TextFormatting.RED, "Plan not loaded: " + e.getMessage());
      }
    });
    return null;
  }

  private static void tell(EntityPlayerMP player, TextFormatting color, String text) {
    TextComponentString msg = new TextComponentString(text);
    msg.getStyle().setColor(color);
    player.sendMessage(msg);
  }
}

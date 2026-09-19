package com.micatechnologies.minecraft.csm.buildingmaterials;

import com.micatechnologies.minecraft.csm.codeutils.CsmPacketUtils;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

/**
 * Applies a {@link DoorWorkshopPacket} on the server.
 *
 * <p>The conventions every CSM packet follows (see "Network safety" in
 * {@code assets/docs/PERFORMANCE_AND_SECURITY.md}): the work is scheduled onto the server thread,
 * the player must be able to reach the workshop, and must have its screen open -- the container
 * they are looking at must be this very workshop's. Materials come from the slots, never from the
 * packet, a player in creative uses nothing up, and the settings are clamped as they are
 * built.</p>
 *
 * @version 1.0
 * @since 2026.9
 */
public class DoorWorkshopPacketHandler implements IMessageHandler<DoorWorkshopPacket, IMessage> {

  @Override
  public IMessage onMessage(DoorWorkshopPacket message, MessageContext ctx) {
    EntityPlayerMP player = ctx.getServerHandler().player;
    player.server.addScheduledTask(() -> {
      if (!CsmPacketUtils.canPlayerReach(player, message.getPos())
          || !(player.openContainer instanceof ContainerDoorWorkshop)) {
        return;
      }
      TileEntityDoorWorkshop workshop =
          ((ContainerDoorWorkshop) player.openContainer).getWorkshop();
      if (!workshop.getPos().equals(message.getPos())) {
        return;
      }
      boolean creative = player.capabilities.isCreativeMode;
      switch (message.getAction()) {
        case DoorWorkshopPacket.COPY:
          workshop.copy();
          break;
        case DoorWorkshopPacket.LOAD:
          workshop.loadDesign(message.getIndex());
          break;
        case DoorWorkshopPacket.DELETE:
          workshop.deleteDesign(message.getIndex());
          break;
        default:
          workshop.setBehaviour(message.getSettings());
          if (message.getAction() == DoorWorkshopPacket.MAKE) {
            workshop.make(creative);
          } else if (message.getAction() == DoorWorkshopPacket.MAKE_ALL) {
            workshop.makeAll(creative);
          } else if (message.getAction() == DoorWorkshopPacket.APPLY) {
            workshop.apply();
          } else if (message.getAction() == DoorWorkshopPacket.SAVE) {
            workshop.saveDesign(message.getName());
          }
      }
      player.openContainer.detectAndSendChanges();
    });
    return null;
  }
}

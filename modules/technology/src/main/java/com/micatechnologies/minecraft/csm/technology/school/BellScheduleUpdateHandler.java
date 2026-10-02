package com.micatechnologies.minecraft.csm.technology.school;

import com.micatechnologies.minecraft.csm.codeutils.CsmPacketUtils;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

/**
 * Server side of {@link BellScheduleUpdatePacket}: on the main thread, for a player within reach
 * of a loaded controller (the screen is open to every player, as the other config screens are),
 * saves the schedule or rings the tone. A ring now is held to one every two seconds a controller,
 * so a held button cannot flood the players round it with sounds.
 *
 * @since 2026.10
 */
public class BellScheduleUpdateHandler
    implements IMessageHandler<BellScheduleUpdatePacket, IMessage> {

  private static final long RING_COOLDOWN_TICKS = 40L;

  @Override
  public IMessage onMessage(BellScheduleUpdatePacket message, MessageContext ctx) {
    EntityPlayerMP player = ctx.getServerHandler().player;
    player.server.addScheduledTask(() -> {
      if (!CsmPacketUtils.canPlayerReach(player, message.getPos())) {
        return;
      }
      TileEntity te = player.world.getTileEntity(message.getPos());
      if (!(te instanceof TileEntityBellController)) {
        return;
      }
      TileEntityBellController controller = (TileEntityBellController) te;
      if (message.getAction() == BellScheduleUpdatePacket.SAVE) {
        controller.setSchedule(new BellSchedule(message.getEntries()));
      } else if (message.getAction() == BellScheduleUpdatePacket.RING) {
        long now = player.world.getTotalWorldTime();
        if (now - controller.lastRingNow >= RING_COOLDOWN_TICKS || now < controller.lastRingNow) {
          controller.lastRingNow = now;
          BellTone tone = message.getRingTone();
          controller.ring(tone, tone.speaks() ? firstAnnouncement(controller) : "");
        }
      }
    });
    return null;
  }

  /** The text of the controller's first announcement, for an announcement rung now. */
  private static String firstAnnouncement(TileEntityBellController controller) {
    for (BellSchedule.Entry e : controller.getSchedule().getEntries()) {
      if (e.getTone().speaks() && !e.getText().isEmpty()) {
        return e.getText();
      }
    }
    return "";
  }
}

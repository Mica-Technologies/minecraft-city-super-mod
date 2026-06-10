package com.micatechnologies.minecraft.csm.lifesafety;

import com.micatechnologies.minecraft.csm.codeutils.CsmPacketUtils;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

/**
 * Server-side handler for {@link FireAlarmPanelConfigPacket} messages. Dispatches the
 * requested configuration action to the target fire alarm control panel tile entity.
 *
 * @author Mica Technologies
 * @since 2026.4
 */

public class FireAlarmPanelConfigPacketHandler implements
    IMessageHandler<FireAlarmPanelConfigPacket, IMessage> {

  @Override
  public IMessage onMessage(FireAlarmPanelConfigPacket message, MessageContext ctx) {
    EntityPlayerMP player = ctx.getServerHandler().player;
    player.server.addScheduledTask(() -> {
      if (!CsmPacketUtils.canPlayerReach(player, message.getPos())) {
        return;
      }
      World world = player.world;
      TileEntity te = world.getTileEntity(message.getPos());
      if (!(te instanceof TileEntityFireAlarmControlPanel)) {
        return;
      }

      int ordinal = message.getActionOrdinal();
      if (ordinal < 0 || ordinal >= FireAlarmPanelConfigAction.values().length) {
        return;
      }

      TileEntityFireAlarmControlPanel panel = (TileEntityFireAlarmControlPanel) te;
      switch (FireAlarmPanelConfigAction.values()[ordinal]) {
        case CYCLE_VOICE_EVAC_SOUND:
          panel.switchSound();
          break;
        case AUDIBLE_SILENCE:
          if (panel.getAlarmState()) {
            panel.setAudibleSilence(true);
          }
          break;
        case RESET_PANEL:
          panel.setAlarmState(false);
          break;
        case TOGGLE_GLITCHY:
          panel.toggleGlitchy();
          break;
      }
      panel.syncServerToClient(world);
    });
    return null;
  }
}

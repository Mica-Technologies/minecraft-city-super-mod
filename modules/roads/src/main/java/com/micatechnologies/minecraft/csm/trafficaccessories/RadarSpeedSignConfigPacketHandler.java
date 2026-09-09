package com.micatechnologies.minecraft.csm.trafficaccessories;

import com.micatechnologies.minecraft.csm.codeutils.CsmPacketUtils;
import com.micatechnologies.minecraft.csm.trafficsignals.logic.TrafficSignalBodyColor;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

/**
 * Server-side handler for {@link RadarSpeedSignConfigPacket}. Validates the tile entity and the
 * action ordinal, then applies the change. Every value is clamped or wrapped by the tile entity
 * itself, so a crafted packet can only ever produce a legal configuration.
 */
public class RadarSpeedSignConfigPacketHandler
    implements IMessageHandler<RadarSpeedSignConfigPacket, IMessage> {

  @Override
  public IMessage onMessage(RadarSpeedSignConfigPacket message, MessageContext ctx) {
    EntityPlayerMP player = ctx.getServerHandler().player;
    player.server.addScheduledTask(() -> {
      if (!CsmPacketUtils.canPlayerReach(player, message.getPos())) {
        return;
      }
      World world = player.world;
      TileEntity te = world.getTileEntity(message.getPos());
      if (!(te instanceof TileEntityRadarSpeedSign)) {
        return;
      }
      int ordinal = message.getActionOrdinal();
      if (ordinal < 0 || ordinal >= RadarSpeedSignConfigAction.values().length) {
        return;
      }

      TileEntityRadarSpeedSign sign = (TileEntityRadarSpeedSign) te;
      // A step of zero would make a click do nothing, which reads as a broken button; treat it
      // as the forward direction.
      int step = message.getStep() == 0 ? 1 : message.getStep();

      switch (RadarSpeedSignConfigAction.values()[ordinal]) {
        case CYCLE_POSTED_SPEED: {
          int next = sign.getPostedSpeed() + step * TileEntityRadarSpeedSign.SPEED_STEP;
          if (next > TileEntityRadarSpeedSign.MAX_SPEED) {
            next = TileEntityRadarSpeedSign.MIN_SPEED;
          } else if (next < TileEntityRadarSpeedSign.MIN_SPEED) {
            next = TileEntityRadarSpeedSign.MAX_SPEED;
          }
          sign.setPostedSpeed(next);
          break;
        }
        case CYCLE_MULTIPLIER:
          sign.setMultiplierIndex(wrap(sign.getMultiplierIndex() + step,
              TileEntityRadarSpeedSign.MULTIPLIERS.length));
          break;
        case CYCLE_SCALE:
          sign.setScaleIndex(wrap(sign.getScaleIndex() + step,
              TileEntityRadarSpeedSign.SCALES.length));
          break;
        case CYCLE_PANEL_COLOR:
          sign.setPanelColor(TrafficSignalBodyColor.values()[
              wrap(sign.getPanelColor().ordinal() + step,
                  TrafficSignalBodyColor.values().length)]);
          break;
        case TOGGLE_HEADER:
          sign.setShowHeader(!sign.isShowHeader());
          break;
        case CLEAR_ZONE:
          sign.clearZoneCorners();
          break;
        default:
          break;
      }
    });
    return null;
  }

  private static int wrap(int value, int count) {
    return ((value % count) + count) % count;
  }
}

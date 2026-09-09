package com.micatechnologies.minecraft.csm.trafficaccessories;

import com.micatechnologies.minecraft.csm.codeutils.CsmPacketUtils;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

/**
 * Server-side handler for {@link SchoolZoneBeaconConfigPacket}. Validates the tile entity and
 * the action ordinal, then applies the change. Every value is clamped or wrapped by the tile
 * entity itself, so a crafted packet can only ever produce a legal configuration.
 */
public class SchoolZoneBeaconConfigPacketHandler
    implements IMessageHandler<SchoolZoneBeaconConfigPacket, IMessage> {

  @Override
  public IMessage onMessage(SchoolZoneBeaconConfigPacket message, MessageContext ctx) {
    EntityPlayerMP player = ctx.getServerHandler().player;
    player.server.addScheduledTask(() -> {
      if (!CsmPacketUtils.canPlayerReach(player, message.getPos())) {
        return;
      }
      World world = player.world;
      TileEntity te = world.getTileEntity(message.getPos());
      if (!(te instanceof TileEntitySchoolZoneBeacon)) {
        return;
      }
      int ordinal = message.getActionOrdinal();
      if (ordinal < 0 || ordinal >= SchoolZoneBeaconConfigAction.values().length) {
        return;
      }

      TileEntitySchoolZoneBeacon beacon = (TileEntitySchoolZoneBeacon) te;
      // A step of zero would make a click do nothing, which reads as a broken button; treat it
      // as the forward direction.
      int step = message.getStep() == 0 ? 1 : message.getStep();

      switch (SchoolZoneBeaconConfigAction.values()[ordinal]) {
        case CYCLE_SPEED_LIMIT: {
          int next = beacon.getSpeedLimit() + step * 5;
          if (next > TileEntitySchoolZoneBeacon.MAX_SPEED) {
            next = TileEntitySchoolZoneBeacon.MIN_SPEED;
          } else if (next < TileEntitySchoolZoneBeacon.MIN_SPEED) {
            next = TileEntitySchoolZoneBeacon.MAX_SPEED;
          }
          beacon.setSpeedLimit(next);
          break;
        }
        case CYCLE_SCALE:
          beacon.setScaleIndex(wrap(beacon.getScaleIndex() + step,
              TileEntitySchoolZoneBeacon.SCALES.length));
          break;
        case CYCLE_ARRANGEMENT:
          beacon.setArrangement(wrap(beacon.getArrangement() + step,
              TileEntitySchoolZoneBeacon.BEACON_ARRANGEMENT_COUNT));
          break;
        case CYCLE_BEACON_SIZE:
          beacon.setBeaconSize(wrap(beacon.getBeaconSize() + step,
              TileEntitySchoolZoneBeacon.BEACON_SIZE_COUNT));
          break;
        case CYCLE_MODE:
          beacon.setMode(wrap(beacon.getMode() + step,
              TileEntitySchoolZoneBeacon.MODE_COUNT));
          break;
        case ADJUST_SCHEDULE_HOUR: {
          int which = message.getIndex();
          if (which < 0 || which > 3) {
            return;
          }
          beacon.setScheduleHour(which, beacon.getScheduleHour(which) + step);
          break;
        }
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

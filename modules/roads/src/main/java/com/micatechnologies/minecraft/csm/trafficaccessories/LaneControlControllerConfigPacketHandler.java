package com.micatechnologies.minecraft.csm.trafficaccessories;

import com.micatechnologies.minecraft.csm.codeutils.CsmPacketUtils;
import com.micatechnologies.minecraft.csm.trafficsignals.logic.TrafficTimeOfDaySchedule;
import java.util.List;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

/**
 * Server-side handler for {@link LaneControlControllerConfigPacket}. Every index is bounds
 * checked here and every value clamped by the group itself, so a crafted packet can only ever
 * produce a legal configuration.
 */
public class LaneControlControllerConfigPacketHandler
    implements IMessageHandler<LaneControlControllerConfigPacket, IMessage> {

  @Override
  public IMessage onMessage(LaneControlControllerConfigPacket message, MessageContext ctx) {
    EntityPlayerMP player = ctx.getServerHandler().player;
    player.server.addScheduledTask(() -> {
      if (!CsmPacketUtils.canPlayerReach(player, message.getPos())) {
        return;
      }
      World world = player.world;
      TileEntity te = world.getTileEntity(message.getPos());
      if (!(te instanceof TileEntityLaneControlController)) {
        return;
      }
      int ordinal = message.getActionOrdinal();
      if (ordinal < 0 || ordinal >= LaneControlControllerConfigAction.values().length) {
        return;
      }

      TileEntityLaneControlController controller = (TileEntityLaneControlController) te;
      List<LaneControlGroup> groups = controller.getGroups();
      int groupIndex = message.getGroup();
      int slot = message.getSlot();
      boolean validGroup = groupIndex >= 0 && groupIndex < groups.size();

      switch (LaneControlControllerConfigAction.values()[ordinal]) {
        case ADD_GROUP:
          controller.addGroup();
          break;
        case REMOVE_GROUP:
          controller.removeGroup(groupIndex);
          break;
        case CYCLE_ASPECT:
          if (validGroup) {
            groups.get(groupIndex).cycleAspect(slot);
            controller.markConfigChanged();
          }
          break;
        case ADJUST_CLEARANCE:
          if (validGroup) {
            LaneControlGroup group = groups.get(groupIndex);
            // Stepped in whole seconds; slot carries the direction rather than a slot here.
            group.setClearanceTicks(group.getClearanceTicks() + slot * 20L);
            controller.markConfigChanged();
          }
          break;
        case ADJUST_SLOT_START:
          controller.getSchedule().setStartHour(groupIndex,
              controller.getSchedule().getStartHour(groupIndex) + slot);
          controller.markConfigChanged();
          break;
        case CYCLE_MANUAL: {
          // -1 (follow the clock) sits before slot 0, so the cycle runs -1, 0, 1, 2, 3 and back.
          int next = controller.getManualSlot() + 1;
          controller.setManualSlot(
              next >= TrafficTimeOfDaySchedule.SLOT_COUNT
                  ? TileEntityLaneControlController.MANUAL_OFF
                  : next);
          break;
        }
        default:
          break;
      }
    });
    return null;
  }
}

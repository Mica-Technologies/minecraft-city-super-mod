package com.micatechnologies.minecraft.csm.trafficsignals;

import com.micatechnologies.minecraft.csm.codeutils.CsmPacketUtils;
import com.micatechnologies.minecraft.csm.trafficsignals.logic.AbstractBlockTrafficSignalSensorAngled;
import com.micatechnologies.minecraft.csm.trafficsignals.logic.SensorAngle;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

/**
 * Server-side handler for {@link SensorConfigPacket}. Validates the tile entity and applies the
 * requested {@link SensorConfigAction}: clearing a detection zone, or setting the cosmetic aim angle
 * on the sensor block state.
 */
public class SensorConfigPacketHandler implements IMessageHandler<SensorConfigPacket, IMessage> {

  @Override
  public IMessage onMessage(SensorConfigPacket message, MessageContext ctx) {
    EntityPlayerMP player = ctx.getServerHandler().player;
    player.server.addScheduledTask(() -> {
      if (!CsmPacketUtils.canPlayerReach(player, message.getPos())) {
        return;
      }
      World world = player.world;
      TileEntity te = world.getTileEntity(message.getPos());
      if (!(te instanceof TileEntityTrafficSignalSensor)) {
        return;
      }

      int ordinal = message.getActionOrdinal();
      if (ordinal < 0 || ordinal >= SensorConfigAction.values().length) {
        return;
      }

      TileEntityTrafficSignalSensor sensor = (TileEntityTrafficSignalSensor) te;
      switch (SensorConfigAction.values()[ordinal]) {
        case CLEAR_STANDARD:
          sensor.clearStandardZone();
          break;
        case CLEAR_LEFT:
          sensor.clearLeftZone();
          break;
        case CLEAR_RIGHT:
          sensor.clearRightZone();
          break;
        case CLEAR_PROTECTED:
          sensor.clearProtectedZone();
          break;
        case CLEAR_ALL:
          sensor.clearAllZones();
          break;
        case ANGLE_NONE:
          applyAngle(world, message.getPos(), SensorAngle.NONE);
          break;
        case ANGLE_LEFT:
          applyAngle(world, message.getPos(), SensorAngle.LEFT);
          break;
        case ANGLE_RIGHT:
          applyAngle(world, message.getPos(), SensorAngle.RIGHT);
          break;
      }
    });
    return null;
  }

  /** Sets the cosmetic aim angle on an angled sensor, re-rendering and syncing the state. */
  private static void applyAngle(World world, net.minecraft.util.math.BlockPos pos,
      SensorAngle angle) {
    IBlockState state = world.getBlockState(pos);
    if (state.getBlock() instanceof AbstractBlockTrafficSignalSensorAngled) {
      world.setBlockState(pos,
          state.withProperty(AbstractBlockTrafficSignalSensorAngled.ANGLE, angle), 3);
    }
  }
}

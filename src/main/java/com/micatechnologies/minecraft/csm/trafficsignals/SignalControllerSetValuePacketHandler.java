package com.micatechnologies.minecraft.csm.trafficsignals;

import com.micatechnologies.minecraft.csm.trafficsignals.logic.TrafficSignalControllerMode;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

/**
 * Server-side handler for signal controller set-value packets. Validates the tile entity
 * and applies the requested parameter value directly, rather than cycling through presets.
 */
public class SignalControllerSetValuePacketHandler implements
    IMessageHandler<SignalControllerSetValuePacket, IMessage> {

  @Override
  public IMessage onMessage(SignalControllerSetValuePacket message, MessageContext ctx) {
    ctx.getServerHandler().player.server.addScheduledTask(() -> {
      World world = ctx.getServerHandler().player.world;
      TileEntity te = world.getTileEntity(message.getPos());
      if (!(te instanceof TileEntityTrafficSignalController)) {
        return;
      }

      TileEntityTrafficSignalController controller = (TileEntityTrafficSignalController) te;
      String paramKey = message.getParamKey();
      long tickValue = message.getTickValue();

      switch (paramKey) {
        case "mode":
          int modeOrdinal = (int) tickValue;
          TrafficSignalControllerMode[] modes = TrafficSignalControllerMode.values();
          if (modeOrdinal >= 0 && modeOrdinal < modes.length) {
            controller.setModeByOrdinal(modeOrdinal);
          }
          break;
        case "yellowTime":
          if (tickValue >= 20 && tickValue <= 200) {
            controller.setYellowTime(tickValue);
          }
          break;
        case "allRedTime":
          if (tickValue >= 0 && tickValue <= 200) {
            controller.setAllRedTime(tickValue);
          }
          break;
        case "minGreenTime":
          if (tickValue >= 60 && tickValue <= 600) {
            controller.setMinGreenTime(tickValue);
          }
          break;
        case "maxGreenTime":
          if (tickValue >= 400 && tickValue <= 2400) {
            controller.setMaxGreenTime(tickValue);
          }
          break;
        case "minGreenSecondary":
          if (tickValue >= 60 && tickValue <= 600) {
            controller.setMinGreenTimeSecondary(tickValue);
          }
          break;
        case "maxGreenSecondary":
          if (tickValue >= 400 && tickValue <= 2400) {
            controller.setMaxGreenTimeSecondary(tickValue);
          }
          break;
        case "flashDontWalk":
          if (tickValue >= 60 && tickValue <= 800) {
            controller.setFlashDontWalkTime(tickValue);
          }
          break;
        case "pedSignal":
          if (tickValue >= 60 && tickValue <= 600) {
            controller.setDedicatedPedSignalTime(tickValue);
          }
          break;
        case "lpi":
          if (tickValue >= 0 && tickValue <= 200) {
            controller.setLeadPedestrianIntervalTime(tickValue);
          }
          break;
        case "nightlyFlash":
          controller.setNightlyFallbackToFlashMode(tickValue != 0);
          break;
        case "powerLossFlash":
          controller.setPowerLossFallbackToFlashMode(tickValue != 0);
          break;
        case "overlapPed":
          controller.setOverlapPedestrianSignals(tickValue != 0);
          break;
        case "allRedFlash":
          controller.setAllRedFlash(tickValue != 0);
          break;
        case "rampMeterNight":
          controller.setRampMeterNightMode((int) (tickValue % 3));
          break;
        default:
          System.out.println(
              "[CSM] SignalControllerSetValuePacketHandler: unrecognized param key '" + paramKey
                  + "'");
          return;
      }
      // Force sync to client so the GUI sees updated values
      controller.syncServerToClient(world);
    });
    return null;
  }
}

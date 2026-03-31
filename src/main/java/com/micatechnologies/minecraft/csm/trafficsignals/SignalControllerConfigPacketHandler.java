package com.micatechnologies.minecraft.csm.trafficsignals;

import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

/**
 * Server-side handler for signal controller configuration packets. Validates the tile entity
 * and applies the requested property change using the same cycling options as the config tool.
 */
public class SignalControllerConfigPacketHandler implements
    IMessageHandler<SignalControllerConfigPacket, IMessage> {

  // Timing option arrays matching ItemSignalConfigurationTool
  static final long[] LPI_OPTIONS = {0, 20, 40, 60, 100, 140};
  static final long[] YELLOW_TIME_OPTIONS = {40, 60, 80, 100, 120};
  static final long[] ALL_RED_TIME_OPTIONS = {0, 20, 40, 60, 80};
  static final long[] FLASH_DONT_WALK_OPTIONS = {140, 200, 300, 400, 600};
  static final long[] DEDICATED_PED_OPTIONS = {140, 160, 200, 300, 400};
  static final long[] MIN_GREEN_OPTIONS = {100, 140, 200, 300, 400, 500};
  static final long[] MAX_GREEN_OPTIONS = {600, 900, 1000, 1200, 1400, 1600, 1800};

  @Override
  public IMessage onMessage(SignalControllerConfigPacket message, MessageContext ctx) {
    ctx.getServerHandler().player.server.addScheduledTask(() -> {
      World world = ctx.getServerHandler().player.world;
      TileEntity te = world.getTileEntity(message.getPos());
      if (!(te instanceof TileEntityTrafficSignalController)) {
        return;
      }

      int ordinal = message.getActionOrdinal();
      if (ordinal < 0 || ordinal >= SignalControllerConfigAction.values().length) {
        return;
      }

      TileEntityTrafficSignalController controller = (TileEntityTrafficSignalController) te;
      switch (SignalControllerConfigAction.values()[ordinal]) {
        case SWITCH_MODE:
          controller.switchMode();
          break;
        case CYCLE_YELLOW_TIME:
          controller.setYellowTime(cycleValue(controller.getYellowTime(), YELLOW_TIME_OPTIONS));
          break;
        case CYCLE_ALL_RED_TIME:
          controller.setAllRedTime(cycleValue(controller.getAllRedTime(), ALL_RED_TIME_OPTIONS));
          break;
        case CYCLE_FLASH_DONT_WALK_TIME:
          controller.setFlashDontWalkTime(
              cycleValue(controller.getFlashDontWalkTime(), FLASH_DONT_WALK_OPTIONS));
          break;
        case CYCLE_DEDICATED_PED_SIGNAL_TIME:
          controller.setDedicatedPedSignalTime(
              cycleValue(controller.getDedicatedPedSignalTime(), DEDICATED_PED_OPTIONS));
          break;
        case CYCLE_MIN_GREEN_TIME:
          controller.setMinGreenTime(
              cycleValue(controller.getMinGreenTime(), MIN_GREEN_OPTIONS));
          break;
        case CYCLE_MAX_GREEN_TIME:
          controller.setMaxGreenTime(
              cycleValue(controller.getMaxGreenTime(), MAX_GREEN_OPTIONS));
          break;
        case CYCLE_MIN_GREEN_TIME_SECONDARY:
          controller.setMinGreenTimeSecondary(
              cycleValue(controller.getMinGreenTimeSecondary(), MIN_GREEN_OPTIONS));
          break;
        case CYCLE_MAX_GREEN_TIME_SECONDARY:
          controller.setMaxGreenTimeSecondary(
              cycleValue(controller.getMaxGreenTimeSecondary(), MAX_GREEN_OPTIONS));
          break;
        case CYCLE_LPI_TIME:
          controller.setLeadPedestrianIntervalTime(
              cycleValue(controller.getLeadPedestrianIntervalTime(), LPI_OPTIONS));
          break;
        case TOGGLE_NIGHTLY_FLASH:
          controller.setNightlyFallbackToFlashMode(!controller.getNightlyFallbackToFlashMode());
          break;
        case TOGGLE_POWER_LOSS_FLASH:
          controller.setPowerLossFallbackToFlashMode(
              !controller.getPowerLossFallbackToFlashMode());
          break;
        case TOGGLE_OVERLAP_PED_SIGNALS:
          controller.setOverlapPedestrianSignals(!controller.getOverlapPedestrianSignals());
          break;
        case TOGGLE_ALL_RED_FLASH:
          controller.setAllRedFlash(!controller.getAllRedFlash());
          break;
        case CLEAR_FAULTS:
          controller.clearFaultState();
          break;
      }
    });
    return null;
  }

  private static long cycleValue(long current, long[] options) {
    for (int i = 0; i < options.length; i++) {
      if (options[i] == current) {
        return options[(i + 1) % options.length];
      }
    }
    return options[0];
  }
}

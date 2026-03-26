package com.micatechnologies.minecraft.csm.trafficsignals;

import com.micatechnologies.minecraft.csm.codeutils.AbstractItem;
import com.micatechnologies.minecraft.csm.trafficsignals.logic.AbstractBlockControllableSignal;
import com.micatechnologies.minecraft.csm.trafficsignals.logic.AbstractBlockTrafficSignalAPS;
import com.micatechnologies.minecraft.csm.trafficsignals.logic.AbstractBlockTrafficSignalSensor;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.World;

public class ItemSignalConfigurationTool extends AbstractItem {

  private final Map<UUID, ItemSignalConfigurationToolMode> modeMap = new HashMap<>();

  private final Map<UUID, BlockPos> posMap1 = new HashMap<>();
  private final Map<UUID, BlockPos> posMap2 = new HashMap<>();

  @Override
  public EnumActionResult onItemUse(EntityPlayer player,
      World worldIn,
      BlockPos pos,
      EnumHand hand,
      EnumFacing facing,
      float hitX,
      float hitY,
      float hitZ) {
    if (!worldIn.isRemote) {
      IBlockState state = worldIn.getBlockState(pos);
      Block clickedBlock = state.getBlock();
      ItemSignalConfigurationToolMode mode = getMode(player);

      // If sneaking, change mode
      EnumActionResult expectedResult = EnumActionResult.PASS;
      EnumActionResult result = EnumActionResult.FAIL;
      if (player.isSneaking()) {
        // Switch to next mode
        switchToNextMode(player);

        // Notify player
        player.sendMessage(
            new TextComponentString("Mode changed to: " + getMode(player).getFriendlyName()));

        // Mark result as success
        result = EnumActionResult.PASS;
      }
      // If in cycle signal colors mode, cycle signal color if clicked block is a signal
      else if (mode == ItemSignalConfigurationToolMode.CYCLE_SIGNAL_COLORS) {
        // Check if clicked block is a signal
        if (clickedBlock instanceof AbstractBlockControllableSignal) {
          // Cycle signal color
          worldIn.setBlockState(pos, state.cycleProperty(AbstractBlockControllableSignal.COLOR));

          // Notify player
          player.sendMessage(new TextComponentString("Cycling traffic signal color/state!"));

          // Mark result as success
          result = EnumActionResult.PASS;
        }
      }
      // If in aps arrow direction change mode, change APS arrow direction if clicked block is an
      // aps crosswalk button
      else if (mode == ItemSignalConfigurationToolMode.CHANGE_APS_ARROW_DIRECTION) {
        // Check if clicked block is a sensor
        if (clickedBlock instanceof AbstractBlockTrafficSignalAPS) {
          // Get block object
          int arrowDirection =
              AbstractBlockTrafficSignalAPS.incrementArrowDirection(worldIn, pos, state);

          // Get direction string
          String direction = "";
          String oldDirection = "";
          switch (arrowDirection) {
            case 0:
              oldDirection = "NONE";
              direction = "LEFT";
              break;
            case 1:
              oldDirection = "LEFT";
              direction = "RIGHT";
              break;
            case 2:
              oldDirection = "RIGHT";
              direction = "BOTH";
              break;
            case 3:
              oldDirection = "BOTH";
              direction = "NONE";
              break;
          }

          // Notify player
          player.sendMessage(new TextComponentString("Changed APS arrow direction from " +
              oldDirection + " to " + direction + "!"));

          // Mark result as success
          result = EnumActionResult.PASS;
        }
      }
      // If in re-orient sensor mode, re-orient sensor if clicked block is a sensor
      else if (mode == ItemSignalConfigurationToolMode.REORIENT_SENSOR) {
        // Check if clicked block is a sensor
        if (clickedBlock instanceof AbstractBlockTrafficSignalSensor) {
          // Re-orient sensor
          worldIn.setBlockState(pos, state.withProperty(AbstractBlockTrafficSignalSensor.FACING,
              player.getHorizontalFacing().getOpposite()));

          // Notify player
          player.sendMessage(new TextComponentString("Re-oriented traffic signal sensor!"));

          // Mark result as success
          result = EnumActionResult.PASS;
        }
      }
      // If in toggle controller nightly flash setting mode, toggle controller nightly flash
      // setting if clicked
      // block is a controller
      else if (mode == ItemSignalConfigurationToolMode.TOGGLE_CONTROLLER_NIGHTLY_FLASH_SETTING) {
        TileEntity tileEntity = worldIn.getTileEntity(pos);
        if (tileEntity instanceof TileEntityTrafficSignalController) {
          // Invert nighttime flash setting on controller
          TileEntityTrafficSignalController tileEntityTrafficSignalController
              = (TileEntityTrafficSignalController) tileEntity;
          boolean newSetting = !tileEntityTrafficSignalController.getNightlyFallbackToFlashMode();
          tileEntityTrafficSignalController.setNightlyFallbackToFlashMode(newSetting);

          // Notify player
          player.sendMessage(new TextComponentString(
              "Set traffic signal controller nightly flash setting to " + newSetting));

          // Mark result as success
          result = EnumActionResult.PASS;
        }
      }
      // If in toggle controller power loss flash setting mode, toggle controller power loss
      // flash setting if
      // clicked block is a controller
      else if (mode == ItemSignalConfigurationToolMode.TOGGLE_CONTROLLER_POWER_LOSS_FLASH_SETTING) {
        TileEntity tileEntity = worldIn.getTileEntity(pos);
        if (tileEntity instanceof TileEntityTrafficSignalController) {
          // Invert power loss flash setting on controller
          TileEntityTrafficSignalController tileEntityTrafficSignalController
              = (TileEntityTrafficSignalController) tileEntity;
          boolean newSetting = !tileEntityTrafficSignalController.getPowerLossFallbackToFlashMode();
          tileEntityTrafficSignalController.setPowerLossFallbackToFlashMode(newSetting);

          // Notify player
          player.sendMessage(new TextComponentString(
              "Set traffic signal controller power loss flash setting to " + newSetting));

          // Mark result as success
          result = EnumActionResult.PASS;
        }
      }
      // If in toggle controller overlap pedestrian signals setting mode, toggle controller
      // overlap pedestrian
      // signals setting if clicked block is a controller
      else if (mode
          == ItemSignalConfigurationToolMode.TOGGLE_CONTROLLER_OVERLAP_PEDESTRIAN_SIGNALS_SETTING) {
        TileEntity tileEntity = worldIn.getTileEntity(pos);
        if (tileEntity instanceof TileEntityTrafficSignalController) {
          // Invert overlap pedestrian signals setting on controller
          TileEntityTrafficSignalController tileEntityTrafficSignalController
              = (TileEntityTrafficSignalController) tileEntity;
          boolean newSetting = !tileEntityTrafficSignalController.getOverlapPedestrianSignals();
          tileEntityTrafficSignalController.setOverlapPedestrianSignals(newSetting);

          // Notify player
          player.sendMessage(new TextComponentString(
              "Set traffic signal controller overlap pedestrian signals setting to " + newSetting));

          // Mark result as success
          result = EnumActionResult.PASS;
        }
      }
      // If in cycle controller LPI setting mode, cycle through LPI values if clicked block is a
      // controller
      else if (mode == ItemSignalConfigurationToolMode.CYCLE_CONTROLLER_LPI_SETTING) {
        TileEntity tileEntity = worldIn.getTileEntity(pos);
        if (tileEntity instanceof TileEntityTrafficSignalController) {
          TileEntityTrafficSignalController tileEntityTrafficSignalController
              = (TileEntityTrafficSignalController) tileEntity;

          long[] lpiOptions = {0, 20, 40, 60, 100, 140};
          String[] lpiLabels = {"0s (Disabled)", "1s", "2s", "3s", "5s", "7s"};
          int nextIndex = findNextIndex(lpiOptions,
              tileEntityTrafficSignalController.getLeadPedestrianIntervalTime());
          tileEntityTrafficSignalController.setLeadPedestrianIntervalTime(lpiOptions[nextIndex]);
          player.sendMessage(new TextComponentString(
              "Set traffic signal controller lead pedestrian interval to "
                  + lpiLabels[nextIndex]));

          // Mark result as success
          result = EnumActionResult.PASS;
        }
      }
      // If in cycle controller yellow time mode, cycle through yellow time values
      else if (mode == ItemSignalConfigurationToolMode.CYCLE_CONTROLLER_YELLOW_TIME) {
        TileEntity tileEntity = worldIn.getTileEntity(pos);
        if (tileEntity instanceof TileEntityTrafficSignalController) {
          TileEntityTrafficSignalController controller
              = (TileEntityTrafficSignalController) tileEntity;
          long[] options = {40, 60, 80, 100, 120};
          String[] labels = {"2s", "3s", "4s", "5s", "6s"};
          int nextIndex = findNextIndex(options, controller.getYellowTime());
          controller.setYellowTime(options[nextIndex]);
          player.sendMessage(new TextComponentString(
              "Set traffic signal controller yellow time to " + labels[nextIndex]));
          result = EnumActionResult.PASS;
        }
      }
      // If in cycle controller all red time mode, cycle through all red time values
      else if (mode == ItemSignalConfigurationToolMode.CYCLE_CONTROLLER_ALL_RED_TIME) {
        TileEntity tileEntity = worldIn.getTileEntity(pos);
        if (tileEntity instanceof TileEntityTrafficSignalController) {
          TileEntityTrafficSignalController controller
              = (TileEntityTrafficSignalController) tileEntity;
          long[] options = {0, 20, 40, 60, 80};
          String[] labels = {"0s", "1s", "2s", "3s", "4s"};
          int nextIndex = findNextIndex(options, controller.getAllRedTime());
          controller.setAllRedTime(options[nextIndex]);
          player.sendMessage(new TextComponentString(
              "Set traffic signal controller all red time to " + labels[nextIndex]));
          result = EnumActionResult.PASS;
        }
      }
      // If in cycle controller flash don't walk time mode, cycle through ped clearance time values
      else if (mode == ItemSignalConfigurationToolMode.CYCLE_CONTROLLER_FLASH_DONT_WALK_TIME) {
        TileEntity tileEntity = worldIn.getTileEntity(pos);
        if (tileEntity instanceof TileEntityTrafficSignalController) {
          TileEntityTrafficSignalController controller
              = (TileEntityTrafficSignalController) tileEntity;
          long[] options = {140, 200, 300, 400, 600};
          String[] labels = {"7s", "10s", "15s", "20s", "30s"};
          int nextIndex = findNextIndex(options, controller.getFlashDontWalkTime());
          controller.setFlashDontWalkTime(options[nextIndex]);
          player.sendMessage(new TextComponentString(
              "Set traffic signal controller ped clearance time to " + labels[nextIndex]));
          result = EnumActionResult.PASS;
        }
      }
      // If in cycle controller dedicated ped signal time mode, cycle through ped signal time values
      else if (mode == ItemSignalConfigurationToolMode.CYCLE_CONTROLLER_DEDICATED_PED_SIGNAL_TIME) {
        TileEntity tileEntity = worldIn.getTileEntity(pos);
        if (tileEntity instanceof TileEntityTrafficSignalController) {
          TileEntityTrafficSignalController controller
              = (TileEntityTrafficSignalController) tileEntity;
          long[] options = {140, 160, 200, 300, 400};
          String[] labels = {"7s", "8s", "10s", "15s", "20s"};
          int nextIndex = findNextIndex(options, controller.getDedicatedPedSignalTime());
          controller.setDedicatedPedSignalTime(options[nextIndex]);
          player.sendMessage(new TextComponentString(
              "Set traffic signal controller ped signal time to " + labels[nextIndex]));
          result = EnumActionResult.PASS;
        }
      }
      // If in cycle controller min green time mode, cycle through min green time values
      else if (mode == ItemSignalConfigurationToolMode.CYCLE_CONTROLLER_MIN_GREEN_TIME) {
        TileEntity tileEntity = worldIn.getTileEntity(pos);
        if (tileEntity instanceof TileEntityTrafficSignalController) {
          TileEntityTrafficSignalController controller
              = (TileEntityTrafficSignalController) tileEntity;
          long[] options = {100, 140, 200, 300, 400, 500};
          String[] labels = {"5s", "7s", "10s", "15s", "20s", "25s"};
          int nextIndex = findNextIndex(options, controller.getMinGreenTime());
          controller.setMinGreenTime(options[nextIndex]);
          player.sendMessage(new TextComponentString(
              "Set traffic signal controller min green time to " + labels[nextIndex]));
          result = EnumActionResult.PASS;
        }
      }
      // If in cycle controller max green time mode, cycle through max green time values
      else if (mode == ItemSignalConfigurationToolMode.CYCLE_CONTROLLER_MAX_GREEN_TIME) {
        TileEntity tileEntity = worldIn.getTileEntity(pos);
        if (tileEntity instanceof TileEntityTrafficSignalController) {
          TileEntityTrafficSignalController controller
              = (TileEntityTrafficSignalController) tileEntity;
          long[] options = {600, 900, 1000, 1200, 1400, 1600, 1800};
          String[] labels = {"30s", "45s", "50s", "60s", "70s", "80s", "90s"};
          int nextIndex = findNextIndex(options, controller.getMaxGreenTime());
          controller.setMaxGreenTime(options[nextIndex]);
          player.sendMessage(new TextComponentString(
              "Set traffic signal controller max green time to " + labels[nextIndex]));
          result = EnumActionResult.PASS;
        }
      }
      // If in cycle controller min green time secondary mode
      else if (mode == ItemSignalConfigurationToolMode.CYCLE_CONTROLLER_MIN_GREEN_TIME_SECONDARY) {
        TileEntity tileEntity = worldIn.getTileEntity(pos);
        if (tileEntity instanceof TileEntityTrafficSignalController) {
          TileEntityTrafficSignalController controller
              = (TileEntityTrafficSignalController) tileEntity;
          long[] options = {100, 140, 200, 300, 400, 500};
          String[] labels = {"5s", "7s", "10s", "15s", "20s", "25s"};
          int nextIndex = findNextIndex(options, controller.getMinGreenTimeSecondary());
          controller.setMinGreenTimeSecondary(options[nextIndex]);
          player.sendMessage(new TextComponentString(
              "Set traffic signal controller min green time (secondary) to "
                  + labels[nextIndex]));
          result = EnumActionResult.PASS;
        }
      }
      // If in cycle controller max green time secondary mode
      else if (mode == ItemSignalConfigurationToolMode.CYCLE_CONTROLLER_MAX_GREEN_TIME_SECONDARY) {
        TileEntity tileEntity = worldIn.getTileEntity(pos);
        if (tileEntity instanceof TileEntityTrafficSignalController) {
          TileEntityTrafficSignalController controller
              = (TileEntityTrafficSignalController) tileEntity;
          long[] options = {600, 900, 1000, 1200, 1400, 1600, 1800};
          String[] labels = {"30s", "45s", "50s", "60s", "70s", "80s", "90s"};
          int nextIndex = findNextIndex(options, controller.getMaxGreenTimeSecondary());
          controller.setMaxGreenTimeSecondary(options[nextIndex]);
          player.sendMessage(new TextComponentString(
              "Set traffic signal controller max green time (secondary) to "
                  + labels[nextIndex]));
          result = EnumActionResult.PASS;
        }
      }
      // If in toggle controller all red flash mode, toggle all red flash setting
      else if (mode == ItemSignalConfigurationToolMode.TOGGLE_CONTROLLER_ALL_RED_FLASH) {
        TileEntity tileEntity = worldIn.getTileEntity(pos);
        if (tileEntity instanceof TileEntityTrafficSignalController) {
          TileEntityTrafficSignalController controller
              = (TileEntityTrafficSignalController) tileEntity;
          boolean newSetting = !controller.getAllRedFlash();
          controller.setAllRedFlash(newSetting);
          player.sendMessage(new TextComponentString(
              "Set traffic signal controller all red flash to " + newSetting));
          result = EnumActionResult.PASS;
        }
      }
      // If in clear controller faults mode, clear controller faults if clicked block is a
      // controller
      else if (mode == ItemSignalConfigurationToolMode.CLEAR_CONTROLLER_FAULTS) {
        TileEntity tileEntity = worldIn.getTileEntity(pos);
        if (tileEntity instanceof TileEntityTrafficSignalController) {
          // Clear fault state from controller
          TileEntityTrafficSignalController tileEntityTrafficSignalController
              = (TileEntityTrafficSignalController) tileEntity;
          boolean isInFaultState = tileEntityTrafficSignalController.isInFaultState();
          String faultMessage = "";
          if (isInFaultState) {
            faultMessage = tileEntityTrafficSignalController.getCurrentFaultMessage();
            tileEntityTrafficSignalController.clearFaultState();
          }

          // Notify player
          // Controller in fault state
          if (isInFaultState) {
            player.sendMessage(new TextComponentString("Controller fault state has been reset."));
            player.sendMessage(
                new TextComponentString("Cleared controller fault message: " + faultMessage));
          }
          // Controller in non-fault state
          else {
            player.sendMessage(
                new TextComponentString("Controller is not in fault state. Unable to reset!"));
          }

          // Mark result as success
          result = EnumActionResult.PASS;
        }
      } else if (mode == ItemSignalConfigurationToolMode.CREATE_SIGNAL_OVERLAPS) {
        BlockPos controllerPos = posMap1.getOrDefault(player.getUniqueID(), null);
        BlockPos overlapSourcePos = posMap2.getOrDefault(player.getUniqueID(), null);

        // Check if clicked block is a signal controller
        if (clickedBlock instanceof BlockTrafficSignalController) {
          // Store controller position
          posMap1.put(player.getUniqueID(), pos);

          // Notify player
          player.sendMessage(new TextComponentString("Controller position set to " +
              pos +
              ". Click the overlap source signal " +
              "followed by the overlap target signal."));

          // Mark result as success
          result = EnumActionResult.PASS;
        }
        // Check if clicked block is a signal
        else if (clickedBlock instanceof AbstractBlockControllableSignal) {
          // Check if controller position is set
          if (controllerPos == null) {
            // Notify player
            player.sendMessage(new TextComponentString("Controller position not set. Click a " +
                "controller to begin configuring an " +
                "overlap."));

            // Mark result as success
            result = EnumActionResult.PASS;
          }
          // Check if overlap source position is set
          else if (overlapSourcePos == null) {
            // Store overlap source position
            posMap2.put(player.getUniqueID(), pos);

            // Notify player
            player.sendMessage(new TextComponentString("Overlap source signal position set to " +
                pos +
                ". Click the overlap target signal."));

            // Mark result as success
            result = EnumActionResult.PASS;
          }
          // Check if overlap source position and target position are the same
          else if (overlapSourcePos.equals(pos)) {
            // Notify player
            player.sendMessage(new TextComponentString("Overlap target signal position cannot " +
                "be the same as source signal " +
                "position!"));
          }
          // Otherwise, create overlap on selected controller using source and target signal
          // positions
          else {
            // Get controller tile entity
            TileEntity tileEntity = worldIn.getTileEntity(controllerPos);

            // Double-check that tile entity is controller
            if (tileEntity instanceof TileEntityTrafficSignalController) {
              TileEntityTrafficSignalController tileEntityTrafficSignalController
                  = (TileEntityTrafficSignalController) tileEntity;

              // Create overlap
              boolean created =
                  tileEntityTrafficSignalController.createOverlap(overlapSourcePos, pos);

              // Notify player whether overlap was created and mark result as success if so
              if (created) {
                // Reset pos2
                posMap2.remove(player.getUniqueID());

                // Notify player
                player.sendMessage(new TextComponentString("Overlap created on controller at " +
                    controllerPos +
                    " from source signal at " +
                    overlapSourcePos +
                    " to target signal at " +
                    pos));

                // Mark result as success
                result = EnumActionResult.PASS;
              } else {
                // Notify player
                player.sendMessage(new TextComponentString("Unable to create overlap on " +
                    "controller at " +
                    controllerPos +
                    " from source signal at " +
                    overlapSourcePos +
                    " to target signal at " +
                    pos +
                    ". Overlap already exists!"));
              }

            }
          }
        }
        // If clicked block is not a signal or controller, reset pos1 and pos2
        else {
          // Reset pos2
          posMap2.remove(player.getUniqueID());

          // Notify player
          player.sendMessage(new TextComponentString(
              "Overlap source signal position " + "has been reset. (Clicked non-signal "
                  + "block)"));

          // Mark result as success
          result = EnumActionResult.PASS;
        }
      } else {
        player.sendMessage(
            new TextComponentString("Not sure how we got here, but something has clearly" +
                " gone very wrong. Please report this bug!"));
        expectedResult = EnumActionResult.FAIL;
      }

      // Check if result is expected, and if not, notify player
      if (result != expectedResult) {
        player.sendMessage(new TextComponentString("Something went wrong. Please try again!"));
      }

      return result;
    } else {
      return EnumActionResult.SUCCESS;
    }
  }

  public ItemSignalConfigurationToolMode getMode(EntityPlayer player) {
    return modeMap.getOrDefault(player.getUniqueID(),
        ItemSignalConfigurationToolMode.CYCLE_SIGNAL_COLORS);
  }

  public String switchToNextMode(EntityPlayer player) {
    ItemSignalConfigurationToolMode newMode = modeMap.getOrDefault(player.getUniqueID(),
        ItemSignalConfigurationToolMode.CYCLE_SIGNAL_COLORS);
    int newModeOrdinal = newMode.ordinal() + 1;
    newModeOrdinal %= ItemSignalConfigurationToolMode.values().length;
    newMode = ItemSignalConfigurationToolMode.values()[newModeOrdinal];
    modeMap.put(player.getUniqueID(), newMode);
    return newMode.getFriendlyName();
  }

  /**
   * Finds the current value in the options array and returns the next index (wrapping around). If
   * the current value is not found, returns 0.
   */
  private static int findNextIndex(long[] options, long currentValue) {
    int currentIndex = 0;
    for (int i = 0; i < options.length; i++) {
      if (options[i] == currentValue) {
        currentIndex = i;
        break;
      }
    }
    return (currentIndex + 1) % options.length;
  }

  @Override
  public void addInformation(ItemStack itemstack, World world, List<String> list,
      ITooltipFlag flag) {
    super.addInformation(itemstack, world, list, flag);
    list.add(
        "Configuration tool for changing signal colors, resetting controller faults, re-orienting "
            +
            "sensors, and more...");
  }

  /**
   * Retrieves the registry name of the item.
   *
   * @return The registry name of the item.
   *
   * @since 1.0
   */
  @Override
  public String getItemRegistryName() {
    return "signalconfigurationtool";

  }
}

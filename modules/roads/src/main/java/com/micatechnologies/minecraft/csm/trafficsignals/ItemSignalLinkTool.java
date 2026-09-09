package com.micatechnologies.minecraft.csm.trafficsignals;

import com.micatechnologies.minecraft.csm.codeutils.AbstractItem;
import com.micatechnologies.minecraft.csm.trafficaccessories.BlockLaneControlController;
import com.micatechnologies.minecraft.csm.trafficaccessories.BlockLaneControlSignal;
import com.micatechnologies.minecraft.csm.trafficaccessories.TileEntityLaneControlController;
import com.micatechnologies.minecraft.csm.trafficaccessories.AbstractBlockSignalBackplate;
import com.micatechnologies.minecraft.csm.trafficsignals.logic.AbstractBlockControllableCrosswalkAccessory;
import com.micatechnologies.minecraft.csm.trafficsignals.logic.AbstractBlockControllableSignal;
import com.micatechnologies.minecraft.csm.trafficsignals.logic.ITrafficSignalSensor;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
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

/**
 * Signal link tool item. Allows players to link traffic signal components (signal heads,
 * crosswalk signals, sensors, APS buttons) to a specific circuit on a traffic signal controller.
 *
 * @author Mica Technologies
 * @since 1.0
 */
public class ItemSignalLinkTool extends AbstractItem {

  private final Map<UUID, BlockPos> signalControllerPosMap = new HashMap<>();
  /** Lane control controllers are a separate subject, so the two selections coexist. */
  private final Map<UUID, BlockPos> laneControllerPosMap = new HashMap<>();
  private final Map<UUID, Integer> laneGroupIndexMap = new HashMap<>();
  private final Map<UUID, Integer> circuitLinkIndexMap = new HashMap<>();

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

      // Resolve backplate clicks to the signal behind them
      if (clickedBlock instanceof AbstractBlockSignalBackplate) {
        BlockPos signalPos = AbstractBlockSignalBackplate.findSignalBehind(worldIn, pos);
        if (signalPos != null) {
          pos = signalPos;
          state = worldIn.getBlockState(pos);
        } else {
          player.sendMessage(
              new TextComponentString("Backplate not connected to a configurable signal."));
          return EnumActionResult.FAIL;
        }
      }

      // Lane control is its own subject and its own targets, so it is resolved before the
      // signal controller flow rather than threaded through it.
      if (state.getBlock() instanceof BlockLaneControlController) {
        int group = player.isSneaking()
            ? laneGroupIndexMap.getOrDefault(player.getUniqueID(), 0) + 1
            : 0;
        TileEntity laneTe = worldIn.getTileEntity(pos);
        int groupCount = laneTe instanceof TileEntityLaneControlController
            ? ((TileEntityLaneControlController) laneTe).getGroups().size()
            : 0;
        if (groupCount == 0) {
          player.sendMessage(new TextComponentString(
              "That lane control controller has no groups yet. Open it and add one first."));
          return EnumActionResult.SUCCESS;
        }
        if (group >= groupCount) {
          group = 0;
        }
        laneControllerPosMap.put(player.getUniqueID(), pos);
        laneGroupIndexMap.put(player.getUniqueID(), group);
        player.sendMessage(new TextComponentString(
            "Linking to lane control group " + (group + 1) + " of " + groupCount
                + " at (" + pos.getX() + "," + pos.getY() + "," + pos.getZ() + ")."
                + " Sneak-click the controller to change group."));
        return EnumActionResult.SUCCESS;
      }
      if (state.getBlock() instanceof BlockLaneControlSignal) {
        BlockPos lanePos = laneControllerPosMap.getOrDefault(player.getUniqueID(), null);
        if (lanePos == null) {
          player.sendMessage(
              new TextComponentString("No lane control controller has been selected."));
          return EnumActionResult.SUCCESS;
        }
        TileEntity laneTe = worldIn.getTileEntity(lanePos);
        if (!(laneTe instanceof TileEntityLaneControlController)) {
          player.sendMessage(new TextComponentString(
              "Unable to link! Lost connection to the selected lane control controller."));
          return EnumActionResult.SUCCESS;
        }
        TileEntityLaneControlController controller = (TileEntityLaneControlController) laneTe;
        int group = laneGroupIndexMap.getOrDefault(player.getUniqueID(), 0);
        if (player.isSneaking()) {
          boolean removed = controller.unlinkSignal(pos);
          player.sendMessage(new TextComponentString(removed
              ? "Lane control signal unlinked."
              : "That lane control signal was not linked to this controller."));
        } else {
          boolean linked = controller.linkSignal(group, pos);
          player.sendMessage(new TextComponentString(linked
              ? "Lane control signal linked to group " + (group + 1) + "."
              : "That lane control signal is already in group " + (group + 1) + "."));
        }
        return EnumActionResult.SUCCESS;
      }

      final BlockPos signalControllerPos =
          signalControllerPosMap.getOrDefault(player.getUniqueID(), null);
      final int circuitLinkIndex = circuitLinkIndexMap.getOrDefault(player.getUniqueID(), 1);
      if (state.getBlock() instanceof BlockTrafficSignalController) {
        signalControllerPosMap.put(player.getUniqueID(), pos);
        circuitLinkIndexMap.put(player.getUniqueID(), 1);
        player.sendMessage(new TextComponentString("Linking to signal controller at " +
            "(" +
            pos.getX() +
            "," +
            pos.getY() +
            "," +
            pos.getZ() +
            ")"));

        return EnumActionResult.SUCCESS;
      } else if (signalControllerPos == null &&
          (state.getBlock() instanceof AbstractBlockControllableSignal ||
              state.getBlock() instanceof ITrafficSignalSensor ||
              state.getBlock() instanceof BlockOverheightDetectionSensor)) {

        player.sendMessage(new TextComponentString("No signal controller has been selected."));

        return EnumActionResult.SUCCESS;
      } else if (signalControllerPos != null &&
          (state.getBlock() instanceof ITrafficSignalSensor ||
              state.getBlock() instanceof BlockOverheightDetectionSensor) &&
          !player.isSneaking()) {

        TileEntity tileEntity = worldIn.getTileEntity(signalControllerPos);
        if (tileEntity instanceof TileEntityTrafficSignalController) {
          TileEntityTrafficSignalController tileEntityTrafficSignalController
              = (TileEntityTrafficSignalController) tileEntity;
          boolean linked = tileEntityTrafficSignalController.linkDevice(pos,
              AbstractBlockControllableSignal.SIGNAL_SIDE.NA_SENSOR,
              circuitLinkIndex);

          if (linked) {
            player.sendMessage(new TextComponentString("Sensor connected to circuit " +
                circuitLinkIndex +
                " of signal controller at " +
                "(" +
                signalControllerPos.getX() +
                "," +
                signalControllerPos.getY() +
                "," +
                signalControllerPos.getZ() +
                ")"));
          }
        } else {
          player.sendMessage(new TextComponentString(
              "Unable to link sensor! Lost connection to previously connected controller."));

        }

        return EnumActionResult.SUCCESS;
      } else if (signalControllerPos != null &&
          (state.getBlock() instanceof ITrafficSignalSensor ||
              state.getBlock() instanceof BlockOverheightDetectionSensor) &&
          player.isSneaking()) {

        TileEntity tileEntity = worldIn.getTileEntity(signalControllerPos);
        if (tileEntity instanceof TileEntityTrafficSignalController) {
          TileEntityTrafficSignalController tileEntityTrafficSignalController
              = (TileEntityTrafficSignalController) tileEntity;
          tileEntityTrafficSignalController.unlinkDevice(pos);

          player.sendMessage(new TextComponentString("Sensor unlinked from signal controller at " +
              "(" +
              signalControllerPos.getX() +
              "," +
              signalControllerPos.getY() +
              "," +
              signalControllerPos.getZ() +
              ")"));

        } else {
          player.sendMessage(new TextComponentString(
              "Unable to link sensor! Lost connection to previously connected controller."));

        }

        return EnumActionResult.SUCCESS;
      } else if (signalControllerPos != null &&
          state.getBlock() instanceof AbstractBlockControllableSignal &&
          !player.isSneaking()) {
        AbstractBlockControllableSignal signalBlock =
            (AbstractBlockControllableSignal) state.getBlock();

        TileEntity tileEntity = worldIn.getTileEntity(signalControllerPos);
        if (tileEntity instanceof TileEntityTrafficSignalController) {
          TileEntityTrafficSignalController tileEntityTrafficSignalController
              = (TileEntityTrafficSignalController) tileEntity;
          boolean linked = tileEntityTrafficSignalController.linkDevice(pos,
              signalBlock.getSignalSide(worldIn,
                  pos),
              circuitLinkIndex);

          if (linked &&
              (signalBlock instanceof BlockControllableCrosswalkLeftMount ||
                  signalBlock instanceof BlockControllableCrosswalkRightMount ||
                  signalBlock instanceof BlockControllableCrosswalkMount ||
                  signalBlock instanceof BlockControllableCrosswalkMount90Deg)) {
            player.sendMessage(new TextComponentString("Crosswalk light connected to circuit " +
                circuitLinkIndex +
                " of signal controller at " +
                "(" +
                signalControllerPos.getX() +
                "," +
                signalControllerPos.getY() +
                "," +
                signalControllerPos.getZ() +
                ")"));
          } else if (linked
              && signalBlock instanceof BlockControllableTrafficSignalTrainController) {
            player.sendMessage(new TextComponentString(
                "Train locking rail controller connected to circuit " +
                    circuitLinkIndex +
                    " of signal controller at " +
                    "(" +
                    signalControllerPos.getX() +
                    "," +
                    signalControllerPos.getY() +
                    "," +
                    signalControllerPos.getZ() +
                    ")"));
          } else if (linked
              && signalBlock instanceof AbstractBlockControllableCrosswalkAccessory) {
            player.sendMessage(new TextComponentString("Crosswalk accessory connected to circuit " +
                circuitLinkIndex +
                " of signal controller at " +
                "(" +
                signalControllerPos.getX() +
                "," +
                signalControllerPos.getY() +
                "," +
                signalControllerPos.getZ() +
                ")"));
          } else if (linked) {
            player.sendMessage(new TextComponentString("Signal connected to circuit " +
                circuitLinkIndex +
                " of signal controller at " +
                "(" +
                signalControllerPos.getX() +
                "," +
                signalControllerPos.getY() +
                "," +
                signalControllerPos.getZ() +
                ")"));
          }
        } else {
          player.sendMessage(new TextComponentString(
              "Unable to link device! Lost connection to previously connected controller."));

        }

        return EnumActionResult.SUCCESS;
      } else if (signalControllerPos != null &&
          state.getBlock() instanceof AbstractBlockControllableSignal &&
          player.isSneaking()) {
        AbstractBlockControllableSignal signalBlock =
            (AbstractBlockControllableSignal) state.getBlock();

        TileEntity tileEntity = worldIn.getTileEntity(signalControllerPos);
        if (tileEntity instanceof TileEntityTrafficSignalController) {
          TileEntityTrafficSignalController tileEntityTrafficSignalController
              = (TileEntityTrafficSignalController) tileEntity;
          boolean removed = tileEntityTrafficSignalController.unlinkDevice(pos);

          // If unlinked, change device state to off
          AbstractBlockControllableSignal.changeSignalColor(worldIn, pos,
              AbstractBlockControllableSignal.SIGNAL_OFF);

          if (removed &&
              (signalBlock instanceof BlockControllableCrosswalkLeftMount ||
                  signalBlock instanceof BlockControllableCrosswalkRightMount ||
                  signalBlock instanceof BlockControllableCrosswalkMount ||
                  signalBlock instanceof BlockControllableCrosswalkMount90Deg)) {
            player.sendMessage(
                new TextComponentString("Crosswalk light unlinked from signal controller" +
                    " " +
                    "at " +
                    "(" +
                    signalControllerPos.getX() +
                    "," +
                    signalControllerPos.getY() +
                    "," +
                    signalControllerPos.getZ() +
                    ")"));
          } else if (removed
              && signalBlock instanceof BlockControllableTrafficSignalTrainController) {
            player.sendMessage(new TextComponentString(
                "Train locking rail controller unlinked from signal controller at " +
                    "(" +
                    signalControllerPos.getX() +
                    "," +
                    signalControllerPos.getY() +
                    "," +
                    signalControllerPos.getZ() +
                    ")"));
          } else if (removed
              && signalBlock instanceof AbstractBlockControllableCrosswalkAccessory) {
            player.sendMessage(new TextComponentString(
                "Crosswalk accessory unlinked from signal controller at " +
                    "(" +
                    signalControllerPos.getX() +
                    "," +
                    signalControllerPos.getY() +
                    "," +
                    signalControllerPos.getZ() +
                    ")"));
          } else if (removed) {
            player.sendMessage(
                new TextComponentString("Signal unlinked from signal controller at " +
                    "(" +
                    signalControllerPos.getX() +
                    "," +
                    signalControllerPos.getY() +
                    "," +
                    signalControllerPos.getZ() +
                    ")"));
          }
        } else {
          player.sendMessage(new TextComponentString(
              "Unable to unlink device! Lost connection to previously " + "connected controller."));

        }

        return EnumActionResult.SUCCESS;
      } else {
        if (signalControllerPos != null) {
          TileEntity tileEntity = worldIn.getTileEntity(signalControllerPos);
          if (tileEntity instanceof TileEntityTrafficSignalController) {
            TileEntityTrafficSignalController tileEntityTrafficSignalController
                = (TileEntityTrafficSignalController) tileEntity;

            if (circuitLinkIndex > tileEntityTrafficSignalController.getSignalCircuitCount()) {
              circuitLinkIndexMap.put(player.getUniqueID(), 0);
            }
            circuitLinkIndexMap.put(player.getUniqueID(),
                circuitLinkIndexMap.getOrDefault(player.getUniqueID(), 1) + 1);

            if (circuitLinkIndexMap.getOrDefault(player.getUniqueID(), 1) >
                tileEntityTrafficSignalController.getSignalCircuitCount()) {

              player.sendMessage(new TextComponentString("Linking to circuit #" +
                  circuitLinkIndexMap.getOrDefault(
                      player.getUniqueID(), 1) +
                  " (new)"));
            } else {

              player.sendMessage(new TextComponentString("Linking to circuit #" +
                  circuitLinkIndexMap.getOrDefault(
                      player.getUniqueID(), 1)));
            }

          } else {
            player.sendMessage(new TextComponentString(
                "Cannot change circuit until a signal controller has been selected!"));

          }
        } else {
          player.sendMessage(new TextComponentString(
              "Cannot change circuit until a signal controller has been selected!   " +
                  circuitLinkIndexMap.size() +
                  "  " +
                  signalControllerPosMap.entrySet()
                      .stream()
                      .map(Object::toString)
                      .collect(Collectors.joining(", "))));

        }
        return EnumActionResult.SUCCESS;
      }
    }
    return EnumActionResult.SUCCESS;
  }

  @Override
  public void addInformation(ItemStack itemstack, World world, List<String> list,
      ITooltipFlag flag) {
    super.addInformation(itemstack, world, list, flag);
    list.add("Link signals, sensors, and crosswalks to controller circuits.");
    list.add("Click controller to select, click device to link.");
    list.add("Sneak + click to unlink. Click empty to change circuit.");
    list.add("Also links lane control signals to a lane control controller.");
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
    return "nssignallinker";

  }
}

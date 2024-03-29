package com.micatechnologies.minecraft.csm.trafficsignals;

import com.micatechnologies.minecraft.csm.codeutils.AbstractItem;
import com.micatechnologies.minecraft.csm.trafficsignals.logic.AbstractBlockControllableCrosswalkAccessory;
import com.micatechnologies.minecraft.csm.trafficsignals.logic.AbstractBlockControllableSignal;
import com.micatechnologies.minecraft.csm.trafficsignals.logic.AbstractBlockTrafficSignalSensor;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
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

public class ItemNSSignalLinker extends AbstractItem {

  private final Map<UUID, BlockPos> signalControllerPosMap = new HashMap<>();
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
              state.getBlock() instanceof AbstractBlockTrafficSignalSensor)) {

        player.sendMessage(new TextComponentString("No signal controller has been selected."));

        return EnumActionResult.SUCCESS;
      } else if (signalControllerPos != null &&
          state.getBlock() instanceof AbstractBlockTrafficSignalSensor &&
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
          state.getBlock() instanceof AbstractBlockTrafficSignalSensor &&
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
        AbstractBlockControllableSignal clickedBlock =
            (AbstractBlockControllableSignal) state.getBlock();

        TileEntity tileEntity = worldIn.getTileEntity(signalControllerPos);
        if (tileEntity instanceof TileEntityTrafficSignalController) {
          TileEntityTrafficSignalController tileEntityTrafficSignalController
              = (TileEntityTrafficSignalController) tileEntity;
          boolean linked = tileEntityTrafficSignalController.linkDevice(pos,
              clickedBlock.getSignalSide(worldIn,
                  pos),
              circuitLinkIndex);

          if (linked &&
              (clickedBlock instanceof BlockControllableCrosswalkLeftMount ||
                  clickedBlock instanceof BlockControllableCrosswalkRightMount ||
                  clickedBlock instanceof BlockControllableCrosswalkMount ||
                  clickedBlock instanceof BlockControllableCrosswalkMount90Deg)) {
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
              && clickedBlock instanceof BlockControllableTrafficSignalTrainController) {
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
              && clickedBlock instanceof AbstractBlockControllableCrosswalkAccessory) {
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
        AbstractBlockControllableSignal clickedBlock =
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
              (clickedBlock instanceof BlockControllableCrosswalkLeftMount ||
                  clickedBlock instanceof BlockControllableCrosswalkRightMount ||
                  clickedBlock instanceof BlockControllableCrosswalkMount ||
                  clickedBlock instanceof BlockControllableCrosswalkMount90Deg)) {
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
              && clickedBlock instanceof BlockControllableTrafficSignalTrainController) {
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
              && clickedBlock instanceof AbstractBlockControllableCrosswalkAccessory) {
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
    list.add("Link traffic signals to the Primary circuit of a signal controller.");
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

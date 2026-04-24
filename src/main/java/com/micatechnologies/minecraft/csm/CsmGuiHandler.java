package com.micatechnologies.minecraft.csm;

import com.micatechnologies.minecraft.csm.hvac.HvacThermostatGui;
import com.micatechnologies.minecraft.csm.hvac.HvacZoneThermostatGui;
import com.micatechnologies.minecraft.csm.hvac.TileEntityHvacThermostat;
import com.micatechnologies.minecraft.csm.hvac.TileEntityHvacZoneThermostat;
import com.micatechnologies.minecraft.csm.lifesafety.FireAlarmPanelConfigGui;
import com.micatechnologies.minecraft.csm.lifesafety.TileEntityFireAlarmControlPanel;
import com.micatechnologies.minecraft.csm.technology.BlockRedstoneTTSGui;
import com.micatechnologies.minecraft.csm.technology.TileEntityRedstoneTTS;
import com.micatechnologies.minecraft.csm.trafficsignals.CrosswalkConfigGui;
import com.micatechnologies.minecraft.csm.trafficsignals.SignalControllerConfigGui;
import com.micatechnologies.minecraft.csm.trafficsignals.SignalControllerVisualGui;
import com.micatechnologies.minecraft.csm.trafficsignals.SignalHeadConfigGui;
import com.micatechnologies.minecraft.csm.trafficsignals.TileEntityCrosswalkSignalNew;
import com.micatechnologies.minecraft.csm.trafficsignals.TileEntityTrafficSignalController;
import com.micatechnologies.minecraft.csm.trafficsignals.TileEntityTrafficSignalHead;
import javax.annotation.Nullable;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.IGuiHandler;

/**
 * The GUI handler for the City Super Mod.
 *
 * @version 1.0
 * @since 2023.2.1
 */
public class CsmGuiHandler implements IGuiHandler {

  /**
   * Returns a server-side GUI container to be displayed to the user.
   *
   * @param id     The GUI ID Number
   * @param player The player viewing the GUI
   * @param world  The current world
   * @param x      X Position
   * @param y      Y Position
   * @param z      Z Position
   *
   * @return A server-side GUI container to be displayed to the user, null if none.
   *
   * @since 1.0
   */
  @Nullable
  @Override
  public Object getServerGuiElement(int id, EntityPlayer player, World world, int x, int y, int z) {
    return null;
  }

  /**
   * Returns a client-side GUI container to be displayed to the user.
   *
   * @param id     The GUI ID Number
   * @param player The player viewing the GUI
   * @param world  The current world
   * @param x      X Position
   * @param y      Y Position
   * @param z      Z Position
   *
   * @return A client-side GUI container to be displayed to the user, null if none.
   *
   * @since 1.0
   */
  @Override
  public Object getClientGuiElement(int id, EntityPlayer player, World world, int x, int y, int z) {
    BlockPos pos = new BlockPos(x, y, z);
    TileEntity tileEntity = world.getTileEntity(pos);
    Object returnValue = null;
    if (id == 0) {
      returnValue = new BlockRedstoneTTSGui((TileEntityRedstoneTTS) tileEntity);
    } else if (id == 1 && tileEntity instanceof TileEntityTrafficSignalHead) {
      returnValue = new SignalHeadConfigGui((TileEntityTrafficSignalHead) tileEntity);
    } else if (id == 2 && tileEntity instanceof TileEntityTrafficSignalController) {
      returnValue = new SignalControllerConfigGui((TileEntityTrafficSignalController) tileEntity);
    } else if (id == 5 && tileEntity instanceof TileEntityTrafficSignalController) {
      returnValue = new SignalControllerVisualGui((TileEntityTrafficSignalController) tileEntity);
    } else if (id == 3 && tileEntity instanceof TileEntityFireAlarmControlPanel) {
      returnValue = new FireAlarmPanelConfigGui((TileEntityFireAlarmControlPanel) tileEntity);
    } else if (id == 4 && tileEntity instanceof TileEntityCrosswalkSignalNew) {
      // The extra data bit (encoded in the GUI ID) tells us if it's the double signal
      boolean isDouble = world.getBlockState(pos).getBlock()
          instanceof com.micatechnologies.minecraft.csm.trafficsignals.BlockControllableCrosswalkSignalDouble;
      returnValue = new CrosswalkConfigGui((TileEntityCrosswalkSignalNew) tileEntity, isDouble);
    } else if (id == 6 && tileEntity instanceof TileEntityHvacThermostat) {
      returnValue = new HvacThermostatGui((TileEntityHvacThermostat) tileEntity);
    } else if (id == 7 && tileEntity instanceof TileEntityHvacZoneThermostat) {
      returnValue = new HvacZoneThermostatGui((TileEntityHvacZoneThermostat) tileEntity);
    } else if (id == 8 && tileEntity instanceof com.micatechnologies.minecraft.csm.trafficsignals.TileEntityBlankoutBox) {
      returnValue = new com.micatechnologies.minecraft.csm.trafficsignals.BlankoutBoxConfigGui(
          (com.micatechnologies.minecraft.csm.trafficsignals.TileEntityBlankoutBox) tileEntity);
    } else if (id == 9 && tileEntity instanceof com.micatechnologies.minecraft.csm.trafficaccessories.TileEntityPortableMessageSign) {
      returnValue = new com.micatechnologies.minecraft.csm.trafficaccessories.BlockPortableMessageSignGui(
          (com.micatechnologies.minecraft.csm.trafficaccessories.TileEntityPortableMessageSign) tileEntity);
    } else if (id == 10 && tileEntity instanceof com.micatechnologies.minecraft.csm.trafficaccessories.TileEntityOverheadMessageSign) {
      returnValue = new com.micatechnologies.minecraft.csm.trafficaccessories.BlockOverheadMessageSignGui(
          (com.micatechnologies.minecraft.csm.trafficaccessories.TileEntityOverheadMessageSign) tileEntity);
    } else if (id == 11 && tileEntity instanceof com.micatechnologies.minecraft.csm.trafficaccessories.TileEntityVariableSpeedLimit) {
      returnValue = new com.micatechnologies.minecraft.csm.trafficaccessories.BlockPortableSpeedLimitGui(
          (com.micatechnologies.minecraft.csm.trafficaccessories.TileEntityVariableSpeedLimit) tileEntity);
    } else if (id == 12 && tileEntity instanceof com.micatechnologies.minecraft.csm.trafficaccessories.TileEntityOverheadSpeedLimit) {
      returnValue = new com.micatechnologies.minecraft.csm.trafficaccessories.BlockOverheadSpeedLimitGui(
          (com.micatechnologies.minecraft.csm.trafficaccessories.TileEntityOverheadSpeedLimit) tileEntity);
    }
    return returnValue;
  }
}

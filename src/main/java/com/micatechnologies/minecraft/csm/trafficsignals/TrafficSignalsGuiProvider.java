package com.micatechnologies.minecraft.csm.trafficsignals;

import com.micatechnologies.minecraft.csm.codeutils.gui.ICsmGuiProvider;
import javax.annotation.Nullable;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * Supplies the traffic signals package's GUI screens: signal heads, the signal controller (config
 * and visual views), the crosswalk signal, the blankout box and the signal sensor.
 *
 * @version 1.0
 * @since 2026.9
 */
public class TrafficSignalsGuiProvider implements ICsmGuiProvider {

  /**
   * {@inheritDoc}
   *
   * @since 1.0
   */
  @Nullable
  @Override
  public Object getClientGuiElement(int id, EntityPlayer player, World world, BlockPos pos) {
    TileEntity tileEntity = world.getTileEntity(pos);
    Object returnValue = null;
    if (id == 1 && tileEntity instanceof TileEntityTrafficSignalHead) {
      returnValue = new SignalHeadConfigGui((TileEntityTrafficSignalHead) tileEntity);
    } else if (id == 2 && tileEntity instanceof TileEntityTrafficSignalController) {
      returnValue = new SignalControllerConfigGui((TileEntityTrafficSignalController) tileEntity);
    } else if (id == 5 && tileEntity instanceof TileEntityTrafficSignalController) {
      returnValue = new SignalControllerVisualGui((TileEntityTrafficSignalController) tileEntity);
    } else if (id == 4 && tileEntity instanceof TileEntityCrosswalkSignalNew) {
      // The extra data bit (encoded in the GUI ID) tells us if it's the double signal
      boolean isDouble = world.getBlockState(pos).getBlock()
          instanceof BlockControllableCrosswalkSignalDouble;
      returnValue = new CrosswalkConfigGui((TileEntityCrosswalkSignalNew) tileEntity, isDouble);
    } else if (id == 8 && tileEntity instanceof TileEntityBlankoutBox) {
      returnValue = new BlankoutBoxConfigGui((TileEntityBlankoutBox) tileEntity);
    } else if (id == 19 && tileEntity instanceof TileEntityTrafficSignalSensor) {
      returnValue = new SensorConfigGui((TileEntityTrafficSignalSensor) tileEntity);
    }
    return returnValue;
  }
}

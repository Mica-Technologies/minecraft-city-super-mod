package com.micatechnologies.minecraft.csm.hvac;

import com.micatechnologies.minecraft.csm.codeutils.gui.ICsmGuiProvider;
import javax.annotation.Nullable;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * Supplies the HVAC package's GUI screens: the thermostat and the zone thermostat.
 *
 * @version 1.0
 * @since 2026.9
 */
public class HvacGuiProvider implements ICsmGuiProvider {

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
    if (id == 6 && tileEntity instanceof TileEntityHvacThermostat) {
      returnValue = new HvacThermostatGui((TileEntityHvacThermostat) tileEntity);
    } else if (id == 7 && tileEntity instanceof TileEntityHvacZoneThermostat) {
      returnValue = new HvacZoneThermostatGui((TileEntityHvacZoneThermostat) tileEntity);
    }
    return returnValue;
  }
}

package com.micatechnologies.minecraft.csm.lifesafety;

import com.micatechnologies.minecraft.csm.codeutils.gui.ICsmGuiProvider;
import javax.annotation.Nullable;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * Supplies the life safety package's GUI screens: the fire alarm control panel.
 *
 * @version 1.0
 * @since 2026.9
 */
public class LifeSafetyGuiProvider implements ICsmGuiProvider {

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
    if (id == 3 && tileEntity instanceof TileEntityFireAlarmControlPanel) {
      returnValue = new FireAlarmControlPanelGui((TileEntityFireAlarmControlPanel) tileEntity);
    }
    return returnValue;
  }
}

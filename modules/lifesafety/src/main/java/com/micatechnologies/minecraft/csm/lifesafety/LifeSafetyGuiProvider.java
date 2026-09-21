package com.micatechnologies.minecraft.csm.lifesafety;

import com.micatechnologies.minecraft.csm.lifesafety.exitsign.AbstractBlockExitSign;
import com.micatechnologies.minecraft.csm.lifesafety.exitsign.GuiExitSign;
import com.micatechnologies.minecraft.csm.lifesafety.exitsign.TileEntityExitSign;

import com.micatechnologies.minecraft.csm.codeutils.gui.ICsmGuiProvider;
import javax.annotation.Nullable;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * Supplies the life safety package's GUI screens: the fire alarm control panel and the exit
 * signs' setup screen.
 *
 * @version 1.0
 * @since 2026.9
 */
public class LifeSafetyGuiProvider implements ICsmGuiProvider {

  /** An exit sign's setup screen. GUI ids are global across the mod; 0 to 30 were taken. */
  public static final int EXIT_SIGN_GUI_ID = 31;

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
    } else if (id == EXIT_SIGN_GUI_ID && tileEntity instanceof TileEntityExitSign
        && world.getBlockState(pos).getBlock() instanceof AbstractBlockExitSign) {
      returnValue = new GuiExitSign((AbstractBlockExitSign) world.getBlockState(pos).getBlock(),
          (TileEntityExitSign) tileEntity);
    }
    return returnValue;
  }
}

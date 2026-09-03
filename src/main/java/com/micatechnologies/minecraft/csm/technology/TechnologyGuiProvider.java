package com.micatechnologies.minecraft.csm.technology;

import com.micatechnologies.minecraft.csm.codeutils.gui.ICsmGuiProvider;
import javax.annotation.Nullable;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * Supplies the technology package's GUI screens: the Redstone TTS block, the computer, the fare
 * vending machine and the fare gate.
 *
 * @version 1.0
 * @since 2026.9
 */
public class TechnologyGuiProvider implements ICsmGuiProvider {

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
    if (id == 0 && tileEntity instanceof TileEntityRedstoneTTS) {
      // Guarded like every other id below. Without the instanceof this branch would throw on a
      // missing or mismatched tile entity instead of simply declining to open a screen.
      returnValue = new BlockRedstoneTTSGui((TileEntityRedstoneTTS) tileEntity);
    } else if (id == 15 && tileEntity instanceof TileEntityComputer) {
      returnValue = new ComputerGui((TileEntityComputer) tileEntity);
    } else if (id == 16
        && world.getBlockState(pos).getBlock() instanceof BlockFareVendingMachine) {
      returnValue = new FareVendingGui(pos);
    } else if (id == 17 && tileEntity instanceof TileEntityFareGate) {
      returnValue = new FareGateConfigGui((TileEntityFareGate) tileEntity);
    }
    return returnValue;
  }
}

package com.micatechnologies.minecraft.csm.technology;

import com.micatechnologies.minecraft.csm.codeutils.gui.ICsmGuiProvider;
import javax.annotation.Nullable;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * Supplies the technology package's GUI screens: the computer, the fare vending machine and the
 * fare gate.
 *
 * <p>GUI id 0 — the Redstone TTS Module — is not here. That block ships in the Text to Speech
 * module, which registers its own provider for that id.</p>
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
    if (id == 15 && tileEntity instanceof TileEntityComputer) {
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

package com.micatechnologies.minecraft.csm.transit;

import com.micatechnologies.minecraft.csm.codeutils.gui.ICsmGuiProvider;
import com.micatechnologies.minecraft.csm.transit.fare.BlockFareGate;
import com.micatechnologies.minecraft.csm.transit.fare.BlockFareVendingMachine;
import com.micatechnologies.minecraft.csm.transit.fare.FareGateConfigGui;
import com.micatechnologies.minecraft.csm.transit.fare.FareVendingGui;
import com.micatechnologies.minecraft.csm.transit.fare.TileEntityFareGate;
import javax.annotation.Nullable;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * The transit module's screens, handed to Core's GUI handler through {@code CsmGuiRegistry}: the
 * fare vending machine (16) and the fare gate's configuration (17). GUI ids are global across the
 * mod; these two kept the ids they had before the fare equipment moved here from Technology.
 *
 * @since 2026.9
 */
public class TransitGuiProvider implements ICsmGuiProvider {

  @Nullable
  @Override
  public Object getClientGuiElement(int id, EntityPlayer player, World world, BlockPos pos) {
    if (id == BlockFareVendingMachine.GUI_ID
        && world.getBlockState(pos).getBlock() instanceof BlockFareVendingMachine) {
      return new FareVendingGui(pos);
    }
    if (id == BlockFareGate.OP_CONFIG_GUI_ID) {
      TileEntity tileEntity = world.getTileEntity(pos);
      if (tileEntity instanceof TileEntityFareGate) {
        return new FareGateConfigGui((TileEntityFareGate) tileEntity);
      }
    }
    return null;
  }
}

package com.micatechnologies.minecraft.csm.trafficsigns;

import com.micatechnologies.minecraft.csm.codeutils.gui.ICsmGuiProvider;
import javax.annotation.Nullable;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * Supplies the road signs package's GUI screens. Only the dynamic route marker sign has one --
 * every other sign in the catalogue says one fixed thing -- but it has its own provider rather
 * than borrowing the traffic accessories one so the two subsystems stay separable.
 *
 * @version 1.0
 * @since 2026.9.20
 */
public class TrafficSignsGuiProvider implements ICsmGuiProvider {

  @Nullable
  @Override
  public Object getClientGuiElement(int id, EntityPlayer player, World world, BlockPos pos) {
    TileEntity tileEntity = world.getTileEntity(pos);
    if (id == BlockDynamicRouteMarkerSign.GUI_ID
        && tileEntity instanceof TileEntityDynamicRouteMarkerSign) {
      return new DynamicRouteMarkerSignGui((TileEntityDynamicRouteMarkerSign) tileEntity);
    }
    return null;
  }
}

package com.micatechnologies.minecraft.csm.trafficsigns;

import com.micatechnologies.minecraft.csm.codeutils.gui.ICsmGuiProvider;
import javax.annotation.Nullable;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * Supplies the road signs package's GUI screens: the dynamic route marker sign and the two
 * street name blades. Every other sign in the catalogue says one fixed thing, but these have
 * their own provider rather than borrowing the traffic accessories one so the two subsystems
 * stay separable.
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
    if (id == BlockStreetNameBlade.GUI_ID
        && tileEntity instanceof TileEntityStreetNameBlade) {
      // The dynamic street sign's own editor: a blade is that sign's document, and the
      // tile entity is that tile entity, so the screen needs nothing of its own.
      return new com.micatechnologies.minecraft.csm.trafficaccessories.DynamicStreetSignGui(
          (TileEntityStreetNameBlade) tileEntity);
    }
    return null;
  }
}

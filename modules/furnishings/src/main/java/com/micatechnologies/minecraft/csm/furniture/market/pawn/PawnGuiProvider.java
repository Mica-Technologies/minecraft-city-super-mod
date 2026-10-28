package com.micatechnologies.minecraft.csm.furniture.market.pawn;

import com.micatechnologies.minecraft.csm.codeutils.gui.ICsmGuiProvider;
import javax.annotation.Nullable;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * The pawn counter's two screens: the pawn screen and the resale screen (only while the counter
 * resells).
 *
 * @since 2026.10
 */
public class PawnGuiProvider implements ICsmGuiProvider {

  @Nullable
  @Override
  public Object getClientGuiElement(int id, EntityPlayer player, World world, BlockPos pos) {
    TileEntity te = world.getTileEntity(pos);
    if (!(te instanceof TileEntityPawnCounter)) {
      return null;
    }
    TileEntityPawnCounter counter = (TileEntityPawnCounter) te;
    String title = world.getBlockState(pos).getBlock().getLocalizedName();
    if (id == BlockPawnCounter.GUI_PAWN) {
      return new GuiPawnCounter(new ContainerPawnCounter(player.inventory, counter), title);
    }
    if (id == BlockPawnCounter.GUI_RESALE) {
      return new GuiPawnResale(new ContainerPawnResale(player.inventory, counter, false), title);
    }
    return null;
  }

  @Nullable
  @Override
  public Object getServerGuiElement(int id, EntityPlayer player, World world, BlockPos pos) {
    TileEntity te = world.getTileEntity(pos);
    if (!(te instanceof TileEntityPawnCounter)) {
      return null;
    }
    TileEntityPawnCounter counter = (TileEntityPawnCounter) te;
    if (id == BlockPawnCounter.GUI_PAWN) {
      return new ContainerPawnCounter(player.inventory, counter);
    }
    if (id == BlockPawnCounter.GUI_RESALE && counter.isResale()) {
      return new ContainerPawnResale(player.inventory, counter, true);
    }
    return null;
  }
}

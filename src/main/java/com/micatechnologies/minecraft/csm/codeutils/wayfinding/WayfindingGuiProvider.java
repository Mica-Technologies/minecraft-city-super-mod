package com.micatechnologies.minecraft.csm.codeutils.wayfinding;

import com.micatechnologies.minecraft.csm.codeutils.gui.ICsmGuiProvider;
import javax.annotation.Nullable;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * Opens the wayfinding panel editor ({@link AbstractBlockWayfindingPanel#GUI_ID}) for any of the
 * panel blocks, whichever module ships it. Registered by Core.
 *
 * @since 2026.10
 */
public class WayfindingGuiProvider implements ICsmGuiProvider {

  @Nullable
  @Override
  public Object getClientGuiElement(int id, EntityPlayer player, World world, BlockPos pos) {
    if (id != AbstractBlockWayfindingPanel.GUI_ID) {
      return null;
    }
    IBlockState state = world.getBlockState(pos);
    if (!(state.getBlock() instanceof AbstractBlockWayfindingPanel)) {
      return null;
    }
    AbstractBlockWayfindingPanel block = (AbstractBlockWayfindingPanel) state.getBlock();
    // opened on the cell clicked; the sign shown is its panel's controller's
    EnumFacing facing = state.getValue(AbstractBlockWayfindingPanel.FACING);
    TileEntity controller = world.getTileEntity(
        WayfindingPanels.controllerOf(world, pos, block, facing));
    if (!(controller instanceof AbstractTileEntityWayfindingPanel)) {
      controller = world.getTileEntity(pos);
    }
    if (!(controller instanceof AbstractTileEntityWayfindingPanel)) {
      return null;
    }
    return new WayfindingPanelGui(pos, (AbstractTileEntityWayfindingPanel) controller,
        block.getPresets(), I18n.format(block.getTranslationKey() + ".name"));
  }
}

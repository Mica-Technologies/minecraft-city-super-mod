package com.micatechnologies.minecraft.csm.streetscape;

import com.micatechnologies.minecraft.csm.codeutils.gui.ICsmGuiProvider;
import javax.annotation.Nullable;
import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * The Streetscape tab's screens: the utility box number editor.
 *
 * @version 1.0
 */
public class StreetscapeGuiProvider implements ICsmGuiProvider {

  @Nullable
  @Override
  public Object getClientGuiElement(int id, EntityPlayer player, World world, BlockPos pos) {
    if (id != BlockUtilityBoxLabelled.GUI_ID) {
      return null;
    }
    TileEntity te = world.getTileEntity(pos);
    Block block = world.getBlockState(pos).getBlock();
    if (!(te instanceof TileEntityUtilityBoxLabel) || !(block instanceof BlockUtilityBoxLabelled)) {
      return null;
    }
    UtilityBoxSpec.Label label = ((BlockUtilityBoxLabelled) block).getSpec().getLabel();
    int lines = label != null ? label.getLines() : 1;
    return new UtilityBoxLabelGui((TileEntityUtilityBoxLabel) te, lines);
  }
}

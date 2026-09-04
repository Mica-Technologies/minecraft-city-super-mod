package com.micatechnologies.minecraft.csm.materials;

import com.micatechnologies.minecraft.csm.codeutils.gui.ICsmGuiProvider;
import javax.annotation.Nullable;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * Supplies the materials package's GUI screens: the CSM Fabricator picker. This provider belongs
 * to Core, which registers it from its own pre-initialization.
 *
 * @version 1.0
 * @since 2026.9
 */
public class MaterialsGuiProvider implements ICsmGuiProvider {

  /**
   * {@inheritDoc}
   *
   * @since 1.0
   */
  @Nullable
  @Override
  public Object getClientGuiElement(int id, EntityPlayer player, World world, BlockPos pos) {
    Object returnValue = null;
    if (id == BlockCsmFabricator.GUI_ID
        && world.getBlockState(pos).getBlock() instanceof BlockCsmFabricator) {
      // The Fabricator has no tile entity; the picker only needs the block position so the
      // server can verify proximity when a selection is confirmed.
      returnValue = new CsmFabricatorGui(pos);
    }
    return returnValue;
  }
}

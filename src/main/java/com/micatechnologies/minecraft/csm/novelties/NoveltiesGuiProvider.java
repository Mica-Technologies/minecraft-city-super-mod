package com.micatechnologies.minecraft.csm.novelties;

import com.micatechnologies.minecraft.csm.codeutils.gui.ICsmGuiProvider;
import javax.annotation.Nullable;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * Supplies the novelties package's GUI screens: the multi-game arcade cabinet.
 *
 * @version 1.0
 * @since 2026.9
 */
public class NoveltiesGuiProvider implements ICsmGuiProvider {

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
    if (id == BlockArcadeMultiGame.GUI_ID && tileEntity instanceof TileEntityArcadeCabinet) {
      // The GUI opens on its own game-select screen and instantiates a game only once the player
      // picks one, so nothing outside this client-side branch ever names a game class.
      returnValue = new ArcadeGui((TileEntityArcadeCabinet) tileEntity);
    }
    return returnValue;
  }
}

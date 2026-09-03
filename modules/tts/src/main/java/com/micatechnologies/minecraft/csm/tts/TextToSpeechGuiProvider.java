package com.micatechnologies.minecraft.csm.tts;

import com.micatechnologies.minecraft.csm.codeutils.gui.ICsmGuiProvider;
import javax.annotation.Nullable;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * Supplies this module's one GUI screen: the Redstone TTS Module's message editor.
 *
 * <p>Id 0 was Technology's, because the block used to live in the technology package. The id is
 * unchanged so a client and a server that disagree about which module owns it still open the same
 * screen; only the provider that answers for it moved.</p>
 *
 * @version 1.0
 * @since 2026.9
 */
public class TextToSpeechGuiProvider implements ICsmGuiProvider {

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
      // Guarded on the tile entity. Without the instanceof this branch would throw on a missing
      // or mismatched tile entity instead of simply declining to open a screen.
      returnValue = new BlockRedstoneTTSGui((TileEntityRedstoneTTS) tileEntity);
    }
    return returnValue;
  }
}

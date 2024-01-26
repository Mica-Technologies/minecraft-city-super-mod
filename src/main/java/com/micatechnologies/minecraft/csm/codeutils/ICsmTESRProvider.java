package com.micatechnologies.minecraft.csm.codeutils;

import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.tileentity.TileEntity;

/**
 * Extended tile entity provider interface for tile entity classes which also have a tile entity
 * special renderer (TESR)
 */
public interface ICsmTESRProvider<T extends TileEntity>
    extends ICsmTileEntityProvider<T> {

  /**
   * Gets an new tile entity special renderer (TESR) instance for the block.
   *
   * @return the new TESR instance for the block
   *
   * @since 1.0
   */
  TileEntitySpecialRenderer<T> getNewTESR();
}

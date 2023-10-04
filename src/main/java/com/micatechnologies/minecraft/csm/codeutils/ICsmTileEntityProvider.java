package com.micatechnologies.minecraft.csm.codeutils;

import net.minecraft.block.ITileEntityProvider;
import net.minecraft.tileentity.TileEntity;

/**
 * Mod tile entity provider interface which provides common method stubs and properties for all providers in this mod.
 *
 * @version 1.0
 * @since 2023.3
 */
public interface ICsmTileEntityProvider extends ITileEntityProvider
{
    /**
     * Gets the tile entity class for the block.
     *
     * @return the tile entity class for the block
     *
     * @since 1.0
     */
    Class< ? extends TileEntity > getTileEntityClass();

    /**
     * Gets the tile entity name for the block.
     *
     * @return the tile entity name for the block
     *
     * @since 1.0
     */
    String getTileEntityName();
}

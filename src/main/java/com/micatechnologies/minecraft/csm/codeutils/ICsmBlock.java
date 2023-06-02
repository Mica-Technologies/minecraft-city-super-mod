package com.micatechnologies.minecraft.csm.codeutils;

import com.micatechnologies.minecraft.csm.CsmRegistry;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.math.AxisAlignedBB;

import javax.annotation.Nonnull;

/**
 * Mod block interface which provides common method stubs and properties for all blocks in this mod.
 *
 * @version 1.0
 * @since 2023.3
 */
public interface ICsmBlock
{

    /**
     * The square (default) bounding box for a block.
     *
     * @since 1.0
     */
    AxisAlignedBB SQUARE_BOUNDING_BOX = new AxisAlignedBB( 0.0D, 0.0D, 0.0D, 1.0D, 1.0D, 1.0D );

    /**
     * Retrieves the registry name of the block.
     *
     * @return The registry name of the block.
     *
     * @since 1.0
     */
    String getBlockRegistryName();

    /**
     * Retrieves the bounding box of the block.
     *
     * @return The bounding box of the block.
     *
     * @since 1.0
     */
    AxisAlignedBB getBlockBoundingBox();

    /**
     * Retrieves whether the block is an opaque cube.
     *
     * @param state The block state.
     *
     * @return {@code true} if the block is an opaque cube, {@code false} otherwise.
     *
     * @since 1.0
     */
    boolean getBlockIsOpaqueCube( IBlockState state );

    /**
     * Retrieves whether the block is a full cube.
     *
     * @param state The block state.
     *
     * @return {@code true} if the block is a full cube, {@code false} otherwise.
     *
     * @since 1.0
     */
    boolean getBlockIsFullCube( IBlockState state );

    /**
     * Retrieves whether the block connects to redstone.
     *
     * @return {@code true} if the block connects to redstone, {@code false} otherwise.
     *
     * @since 1.0
     */
    boolean getBlockConnectsRedstone();

    /**
     * Retrieves the block's render layer.
     *
     * @return The block's render layer.
     *
     * @since 1.0
     */
    @Nonnull
    BlockRenderLayer getBlockRenderLayer();
}

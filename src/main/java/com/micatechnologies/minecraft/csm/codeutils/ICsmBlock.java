package com.micatechnologies.minecraft.csm.codeutils;

import com.micatechnologies.minecraft.csm.CsmRegistry;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

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
     * @param state  the block state
     * @param source the block access
     * @param pos    the block position
     *
     * @return The bounding box of the block.
     *
     * @since 1.0
     */
    AxisAlignedBB getBlockBoundingBox( IBlockState state, IBlockAccess source, BlockPos pos );

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
     * @param state  the block state
     * @param access the block access
     * @param pos    the block position
     * @param facing the block facing direction
     *
     * @return {@code true} if the block connects to redstone, {@code false} otherwise.
     *
     * @since 1.0
     */
    boolean getBlockConnectsRedstone( IBlockState state,
                                      IBlockAccess access,
                                      BlockPos pos,
                                      @Nullable EnumFacing facing );

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

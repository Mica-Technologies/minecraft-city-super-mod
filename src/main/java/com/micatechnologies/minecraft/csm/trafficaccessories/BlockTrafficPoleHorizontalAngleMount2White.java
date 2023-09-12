package com.micatechnologies.minecraft.csm.trafficaccessories;

import com.micatechnologies.minecraft.csm.codeutils.AbstractBlockRotatableNSEWUD;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.block.BlockDirectional;
import net.minecraft.block.BlockHorizontal;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class BlockTrafficPoleHorizontalAngleMount2White extends AbstractBlockRotatableNSEWUD
{
    public BlockTrafficPoleHorizontalAngleMount2White() {
        super( Material.ROCK, SoundType.STONE, "pickaxe", 1, 2F, 10F, 0F, 0 );
    }

    /**
     * Retrieves the registry name of the block.
     *
     * @return The registry name of the block.
     *
     * @since 1.0
     */
    @Override
    public String getBlockRegistryName() {
        return "trafficpolehorizontalanglemount2white";
    }

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
    @Override
    public AxisAlignedBB getBlockBoundingBox( IBlockState state, IBlockAccess source, BlockPos pos ) {
        return SQUARE_BOUNDING_BOX;
    }

    /**
     * Retrieves whether the block is an opaque cube.
     *
     * @param state The block state.
     *
     * @return {@code true} if the block is an opaque cube, {@code false} otherwise.
     *
     * @since 1.0
     */
    @Override
    public boolean getBlockIsOpaqueCube( IBlockState state ) {
        return false;
    }

    /**
     * Retrieves whether the block is a full cube.
     *
     * @param state The block state.
     *
     * @return {@code true} if the block is a full cube, {@code false} otherwise.
     *
     * @since 1.0
     */
    @Override
    public boolean getBlockIsFullCube( IBlockState state ) {
        return true;
    }

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
    @Override
    public boolean getBlockConnectsRedstone( IBlockState state,
                                             IBlockAccess access,
                                             BlockPos pos,
                                             @Nullable EnumFacing facing )
    {
        return false;
    }

    /**
     * Retrieves the block's render layer.
     *
     * @return The block's render layer.
     *
     * @since 1.0
     */
    @Nonnull
    @Override
    public BlockRenderLayer getBlockRenderLayer() {
        return BlockRenderLayer.CUTOUT_MIPPED;
    }





}

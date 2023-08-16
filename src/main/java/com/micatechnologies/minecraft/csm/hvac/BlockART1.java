package com.micatechnologies.minecraft.csm.buildingmaterials;

import com.micatechnologies.minecraft.csm.codeutils.AbstractBlockRotatableNSEWUD;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.math.AxisAlignedBB;
import javax.annotation.Nonnull;

public class BlockART1 extends AbstractBlockRotatableNSEWUD
{
    public BlockART1() {
        super( Material.ROCK, SoundType.STONE , "pickaxe", 1 , 2F , 10F , 0F , 255  );
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
        return "art1";
    }

    /**
     * Retrieves the bounding box of the block.
     *
     * @return The bounding box of the block.
     *
     * @since 1.0
     */
    @Override
    public AxisAlignedBB getBlockBoundingBox() {
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
        return true;
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
     * @return {@code true} if the block connects to redstone, {@code false} otherwise.
     *
     * @since 1.0
     */
    @Override
    public boolean getBlockConnectsRedstone() {
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
        return BlockRenderLayer.SOLID;
    }
}

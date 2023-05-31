package com.micatechnologies.minecraft.csm.lifesafety;

import com.micatechnologies.minecraft.csm.codeutils.AbstractBlockRotatableNSEWUD;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;

import com.micatechnologies.minecraft.csm.tabs.CsmTabLifeSafety;

public abstract class AbstractBlockFireAlarmSounder extends AbstractBlockRotatableNSEWUD
{
    public AbstractBlockFireAlarmSounder() {
        super( Material.ROCK, SoundType.STONE, "pickaxe", 1, 2F, 10F, 0F, 0, true, BlockRenderLayer.CUTOUT_MIPPED,
               CsmTabLifeSafety.get() );
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
        return new AxisAlignedBB( 0D, 0D, 0.8D, 1D, 1D, 1D );
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
        return false;
    }

    @Override
    public int getLightValue( IBlockState state, IBlockAccess world, BlockPos pos ) {
        return 0;
    }

    abstract public String getSoundResourceName( IBlockState blockState );

    abstract public int getSoundTickLen( IBlockState blockState );
}
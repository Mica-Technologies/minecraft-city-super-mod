package com.micatechnologies.minecraft.csm.codeutils;

import net.minecraft.block.state.IBlockState;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Mod item interface which provides common method stubs and properties for all items in this mod.
 *
 * @version 1.0
 * @since 2023.3
 */
public interface ICsmItem
{
    /**
     * Retrieves the registry name of the item.
     *
     * @return The registry name of the item.
     *
     * @since 1.0
     */
    String getItemRegistryName();
}

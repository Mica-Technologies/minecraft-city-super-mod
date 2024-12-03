package com.micatechnologies.minecraft.csm.codeutils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;

/**
 * Mod block interface for blocks which are scheduled to be replaced.
 *
 * @version 1.0
 * @since 2024.8.7
 */
public interface ICsmRetiringBlock {

  /**
   * Retrieves the replacement block ID.
   *
   * @return The replacement block ID.
   *
   * @since 1.0
   */
  String getReplacementBlockId();
}

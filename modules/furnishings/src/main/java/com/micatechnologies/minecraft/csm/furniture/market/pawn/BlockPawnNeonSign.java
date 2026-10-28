package com.micatechnologies.minecraft.csm.furniture.market.pawn;

import com.micatechnologies.minecraft.csm.furniture.residential.BlockCounterPiece;
import javax.annotation.Nonnull;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;

/**
 * The pawn shop's PAWN sign: a board printed both ways round that stands in a window or on a
 * counter (as any {@link BlockCounterPiece} does) and always glows. Decorative.
 *
 * @since 2026.10
 */
public class BlockPawnNeonSign extends BlockCounterPiece {

  /** The light the sign gives. */
  private static final int LIGHT = 9;

  public BlockPawnNeonSign(String registryName, int[] box) {
    super(registryName, box, Material.GLASS, SoundType.GLASS, BlockRenderLayer.CUTOUT);
  }

  @Override
  @SuppressWarnings("deprecation")
  public int getLightValue(@Nonnull IBlockState state) {
    return LIGHT;
  }

  @Override
  public int getLightValue(@Nonnull IBlockState state, IBlockAccess world, @Nonnull BlockPos pos) {
    return LIGHT;
  }
}

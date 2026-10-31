package com.micatechnologies.minecraft.csm.venues.seating;

import com.micatechnologies.minecraft.csm.venues.BlockVenue;
import javax.annotation.Nullable;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;

/**
 * The marking of a wheelchair space on a seating platform ({@code stadium_wheelchair_space}):
 * the international symbol of access painted on the floor in a bordered blue square, laid on
 * top of the platform's floor block. No collision.
 *
 * <p><b>Metadata</b>: the facing (S 0, W 1, N 2, E 3): the way the space faces, towards the
 * event floor; the symbol reads from behind it.</p>
 *
 * @since 2026.10
 */
public class BlockStadiumWheelchairSpace extends BlockVenue {

  private static final AxisAlignedBB BOX = new AxisAlignedBB(0, 0, 0, 1, 1 / 16.0, 1);

  /**
   * Constructs the marking.
   *
   * @param registryName its registry name
   */
  public BlockStadiumWheelchairSpace(String registryName) {
    super(registryName, Material.CIRCUITS, SoundType.STONE, 0.3F);
    setDefaultState(blockState.getBaseState().withProperty(FACING, EnumFacing.NORTH));
  }

  @Override
  public AxisAlignedBB getBlockBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
    return BOX;
  }

  @Nullable
  @Override
  @SuppressWarnings("deprecation")
  public AxisAlignedBB getCollisionBoundingBox(IBlockState state, IBlockAccess source,
      BlockPos pos) {
    return NULL_AABB;
  }
}

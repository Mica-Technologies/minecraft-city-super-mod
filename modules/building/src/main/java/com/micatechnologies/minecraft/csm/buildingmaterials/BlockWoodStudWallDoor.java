package com.micatechnologies.minecraft.csm.buildingmaterials;

import javax.annotation.Nullable;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;

/**
 * A wood stud wall framed for a door: king studs each side and a header over, with no sole plate
 * across the opening, because the floor runs through a doorway.
 *
 * <p>Has no collision at all, for the reason {@link BlockSteelStudWallDoor} does: the whole point
 * of an opening is that something walks through it.</p>
 *
 * @version 1.0
 * @since 2026.9
 */
public class BlockWoodStudWallDoor extends AbstractBlockWoodFraming {

  @Override
  public String getBlockRegistryName() {
    return "wood_stud_wall_door";
  }

  /**
   * No collision box, so the opening can be walked through.
   *
   * @since 1.0
   */
  @Nullable
  @Override
  public AxisAlignedBB getCollisionBoundingBox(IBlockState state, IBlockAccess source,
      BlockPos pos) {
    return NULL_AABB;
  }
}

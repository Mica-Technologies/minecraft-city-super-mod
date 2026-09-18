package com.micatechnologies.minecraft.csm.buildingmaterials;

import javax.annotation.Nullable;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;

/**
 * A hollow metal door frame: the pressed steel jambs and head set into a stud wall.
 *
 * <p>Stands slightly proud of the studs either side of it, as a real frame does, and is
 * smooth rather than knocked out — a frame is a finished component, not a structural stud.</p>
 *
 * <p>Has no collision at all. The whole point of a door frame is that something walks through it, and the
 * family's default of a full cube would have made an opening that cannot be walked through —
 * a doorway that is solid is worse than one whose jambs can be stepped into.</p>
 *
 * @version 1.0
 * @since 2026.9
 */
public class BlockHollowMetalDoorFrame extends AbstractBlockSteelFraming {

  @Override
  public String getBlockRegistryName() {
    return "hollow_metal_door_frame";
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

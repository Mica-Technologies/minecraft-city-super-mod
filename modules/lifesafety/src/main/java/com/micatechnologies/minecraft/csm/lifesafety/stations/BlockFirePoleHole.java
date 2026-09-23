package com.micatechnologies.minecraft.csm.lifesafety.stations;

import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

/**
 * The floor a fire pole passes through: a full floor block with a hole round the pole wide enough
 * to drop through (ten pixels, and a player is under ten), and the pole through it. Walk onto the
 * rim and it holds you like floor; step into the hole and you slide as on the pole.
 *
 * @since 2026.9
 */
public class BlockFirePoleHole extends BlockFirePole {

  /** The rim round the hole, as collision boxes. */
  private static final AxisAlignedBB[] RIM = {
      new AxisAlignedBB(0, 0, 0, 3 / 16.0, 1, 1),
      new AxisAlignedBB(13 / 16.0, 0, 0, 1, 1, 1),
      new AxisAlignedBB(3 / 16.0, 0, 0, 13 / 16.0, 1, 3 / 16.0),
      new AxisAlignedBB(3 / 16.0, 0, 13 / 16.0, 13 / 16.0, 1, 1)};

  @Override
  public String getBlockRegistryName() {
    return "fire_pole_hole";
  }

  @Override
  public AxisAlignedBB getBlockBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
    return FULL_BLOCK_AABB;
  }

  @Override
  @Nullable
  @SuppressWarnings("deprecation")
  public AxisAlignedBB getCollisionBoundingBox(IBlockState state, IBlockAccess source,
      BlockPos pos) {
    return FULL_BLOCK_AABB;
  }

  @Override
  @SuppressWarnings("deprecation")
  public void addCollisionBoxToList(@Nonnull IBlockState state, @Nonnull World world,
      @Nonnull BlockPos pos, @Nonnull AxisAlignedBB entityBox,
      @Nonnull List<AxisAlignedBB> collidingBoxes, @Nullable Entity entity, boolean actual) {
    for (AxisAlignedBB box : RIM) {
      addCollisionBoxToList(pos, entityBox, collidingBoxes, box);
    }
  }

  @Override
  public boolean isPassable(IBlockAccess world, BlockPos pos) {
    return false;
  }
}

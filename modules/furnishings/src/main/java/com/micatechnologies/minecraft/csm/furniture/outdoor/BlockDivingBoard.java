package com.micatechnologies.minecraft.csm.furniture.outdoor;

import com.micatechnologies.minecraft.csm.furniture.residential.BlockResidentialFurniture;
import javax.annotation.Nonnull;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * A springboard for the end of a pool: a fibreglass board on a steel stand, facing whoever places
 * it, its tip running out over the water past the block's front. It is springy ({@link Bounce}):
 * landing on it throws you back up, and a jump from the front half, toward the tip, goes a good
 * deal higher than one from over the stand.
 *
 * @since 2026.9
 */
public class BlockDivingBoard extends BlockResidentialFurniture implements IBouncy {

  /** What a jump adds over the stand, and at the tip, in blocks a tick. */
  private static final double BOOST_STAND = 0.15;
  private static final double BOOST_TIP = 0.55;

  /**
   * Constructs a diving board.
   *
   * @param registryName its registry name, ending in its finish
   * @param box          its box facing north, in sixteenths
   */
  public BlockDivingBoard(String registryName, int[] box) {
    super(registryName, box, Material.WOOD, SoundType.WOOD, 1.5F);
  }

  @Override
  public double getRestitution() {
    return 0.8;
  }

  @Override
  public double getMaxLaunch() {
    return 1.2;
  }

  /** More at the tip: the front half of the block, toward the way the board faces. */
  @Override
  public double getJumpBoost(World world, BlockPos pos, Entity entity) {
    IBlockState state = world.getBlockState(pos);
    if (state.getBlock() != this) {
      return BOOST_STAND;
    }
    EnumFacing front = state.getValue(FACING);
    double along = (entity.posX - pos.getX() - 0.5) * front.getXOffset()
        + (entity.posZ - pos.getZ() - 0.5) * front.getZOffset();
    return along > 0 ? BOOST_TIP : BOOST_STAND;
  }

  @Override
  public void onFallenUpon(@Nonnull World world, @Nonnull BlockPos pos, @Nonnull Entity entity,
      float fallDistance) {
    Bounce.fallenUpon(world, pos, entity, fallDistance);
  }

  @Override
  public void onLanded(@Nonnull World world, @Nonnull Entity entity) {
    Bounce.landed(this, world, entity);
  }
}

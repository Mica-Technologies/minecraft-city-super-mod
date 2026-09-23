package com.micatechnologies.minecraft.csm.lifesafety.stations;

import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;

/**
 * The floor a fire pole passes through: a floor block with the hole round the pole, and the pole
 * through it. The hole is open -- step into it and you drop onto the pole and slide, as a real
 * pole hole is -- so it has no collision at all; the rim is only drawn. (An earlier rim that held
 * a player up left a hole ten pixels wide for a player nearly that wide, which nobody could drop
 * through.) Right-click it to grab the pole, as on the pole itself.
 *
 * @since 2026.9
 */
public class BlockFirePoleHole extends BlockFirePole {

  @Override
  public String getBlockRegistryName() {
    return "fire_pole_hole";
  }

  @Override
  public AxisAlignedBB getBlockBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
    return FULL_BLOCK_AABB;
  }
}

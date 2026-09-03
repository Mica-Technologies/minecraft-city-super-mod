package com.micatechnologies.minecraft.csm.lifesafety;

import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * Base class for fire sprinkler heads: a detector that discharges water when it finds fire.
 * <p>
 * Every sprinkler in the mod carried an identical copy of this discharge code, all of which
 * flooded the single block underneath the head — so a head could detect a fire fifteen blocks
 * away and put water nowhere near it. Discharge now goes to the fire as well, and every block
 * flooded is recorded on the head's tile entity so the panel can drain it again on reset.
 *
 * @author Mica Technologies
 * @since 2026.8
 */
public abstract class AbstractBlockFireSprinkler extends AbstractBlockFireAlarmDetector {

  @Override
  public void onFire(World world, BlockPos blockPos, IBlockState blockState, BlockPos firePos) {
    TileEntity tileEntity = world.getTileEntity(blockPos);
    TileEntityFireAlarmSensor sensor = tileEntity instanceof TileEntityFireAlarmSensor
        ? (TileEntityFireAlarmSensor) tileEntity : null;

    // Water below the head, which is what the head visibly doing something looks like...
    discharge(world, blockPos.down(), sensor);
    // ...and water on the fire it actually found, which is what puts the fire out.
    if (firePos != null) {
      discharge(world, firePos, sensor);
    }
  }

  /**
   * Floods one block, if flooding it is harmless, and remembers having done so.
   * <p>
   * Only empty space and fire are flooded. The previous implementation called
   * {@code setBlockState} unconditionally, so a sprinkler could quietly delete whatever a player
   * had built directly beneath it.
   *
   * @param world  the world
   * @param pos    the position to flood
   * @param sensor the head's tile entity, which tracks the discharge for later cleanup; may be
   *               {@code null}, in which case the water is placed but not tracked
   */
  private static void discharge(World world, BlockPos pos, TileEntityFireAlarmSensor sensor) {
    if (!world.isBlockLoaded(pos)) {
      return;
    }
    IBlockState existing = world.getBlockState(pos);
    Material material = existing.getMaterial();
    if (material == Material.WATER) {
      return;
    }
    if (material != Material.FIRE && !existing.getBlock().isReplaceable(world, pos)) {
      return;
    }
    world.setBlockState(pos, Blocks.FLOWING_WATER.getDefaultState(), 3);
    if (sensor != null) {
      sensor.addDischargedWater(pos);
    }
  }
}

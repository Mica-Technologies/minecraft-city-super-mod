package com.micatechnologies.minecraft.csm.furniture.residential;

import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;

/**
 * A ceiling fan's tile entity: {@code TileEntityCeilingFanRenderer} turns its blades while the
 * fan runs, and, as any {@link TileEntityPowerMemory}, it remembers whether redstone last
 * powered the fan.
 *
 * @since 2026.9
 */
public class TileEntityCeilingFan extends TileEntityPowerMemory {

  /** The blades reach past the block on every side. */
  @Override
  public AxisAlignedBB getRenderBoundingBox() {
    BlockPos p = getPos();
    return new AxisAlignedBB(p.getX() - 0.5, p.getY(), p.getZ() - 0.5, p.getX() + 1.5,
        p.getY() + 1, p.getZ() + 1.5);
  }
}

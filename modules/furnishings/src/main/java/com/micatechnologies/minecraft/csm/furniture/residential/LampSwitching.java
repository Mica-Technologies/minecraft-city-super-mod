package com.micatechnologies.minecraft.csm.furniture.residential;

import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * How the Residential lamps follow redstone: a lamp remembers whether it was powered
 * ({@link #POWERED}, stored), and only a change of power switches it -- on when power comes, off
 * when it goes -- so a light switch on the wall works it and a click still does in between.
 *
 * @since 2026.9
 */
public final class LampSwitching {

  /** Whether redstone powered the lamp when it last looked. */
  public static final PropertyBool POWERED = PropertyBool.create("powered");

  private LampSwitching() {
  }

  /**
   * Switches the lamp at {@code pos} if its power has changed (server side).
   *
   * @param world the world
   * @param pos   the lamp
   * @param state its state
   * @param lit   the property that says it is on
   */
  public static void follow(World world, BlockPos pos, IBlockState state, PropertyBool lit) {
    if (world.isRemote) {
      return;
    }
    boolean powered = world.isBlockPowered(pos);
    if (powered != state.getValue(POWERED)) {
      world.setBlockState(pos, state.withProperty(POWERED, powered).withProperty(lit, powered),
          3);
    }
  }
}

package com.micatechnologies.minecraft.csm.lighting;

import net.minecraft.util.EnumFacing;

/**
 * Turns a bright light's column offset with the block's facing, for the lights that opt in
 * ({@link AbstractBrightLight#isBrightLightOffsetFacingRelative()}): a post-top head cantilevered
 * to one side of its pole, whose light belongs under the head whichever way it faces.
 *
 * <p>Read relative to the facing, the X offset runs forward (the way the fixture faces, where its
 * head is) and the Z offset to its right. A light that does not opt in keeps reading them as world
 * X and Z, as every light did before.
 *
 * @since 2026.11
 */
public final class BrightLightOffset {

  private BrightLightOffset() {
  }

  /**
   * The world (x, z) offset of a light column {@code forward} blocks ahead of a fixture facing
   * {@code facing} and {@code right} blocks to its right.
   *
   * @param forward the offset along the facing
   * @param right   the offset to the facing's right (clockwise from above)
   * @param facing  a horizontal facing
   *
   * @return {@code {dx, dz}}
   */
  public static int[] worldOffset(int forward, int right, EnumFacing facing) {
    EnumFacing side = facing.rotateY();
    return new int[]{forward * facing.getXOffset() + right * side.getXOffset(),
        forward * facing.getZOffset() + right * side.getZOffset()};
  }
}

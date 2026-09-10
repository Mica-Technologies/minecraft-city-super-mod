package com.micatechnologies.minecraft.csm.trafficaccessories;

import net.minecraft.util.math.AxisAlignedBB;

/**
 * The Type II folding barricade: a two-rail panel on a pair of legs that swing out behind it.
 *
 * <p>This one does not join onto its neighbours the way the trestle barricades do. A folding
 * barricade is a self-contained unit that is carried in, opened and set down, so a row of them
 * is a row of separate devices each standing on its own legs rather than one continuous rail.
 * That is why it has no connection properties and one baked model rather than a core with ends
 * that come and go.</p>
 *
 * <p>It still carries signs and warning lights, which is the whole reason it shares a base class
 * with the trestle barricades. What it has to correct is WHERE: its panel is narrower than a
 * cell and, because its legs trail off behind it, sits forward of the block's axis rather than
 * on it.</p>
 *
 * @version 1.0
 * @since 2026.9
 */
public class BlockWorkZoneBarricadeFolding extends AbstractBlockWorkZoneBarricade {

  /**
   * Constructs a {@link BlockWorkZoneBarricadeFolding} instance.
   *
   * @param registryName the registry name of the barricade
   * @param topY         the height of its uprights, in 1/16 block units
   * @param boundingBox  the bounding box of the barricade, in block space
   *
   * @since 1.0
   */
  public BlockWorkZoneBarricadeFolding(String registryName, float topY,
      AxisAlignedBB boundingBox) {
    super(registryName, topY, boundingBox);
  }

  @Override
  public float getLeftUprightX() {
    return BarricadeGeometry.FOLDING_LEFT_UPRIGHT_X;
  }

  @Override
  public float getRightUprightX() {
    return BarricadeGeometry.FOLDING_RIGHT_UPRIGHT_X;
  }

  @Override
  public float getRailHalfZ() {
    return BarricadeGeometry.FOLDING_RAIL_HALF_Z;
  }

  @Override
  public float getRailCentreZ() {
    return BarricadeGeometry.FOLDING_RAIL_CENTRE_Z;
  }
}

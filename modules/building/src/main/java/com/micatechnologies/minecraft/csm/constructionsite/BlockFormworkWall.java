package com.micatechnologies.minecraft.csm.constructionsite;

import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.util.math.AxisAlignedBB;

/**
 * Wall formwork: concrete cast between two plywood faces, with walers and form-tie ends.
 *
 * <p>Set out across the placer's line of sight, as a form is set out along the line of the wall
 * you are facing. Runs and stacks: every part is the full length and height of its cell. The
 * model comes from {@code dev-env-utils/scripts/gen_formwork.py}.</p>
 *
 * @version 1.0
 * @since 2026.9
 */
public class BlockFormworkWall extends AbstractBlockSiteAxial {

  /** From the outside of one row of form-tie cones to the other, running along x. */
  private static final AxisAlignedBB RUN_BOX = new AxisAlignedBB(0.0, 0.0, 0.75 / 16.0, 1.0, 1.0,
      15.25 / 16.0);

  /**
   * Constructs a {@link BlockFormworkWall}.
   *
   * @since 1.0
   */
  public BlockFormworkWall() {
    super(Material.WOOD, SoundType.WOOD, "axe");
  }

  @Override
  public String getBlockRegistryName() {
    return "formwork_wall";
  }

  @Override
  protected AxisAlignedBB getRunBox() {
    return RUN_BOX;
  }

  @Override
  protected boolean runsAlongLook() {
    return false;
  }
}

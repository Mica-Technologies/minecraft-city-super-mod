package com.micatechnologies.minecraft.csm.constructionsite;

import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.util.math.AxisAlignedBB;

/**
 * A bundle of rebar lying on two timbers, banded with wire.
 *
 * <p>Laid along the placer's line of sight, as a bundle is set down in front of you. The bars run
 * the full length of the cell, so bundles laid end to end read as one. The model comes from
 * {@code dev-env-utils/scripts/gen_formwork.py}.</p>
 *
 * @version 1.0
 * @since 2026.9
 */
public class BlockRebarBundle extends AbstractBlockSiteAxial {

  /** The dunnage and the bundle on it, running along x. */
  private static final AxisAlignedBB RUN_BOX = new AxisAlignedBB(0.0, 0.0, 2.0 / 16.0, 1.0,
      5.0 / 16.0, 14.0 / 16.0);

  /**
   * Constructs a {@link BlockRebarBundle}.
   *
   * @since 1.0
   */
  public BlockRebarBundle() {
    super(Material.IRON, SoundType.METAL, "pickaxe");
  }

  @Override
  public String getBlockRegistryName() {
    return "rebar_bundle";
  }

  @Override
  protected AxisAlignedBB getRunBox() {
    return RUN_BOX;
  }

  @Override
  protected boolean runsAlongLook() {
    return true;
  }
}

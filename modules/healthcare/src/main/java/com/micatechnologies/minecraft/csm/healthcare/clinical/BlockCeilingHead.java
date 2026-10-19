package com.micatechnologies.minecraft.csm.healthcare.clinical;

import com.micatechnologies.minecraft.csm.healthcare.BlockHealthcareScreen;
import com.micatechnologies.minecraft.csm.healthcare.HealthcareMaterial;
import javax.annotation.Nonnull;
import net.minecraft.block.state.IBlockState;
import net.minecraft.item.ItemBlock;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * The head of a ceiling-hung piece with a screen a click turns on: the ICU pendant (its monitor)
 * and the surgical equipment boom. Metadata as a floor piece: facing in bits 0-1 (S 0, W 1, N 2,
 * E 3, the way the front faces), the screen on in bit 2. The drop column above it is
 * {@link BlockCeilingColumn} cells; see {@link CeilingHung}.
 *
 * @since 2026.10
 */
public class BlockCeilingHead extends BlockHealthcareScreen implements ICeilingHung {

  private final int lift;

  /**
   * Constructs a head.
   *
   * @param registryName its registry name
   * @param box          its box facing north, in sixteenths
   * @param lift         its working height in cells above the floor cell, when placed by hand
   */
  public BlockCeilingHead(String registryName, int[] box, int lift) {
    super(registryName, box, HealthcareMaterial.METAL, Mount.FLOOR, true);
    this.lift = lift;
  }

  @Override
  public int getLift() {
    return lift;
  }

  @Override
  protected ItemBlock createItemBlock() {
    return new CeilingHung.ItemCeilingHung(this);
  }

  @Override
  public void breakBlock(@Nonnull World world, @Nonnull BlockPos pos, @Nonnull IBlockState state) {
    CeilingHung.takeDown(world, pos, true);
    super.breakBlock(world, pos, state);
  }
}

package com.micatechnologies.minecraft.csm.healthcare.clinical;

import com.micatechnologies.minecraft.csm.healthcare.BlockHealthcareLight;
import javax.annotation.Nonnull;
import net.minecraft.block.state.IBlockState;
import net.minecraft.item.ItemBlock;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * A ceiling-hung light head: the trauma bay exam light. Lighting's metadata
 * ({@code facing * 4 + state}, see {@link BlockHealthcareLight}); the drop column above it is
 * {@link BlockCeilingColumn} cells ({@link CeilingHung}).
 *
 * @since 2026.10
 */
public class BlockCeilingHeadLight extends BlockHealthcareLight implements ICeilingHung {

  private final int lift;

  /**
   * Constructs a light head.
   *
   * @param registryName its registry name
   * @param box          its box facing north, in sixteenths
   * @param lightLevel   the light it gives when lit
   * @param lift         its working height in cells above the floor cell, when placed by hand
   */
  public BlockCeilingHeadLight(String registryName, int[] box, int lightLevel, int lift) {
    super(registryName, box, lightLevel);
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

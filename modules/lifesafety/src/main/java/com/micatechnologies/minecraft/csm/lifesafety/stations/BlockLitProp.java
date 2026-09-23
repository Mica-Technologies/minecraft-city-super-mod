package com.micatechnologies.minecraft.csm.lifesafety.stations;

import com.micatechnologies.minecraft.csm.lifesafety.fireprotection.BlockFireProtectionProp;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;

/**
 * A prop that is always lit: the blue lamp over a police station's door, a lit sign. The light
 * level is given with the box, from the tab line.
 *
 * @since 2026.9
 */
public class BlockLitProp extends BlockFireProtectionProp {

  private final int light;

  public BlockLitProp(String registryName, int[] box, int light) {
    super(registryName, box, false);
    this.light = light;
  }

  @Override
  public int getLightValue(IBlockState state, IBlockAccess world, BlockPos pos) {
    return light;
  }
}

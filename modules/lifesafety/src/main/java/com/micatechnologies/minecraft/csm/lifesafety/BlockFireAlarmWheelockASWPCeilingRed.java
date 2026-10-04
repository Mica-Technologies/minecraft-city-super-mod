package com.micatechnologies.minecraft.csm.lifesafety;

import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;

/**
 * The Wheelock ASWP ceiling horn strobe in red: the AS horn strobe's face on its square WPBB
 * weatherproof back box, for a ceiling open to the weather (a parking garage deck, a loading dock
 * canopy). It sounds, flashes, links and changes sound exactly as the indoor AS does; only the
 * model differs, the face standing a quarter block off the ceiling on its gasketed box.
 *
 * @since 2026.10
 */
public class BlockFireAlarmWheelockASWPCeilingRed extends BlockFireAlarmWheelockASRed {

  @Override
  public float[] getStrobeLensFrom() {
    return new float[]{3f, 10.5f, 8f};
  }

  @Override
  public float[] getStrobeLensTo() {
    return new float[]{13f, 13.2f, 10f};
  }

  @Override
  public AxisAlignedBB getBlockBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
    return new AxisAlignedBB(0.125, 0.15625, 0.5, 0.875, 0.84375, 1.0);
  }

  @Override
  public String getBlockRegistryName() {
    return "firealarmwheelockaswpceilingred";
  }
}

package com.micatechnologies.minecraft.csm.technology;

import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;

/**
 * Powered MacBook Pro block. See {@link AbstractBlockPoweredComputer}. The lit screen
 * contributes a slightly lower light level than the larger desktops to match the smaller
 * panel.
 *
 * @author Mica Technologies
 * @since 2026.5
 */
public class BlockMacBookPro extends AbstractBlockPoweredComputer {

  private static final AxisAlignedBB BBOX = new AxisAlignedBB(
      0.125, 0.0, 0.3125, 0.875, 0.4375, 0.75);

  public BlockMacBookPro() {
    super(Material.ROCK, SoundType.STONE, "pickaxe", 1, 2F, 10F, 0);
  }

  @Override
  public String getBlockRegistryName() {
    return "mbp";
  }

  @Override
  public AxisAlignedBB getBlockBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
    return BBOX;
  }

  @Override
  protected int getPoweredLightLevel() {
    return 6;
  }
}

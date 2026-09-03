package com.micatechnologies.minecraft.csm.technology;

import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;

/**
 * Powered iMac block. Same bounding box and rendering as the original cosmetic factory
 * registration so existing world placements look identical until the player toggles the
 * screen on.
 *
 * @author Mica Technologies
 * @since 2026.5
 */
public class BlockImac extends AbstractBlockPoweredComputer {

  private static final AxisAlignedBB BBOX = new AxisAlignedBB(
      -0.573102, 0.0, 0.117500, 1.4375, 1.421875, 1.439273);

  public BlockImac() {
    super(Material.ROCK, SoundType.STONE, "pickaxe", 1, 2F, 10F, 0);
  }

  @Override
  public String getBlockRegistryName() {
    return "imac";
  }

  @Override
  public AxisAlignedBB getBlockBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
    return BBOX;
  }
}

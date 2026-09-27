package com.micatechnologies.minecraft.csm.parks.landscape;

import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;

/**
 * A row of crop plants on a ridge of tilled soil, which faces so a farm can lay its rows out
 * either way: a plant, walked through and broken by hand, that burns
 * ({@code gen_park_plantings.py}).
 *
 * @since 2026.9
 */
public class BlockParkCrop extends BlockParkFacing {

  /**
   * Constructs a crop row.
   *
   * @param registryName its registry name
   * @param box          its box facing north, in sixteenths: {x0, y0, z0, x1, y1, z1}
   */
  public BlockParkCrop(String registryName, int[] box) {
    super(Material.PLANTS, SoundType.PLANT, null, 0, 0.2F, 0.4F, registryName, box, false);
  }

  @Override
  public int getFlammability(IBlockAccess world, BlockPos pos, EnumFacing face) {
    return 60;
  }

  @Override
  public int getFireSpreadSpeed(IBlockAccess world, BlockPos pos, EnumFacing face) {
    return 30;
  }
}

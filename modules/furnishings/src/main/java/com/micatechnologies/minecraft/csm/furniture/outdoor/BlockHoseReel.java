package com.micatechnologies.minecraft.csm.furniture.outdoor;

import com.micatechnologies.minecraft.csm.furniture.residential.BlockResidentialFurniture;
import com.micatechnologies.minecraft.csm.furniture.residential.IWaterTap;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * A garden hose reel on the wall, its green hose wound on the drum and its nozzle hanging down,
 * over the outside tap it is plumbed to. The tap works ({@link IWaterTap}): an empty bucket or a
 * glass bottle held to it comes back full, and an appliance that uses water standing beside it
 * is plumbed in.
 *
 * @since 2026.9
 */
public class BlockHoseReel extends BlockResidentialFurniture implements IWaterTap {

  /**
   * Constructs a hose reel.
   *
   * @param registryName its registry name, ending in its colour
   * @param box          its box facing north, in sixteenths, its back on the wall at +Z
   */
  public BlockHoseReel(String registryName, int[] box) {
    super(registryName, box, Material.WOOD, SoundType.METAL, 1.0F);
  }

  @Override
  public boolean onBlockActivated(World world, BlockPos pos, IBlockState state,
      EntityPlayer player, EnumHand hand, EnumFacing side, float hitX, float hitY, float hitZ) {
    return !player.isSneaking() && IWaterTap.fillAtTap(world, pos, player, hand);
  }
}

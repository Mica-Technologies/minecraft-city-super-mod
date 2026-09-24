package com.micatechnologies.minecraft.csm.novelties;

import com.micatechnologies.minecraft.csm.codeutils.AbstractBlockRotatableNSEW;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

/**
 * A 1990s Sony Dream Machine clock radio, its display blinking 12:00 as one does after a power
 * cut. Right-clicking it sounds its alarm.
 *
 * @since 2026.9
 */
public class BlockSonyDreamMachine extends AbstractBlockRotatableNSEW {

  private static final AxisAlignedBB BOUNDING_BOX =
      new AxisAlignedBB(2.5 / 16, 0, 4 / 16.0, 13.5 / 16, 11 / 16.0, 12.5 / 16);

  public BlockSonyDreamMachine() {
    super(Material.ROCK, SoundType.STONE, "pickaxe", 1, 1F, 10F, 0F, 0);
  }

  @Override
  public String getBlockRegistryName() {
    return "sony_dream_machine";
  }

  @Override
  public AxisAlignedBB getBlockBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
    return BOUNDING_BOX;
  }

  @Override
  public boolean getBlockIsOpaqueCube(IBlockState state) {
    return false;
  }

  @Override
  public boolean getBlockIsFullCube(IBlockState state) {
    return false;
  }

  @Override
  public boolean getBlockConnectsRedstone(IBlockState state, IBlockAccess access, BlockPos pos,
      @Nullable EnumFacing facing) {
    return false;
  }

  @Nonnull
  @Override
  public BlockRenderLayer getBlockRenderLayer() {
    return BlockRenderLayer.SOLID;
  }

  @Override
  public boolean onBlockActivated(World world, BlockPos pos, IBlockState state,
      EntityPlayer player, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
    if (!world.isRemote) {
      SoundEvent sound = FurnishingsSounds.SONY_DREAM_MACHINE_1980S.getSoundEvent();
      if (sound != null) {
        world.playSound(null, pos, sound, SoundCategory.BLOCKS, 1.0F, 1.0F);
      }
    }
    return true;
  }
}

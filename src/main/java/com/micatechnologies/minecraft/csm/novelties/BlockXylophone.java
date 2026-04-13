package com.micatechnologies.minecraft.csm.novelties;

import com.micatechnologies.minecraft.csm.codeutils.AbstractBlockRotatableNSEWUD;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

public class BlockXylophone extends AbstractBlockRotatableNSEWUD {

  public BlockXylophone() {
    super(Material.ROCK, SoundType.STONE, "pickaxe", 1, 2F, 10F, 0F, 0);
  }

  @Override
  public String getBlockRegistryName() {
    return "xylophone";
  }

  @Override
  public AxisAlignedBB getBlockBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
    return new AxisAlignedBB(-0.025000, 0.000000, 0.162500, 0.975000, 0.150000, 0.787500);
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
    return BlockRenderLayer.CUTOUT_MIPPED;
  }

  private static final int[] NOTES = {0, 4, 7, 12, 16, 19};

  @Override
  public boolean onBlockActivated(World world, BlockPos pos, IBlockState state,
      EntityPlayer player, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
    EnumFacing blockFacing = state.getValue(FACING);
    float position;
    if (blockFacing == EnumFacing.EAST || blockFacing == EnumFacing.WEST) {
      position = hitZ;
    } else {
      position = hitX;
    }
    int barIndex = Math.min(5, Math.max(0, (int) (position * 6)));
    int note = NOTES[barIndex];
    float pitch = (float) Math.pow(2.0, (note - 12) / 12.0);
    world.playSound(null, pos, SoundEvents.BLOCK_NOTE_XYLOPHONE, SoundCategory.BLOCKS, 3.0F,
        pitch);
    if (world.isRemote) {
      double color = (double) note / 24.0;
      world.spawnParticle(EnumParticleTypes.NOTE, pos.getX() + 0.5, pos.getY() + 0.5,
          pos.getZ() + 0.5, color, 0.0, 0.0);
    }
    return true;
  }
}

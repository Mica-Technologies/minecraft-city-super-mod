package com.micatechnologies.minecraft.csm.furniture.residential;

import com.micatechnologies.minecraft.csm.novelties.FurnishingsSounds;
import java.util.Random;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

/**
 * A doorbell push on the wall beside a door. Pressed, it rings the house's ding-dong (a
 * synthesised chime) and, like a stone button, gives a redstone pulse for a second
 * ({@link #POWERED}, stored), so it can also work a lamp, a note block or a bell of the
 * player's own. It cannot be pressed again until it springs back.
 *
 * @since 2026.9
 */
public class BlockDoorbell extends BlockResidentialFurniture {

  /** Whether it is pressed in. */
  public static final PropertyBool POWERED = PropertyBool.create("powered");

  /** How long the pulse lasts, in ticks: a stone button's. */
  private static final int PULSE_TICKS = 20;

  /**
   * Constructs a doorbell.
   *
   * @param registryName its registry name, ending in its finish
   * @param box          its box facing north, in sixteenths
   */
  public BlockDoorbell(String registryName, int[] box) {
    super(registryName, box, Material.CIRCUITS, SoundType.STONE, 0.5F);
    setDefaultState(getDefaultState().withProperty(POWERED, false));
  }

  @Override
  @Nonnull
  protected BlockStateContainer createBlockState() {
    return new BlockStateContainer(this, FACING, POWERED);
  }

  @Override
  @Nonnull
  public IBlockState getStateFromMeta(int meta) {
    return super.getStateFromMeta(meta & 3).withProperty(POWERED, (meta & 4) != 0);
  }

  @Override
  public int getMetaFromState(IBlockState state) {
    return super.getMetaFromState(state) | (state.getValue(POWERED) ? 4 : 0);
  }

  @Override
  @Nonnull
  public IBlockState getStateForPlacement(World world, BlockPos pos, EnumFacing facing,
      float hitX, float hitY, float hitZ, int meta, EntityLivingBase placer) {
    return super.getStateForPlacement(world, pos, facing, hitX, hitY, hitZ, meta, placer)
        .withProperty(POWERED, false);
  }

  @Override
  public boolean onBlockActivated(World world, BlockPos pos, IBlockState state,
      EntityPlayer player, EnumHand hand, EnumFacing side, float hitX, float hitY, float hitZ) {
    if (player.isSneaking()) {
      return false;
    }
    if (state.getValue(POWERED)) {
      return true;
    }
    if (!world.isRemote) {
      world.setBlockState(pos, state.withProperty(POWERED, true), 3);
      world.playSound(null, pos, SoundEvents.BLOCK_STONE_BUTTON_CLICK_ON, SoundCategory.BLOCKS,
          0.3F, 1.5F);
      SoundEvent chime = FurnishingsSounds.DOORBELL_CHIME.getSoundEvent();
      if (chime != null) {
        world.playSound(null, pos, chime, SoundCategory.BLOCKS, 1.0F, 1.0F);
      }
      BlockLightSwitch.notifyPowered(world, pos, state);
      world.scheduleUpdate(pos, this, PULSE_TICKS);
    }
    return true;
  }

  /** It springs back, and the pulse ends. */
  @Override
  public void updateTick(World world, BlockPos pos, IBlockState state, Random rand) {
    if (!world.isRemote && state.getValue(POWERED)) {
      world.setBlockState(pos, state.withProperty(POWERED, false), 3);
      BlockLightSwitch.notifyPowered(world, pos, state);
      world.playSound(null, pos, SoundEvents.BLOCK_STONE_BUTTON_CLICK_OFF, SoundCategory.BLOCKS,
          0.2F, 1.6F);
    }
  }

  @Override
  public void breakBlock(@Nonnull World world, @Nonnull BlockPos pos,
      @Nonnull IBlockState state) {
    if (state.getValue(POWERED)) {
      BlockLightSwitch.notifyPowered(world, pos, state);
    }
    super.breakBlock(world, pos, state);
  }

  @Override
  @SuppressWarnings("deprecation")
  public boolean canProvidePower(@Nonnull IBlockState state) {
    return true;
  }

  @Override
  @SuppressWarnings("deprecation")
  public int getWeakPower(@Nonnull IBlockState state, @Nonnull IBlockAccess world,
      @Nonnull BlockPos pos, @Nonnull EnumFacing side) {
    return state.getValue(POWERED) ? 15 : 0;
  }

  @Override
  @SuppressWarnings("deprecation")
  public int getStrongPower(@Nonnull IBlockState state, @Nonnull IBlockAccess world,
      @Nonnull BlockPos pos, @Nonnull EnumFacing side) {
    return state.getValue(POWERED) && state.getValue(FACING) == side ? 15 : 0;
  }

  @Override
  public boolean getBlockConnectsRedstone(IBlockState state, IBlockAccess access, BlockPos pos,
      @Nullable EnumFacing facing) {
    return true;
  }

  @Override
  @Nonnull
  public BlockRenderLayer getBlockRenderLayer() {
    return BlockRenderLayer.CUTOUT;
  }
}

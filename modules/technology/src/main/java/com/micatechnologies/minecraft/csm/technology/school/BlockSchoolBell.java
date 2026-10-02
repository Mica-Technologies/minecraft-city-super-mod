package com.micatechnologies.minecraft.csm.technology.school;

import com.micatechnologies.minecraft.csm.codeutils.CsmBlockStateContainer;
import com.micatechnologies.minecraft.csm.technology.TechnologySounds;
import java.util.Random;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.block.Block;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

/**
 * The hallway bell: a red dome gong on the wall. It rings while it is powered by redstone, the
 * ring played again every {@link #RING_TICKS} ticks for as long as the power stays on, and when
 * a linked bell schedule controller rings a class-change tone ({@link BellTone#ringsBells()}).
 *
 * <p>No tile entity: the controller recognises a bell by its block, and the redstone ring is a
 * scheduled block tick. {@link #POWERED} is stored with the facing, only to tell a rising edge
 * from a neighbour changing; the model is the same either way.</p>
 *
 * @since 2026.10
 */
public class BlockSchoolBell extends BlockSchoolFixture {

  /** Whether the bell is powered. */
  public static final PropertyBool POWERED = PropertyBool.create("powered");

  /** The ring's length, in ticks: it starts again after this while the power is on. */
  static final int RING_TICKS = 60;

  /** How loud a redstone ring is (vanilla volume: it carries 16 blocks a unit). */
  private static final float VOLUME = 2.5F;

  /**
   * Constructs the bell.
   *
   * @param registryName its registry name
   * @param box          its box facing north, in sixteenths
   */
  public BlockSchoolBell(String registryName, double[] box) {
    super(registryName, box);
    setDefaultState(getDefaultState().withProperty(POWERED, false));
  }

  @Override
  @Nonnull
  protected BlockStateContainer createBlockState() {
    return new CsmBlockStateContainer(this, FACING, POWERED);
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
  public void onBlockAdded(World world, BlockPos pos, IBlockState state) {
    super.onBlockAdded(world, pos, state);
    update(world, pos, state);
  }

  @Override
  @SuppressWarnings("deprecation")
  public void neighborChanged(IBlockState state, World world, BlockPos pos, Block block,
      BlockPos fromPos) {
    update(world, pos, state);
  }

  private void update(World world, BlockPos pos, IBlockState state) {
    if (world.isRemote) {
      return;
    }
    boolean powered = world.isBlockPowered(pos);
    if (powered == state.getValue(POWERED)) {
      return;
    }
    world.setBlockState(pos, state.withProperty(POWERED, powered), 2);
    if (powered) {
      ring(world, pos);
      world.scheduleUpdate(pos, this, RING_TICKS);
    }
  }

  @Override
  public void updateTick(World world, BlockPos pos, IBlockState state, Random rand) {
    if (!world.isRemote && state.getValue(POWERED) && world.isBlockPowered(pos)) {
      ring(world, pos);
      world.scheduleUpdate(pos, this, RING_TICKS);
    }
  }

  private static void ring(World world, BlockPos pos) {
    SoundEvent sound = TechnologySounds.SCHOOL_BELL_RING.getSoundEvent();
    if (sound != null) {
      world.playSound(null, pos, sound, SoundCategory.BLOCKS, VOLUME, 1.0F);
    }
  }

  @Override
  public boolean getBlockConnectsRedstone(IBlockState state, IBlockAccess access, BlockPos pos,
      @Nullable EnumFacing facing) {
    return true;
  }
}

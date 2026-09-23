package com.micatechnologies.minecraft.csm.lifesafety.stations;

import com.micatechnologies.minecraft.csm.lifesafety.LifeSafetySounds;
import com.micatechnologies.minecraft.csm.lifesafety.fireprotection.BlockFireProtectionProp;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.block.Block;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

/**
 * The firehouse's brass alarm gong. It strikes its signal (three, three and three) when a
 * redstone signal comes on, or when it is clicked. The station alerting controller's relay can
 * drive it, as can a button by the watch desk.
 *
 * <p>Stored in metadata: the facing and whether it is powered, so it strikes on the rising edge of
 * a signal and not again until the signal has gone off.</p>
 *
 * @since 2026.9
 */
public class BlockStationBell extends BlockFireProtectionProp {

  public static final PropertyBool POWERED = PropertyBool.create("powered");

  public BlockStationBell(String registryName, int[] box) {
    super(registryName, box, false);
    setDefaultState(blockState.getBaseState().withProperty(FACING, EnumFacing.NORTH)
        .withProperty(POWERED, false));
  }

  @Override
  @Nonnull
  protected BlockStateContainer createBlockState() {
    return new BlockStateContainer(this, FACING, POWERED);
  }

  @Override
  public int getMetaFromState(IBlockState state) {
    return super.getMetaFromState(state) | (state.getValue(POWERED) ? 4 : 0);
  }

  @Override
  @Nonnull
  public IBlockState getStateFromMeta(int meta) {
    return super.getStateFromMeta(meta & 3).withProperty(POWERED, (meta & 4) != 0);
  }

  @Override
  @SuppressWarnings("deprecation")
  public void neighborChanged(IBlockState state, World world, BlockPos pos, Block block,
      BlockPos fromPos) {
    if (world.isRemote) {
      return;
    }
    boolean powered = world.isBlockPowered(pos);
    if (powered != state.getValue(POWERED)) {
      world.setBlockState(pos, state.withProperty(POWERED, powered), 2);
      if (powered) {
        strike(world, pos);
      }
    }
  }

  @Override
  public boolean onBlockActivated(World world, BlockPos pos, IBlockState state,
      EntityPlayer player, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
    if (hand == EnumHand.MAIN_HAND && !world.isRemote) {
      strike(world, pos);
    }
    return true;
  }

  /** Plays the gong's signal from this block, heard across a station. */
  public static void strike(World world, BlockPos pos) {
    SoundEvent sound = LifeSafetySounds.STATION_BELL.getSoundEvent();
    if (sound != null) {
      world.playSound(null, pos, sound, SoundCategory.BLOCKS, 3.0F, 1.0F);
    }
  }

  @Override
  public boolean getBlockConnectsRedstone(IBlockState state, IBlockAccess access, BlockPos pos,
      @Nullable EnumFacing facing) {
    return true;
  }
}

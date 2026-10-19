package com.micatechnologies.minecraft.csm.healthcare.codeblue;

import com.micatechnologies.minecraft.csm.healthcare.BlockHealthcare;
import com.micatechnologies.minecraft.csm.healthcare.HealthcareMaterial;
import com.micatechnologies.minecraft.csm.healthcare.HealthcareSounds;
import java.util.Random;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.block.Block;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

/**
 * The code blue annunciator ({@code code_blue_annunciator}): a wall unit with a blue strobe and a
 * chime speaker, sounding while a linked code blue button is active or while it is powered by
 * redstone.
 *
 * <p>The strobe is an animated texture swapped in by the {@code active} state, so nothing ticks
 * and nothing is drawn per frame; the unit also gives a little light while active. The chime is
 * played once as the unit goes active and again every {@link #CHIME_TICKS} ticks while it stays
 * active, from a scheduled block tick that each chime schedules again: no tile entity, nothing
 * ticking while it is quiet.</p>
 *
 * <p><b>Metadata</b>: the facing out of the wall in bits 0-2 (N 2, S 3, W 4, E 5), bit 3 active
 * (worked out, not set: a {@code /setblock} with it is put right at once). A sneak-click resets
 * the code (every active button linked to it); it gives no redstone signal, so it cannot hold
 * itself on.</p>
 *
 * @since 2026.10
 */
public class BlockCodeBlueAnnunciator extends BlockHealthcare {

  /** Sounding. */
  public static final PropertyBool ACTIVE = PropertyBool.create("active");

  /** From one chime to the next, in ticks (the chime is 2.25 s). */
  public static final int CHIME_TICKS = 70;
  /** The light it gives while active. */
  private static final int LIGHT = 6;

  private static final PropertyBool[] FLAGS = {ACTIVE};

  public BlockCodeBlueAnnunciator(String registryName, int[] box) {
    super(registryName, box, HealthcareMaterial.PLASTIC, Mount.WALL, false);
  }

  @Override
  protected PropertyBool[] storedFlags() {
    return FLAGS;
  }

  /**
   * Puts the unit right: active while called or powered. Going active chimes and starts the
   * repeat; going quiet lets the pending repeat find it quiet and stop.
   *
   * @param world  the world
   * @param pos    the unit
   * @param state  its state
   * @param called whether an active button is linked to it
   */
  void update(World world, BlockPos pos, IBlockState state, boolean called) {
    boolean on = called || world.isBlockPowered(pos);
    if (on == state.getValue(ACTIVE)) {
      return;
    }
    world.setBlockState(pos, state.withProperty(ACTIVE, on), 3);
    if (on) {
      chime(world, pos);
      if (!world.isBlockTickPending(pos, this)) {
        world.scheduleBlockUpdate(pos, this, CHIME_TICKS, 0);
      }
    }
  }

  private static void chime(World world, BlockPos pos) {
    SoundEvent chime = HealthcareSounds.CODE_BLUE_CHIME.getSoundEvent();
    if (chime != null) {
      world.playSound(null, pos, chime, SoundCategory.BLOCKS, 1.6F, 1.0F);
    }
  }

  @Override
  public void updateTick(World world, BlockPos pos, IBlockState state, Random rand) {
    if (world.isRemote || !state.getValue(ACTIVE)) {
      return;
    }
    chime(world, pos);
    world.scheduleBlockUpdate(pos, this, CHIME_TICKS, 0);
  }

  @Override
  @SuppressWarnings("deprecation")
  public void neighborChanged(IBlockState state, World world, BlockPos pos, Block block,
      BlockPos fromPos) {
    if (!world.isRemote) {
      CodeBlueNetwork.get(world).refresh(world, pos);
    }
  }

  @Override
  public void onBlockAdded(@Nonnull World world, @Nonnull BlockPos pos,
      @Nonnull IBlockState state) {
    super.onBlockAdded(world, pos, state);
    if (!world.isRemote) {
      CodeBlueNetwork.get(world).refresh(world, pos);
    }
  }

  @Override
  public boolean onBlockActivated(World world, BlockPos pos, IBlockState state,
      EntityPlayer player, EnumHand hand, EnumFacing side, float hitX, float hitY, float hitZ) {
    if (!player.isSneaking()) {
      return false;
    }
    if (!world.isRemote) {
      int n = CodeBlueNetwork.get(world).reset(world, pos);
      IBlockState now = world.getBlockState(pos);
      player.sendStatusMessage(new TextComponentTranslation(
          now.getBlock() == this && now.getValue(ACTIVE) ? "csm.healthcare.codeblue.powered"
              : "csm.healthcare.codeblue.reset", n), true);
    }
    return true;
  }

  @Override
  @SuppressWarnings("deprecation")
  public int getLightValue(IBlockState state, IBlockAccess world, BlockPos pos) {
    return state.getBlock() == this && state.getValue(ACTIVE) ? LIGHT : 0;
  }

  @Override
  public boolean getBlockConnectsRedstone(IBlockState state, IBlockAccess access, BlockPos pos,
      @Nullable EnumFacing facing) {
    return true;
  }
}

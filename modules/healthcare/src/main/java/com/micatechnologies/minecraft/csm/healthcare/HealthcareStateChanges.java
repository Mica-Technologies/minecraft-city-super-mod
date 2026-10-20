package com.micatechnologies.minecraft.csm.healthcare;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IWorldEventListener;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/**
 * Hears a Healthcare block set over itself with different metadata, on the server, and hands it
 * to the block.
 *
 * <p><b>Why.</b> The game calls {@code onBlockAdded} and {@code breakBlock} only when a block
 * replaces a <i>different</i> block. A {@code /setblock} of a nurse call device over itself with
 * the calling bit set, or cleared, changes the state and nothing else: no call started or ended,
 * so a generator that placed a room quiet and then set one device calling saw nothing happen.
 * Every block change the server sends to its clients passes through a world's listeners, with
 * the state before and after, which is exactly the hook missing; this listener passes the ones
 * where the block stayed the same and the state did not to a {@link Listening} block.</p>
 *
 * <p>Cheap: a reference compare and an {@code instanceof} on each block change. A change made
 * without flag 2 (not sent to clients) is not heard; {@code /setblock} and a click both send
 * it.</p>
 *
 * @since 2026.10
 */
public final class HealthcareStateChanges implements IWorldEventListener {

  /**
   * A block that wants to hear its own state changed in place.
   */
  public interface Listening {

    /**
     * Called on the server after the block at {@code pos} went from {@code before} to
     * {@code after}, both of this block, by any means that tells clients (a click, a
     * {@code /setblock}, another block's code). It may be called for a change the block made
     * itself, so it must do nothing when the network already agrees.
     *
     * @param world  the server world
     * @param pos    the position
     * @param before the state before
     * @param after  the state now
     */
    void onStateChangedInPlace(World world, BlockPos pos, IBlockState before, IBlockState after);
  }

  private HealthcareStateChanges() {
  }

  /** Adds the listener to every server world as it loads. */
  public static void register() {
    MinecraftForge.EVENT_BUS.register(HealthcareStateChanges.class);
  }

  @SubscribeEvent
  public static void onWorldLoad(WorldEvent.Load event) {
    World world = event.getWorld();
    if (!world.isRemote) {
      world.addEventListener(new HealthcareStateChanges());
    }
  }

  @Override
  public void notifyBlockUpdate(@Nonnull World worldIn, @Nonnull BlockPos pos,
      @Nonnull IBlockState oldState, @Nonnull IBlockState newState, int flags) {
    if (oldState == newState || worldIn.isRemote) {
      return;
    }
    Block block = newState.getBlock();
    if (block instanceof Listening && oldState.getBlock() == block) {
      ((Listening) block).onStateChangedInPlace(worldIn, pos, oldState, newState);
    }
  }

  @Override
  public void notifyLightSet(@Nonnull BlockPos pos) {
  }

  @Override
  public void markBlockRangeForRenderUpdate(int x1, int y1, int z1, int x2, int y2, int z2) {
  }

  @Override
  public void playSoundToAllNearExcept(@Nullable EntityPlayer player, @Nonnull SoundEvent soundIn,
      @Nonnull SoundCategory category, double x, double y, double z, float volume, float pitch) {
  }

  @Override
  public void playRecord(@Nonnull SoundEvent soundIn, @Nonnull BlockPos pos) {
  }

  @Override
  public void spawnParticle(int particleID, boolean ignoreRange, double xCoord, double yCoord,
      double zCoord, double xSpeed, double ySpeed, double zSpeed, @Nonnull int... parameters) {
  }

  @Override
  public void spawnParticle(int id, boolean ignoreRange, boolean minimiseParticleLevel, double x,
      double y, double z, double xSpeed, double ySpeed, double zSpeed, @Nonnull int... parameters) {
  }

  @Override
  public void onEntityAdded(@Nonnull Entity entityIn) {
  }

  @Override
  public void onEntityRemoved(@Nonnull Entity entityIn) {
  }

  @Override
  public void broadcastSound(int soundID, @Nonnull BlockPos pos, int data) {
  }

  @Override
  public void playEvent(@Nullable EntityPlayer player, int type, @Nonnull BlockPos blockPosIn,
      int data) {
  }

  @Override
  public void sendBlockBreakProgress(int breakerId, @Nonnull BlockPos pos, int progress) {
  }
}

package com.micatechnologies.minecraft.csm.constructionsite;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
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
 * Rebuilds the job trailer blocks a placed or broken trailer block can change, on the client.
 *
 * <p>Whether a trailer face is drawn as the inside or the outside depends on trailer up to
 * {@link BlockJobTrailer#REACH} blocks above or below the space it faces, and the game only
 * rebuilds the blocks right beside one that changed. Laying a floor under walls that cross into
 * the next sixteen-block section, or roofing them over, would leave the walls in that section
 * drawn as they were until something else rebuilt it. So each client world gets this listener,
 * which, when a trailer block comes or goes, rebuilds the columns round it as far up and down as
 * the rule looks. Nothing is sent: every client sees the block change and does this itself.</p>
 *
 * @version 1.0
 * @since 2026.9
 */
public final class JobTrailerRenderUpdater implements IWorldEventListener {

  private JobTrailerRenderUpdater() {
  }

  /**
   * Adds the listener to every client world as it loads.
   *
   * @since 1.0
   */
  public static void register() {
    MinecraftForge.EVENT_BUS.register(JobTrailerRenderUpdater.class);
  }

  @SubscribeEvent
  public static void onWorldLoad(WorldEvent.Load event) {
    World world = event.getWorld();
    if (world.isRemote) {
      world.addEventListener(new JobTrailerRenderUpdater());
    }
  }

  @Override
  public void notifyBlockUpdate(@Nonnull World worldIn, @Nonnull BlockPos pos,
      @Nonnull IBlockState oldState, @Nonnull IBlockState newState, int flags) {
    boolean was = BlockJobTrailer.isTrailer(oldState);
    if (was == BlockJobTrailer.isTrailer(newState)) {
      return;
    }
    int reach = BlockJobTrailer.REACH;
    worldIn.markBlockRangeForRenderUpdate(pos.add(-1, -reach, -1), pos.add(1, reach, 1));
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

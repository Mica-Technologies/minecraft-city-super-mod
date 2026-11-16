package com.micatechnologies.minecraft.csm.buildingmaterials;

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
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * Rebuilds a glazing window's whole stack when one of its blocks changes, on the client.
 *
 * <p>A window's look depends on the stack it is in: its height and its place in it lay out the
 * sashes and meeting rail, and the foot's and the top's stored settings are read by every block of
 * it. The game rebuilds only the blocks right beside a changed one, so a block two up, in the next
 * sixteen-block section, would keep its old look. So each client world gets this listener, which,
 * when a window block comes, goes or has its tile entity synced (each of which notifies a block
 * update), rebuilds the column as far as a stack reaches. Nothing is sent: every client does this
 * itself. The copy of {@code JobTrailerRenderUpdater}'s approach.</p>
 *
 * @version 1.0
 * @since 2026.11
 */
@SideOnly(Side.CLIENT)
public final class GlazingWindowRenderUpdater implements IWorldEventListener {

  private GlazingWindowRenderUpdater() {
  }

  /** Adds the listener to every client world as it loads. */
  static void register() {
    MinecraftForge.EVENT_BUS.register(GlazingWindowRenderUpdater.class);
  }

  @SubscribeEvent
  public static void onWorldLoad(WorldEvent.Load event) {
    World world = event.getWorld();
    if (world.isRemote) {
      world.addEventListener(new GlazingWindowRenderUpdater());
    }
  }

  @Override
  public void notifyBlockUpdate(@Nonnull World worldIn, @Nonnull BlockPos pos,
      @Nonnull IBlockState oldState, @Nonnull IBlockState newState, int flags) {
    if (!(oldState.getBlock() instanceof BlockGlazingWindow)
        && !(newState.getBlock() instanceof BlockGlazingWindow)) {
      return;
    }
    int reach = WindowLayout.MAX_STACK;
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

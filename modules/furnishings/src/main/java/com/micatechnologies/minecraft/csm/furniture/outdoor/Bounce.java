package com.micatechnologies.minecraft.csm.furniture.outdoor;

import com.micatechnologies.minecraft.csm.novelties.FurnishingsSounds;
import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import net.minecraftforge.event.entity.living.LivingEvent.LivingJumpEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/**
 * The bounce of an {@link IBouncy} block, shared by the trampoline, the bounce castle and the
 * diving board.
 *
 * <ul>
 *   <li><b>Landing</b> ({@link #landed}, from {@code onLanded}): something coming down faster
 *   than a step sends it back up at {@link IBouncy#getRestitution()} of its speed, as slime does,
 *   so a bounce dies away over a few hops. Sneaking lands dead.</li>
 *   <li><b>Falling</b> ({@link #fallenUpon}, from {@code onFallenUpon}): no fall damage from any
 *   height, and a quiet spring's boing from a fall of more than a block.</li>
 *   <li><b>Jumping</b> ({@link #onJump}, the jump event): a jump off the block adds
 *   {@link IBouncy#getJumpBoost} to the bounce it was made on. Jumping each time you land builds
 *   the bounce up to {@link IBouncy#getMaxLaunch()}.</li>
 * </ul>
 *
 * <p>A player's movement is the client's, so both sides run this, each with its own entities;
 * the speed of the last bounce is kept per entity for the jump that may follow it, in a weak
 * map, not in the entity's saved data.</p>
 *
 * @since 2026.9
 */
public final class Bounce {

  /** Below this landing speed, in blocks a tick, a landing is a step and does not bounce. */
  private static final double MIN_SPEED = 0.2;
  /** A fall, in blocks, long enough to make the spring boing. */
  private static final float SOUND_FALL = 1.0F;
  /** How many ticks after a bounce a jump still builds on it. */
  private static final long JUMP_WINDOW = 3;

  /** Per entity: {the speed of its last bounce, the world tick it was made}. */
  private static final Map<Entity, double[]> LAST = Collections.synchronizedMap(
      new WeakHashMap<>());

  /** The jump handler; the module registers one on the event bus. */
  public Bounce() {
  }

  /**
   * Something has landed on a bouncy block: sends it back up, or stops it.
   *
   * @param bouncy the block
   * @param world  the world
   * @param entity what landed
   */
  public static void landed(IBouncy bouncy, World world, Entity entity) {
    if (entity.isSneaking() || entity.motionY >= -MIN_SPEED) {
      entity.motionY = 0.0;
      return;
    }
    double up = Math.min(bouncy.getMaxLaunch(), -entity.motionY * bouncy.getRestitution());
    if (!(entity instanceof EntityLivingBase)) {
      up *= 0.8;
    }
    entity.motionY = up;
    LAST.put(entity, new double[]{up, world.getTotalWorldTime()});
  }

  /**
   * Something has fallen onto a bouncy block: it takes no damage, and a real fall boings.
   *
   * @param world        the world
   * @param pos          the block
   * @param entity       what fell
   * @param fallDistance how far it fell, in blocks
   */
  public static void fallenUpon(World world, BlockPos pos, Entity entity, float fallDistance) {
    if (!world.isRemote && fallDistance > SOUND_FALL && !entity.isSneaking()) {
      SoundEvent boing = FurnishingsSounds.TRAMPOLINE_BOING.getSoundEvent();
      if (boing != null) {
        float volume = MathHelper.clamp(0.25F + fallDistance * 0.05F, 0.25F, 0.6F);
        world.playSound(null, pos, boing, SoundCategory.BLOCKS, volume,
            0.9F + world.rand.nextFloat() * 0.2F);
      }
    }
    entity.fall(fallDistance, 0.0F);
  }

  /**
   * A jump off a bouncy block goes higher: its boost on top of the bounce it was made on, or on
   * the jump's own speed from standing.
   *
   * @param event the jump
   */
  @SubscribeEvent
  public void onJump(LivingJumpEvent event) {
    EntityLivingBase entity = event.getEntityLiving();
    World world = entity.world;
    BlockPos pos = new BlockPos(entity.posX, entity.posY - 0.2, entity.posZ);
    IBlockState state = world.getBlockState(pos);
    if (!(state.getBlock() instanceof IBouncy) || entity.isSneaking()) {
      return;
    }
    IBouncy bouncy = (IBouncy) state.getBlock();
    if (!bouncy.isBouncy(state)) {
      return;
    }
    double base = entity.motionY;
    double[] last = LAST.get(entity);
    if (last != null && world.getTotalWorldTime() - (long) last[1] <= JUMP_WINDOW) {
      base = Math.max(base, last[0]);
    }
    entity.motionY = Math.min(bouncy.getMaxLaunch(),
        base + bouncy.getJumpBoost(world, pos, entity));
  }
}

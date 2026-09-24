package com.micatechnologies.minecraft.csm.furniture.residential;

import com.micatechnologies.minecraft.csm.novelties.FurnishingsSounds;
import net.minecraft.block.Block;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.init.Blocks;
import net.minecraft.init.SoundEvents;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;

/**
 * What a running shower does, shared by the shower head and the shower enclosure: while
 * {@link #ON}, the block ticks itself every {@link #EVERY} ticks (a scheduled block update, so
 * no tile entity and nothing ticking while it is off), and each tick the server sends one
 * particle packet of water falling from the rose and splashing where it lands; every
 * {@link #SOUND_EVERY} ticks it plays the spray's loop again, which is written to be that long
 * and to join itself without a seam. Switching it off stops both at the next tick.
 *
 * @since 2026.9
 */
public final class ShowerSpray {

  /** Whether the water is running. */
  public static final PropertyBool ON = PropertyBool.create("on");
  /** How often a running shower sprays, in ticks. */
  public static final int EVERY = 2;
  /** How often its loop is played again, in ticks: the length of the loop. */
  private static final int SOUND_EVERY = 40;
  /** How far below the rose to look for what the water lands on, in blocks. */
  private static final int FALL = 3;

  private ShowerSpray() {
  }

  /**
   * Turns the water on or off: the vanilla lever click and, turned on, the first burst of the
   * spray and its sound. The caller stores {@link #ON} and schedules the first tick.
   *
   * @param world the world
   * @param pos   the block with the rose
   * @param on    whether it is now on
   */
  public static void turned(World world, BlockPos pos, boolean on) {
    world.playSound(null, pos, SoundEvents.BLOCK_LEVER_CLICK, SoundCategory.BLOCKS, 0.3F,
        on ? 0.7F : 0.6F);
    if (on) {
      sound(world, pos);
    }
  }

  /**
   * One tick of a running shower whose rose is at {@code rose} (in sixteenths, facing north, in
   * the block {@code pos}): water from the rose, a splash where it lands, the loop again when it
   * is due.
   *
   * @param world  the world
   * @param pos    the block with the rose
   * @param facing the way the block faces
   * @param rose   the middle of the rose's underside, {x, y, z} facing north in sixteenths
   */
  public static void spray(World world, BlockPos pos, EnumFacing facing, double[] rose) {
    if (!(world instanceof WorldServer)) {
      return;
    }
    WorldServer server = (WorldServer) world;
    double x = rose[0] / 16.0 - 0.5;
    double z = rose[2] / 16.0 - 0.5;
    // Turn (x, z) from north to the block's facing, as the blockstate's y does.
    for (int i = 0; i < (facing.getHorizontalIndex() + 2) % 4; i++) {
      double t = x;
      x = -z;
      z = t;
    }
    double wx = pos.getX() + 0.5 + x;
    double wz = pos.getZ() + 0.5 + z;
    int water = Block.getStateId(Blocks.WATER.getDefaultState());
    server.spawnParticle(EnumParticleTypes.BLOCK_DUST, wx, pos.getY() + rose[1] / 16.0, wz, 8,
        0.07, 0.0, 0.07, 0.04, water);
    BlockPos below = new BlockPos(wx, pos.getY() + rose[1] / 16.0 - 0.3, wz);
    for (int i = 0; i < FALL; i++, below = below.down()) {
      if (!world.isAirBlock(below)) {
        // The enclosure's own lower half is a full block to click, but its floor is the tray.
        double top = world.getBlockState(below).getBlock() instanceof BlockShower
            ? BlockShower.TRAY_TOP : world.getBlockState(below).getBoundingBox(world, below).maxY;
        server.spawnParticle(EnumParticleTypes.WATER_SPLASH, wx, below.getY() + top, wz, 3,
            0.12, 0.0, 0.12, 0.0);
        break;
      }
    }
    if (world.getTotalWorldTime() % SOUND_EVERY < EVERY) {
      sound(world, pos);
    }
  }

  private static void sound(World world, BlockPos pos) {
    SoundEvent event = FurnishingsSounds.SHOWER_SPRAY.getSoundEvent();
    if (event != null) {
      world.playSound(null, pos, event, SoundCategory.BLOCKS, 0.6F, 1.0F);
    }
  }
}

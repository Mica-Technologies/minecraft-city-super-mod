package com.micatechnologies.minecraft.csm.buildingmaterials;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;

/**
 * Linking a wall control to a garage door: sneak-click the control, then within thirty seconds
 * sneak-click any block of the door, or its opener.
 *
 * <p>The control a player is linking is remembered per player, and separately on each side: the
 * client needs to know too, so that the second sneak-click is taken by the door rather than going
 * on to place whatever the player is holding against it. The two sides are kept apart because a
 * singleplayer game runs both in one process, and one side finishing the link must not take it
 * away from the other before it has seen the click. Nothing here is saved.</p>
 *
 * @version 1.0
 * @since 2026.9
 */
public final class GarageDoorLinks {

  /** How long a started link waits for its door, in ticks. */
  private static final long WAIT_TICKS = 600L;

  private static final Map<UUID, Pending> CLIENT = new HashMap<>();
  private static final Map<UUID, Pending> SERVER = new HashMap<>();

  private GarageDoorLinks() {
  }

  private static final class Pending {

    final int dimension;
    final BlockPos control;
    final long started;

    Pending(int dimension, BlockPos control, long started) {
      this.dimension = dimension;
      this.control = control;
      this.started = started;
    }
  }

  private static Map<UUID, Pending> side(World world) {
    return world.isRemote ? CLIENT : SERVER;
  }

  /**
   * The player sneak-clicked a control: remember it.
   *
   * @param player  the player
   * @param world   the world
   * @param control the control
   *
   * @since 1.0
   */
  static synchronized void start(EntityPlayer player, World world, BlockPos control) {
    side(world).put(player.getUniqueID(), new Pending(world.provider.getDimension(),
        control.toImmutable(), world.getTotalWorldTime()));
    if (!world.isRemote) {
      player.sendStatusMessage(new TextComponentTranslation("gui.csm.garage.link_start"), true);
    }
  }

  /**
   * The player sneak-clicked a door or an opener: finish the link they started, if they started
   * one.
   *
   * @param player the player
   * @param world  the world
   * @param target the door or opener
   *
   * @return whether a link was waiting, and so whether the click was taken
   *
   * @since 1.0
   */
  static synchronized boolean finish(EntityPlayer player, World world, BlockPos target) {
    Pending p = side(world).remove(player.getUniqueID());
    if (p == null || p.dimension != world.provider.getDimension()
        || world.getTotalWorldTime() - p.started > WAIT_TICKS) {
      return false;
    }
    if (!world.isRemote) {
      TileEntity te = world.getTileEntity(p.control);
      if (te instanceof TileEntityGarageDoorControl) {
        ((TileEntityGarageDoorControl) te).setTarget(target);
        player.sendStatusMessage(new TextComponentTranslation("gui.csm.garage.link_done"), true);
      }
    }
    return true;
  }
}

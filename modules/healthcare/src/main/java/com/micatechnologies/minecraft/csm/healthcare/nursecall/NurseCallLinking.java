package com.micatechnologies.minecraft.csm.healthcare.nursecall;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;

/**
 * Linking by hand: sneak-click a master panel or a dome light to pick it, then sneak-click each
 * dome light or device to link it there. The pick is kept per player on the server until they
 * pick another or log out. Generators link with {@code /csmhealthcare nursecall link} instead.
 *
 * @since 2026.10
 */
public final class NurseCallLinking {

  private static final Map<UUID, Pick> PICKS = new ConcurrentHashMap<>();

  private NurseCallLinking() {
  }

  private static final class Pick {
    final int dimension;
    final BlockPos pos;

    Pick(int dimension, BlockPos pos) {
      this.dimension = dimension;
      this.pos = pos;
    }
  }

  /**
   * Forgets a player's pick (on logout).
   *
   * @param player the player's id
   */
  public static void forget(UUID player) {
    PICKS.remove(player);
  }

  /**
   * A sneak-click on a nurse call block, on the server: picks a panel; links a dome light to the
   * picked panel, or picks it if no panel is picked; links a device to the picked panel or dome
   * light.
   *
   * @param world  the world
   * @param pos    the block clicked
   * @param kind   what it is
   * @param player who clicked
   */
  public static void sneakClick(World world, BlockPos pos, NurseCallNetwork.Kind kind,
      EntityPlayer player) {
    Pick pick = PICKS.get(player.getUniqueID());
    if (pick != null && (pick.dimension != world.provider.getDimension()
        || NurseCallNetwork.kindOf(world, pick.pos) == null)) {
      pick = null;
    }
    NurseCallNetwork.Kind picked = pick == null ? null : NurseCallNetwork.kindOf(world, pick.pos);
    boolean linkHere = pick != null && !pick.pos.equals(pos)
        && (kind == NurseCallNetwork.Kind.DEVICE
        || kind == NurseCallNetwork.Kind.DOME && picked == NurseCallNetwork.Kind.PANEL);
    if (linkHere) {
      try {
        NurseCallNetwork.get(world).link(world, pick.pos, pos);
        player.sendStatusMessage(new TextComponentTranslation("csm.healthcare.nursecall.linked",
            NurseCallNetwork.describe(pick.pos)), true);
      } catch (IllegalArgumentException e) {
        player.sendStatusMessage(new TextComponentTranslation(
            "csm.healthcare.nursecall.link_failed", e.getMessage()), true);
      }
      return;
    }
    if (kind == NurseCallNetwork.Kind.DEVICE) {
      player.sendStatusMessage(new TextComponentTranslation(
          "csm.healthcare.nursecall.pick_first"), true);
      return;
    }
    PICKS.put(player.getUniqueID(), new Pick(world.provider.getDimension(), pos.toImmutable()));
    player.sendStatusMessage(new TextComponentTranslation("csm.healthcare.nursecall.picked",
        NurseCallNetwork.describe(pos)), true);
  }
}

package com.micatechnologies.minecraft.csm.constructionsite;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * Lets the local player climb a scaffold or a crane mast -- any {@link ICsmSiteClimbable} --
 * by holding jump.
 *
 * <p>1.12 climbs a ladder only while the climber is pushing against something solid: the
 * ladder's own box, or the wall behind it. A scaffold has no side collision -- the player stands
 * inside it -- so walking forward in one never climbs. Vanilla added jump-to-climb for scaffolding
 * in 1.14; this is that rule, for the one player whose movement the client decides.</p>
 *
 * <p>It sets the same upward speed a ladder gives, before the player's own movement runs, and only
 * while the player's feet are inside a scaffold. The ladder handling the block already gets from
 * {@code isLadder} does everything else: a slow fall when jump is released, holding still while
 * sneaking, and no fall damage. A dedicated server needs nothing, since it does not kick a player
 * for floating while their box overlaps a block.</p>
 *
 * @version 1.0
 * @since 2026.9
 */
@SideOnly(Side.CLIENT)
public final class ScaffoldClimbHandler {

  /** A ladder's climbing speed, in blocks per tick. */
  private static final double CLIMB_SPEED = 0.2;

  private ScaffoldClimbHandler() {
  }

  /**
   * Registers the handler. Call from the client only.
   *
   * @since 1.0
   */
  public static void register() {
    MinecraftForge.EVENT_BUS.register(new ScaffoldClimbHandler());
  }

  /**
   * Before the local player moves: if their feet are in a scaffold and jump is held, climb.
   *
   * @param event the player tick
   *
   * @since 1.0
   */
  @SubscribeEvent
  public void onPlayerTick(TickEvent.PlayerTickEvent event) {
    if (event.phase != TickEvent.Phase.START || event.side != Side.CLIENT) {
      return;
    }
    EntityPlayerSP player = Minecraft.getMinecraft().player;
    if (event.player != player || player.capabilities.isFlying
        || !player.movementInput.jump) {
      return;
    }
    BlockPos feet = new BlockPos(MathHelper.floor(player.posX),
        MathHelper.floor(player.getEntityBoundingBox().minY), MathHelper.floor(player.posZ));
    if (player.world.getBlockState(feet).getBlock() instanceof ICsmSiteClimbable) {
      player.motionY = CLIMB_SPEED;
    }
  }
}

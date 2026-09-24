package com.micatechnologies.minecraft.csm.furniture.residential;

import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;

/**
 * Lays a player asleep in a {@link BlockResidentialBed} out along it. Vanilla gives a sleeper
 * the render offset that puts their feet at the foot of the bed only in a
 * {@code BlockHorizontal}, which the Residential beds are not; without it the body would lie
 * from the pillow on through the headboard. Each client tick, every player sleeping in one of
 * these beds gets the offset vanilla would give it -- their feet 1.8 blocks back from their
 * head, at the height of their own mattress (a top bunk's is not a vanilla bed's).
 *
 * <p>Registered on the Forge event bus by the module on the client only: it touches
 * {@code renderOffsetY}, which a dedicated server does not have.</p>
 *
 * @since 2026.9
 */
public class BedSleepClientHandler {

  /** How far a sleeper's feet lie from their head, in blocks: vanilla's. */
  private static final double BODY = 1.8;

  /**
   * Sets the render offset of a player sleeping in a Residential bed.
   *
   * @param event the player's tick
   */
  @SubscribeEvent
  public void onPlayerTick(TickEvent.PlayerTickEvent event) {
    if (event.phase != TickEvent.Phase.END || event.side != Side.CLIENT) {
      return;
    }
    EntityPlayer player = event.player;
    if (!player.isPlayerSleeping() || player.bedLocation == null) {
      return;
    }
    IBlockState state = player.world.getBlockState(player.bedLocation);
    if (!(state.getBlock() instanceof BlockResidentialBed)) {
      return;
    }
    BlockResidentialBed bed = (BlockResidentialBed) state.getBlock();
    Vec3d head = bed.pillow(state, player.bedLocation);
    if (head == null) {
      return;
    }
    EnumFacing dir = bed.getBedDirection(state, player.world, player.bedLocation);
    player.renderOffsetX = (float) (head.x - BODY * dir.getXOffset() - player.posX);
    player.renderOffsetY = (float) (head.y - player.posY);
    player.renderOffsetZ = (float) (head.z - BODY * dir.getZOffset() - player.posZ);
  }
}

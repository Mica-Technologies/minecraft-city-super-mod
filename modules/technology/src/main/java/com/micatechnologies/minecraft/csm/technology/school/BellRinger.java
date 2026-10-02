package com.micatechnologies.minecraft.csm.technology.school;

import com.micatechnologies.minecraft.csm.technology.CsmTechnology;
import com.micatechnologies.minecraft.csm.technology.TechnologySounds;
import com.micatechnologies.minecraft.csm.technology.TileEntitySpeaker;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.play.server.SPacketSoundEffect;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * Sounds a bell controller's ring, on the server: works out where each player hears it from and
 * sends them the sound, then pulses the controller's redstone.
 *
 * <p>Each player is sent the tone once, from the nearest linked speaker (the controller counts as
 * one) within {@link #SPEAKER_RANGE}, and the bell once, from the nearest linked hallway bell
 * within {@link #BELL_RANGE}. A school with forty speakers in it would otherwise play forty copies
 * of the same chime a few ticks apart to someone in the corridor. The packet is vanilla's own
 * sound packet, so the client plays it positioned and attenuated like any block sound. The work
 * is players times links, and happens only when a bell rings.</p>
 *
 * <p>A linked position is read only when its chunk is loaded (an unloaded one is skipped, not
 * dropped), and a loaded one that is no longer a speaker or bell is unlinked.</p>
 *
 * @since 2026.10
 */
public final class BellRinger {

  /** How far a speaker carries, in blocks (the vanilla volume that gives it). */
  static final double SPEAKER_RANGE = 24.0;
  private static final float SPEAKER_VOLUME = (float) (SPEAKER_RANGE / 16.0);
  /** How far a hallway bell carries, in blocks. */
  static final double BELL_RANGE = 40.0;
  private static final float BELL_VOLUME = (float) (BELL_RANGE / 16.0);

  private BellRinger() {
  }

  /**
   * Rings a tone at everything linked to a controller.
   *
   * @param controller the controller
   * @param tone       the tone
   */
  static void ring(TileEntityBellController controller, BellTone tone) {
    World world = controller.getWorld();
    List<BlockPos> speakers = new ArrayList<>();
    List<BlockPos> bells = new ArrayList<>();
    collect(controller, speakers, bells);
    SoundEvent toneSound = tone.getSound().getSoundEvent();
    SoundEvent bellSound = TechnologySounds.SCHOOL_BELL_RING.getSoundEvent();
    for (EntityPlayer p : world.playerEntities) {
      if (!(p instanceof EntityPlayerMP)) {
        continue;
      }
      EntityPlayerMP player = (EntityPlayerMP) p;
      send(player, nearest(player, speakers, SPEAKER_RANGE), toneSound, SPEAKER_VOLUME);
      if (tone.ringsBells()) {
        send(player, nearest(player, bells, BELL_RANGE), bellSound, BELL_VOLUME);
      }
    }
    BlockBellController.pulse(world, controller.getPos());
  }

  /**
   * Speaks an announcement to every player in hearing range of the controller or one of its
   * speakers, through Core's speech service on each client (Text to Speech's voice when that
   * module is installed, the system narrator when not).
   *
   * @param controller the controller
   * @param text       what to say
   */
  static void announce(TileEntityBellController controller, String text) {
    World world = controller.getWorld();
    if (world == null) {
      return;
    }
    List<BlockPos> speakers = new ArrayList<>();
    collect(controller, speakers, new ArrayList<>());
    BellAnnouncePacket packet = new BellAnnouncePacket(text);
    for (EntityPlayer p : world.playerEntities) {
      if (p instanceof EntityPlayerMP && nearest(p, speakers, SPEAKER_RANGE) != null) {
        CsmTechnology.NETWORK.sendTo(packet, (EntityPlayerMP) p);
      }
    }
  }

  /** The controller and its loaded linked speakers, and its loaded linked bells. */
  private static void collect(TileEntityBellController controller, List<BlockPos> speakers,
      List<BlockPos> bells) {
    World world = controller.getWorld();
    speakers.add(controller.getPos());
    List<BlockPos> stale = new ArrayList<>();
    for (BlockPos p : controller.getLinks()) {
      if (!world.isBlockLoaded(p)) {
        continue;
      }
      if (world.getBlockState(p).getBlock() instanceof BlockSchoolBell) {
        bells.add(p);
      } else if (world.getTileEntity(p) instanceof TileEntitySpeaker) {
        speakers.add(p);
      } else {
        stale.add(p);
      }
    }
    controller.pruneLinks(stale);
  }

  @Nullable
  private static BlockPos nearest(EntityPlayer player, List<BlockPos> from, double range) {
    BlockPos best = null;
    double bestSq = range * range;
    for (BlockPos p : from) {
      double d = player.getDistanceSqToCenter(p);
      if (d <= bestSq) {
        best = p;
        bestSq = d;
      }
    }
    return best;
  }

  private static void send(EntityPlayerMP player, @Nullable BlockPos at,
      @Nullable SoundEvent sound, float volume) {
    if (at == null || sound == null) {
      return;
    }
    player.connection.sendPacket(new SPacketSoundEffect(sound, SoundCategory.BLOCKS,
        at.getX() + 0.5, at.getY() + 0.5, at.getZ() + 0.5, volume, 1.0F));
  }
}

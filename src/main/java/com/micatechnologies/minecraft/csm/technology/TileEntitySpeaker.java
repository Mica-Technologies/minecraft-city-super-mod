package com.micatechnologies.minecraft.csm.technology;

import com.micatechnologies.minecraft.csm.CsmConstants;
import com.micatechnologies.minecraft.csm.CsmNetwork;
import com.micatechnologies.minecraft.csm.codeutils.AbstractTickableTileEntity;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;

/**
 * Backing tile entity for every block in the speaker family (Atlas, Bose, FourJay, JBL,
 * Valcom, etc.). A speaker has two independent activation modes:
 *
 * <ul>
 *   <li><b>TTS endpoint</b> (Phase 2a) — when bound to a {@link TileEntityRedstoneTTS} via
 *   {@link ItemTtsLinker}, the TTS module extends its broadcast radius to include this
 *   speaker's position so a player anywhere in the union of speaker coverage areas hears
 *   the announcement.</li>
 *
 *   <li><b>Standalone ambient</b> (Phase 2b) — when an ambient sound is selected (right-click
 *   to cycle) AND the speaker is redstone-powered, the speaker plays a looping
 *   {@link SpeakerAmbientSound} via MovingSound for every player within
 *   {@link #AMBIENT_HEARING_RANGE} blocks. Per-player active tracking + start/stop packets
 *   keep the audio reliable as players move into and out of range.</li>
 * </ul>
 *
 * <p>The TE pauses ticking entirely when neither mode is active, so purely cosmetic
 * placements impose no per-tick cost.</p>
 *
 * @author Mica Technologies
 * @since 2026.5
 */
public class TileEntitySpeaker extends AbstractTickableTileEntity {

  private static final String NBT_HAS_LINK = "hasLink";
  private static final String NBT_LINK_X = "linkX";
  private static final String NBT_LINK_Y = "linkY";
  private static final String NBT_LINK_Z = "linkZ";
  private static final String NBT_AMBIENT_SOUND = "ambSnd";

  /** Maximum distance (in blocks) at which a player can hear the ambient track. */
  private static final float AMBIENT_HEARING_RANGE = 24.0f;

  /**
   * Available ambient sound resources, in cycle order. The first entry is the "no ambient
   * sound selected" sentinel. Adding more sounds later is just a matter of appending to
   * this list and registering them in sounds.json + CsmSounds.
   */
  private static final List<String> AMBIENT_SOUND_CYCLE = Arrays.asList(
      "",
      "csm:mii_channel_remix");

  private BlockPos linkedTtsPos = null;
  /**
   * Currently selected ambient sound resource (e.g. "csm:mii_channel_remix"), or empty
   * string when no ambient sound is selected. Persisted in NBT.
   */
  private String ambientSound = "";

  /** Players currently hearing this speaker's ambient track; transient. */
  private final Set<UUID> ambientActivePlayers = new HashSet<>();
  /** Last-known redstone state, transient. Used to detect transitions. */
  private boolean wasPowered = false;
  /** Cached display name for chat feedback. Recomputed on cycle. */
  private transient String lastSelectedDisplayName = "";

  // region Link management (TTS endpoint mode)

  public boolean hasLink() {
    return linkedTtsPos != null;
  }

  public BlockPos getLinkedTtsPos() {
    return linkedTtsPos;
  }

  public void setLinkedTtsPos(BlockPos pos) {
    this.linkedTtsPos = pos == null ? null : pos.toImmutable();
    if (world != null && !world.isRemote) {
      markDirtySync(world, this.pos, true);
    }
  }

  public void clearLink() {
    setLinkedTtsPos(null);
  }

  // endregion

  // region Ambient mode

  public String getAmbientSound() {
    return ambientSound;
  }

  /** Returns true if this speaker has an ambient sound selected (regardless of power). */
  public boolean hasAmbientSelection() {
    return ambientSound != null && !ambientSound.isEmpty();
  }

  /**
   * Cycles the selected ambient sound to the next entry in {@link #AMBIENT_SOUND_CYCLE}.
   * If the speaker is currently broadcasting, stops the existing playback so the next tick
   * picks up the new selection.
   *
   * @return the user-facing display name of the newly selected sound (or "off")
   */
  public String cycleAmbientSound() {
    int idx = AMBIENT_SOUND_CYCLE.indexOf(ambientSound);
    if (idx < 0) idx = 0;
    int next = (idx + 1) % AMBIENT_SOUND_CYCLE.size();
    String newSel = AMBIENT_SOUND_CYCLE.get(next);
    if (!newSel.equals(this.ambientSound)) {
      // Stop existing playback for everyone — the next tick will start the new sound for
      // anyone in range, or do nothing if the new selection is the off sentinel.
      stopAmbientForAll();
      this.ambientSound = newSel;
      if (world != null && !world.isRemote) {
        markDirtySync(world, pos, true);
      }
    }
    lastSelectedDisplayName = newSel.isEmpty() ? "off" : displayNameFor(newSel);
    return lastSelectedDisplayName;
  }

  public String getLastSelectedDisplayName() {
    return lastSelectedDisplayName;
  }

  /** Friendly display name for chat — strips namespace prefix and snake-case-to-Title. */
  private static String displayNameFor(String resourceId) {
    if (resourceId == null || resourceId.isEmpty()) return "off";
    int colon = resourceId.indexOf(':');
    String path = colon >= 0 ? resourceId.substring(colon + 1) : resourceId;
    StringBuilder sb = new StringBuilder(path.length());
    boolean upper = true;
    for (int i = 0; i < path.length(); i++) {
      char c = path.charAt(i);
      if (c == '_' || c == '-') {
        sb.append(' ');
        upper = true;
      } else if (upper) {
        sb.append(Character.toUpperCase(c));
        upper = false;
      } else {
        sb.append(c);
      }
    }
    return sb.toString();
  }

  /**
   * Sends stop packets to all players currently hearing this speaker and clears the
   * active-players set. Called when the ambient sound changes, when the speaker loses
   * power, or when the TE is invalidated.
   */
  private void stopAmbientForAll() {
    if (ambientActivePlayers.isEmpty() || world == null || world.isRemote) {
      ambientActivePlayers.clear();
      return;
    }
    SpeakerAmbientPacket stopPacket = SpeakerAmbientPacket.stop(pos);
    for (Object obj : world.playerEntities) {
      if (!(obj instanceof EntityPlayerMP)) continue;
      EntityPlayerMP p = (EntityPlayerMP) obj;
      if (ambientActivePlayers.contains(p.getUniqueID())) {
        CsmNetwork.sendTo(stopPacket, p);
      }
    }
    ambientActivePlayers.clear();
  }

  /**
   * Per-tick management of the ambient broadcast. Iterates server players, sends start
   * packets to those entering range and stop packets to those leaving range. Cheap because
   * the work scales with player count, not block count, and only runs when this speaker
   * actually has an active broadcast.
   */
  private void manageAmbientBroadcast() {
    if (world == null || world.isRemote || ambientSound == null || ambientSound.isEmpty()) {
      return;
    }
    double radiusSq = AMBIENT_HEARING_RANGE * (double) AMBIENT_HEARING_RANGE;
    int dim = world.provider.getDimension();
    Set<UUID> stillActive = new HashSet<>();
    SpeakerAmbientPacket startPacket = null;

    for (Object obj : world.playerEntities) {
      if (!(obj instanceof EntityPlayerMP)) continue;
      EntityPlayerMP p = (EntityPlayerMP) obj;
      if (p.dimension != dim) continue;
      double dx = p.posX - (pos.getX() + 0.5);
      double dy = p.posY - (pos.getY() + 0.5);
      double dz = p.posZ - (pos.getZ() + 0.5);
      boolean inRange = (dx * dx + dy * dy + dz * dz) <= radiusSq;
      UUID id = p.getUniqueID();

      if (inRange) {
        stillActive.add(id);
        if (!ambientActivePlayers.contains(id)) {
          if (startPacket == null) {
            startPacket = SpeakerAmbientPacket.start(pos, ambientSound, AMBIENT_HEARING_RANGE);
          }
          CsmNetwork.sendTo(startPacket, p);
        }
      } else if (ambientActivePlayers.contains(id)) {
        CsmNetwork.sendTo(SpeakerAmbientPacket.stop(pos), p);
      }
    }

    ambientActivePlayers.clear();
    ambientActivePlayers.addAll(stillActive);
  }

  // endregion

  // region Tick

  @Override
  public boolean doClientTick() {
    return false;
  }

  /**
   * Pause ticking only when both modes are inactive — TTS link absent and no ambient
   * selection. Cosmetic-only placements (vast majority) cost nothing per tick.
   */
  @Override
  public boolean pauseTicking() {
    return linkedTtsPos == null && (ambientSound == null || ambientSound.isEmpty());
  }

  @Override
  public long getTickRate() {
    return 40;
  }

  @Override
  public void onTick() {
    if (world == null || world.isRemote) {
      return;
    }

    // TTS link validation (Phase 2a behavior).
    if (linkedTtsPos != null && world.isBlockLoaded(linkedTtsPos)) {
      TileEntity te = world.getTileEntity(linkedTtsPos);
      if (!(te instanceof TileEntityRedstoneTTS)) {
        clearLink();
      }
    }

    // Ambient mode (Phase 2b behavior). Selection drives ticking; redstone state gates
    // whether the broadcast is active this tick.
    if (ambientSound != null && !ambientSound.isEmpty()) {
      boolean powered = world.isBlockPowered(pos);
      if (powered) {
        wasPowered = true;
        manageAmbientBroadcast();
      } else if (wasPowered) {
        wasPowered = false;
        stopAmbientForAll();
      }
    } else if (!ambientActivePlayers.isEmpty()) {
      // Defensive: if the selection was cleared but the active set wasn't drained for any
      // reason, drain it now so the next ambient cycle starts fresh.
      stopAmbientForAll();
    }
  }

  @Override
  public void invalidate() {
    super.invalidate();
    // Block was broken / chunk unloaded — make sure no client thinks this speaker is still
    // playing. Without this, a player who moves back into range after the speaker is gone
    // would still hear a phantom loop until they reload.
    stopAmbientForAll();
  }

  // endregion

  // region NBT

  @Override
  public void readNBT(NBTTagCompound compound) {
    if (compound.getBoolean(NBT_HAS_LINK)) {
      linkedTtsPos = new BlockPos(
          compound.getInteger(NBT_LINK_X),
          compound.getInteger(NBT_LINK_Y),
          compound.getInteger(NBT_LINK_Z));
    } else {
      linkedTtsPos = null;
    }
    if (compound.hasKey(NBT_AMBIENT_SOUND)) {
      ambientSound = compound.getString(NBT_AMBIENT_SOUND);
      if (ambientSound == null) ambientSound = "";
    } else {
      ambientSound = "";
    }
  }

  @Override
  public NBTTagCompound writeNBT(NBTTagCompound compound) {
    if (linkedTtsPos != null) {
      compound.setBoolean(NBT_HAS_LINK, true);
      compound.setInteger(NBT_LINK_X, linkedTtsPos.getX());
      compound.setInteger(NBT_LINK_Y, linkedTtsPos.getY());
      compound.setInteger(NBT_LINK_Z, linkedTtsPos.getZ());
    } else {
      compound.setBoolean(NBT_HAS_LINK, false);
    }
    if (ambientSound != null && !ambientSound.isEmpty()) {
      compound.setString(NBT_AMBIENT_SOUND, ambientSound);
    }
    return compound;
  }

  // endregion

  /** Helper for the block class — referenced for the constants prefix. */
  static String namespacePrefix() {
    return CsmConstants.MOD_NAMESPACE + ":";
  }
}

package com.micatechnologies.minecraft.csm.technology;

import com.micatechnologies.minecraft.csm.CsmNetwork;
import com.micatechnologies.minecraft.csm.codeutils.AbstractTileEntity;
import com.micatechnologies.minecraft.csm.codeutils.packets.TileEntityRedstoneTTSInvokePacket;
import com.micatechnologies.minecraft.csm.codeutils.packets.TileEntityRedstoneTTSUpdatePacket;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.common.util.Constants;

public class TileEntityRedstoneTTS extends AbstractTileEntity {

  private static final String TTS_STRING_KEY = "tts";
  private static final String LEGACY_TTS_STRING_KEY = "ttsString";
  private static final String TTS_STRING_DEFAULT = "Setup is Required!";
  private static final String TTS_RADIUS_KEY = "ttR";
  private static final String LEGACY_TTS_RADIUS_KEY = "ttsRadius";
  private static final String TTS_VOICE_KEY = "ttV";
  private static final String TTS_VOICE_DEFAULT = "cmu-slt-hsmm";
  private static final String TTS_LINKED_SPEAKERS_KEY = "lSp";
  private static final long COOLDOWN_DURATION = 2000;

  private AtomicLong lastTtsInvocationTime = new AtomicLong(0);
  private String ttsString = TTS_STRING_DEFAULT;
  private double ttsRadius = 32.0;
  private String ttsVoice = TTS_VOICE_DEFAULT;

  /** Positions of linked speaker blocks. Each one extends the broadcast radius. */
  private final List<BlockPos> linkedSpeakers = new ArrayList<>();

  @Override
  public void readNBT(NBTTagCompound compound) {
    if (compound.hasKey(TTS_STRING_KEY)) {
      ttsString = compound.getString(TTS_STRING_KEY);
    } else if (compound.hasKey(LEGACY_TTS_STRING_KEY)) {
      ttsString = compound.getString(LEGACY_TTS_STRING_KEY);
    }

    if (compound.hasKey(TTS_RADIUS_KEY)) {
      ttsRadius = compound.getDouble(TTS_RADIUS_KEY);
    } else if (compound.hasKey(LEGACY_TTS_RADIUS_KEY)) {
      ttsRadius = compound.getDouble(LEGACY_TTS_RADIUS_KEY);
    }

    if (compound.hasKey(TTS_VOICE_KEY)) {
      ttsVoice = compound.getString(TTS_VOICE_KEY);
    }

    linkedSpeakers.clear();
    if (compound.hasKey(TTS_LINKED_SPEAKERS_KEY)) {
      NBTTagList list = compound.getTagList(TTS_LINKED_SPEAKERS_KEY, Constants.NBT.TAG_COMPOUND);
      for (int i = 0; i < list.tagCount(); i++) {
        NBTTagCompound tag = list.getCompoundTagAt(i);
        linkedSpeakers.add(new BlockPos(
            tag.getInteger("x"), tag.getInteger("y"), tag.getInteger("z")));
      }
    }

    compound.removeTag(LEGACY_TTS_STRING_KEY);
    compound.removeTag(LEGACY_TTS_RADIUS_KEY);
  }

  @Override
  public NBTTagCompound writeNBT(NBTTagCompound compound) {
    compound.setString(TTS_STRING_KEY, ttsString);
    compound.setDouble(TTS_RADIUS_KEY, ttsRadius);
    compound.setString(TTS_VOICE_KEY, ttsVoice);
    if (!linkedSpeakers.isEmpty()) {
      NBTTagList list = new NBTTagList();
      for (BlockPos sp : linkedSpeakers) {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setInteger("x", sp.getX());
        tag.setInteger("y", sp.getY());
        tag.setInteger("z", sp.getZ());
        list.appendTag(tag);
      }
      compound.setTag(TTS_LINKED_SPEAKERS_KEY, list);
    }
    return compound;
  }

  public void readTtsString() {
    if (world.isRemote) {
      System.err.println(
          "Attempted to directly invoke TTS string read on client side! This is a bug!");
    }

    long currentTime = System.currentTimeMillis();
    if (currentTime - lastTtsInvocationTime.get() < COOLDOWN_DURATION) {
      return;
    }
    lastTtsInvocationTime.set(currentTime);

    // Build the set of broadcast origins: the TTS module itself plus every linked speaker
    // whose chunk is loaded and whose block is still actually a speaker. Stale links get
    // pruned in-line so the next save shrinks. Each origin contributes a ttsRadius sphere
    // and we send the invoke packet to the union of players inside any sphere — deduped by
    // player UUID so a player standing near both the TTS module and a speaker only hears the
    // narration once.
    List<BlockPos> origins = new ArrayList<>();
    origins.add(pos);
    pruneAndCollectSpeakerOrigins(origins);

    Set<EntityPlayerMP> recipients = collectPlayersWithinRadius(origins, ttsRadius);
    if (recipients.isEmpty()) {
      return;
    }
    TileEntityRedstoneTTSInvokePacket packet =
        new TileEntityRedstoneTTSInvokePacket(ttsString, ttsVoice);
    for (EntityPlayerMP player : recipients) {
      CsmNetwork.sendTo(packet, player);
    }
  }

  /**
   * Walks {@link #linkedSpeakers}, drops any entry whose target block is no longer a
   * {@link TileEntitySpeaker}, and appends the live speaker positions to {@code outOrigins}.
   * Speakers in unloaded chunks are kept (we can't validate them) but skipped as origins so
   * they don't extend the broadcast unless their chunk is actively loaded.
   */
  private void pruneAndCollectSpeakerOrigins(List<BlockPos> outOrigins) {
    Iterator<BlockPos> it = linkedSpeakers.iterator();
    boolean modified = false;
    while (it.hasNext()) {
      BlockPos sp = it.next();
      if (!world.isBlockLoaded(sp)) {
        // Don't broadcast through it (can't tell anyone's in range there anyway), don't drop it.
        continue;
      }
      TileEntity te = world.getTileEntity(sp);
      if (te instanceof TileEntitySpeaker) {
        outOrigins.add(sp);
      } else {
        it.remove();
        modified = true;
      }
    }
    if (modified) {
      markDirtySync(world, pos, true);
    }
  }

  /** Players within {@code radius} blocks of any origin, deduped by identity. */
  private Set<EntityPlayerMP> collectPlayersWithinRadius(List<BlockPos> origins, double radius) {
    Set<EntityPlayerMP> recipients = new HashSet<>();
    double radiusSq = radius * radius;
    int dim = world.provider.getDimension();
    for (Object obj : world.playerEntities) {
      if (!(obj instanceof EntityPlayerMP)) continue;
      EntityPlayerMP p = (EntityPlayerMP) obj;
      if (p.dimension != dim) continue;
      for (BlockPos origin : origins) {
        double dx = p.posX - (origin.getX() + 0.5);
        double dy = p.posY - (origin.getY() + 0.5);
        double dz = p.posZ - (origin.getZ() + 0.5);
        if (dx * dx + dy * dy + dz * dz <= radiusSq) {
          recipients.add(p);
          break;
        }
      }
    }
    return recipients;
  }

  // region Speaker links

  /**
   * Adds a speaker to this module's broadcast set and back-links the speaker to this TTS
   * module. Returns false if the position is already linked or doesn't host a
   * {@link TileEntitySpeaker}.
   */
  public boolean linkSpeaker(BlockPos speakerPos) {
    if (speakerPos == null || linkedSpeakers.contains(speakerPos)) {
      return false;
    }
    if (world == null) {
      return false;
    }
    TileEntity te = world.getTileEntity(speakerPos);
    if (!(te instanceof TileEntitySpeaker)) {
      return false;
    }
    linkedSpeakers.add(speakerPos.toImmutable());
    ((TileEntitySpeaker) te).setLinkedTtsPos(pos);
    if (!world.isRemote) {
      markDirtySync(world, pos, true);
    }
    return true;
  }

  /**
   * Removes a speaker from this module's broadcast set and clears the speaker's back-link
   * if the block is still a speaker. Returns true if the position was removed from the list.
   */
  public boolean unlinkSpeaker(BlockPos speakerPos) {
    if (speakerPos == null) return false;
    boolean removed = linkedSpeakers.remove(speakerPos);
    if (removed && world != null && !world.isRemote) {
      if (world.isBlockLoaded(speakerPos)) {
        TileEntity te = world.getTileEntity(speakerPos);
        if (te instanceof TileEntitySpeaker) {
          ((TileEntitySpeaker) te).clearLink();
        }
      }
      markDirtySync(world, pos, true);
    }
    return removed;
  }

  public List<BlockPos> getLinkedSpeakers() {
    return linkedSpeakers;
  }

  public int getLinkedSpeakerCount() {
    return linkedSpeakers.size();
  }

  // endregion

  public String getTtsString() {
    return ttsString;
  }

  public String getTtsVoice() {
    return ttsVoice;
  }

  public void setTtsConfigFromGui(String newTtsString, String newTtsVoice) {
    if (world.isRemote) {
      CsmNetwork.sendToServer(
          new TileEntityRedstoneTTSUpdatePacket(this.pos, newTtsString, newTtsVoice));
    } else {
      setTtsString(newTtsString);
      setTtsVoice(newTtsVoice);
    }
  }

  public void setTtsString(String ttsString) {
    if (world.isRemote) {
      System.err.println("Attempted to set TTS string on client side! This is a bug!");
    }
    this.ttsString = ttsString;
    markDirtySync(getWorld(), getPos(), true);
  }

  public void setTtsVoice(String ttsVoice) {
    if (world.isRemote) {
      System.err.println("Attempted to set TTS voice on client side! This is a bug!");
    }
    this.ttsVoice = ttsVoice;
    markDirtySync(getWorld(), getPos(), true);
  }
}

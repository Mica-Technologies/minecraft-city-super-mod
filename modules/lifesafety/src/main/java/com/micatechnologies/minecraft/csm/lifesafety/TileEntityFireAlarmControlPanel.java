package com.micatechnologies.minecraft.csm.lifesafety;

import com.micatechnologies.minecraft.csm.api.firealarm.CsmFireAlarmQuery;
import com.micatechnologies.minecraft.csm.api.firealarm.FireAlarmEvent;
import com.micatechnologies.minecraft.csm.api.firealarm.FireAlarmPanelRegistry;
import com.micatechnologies.minecraft.csm.codeutils.AbstractTickableTileEntity;
import com.micatechnologies.minecraft.csm.codeutils.CsmChunks;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.FMLCommonHandler;

/**
 * Tile entity for the fire alarm control panel block. Manages alarm state, device scanning,
 * sound dispatching to linked sounder/speaker blocks, and synchronization with clients.
 *
 * @author Mica Technologies
 * @since 2026.4
 */

public class TileEntityFireAlarmControlPanel extends AbstractTickableTileEntity {

  private static final int tickRate = 20;

  // NBT keys — shortened for save/sync size. LEGACY_* variants are only read for worlds saved
  // before the optimization; new writes only emit the short form.
  private static final String soundIndexKey = "sIx";
  private static final String legacySoundIndexKey = "soundIndex";
  private static final String alarmKey = "a";
  private static final String legacyAlarmKey = "alarm";
  private static final String alarmStormKey = "aSm";
  private static final String legacyAlarmStormKey = "alarmStorm";
  private static final String connectedAppliancesKey = "apps";
  private static final String legacyConnectedAppliancesKey = "connectedAppliances";
  private static final String alarmAnnouncedKey = "aAn";
  private static final String legacyAlarmAnnouncedKey = "alarmAnnounced";
  private static final String audibleSilenceKey = "aSi";
  private static final String legacyAudibleSilenceKey = "audibleSilence";
  private static final String glitchyKey = "gl";
  private static final String legacyGlitchyKey = "glitchy";
  private static final String acknowledgedKey = "akd";
  private static final String drillKey = "drl";
  private static final String troubleKey = "trb";
  private static final String troubleAcknowledgedKey = "trA";
  private static final String alarmOriginPosKey = "aoP";
  private static final String alarmOriginNameKey = "aoN";
  /** Initiating devices that report to this panel, as a flat IntArray of x, y, z triples. */
  private static final String initiatingDevicesKey = "init";

  /**
   * Appliances to add, as a flat int array of x, y, z triples. Never written: it is read once,
   * merged into {@link #connectedAppliancesKey} and dropped. It exists because {@code apps} is a
   * newline-separated string and chat cannot carry a newline, so {@code /blockdata} could add only
   * one appliance at a time; {@code /blockdata x y z {appsList:[I;x,y,z,x,y,z]}} adds many.
   */
  private static final String appliancesListKey = "appsList";
  private static final String[] SOUND_RESOURCE_NAMES = {"csm:svenew",
      "csm:sveold",
      "csm:simplex_voice_evac_old_alt",
      "csm:mills_firealarm",
      "csm:lms_voice_evac",
      "csm:notifier_voice_evac",
      "csm:notifier_voice_evac_alt",
      "csm:notifier_voice_evac_alt2",
      "csm:notifier_ucla_voice_evac",
      "csm:awful_notifier_ve",
      "csm:mclalsve",
      "csm:firecom8500",
      "csm:simplex_voice_evac_old_alt2",
      "csm:bsp_the_lofts_voic_evac",
      "csm:est_preint_voice_evac",
      "csm:fci_voic_evac_female",
      "csm:fci_voic_evac_male"};
  private static final String[] SOUND_NAMES = {"Simplex Voice Evac 1",
      "Simplex Voice Evac 2",
      "Simplex Voice Evac 3",
      "Notifier Voice Evac 1",
      "Notifier Voice Evac 2",
      "Notifier Voice Evac 3",
      "Notifier Voice Evac 4",
      "Notifier Voice Evac 5",
      "Notifier UCLA Voice Evac",
      "Notifier Voice Evac 6",
      "Mica Voice Evac 1",
      "Firecom 8500",
      "Simplex Voice Evac 4",
      "BSP The Lofts Voice Evac",
      "EST Preintelligent Voice Evac",
      "FCI Voice Evac (Female)",
      "FCI Voice Evac (Male)"};
  private static final String STORM_SOUND_NAME = "csm:notifier_tornado_voice_evac";
  private static final float SOUNDER_VOLUME = 2.0f;
  private static final float VOICE_EVAC_VOLUME = 3.0f;
  private static final float STORM_VOICE_EVAC_VOLUME = 3.0f;
  /** How often the appliance cache is rebuilt, to catch a horn's tone changed by hand. */
  private static final int CACHE_REFRESH_TICKS = 6000; // ~5 minutes

  private static final String CHANNEL_VOICE_EVAC = "voiceevac";
  private static final String CHANNEL_STORM = "storm";
  private static final String CHANNEL_STROBE_ONLY = "strobeonly";
  /** The panel's own buzzer: a channel per panel (the position is appended), heard close by. */
  private static final String CHANNEL_BUZZER_PREFIX = "panelbuzzer_";
  private static final float BUZZER_HEARING_RANGE = 12.0f;

  private final ArrayList<BlockPos> connectedAppliances = new ArrayList<>();
  private int soundIndex;
  private boolean alarm;
  private boolean alarmStorm;
  private boolean alarmAnnounced;
  private boolean audibleSilence;
  private boolean glitchy;
  private boolean acknowledged;
  private boolean drill;
  /**
   * A linked appliance has gone missing and nobody has acknowledged it yet. Latched: it stays
   * set until ACK even once the panel forgets the device, as a real panel's trouble does.
   */
  private boolean trouble;
  /**
   * How many missing devices the last ACK covered, so only a new loss sounds trouble again. Saved,
   * or every world load would beep again for devices already acknowledged.
   */
  private int acknowledgedMissing;
  private String lastBuzzerSound = null;
  private BlockPos alarmOriginPos;
  private String alarmOriginName = "";
  private final ArrayList<BlockPos> initiatingDevices = new ArrayList<>();
  private boolean alarmWasActive = false;
  private int cacheRefreshTickCounter = 0;

  // Channel-based active player tracking (voice evac, storm, and each horn sound)
  private final Map<String, HashSet<UUID>> channelActivePlayers = new HashMap<>();
  private final Set<String> lastActiveChannels = new HashSet<>();
  private String lastVoiceEvacSoundSent = null;
  private boolean lastGlitchySent = false;

  // Cached appliance categorization — avoids getBlockState on every appliance every tick.
  // Rebuilt when connectedAppliances changes or periodically to catch manual sound changes.
  private transient List<BlockPos> cachedVoiceEvacPositions;
  private transient Map<String, List<BlockPos>> cachedHornGroups;
  private transient List<BlockPos> cachedStrobeOnlyPositions;
  private transient List<BlockPos> cachedAllStrobePositions;

  @Override
  public void readNBT(NBTTagCompound compound) {
    soundIndex = readInt(compound, soundIndexKey, legacySoundIndexKey, 0);
    alarm = readBool(compound, alarmKey, legacyAlarmKey);
    alarmStorm = readBool(compound, alarmStormKey, legacyAlarmStormKey);
    alarmAnnounced = readBool(compound, alarmAnnouncedKey, legacyAlarmAnnouncedKey);
    audibleSilence = readBool(compound, audibleSilenceKey, legacyAudibleSilenceKey);
    glitchy = readBool(compound, glitchyKey, legacyGlitchyKey);
    acknowledged = compound.getBoolean(acknowledgedKey);
    drill = compound.getBoolean(drillKey);
    trouble = compound.getBoolean(troubleKey);
    acknowledgedMissing = compound.getInteger(troubleAcknowledgedKey);

    int[] origin = compound.getIntArray(alarmOriginPosKey);
    alarmOriginPos = origin.length == 3 ? new BlockPos(origin[0], origin[1], origin[2]) : null;
    alarmOriginName = compound.getString(alarmOriginNameKey);

    initiatingDevices.clear();
    int[] inits = compound.getIntArray(initiatingDevicesKey);
    for (int i = 0; i + 2 < inits.length; i += 3) {
      initiatingDevices.add(new BlockPos(inits[i], inits[i + 1], inits[i + 2]));
    }

    connectedAppliances.clear();
    String appliancesBlob = null;
    if (compound.hasKey(connectedAppliancesKey)) {
      appliancesBlob = compound.getString(connectedAppliancesKey);
    } else if (compound.hasKey(legacyConnectedAppliancesKey)) {
      appliancesBlob = compound.getString(legacyConnectedAppliancesKey);
    }
    if (appliancesBlob != null && !appliancesBlob.isEmpty()) {
      String[] positions = appliancesBlob.split("\n");
      for (String position : positions) {
        String[] coordinates = position.split(" ");
        if (coordinates.length == 3) {
          connectedAppliances.add(
              new BlockPos(Integer.parseInt(coordinates[0]), Integer.parseInt(coordinates[1]),
                  Integer.parseInt(coordinates[2])));
        }
      }
    }

    int[] added = compound.getIntArray(appliancesListKey);
    for (int i = 0; i + 2 < added.length; i += 3) {
      BlockPos bp = new BlockPos(added[i], added[i + 1], added[i + 2]);
      if (!connectedAppliances.contains(bp)) {
        connectedAppliances.add(bp);
      }
    }
    compound.removeTag(appliancesListKey);

    cachedVoiceEvacPositions = null;

    // Strip legacy long-form keys so subsequent writes produce only short-form output
    compound.removeTag(legacySoundIndexKey);
    compound.removeTag(legacyAlarmKey);
    compound.removeTag(legacyAlarmStormKey);
    compound.removeTag(legacyAlarmAnnouncedKey);
    compound.removeTag(legacyAudibleSilenceKey);
    compound.removeTag(legacyGlitchyKey);
    compound.removeTag(legacyConnectedAppliancesKey);

    // Re-register with the API registry if this panel was saved with an active alarm.
    // NOTE: this does NOT cover the chunk-load path — TileEntity.create calls setWorldCreate
    // (an empty no-op in vanilla 1.12.2) before readFromNBT, so `world` is still null here when
    // a panel is read back off disk. onLoad() below is what actually restores the registration;
    // this block only helps on paths where the TE already has a world (e.g. a re-read after
    // placement). Keep both — they are cheap and idempotent.
    registerActiveAlarms();
  }

  /**
   * Restores this panel's entries in {@link FireAlarmPanelRegistry} when the tile entity is
   * added to the world.
   * <p>
   * Required for correctness on dedicated servers: {@link #onChunkUnload()} drops the
   * registration when a chunk unloads, and nothing else adds it back. Registration otherwise
   * only happens on an OFF -&gt; ON transition in {@link #setAlarmState(boolean)} /
   * {@link #setAlarmStormState(boolean)}, so a latched alarm that survives a chunk reload
   * would keep sounding while {@code CsmFireAlarmQuery} reported no alarm at all — silently
   * breaking every consumer of the API (SUM's roamer evacuation AI, for one). Fire alarms
   * latch until manually reset, so they never recovered; redstone-driven storm alarms only
   * recovered if something re-triggered them.
   * <p>
   * {@code onLoad} is the correct hook because {@code World.addTileEntity} calls it after
   * {@code setWorld}, so {@code world} is populated (unlike during {@code readFromNBT}).
   */
  @Override
  public void onLoad() {
    super.onLoad();
    registerActiveAlarms();
  }

  /**
   * Puts an already-alarming panel back into the registry when it loads, and says so.
   * <p>
   * The events are posted here for the same reason {@link #unregisterActiveAlarms()} posts them:
   * {@link FireAlarmEvent} is meant to be a faithful running commentary on what
   * {@link CsmFireAlarmQuery} would answer, and a panel appearing in that answer without an
   * {@code Activated} behind it leaves an event-driven listener believing the building is quiet
   * while the query says otherwise. A listener that keeps no state of its own sees nothing new; one
   * that mirrors the events now stays in step across a chunk cycle.
   * <p>
   * Guarded on the registry actually changing, so the load path cannot announce an alarm that was
   * already listed -- {@code onLoad} can run more than once for one panel.
   */
  private void registerActiveAlarms() {
    if (world == null || world.isRemote) {
      return;
    }
    int dim = world.provider.getDimension();
    if (alarm && FireAlarmPanelRegistry.registerFireAlarm(dim, getPos())) {
      MinecraftForge.EVENT_BUS.post(new FireAlarmEvent.Activated(world, getPos()));
    }
    if (alarmStorm && FireAlarmPanelRegistry.registerStormAlarm(dim, getPos())) {
      MinecraftForge.EVENT_BUS.post(new FireAlarmEvent.StormActivated(world, getPos()));
    }
  }

  /**
   * Takes the panel out of the registry when it goes away, and says so.
   * <p>
   * Both of the paths that call this -- the block being broken and the chunk unloading -- used to
   * remove the panel silently, so anything driven by the events heard an alarm start and never
   * heard it stop. Polling consumers were unaffected, which is what kept this hidden.
   * <p>
   * Note that a chunk unload is not the alarm being reset: the state is saved, and loading the
   * chunk again re-registers it and posts a fresh {@code Activated}. What the pair of events
   * describes is the panel being answerable, which is the only thing anything outside this mod can
   * observe about it.
   * <p>
   * Safe to call twice. Forge runs both {@code invalidate} and {@code onChunkUnload} on some
   * unload paths, and the registry reports whether the removal did anything, so the second call is
   * silent rather than a repeated event.
   */
  private void unregisterActiveAlarms() {
    if (world == null || world.isRemote) {
      return;
    }
    int dim = world.provider.getDimension();
    if (FireAlarmPanelRegistry.unregisterFireAlarm(dim, getPos())) {
      MinecraftForge.EVENT_BUS.post(new FireAlarmEvent.Deactivated(world, getPos()));
    }
    if (FireAlarmPanelRegistry.unregisterStormAlarm(dim, getPos())) {
      MinecraftForge.EVENT_BUS.post(new FireAlarmEvent.StormDeactivated(world, getPos()));
    }
  }

  private static int readInt(NBTTagCompound compound, String key, String legacyKey, int def) {
    if (compound.hasKey(key)) {
      return compound.getInteger(key);
    }
    if (compound.hasKey(legacyKey)) {
      return compound.getInteger(legacyKey);
    }
    return def;
  }

  private static boolean readBool(NBTTagCompound compound, String key, String legacyKey) {
    if (compound.hasKey(key)) {
      return compound.getBoolean(key);
    }
    return compound.hasKey(legacyKey) && compound.getBoolean(legacyKey);
  }

  @Override
  public NBTTagCompound writeNBT(NBTTagCompound compound) {
    compound.setInteger(soundIndexKey, soundIndex);
    compound.setBoolean(alarmKey, alarm);
    compound.setBoolean(alarmStormKey, alarmStorm);
    compound.setBoolean(alarmAnnouncedKey, alarmAnnounced);
    compound.setBoolean(audibleSilenceKey, audibleSilence);
    compound.setBoolean(glitchyKey, glitchy);
    compound.setBoolean(acknowledgedKey, acknowledged);
    compound.setBoolean(drillKey, drill);
    compound.setBoolean(troubleKey, trouble);
    compound.setInteger(troubleAcknowledgedKey, acknowledgedMissing);
    if (alarmOriginPos != null) {
      compound.setIntArray(alarmOriginPosKey, new int[] {alarmOriginPos.getX(),
          alarmOriginPos.getY(), alarmOriginPos.getZ()});
      compound.setString(alarmOriginNameKey, alarmOriginName);
    }
    if (!initiatingDevices.isEmpty()) {
      int[] inits = new int[initiatingDevices.size() * 3];
      for (int i = 0; i < initiatingDevices.size(); i++) {
        BlockPos bp = initiatingDevices.get(i);
        inits[i * 3] = bp.getX();
        inits[i * 3 + 1] = bp.getY();
        inits[i * 3 + 2] = bp.getZ();
      }
      compound.setIntArray(initiatingDevicesKey, inits);
    }

    StringBuilder connectedAppliancesString = new StringBuilder();
    for (BlockPos bp : connectedAppliances) {
      connectedAppliancesString.append(bp.getX())
          .append(" ")
          .append(bp.getY())
          .append(" ")
          .append(bp.getZ())
          .append("\n");
    }
    compound.setString(connectedAppliancesKey, connectedAppliancesString.toString());
    return compound;
  }

  public void switchSound() {
    soundIndex++;
    if (soundIndex >= SOUND_RESOURCE_NAMES.length) {
      soundIndex = 0;
    }
    markDirty();
  }

  public synchronized boolean addLinkedAlarm(BlockPos blockPos) {
    if (!connectedAppliances.contains(blockPos)) {
      connectedAppliances.add(blockPos);
      cachedVoiceEvacPositions = null;
      markDirty();
      return true;
    }
    return false;
  }

  /**
   * Returns a read-only copy of the connected appliance positions (sounders, strobes, etc.).
   * This is part of the public API for cross-mod integration.
   */
  public List<BlockPos> getConnectedAppliances() {
    return Collections.unmodifiableList(new ArrayList<>(connectedAppliances));
  }

  public boolean getAlarmState() {
    return alarm;
  }

  public boolean getAlarmAnnouncedState() {
    return alarmAnnounced;
  }

  public void setAlarmState(boolean alarmState) {
    boolean wasActive = this.alarm;

    // New alarm activation while silenced cancels audible silence (re-enables sound)
    if (alarmState && audibleSilence) {
      audibleSilence = false;
    }
    // Reset clears audible silence
    if (!alarmState) {
      audibleSilence = false;
    }
    // A fresh alarm arrives unacknowledged, so the panel's FIRE ALARM lamp flashes until someone
    // presses ACK. A reset clears both the acknowledgement and any drill the reset ended.
    if (!wasActive && alarmState) {
      acknowledged = false;
    } else if (!alarmState) {
      acknowledged = false;
      drill = false;
      alarmOriginPos = null;
      alarmOriginName = "";
    }
    alarm = alarmState;
    markDirty();

    // Resetting the panel also shuts the sprinklers off: every head that discharged took note of
    // what it flooded, and this is where that water is handed back. Without it the only way to
    // clear a discharge was to place a block in the puddle.
    if (!alarmState && wasActive && world != null && !world.isRemote) {
      resetInitiatingDevices();
      playBuzzerTone(panelBlock() != null ? panelBlock().getBuzzerResetSound() : null);
    }

    // Post API events and update registry on state transitions
    if (world != null && !world.isRemote) {
      int dim = world.provider.getDimension();
      if (!wasActive && alarmState) {
        FireAlarmPanelRegistry.registerFireAlarm(dim, getPos());
        MinecraftForge.EVENT_BUS.post(new FireAlarmEvent.Activated(world, getPos()));
      } else if (wasActive && !alarmState) {
        FireAlarmPanelRegistry.unregisterFireAlarm(dim, getPos());
        MinecraftForge.EVENT_BUS.post(new FireAlarmEvent.Deactivated(world, getPos()));
      }
      if (wasActive != alarmState) {
        syncServerToClient(world);
      }
    }
  }

  /**
   * Activates the alarm and records which device raised it, so the panel can say where the alarm
   * came from instead of only that there is one.
   *
   * @param originPos  the initiating device's position
   * @param originName the initiating device's block registry name, resolved to a display name by
   *                   the panel GUI
   */
  public void activateAlarmFrom(BlockPos originPos, String originName) {
    // Only the first device to report is kept, matching a real panel's "first alarm" display: a
    // second station pulled during an active alarm does not overwrite where it started.
    if (!alarm) {
      alarmOriginPos = originPos;
      alarmOriginName = originName == null ? "" : originName;
    }
    setAlarmState(true);
  }

  /**
   * The position of the device that raised the current alarm, or {@code null} if the alarm was
   * started some other way (a drill, redstone, or an external caller).
   *
   * @return the initiating device's position, or {@code null}
   */
  public BlockPos getAlarmOriginPos() {
    return alarmOriginPos;
  }

  /**
   * The block registry name of the device that raised the current alarm, empty if unknown.
   *
   * @return the initiating device's registry name, or an empty string
   */
  public String getAlarmOriginName() {
    return alarmOriginName;
  }

  /**
   * Records an initiating device (pull station, detector, sprinkler) as reporting to this panel.
   * <p>
   * The link itself lives on the device, which stores the panel it reports to; this is the
   * reverse index, and it exists so the panel can reach its devices on reset. Without it a panel
   * has no way to enumerate what feeds it.
   *
   * @param blockPos the device's position
   *
   * @return {@code true} if this device was not already indexed
   */
  public synchronized boolean addLinkedInitiatingDevice(BlockPos blockPos) {
    if (initiatingDevices.contains(blockPos)) {
      return false;
    }
    initiatingDevices.add(blockPos);
    // A detector with a strobe built in flashes with the appliances
    cachedVoiceEvacPositions = null;
    markDirty();
    return true;
  }

  /**
   * The initiating devices indexed against this panel.
   *
   * @return an unmodifiable copy of the initiating device positions
   */
  public List<BlockPos> getLinkedInitiatingDevices() {
    return Collections.unmodifiableList(new ArrayList<>(initiatingDevices));
  }

  /**
   * Tells every indexed initiating device the panel has been reset, and drops the ones that are
   * no longer there. Sprinklers use this to drain what they discharged.
   */
  private void resetInitiatingDevices() {
    Iterator<BlockPos> it = initiatingDevices.iterator();
    boolean changed = false;
    while (it.hasNext()) {
      BlockPos bp = it.next();
      if (!world.isBlockLoaded(bp)) {
        continue;
      }
      TileEntity te = world.getTileEntity(bp);
      if (te instanceof TileEntityFireAlarmSensor) {
        ((TileEntityFireAlarmSensor) te).clearDischargedWater(world);
      }
      // A device that is gone stays indexed: it is in trouble until put back or unlinked
    }
    if (changed) {
      markDirty();
    }
  }

  /**
   * Whether the panel is running the storm warning announcement, which is driven by redstone
   * power rather than by a fire alarm activation.
   *
   * @return {@code true} if the storm alarm is active
   */
  public boolean getAlarmStormState() {
    return alarmStorm;
  }

  public void setAlarmStormState(boolean alarmStormState) {
    boolean wasActive = this.alarmStorm;
    alarmStorm = alarmStormState;
    markDirty();

    if (world != null && !world.isRemote) {
      int dim = world.provider.getDimension();
      if (!wasActive && alarmStormState) {
        FireAlarmPanelRegistry.registerStormAlarm(dim, getPos());
        MinecraftForge.EVENT_BUS.post(new FireAlarmEvent.StormActivated(world, getPos()));
      } else if (wasActive && !alarmStormState) {
        FireAlarmPanelRegistry.unregisterStormAlarm(dim, getPos());
        MinecraftForge.EVENT_BUS.post(new FireAlarmEvent.StormDeactivated(world, getPos()));
      }
      if (wasActive != alarmStormState) {
        syncServerToClient(world);
      }
    }
  }

  public void setAlarmAnnouncedState(boolean alarmAnnouncedState) {
    alarmAnnounced = alarmAnnouncedState;
    markDirty();
  }

  public boolean getAudibleSilence() {
    return audibleSilence;
  }

  public void setAudibleSilence(boolean audibleSilenceState) {
    audibleSilence = audibleSilenceState;
    markDirty();

    if (world != null && !world.isRemote) {
      if (audibleSilenceState) {
        MinecraftForge.EVENT_BUS.post(new FireAlarmEvent.AudibleSilenced(world, getPos()));
      }
      syncServerToClient(world);
    }
  }

  /**
   * Whether the current alarm has been acknowledged at the panel. Drives the difference between a
   * flashing and a steady FIRE ALARM lamp, exactly as it does on a real panel: acknowledging says
   * an operator has seen the alarm, and does nothing to the notification appliances.
   *
   * @return {@code true} if the active alarm has been acknowledged
   */
  public boolean getAcknowledged() {
    return acknowledged;
  }

  /**
   * Acknowledges the active alarm and any trouble, which quiets the panel's own buzzer. Only an
   * active alarm is marked acknowledged, so a stray press cannot leave a quiet panel latched into
   * that state; a trouble is cleared, and sounds again only if another device goes missing.
   */
  public void acknowledge() {
    boolean changed = false;
    if (alarm && !acknowledged) {
      acknowledged = true;
      changed = true;
    }
    if (trouble) {
      trouble = false;
      acknowledgedMissing = world != null ? countMissingDevices() : 0;
      changed = true;
    }
    if (!changed) {
      return;
    }
    markDirty();
    if (world != null && !world.isRemote) {
      syncServerToClient(world);
    }
  }

  /**
   * Whether the active alarm is an evacuation drill started at the panel rather than a real alarm
   * from a pull station or detector. Purely presentational -- the appliances do exactly what they
   * do in a real alarm -- but it lets the panel and the chat announcement say "drill".
   *
   * @return {@code true} if the active alarm was started as a drill
   */
  public boolean getDrill() {
    return drill;
  }

  /**
   * Starts an evacuation drill: marks the alarm as a drill and then activates it. The flag is set
   * first so the announcement made on the next tick already knows this is a drill.
   */
  public void startDrill() {
    if (alarm) {
      return;
    }
    drill = true;
    setAlarmState(true);
  }

  /**
   * Selects the voice evacuation message by index, ignoring out-of-range values.
   *
   * @param index index into the voice evacuation message table
   */
  public void setSoundIndex(int index) {
    if (index < 0 || index >= SOUND_RESOURCE_NAMES.length || index == soundIndex) {
      return;
    }
    soundIndex = index;
    markDirty();
  }

  public boolean getGlitchy() {
    return glitchy;
  }

  public void toggleGlitchy() {
    glitchy = !glitchy;
    // Force the voice evac channel to restart so clients pick up the new glitchy setting
    lastVoiceEvacSoundSent = null;
    markDirty();
  }

  public String getStatusString() {
    if (alarm && audibleSilence) {
      return "Audible Silence";
    } else if (alarm) {
      return drill ? "Drill Active" : "Alarm Active";
    } else if (trouble) {
      return "Trouble";
    }
    return "Normal";
  }

  /**
   * Whether an unacknowledged trouble is latched: a linked appliance went missing since the last
   * ACK.
   *
   * @return {@code true} while the trouble buzzer should sound
   *
   * @since 2026.9
   */
  public boolean getTrouble() {
    return trouble;
  }

  public int getSoundIndex() {
    return soundIndex;
  }

  public static String[] getSoundNames() {
    return SOUND_NAMES;
  }

  public String getCurrentSoundName() {
    return SOUND_NAMES[soundIndex];
  }

  @Override
  public void invalidate() {
    stopEverythingOnRemoval();
    unregisterActiveAlarms();
    super.invalidate();
  }

  @Override
  public void onChunkUnload() {
    stopEverythingOnRemoval();
    unregisterActiveAlarms();
    super.onChunkUnload();
  }

  @Override
  public boolean doClientTick() {
    return false;
  }

  @Override
  public boolean pauseTicking() {
    return false;
  }

  @Override
  public long getTickRate() {
    return tickRate;
  }

  @Override
  public void onTick() {
    if (world.isRemote) {
      return;
    }

    try {
      // Trouble first, from the devices as they stand this tick
      updateTrouble();

      // A missing device is never dropped here: it stays listed, in trouble and with its
      // coordinates on the display, until it is put back or unlinked with the linker
      cacheRefreshTickCounter += tickRate;
      if (cacheRefreshTickCounter >= CACHE_REFRESH_TICKS) {
        cacheRefreshTickCounter = 0;
        cachedVoiceEvacPositions = null;
      }

      if (cachedVoiceEvacPositions == null) {
        rebuildApplianceCache();
      }

      MinecraftServer mcserv = FMLCommonHandler.instance().getMinecraftServerInstance();
      if (mcserv == null) {
        return;
      }
      List<EntityPlayerMP> players = mcserv.getPlayerList().getPlayers();

      if (alarm) {
        // Fire alarm active -- stop storm channel if it was playing
        stopChannel(players, CHANNEL_STORM);

        // Announce alarm if not announced
        if (!alarmWasActive) {
          alarmWasActive = true;
          if (!getAlarmAnnouncedState()) {
            BlockPos blockPos = getPos();
            mcserv.getPlayerList()
                .sendMessage(new TextComponentString("The fire alarm at [" +
                    blockPos.getX() +
                    "," +
                    blockPos.getY() +
                    "," +
                    blockPos.getZ() +
                    "] " +
                    (drill ? "has started an evacuation drill!" : "has been activated!")));
            setAlarmAnnouncedState(true);
          }
        }

        String voiceEvacSoundName = getCurrentSoundResourceName();
        float voiceEvacHearingRange = VOICE_EVAC_VOLUME * 16.0f;
        List<BlockPos> voiceEvacPositions = cachedVoiceEvacPositions;
        Map<String, List<BlockPos>> hornGroups = cachedHornGroups;
        List<BlockPos> strobeOnlyPositions = cachedStrobeOnlyPositions;

        // Track which channels are active this tick
        Set<String> currentActiveChannels = new HashSet<>();

        if (audibleSilence) {
          // Audible silence: stop all horn and voice evac sounds, keep strobes active
          stopChannel(players, CHANNEL_VOICE_EVAC);
          float hornHearingRange = SOUNDER_VOLUME * 16.0f;
          for (String hornChannel : hornGroups.keySet()) {
            stopChannel(players, hornChannel);
          }

          if (!cachedAllStrobePositions.isEmpty()) {
            List<BlockPos> allStrobePositions = cachedAllStrobePositions;
            manageSoundForPlayers(players, allStrobePositions, CHANNEL_STROBE_ONLY,
                "", hornHearingRange);
            currentActiveChannels.add(CHANNEL_STROBE_ONLY);
          }
        } else {
          // Normal alarm: manage all sounds

          // Voice evac: manage client-side MovingSound via packets
          if (!voiceEvacPositions.isEmpty()) {
            // If the sound or glitchy setting changed, restart on all active clients
            boolean soundChanged = lastVoiceEvacSoundSent != null &&
                !lastVoiceEvacSoundSent.equals(voiceEvacSoundName);
            boolean glitchyChanged = lastGlitchySent != glitchy;
            if (soundChanged || glitchyChanged) {
              stopChannel(players, CHANNEL_VOICE_EVAC);
            }

            manageSoundForPlayers(players, voiceEvacPositions, CHANNEL_VOICE_EVAC,
                voiceEvacSoundName, voiceEvacHearingRange, glitchy);
            lastVoiceEvacSoundSent = voiceEvacSoundName;
            lastGlitchySent = glitchy;
            currentActiveChannels.add(CHANNEL_VOICE_EVAC);
          }

          // Horns: one MovingSound channel per unique horn sound
          float hornHearingRange = SOUNDER_VOLUME * 16.0f;
          for (Map.Entry<String, List<BlockPos>> entry : hornGroups.entrySet()) {
            String hornChannel = entry.getKey();
            List<BlockPos> hornPositions = entry.getValue();
            manageSoundForPlayers(players, hornPositions, hornChannel, hornChannel,
                hornHearingRange);
            currentActiveChannels.add(hornChannel);
          }

          // Strobe-only devices: send positions with empty sound resource so the client
          // registers them in ActiveStrobeRegistry without creating a MovingSound
          if (!strobeOnlyPositions.isEmpty()) {
            manageSoundForPlayers(players, strobeOnlyPositions, CHANNEL_STROBE_ONLY,
                "", hornHearingRange);
            currentActiveChannels.add(CHANNEL_STROBE_ONLY);
          }
        }

        // Stop channels that were active last tick but are no longer (horn removed/sound changed)
        for (String oldChannel : lastActiveChannels) {
          if (!currentActiveChannels.contains(oldChannel)) {
            stopChannel(players, oldChannel);
          }
        }
        lastActiveChannels.clear();
        lastActiveChannels.addAll(currentActiveChannels);

      } else {
        // Alarm has ended
        if (alarmWasActive) {
          alarmWasActive = false;
          BlockPos blockPos = getPos();
          mcserv.getPlayerList()
              .sendMessage(new TextComponentString("The fire alarm at [" +
                  blockPos.getX() +
                  "," +
                  blockPos.getY() +
                  "," +
                  blockPos.getZ() +
                  "] " +
                  "has been reset."));
          setAlarmAnnouncedState(false);

          // Stop all fire alarm sounds on all clients
          stopAllChannels(players);
          lastVoiceEvacSoundSent = null;
        }

        // Handle storm alarm (reuses cached voice evac positions — same speakers)
        if (alarmStorm) {
          float stormHearingRange = STORM_VOICE_EVAC_VOLUME * 16.0f;
          if (!cachedVoiceEvacPositions.isEmpty()) {
            manageSoundForPlayers(players, cachedVoiceEvacPositions, CHANNEL_STORM,
                STORM_SOUND_NAME, stormHearingRange);
          }
        } else {
          // Storm alarm off - stop storm sounds
          stopChannel(players, CHANNEL_STORM);
        }
      }

      manageBuzzer(players);
    } catch (Exception e) {
      com.micatechnologies.minecraft.csm.Csm.getLogger()
          .error("Error ticking fire alarm control panel at {}", getPos(), e);
    }
  }

  /**
   * Manages the client-side MovingSound for a specific channel for all players. Sends start
   * packets to players who are in range but don't have the sound playing yet. Sends stop
   * packets to players who have moved out of range of all speakers/horns.
   */
  private void manageSoundForPlayers(List<EntityPlayerMP> players,
      List<BlockPos> positions, String channel, String soundName, float hearingRange) {
    manageSoundForPlayers(players, positions, channel, soundName, hearingRange, false);
  }

  /**
   * Manages the client-side MovingSound for a specific channel for all players. Sends start
   * packets to players who are in range but don't have the sound playing yet. Sends stop
   * packets to players who have moved out of range of all speakers/horns.
   *
   * @param glitchy if true, the client-side sound will play with occasional stutters/dropouts
   */
  private void manageSoundForPlayers(List<EntityPlayerMP> players,
      List<BlockPos> positions, String channel, String soundName, float hearingRange,
      boolean glitchy) {
    double hearingRangeSq = hearingRange * (double) hearingRange;
    HashSet<UUID> activePlayers =
        channelActivePlayers.computeIfAbsent(channel, k -> new HashSet<>());

    for (EntityPlayerMP player : players) {
      UUID playerId = player.getUniqueID();
      boolean inRange = isPlayerInRangeOfAny(player, positions, hearingRangeSq);

      if (inRange && !activePlayers.contains(playerId)) {
        // Player entered range - start their client-side MovingSound
        CsmLifeSafety.NETWORK.sendTo(
            FireAlarmSoundPacket.start(scoped(channel), soundName, hearingRange, positions,
                glitchy),
            player);
        activePlayers.add(playerId);
      } else if (!inRange && activePlayers.contains(playerId)) {
        // Player left range - stop their client-side MovingSound for this channel
        CsmLifeSafety.NETWORK.sendTo(FireAlarmSoundPacket.stop(scoped(channel)), player);
        activePlayers.remove(playerId);
      }
    }

    // Clean up players who disconnected (use HashSet for O(1) lookup)
    Set<UUID> onlinePlayerIds = new HashSet<>();
    for (EntityPlayerMP p : players) {
      onlinePlayerIds.add(p.getUniqueID());
    }
    activePlayers.removeIf(id -> !onlinePlayerIds.contains(id));
  }

  /**
   * Stops a specific channel: sends stop packets to all active players on that channel.
   */
  private void stopChannel(List<EntityPlayerMP> players, String channel) {
    HashSet<UUID> activePlayers = channelActivePlayers.get(channel);
    if (activePlayers == null || activePlayers.isEmpty()) {
      return;
    }
    FireAlarmSoundPacket stopPacket = FireAlarmSoundPacket.stop(scoped(channel));
    for (EntityPlayerMP player : players) {
      if (activePlayers.contains(player.getUniqueID())) {
        CsmLifeSafety.NETWORK.sendTo(stopPacket, player);
      }
    }
    activePlayers.clear();
  }

  /**
   * Stops all of this panel's channels: a stop for each channel to every player it is playing
   * for. Never the stop-all packet, which would also silence every other panel the player hears.
   */
  private void stopAllChannels(List<EntityPlayerMP> players) {
    for (String channel : new ArrayList<>(channelActivePlayers.keySet())) {
      stopChannel(players, channel);
    }
    channelActivePlayers.clear();
    lastActiveChannels.clear();
  }

  /**
   * The name a channel goes out under: the panel's position appended, so two panels that play
   * the same sound (or both run strobes) keep separate channels on a client that hears both.
   * Shared names let one panel's start replace the other's positions and its stop put the other's
   * sound and strobes out. The buzzer's channel already carries the position.
   */
  private String scoped(String channel) {
    if (channel.startsWith(CHANNEL_BUZZER_PREFIX)) {
      return channel;
    }
    return channel + "@" + getPos().getX() + "_" + getPos().getY() + "_" + getPos().getZ();
  }

  /**
   * Stops whatever this panel is playing when it goes away. A broken panel never ticks again,
   * so without this its sounds kept playing and its strobes kept flashing on every client that
   * had them, until a new panel reused the same positions.
   */
  private void stopEverythingOnRemoval() {
    if (world == null || world.isRemote || channelActivePlayers.isEmpty()) {
      return;
    }
    stopAllChannels(world.getPlayers(EntityPlayerMP.class, p -> true));
  }

  /**
   * Checks if a specific player is within hearing range of any position in the list.
   */
  private boolean isPlayerInRangeOfAny(EntityPlayerMP player, List<BlockPos> positions,
      double hearingRangeSq) {
    for (BlockPos pos : positions) {
      if (player.getDistanceSq(pos) <= hearingRangeSq) {
        return true;
      }
    }
    return false;
  }

  private void rebuildApplianceCache() {
    cachedVoiceEvacPositions = new ArrayList<>();
    cachedHornGroups = new HashMap<>();
    cachedStrobeOnlyPositions = new ArrayList<>();
    cachedAllStrobePositions = new ArrayList<>();

    for (BlockPos bp : connectedAppliances) {
      if (!world.isBlockLoaded(bp)) continue;

      IBlockState blockStateAtPos = world.getBlockState(bp);
      Block blockAtPos = blockStateAtPos.getBlock();

      if (blockAtPos instanceof IStrobeBlock && world.getTileEntity(bp) == null) {
        world.setTileEntity(bp, new TileEntityFireAlarmStrobe());
      }

      boolean hasStrobe = blockAtPos instanceof IStrobeBlock;

      if (blockAtPos instanceof AbstractBlockFireAlarmSounderVoiceEvac) {
        cachedVoiceEvacPositions.add(bp);
        if (hasStrobe) cachedAllStrobePositions.add(bp);
      } else if (blockAtPos instanceof AbstractBlockFireAlarmSounder) {
        AbstractBlockFireAlarmSounder sounder = (AbstractBlockFireAlarmSounder) blockAtPos;
        String soundName;
        if (blockAtPos instanceof ISoundIndexBlock) {
          // Its tone is stored per placed block rather than in metadata, so it takes the world.
          soundName = ((ISoundIndexBlock) blockAtPos)
              .getSoundResourceName(world, bp, blockStateAtPos);
        } else {
          soundName = sounder.getSoundResourceName(blockStateAtPos);
        }
        if (soundName != null) {
          cachedHornGroups.computeIfAbsent(soundName, k -> new ArrayList<>()).add(bp);
          if (hasStrobe) cachedAllStrobePositions.add(bp);
        } else if (hasStrobe) {
          cachedStrobeOnlyPositions.add(bp);
          cachedAllStrobePositions.add(bp);
        }
      }
    }

    // Detectors with a strobe built in (the Gentex 710CS-C) are initiating devices, not
    // appliances, but their strobe flashes with the rest in an alarm
    for (BlockPos bp : initiatingDevices) {
      if (world.isBlockLoaded(bp) && world.getBlockState(bp).getBlock() instanceof IStrobeBlock) {
        cachedStrobeOnlyPositions.add(bp);
        cachedAllStrobePositions.add(bp);
      }
    }
  }

  public String getCurrentSoundResourceName() {
    return SOUND_RESOURCE_NAMES[soundIndex];
  }

  /** The panel block this tile entity belongs to, or {@code null} if it is not one. */
  private BlockFireAlarmControlPanel panelBlock() {
    if (world == null) {
      return null;
    }
    Block block = world.getBlockState(getPos()).getBlock();
    return block instanceof BlockFireAlarmControlPanel ? (BlockFireAlarmControlPanel) block : null;
  }

  /**
   * Linked appliances (horns, strobes, speakers) that are loaded but no longer a fire alarm
   * appliance -- broken or replaced. Works on either side, so the display can list them.
   *
   * @return the missing appliances' positions, in link order
   *
   * @since 2026.9
   */
  public List<BlockPos> getMissingAppliances() {
    List<BlockPos> missing = new ArrayList<>();
    if (world == null) {
      return missing;
    }
    for (BlockPos bp : connectedAppliances) {
      // really loaded: on the client a chunk it was never sent counts as loaded, and reads as air
      if (CsmChunks.isReallyLoaded(world, bp)
          && !(world.getBlockState(bp).getBlock() instanceof AbstractBlockFireAlarmSounder)) {
        missing.add(bp);
      }
    }
    return missing;
  }

  /**
   * Linked initiating devices (pull stations, detectors, sprinklers) that are loaded but no longer
   * one.
   *
   * @return the missing initiating devices' positions, in link order
   *
   * @since 2026.9
   */
  public List<BlockPos> getMissingInitiatingDevices() {
    List<BlockPos> missing = new ArrayList<>();
    if (world == null) {
      return missing;
    }
    for (BlockPos bp : initiatingDevices) {
      if (CsmChunks.isReallyLoaded(world, bp)
          && !(world.getTileEntity(bp) instanceof TileEntityFireAlarmSensor)) {
        missing.add(bp);
      }
    }
    return missing;
  }

  private int countMissingDevices() {
    return getMissingAppliances().size() + getMissingInitiatingDevices().size();
  }

  /**
   * Unlinks an appliance, as the linker does on a sneak-click.
   *
   * @param blockPos the appliance's position
   *
   * @return {@code true} if it was linked to this panel
   *
   * @since 2026.9
   */
  public synchronized boolean removeLinkedAlarm(BlockPos blockPos) {
    boolean removed = connectedAppliances.remove(blockPos);
    if (removed) {
      afterUnlink();
    }
    return removed;
  }

  /**
   * Unlinks an initiating device from the panel's index. The caller clears the device's own link.
   *
   * @param blockPos the device's position
   *
   * @return {@code true} if it was indexed on this panel
   *
   * @since 2026.9
   */
  public synchronized boolean removeLinkedInitiatingDevice(BlockPos blockPos) {
    boolean removed = initiatingDevices.remove(blockPos);
    if (removed) {
      afterUnlink();
    }
    return removed;
  }

  /**
   * Unlinks every device that is missing -- the only way to be rid of one, since a device that is
   * gone cannot be clicked.
   *
   * @return how many were unlinked
   *
   * @since 2026.9
   */
  public synchronized int removeMissingDevices() {
    List<BlockPos> appliances = getMissingAppliances();
    List<BlockPos> initiating = getMissingInitiatingDevices();
    connectedAppliances.removeAll(appliances);
    initiatingDevices.removeAll(initiating);
    int removed = appliances.size() + initiating.size();
    if (removed > 0) {
      afterUnlink();
    }
    return removed;
  }

  /**
   * Unlinks every appliance and indexed initiating device whose position matches, with one sync at
   * the end. The caller clears the initiating devices' own links.
   *
   * @param which the positions to unlink
   *
   * @return how many were unlinked
   *
   * @since 2026.10
   */
  public synchronized int removeLinkedDevices(Predicate<BlockPos> which) {
    int before = connectedAppliances.size() + initiatingDevices.size();
    connectedAppliances.removeIf(which);
    initiatingDevices.removeIf(which);
    int removed = before - connectedAppliances.size() - initiatingDevices.size();
    if (removed > 0) {
      afterUnlink();
    }
    return removed;
  }

  /**
   * Saves and syncs once after many devices were linked through {@link #addLinkedAlarm} and
   * {@link #addLinkedInitiatingDevice}, which do neither on their own.
   *
   * @since 2026.10
   */
  public void afterBulkLink() {
    cachedVoiceEvacPositions = null;
    markDirty();
    if (world != null && !world.isRemote) {
      syncServerToClient(world);
    }
  }

  private void afterUnlink() {
    cachedVoiceEvacPositions = null;
    int missing = countMissingDevices();
    acknowledgedMissing = Math.min(acknowledgedMissing, missing);
    // Unlinking the last missing device resolves the trouble; there is nothing left to ACK
    if (missing == 0) {
      trouble = false;
    }
    markDirty();
    if (world != null && !world.isRemote) {
      syncServerToClient(world);
    }
  }

  /**
   * Raises trouble when more devices are missing than the last ACK covered. An empty panel is
   * not in trouble here (the display still says NO APPLIANCES LINKED): a panel that has just been
   * placed should not beep until it has been set up.
   */
  private void updateTrouble() {
    int missing = countMissingDevices();
    if (missing > acknowledgedMissing && !trouble) {
      trouble = true;
      markDirty();
      syncServerToClient(world);
    }
    if (missing < acknowledgedMissing) {
      acknowledgedMissing = missing;
    }
  }

  /**
   * The panel's own buzzer: its alarm sound while an alarm is neither acknowledged nor silenced,
   * otherwise its trouble sound while a trouble is latched, otherwise quiet. It plays on a channel
   * of its own at the panel, so it never disturbs the appliances' channels.
   */
  private void manageBuzzer(List<EntityPlayerMP> players) {
    BlockFireAlarmControlPanel block = panelBlock();
    String channel = CHANNEL_BUZZER_PREFIX + getPos().getX() + "_" + getPos().getY() + "_"
        + getPos().getZ();
    LifeSafetySounds sound = null;
    if (block != null && alarm && !acknowledged && !audibleSilence) {
      sound = block.getBuzzerAlarmSound();
    } else if (block != null && trouble) {
      sound = block.getBuzzerTroubleSound();
    }
    if (sound == null) {
      if (lastBuzzerSound != null) {
        stopChannel(players, channel);
        lastBuzzerSound = null;
      }
      return;
    }
    String resource = "csm:" + sound.getSoundName();
    if (!resource.equals(lastBuzzerSound)) {
      stopChannel(players, channel);
      lastBuzzerSound = resource;
    }
    manageSoundForPlayers(players, Collections.singletonList(getPos()), channel, resource,
        BUZZER_HEARING_RANGE);
  }

  /** Plays a one-off tone at the panel (the reset chirp). */
  private void playBuzzerTone(LifeSafetySounds sound) {
    if (sound == null || world == null || world.isRemote) {
      return;
    }
    SoundEvent event = sound.getSoundEvent();
    if (event != null) {
      world.playSound(null, getPos(), event, SoundCategory.BLOCKS, 1.0F, 1.0F);
    }
  }
}

package com.micatechnologies.minecraft.csm.lifesafety;

import com.micatechnologies.minecraft.csm.CsmNetwork;
import com.micatechnologies.minecraft.csm.api.firealarm.CsmFireAlarmQuery;
import com.micatechnologies.minecraft.csm.api.firealarm.FireAlarmEvent;
import com.micatechnologies.minecraft.csm.api.firealarm.FireAlarmPanelRegistry;
import com.micatechnologies.minecraft.csm.codeutils.AbstractTickableTileEntity;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.FMLCommonHandler;

public class TileEntityFireAlarmControlPanel extends AbstractTickableTileEntity {

  private static final int tickRate = 20;

  private static final String soundIndexKey = "soundIndex";
  private static final String alarmKey = "alarm";
  private static final String alarmStormKey = "alarmStorm";
  private static final String connectedAppliancesKey = "connectedAppliances";
  private static final String alarmAnnouncedKey = "alarmAnnounced";
  private static final String audibleSilenceKey = "audibleSilence";
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
      "csm:firecom8500"};
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
      "Firecom 8500"};
  private static final String STORM_SOUND_NAME = "csm:notifier_tornado_voice_evac";
  private static final float SOUNDER_VOLUME = 2.0f;
  private static final float VOICE_EVAC_VOLUME = 3.0f;
  private static final float STORM_VOICE_EVAC_VOLUME = 3.0f;
  private static final int PRUNE_INTERVAL_TICKS = 6000; // ~5 minutes

  private static final String CHANNEL_VOICE_EVAC = "voiceevac";
  private static final String CHANNEL_STORM = "storm";
  private static final String CHANNEL_STROBE_ONLY = "strobeonly";

  private final ArrayList<BlockPos> connectedAppliances = new ArrayList<>();
  private int soundIndex;
  private boolean alarm;
  private boolean alarmStorm;
  private boolean alarmAnnounced;
  private boolean audibleSilence;
  private boolean alarmWasActive = false;
  private int pruneTickCounter = 0;

  // Channel-based active player tracking (voice evac, storm, and each horn sound)
  private final Map<String, HashSet<UUID>> channelActivePlayers = new HashMap<>();
  private final Set<String> lastActiveChannels = new HashSet<>();
  private String lastVoiceEvacSoundSent = null;

  @Override
  public void readNBT(NBTTagCompound compound) {
    soundIndex = compound.hasKey(soundIndexKey) ? compound.getInteger(soundIndexKey) : 0;
    alarm = compound.hasKey(alarmKey) && compound.getBoolean(alarmKey);
    alarmStorm = compound.hasKey(alarmStormKey) && compound.getBoolean(alarmStormKey);
    alarmAnnounced = compound.hasKey(alarmAnnouncedKey) && compound.getBoolean(alarmAnnouncedKey);
    audibleSilence = compound.hasKey(audibleSilenceKey) && compound.getBoolean(audibleSilenceKey);

    connectedAppliances.clear();
    if (compound.hasKey(connectedAppliancesKey)) {
      String[] positions = compound.getString(connectedAppliancesKey).split("\n");
      for (String position : positions) {
        String[] coordinates = position.split(" ");
        if (coordinates.length == 3) {
          connectedAppliances.add(
              new BlockPos(Integer.parseInt(coordinates[0]), Integer.parseInt(coordinates[1]),
                  Integer.parseInt(coordinates[2])));
        }
      }
    }

    // Re-register with the API registry if this panel was saved with an active alarm
    if (world != null && !world.isRemote) {
      int dim = world.provider.getDimension();
      if (alarm) {
        FireAlarmPanelRegistry.registerFireAlarm(dim, getPos());
      }
      if (alarmStorm) {
        FireAlarmPanelRegistry.registerStormAlarm(dim, getPos());
      }
    }
  }

  @Override
  public NBTTagCompound writeNBT(NBTTagCompound compound) {
    compound.setInteger(soundIndexKey, soundIndex);
    compound.setBoolean(alarmKey, alarm);
    compound.setBoolean(alarmStormKey, alarmStorm);
    compound.setBoolean(alarmAnnouncedKey, alarmAnnounced);
    compound.setBoolean(audibleSilenceKey, audibleSilence);

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
    alarm = alarmState;
    markDirty();

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
    }
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

    if (world != null && !world.isRemote && audibleSilenceState) {
      MinecraftForge.EVENT_BUS.post(new FireAlarmEvent.AudibleSilenced(world, getPos()));
    }
  }

  public String getStatusString() {
    if (alarm && audibleSilence) {
      return "Audible Silence";
    } else if (alarm) {
      return "Alarm Active";
    }
    return "Normal";
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
    if (world != null && !world.isRemote) {
      FireAlarmPanelRegistry.unregisterAll(world.provider.getDimension(), getPos());
    }
    super.invalidate();
  }

  @Override
  public void onChunkUnload() {
    if (world != null && !world.isRemote) {
      FireAlarmPanelRegistry.unregisterAll(world.provider.getDimension(), getPos());
    }
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
      // Periodic lightweight pruning of invalid connected appliances
      pruneTickCounter += tickRate;
      if (pruneTickCounter >= PRUNE_INTERVAL_TICKS) {
        pruneTickCounter = 0;
        pruneInvalidAppliances();
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
                    "has been activated!"));
            setAlarmAnnouncedState(true);
          }
        }

        // Collect voice evac speaker positions and group horn positions by sound
        String voiceEvacSoundName = getCurrentSoundResourceName();
        float voiceEvacHearingRange = VOICE_EVAC_VOLUME * 16.0f;
        List<BlockPos> voiceEvacPositions = new ArrayList<>();
        Map<String, List<BlockPos>> hornGroups = new HashMap<>();
        List<BlockPos> strobeOnlyPositions = new ArrayList<>();

        for (BlockPos bp : connectedAppliances) {
          // Skip appliances in unloaded chunks to avoid false-negative categorization
          // that would cause sound channels to be stopped and restarted
          if (!world.isBlockLoaded(bp)) {
            continue;
          }

          IBlockState blockStateAtPos = world.getBlockState(bp);
          Block blockAtPos = blockStateAtPos.getBlock();

          // Ensure strobe blocks have their TE (migration for blocks placed before TESR)
          if (blockAtPos instanceof IStrobeBlock && world.getTileEntity(bp) == null) {
            world.setTileEntity(bp, new TileEntityFireAlarmStrobe());
          }

          if (blockAtPos instanceof AbstractBlockFireAlarmSounderVoiceEvac) {
            voiceEvacPositions.add(bp);
          } else if (blockAtPos instanceof AbstractBlockFireAlarmSounder) {
            AbstractBlockFireAlarmSounder sounder = (AbstractBlockFireAlarmSounder) blockAtPos;
            String soundName;
            // Check for tile entity-based sound selection (e.g., Gentex Commander 3)
            if (blockAtPos instanceof BlockFireAlarmGentexCommander3Red) {
              soundName = ((BlockFireAlarmGentexCommander3Red) blockAtPos)
                  .getSoundResourceName(world, bp, blockStateAtPos);
            } else if (blockAtPos instanceof BlockFireAlarmGentexCommander3White) {
              soundName = ((BlockFireAlarmGentexCommander3White) blockAtPos)
                  .getSoundResourceName(world, bp, blockStateAtPos);
            } else {
              soundName = sounder.getSoundResourceName(blockStateAtPos);
            }
            if (soundName != null) {
              hornGroups.computeIfAbsent(soundName, k -> new ArrayList<>()).add(bp);
            } else if (blockAtPos instanceof IStrobeBlock) {
              strobeOnlyPositions.add(bp);
            }
          }
        }

        // Track which channels are active this tick
        Set<String> currentActiveChannels = new HashSet<>();

        if (audibleSilence) {
          // Audible silence: stop all horn and voice evac sounds, keep strobes active
          stopChannel(players, CHANNEL_VOICE_EVAC);
          float hornHearingRange = SOUNDER_VOLUME * 16.0f;
          for (String hornChannel : hornGroups.keySet()) {
            stopChannel(players, hornChannel);
          }

          // Collect all strobe positions (from strobe-only devices and from horn/voice evac
          // devices that have strobes) so strobes remain active during audible silence
          List<BlockPos> allStrobePositions = new ArrayList<>(strobeOnlyPositions);
          for (BlockPos bp : voiceEvacPositions) {
            if (world.getBlockState(bp).getBlock() instanceof IStrobeBlock) {
              allStrobePositions.add(bp);
            }
          }
          for (List<BlockPos> hornPositions : hornGroups.values()) {
            for (BlockPos bp : hornPositions) {
              if (world.getBlockState(bp).getBlock() instanceof IStrobeBlock) {
                allStrobePositions.add(bp);
              }
            }
          }

          if (!allStrobePositions.isEmpty()) {
            manageSoundForPlayers(players, allStrobePositions, CHANNEL_STROBE_ONLY,
                "", hornHearingRange);
            currentActiveChannels.add(CHANNEL_STROBE_ONLY);
          }
        } else {
          // Normal alarm: manage all sounds

          // Voice evac: manage client-side MovingSound via packets
          if (!voiceEvacPositions.isEmpty()) {
            // If the sound changed (user switched voice evac), restart on all active clients
            boolean soundChanged = lastVoiceEvacSoundSent != null &&
                !lastVoiceEvacSoundSent.equals(voiceEvacSoundName);
            if (soundChanged) {
              stopChannel(players, CHANNEL_VOICE_EVAC);
            }

            manageSoundForPlayers(players, voiceEvacPositions, CHANNEL_VOICE_EVAC,
                voiceEvacSoundName, voiceEvacHearingRange);
            lastVoiceEvacSoundSent = voiceEvacSoundName;
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

        // Handle storm alarm
        if (alarmStorm) {
          float stormHearingRange = STORM_VOICE_EVAC_VOLUME * 16.0f;
          List<BlockPos> stormVoiceEvacPositions = new ArrayList<>();

          for (BlockPos bp : connectedAppliances) {
            if (!world.isBlockLoaded(bp)) {
              continue;
            }
            IBlockState blockStateAtPos = world.getBlockState(bp);
            Block blockAtPos = blockStateAtPos.getBlock();
            if (blockAtPos instanceof AbstractBlockFireAlarmSounderVoiceEvac) {
              stormVoiceEvacPositions.add(bp);
            }
          }

          if (!stormVoiceEvacPositions.isEmpty()) {
            manageSoundForPlayers(players, stormVoiceEvacPositions, CHANNEL_STORM,
                STORM_SOUND_NAME, stormHearingRange);
          }
        } else {
          // Storm alarm off - stop storm sounds
          stopChannel(players, CHANNEL_STORM);
        }
      }
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
    double hearingRangeSq = hearingRange * (double) hearingRange;
    HashSet<UUID> activePlayers =
        channelActivePlayers.computeIfAbsent(channel, k -> new HashSet<>());

    for (EntityPlayerMP player : players) {
      UUID playerId = player.getUniqueID();
      boolean inRange = isPlayerInRangeOfAny(player, positions, hearingRangeSq);

      if (inRange && !activePlayers.contains(playerId)) {
        // Player entered range - start their client-side MovingSound
        CsmNetwork.sendTo(
            FireAlarmSoundPacket.start(channel, soundName, hearingRange, positions), player);
        activePlayers.add(playerId);
      } else if (!inRange && activePlayers.contains(playerId)) {
        // Player left range - stop their client-side MovingSound for this channel
        CsmNetwork.sendTo(FireAlarmSoundPacket.stop(channel), player);
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
    FireAlarmSoundPacket stopPacket = FireAlarmSoundPacket.stop(channel);
    for (EntityPlayerMP player : players) {
      if (activePlayers.contains(player.getUniqueID())) {
        CsmNetwork.sendTo(stopPacket, player);
      }
    }
    activePlayers.clear();
  }

  /**
   * Stops all channels: sends a stop-all packet to every player that has any active sound.
   */
  private void stopAllChannels(List<EntityPlayerMP> players) {
    // Collect all players with any active channel
    HashSet<UUID> allActive = new HashSet<>();
    for (HashSet<UUID> active : channelActivePlayers.values()) {
      allActive.addAll(active);
    }
    if (allActive.isEmpty()) {
      return;
    }
    FireAlarmSoundPacket stopAllPacket = FireAlarmSoundPacket.stopAll();
    for (EntityPlayerMP player : players) {
      if (allActive.contains(player.getUniqueID())) {
        CsmNetwork.sendTo(stopAllPacket, player);
      }
    }
    channelActivePlayers.clear();
    lastActiveChannels.clear();
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

  /**
   * Removes connected appliance entries that no longer point to valid fire alarm sounder blocks.
   * Only prunes one invalid entry per call to keep the operation lightweight.
   */
  private void pruneInvalidAppliances() {
    Iterator<BlockPos> it = connectedAppliances.iterator();
    boolean pruned = false;
    while (it.hasNext()) {
      BlockPos bp = it.next();
      if (world.isBlockLoaded(bp)) {
        Block blockAtPos = world.getBlockState(bp).getBlock();
        if (!(blockAtPos instanceof AbstractBlockFireAlarmSounder)) {
          it.remove();
          pruned = true;
          break;
        }
      }
    }
    if (pruned) {
      markDirty();
    }
  }

  public String getCurrentSoundResourceName() {
    return SOUND_RESOURCE_NAMES[soundIndex];
  }
}

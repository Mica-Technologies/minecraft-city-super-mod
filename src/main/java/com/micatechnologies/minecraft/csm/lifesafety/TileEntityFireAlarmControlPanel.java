package com.micatechnologies.minecraft.csm.lifesafety;

import com.micatechnologies.minecraft.csm.CsmNetwork;
import com.micatechnologies.minecraft.csm.codeutils.AbstractTickableTileEntity;
import java.util.ArrayList;
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
import net.minecraftforge.fml.common.FMLCommonHandler;

public class TileEntityFireAlarmControlPanel extends AbstractTickableTileEntity {

  private static final int tickRate = 20;

  private static final String soundIndexKey = "soundIndex";
  private static final String alarmKey = "alarm";
  private static final String alarmStormKey = "alarmStorm";
  private static final String connectedAppliancesKey = "connectedAppliances";
  private static final String alarmAnnouncedKey = "alarmAnnounced";
  private static final String[] SOUND_RESOURCE_NAMES = {"csm:svenew",
      "csm:sveold",
      "csm:simplex_voice_evac_old_alt",
      "csm:mills_firealarm",
      "csm:lms_voice_evac",
      "csm:notifier_voice_evac",
      "csm:notifier_voice_evac_alt",
      "csm:notifier_voice_evac_alt2",
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
  private boolean alarmWasActive = false;
  private int pruneTickCounter = 0;

  // Channel-based active player tracking (voice evac, storm, and each horn sound)
  private final Map<String, HashSet<UUID>> channelActivePlayers = new HashMap<>();
  private final Set<String> lastActiveChannels = new HashSet<>();
  private String lastVoiceEvacSoundSent = null;

  @Override
  public void readNBT(NBTTagCompound compound) {
    try {
      soundIndex = compound.getInteger(soundIndexKey);
    } catch (Exception e) {
      soundIndex = 0;
    }

    try {
      alarm = compound.getBoolean(alarmKey);
    } catch (Exception e) {
      alarm = false;
    }

    try {
      alarmStorm = compound.getBoolean(alarmStormKey);
    } catch (Exception e) {
      alarmStorm = false;
    }

    try {
      alarmAnnounced = compound.getBoolean(alarmAnnouncedKey);
    } catch (Exception e) {
      alarmAnnounced = false;
    }

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
  }

  @Override
  public NBTTagCompound writeNBT(NBTTagCompound compound) {
    compound.setInteger(soundIndexKey, soundIndex);
    compound.setBoolean(alarmKey, alarm);
    compound.setBoolean(alarmStormKey, alarmStorm);
    compound.setBoolean(alarmAnnouncedKey, alarmAnnounced);

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

  public boolean getAlarmState() {
    return alarm;
  }

  public boolean getAlarmAnnouncedState() {
    return alarmAnnounced;
  }

  public void setAlarmState(boolean alarmState) {
    alarm = alarmState;
    markDirty();
  }

  public void setAlarmStormState(boolean alarmStormState) {
    alarmStorm = alarmStormState;
    markDirty();
  }

  public void setAlarmAnnouncedState(boolean alarmAnnouncedState) {
    alarmAnnounced = alarmAnnouncedState;
    markDirty();
  }

  public String getCurrentSoundName() {
    return SOUND_NAMES[soundIndex];
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
          IBlockState blockStateAtPos = world.getBlockState(bp);
          Block blockAtPos = blockStateAtPos.getBlock();

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
      System.err.println("An error occurred while ticking a fire alarm control panel: ");
      e.printStackTrace(System.err);
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

    // Clean up players who disconnected
    activePlayers.removeIf(id -> {
      for (EntityPlayerMP p : players) {
        if (p.getUniqueID().equals(id)) {
          return false;
        }
      }
      return true;
    });
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

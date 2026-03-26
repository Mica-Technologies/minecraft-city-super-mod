package com.micatechnologies.minecraft.csm.lifesafety;

import com.micatechnologies.minecraft.csm.CsmNetwork;
import com.micatechnologies.minecraft.csm.codeutils.AbstractTickableTileEntity;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
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
  private static final int[] SOUND_LENGTHS = {2100, 560, 560, 755, 700, 520, 520, 440, 460, 600,660};
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
  private static final int STORM_SOUND_LENGTH = 460;
  private static final float SOUNDER_VOLUME = 2.0f;
  private static final float VOICE_EVAC_VOLUME = 3.0f;
  private static final float STORM_VOICE_EVAC_VOLUME = 3.0f;
  private static final int PRUNE_INTERVAL_TICKS = 6000; // ~5 minutes

  private final ArrayList<BlockPos> connectedAppliances = new ArrayList<>();
  private int soundIndex;
  private boolean alarm;
  private boolean alarmStorm;
  private int alarmStormSoundTracking = 0;
  private HashMap<String, Integer> alarmSoundTracking = null;
  private boolean alarmAnnounced;
  private int pruneTickCounter = 0;
  private final HashSet<UUID> voiceEvacActivePlayers = new HashSet<>();
  private final HashSet<UUID> stormActivePlayers = new HashSet<>();
  private String lastVoiceEvacSoundSent = null;
  private String lastStormSoundSent = null;

  /**
   * Abstract method which must be implemented to process the reading of the tile entity's NBT data
   * from the supplied NBT tag compound.
   *
   * @param compound the NBT tag compound to read the tile entity's NBT data from
   */
  @Override
  public void readNBT(NBTTagCompound compound) {

    // Read sound index
    try {
      soundIndex = compound.getInteger(soundIndexKey);
    } catch (Exception e) {
      soundIndex = 0;
    }

    // Read alarm state
    try {
      alarm = compound.getBoolean(alarmKey);
    } catch (Exception e) {
      alarm = false;
    }

    // Read alarm storm state
    try {
      alarmStorm = compound.getBoolean(alarmStormKey);
    } catch (Exception e) {
      alarmStorm = false;
    }

    // Read alarm announced state
    try {
      alarmAnnounced = compound.getBoolean(alarmAnnouncedKey);
    } catch (Exception e) {
      alarmAnnounced = false;
    }

    // Read connected appliance locations
    connectedAppliances.clear();
    if (compound.hasKey(connectedAppliancesKey)) {
      // Split into each block position
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

  /**
   * Abstract method which must be implemented to return the NBT tag compound with the tile entity's
   * NBT data.
   *
   * @param compound the NBT tag compound to write the tile entity's NBT data to
   *
   * @return the NBT tag compound with the tile entity's NBT data
   */
  @Override
  public NBTTagCompound writeNBT(NBTTagCompound compound) {
    // Write sound index
    compound.setInteger(soundIndexKey, soundIndex);

    // Write alarm state
    compound.setBoolean(alarmKey, alarm);

    // Write alarm storm state
    compound.setBoolean(alarmStormKey, alarmStorm);

    // Write alarm announced state
    compound.setBoolean(alarmAnnouncedKey, alarmAnnounced);

    // Write connected appliance locations
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

  /**
   * Abstract method which must be implemented to return a boolean indicating if the tile entity
   * should also tick on the client side. By default, the tile entity will always tick on the server
   * side, and in the event of singleplayer/local mode, the host client is considered the server.
   *
   * @return a boolean indicating if the tile entity should also tick on the client side
   */
  @Override
  public boolean doClientTick() {
    return false;
  }

  /**
   * Abstract method which must be implemented to return a boolean indicating if the tile entity
   * ticking should be paused. If the tile entity is paused, the tick event will not be called.
   *
   * @return a boolean indicating if the tile entity ticking should be paused
   */
  @Override
  public boolean pauseTicking() {
    return false;
  }

  /**
   * Abstract method which must be implemented to return the tick rate of the tile entity.
   *
   * @return the tick rate of the tile entity
   */
  @Override
  public long getTickRate() {
    return tickRate;
  }

  /**
   * Abstract method which must be implemented to handle the tick event of the tile entity.
   */
  @Override
  public void onTick() {
    // Only process on server side (doClientTick is false, but guard just in case)
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
        // Reset storm alarm (fire alarm overrides storm)
        if (alarmStormSoundTracking > 0) {
          alarmStormSoundTracking = 0;
        }
        // Stop storm sounds on clients if storm was playing
        if (!stormActivePlayers.isEmpty()) {
          sendStopToPlayers(players, stormActivePlayers);
          lastStormSoundSent = null;
        }

        // Alarm is starting
        if (alarmSoundTracking == null) {
          alarmSoundTracking = new HashMap<>();

          // Announce alarm if not announced
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

        // Collect voice evac speaker positions
        String voiceEvacSoundName = getCurrentSoundResourceName();
        float voiceEvacHearingRange = VOICE_EVAC_VOLUME * 16.0f;
        List<BlockPos> voiceEvacPositions = new ArrayList<>();

        for (BlockPos bp : connectedAppliances) {
          IBlockState blockStateAtPos = world.getBlockState(bp);
          Block blockAtPos = blockStateAtPos.getBlock();

          if (blockAtPos instanceof AbstractBlockFireAlarmSounderVoiceEvac) {
            voiceEvacPositions.add(bp);
          } else if (blockAtPos instanceof AbstractBlockFireAlarmSounder) {
            // Horn/sounder devices: positional audio (short sounds, loop quickly)
            AbstractBlockFireAlarmSounder blockFireAlarmSounder =
                (AbstractBlockFireAlarmSounder) blockAtPos;
            String alarmSoundName = blockFireAlarmSounder.getSoundResourceName(blockStateAtPos);
            int alarmSoundLength = blockFireAlarmSounder.getSoundTickLen(blockStateAtPos);

            if (alarmSoundName != null && alarmSoundLength != -1) {
              if (!alarmSoundTracking.containsKey(alarmSoundName) ||
                  alarmSoundTracking.get(alarmSoundName) > alarmSoundLength) {
                alarmSoundTracking.put(alarmSoundName, 0);
              }

              if (alarmSoundTracking.get(alarmSoundName) == 0) {
                if (isAnyPlayerInRange(players, bp, SOUNDER_VOLUME)) {
                  world.playSound(null, bp.getX(), bp.getY(), bp.getZ(),
                      SoundEvent.REGISTRY.getObject(new ResourceLocation(alarmSoundName)),
                      SoundCategory.AMBIENT, SOUNDER_VOLUME, (float) 1);
                }
              }
            }
          }
        }

        // Voice evac: manage client-side MovingSound via packets
        if (!voiceEvacPositions.isEmpty()) {
          // If the sound changed (user switched voice evac), restart on all active clients
          boolean soundChanged = lastVoiceEvacSoundSent != null &&
              !lastVoiceEvacSoundSent.equals(voiceEvacSoundName);
          if (soundChanged) {
            sendStopToPlayers(players, voiceEvacActivePlayers);
          }

          // Send start packets to players who are in range but don't have the sound yet
          manageVoiceEvacForPlayers(players, voiceEvacPositions, voiceEvacSoundName,
              voiceEvacHearingRange, voiceEvacActivePlayers);
          lastVoiceEvacSoundSent = voiceEvacSoundName;
        }

        // Increment sound trackers (for horns/sounders only now)
        final int incrementSize = tickRate;
        HashMap<String, Integer> updatedTracking = new HashMap<>();
        for (HashMap.Entry<String, Integer> entry : alarmSoundTracking.entrySet()) {
          updatedTracking.put(entry.getKey(), entry.getValue() + incrementSize);
        }
        alarmSoundTracking = updatedTracking;
      } else {
        // Alarm has ended
        if (alarmSoundTracking != null) {
          alarmSoundTracking = null;
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
        }

        // Stop voice evac sounds on all clients
        if (!voiceEvacActivePlayers.isEmpty()) {
          sendStopToPlayers(players, voiceEvacActivePlayers);
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
            manageVoiceEvacForPlayers(players, stormVoiceEvacPositions, STORM_SOUND_NAME,
                stormHearingRange, stormActivePlayers);
            lastStormSoundSent = STORM_SOUND_NAME;
          }
        } else {
          // Storm alarm off - stop storm sounds
          if (!stormActivePlayers.isEmpty()) {
            sendStopToPlayers(players, stormActivePlayers);
            lastStormSoundSent = null;
          }
          if (alarmStormSoundTracking > 0) {
            alarmStormSoundTracking = 0;
          }
        }
      }
    } catch (Exception e) {
      System.err.println("An error occurred while ticking a fire alarm control panel: ");
      e.printStackTrace(System.err);
    }
  }

  /**
   * Manages the client-side voice evac MovingSound for all players. Sends start packets to
   * players who are in range of a speaker but don't have the sound playing yet. Sends stop
   * packets to players who have moved out of range of all speakers.
   */
  private void manageVoiceEvacForPlayers(List<EntityPlayerMP> players,
      List<BlockPos> speakerPositions, String soundName, float hearingRange,
      HashSet<UUID> activePlayers) {
    double hearingRangeSq = hearingRange * (double) hearingRange;

    for (EntityPlayerMP player : players) {
      UUID playerId = player.getUniqueID();
      boolean inRange = isPlayerInRangeOfAny(player, speakerPositions, hearingRangeSq);

      if (inRange && !activePlayers.contains(playerId)) {
        // Player entered range - start their client-side MovingSound
        CsmNetwork.sendTo(
            FireAlarmSoundPacket.start(soundName, hearingRange, speakerPositions), player);
        activePlayers.add(playerId);
      } else if (!inRange && activePlayers.contains(playerId)) {
        // Player left range - stop their client-side MovingSound
        CsmNetwork.sendTo(FireAlarmSoundPacket.stop(), player);
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
   * Sends stop packets to all players in the active set and clears it.
   */
  private void sendStopToPlayers(List<EntityPlayerMP> players, HashSet<UUID> activePlayers) {
    FireAlarmSoundPacket stopPacket = FireAlarmSoundPacket.stop();
    for (EntityPlayerMP player : players) {
      if (activePlayers.contains(player.getUniqueID())) {
        CsmNetwork.sendTo(stopPacket, player);
      }
    }
    activePlayers.clear();
  }

  /**
   * Checks if any player is within hearing range of the given position. The hearing range is
   * derived from the sound volume (volume * 16 blocks, matching Minecraft's sound attenuation).
   */
  private boolean isAnyPlayerInRange(List<EntityPlayerMP> players, BlockPos pos, float volume) {
    double rangeSq = (volume * 16.0) * (volume * 16.0);
    for (EntityPlayerMP player : players) {
      if (player.getDistanceSq(pos) <= rangeSq) {
        return true;
      }
    }
    return false;
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
      // Only check loaded chunks to avoid forcing chunk loads
      if (world.isBlockLoaded(bp)) {
        Block blockAtPos = world.getBlockState(bp).getBlock();
        if (!(blockAtPos instanceof AbstractBlockFireAlarmSounder)) {
          it.remove();
          pruned = true;
          // Only prune one per cycle to stay lightweight
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

  public int getCurrentSoundLength() {
    return SOUND_LENGTHS[soundIndex];
  }
}

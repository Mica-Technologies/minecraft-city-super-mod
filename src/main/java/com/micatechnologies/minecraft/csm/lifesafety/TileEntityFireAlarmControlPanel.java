package com.micatechnologies.minecraft.csm.lifesafety;

import com.micatechnologies.minecraft.csm.codeutils.AbstractTickableTileEntity;
import java.util.ArrayList;
import java.util.HashMap;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
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
      "csm:mclalsve"};
  private static final int[] SOUND_LENGTHS = {2100, 560, 560, 755, 700, 520, 520, 440, 460, 600};
  private static final String[] SOUND_NAMES = {"Simplex Voice Evac 1",
      "Simplex Voice Evac 2",
      "Simplex Voice Evac 3",
      "Notifier Voice Evac 1",
      "Notifier Voice Evac 2",
      "Notifier Voice Evac 3",
      "Notifier Voice Evac 4",
      "Notifier Voice Evac 5",
      "Notifier Voice Evac 6",
      "Mica Voice Evac 1"};
  private static final String STORM_SOUND_NAME = "csm:notifier_tornado_voice_evac";
  private static final int STORM_SOUND_LENGTH = 460;
  private final ArrayList<BlockPos> connectedAppliances = new ArrayList<>();
  private int soundIndex;
  private boolean alarm;
  private boolean alarmStorm;
  private int alarmStormSoundTracking = 0;
  private HashMap<String, Integer> alarmSoundTracking = null;
  private boolean alarmAnnounced;

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
    return true;
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

    try {
      if (alarm) {
        // Reset storm alarm (fire alarm overrides storm)
        if (alarmStormSoundTracking > 0) {
          alarmStormSoundTracking = 0;
        }

        // Alarm is starting
        if (alarmSoundTracking == null) {
          alarmSoundTracking = new HashMap<>();

          // Announce alarm if not announced
          if (!getAlarmAnnouncedState()) {
            MinecraftServer mcserv = FMLCommonHandler.instance().getMinecraftServerInstance();
            if (mcserv != null) {
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
            }
            setAlarmAnnouncedState(true);
          }
        }

        // Perform sound handling
        for (BlockPos bp : connectedAppliances) {
          // Get block at linked position
          IBlockState blockStateAtPos = world.getBlockState(bp);
          Block blockAtPos = blockStateAtPos.getBlock();

          // Check for alarm sound values at location
          String alarmSoundName = null;
          int alarmSoundLength = -1;
          if (blockAtPos instanceof AbstractBlockFireAlarmSounderVoiceEvac) {
            alarmSoundName = getCurrentSoundResourceName();
            alarmSoundLength = getCurrentSoundLength();
          } else if (blockAtPos instanceof AbstractBlockFireAlarmSounder) {
            AbstractBlockFireAlarmSounder blockFireAlarmSounder =
                (AbstractBlockFireAlarmSounder) blockAtPos;
            alarmSoundName = blockFireAlarmSounder.getSoundResourceName(blockStateAtPos);
            alarmSoundLength = blockFireAlarmSounder.getSoundTickLen(blockStateAtPos);
          }

          // Handle only if alarm at location
          if (alarmSoundName != null && alarmSoundLength != -1) {
            // Add sound tracker if does not exist and reset sound tracker to 0 if sound length
            // reached (or
            // greater)
            if (!alarmSoundTracking.containsKey(alarmSoundName) ||
                alarmSoundTracking.get(alarmSoundName) > alarmSoundLength) {
              alarmSoundTracking.put(alarmSoundName, 0);
            }

            // Play sound
            if (alarmSoundTracking.get(alarmSoundName) == 0) {
              world.playSound(null, bp.getX(), bp.getY(), bp.getZ(),
                  net.minecraft.util.SoundEvent.REGISTRY.getObject(
                      new ResourceLocation(alarmSoundName)), SoundCategory.AMBIENT,
                  (float) 2, (float) 1);

            }
          }
        }

        // Increment sound trackers
        final int incrementSize = tickRate;
        for (String key : alarmSoundTracking.keySet()) {
          int valForKey = alarmSoundTracking.get(key);
          alarmSoundTracking.put(key, valForKey + incrementSize);
        }
      } else {
        // Alarm has ended
        if (alarmSoundTracking != null) {
          alarmSoundTracking = null;
          MinecraftServer mcserv = FMLCommonHandler.instance().getMinecraftServerInstance();
          if (mcserv != null) {
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
          }
          setAlarmAnnouncedState(false);
        }

        // Handle storm alarm
        if (alarmStorm) {
          // Perform sound handling
          for (BlockPos bp : connectedAppliances) {
            // Get block at linked position
            IBlockState blockStateAtPos = world.getBlockState(bp);
            Block blockAtPos = blockStateAtPos.getBlock();

            // Check for alarm sound values at location
            if (blockAtPos instanceof AbstractBlockFireAlarmSounderVoiceEvac) {
              if (alarmStormSoundTracking > STORM_SOUND_LENGTH) {
                alarmStormSoundTracking = 0;
              }

              // Play sound
              if (alarmStormSoundTracking == 0) {
                world.playSound(null, bp.getX(), bp.getY(), bp.getZ(),
                    net.minecraft.util.SoundEvent.REGISTRY.getObject(
                        new ResourceLocation(STORM_SOUND_NAME)), SoundCategory.AMBIENT,
                    (float) 3, (float) 1);

              }
            }
          }

          // Increment storm sound tracker
          alarmStormSoundTracking += tickRate;
        } else {
          // Reset storm alarm
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

  public String getCurrentSoundResourceName() {
    return SOUND_RESOURCE_NAMES[soundIndex];
  }

  public int getCurrentSoundLength() {
    return SOUND_LENGTHS[soundIndex];
  }
}

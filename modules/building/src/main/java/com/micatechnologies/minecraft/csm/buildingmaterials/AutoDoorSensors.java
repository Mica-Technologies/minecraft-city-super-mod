package com.micatechnologies.minecraft.csm.buildingmaterials;

import com.micatechnologies.minecraft.csm.CsmConfig;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nonnull;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import net.minecraft.world.storage.MapStorage;
import net.minecraft.world.storage.WorldSavedData;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

/**
 * The approach sensor of the automatic sliding doors ({@link BlockAutoSlidingDoor}): a door opens
 * when a player comes within {@link CsmConfig#getAutoDoorSensorRange()} blocks of either face, as a
 * real door's header-mounted motion sensor would, and shuts by its own scheduled tick a few seconds
 * after the last one has gone. It replaces the activation mat ({@link BlockAutoDoorMat}) as the way
 * a sliding door is opened; mats already laid keep working.
 *
 * <p><b>What it costs.</b> The doors keep their rule that nothing ticks on an idle door and no
 * tile entity exists at rest. Instead, each world keeps this list of its sliding doors (their lower
 * halves), indexed by chunk, and every few ticks ({@link CsmConfig#getAutoDoorSensorInterval()})
 * looks only in the chunks within reach of each player. A world with no players does nothing; a
 * player with no sliding door nearby costs a map lookup or four. So the work grows with players and
 * the doors near them, never with every door in the world.</p>
 *
 * <p><b>How a door gets on the list.</b> Placing one adds it ({@code onBlockAdded}), breaking it
 * takes it off, and a door the sensor finds gone or changed is dropped as it is looked at. A slider
 * also has a random tick that does nothing but add itself, so doors placed before the sensor
 * existed, or ever missed, find their way on within a few minutes of their chunk being loaded.
 * Kept in the world's saved data, per dimension, as {@link DoorLocks} is. Server side only.</p>
 *
 * <p>Only players open a door this way; a creature near a door that is already open keeps it open
 * (the door's own doorway check). A door locked by a keypad does not open for a player outside it,
 * and does for one inside (free egress), as with the mat.</p>
 *
 * @version 1.0
 * @since 2026.11
 */
public class AutoDoorSensors extends WorldSavedData {

  private static final String NAME = "csm_auto_door_sensors";
  private static final String KEY_DOORS = "doors";

  /** Chunk ({@link ChunkPos#asLong}) to the sliding doors' lower halves in it ({@code toLong}). */
  private final Map<Long, Set<Long>> byChunk = new HashMap<>();

  /**
   * For the world's storage, which constructs it by name.
   *
   * @param name the data's name
   *
   * @since 1.0
   */
  public AutoDoorSensors(String name) {
    super(name);
  }

  /**
   * The sliding doors of a world (per dimension).
   *
   * @param world the world, server side
   *
   * @return its list
   *
   * @since 1.0
   */
  public static AutoDoorSensors get(World world) {
    MapStorage storage = world.getPerWorldStorage();
    AutoDoorSensors data = (AutoDoorSensors) storage.getOrLoadData(AutoDoorSensors.class, NAME);
    if (data == null) {
      data = new AutoDoorSensors(NAME);
      storage.setData(NAME, data);
    }
    return data;
  }

  /** Starts the sensor. Called once, from the module's pre-initialization. */
  static void register() {
    MinecraftForge.EVENT_BUS.register(new Sensor());
  }

  private static long chunkOf(BlockPos pos) {
    return ChunkPos.asLong(pos.getX() >> 4, pos.getZ() >> 4);
  }

  /**
   * Puts a sliding door on the list; nothing if it is on it already.
   *
   * @param lowerPos the door's lower half
   *
   * @since 1.0
   */
  void add(BlockPos lowerPos) {
    if (byChunk.computeIfAbsent(chunkOf(lowerPos), k -> new HashSet<>()).add(lowerPos.toLong())) {
      markDirty();
    }
  }

  /**
   * Takes a sliding door off the list.
   *
   * @param lowerPos the door's lower half
   *
   * @since 1.0
   */
  void remove(BlockPos lowerPos) {
    long chunk = chunkOf(lowerPos);
    Set<Long> doors = byChunk.get(chunk);
    if (doors != null && doors.remove(lowerPos.toLong())) {
      if (doors.isEmpty()) {
        byChunk.remove(chunk);
      }
      markDirty();
    }
  }

  /**
   * One look by the sensor: every player against the doors in the chunks within reach.
   *
   * @param world the world, server side, with players in it
   */
  private void sense(World world) {
    if (byChunk.isEmpty()) {
      return;
    }
    double range = CsmConfig.getAutoDoorSensorRange();
    // How far from a player a door's centre can be and still matter, with the door's own size.
    double reach = range + 1.5;
    List<BlockPos> gone = null;
    for (EntityPlayer player : world.playerEntities) {
      if (player.isSpectator() || !player.isEntityAlive()) {
        continue;
      }
      int cx0 = MathHelper.floor(player.posX - reach) >> 4;
      int cx1 = MathHelper.floor(player.posX + reach) >> 4;
      int cz0 = MathHelper.floor(player.posZ - reach) >> 4;
      int cz1 = MathHelper.floor(player.posZ + reach) >> 4;
      for (int cx = cx0; cx <= cx1; cx++) {
        for (int cz = cz0; cz <= cz1; cz++) {
          Set<Long> doors = byChunk.get(ChunkPos.asLong(cx, cz));
          if (doors == null) {
            continue;
          }
          for (long packed : doors) {
            BlockPos lowerPos = BlockPos.fromLong(packed);
            if (Math.abs(player.posX - (lowerPos.getX() + 0.5)) > reach
                || Math.abs(player.posZ - (lowerPos.getZ() + 0.5)) > reach
                || Math.abs(player.posY - lowerPos.getY()) > 3) {
              continue;
            }
            if (!world.isBlockLoaded(lowerPos)) {
              continue;
            }
            IBlockState state = world.getBlockState(lowerPos);
            if (!(state.getBlock() instanceof BlockAutoSlidingDoor)
                || state.getValue(BlockBuildingDoor.HALF) != BlockBuildingDoor.Half.LOWER
                || ((BlockAutoSlidingDoor) state.getBlock()).fixed()) {
              // Broken some way that did not tell the list (a command, an editor): drop it.
              if (gone == null) {
                gone = new ArrayList<>();
              }
              gone.add(lowerPos);
              continue;
            }
            ((BlockAutoSlidingDoor) state.getBlock()).sensed(world, lowerPos, player, range);
          }
        }
      }
    }
    if (gone != null) {
      for (BlockPos lowerPos : gone) {
        remove(lowerPos);
      }
    }
  }

  @Override
  public void readFromNBT(@Nonnull NBTTagCompound nbt) {
    byChunk.clear();
    int[] a = nbt.getIntArray(KEY_DOORS);
    for (int i = 0; i + 1 < a.length; i += 2) {
      long packed = ((long) a[i] << 32) | (a[i + 1] & 0xFFFFFFFFL);
      BlockPos pos = BlockPos.fromLong(packed);
      byChunk.computeIfAbsent(chunkOf(pos), k -> new HashSet<>()).add(packed);
    }
  }

  @Override
  @Nonnull
  public NBTTagCompound writeToNBT(@Nonnull NBTTagCompound compound) {
    int size = 0;
    for (Set<Long> doors : byChunk.values()) {
      size += doors.size();
    }
    int[] a = new int[size * 2];
    int i = 0;
    for (Set<Long> doors : byChunk.values()) {
      for (long packed : doors) {
        a[i++] = (int) (packed >> 32);
        a[i++] = (int) packed;
      }
    }
    compound.setIntArray(KEY_DOORS, a);
    return compound;
  }

  /** The sensor's tick: once every few ticks a world, and only a world with players in it. */
  public static final class Sensor {

    @SubscribeEvent
    public void onWorldTick(TickEvent.WorldTickEvent event) {
      World world = event.world;
      if (event.phase != TickEvent.Phase.END || world.isRemote || world.playerEntities.isEmpty()
          || !CsmConfig.isAutoDoorSensorEnabled()
          || world.getTotalWorldTime() % CsmConfig.getAutoDoorSensorInterval() != 0) {
        return;
      }
      get(world).sense(world);
    }
  }
}

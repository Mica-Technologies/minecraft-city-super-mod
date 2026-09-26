package com.micatechnologies.minecraft.csm.buildingmaterials;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;
import javax.annotation.Nonnull;
import net.minecraft.block.state.IBlockState;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.storage.MapStorage;
import net.minecraft.world.storage.WorldSavedData;

/**
 * Which doors are locked -- linked to a keypad -- in one world, and by which keypad. A locked door
 * opens from outside only with the keypad's code, and from inside as ever.
 *
 * <p>Kept here, in the world's saved data, rather than on the door: a door's two halves have used
 * all eight of their state bits, and a lock is rare enough that a tile entity on every door to
 * hold one bit would be a poor trade. Server side only: only the server decides who may open a
 * door. Keyed by the door's lower half.</p>
 *
 * <p><b>A lock heals itself when its keypad is gone</b> ({@link #holds}). A keypad unlocks its door
 * when it is broken, but one removed some other way (a command, an editor, or any keypad broken
 * before that worked) left its door locked for good. So whenever a lock is about to stop someone,
 * the keypad it names is looked at, and if its chunk is loaded and there is no keypad there linked
 * to this door any more, the lock is cleared and the door opens as an unlocked one would. Only
 * then: never on a tick, and never loading a chunk to look.</p>
 *
 * @version 1.1
 * @since 2026.9
 */
public class DoorLocks extends WorldSavedData {

  private static final String NAME = "csm_door_locks";

  /** The keypad of a lock saved before locks named their keypad. */
  static final long UNKNOWN_KEYPAD = Long.MIN_VALUE;

  /** How far round the door, in chunks, a lock with no known keypad needs loaded to be judged. */
  static final int UNKNOWN_KEYPAD_CHUNK_REACH = 1;

  /** Door (lower half) to the keypad that locks it, both as {@link BlockPos#toLong()}. */
  private final Map<Long, Long> locked = new HashMap<>();

  /**
   * For the world's storage, which constructs it by name.
   *
   * @param name the data's name
   *
   * @since 1.0
   */
  public DoorLocks(String name) {
    super(name);
  }

  /**
   * The locks of a world (per dimension).
   *
   * @param world the world
   *
   * @return its locks
   *
   * @since 1.0
   */
  public static DoorLocks get(World world) {
    MapStorage storage = world.getPerWorldStorage();
    DoorLocks data = (DoorLocks) storage.getOrLoadData(DoorLocks.class, NAME);
    if (data == null) {
      data = new DoorLocks(NAME);
      storage.setData(NAME, data);
    }
    return data;
  }

  public boolean isLocked(BlockPos lowerPos) {
    return locked.containsKey(lowerPos.toLong());
  }

  /**
   * The keypad that locks a door.
   *
   * @return its position as {@link BlockPos#toLong()}, {@link #UNKNOWN_KEYPAD} for a lock saved
   *     before locks named their keypad, or null if the door is not locked
   */
  Long keypadOf(BlockPos lowerPos) {
    return locked.get(lowerPos.toLong());
  }

  void lock(BlockPos lowerPos, BlockPos keypad) {
    Long previous = locked.put(lowerPos.toLong(), keypad.toLong());
    if (previous == null || previous != keypad.toLong()) {
      markDirty();
    }
  }

  void unlock(BlockPos lowerPos) {
    if (locked.remove(lowerPos.toLong()) != null) {
      markDirty();
    }
  }

  /**
   * A keypad that has just loaded names itself on its door's lock, if that lock was saved before
   * locks named their keypad. From then on the lock can be judged by its keypad alone.
   *
   * @since 1.1
   */
  void adopt(BlockPos lowerPos, BlockPos keypad) {
    Long current = locked.get(lowerPos.toLong());
    if (current != null && current == UNKNOWN_KEYPAD) {
      locked.put(lowerPos.toLong(), keypad.toLong());
      markDirty();
    }
  }

  /**
   * Whether a door's lock still stops someone from outside. Called only as it is about to: a
   * click on the shut door from outside, or someone outside in a locked sensor door's zone. A lock
   * whose keypad is gone is cleared here, and the answer is then no.
   *
   * @param world    the world, server side
   * @param lowerPos the door's lower half
   *
   * @return whether the door is locked
   *
   * @since 1.1
   */
  public static boolean holds(World world, BlockPos lowerPos) {
    DoorLocks locks = get(world);
    Long keypad = locks.keypadOf(lowerPos);
    if (keypad == null) {
      return false;
    }
    if (stale(keypad, lowerPos, world::isBlockLoaded, k -> linkedKeypadAt(world, k, lowerPos))) {
      locks.unlock(lowerPos);
      return false;
    }
    return true;
  }

  /**
   * Whether a lock's keypad is gone, so the lock can be cleared. Never when the chunk that would
   * answer is not loaded. A lock saved before locks named their keypad has had every keypad that
   * loaded since name itself on it ({@link #adopt}); if it is still nameless, no keypad linked to
   * it has been loaded, and it is judged gone once the chunks round the door, where a keypad is put
   * up, are all loaded.
   *
   * @param keypad       the lock's keypad, or {@link #UNKNOWN_KEYPAD}
   * @param door         the door's lower half
   * @param loaded       whether the chunk holding a position is loaded
   * @param linkedKeypad whether a keypad linked to this door stands at a (loaded) position
   *
   * @return whether the lock's keypad is gone
   *
   * @since 1.1
   */
  static boolean stale(long keypad, BlockPos door, Predicate<BlockPos> loaded,
      Predicate<BlockPos> linkedKeypad) {
    if (keypad == UNKNOWN_KEYPAD) {
      int reach = UNKNOWN_KEYPAD_CHUNK_REACH * 16;
      for (int dx = -reach; dx <= reach; dx += 16) {
        for (int dz = -reach; dz <= reach; dz += 16) {
          if (!loaded.test(door.add(dx, 0, dz))) {
            return false;
          }
        }
      }
      return true;
    }
    BlockPos at = BlockPos.fromLong(keypad);
    return loaded.test(at) && !linkedKeypad.test(at);
  }

  /** Whether a keypad linked to {@code door} stands at {@code at}; the chunk must be loaded. */
  private static boolean linkedKeypadAt(World world, BlockPos at, BlockPos door) {
    IBlockState state = world.getBlockState(at);
    if (!(state.getBlock() instanceof BlockGarageDoorControl)
        || ((BlockGarageDoorControl) state.getBlock()).kind()
        != BlockGarageDoorControl.Kind.KEYPAD) {
      return false;
    }
    TileEntity te = world.getTileEntity(at);
    return te instanceof TileEntityGarageDoorControl
        && door.equals(((TileEntityGarageDoorControl) te).target());
  }

  // Positions are stored as pairs of ints: 1.12's long array tag cannot be read back. "l" holds
  // the doors alone, as every version has written it, so a world opened in an older version keeps
  // its locks; "k" holds door and keypad, two pairs to an entry.
  private static final String KEY_DOORS = "l";
  private static final String KEY_KEYPADS = "k";

  @Override
  public void readFromNBT(@Nonnull NBTTagCompound nbt) {
    locked.clear();
    int[] a = nbt.getIntArray(KEY_DOORS);
    for (int i = 0; i + 1 < a.length; i += 2) {
      locked.put(join(a[i], a[i + 1]), UNKNOWN_KEYPAD);
    }
    int[] k = nbt.getIntArray(KEY_KEYPADS);
    for (int i = 0; i + 3 < k.length; i += 4) {
      locked.put(join(k[i], k[i + 1]), join(k[i + 2], k[i + 3]));
    }
  }

  @Override
  @Nonnull
  public NBTTagCompound writeToNBT(@Nonnull NBTTagCompound compound) {
    int[] doors = new int[locked.size() * 2];
    int known = 0;
    for (long keypad : locked.values()) {
      if (keypad != UNKNOWN_KEYPAD) {
        known++;
      }
    }
    int[] keypads = new int[known * 4];
    int i = 0;
    int j = 0;
    for (Map.Entry<Long, Long> e : locked.entrySet()) {
      long door = e.getKey();
      doors[i++] = (int) (door >> 32);
      doors[i++] = (int) door;
      if (e.getValue() != UNKNOWN_KEYPAD) {
        keypads[j++] = (int) (door >> 32);
        keypads[j++] = (int) door;
        keypads[j++] = (int) (e.getValue() >> 32);
        keypads[j++] = (int) (long) e.getValue();
      }
    }
    compound.setIntArray(KEY_DOORS, doors);
    compound.setIntArray(KEY_KEYPADS, keypads);
    return compound;
  }

  private static long join(int high, int low) {
    return ((long) high << 32) | (low & 0xFFFFFFFFL);
  }
}

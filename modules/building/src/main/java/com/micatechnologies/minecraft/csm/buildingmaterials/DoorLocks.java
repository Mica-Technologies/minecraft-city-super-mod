package com.micatechnologies.minecraft.csm.buildingmaterials;

import java.util.HashSet;
import java.util.Set;
import javax.annotation.Nonnull;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.storage.MapStorage;
import net.minecraft.world.storage.WorldSavedData;

/**
 * Which doors are locked -- linked to a keypad -- in one world. A locked door opens from outside
 * only with the keypad's code, and from inside as ever.
 *
 * <p>Kept here, in the world's saved data, rather than on the door: a door's two halves have used
 * all eight of their state bits, and a lock is rare enough that a tile entity on every door to
 * hold one bit would be a poor trade. Server side only: only the server decides who may open a
 * door. Keyed by the door's lower half.</p>
 *
 * @version 1.0
 * @since 2026.9
 */
public class DoorLocks extends WorldSavedData {

  private static final String NAME = "csm_door_locks";

  private final Set<Long> locked = new HashSet<>();

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
    return locked.contains(lowerPos.toLong());
  }

  void lock(BlockPos lowerPos) {
    if (locked.add(lowerPos.toLong())) {
      markDirty();
    }
  }

  void unlock(BlockPos lowerPos) {
    if (locked.remove(lowerPos.toLong())) {
      markDirty();
    }
  }

  // Stored as an int array, two ints to a position: 1.12's long array tag cannot be read back.
  @Override
  public void readFromNBT(@Nonnull NBTTagCompound nbt) {
    locked.clear();
    int[] a = nbt.getIntArray("l");
    for (int i = 0; i + 1 < a.length; i += 2) {
      locked.add(((long) a[i] << 32) | (a[i + 1] & 0xFFFFFFFFL));
    }
  }

  @Override
  @Nonnull
  public NBTTagCompound writeToNBT(@Nonnull NBTTagCompound compound) {
    int[] out = new int[locked.size() * 2];
    int i = 0;
    for (long l : locked) {
      out[i++] = (int) (l >> 32);
      out[i++] = (int) l;
    }
    compound.setIntArray("l", out);
    return compound;
  }
}

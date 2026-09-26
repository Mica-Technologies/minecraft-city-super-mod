package com.micatechnologies.minecraft.csm.buildingmaterials;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.BlockPos;
import org.junit.jupiter.api.Test;

/**
 * The door lock's saved data and the rule that clears a lock whose keypad is gone.
 */
class DoorLocksTest {

  private static final BlockPos DOOR = new BlockPos(-200, 4, -400);
  private static final BlockPos KEYPAD = new BlockPos(-202, 5, -401);

  @Test
  void lockNamesItsKeypadAndSurvivesASave() {
    DoorLocks locks = new DoorLocks("t");
    locks.lock(DOOR, KEYPAD);
    DoorLocks read = new DoorLocks("t");
    read.readFromNBT(locks.writeToNBT(new NBTTagCompound()));
    assertTrue(read.isLocked(DOOR));
    assertEquals(KEYPAD.toLong(), (long) read.keypadOf(DOOR));
  }

  @Test
  void aSaveStillHoldsTheDoorsAloneForOlderVersions() {
    DoorLocks locks = new DoorLocks("t");
    locks.lock(DOOR, KEYPAD);
    int[] doors = locks.writeToNBT(new NBTTagCompound()).getIntArray("l");
    assertEquals(2, doors.length);
    assertEquals(DOOR.toLong(), ((long) doors[0] << 32) | (doors[1] & 0xFFFFFFFFL));
  }

  @Test
  void anOldSaveReadsAsLockedByAnUnknownKeypadUntilOneAdoptsIt() {
    NBTTagCompound old = new NBTTagCompound();
    long d = DOOR.toLong();
    old.setIntArray("l", new int[]{(int) (d >> 32), (int) d});
    DoorLocks locks = new DoorLocks("t");
    locks.readFromNBT(old);
    assertTrue(locks.isLocked(DOOR));
    assertEquals(DoorLocks.UNKNOWN_KEYPAD, (long) locks.keypadOf(DOOR));
    locks.adopt(DOOR, KEYPAD);
    assertEquals(KEYPAD.toLong(), (long) locks.keypadOf(DOOR));
    // A second keypad cannot take over a lock that already names one.
    locks.adopt(DOOR, KEYPAD.east());
    assertEquals(KEYPAD.toLong(), (long) locks.keypadOf(DOOR));
  }

  @Test
  void adoptNeverLocksAnUnlockedDoor() {
    DoorLocks locks = new DoorLocks("t");
    locks.adopt(DOOR, KEYPAD);
    assertFalse(locks.isLocked(DOOR));
    assertNull(locks.keypadOf(DOOR));
  }

  @Test
  void aLockWhoseKeypadIsThereHolds() {
    assertFalse(DoorLocks.stale(KEYPAD.toLong(), DOOR, p -> true, KEYPAD::equals));
  }

  @Test
  void aLockWhoseKeypadIsGoneIsStale() {
    assertTrue(DoorLocks.stale(KEYPAD.toLong(), DOOR, p -> true, p -> false));
  }

  @Test
  void aLockIsNeverJudgedWithItsKeypadsChunkUnloaded() {
    assertFalse(DoorLocks.stale(KEYPAD.toLong(), DOOR, p -> false, p -> false));
  }

  @Test
  void aNamelessLockIsJudgedOnlyWithTheChunksRoundTheDoorLoaded() {
    assertTrue(DoorLocks.stale(DoorLocks.UNKNOWN_KEYPAD, DOOR, p -> true, p -> false));
    int doorChunkX = DOOR.getX() >> 4;
    assertFalse(DoorLocks.stale(DoorLocks.UNKNOWN_KEYPAD, DOOR,
        p -> (p.getX() >> 4) <= doorChunkX, p -> false));
  }

  @Test
  void unlockClears() {
    DoorLocks locks = new DoorLocks("t");
    locks.lock(DOOR, KEYPAD);
    locks.unlock(DOOR);
    assertFalse(locks.isLocked(DOOR));
  }
}

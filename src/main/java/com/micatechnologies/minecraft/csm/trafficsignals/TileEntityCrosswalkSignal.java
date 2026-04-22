package com.micatechnologies.minecraft.csm.trafficsignals;

import com.micatechnologies.minecraft.csm.codeutils.AbstractTickableTileEntity;
import com.micatechnologies.minecraft.csm.trafficsignals.logic.AbstractBlockControllableSignal;
import net.minecraft.block.state.IBlockState;
import net.minecraft.nbt.NBTTagCompound;

/**
 * Tile entity for crosswalk signals that implements countdown functionality.
 * Learns the pedestrian clearance phase duration by measuring the first clearance
 * interval, then displays a countdown on subsequent clearance phases — matching
 * how real-world countdown pedestrian signal modules operate.
 */
public class TileEntityCrosswalkSignal extends AbstractTickableTileEntity {

  // Short-form NBT keys. LEGACY_* counterparts are retained only so readNBT can still
  // load worlds saved before the short-key optimization; writeNBT only ever emits the
  // short form.
  private static final String NBT_KEY_LEARNED_CLEARANCE = "lCT";
  private static final String LEGACY_NBT_KEY_LEARNED_CLEARANCE = "learnedClearanceTicks";
  private static final String NBT_KEY_COUNTDOWN = "cCd";
  private static final String LEGACY_NBT_KEY_COUNTDOWN = "currentCountdown";
  private static final String NBT_KEY_LAST_COLOR = "lCS";
  private static final String LEGACY_NBT_KEY_LAST_COLOR = "lastColorState";
  private static final String NBT_KEY_MEASURING = "ms";
  private static final String LEGACY_NBT_KEY_MEASURING = "measuring";
  private static final String NBT_KEY_MEASURE_TICKS = "mTk";
  private static final String LEGACY_NBT_KEY_MEASURE_TICKS = "measureTicks";
  private static final String NBT_KEY_VERIFYING = "vf";
  private static final String LEGACY_NBT_KEY_VERIFYING = "verifying";
  private static final String NBT_KEY_VERIFY_TICKS = "vTk";
  private static final String LEGACY_NBT_KEY_VERIFY_TICKS = "verifyTicks";

  private int learnedClearanceTicks = 0;
  private int currentCountdown = -1;
  private int lastColorState = 3; // OFF
  private boolean measuring = false;
  private int measureTicks = 0;
  private boolean verifying = false;
  private int verifyTicks = 0;

  @Override
  public void readNBT(NBTTagCompound compound) {
    learnedClearanceTicks = readInt(compound, NBT_KEY_LEARNED_CLEARANCE,
        LEGACY_NBT_KEY_LEARNED_CLEARANCE);
    currentCountdown = readInt(compound, NBT_KEY_COUNTDOWN, LEGACY_NBT_KEY_COUNTDOWN);
    lastColorState = readInt(compound, NBT_KEY_LAST_COLOR, LEGACY_NBT_KEY_LAST_COLOR);
    measuring = readBool(compound, NBT_KEY_MEASURING, LEGACY_NBT_KEY_MEASURING);
    measureTicks = readInt(compound, NBT_KEY_MEASURE_TICKS, LEGACY_NBT_KEY_MEASURE_TICKS);
    verifying = readBool(compound, NBT_KEY_VERIFYING, LEGACY_NBT_KEY_VERIFYING);
    verifyTicks = readInt(compound, NBT_KEY_VERIFY_TICKS, LEGACY_NBT_KEY_VERIFY_TICKS);

    // Strip legacy long-form keys so the next save produces only short-form output
    compound.removeTag(LEGACY_NBT_KEY_LEARNED_CLEARANCE);
    compound.removeTag(LEGACY_NBT_KEY_COUNTDOWN);
    compound.removeTag(LEGACY_NBT_KEY_LAST_COLOR);
    compound.removeTag(LEGACY_NBT_KEY_MEASURING);
    compound.removeTag(LEGACY_NBT_KEY_MEASURE_TICKS);
    compound.removeTag(LEGACY_NBT_KEY_VERIFYING);
    compound.removeTag(LEGACY_NBT_KEY_VERIFY_TICKS);
  }

  private static int readInt(NBTTagCompound compound, String key, String legacyKey) {
    if (compound.hasKey(key)) return compound.getInteger(key);
    if (compound.hasKey(legacyKey)) return compound.getInteger(legacyKey);
    return 0;
  }

  private static boolean readBool(NBTTagCompound compound, String key, String legacyKey) {
    if (compound.hasKey(key)) return compound.getBoolean(key);
    return compound.hasKey(legacyKey) && compound.getBoolean(legacyKey);
  }

  @Override
  public NBTTagCompound writeNBT(NBTTagCompound compound) {
    compound.setInteger(NBT_KEY_LEARNED_CLEARANCE, learnedClearanceTicks);
    compound.setInteger(NBT_KEY_COUNTDOWN, currentCountdown);
    compound.setInteger(NBT_KEY_LAST_COLOR, lastColorState);
    compound.setBoolean(NBT_KEY_MEASURING, measuring);
    compound.setInteger(NBT_KEY_MEASURE_TICKS, measureTicks);
    compound.setBoolean(NBT_KEY_VERIFYING, verifying);
    compound.setInteger(NBT_KEY_VERIFY_TICKS, verifyTicks);
    return compound;
  }

  @Override
  public boolean doClientTick() {
    return false; // Server-side only, synced to client via NBT
  }

  @Override
  public boolean pauseTicking() {
    return false;
  }

  @Override
  public long getTickRate() {
    return 1; // Tick every game tick for accurate counting
  }

  @Override
  public void onTick() {
    if (world == null || world.isRemote) return;

    IBlockState state = world.getBlockState(pos);
    if (!(state.getBlock() instanceof AbstractBlockControllableSignal)) return;

    int currentColor = state.getValue(AbstractBlockControllableSignal.COLOR);

    // Detect state transitions
    if (currentColor != lastColorState) {
      onColorChanged(lastColorState, currentColor);
      lastColorState = currentColor;
    }

    // During clearance phase (color=1), count down if we've learned the duration
    if (currentColor == 1) {
      if (measuring) {
        measureTicks++;
      }
      if (verifying) {
        verifyTicks++;
      }
      if (currentCountdown > 0) {
        // Decrement every 20 ticks (1 second)
        if (world.getTotalWorldTime() % 20 == 0) {
          currentCountdown--;
          markDirtySync(world, pos, true);
        }
      }
    }
  }

  /**
   * Tolerance in ticks for clearance duration verification. If the actual clearance
   * differs from the learned value by more than this, the learned value is reset and
   * a new learning cycle begins — matching real-world countdown module behavior.
   * Controller timing options are in whole seconds, so 30 ticks (1.5s) gives enough
   * margin for minor tick-level variation between cycles while still detecting real
   * timing changes.
   */
  private static final int VERIFY_TOLERANCE_TICKS = 30;

  /**
   * Rounds a tick count to the nearest 10 ticks (0.5 seconds). This stabilizes the
   * learned clearance duration against minor tick-level variation between cycles,
   * since controller phase timing is configured in whole-second increments.
   */
  private static int roundToHalfSecond(int ticks) {
    return ((ticks + 5) / 10) * 10;
  }

  private void onColorChanged(int oldColor, int newColor) {
    // Transition from WALK (2) to CLEARANCE (1): start measuring or counting down
    if (oldColor == 2 && newColor == 1) {
      if (learnedClearanceTicks == 0) {
        // First cycle: measure the clearance duration
        measuring = true;
        measureTicks = 0;
      } else {
        // Subsequent cycles: start countdown and verify actual duration
        currentCountdown = learnedClearanceTicks / 20;
        verifying = true;
        verifyTicks = 0;
      }
      markDirtySync(world, pos, true);
    }
    // Transition from CLEARANCE (1) to DON'T WALK (0) or OFF (3): normal end of clearance
    else if (oldColor == 1 && (newColor == 0 || newColor == 3)) {
      if (measuring) {
        learnedClearanceTicks = roundToHalfSecond(measureTicks);
        measuring = false;
      } else if (verifying) {
        // Round the verify ticks before comparing against the rounded learned value
        int roundedVerifyTicks = roundToHalfSecond(verifyTicks);
        int difference = Math.abs(roundedVerifyTicks - learnedClearanceTicks);
        if (difference > VERIFY_TOLERANCE_TICKS) {
          // Duration changed significantly — reset and re-learn next cycle
          learnedClearanceTicks = 0;
        }
        verifying = false;
        verifyTicks = 0;
      }
      currentCountdown = -1;
      markDirtySync(world, pos, true);
    }
    // Transition from CLEARANCE (1) to WALK (2): ped recycle — the controller recycled
    // pedestrians back to walk because demand dropped. This is normal operation, not a
    // fault. Stop measuring/verifying gracefully without resetting learned timing.
    else if (oldColor == 1 && newColor == 2) {
      if (measuring) {
        // Partial measurement during recycle — discard but don't reset learned value
        measuring = false;
        measureTicks = 0;
      }
      if (verifying) {
        // Verification interrupted by recycle — keep learned value (it's still valid)
        verifying = false;
        verifyTicks = 0;
      }
      currentCountdown = -1;
      markDirtySync(world, pos, true);
    }
    // Any other unexpected transition while measuring/verifying: reset gracefully
    else if (newColor != 1) {
      if (measuring) {
        measuring = false;
        measureTicks = 0;
      }
      if (verifying) {
        // Only reset learned value for truly unexpected transitions (e.g., OFF→RED)
        // that indicate the controller changed behavior, not normal operation
        verifying = false;
        verifyTicks = 0;
      }
      if (currentCountdown != -1) {
        currentCountdown = -1;
        markDirtySync(world, pos, true);
      }
    }
  }

  /**
   * Returns the current countdown value in seconds. -1 means no countdown active.
   * 0 means countdown just finished. Positive = seconds remaining.
   */
  public int getCurrentCountdown() {
    return currentCountdown;
  }

  /**
   * Returns whether this signal has learned a clearance duration yet.
   */
  public boolean hasLearnedTiming() {
    return learnedClearanceTicks > 0;
  }
}

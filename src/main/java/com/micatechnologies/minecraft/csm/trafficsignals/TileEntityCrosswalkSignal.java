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

  private static final String NBT_KEY_LEARNED_CLEARANCE = "learnedClearanceTicks";
  private static final String NBT_KEY_COUNTDOWN = "currentCountdown";
  private static final String NBT_KEY_LAST_COLOR = "lastColorState";
  private static final String NBT_KEY_MEASURING = "measuring";
  private static final String NBT_KEY_MEASURE_TICKS = "measureTicks";
  private static final String NBT_KEY_VERIFYING = "verifying";
  private static final String NBT_KEY_VERIFY_TICKS = "verifyTicks";

  private int learnedClearanceTicks = 0;
  private int currentCountdown = -1;
  private int lastColorState = 3; // OFF
  private boolean measuring = false;
  private int measureTicks = 0;
  private boolean verifying = false;
  private int verifyTicks = 0;

  @Override
  public void readNBT(NBTTagCompound compound) {
    learnedClearanceTicks = compound.getInteger(NBT_KEY_LEARNED_CLEARANCE);
    currentCountdown = compound.getInteger(NBT_KEY_COUNTDOWN);
    lastColorState = compound.getInteger(NBT_KEY_LAST_COLOR);
    measuring = compound.getBoolean(NBT_KEY_MEASURING);
    measureTicks = compound.getInteger(NBT_KEY_MEASURE_TICKS);
    verifying = compound.getBoolean(NBT_KEY_VERIFYING);
    verifyTicks = compound.getInteger(NBT_KEY_VERIFY_TICKS);
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
   * 20 ticks = 1 second of tolerance.
   */
  private static final int VERIFY_TOLERANCE_TICKS = 20;

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
    // Transition from CLEARANCE (1) to DON'T WALK (0) or OFF (3): finish measuring/verifying
    else if (oldColor == 1 && (newColor == 0 || newColor == 3)) {
      if (measuring) {
        learnedClearanceTicks = measureTicks;
        measuring = false;
      } else if (verifying) {
        // Check if actual duration matches learned value within tolerance
        int difference = Math.abs(verifyTicks - learnedClearanceTicks);
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
    // Any other transition (e.g., clearance cut short by unexpected state): reset
    else if (newColor != 1) {
      if (measuring) {
        // Clearance was interrupted before completing — don't trust partial measurement
        measuring = false;
        measureTicks = 0;
      }
      if (verifying) {
        // Clearance was cut short — reset learned value and re-learn
        learnedClearanceTicks = 0;
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

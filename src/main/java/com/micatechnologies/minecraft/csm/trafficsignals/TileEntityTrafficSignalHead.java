package com.micatechnologies.minecraft.csm.trafficsignals;

import com.micatechnologies.minecraft.csm.codeutils.AbstractTileEntity;
import com.micatechnologies.minecraft.csm.trafficsignals.logic.AbstractBlockControllableSignal.SIGNAL_SIDE;
import com.micatechnologies.minecraft.csm.trafficsignals.logic.AbstractBlockControllableSignalHead;
import com.micatechnologies.minecraft.csm.trafficsignals.logic.TrafficSignalBodyColor;
import com.micatechnologies.minecraft.csm.trafficsignals.logic.TrafficSignalBodyTilt;
import com.micatechnologies.minecraft.csm.trafficsignals.logic.TrafficSignalBulbColor;
import com.micatechnologies.minecraft.csm.trafficsignals.logic.TrafficSignalBulbStyle;
import com.micatechnologies.minecraft.csm.trafficsignals.logic.TrafficSignalBulbType;
import com.micatechnologies.minecraft.csm.trafficsignals.logic.TrafficSignalSectionInfo;
import com.micatechnologies.minecraft.csm.trafficsignals.logic.TrafficSignalVisorType;
import java.util.Random;
import net.minecraft.block.Block;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.AxisAlignedBB;

/**
 * Tile entity utility class for traffic signal heads. This class assists in tracking and managing
 * the paint colors of the traffic signal head and visor configuration(s).
 *
 * @author Mica Technologies
 * @version 1.0
 * @since 2024.8.19
 */
public class TileEntityTrafficSignalHead extends AbstractTileEntity {

  /**
   * Returns an expanded render bounding box so Minecraft's frustum culling doesn't hide
   * the signal TESR when sections extend beyond the block position (e.g., 3-section
   * vertical signals extend ~1.5 blocks above and below center, plus visors).
   */
  @Override
  public AxisAlignedBB getRenderBoundingBox() {
    return new AxisAlignedBB(
        pos.getX() - 1.0, pos.getY() - 1.0, pos.getZ() - 1.0,
        pos.getX() + 2.0, pos.getY() + 2.0, pos.getZ() + 2.0);
  }

  /**
   * The key used to store the body paint color in NBT data.
   *
   * @since 1.0
   */
  private static final String SECTION_INFOS_KEY = "sectionInfos";

  /**
   * The key used to store the body tilt in NBT data.
   *
   * @since 1.0
   */
  private static final String BODY_TILT_KEY = "bodyTilt";

  /**
   * The key used to store the section count in the section info NBT data.
   *
   * @since 1.0
   */
  private static final String SECTION_INFO_COMPOUND_COUNT_KEY = "count";

  /**
   * The key prefix used to store each section info in the section info NBT data.
   *
   * @since 1.0
   */
  private static final String SECTION_INFO_COMPOUND_KEY_PREFIX = "s_";

  /**
   * The current visor type.
   *
   * @since 1.0
   */
  private TrafficSignalSectionInfo[] sectionInfos = {};


  /**
   * The current body tilt.
   *
   * @since 1.0
   */
  private TrafficSignalBodyTilt bodyTilt = TrafficSignalBodyTilt.NONE;

  private static final String ALTERNATE_FLASH_KEY = "alternateFlash";
  private boolean alternateFlash = false;

  private boolean dirty = true;
  private boolean powerLossOff = true;

  // Bulb aging fields
  private static final String AGING_ENABLED_KEY = "agingEnabled";
  private static final String AGING_LAST_DAY_KEY = "lastAgingDay";
  private static final String AGING_STATES_KEY = "bulbAgingStates";
  private static final String AGING_SEED_KEY = "agingSeed";
  public static final int AGING_HEALTHY = 0;
  public static final int AGING_FAILING = 1;
  public static final int AGING_DEAD = 2;
  public static final int AGING_STATE_COUNT = 3;
  private static final double CHANCE_HEALTHY_TO_FAILING = 0.00025; // 0.025% per day
  private static final double CHANCE_FAILING_TO_DEAD = 0.005;     // 0.5% per day
  private static final int MAX_CATCHUP_DAYS = 365;

  private boolean agingEnabled = false;
  private long lastAgingDay = -1L;
  private int[] bulbAgingStates = new int[0];
  private long agingSeed = 0L;

  /**
   * Constructs a new TileEntityTrafficSignalHead instance.
   *
   * @since 1.0
   */
  public TileEntityTrafficSignalHead() {
    super();

    // Initialize the section infos with default values
    sectionInfos= new TrafficSignalSectionInfo[]{};
  }

  public TileEntityTrafficSignalHead(TrafficSignalSectionInfo[] sectionInfos) {
    super();

    // Initialize the section infos with the provided values
    this.sectionInfos = sectionInfos;
  }

  /**
   * Processes the reading of the tile entity's NBT data from the supplied NBT tag compound.
   *
   * @param compound the NBT tag compound to read the tile entity's NBT data from
   *
   * @since 2.0
   */
  @Override
  public void readNBT(NBTTagCompound compound) {
    // Get the traffic signal section infos
    readSectionInfo(compound);

    // Get the body tilt
    if (compound.hasKey(BODY_TILT_KEY)) {
      bodyTilt = TrafficSignalBodyTilt.fromNBT(compound.getInteger(BODY_TILT_KEY));
    }

    // Get the alternate flash setting
    if (compound.hasKey(ALTERNATE_FLASH_KEY)) {
      alternateFlash = compound.getBoolean(ALTERNATE_FLASH_KEY);
    }

    // Get aging settings
    if (compound.hasKey(AGING_ENABLED_KEY)) {
      agingEnabled = compound.getBoolean(AGING_ENABLED_KEY);
    }
    if (compound.hasKey(AGING_LAST_DAY_KEY)) {
      lastAgingDay = compound.getLong(AGING_LAST_DAY_KEY);
    }
    if (compound.hasKey(AGING_STATES_KEY)) {
      bulbAgingStates = compound.getIntArray(AGING_STATES_KEY);
    }
    if (compound.hasKey(AGING_SEED_KEY)) {
      agingSeed = compound.getLong(AGING_SEED_KEY);
    }

    // Mark as dirty so the renderer recompiles the display list with updated state
    dirty = true;
  }


  private void readSectionInfo(NBTTagCompound compound) {
    if (compound.hasKey(SECTION_INFOS_KEY)) {
      NBTTagCompound sectionInfoCompound = compound.getCompoundTag(SECTION_INFOS_KEY);
      sectionInfos = new TrafficSignalSectionInfo[sectionInfoCompound.getInteger(SECTION_INFO_COMPOUND_COUNT_KEY)];
      for (int i = 0; i < sectionInfos.length; i++) {
        int[] sectionData = sectionInfoCompound.getIntArray(SECTION_INFO_COMPOUND_KEY_PREFIX + i);
        sectionInfos[i] = TrafficSignalSectionInfo.fromNBTArray(sectionData);
      }
    }
  }

  private void writeSectionInfo(NBTTagCompound compound) {
    NBTTagCompound sectionInfoCompound = new NBTTagCompound();
    sectionInfoCompound.setInteger(SECTION_INFO_COMPOUND_COUNT_KEY, sectionInfos.length);
    for (int i = 0; i < sectionInfos.length; i++) {
      sectionInfoCompound.setIntArray(SECTION_INFO_COMPOUND_KEY_PREFIX + i, sectionInfos[i].toNBTArray());
    }
    compound.setTag(SECTION_INFOS_KEY, sectionInfoCompound);
  }

  /**
   * Returns the NBT tag compound with the tile entity's NBT data.
   *
   * @param compound the NBT tag compound to write the tile entity's NBT data to
   *
   * @return the NBT tag compound with the tile entity's NBT data
   *
   * @since 2.0
   */
  @Override
  public NBTTagCompound writeNBT(NBTTagCompound compound) {
    // Write the section infos to the compound
    writeSectionInfo(compound);

    // Set the body tilt
    compound.setInteger(BODY_TILT_KEY, bodyTilt.toNBT());

    // Set the alternate flash setting
    compound.setBoolean(ALTERNATE_FLASH_KEY, alternateFlash);

    // Set aging settings
    compound.setBoolean(AGING_ENABLED_KEY, agingEnabled);
    compound.setLong(AGING_LAST_DAY_KEY, lastAgingDay);
    compound.setIntArray(AGING_STATES_KEY, bulbAgingStates);
    compound.setLong(AGING_SEED_KEY, agingSeed);

    // Return the compound
    return compound;
  }

  /**
   * Gets the current section infos (non-live, meaning the bulb colors are not
   * updated based on the current bulb color).
   *
   * @return the current section infos (non-live).
   *
   * @since 1.0
   */
  public TrafficSignalSectionInfo[] getSectionInfos() {
    return sectionInfos;
  }

  /**
   * Gets the current section infos.
   *
   * @return the current section infos.
   *
   * @since 1.0
   */
  public TrafficSignalSectionInfo[] getSectionInfos(int currentBulbColor) {
    // Evaluate aging progression (server-side, lazy — only when a new day has passed)
    tickAging();

    // Determine lighting using the block's shouldLightBulb/shouldLightAllSections methods
    // which allows per-block-type color mapping (e.g., single flashers light on both 0 and 1)
    Block block = world != null ? world.getBlockState(pos).getBlock() : null;
    boolean useBlockMapping = block instanceof AbstractBlockControllableSignalHead;

    for (TrafficSignalSectionInfo sectionInfo : sectionInfos) {
      if (currentBulbColor == 3) {
        sectionInfo.setBulbLit(false);
      } else if (useBlockMapping) {
        AbstractBlockControllableSignalHead signalBlock = (AbstractBlockControllableSignalHead) block;
        sectionInfo.setBulbLit(
            signalBlock.shouldLightAllSections(currentBulbColor)
            || signalBlock.shouldLightBulb(currentBulbColor, sectionInfo.getBulbColor()));
      } else {
        // Fallback: default color mapping
        boolean lit = (currentBulbColor == 0 && sectionInfo.getBulbColor() == TrafficSignalBulbColor.RED)
            || (currentBulbColor == 1 && sectionInfo.getBulbColor() == TrafficSignalBulbColor.YELLOW)
            || (currentBulbColor == 2 && sectionInfo.getBulbColor() == TrafficSignalBulbColor.GREEN);
        sectionInfo.setBulbLit(lit);
      }
    }

    // HAWK wigwag: for color=2 on HAWK signals, override per-section lighting with
    // alternating pattern. This is handled in the block class via shouldLightWigwagSection.
    if (currentBulbColor == 2 && block instanceof BlockControllableHawkSignal) {
      BlockControllableHawkSignal hawkBlock = (BlockControllableHawkSignal) block;
      for (int i = 0; i < sectionInfos.length; i++) {
        sectionInfos[i].setBulbLit(hawkBlock.shouldLightWigwagSection(i));
      }
    }

    // Enforce bulb style if the block requires a specific style (e.g., bi-modal arrows)
    if (useBlockMapping) {
      TrafficSignalBulbStyle enforced =
          ((AbstractBlockControllableSignalHead) block).getEnforcedBulbStyle();
      if (enforced != null) {
        for (TrafficSignalSectionInfo sectionInfo : sectionInfos) {
          sectionInfo.setBulbStyle(enforced);
        }
      }
    }

    // Loop again, and if the bulb is lit and set to flashing, handle the flashing logic.
    // alternateFlash inverts the flash phase for wig-wag beacon pairs.
    long blinkInterval = 500; // ms
    boolean firstHalfOfSecond = (System.currentTimeMillis() % (blinkInterval * 2)) < blinkInterval;
    if (alternateFlash) firstHalfOfSecond = !firstHalfOfSecond;
    for (TrafficSignalSectionInfo sectionInfo : sectionInfos) {
      if (sectionInfo.isBulbLit() && sectionInfo.isBulbFlashing() && firstHalfOfSecond) {
        sectionInfo.setBulbLit(false);
      }
    }

    // Apply aging effects (failing flicker and dead bulbs)
    applyAgingEffects();

    return sectionInfos;
  }

  /**
   * Sets the section infos.
   *
   * @param sectionInfos the new section infos
   *
   * @since 1.0
   */
  public void setSectionInfos(TrafficSignalSectionInfo[] sectionInfos) {
    this.sectionInfos = sectionInfos;
    markDirtySync(world, pos, true);
  }

  public int getSectionCount() {
    return sectionInfos.length;
  }

  /**
   * Gets the current body tilt.
   *
   * @return the current body tilt.
   *
   * @since 1.0
   */
  public TrafficSignalBodyTilt getBodyTilt() {
    return bodyTilt;
  }

  /**
   * Sets the body tilt.
   *
   * @param bodyTilt the new body tilt
   *
   * @since 1.0
   */
  public void setBodyTilt(TrafficSignalBodyTilt bodyTilt) {
    this.bodyTilt = bodyTilt;
    markDirtySync(world, pos, true);
  }

  /**
   * Gets the next body paint color in the sequence.
   *
   * @return the next body paint color in the sequence
   *
   * @since 1.0
   */
  public TrafficSignalBodyColor getNextBodyPaintColor() {
    if (sectionInfos.length == 0) {
      System.err.println("No section infos available to get the next body paint color.");
      return TrafficSignalBodyColor.FLAT_BLACK; // Default fallback color
    }
    TrafficSignalBodyColor nextPaintColor = sectionInfos[0].getBodyColor().getNextColor();
    // Update the body color for all sections
    for (TrafficSignalSectionInfo sectionInfo : sectionInfos) {
      sectionInfo.setBodyColor(nextPaintColor);
    }
    markDirtySync(world, pos, true);
    return nextPaintColor;
  }

  /**
   * Gets the next door paint color in the sequence.
   *
   * @return the next door paint color in the sequence
   *
   * @since 1.0
   */
  public TrafficSignalBodyColor getNextDoorPaintColor() {
    TrafficSignalBodyColor nextPaintColor = sectionInfos[0].getDoorColor().getNextColor();
    // Update the door color for all sections
    for (TrafficSignalSectionInfo sectionInfo : sectionInfos) {
      sectionInfo.setDoorColor(nextPaintColor);
    }
    markDirtySync(world, pos, true);
    return nextPaintColor;
  }

  /**
   * Gets the next visor paint color in the sequence.
   *
   * @return the next visor paint color in the sequence
   *
   * @since 1.0
   */
  public TrafficSignalBodyColor getNextVisorPaintColor() {
    TrafficSignalBodyColor nextPaintColor = sectionInfos[0].getVisorColor().getNextColor();
    // Update the visor color for all sections
    for (TrafficSignalSectionInfo sectionInfo : sectionInfos) {
      sectionInfo.setVisorColor(nextPaintColor);
    }
    markDirtySync(world, pos, true);
    return nextPaintColor;
  }

  /**
   * Gets the next body tilt in the sequence.
   *
   * @return the next body tilt in the sequence
   *
   * @since 1.0
   */
  public TrafficSignalBodyTilt getNextBodyTilt() {
    TrafficSignalBodyTilt nextBodyTilt = bodyTilt.getNextTilt();
    setBodyTilt(nextBodyTilt);
    return getBodyTilt();
  }

  /**
   * Returns whether this signal head uses alternate (inverted) flash timing for wig-wag.
   */
  public boolean isAlternateFlash() {
    return alternateFlash;
  }

  /**
   * Toggles the alternate flash setting and returns the new value.
   */
  public boolean toggleAlternateFlash() {
    alternateFlash = !alternateFlash;
    markDirtySync(world, pos, true);
    return alternateFlash;
  }

  /**
   * Gets the next visor type in the sequence.
   *
   * @return the next visor type in the sequence
   *
   * @since 1.0
   */
  public TrafficSignalVisorType getNextVisorType() {
    TrafficSignalVisorType nextVisorType = sectionInfos[0].getVisorType().getNextVisorType();
    // Update the visor type for all sections
    for (TrafficSignalSectionInfo sectionInfo : sectionInfos) {
      sectionInfo.setVisorType(nextVisorType);
    }
    markDirtySync(world, pos, true);
    return nextVisorType;
  }

  public TrafficSignalBulbStyle getNextBulbStyle() {
    if (sectionInfos.length == 0) {
      return TrafficSignalBulbStyle.INCANDESCENT;
    }
    // If the block enforces a specific style, don't cycle — return the enforced style
    if (world != null) {
      Block block = world.getBlockState(pos).getBlock();
      if (block instanceof AbstractBlockControllableSignalHead) {
        TrafficSignalBulbStyle enforced =
            ((AbstractBlockControllableSignalHead) block).getEnforcedBulbStyle();
        if (enforced != null) {
          return enforced;
        }
      }
    }
    TrafficSignalBulbStyle next = sectionInfos[0].getBulbStyle().getNextBulbStyle();
    for (TrafficSignalSectionInfo sectionInfo : sectionInfos) {
      sectionInfo.setBulbStyle(next);
    }
    markDirtySync(world, pos, true);
    return next;
  }

  public TrafficSignalBulbType getNextBulbType() {
    if (sectionInfos.length == 0) {
      return TrafficSignalBulbType.BALL;
    }
    TrafficSignalBulbType next = sectionInfos[0].getBulbType().getNextBulbType();
    for (TrafficSignalSectionInfo sectionInfo : sectionInfos) {
      sectionInfo.setBulbType(next);
    }
    markDirtySync(world, pos, true);
    return next;
  }

  // --- Per-section property cyclers ---
  //
  // These mirror the whole-head getNext* methods above, but advance only the section at the
  // given index. The per-section config GUI dispatches through these so individual sections can
  // be customized ("frankenstein" signal heads). The whole-head variants remain the source of
  // truth for the simple config GUI and for the item tool's sneak-click cycle shortcuts.

  private boolean isValidSectionIndex(int idx) {
    return idx >= 0 && idx < sectionInfos.length;
  }

  public TrafficSignalBodyColor getNextBodyPaintColor(int sectionIndex) {
    if (!isValidSectionIndex(sectionIndex)) {
      return TrafficSignalBodyColor.FLAT_BLACK;
    }
    TrafficSignalBodyColor next = sectionInfos[sectionIndex].getBodyColor().getNextColor();
    sectionInfos[sectionIndex].setBodyColor(next);
    markDirtySync(world, pos, true);
    return next;
  }

  public TrafficSignalBodyColor getNextDoorPaintColor(int sectionIndex) {
    if (!isValidSectionIndex(sectionIndex)) {
      return TrafficSignalBodyColor.FLAT_BLACK;
    }
    TrafficSignalBodyColor next = sectionInfos[sectionIndex].getDoorColor().getNextColor();
    sectionInfos[sectionIndex].setDoorColor(next);
    markDirtySync(world, pos, true);
    return next;
  }

  public TrafficSignalBodyColor getNextVisorPaintColor(int sectionIndex) {
    if (!isValidSectionIndex(sectionIndex)) {
      return TrafficSignalBodyColor.FLAT_BLACK;
    }
    TrafficSignalBodyColor next = sectionInfos[sectionIndex].getVisorColor().getNextColor();
    sectionInfos[sectionIndex].setVisorColor(next);
    markDirtySync(world, pos, true);
    return next;
  }

  public TrafficSignalVisorType getNextVisorType(int sectionIndex) {
    if (!isValidSectionIndex(sectionIndex)) {
      return TrafficSignalVisorType.TUNNEL;
    }
    TrafficSignalVisorType next = sectionInfos[sectionIndex].getVisorType().getNextVisorType();
    sectionInfos[sectionIndex].setVisorType(next);
    markDirtySync(world, pos, true);
    return next;
  }

  public TrafficSignalBulbStyle getNextBulbStyle(int sectionIndex) {
    if (!isValidSectionIndex(sectionIndex)) {
      return TrafficSignalBulbStyle.INCANDESCENT;
    }
    // Block-level enforcement wins over per-section customization: a head that hard-locks its
    // bulb style (e.g. modern LED-only housings) must remain consistent across all sections.
    if (world != null) {
      Block block = world.getBlockState(pos).getBlock();
      if (block instanceof AbstractBlockControllableSignalHead) {
        TrafficSignalBulbStyle enforced =
            ((AbstractBlockControllableSignalHead) block).getEnforcedBulbStyle();
        if (enforced != null) {
          return enforced;
        }
      }
    }
    TrafficSignalBulbStyle next = sectionInfos[sectionIndex].getBulbStyle().getNextBulbStyle();
    sectionInfos[sectionIndex].setBulbStyle(next);
    markDirtySync(world, pos, true);
    return next;
  }

  public TrafficSignalBulbType getNextBulbType(int sectionIndex) {
    if (!isValidSectionIndex(sectionIndex)) {
      return TrafficSignalBulbType.BALL;
    }
    TrafficSignalBulbType next = sectionInfos[sectionIndex].getBulbType().getNextBulbType();
    sectionInfos[sectionIndex].setBulbType(next);
    markDirtySync(world, pos, true);
    return next;
  }

  public boolean isStateDirty() {
    return dirty;
  }

  public void clearDirtyFlag() {
    dirty = false;
  }

  public boolean isPowerLossOff() {
    return powerLossOff;
  }

  public void setPowerLossOff( boolean powerLossOff ) {
    this.powerLossOff = powerLossOff;
  }

  // --- Bulb Aging ---

  public boolean isAgingEnabled() {
    return agingEnabled;
  }

  public boolean toggleAging() {
    agingEnabled = !agingEnabled;
    if (agingEnabled) {
      if (bulbAgingStates.length != sectionInfos.length) {
        bulbAgingStates = new int[sectionInfos.length];
      }
      if (agingSeed == 0L && world != null) {
        agingSeed = pos.toLong() ^ world.getSeed();
      }
      if (lastAgingDay < 0 && world != null) {
        lastAgingDay = world.getTotalWorldTime() / 24000L;
      }
    } else {
      // Turning aging off resets all bulbs to healthy ("replacing" broken bulbs). This also
      // wipes any manual per-section overrides by design — treat the toggle as a maintenance
      // crew that has visited every head in the system.
      bulbAgingStates = new int[sectionInfos.length];
      lastAgingDay = -1L;
      agingSeed = 0L;
    }
    markDirtySync(world, pos, true);
    return agingEnabled;
  }

  /**
   * Ensures {@link #bulbAgingStates} is sized to the current section count so callers can index
   * into it directly. Used by manual-override setters, which need a valid slot even when aging
   * has never been toggled on. Existing values are preserved on resize.
   */
  private void ensureBulbAgingStatesSize() {
    if (bulbAgingStates.length == sectionInfos.length) {
      return;
    }
    int[] resized = new int[sectionInfos.length];
    System.arraycopy(bulbAgingStates, 0, resized, 0,
        Math.min(bulbAgingStates.length, sectionInfos.length));
    bulbAgingStates = resized;
  }

  /**
   * Returns the current aging state for the given section ({@link #AGING_HEALTHY},
   * {@link #AGING_FAILING}, or {@link #AGING_DEAD}). Returns {@code AGING_HEALTHY} for
   * out-of-range indices so callers never have to null-check.
   */
  public int getBulbAgingState(int sectionIndex) {
    if (sectionIndex < 0 || sectionIndex >= bulbAgingStates.length) {
      return AGING_HEALTHY;
    }
    return bulbAgingStates[sectionIndex];
  }

  /**
   * Directly sets a section's aging state. Works regardless of whether the global aging
   * simulation is enabled — this is how the per-section config GUI lets players force a bulb
   * to fail or die for aesthetic purposes. Out-of-range states are clamped to healthy.
   */
  public void setBulbAgingState(int sectionIndex, int state) {
    if (sectionIndex < 0 || sectionIndex >= sectionInfos.length) {
      return;
    }
    int clamped = (state < AGING_HEALTHY || state > AGING_DEAD) ? AGING_HEALTHY : state;
    ensureBulbAgingStatesSize();
    if (bulbAgingStates[sectionIndex] == clamped) {
      return;
    }
    bulbAgingStates[sectionIndex] = clamped;
    markDirtySync(world, pos, true);
  }

  /**
   * Cycles a section's aging state forward (healthy → failing → dead → healthy) and returns
   * the new value. Invoked by the per-section config GUI's bulb-state button.
   */
  public int getNextBulbAgingState(int sectionIndex) {
    if (sectionIndex < 0 || sectionIndex >= sectionInfos.length) {
      return AGING_HEALTHY;
    }
    ensureBulbAgingStatesSize();
    int next = (bulbAgingStates[sectionIndex] + 1) % AGING_STATE_COUNT;
    setBulbAgingState(sectionIndex, next);
    return next;
  }

  private void tickAging() {
    if (world == null || !agingEnabled) return;
    long currentDay = world.getTotalWorldTime() / 24000L;
    if (currentDay <= lastAgingDay) return;

    // Ensure state array matches section count
    if (bulbAgingStates.length != sectionInfos.length) {
      int[] resized = new int[sectionInfos.length];
      System.arraycopy(bulbAgingStates, 0, resized, 0,
          Math.min(bulbAgingStates.length, sectionInfos.length));
      bulbAgingStates = resized;
    }

    // Cap catch-up to avoid long loops for signals unloaded for a very long time
    long startDay = Math.max(lastAgingDay + 1, currentDay - MAX_CATCHUP_DAYS);
    Random rng = new Random();
    boolean stateChanged = false;
    for (long day = startDay; day <= currentDay; day++) {
      rng.setSeed(agingSeed ^ day);
      for (int i = 0; i < bulbAgingStates.length; i++) {
        if (bulbAgingStates[i] == AGING_HEALTHY) {
          if (rng.nextDouble() < CHANCE_HEALTHY_TO_FAILING) {
            bulbAgingStates[i] = AGING_FAILING;
            stateChanged = true;
          }
        } else if (bulbAgingStates[i] == AGING_FAILING) {
          if (rng.nextDouble() < CHANCE_FAILING_TO_DEAD) {
            bulbAgingStates[i] = AGING_DEAD;
            stateChanged = true;
          }
        }
      }
    }
    lastAgingDay = currentDay;
    // Only persist on server side; client computes the same result via deterministic seed
    if (!world.isRemote && stateChanged) {
      markDirtySync(world, pos, true);
    }
  }

  private void applyAgingEffects() {
    // Intentionally no !agingEnabled gate: bulbAgingStates[] is the single source of truth for
    // what a bulb looks like, so manual state overrides (set via the per-section config GUI)
    // must still render as failing/dead even when the natural aging simulation is turned off.
    // Toggling aging off zeros the array (see toggleAging), which keeps the prior behavior of
    // "turning off aging resets all bulbs to healthy."
    if (bulbAgingStates.length == 0) return;

    long now = System.currentTimeMillis();
    for (int i = 0; i < sectionInfos.length && i < bulbAgingStates.length; i++) {
      if (bulbAgingStates[i] == AGING_DEAD) {
        sectionInfos[i].setBulbLit(false);
      } else if (bulbAgingStates[i] == AGING_FAILING && sectionInfos[i].isBulbLit()) {
        long seed = pos.toLong() + i;

        // Determine which failure mode is active using a slow-cycling wave seeded
        // per bulb. This produces irregular stretches of each mode:
        //   mode < 0.3  → rapid strobe burst (2-3 seconds)
        //   mode >= 0.3 → organic sine-wave flicker (majority of the time)
        double mode = (Math.sin((now + seed * 79) * 0.0004) + 1.0) / 2.0; // 0..1

        if (mode < 0.3) {
          // Rapid strobe with varying speed — a secondary wave picks the frequency
          // so the strobe accelerates and decelerates organically.
          double speedWave = (Math.sin((now + seed * 173) * 0.0011) + 1.0) / 2.0; // 0..1
          // Cycle period ranges from 16ms (~60 Hz, very fast) to 50ms (~20 Hz)
          long period = 16L + (long) (speedWave * 34L);
          boolean strobeOff = ((now / period) + seed) % 3L != 0L;
          if (strobeOff) {
            sectionInfos[i].setBulbLit(false);
          }
        } else {
          // Organic flicker using overlapping sine waves
          double phase1 = Math.sin((now + seed * 137) * 0.013);
          double phase2 = Math.sin((now + seed * 251) * 0.007);
          double phase3 = Math.sin((now + seed * 397) * 0.031);
          double combined = phase1 + phase2 * 0.5 + phase3 * 0.3;
          if (combined < -0.2) {
            sectionInfos[i].setBulbLit(false);
          }
        }
      }
    }
  }

  // Call dirty = true; in methods that change state (e.g., set color, lit)
  @Override
  public void onDataPacket(net.minecraft.network.NetworkManager net, net.minecraft.network.play.server.SPacketUpdateTileEntity pkt) {
    super.onDataPacket(net, pkt);
    dirty = true;
  }

  @Override
  public double getMaxRenderDistanceSquared() {
    return 128.0 * 128.0; // 128 blocks
  }
}

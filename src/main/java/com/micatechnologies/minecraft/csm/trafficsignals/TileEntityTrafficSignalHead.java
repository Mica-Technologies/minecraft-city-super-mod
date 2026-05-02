package com.micatechnologies.minecraft.csm.trafficsignals;

import com.micatechnologies.minecraft.csm.codeutils.AbstractTileEntity;
import com.micatechnologies.minecraft.csm.codeutils.CsmRenderUtils;
import com.micatechnologies.minecraft.csm.trafficsignals.logic.AbstractBlockControllableSignal.SIGNAL_SIDE;
import com.micatechnologies.minecraft.csm.trafficsignals.logic.AbstractBlockControllableSignalHead;
import com.micatechnologies.minecraft.csm.trafficsignals.logic.SignalHeadMountType;
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
   * The key used to store the section info compound in NBT data. Shortened from the historical
   * {@code sectionInfos} literal; reads fall back to {@link #LEGACY_SECTION_INFOS_KEY} for
   * worlds saved before this optimization.
   *
   * @since 1.0
   */
  private static final String SECTION_INFOS_KEY = "sInfs";

  /** Legacy long-form counterpart of {@link #SECTION_INFOS_KEY}. Read-only. */
  private static final String LEGACY_SECTION_INFOS_KEY = "sectionInfos";

  /**
   * The key used to store the body tilt in NBT data.
   *
   * @since 1.0
   */
  private static final String BODY_TILT_KEY = "tlt";

  /** Legacy long-form counterpart of {@link #BODY_TILT_KEY}. Read-only. */
  private static final String LEGACY_BODY_TILT_KEY = "bodyTilt";

  /**
   * The key used to store the section count inside the section info NBT data. Nested key — kept
   * short; not migrated because it only appears as a child of {@link #SECTION_INFOS_KEY}.
   *
   * @since 1.0
   */
  private static final String SECTION_INFO_COMPOUND_COUNT_KEY = "count";

  /**
   * The key prefix used to store each section info in the section info NBT data. Nested — kept.
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

  private static final String ALTERNATE_FLASH_KEY = "altF";
  /** Legacy long-form counterpart of {@link #ALTERNATE_FLASH_KEY}. Read-only. */
  private static final String LEGACY_ALTERNATE_FLASH_KEY = "alternateFlash";
  private boolean alternateFlash = false;

  /**
   * Player-set horizontal-orientation override. When {@code true}, the signal renders in
   * horizontal mode even if its block class isn't statically horizontal. The block's
   * {@code isHorizontal(world, pos)} reads this flag (gated on the block's
   * {@code allowsHorizontalFlip()}) so the TESR, add-on detection, and mount kit layout
   * all pick up the change without needing a separate horizontal block variant.
   */
  private static final String HORIZONTAL_FLIP_KEY = "hF";
  /** Legacy long-form counterpart of {@link #HORIZONTAL_FLIP_KEY}. Read-only. */
  private static final String LEGACY_HORIZONTAL_FLIP_KEY = "horizontalFlip";
  private boolean horizontalFlip = false;

  /**
   * Player-selected bracket hardware style. The TESR renders matching geometry around the
   * signal body at render time; add-on-aware suppression logic hides the bracket on the
   * edge touching an adjacent stacked signal so the hardware doesn't double up.
   */
  private static final String MOUNT_TYPE_KEY = "mT";
  /** Legacy long-form counterpart of {@link #MOUNT_TYPE_KEY}. Read-only. */
  private static final String LEGACY_MOUNT_TYPE_KEY = "mountType";
  private SignalHeadMountType mountType = SignalHeadMountType.NONE;

  /**
   * Paint color for the bracket hardware. Shares the {@link TrafficSignalBodyColor} palette
   * with the body/door/visor for a consistent finish without introducing a new enum.
   */
  private static final String MOUNT_COLOR_KEY = "mC";
  /** Legacy long-form counterpart of {@link #MOUNT_COLOR_KEY}. Read-only. */
  private static final String LEGACY_MOUNT_COLOR_KEY = "mountColor";
  private TrafficSignalBodyColor mountColor = TrafficSignalBodyColor.FLAT_BLACK;

  private boolean dirty = true;
  private boolean powerLossOff = true;

  // Bulb aging fields (short-form keys; LEGACY_* variants retained for back-compat reads)
  private static final String AGING_ENABLED_KEY = "agE";
  private static final String LEGACY_AGING_ENABLED_KEY = "agingEnabled";
  private static final String AGING_LAST_DAY_KEY = "agD";
  private static final String LEGACY_AGING_LAST_DAY_KEY = "lastAgingDay";
  private static final String AGING_STATES_KEY = "agS";
  private static final String LEGACY_AGING_STATES_KEY = "bulbAgingStates";
  private static final String AGING_SEED_KEY = "agSd";
  private static final String LEGACY_AGING_SEED_KEY = "agingSeed";
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
    } else if (compound.hasKey(LEGACY_BODY_TILT_KEY)) {
      bodyTilt = TrafficSignalBodyTilt.fromNBT(compound.getInteger(LEGACY_BODY_TILT_KEY));
    }

    // Get the alternate flash setting
    if (compound.hasKey(ALTERNATE_FLASH_KEY)) {
      alternateFlash = compound.getBoolean(ALTERNATE_FLASH_KEY);
    } else if (compound.hasKey(LEGACY_ALTERNATE_FLASH_KEY)) {
      alternateFlash = compound.getBoolean(LEGACY_ALTERNATE_FLASH_KEY);
    }

    // Get the horizontal-flip setting
    if (compound.hasKey(HORIZONTAL_FLIP_KEY)) {
      horizontalFlip = compound.getBoolean(HORIZONTAL_FLIP_KEY);
    } else if (compound.hasKey(LEGACY_HORIZONTAL_FLIP_KEY)) {
      horizontalFlip = compound.getBoolean(LEGACY_HORIZONTAL_FLIP_KEY);
    }

    // Get the mount type + color
    if (compound.hasKey(MOUNT_TYPE_KEY)) {
      mountType = SignalHeadMountType.fromNBT(compound.getInteger(MOUNT_TYPE_KEY));
    } else if (compound.hasKey(LEGACY_MOUNT_TYPE_KEY)) {
      mountType = SignalHeadMountType.fromNBT(compound.getInteger(LEGACY_MOUNT_TYPE_KEY));
    }
    if (compound.hasKey(MOUNT_COLOR_KEY)) {
      mountColor = TrafficSignalBodyColor.fromNBT(compound.getInteger(MOUNT_COLOR_KEY));
    } else if (compound.hasKey(LEGACY_MOUNT_COLOR_KEY)) {
      mountColor = TrafficSignalBodyColor.fromNBT(compound.getInteger(LEGACY_MOUNT_COLOR_KEY));
    }

    // Get aging settings
    if (compound.hasKey(AGING_ENABLED_KEY)) {
      agingEnabled = compound.getBoolean(AGING_ENABLED_KEY);
    } else if (compound.hasKey(LEGACY_AGING_ENABLED_KEY)) {
      agingEnabled = compound.getBoolean(LEGACY_AGING_ENABLED_KEY);
    }
    if (compound.hasKey(AGING_LAST_DAY_KEY)) {
      lastAgingDay = compound.getLong(AGING_LAST_DAY_KEY);
    } else if (compound.hasKey(LEGACY_AGING_LAST_DAY_KEY)) {
      lastAgingDay = compound.getLong(LEGACY_AGING_LAST_DAY_KEY);
    }
    if (compound.hasKey(AGING_STATES_KEY)) {
      bulbAgingStates = compound.getIntArray(AGING_STATES_KEY);
    } else if (compound.hasKey(LEGACY_AGING_STATES_KEY)) {
      bulbAgingStates = compound.getIntArray(LEGACY_AGING_STATES_KEY);
    }
    if (compound.hasKey(AGING_SEED_KEY)) {
      agingSeed = compound.getLong(AGING_SEED_KEY);
    } else if (compound.hasKey(LEGACY_AGING_SEED_KEY)) {
      agingSeed = compound.getLong(LEGACY_AGING_SEED_KEY);
    }

    // Strip legacy long-form keys after successful migration so the next save is short-only
    compound.removeTag(LEGACY_SECTION_INFOS_KEY);
    compound.removeTag(LEGACY_BODY_TILT_KEY);
    compound.removeTag(LEGACY_ALTERNATE_FLASH_KEY);
    compound.removeTag(LEGACY_HORIZONTAL_FLIP_KEY);
    compound.removeTag(LEGACY_MOUNT_TYPE_KEY);
    compound.removeTag(LEGACY_MOUNT_COLOR_KEY);
    compound.removeTag(LEGACY_AGING_ENABLED_KEY);
    compound.removeTag(LEGACY_AGING_LAST_DAY_KEY);
    compound.removeTag(LEGACY_AGING_STATES_KEY);
    compound.removeTag(LEGACY_AGING_SEED_KEY);

    // Mark as dirty so the renderer recompiles the display list with updated state
    dirty = true;
  }


  private void readSectionInfo(NBTTagCompound compound) {
    String keyToUse = null;
    if (compound.hasKey(SECTION_INFOS_KEY)) {
      keyToUse = SECTION_INFOS_KEY;
    } else if (compound.hasKey(LEGACY_SECTION_INFOS_KEY)) {
      keyToUse = LEGACY_SECTION_INFOS_KEY;
    }
    if (keyToUse != null) {
      NBTTagCompound sectionInfoCompound = compound.getCompoundTag(keyToUse);
      sectionInfos = new TrafficSignalSectionInfo[sectionInfoCompound.getInteger(
          SECTION_INFO_COMPOUND_COUNT_KEY)];
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
    compound.setBoolean(HORIZONTAL_FLIP_KEY, horizontalFlip);
    compound.setInteger(MOUNT_TYPE_KEY, mountType.toNBT());
    compound.setInteger(MOUNT_COLOR_KEY, mountColor.toNBT());

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

    // Hoist every per-frame decision that doesn't vary per-section to local constants so
    // the section loop below can stay a single pass. Before this consolidation the method
    // made four to five linear passes over sectionInfos (initial lighting -> HAWK wigwag
    // override -> bulb-style enforcement -> flash-flip -> aging) which added up quickly
    // on the client side of a signal-dense city.
    Block block = world != null ? world.getBlockState(pos).getBlock() : null;
    boolean useBlockMapping = block instanceof AbstractBlockControllableSignalHead;
    AbstractBlockControllableSignalHead signalBlock =
        useBlockMapping ? (AbstractBlockControllableSignalHead) block : null;
    boolean lightAllSections = useBlockMapping && signalBlock.shouldLightAllSections(currentBulbColor);
    TrafficSignalBulbStyle enforcedStyle = useBlockMapping ? signalBlock.getEnforcedBulbStyle() : null;

    boolean hawkWigwag = currentBulbColor == 2 && block instanceof BlockControllableHawkSignal;
    BlockControllableHawkSignal hawkBlock = hawkWigwag ? (BlockControllableHawkSignal) block : null;

    // Flash flip — alternateFlash inverts the flash phase for wig-wag beacon pairs. Uses
    // the pause-aware game clock so flashes freeze when paused.
    long blinkInterval = 500L; // ms
    long gameMillis = world != null ? CsmRenderUtils.gameMillis(world) : 0L;
    boolean firstHalfOfSecond = world != null
        && (gameMillis % (blinkInterval * 2L)) < blinkInterval;
    if (alternateFlash) firstHalfOfSecond = !firstHalfOfSecond;

    // Aging-effect pre-checks — skipping these per-section entirely when no bulb has an
    // aging state keeps the happy path branch-free.
    boolean anyAging = bulbAgingStates.length > 0;

    for (int i = 0; i < sectionInfos.length; i++) {
      TrafficSignalSectionInfo sectionInfo = sectionInfos[i];

      // Pass 1/2 fused: determine initial lit state. HAWK wigwag (color=2 on HAWK signals)
      // overrides the normal mapping with a per-section alternating pattern; otherwise
      // fall through to the block's color mapping or the default mapping.
      boolean lit;
      if (hawkWigwag) {
        lit = hawkBlock.shouldLightWigwagSection(i, gameMillis);
      } else if (currentBulbColor == 3) {
        lit = false;
      } else if (useBlockMapping) {
        lit = lightAllSections
            || signalBlock.shouldLightBulb(currentBulbColor, sectionInfo.getBulbColor());
      } else {
        // Fallback: default color mapping
        TrafficSignalBulbColor bulbColor = sectionInfo.getBulbColor();
        lit = (currentBulbColor == 0 && bulbColor == TrafficSignalBulbColor.RED)
            || (currentBulbColor == 1 && bulbColor == TrafficSignalBulbColor.YELLOW)
            || (currentBulbColor == 2 && bulbColor == TrafficSignalBulbColor.GREEN);
      }

      // Pass 3 fused: bulb style enforcement (e.g., bi-modal arrows).
      if (enforcedStyle != null) {
        sectionInfo.setBulbStyle(enforcedStyle);
      }

      // Pass 4 fused: flash flip — if the bulb is lit and set to flashing, blink it off
      // during the first half of the cycle.
      if (lit && firstHalfOfSecond && sectionInfo.isBulbFlashing()) {
        lit = false;
      }

      // Snapshot the controller-commanded lit state before aging dims/blanks it. The
      // bulb itself may be failing or burned out, but accessories on the same section
      // (e.g. the Barlo strobe) are physically separate and should keep working as
      // long as the controller is energizing this section.
      sectionInfo.setBulbCommandedLit(lit);

      // Pass 5 fused: aging effects. Dead bulbs always render dark; failing bulbs get a
      // sin-wave flicker / strobe-burst modulation that can drop them dark this frame.
      if (anyAging && i < bulbAgingStates.length) {
        int agingState = bulbAgingStates[i];
        if (agingState == AGING_DEAD) {
          lit = false;
        } else if (agingState == AGING_FAILING && lit) {
          lit = applyFailingBulbModulation(i, gameMillis);
        }
      }

      sectionInfo.setBulbLit(lit);
    }

    return sectionInfos;
  }

  /**
   * Returns whether a failing bulb should be lit this frame. Extracted from the former
   * {@code applyAgingEffects} loop so the per-section branch in the fused
   * {@link #getSectionInfos(int)} pass can short-circuit to {@code lit=false} without an
   * extra method call when aging isn't in play.
   *
   * <p>Uses a slow-cycling sine wave to switch between two failure modes: rapid strobe
   * bursts (~30% of the time) vs. organic flicker from overlapping sine waves (majority).
   *
   * @param sectionIndex the section that's in AGING_FAILING state
   * @param now          the pause-aware game clock, used as the phase input to the flicker
   *                     waves
   * @return {@code true} if the bulb should be lit this frame, {@code false} if the
   *     failure mode blanks it
   */
  private boolean applyFailingBulbModulation(int sectionIndex, long now) {
    long seed = pos.toLong() + sectionIndex;

    // Determine which failure mode is active using a slow-cycling wave seeded per bulb.
    //   mode < 0.3  → rapid strobe burst
    //   mode >= 0.3 → organic sine-wave flicker (majority of the time)
    double mode = (Math.sin((now + seed * 79) * 0.0004) + 1.0) / 2.0; // 0..1

    if (mode < 0.3) {
      // Rapid strobe with varying speed — a secondary wave picks the frequency so the
      // strobe accelerates and decelerates organically.
      double speedWave = (Math.sin((now + seed * 173) * 0.0011) + 1.0) / 2.0; // 0..1
      long period = 16L + (long) (speedWave * 34L); // 16-50 ms → 60-20 Hz
      boolean strobeOff = ((now / period) + seed) % 3L != 0L;
      return !strobeOff;
    }

    // Organic flicker using overlapping sine waves
    double phase1 = Math.sin((now + seed * 137) * 0.013);
    double phase2 = Math.sin((now + seed * 251) * 0.007);
    double phase3 = Math.sin((now + seed * 397) * 0.031);
    double combined = phase1 + phase2 * 0.5 + phase3 * 0.3;
    return combined >= -0.2;
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
   * Returns the player-set horizontal-orientation override. Only meaningful when the block
   * claims {@code allowsHorizontalFlip()} — blocks that don't allow it should ignore this value.
   */
  public boolean isHorizontalFlip() {
    return horizontalFlip;
  }

  /**
   * Toggles the horizontal-orientation override and returns the new value.
   */
  public boolean toggleHorizontalFlip() {
    horizontalFlip = !horizontalFlip;
    markDirtySync(world, pos, true);
    return horizontalFlip;
  }

  /** Returns the currently-selected bracket mount style. Never null. */
  public SignalHeadMountType getMountType() {
    return mountType;
  }

  /** Cycles to the next mount style (NONE → REAR → LEFT → RIGHT → NONE) and returns it. */
  public SignalHeadMountType getNextMountType() {
    mountType = mountType.getNext();
    markDirtySync(world, pos, true);
    return mountType;
  }

  /** Returns the currently-selected mount paint color. Never null. */
  public TrafficSignalBodyColor getMountColor() {
    return mountColor;
  }

  /** Cycles to the next mount paint color and returns it. */
  public TrafficSignalBodyColor getNextMountColor() {
    mountColor = mountColor.getNextColor();
    markDirtySync(world, pos, true);
    return mountColor;
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

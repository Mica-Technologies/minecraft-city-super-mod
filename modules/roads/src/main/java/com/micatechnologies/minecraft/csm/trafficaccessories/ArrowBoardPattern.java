package com.micatechnologies.minecraft.csm.trafficaccessories;

import com.micatechnologies.minecraft.csm.CsmConfig;
import net.minecraft.util.IStringSerializable;
import org.jetbrains.annotations.NotNull;

/**
 * What an arrow board is displaying, as a sequence of stages.
 *
 * <p>Every mode here is animated, because every mode on a real board is: the MUTCD ones are the
 * sequential chevron, the sequential arrow, the flashing arrow and the flashing caution, and a
 * board showing a steady arrow is a board with a fault. A stage is the set of lamps lit at one
 * moment, given as {@code {column, row}} pairs with column 0 at the left of the panel and row 0
 * at the top; the renderer walks the stages on a timer.</p>
 *
 * <p>Lamps are addressed by GRID POSITION rather than by coordinates. Where those positions
 * actually fall on the panel is {@link ArrowBoardGeometry}'s business, which is generated
 * alongside the model, so a change to the board's size moves the lamps without touching the
 * patterns.</p>
 *
 * <p>The right-hand forms are written out and the left-hand ones mirrored from them, so the pair
 * cannot drift apart. The direction is the entire message of the device.</p>
 *
 * @version 1.0
 * @since 2026.9
 */
public enum ArrowBoardPattern implements IStringSerializable {

  /** Sequential chevron pointing right: three chevrons chase toward the point. */
  CHEVRON_RIGHT("chevron_right", "Sequential Chevron (Right)", 420L,
      ArrowBoardPatterns.chevronSequence(false)),

  /** Sequential chevron pointing left. */
  CHEVRON_LEFT("chevron_left", "Sequential Chevron (Left)", 420L,
      ArrowBoardPatterns.chevronSequence(true)),

  /** Sequential arrow pointing right: the shaft draws itself, then the head appears. */
  BAR_SEQUENTIAL_RIGHT("bar_sequential_right", "Sequential Arrow (Right)", 380L,
      ArrowBoardPatterns.sequentialArrow(false)),

  /** Sequential arrow pointing left. */
  BAR_SEQUENTIAL_LEFT("bar_sequential_left", "Sequential Arrow (Left)", 380L,
      ArrowBoardPatterns.sequentialArrow(true)),

  /** Flashing bar arrow pointing right: the whole arrow on, then off. */
  BAR_RIGHT("bar_right", "Flashing Arrow (Right)", 700L,
      ArrowBoardPatterns.flashingArrow(false)),

  /** Flashing bar arrow pointing left. */
  BAR_LEFT("bar_left", "Flashing Arrow (Left)", 700L,
      ArrowBoardPatterns.flashingArrow(true)),

  /** Flashing caution: the four corner lamps together, then dark. */
  CAUTION("caution", "Flashing Caution", 700L, ArrowBoardPatterns.flashingCaution());

  /** The serialized name. */
  private final String name;

  /** The name shown to the player. */
  private final String friendlyName;

  /**
   * How long each stage is held, in milliseconds, at the standard rate. The configured arrow
   * board speed scales this; see {@link CsmConfig#scaleArrowBoardStage(long)}.
   */
  private final long stageMillis;

  /** The stages, each a set of {column, row} lamp positions. */
  private final int[][][] stages;

  ArrowBoardPattern(String name, String friendlyName, long stageMillis, int[][][] stages) {
    this.name = name;
    this.friendlyName = friendlyName;
    this.stageMillis = stageMillis;
    this.stages = stages;
  }

  @Override
  public @NotNull String getName() {
    return name;
  }

  /**
   * Gets the name shown to the player when the board is switched.
   *
   * @return the friendly name
   *
   * @since 1.0
   */
  public String getFriendlyName() {
    return friendlyName;
  }

  /**
   * Gets the lamps lit at the given moment.
   *
   * @param millis the board's own clock, in milliseconds
   *
   * @return the lit lamps, each {column, row}
   *
   * @since 1.0
   */
  public int[][] getLitLamps(long millis) {
    long scaled = CsmConfig.scaleArrowBoardStage(stageMillis);
    int stage = (int) (Math.floorDiv(millis, scaled) % stages.length);
    return stages[stage];
  }

  /**
   * Gets the next pattern, wrapping, for cycling the board in world.
   *
   * @return the next pattern
   *
   * @since 1.0
   */
  public ArrowBoardPattern next() {
    ArrowBoardPattern[] values = values();
    return values[(ordinal() + 1) % values.length];
  }

  /**
   * Gets the previous pattern, wrapping.
   *
   * <p>There are seven of these, so a board that only cycles one way is six clicks from the mode
   * just behind it.</p>
   *
   * @return the previous pattern
   *
   * @since 1.0
   */
  public ArrowBoardPattern previous() {
    ArrowBoardPattern[] values = values();
    return values[(ordinal() + values.length - 1) % values.length];
  }

  /**
   * Gets the pattern with the given ordinal, falling back to the first.
   *
   * @param ordinal the ordinal, as persisted
   *
   * @return the pattern
   *
   * @since 1.0
   */
  public static ArrowBoardPattern fromOrdinal(int ordinal) {
    ArrowBoardPattern[] values = values();
    return ordinal < 0 || ordinal >= values.length ? values[0] : values[ordinal];
  }
}

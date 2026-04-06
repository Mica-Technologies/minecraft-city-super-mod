package com.micatechnologies.minecraft.csm.trafficsignals;

import com.micatechnologies.minecraft.csm.trafficsignals.logic.AbstractBlockControllableSignal.SIGNAL_SIDE;
import com.micatechnologies.minecraft.csm.trafficsignals.logic.TrafficSignalBodyColor;
import com.micatechnologies.minecraft.csm.trafficsignals.logic.TrafficSignalBodyTilt;
import com.micatechnologies.minecraft.csm.trafficsignals.logic.TrafficSignalBulbColor;
import com.micatechnologies.minecraft.csm.trafficsignals.logic.TrafficSignalBulbStyle;
import com.micatechnologies.minecraft.csm.trafficsignals.logic.TrafficSignalBulbType;
import com.micatechnologies.minecraft.csm.trafficsignals.logic.TrafficSignalSectionInfo;
import com.micatechnologies.minecraft.csm.trafficsignals.logic.TrafficSignalVisorType;

/**
 * Central registry of all factory-created traffic signal head blocks. Each public static
 * field holds a pre-constructed {@link BlockControllableSignal} instance that replaces a
 * former boilerplate subclass of AbstractBlockControllableSignalHead.
 *
 * <p>Fields are initialized eagerly in declaration order. Registration with
 * {@link com.micatechnologies.minecraft.csm.CsmRegistry} happens inside the
 * {@link BlockControllableSignal} constructor (inherited from AbstractBlock).</p>
 *
 * @since 2026.4
 */
public final class TrafficSignalBlocks {

  private TrafficSignalBlocks() {}

  public static final BlockControllableSignal DOGHOUSE_MAIN_LEFT_SIGNAL =
      new BlockControllableSignal.Builder(
          "controllabledoghousesignalmainleft",
          SIGNAL_SIDE.THROUGH,
          true,
          () -> {
            return new TrafficSignalSectionInfo[] {
            new TrafficSignalSectionInfo(TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK,
            TrafficSignalVisorType.CIRCLE, TrafficSignalBulbStyle.LED, TrafficSignalBulbType.BALL,
            TrafficSignalBulbColor.RED, false),
            new TrafficSignalSectionInfo(TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK,
            TrafficSignalVisorType.CIRCLE, TrafficSignalBulbStyle.LED, TrafficSignalBulbType.BALL,
            TrafficSignalBulbColor.YELLOW, false),
            new TrafficSignalSectionInfo(TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK,
            TrafficSignalVisorType.CIRCLE, TrafficSignalBulbStyle.LED, TrafficSignalBulbType.BALL,
            TrafficSignalBulbColor.GREEN, false)
            };
          })
          .sectionXPositions(0, 6, 6)
          .build();

  public static final BlockControllableSignal DOGHOUSE_MAIN_RIGHT_SIGNAL =
      new BlockControllableSignal.Builder(
          "controllabledoghousesignalmainright",
          SIGNAL_SIDE.THROUGH,
          true,
          () -> {
            return new TrafficSignalSectionInfo[] {
            new TrafficSignalSectionInfo(TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK,
            TrafficSignalVisorType.CIRCLE, TrafficSignalBulbStyle.LED, TrafficSignalBulbType.BALL,
            TrafficSignalBulbColor.RED, false),
            new TrafficSignalSectionInfo(TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK,
            TrafficSignalVisorType.CIRCLE, TrafficSignalBulbStyle.LED, TrafficSignalBulbType.BALL,
            TrafficSignalBulbColor.YELLOW, false),
            new TrafficSignalSectionInfo(TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK,
            TrafficSignalVisorType.CIRCLE, TrafficSignalBulbStyle.LED, TrafficSignalBulbType.BALL,
            TrafficSignalBulbColor.GREEN, false)
            };
          })
          .sectionXPositions(0, -6, -6)
          .build();

  public static final BlockControllableSignal DOGHOUSE_SECONDARY_LEFT_FYA_SIGNAL =
      new BlockControllableSignal.Builder(
          "controllabledoghousesignalsecondaryleftfya",
          SIGNAL_SIDE.THROUGH,
          false,
          () -> {
            return new TrafficSignalSectionInfo[] {
            new TrafficSignalSectionInfo(TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK,
            TrafficSignalVisorType.CIRCLE, TrafficSignalBulbStyle.LED, TrafficSignalBulbType.RIGHT,
            TrafficSignalBulbColor.YELLOW, false),
            new TrafficSignalSectionInfo(TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK,
            TrafficSignalVisorType.CIRCLE, TrafficSignalBulbStyle.LED, TrafficSignalBulbType.RIGHT,
            TrafficSignalBulbColor.GREEN, TrafficSignalBulbColor.YELLOW, false, true)
            };
          })
          .sectionYPositions(16.0f, 4.0f)
          .sectionXPositions(-6, -6)
          .build();

  public static final BlockControllableSignal DOGHOUSE_SECONDARY_LEFT_SIGNAL =
      new BlockControllableSignal.Builder(
          "controllabledoghousesignalsecondaryleft",
          SIGNAL_SIDE.RIGHT,
          true,
          () -> {
            return new TrafficSignalSectionInfo[] {
            new TrafficSignalSectionInfo(TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK,
            TrafficSignalVisorType.CIRCLE, TrafficSignalBulbStyle.LED, TrafficSignalBulbType.RIGHT,
            TrafficSignalBulbColor.YELLOW, false),
            new TrafficSignalSectionInfo(TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK,
            TrafficSignalVisorType.CIRCLE, TrafficSignalBulbStyle.LED, TrafficSignalBulbType.RIGHT,
            TrafficSignalBulbColor.GREEN, false)
            };
          })
          .sectionYPositions(16.0f, 4.0f)
          .sectionXPositions(-6, -6)
          .build();

  public static final BlockControllableSignal DOGHOUSE_SECONDARY_RIGHT_FYA_SIGNAL =
      new BlockControllableSignal.Builder(
          "controllabledoghousesignalsecondaryrightfya",
          SIGNAL_SIDE.THROUGH,
          false,
          () -> {
            return new TrafficSignalSectionInfo[] {
            new TrafficSignalSectionInfo(TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK,
            TrafficSignalVisorType.CIRCLE, TrafficSignalBulbStyle.LED, TrafficSignalBulbType.LEFT,
            TrafficSignalBulbColor.YELLOW, false),
            new TrafficSignalSectionInfo(TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK,
            TrafficSignalVisorType.CIRCLE, TrafficSignalBulbStyle.LED, TrafficSignalBulbType.LEFT,
            TrafficSignalBulbColor.GREEN, TrafficSignalBulbColor.YELLOW, false, true)
            };
          })
          .sectionYPositions(16.0f, 4.0f)
          .sectionXPositions(6, 6)
          .build();

  public static final BlockControllableSignal DOGHOUSE_SECONDARY_RIGHT_SIGNAL =
      new BlockControllableSignal.Builder(
          "controllabledoghousesignalsecondaryright",
          SIGNAL_SIDE.LEFT,
          true,
          () -> {
            return new TrafficSignalSectionInfo[] {
            new TrafficSignalSectionInfo(TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK,
            TrafficSignalVisorType.CIRCLE, TrafficSignalBulbStyle.LED, TrafficSignalBulbType.LEFT,
            TrafficSignalBulbColor.YELLOW, false),
            new TrafficSignalSectionInfo(TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK,
            TrafficSignalVisorType.CIRCLE, TrafficSignalBulbStyle.LED, TrafficSignalBulbType.LEFT,
            TrafficSignalBulbColor.GREEN, false)
            };
          })
          .sectionYPositions(16.0f, 4.0f)
          .sectionXPositions(6, 6)
          .build();

  public static final BlockControllableSignal HORIZONTAL_AHEAD_SIGNAL =
      new BlockControllableSignal.Builder(
          "controllablehorizontalaheadsignal",
          SIGNAL_SIDE.THROUGH,
          true,
          () -> {
            return new TrafficSignalSectionInfo[] {
            new TrafficSignalSectionInfo(TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK,
            TrafficSignalVisorType.CIRCLE, TrafficSignalBulbStyle.LED, TrafficSignalBulbType.BALL,
            TrafficSignalBulbColor.RED, false),
            new TrafficSignalSectionInfo(TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK,
            TrafficSignalVisorType.CIRCLE, TrafficSignalBulbStyle.LED, TrafficSignalBulbType.BALL,
            TrafficSignalBulbColor.YELLOW, false),
            new TrafficSignalSectionInfo(TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK,
            TrafficSignalVisorType.CIRCLE, TrafficSignalBulbStyle.LED, TrafficSignalBulbType.UP,
            TrafficSignalBulbColor.GREEN, false)
            };
          })
          .horizontal(true)
          .sectionYPositions(0.0f, 0.0f, 0.0f)
          .sectionXPositions(12.0f, 0.0f, -12.0f)
          .build();

  public static final BlockControllableSignal HORIZONTAL_ANGLE_AHEAD_SIGNAL =
      new BlockControllableSignal.Builder(
          "controllablehorizontalangleaheadsignal",
          SIGNAL_SIDE.THROUGH,
          true,
          () -> {
            return new TrafficSignalSectionInfo[] {
            new TrafficSignalSectionInfo(TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK,
            TrafficSignalVisorType.CIRCLE, TrafficSignalBulbStyle.LED, TrafficSignalBulbType.BALL,
            TrafficSignalBulbColor.RED, false),
            new TrafficSignalSectionInfo(TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK,
            TrafficSignalVisorType.CIRCLE, TrafficSignalBulbStyle.LED, TrafficSignalBulbType.BALL,
            TrafficSignalBulbColor.YELLOW, false),
            new TrafficSignalSectionInfo(TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK,
            TrafficSignalVisorType.CIRCLE, TrafficSignalBulbStyle.LED, TrafficSignalBulbType.UP,
            TrafficSignalBulbColor.GREEN, false)
            };
          })
          .horizontal(true)
          .sectionYPositions(0.0f, 0.0f, 0.0f)
          .sectionXPositions(12.0f, 0.0f, -12.0f)
          .retiring("controllablehorizontalaheadsignal", TrafficSignalBodyTilt.RIGHT_ANGLE)
          .build();

  public static final BlockControllableSignal HORIZONTAL_ANGLE_BIKE_SIGNAL =
      new BlockControllableSignal.Builder(
          "controllablehorizontalanglebikesignal",
          SIGNAL_SIDE.PROTECTED,
          true,
          () -> {
            return new TrafficSignalSectionInfo[] {
            new TrafficSignalSectionInfo(TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK,
            TrafficSignalVisorType.CIRCLE, TrafficSignalBulbStyle.LED, TrafficSignalBulbType.BIKE,
            TrafficSignalBulbColor.RED, false),
            new TrafficSignalSectionInfo(TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK,
            TrafficSignalVisorType.CIRCLE, TrafficSignalBulbStyle.LED, TrafficSignalBulbType.BIKE,
            TrafficSignalBulbColor.YELLOW, false),
            new TrafficSignalSectionInfo(TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK,
            TrafficSignalVisorType.CIRCLE, TrafficSignalBulbStyle.LED, TrafficSignalBulbType.BIKE,
            TrafficSignalBulbColor.GREEN, false)
            };
          })
          .horizontal(true)
          .sectionYPositions(0.0f, 0.0f, 0.0f)
          .sectionXPositions(12.0f, 0.0f, -12.0f)
          .retiring("controllablehorizontalbikesignal", TrafficSignalBodyTilt.RIGHT_ANGLE)
          .build();

  public static final BlockControllableSignal HORIZONTAL_ANGLE_LEFT_SIGNAL =
      new BlockControllableSignal.Builder(
          "controllablehorizontalangleleftsignal",
          SIGNAL_SIDE.LEFT,
          true,
          () -> {
            return new TrafficSignalSectionInfo[] {
            new TrafficSignalSectionInfo(TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK,
            TrafficSignalVisorType.CIRCLE, TrafficSignalBulbStyle.LED, TrafficSignalBulbType.LEFT,
            TrafficSignalBulbColor.RED, false),
            new TrafficSignalSectionInfo(TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK,
            TrafficSignalVisorType.CIRCLE, TrafficSignalBulbStyle.LED, TrafficSignalBulbType.LEFT,
            TrafficSignalBulbColor.YELLOW, false),
            new TrafficSignalSectionInfo(TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK,
            TrafficSignalVisorType.CIRCLE, TrafficSignalBulbStyle.LED, TrafficSignalBulbType.LEFT,
            TrafficSignalBulbColor.GREEN, false)
            };
          })
          .horizontal(true)
          .sectionYPositions(0.0f, 0.0f, 0.0f)
          .sectionXPositions(12.0f, 0.0f, -12.0f)
          .retiring("controllablehorizontalleftsignal", TrafficSignalBodyTilt.RIGHT_ANGLE)
          .build();

  public static final BlockControllableSignal HORIZONTAL_ANGLE_RAIL_SIGNAL =
      new BlockControllableSignal.Builder(
          "controllablehorizontalanglerailsignal",
          SIGNAL_SIDE.PROTECTED,
          true,
          () -> {
            return new TrafficSignalSectionInfo[] {
            new TrafficSignalSectionInfo(TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK,
            TrafficSignalVisorType.CIRCLE, TrafficSignalBulbStyle.LED, TrafficSignalBulbType.TRANSIT,
            TrafficSignalBulbColor.RED, false),
            new TrafficSignalSectionInfo(TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK,
            TrafficSignalVisorType.CIRCLE, TrafficSignalBulbStyle.LED, TrafficSignalBulbType.TRANSIT,
            TrafficSignalBulbColor.YELLOW, false),
            new TrafficSignalSectionInfo(TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK,
            TrafficSignalVisorType.CIRCLE, TrafficSignalBulbStyle.LED, TrafficSignalBulbType.TRANSIT,
            TrafficSignalBulbColor.GREEN, false)
            };
          })
          .horizontal(true)
          .sectionYPositions(0.0f, 0.0f, 0.0f)
          .sectionXPositions(12.0f, 0.0f, -12.0f)
          .retiring("controllablehorizontalrailsignal", TrafficSignalBodyTilt.RIGHT_ANGLE)
          .build();

  public static final BlockControllableSignal HORIZONTAL_ANGLE_RIGHT2_SIGNAL =
      new BlockControllableSignal.Builder(
          "controllablehorizontalangleright2signal",
          SIGNAL_SIDE.RIGHT,
          true,
          () -> {
            return new TrafficSignalSectionInfo[] {
            new TrafficSignalSectionInfo(TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK,
            TrafficSignalVisorType.CIRCLE, TrafficSignalBulbStyle.LED, TrafficSignalBulbType.RIGHT,
            TrafficSignalBulbColor.RED, false),
            new TrafficSignalSectionInfo(TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK,
            TrafficSignalVisorType.CIRCLE, TrafficSignalBulbStyle.LED, TrafficSignalBulbType.RIGHT,
            TrafficSignalBulbColor.YELLOW, false),
            new TrafficSignalSectionInfo(TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK,
            TrafficSignalVisorType.CIRCLE, TrafficSignalBulbStyle.LED, TrafficSignalBulbType.RIGHT,
            TrafficSignalBulbColor.GREEN, false)
            };
          })
          .horizontal(true)
          .sectionYPositions(0.0f, 0.0f, 0.0f)
          .sectionXPositions(12.0f, 0.0f, -12.0f)
          .retiring("controllablehorizontalright2signal", TrafficSignalBodyTilt.RIGHT_ANGLE)
          .build();

  public static final BlockControllableSignal HORIZONTAL_ANGLE_RIGHT_SIGNAL =
      new BlockControllableSignal.Builder(
          "controllablehorizontalanglerightsignal",
          SIGNAL_SIDE.RIGHT,
          true,
          () -> {
            return new TrafficSignalSectionInfo[] {
            new TrafficSignalSectionInfo(TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK,
            TrafficSignalVisorType.CIRCLE, TrafficSignalBulbStyle.LED, TrafficSignalBulbType.RIGHT,
            TrafficSignalBulbColor.RED, false),
            new TrafficSignalSectionInfo(TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK,
            TrafficSignalVisorType.CIRCLE, TrafficSignalBulbStyle.LED, TrafficSignalBulbType.RIGHT,
            TrafficSignalBulbColor.YELLOW, false),
            new TrafficSignalSectionInfo(TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK,
            TrafficSignalVisorType.CIRCLE, TrafficSignalBulbStyle.LED, TrafficSignalBulbType.RIGHT,
            TrafficSignalBulbColor.GREEN, false)
            };
          })
          .horizontal(true)
          .sectionYPositions(0.0f, 0.0f, 0.0f)
          .sectionXPositions(12.0f, 0.0f, -12.0f)
          .retiring("controllablehorizontalrightsignal", TrafficSignalBodyTilt.RIGHT_ANGLE)
          .build();

  public static final BlockControllableSignal HORIZONTAL_ANGLE_SOLID_SIGNAL =
      new BlockControllableSignal.Builder(
          "controllablehorizontalanglesolidsignal",
          SIGNAL_SIDE.THROUGH,
          true,
          () -> {
            return new TrafficSignalSectionInfo[] {
            new TrafficSignalSectionInfo(TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK,
            TrafficSignalVisorType.CIRCLE, TrafficSignalBulbStyle.LED, TrafficSignalBulbType.BALL,
            TrafficSignalBulbColor.RED, false),
            new TrafficSignalSectionInfo(TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK,
            TrafficSignalVisorType.CIRCLE, TrafficSignalBulbStyle.LED, TrafficSignalBulbType.BALL,
            TrafficSignalBulbColor.YELLOW, false),
            new TrafficSignalSectionInfo(TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK,
            TrafficSignalVisorType.CIRCLE, TrafficSignalBulbStyle.LED, TrafficSignalBulbType.BALL,
            TrafficSignalBulbColor.GREEN, false)
            };
          })
          .horizontal(true)
          .sectionYPositions(0.0f, 0.0f, 0.0f)
          .sectionXPositions(12.0f, 0.0f, -12.0f)
          .retiring("controllablehorizontalsolidsignal", TrafficSignalBodyTilt.RIGHT_ANGLE)
          .build();

  public static final BlockControllableSignal HORIZONTAL_ANGLE_U_TURN_SIGNAL =
      new BlockControllableSignal.Builder(
          "controllablehorizontalangleuturnsignal",
          SIGNAL_SIDE.LEFT,
          true,
          () -> {
            return new TrafficSignalSectionInfo[] {
            new TrafficSignalSectionInfo(TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK,
            TrafficSignalVisorType.CIRCLE, TrafficSignalBulbStyle.LED, TrafficSignalBulbType.UTURN,
            TrafficSignalBulbColor.RED, false),
            new TrafficSignalSectionInfo(TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK,
            TrafficSignalVisorType.CIRCLE, TrafficSignalBulbStyle.LED, TrafficSignalBulbType.UTURN,
            TrafficSignalBulbColor.YELLOW, false),
            new TrafficSignalSectionInfo(TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK,
            TrafficSignalVisorType.CIRCLE, TrafficSignalBulbStyle.LED, TrafficSignalBulbType.UTURN,
            TrafficSignalBulbColor.GREEN, false)
            };
          })
          .horizontal(true)
          .sectionYPositions(0.0f, 0.0f, 0.0f)
          .sectionXPositions(12.0f, 0.0f, -12.0f)
          .retiring("controllablehorizontaluturnsignal", TrafficSignalBodyTilt.RIGHT_ANGLE)
          .build();

  public static final BlockControllableSignal HORIZONTAL_ANGLE_UP_LEFT_SIGNAL =
      new BlockControllableSignal.Builder(
          "controllablehorizontalangleupleftsignal",
          SIGNAL_SIDE.LEFT,
          true,
          () -> {
            return new TrafficSignalSectionInfo[] {
            new TrafficSignalSectionInfo(TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK,
            TrafficSignalVisorType.CIRCLE, TrafficSignalBulbStyle.LED, TrafficSignalBulbType.UP_LEFT,
            TrafficSignalBulbColor.RED, false),
            new TrafficSignalSectionInfo(TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK,
            TrafficSignalVisorType.CIRCLE, TrafficSignalBulbStyle.LED, TrafficSignalBulbType.UP_LEFT,
            TrafficSignalBulbColor.YELLOW, false),
            new TrafficSignalSectionInfo(TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK,
            TrafficSignalVisorType.CIRCLE, TrafficSignalBulbStyle.LED, TrafficSignalBulbType.UP_LEFT,
            TrafficSignalBulbColor.GREEN, false)
            };
          })
          .horizontal(true)
          .sectionYPositions(0.0f, 0.0f, 0.0f)
          .sectionXPositions(12.0f, 0.0f, -12.0f)
          .retiring("controllablehorizontalupleftsignal", TrafficSignalBodyTilt.RIGHT_ANGLE)
          .build();

  public static final BlockControllableSignal HORIZONTAL_BIKE_SIGNAL =
      new BlockControllableSignal.Builder(
          "controllablehorizontalbikesignal",
          SIGNAL_SIDE.PROTECTED,
          true,
          () -> {
            return new TrafficSignalSectionInfo[] {
            new TrafficSignalSectionInfo(TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK,
            TrafficSignalVisorType.CIRCLE, TrafficSignalBulbStyle.LED, TrafficSignalBulbType.BIKE,
            TrafficSignalBulbColor.RED, false),
            new TrafficSignalSectionInfo(TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK,
            TrafficSignalVisorType.CIRCLE, TrafficSignalBulbStyle.LED, TrafficSignalBulbType.BIKE,
            TrafficSignalBulbColor.YELLOW, false),
            new TrafficSignalSectionInfo(TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK,
            TrafficSignalVisorType.CIRCLE, TrafficSignalBulbStyle.LED, TrafficSignalBulbType.BIKE,
            TrafficSignalBulbColor.GREEN, false)
            };
          })
          .horizontal(true)
          .sectionYPositions(0.0f, 0.0f, 0.0f)
          .sectionXPositions(12.0f, 0.0f, -12.0f)
          .build();

  public static final BlockControllableSignal HORIZONTAL_LEFT_SIGNAL =
      new BlockControllableSignal.Builder(
          "controllablehorizontalleftsignal",
          SIGNAL_SIDE.LEFT,
          true,
          () -> {
            return new TrafficSignalSectionInfo[] {
            new TrafficSignalSectionInfo(TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK,
            TrafficSignalVisorType.CIRCLE, TrafficSignalBulbStyle.LED, TrafficSignalBulbType.LEFT,
            TrafficSignalBulbColor.RED, false),
            new TrafficSignalSectionInfo(TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK,
            TrafficSignalVisorType.CIRCLE, TrafficSignalBulbStyle.LED, TrafficSignalBulbType.LEFT,
            TrafficSignalBulbColor.YELLOW, false),
            new TrafficSignalSectionInfo(TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK,
            TrafficSignalVisorType.CIRCLE, TrafficSignalBulbStyle.LED, TrafficSignalBulbType.LEFT,
            TrafficSignalBulbColor.GREEN, false)
            };
          })
          .horizontal(true)
          .sectionYPositions(0.0f, 0.0f, 0.0f)
          .sectionXPositions(12.0f, 0.0f, -12.0f)
          .build();

  public static final BlockControllableSignal HORIZONTAL_RAIL_SIGNAL =
      new BlockControllableSignal.Builder(
          "controllablehorizontalrailsignal",
          SIGNAL_SIDE.PROTECTED,
          true,
          () -> {
            return new TrafficSignalSectionInfo[] {
            new TrafficSignalSectionInfo(TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK,
            TrafficSignalVisorType.CIRCLE, TrafficSignalBulbStyle.LED, TrafficSignalBulbType.TRANSIT,
            TrafficSignalBulbColor.RED, false),
            new TrafficSignalSectionInfo(TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK,
            TrafficSignalVisorType.CIRCLE, TrafficSignalBulbStyle.LED, TrafficSignalBulbType.TRANSIT,
            TrafficSignalBulbColor.YELLOW, false),
            new TrafficSignalSectionInfo(TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK,
            TrafficSignalVisorType.CIRCLE, TrafficSignalBulbStyle.LED, TrafficSignalBulbType.TRANSIT,
            TrafficSignalBulbColor.GREEN, false)
            };
          })
          .horizontal(true)
          .sectionYPositions(0.0f, 0.0f, 0.0f)
          .sectionXPositions(12.0f, 0.0f, -12.0f)
          .build();

  public static final BlockControllableSignal HORIZONTAL_RIGHT2_SIGNAL =
      new BlockControllableSignal.Builder(
          "controllablehorizontalright2signal",
          SIGNAL_SIDE.RIGHT,
          true,
          () -> {
            return new TrafficSignalSectionInfo[] {
            new TrafficSignalSectionInfo(TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK,
            TrafficSignalVisorType.CIRCLE, TrafficSignalBulbStyle.LED, TrafficSignalBulbType.RIGHT,
            TrafficSignalBulbColor.RED, false),
            new TrafficSignalSectionInfo(TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK,
            TrafficSignalVisorType.CIRCLE, TrafficSignalBulbStyle.LED, TrafficSignalBulbType.RIGHT,
            TrafficSignalBulbColor.YELLOW, false),
            new TrafficSignalSectionInfo(TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK,
            TrafficSignalVisorType.CIRCLE, TrafficSignalBulbStyle.LED, TrafficSignalBulbType.RIGHT,
            TrafficSignalBulbColor.GREEN, false)
            };
          })
          .horizontal(true)
          .sectionYPositions(0.0f, 0.0f, 0.0f)
          .sectionXPositions(12.0f, 0.0f, -12.0f)
          .build();

  public static final BlockControllableSignal HORIZONTAL_RIGHT_SIGNAL =
      new BlockControllableSignal.Builder(
          "controllablehorizontalrightsignal",
          SIGNAL_SIDE.RIGHT,
          true,
          () -> {
            return new TrafficSignalSectionInfo[] {
            new TrafficSignalSectionInfo(TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK,
            TrafficSignalVisorType.CIRCLE, TrafficSignalBulbStyle.LED, TrafficSignalBulbType.RIGHT,
            TrafficSignalBulbColor.RED, false),
            new TrafficSignalSectionInfo(TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK,
            TrafficSignalVisorType.CIRCLE, TrafficSignalBulbStyle.LED, TrafficSignalBulbType.RIGHT,
            TrafficSignalBulbColor.YELLOW, false),
            new TrafficSignalSectionInfo(TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK,
            TrafficSignalVisorType.CIRCLE, TrafficSignalBulbStyle.LED, TrafficSignalBulbType.RIGHT,
            TrafficSignalBulbColor.GREEN, false)
            };
          })
          .horizontal(true)
          .sectionYPositions(0.0f, 0.0f, 0.0f)
          .sectionXPositions(12.0f, 0.0f, -12.0f)
          .build();

  public static final BlockControllableSignal HORIZONTAL_SOLID_SIGNAL =
      new BlockControllableSignal.Builder(
          "controllablehorizontalsolidsignal",
          SIGNAL_SIDE.THROUGH,
          true,
          () -> {
            return new TrafficSignalSectionInfo[] {
            new TrafficSignalSectionInfo(TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK,
            TrafficSignalVisorType.CIRCLE, TrafficSignalBulbStyle.LED, TrafficSignalBulbType.BALL,
            TrafficSignalBulbColor.RED, false),
            new TrafficSignalSectionInfo(TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK,
            TrafficSignalVisorType.CIRCLE, TrafficSignalBulbStyle.LED, TrafficSignalBulbType.BALL,
            TrafficSignalBulbColor.YELLOW, false),
            new TrafficSignalSectionInfo(TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK,
            TrafficSignalVisorType.CIRCLE, TrafficSignalBulbStyle.LED, TrafficSignalBulbType.BALL,
            TrafficSignalBulbColor.GREEN, false)
            };
          })
          .horizontal(true)
          .sectionYPositions(0.0f, 0.0f, 0.0f)
          .sectionXPositions(12.0f, 0.0f, -12.0f)
          .build();

  public static final BlockControllableSignal HORIZONTAL_U_TURN_SIGNAL =
      new BlockControllableSignal.Builder(
          "controllablehorizontaluturnsignal",
          SIGNAL_SIDE.LEFT,
          true,
          () -> {
            return new TrafficSignalSectionInfo[] {
            new TrafficSignalSectionInfo(TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK,
            TrafficSignalVisorType.CIRCLE, TrafficSignalBulbStyle.LED, TrafficSignalBulbType.UTURN,
            TrafficSignalBulbColor.RED, false),
            new TrafficSignalSectionInfo(TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK,
            TrafficSignalVisorType.CIRCLE, TrafficSignalBulbStyle.LED, TrafficSignalBulbType.UTURN,
            TrafficSignalBulbColor.YELLOW, false),
            new TrafficSignalSectionInfo(TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK,
            TrafficSignalVisorType.CIRCLE, TrafficSignalBulbStyle.LED, TrafficSignalBulbType.UTURN,
            TrafficSignalBulbColor.GREEN, false)
            };
          })
          .horizontal(true)
          .sectionYPositions(0.0f, 0.0f, 0.0f)
          .sectionXPositions(12.0f, 0.0f, -12.0f)
          .build();

  public static final BlockControllableSignal HORIZONTAL_UP_LEFT_SIGNAL =
      new BlockControllableSignal.Builder(
          "controllablehorizontalupleftsignal",
          SIGNAL_SIDE.LEFT,
          true,
          () -> {
            return new TrafficSignalSectionInfo[] {
            new TrafficSignalSectionInfo(TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK,
            TrafficSignalVisorType.CIRCLE, TrafficSignalBulbStyle.LED, TrafficSignalBulbType.UP_LEFT,
            TrafficSignalBulbColor.RED, false),
            new TrafficSignalSectionInfo(TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK,
            TrafficSignalVisorType.CIRCLE, TrafficSignalBulbStyle.LED, TrafficSignalBulbType.UP_LEFT,
            TrafficSignalBulbColor.YELLOW, false),
            new TrafficSignalSectionInfo(TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK,
            TrafficSignalVisorType.CIRCLE, TrafficSignalBulbStyle.LED, TrafficSignalBulbType.UP_LEFT,
            TrafficSignalBulbColor.GREEN, false)
            };
          })
          .horizontal(true)
          .sectionYPositions(0.0f, 0.0f, 0.0f)
          .sectionXPositions(12.0f, 0.0f, -12.0f)
          .build();

  public static final BlockControllableSignal SINGLE_SOLID_SIGNAL_GREEN =
      new BlockControllableSignal.Builder(
          "controllablesinglesolidsignalgreen",
          SIGNAL_SIDE.BEACON,
          true,
          () -> {
            TrafficSignalSectionInfo[] infos = new TrafficSignalSectionInfo[] {
            new TrafficSignalSectionInfo(TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK,
            TrafficSignalVisorType.CIRCLE, TrafficSignalBulbStyle.LED, TrafficSignalBulbType.BALL,
            TrafficSignalBulbColor.GREEN, true)
            };
            return infos;
          })
          .signalYOffset(2.0f)
          .lightAllOnRedYellow(true)
          .build();

  public static final BlockControllableSignal SINGLE_SOLID_SIGNAL_GREEN_GRAY =
      new BlockControllableSignal.Builder(
          "controllablesinglesolidsignalgreengray",
          SIGNAL_SIDE.BEACON,
          true,
          () -> {
            TrafficSignalSectionInfo[] infos = new TrafficSignalSectionInfo[] {
            new TrafficSignalSectionInfo(TrafficSignalBodyColor.BATTLESHIP_GRAY, TrafficSignalBodyColor.BATTLESHIP_GRAY, TrafficSignalBodyColor.BATTLESHIP_GRAY,
            TrafficSignalVisorType.CIRCLE, TrafficSignalBulbStyle.LED, TrafficSignalBulbType.BALL,
            TrafficSignalBulbColor.GREEN, true)
            };
            return infos;
          })
          .signalYOffset(2.0f)
          .lightAllOnRedYellow(true)
          .retiring("controllablesinglesolidsignalgreen")
          .build();

  public static final BlockControllableSignal SINGLE_SOLID_SIGNAL_GREEN_LEFT_ANGLE =
      new BlockControllableSignal.Builder(
          "controllablesinglesolidsignalgreenleftangle",
          SIGNAL_SIDE.BEACON,
          true,
          () -> {
            TrafficSignalSectionInfo[] infos = new TrafficSignalSectionInfo[] {
            new TrafficSignalSectionInfo(TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK,
            TrafficSignalVisorType.CIRCLE, TrafficSignalBulbStyle.LED, TrafficSignalBulbType.BALL,
            TrafficSignalBulbColor.GREEN, true)
            };
            return infos;
          })
          .signalYOffset(2.0f)
          .lightAllOnRedYellow(true)
          .retiring("controllablesinglesolidsignalgreen", TrafficSignalBodyTilt.LEFT_ANGLE)
          .build();

  public static final BlockControllableSignal SINGLE_SOLID_SIGNAL_GREEN_RIGHT_ANGLE =
      new BlockControllableSignal.Builder(
          "controllablesinglesolidsignalgreenrightangle",
          SIGNAL_SIDE.BEACON,
          true,
          () -> {
            TrafficSignalSectionInfo[] infos = new TrafficSignalSectionInfo[] {
            new TrafficSignalSectionInfo(TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK,
            TrafficSignalVisorType.CIRCLE, TrafficSignalBulbStyle.LED, TrafficSignalBulbType.BALL,
            TrafficSignalBulbColor.GREEN, true)
            };
            return infos;
          })
          .signalYOffset(2.0f)
          .lightAllOnRedYellow(true)
          .retiring("controllablesinglesolidsignalgreen", TrafficSignalBodyTilt.RIGHT_ANGLE)
          .build();

  public static final BlockControllableSignal SINGLE_SOLID_SIGNAL_RED =
      new BlockControllableSignal.Builder(
          "controllablesinglesolidsignalred",
          SIGNAL_SIDE.BEACON,
          true,
          () -> {
            TrafficSignalSectionInfo[] infos = new TrafficSignalSectionInfo[] {
            new TrafficSignalSectionInfo(TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK,
            TrafficSignalVisorType.CIRCLE, TrafficSignalBulbStyle.LED, TrafficSignalBulbType.BALL,
            TrafficSignalBulbColor.RED, true)
            };
            return infos;
          })
          .signalYOffset(2.0f)
          .lightAllOnRedYellow(true)
          .build();

  public static final BlockControllableSignal SINGLE_SOLID_SIGNAL_RED_GRAY =
      new BlockControllableSignal.Builder(
          "controllablesinglesolidsignalredgray",
          SIGNAL_SIDE.BEACON,
          true,
          () -> {
            TrafficSignalSectionInfo[] infos = new TrafficSignalSectionInfo[] {
            new TrafficSignalSectionInfo(TrafficSignalBodyColor.BATTLESHIP_GRAY, TrafficSignalBodyColor.BATTLESHIP_GRAY, TrafficSignalBodyColor.BATTLESHIP_GRAY,
            TrafficSignalVisorType.CIRCLE, TrafficSignalBulbStyle.LED, TrafficSignalBulbType.BALL,
            TrafficSignalBulbColor.RED, true)
            };
            return infos;
          })
          .signalYOffset(2.0f)
          .lightAllOnRedYellow(true)
          .retiring("controllablesinglesolidsignalred")
          .build();

  public static final BlockControllableSignal SINGLE_SOLID_SIGNAL_RED_LEFT_ANGLE =
      new BlockControllableSignal.Builder(
          "controllablesinglesolidsignalredleftangle",
          SIGNAL_SIDE.BEACON,
          true,
          () -> {
            TrafficSignalSectionInfo[] infos = new TrafficSignalSectionInfo[] {
            new TrafficSignalSectionInfo(TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK,
            TrafficSignalVisorType.CIRCLE, TrafficSignalBulbStyle.LED, TrafficSignalBulbType.BALL,
            TrafficSignalBulbColor.RED, true)
            };
            return infos;
          })
          .signalYOffset(2.0f)
          .lightAllOnRedYellow(true)
          .retiring("controllablesinglesolidsignalred", TrafficSignalBodyTilt.LEFT_ANGLE)
          .build();

  public static final BlockControllableSignal SINGLE_SOLID_SIGNAL_RED_RIGHT_ANGLE =
      new BlockControllableSignal.Builder(
          "controllablesinglesolidsignalredrightangle",
          SIGNAL_SIDE.BEACON,
          true,
          () -> {
            TrafficSignalSectionInfo[] infos = new TrafficSignalSectionInfo[] {
            new TrafficSignalSectionInfo(TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK,
            TrafficSignalVisorType.CIRCLE, TrafficSignalBulbStyle.LED, TrafficSignalBulbType.BALL,
            TrafficSignalBulbColor.RED, true)
            };
            return infos;
          })
          .signalYOffset(2.0f)
          .lightAllOnRedYellow(true)
          .retiring("controllablesinglesolidsignalred", TrafficSignalBodyTilt.RIGHT_ANGLE)
          .build();

  public static final BlockControllableSignal SINGLE_SOLID_SIGNAL_YELLOW =
      new BlockControllableSignal.Builder(
          "controllablesinglesolidsignalyellow",
          SIGNAL_SIDE.BEACON,
          true,
          () -> {
            TrafficSignalSectionInfo[] infos = new TrafficSignalSectionInfo[] {
            new TrafficSignalSectionInfo(TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK,
            TrafficSignalVisorType.CIRCLE, TrafficSignalBulbStyle.LED, TrafficSignalBulbType.BALL,
            TrafficSignalBulbColor.YELLOW, true)
            };
            return infos;
          })
          .signalYOffset(2.0f)
          .lightAllOnRedYellow(true)
          .build();

  public static final BlockControllableSignal SINGLE_SOLID_SIGNAL_YELLOW_ADVANCE_FLASH =
      new BlockControllableSignal.Builder(
          "controllablesinglesolidsignalyellowadvanceflash",
          SIGNAL_SIDE.BEACON,
          true,
          () -> {
            TrafficSignalSectionInfo[] infos = new TrafficSignalSectionInfo[] {
            new TrafficSignalSectionInfo(TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK,
            TrafficSignalVisorType.CIRCLE, TrafficSignalBulbStyle.LED, TrafficSignalBulbType.BALL,
            TrafficSignalBulbColor.GREEN, TrafficSignalBulbColor.YELLOW, false, true)
            };
            return infos;
          })
          .signalYOffset(2.0f)
          .build();

  public static final BlockControllableSignal SINGLE_SOLID_SIGNAL_YELLOW_ADVANCE_FLASH_GRAY_A =
      new BlockControllableSignal.Builder(
          "controllablesinglesolidsignalyellowadvanceflashgraya",
          SIGNAL_SIDE.BEACON,
          true,
          () -> {
            TrafficSignalSectionInfo[] infos = new TrafficSignalSectionInfo[] {
            new TrafficSignalSectionInfo(TrafficSignalBodyColor.BATTLESHIP_GRAY, TrafficSignalBodyColor.BATTLESHIP_GRAY, TrafficSignalBodyColor.BATTLESHIP_GRAY,
            TrafficSignalVisorType.CIRCLE, TrafficSignalBulbStyle.LED, TrafficSignalBulbType.BALL,
            TrafficSignalBulbColor.GREEN, TrafficSignalBulbColor.YELLOW, false, true)
            };
            return infos;
          })
          .signalYOffset(2.0f)
          .retiring("controllablesinglesolidsignalyellowadvanceflash")
          .build();

  public static final BlockControllableSignal SINGLE_SOLID_SIGNAL_YELLOW_ADVANCE_FLASH_GRAY_B =
      new BlockControllableSignal.Builder(
          "controllablesinglesolidsignalyellowadvanceflashgrayb",
          SIGNAL_SIDE.BEACON,
          true,
          () -> {
            TrafficSignalSectionInfo[] infos = new TrafficSignalSectionInfo[] {
            new TrafficSignalSectionInfo(TrafficSignalBodyColor.BATTLESHIP_GRAY, TrafficSignalBodyColor.BATTLESHIP_GRAY, TrafficSignalBodyColor.BATTLESHIP_GRAY,
            TrafficSignalVisorType.CIRCLE, TrafficSignalBulbStyle.LED, TrafficSignalBulbType.BALL,
            TrafficSignalBulbColor.GREEN, TrafficSignalBulbColor.YELLOW, false, true)
            };
            return infos;
          })
          .signalYOffset(2.0f)
          .retiring("controllablesinglesolidsignalyellowadvanceflash")
          .build();

  public static final BlockControllableSignal SINGLE_SOLID_SIGNAL_YELLOW_GRAY =
      new BlockControllableSignal.Builder(
          "controllablesinglesolidsignalyellowgray",
          SIGNAL_SIDE.BEACON,
          true,
          () -> {
            TrafficSignalSectionInfo[] infos = new TrafficSignalSectionInfo[] {
            new TrafficSignalSectionInfo(TrafficSignalBodyColor.BATTLESHIP_GRAY, TrafficSignalBodyColor.BATTLESHIP_GRAY, TrafficSignalBodyColor.BATTLESHIP_GRAY,
            TrafficSignalVisorType.CIRCLE, TrafficSignalBulbStyle.LED, TrafficSignalBulbType.BALL,
            TrafficSignalBulbColor.YELLOW, true)
            };
            return infos;
          })
          .signalYOffset(2.0f)
          .lightAllOnRedYellow(true)
          .retiring("controllablesinglesolidsignalyellow")
          .build();

  public static final BlockControllableSignal SINGLE_SOLID_SIGNAL_YELLOW_LEFT_ANGLE =
      new BlockControllableSignal.Builder(
          "controllablesinglesolidsignalyellowleftangle",
          SIGNAL_SIDE.BEACON,
          true,
          () -> {
            TrafficSignalSectionInfo[] infos = new TrafficSignalSectionInfo[] {
            new TrafficSignalSectionInfo(TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK,
            TrafficSignalVisorType.CIRCLE, TrafficSignalBulbStyle.LED, TrafficSignalBulbType.BALL,
            TrafficSignalBulbColor.YELLOW, true)
            };
            return infos;
          })
          .signalYOffset(2.0f)
          .lightAllOnRedYellow(true)
          .retiring("controllablesinglesolidsignalyellow", TrafficSignalBodyTilt.LEFT_ANGLE)
          .build();

  public static final BlockControllableSignal SINGLE_SOLID_SIGNAL_YELLOW_RIGHT_ANGLE =
      new BlockControllableSignal.Builder(
          "controllablesinglesolidsignalyellowrightangle",
          SIGNAL_SIDE.BEACON,
          true,
          () -> {
            TrafficSignalSectionInfo[] infos = new TrafficSignalSectionInfo[] {
            new TrafficSignalSectionInfo(TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK,
            TrafficSignalVisorType.CIRCLE, TrafficSignalBulbStyle.LED, TrafficSignalBulbType.BALL,
            TrafficSignalBulbColor.YELLOW, true)
            };
            return infos;
          })
          .signalYOffset(2.0f)
          .lightAllOnRedYellow(true)
          .retiring("controllablesinglesolidsignalyellow", TrafficSignalBodyTilt.RIGHT_ANGLE)
          .build();

  public static final BlockControllableSignal VERTICAL_AHEAD_SIGNAL =
      new BlockControllableSignal.Builder(
          "controllableverticalaheadsignal",
          SIGNAL_SIDE.THROUGH,
          true,
          () -> {
            return new TrafficSignalSectionInfo[] {
            new TrafficSignalSectionInfo(TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK,
            TrafficSignalVisorType.CIRCLE, TrafficSignalBulbStyle.LED, TrafficSignalBulbType.BALL,
            TrafficSignalBulbColor.RED, false),
            new TrafficSignalSectionInfo(TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK,
            TrafficSignalVisorType.CIRCLE, TrafficSignalBulbStyle.LED, TrafficSignalBulbType.BALL,
            TrafficSignalBulbColor.YELLOW, false),
            new TrafficSignalSectionInfo(TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK,
            TrafficSignalVisorType.CIRCLE, TrafficSignalBulbStyle.LED, TrafficSignalBulbType.UP,
            TrafficSignalBulbColor.GREEN, false)
            };
          })
          .build();

  public static final BlockControllableSignal VERTICAL_AHEAD_SIGNAL8812_INCH =
      new BlockControllableSignal.Builder(
          "controllableverticalaheadsignal8812inch",
          SIGNAL_SIDE.THROUGH,
          true,
          () -> {
            return new TrafficSignalSectionInfo[] {
            new TrafficSignalSectionInfo(TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK,
            TrafficSignalVisorType.CIRCLE, TrafficSignalBulbStyle.LED, TrafficSignalBulbType.BALL,
            TrafficSignalBulbColor.RED, false),
            new TrafficSignalSectionInfo(TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK,
            TrafficSignalVisorType.CIRCLE, TrafficSignalBulbStyle.LED, TrafficSignalBulbType.BALL,
            TrafficSignalBulbColor.YELLOW, false),
            new TrafficSignalSectionInfo(TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK,
            TrafficSignalVisorType.CIRCLE, TrafficSignalBulbStyle.LED, TrafficSignalBulbType.UP,
            TrafficSignalBulbColor.GREEN, false)
            };
          })
          .sectionYPositions(6.0f, -2.0f, -12.0f)
          .sectionSizes(8, 8, 12)
          .build();

  public static final BlockControllableSignal VERTICAL_AHEAD_SIGNAL_GRAY =
      new BlockControllableSignal.Builder(
          "controllableverticalaheadsignalgray",
          SIGNAL_SIDE.THROUGH,
          true,
          () -> {
            return new TrafficSignalSectionInfo[] {
            new TrafficSignalSectionInfo(TrafficSignalBodyColor.BATTLESHIP_GRAY, TrafficSignalBodyColor.BATTLESHIP_GRAY, TrafficSignalBodyColor.BATTLESHIP_GRAY,
            TrafficSignalVisorType.CIRCLE, TrafficSignalBulbStyle.LED, TrafficSignalBulbType.BALL,
            TrafficSignalBulbColor.RED, false),
            new TrafficSignalSectionInfo(TrafficSignalBodyColor.BATTLESHIP_GRAY, TrafficSignalBodyColor.BATTLESHIP_GRAY, TrafficSignalBodyColor.BATTLESHIP_GRAY,
            TrafficSignalVisorType.CIRCLE, TrafficSignalBulbStyle.LED, TrafficSignalBulbType.BALL,
            TrafficSignalBulbColor.YELLOW, false),
            new TrafficSignalSectionInfo(TrafficSignalBodyColor.BATTLESHIP_GRAY, TrafficSignalBodyColor.BATTLESHIP_GRAY, TrafficSignalBodyColor.BATTLESHIP_GRAY,
            TrafficSignalVisorType.CIRCLE, TrafficSignalBulbStyle.LED, TrafficSignalBulbType.UP,
            TrafficSignalBulbColor.GREEN, false)
            };
          })
          .retiring("controllableverticalaheadsignal")
          .build();

  public static final BlockControllableSignal VERTICAL_ANGLE2_AHEAD_SIGNAL =
      new BlockControllableSignal.Builder(
          "controllableverticalangle2aheadsignal",
          SIGNAL_SIDE.THROUGH,
          true,
          () -> {
            return new TrafficSignalSectionInfo[] {
            new TrafficSignalSectionInfo(TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK,
            TrafficSignalVisorType.CIRCLE, TrafficSignalBulbStyle.LED, TrafficSignalBulbType.BALL,
            TrafficSignalBulbColor.RED, false),
            new TrafficSignalSectionInfo(TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK,
            TrafficSignalVisorType.CIRCLE, TrafficSignalBulbStyle.LED, TrafficSignalBulbType.BALL,
            TrafficSignalBulbColor.YELLOW, false),
            new TrafficSignalSectionInfo(TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK,
            TrafficSignalVisorType.CIRCLE, TrafficSignalBulbStyle.LED, TrafficSignalBulbType.UP,
            TrafficSignalBulbColor.GREEN, false)
            };
          })
          .retiring("controllableverticalaheadsignal", TrafficSignalBodyTilt.RIGHT_ANGLE)
          .build();

  public static final BlockControllableSignal VERTICAL_ANGLE2_BIKE_SIGNAL =
      new BlockControllableSignal.Builder(
          "controllableverticalangle2bikesignal",
          SIGNAL_SIDE.PROTECTED,
          true,
          () -> {
            return new TrafficSignalSectionInfo[] {
            new TrafficSignalSectionInfo(TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK,
            TrafficSignalVisorType.CIRCLE, TrafficSignalBulbStyle.LED, TrafficSignalBulbType.BIKE,
            TrafficSignalBulbColor.RED, false),
            new TrafficSignalSectionInfo(TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK,
            TrafficSignalVisorType.CIRCLE, TrafficSignalBulbStyle.LED, TrafficSignalBulbType.BIKE,
            TrafficSignalBulbColor.YELLOW, false),
            new TrafficSignalSectionInfo(TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK,
            TrafficSignalVisorType.CIRCLE, TrafficSignalBulbStyle.LED, TrafficSignalBulbType.BIKE,
            TrafficSignalBulbColor.GREEN, false)
            };
          })
          .retiring("controllableverticalbikesignal", TrafficSignalBodyTilt.RIGHT_ANGLE)
          .build();

  public static final BlockControllableSignal VERTICAL_ANGLE2_LEFT_SIGNAL =
      new BlockControllableSignal.Builder(
          "controllableverticalangle2leftsignal",
          SIGNAL_SIDE.LEFT,
          true,
          () -> {
            return new TrafficSignalSectionInfo[] {
            new TrafficSignalSectionInfo(TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK,
            TrafficSignalVisorType.CIRCLE, TrafficSignalBulbStyle.LED, TrafficSignalBulbType.LEFT,
            TrafficSignalBulbColor.RED, false),
            new TrafficSignalSectionInfo(TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK,
            TrafficSignalVisorType.CIRCLE, TrafficSignalBulbStyle.LED, TrafficSignalBulbType.LEFT,
            TrafficSignalBulbColor.YELLOW, false),
            new TrafficSignalSectionInfo(TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK,
            TrafficSignalVisorType.CIRCLE, TrafficSignalBulbStyle.LED, TrafficSignalBulbType.LEFT,
            TrafficSignalBulbColor.GREEN, false)
            };
          })
          .retiring("controllableverticalleftsignal", TrafficSignalBodyTilt.RIGHT_ANGLE)
          .build();

  public static final BlockControllableSignal VERTICAL_ANGLE2_RAIL_SIGNAL =
      new BlockControllableSignal.Builder(
          "controllableverticalangle2railsignal",
          SIGNAL_SIDE.PROTECTED,
          true,
          () -> {
            return new TrafficSignalSectionInfo[] {
            new TrafficSignalSectionInfo(TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK,
            TrafficSignalVisorType.CIRCLE, TrafficSignalBulbStyle.LED, TrafficSignalBulbType.TRANSIT,
            TrafficSignalBulbColor.RED, false),
            new TrafficSignalSectionInfo(TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK,
            TrafficSignalVisorType.CIRCLE, TrafficSignalBulbStyle.LED, TrafficSignalBulbType.TRANSIT,
            TrafficSignalBulbColor.YELLOW, false),
            new TrafficSignalSectionInfo(TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK,
            TrafficSignalVisorType.CIRCLE, TrafficSignalBulbStyle.LED, TrafficSignalBulbType.TRANSIT,
            TrafficSignalBulbColor.GREEN, false)
            };
          })
          .retiring("controllableverticalrailsignal", TrafficSignalBodyTilt.RIGHT_ANGLE)
          .build();

  public static final BlockControllableSignal VERTICAL_ANGLE2_RIGHT2_SIGNAL =
      new BlockControllableSignal.Builder(
          "controllableverticalangle2right2signal",
          SIGNAL_SIDE.RIGHT,
          true,
          () -> {
            return new TrafficSignalSectionInfo[] {
            new TrafficSignalSectionInfo(TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK,
            TrafficSignalVisorType.CIRCLE, TrafficSignalBulbStyle.LED, TrafficSignalBulbType.RIGHT,
            TrafficSignalBulbColor.RED, false),
            new TrafficSignalSectionInfo(TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK,
            TrafficSignalVisorType.CIRCLE, TrafficSignalBulbStyle.LED, TrafficSignalBulbType.RIGHT,
            TrafficSignalBulbColor.YELLOW, false),
            new TrafficSignalSectionInfo(TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK,
            TrafficSignalVisorType.CIRCLE, TrafficSignalBulbStyle.LED, TrafficSignalBulbType.RIGHT,
            TrafficSignalBulbColor.GREEN, false)
            };
          })
          .retiring("controllableverticalright2signal", TrafficSignalBodyTilt.RIGHT_ANGLE)
          .build();

  public static final BlockControllableSignal VERTICAL_ANGLE2_RIGHT_SIGNAL =
      new BlockControllableSignal.Builder(
          "controllableverticalangle2rightsignal",
          SIGNAL_SIDE.RIGHT,
          true,
          () -> {
            return new TrafficSignalSectionInfo[] {
            new TrafficSignalSectionInfo(TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK,
            TrafficSignalVisorType.CIRCLE, TrafficSignalBulbStyle.LED, TrafficSignalBulbType.RIGHT,
            TrafficSignalBulbColor.RED, false),
            new TrafficSignalSectionInfo(TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK,
            TrafficSignalVisorType.CIRCLE, TrafficSignalBulbStyle.LED, TrafficSignalBulbType.RIGHT,
            TrafficSignalBulbColor.YELLOW, false),
            new TrafficSignalSectionInfo(TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK,
            TrafficSignalVisorType.CIRCLE, TrafficSignalBulbStyle.LED, TrafficSignalBulbType.RIGHT,
            TrafficSignalBulbColor.GREEN, false)
            };
          })
          .retiring("controllableverticalrightsignal", TrafficSignalBodyTilt.RIGHT_ANGLE)
          .build();

  public static final BlockControllableSignal VERTICAL_ANGLE2_SOLID_SIGNAL =
      new BlockControllableSignal.Builder(
          "controllableverticalangle2solidsignal",
          SIGNAL_SIDE.THROUGH,
          true,
          () -> {
            return new TrafficSignalSectionInfo[] {
            new TrafficSignalSectionInfo(TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK,
            TrafficSignalVisorType.CIRCLE, TrafficSignalBulbStyle.LED, TrafficSignalBulbType.BALL,
            TrafficSignalBulbColor.RED, false),
            new TrafficSignalSectionInfo(TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK,
            TrafficSignalVisorType.CIRCLE, TrafficSignalBulbStyle.LED, TrafficSignalBulbType.BALL,
            TrafficSignalBulbColor.YELLOW, false),
            new TrafficSignalSectionInfo(TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK,
            TrafficSignalVisorType.CIRCLE, TrafficSignalBulbStyle.LED, TrafficSignalBulbType.BALL,
            TrafficSignalBulbColor.GREEN, false)
            };
          })
          .retiring("controllableverticalsolidsignal", TrafficSignalBodyTilt.RIGHT_ANGLE)
          .build();

  public static final BlockControllableSignal VERTICAL_ANGLE2_U_TURN_SIGNAL =
      new BlockControllableSignal.Builder(
          "controllableverticalangle2uturnsignal",
          SIGNAL_SIDE.LEFT,
          true,
          () -> {
            return new TrafficSignalSectionInfo[] {
            new TrafficSignalSectionInfo(TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK,
            TrafficSignalVisorType.CIRCLE, TrafficSignalBulbStyle.LED, TrafficSignalBulbType.UTURN,
            TrafficSignalBulbColor.RED, false),
            new TrafficSignalSectionInfo(TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK,
            TrafficSignalVisorType.CIRCLE, TrafficSignalBulbStyle.LED, TrafficSignalBulbType.UTURN,
            TrafficSignalBulbColor.YELLOW, false),
            new TrafficSignalSectionInfo(TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK,
            TrafficSignalVisorType.CIRCLE, TrafficSignalBulbStyle.LED, TrafficSignalBulbType.UTURN,
            TrafficSignalBulbColor.GREEN, false)
            };
          })
          .retiring("controllableverticaluturnsignal", TrafficSignalBodyTilt.RIGHT_ANGLE)
          .build();

  public static final BlockControllableSignal VERTICAL_ANGLE2_UP_LEFT_SIGNAL =
      new BlockControllableSignal.Builder(
          "controllableverticalangle2upleftsignal",
          SIGNAL_SIDE.LEFT,
          true,
          () -> {
            return new TrafficSignalSectionInfo[] {
            new TrafficSignalSectionInfo(TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK,
            TrafficSignalVisorType.CIRCLE, TrafficSignalBulbStyle.LED, TrafficSignalBulbType.UP_LEFT,
            TrafficSignalBulbColor.RED, false),
            new TrafficSignalSectionInfo(TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK,
            TrafficSignalVisorType.CIRCLE, TrafficSignalBulbStyle.LED, TrafficSignalBulbType.UP_LEFT,
            TrafficSignalBulbColor.YELLOW, false),
            new TrafficSignalSectionInfo(TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK,
            TrafficSignalVisorType.CIRCLE, TrafficSignalBulbStyle.LED, TrafficSignalBulbType.UP_LEFT,
            TrafficSignalBulbColor.GREEN, false)
            };
          })
          .retiring("controllableverticalupleftsignal", TrafficSignalBodyTilt.RIGHT_ANGLE)
          .build();

  public static final BlockControllableSignal VERTICAL_ANGLE_AHEAD_SIGNAL =
      new BlockControllableSignal.Builder(
          "controllableverticalangleaheadsignal",
          SIGNAL_SIDE.THROUGH,
          true,
          () -> {
            return new TrafficSignalSectionInfo[] {
            new TrafficSignalSectionInfo(TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK,
            TrafficSignalVisorType.CIRCLE, TrafficSignalBulbStyle.LED, TrafficSignalBulbType.BALL,
            TrafficSignalBulbColor.RED, false),
            new TrafficSignalSectionInfo(TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK,
            TrafficSignalVisorType.CIRCLE, TrafficSignalBulbStyle.LED, TrafficSignalBulbType.BALL,
            TrafficSignalBulbColor.YELLOW, false),
            new TrafficSignalSectionInfo(TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK,
            TrafficSignalVisorType.CIRCLE, TrafficSignalBulbStyle.LED, TrafficSignalBulbType.UP,
            TrafficSignalBulbColor.GREEN, false)
            };
          })
          .retiring("controllableverticalaheadsignal", TrafficSignalBodyTilt.LEFT_ANGLE)
          .build();

  public static final BlockControllableSignal VERTICAL_ANGLE_BIKE_SIGNAL =
      new BlockControllableSignal.Builder(
          "controllableverticalanglebikesignal",
          SIGNAL_SIDE.PROTECTED,
          true,
          () -> {
            return new TrafficSignalSectionInfo[] {
            new TrafficSignalSectionInfo(TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK,
            TrafficSignalVisorType.CIRCLE, TrafficSignalBulbStyle.LED, TrafficSignalBulbType.BIKE,
            TrafficSignalBulbColor.RED, false),
            new TrafficSignalSectionInfo(TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK,
            TrafficSignalVisorType.CIRCLE, TrafficSignalBulbStyle.LED, TrafficSignalBulbType.BIKE,
            TrafficSignalBulbColor.YELLOW, false),
            new TrafficSignalSectionInfo(TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK,
            TrafficSignalVisorType.CIRCLE, TrafficSignalBulbStyle.LED, TrafficSignalBulbType.BIKE,
            TrafficSignalBulbColor.GREEN, false)
            };
          })
          .retiring("controllableverticalbikesignal", TrafficSignalBodyTilt.LEFT_ANGLE)
          .build();

  public static final BlockControllableSignal VERTICAL_ANGLE_LEFT_SIGNAL =
      new BlockControllableSignal.Builder(
          "controllableverticalangleleftsignal",
          SIGNAL_SIDE.LEFT,
          true,
          () -> {
            return new TrafficSignalSectionInfo[] {
            new TrafficSignalSectionInfo(TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK,
            TrafficSignalVisorType.CIRCLE, TrafficSignalBulbStyle.LED, TrafficSignalBulbType.LEFT,
            TrafficSignalBulbColor.RED, false),
            new TrafficSignalSectionInfo(TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK,
            TrafficSignalVisorType.CIRCLE, TrafficSignalBulbStyle.LED, TrafficSignalBulbType.LEFT,
            TrafficSignalBulbColor.YELLOW, false),
            new TrafficSignalSectionInfo(TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK,
            TrafficSignalVisorType.CIRCLE, TrafficSignalBulbStyle.LED, TrafficSignalBulbType.LEFT,
            TrafficSignalBulbColor.GREEN, false)
            };
          })
          .retiring("controllableverticalleftsignal", TrafficSignalBodyTilt.LEFT_ANGLE)
          .build();

  public static final BlockControllableSignal VERTICAL_ANGLE_RAIL_SIGNAL =
      new BlockControllableSignal.Builder(
          "controllableverticalanglerailsignal",
          SIGNAL_SIDE.PROTECTED,
          true,
          () -> {
            return new TrafficSignalSectionInfo[] {
            new TrafficSignalSectionInfo(TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK,
            TrafficSignalVisorType.CIRCLE, TrafficSignalBulbStyle.LED, TrafficSignalBulbType.TRANSIT,
            TrafficSignalBulbColor.RED, false),
            new TrafficSignalSectionInfo(TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK,
            TrafficSignalVisorType.CIRCLE, TrafficSignalBulbStyle.LED, TrafficSignalBulbType.TRANSIT,
            TrafficSignalBulbColor.YELLOW, false),
            new TrafficSignalSectionInfo(TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK,
            TrafficSignalVisorType.CIRCLE, TrafficSignalBulbStyle.LED, TrafficSignalBulbType.TRANSIT,
            TrafficSignalBulbColor.GREEN, false)
            };
          })
          .retiring("controllableverticalrailsignal", TrafficSignalBodyTilt.LEFT_ANGLE)
          .build();

  public static final BlockControllableSignal VERTICAL_ANGLE_RIGHT2_SIGNAL =
      new BlockControllableSignal.Builder(
          "controllableverticalangleright2signal",
          SIGNAL_SIDE.RIGHT,
          true,
          () -> {
            return new TrafficSignalSectionInfo[] {
            new TrafficSignalSectionInfo(TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK,
            TrafficSignalVisorType.CIRCLE, TrafficSignalBulbStyle.LED, TrafficSignalBulbType.RIGHT,
            TrafficSignalBulbColor.RED, false),
            new TrafficSignalSectionInfo(TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK,
            TrafficSignalVisorType.CIRCLE, TrafficSignalBulbStyle.LED, TrafficSignalBulbType.RIGHT,
            TrafficSignalBulbColor.YELLOW, false),
            new TrafficSignalSectionInfo(TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK,
            TrafficSignalVisorType.CIRCLE, TrafficSignalBulbStyle.LED, TrafficSignalBulbType.RIGHT,
            TrafficSignalBulbColor.GREEN, false)
            };
          })
          .retiring("controllableverticalright2signal", TrafficSignalBodyTilt.LEFT_ANGLE)
          .build();

  public static final BlockControllableSignal VERTICAL_ANGLE_RIGHT_SIGNAL =
      new BlockControllableSignal.Builder(
          "controllableverticalanglerightsignal",
          SIGNAL_SIDE.RIGHT,
          true,
          () -> {
            return new TrafficSignalSectionInfo[] {
            new TrafficSignalSectionInfo(TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK,
            TrafficSignalVisorType.CIRCLE, TrafficSignalBulbStyle.LED, TrafficSignalBulbType.RIGHT,
            TrafficSignalBulbColor.RED, false),
            new TrafficSignalSectionInfo(TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK,
            TrafficSignalVisorType.CIRCLE, TrafficSignalBulbStyle.LED, TrafficSignalBulbType.RIGHT,
            TrafficSignalBulbColor.YELLOW, false),
            new TrafficSignalSectionInfo(TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK,
            TrafficSignalVisorType.CIRCLE, TrafficSignalBulbStyle.LED, TrafficSignalBulbType.RIGHT,
            TrafficSignalBulbColor.GREEN, false)
            };
          })
          .retiring("controllableverticalrightsignal", TrafficSignalBodyTilt.LEFT_ANGLE)
          .build();

  public static final BlockControllableSignal VERTICAL_ANGLE_SOLID_SIGNAL =
      new BlockControllableSignal.Builder(
          "controllableverticalanglesolidsignal",
          SIGNAL_SIDE.THROUGH,
          true,
          () -> {
            return new TrafficSignalSectionInfo[] {
            new TrafficSignalSectionInfo(TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK,
            TrafficSignalVisorType.CIRCLE, TrafficSignalBulbStyle.LED, TrafficSignalBulbType.BALL,
            TrafficSignalBulbColor.RED, false),
            new TrafficSignalSectionInfo(TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK,
            TrafficSignalVisorType.CIRCLE, TrafficSignalBulbStyle.LED, TrafficSignalBulbType.BALL,
            TrafficSignalBulbColor.YELLOW, false),
            new TrafficSignalSectionInfo(TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK,
            TrafficSignalVisorType.CIRCLE, TrafficSignalBulbStyle.LED, TrafficSignalBulbType.BALL,
            TrafficSignalBulbColor.GREEN, false)
            };
          })
          .retiring("controllableverticalsolidsignal", TrafficSignalBodyTilt.LEFT_ANGLE)
          .build();

  public static final BlockControllableSignal VERTICAL_ANGLE_U_TURN_SIGNAL =
      new BlockControllableSignal.Builder(
          "controllableverticalangleuturnsignal",
          SIGNAL_SIDE.LEFT,
          true,
          () -> {
            return new TrafficSignalSectionInfo[] {
            new TrafficSignalSectionInfo(TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK,
            TrafficSignalVisorType.CIRCLE, TrafficSignalBulbStyle.LED, TrafficSignalBulbType.UTURN,
            TrafficSignalBulbColor.RED, false),
            new TrafficSignalSectionInfo(TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK,
            TrafficSignalVisorType.CIRCLE, TrafficSignalBulbStyle.LED, TrafficSignalBulbType.UTURN,
            TrafficSignalBulbColor.YELLOW, false),
            new TrafficSignalSectionInfo(TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK,
            TrafficSignalVisorType.CIRCLE, TrafficSignalBulbStyle.LED, TrafficSignalBulbType.UTURN,
            TrafficSignalBulbColor.GREEN, false)
            };
          })
          .retiring("controllableverticaluturnsignal", TrafficSignalBodyTilt.LEFT_ANGLE)
          .build();

  public static final BlockControllableSignal VERTICAL_ANGLE_UP_LEFT_SIGNAL =
      new BlockControllableSignal.Builder(
          "controllableverticalangleupleftsignal",
          SIGNAL_SIDE.LEFT,
          true,
          () -> {
            return new TrafficSignalSectionInfo[] {
            new TrafficSignalSectionInfo(TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK,
            TrafficSignalVisorType.CIRCLE, TrafficSignalBulbStyle.LED, TrafficSignalBulbType.UP_LEFT,
            TrafficSignalBulbColor.RED, false),
            new TrafficSignalSectionInfo(TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK,
            TrafficSignalVisorType.CIRCLE, TrafficSignalBulbStyle.LED, TrafficSignalBulbType.UP_LEFT,
            TrafficSignalBulbColor.YELLOW, false),
            new TrafficSignalSectionInfo(TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK,
            TrafficSignalVisorType.CIRCLE, TrafficSignalBulbStyle.LED, TrafficSignalBulbType.UP_LEFT,
            TrafficSignalBulbColor.GREEN, false)
            };
          })
          .retiring("controllableverticalupleftsignal", TrafficSignalBodyTilt.LEFT_ANGLE)
          .build();

  public static final BlockControllableSignal VERTICAL_BIKE_SIGNAL =
      new BlockControllableSignal.Builder(
          "controllableverticalbikesignal",
          SIGNAL_SIDE.PROTECTED,
          true,
          () -> {
            TrafficSignalSectionInfo[] infos = new TrafficSignalSectionInfo[] {
            new TrafficSignalSectionInfo(TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK,
            TrafficSignalVisorType.CIRCLE, TrafficSignalBulbStyle.LED, TrafficSignalBulbType.BIKE,
            TrafficSignalBulbColor.RED, false),
            new TrafficSignalSectionInfo(TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK,
            TrafficSignalVisorType.CIRCLE, TrafficSignalBulbStyle.LED, TrafficSignalBulbType.BIKE,
            TrafficSignalBulbColor.YELLOW, false),
            new TrafficSignalSectionInfo(TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK,
            TrafficSignalVisorType.CIRCLE, TrafficSignalBulbStyle.LED, TrafficSignalBulbType.BIKE,
            TrafficSignalBulbColor.GREEN, false)
            };
            return infos;
          })
          .build();

  public static final BlockControllableSignal VERTICAL_BIKE_SIGNAL4_INCH =
      new BlockControllableSignal.Builder(
          "controllableverticalbikesignal4inch",
          SIGNAL_SIDE.PROTECTED,
          true,
          () -> {
            return new TrafficSignalSectionInfo[] {
            new TrafficSignalSectionInfo(TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK,
            TrafficSignalVisorType.CIRCLE, TrafficSignalBulbStyle.LED, TrafficSignalBulbType.BIKE,
            TrafficSignalBulbColor.RED, false),
            new TrafficSignalSectionInfo(TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK,
            TrafficSignalVisorType.CIRCLE, TrafficSignalBulbStyle.LED, TrafficSignalBulbType.BIKE,
            TrafficSignalBulbColor.YELLOW, false),
            new TrafficSignalSectionInfo(TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK,
            TrafficSignalVisorType.CIRCLE, TrafficSignalBulbStyle.LED, TrafficSignalBulbType.BIKE,
            TrafficSignalBulbColor.GREEN, false)
            };
          })
          .sectionYPositions(-1.0f, -5.0f, -9.0f)
          .sectionSizes(4, 4, 4)
          .build();

  public static final BlockControllableSignal VERTICAL_BIKE_SIGNAL8_INCH =
      new BlockControllableSignal.Builder(
          "controllableverticalbikesignal8inch",
          SIGNAL_SIDE.PROTECTED,
          true,
          () -> {
            return new TrafficSignalSectionInfo[] {
            new TrafficSignalSectionInfo(TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK,
            TrafficSignalVisorType.CIRCLE, TrafficSignalBulbStyle.LED, TrafficSignalBulbType.BIKE,
            TrafficSignalBulbColor.RED, false),
            new TrafficSignalSectionInfo(TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK,
            TrafficSignalVisorType.CIRCLE, TrafficSignalBulbStyle.LED, TrafficSignalBulbType.BIKE,
            TrafficSignalBulbColor.YELLOW, false),
            new TrafficSignalSectionInfo(TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK,
            TrafficSignalVisorType.CIRCLE, TrafficSignalBulbStyle.LED, TrafficSignalBulbType.BIKE,
            TrafficSignalBulbColor.GREEN, false)
            };
          })
          .sectionYPositions(2.0f, -6.0f, -14.0f)
          .sectionSizes(8, 8, 8)
          .build();

  public static final BlockControllableSignal VERTICAL_BIKE_SIGNAL8_INCH_BLACK =
      new BlockControllableSignal.Builder(
          "controllableverticalbikesignal8inchblack",
          SIGNAL_SIDE.PROTECTED,
          true,
          () -> {
            return new TrafficSignalSectionInfo[] {
            new TrafficSignalSectionInfo(TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK,
            TrafficSignalVisorType.CIRCLE, TrafficSignalBulbStyle.LED, TrafficSignalBulbType.BIKE,
            TrafficSignalBulbColor.RED, false),
            new TrafficSignalSectionInfo(TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK,
            TrafficSignalVisorType.CIRCLE, TrafficSignalBulbStyle.LED, TrafficSignalBulbType.BIKE,
            TrafficSignalBulbColor.YELLOW, false),
            new TrafficSignalSectionInfo(TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK,
            TrafficSignalVisorType.CIRCLE, TrafficSignalBulbStyle.LED, TrafficSignalBulbType.BIKE,
            TrafficSignalBulbColor.GREEN, false)
            };
          })
          .sectionYPositions(2.0f, -6.0f, -14.0f)
          .sectionSizes(8, 8, 8)
          .build();

  public static final BlockControllableSignal VERTICAL_BIKE_SIGNAL_GRAY =
      new BlockControllableSignal.Builder(
          "controllableverticalbikesignalgray",
          SIGNAL_SIDE.PROTECTED,
          true,
          () -> {
            TrafficSignalSectionInfo[] infos = new TrafficSignalSectionInfo[] {
            new TrafficSignalSectionInfo(TrafficSignalBodyColor.BATTLESHIP_GRAY, TrafficSignalBodyColor.BATTLESHIP_GRAY, TrafficSignalBodyColor.BATTLESHIP_GRAY,
            TrafficSignalVisorType.CIRCLE, TrafficSignalBulbStyle.LED, TrafficSignalBulbType.BIKE,
            TrafficSignalBulbColor.RED, false),
            new TrafficSignalSectionInfo(TrafficSignalBodyColor.BATTLESHIP_GRAY, TrafficSignalBodyColor.BATTLESHIP_GRAY, TrafficSignalBodyColor.BATTLESHIP_GRAY,
            TrafficSignalVisorType.CIRCLE, TrafficSignalBulbStyle.LED, TrafficSignalBulbType.BIKE,
            TrafficSignalBulbColor.YELLOW, false),
            new TrafficSignalSectionInfo(TrafficSignalBodyColor.BATTLESHIP_GRAY, TrafficSignalBodyColor.BATTLESHIP_GRAY, TrafficSignalBodyColor.BATTLESHIP_GRAY,
            TrafficSignalVisorType.CIRCLE, TrafficSignalBulbStyle.LED, TrafficSignalBulbType.BIKE,
            TrafficSignalBulbColor.GREEN, false)
            };
            return infos;
          })
          .retiring("controllableverticalbikesignal")
          .build();

  public static final BlockControllableSignal VERTICAL_HYBRID_LEFT_ADD_ON_SIGNAL =
      new BlockControllableSignal.Builder(
          "controllableverticalhybridleftaddonsignal",
          SIGNAL_SIDE.LEFT,
          true,
          () -> {
            return new TrafficSignalSectionInfo[] {
            new TrafficSignalSectionInfo(TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK,
            TrafficSignalVisorType.CIRCLE, TrafficSignalBulbStyle.LED_DOTTED, TrafficSignalBulbType.LEFT,
            TrafficSignalBulbColor.GREEN, false)
            };
          })
          .signalYOffset(-7.9f)
          .enforcedBulbStyle(TrafficSignalBulbStyle.LED_DOTTED)
          .build();

  public static final BlockControllableSignal VERTICAL_HYBRID_LEFT_ADD_ON_SIGNAL_GRAY =
      new BlockControllableSignal.Builder(
          "controllableverticalhybridleftaddonsignalgray",
          SIGNAL_SIDE.LEFT,
          true,
          () -> {
            return new TrafficSignalSectionInfo[] {
            new TrafficSignalSectionInfo(TrafficSignalBodyColor.BATTLESHIP_GRAY, TrafficSignalBodyColor.BATTLESHIP_GRAY, TrafficSignalBodyColor.BATTLESHIP_GRAY,
            TrafficSignalVisorType.CIRCLE, TrafficSignalBulbStyle.LED_DOTTED, TrafficSignalBulbType.LEFT,
            TrafficSignalBulbColor.GREEN, false)
            };
          })
          .signalYOffset(-7.9f)
          .enforcedBulbStyle(TrafficSignalBulbStyle.LED_DOTTED)
          .retiring("controllableverticalhybridleftaddonsignal")
          .build();

  public static final BlockControllableSignal VERTICAL_HYBRID_LEFT_SIGNAL =
      new BlockControllableSignal.Builder(
          "controllableverticalhybridleftsignal",
          SIGNAL_SIDE.FLASHING_LEFT,
          true,
          () -> {
            TrafficSignalSectionInfo[] infos = new TrafficSignalSectionInfo[] {
            new TrafficSignalSectionInfo(TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK,
            TrafficSignalVisorType.CIRCLE, TrafficSignalBulbStyle.LED, TrafficSignalBulbType.LEFT,
            TrafficSignalBulbColor.RED, false),
            new TrafficSignalSectionInfo(TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK,
            TrafficSignalVisorType.CIRCLE, TrafficSignalBulbStyle.LED, TrafficSignalBulbType.LEFT,
            TrafficSignalBulbColor.YELLOW, false),
            new TrafficSignalSectionInfo(TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK,
            TrafficSignalVisorType.CIRCLE, TrafficSignalBulbStyle.LED, TrafficSignalBulbType.LEFT,
            TrafficSignalBulbColor.GREEN, TrafficSignalBulbColor.YELLOW, false, true)
            };
            return infos;
          })
          .build();

  public static final BlockControllableSignal VERTICAL_HYBRID_LEFT_SIGNAL_GRAY =
      new BlockControllableSignal.Builder(
          "controllableverticalhybridleftsignalgray",
          SIGNAL_SIDE.FLASHING_LEFT,
          true,
          () -> {
            TrafficSignalSectionInfo[] infos = new TrafficSignalSectionInfo[] {
            new TrafficSignalSectionInfo(TrafficSignalBodyColor.BATTLESHIP_GRAY, TrafficSignalBodyColor.BATTLESHIP_GRAY, TrafficSignalBodyColor.BATTLESHIP_GRAY,
            TrafficSignalVisorType.CIRCLE, TrafficSignalBulbStyle.LED, TrafficSignalBulbType.LEFT,
            TrafficSignalBulbColor.RED, false),
            new TrafficSignalSectionInfo(TrafficSignalBodyColor.BATTLESHIP_GRAY, TrafficSignalBodyColor.BATTLESHIP_GRAY, TrafficSignalBodyColor.BATTLESHIP_GRAY,
            TrafficSignalVisorType.CIRCLE, TrafficSignalBulbStyle.LED, TrafficSignalBulbType.LEFT,
            TrafficSignalBulbColor.YELLOW, false),
            new TrafficSignalSectionInfo(TrafficSignalBodyColor.BATTLESHIP_GRAY, TrafficSignalBodyColor.BATTLESHIP_GRAY, TrafficSignalBodyColor.BATTLESHIP_GRAY,
            TrafficSignalVisorType.CIRCLE, TrafficSignalBulbStyle.LED, TrafficSignalBulbType.LEFT,
            TrafficSignalBulbColor.GREEN, TrafficSignalBulbColor.YELLOW, false, true)
            };
            return infos;
          })
          .retiring("controllableverticalhybridleftsignal")
          .build();

  public static final BlockControllableSignal VERTICAL_LEFT_ADD_ON_FYA_SIGNAL =
      new BlockControllableSignal.Builder(
          "controllableverticalleftaddonfyasignal",
          SIGNAL_SIDE.THROUGH,
          false,
          () -> {
            return new TrafficSignalSectionInfo[] {
            new TrafficSignalSectionInfo(TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK,
            TrafficSignalVisorType.CIRCLE, TrafficSignalBulbStyle.LED, TrafficSignalBulbType.LEFT,
            TrafficSignalBulbColor.GREEN, TrafficSignalBulbColor.YELLOW, false, true)
            };
          })
          .signalYOffset(-7.9f)
          .build();

  public static final BlockControllableSignal VERTICAL_LEFT_ADD_ON_FYA_SIGNAL_GRAY =
      new BlockControllableSignal.Builder(
          "controllableverticalleftaddonfyasignalgray",
          SIGNAL_SIDE.THROUGH,
          false,
          () -> {
            return new TrafficSignalSectionInfo[] {
            new TrafficSignalSectionInfo(TrafficSignalBodyColor.BATTLESHIP_GRAY, TrafficSignalBodyColor.BATTLESHIP_GRAY, TrafficSignalBodyColor.BATTLESHIP_GRAY,
            TrafficSignalVisorType.CIRCLE, TrafficSignalBulbStyle.LED, TrafficSignalBulbType.LEFT,
            TrafficSignalBulbColor.GREEN, TrafficSignalBulbColor.YELLOW, false, true)
            };
          })
          .signalYOffset(-7.9f)
          .retiring("controllableverticalleftaddonfyasignal")
          .build();

  public static final BlockControllableSignal VERTICAL_LEFT_ADD_ON_SIGNAL =
      new BlockControllableSignal.Builder(
          "controllableverticalleftaddonsignal",
          SIGNAL_SIDE.LEFT,
          true,
          () -> {
            return new TrafficSignalSectionInfo[] {
            new TrafficSignalSectionInfo(TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK,
            TrafficSignalVisorType.CIRCLE, TrafficSignalBulbStyle.LED_DOTTED, TrafficSignalBulbType.LEFT,
            TrafficSignalBulbColor.YELLOW, false),
            new TrafficSignalSectionInfo(TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK,
            TrafficSignalVisorType.CIRCLE, TrafficSignalBulbStyle.LED_DOTTED, TrafficSignalBulbType.LEFT,
            TrafficSignalBulbColor.GREEN, false)
            };
          })
          .sectionYPositions(0, 0)
          .signalYOffset(-7.9f)
          .build();

  public static final BlockControllableSignal VERTICAL_LEFT_ADD_ON_SIGNAL_GRAY =
      new BlockControllableSignal.Builder(
          "controllableverticalleftaddonsignalgray",
          SIGNAL_SIDE.LEFT,
          true,
          () -> {
            return new TrafficSignalSectionInfo[] {
            new TrafficSignalSectionInfo(TrafficSignalBodyColor.BATTLESHIP_GRAY, TrafficSignalBodyColor.BATTLESHIP_GRAY, TrafficSignalBodyColor.BATTLESHIP_GRAY,
            TrafficSignalVisorType.CIRCLE, TrafficSignalBulbStyle.LED_DOTTED, TrafficSignalBulbType.LEFT,
            TrafficSignalBulbColor.YELLOW, false),
            new TrafficSignalSectionInfo(TrafficSignalBodyColor.BATTLESHIP_GRAY, TrafficSignalBodyColor.BATTLESHIP_GRAY, TrafficSignalBodyColor.BATTLESHIP_GRAY,
            TrafficSignalVisorType.CIRCLE, TrafficSignalBulbStyle.LED_DOTTED, TrafficSignalBulbType.LEFT,
            TrafficSignalBulbColor.GREEN, false)
            };
          })
          .sectionYPositions(0, 0)
          .signalYOffset(-7.9f)
          .retiring("controllableverticalleftaddonsignal")
          .build();

  public static final BlockControllableSignal VERTICAL_LEFT_DOUBLE_ADD_ON_SIGNAL =
      new BlockControllableSignal.Builder(
          "controllableverticalleftdoubleaddonsignal",
          SIGNAL_SIDE.LEFT,
          true,
          () -> {
            return new TrafficSignalSectionInfo[] {
            new TrafficSignalSectionInfo(TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK,
            TrafficSignalVisorType.CIRCLE, TrafficSignalBulbStyle.LED, TrafficSignalBulbType.LEFT,
            TrafficSignalBulbColor.YELLOW, false),
            new TrafficSignalSectionInfo(TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK,
            TrafficSignalVisorType.CIRCLE, TrafficSignalBulbStyle.LED, TrafficSignalBulbType.LEFT,
            TrafficSignalBulbColor.GREEN, false)
            };
          })
          .sectionYPositions(0, -12)
          .signalYOffset(8.1f)
          .build();

  public static final BlockControllableSignal VERTICAL_LEFT_DOUBLE_ADD_ON_SIGNAL_GRAY =
      new BlockControllableSignal.Builder(
          "controllableverticalleftdoubleaddonsignalgray",
          SIGNAL_SIDE.LEFT,
          true,
          () -> {
            return new TrafficSignalSectionInfo[] {
            new TrafficSignalSectionInfo(TrafficSignalBodyColor.BATTLESHIP_GRAY, TrafficSignalBodyColor.BATTLESHIP_GRAY, TrafficSignalBodyColor.BATTLESHIP_GRAY,
            TrafficSignalVisorType.CIRCLE, TrafficSignalBulbStyle.LED, TrafficSignalBulbType.LEFT,
            TrafficSignalBulbColor.YELLOW, false),
            new TrafficSignalSectionInfo(TrafficSignalBodyColor.BATTLESHIP_GRAY, TrafficSignalBodyColor.BATTLESHIP_GRAY, TrafficSignalBodyColor.BATTLESHIP_GRAY,
            TrafficSignalVisorType.CIRCLE, TrafficSignalBulbStyle.LED, TrafficSignalBulbType.LEFT,
            TrafficSignalBulbColor.GREEN, false)
            };
          })
          .sectionYPositions(0, -12)
          .signalYOffset(8.1f)
          .retiring("controllableverticalleftdoubleaddonsignal")
          .build();

  public static final BlockControllableSignal VERTICAL_LEFT_SIGNAL =
      new BlockControllableSignal.Builder(
          "controllableverticalleftsignal",
          SIGNAL_SIDE.LEFT,
          true,
          () -> {
            return new TrafficSignalSectionInfo[] {
            new TrafficSignalSectionInfo(TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK,
            TrafficSignalVisorType.CIRCLE, TrafficSignalBulbStyle.LED, TrafficSignalBulbType.LEFT,
            TrafficSignalBulbColor.RED, false),
            new TrafficSignalSectionInfo(TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK,
            TrafficSignalVisorType.CIRCLE, TrafficSignalBulbStyle.LED, TrafficSignalBulbType.LEFT,
            TrafficSignalBulbColor.YELLOW, false),
            new TrafficSignalSectionInfo(TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK,
            TrafficSignalVisorType.CIRCLE, TrafficSignalBulbStyle.LED, TrafficSignalBulbType.LEFT,
            TrafficSignalBulbColor.GREEN, false)
            };
          })
          .build();

  public static final BlockControllableSignal VERTICAL_LEFT_SIGNAL8812_INCH =
      new BlockControllableSignal.Builder(
          "controllableverticalleftsignal8812inch",
          SIGNAL_SIDE.LEFT,
          true,
          () -> {
            return new TrafficSignalSectionInfo[] {
            new TrafficSignalSectionInfo(TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK,
            TrafficSignalVisorType.CIRCLE, TrafficSignalBulbStyle.LED, TrafficSignalBulbType.BALL,
            TrafficSignalBulbColor.RED, false),
            new TrafficSignalSectionInfo(TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK,
            TrafficSignalVisorType.CIRCLE, TrafficSignalBulbStyle.LED, TrafficSignalBulbType.BALL,
            TrafficSignalBulbColor.YELLOW, false),
            new TrafficSignalSectionInfo(TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK,
            TrafficSignalVisorType.CIRCLE, TrafficSignalBulbStyle.LED, TrafficSignalBulbType.LEFT,
            TrafficSignalBulbColor.GREEN, false)
            };
          })
          .sectionYPositions(6.0f, -2.0f, -12.0f)
          .sectionSizes(8, 8, 12)
          .build();

  public static final BlockControllableSignal VERTICAL_LEFT_SIGNAL_GRAY =
      new BlockControllableSignal.Builder(
          "controllableverticalleftsignalgray",
          SIGNAL_SIDE.LEFT,
          true,
          () -> {
            return new TrafficSignalSectionInfo[] {
            new TrafficSignalSectionInfo(TrafficSignalBodyColor.BATTLESHIP_GRAY, TrafficSignalBodyColor.BATTLESHIP_GRAY, TrafficSignalBodyColor.BATTLESHIP_GRAY,
            TrafficSignalVisorType.CIRCLE, TrafficSignalBulbStyle.LED, TrafficSignalBulbType.LEFT,
            TrafficSignalBulbColor.RED, false),
            new TrafficSignalSectionInfo(TrafficSignalBodyColor.BATTLESHIP_GRAY, TrafficSignalBodyColor.BATTLESHIP_GRAY, TrafficSignalBodyColor.BATTLESHIP_GRAY,
            TrafficSignalVisorType.CIRCLE, TrafficSignalBulbStyle.LED, TrafficSignalBulbType.LEFT,
            TrafficSignalBulbColor.YELLOW, false),
            new TrafficSignalSectionInfo(TrafficSignalBodyColor.BATTLESHIP_GRAY, TrafficSignalBodyColor.BATTLESHIP_GRAY, TrafficSignalBodyColor.BATTLESHIP_GRAY,
            TrafficSignalVisorType.CIRCLE, TrafficSignalBulbStyle.LED, TrafficSignalBulbType.LEFT,
            TrafficSignalBulbColor.GREEN, false)
            };
          })
          .retiring("controllableverticalleftsignal")
          .build();

  public static final BlockControllableSignal VERTICAL_LEFT_SIGNAL_LED =
      new BlockControllableSignal.Builder(
          "controllableverticalleftsignalled",
          SIGNAL_SIDE.LEFT,
          true,
          () -> {
            return new TrafficSignalSectionInfo[] {
            new TrafficSignalSectionInfo(TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK,
            TrafficSignalVisorType.CIRCLE, TrafficSignalBulbStyle.LED_DOTTED, TrafficSignalBulbType.LEFT,
            TrafficSignalBulbColor.RED, false),
            new TrafficSignalSectionInfo(TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK,
            TrafficSignalVisorType.CIRCLE, TrafficSignalBulbStyle.LED_DOTTED, TrafficSignalBulbType.LEFT,
            TrafficSignalBulbColor.YELLOW, false),
            new TrafficSignalSectionInfo(TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK,
            TrafficSignalVisorType.CIRCLE, TrafficSignalBulbStyle.LED_DOTTED, TrafficSignalBulbType.LEFT,
            TrafficSignalBulbColor.GREEN, false)
            };
          })
          .build();

  public static final BlockControllableSignal VERTICAL_LEFT_SIGNAL_SOLID_RED =
      new BlockControllableSignal.Builder(
          "controllableverticalleftsignalsolidred",
          SIGNAL_SIDE.LEFT,
          true,
          () -> {
            return new TrafficSignalSectionInfo[] {
            new TrafficSignalSectionInfo(TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK,
            TrafficSignalVisorType.CIRCLE, TrafficSignalBulbStyle.LED, TrafficSignalBulbType.BALL,
            TrafficSignalBulbColor.RED, false),
            new TrafficSignalSectionInfo(TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK,
            TrafficSignalVisorType.CIRCLE, TrafficSignalBulbStyle.LED, TrafficSignalBulbType.LEFT,
            TrafficSignalBulbColor.YELLOW, false),
            new TrafficSignalSectionInfo(TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK,
            TrafficSignalVisorType.CIRCLE, TrafficSignalBulbStyle.LED, TrafficSignalBulbType.LEFT,
            TrafficSignalBulbColor.GREEN, false)
            };
          })
          .build();

  public static final BlockControllableSignal VERTICAL_RAIL_SIGNAL =
      new BlockControllableSignal.Builder(
          "controllableverticalrailsignal",
          SIGNAL_SIDE.PROTECTED,
          true,
          () -> {
            TrafficSignalSectionInfo[] infos = new TrafficSignalSectionInfo[] {
            new TrafficSignalSectionInfo(TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK,
            TrafficSignalVisorType.CIRCLE, TrafficSignalBulbStyle.LED, TrafficSignalBulbType.TRANSIT,
            TrafficSignalBulbColor.RED, false),
            new TrafficSignalSectionInfo(TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK,
            TrafficSignalVisorType.CIRCLE, TrafficSignalBulbStyle.LED, TrafficSignalBulbType.TRANSIT,
            TrafficSignalBulbColor.YELLOW, false),
            new TrafficSignalSectionInfo(TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK,
            TrafficSignalVisorType.CIRCLE, TrafficSignalBulbStyle.LED, TrafficSignalBulbType.TRANSIT,
            TrafficSignalBulbColor.GREEN, false)
            };
            return infos;
          })
          .build();

  public static final BlockControllableSignal VERTICAL_RAIL_SIGNAL_GRAY =
      new BlockControllableSignal.Builder(
          "controllableverticalrailsignalgray",
          SIGNAL_SIDE.PROTECTED,
          true,
          () -> {
            TrafficSignalSectionInfo[] infos = new TrafficSignalSectionInfo[] {
            new TrafficSignalSectionInfo(TrafficSignalBodyColor.BATTLESHIP_GRAY, TrafficSignalBodyColor.BATTLESHIP_GRAY, TrafficSignalBodyColor.BATTLESHIP_GRAY,
            TrafficSignalVisorType.CIRCLE, TrafficSignalBulbStyle.LED, TrafficSignalBulbType.TRANSIT,
            TrafficSignalBulbColor.RED, false),
            new TrafficSignalSectionInfo(TrafficSignalBodyColor.BATTLESHIP_GRAY, TrafficSignalBodyColor.BATTLESHIP_GRAY, TrafficSignalBodyColor.BATTLESHIP_GRAY,
            TrafficSignalVisorType.CIRCLE, TrafficSignalBulbStyle.LED, TrafficSignalBulbType.TRANSIT,
            TrafficSignalBulbColor.YELLOW, false),
            new TrafficSignalSectionInfo(TrafficSignalBodyColor.BATTLESHIP_GRAY, TrafficSignalBodyColor.BATTLESHIP_GRAY, TrafficSignalBodyColor.BATTLESHIP_GRAY,
            TrafficSignalVisorType.CIRCLE, TrafficSignalBulbStyle.LED, TrafficSignalBulbType.TRANSIT,
            TrafficSignalBulbColor.GREEN, false)
            };
            return infos;
          })
          .retiring("controllableverticalrailsignal")
          .build();

  public static final BlockControllableSignal VERTICAL_RIGHT2_SIGNAL =
      new BlockControllableSignal.Builder(
          "controllableverticalright2signal",
          SIGNAL_SIDE.RIGHT,
          true,
          () -> {
            return new TrafficSignalSectionInfo[] {
            new TrafficSignalSectionInfo(TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK,
            TrafficSignalVisorType.CIRCLE, TrafficSignalBulbStyle.LED, TrafficSignalBulbType.RIGHT,
            TrafficSignalBulbColor.RED, false),
            new TrafficSignalSectionInfo(TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK,
            TrafficSignalVisorType.CIRCLE, TrafficSignalBulbStyle.LED, TrafficSignalBulbType.RIGHT,
            TrafficSignalBulbColor.YELLOW, false),
            new TrafficSignalSectionInfo(TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK,
            TrafficSignalVisorType.CIRCLE, TrafficSignalBulbStyle.LED, TrafficSignalBulbType.RIGHT,
            TrafficSignalBulbColor.GREEN, false)
            };
          })
          .build();

  public static final BlockControllableSignal VERTICAL_RIGHT_ADD_ON_FYA_SIGNAL =
      new BlockControllableSignal.Builder(
          "controllableverticalrightaddonfyasignal",
          SIGNAL_SIDE.THROUGH,
          false,
          () -> {
            return new TrafficSignalSectionInfo[] {
            new TrafficSignalSectionInfo(TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK,
            TrafficSignalVisorType.CIRCLE, TrafficSignalBulbStyle.LED, TrafficSignalBulbType.RIGHT,
            TrafficSignalBulbColor.GREEN, TrafficSignalBulbColor.YELLOW, false, true)
            };
          })
          .signalYOffset(-7.9f)
          .build();

  public static final BlockControllableSignal VERTICAL_RIGHT_ADD_ON_FYA_SIGNAL_GRAY =
      new BlockControllableSignal.Builder(
          "controllableverticalrightaddonfyasignalgray",
          SIGNAL_SIDE.THROUGH,
          false,
          () -> {
            return new TrafficSignalSectionInfo[] {
            new TrafficSignalSectionInfo(TrafficSignalBodyColor.BATTLESHIP_GRAY, TrafficSignalBodyColor.BATTLESHIP_GRAY, TrafficSignalBodyColor.BATTLESHIP_GRAY,
            TrafficSignalVisorType.CIRCLE, TrafficSignalBulbStyle.LED, TrafficSignalBulbType.RIGHT,
            TrafficSignalBulbColor.GREEN, TrafficSignalBulbColor.YELLOW, false, true)
            };
          })
          .signalYOffset(-7.9f)
          .retiring("controllableverticalrightaddonfyasignal")
          .build();

  public static final BlockControllableSignal VERTICAL_RIGHT_ADD_ON_SIGNAL =
      new BlockControllableSignal.Builder(
          "controllableverticalrightaddonsignal",
          SIGNAL_SIDE.RIGHT,
          false,
          () -> {
            return new TrafficSignalSectionInfo[] {
            new TrafficSignalSectionInfo(TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK,
            TrafficSignalVisorType.CIRCLE, TrafficSignalBulbStyle.LED_DOTTED, TrafficSignalBulbType.RIGHT,
            TrafficSignalBulbColor.YELLOW, false),
            new TrafficSignalSectionInfo(TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK,
            TrafficSignalVisorType.CIRCLE, TrafficSignalBulbStyle.LED_DOTTED, TrafficSignalBulbType.RIGHT,
            TrafficSignalBulbColor.GREEN, false)
            };
          })
          .sectionYPositions(0, 0)
          .signalYOffset(-7.9f)
          .build();

  public static final BlockControllableSignal VERTICAL_RIGHT_ADD_ON_SIGNAL_GRAY =
      new BlockControllableSignal.Builder(
          "controllableverticalrightaddonsignalgray",
          SIGNAL_SIDE.RIGHT,
          false,
          () -> {
            return new TrafficSignalSectionInfo[] {
            new TrafficSignalSectionInfo(TrafficSignalBodyColor.BATTLESHIP_GRAY, TrafficSignalBodyColor.BATTLESHIP_GRAY, TrafficSignalBodyColor.BATTLESHIP_GRAY,
            TrafficSignalVisorType.CIRCLE, TrafficSignalBulbStyle.LED_DOTTED, TrafficSignalBulbType.RIGHT,
            TrafficSignalBulbColor.YELLOW, false),
            new TrafficSignalSectionInfo(TrafficSignalBodyColor.BATTLESHIP_GRAY, TrafficSignalBodyColor.BATTLESHIP_GRAY, TrafficSignalBodyColor.BATTLESHIP_GRAY,
            TrafficSignalVisorType.CIRCLE, TrafficSignalBulbStyle.LED_DOTTED, TrafficSignalBulbType.RIGHT,
            TrafficSignalBulbColor.GREEN, false)
            };
          })
          .sectionYPositions(0, 0)
          .signalYOffset(-7.9f)
          .retiring("controllableverticalrightaddonsignal")
          .build();

  public static final BlockControllableSignal VERTICAL_RIGHT_DOUBLE_ADD_ON_SIGNAL =
      new BlockControllableSignal.Builder(
          "controllableverticalrightdoubleaddonsignal",
          SIGNAL_SIDE.RIGHT,
          false,
          () -> {
            return new TrafficSignalSectionInfo[] {
            new TrafficSignalSectionInfo(TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK,
            TrafficSignalVisorType.CIRCLE, TrafficSignalBulbStyle.LED, TrafficSignalBulbType.RIGHT,
            TrafficSignalBulbColor.YELLOW, false),
            new TrafficSignalSectionInfo(TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK,
            TrafficSignalVisorType.CIRCLE, TrafficSignalBulbStyle.LED, TrafficSignalBulbType.RIGHT,
            TrafficSignalBulbColor.GREEN, false)
            };
          })
          .sectionYPositions(0, -12)
          .signalYOffset(8.1f)
          .build();

  public static final BlockControllableSignal VERTICAL_RIGHT_DOUBLE_ADD_ON_SIGNAL_GRAY =
      new BlockControllableSignal.Builder(
          "controllableverticalrightdoubleaddonsignalgray",
          SIGNAL_SIDE.RIGHT,
          false,
          () -> {
            return new TrafficSignalSectionInfo[] {
            new TrafficSignalSectionInfo(TrafficSignalBodyColor.BATTLESHIP_GRAY, TrafficSignalBodyColor.BATTLESHIP_GRAY, TrafficSignalBodyColor.BATTLESHIP_GRAY,
            TrafficSignalVisorType.CIRCLE, TrafficSignalBulbStyle.LED, TrafficSignalBulbType.RIGHT,
            TrafficSignalBulbColor.YELLOW, false),
            new TrafficSignalSectionInfo(TrafficSignalBodyColor.BATTLESHIP_GRAY, TrafficSignalBodyColor.BATTLESHIP_GRAY, TrafficSignalBodyColor.BATTLESHIP_GRAY,
            TrafficSignalVisorType.CIRCLE, TrafficSignalBulbStyle.LED, TrafficSignalBulbType.RIGHT,
            TrafficSignalBulbColor.GREEN, false)
            };
          })
          .sectionYPositions(0, -12)
          .signalYOffset(8.1f)
          .retiring("controllableverticalrightdoubleaddonsignal")
          .build();

  public static final BlockControllableSignal VERTICAL_RIGHT_FLASH_YELLOW_ADD_ON_SIGNAL =
      new BlockControllableSignal.Builder(
          "controllableverticalrightflashyellowaddonsignal",
          SIGNAL_SIDE.RIGHT,
          true,
          () -> {
            return new TrafficSignalSectionInfo[] {
            new TrafficSignalSectionInfo(TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK,
            TrafficSignalVisorType.CIRCLE, TrafficSignalBulbStyle.LED, TrafficSignalBulbType.RIGHT,
            TrafficSignalBulbColor.GREEN, false)
            };
          })
          .signalYOffset(-7.9f)
          .build();

  public static final BlockControllableSignal VERTICAL_RIGHT_FLASH_YELLOW_SR_SIGNAL =
      new BlockControllableSignal.Builder(
          "controllableverticalrightflashyellowsrsignal",
          SIGNAL_SIDE.FLASHING_RIGHT,
          true,
          () -> {
            TrafficSignalSectionInfo[] infos = new TrafficSignalSectionInfo[] {
            new TrafficSignalSectionInfo(TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK,
            TrafficSignalVisorType.CIRCLE, TrafficSignalBulbStyle.LED, TrafficSignalBulbType.BALL,
            TrafficSignalBulbColor.RED, false),
            new TrafficSignalSectionInfo(TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK,
            TrafficSignalVisorType.CIRCLE, TrafficSignalBulbStyle.LED, TrafficSignalBulbType.RIGHT,
            TrafficSignalBulbColor.YELLOW, false),
            new TrafficSignalSectionInfo(TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK,
            TrafficSignalVisorType.CIRCLE, TrafficSignalBulbStyle.LED, TrafficSignalBulbType.RIGHT,
            TrafficSignalBulbColor.GREEN, TrafficSignalBulbColor.YELLOW, false, true)
            };
            return infos;
          })
          .build();

  public static final BlockControllableSignal VERTICAL_RIGHT_FLASH_YELLOW_SIGNAL =
      new BlockControllableSignal.Builder(
          "controllableverticalrightflashyellowsignal",
          SIGNAL_SIDE.FLASHING_RIGHT,
          true,
          () -> {
            TrafficSignalSectionInfo[] infos = new TrafficSignalSectionInfo[] {
            new TrafficSignalSectionInfo(TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK,
            TrafficSignalVisorType.CIRCLE, TrafficSignalBulbStyle.LED, TrafficSignalBulbType.RIGHT,
            TrafficSignalBulbColor.RED, false),
            new TrafficSignalSectionInfo(TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK,
            TrafficSignalVisorType.CIRCLE, TrafficSignalBulbStyle.LED, TrafficSignalBulbType.RIGHT,
            TrafficSignalBulbColor.YELLOW, false),
            new TrafficSignalSectionInfo(TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK,
            TrafficSignalVisorType.CIRCLE, TrafficSignalBulbStyle.LED, TrafficSignalBulbType.RIGHT,
            TrafficSignalBulbColor.GREEN, TrafficSignalBulbColor.YELLOW, false, true)
            };
            return infos;
          })
          .build();

  public static final BlockControllableSignal VERTICAL_RIGHT_SIGNAL =
      new BlockControllableSignal.Builder(
          "controllableverticalrightsignal",
          SIGNAL_SIDE.RIGHT,
          true,
          () -> {
            return new TrafficSignalSectionInfo[] {
            new TrafficSignalSectionInfo(TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK,
            TrafficSignalVisorType.CIRCLE, TrafficSignalBulbStyle.LED, TrafficSignalBulbType.RIGHT,
            TrafficSignalBulbColor.RED, false),
            new TrafficSignalSectionInfo(TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK,
            TrafficSignalVisorType.CIRCLE, TrafficSignalBulbStyle.LED, TrafficSignalBulbType.RIGHT,
            TrafficSignalBulbColor.YELLOW, false),
            new TrafficSignalSectionInfo(TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK,
            TrafficSignalVisorType.CIRCLE, TrafficSignalBulbStyle.LED, TrafficSignalBulbType.RIGHT,
            TrafficSignalBulbColor.GREEN, false)
            };
          })
          .build();

  public static final BlockControllableSignal VERTICAL_RIGHT_SIGNAL8812_INCH =
      new BlockControllableSignal.Builder(
          "controllableverticalrightsignal8812inch",
          SIGNAL_SIDE.RIGHT,
          true,
          () -> {
            return new TrafficSignalSectionInfo[] {
            new TrafficSignalSectionInfo(TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK,
            TrafficSignalVisorType.CIRCLE, TrafficSignalBulbStyle.LED, TrafficSignalBulbType.BALL,
            TrafficSignalBulbColor.RED, false),
            new TrafficSignalSectionInfo(TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK,
            TrafficSignalVisorType.CIRCLE, TrafficSignalBulbStyle.LED, TrafficSignalBulbType.BALL,
            TrafficSignalBulbColor.YELLOW, false),
            new TrafficSignalSectionInfo(TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK,
            TrafficSignalVisorType.CIRCLE, TrafficSignalBulbStyle.LED, TrafficSignalBulbType.RIGHT,
            TrafficSignalBulbColor.GREEN, false)
            };
          })
          .sectionYPositions(6.0f, -2.0f, -12.0f)
          .sectionSizes(8, 8, 12)
          .build();

  public static final BlockControllableSignal VERTICAL_RIGHT_SIGNAL_GRAY =
      new BlockControllableSignal.Builder(
          "controllableverticalrightsignalgray",
          SIGNAL_SIDE.RIGHT,
          true,
          () -> {
            return new TrafficSignalSectionInfo[] {
            new TrafficSignalSectionInfo(TrafficSignalBodyColor.BATTLESHIP_GRAY, TrafficSignalBodyColor.BATTLESHIP_GRAY, TrafficSignalBodyColor.BATTLESHIP_GRAY,
            TrafficSignalVisorType.CIRCLE, TrafficSignalBulbStyle.LED, TrafficSignalBulbType.RIGHT,
            TrafficSignalBulbColor.RED, false),
            new TrafficSignalSectionInfo(TrafficSignalBodyColor.BATTLESHIP_GRAY, TrafficSignalBodyColor.BATTLESHIP_GRAY, TrafficSignalBodyColor.BATTLESHIP_GRAY,
            TrafficSignalVisorType.CIRCLE, TrafficSignalBulbStyle.LED, TrafficSignalBulbType.RIGHT,
            TrafficSignalBulbColor.YELLOW, false),
            new TrafficSignalSectionInfo(TrafficSignalBodyColor.BATTLESHIP_GRAY, TrafficSignalBodyColor.BATTLESHIP_GRAY, TrafficSignalBodyColor.BATTLESHIP_GRAY,
            TrafficSignalVisorType.CIRCLE, TrafficSignalBulbStyle.LED, TrafficSignalBulbType.RIGHT,
            TrafficSignalBulbColor.GREEN, false)
            };
          })
          .retiring("controllableverticalrightsignal")
          .build();

  public static final BlockControllableSignal VERTICAL_SOLID_FLASH_GREEN_SIGNAL =
      new BlockControllableSignal.Builder(
          "controllableverticalsolidflashgreensignal",
          SIGNAL_SIDE.THROUGH,
          true,
          () -> {
            TrafficSignalSectionInfo[] infos = new TrafficSignalSectionInfo[] {
            new TrafficSignalSectionInfo(TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK,
            TrafficSignalVisorType.CIRCLE, TrafficSignalBulbStyle.LED, TrafficSignalBulbType.BALL,
            TrafficSignalBulbColor.RED, false),
            new TrafficSignalSectionInfo(TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK,
            TrafficSignalVisorType.CIRCLE, TrafficSignalBulbStyle.LED, TrafficSignalBulbType.BALL,
            TrafficSignalBulbColor.YELLOW, false),
            new TrafficSignalSectionInfo(TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK,
            TrafficSignalVisorType.CIRCLE, TrafficSignalBulbStyle.LED, TrafficSignalBulbType.BALL,
            TrafficSignalBulbColor.GREEN, true)
            };
            return infos;
          })
          .build();

  public static final BlockControllableSignal VERTICAL_SOLID_FLASH_GREEN_SIGNAL_GRAY =
      new BlockControllableSignal.Builder(
          "controllableverticalsolidflashgreensignalgray",
          SIGNAL_SIDE.THROUGH,
          true,
          () -> {
            TrafficSignalSectionInfo[] infos = new TrafficSignalSectionInfo[] {
            new TrafficSignalSectionInfo(TrafficSignalBodyColor.BATTLESHIP_GRAY, TrafficSignalBodyColor.BATTLESHIP_GRAY, TrafficSignalBodyColor.BATTLESHIP_GRAY,
            TrafficSignalVisorType.CIRCLE, TrafficSignalBulbStyle.LED, TrafficSignalBulbType.BALL,
            TrafficSignalBulbColor.RED, false),
            new TrafficSignalSectionInfo(TrafficSignalBodyColor.BATTLESHIP_GRAY, TrafficSignalBodyColor.BATTLESHIP_GRAY, TrafficSignalBodyColor.BATTLESHIP_GRAY,
            TrafficSignalVisorType.CIRCLE, TrafficSignalBulbStyle.LED, TrafficSignalBulbType.BALL,
            TrafficSignalBulbColor.YELLOW, false),
            new TrafficSignalSectionInfo(TrafficSignalBodyColor.BATTLESHIP_GRAY, TrafficSignalBodyColor.BATTLESHIP_GRAY, TrafficSignalBodyColor.BATTLESHIP_GRAY,
            TrafficSignalVisorType.CIRCLE, TrafficSignalBulbStyle.LED, TrafficSignalBulbType.BALL,
            TrafficSignalBulbColor.GREEN, true)
            };
            return infos;
          })
          .retiring("controllableverticalsolidflashgreensignal")
          .build();

  public static final BlockControllableSignal VERTICAL_SOLID_FLASH_RED_SIGNAL =
      new BlockControllableSignal.Builder(
          "controllableverticalsolidflashredsignal",
          SIGNAL_SIDE.THROUGH,
          true,
          () -> {
            TrafficSignalSectionInfo[] infos = new TrafficSignalSectionInfo[] {
            new TrafficSignalSectionInfo(TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK,
            TrafficSignalVisorType.CIRCLE, TrafficSignalBulbStyle.LED, TrafficSignalBulbType.BALL,
            TrafficSignalBulbColor.RED, false),
            new TrafficSignalSectionInfo(TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK,
            TrafficSignalVisorType.CIRCLE, TrafficSignalBulbStyle.LED, TrafficSignalBulbType.BALL,
            TrafficSignalBulbColor.YELLOW, false),
            new TrafficSignalSectionInfo(TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK,
            TrafficSignalVisorType.CIRCLE, TrafficSignalBulbStyle.LED, TrafficSignalBulbType.BALL,
            TrafficSignalBulbColor.GREEN, TrafficSignalBulbColor.RED, false, true)
            };
            return infos;
          })
          .build();

  public static final BlockControllableSignal VERTICAL_SOLID_FLASH_RED_SIGNAL_GRAY =
      new BlockControllableSignal.Builder(
          "controllableverticalsolidflashredsignalgray",
          SIGNAL_SIDE.THROUGH,
          true,
          () -> {
            TrafficSignalSectionInfo[] infos = new TrafficSignalSectionInfo[] {
            new TrafficSignalSectionInfo(TrafficSignalBodyColor.BATTLESHIP_GRAY, TrafficSignalBodyColor.BATTLESHIP_GRAY, TrafficSignalBodyColor.BATTLESHIP_GRAY,
            TrafficSignalVisorType.CIRCLE, TrafficSignalBulbStyle.LED, TrafficSignalBulbType.BALL,
            TrafficSignalBulbColor.RED, false),
            new TrafficSignalSectionInfo(TrafficSignalBodyColor.BATTLESHIP_GRAY, TrafficSignalBodyColor.BATTLESHIP_GRAY, TrafficSignalBodyColor.BATTLESHIP_GRAY,
            TrafficSignalVisorType.CIRCLE, TrafficSignalBulbStyle.LED, TrafficSignalBulbType.BALL,
            TrafficSignalBulbColor.YELLOW, false),
            new TrafficSignalSectionInfo(TrafficSignalBodyColor.BATTLESHIP_GRAY, TrafficSignalBodyColor.BATTLESHIP_GRAY, TrafficSignalBodyColor.BATTLESHIP_GRAY,
            TrafficSignalVisorType.CIRCLE, TrafficSignalBulbStyle.LED, TrafficSignalBulbType.BALL,
            TrafficSignalBulbColor.GREEN, TrafficSignalBulbColor.RED, false, true)
            };
            return infos;
          })
          .retiring("controllableverticalsolidflashredsignal")
          .build();

  public static final BlockControllableSignal VERTICAL_SOLID_FLASH_YELLOW_SIGNAL =
      new BlockControllableSignal.Builder(
          "controllableverticalsolidflashyellowsignal",
          SIGNAL_SIDE.THROUGH,
          true,
          () -> {
            TrafficSignalSectionInfo[] infos = new TrafficSignalSectionInfo[] {
            new TrafficSignalSectionInfo(TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK,
            TrafficSignalVisorType.CIRCLE, TrafficSignalBulbStyle.LED, TrafficSignalBulbType.BALL,
            TrafficSignalBulbColor.RED, false),
            new TrafficSignalSectionInfo(TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK,
            TrafficSignalVisorType.CIRCLE, TrafficSignalBulbStyle.LED, TrafficSignalBulbType.BALL,
            TrafficSignalBulbColor.YELLOW, false),
            new TrafficSignalSectionInfo(TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK,
            TrafficSignalVisorType.CIRCLE, TrafficSignalBulbStyle.LED, TrafficSignalBulbType.BALL,
            TrafficSignalBulbColor.GREEN, TrafficSignalBulbColor.YELLOW, false, true)
            };
            return infos;
          })
          .build();

  public static final BlockControllableSignal VERTICAL_SOLID_FLASH_YELLOW_SIGNAL_GRAY =
      new BlockControllableSignal.Builder(
          "controllableverticalsolidflashyellowsignalgray",
          SIGNAL_SIDE.THROUGH,
          true,
          () -> {
            TrafficSignalSectionInfo[] infos = new TrafficSignalSectionInfo[] {
            new TrafficSignalSectionInfo(TrafficSignalBodyColor.BATTLESHIP_GRAY, TrafficSignalBodyColor.BATTLESHIP_GRAY, TrafficSignalBodyColor.BATTLESHIP_GRAY,
            TrafficSignalVisorType.CIRCLE, TrafficSignalBulbStyle.LED, TrafficSignalBulbType.BALL,
            TrafficSignalBulbColor.RED, false),
            new TrafficSignalSectionInfo(TrafficSignalBodyColor.BATTLESHIP_GRAY, TrafficSignalBodyColor.BATTLESHIP_GRAY, TrafficSignalBodyColor.BATTLESHIP_GRAY,
            TrafficSignalVisorType.CIRCLE, TrafficSignalBulbStyle.LED, TrafficSignalBulbType.BALL,
            TrafficSignalBulbColor.YELLOW, false),
            new TrafficSignalSectionInfo(TrafficSignalBodyColor.BATTLESHIP_GRAY, TrafficSignalBodyColor.BATTLESHIP_GRAY, TrafficSignalBodyColor.BATTLESHIP_GRAY,
            TrafficSignalVisorType.CIRCLE, TrafficSignalBulbStyle.LED, TrafficSignalBulbType.BALL,
            TrafficSignalBulbColor.GREEN, TrafficSignalBulbColor.YELLOW, false, true)
            };
            return infos;
          })
          .retiring("controllableverticalsolidflashyellowsignal")
          .build();

  public static final BlockControllableSignal VERTICAL_SOLID_SIGNAL =
      new BlockControllableSignal.Builder(
          "controllableverticalsolidsignal",
          SIGNAL_SIDE.THROUGH,
          true,
          () -> {
            return new TrafficSignalSectionInfo[] {
            new TrafficSignalSectionInfo(TrafficSignalBodyColor.FLAT_BLACK,TrafficSignalBodyColor.FLAT_BLACK,TrafficSignalBodyColor.FLAT_BLACK,
            TrafficSignalVisorType.CIRCLE, TrafficSignalBulbStyle.LED, TrafficSignalBulbType.BALL,
            TrafficSignalBulbColor.RED,false),
            new TrafficSignalSectionInfo(TrafficSignalBodyColor.FLAT_BLACK,TrafficSignalBodyColor.FLAT_BLACK,TrafficSignalBodyColor.FLAT_BLACK,
            TrafficSignalVisorType.CIRCLE, TrafficSignalBulbStyle.LED, TrafficSignalBulbType.BALL,
            TrafficSignalBulbColor.YELLOW,false),
            new TrafficSignalSectionInfo(TrafficSignalBodyColor.FLAT_BLACK,TrafficSignalBodyColor.FLAT_BLACK,TrafficSignalBodyColor.FLAT_BLACK,
            TrafficSignalVisorType.CIRCLE, TrafficSignalBulbStyle.LED, TrafficSignalBulbType.BALL,
            TrafficSignalBulbColor.GREEN,false)
            };
          })
          .build();

  public static final BlockControllableSignal VERTICAL_SOLID_SIGNAL1288_INCH =
      new BlockControllableSignal.Builder(
          "controllableverticalsolidsignal1288inch",
          SIGNAL_SIDE.THROUGH,
          true,
          () -> {
            return new TrafficSignalSectionInfo[] {
            new TrafficSignalSectionInfo(TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK,
            TrafficSignalVisorType.CIRCLE, TrafficSignalBulbStyle.LED, TrafficSignalBulbType.BALL,
            TrafficSignalBulbColor.RED, false),
            new TrafficSignalSectionInfo(TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK,
            TrafficSignalVisorType.CIRCLE, TrafficSignalBulbStyle.LED, TrafficSignalBulbType.BALL,
            TrafficSignalBulbColor.YELLOW, false),
            new TrafficSignalSectionInfo(TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK,
            TrafficSignalVisorType.CIRCLE, TrafficSignalBulbStyle.LED, TrafficSignalBulbType.BALL,
            TrafficSignalBulbColor.GREEN, false)
            };
          })
          .sectionYPositions(4.0f, -6.0f, -14.0f)
          .sectionSizes(12, 8, 8)
          .build();

  public static final BlockControllableSignal VERTICAL_SOLID_SIGNAL8812_INCH =
      new BlockControllableSignal.Builder(
          "controllableverticalsolidsignal8812inch",
          SIGNAL_SIDE.THROUGH,
          true,
          () -> {
            return new TrafficSignalSectionInfo[] {
            new TrafficSignalSectionInfo(TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK,
            TrafficSignalVisorType.CIRCLE, TrafficSignalBulbStyle.LED, TrafficSignalBulbType.BALL,
            TrafficSignalBulbColor.RED, false),
            new TrafficSignalSectionInfo(TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK,
            TrafficSignalVisorType.CIRCLE, TrafficSignalBulbStyle.LED, TrafficSignalBulbType.BALL,
            TrafficSignalBulbColor.YELLOW, false),
            new TrafficSignalSectionInfo(TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK,
            TrafficSignalVisorType.CIRCLE, TrafficSignalBulbStyle.LED, TrafficSignalBulbType.BALL,
            TrafficSignalBulbColor.GREEN, false)
            };
          })
          .sectionYPositions(6.0f, -2.0f, -12.0f)
          .sectionSizes(8, 8, 12)
          .build();

  public static final BlockControllableSignal VERTICAL_SOLID_SIGNAL8_INCH =
      new BlockControllableSignal.Builder(
          "controllableverticalsolidsignal8inch",
          SIGNAL_SIDE.THROUGH,
          true,
          () -> {
            return new TrafficSignalSectionInfo[] {
            new TrafficSignalSectionInfo(TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK,
            TrafficSignalVisorType.CIRCLE, TrafficSignalBulbStyle.LED, TrafficSignalBulbType.BALL,
            TrafficSignalBulbColor.RED, false),
            new TrafficSignalSectionInfo(TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK,
            TrafficSignalVisorType.CIRCLE, TrafficSignalBulbStyle.LED, TrafficSignalBulbType.BALL,
            TrafficSignalBulbColor.YELLOW, false),
            new TrafficSignalSectionInfo(TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK,
            TrafficSignalVisorType.CIRCLE, TrafficSignalBulbStyle.LED, TrafficSignalBulbType.BALL,
            TrafficSignalBulbColor.GREEN, false)
            };
          })
          .sectionYPositions(2.0f, -6.0f, -14.0f)
          .sectionSizes(8, 8, 8)
          .build();

  public static final BlockControllableSignal VERTICAL_SOLID_SIGNAL_BARLO =
      new BlockControllableSignal.Builder(
          "controllableverticalsolidsignalbarlo",
          SIGNAL_SIDE.THROUGH,
          true,
          () -> {
            return new TrafficSignalSectionInfo[] {
            new TrafficSignalSectionInfo(TrafficSignalBodyColor.FLAT_BLACK,TrafficSignalBodyColor.FLAT_BLACK,TrafficSignalBodyColor.FLAT_BLACK,
            TrafficSignalVisorType.BARLO, TrafficSignalBulbStyle.LED, TrafficSignalBulbType.BALL,
            TrafficSignalBulbColor.RED,false),
            new TrafficSignalSectionInfo(TrafficSignalBodyColor.FLAT_BLACK,TrafficSignalBodyColor.FLAT_BLACK,TrafficSignalBodyColor.FLAT_BLACK,
            TrafficSignalVisorType.CIRCLE, TrafficSignalBulbStyle.LED, TrafficSignalBulbType.BALL,
            TrafficSignalBulbColor.YELLOW,false),
            new TrafficSignalSectionInfo(TrafficSignalBodyColor.FLAT_BLACK,TrafficSignalBodyColor.FLAT_BLACK,TrafficSignalBodyColor.FLAT_BLACK,
            TrafficSignalVisorType.CIRCLE, TrafficSignalBulbStyle.LED, TrafficSignalBulbType.BALL,
            TrafficSignalBulbColor.GREEN,false)
            };
          })
          .retiring("controllableverticalsolidsignal")
          .build();

  public static final BlockControllableSignal VERTICAL_SOLID_SIGNAL_GRAY =
      new BlockControllableSignal.Builder(
          "controllableverticalsolidsignalgray",
          SIGNAL_SIDE.THROUGH,
          true,
          () -> {
            return new TrafficSignalSectionInfo[] {
            new TrafficSignalSectionInfo(TrafficSignalBodyColor.BATTLESHIP_GRAY,TrafficSignalBodyColor.BATTLESHIP_GRAY,TrafficSignalBodyColor.BATTLESHIP_GRAY,
            TrafficSignalVisorType.CIRCLE, TrafficSignalBulbStyle.LED, TrafficSignalBulbType.BALL,
            TrafficSignalBulbColor.RED,false),
            new TrafficSignalSectionInfo(TrafficSignalBodyColor.BATTLESHIP_GRAY,TrafficSignalBodyColor.BATTLESHIP_GRAY,TrafficSignalBodyColor.BATTLESHIP_GRAY,
            TrafficSignalVisorType.CIRCLE, TrafficSignalBulbStyle.LED, TrafficSignalBulbType.BALL,
            TrafficSignalBulbColor.YELLOW,false),
            new TrafficSignalSectionInfo(TrafficSignalBodyColor.BATTLESHIP_GRAY,TrafficSignalBodyColor.BATTLESHIP_GRAY,TrafficSignalBodyColor.BATTLESHIP_GRAY,
            TrafficSignalVisorType.CIRCLE, TrafficSignalBulbStyle.LED, TrafficSignalBulbType.BALL,
            TrafficSignalBulbColor.GREEN,false)
            };
          })
          .retiring("controllableverticalsolidsignal")
          .build();

  public static final BlockControllableSignal VERTICAL_SOLID_SIGNAL_LED =
      new BlockControllableSignal.Builder(
          "controllableverticalsolidsignalled",
          SIGNAL_SIDE.THROUGH,
          true,
          () -> {
            return new TrafficSignalSectionInfo[] {
            new TrafficSignalSectionInfo(TrafficSignalBodyColor.FLAT_BLACK,TrafficSignalBodyColor.FLAT_BLACK,TrafficSignalBodyColor.FLAT_BLACK,
            TrafficSignalVisorType.CIRCLE, TrafficSignalBulbStyle.LED_DOTTED, TrafficSignalBulbType.BALL,
            TrafficSignalBulbColor.RED,false),
            new TrafficSignalSectionInfo(TrafficSignalBodyColor.FLAT_BLACK,TrafficSignalBodyColor.FLAT_BLACK,TrafficSignalBodyColor.FLAT_BLACK,
            TrafficSignalVisorType.CIRCLE, TrafficSignalBulbStyle.LED_DOTTED, TrafficSignalBulbType.BALL,
            TrafficSignalBulbColor.YELLOW,false),
            new TrafficSignalSectionInfo(TrafficSignalBodyColor.FLAT_BLACK,TrafficSignalBodyColor.FLAT_BLACK,TrafficSignalBodyColor.FLAT_BLACK,
            TrafficSignalVisorType.CIRCLE, TrafficSignalBulbStyle.LED_DOTTED, TrafficSignalBulbType.BALL,
            TrafficSignalBulbColor.GREEN,false)
            };
          })
          .build();

  public static final BlockControllableSignal VERTICAL_SOLID_SIGNAL_LED1288_INCH =
      new BlockControllableSignal.Builder(
          "controllableverticalsolidsignalled1288inch",
          SIGNAL_SIDE.THROUGH,
          true,
          () -> {
            return new TrafficSignalSectionInfo[] {
            new TrafficSignalSectionInfo(TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK,
            TrafficSignalVisorType.CIRCLE, TrafficSignalBulbStyle.LED_DOTTED, TrafficSignalBulbType.BALL,
            TrafficSignalBulbColor.RED, false),
            new TrafficSignalSectionInfo(TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK,
            TrafficSignalVisorType.CIRCLE, TrafficSignalBulbStyle.LED_DOTTED, TrafficSignalBulbType.BALL,
            TrafficSignalBulbColor.YELLOW, false),
            new TrafficSignalSectionInfo(TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK,
            TrafficSignalVisorType.CIRCLE, TrafficSignalBulbStyle.LED_DOTTED, TrafficSignalBulbType.BALL,
            TrafficSignalBulbColor.GREEN, false)
            };
          })
          .sectionYPositions(4.0f, -6.0f, -14.0f)
          .sectionSizes(12, 8, 8)
          .build();

  public static final BlockControllableSignal VERTICAL_SOLID_SIGNAL_LED8812_INCH =
      new BlockControllableSignal.Builder(
          "controllableverticalsolidsignalled8812inch",
          SIGNAL_SIDE.THROUGH,
          true,
          () -> {
            return new TrafficSignalSectionInfo[] {
            new TrafficSignalSectionInfo(TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK,
            TrafficSignalVisorType.CIRCLE, TrafficSignalBulbStyle.LED_DOTTED, TrafficSignalBulbType.BALL,
            TrafficSignalBulbColor.RED, false),
            new TrafficSignalSectionInfo(TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK,
            TrafficSignalVisorType.CIRCLE, TrafficSignalBulbStyle.LED_DOTTED, TrafficSignalBulbType.BALL,
            TrafficSignalBulbColor.YELLOW, false),
            new TrafficSignalSectionInfo(TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK,
            TrafficSignalVisorType.CIRCLE, TrafficSignalBulbStyle.LED_DOTTED, TrafficSignalBulbType.BALL,
            TrafficSignalBulbColor.GREEN, false)
            };
          })
          .sectionYPositions(6.0f, -2.0f, -12.0f)
          .sectionSizes(8, 8, 12)
          .build();

  public static final BlockControllableSignal VERTICAL_SOLID_SIGNAL_LED8_INCH =
      new BlockControllableSignal.Builder(
          "controllableverticalsolidsignalled8inch",
          SIGNAL_SIDE.THROUGH,
          true,
          () -> {
            return new TrafficSignalSectionInfo[] {
            new TrafficSignalSectionInfo(TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK,
            TrafficSignalVisorType.CIRCLE, TrafficSignalBulbStyle.LED_DOTTED, TrafficSignalBulbType.BALL,
            TrafficSignalBulbColor.RED, false),
            new TrafficSignalSectionInfo(TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK,
            TrafficSignalVisorType.CIRCLE, TrafficSignalBulbStyle.LED_DOTTED, TrafficSignalBulbType.BALL,
            TrafficSignalBulbColor.YELLOW, false),
            new TrafficSignalSectionInfo(TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK,
            TrafficSignalVisorType.CIRCLE, TrafficSignalBulbStyle.LED_DOTTED, TrafficSignalBulbType.BALL,
            TrafficSignalBulbColor.GREEN, false)
            };
          })
          .sectionYPositions(2.0f, -6.0f, -14.0f)
          .sectionSizes(8, 8, 8)
          .build();

  public static final BlockControllableSignal VERTICAL_SOLID_SIGNAL_NO_RED_VISOR =
      new BlockControllableSignal.Builder(
          "controllableverticalsolidsignalnoredvisor",
          SIGNAL_SIDE.THROUGH,
          true,
          () -> {
            return new TrafficSignalSectionInfo[] {
            new TrafficSignalSectionInfo(TrafficSignalBodyColor.FLAT_BLACK,TrafficSignalBodyColor.FLAT_BLACK,TrafficSignalBodyColor.FLAT_BLACK,
            TrafficSignalVisorType.CIRCLE, TrafficSignalBulbStyle.LED, TrafficSignalBulbType.BALL,
            TrafficSignalBulbColor.RED,false),
            new TrafficSignalSectionInfo(TrafficSignalBodyColor.FLAT_BLACK,TrafficSignalBodyColor.FLAT_BLACK,TrafficSignalBodyColor.FLAT_BLACK,
            TrafficSignalVisorType.CIRCLE, TrafficSignalBulbStyle.LED, TrafficSignalBulbType.BALL,
            TrafficSignalBulbColor.YELLOW,false),
            new TrafficSignalSectionInfo(TrafficSignalBodyColor.FLAT_BLACK,TrafficSignalBodyColor.FLAT_BLACK,TrafficSignalBodyColor.FLAT_BLACK,
            TrafficSignalVisorType.CIRCLE, TrafficSignalBulbStyle.LED, TrafficSignalBulbType.BALL,
            TrafficSignalBulbColor.GREEN,false)
            };
          })
          .build();

  public static final BlockControllableSignal VERTICAL_SOLID_SIGNAL_NO_VISORS =
      new BlockControllableSignal.Builder(
          "controllableverticalsolidsignalnovisors",
          SIGNAL_SIDE.THROUGH,
          true,
          () -> {
            return new TrafficSignalSectionInfo[] {
            new TrafficSignalSectionInfo(TrafficSignalBodyColor.FLAT_BLACK,TrafficSignalBodyColor.FLAT_BLACK,TrafficSignalBodyColor.FLAT_BLACK,
            TrafficSignalVisorType.CIRCLE, TrafficSignalBulbStyle.LED, TrafficSignalBulbType.BALL,
            TrafficSignalBulbColor.RED,false),
            new TrafficSignalSectionInfo(TrafficSignalBodyColor.FLAT_BLACK,TrafficSignalBodyColor.FLAT_BLACK,TrafficSignalBodyColor.FLAT_BLACK,
            TrafficSignalVisorType.CIRCLE, TrafficSignalBulbStyle.LED, TrafficSignalBulbType.BALL,
            TrafficSignalBulbColor.YELLOW,false),
            new TrafficSignalSectionInfo(TrafficSignalBodyColor.FLAT_BLACK,TrafficSignalBodyColor.FLAT_BLACK,TrafficSignalBodyColor.FLAT_BLACK,
            TrafficSignalVisorType.CIRCLE, TrafficSignalBulbStyle.LED, TrafficSignalBulbType.BALL,
            TrafficSignalBulbColor.GREEN,false)
            };
          })
          .build();

  public static final BlockControllableSignal VERTICAL_SOLID_SIGNAL_REVERSED =
      new BlockControllableSignal.Builder(
          "controllableverticalsolidsignalreversed",
          SIGNAL_SIDE.THROUGH,
          true,
          () -> {
            return new TrafficSignalSectionInfo[] {
            new TrafficSignalSectionInfo(TrafficSignalBodyColor.FLAT_BLACK,TrafficSignalBodyColor.FLAT_BLACK,TrafficSignalBodyColor.FLAT_BLACK,
            TrafficSignalVisorType.CIRCLE, TrafficSignalBulbStyle.LED, TrafficSignalBulbType.BALL,
            TrafficSignalBulbColor.RED,false),
            new TrafficSignalSectionInfo(TrafficSignalBodyColor.FLAT_BLACK,TrafficSignalBodyColor.FLAT_BLACK,TrafficSignalBodyColor.FLAT_BLACK,
            TrafficSignalVisorType.CIRCLE, TrafficSignalBulbStyle.LED, TrafficSignalBulbType.BALL,
            TrafficSignalBulbColor.YELLOW,false),
            new TrafficSignalSectionInfo(TrafficSignalBodyColor.FLAT_BLACK,TrafficSignalBodyColor.FLAT_BLACK,TrafficSignalBodyColor.FLAT_BLACK,
            TrafficSignalVisorType.CIRCLE, TrafficSignalBulbStyle.LED, TrafficSignalBulbType.BALL,
            TrafficSignalBulbColor.GREEN,false)
            };
          })
          .build();

  public static final BlockControllableSignal VERTICAL_UP_LEFT_ADD_ON_SIGNAL =
      new BlockControllableSignal.Builder(
          "controllableverticalupleftaddonsignal",
          SIGNAL_SIDE.LEFT,
          true,
          () -> {
            return new TrafficSignalSectionInfo[] {
            new TrafficSignalSectionInfo(TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK,
            TrafficSignalVisorType.CIRCLE, TrafficSignalBulbStyle.LED, TrafficSignalBulbType.UP_LEFT,
            TrafficSignalBulbColor.YELLOW, false),
            new TrafficSignalSectionInfo(TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK,
            TrafficSignalVisorType.CIRCLE, TrafficSignalBulbStyle.LED, TrafficSignalBulbType.UP_LEFT,
            TrafficSignalBulbColor.GREEN, false)
            };
          })
          .sectionYPositions(0, 0)
          .signalYOffset(-7.9f)
          .build();

  public static final BlockControllableSignal VERTICAL_UP_LEFT_ADD_ON_SIGNAL_GRAY =
      new BlockControllableSignal.Builder(
          "controllableverticalupleftaddonsignalgray",
          SIGNAL_SIDE.LEFT,
          true,
          () -> {
            return new TrafficSignalSectionInfo[] {
            new TrafficSignalSectionInfo(TrafficSignalBodyColor.BATTLESHIP_GRAY, TrafficSignalBodyColor.BATTLESHIP_GRAY, TrafficSignalBodyColor.BATTLESHIP_GRAY,
            TrafficSignalVisorType.CIRCLE, TrafficSignalBulbStyle.LED, TrafficSignalBulbType.UP_LEFT,
            TrafficSignalBulbColor.YELLOW, false),
            new TrafficSignalSectionInfo(TrafficSignalBodyColor.BATTLESHIP_GRAY, TrafficSignalBodyColor.BATTLESHIP_GRAY, TrafficSignalBodyColor.BATTLESHIP_GRAY,
            TrafficSignalVisorType.CIRCLE, TrafficSignalBulbStyle.LED, TrafficSignalBulbType.UP_LEFT,
            TrafficSignalBulbColor.GREEN, false)
            };
          })
          .sectionYPositions(0, 0)
          .signalYOffset(-7.9f)
          .retiring("controllableverticalupleftaddonsignal")
          .build();

  public static final BlockControllableSignal VERTICAL_UP_LEFT_SIGNAL =
      new BlockControllableSignal.Builder(
          "controllableverticalupleftsignal",
          SIGNAL_SIDE.LEFT,
          true,
          () -> {
            return new TrafficSignalSectionInfo[] {
            new TrafficSignalSectionInfo(TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK,
            TrafficSignalVisorType.CIRCLE, TrafficSignalBulbStyle.LED, TrafficSignalBulbType.UP_LEFT,
            TrafficSignalBulbColor.RED, false),
            new TrafficSignalSectionInfo(TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK,
            TrafficSignalVisorType.CIRCLE, TrafficSignalBulbStyle.LED, TrafficSignalBulbType.UP_LEFT,
            TrafficSignalBulbColor.YELLOW, false),
            new TrafficSignalSectionInfo(TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK,
            TrafficSignalVisorType.CIRCLE, TrafficSignalBulbStyle.LED, TrafficSignalBulbType.UP_LEFT,
            TrafficSignalBulbColor.GREEN, false)
            };
          })
          .build();

  public static final BlockControllableSignal VERTICAL_UP_LEFT_SIGNAL_GRAY =
      new BlockControllableSignal.Builder(
          "controllableverticalupleftsignalgray",
          SIGNAL_SIDE.LEFT,
          true,
          () -> {
            return new TrafficSignalSectionInfo[] {
            new TrafficSignalSectionInfo(TrafficSignalBodyColor.BATTLESHIP_GRAY, TrafficSignalBodyColor.BATTLESHIP_GRAY, TrafficSignalBodyColor.BATTLESHIP_GRAY,
            TrafficSignalVisorType.CIRCLE, TrafficSignalBulbStyle.LED, TrafficSignalBulbType.UP_LEFT,
            TrafficSignalBulbColor.RED, false),
            new TrafficSignalSectionInfo(TrafficSignalBodyColor.BATTLESHIP_GRAY, TrafficSignalBodyColor.BATTLESHIP_GRAY, TrafficSignalBodyColor.BATTLESHIP_GRAY,
            TrafficSignalVisorType.CIRCLE, TrafficSignalBulbStyle.LED, TrafficSignalBulbType.UP_LEFT,
            TrafficSignalBulbColor.YELLOW, false),
            new TrafficSignalSectionInfo(TrafficSignalBodyColor.BATTLESHIP_GRAY, TrafficSignalBodyColor.BATTLESHIP_GRAY, TrafficSignalBodyColor.BATTLESHIP_GRAY,
            TrafficSignalVisorType.CIRCLE, TrafficSignalBulbStyle.LED, TrafficSignalBulbType.UP_LEFT,
            TrafficSignalBulbColor.GREEN, false)
            };
          })
          .retiring("controllableverticalupleftsignal")
          .build();

  public static final BlockControllableSignal VERTICAL_UTURN_SIGNAL =
      new BlockControllableSignal.Builder(
          "controllableverticaluturnsignal",
          SIGNAL_SIDE.LEFT,
          true,
          () -> {
            return new TrafficSignalSectionInfo[] {
            new TrafficSignalSectionInfo(TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK,
            TrafficSignalVisorType.CIRCLE, TrafficSignalBulbStyle.LED, TrafficSignalBulbType.UTURN,
            TrafficSignalBulbColor.RED, false),
            new TrafficSignalSectionInfo(TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK,
            TrafficSignalVisorType.CIRCLE, TrafficSignalBulbStyle.LED, TrafficSignalBulbType.UTURN,
            TrafficSignalBulbColor.YELLOW, false),
            new TrafficSignalSectionInfo(TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK, TrafficSignalBodyColor.FLAT_BLACK,
            TrafficSignalVisorType.CIRCLE, TrafficSignalBulbStyle.LED, TrafficSignalBulbType.UTURN,
            TrafficSignalBulbColor.GREEN, false)
            };
          })
          .build();

}

package com.micatechnologies.minecraft.csm.trafficaccessories;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import org.junit.jupiter.api.Test;

class TileEntityRadarSpeedSignTest {

  @Test
  void defaultConversionMatchesTheSumSpeedHud() {
    // SUM's HudFormat prints perTick * 20 * 2.2369362920544 for mph. A sign and that HUD
    // disagreeing about the same journey is exactly the kind of discrepancy nobody tracks down,
    // so the untouched 1x setting has to reproduce it to the rounding.
    double[] blocksPerSecond = {1.0, 4.317, 5.612, 10.0, 13.4};
    for (double speed : blocksPerSecond) {
      int expected = (int) Math.round(speed * 2.2369362920544);
      assertEquals(expected, TileEntityRadarSpeedSign.toDisplayedSpeed(speed, 0),
          "at " + speed + " blocks/sec");
    }
    // The headline number: a sprint is about 12.5 mph, which is why the multiplier exists.
    assertEquals(13, TileEntityRadarSpeedSign.toDisplayedSpeed(
        TileEntityRadarSpeedSign.SPRINT_BLOCKS_PER_SECOND, 0));
  }

  @Test
  void multiplierScalesTheTrueReadingAndNothingElse() {
    double sprint = TileEntityRadarSpeedSign.SPRINT_BLOCKS_PER_SECOND;
    for (int index = 0; index < TileEntityRadarSpeedSign.MULTIPLIERS.length; index++) {
      int expected = (int) Math.round(
          sprint * 2.2369362920544 * TileEntityRadarSpeedSign.MULTIPLIERS[index]);
      assertEquals(expected, TileEntityRadarSpeedSign.toDisplayedSpeed(sprint, index),
          "at multiplier index " + index);
    }
    // 5x is the setting that makes a sprint read like a car in a 25 zone, which is the whole
    // reason the multiplier is offered.
    assertEquals(63, TileEntityRadarSpeedSign.toDisplayedSpeed(sprint, 6));
  }

  @Test
  void speedScalesLinearlyAndRoundsToNearest() {
    // Standing still is always zero rather than a rounding artefact, and a crawl still
    // registers as 1 rather than reading as dark while it is in fact being measured.
    assertEquals(0, TileEntityRadarSpeedSign.toDisplayedSpeed(0.0, 0));
    assertEquals(1, TileEntityRadarSpeedSign.toDisplayedSpeed(0.3, 0));
    assertEquals(2, TileEntityRadarSpeedSign.toDisplayedSpeed(1.0, 0));
  }

  @Test
  void multiplierIndexOutOfRangeIsClampedRatherThanThrowing() {
    // The index reaches this from NBT, so a hand-edited or corrupt value must not crash a
    // chunk load.
    assertTrue(TileEntityRadarSpeedSign.toDisplayedSpeed(5.0, -3) > 0);
    assertTrue(TileEntityRadarSpeedSign.toDisplayedSpeed(5.0, 99) > 0);
  }

  @Test
  void overLimitAndSlowDownSitEitherSideOfTheirThresholds() {
    TileEntityRadarSpeedSign sign = new TileEntityRadarSpeedSign();
    sign.setPostedSpeed(25);
    NBTTagCompound nbt = sign.writeNBT(new NBTTagCompound());

    // Exactly the posted speed is not over it; one more is.
    assertFalse(readingOf(nbt, 25).isOverLimit());
    assertTrue(readingOf(nbt, 26).isOverLimit());

    // SLOW DOWN starts strictly above posted + margin, so posted + margin still shows a number.
    int margin = TileEntityRadarSpeedSign.SLOW_DOWN_MARGIN;
    assertFalse(readingOf(nbt, 25 + margin).isSlowDown());
    assertTrue(readingOf(nbt, 25 + margin + 1).isSlowDown());
    assertTrue(readingOf(nbt, 25 + margin).isOverLimit());
  }

  /** Builds a sign holding a given reading, which is otherwise only reachable by ticking. */
  private static TileEntityRadarSpeedSign readingOf(NBTTagCompound base, int reading) {
    NBTTagCompound nbt = base.copy();
    nbt.setInteger("rdg", reading);
    TileEntityRadarSpeedSign sign = new TileEntityRadarSpeedSign();
    sign.readNBT(nbt);
    return sign;
  }

  @Test
  void defaultZoneReachesOutInFrontForEveryFacing() {
    // EnumFacing.byHorizontalIndex(0) is SOUTH, not north, and a sign whose zone pointed the
    // wrong way would only ever read vehicles that had already gone past it.
    BlockPos pos = new BlockPos(100, 64, 200);
    for (EnumFacing facing : EnumFacing.HORIZONTALS) {
      AxisAlignedBB zone = TileEntityRadarSpeedSign.defaultScanZone(pos, facing);
      assertNotNull(zone);
      BlockPos ahead = pos.offset(facing, 8);
      assertTrue(zone.contains(new net.minecraft.util.math.Vec3d(
              ahead.getX() + 0.5, ahead.getY() + 0.5, ahead.getZ() + 0.5)),
          "8 blocks ahead should be inside the zone facing " + facing);
      BlockPos behind = pos.offset(facing.getOpposite(), 4);
      assertFalse(zone.contains(new net.minecraft.util.math.Vec3d(
              behind.getX() + 0.5, behind.getY() + 0.5, behind.getZ() + 0.5)),
          "4 blocks behind should be outside the zone facing " + facing);
    }
  }

  @Test
  void nbtRoundTripsEverySetting() {
    TileEntityRadarSpeedSign sign = new TileEntityRadarSpeedSign();
    sign.setPostedSpeed(45);
    sign.setMultiplierIndex(5);
    sign.setScaleIndex(3);
    sign.setShowHeader(true);
    sign.setFaceColor(MutcdSignFaceColor.FLUORESCENT_YELLOW_GREEN);
    sign.setZoneCorners(new BlockPos(1, 2, 3), new BlockPos(9, 8, 7));

    TileEntityRadarSpeedSign restored = new TileEntityRadarSpeedSign();
    restored.readNBT(sign.writeNBT(new NBTTagCompound()));

    assertEquals(45, restored.getPostedSpeed());
    assertEquals(5, restored.getMultiplierIndex());
    assertEquals(3, restored.getScaleIndex());
    assertTrue(restored.isShowHeader());
    assertEquals(MutcdSignFaceColor.FLUORESCENT_YELLOW_GREEN, restored.getFaceColor());
    assertTrue(restored.hasCustomZone());
  }

  @Test
  void emptyCompoundProducesUsableDefaults() {
    // A posted speed of zero would be the give-away that an absent tag had been taken
    // literally. The multiplier defaults to 1x, true to the SUM HUD, and is checked by value
    // rather than by index so a reordering of the list cannot silently change it.
    TileEntityRadarSpeedSign sign = new TileEntityRadarSpeedSign();
    sign.readNBT(new NBTTagCompound());
    assertEquals(25, sign.getPostedSpeed());
    assertEquals(1.0, TileEntityRadarSpeedSign.MULTIPLIERS[sign.getMultiplierIndex()]);
    assertEquals(MutcdSignFaceColor.YELLOW, sign.getFaceColor());
    assertFalse(sign.isShowHeader());
    assertFalse(sign.hasCustomZone());
    assertEquals(0, sign.getReading());
    assertTrue(sign.getScale() > 0.0f);
  }

  @Test
  void bothFaceColoursSurviveAWriteAndAnAbsentTagFallsBackToYellow() {
    // Fluorescent yellow-green is ordinal 1, so an absent tag reading as zero happens to be
    // correct here -- but only by luck, and the assertion is what keeps it correct if the
    // order ever changes.
    for (MutcdSignFaceColor colour : MutcdSignFaceColor.values()) {
      TileEntityRadarSpeedSign sign = new TileEntityRadarSpeedSign();
      sign.setFaceColor(colour);
      TileEntityRadarSpeedSign restored = new TileEntityRadarSpeedSign();
      restored.readNBT(sign.writeNBT(new NBTTagCompound()));
      assertEquals(colour, restored.getFaceColor());
    }
    assertEquals(MutcdSignFaceColor.YELLOW, MutcdSignFaceColor.fromNBT(-1));
    assertEquals(MutcdSignFaceColor.YELLOW, MutcdSignFaceColor.fromNBT(99));
  }

  @Test
  void postedSpeedIsHeldInsideItsRange() {
    TileEntityRadarSpeedSign sign = new TileEntityRadarSpeedSign();
    sign.setPostedSpeed(1000);
    assertEquals(TileEntityRadarSpeedSign.MAX_SPEED, sign.getPostedSpeed());
    sign.setPostedSpeed(-5);
    assertEquals(TileEntityRadarSpeedSign.MIN_SPEED, sign.getPostedSpeed());
  }

  @Test
  void clearingTheZoneReturnsTheSignToItsDefault() {
    TileEntityRadarSpeedSign sign = new TileEntityRadarSpeedSign();
    assertFalse(sign.hasCustomZone());
    assertFalse(sign.setZoneCorners(new BlockPos(0, 0, 0), new BlockPos(4, 4, 4)),
        "the first zone replaces nothing");
    assertTrue(sign.hasCustomZone());
    assertTrue(sign.setZoneCorners(new BlockPos(1, 1, 1), new BlockPos(5, 5, 5)),
        "a second zone reports that it replaced one");
    sign.clearZoneCorners();
    assertFalse(sign.hasCustomZone());
  }
}

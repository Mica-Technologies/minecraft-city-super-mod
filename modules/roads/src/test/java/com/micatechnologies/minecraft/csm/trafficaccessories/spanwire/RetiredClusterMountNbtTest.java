package com.micatechnologies.minecraft.csm.trafficaccessories.spanwire;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.micatechnologies.minecraft.csm.trafficsignals.logic.TrafficSignalBodyColor;
import java.util.Arrays;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.BlockPos;
import org.junit.jupiter.api.Test;

/**
 * A retired span wire cluster mount becomes a signal cluster hanger on a random tick, and the
 * hanger reads the old tile entity's data after it has been through
 * {@link TileEntitySignalClusterHanger#fromRetiredClusterMountNBT}. What a builder set up on the
 * old mount -- the span and its tether, the mount and coil styles -- has to come through intact.
 */
class RetiredClusterMountNbtTest {

  private static final String MOUNT_STYLE_KEY = "swMS";
  private static final String COIL_STYLE_KEY = "swCS";
  private static final String COLOR_KEY = "scC";

  private static SpanWireDefinition boxSpan() {
    return new SpanWireDefinition(
        new BlockPos(100, 70, 40),
        new BlockPos(120, 70, 40),
        Arrays.asList(new BlockPos(106, 69, 40), new BlockPos(110, 69, 40)),
        SpanWireCatenary.DEFAULT_SLACK)
        .withBoxSpan(true)
        .withTetherClearance(2.25);
  }

  private static NBTTagCompound oldMount(SpanWireDefinition span) {
    final NBTTagCompound compound = span.writeToNBT(new NBTTagCompound());
    compound.setInteger(MOUNT_STYLE_KEY, 1);
    compound.setInteger(COIL_STYLE_KEY, 2);
    compound.setInteger(TileEntitySignalClusterHanger.RETIRED_WIDTH_KEY, 4);
    return compound;
  }

  @Test
  void keepsTheSpanAndItsTether() {
    final SpanWireDefinition span = boxSpan();
    final NBTTagCompound converted =
        TileEntitySignalClusterHanger.fromRetiredClusterMountNBT(oldMount(span));

    final SpanWireDefinition restored = SpanWireDefinition.readFromNBT(converted);
    assertNotNull(restored);
    assertEquals(span, restored);
    assertEquals(span.getHangers(), restored.getHangers());
    assertTrue(restored.isBoxSpan());
    assertEquals(span.getTetherClearance(), restored.getTetherClearance(), 1.0e-9);
  }

  @Test
  void keepsTheMountAndCoilStyles() {
    final NBTTagCompound converted =
        TileEntitySignalClusterHanger.fromRetiredClusterMountNBT(oldMount(boxSpan()));
    assertEquals(1, converted.getInteger(MOUNT_STYLE_KEY));
    assertEquals(2, converted.getInteger(COIL_STYLE_KEY));
  }

  @Test
  void dropsTheWidthAndPaintsTheOldSteel() {
    final NBTTagCompound converted =
        TileEntitySignalClusterHanger.fromRetiredClusterMountNBT(oldMount(boxSpan()));
    assertFalse(converted.hasKey(TileEntitySignalClusterHanger.RETIRED_WIDTH_KEY));
    assertEquals(TrafficSignalBodyColor.BATTLESHIP_GRAY,
        TrafficSignalBodyColor.fromNBT(converted.getInteger(COLOR_KEY)));
    assertEquals(TrafficSignalBodyColor.BATTLESHIP_GRAY,
        TileEntitySignalClusterHanger.RETIRED_CLUSTER_MOUNT_COLOR);
  }

  @Test
  void leavesTheOldCompoundAlone() {
    final NBTTagCompound old = oldMount(boxSpan());
    final NBTTagCompound before = old.copy();
    TileEntitySignalClusterHanger.fromRetiredClusterMountNBT(old);
    assertEquals(before, old);
  }

  @Test
  void anUnlinkedMountStillConverts() {
    final NBTTagCompound old = new NBTTagCompound();
    old.setInteger(TileEntitySignalClusterHanger.RETIRED_WIDTH_KEY, 3);
    final NBTTagCompound converted = TileEntitySignalClusterHanger.fromRetiredClusterMountNBT(old);
    assertEquals(null, SpanWireDefinition.readFromNBT(converted));
    assertFalse(converted.hasKey(TileEntitySignalClusterHanger.RETIRED_WIDTH_KEY));
    assertTrue(converted.hasKey(COLOR_KEY));
  }

  @Test
  void aColourAlreadySetIsKept() {
    final NBTTagCompound old = oldMount(boxSpan());
    old.setInteger(COLOR_KEY, TrafficSignalBodyColor.MAROON.toNBT());
    final NBTTagCompound converted = TileEntitySignalClusterHanger.fromRetiredClusterMountNBT(old);
    assertEquals(TrafficSignalBodyColor.MAROON.toNBT(), converted.getInteger(COLOR_KEY));
  }
}

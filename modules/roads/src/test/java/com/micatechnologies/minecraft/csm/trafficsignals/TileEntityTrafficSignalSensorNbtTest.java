package com.micatechnologies.minecraft.csm.trafficsignals;

import static org.junit.jupiter.api.Assertions.*;

import com.micatechnologies.minecraft.csm.trafficsignals.TileEntityTrafficSignalSensor.ZoneType;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.JsonToNBT;
import net.minecraft.nbt.NBTException;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import org.junit.jupiter.api.Test;

class TileEntityTrafficSignalSensorNbtTest {

  private static final String[] EXTRA_KEYS = {"mx", "lx", "px", "rx"};

  @Test
  void legacyKeysAreMigrated() {
    BlockPos corner1 = new BlockPos(10, 64, -20);
    BlockPos corner2 = new BlockPos(20, 70, -10);
    BlockPos left1 = new BlockPos(30, 64, -40);
    BlockPos left2 = new BlockPos(40, 70, -30);

    NBTTagCompound legacy = new NBTTagCompound();
    legacy.setLong("blockPos1", corner1.toLong());
    legacy.setLong("blockPos2", corner2.toLong());
    legacy.setLong("leftBlockPos1", left1.toLong());
    legacy.setLong("leftBlockPos2", left2.toLong());
    legacy.setLong("protectedBlockPos1", corner1.toLong());
    legacy.setLong("protectedBlockPos2", corner2.toLong());
    legacy.setLong("rightBlockPos1", left1.toLong());
    legacy.setLong("rightBlockPos2", left2.toLong());

    TileEntityTrafficSignalSensor te = new TileEntityTrafficSignalSensor();
    te.readNBT(legacy);

    NBTTagCompound output = te.writeNBT(new NBTTagCompound());

    // Short keys present
    assertEquals(corner1.toLong(), output.getLong("m1"));
    assertEquals(corner2.toLong(), output.getLong("m2"));
    assertEquals(left1.toLong(), output.getLong("l1"));
    assertEquals(left2.toLong(), output.getLong("l2"));
    assertEquals(corner1.toLong(), output.getLong("p1"));
    assertEquals(corner2.toLong(), output.getLong("p2"));
    assertEquals(left1.toLong(), output.getLong("r1"));
    assertEquals(left2.toLong(), output.getLong("r2"));

    // Legacy keys absent
    assertFalse(output.hasKey("blockPos1"));
    assertFalse(output.hasKey("blockPos2"));
    assertFalse(output.hasKey("leftBlockPos1"));
    assertFalse(output.hasKey("leftBlockPos2"));
    assertFalse(output.hasKey("protectedBlockPos1"));
    assertFalse(output.hasKey("protectedBlockPos2"));
    assertFalse(output.hasKey("rightBlockPos1"));
    assertFalse(output.hasKey("rightBlockPos2"));

    // A legacy sensor is a one-box sensor: one box per type, the same corners, no list tags
    assertBoxes(te, ZoneType.STANDARD, corner1, corner2);
    assertBoxes(te, ZoneType.LEFT, left1, left2);
    assertBoxes(te, ZoneType.PROTECTED, corner1, corner2);
    assertBoxes(te, ZoneType.RIGHT, left1, left2);
    assertNoExtraKeys(output);
  }

  @Test
  void shortKeyRoundTrip() {
    BlockPos pos = new BlockPos(100, 64, 200);

    NBTTagCompound input = new NBTTagCompound();
    input.setLong("m1", pos.toLong());
    input.setLong("m2", pos.toLong());

    TileEntityTrafficSignalSensor te = new TileEntityTrafficSignalSensor();
    te.readNBT(input);

    NBTTagCompound output = te.writeNBT(new NBTTagCompound());

    assertEquals(pos.toLong(), output.getLong("m1"));
    assertEquals(pos.toLong(), output.getLong("m2"));
  }

  @Test
  void emptyCompoundProducesNullPositions() {
    TileEntityTrafficSignalSensor te = new TileEntityTrafficSignalSensor();
    te.readNBT(new NBTTagCompound());

    NBTTagCompound output = te.writeNBT(new NBTTagCompound());

    assertFalse(output.hasKey("m1"), "Null positions should not be written");
    assertNoExtraKeys(output);
    for (ZoneType type : ZoneType.values()) {
      assertEquals(0, te.getBoxCount(type));
    }
    assertFalse(te.hasStandardZone());
  }

  // --- Regression: a one-box sensor saves and scans exactly as before multi-box zones ---

  /** What the single-box writeNBT wrote: the eight corner longs and nothing else. */
  private static NBTTagCompound oldFormatOneBoxPerType() {
    NBTTagCompound old = new NBTTagCompound();
    old.setLong("m1", new BlockPos(1, 64, 2).toLong());
    old.setLong("m2", new BlockPos(5, 68, 9).toLong());
    old.setLong("l1", new BlockPos(-3, 64, 2).toLong());
    old.setLong("l2", new BlockPos(-1, 68, 9).toLong());
    old.setLong("p1", new BlockPos(7, 64, 2).toLong());
    old.setLong("p2", new BlockPos(8, 68, 9).toLong());
    old.setLong("r1", new BlockPos(9, 64, 2).toLong());
    old.setLong("r2", new BlockPos(10, 68, 9).toLong());
    return old;
  }

  @Test
  void oneBoxSensorWritesByteIdenticalNbt() throws IOException {
    NBTTagCompound old = oldFormatOneBoxPerType();
    TileEntityTrafficSignalSensor te = new TileEntityTrafficSignalSensor();
    te.readNBT(old.copy());
    NBTTagCompound output = te.writeNBT(new NBTTagCompound());

    assertEquals(old, output);
    assertArrayEquals(bytes(old), bytes(output));
    assertNoExtraKeys(output);
  }

  @Test
  void oneBoxSensorBuiltThroughTheApiWritesTheOldFormat() throws IOException {
    TileEntityTrafficSignalSensor te = new TileEntityTrafficSignalSensor();
    te.setScanCorners(new BlockPos(1, 64, 2), new BlockPos(5, 68, 9));
    te.setLeftScanCorners(new BlockPos(-3, 64, 2), new BlockPos(-1, 68, 9));
    te.setProtectedScanCorners(new BlockPos(7, 64, 2), new BlockPos(8, 68, 9));
    te.setRightScanCorners(new BlockPos(9, 64, 2), new BlockPos(10, 68, 9));

    assertArrayEquals(bytes(oldFormatOneBoxPerType()), bytes(te.writeNBT(new NBTTagCompound())));
  }

  @Test
  void oneBoxScanQueriesTheSameBoxOnceAsTheOldCode() {
    NBTTagCompound old = oldFormatOneBoxPerType();
    TileEntityTrafficSignalSensor te = new TileEntityTrafficSignalSensor();
    te.readNBT(old.copy());

    String[][] keys = {{"m1", "m2"}, {"l1", "l2"}, {"p1", "p2"}, {"r1", "r2"}};
    ZoneType[] types = {ZoneType.STANDARD, ZoneType.LEFT, ZoneType.PROTECTED, ZoneType.RIGHT};
    for (int i = 0; i < types.length; i++) {
      // The old scan: new AxisAlignedBB(corner1, corner2), one query, its size
      AxisAlignedBB oldBox = new AxisAlignedBB(BlockPos.fromLong(old.getLong(keys[i][0])),
          BlockPos.fromLong(old.getLong(keys[i][1])));
      AxisAlignedBB[] boxes = te.getScanBoxes(types[i]);
      assertEquals(1, boxes.length);
      assertEquals(oldBox, boxes[0]);

      List<AxisAlignedBB> queried = new ArrayList<>();
      int count = TileEntityTrafficSignalSensor.countDistinct(boxes, box -> {
        queried.add(box);
        return Arrays.asList(1, 2, 2, 3); // a list size, as getEntitiesWithinAABB returns
      });
      assertEquals(4, count, "one box counts the query's size, as the old code did");
      assertEquals(Collections.singletonList(oldBox), queried);
    }
  }

  @Test
  void noBoxesCountsZeroWithoutQuerying() {
    assertEquals(0, TileEntityTrafficSignalSensor.countDistinct(new AxisAlignedBB[0],
        box -> fail("no query expected")));
  }

  // --- Multi-box zones ---

  @Test
  void multiBoxCountsDistinctResults() {
    AxisAlignedBB a = new AxisAlignedBB(0, 0, 0, 4, 4, 4);
    AxisAlignedBB b = new AxisAlignedBB(3, 0, 0, 8, 4, 4);
    int count = TileEntityTrafficSignalSensor.countDistinct(new AxisAlignedBB[]{a, b},
        box -> box == a ? Arrays.asList(1, 2) : Arrays.asList(2, 3));
    assertEquals(3, count, "the entity in both boxes counts once");
  }

  @Test
  void multiBoxRoundTrip() throws NBTException {
    TileEntityTrafficSignalSensor te = new TileEntityTrafficSignalSensor();
    assertEquals(1, te.addBox(ZoneType.STANDARD, new BlockPos(1, 64, 2), new BlockPos(5, 68, 9)));
    assertEquals(2, te.addBox(ZoneType.STANDARD, new BlockPos(10, 64, 20), new BlockPos(14, 68,
        30)));
    assertEquals(3, te.addBox(ZoneType.STANDARD, new BlockPos(-4, 60, -4), new BlockPos(-1, 64,
        -1)));
    assertEquals(1, te.addBox(ZoneType.RIGHT, new BlockPos(0, 0, 0), new BlockPos(1, 1, 1)));

    NBTTagCompound output = te.writeNBT(new NBTTagCompound());
    assertEquals(new BlockPos(1, 64, 2).toLong(), output.getLong("m1"));
    assertEquals(new BlockPos(5, 68, 9).toLong(), output.getLong("m2"));
    NBTTagList mx = output.getTagList("mx", 11);
    assertEquals(2, mx.tagCount());
    assertArrayEquals(new int[]{10, 64, 20, 14, 68, 30}, mx.getIntArrayAt(0));
    assertArrayEquals(new int[]{-4, 60, -4, -1, 64, -1}, mx.getIntArrayAt(1));
    assertFalse(output.hasKey("rx"), "a one-box type writes no list");
    assertFalse(output.hasKey("lx"));
    assertFalse(output.hasKey("l1"));

    TileEntityTrafficSignalSensor read = new TileEntityTrafficSignalSensor();
    read.readNBT(output.copy());
    assertEquals(3, read.getBoxCount(ZoneType.STANDARD));
    assertEquals(1, read.getBoxCount(ZoneType.RIGHT));
    assertEquals(0, read.getBoxCount(ZoneType.LEFT));
    assertEquals(output, read.writeNBT(new NBTTagCompound()));

    // The documented /blockdata shape adds a second standard box
    NBTTagCompound blockData = JsonToNBT.getTagFromJson(
        "{m1:" + new BlockPos(1, 64, 2).toLong() + "L,m2:" + new BlockPos(5, 68, 9).toLong()
            + "L,mx:[[I;10,64,20,14,68,30]]}");
    TileEntityTrafficSignalSensor fromCommand = new TileEntityTrafficSignalSensor();
    fromCommand.readNBT(blockData);
    assertBoxes(fromCommand, ZoneType.STANDARD, new BlockPos(1, 64, 2), new BlockPos(5, 68, 9),
        new BlockPos(10, 64, 20), new BlockPos(14, 68, 30));
  }

  @Test
  void listWithoutBoxZeroCompactsToBoxZero() {
    NBTTagCompound input = new NBTTagCompound();
    NBTTagList lx = new NBTTagList();
    lx.appendTag(new net.minecraft.nbt.NBTTagIntArray(new int[]{1, 2, 3, 4, 5, 6}));
    lx.appendTag(new net.minecraft.nbt.NBTTagIntArray(new int[]{1, 2})); // malformed, skipped
    input.setTag("lx", lx);

    TileEntityTrafficSignalSensor te = new TileEntityTrafficSignalSensor();
    te.readNBT(input);
    assertBoxes(te, ZoneType.LEFT, new BlockPos(1, 2, 3), new BlockPos(4, 5, 6));

    NBTTagCompound output = te.writeNBT(new NBTTagCompound());
    assertEquals(new BlockPos(1, 2, 3).toLong(), output.getLong("l1"));
    assertFalse(output.hasKey("lx"));
  }

  @Test
  void capDuplicateRemoveAndClear() {
    TileEntityTrafficSignalSensor te = new TileEntityTrafficSignalSensor();
    for (int i = 0; i < TileEntityTrafficSignalSensor.MAX_BOXES_PER_ZONE; i++) {
      assertEquals(i + 1, te.addBox(ZoneType.LEFT, new BlockPos(i, 0, 0), new BlockPos(i, 4, 4)));
    }
    assertEquals(TileEntityTrafficSignalSensor.ADD_REFUSED_FULL,
        te.addBox(ZoneType.LEFT, new BlockPos(50, 0, 0), new BlockPos(51, 4, 4)));
    // Same extent with the corners swapped is the same box
    assertEquals(TileEntityTrafficSignalSensor.ADD_REFUSED_DUPLICATE,
        te.addBox(ZoneType.LEFT, new BlockPos(0, 4, 4), new BlockPos(0, 0, 0)));

    assertTrue(te.removeLastBox(ZoneType.LEFT));
    assertEquals(TileEntityTrafficSignalSensor.MAX_BOXES_PER_ZONE - 1,
        te.getBoxCount(ZoneType.LEFT));
    assertEquals(new BlockPos(6, 0, 0), te.getBoxes(ZoneType.LEFT).get(6)[0]);

    te.clearLeftZone();
    assertFalse(te.hasLeftZone());
    assertFalse(te.removeLastBox(ZoneType.LEFT));

    te.addBox(ZoneType.STANDARD, new BlockPos(0, 0, 0), new BlockPos(1, 1, 1));
    te.addBox(ZoneType.PROTECTED, new BlockPos(0, 0, 0), new BlockPos(1, 1, 1));
    te.clearAllZones();
    for (ZoneType type : ZoneType.values()) {
      assertEquals(0, te.getBoxCount(type));
    }
  }

  @Test
  void readCapsTheBoxCount() {
    NBTTagCompound input = new NBTTagCompound();
    NBTTagList px = new NBTTagList();
    for (int i = 0; i < 20; i++) {
      px.appendTag(new net.minecraft.nbt.NBTTagIntArray(new int[]{i, 0, 0, i, 1, 1}));
    }
    input.setTag("px", px);
    TileEntityTrafficSignalSensor te = new TileEntityTrafficSignalSensor();
    te.readNBT(input);
    assertEquals(TileEntityTrafficSignalSensor.MAX_BOXES_PER_ZONE,
        te.getBoxCount(ZoneType.PROTECTED));
  }

  private static void assertBoxes(TileEntityTrafficSignalSensor te, ZoneType type,
      BlockPos... corners) {
    List<BlockPos[]> boxes = te.getBoxes(type);
    assertEquals(corners.length / 2, boxes.size(), type.name());
    for (int i = 0; i < boxes.size(); i++) {
      assertEquals(corners[i * 2], boxes.get(i)[0]);
      assertEquals(corners[i * 2 + 1], boxes.get(i)[1]);
    }
  }

  private static void assertNoExtraKeys(NBTTagCompound output) {
    for (String key : EXTRA_KEYS) {
      assertFalse(output.hasKey(key), key);
    }
  }

  private static byte[] bytes(NBTTagCompound compound) throws IOException {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    CompressedStreamTools.write(compound, new DataOutputStream(out));
    return out.toByteArray();
  }
}

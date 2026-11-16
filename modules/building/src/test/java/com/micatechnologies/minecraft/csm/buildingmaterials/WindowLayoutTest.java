package com.micatechnologies.minecraft.csm.buildingmaterials;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import net.minecraft.nbt.NBTTagCompound;
import org.junit.jupiter.api.Test;

/**
 * The glazing window's layout over stacks, its packed shape and its stored settings.
 */
class WindowLayoutTest {

  private static WindowLayout.Shape shape(int pattern, int height, int index) {
    WindowLayout.Shape s = new WindowLayout.Shape();
    s.endW = WindowLayout.END_SOLID;
    s.endE = WindowLayout.END_SOLID;
    s.pattern = pattern;
    s.height = height;
    s.index = index;
    return s;
  }

  private static boolean any(List<WindowLayout.Box> boxes, Predicate<WindowLayout.Box> p) {
    return boxes.stream().anyMatch(p);
  }

  @Test
  void meetingRailIsHalfwayUpTheOpening() {
    assertEquals(8f, WindowLayout.meetingRail(1), 1e-4);
    assertEquals(16f, WindowLayout.meetingRail(2), 1e-4);   // the joint of a 2-tall stack
    assertEquals(24f, WindowLayout.meetingRail(3), 1e-4);   // the middle of the middle block
  }

  @Test
  void twoTallDoubleHungIsOneSashABlock() {
    // the lower sash, on the inner track (z > 8), is all in the bottom block; the upper, on the
    // outer track, all in the top one
    List<WindowLayout.Box> bottom = WindowLayout.boxes(shape(WindowLayout.HUNG_1_1, 2, 0));
    List<WindowLayout.Box> top = WindowLayout.boxes(shape(WindowLayout.HUNG_1_1, 2, 1));
    assertTrue(any(bottom, b -> b.part == WindowLayout.GLASS && b.z0 > 8));
    assertFalse(any(bottom, b -> b.part == WindowLayout.GLASS && b.z1 < 8));
    assertTrue(any(top, b -> b.part == WindowLayout.GLASS && b.z1 < 8));
    assertFalse(any(top, b -> b.part == WindowLayout.GLASS && b.z0 > 8));
  }

  @Test
  void threeTallDoubleHungMeetsInTheMiddleBlock() {
    List<WindowLayout.Box> middle = WindowLayout.boxes(shape(WindowLayout.HUNG_1_1, 3, 1));
    // both sashes' glass reach into it, and a meeting rail crosses it at y 8
    assertTrue(any(middle, b -> b.part == WindowLayout.GLASS && b.z0 > 8));
    assertTrue(any(middle, b -> b.part == WindowLayout.GLASS && b.z1 < 8));
    assertTrue(any(middle, b -> b.part == WindowLayout.FRAME && b.y0 <= 8 && b.y1 >= 8
        && b.x1 - b.x0 > 10 && b.y1 - b.y0 < 2));
  }

  @Test
  void sillAndHeadOnlyAtTheStacksEnds() {
    for (int h = 1; h <= 3; h++) {
      for (int i = 0; i < h; i++) {
        WindowLayout.Shape s = shape(WindowLayout.HUNG_6_6, h, i);
        s.sill = WindowLayout.SILL_STONE;
        s.head = WindowLayout.HEAD_KEYSTONE;
        List<WindowLayout.Box> boxes = WindowLayout.boxes(s);
        boolean stone = any(boxes, b -> b.part == WindowLayout.STONE && b.y0 <= 0);
        assertEquals(i == 0, stone, "sill in block " + i + " of " + h);
        boolean keystone = any(boxes, b -> b.part == WindowLayout.STONE && b.y1 > 16);
        assertEquals(i == h - 1, keystone, "keystone in block " + i + " of " + h);
      }
    }
  }

  @Test
  void openLowerSashSlidesUpAndLeavesTheFootClear() {
    WindowLayout.Shape s = shape(WindowLayout.HUNG_1_1, 2, 0);
    s.open = true;
    // the bottom block of an open 2-tall double-hung has no glass: its sash is up in the top one
    assertFalse(any(WindowLayout.boxes(s), b -> b.part == WindowLayout.GLASS));
    s.index = 1;
    List<WindowLayout.Box> top = WindowLayout.boxes(s);
    assertTrue(any(top, b -> b.part == WindowLayout.GLASS && b.z0 > 8));
    assertTrue(any(top, b -> b.part == WindowLayout.GLASS && b.z1 < 8));
  }

  @Test
  void openWindowCollidesLikeAClosedOne() {
    WindowLayout.Shape s = shape(WindowLayout.CASEMENT, 1, 0);
    String closed = describe(WindowLayout.collision(s));
    s.open = true;
    assertEquals(closed, describe(WindowLayout.collision(s)));
  }

  private static String describe(List<float[]> boxes) {
    StringBuilder b = new StringBuilder();
    for (float[] f : boxes) {
      b.append(java.util.Arrays.toString(f));
    }
    return b.toString();
  }

  @Test
  void plainJoinedWindowsHaveNoMullion() {
    WindowLayout.Shape s = shape(WindowLayout.PLAIN, 1, 0);
    s.endW = WindowLayout.END_JOIN;
    s.endE = WindowLayout.END_JOIN;
    // only the sill and head rails: nothing upright
    assertFalse(any(WindowLayout.boxes(s), b -> b.part == WindowLayout.FRAME
        && b.y1 - b.y0 > 8));
  }

  @Test
  void patternedJoinedWindowsShareOneMullion() {
    WindowLayout.Shape s = shape(WindowLayout.HUNG_1_1, 1, 0);
    s.endW = WindowLayout.END_JOIN;
    List<WindowLayout.Box> boxes = WindowLayout.boxes(s);
    // half a mullion, 0.75 wide, at the joined (west) edge
    assertTrue(any(boxes, b -> b.part == WindowLayout.FRAME && b.x0 == 0 && b.x1 == 0.75f));
  }

  @Test
  void depthMovesTheGlassPlane() {
    for (int d = 0; d < WindowLayout.DEPTHS; d++) {
      WindowLayout.Shape s = shape(WindowLayout.PLAIN, 1, 0);
      s.depth = d;
      float plane = WindowLayout.planeOf(d);
      assertTrue(any(WindowLayout.boxes(s), b -> b.part == WindowLayout.GLASS
          && b.z0 < plane && b.z1 > plane));
    }
  }

  @Test
  void outsideCornerTurnsBackToTheWallWithOnePost() {
    WindowLayout.Shape s = shape(WindowLayout.PLAIN, 1, 0);
    s.depth = WindowLayout.FLUSH;
    s.endW = WindowLayout.END_FREE;
    s.endE = WindowLayout.END_JOIN;
    s.crossS = WindowLayout.CROSS_SOLID;
    List<WindowLayout.Box> boxes = WindowLayout.boxes(s);
    // the side glass runs along the west face (x = 1.5) all the way to the south edge
    assertTrue(any(boxes, b -> b.part == WindowLayout.GLASS && b.x0 < 1.5f && b.x1 > 1.5f
        && b.z1 >= 16f));
    // the front glass from the post to the east edge
    assertTrue(any(boxes, b -> b.part == WindowLayout.GLASS && b.z0 < 1.5f && b.z1 > 1.5f
        && b.x1 >= 16f && b.x0 >= 1.4f));
  }

  @Test
  void everyBoxStaysInsideTheModelLimits() {
    for (int canted = 0; canted < 2; canted++) {
      for (int p = 0; p < WindowLayout.PATTERNS; p++) {
        for (int d = 0; d < WindowLayout.DEPTHS; d++) {
          for (int open = 0; open < 2; open++) {
            WindowLayout.Shape s = shape(p, 1, 0);
            s.canted = canted == 1;
            s.depth = d;
            s.open = open == 1;
            s.arch = true;
            s.sill = WindowLayout.SILL_STONE;
            s.head = WindowLayout.HEAD_KEYSTONE;
            s.solidAbove = true;
            s.solidBelow = true;
            for (WindowLayout.Box b : WindowLayout.boxes(s)) {
              for (float v : new float[]{b.x0, b.y0, b.z0, b.x1, b.y1, b.z1}) {
                assertTrue(v >= -16 && v <= 32, b.toString());
              }
              assertTrue(s.canted || !b.rotated, b.toString());
            }
          }
        }
      }
    }
  }

  @Test
  void cantedGlassIsTurnedAboutTheDiagonalsMiddle() {
    WindowLayout.Shape s = shape(WindowLayout.PLAIN, 1, 0);
    s.canted = true;
    s.depth = WindowLayout.CENTRED;
    List<WindowLayout.Box> boxes = WindowLayout.boxes(s);
    WindowLayout.Box glass = boxes.stream().filter(b -> b.part == WindowLayout.GLASS)
        .findFirst().orElseThrow(AssertionError::new);
    assertTrue(glass.rotated);
    assertEquals(12f, glass.ox, 1e-4);   // the middle of (16, 8) and (8, 16)
    assertEquals(12f, glass.oz, 1e-4);
    assertEquals(8 * Math.sqrt(2), glass.x1 - glass.x0, 1e-3);
  }

  @Test
  void packedShapeRoundTripsAndDiffers() {
    Set<Long> keys = new HashSet<>();
    for (int p = 0; p < WindowLayout.PATTERNS; p++) {
      for (int h = 1; h <= WindowLayout.MAX_STACK; h++) {
        WindowLayout.Shape s = shape(p, h, h - 1);
        s.facing = h & 3;
        s.depth = p % 3;
        s.open = (h & 1) == 1;
        s.arch = true;
        s.sill = 2;
        s.head = 1;
        s.crossS = WindowLayout.CROSS_JOIN;
        s.solidAbove = true;
        long k = s.pack();
        assertEquals(k, WindowLayout.Shape.unpack(k).pack());
        assertTrue(keys.add(k));
      }
    }
  }

  @Test
  void settingsDefaultWhenAbsentAndRoundTrip() {
    TileEntityGlazingWindow te = new TileEntityGlazingWindow();
    te.readNBT(new NBTTagCompound());
    assertEquals(WindowLayout.PLAIN, te.getPattern());
    assertFalse(te.isOpen());
    assertFalse(te.isArch());
    assertEquals(WindowLayout.SILL_NONE, te.getSill());
    assertEquals(WindowLayout.HEAD_NONE, te.getHead());
    long plain = te.settingsKey();

    te.setPattern(WindowLayout.HUNG_6_6);
    te.setOpen(true);
    te.setArch(true);
    te.setSill(WindowLayout.SILL_FRAME);
    te.setHead(WindowLayout.HEAD_KEYSTONE);
    NBTTagCompound nbt = te.writeNBT(new NBTTagCompound());
    assertEquals(5, nbt.getKeySet().size());
    TileEntityGlazingWindow read = new TileEntityGlazingWindow();
    read.readNBT(nbt);
    assertEquals(WindowLayout.HUNG_6_6, read.getPattern());
    assertTrue(read.isOpen());
    assertTrue(read.isArch());
    assertEquals(WindowLayout.SILL_FRAME, read.getSill());
    assertEquals(WindowLayout.HEAD_KEYSTONE, read.getHead());
    assertEquals(te.settingsKey(), read.settingsKey());
    assertNotEquals(plain, read.settingsKey());
  }

  @Test
  void outOfRangeValuesReadAsDefaults() {
    NBTTagCompound nbt = new NBTTagCompound();
    nbt.setByte(TileEntityGlazingWindow.KEY_PATTERN, (byte) 9);
    nbt.setByte(TileEntityGlazingWindow.KEY_SILL, (byte) -1);
    TileEntityGlazingWindow te = new TileEntityGlazingWindow();
    te.readNBT(nbt);
    assertEquals(WindowLayout.PLAIN, te.getPattern());
    assertEquals(WindowLayout.SILL_NONE, te.getSill());
  }

  @Test
  void everySettingChangesTheModelKey() {
    Set<Long> keys = new HashSet<>();
    TileEntityGlazingWindow te = new TileEntityGlazingWindow();
    for (int p = 0; p < WindowLayout.PATTERNS; p++) {
      for (int o = 0; o < 2; o++) {
        for (int a = 0; a < 2; a++) {
          for (int sl = 0; sl < WindowLayout.SILLS; sl++) {
            for (int hd = 0; hd < WindowLayout.HEADS; hd++) {
              te.setPattern(p);
              te.setOpen(o == 1);
              te.setArch(a == 1);
              te.setSill(sl);
              te.setHead(hd);
              assertTrue(keys.add(te.settingsKey()));
            }
          }
        }
      }
    }
  }

  @Test
  void cantedFacingNamesTheQuarterLookedInto() {
    // horizontal indices: S0 W1 N2 E3
    assertEquals(2, WindowLayout.cantedFacing(135f));    // north-west: north
    assertEquals(3, WindowLayout.cantedFacing(225f));    // north-east: east
    assertEquals(0, WindowLayout.cantedFacing(-45f));    // south-east: south
    assertEquals(1, WindowLayout.cantedFacing(45f));     // south-west: west
  }
}

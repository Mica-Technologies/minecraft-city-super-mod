package com.micatechnologies.minecraft.csm.trafficaccessories.bridgecable;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import net.minecraft.nbt.JsonToNBT;
import net.minecraft.nbt.NBTException;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.BlockPos;
import org.junit.jupiter.api.Test;

/**
 * The NBT is the interface the bridge generators write against, so these read it exactly as
 * {@code /blockdata} would hand it over: parsed from the text a builder types.
 */
class BridgeCableLinkTest {

  private static final BlockPos PYLON = new BlockPos(-2535, 117, -2340);

  private static NBTTagCompound nbt(String text) throws NBTException {
    return JsonToNBT.getTagFromJson(text);
  }

  @Test
  void readsTheSingleCableFormWithAnAbsoluteTarget() throws NBTException {
    final List<BridgeCableLink> links = BridgeCableLink.readAll(
        nbt("{target:[-2532,85,-2387],style:\"stay\",thick:4,color:\"white\",sag:0}"), PYLON);
    assertEquals(1, links.size());
    final BridgeCableLink link = links.get(0);
    assertEquals(new BlockPos(-2532, 85, -2387), link.getTarget(PYLON));
    assertEquals(BridgeCableStyle.STAY, link.getStyle());
    assertEquals(4, link.getThickness());
    assertEquals("white", link.getColour());
    assertEquals(0.0, link.getSagBlocks(50.0));
  }

  @Test
  void readsARelativeTarget() throws NBTException {
    final List<BridgeCableLink> links =
        BridgeCableLink.readAll(nbt("{dx:3,dy:-32,dz:-47}"), PYLON);
    assertEquals(PYLON.add(3, -32, -47), links.get(0).getTarget(PYLON));
  }

  @Test
  void readsAnIntArrayTarget() throws NBTException {
    final List<BridgeCableLink> links =
        BridgeCableLink.readAll(nbt("{target:[I;-2532,85,-2387]}"), PYLON);
    assertEquals(new BlockPos(-2532, 85, -2387), links.get(0).getTarget(PYLON));
  }

  @Test
  void theSingleFormAddsToAnAnchorAndReplacesTheSameTarget() throws NBTException {
    NBTTagCompound compound = new NBTTagCompound();
    // three /blockdata runs against one socket, the last restyling the first stay
    for (String run : new String[]{
        "{target:[-2532,85,-2387],style:\"stay\"}",
        "{target:[-2532,85,-2381],style:\"stay\"}",
        "{target:[-2532,85,-2387],style:\"stay\",color:\"red\"}"}) {
      final NBTTagCompound merged = compound.copy();
      merged.merge(nbt(run));
      final List<BridgeCableLink> links = BridgeCableLink.readAll(merged, PYLON);
      compound = new NBTTagCompound();
      BridgeCableLink.writeAll(compound, links);
    }
    final List<BridgeCableLink> links = BridgeCableLink.readAll(compound, PYLON);
    assertEquals(2, links.size());
    assertEquals("red", links.get(0).getColour());
    assertEquals(new BlockPos(-2532, 85, -2381), links.get(1).getTarget(PYLON));
    // and the single-cable keys are not written back
    assertFalse(compound.hasKey("target"));
  }

  @Test
  void readsTheListForm() throws NBTException {
    final List<BridgeCableLink> links = BridgeCableLink.readAll(nbt("{cables:["
        + "{target:[-2532,85,-2387],style:\"stay\"},"
        + "{target:[-2532,85,-2381],style:\"stay\",thick:6}]}"), PYLON);
    assertEquals(2, links.size());
    assertEquals(6, links.get(1).getThickness());
  }

  @Test
  void removeDropsTheCableToABlock() throws NBTException {
    final List<BridgeCableLink> links = BridgeCableLink.readAll(nbt("{cables:["
        + "{target:[-2532,85,-2387]},{target:[-2532,85,-2381]}],"
        + "remove:[-2532,85,-2387]}"), PYLON);
    assertEquals(1, links.size());
    assertEquals(new BlockPos(-2532, 85, -2381), links.get(0).getTarget(PYLON));
  }

  @Test
  void roundTripsEverythingThroughNbt() throws NBTException {
    final List<BridgeCableLink> original = BridgeCableLink.readAll(nbt("{cables:[{"
        + "target:[-1320,111,-2280],style:\"main\",thick:8,color:\"international_orange\","
        + "sag:6,stripes:\"black\",led:\"blue\",hangers:5,hangerThick:2,"
        + "hangerColor:\"galvanized\"},{target:[-1320,85,-2397],style:\"main\",sag:\"8%\"}]}"),
        PYLON);
    final NBTTagCompound written = new NBTTagCompound();
    BridgeCableLink.writeAll(written, original);
    assertEquals(original, BridgeCableLink.readAll(written, PYLON));
  }

  @Test
  void anAnchorMovedElsewhereKeepsItsCables() throws NBTException {
    final List<BridgeCableLink> links =
        BridgeCableLink.readAll(nbt("{target:[-2532,85,-2387]}"), PYLON);
    final NBTTagCompound written = new NBTTagCompound();
    BridgeCableLink.writeAll(written, links);
    final BlockPos moved = PYLON.add(1000, 0, 0);
    assertEquals(new BlockPos(-1532, 85, -2387),
        BridgeCableLink.readAll(written, moved).get(0).getTarget(moved));
  }

  @Test
  void sagIsBlocksOrAPercentOfThePlanSpan() throws NBTException {
    final BridgeCableLink blocks = BridgeCableLink.readAll(
        nbt("{target:[0,0,0],style:\"main\",sag:6}"), PYLON).get(0);
    final BridgeCableLink percent = BridgeCableLink.readAll(
        nbt("{target:[0,0,0],style:\"main\",sag:\"8%\"}"), PYLON).get(0);
    assertEquals(6.0, blocks.getSagBlocks(117.0), 1.0e-6);
    assertEquals(8.0, percent.getSagBlocks(100.0), 1.0e-6);
  }

  @Test
  void defaultsComeFromTheStyle() throws NBTException {
    final BridgeCableLink stay =
        BridgeCableLink.readAll(nbt("{target:[0,0,0]}"), PYLON).get(0);
    assertEquals(BridgeCableStyle.STAY, stay.getStyle());
    assertEquals(4, stay.getThickness());
    assertEquals("white", stay.getColour());
    // a stay never sags, whatever it was given
    assertEquals(0.0, BridgeCableLink.readAll(nbt("{target:[0,0,0],sag:5}"), PYLON).get(0)
        .getSagBlocks(40.0));
    final BridgeCableLink main =
        BridgeCableLink.readAll(nbt("{target:[0,0,0],style:\"main\"}"), PYLON).get(0);
    assertEquals(8, main.getThickness());
    assertEquals(10.0, main.getSagBlocks(100.0), 1.0e-9);
  }

  @Test
  void coloursAreNormalised() throws NBTException {
    assertEquals("international_orange", BridgeCableLink.readAll(
        nbt("{target:[0,0,0],color:\"Orange\"}"), PYLON).get(0).getColour());
    assertEquals("light_grey", BridgeCableColour.normalise("light_gray"));
    assertEquals("#1f55a0", BridgeCableColour.normalise("#1F55A0"));
    assertNull(BridgeCableColour.normalise("not a colour"));
    assertEquals(0xC0362C, BridgeCableColour.rgb("international_orange"));
  }

  @Test
  void thicknessIsClamped() throws NBTException {
    assertEquals(BridgeCableLink.MAX_THICKNESS, BridgeCableLink.readAll(
        nbt("{target:[0,0,0],thick:99}"), PYLON).get(0).getThickness());
  }

  @Test
  void noTargetMeansNoCable() throws NBTException {
    assertTrue(BridgeCableLink.readAll(nbt("{style:\"stay\"}"), PYLON).isEmpty());
    // a target on the anchor itself is not a cable either
    assertNull(BridgeCableLink.read(nbt("{target:[-2535,117,-2340]}"), PYLON));
  }

  @Test
  void anAnchorHoldsAtMostSixteen() {
    final List<BridgeCableLink> links = new java.util.ArrayList<>();
    for (int i = 1; i <= BridgeCableLink.MAX_CABLES; i++) {
      assertTrue(BridgeCableLink.putInto(links,
          BridgeCableLink.of(PYLON, PYLON.add(i, -10, 0), BridgeCableStyle.STAY)));
    }
    assertFalse(BridgeCableLink.putInto(links,
        BridgeCableLink.of(PYLON, PYLON.add(40, -10, 0), BridgeCableStyle.STAY)));
    // replacing one already there still works when full
    assertTrue(BridgeCableLink.putInto(links,
        BridgeCableLink.of(PYLON, PYLON.add(1, -10, 0), BridgeCableStyle.ROPE)));
  }
}

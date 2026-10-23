package com.micatechnologies.minecraft.csm.codeutils.wayfinding;

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;
import org.junit.jupiter.api.Test;

/**
 * The wayfinding panels' NBT: what the airport and hospital panels wrote before #307 must read
 * and write back byte for byte, the keys added for #307 must round-trip, and a cell never edited
 * must keep its block's default.
 */
class WayfindingSignNbtTest {

  private static final WayfindingSign DEFAULT =
      WayfindingPreset.HOSPITAL[0].toSign(WayfindingArrow.RIGHT, true);

  /** A cell of a panel whose block defaults to the hospital's sign. */
  private static final class Cell extends AbstractTileEntityWayfindingPanel {

    @Override
    public WayfindingSign defaultSign() {
      return DEFAULT;
    }
  }

  /** A sign as the hospital builder wrote it, with its place, and no text scale. */
  private static NBTTagCompound hospitalSample() {
    NBTTagCompound c = new NBTTagCompound();
    c.setString("l1", "IMAGING");
    c.setString("l2", "X-RAY");
    c.setString("p", "xray");
    c.setString("a", "up_left");
    c.setString("s", "neutral");
    c.setBoolean("d", false);
    c.setFloat("ts", 0.8f);
    c.setFloat("ts2", 0.75f);
    c.setByte("pc", (byte) 2);
    c.setByte("pr", (byte) 1);
    c.setByte("pw", (byte) 6);
    c.setByte("ph", (byte) 2);
    return c;
  }

  /** A sign as the airport panel's editor wrote it: no place keys. */
  private static NBTTagCompound airportSample() {
    NBTTagCompound c = new NBTTagCompound();
    c.setString("l1", "GATES");
    c.setString("l2", "A1-A20");
    c.setString("p", "depart");
    c.setString("a", "right");
    c.setString("s", "airport");
    c.setBoolean("d", true);
    return c;
  }

  private static byte[] bytes(NBTTagCompound c) throws IOException {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    CompressedStreamTools.write(c, new DataOutputStream(out));
    return out.toByteArray();
  }

  private static NBTTagCompound roundTrip(NBTTagCompound in) {
    Cell te = new Cell();
    te.readNBT(in);
    return te.writeNBT(new NBTTagCompound());
  }

  @Test
  void hospitalSignWritesBackByteIdentical() throws IOException {
    NBTTagCompound out = roundTrip(hospitalSample());
    assertEquals(hospitalSample(), out);
    assertArrayEquals(bytes(hospitalSample()), bytes(out));
  }

  @Test
  void airportSignWritesBackByteIdenticalAndHasNoPlace() throws IOException {
    Cell te = new Cell();
    te.readNBT(airportSample());
    assertFalse(te.isPlaceKnown());
    NBTTagCompound out = te.writeNBT(new NBTTagCompound());
    assertArrayEquals(bytes(airportSample()), bytes(out));
  }

  @Test
  void oldSignReadsWithEveryNewOptionAtItsDefault() {
    Cell te = new Cell();
    te.readNBT(airportSample());
    WayfindingSign s = te.getSign();
    assertEquals(WayfindingMount.AUTO, s.getMount());
    assertEquals(1f, s.getPanelScale());
    assertEquals(0, s.getLineCount());
    assertEquals(WayfindingSign.Align.LEFT, s.getAlign());
    assertTrue(s.isLit());
    assertEquals(WayfindingSign.NO_COLOUR, s.getCustomBackground());
    assertArrayEquals(new int[]{0, 1}, s.shownLines());
  }

  @Test
  void neverEditedCellWritesNoSignAndKeepsTheDefault() {
    Cell te = new Cell();
    assertTrue(te.writeNBT(new NBTTagCompound()).isEmpty());
    assertEquals(DEFAULT, te.getSign());
    assertFalse(te.hasOwnSign());
  }

  @Test
  void compoundWithoutPictogramOrNewKeyLeavesTheSignAlone() {
    Cell te = new Cell();
    te.readNBT(hospitalSample());
    NBTTagCompound partial = new NBTTagCompound();
    partial.setString("l1", "IGNORED");
    te.readNBT(partial);
    assertEquals("IMAGING", te.getSign().getLine(0));
  }

  @Test
  void anyNewKeyMakesACompoundASign() {
    Cell te = new Cell();
    NBTTagCompound c = new NBTTagCompound();
    c.setString("l1", "L3");
    c.setString("l2", "STAIR A");
    c.setString("m", "wall");
    te.readNBT(c);
    assertTrue(te.hasOwnSign());
    assertEquals("L3", te.getSign().getLine(0));
    assertEquals(WayfindingMount.WALL, te.getSign().getMount());
    // the scheme falls back to the block's, everything else to none
    assertEquals(WayfindingScheme.NEUTRAL, te.getSign().getScheme());
    assertEquals(WayfindingIcon.NONE, te.getSign().getPictogram());
  }

  @Test
  void everyNewKeyRoundTrips() {
    WayfindingSign sign = WayfindingSign.builder()
        .line(0, "LEVEL 3").line(1, "").line(2, "STAIR A").line(3, "ROOF ACCESS")
        .lineScale(3, 0.6f).textScale(0.9f).lineCount(4).align(WayfindingSign.Align.CENTRE)
        .iconLeft(2, WayfindingIcon.STAIRS).iconRight(3, WayfindingIcon.ARROW_UP_LEFT)
        .iconStyle(WayfindingSign.IconStyle.PLAIN).mount(WayfindingMount.POST).drop(6)
        .panelScale(0.75f).background(0x123456).legend(0xFFEE00).border(2).lit(false)
        .pictogram(WayfindingIcon.ESCALATOR).arrow(WayfindingArrow.DOWN_RIGHT)
        .scheme(WayfindingScheme.TRANSIT).doubleSided(false).build();
    NBTTagCompound nbt = sign.write(new NBTTagCompound());
    assertEquals("#123456", nbt.getString("bg"));
    assertEquals("c", nbt.getString("al"));
    assertEquals(sign, WayfindingSign.read(nbt, WayfindingScheme.CORPORATE));
    Cell te = new Cell();
    te.readNBT(nbt);
    assertEquals(sign, te.getSign());
    assertArrayEquals(new int[]{0, 1, 2, 3}, sign.shownLines());
  }

  @Test
  void valuesAreClampedAndCleaned() {
    NBTTagCompound c = new NBTTagCompound();
    c.setString("p", "info");
    c.setString("l1", "  café au lait  ");
    c.setFloat("sc", 9f);
    c.setInteger("rd", 99);
    c.setInteger("bd", -3);
    c.setString("fg", "not a colour");
    c.setDouble("ts1", 0.1);
    c.setString("il1", "a later icon");
    WayfindingSign s = WayfindingSign.read(c, WayfindingScheme.CORPORATE);
    assertEquals("caf au lait", s.getLine(0));
    assertEquals(WayfindingSign.MAX_PANEL_SCALE, s.getPanelScale());
    assertEquals(WayfindingSign.MAX_DROP, s.getDrop());
    assertEquals(0, s.getBorder());
    assertEquals(WayfindingSign.NO_COLOUR, s.getCustomLegend());
    assertEquals(WayfindingSign.MIN_TEXT_SCALE, s.getLineScale(0));
    assertEquals(WayfindingIcon.NONE, s.getIconLeft(0));
  }

  @Test
  void coloursParseAsTyped() {
    assertEquals(0xAABBCC, WayfindingSign.parseColour("#aabbcc"));
    assertEquals(0xAABBCC, WayfindingSign.parseColour("AABBCC"));
    assertEquals(0xAABBCC, WayfindingSign.parseColour("0xAABBCC"));
    assertEquals(0xAABBCC, WayfindingSign.parseColour("#abc"));
    assertEquals(WayfindingSign.NO_COLOUR, WayfindingSign.parseColour(""));
    assertEquals(WayfindingSign.NO_COLOUR, WayfindingSign.parseColour("#12345"));
    assertEquals(WayfindingSign.NO_COLOUR, WayfindingSign.parseColour("#GGGGGG"));
    NBTTagCompound c = new NBTTagCompound();
    c.setString("p", "none");
    c.setInteger("bg", 0x00FF00);
    assertEquals(0x00FF00, WayfindingSign.read(c, WayfindingScheme.CORPORATE).background());
  }

  @Test
  void presetKeepsMountAndScaleButResetsTheWords() {
    WayfindingSign sign = WayfindingSign.builder().line(2, "OLD").mount(WayfindingMount.WALL)
        .panelScale(0.5f).legend(0xFF0000).iconLeft(0, WayfindingIcon.WIFI).build();
    WayfindingSign p = WayfindingPreset.BUILDING[1].applyTo(sign);
    assertEquals("LEVEL 1", p.getLine(0));
    assertEquals("STAIR A", p.getLine(1));
    assertEquals("", p.getLine(2));
    assertEquals(WayfindingIcon.NONE, p.getIconLeft(0));
    assertEquals(WayfindingSign.NO_COLOUR, p.getCustomLegend());
    assertEquals(WayfindingMount.WALL, p.getMount());
    assertEquals(0.5f, p.getPanelScale());
  }

  @Test
  void everyIconIdIsOnTheSheet() {
    for (WayfindingIcon icon : WayfindingIcon.values()) {
      if (icon != WayfindingIcon.NONE && !icon.isArrow()) {
        assertNotNull(WayfindingSheet.icon(icon.getId()), icon.getId());
      }
    }
    for (WayfindingPreset[] set : new WayfindingPreset[][]{WayfindingPreset.AIRPORT,
        WayfindingPreset.HOSPITAL, WayfindingPreset.BUILDING}) {
      for (WayfindingPreset p : set) {
        assertEquals(p.getLine1(), WayfindingSign.clamp(p.getLine1()));
      }
    }
  }
}

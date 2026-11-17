package com.micatechnologies.minecraft.csm.buildingmaterials.plaques;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.micatechnologies.minecraft.csm.buildingmaterials.plaques.PlaqueLayout.Piece;
import com.micatechnologies.minecraft.csm.buildingmaterials.plaques.PlaqueLayout.Result;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.nbt.NBTTagCompound;
import org.junit.jupiter.api.Test;

/**
 * The address plaque's NBT (what scripts write) and its layout (what the baked model and the
 * hitbox draw from).
 */
class AddressPlaqueTest {

  private static AddressPlaque nbt(String snbt) throws Exception {
    return AddressPlaque.read(net.minecraft.nbt.JsonToNBT.getTagFromJson(snbt));
  }

  // --- NBT -----------------------------------------------------------------------------------

  @Test
  void anEmptyCompoundIsTheDefault() {
    assertEquals(AddressPlaque.DEFAULT, AddressPlaque.read(new NBTTagCompound()));
    assertFalse(AddressPlaque.isPlaque(new NBTTagCompound()));
  }

  @Test
  void everyKeyRoundTrips() {
    AddressPlaque p = new AddressPlaque.Builder().number("12345").street("West 86th St")
        .font(PlaqueFont.NARROW).size(0.7f).plate(PlaqueSize.SMALL).vertical(true)
        .ink(PlaqueInk.GOLD).dx(-3).dy(4).dz(7).lit(false).build();
    NBTTagCompound c = p.write(new NBTTagCompound());
    assertTrue(AddressPlaque.isPlaque(c));
    assertEquals(p, AddressPlaque.read(c));
    assertEquals("12345", c.getString("num"));
    assertEquals("West 86th St", c.getString("st"));
    assertEquals("narrow", c.getString("font"));
    assertEquals("s", c.getString("plate"));
    assertEquals("v", c.getString("lay"));
    assertEquals("gold", c.getString("ink"));
    assertEquals(-3, c.getByte("dx"));
    assertEquals(4, c.getByte("dy"));
    assertEquals(7, c.getByte("dz"));
    assertEquals(0, c.getByte("lit"));
  }

  @Test
  void defaultsAreNotWrittenExceptTheNumber() {
    NBTTagCompound c = new AddressPlaque.Builder().number("44").build()
        .write(new NBTTagCompound());
    assertEquals(1, c.getKeySet().size());
    assertEquals("44", c.getString("num"));
  }

  @Test
  void aScriptsCompoundReadsAsTheEditorWouldWriteIt() throws Exception {
    AddressPlaque p = nbt("{num:\"245\",st:\"W 86th St\",font:\"didone\",ink:\"blue\"}");
    assertEquals("245", p.getNumber());
    assertEquals("W 86th St", p.getStreet());
    assertEquals(PlaqueFont.DIDONE, p.getFont());
    assertEquals(PlaqueInk.BLUE, p.getInk());
    // a number written as a number
    assertEquals("17", nbt("{num:17}").getNumber());
  }

  @Test
  void valuesAreCleanedAndClamped() throws Exception {
    AddressPlaque p = nbt("{num:\"  1234567890 \",st:\"Rueé de la Paix!\",size:3.0f,"
        + "dx:99b,dy:-99b,dz:-100b,font:\"comic\",ink:\"plaid\",plate:\"xl\",lay:\"q\"}");
    assertEquals("12345678", p.getNumber());
    assertEquals("Rue de la Paix", p.getStreet());
    assertEquals(1f, p.getSize());
    assertEquals(AddressPlaque.MAX_OFFSET, p.getDx());
    assertEquals(-AddressPlaque.MAX_OFFSET, p.getDy());
    assertEquals(AddressPlaque.MIN_DEPTH, p.getDz());
    assertNull(p.getFont());
    assertEquals(PlaqueInk.DEFAULT, p.getInk());
    assertEquals(PlaqueSize.MEDIUM, p.getPlate());
    assertFalse(p.isVertical());
    assertEquals(AddressPlaque.MIN_SIZE, nbt("{size:0.1f}").getSize());
  }

  @Test
  void theStyleFillsInWhatIsLeftDefault() {
    AddressPlaque p = AddressPlaque.DEFAULT;
    assertEquals(PlaqueFont.CLASSIC, p.fontFor(PlaqueStyle.BRASS));
    assertEquals(PlaqueInk.GOLD.getRgb(), p.inkFor(PlaqueStyle.CAST_IRON));
    assertEquals(PlaqueInk.BLUE.getRgb(), p.inkFor(PlaqueStyle.ENAMEL));
    assertEquals(PlaqueInk.RED.getRgb(),
        p.toBuilder().ink(PlaqueInk.RED).build().inkFor(PlaqueStyle.ENAMEL));
  }

  @Test
  void everyStyleHasABlockNameAndATile() {
    for (PlaqueStyle s : PlaqueStyle.values()) {
      assertEquals(s, PlaqueStyle.byRegistryName(s.getRegistryName()));
      if (s.hasPlate()) {
        boolean found = false;
        for (String t : PlaqueSheet.TILE_NAMES) {
          found |= t.equals(s.getTile());
        }
        assertTrue(found, s.getRegistryName());
      }
    }
  }

  // --- layout --------------------------------------------------------------------------------

  private static List<Piece> glyphs(Result r) {
    List<Piece> out = new ArrayList<>();
    for (Piece p : r.pieces) {
      if (p.sheet == 0) {
        out.add(p);
      }
    }
    return out;
  }

  private static float[] inkBounds(List<Piece> pieces, float d) {
    float[] b = {99, 99, -99, -99};
    for (Piece p : pieces) {
      if (Math.abs(p.d - d) < 1e-4 && !p.back) {
        b[0] = Math.min(b[0], p.x0);
        b[1] = Math.min(b[1], p.y0);
        b[2] = Math.max(b[2], p.x1);
        b[3] = Math.max(b[3], p.y1);
      }
    }
    return b;
  }

  private static float front(List<Piece> pieces) {
    float d = -99;
    for (Piece p : pieces) {
      if (!p.back) {
        d = Math.max(d, p.d);
      }
    }
    return d;
  }

  @Test
  void twoToFiveDigitsFitInsideThePlateAndAreLegible() {
    for (PlaqueStyle style : PlaqueStyle.values()) {
      for (PlaqueSize size : PlaqueSize.values()) {
        for (boolean vertical : new boolean[]{false, true}) {
          for (String number : new String[]{"12", "345", "4567", "12345"}) {
            for (String street : new String[]{"", "West 86th Street"}) {
              for (PlaqueFont font : PlaqueFont.values()) {
                AddressPlaque p = new AddressPlaque.Builder().number(number).street(street)
                    .plate(size).vertical(vertical).font(font).build();
                Result r = PlaqueLayout.layout(style, p);
                List<Piece> g = glyphs(r);
                String what = style + " " + size + " " + vertical + " " + number + " '"
                    + street + "' " + font;
                assertFalse(g.isEmpty(), what);
                float[] b = inkBounds(g, front(g));
                float[] box = r.plate != null ? r.plate : new float[]{0, 0, 16, 16};
                assertTrue(b[0] >= box[0] - 0.3f && b[2] <= box[2] + 0.3f
                    && b[1] >= box[1] - 0.3f && b[3] <= box[3] + 0.3f, what);
                // the numerals' height: at least 1.5 px (9 cm) even five stacked on a small
                // plate; at ten blocks that is still about ten screen pixels at 1080p
                float digitH = 0;
                for (Piece q : g) {
                  digitH = Math.max(digitH, q.y1 - q.y0);
                }
                assertTrue(digitH >= 1.5f, what + " " + digitH);
              }
            }
          }
        }
      }
    }
  }

  @Test
  void aMediumPlaqueWithThreeDigitsHasNumeralsAQuarterBlockTall() {
    Result r = PlaqueLayout.layout(PlaqueStyle.BRASS, AddressPlaque.DEFAULT);
    float h = 0;
    for (Piece q : glyphs(r)) {
      h = Math.max(h, q.y1 - q.y0);
    }
    assertTrue(h >= 3.5f, "numerals " + h + " px");
  }

  @Test
  void layersStandApart() {
    for (PlaqueStyle style : PlaqueStyle.values()) {
      Result r = PlaqueLayout.layout(style, AddressPlaque.DEFAULT);
      List<Float> ds = new ArrayList<>();
      for (Piece p : r.pieces) {
        if (!p.back && !ds.contains(p.d)) {
          ds.add(p.d);
        }
      }
      for (int i = 0; i < ds.size(); i++) {
        for (int j = i + 1; j < ds.size(); j++) {
          assertTrue(Math.abs(ds.get(i) - ds.get(j)) >= PlaqueLayout.LIFT - 1e-4f,
              style + " " + ds);
        }
      }
      if (r.plate != null) {
        for (Piece p : r.pieces) {
          assertTrue(p.d >= r.depth, style + " piece behind the plate face");
        }
      }
    }
  }

  @Test
  void theHitboxIsThinAndInsideTheBlock() {
    for (PlaqueStyle style : PlaqueStyle.values()) {
      for (int dz : new int[]{-24, -8, 0, 7, 16}) {
        for (int dx : new int[]{-8, 0, 8}) {
          AddressPlaque p = new AddressPlaque.Builder().dz(dz).dx(dx).dy(-dx)
              .plate(PlaqueSize.LARGE).build();
          float[] b = PlaqueLayout.layout(style, p).box;
          String what = style + " dz " + dz + " dx " + dx;
          assertTrue(b[0] >= 0 && b[1] >= 0 && b[2] >= 0, what);
          assertTrue(b[3] <= 16 && b[4] <= 16 && b[5] <= 16, what);
          assertTrue(b[5] - b[2] <= 3.01f && b[5] - b[2] >= 0.5f, what + " depth");
          assertTrue(b[3] > b[0] && b[4] > b[1], what);
        }
      }
    }
  }

  @Test
  void offsetsMoveThePlateButNeverOutOfTheBlock() {
    AddressPlaque p = new AddressPlaque.Builder().dx(8).dy(-8).build();
    Result r = PlaqueLayout.layout(PlaqueStyle.BRASS, p);
    assertEquals(16f, r.plate[2], 1e-4);
    assertEquals(0f, r.plate[1], 1e-4);
    Result c = PlaqueLayout.layout(PlaqueStyle.BRASS, AddressPlaque.DEFAULT);
    assertEquals(8f, (c.plate[0] + c.plate[2]) / 2f, 1e-4);
  }

  @Test
  void theDecalIsDrawnFromBothSidesAndFollowsItsDepth() {
    AddressPlaque p = new AddressPlaque.Builder().dz(7).build();
    Result r = PlaqueLayout.layout(PlaqueStyle.TRANSOM_DECAL, p);
    boolean back = false;
    for (Piece q : r.pieces) {
      back |= q.back;
      assertTrue(q.d < 0 && q.d > -7, "decal layer at " + q.d);
    }
    assertTrue(back);
    for (PlaqueStyle s : PlaqueStyle.values()) {
      if (s != PlaqueStyle.TRANSOM_DECAL) {
        for (Piece q : PlaqueLayout.layout(s, AddressPlaque.DEFAULT).pieces) {
          assertFalse(q.back, s.toString());
        }
      }
    }
  }

  @Test
  void theLitLensIsUnshadedAndTheUnlitOneShaded() {
    Result lit = PlaqueLayout.layout(PlaqueStyle.LAMP_BOX, AddressPlaque.DEFAULT);
    Result off = PlaqueLayout.layout(PlaqueStyle.LAMP_BOX,
        AddressPlaque.DEFAULT.toBuilder().lit(false).build());
    for (Piece p : lit.pieces) {
      assertFalse(p.shade);
    }
    for (Piece p : off.pieces) {
      assertTrue(p.shade);
    }
  }

  @Test
  void theStackIsOneCharacterAColumnTopToBottom() {
    AddressPlaque p = new AddressPlaque.Builder().number("123").vertical(true).build();
    List<Piece> g = glyphs(PlaqueLayout.layout(PlaqueStyle.BRASS, p));
    assertEquals(3, g.size());
    assertTrue(g.get(0).y0 > g.get(1).y1 - 0.01f && g.get(1).y0 > g.get(2).y1 - 0.01f);
  }
}

package com.micatechnologies.minecraft.csm.signage;

import static org.junit.jupiter.api.Assertions.*;

import java.io.InputStream;
import java.util.EnumSet;
import org.junit.jupiter.api.Test;

class AdLibraryTest {

  @Test
  void theShippedIndexLoadsAndEveryTextureExists() throws Exception {
    AdLibrary library = AdLibrary.get();
    assertFalse(library.rotation().isEmpty(), "no ads in rotation");
    for (AdEntry entry : library.all()) {
      for (AdShape shape : entry.getShapes()) {
        String path = "/assets/" + entry.texture(shape).getNamespace() + "/"
            + entry.texture(shape).getPath();
        try (InputStream in = AdLibraryTest.class.getResourceAsStream(path)) {
          assertNotNull(in, "missing texture " + path);
        }
      }
    }
  }

  @Test
  void theHouseAdStandsInForUnknownIdsAndStaysOutOfRotation() {
    AdLibrary library = AdLibrary.get();
    assertNotNull(library.find(AdLibrary.HOUSE_AD), "the house ad must ship");
    assertEquals(AdLibrary.HOUSE_AD, library.resolve("no_such_ad").getId());
    assertTrue(library.rotation().stream()
        .noneMatch(e -> e.getId().equals(AdLibrary.HOUSE_AD)));
  }

  @Test
  void aBoardGetsTheShapeNearestItsProportions() {
    EnumSet<AdShape> all = EnumSet.allOf(AdShape.class);
    assertEquals(AdShape.PORTRAIT, AdShape.nearest(2, 3, all));
    assertEquals(AdShape.PORTRAIT, AdShape.nearest(4, 6, all));
    assertEquals(AdShape.POSTER, AdShape.nearest(15, 8, all));
    assertEquals(AdShape.BULLETIN, AdShape.nearest(15, 4, all));
    assertEquals(AdShape.BULLETIN, AdShape.nearest(40, 8, all));
    assertEquals(AdShape.SQUARE, AdShape.nearest(40, 40, all));
    // Only what the ad was drawn in is offered.
    assertEquals(AdShape.POSTER, AdShape.nearest(40, 8, EnumSet.of(AdShape.POSTER,
        AdShape.PORTRAIT)));
  }

  @Test
  void coverCropsTheLongAxisAndContainLetterboxesTheShortOne() {
    // A 7:2 image on a 2:1 face.
    double[] cover = AdFit.COVER.place(2.0, 3.5);
    assertArrayEquals(new double[]{0, 0, 1, 1}, slice(cover, 0), 1e-9);
    assertEquals(1 - 2.0 / 3.5, cover[4] * 2, 1e-9);
    assertEquals(0, cover[5], 1e-9);

    double[] contain = AdFit.CONTAIN.place(2.0, 3.5);
    assertArrayEquals(new double[]{0, 0, 1, 1}, slice(contain, 4), 1e-9);
    assertEquals(0, contain[0], 1e-9);
    assertEquals(1 - 2.0 / 3.5, contain[1] * 2, 1e-9);

    assertArrayEquals(new double[]{0, 0, 1, 1, 0, 0, 1, 1}, AdFit.STRETCH.place(2.0, 3.5));
    assertEquals(AdFit.COVER, AdFit.fromOrdinal(99));
  }

  @Test
  void aBadColourIsBlackNotACrash() {
    assertEquals(0xd7261e, AdLibrary.parseColour("#d7261e"));
    assertEquals(0, AdLibrary.parseColour("red"));
    assertEquals(0, AdLibrary.parseColour(null));
  }

  private static double[] slice(double[] values, int from) {
    return new double[]{values[from], values[from + 1], values[from + 2], values[from + 3]};
  }
}

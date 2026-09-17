package com.micatechnologies.minecraft.csm.codeutils;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class CsmScrollViewportTest {

  // The dynamic sign editors' fixed strips: tabs at y 25 (18 tall) with the scrolling area
  // starting 6 below them, and the Save row 30 up from the bottom of the screen.
  private static final int VIEWPORT_TOP = 49;
  private static final int BUTTON_HEIGHT = 18;

  @Test
  void aRowFullyBetweenTheStripsIsVisible() {
    assertTrue(CsmScrollViewport.isRowVisible(100, BUTTON_HEIGHT, VIEWPORT_TOP, 227));
    // Flush against either edge still counts.
    assertTrue(CsmScrollViewport.isRowVisible(VIEWPORT_TOP, BUTTON_HEIGHT, VIEWPORT_TOP, 227));
    assertTrue(CsmScrollViewport.isRowVisible(227 - BUTTON_HEIGHT, BUTTON_HEIGHT,
        VIEWPORT_TOP, 227));
  }

  @Test
  void aRowPeekingPastEitherStripIsNot() {
    assertFalse(CsmScrollViewport.isRowVisible(VIEWPORT_TOP - 1, BUTTON_HEIGHT,
        VIEWPORT_TOP, 227));
    assertFalse(CsmScrollViewport.isRowVisible(227 - BUTTON_HEIGHT + 1, BUTTON_HEIGHT,
        VIEWPORT_TOP, 227));
  }

  @Test
  void theRowThatUsedToSitUnderTheSaveButtonIsHidden() {
    // The case this exists for. On a 1904x1041 window the game picks GUI scale 4, so the
    // screen is 476x261 scaled: the Save row runs 231..249 and the scrolling area ends at 227.
    // The street sign editor's emblem-kind button lands at y 225 -- four pixels of it showed
    // under the Save button, and the click that pressed Save pressed it too, cycling the
    // blade's emblem on every save.
    int saveRowTop = 261 - 30;
    int viewportBottom = saveRowTop - 4;
    int emblemKindRow = 225;
    int saveClickY = saveRowTop + BUTTON_HEIGHT / 2;

    assertTrue(emblemKindRow <= saveClickY && saveClickY < emblemKindRow + BUTTON_HEIGHT,
        "the geometry that caused the bug");
    assertFalse(CsmScrollViewport.isRowVisible(emblemKindRow, BUTTON_HEIGHT, VIEWPORT_TOP,
        viewportBottom));
  }
}

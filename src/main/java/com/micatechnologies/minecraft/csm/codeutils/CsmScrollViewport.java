package com.micatechnologies.minecraft.csm.codeutils;

/**
 * Whether a row of a scrolling GUI viewport may be drawn and clicked.
 *
 * <p>The dynamic sign editors put their scrolling content between two <b>fixed</b> strips: the
 * tab buttons above and the Save/Cancel row below. A content widget that only <i>overlaps</i> the
 * viewport is half in and half under one of those strips -- and a widget under the Save row is
 * still clickable, while the Save button drawn over it hides it completely.
 *
 * <p>That is not cosmetic. Vanilla's {@code GuiScreen.mouseClicked} walks the whole button list
 * and does not stop at the first hit, so one click on Save pressed <b>both</b> Save and whatever
 * content button was peeking out under it. On a 1904x1041 window (GUI scale 4, so 476x261
 * scaled), the street sign editor's Save row lands exactly on the Text tab's emblem-kind button:
 * every save quietly cycled the blade's emblem, which is how a sign nobody had given an emblem
 * came back wearing an Interstate shield. Which row it hits depends on the window height, so it
 * looks like nothing at all at most sizes.
 *
 * <p>Requiring a row to be <b>fully</b> inside the viewport costs a partially scrolled row its
 * sliver of visibility and removes the whole class of bug.
 *
 * @since 2026.9.17
 */
public final class CsmScrollViewport {

  private CsmScrollViewport() {
  }

  /**
   * Whether a row at this position fits entirely between the fixed strips.
   *
   * @param y            the row's top edge, already scrolled
   * @param height       the row's height
   * @param viewportTop  the first y the scrolling area owns
   * @param viewportBottom the first y below the scrolling area
   *
   * @return true when the row may be drawn and clicked
   */
  public static boolean isRowVisible(int y, int height, int viewportTop, int viewportBottom) {
    return y >= viewportTop && y + height <= viewportBottom;
  }
}

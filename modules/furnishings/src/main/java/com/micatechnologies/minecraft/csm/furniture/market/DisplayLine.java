package com.micatechnologies.minecraft.csm.furniture.market;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;

/**
 * Which displays a light switch reaches: the one switched and the displays joined to it in its
 * line, up to {@link #REACH} in all, taken alternately from each side so a switch at the end of a
 * row works the next four and one in the middle works two either way. A longer row needs a
 * second switch; the cap keeps one change of power from walking an arbitrarily long row.
 *
 * @since 2026.9
 */
final class DisplayLine {

  /** The most displays one switch works, the switched one included. */
  static final int REACH = 5;

  private DisplayLine() {
  }

  /**
   * Lists the displays one switch at {@code pos} works.
   *
   * @param pos    the display switched
   * @param facing the way it faces; its line runs across that
   * @param joined whether a position holds a display joined to it in the same line
   * @return {@code pos} first, then up to {@link #REACH} - 1 joined displays
   */
  static List<BlockPos> reach(BlockPos pos, EnumFacing facing, Predicate<BlockPos> joined) {
    List<BlockPos> line = new ArrayList<>(REACH);
    line.add(pos);
    BlockPos left = pos;
    BlockPos right = pos;
    boolean leftOpen = true;
    boolean rightOpen = true;
    while (line.size() < REACH && (leftOpen || rightOpen)) {
      if (leftOpen) {
        left = left.offset(facing.rotateYCCW());
        leftOpen = joined.test(left);
        if (leftOpen) {
          line.add(left);
        }
      }
      if (rightOpen && line.size() < REACH) {
        right = right.offset(facing.rotateY());
        rightOpen = joined.test(right);
        if (rightOpen) {
          line.add(right);
        }
      }
    }
    return line;
  }
}

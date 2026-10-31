package com.micatechnologies.minecraft.csm.venues.bowling;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * One bowler's line on a scoring monitor: a name and the balls rolled, in the notation a score
 * sheet uses, worked out into the ten frames a monitor shows. Plain logic, no Minecraft classes.
 *
 * <p>A line is {@code NAME: rolls}. The name is everything before the first colon (cut to ten
 * characters; a line with no colon is all name). The rolls are one character a ball, spaces
 * ignored: {@code X} a strike, {@code /} a spare, {@code -} or {@code 0} a miss, {@code F} a foul
 * (no pins), {@code 1}-{@code 9} the pins knocked down. Anything else is skipped. A ball that
 * would knock down more pins than are standing is cut to what is standing, so every line scores.
 * At most 21 balls are read.</p>
 *
 * <p>For each frame the monitor shows the marks in its small boxes (frames 1-9 two, the tenth
 * three) and the running total once it can be known: a strike waits for the next two balls, a
 * spare for the next one.</p>
 *
 * @since 2026.10
 */
public final class BowlingScore {

  /** The longest name shown. */
  public static final int NAME_MAX = 10;

  private final String name;
  private final String[][] marks = new String[10][];
  private final int[] totals = new int[10];
  private int balls;

  private BowlingScore(String name) {
    this.name = name;
    for (int f = 0; f < 10; f++) {
      marks[f] = f == 9 ? new String[]{"", "", ""} : new String[]{"", ""};
      totals[f] = -1;
    }
  }

  /**
   * Works out a line.
   *
   * @param line {@code NAME: rolls}, or null
   *
   * @return the bowler's frames
   */
  public static BowlingScore parse(String line) {
    String s = line == null ? "" : line;
    int colon = s.indexOf(':');
    String name = (colon < 0 ? s : s.substring(0, colon)).trim();
    if (name.length() > NAME_MAX) {
      name = name.substring(0, NAME_MAX).trim();
    }
    BowlingScore out = new BowlingScore(name.toUpperCase(Locale.ROOT));
    if (colon >= 0) {
      out.score(s.substring(colon + 1));
    }
    return out;
  }

  /** The bowler's name, upper case; empty for a blank line. */
  public String getName() {
    return name;
  }

  /** Whether the line has a name or any ball: a monitor leaves an empty row dark. */
  public boolean isEmpty() {
    return name.isEmpty() && balls == 0;
  }

  /** The marks in frame {@code f}'s small boxes (0-9): two, or three for the tenth. */
  public String[] marks(int f) {
    return marks[f];
  }

  /** The running total at frame {@code f} (0-9), or -1 while it cannot be known. */
  public int total(int f) {
    return totals[f];
  }

  /** The last running total known, or 0. */
  public int knownTotal() {
    int t = 0;
    for (int f = 0; f < 10; f++) {
      if (totals[f] >= 0) {
        t = totals[f];
      }
    }
    return t;
  }

  /** How many balls were read. */
  public int balls() {
    return balls;
  }

  /** The frame the bowler is in (0-9), or 10 when the game is over. */
  public int currentFrame() {
    int f = 0;
    while (f < 10 && totals[f] >= 0) {
      f++;
    }
    return f;
  }

  // --- working it out ------------------------------------------------------------------------

  private void score(String rolls) {
    List<Integer> pins = new ArrayList<>();
    List<Character> shown = new ArrayList<>();
    // the pins standing before this ball, and whether it is a frame's first ball
    int frame = 0;
    int ballInFrame = 0;
    int standing = 10;
    for (int i = 0; i < rolls.length() && pins.size() < 21 && frame < 10; i++) {
      char c = Character.toUpperCase(rolls.charAt(i));
      int n;
      if (c == 'X') {
        n = standing;
      } else if (c == '/') {
        n = ballInFrame == 0 ? 0 : standing;
      } else if (c == '-' || c == '0' || c == 'F') {
        n = 0;
      } else if (c >= '1' && c <= '9') {
        n = Math.min(standing, c - '0');
      } else {
        continue;
      }
      pins.add(n);
      int before = standing;
      standing -= n;
      // what the box shows, from the pins, so an "X" after a first ball reads as a spare
      char mark;
      if (n == 0) {
        mark = c == 'F' ? 'F' : '-';
      } else if (before == 10 && n == 10) {
        mark = 'X';
      } else if (standing == 0) {
        mark = '/';
      } else {
        mark = (char) ('0' + n);
      }
      shown.add(mark);
      ballInFrame++;
      if (frame < 9) {
        if (standing == 0 || ballInFrame == 2) {
          frame++;
          ballInFrame = 0;
          standing = 10;
        }
      } else {
        // the tenth: a strike or a spare racks the pins again and earns a third ball
        if (standing == 0) {
          standing = 10;
        }
        int first = pins.get(pins.size() - ballInFrame);
        boolean bonus = ballInFrame >= 2 && (first == 10
            || first + pins.get(pins.size() - ballInFrame + 1) == 10);
        if (ballInFrame == 3 || (ballInFrame == 2 && !bonus)) {
          frame = 10;
        }
      }
    }
    balls = pins.size();
    fill(pins, shown);
  }

  private void fill(List<Integer> pins, List<Character> shown) {
    int i = 0;
    int running = 0;
    boolean known = true;
    for (int f = 0; f < 10 && i < pins.size(); f++) {
      int first = pins.get(i);
      if (f < 9) {
        if (first == 10) {
          marks[f][1] = "X";
          if (known && i + 2 < pins.size()) {
            running += 10 + pins.get(i + 1) + pins.get(i + 2);
            totals[f] = running;
          } else {
            known = false;
          }
          i += 1;
          continue;
        }
        marks[f][0] = String.valueOf(shown.get(i));
        if (i + 1 >= pins.size()) {
          known = false;
          i += 1;
          continue;
        }
        marks[f][1] = String.valueOf(shown.get(i + 1));
        int second = pins.get(i + 1);
        if (first + second == 10) {
          if (known && i + 2 < pins.size()) {
            running += 10 + pins.get(i + 2);
            totals[f] = running;
          } else {
            known = false;
          }
        } else if (known) {
          running += first + second;
          totals[f] = running;
        }
        i += 2;
      } else {
        int n = Math.min(3, pins.size() - i);
        int sum = 0;
        for (int k = 0; k < n; k++) {
          marks[f][k] = String.valueOf(shown.get(i + k));
          sum += pins.get(i + k);
        }
        boolean bonus = n >= 2 && (first == 10 || first + pins.get(i + 1) == 10);
        boolean done = n == 3 || (n == 2 && !bonus);
        if (known && done) {
          totals[f] = running + sum;
        }
        i += n;
      }
    }
  }
}

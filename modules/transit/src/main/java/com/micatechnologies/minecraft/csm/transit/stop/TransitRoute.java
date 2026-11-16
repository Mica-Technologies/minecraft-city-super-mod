package com.micatechnologies.minecraft.csm.transit.stop;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import javax.annotation.Nullable;

/**
 * A bus route's id, carried as an int key so {@link BusDepartures}, which is all ints, needs no
 * change. An id is one to {@link #MAX_LENGTH} characters of A-Z and 0-9 ("12", "B", "M15").
 *
 * <ul>
 *   <li>A number 1 to 99 is its own key, the route number the stop flag's plates have always
 *   stored, so an old flag and every board reading it keep their routes and their countdowns.</li>
 *   <li>Any other id packs base 37 (0 for none, 1-10 the digits, 11-36 the letters) into a key of
 *   {@link #PACKED_BASE} or more. Its headway and destination in {@link BusDepartures} come from
 *   that key as they would from a number, so a lettered route is as steady as a numbered one.</li>
 * </ul>
 *
 * <p>{@link #label} turns a key back into its id; a board prints labels, never the key.</p>
 *
 * @since 2026.11
 */
public final class TransitRoute {

  /** The longest id. */
  public static final int MAX_LENGTH = 3;
  /** The smallest packed key: above every plain route number. */
  public static final int PACKED_BASE = 100;
  /** The most routes a stop-name sign or a display line is read for. */
  public static final int MAX_PER_LINE = 6;

  private static final String ALPHABET = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ";

  private TransitRoute() {
  }

  /**
   * The key of a route id, or 0 if it is not one: empty, too long, or holding anything but A-Z
   * and 0-9 (case does not matter). "07" is the route 7, as "7" is.
   *
   * @param id the id
   *
   * @return the key, or 0
   */
  public static int parse(@Nullable String id) {
    if (id == null) {
      return 0;
    }
    String s = id.trim().toUpperCase(Locale.ROOT);
    if (s.isEmpty() || s.length() > MAX_LENGTH) {
      return 0;
    }
    boolean digits = true;
    for (int i = 0; i < s.length(); i++) {
      char c = s.charAt(i);
      if (ALPHABET.indexOf(c) < 0) {
        return 0;
      }
      digits &= c >= '0' && c <= '9';
    }
    if (digits) {
      int n = Integer.parseInt(s);
      if (n >= 1 && n < PACKED_BASE) {
        return n;
      }
      if (n == 0) {
        return 0;
      }
      // a three-digit number packs like any other id; strip leading zeros first so "007" is 7
    }
    int packed = 0;
    for (int i = 0; i < s.length(); i++) {
      packed = packed * 37 + ALPHABET.indexOf(s.charAt(i)) + 1;
    }
    return PACKED_BASE + packed;
  }

  /**
   * The id a key stands for.
   *
   * @param key a key from {@link #parse}
   *
   * @return the id, or "" for 0 or a key no id makes
   */
  public static String label(int key) {
    if (key <= 0) {
      return "";
    }
    if (key < PACKED_BASE) {
      return Integer.toString(key);
    }
    int packed = key - PACKED_BASE;
    StringBuilder out = new StringBuilder();
    while (packed > 0) {
      int d = packed % 37;
      if (d == 0) {
        return "";
      }
      out.append(ALPHABET.charAt(d - 1));
      packed /= 37;
    }
    if (out.length() == 0 || out.length() > MAX_LENGTH) {
      return "";
    }
    return out.reverse().toString();
  }

  /**
   * The route ids in a line of text: every word of one to {@link #MAX_LENGTH} letters and digits,
   * in order, without repeats, up to {@link #MAX_PER_LINE}. Commas, slashes and other words are
   * passed over, so "Routes B, 30" reads as B and 30 only if "Routes" is longer than an id; a
   * caller wanting a stricter reading checks the line itself.
   *
   * @param line the line
   *
   * @return the keys
   */
  public static int[] parseLine(@Nullable String line) {
    List<Integer> keys = new ArrayList<>();
    if (line != null) {
      for (String word : line.split("[^A-Za-z0-9]+")) {
        int key = parse(word);
        if (key != 0 && !keys.contains(key)) {
          keys.add(key);
          if (keys.size() == MAX_PER_LINE) {
            break;
          }
        }
      }
    }
    int[] out = new int[keys.size()];
    for (int i = 0; i < out.length; i++) {
      out[i] = keys.get(i);
    }
    return out;
  }
}

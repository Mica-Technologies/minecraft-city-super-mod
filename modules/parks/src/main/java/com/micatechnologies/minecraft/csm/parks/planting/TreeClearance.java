package com.micatechnologies.minecraft.csm.parks.planting;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;
import net.minecraft.util.math.BlockPos;

/**
 * Space a planted tree must keep clear above chosen surfaces: so many blocks over a road, fewer
 * over a sidewalk, the way a street tree is pruned up to let traffic and people pass under it.
 *
 * <p>A rule names surface blocks by registry name, {@code *} matching any run of characters
 * ({@code somemod:road*:12}), and how many cells above the surface stay clear. A cell is kept
 * clear when, looking down its column, the first surface met matches a rule and the cell is no
 * higher above it than the rule's height. Looking down passes through what stands on a road or a
 * walk without being the ground (paint, bumps, a fallen-petal cover, posts, signs, railings):
 * anything that is not a full opaque cube, plus whatever a {@code skip} pattern names. A full
 * opaque cube that matches no rule is the ground of something else (a lawn, a planter, a roof)
 * and keeps nothing clear.</p>
 *
 * <p>The tree's own trunk column is never kept clear, so a trunk in a narrow strip between a
 * road and a walk still grows; only its crown is shaped. Kept-clear cells are only ever free
 * cells: what is already built is in the way whatever the rules say.</p>
 *
 * @since 2026.10
 */
public final class TreeClearance {

  /** No rules: nothing kept clear. */
  public static final TreeClearance NONE = new TreeClearance(Collections.emptyList(),
      Collections.emptyList());

  /** What the clearance needs to know of a cell below the tree. */
  public interface Ground {

    /** The registry name of the block there ({@code namespace:path}). */
    String id(BlockPos pos);

    /** Whether looking down passes through it: anything but a full opaque cube. */
    boolean seeThrough(BlockPos pos);
  }

  private static final class Rule {

    final Pattern pattern;
    final int height;

    Rule(Pattern pattern, int height) {
      this.pattern = pattern;
      this.height = height;
    }
  }

  private final List<Rule> rules;
  private final List<Pattern> skips;
  private final int maxHeight;
  /** Rule height by block id, or -1 for none; -2 for an id a skip pattern names. */
  private final Map<String, Integer> byId = new HashMap<>();

  private TreeClearance(List<Rule> rules, List<Pattern> skips) {
    this.rules = rules;
    this.skips = skips;
    int max = 0;
    for (Rule r : rules) {
      max = Math.max(max, r.height);
    }
    this.maxHeight = max;
  }

  /**
   * Parses the rules.
   *
   * @param clear comma-separated {@code pattern:height} rules; the height follows the last colon,
   *              so a pattern may name its namespace. May be null or empty
   * @param skip  comma-separated patterns of blocks to look through as well. May be null or empty
   *
   * @return the clearance
   *
   * @throws IllegalArgumentException on a rule without a height
   */
  public static TreeClearance parse(String clear, String skip) {
    List<Rule> rules = new ArrayList<>();
    for (String part : split(clear)) {
      int colon = part.lastIndexOf(':');
      if (colon <= 0 || colon == part.length() - 1) {
        throw new IllegalArgumentException("Clearance rule '" + part
            + "' needs a height: pattern:height");
      }
      int height;
      try {
        height = Integer.parseInt(part.substring(colon + 1).trim());
      } catch (NumberFormatException e) {
        throw new IllegalArgumentException("Clearance rule '" + part
            + "' has no whole-number height");
      }
      if (height < 1 || height > 64) {
        throw new IllegalArgumentException("Clearance rule '" + part
            + "': height must be 1 to 64");
      }
      rules.add(new Rule(glob(part.substring(0, colon)), height));
    }
    List<Pattern> skips = new ArrayList<>();
    for (String part : split(skip)) {
      skips.add(glob(part));
    }
    return rules.isEmpty() ? NONE : new TreeClearance(rules, skips);
  }

  private static List<String> split(String spec) {
    List<String> out = new ArrayList<>();
    if (spec != null) {
      for (String s : spec.split(",")) {
        if (!s.trim().isEmpty()) {
          out.add(s.trim());
        }
      }
    }
    return out;
  }

  /** A registry-name pattern: {@code *} is any run, a name without a namespace is minecraft's. */
  private static Pattern glob(String pattern) {
    String p = pattern.trim().toLowerCase(Locale.ROOT);
    if (p.indexOf(':') < 0 && !p.startsWith("*")) {
      p = "minecraft:" + p;
    }
    StringBuilder re = new StringBuilder();
    String[] pieces = p.split("\\*", -1);
    for (int i = 0; i < pieces.length; i++) {
      if (i > 0) {
        re.append(".*");
      }
      if (!pieces[i].isEmpty()) {
        re.append(Pattern.quote(pieces[i]));
      }
    }
    return Pattern.compile(re.toString());
  }

  /** Whether there are any rules. */
  public boolean isEmpty() {
    return rules.isEmpty();
  }

  /**
   * Whether a cell is kept clear.
   *
   * @param at     the cell, in the world
   * @param trunk  the base of the trunk; its column is never kept clear
   * @param ground the world below
   *
   * @return whether the tree must not grow into the cell
   */
  public boolean keepsClear(BlockPos at, BlockPos trunk, Ground ground) {
    if (rules.isEmpty() || at.getX() == trunk.getX() && at.getZ() == trunk.getZ()) {
      return false;
    }
    for (int d = 1; d <= maxHeight && at.getY() - d >= 0; d++) {
      BlockPos below = at.down(d);
      String id = ground.id(below);
      int h = heightOf(id);
      if (h >= 0) {
        return d <= h;
      }
      if (h == -1 && !ground.seeThrough(below)) {
        return false;
      }
    }
    return false;
  }

  private int heightOf(String id) {
    Integer known = byId.get(id);
    if (known != null) {
      return known;
    }
    String lower = id.toLowerCase(Locale.ROOT);
    int h = -1;
    for (Rule r : rules) {
      if (r.pattern.matcher(lower).matches()) {
        h = Math.max(h, r.height);
      }
    }
    if (h < 0) {
      for (Pattern s : skips) {
        if (s.matcher(lower).matches()) {
          h = -2;
          break;
        }
      }
    }
    byId.put(id, h);
    return h;
  }
}

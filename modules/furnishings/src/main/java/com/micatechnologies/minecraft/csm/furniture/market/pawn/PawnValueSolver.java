package com.micatechnologies.minecraft.csm.furniture.market.pawn;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Works out what every item is worth from what its recipes cost, starting from a table of fixed
 * values. Knows nothing of Minecraft: items are keys, {@code modid:item@meta} for one subtype and
 * {@code modid:item} for any subtype, and a recipe is an output, a count and its input slots.
 *
 * <p>An item's value is the cheapest way to make it: its cheapest recipe's inputs (each slot at
 * its cheapest choice, less any container the recipe hands back) plus the recipe's extra cost,
 * divided by how many it makes. Taking the cheapest everywhere means no item is ever worth more
 * than another way to make it, so nothing can be crafted the expensive way and pawned at a
 * profit. Values only ever fall as the passes run, so loops (a block and its nine ingots, wool
 * and string) settle; a loop that loses value each way round drifts towards nothing, which is
 * the safe direction, and is stopped by the pass limit.</p>
 *
 * <p>An item that is an input somewhere but that no recipe makes and no table fixes (a raw
 * material from another mod) gets the default value, and the passes run again from there.</p>
 *
 * @since 2026.10
 */
public final class PawnValueSolver {

  /** The most passes; a recipe tree is far shallower, so only a lossy loop reaches it. */
  static final int MAX_PASSES = 100;

  /** Values below this are nothing: what a lossy loop drifts towards. */
  static final double NEGLIGIBLE = 0.0001;

  /** One input slot: any one of its alternatives, {@code count} times. */
  public static final class Input {

    final String[] alternatives;
    /** Per alternative, the container key handed back (a bucket), or null. */
    final String[] refunds;
    final int count;

    public Input(String[] alternatives, String[] refunds, int count) {
      this.alternatives = alternatives;
      this.refunds = refunds;
      this.count = count;
    }

    public Input(String alternative, int count) {
      this(new String[]{alternative}, new String[1], count);
    }
  }

  /** A way to make {@code outputCount} of {@code output}. */
  public static final class Recipe {

    final String output;
    final int outputCount;
    final Input[] inputs;
    final double extraCost;
    /** What kind of recipe, for explaining a value: crafting, smelting, Fabricator. */
    final String via;

    public Recipe(String output, int outputCount, Input[] inputs, double extraCost, String via) {
      this.output = output;
      this.outputCount = Math.max(1, outputCount);
      this.inputs = inputs;
      this.extraCost = extraCost;
      this.via = via;
    }

    public String getOutput() {
      return output;
    }

    public int getOutputCount() {
      return outputCount;
    }

    public String getVia() {
      return via;
    }
  }

  /** How an item got its value. */
  public enum Source {
    /** Fixed by the values file. */
    FIXED,
    /** Its cheapest recipe. */
    RECIPE,
    /** No recipe and no fixed value: the default. */
    DEFAULT
  }

  /** The solved values. */
  public static final class Result {

    private final Map<String, Double> values;
    private final Map<String, Double> anyMin;
    private final Map<String, Double> fixedAny;
    private final Map<String, Recipe> best;
    private final Set<String> defaulted;
    private final int passes;

    Result(Map<String, Double> values, Map<String, Double> anyMin, Map<String, Double> fixedAny,
        Map<String, Recipe> best, Set<String> defaulted, int passes) {
      this.values = values;
      this.anyMin = anyMin;
      this.fixedAny = fixedAny;
      this.best = best;
      this.defaulted = defaulted;
      this.passes = passes;
    }

    /**
     * An item's value: the subtype's own, else a value fixed for every subtype, else the
     * cheapest subtype's.
     *
     * @return the value, or NaN if the item has none
     */
    public double value(String key) {
      Double v = lookup(values, anyMin, fixedAny, key);
      return v == null ? Double.NaN : v;
    }

    /** How {@code key} got its value, or null if it has none. */
    public Source source(String key) {
      if (Double.isNaN(value(key))) {
        return null;
      }
      if (best.containsKey(key)) {
        return Source.RECIPE;
      }
      if (defaulted.contains(key) || defaulted.contains(item(key))) {
        return Source.DEFAULT;
      }
      return values.containsKey(key) || fixedAny.containsKey(item(key)) ? Source.FIXED
          : Source.RECIPE;
    }

    /** The cheapest recipe for {@code key}, or null if its value is not a recipe's. */
    public Recipe bestRecipe(String key) {
      return best.get(key);
    }

    /** The cheapest choice for one input slot of a recipe, for explaining it. */
    public String cheapestAlternative(Input input) {
      String pick = null;
      double low = Double.POSITIVE_INFINITY;
      for (int i = 0; i < input.alternatives.length; i++) {
        double c = alternativeCost(values, anyMin, fixedAny, input, i);
        if (c < low) {
          low = c;
          pick = input.alternatives[i];
        }
      }
      return pick;
    }

    /** Every key with a value of its own, for a dump. */
    public Set<String> keys() {
      return Collections.unmodifiableSet(values.keySet());
    }

    public int getPasses() {
      return passes;
    }
  }

  private PawnValueSolver() {
  }

  /**
   * Solves.
   *
   * @param fixed        values that recipes never change, by key (a key without a subtype fixes
   *                     every subtype)
   * @param recipes      every way to make something
   * @param defaultValue the value of an input nothing makes; 0 or less leaves such inputs
   *                     without one
   */
  public static Result solve(Map<String, Double> fixed, List<Recipe> recipes,
      double defaultValue) {
    Map<String, Double> values = new HashMap<>();
    Map<String, Double> fixedAny = new HashMap<>();
    for (Map.Entry<String, Double> e : fixed.entrySet()) {
      if (e.getValue() == null || e.getValue() < 0) {
        continue;
      }
      if (e.getKey().indexOf('@') < 0) {
        fixedAny.put(e.getKey(), e.getValue());
      } else {
        values.put(e.getKey(), e.getValue());
      }
    }
    Set<String> pinned = new HashSet<>(values.keySet());
    Map<String, Double> anyMin = new HashMap<>();
    for (Map.Entry<String, Double> e : values.entrySet()) {
      anyMin.merge(item(e.getKey()), e.getValue(), Math::min);
    }
    Map<String, Recipe> best = new HashMap<>();

    int passes = relax(recipes, values, anyMin, fixedAny, pinned, best);

    // Inputs nothing makes and nothing fixes: the default, then again from there.
    Set<String> defaulted = new HashSet<>();
    if (defaultValue > 0) {
      Set<String> made = new HashSet<>();
      Set<String> madeItems = new HashSet<>();
      for (Recipe r : recipes) {
        made.add(r.output);
        madeItems.add(item(r.output));
      }
      for (Recipe r : recipes) {
        for (Input in : r.inputs) {
          for (String alt : in.alternatives) {
            if (lookup(values, anyMin, fixedAny, alt) != null) {
              continue;
            }
            boolean any = alt.indexOf('@') < 0;
            if (any ? madeItems.contains(alt) : made.contains(alt)) {
              continue;
            }
            if (any) {
              fixedAny.put(alt, defaultValue);
            } else {
              values.put(alt, defaultValue);
              pinned.add(alt);
              anyMin.merge(item(alt), defaultValue, Math::min);
            }
            defaulted.add(alt);
          }
        }
      }
      if (!defaulted.isEmpty()) {
        passes += relax(recipes, values, anyMin, fixedAny, pinned, best);
      }
    }
    return new Result(values, anyMin, fixedAny, best, defaulted, passes);
  }

  private static int relax(List<Recipe> recipes, Map<String, Double> values,
      Map<String, Double> anyMin, Map<String, Double> fixedAny, Set<String> pinned,
      Map<String, Recipe> best) {
    int pass = 0;
    boolean changed = true;
    while (changed && pass < MAX_PASSES) {
      changed = false;
      pass++;
      for (Recipe r : recipes) {
        if (pinned.contains(r.output) || fixedAny.containsKey(item(r.output))) {
          continue;
        }
        double cost = r.extraCost;
        for (Input in : r.inputs) {
          double low = Double.POSITIVE_INFINITY;
          for (int i = 0; i < in.alternatives.length; i++) {
            low = Math.min(low, alternativeCost(values, anyMin, fixedAny, in, i));
          }
          cost += low * in.count;
          if (cost == Double.POSITIVE_INFINITY) {
            break;
          }
        }
        if (cost == Double.POSITIVE_INFINITY) {
          continue;
        }
        double each = cost / r.outputCount;
        if (each < NEGLIGIBLE) {
          each = 0;
        }
        Double current = values.get(r.output);
        if (current == null || each < current - 1e-9 * Math.max(1, current)) {
          values.put(r.output, each);
          anyMin.merge(item(r.output), each, Math::min);
          best.put(r.output, r);
          changed = true;
        }
      }
    }
    return pass;
  }

  private static double alternativeCost(Map<String, Double> values, Map<String, Double> anyMin,
      Map<String, Double> fixedAny, Input in, int i) {
    Double v = lookup(values, anyMin, fixedAny, in.alternatives[i]);
    if (v == null) {
      return Double.POSITIVE_INFINITY;
    }
    String refund = in.refunds == null ? null : in.refunds[i];
    if (refund != null) {
      Double back = lookup(values, anyMin, fixedAny, refund);
      if (back != null) {
        return Math.max(0, v - back);
      }
    }
    return v;
  }

  private static Double lookup(Map<String, Double> values, Map<String, Double> anyMin,
      Map<String, Double> fixedAny, String key) {
    if (key.indexOf('@') < 0) {
      Double fixed = fixedAny.get(key);
      Double low = anyMin.get(key);
      if (fixed == null) {
        return low;
      }
      return low == null ? fixed : Math.min(fixed, low);
    }
    Double v = values.get(key);
    return v != null ? v : fixedAny.get(item(key));
  }

  /** {@code modid:item} of a key. */
  static String item(String key) {
    int at = key.indexOf('@');
    return at < 0 ? key : key.substring(0, at);
  }

  /** The cheapest recipe chain behind a key, one line a step, for {@code /csmpawn explain}. */
  public static List<String> explain(Result result, String key, int maxDepth) {
    List<String> lines = new ArrayList<>();
    explain(result, key, 1, 0, maxDepth, new HashSet<>(), lines);
    return lines;
  }

  private static void explain(Result result, String key, int count, int depth, int maxDepth,
      Set<String> seen, List<String> lines) {
    StringBuilder pad = new StringBuilder();
    for (int i = 0; i < depth; i++) {
      pad.append("  ");
    }
    // Any subtype: follow the cheapest one, unless a value is fixed for them all.
    if (key.indexOf('@') < 0 && !result.fixedAny.containsKey(key)) {
      String cheapest = null;
      for (Map.Entry<String, Double> e : result.values.entrySet()) {
        if (item(e.getKey()).equals(key)
            && (cheapest == null || e.getValue() < result.values.get(cheapest))) {
          cheapest = e.getKey();
        }
      }
      if (cheapest != null) {
        key = cheapest;
      }
    }
    double v = result.value(key);
    Source source = result.source(key);
    Recipe r = result.bestRecipe(key);
    if (source == null) {
      lines.add(pad + (count > 1 ? count + " x " : "") + key + ": no value");
      return;
    }
    String head = pad + (count > 1 ? count + " x " : "") + key + " = "
        + String.format(Locale.ROOT, "%.4f", v);
    if (r == null || !seen.add(key)) {
      lines.add(head + " (" + source.name().toLowerCase(Locale.ROOT) + ")");
      return;
    }
    lines.add(head + " (" + r.via + ", makes " + r.outputCount
        + (r.extraCost > 0 ? String.format(Locale.ROOT, ", +%.2f", r.extraCost) : "") + ")");
    if (depth >= maxDepth) {
      return;
    }
    // A shaped recipe lists each grid slot; show each input once with how many.
    Map<String, Integer> picks = new LinkedHashMap<>();
    for (Input in : r.inputs) {
      String pick = result.cheapestAlternative(in);
      if (pick != null) {
        picks.merge(pick, in.count, Integer::sum);
      }
    }
    for (Map.Entry<String, Integer> e : picks.entrySet()) {
      explain(result, e.getKey(), e.getValue(), depth + 1, maxDepth, seen, lines);
    }
  }
}

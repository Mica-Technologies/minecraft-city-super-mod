package com.micatechnologies.minecraft.csm.parks.planters;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * The mixed bed {@code /csmparks planter fill ... mix} plants: a curated list of CSM plantings
 * that read well together in a city planter (low shrubs, ornamental grasses and bedding
 * flowers, nothing taller than a block, nothing regional enough to look out of place), one
 * picked for each planter by its position and a seed.
 *
 * <p>The pick is a hash, not a random draw, so the same box and seed always plant the same bed
 * whatever order the planters are visited in, and a generator can re-run a fill and get what
 * it got before. Pure, so that is unit tested.</p>
 *
 * @since 2026.10
 */
public final class PlanterMix {

  /** The plants a mixed bed is drawn from, by CSM registry name. */
  public static final List<String> PLANTS = Collections.unmodifiableList(Arrays.asList(
      "shrub_lavender",
      "shrub_boxwood",
      "grass_blue_fescue",
      "grass_fountain",
      "flower_marigolds",
      "flower_zinnias",
      "shrub_rosemary",
      "flower_coreopsis",
      "grass_pink_muhly",
      "shrub_hydrangea",
      "flower_tulips",
      "shrub_star_jasmine",
      "flower_marguerite",
      "grass_hakone"));

  private PlanterMix() {
  }

  /**
   * The plant a mixed bed puts at a position.
   *
   * @param x    the planter's x
   * @param y    its y
   * @param z    its z
   * @param seed the fill's seed
   * @return a CSM registry name from {@link #PLANTS}, without its namespace
   */
  public static String pick(int x, int y, int z, long seed) {
    long h = seed * 0x9E3779B97F4A7C15L;
    h ^= x * 0xC2B2AE3D27D4EB4FL;
    h = mix(h);
    h ^= y * 0x165667B19E3779F9L;
    h = mix(h);
    h ^= z * 0xD6E8FEB86659FD93L;
    h = mix(h);
    return PLANTS.get((int) Math.floorMod(h, (long) PLANTS.size()));
  }

  /** SplitMix64's finaliser: every input bit reaches every output bit. */
  private static long mix(long z) {
    z = (z ^ (z >>> 30)) * 0xBF58476D1CE4E5B9L;
    z = (z ^ (z >>> 27)) * 0x94D049BB133111EBL;
    return z ^ (z >>> 31);
  }
}

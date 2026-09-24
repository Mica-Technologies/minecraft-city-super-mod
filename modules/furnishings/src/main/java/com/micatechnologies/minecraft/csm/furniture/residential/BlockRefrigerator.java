package com.micatechnologies.minecraft.csm.furniture.residential;

import com.micatechnologies.minecraft.csm.novelties.FurnishingsSounds;
import net.minecraft.block.properties.PropertyBool;

/**
 * A French-door refrigerator, two blocks tall ({@link BlockResidentialTall}): placed and broken
 * as one piece; the lower half holds the 27 slots, and right-clicking either half opens them,
 * with the sound of the door seal.
 *
 * <p>The halves are drawn by {@code gen_furniture_kitchen.py} from one model cut at the block
 * line. {@link #UPPER} is stored, in the bit above the facing.</p>
 *
 * @since 2026.9
 */
public class BlockRefrigerator extends BlockResidentialTall {

  /** Whether this is the upper half. */
  public static final PropertyBool UPPER = BlockResidentialTall.UPPER;

  /** Slots: three rows, a chest's worth. */
  private static final int SLOTS = 27;
  /** The whole refrigerator's box facing north: 1.81 m tall, the upper half's top at 13. */
  private static final int[] BOX = {0, 0, 1, 16, 29, 16};

  /**
   * Constructs a refrigerator.
   *
   * @param registryName its registry name, ending in its finish
   */
  public BlockRefrigerator(String registryName) {
    super(registryName, BOX, false, SLOTS, FurnishingsSounds.FRIDGE_OPEN,
        FurnishingsSounds.FRIDGE_CLOSE);
  }
}

package com.micatechnologies.minecraft.csm.hvac;

import net.minecraft.block.Block;

/**
 * A large packaged rooftop unit's master cell ({@link BlockHvacPackagedRtu}): a combination unit
 * of its size's capacity (3,000 /s for the large unit, two and a half rooftop units), delivered
 * only through vents, which may be up to 100 blocks away.
 *
 * @author Mica Technologies
 * @since 2026.11
 */
public class TileEntityHvacPackagedRtu extends TileEntityHvacComboUnit {

  @Override
  public float getHeatCapacity() {
    Block b = world != null ? getBlockType() : null;
    return b instanceof BlockHvacPackagedRtu ? ((BlockHvacPackagedRtu) b).getSize().capacity
        : BlockHvacPackagedRtu.LARGE.capacity;
  }

  @Override
  public boolean isDuctedOnly() {
    return true;
  }

  @Override
  public int getMaxVentLinkDistance() {
    return 100;
  }
}

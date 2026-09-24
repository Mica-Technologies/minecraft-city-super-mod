package com.micatechnologies.minecraft.csm.lifesafety;

import net.minecraft.util.math.AxisAlignedBB;

/**
 * Factory for horn strobes that offer more selectable tones than a block's metadata can hold, and
 * so keep the choice in a {@link TileEntityFireAlarmSoundIndex}. Sneak-right-click cycles the tone
 * and reports the new one.
 *
 * <p>The counterpart to {@link BlockFireAlarmSounderStrobeFactory}, which is for appliances whose
 * sound is fixed or lives in metadata. Instances differ only in registry name, bounding box,
 * strobe lens and tone set, so they need no class of their own. Everything but the strobe is
 * {@link BlockFireAlarmSoundIndexFactory}'s.</p>
 */
public class BlockFireAlarmSoundIndexStrobeFactory extends BlockFireAlarmSoundIndexFactory
    implements IStrobeBlock {

  private final float[] strobeLensFrom;
  private final float[] strobeLensTo;

  public BlockFireAlarmSoundIndexStrobeFactory(String registryName, AxisAlignedBB boundingBox,
      float[] strobeLensFrom, float[] strobeLensTo, String[] soundResourceNames,
      String[] soundDisplayNames) {
    super(registryName, boundingBox, soundResourceNames, soundDisplayNames);
    this.strobeLensFrom = strobeLensFrom;
    this.strobeLensTo = strobeLensTo;
  }

  @Override
  public float[] getStrobeLensFrom() {
    return strobeLensFrom;
  }

  @Override
  public float[] getStrobeLensTo() {
    return strobeLensTo;
  }
}

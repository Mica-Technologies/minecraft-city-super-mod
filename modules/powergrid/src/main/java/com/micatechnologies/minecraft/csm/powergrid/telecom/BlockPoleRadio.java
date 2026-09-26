package com.micatechnologies.minecraft.csm.powergrid.telecom;

import com.micatechnologies.minecraft.csm.codeutils.ICsmPoleFitted;
import com.micatechnologies.minecraft.csm.powergrid.services.BlockUtilityFixture;
import net.minecraft.util.BlockRenderLayer;

/**
 * A small cell's radio unit on the side of a street pole, below its canister antenna: a finned
 * radio on a two-strap bracket whose straps reach back to the pole's skin. It is
 * {@link ICsmPoleFitted}, so its actual state carries the width of the pole behind it and the
 * blockstate picks the bracket drawn for that pole; against anything that is not a pole it takes
 * the widest pole's bracket, which reaches least far. Placed against the pole's side, facing the
 * player. Twelve states, no tile entity.
 *
 * @since 2026.9
 */
public class BlockPoleRadio extends BlockUtilityFixture implements ICsmPoleFitted {

  /**
   * @param registryName its registry name
   * @param box          its box facing north, in sixteenths
   */
  public BlockPoleRadio(String registryName, double[] box) {
    super(registryName, box);
  }

  @Override
  public BlockRenderLayer getBlockRenderLayer() {
    return BlockRenderLayer.SOLID;
  }
}

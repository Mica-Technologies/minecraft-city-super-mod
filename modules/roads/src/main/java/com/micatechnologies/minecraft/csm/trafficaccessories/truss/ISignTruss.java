package com.micatechnologies.minecraft.csm.trafficaccessories.truss;

import net.minecraft.block.state.IBlockState;

/**
 * An overhead sign truss, as what is hung on it sees it: where its top is, for the hangers to
 * reach up to, and how far in from its cell's face its front chords are, for their clamps to
 * reach back to.
 *
 * @since 2026.10
 */
public interface ISignTruss {

  /**
   * The top of the truss in its own cell, in sixteenths, or a negative number if nothing hangs
   * from this block (a support frame's post).
   *
   * @param state the truss's state
   *
   * @return the top, 0 to 16, or negative
   */
  float getSignTrussTop(IBlockState state);

  /**
   * How far the truss's front chords are in from the face of its cell, in sixteenths.
   *
   * @param state the truss's state
   *
   * @return the inset
   */
  float getSignTrussFrontInset(IBlockState state);
}

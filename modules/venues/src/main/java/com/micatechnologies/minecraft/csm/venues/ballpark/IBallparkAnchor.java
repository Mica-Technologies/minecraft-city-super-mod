package com.micatechnologies.minecraft.csm.venues.ballpark;

import net.minecraft.block.state.IBlockState;
import net.minecraft.util.EnumFacing;

/**
 * A block a ballpark picture is laid out round: the field's marking anchors and the pitcher's
 * mound. {@link BlockBallparkPainted} cells find the nearest one of their family and show its
 * picture's piece.
 *
 * @since 2026.10
 */
public interface IBallparkAnchor {

  /** The pictures it anchors. */
  BallparkFamily family();

  /** The kind index of the picture, for this anchor's state (group * 4 + kind). */
  int kindIndex(IBlockState state);

  /** Whether it is a third base side twin, drawing its kind's picture mirrored. */
  boolean third();

  /** The way the picture's forward runs. */
  EnumFacing pictureFacing(IBlockState state);
}

package com.micatechnologies.minecraft.csm.codeutils;

import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

/**
 * Resolves the height of the surface a block is standing on, so that a block placed in the cell
 * above a road can settle onto it instead of floating above it.
 *
 * <p>Road surfaces are not all one block tall. A road that climbs is built from blocks whose top
 * face sits at some fraction of the way up their cell, and a device placed on such a road is
 * placed in the cell <em>above</em> it and must be pulled back down by whatever the road did not
 * fill. That is what {@link #offsetFor(IBlockAccess, BlockPos)} returns.</p>
 *
 * <p>Nothing here knows or asks which mod supplied the surface. The height is read from the
 * bounding box the block below already reports through the vanilla block API, which means the
 * same code lands a traffic cone correctly on a road that steps in sixteenths, on a vanilla
 * bottom slab, and on a snow layer, with no dependency on anything.</p>
 *
 * <p><b>Three consumers must agree.</b> A block using this has to apply the same offset to its
 * rendering ({@code getOffset}), to its bounding box (which in this codebase feeds both selection
 * and collision, see {@link AbstractBlock#getBoundingBox}), and to any tile entity renderer it
 * draws on top. A tile entity renderer draws in world space and knows nothing about
 * {@code getOffset}, so leaving it out puts the lit part of a device at a different height from
 * the device.</p>
 *
 * @version 1.0
 * @since 2026.9
 */
public final class RoadSurfaceHeight {

  /**
   * The surface height that means "do not move": the block below fills its cell, or is not
   * something that should be settled onto at all.
   *
   * @since 1.0
   */
  public static final double NO_OFFSET = 1.0;

  /**
   * The furthest a block may ever be pulled down, in blocks.
   *
   * <p>One block covers settling onto a road of any height. The second covers settling onto a
   * flat marking laid over such a road, which is itself drawn a cell high and pulled down, so its
   * own top face can sit below its cell. Anything asking for more than that is a surface this was
   * not written for, and dropping a device two metres through the world is worse than leaving it
   * where the player put it.</p>
   *
   * @since 1.0
   */
  private static final double MAX_DROP = 2.0;

  /**
   * How far a bounding box may fall short of its cell's horizontal extent and still count as a
   * full footprint, in blocks.
   *
   * @since 1.0
   */
  private static final double FOOTPRINT_TOLERANCE = 0.05;

  /**
   * Utility class; not instantiable.
   *
   * @since 1.0
   */
  private RoadSurfaceHeight() {
    throw new AssertionError("RoadSurfaceHeight is a utility class and must not be instantiated");
  }

  /**
   * Gets the height of the top face of the block below the given position, in that block's own
   * frame, or {@link #NO_OFFSET} if there is nothing there worth settling onto.
   *
   * <p>The cases, in the order they are tested:</p>
   * <ol>
   *   <li><b>Air.</b> Nothing to stand on, so nothing to settle onto.</li>
   *   <li><b>Another surface-aware block.</b> Devices sit on each other as placed rather than
   *       compounding one another's offsets, which also keeps this from recursing down a
   *       stack.</li>
   *   <li><b>Not a full footprint.</b> A torch, a fence post or a block reporting a degenerate
   *       empty box is not a surface; settling onto its top face would leave a device floating
   *       in the middle of its cell.</li>
   *   <li><b>Fills or overfills its cell.</b> Nothing to settle by.</li>
   *   <li><b>Anything else.</b> The top of its box — which may be below its own cell, because a
   *       flat marking laid on a road that climbs is itself drawn pulled down onto that road. A
   *       device settles onto the top of whatever is actually there, so it stands on the marking
   *       rather than through it, at every road height alike.</li>
   * </ol>
   *
   * @param world the block access to read from
   * @param pos   the position of the block that is settling, not of the surface below it
   *
   * @return the height of the surface below, or {@link #NO_OFFSET} if there is none
   *
   * @since 1.0
   */
  public static double surfaceBelow(IBlockAccess world, BlockPos pos) {
    if (world == null || pos == null) {
      return NO_OFFSET;
    }

    BlockPos below = pos.down();
    IBlockState state = world.getBlockState(below);
    if (state.getBlock().isAir(state, world, below)) {
      return NO_OFFSET;
    }
    if (state.getBlock() instanceof ICsmRoadSurfaceAware) {
      return NO_OFFSET;
    }

    AxisAlignedBB box;
    try {
      box = state.getBoundingBox(world, below);
    } catch (Exception e) {
      // A block that cannot describe itself here is not one to settle onto.
      return NO_OFFSET;
    }
    return surfaceFromBox(box);
  }

  /**
   * Gets the height of the surface a bounding box presents, or {@link #NO_OFFSET} if it does not
   * present one.
   *
   * <p>This is the whole of the decision described on
   * {@link #surfaceBelow(IBlockAccess, BlockPos)}, separated from reading the world so that it
   * can be exercised directly.</p>
   *
   * @param box the bounding box of the block below, in its own cell's frame
   *
   * @return the height of the surface it presents, or {@link #NO_OFFSET} if it presents none
   *
   * @since 1.0
   */
  public static double surfaceFromBox(AxisAlignedBB box) {
    if (box == null) {
      return NO_OFFSET;
    }

    boolean fullFootprint = box.minX <= FOOTPRINT_TOLERANCE
        && box.maxX >= 1.0 - FOOTPRINT_TOLERANCE
        && box.minZ <= FOOTPRINT_TOLERANCE
        && box.maxZ >= 1.0 - FOOTPRINT_TOLERANCE;
    if (!fullFootprint) {
      return NO_OFFSET;
    }

    if (box.maxY >= 1.0 || box.maxY <= -MAX_DROP + 1.0) {
      return NO_OFFSET;
    }
    return box.maxY;
  }

  /**
   * Gets the vertical offset a block must apply to settle onto a surface at the given height.
   *
   * @param surface the height of the surface, as returned by {@link #surfaceFromBox}
   *
   * @return the offset to apply, at most zero and never below {@code -}{@link #MAX_DROP}
   *
   * @since 1.0
   */
  public static double offsetForSurface(double surface) {
    if (surface >= NO_OFFSET) {
      return 0.0;
    }
    return Math.max(-(NO_OFFSET - surface), -MAX_DROP);
  }

  /**
   * Gets the vertical offset a block at the given position must apply to sit flush on the
   * surface below it.
   *
   * @param world the block access to read from
   * @param pos   the position of the block that is settling
   *
   * @return the offset to apply, at most zero and never below {@code -}{@link #MAX_DROP}
   *
   * @since 1.0
   */
  public static double offsetFor(IBlockAccess world, BlockPos pos) {
    return offsetForSurface(surfaceBelow(world, pos));
  }

  /**
   * Redraws a settling block when the surface beneath it changes.
   *
   * <p>The offset is read from a neighbour, so a change to that neighbour marks the neighbour's
   * chunk section for a rebuild and not the settling block's. The two are usually the same
   * section, but a device standing on the top block of one is not, which is exactly the case that
   * would be missed.</p>
   *
   * <p>Only a change directly below matters, so the column check is what keeps this from
   * redrawing on every unrelated neighbour update. The range covers one block either side, which
   * carries the update along a stack of settling blocks.</p>
   *
   * @param world   the world the block is in
   * @param pos     the position of the settling block
   * @param fromPos the position of the neighbour that changed
   *
   * @since 1.0
   */
  public static void markSurfaceRenderUpdate(World world, BlockPos pos, BlockPos fromPos) {
    if (world == null || pos == null || fromPos == null) {
      return;
    }
    if (fromPos.getX() != pos.getX() || fromPos.getZ() != pos.getZ()
        || fromPos.getY() >= pos.getY()) {
      return;
    }
    world.markBlockRangeForRenderUpdate(pos.down(), pos.up());
  }
}

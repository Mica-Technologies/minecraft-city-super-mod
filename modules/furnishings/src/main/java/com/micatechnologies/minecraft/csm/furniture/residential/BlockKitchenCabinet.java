package com.micatechnologies.minecraft.csm.furniture.residential;

import net.minecraft.block.state.IBlockState;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;

/**
 * A kitchen cabinet: a base cabinet under a countertop, a wall cabinet or open shelf, or an
 * island, drawn by {@code gen_furniture_kitchen.py}. Cabinets of the same {@link KitchenLine}
 * and finish join into one run whatever their fronts -- a drawer bank, a sink base and a
 * two-door base side by side share one countertop -- with the end panels and the countertop's
 * cut ends only where the run stops. A {@link BlockKitchenCorner} turns a base run.
 *
 * <p>Closed cabinets hold things (nine or eighteen slots a block) and sound their doors or
 * drawers; an open shelf holds nothing.</p>
 *
 * @since 2026.9
 */
public class BlockKitchenCabinet extends BlockResidentialStorage {

  private final KitchenLine line;

  /**
   * Constructs a cabinet.
   *
   * @param registryName its registry name, ending in its finish
   * @param box          its box facing north, in sixteenths
   * @param line         the run it joins
   * @param slots        how many slots it holds, a multiple of nine, or zero
   * @param front        what it opens with
   */
  public BlockKitchenCabinet(String registryName, int[] box, KitchenLine line, int slots,
      KitchenFront front) {
    super(registryName, box, slots, front.getOpenSound(), front.getCloseSound());
    this.line = line;
  }

  /**
   * The run this cabinet joins.
   *
   * @return its line
   */
  public KitchenLine getLine() {
    return line;
  }

  /**
   * Whether this cabinet, in {@code state}, takes a run facing {@code runFacing} that arrives
   * from {@code side} (the world direction from this cabinet to the run's next block). A
   * straight cabinet takes a run facing its own way on either side.
   *
   * @param state     this cabinet's state
   * @param side      the side the run arrives from
   * @param runFacing the way the run faces
   *
   * @return whether the run continues through this cabinet
   */
  public boolean acceptsRun(IBlockState state, EnumFacing side, EnumFacing runFacing) {
    EnumFacing f = state.getValue(FACING);
    return runFacing == f && side.getAxis() != f.getAxis();
  }

  /**
   * Whether a run facing {@code runFacing} goes on from {@code pos} into its neighbour past
   * {@code side}: a cabinet of this line and finish that takes it.
   *
   * @param world     the world
   * @param pos       this block
   * @param side      the side looked past
   * @param runFacing the way the run faces here
   *
   * @return whether it continues
   */
  protected boolean joins(IBlockAccess world, BlockPos pos, EnumFacing side,
      EnumFacing runFacing) {
    IBlockState other = world.getBlockState(pos.offset(side));
    if (!(other.getBlock() instanceof BlockKitchenCabinet)) {
      return false;
    }
    BlockKitchenCabinet cabinet = (BlockKitchenCabinet) other.getBlock();
    return cabinet.line == line && cabinet.getFinish().equals(getFinish())
        && cabinet.acceptsRun(other, side.getOpposite(), runFacing);
  }

  @Override
  protected boolean continues(IBlockAccess world, BlockPos pos, EnumFacing facing,
      EnumFacing side) {
    return joins(world, pos, side, facing);
  }
}

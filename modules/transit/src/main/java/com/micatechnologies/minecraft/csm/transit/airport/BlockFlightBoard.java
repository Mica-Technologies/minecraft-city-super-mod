package com.micatechnologies.minecraft.csm.transit.airport;

import com.micatechnologies.minecraft.csm.codeutils.CsmBlockStateContainer;
import com.micatechnologies.minecraft.csm.codeutils.ICsmTileEntityProvider;
import com.micatechnologies.minecraft.csm.transit.platform.BlockPlatformFixture;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

/**
 * A flight information display: a slim landscape monitor, departures or arrivals, listing the
 * airport's made-up schedule ({@link FlightSchedule}) seven flights at a time. Its bezel and the
 * screen's header and bands are baked; {@link TileEntityFlightBoardRenderer} draws the screen lit
 * and the rows, the column heads and the clock on it.
 *
 * <p>The monitor sits against the back of its block (the model's z = 16). With a wall behind it
 * that is where it hangs; with none, {@link #HUNG} (actual state) draws two rods up to the block
 * above, unless another board is there. Boards facing the same way side by side and stacked are
 * a bank, and each lists the page after the one to its reader's left (or above), so a bank of
 * six lists 42 flights ({@link TileEntityFlightBoard}).</p>
 *
 * @since 2026.9
 */
public class BlockFlightBoard extends BlockPlatformFixture implements ICsmTileEntityProvider {

  /** Nothing behind to hang on, and no board above: drawn on rods from the ceiling. */
  public static final PropertyBool HUNG = PropertyBool.create("hung");

  private final boolean arrivals;

  /**
   * Constructs a board.
   *
   * @param registryName its registry name
   * @param box          its box facing north, in sixteenths
   * @param arrivals     whether it lists arrivals rather than departures
   */
  public BlockFlightBoard(String registryName, double[] box, boolean arrivals) {
    super(registryName, box, false, 3);
    this.arrivals = arrivals;
    setDefaultState(getDefaultState().withProperty(HUNG, false));
  }

  /**
   * Whether the board lists arrivals.
   *
   * @return true for arrivals, false for departures
   */
  public boolean isArrivals() {
    return arrivals;
  }

  @Override
  @Nonnull
  protected BlockStateContainer createBlockState() {
    return new CsmBlockStateContainer(this, FACING, HUNG);
  }

  @Override
  @Nonnull
  @SuppressWarnings("deprecation")
  public IBlockState getActualState(@Nonnull IBlockState state, IBlockAccess world,
      BlockPos pos) {
    EnumFacing facing = state.getValue(FACING);
    BlockPos behind = pos.offset(facing.getOpposite());
    boolean wall = world.getBlockState(behind).getBlockFaceShape(world, behind, facing)
        == net.minecraft.block.state.BlockFaceShape.SOLID;
    boolean boardAbove = world.getBlockState(pos.up()).getBlock() instanceof BlockFlightBoard;
    return state.withProperty(HUNG, !wall && !boardAbove);
  }

  @Override
  public Class<? extends TileEntity> getTileEntityClass() {
    return TileEntityFlightBoard.class;
  }

  @Override
  public String getTileEntityName() {
    return "tileentityflightboard";
  }

  @Nullable
  @Override
  public TileEntity createNewTileEntity(@Nonnull World world, int meta) {
    return new TileEntityFlightBoard();
  }
}

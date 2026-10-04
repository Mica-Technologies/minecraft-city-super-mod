package com.micatechnologies.minecraft.csm.transit.airport;

import com.micatechnologies.minecraft.csm.codeutils.CsmBlockStateContainer;
import com.micatechnologies.minecraft.csm.codeutils.ICsmTileEntityProvider;
import com.micatechnologies.minecraft.csm.transit.panel.CellPanel;
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
 * The large flight information board: a screen of any size up to {@value #MAX_WIDTH} x
 * {@value #MAX_HEIGHT} blocks, built from cells of this block placed side by side and stacked, all
 * facing the same way, for a terminal whose ceiling is too high for the one-block board
 * ({@link BlockFlightBoard}) to be read. Departures and arrivals are two blocks, which do not join
 * each other.
 *
 * <p>Cells join as the large hanging sign's do ({@link CellPanel}): the bottom-left cell as read
 * is the controller, and its renderer ({@link TileEntityFlightBoardLargeRenderer}) draws the
 * screen and every word once across the whole panel, with hanger rods to the ceiling when there
 * is nothing behind the panel. Each cell's baked model is a dark bezel slab against the back of
 * its block with a frame on the panel's outer edges only ({@link #EDGE_LEFT}, {@link #EDGE_RIGHT},
 * {@link #EDGE_TOP}, {@link #EDGE_BOTTOM}, actual state, named as the reader sees them). Nothing
 * is set up and nothing is saved: what it lists is {@link FlightSchedule}'s.</p>
 *
 * <p>The model comes from {@code dev-env-utils/scripts/gen_transit_airport.py}'s
 * {@code boards_large()}.</p>
 *
 * @since 2026.10
 */
public class BlockFlightBoardLarge extends BlockPlatformFixture implements ICsmTileEntityProvider {

  /** The widest board, in blocks. */
  public static final int MAX_WIDTH = 8;
  /** The tallest board, in blocks. */
  public static final int MAX_HEIGHT = 5;

  /** No cell of the board to the reader's left: the frame runs down this side. */
  public static final PropertyBool EDGE_LEFT = PropertyBool.create("edge_left");
  public static final PropertyBool EDGE_RIGHT = PropertyBool.create("edge_right");
  public static final PropertyBool EDGE_TOP = PropertyBool.create("edge_top");
  public static final PropertyBool EDGE_BOTTOM = PropertyBool.create("edge_bottom");

  private final boolean arrivals;

  /**
   * Constructs a board.
   *
   * @param registryName its registry name
   * @param arrivals     whether it lists arrivals rather than departures
   */
  public BlockFlightBoardLarge(String registryName, boolean arrivals) {
    super(registryName, new double[]{0, 0, 12.4, 16, 16, 16}, false, 3);
    this.arrivals = arrivals;
    setDefaultState(getDefaultState().withProperty(EDGE_LEFT, true)
        .withProperty(EDGE_RIGHT, true).withProperty(EDGE_TOP, true)
        .withProperty(EDGE_BOTTOM, true));
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
    return new CsmBlockStateContainer(this, FACING, EDGE_LEFT, EDGE_RIGHT, EDGE_TOP,
        EDGE_BOTTOM);
  }

  @Override
  @Nonnull
  @SuppressWarnings("deprecation")
  public IBlockState getActualState(@Nonnull IBlockState state, IBlockAccess world,
      BlockPos pos) {
    CellPanel.noteActualState(world);
    EnumFacing facing = state.getValue(FACING);
    EnumFacing left = CellPanel.leftOf(facing);
    return state
        .withProperty(EDGE_LEFT, !CellPanel.joins(world, pos.offset(left), this, facing))
        .withProperty(EDGE_RIGHT,
            !CellPanel.joins(world, pos.offset(left.getOpposite()), this, facing))
        .withProperty(EDGE_TOP, !CellPanel.joins(world, pos.up(), this, facing))
        .withProperty(EDGE_BOTTOM, !CellPanel.joins(world, pos.down(), this, facing));
  }

  @Override
  public Class<? extends TileEntity> getTileEntityClass() {
    return TileEntityFlightBoardLarge.class;
  }

  @Override
  public String getTileEntityName() {
    return "tileentityflightboardlarge";
  }

  @Nullable
  @Override
  public TileEntity createNewTileEntity(@Nonnull World world, int meta) {
    return new TileEntityFlightBoardLarge();
  }
}

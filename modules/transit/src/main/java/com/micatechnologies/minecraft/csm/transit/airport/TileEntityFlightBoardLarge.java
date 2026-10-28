package com.micatechnologies.minecraft.csm.transit.airport;

import com.micatechnologies.minecraft.csm.codeutils.AbstractTileEntity;
import com.micatechnologies.minecraft.csm.codeutils.CsmPerformance;
import com.micatechnologies.minecraft.csm.transit.panel.PanelLayout;
import net.minecraft.block.Block;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * A cell of a large flight information board ({@link BlockFlightBoardLarge}). It saves nothing:
 * what the board lists follows from the world's clock. On the client it holds the panel's
 * layout ({@link PanelLayout}: whether this cell is the controller, the board's size, the rods),
 * worked out again only when a nearby chunk section was rebuilt or two seconds have passed.
 *
 * @since 2026.10
 */
public class TileEntityFlightBoardLarge extends AbstractTileEntity {

  /** Client only, never saved: the board this cell is in. */
  private final PanelLayout layout = new PanelLayout(BlockFlightBoardLarge.class,
      BlockFlightBoardLarge.MAX_WIDTH, BlockFlightBoardLarge.MAX_HEIGHT, true);

  /**
   * Client only, never saved, the controller's: the rows last drawn, the version of them the
   * board's text list was compiled for, and what its screen list was compiled for (the layout and
   * the screen sprite's corners, which move when the atlas is stitched again)
   * ({@link TileEntityFlightBoardLargeRenderer}).
   */
  final transient FlightBoardPage clientPage = new FlightBoardPage();
  transient int clientCompiledVersion;
  transient long clientScreenLayout = -1L;
  final transient float[] clientScreenUv = new float[4];

  /**
   * Works the layout out again if it may have changed. The renderer calls this every frame before
   * reading it.
   */
  @SideOnly(Side.CLIENT)
  public void refreshLayout() {
    layout.refresh(world, pos);
  }

  /** The board this cell is in, as last worked out. */
  public PanelLayout getLayout() {
    return layout;
  }

  /** Whether the board lists arrivals; false if this is no longer a board. */
  public boolean isArrivals() {
    Block block = world == null ? null : world.getBlockState(pos).getBlock();
    return block instanceof BlockFlightBoardLarge && ((BlockFlightBoardLarge) block).isArrivals();
  }

  @Override
  @SideOnly(Side.CLIENT)
  public AxisAlignedBB getRenderBoundingBox() {
    return layout.getRenderBox(pos);
  }

  /** A board meant to be read across a concourse. */
  @Override
  public double getMaxRenderDistanceSquared() {
    return CsmPerformance.capRenderDistanceSq(96.0 * 96.0);
  }

  /** Releases the board's lists when a cell is removed (only the controller holds any). */
  @Override
  public void invalidate() {
    super.invalidate();
    if (world != null && world.isRemote) {
      TileEntityFlightBoardLargeRenderer.release(pos);
    }
  }

  /** And when its chunk unloads. */
  @Override
  public void onChunkUnload() {
    super.onChunkUnload();
    if (world != null && world.isRemote) {
      TileEntityFlightBoardLargeRenderer.release(pos);
    }
  }

  /** Nothing a baked model draws comes from here. */
  @Override
  protected long getBakedModelKey() {
    return 0L;
  }
}

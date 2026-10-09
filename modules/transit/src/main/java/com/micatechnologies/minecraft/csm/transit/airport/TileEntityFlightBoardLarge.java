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

  /** Nothing a baked model draws comes from here. */
  @Override
  protected long getBakedModelKey() {
    return 0L;
  }
}

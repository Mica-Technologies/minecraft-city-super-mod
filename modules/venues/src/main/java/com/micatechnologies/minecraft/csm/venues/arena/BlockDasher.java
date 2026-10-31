package com.micatechnologies.minecraft.csm.venues.arena;

import com.micatechnologies.minecraft.csm.venues.BlockVenue;
import javax.annotation.Nonnull;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;

/**
 * A piece of a rink's dasher boards or of the glass on them. Boards are a block high, their ice
 * face on the edge of the cell they face; the glass is a block of its own stacked on a board, so
 * a run is as many blocks of glass high as it should be (two along the sides, three at the
 * ends). Board pieces render in the cutout layer and glass in the translucent one.
 *
 * <p>Every piece's model is drawn facing north, the ice to the north.</p>
 *
 * @since 2026.10
 */
public abstract class BlockDasher extends BlockVenue {

  /** How high a board's collision reaches, in blocks: a fence's, so nobody steps over it. */
  static final double BOARD_HEIGHT = 1.5;

  private final boolean glass;

  /**
   * Constructs a piece.
   *
   * @param registryName its registry name
   * @param glass        whether it is glass (stacked on a board) rather than a board
   */
  protected BlockDasher(String registryName, boolean glass) {
    super(registryName, glass ? Material.GLASS : Material.IRON, glass ? SoundType.GLASS
        : SoundType.WOOD, 1.5F);
    this.glass = glass;
  }

  /** Whether it is glass. */
  public boolean isGlass() {
    return glass;
  }

  /**
   * Whether the piece in a cell carries a run on, as far as a straight beside it is concerned: a
   * piece of the same layer (board or glass) that is not an open door.
   *
   * @param world the world
   * @param pos   the cell
   * @param glass the layer asked about
   *
   * @return true if a run continues into it
   */
  static boolean continues(IBlockAccess world, BlockPos pos, boolean glass) {
    IBlockState s = world.getBlockState(pos);
    if (!(s.getBlock() instanceof BlockDasher)) {
      return false;
    }
    BlockDasher d = (BlockDasher) s.getBlock();
    if (d instanceof BlockDasherDoor) {
      // a door is a board; its glass (the penalty door's) is drawn in the cell above, where the
      // glass beside it ends with a post, as a door frame's stanchion does
      return !glass && !s.getValue(BlockDasherDoor.OPEN);
    }
    return d.isGlass() == glass;
  }

  /** A box in pixels as a block-unit box, from {@code y0} to {@code y1} blocks high. */
  static AxisAlignedBB px(double x0, double z0, double x1, double z1, double y0, double y1) {
    return new AxisAlignedBB(x0 / 16.0, y0, z0 / 16.0, x1 / 16.0, y1, z1 / 16.0);
  }

  /** The height a piece's collision reaches. */
  double collisionHeight() {
    return glass ? 1.0 : BOARD_HEIGHT;
  }

  @Override
  @Nonnull
  public BlockRenderLayer getBlockRenderLayer() {
    return glass ? BlockRenderLayer.TRANSLUCENT : BlockRenderLayer.CUTOUT;
  }
}

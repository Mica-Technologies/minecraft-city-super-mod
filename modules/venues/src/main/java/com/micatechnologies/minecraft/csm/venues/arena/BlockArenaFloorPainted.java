package com.micatechnologies.minecraft.csm.venues.arena;

import com.micatechnologies.minecraft.csm.codeutils.CsmBlockStateContainer;
import javax.annotation.Nonnull;
import net.minecraft.block.properties.PropertyInteger;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;

/**
 * A floor block that shows its piece of a marking laid out round an anchor
 * ({@code rink_ice_painted}, {@code court_painted}): it looks round itself, nearest ring first,
 * for the family's {@link BlockArenaFloorAnchor} on the same level, works out where it sits in
 * that anchor's frame (cells to its right, cells forward) and shows the picture there, turned
 * with the anchor. Where two anchors' pictures could reach, the nearest anchor with something
 * drawn at that cell wins; with nothing drawn it is plain ice or maple (turned with the nearest
 * anchor, so the maple's boards keep running the court's length).
 *
 * <p>Nothing is stored: metadata is always 0. A builder fills an area round an anchor with
 * these, by hand or by {@code /fill}, and the circle, crease or key appears; the anchor's own
 * cell is the anchor. Both properties are actual state: {@code cell} (the picture, 0 plain) and
 * {@code rot} (0 to 3, the anchor's facing as quarter turns from north).</p>
 *
 * <p>The search reads at most {@code (2r + 1)^2} blocks (r is 6 on the rink and 9 on the
 * court) and only when the block's section is drawn, so fill only where a marking is: a whole
 * rink of these would cost a chunk rebuild far more than plain ice.</p>
 *
 * @since 2026.10
 */
public class BlockArenaFloorPainted extends BlockArenaFloorBase {

  /** The quarter turns of the anchor's facing from north. Actual. */
  public static final PropertyInteger ROT = PropertyInteger.create("rot", 0, 3);

  private PropertyInteger cell;
  private final ArenaFloorFamily family;

  /**
   * Constructs a painted floor block.
   *
   * @param registryName its registry name
   * @param family       the markings it shows
   */
  public BlockArenaFloorPainted(String registryName, ArenaFloorFamily family) {
    super(registryName, "ice".equals(family.key) ? Surface.ICE : Surface.WOOD, family);
    this.family = family;
    setDefaultState(blockState.getBaseState().withProperty(cell, 0).withProperty(ROT, 0));
  }

  @Override
  @Nonnull
  protected BlockStateContainer createBlockState() {
    ArenaFloorFamily f = (ArenaFloorFamily) pendingArg();
    cell = PropertyInteger.create("cell", 0, f.cells - 1);
    return new CsmBlockStateContainer(this, cell, ROT);
  }

  @Override
  @Nonnull
  @SuppressWarnings("deprecation")
  public IBlockState getStateFromMeta(int meta) {
    return getDefaultState();
  }

  @Override
  public int getMetaFromState(IBlockState state) {
    return 0;
  }

  @Override
  @Nonnull
  @SuppressWarnings("deprecation")
  public IBlockState getActualState(@Nonnull IBlockState state, IBlockAccess world,
      BlockPos pos) {
    int r = family.radius;
    int rot = -1;
    BlockPos.MutableBlockPos at = new BlockPos.MutableBlockPos();
    for (int ring = 1; ring <= r; ring++) {
      for (int dz = -ring; dz <= ring; dz++) {
        boolean edgeRow = dz == -ring || dz == ring;
        for (int dx = -ring; dx <= ring; dx += edgeRow ? 1 : 2 * ring) {
          at.setPos(pos.getX() + dx, pos.getY(), pos.getZ() + dz);
          IBlockState s = world.getBlockState(at);
          if (!(s.getBlock() instanceof BlockArenaFloorAnchor)
              || ((BlockArenaFloorAnchor) s.getBlock()).family() != family) {
            continue;
          }
          EnumFacing f = s.getValue(BlockArenaFloorAnchor.FACING);
          EnumFacing right = f.rotateY();
          // this block's offset from the anchor, in the anchor's frame
          int j = -dx * f.getXOffset() - dz * f.getZOffset();
          int i = -dx * right.getXOffset() - dz * right.getZOffset();
          int q = (f.getHorizontalIndex() + 2) & 3;
          if (rot < 0) {
            rot = q;
          }
          int c = family.cellAt(s.getValue(BlockArenaFloorAnchor.KIND), i, j);
          if (c != 0) {
            return state.withProperty(cell, c).withProperty(ROT, q);
          }
        }
      }
    }
    return state.withProperty(cell, 0).withProperty(ROT, Math.max(rot, 0));
  }
}

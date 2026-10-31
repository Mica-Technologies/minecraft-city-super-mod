package com.micatechnologies.minecraft.csm.venues.ballpark;

import com.micatechnologies.minecraft.csm.codeutils.CsmBlockStateContainer;
import com.micatechnologies.minecraft.csm.venues.arena.BlockArenaFloorBase;
import javax.annotation.Nonnull;
import net.minecraft.block.properties.PropertyInteger;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;

/**
 * A ballpark field cell that shows its piece of a picture laid out round an anchor
 * ({@code ballpark_painted}): the arena's painted floor for a diamond. It looks round itself,
 * nearest ring first, for an {@link IBallparkAnchor} of its family on the same level, works out
 * where it sits in that anchor's frame (cells to its right, cells forward) and shows the picture
 * there, turned with the anchor and, for a third base side twin, mirrored. Where two anchors
 * could reach, the nearest that draws something at that cell wins; every picture is cut from one
 * drawing of the field, so any of them shows the same thing. With nothing drawn it is plain
 * infield clay.
 *
 * <p>Nothing is stored: metadata is always 0. Both properties are actual state: {@code cell}
 * (the picture, a mirrored one being a cell of its own) and {@code rot} (0 to 3, the anchor's
 * facing as quarter turns from north).</p>
 *
 * <p>The search reads at most {@code (2r + 1)^2} blocks (r is 7 on the field, 3 on the mound)
 * and only when the block's chunk section is drawn: lay these only where a marking is.</p>
 *
 * @since 2026.10
 */
public class BlockBallparkPainted extends BlockArenaFloorBase {

  /** The quarter turns of the anchor's facing from north. Actual. */
  public static final PropertyInteger ROT = PropertyInteger.create("rot", 0, 3);

  private PropertyInteger cell;
  private final BallparkFamily family;

  /**
   * Constructs a painted field block.
   *
   * @param registryName its registry name
   * @param family       the pictures it shows
   */
  public BlockBallparkPainted(String registryName, BallparkFamily family) {
    this(registryName, family, Surface.CLAY);
  }

  /**
   * Constructs a painted field block of a surface.
   *
   * @param registryName its registry name
   * @param family       the pictures it shows
   * @param surface      what it is made of
   */
  protected BlockBallparkPainted(String registryName, BallparkFamily family, Surface surface) {
    super(registryName, surface, family);
    this.family = family;
    setDefaultState(blockState.getBaseState().withProperty(cell, 0).withProperty(ROT, 0));
  }

  @Override
  @Nonnull
  protected BlockStateContainer createBlockState() {
    BallparkFamily f = (BallparkFamily) pendingArg();
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

  /**
   * The picture this cell shows: {cell, rot}, or {-1, rot} where no anchor draws anything
   * (rot that of the nearest anchor, 0 with none).
   */
  protected int[] find(IBlockAccess world, BlockPos pos) {
    int r = family.radius;
    int rot = -1;
    BlockPos.MutableBlockPos at = new BlockPos.MutableBlockPos();
    for (int ring = 1; ring <= r; ring++) {
      for (int dz = -ring; dz <= ring; dz++) {
        boolean edgeRow = dz == -ring || dz == ring;
        for (int dx = -ring; dx <= ring; dx += edgeRow ? 1 : 2 * ring) {
          at.setPos(pos.getX() + dx, pos.getY(), pos.getZ() + dz);
          IBlockState s = world.getBlockState(at);
          if (!(s.getBlock() instanceof IBallparkAnchor)
              || ((IBallparkAnchor) s.getBlock()).family() != family) {
            continue;
          }
          IBallparkAnchor anchor = (IBallparkAnchor) s.getBlock();
          EnumFacing f = anchor.pictureFacing(s);
          EnumFacing right = f.rotateY();
          // this block's offset from the anchor, in the anchor's frame
          int j = -dx * f.getXOffset() - dz * f.getZOffset();
          int i = -dx * right.getXOffset() - dz * right.getZOffset();
          int q = (f.getHorizontalIndex() + 2) & 3;
          if (rot < 0) {
            rot = q;
          }
          int c = family.cellAt(anchor.kindIndex(s), i, j, anchor.third());
          if (c >= 0) {
            return new int[]{c, q};
          }
        }
      }
    }
    return new int[]{-1, Math.max(rot, 0)};
  }

  @Override
  @Nonnull
  @SuppressWarnings("deprecation")
  public IBlockState getActualState(@Nonnull IBlockState state, IBlockAccess world,
      BlockPos pos) {
    int[] found = find(world, pos);
    return state.withProperty(cell, Math.max(0, found[0])).withProperty(ROT, found[1]);
  }
}

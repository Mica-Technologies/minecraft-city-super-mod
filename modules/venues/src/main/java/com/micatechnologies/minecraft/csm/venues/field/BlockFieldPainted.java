package com.micatechnologies.minecraft.csm.venues.field;

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
 * A football or soccer field cell that shows its piece of the field ({@code football_painted},
 * {@code soccer_painted}): the ballpark's painted cell, for a whole field. It looks round itself,
 * nearest ring first, for a {@link BlockFieldAnchor} of its family on the same level, works out
 * where it sits in that anchor's frame and shows the picture the anchor's table gives there,
 * turned with the anchor (and a further half turn where the table says so). The tables are
 * disjoint, so only the anchor that owns a cell draws anything at it.
 *
 * <p>Nothing is stored: metadata is always 0. Both properties are actual state: {@code cell} (the
 * picture) and {@code rot} (its quarter turns from north). The mowing tone and the end zone colour
 * are tints ({@link FieldTint}).</p>
 *
 * <p>The search reads at most 625 blocks (radius 12), and only when the block's chunk section is
 * built: lay these only where a marking is.</p>
 *
 * @since 2026.10
 */
public class BlockFieldPainted extends BlockArenaFloorBase implements IFieldCell {

  /** The quarter turns of the picture from north. Actual. */
  public static final PropertyInteger ROT = PropertyInteger.create("rot", 0, 3);

  private PropertyInteger cell;
  private final FieldFamily family;

  /**
   * Constructs a painted field block.
   *
   * @param registryName its registry name
   * @param family       the pictures it shows
   */
  public BlockFieldPainted(String registryName, FieldFamily family) {
    super(registryName, Surface.TURF, family);
    this.family = family;
    setDefaultState(blockState.getBaseState().withProperty(cell, 0).withProperty(ROT, 0));
  }

  @Override
  @Nonnull
  protected BlockStateContainer createBlockState() {
    FieldFamily f = (FieldFamily) pendingArg();
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
  public int tintRotation(IBlockState actualState) {
    return actualState.getBlock() == this ? actualState.getValue(ROT) : 0;
  }

  /** The picture this cell shows: {cell, rot}; {0, 0} where no anchor draws it. */
  int[] find(IBlockAccess world, BlockPos pos) {
    int r = family.radius;
    BlockPos.MutableBlockPos at = new BlockPos.MutableBlockPos();
    for (int ring = 1; ring <= r; ring++) {
      for (int dz = -ring; dz <= ring; dz++) {
        boolean edgeRow = dz == -ring || dz == ring;
        for (int dx = -ring; dx <= ring; dx += edgeRow ? 1 : 2 * ring) {
          at.setPos(pos.getX() + dx, pos.getY(), pos.getZ() + dz);
          IBlockState s = world.getBlockState(at);
          if (!(s.getBlock() instanceof BlockFieldAnchor)
              || ((BlockFieldAnchor) s.getBlock()).family() != family) {
            continue;
          }
          BlockFieldAnchor anchor = (BlockFieldAnchor) s.getBlock();
          EnumFacing f = anchor.forward(s);
          EnumFacing right = f.rotateY();
          int j = -dx * f.getXOffset() - dz * f.getZOffset();
          int i = -dx * right.getXOffset() - dz * right.getZOffset();
          int v = family.at(anchor.kindIndex(s), i, j);
          if (v >= 0) {
            int q = (f.getHorizontalIndex() + 2) & 3;
            return new int[]{v >> 1, (q + 2 * (v & 1)) & 3};
          }
        }
      }
    }
    return new int[]{0, 0};
  }

  @Override
  @Nonnull
  @SuppressWarnings("deprecation")
  public IBlockState getActualState(@Nonnull IBlockState state, IBlockAccess world,
      BlockPos pos) {
    int[] found = find(world, pos);
    return state.withProperty(cell, Math.min(found[0], family.cells - 1))
        .withProperty(ROT, found[1]);
  }
}

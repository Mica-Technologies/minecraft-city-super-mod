package com.micatechnologies.minecraft.csm.venues.lines;

import com.micatechnologies.minecraft.csm.venues.arena.BlockArenaFloorBase;
import javax.annotation.Nonnull;
import net.minecraft.block.state.IBlockState;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.ChunkCache;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.common.property.IExtendedBlockState;
import net.minecraftforge.common.property.IUnlistedProperty;

/**
 * A court or track cell whose top is painted where the lines really fall: an anchor
 * ({@link BlockLineAnchor}) or a painted cell ({@link BlockLinePainted}). What it shows is a
 * {@link LineArtKey}, found as its chunk section is built and passed to its baked model through
 * {@link #KEY}; the model ({@code LineArtBakedModel}) paints the cell with {@link LinePainter}
 * and caches the quads by key. No tile entity per cell, no renderer, no texture per cell: every
 * line, arc and number is geometry, so a track's thousands of different cells cost no atlas.
 *
 * @since 2026.10
 */
public abstract class BlockLineCell extends BlockArenaFloorBase {

  /** What the cell shows; null where no anchor reaches it. */
  public static final IUnlistedProperty<LineArtKey> KEY = new IUnlistedProperty<LineArtKey>() {
    @Override
    public String getName() {
      return "lineart";
    }

    @Override
    public boolean isValid(LineArtKey value) {
      return true;
    }

    @Override
    public Class<LineArtKey> getType() {
      return LineArtKey.class;
    }

    @Override
    public String valueToString(LineArtKey value) {
      return value == null ? "none" : value.layout + "@" + value.i + "," + value.j;
    }
  };

  private final LineArtFamily family;

  protected BlockLineCell(String registryName, Surface surface, LineArtFamily family,
      Object arg) {
    super(registryName, surface, arg);
    this.family = family;
  }

  /** Its family. */
  public LineArtFamily family() {
    return family;
  }

  /** The cell's key: from the nearest anchor of its family whose layout reaches it. */
  LineArtKey find(IBlockAccess world, BlockPos pos) {
    BlockPos.MutableBlockPos at = new BlockPos.MutableBlockPos();
    int r = LineArtFamily.REACH;
    for (int ring = 0; ring <= r; ring++) {
      for (int dz = -ring; dz <= ring; dz++) {
        boolean edgeRow = dz == -ring || dz == ring;
        int step = edgeRow || ring == 0 ? 1 : 2 * ring;
        for (int dx = -ring; dx <= ring; dx += step) {
          at.setPos(pos.getX() + dx, pos.getY(), pos.getZ() + dz);
          IBlockState s = world.getBlockState(at);
          if (!(s.getBlock() instanceof BlockLineAnchor)
              || ((BlockLineAnchor) s.getBlock()).family() != family) {
            continue;
          }
          TileEntity te = tileAt(world, at);
          if (!(te instanceof TileEntityLineAnchor)) {
            continue;
          }
          TileEntityLineAnchor a = (TileEntityLineAnchor) te;
          int rot = BlockLineAnchor.rotation(s);
          int[] uv = LineArtKey.toLayout(-dx, -dz, rot);
          LineArtLayout lay = family.layout(a.getLayout());
          int i = a.getAx() + uv[0];
          int j = a.getAz() + uv[1];
          if (lay.covers(i, j)) {
            return new LineArtKey(family.id, Math.max(0,
                Math.min(family.layoutCount() - 1, a.getLayout())), i, j, rot,
                a.packedColours(lay));
          }
        }
      }
    }
    return null;
  }

  /** A tile entity, without creating one on a chunk builder's thread. */
  static TileEntity tileAt(IBlockAccess world, BlockPos pos) {
    if (world instanceof ChunkCache) {
      return ((ChunkCache) world).getTileEntity(pos, Chunk.EnumCreateEntityType.CHECK);
    }
    return world.getTileEntity(pos);
  }

  @Override
  @Nonnull
  public IBlockState getExtendedState(@Nonnull IBlockState state, IBlockAccess world,
      BlockPos pos) {
    if (state instanceof IExtendedBlockState) {
      return ((IExtendedBlockState) state).withProperty(KEY, find(world, pos));
    }
    return state;
  }
}

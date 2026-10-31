package com.micatechnologies.minecraft.csm.venues.lines;

import com.micatechnologies.minecraft.csm.codeutils.AbstractTileEntity;
import com.micatechnologies.minecraft.csm.venues.av.VenueColours;
import javax.annotation.Nonnull;
import net.minecraft.block.state.IBlockState;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * A court or track anchor's tile entity: which layout it anchors, where it sits in that layout
 * and the colours. No tick, no renderer. On the client it asks for the cells round it to be drawn
 * again when it is loaded, changed or removed, since a painted cell reads it only as its own
 * chunk section is built.
 *
 * <p>NBT ({@code /blockdata}):</p>
 * <ul>
 *   <li>{@code lay}: the layout's index in its family (a court: 0 tennis, 1 pickleball, 2 tennis
 *   with pickleball, 3 basketball, 4 half court basketball, 5 volleyball, 6 sand volleyball; the
 *   track: 0 the 400 m track, 1 the jumps, 2 the pole vault);</li>
 *   <li>{@code ax}, {@code az}: the anchor's own cell in the layout's frame (0, 0 is the court's
 *   centre cell);</li>
 *   <li>{@code cc}, {@code sc}: the surface and second surface colours ({@link SurfaceColours});
 *   {@code lc}, {@code oc}: the line and second line colours ({@link VenueColours}); -1 or absent
 *   for the layout's own.</li>
 * </ul>
 *
 * @since 2026.10
 */
public class TileEntityLineAnchor extends AbstractTileEntity {

  /** Further than any cell reaches for its anchor. */
  private static final int REDRAW = LineArtFamily.REACH + 1;

  private int layout;
  private int ax;
  private int az;
  private final int[] colours = {-1, -1, -1, -1};

  /** The layout's index. */
  public int getLayout() {
    return layout;
  }

  /** The anchor's cell in the layout's frame. */
  public int getAx() {
    return ax;
  }

  public int getAz() {
    return az;
  }

  /** The colours, four bits each (cc, sc, lc, oc), the layout's defaults filled in. */
  public int packedColours(LineArtLayout lay) {
    int out = 0;
    for (int k = 0; k < 4; k++) {
      int c = colours[k] >= 0 ? colours[k] : lay.defaultColour(k);
      out |= (c & 15) << (4 * k);
    }
    return out;
  }

  /** Steps the layout (a sneak-click), back to its centre cell. Server side. */
  public void stepLayout(int count) {
    layout = (layout + 1) % Math.max(1, count);
    ax = 0;
    az = 0;
    if (world != null) {
      markDirtySync(world, pos, true);
    }
  }

  @Override
  public void readNBT(NBTTagCompound c) {
    layout = Math.max(0, c.getInteger("lay"));
    ax = c.getInteger("ax");
    az = c.getInteger("az");
    String[] keys = {"cc", "sc", "lc", "oc"};
    for (int k = 0; k < 4; k++) {
      colours[k] = c.hasKey(keys[k]) ? Math.max(-1, Math.min(15, c.getInteger(keys[k]))) : -1;
    }
    redraw();
  }

  @Override
  public NBTTagCompound writeNBT(NBTTagCompound c) {
    c.setInteger("lay", layout);
    c.setInteger("ax", ax);
    c.setInteger("az", az);
    String[] keys = {"cc", "sc", "lc", "oc"};
    for (int k = 0; k < 4; k++) {
      if (colours[k] >= 0) {
        c.setInteger(keys[k], colours[k]);
      }
    }
    return c;
  }

  @Override
  public void onLoad() {
    super.onLoad();
    redraw();
  }

  @Override
  public void invalidate() {
    super.invalidate();
    redraw();
  }

  @Override
  public boolean shouldRefresh(World world, BlockPos pos, @Nonnull IBlockState oldState,
      @Nonnull IBlockState newState) {
    return oldState.getBlock() != newState.getBlock();
  }

  private void redraw() {
    if (world != null && world.isRemote && pos != null) {
      world.markBlockRangeForRenderUpdate(pos.add(-REDRAW, 0, -REDRAW),
          pos.add(REDRAW, 0, REDRAW));
    }
  }
}

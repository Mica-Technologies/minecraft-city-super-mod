package com.micatechnologies.minecraft.csm.transit.wayfinding;

import com.micatechnologies.minecraft.csm.codeutils.AbstractTileEntity;
import com.micatechnologies.minecraft.csm.transit.wayfinding.WayfindingSign.Arrow;
import com.micatechnologies.minecraft.csm.transit.wayfinding.WayfindingSign.Pictogram;
import com.micatechnologies.minecraft.csm.transit.wayfinding.WayfindingSign.Scheme;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * A cell of a large hanging sign ({@link BlockWayfindingPanel}).
 *
 * <p><b>The sign.</b> Two lines, a pictogram, an arrow, a colour scheme and whether the back
 * carries the legend too. Every cell of a panel holds a copy: an edit writes them all, and a cell
 * placed against a panel copies its neighbour, so when the controller changes (a cell added to the
 * left or below, or the controller broken) the new one already has the sign. Only the
 * controller's copy is drawn.</p>
 *
 * <p><b>The layout</b> (client only): whether this cell is the controller, the panel's size and
 * where its hanger rods go and how long they are. Worked out by {@link #refreshLayout}, which the
 * renderer calls every frame but which walks the world only when a nearby chunk section was
 * rebuilt ({@link BlockWayfindingPanel#layoutGeneration}) or two seconds have passed, the latter
 * for a ceiling changing out of reach of that signal.</p>
 *
 * @since 2026.10
 */
public class TileEntityWayfindingPanel extends AbstractTileEntity {

  private static final String KEY_LINE1 = "l1";
  private static final String KEY_LINE2 = "l2";
  private static final String KEY_PICTOGRAM = "p";
  private static final String KEY_ARROW = "a";
  private static final String KEY_SCHEME = "s";
  private static final String KEY_DOUBLE = "d";

  /** Ticks after which the layout is looked at again whatever happened. */
  private static final long LAYOUT_TICKS = 40;
  /** Ticks between looks prompted by a chunk rebuild, so a burst of them costs one. */
  private static final long LAYOUT_MIN_TICKS = 2;

  private String line1 = WayfindingSign.Preset.GATES.getLine1();
  private String line2 = "";
  private Pictogram pictogram = Pictogram.DEPART;
  private Arrow arrow = Arrow.RIGHT;
  private Scheme scheme = Scheme.AIRPORT;
  private boolean doubleSided = true;

  // ---- client layout cache (never saved) ----
  private long layoutAt = Long.MIN_VALUE;
  private int layoutGeneration;
  private boolean controller;
  private int width = 1;
  private int height = 1;
  private EnumFacing facing = EnumFacing.NORTH;
  private float[] rodX = new float[0];
  private int[] rodDrop = new int[0];
  private AxisAlignedBB renderBox;

  /** Client only: the renderer's layout of the legend, and the key it was worked out for. */
  public transient Object renderLayout;
  public transient long renderLayoutKey;

  public String getLine1() {
    return line1;
  }

  public String getLine2() {
    return line2;
  }

  public Pictogram getPictogram() {
    return pictogram;
  }

  public Arrow getArrow() {
    return arrow;
  }

  public Scheme getScheme() {
    return scheme;
  }

  public boolean isDoubleSided() {
    return doubleSided;
  }

  /**
   * Sets the whole sign, every string cleaned by {@link WayfindingSign#clamp}.
   */
  public void setSign(String line1, String line2, Pictogram pictogram, Arrow arrow,
      Scheme scheme, boolean doubleSided) {
    this.line1 = WayfindingSign.clamp(line1);
    this.line2 = WayfindingSign.clamp(line2);
    this.pictogram = pictogram == null ? Pictogram.NONE : pictogram;
    this.arrow = arrow == null ? Arrow.NONE : arrow;
    this.scheme = scheme == null ? Scheme.AIRPORT : scheme;
    this.doubleSided = doubleSided;
    renderLayout = null;
  }

  /** Takes another cell's sign. */
  public void copySign(TileEntityWayfindingPanel other) {
    setSign(other.line1, other.line2, other.pictogram, other.arrow, other.scheme,
        other.doubleSided);
  }

  /**
   * Works the layout out again if it may have changed. The renderer calls this every frame before
   * reading it; it costs two block lookups for a cell that is not a controller.
   */
  @SideOnly(Side.CLIENT)
  public void refreshLayout() {
    if (world == null) {
      return;
    }
    long now = world.getTotalWorldTime();
    int generation = BlockWayfindingPanel.layoutGeneration();
    if (layoutAt != Long.MIN_VALUE && now >= layoutAt) {
      long age = now - layoutAt;
      boolean rebuilt = generation != layoutGeneration && age >= LAYOUT_MIN_TICKS;
      if (age < LAYOUT_TICKS && !rebuilt) {
        return;
      }
    }
    layoutAt = now;
    layoutGeneration = generation;
    IBlockState state = world.getBlockState(pos);
    Block block = state.getBlock();
    if (!(block instanceof BlockWayfindingPanel)) {
      controller = false;
      return;
    }
    facing = state.getValue(BlockWayfindingPanel.FACING);
    EnumFacing left = WayfindingSign.leftOf(facing);
    controller = !WayfindingSign.joins(world, pos.offset(left), block, facing)
        && !WayfindingSign.joins(world, pos.down(), block, facing);
    if (!controller) {
      return;
    }
    width = WayfindingSign.widthFrom(world, pos, block, facing);
    height = WayfindingSign.heightFrom(world, pos, block, facing);
    hangers(left.getOpposite());
    renderBox = box(left.getOpposite());
  }

  /**
   * Where the rods go: two, or one for about every four blocks of a wider panel, evenly along
   * the top; each reaches up through air to the first block above, and a rod with a block right
   * on top of the panel, or nothing within {@link WayfindingSign#MAX_ROD_DROP}, is not drawn.
   */
  private void hangers(EnumFacing right) {
    int n = Math.max(2, Math.round(width / 4f));
    if (rodX.length != n) {
      rodX = new float[n];
      rodDrop = new int[n];
    }
    for (int i = 0; i < n; i++) {
      float x = width * (i + 0.5f) / n;
      rodX[i] = x;
      int column = Math.min(width - 1, (int) x);
      BlockPos above = pos.offset(right, column).up(height);
      int drop = 0;
      BlockPos at = above;
      while (drop < WayfindingSign.MAX_ROD_DROP && world.isAirBlock(at)) {
        drop++;
        at = at.up();
      }
      rodDrop[i] = drop < WayfindingSign.MAX_ROD_DROP ? drop : 0;
    }
  }

  private AxisAlignedBB box(EnumFacing right) {
    int maxDrop = 0;
    for (int d : rodDrop) {
      maxDrop = Math.max(maxDrop, d);
    }
    BlockPos far = pos.offset(right, width - 1).up(height - 1 + maxDrop);
    return new AxisAlignedBB(Math.min(pos.getX(), far.getX()), pos.getY(),
        Math.min(pos.getZ(), far.getZ()), Math.max(pos.getX(), far.getX()) + 1, far.getY() + 1,
        Math.max(pos.getZ(), far.getZ()) + 1);
  }

  /** Whether this cell drew the sign when the layout was last worked out. */
  public boolean isController() {
    return controller;
  }

  public int getWidth() {
    return width;
  }

  public int getHeight() {
    return height;
  }

  public EnumFacing getFacing() {
    return facing;
  }

  /** The rods' places along the top, in blocks from the panel's left edge as read. */
  public float[] getRodX() {
    return rodX;
  }

  /** Each rod's length in blocks, 0 for none. */
  public int[] getRodDrop() {
    return rodDrop;
  }

  @Override
  @SideOnly(Side.CLIENT)
  public AxisAlignedBB getRenderBoundingBox() {
    AxisAlignedBB box = renderBox;
    return box != null && controller ? box
        : new AxisAlignedBB(pos, pos.add(1, 1, 1));
  }

  /** A backlit sign meant to be read down a concourse. */
  @Override
  public double getMaxRenderDistanceSquared() {
    return 96.0 * 96.0;
  }

  /** Nothing a baked model draws comes from here. */
  @Override
  protected long getBakedModelKey() {
    return 0L;
  }

  @Override
  public void invalidate() {
    super.invalidate();
    releaseRenderCache();
  }

  @Override
  public void onChunkUnload() {
    super.onChunkUnload();
    releaseRenderCache();
  }

  private void releaseRenderCache() {
    if (world != null && world.isRemote) {
      TileEntityWayfindingPanelRenderer.release(pos);
    }
  }

  @Override
  public void readNBT(NBTTagCompound compound) {
    if (!compound.hasKey(KEY_PICTOGRAM)) {
      return;           // a cell never written keeps the default sign
    }
    setSign(compound.getString(KEY_LINE1), compound.getString(KEY_LINE2),
        Pictogram.byId(compound.getString(KEY_PICTOGRAM)),
        Arrow.byId(compound.getString(KEY_ARROW)), Scheme.byId(compound.getString(KEY_SCHEME)),
        !compound.hasKey(KEY_DOUBLE) || compound.getBoolean(KEY_DOUBLE));
    // a new sign may change which cells are drawn: look again at once
    layoutAt = Long.MIN_VALUE;
  }

  @Override
  public NBTTagCompound writeNBT(NBTTagCompound compound) {
    if (!line1.isEmpty()) {
      compound.setString(KEY_LINE1, line1);
    }
    if (!line2.isEmpty()) {
      compound.setString(KEY_LINE2, line2);
    }
    compound.setString(KEY_PICTOGRAM, pictogram.getId());
    compound.setString(KEY_ARROW, arrow.getId());
    compound.setString(KEY_SCHEME, scheme.getId());
    compound.setBoolean(KEY_DOUBLE, doubleSided);
    return compound;
  }
}

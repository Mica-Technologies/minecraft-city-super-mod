package com.micatechnologies.minecraft.csm.transit.wayfinding;

import com.micatechnologies.minecraft.csm.codeutils.AbstractTileEntity;
import com.micatechnologies.minecraft.csm.transit.panel.PanelLayout;
import com.micatechnologies.minecraft.csm.transit.wayfinding.WayfindingSign.Arrow;
import com.micatechnologies.minecraft.csm.transit.wayfinding.WayfindingSign.Pictogram;
import com.micatechnologies.minecraft.csm.transit.wayfinding.WayfindingSign.Scheme;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * A cell of a large hanging sign ({@link BlockWayfindingPanel}).
 *
 * <p><b>The sign.</b> Two lines, a pictogram, an arrow, a colour scheme and whether the back
 * carries the legend too. Every cell of a panel holds a copy: an edit writes them all (from the
 * GUI or by {@code /blockdata} on any cell), and a cell placed against a panel copies its
 * neighbour, so when the controller changes (a cell added to the left or below, or the controller
 * broken) the new one already has the sign. Only the controller's copy is drawn.</p>
 *
 * <p><b>The layout</b> (client only): whether this cell is the controller, the panel's size and
 * where its hanger rods go and how long they are: a {@link PanelLayout}, worked out by
 * {@link #refreshLayout}, which the renderer calls every frame but which walks the world only
 * when a nearby chunk section was rebuilt or two seconds have passed.</p>
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

  private String line1 = WayfindingSign.Preset.GATES.getLine1();
  private String line2 = "";
  private Pictogram pictogram = Pictogram.DEPART;
  private Arrow arrow = Arrow.RIGHT;
  private Scheme scheme = Scheme.AIRPORT;
  private boolean doubleSided = true;

  /** Client only, never saved: the panel this cell is in ({@link PanelLayout}). */
  private final PanelLayout layout = new PanelLayout(BlockWayfindingPanel.class,
      WayfindingSign.MAX_WIDTH, WayfindingSign.MAX_HEIGHT, false);

  /** Set while an edit spreads to the panel, so the cells it writes do not spread it again. */
  private static boolean spreading;

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
    layout.refresh(world, pos);
  }

  /** The panel this cell is in, as last worked out. */
  public PanelLayout getLayout() {
    return layout;
  }

  /** Whether this cell drew the sign when the layout was last worked out. */
  public boolean isController() {
    return layout.isController();
  }

  public int getWidth() {
    return layout.getWidth();
  }

  public int getHeight() {
    return layout.getHeight();
  }

  public EnumFacing getFacing() {
    return layout.getFacing();
  }

  /** The rods' places along the top, in blocks from the panel's left edge as read. */
  public float[] getRodX() {
    return layout.getRodX();
  }

  /** Each rod's length in blocks, 0 for none. */
  public int[] getRodDrop() {
    return layout.getRodDrop();
  }

  @Override
  @SideOnly(Side.CLIENT)
  public AxisAlignedBB getRenderBoundingBox() {
    return layout.getRenderBox(pos);
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
    layout.invalidate();
    spreadEdit();
  }

  /**
   * A {@code /blockdata} edit of any cell is the whole panel's, as an edit in the GUI is: written
   * to every cell ({@link WayfindingSign#applyToPanel}). Only for a live edit on the server: a
   * cell being loaded with its chunk reads its NBT before its chunk is in the world (so
   * {@code isBlockLoaded} is false, and asking for the tile entity there would load the chunk
   * again), and before it is the tile entity there. The cells the spread writes do not spread it
   * again.
   */
  private void spreadEdit() {
    if (spreading || world == null || world.isRemote || !world.isBlockLoaded(pos)
        || world.getTileEntity(pos) != this) {
      return;
    }
    spreading = true;
    try {
      WayfindingSign.applyToPanel(world, pos, line1, line2, pictogram, arrow, scheme,
          doubleSided);
    } finally {
      spreading = false;
    }
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

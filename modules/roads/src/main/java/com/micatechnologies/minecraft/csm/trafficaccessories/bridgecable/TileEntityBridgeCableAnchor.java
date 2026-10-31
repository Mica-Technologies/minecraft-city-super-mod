package com.micatechnologies.minecraft.csm.trafficaccessories.bridgecable;

import com.micatechnologies.minecraft.csm.codeutils.AbstractTileEntity;
import com.micatechnologies.minecraft.csm.codeutils.CsmPerformance;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;

/**
 * Holds the cables a bridge cable anchor owns. Each cable is drawn by its owner only; the anchor
 * at its other end (if there is one) just wears the fitting.
 *
 * <p>The cables are data and nothing else: no tick, no collision, nothing checked along them.
 * The client works out what to draw from them ({@link BridgeCableLayout}) when it first draws the
 * anchor and again every couple of seconds, so a deck built under a main cable after the cable
 * was strung gets its hangers without anything having to tell the anchor.
 */
public class TileEntityBridgeCableAnchor extends AbstractTileEntity {

  /** Ticks between the client's looks at what the cables' ends and hangers land on. */
  static final int RESOLVE_INTERVAL_TICKS = 40;

  private List<BridgeCableLink> links = Collections.emptyList();

  /** Client only: what is drawn, as of the last look. */
  @Nullable
  private BridgeCableLayout layout = null;

  private long nextResolveTick = Long.MIN_VALUE;

  /** A box over the cables from the links alone, used until the layout has been worked out. */
  @Nullable
  private AxisAlignedBB roughBounds = null;

  @Override
  public void readNBT(NBTTagCompound compound) {
    final BlockPos owner = pos;
    links = BridgeCableLink.copyOf(BridgeCableLink.readAll(compound, owner));
    onLinksChanged();
  }

  @Override
  public NBTTagCompound writeNBT(NBTTagCompound compound) {
    BridgeCableLink.writeAll(compound, links);
    return compound;
  }

  /** The cables this anchor owns. */
  public List<BridgeCableLink> getLinks() {
    return links;
  }

  /**
   * Adds a cable, or replaces the one already running to the same block. Server side.
   *
   * @return false when the anchor already holds {@link BridgeCableLink#MAX_CABLES} cables
   */
  public boolean putLink(BridgeCableLink link) {
    final List<BridgeCableLink> next = new ArrayList<>(links);
    if (!BridgeCableLink.putInto(next, link)) {
      return false;
    }
    setLinks(next);
    return true;
  }

  /**
   * Drops the cable running to a block, if this anchor owns one.
   *
   * @return whether a cable was dropped
   */
  public boolean removeLinkTo(BlockPos target) {
    final List<BridgeCableLink> next = new ArrayList<>(links);
    final boolean removed = next.removeIf(link -> link.getTarget(pos).equals(target));
    if (removed) {
      setLinks(next);
    }
    return removed;
  }

  /** Drops every cable. @return how many there were */
  public int clearLinks() {
    final int count = links.size();
    if (count > 0) {
      setLinks(new ArrayList<>());
    }
    return count;
  }

  private void setLinks(List<BridgeCableLink> next) {
    links = BridgeCableLink.copyOf(next);
    onLinksChanged();
    if (world != null && !world.isRemote) {
      markDirtySync(world, pos, true);
    }
  }

  private void onLinksChanged() {
    layout = null;
    nextResolveTick = Long.MIN_VALUE;
    roughBounds = pos == null ? null : roughBounds();
    if (world != null && world.isRemote) {
      TileEntityBridgeCableAnchorRenderer.cleanupDisplayList(pos);
    }
  }

  /**
   * Client: what this anchor draws, worked out again when it is due. A fresh layout replaces the
   * held one only when something drawn has changed, so the display list keyed on it survives the
   * re-checks.
   */
  @Nullable
  public BridgeCableLayout getLayout() {
    if (world == null) {
      return null;
    }
    if (links.isEmpty()) {
      layout = null;
      return null;
    }
    final long now = world.getTotalWorldTime();
    if (layout == null || now >= nextResolveTick) {
      final BridgeCableLayout fresh = BridgeCableLayout.build(pos, links, new View(world));
      if (layout == null || fresh.getKey() != layout.getKey()) {
        layout = fresh;
      }
      // Spread out so a bridge's sixty anchors do not all look on the same frame.
      nextResolveTick = now + RESOLVE_INTERVAL_TICKS + (pos.hashCode() & 15);
    }
    return layout;
  }

  /** Nothing in the block's model reads this tile entity, so a sync never rebuilds the chunk. */
  @Override
  protected long getBakedModelKey() {
    return 0L;
  }

  /**
   * The whole cable, not the anchor: a stay a hundred blocks long must not vanish because its
   * anchor's corner of it went out of view.
   */
  @Override
  public AxisAlignedBB getRenderBoundingBox() {
    if (layout != null) {
      return layout.getBounds();
    }
    if (roughBounds != null) {
      return roughBounds;
    }
    // Never TileEntity's default, which is infinite and turns culling off.
    return new AxisAlignedBB(pos);
  }

  /** Drawn as far off as the far end of its longest cable is in the long-range distance. */
  @Override
  public double getMaxRenderDistanceSquared() {
    final AxisAlignedBB bounds = getRenderBoundingBox();
    final double cx = pos.getX() + 0.5;
    final double cy = pos.getY() + 0.5;
    final double cz = pos.getZ() + 0.5;
    final double dx = Math.max(cx - bounds.minX, bounds.maxX - cx);
    final double dy = Math.max(cy - bounds.minY, bounds.maxY - cy);
    final double dz = Math.max(cz - bounds.minZ, bounds.maxZ - cz);
    final double distance = LONG_RANGE_RENDER_DISTANCE + Math.sqrt(dx * dx + dy * dy + dz * dz);
    return CsmPerformance.capRenderDistanceSq(distance * distance);
  }

  /** The cables' end blocks, with room below for sag and for hangers to reach a deck. */
  private AxisAlignedBB roughBounds() {
    AxisAlignedBB box = new AxisAlignedBB(pos);
    double drop = 0.0;
    for (BridgeCableLink link : links) {
      final BlockPos target = link.getTarget(pos);
      box = box.union(new AxisAlignedBB(target));
      final double plan = Math.sqrt((double) link.getDx() * link.getDx()
          + (double) link.getDz() * link.getDz());
      double linkDrop = link.getSagBlocks(plan) + 1.0;
      if (link.getHangerSpacing() > 0) {
        linkDrop += 64.0;
      }
      drop = Math.max(drop, linkDrop);
    }
    return new AxisAlignedBB(box.minX, box.minY - drop, box.minZ, box.maxX, box.maxY, box.maxZ)
        .grow(0.5);
  }

  @Override
  public void invalidate() {
    super.invalidate();
    if (world != null && world.isRemote) {
      TileEntityBridgeCableAnchorRenderer.cleanupDisplayList(pos);
    }
  }

  @Override
  public void onChunkUnload() {
    super.onChunkUnload();
    if (world != null && world.isRemote) {
      TileEntityBridgeCableAnchorRenderer.cleanupDisplayList(pos);
    }
  }

  /** The world as {@link BridgeCableLayout} sees it. */
  static final class View implements BridgeCableLayout.WorldView {

    private final IBlockAccess world;

    View(IBlockAccess world) {
      this.world = world;
    }

    @Nullable
    @Override
    public BridgeCableLayout.AnchorEnd anchorAt(BlockPos pos) {
      final IBlockState state = world.getBlockState(pos);
      if (!(state.getBlock() instanceof BlockBridgeCableAnchor)) {
        return null;
      }
      return new BridgeCableLayout.AnchorEnd(
          ((BlockBridgeCableAnchor) state.getBlock()).getKind(),
          state.getValue(BlockBridgeCableAnchor.FACING));
    }

    @Override
    public double solidTop(BlockPos pos) {
      final IBlockState state = world.getBlockState(pos);
      if (state.getMaterial().isLiquid()) {
        return Double.NaN;
      }
      final AxisAlignedBB box = state.getCollisionBoundingBox(world, pos);
      if (box == null || box == Block.NULL_AABB) {
        return Double.NaN;
      }
      return pos.getY() + box.maxY;
    }

    @Override
    public boolean ownsCableTo(BlockPos anchor, BlockPos target) {
      final TileEntity tileEntity = world.getTileEntity(anchor);
      if (!(tileEntity instanceof TileEntityBridgeCableAnchor)) {
        return false;
      }
      for (BridgeCableLink link : ((TileEntityBridgeCableAnchor) tileEntity).getLinks()) {
        if (link.getTarget(anchor).equals(target)) {
          return true;
        }
      }
      return false;
    }
  }
}

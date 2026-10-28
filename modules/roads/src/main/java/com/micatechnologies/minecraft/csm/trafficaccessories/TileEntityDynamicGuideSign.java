package com.micatechnologies.minecraft.csm.trafficaccessories;

import com.micatechnologies.minecraft.csm.codeutils.AbstractTileEntity;
import com.micatechnologies.minecraft.csm.codeutils.CsmPerformance;
import com.micatechnologies.minecraft.csm.trafficaccessories.guidesign.GuideSignData;
import com.micatechnologies.minecraft.csm.trafficaccessories.truss.ISignTruss;
import net.minecraft.block.state.IBlockState;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class TileEntityDynamicGuideSign extends AbstractTileEntity {

  private static final String NBT_KEY = "signData";
  private static final String NBT_KEY_POWERED = "lightPowered";

  private String signDataJson = "";
  // Redstone power at this block, kept server-authoritative and synced so the client-side
  // renderer can light the sign in SignLightMode.REDSTONE without polling neighbors every
  // frame. Only meaningful for lighting; the sign has no other redstone behavior.
  private boolean powered = false;
  private transient GuideSignData cachedData = null;
  private transient boolean stateDirty = true;
  // What the renderer worked out from cachedData -- the sign's layout and its legend's texture
  // run -- so none of it is redone per frame. Cleared everywhere cachedData is; typed Object so
  // this common class never names a client-only renderer type.
  private transient Object renderCache = null;

  /** Furthest the sign looks above and below itself for the truss it hangs from, blocks. */
  static final int TRUSS_LOOK_UP = 8;
  static final int TRUSS_LOOK_DOWN = 4;
  /** How long {@link #getTrussTop} trusts its last look, in ticks. */
  static final long TRUSS_RECHECK_TICKS = 20L;

  // The truss behind a TRUSS sign, looked up at most once a second rather than every frame
  // (up to 13 block reads); dropped by a sync, which the server sends when the cell behind
  // changes (BlockDynamicGuideSign#neighborChanged).
  private final transient NeighbourLookExpiry trussLook =
      new NeighbourLookExpiry(TRUSS_RECHECK_TICKS);
  private transient float trussTop = Float.NaN;
  private transient float trussInset = 0.5f;

  public TileEntityDynamicGuideSign() {
  }

  @Override
  public void readNBT(NBTTagCompound compound) {
    // A sync also arrives when the cell behind changed (see the block's neighborChanged), so
    // the cached truss behind is out of date.
    trussLook.invalidate();
    signDataJson = compound.getString(NBT_KEY);
    powered = compound.getBoolean(NBT_KEY_POWERED);
    cachedData = null;
    renderCache = null;
    stateDirty = true;
  }

  @Override
  public NBTTagCompound writeNBT(NBTTagCompound compound) {
    compound.setString(NBT_KEY, signDataJson);
    compound.setBoolean(NBT_KEY_POWERED, powered);
    return compound;
  }

  public GuideSignData getSignData() {
    if (cachedData == null) {
      cachedData = GuideSignData.fromJson(signDataJson);
    }
    return cachedData;
  }

  public void setSignData(GuideSignData data) {
    if (data == null) {
      data = new GuideSignData();
    }
    this.signDataJson = data.toJson();
    this.cachedData = null;
    this.renderCache = null;
    this.stateDirty = true;
    if (getWorld() != null) {
      markDirtySync(getWorld(), getPos(), true);
    }
  }

  public void setSignDataJson(String json) {
    this.signDataJson = json != null ? json : "";
    this.cachedData = null;
    this.renderCache = null;
    this.stateDirty = true;
    if (getWorld() != null) {
      markDirtySync(getWorld(), getPos(), true);
    }
  }

  public String getSignDataJson() {
    return signDataJson;
  }

  public boolean isPowered() {
    return powered;
  }

  /**
   * Records the redstone power state and, when it actually changed, pushes it to clients
   * so redstone-controlled sign lighting updates without a block update.
   */
  public void setPowered(boolean powered) {
    if (this.powered == powered) {
      return;
    }
    this.powered = powered;
    this.stateDirty = true;
    if (getWorld() != null) {
      markDirtySync(getWorld(), getPos(), true);
    }
  }

  public boolean isStateDirty() {
    return stateDirty;
  }

  public void clearStateDirty() {
    stateDirty = false;
  }

  /** The renderer's per-sign cache, or null after any change to the sign's data. */
  public Object getRenderCache() {
    return renderCache;
  }

  public void setRenderCache(Object renderCache) {
    this.renderCache = renderCache;
  }

  /**
   * The top of the overhead sign truss behind this sign, in the sign's pixel space (16 a block, 0
   * the bottom of its own block), or {@code NaN} with none within reach. The panel sits on the
   * side opposite the way the sign faces, so the truss is in the column behind it that way.
   *
   * <p>Cached for {@link #TRUSS_RECHECK_TICKS}: the renderer asks every frame, and the look is up
   * to 13 block reads. The client never hears {@code neighborChanged}, so the server syncs a truss
   * sign when the cell behind it changes and {@link #readNBT} drops the cache; the expiry covers
   * the rest of the column and a truss whose chunk loads after the sign was first drawn.</p>
   *
   * @param facing the way the sign faces
   *
   * @return the truss's top, or {@code NaN}
   */
  public float getTrussTop(EnumFacing facing) {
    long now = world != null ? world.getTotalWorldTime() : 0L;
    if (trussLook.isStale(now, facing)) {
      lookForTruss(facing);
      trussLook.looked(now, facing);
    }
    return trussTop;
  }

  /**
   * How far behind the sign's block face the front chords of the truss {@link #getTrussTop} found
   * are; 0.5 with none.
   */
  public float getTrussInset() {
    return trussInset;
  }

  private void lookForTruss(EnumFacing facing) {
    trussTop = Float.NaN;
    trussInset = 0.5f;
    if (world == null) {
      return;
    }
    BlockPos.MutableBlockPos at = new BlockPos.MutableBlockPos();
    BlockPos behind = pos.offset(facing.getOpposite());
    for (int dy = TRUSS_LOOK_UP; dy >= -TRUSS_LOOK_DOWN; dy--) {
      at.setPos(behind.getX(), behind.getY() + dy, behind.getZ());
      IBlockState s = world.getBlockState(at);
      if (s.getBlock() instanceof ISignTruss) {
        ISignTruss truss = (ISignTruss) s.getBlock();
        float top = truss.getSignTrussTop(s);
        if (top >= 0) {
          trussInset = truss.getSignTrussFrontInset(s);
          trussTop = dy * 16.0f + top;
          return;
        }
      }
    }
  }

  @Override
  public void invalidate() {
    super.invalidate();
    if (world != null && world.isRemote) {
      TileEntityDynamicGuideSignRenderer.cleanupDisplayList(pos);
    }
  }

  @Override
  public void onChunkUnload() {
    super.onChunkUnload();
    if (world != null && world.isRemote) {
      TileEntityDynamicGuideSignRenderer.cleanupDisplayList(pos);
    }
  }

  @Override
  @SideOnly(Side.CLIENT)
  public AxisAlignedBB getRenderBoundingBox() {
    // Signs may reach 30 blocks wide and 15 tall (centered on the block), plus posts
    // running 3 blocks below; the box must cover that or big signs cull when the
    // block itself leaves the frustum.
    return new AxisAlignedBB(
        pos.getX() - 15, pos.getY() - 11, pos.getZ() - 15,
        pos.getX() + 16, pos.getY() + 9, pos.getZ() + 16);
  }

  @Override
  @SideOnly(Side.CLIENT)
  public double getMaxRenderDistanceSquared() {
    // Matches the traffic signal heads' 128 blocks. The renderer draws full detail
    // (text, shields, arrows) only inside 64 blocks; between 64 and 128 it draws a
    // cheap LOD of just the sign body, back, and posts.
    return CsmPerformance.capRenderDistanceSq(LONG_RANGE_RENDER_DISTANCE_SQUARED);
  }
}

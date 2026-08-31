package com.micatechnologies.minecraft.csm.trafficaccessories.spanwire;

import javax.annotation.Nullable;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;

/**
 * The pole-side terminus of a span of messenger cable.
 *
 * <p>Almost everything an anchor does it shares with every other attachment, so it lives in
 * {@link AbstractTileEntitySpanWireAttachment}. What is its own is the <b>optional</b> back-guy:
 * the strand run down to a ground anchor behind the pole to take the sideways pull of the span.
 * Optional is the important word -- a span is hung pole to pole and most installations have no
 * guy at all, so one is drawn only where a builder has explicitly asked for it.
 *
 * <p>An anchor with no span is inert -- hardware bolted to a pole, which is also exactly what it
 * looks like, so an unlinked anchor is not a broken state that needs reporting.
 */
public class TileEntitySpanWireAnchor extends AbstractTileEntitySpanWireAttachment {

  private static final String GUY_ANCHOR_KEY = "swGA";

  /**
   * The ground anchor this one is guyed down to, or null -- which is the normal case.
   *
   * <p>Set only by an explicit pairing made with the span wire tool. It is never derived from
   * the surrounding blocks: an earlier version searched below itself and guyed to whatever it
   * found, which put a wire on any span that happened to have a ground anchor near it.
   *
   * <p>Held on the server and sent to the client rather than worked out on both sides. That also
   * sidesteps an ordering trap worth remembering: a block's {@code onBlockAdded} runs
   * <em>during</em> the block change, so a tile entity update sent from it reaches the client
   * ahead of the block change that prompted it. A client deriving its own answer from the blocks
   * would look, see nothing there yet, and never look again.
   */
  @Nullable
  private BlockPos guyAnchor = null;

  @Override
  public void readNBT(NBTTagCompound compound) {
    super.readNBT(compound);
    guyAnchor = compound.hasKey(GUY_ANCHOR_KEY)
        ? BlockPos.fromLong(compound.getLong(GUY_ANCHOR_KEY))
        : null;
  }

  @Override
  public NBTTagCompound writeNBT(NBTTagCompound compound) {
    super.writeNBT(compound);
    if (guyAnchor != null) {
      compound.setLong(GUY_ANCHOR_KEY, guyAnchor.toLong());
    } else {
      compound.removeTag(GUY_ANCHOR_KEY);
    }
    return compound;
  }

  /**
   * Runs a back-guy down to a ground anchor, or clears it when given null.
   *
   * <p>Server side only, and the only way a guy is ever established.
   */
  public void setGuyAnchor(@Nullable BlockPos target) {
    if (world == null || world.isRemote) {
      return;
    }
    if (target == null ? guyAnchor == null : target.equals(guyAnchor)) {
      return;
    }
    guyAnchor = target;
    markDirtySync(world, pos, true);
  }

  /**
   * Drops the guy if, and only if, it runs to this position.
   *
   * <p>Called when a ground anchor is broken, so a guy is not left drawn to a block that has
   * gone. The equality test is what keeps a neighbouring anchor's guy from being cleared too.
   */
  public void clearGuyAnchorIf(BlockPos brokenGuyAnchor) {
    if (brokenGuyAnchor.equals(guyAnchor)) {
      setGuyAnchor(null);
    }
  }

  /** The ground anchor this one is guyed down to, or null. */
  @Nullable
  public BlockPos getGuyAnchor() {
    return guyAnchor;
  }

  @Override
  public long getHardwareStateKey() {
    return guyAnchor == null ? 0L : guyAnchor.toLong();
  }

  /**
   * Widened to take in the guy, which reaches below and to the side of everything else this
   * attachment draws and would otherwise be culled while still on screen.
   */
  @Override
  public AxisAlignedBB getRenderBoundingBox() {
    final AxisAlignedBB base = super.getRenderBoundingBox();
    if (guyAnchor == null) {
      return base;
    }
    return base.union(new AxisAlignedBB(guyAnchor));
  }
}

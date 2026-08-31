package com.micatechnologies.minecraft.csm.trafficaccessories.spanwire;

import net.minecraft.block.state.IBlockState;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.Vec3d;

/**
 * A mount hanging from a span of messenger cable.
 *
 * <p>This is the tile entity added to the existing wire mount blocks (the plan's D5), which
 * until now carried nothing. A mount that has never been linked into a span behaves exactly as
 * it always did -- decorative hardware -- so no existing build changes.
 *
 * <p>Beyond the span itself this holds the mount's own settings and one derived number: how far
 * below the cable it hangs. That is fixed for a given span, so it is computed when the span
 * changes rather than re-solved on demand. It sets the length of the mast drawn up to the cable,
 * and Phase 3 hands it to the signal below as its droop offset -- a path read every frame by the
 * signal head renderer.
 */
public class TileEntitySpanWireHanger extends AbstractTileEntitySpanWireAttachment {

  private static final String MOUNT_STYLE_KEY = "swMS";
  private static final String COIL_STYLE_KEY = "swCS";

  /**
   * How far this mount hangs below the cable, in blocks -- the length of hardware between them.
   * Zero when unlinked. Negative would mean the mount is above the cable, which is a placement
   * error rather than a tight fit; see {@link SpanWireDefinition#cableDropAt}.
   */
  private double cableDrop = 0.0;

  /**
   * Whether the signal gives or the mast gives when the two do not meet. Defaults to
   * {@link SpanWireMountStyle#FLUSH}, which is what every mount did before the setting existed,
   * so an already-built span keeps the look it had.
   */
  private SpanWireMountStyle mountStyle = SpanWireMountStyle.FLUSH;

  /** How much coiled conductor slack hangs at this clamp. */
  private SpanWireCoilStyle coilStyle = SpanWireCoilStyle.ONE_SIDE;

  /**
   * Whether whatever hangs directly below wants a strap drawn onto it. Derived from the payload
   * rather than listed here, so a new kind of hangable block needs no edit to this class.
   */
  private transient boolean carriesStrapPayload = false;

  /**
   * Where the hardware should come down to meet the payload, as an offset from the middle of
   * this block. Read from the payload rather than assumed, and cached with everything else that
   * is derived once rather than per frame.
   */
  private transient Vec3d payloadHardwareOffset = Vec3d.ZERO;

  @Override
  public void readNBT(NBTTagCompound compound) {
    super.readNBT(compound);
    // Absent keys leave the defaults in place, so a mount saved before these settings existed
    // reads back as the flush, single-coil mount it was drawn as.
    if (compound.hasKey(MOUNT_STYLE_KEY)) {
      mountStyle = SpanWireMountStyle.fromNBT(compound.getInteger(MOUNT_STYLE_KEY));
    }
    if (compound.hasKey(COIL_STYLE_KEY)) {
      coilStyle = SpanWireCoilStyle.fromNBT(compound.getInteger(COIL_STYLE_KEY));
    }
  }

  @Override
  public NBTTagCompound writeNBT(NBTTagCompound compound) {
    super.writeNBT(compound);
    compound.setInteger(MOUNT_STYLE_KEY, mountStyle.toNBT());
    compound.setInteger(COIL_STYLE_KEY, coilStyle.toNBT());
    return compound;
  }

  @Override
  protected void onSpanChanged() {
    super.onSpanChanged();
    refreshPayload();
    final SpanWireCatenary cable = getCable();
    // Reuses the cable the base class already solved rather than calling span.cableDropAt,
    // which would solve it a second time for the same answer.
    cableDrop = cable == null
        ? 0.0
        : cable.heightAt(getSpanParameter())
            - (pos.getY() + SpanWireDefinition.CABLE_ATTACH_HEIGHT);
  }

  /**
   * How far this mount hangs below the cable, in blocks. Positive is the buildable case -- the
   * hardware reaches up from the mount to the cable. Zero when not part of a span.
   */
  public double getCableDrop() {
    return cableDrop;
  }

  @Override
  public long getHardwareStateKey() {
    long key = (mountStyle.ordinal() * 31L + coilStyle.ordinal()) * 31L
        + (carriesStrapPayload ? 1L : 0L);
    // Quantised rather than taken raw off the double: the offset only ever holds a handful of
    // values, and this keeps the key stable against the last bit of floating point noise.
    key = key * 31L + Math.round(payloadHardwareOffset.x * 64.0);
    key = key * 31L + Math.round(payloadHardwareOffset.z * 64.0);
    return key;
  }

  /** Whether to draw a strap from this mount down onto what it carries. */
  public boolean carriesStrapPayload() {
    return carriesStrapPayload;
  }

  /**
   * Re-reads what hangs below. Called when the span changes and from the block's neighbour
   * notification, which is what catches a sign being placed under an already-strung span.
   */
  public void refreshPayload() {
    final boolean previousStrap = carriesStrapPayload;
    final Vec3d previousOffset = payloadHardwareOffset;

    final IBlockState below = world == null ? null : world.getBlockState(pos.down());
    if (below != null && below.getBlock() instanceof ISpanWireHangable) {
      final ISpanWireHangable payload = (ISpanWireHangable) below.getBlock();
      carriesStrapPayload = payload.needsSpanHangerStrap();
      payloadHardwareOffset = payload.getSpanHardwareOffset(world, pos.down(), below);
    } else {
      carriesStrapPayload = false;
      payloadHardwareOffset = Vec3d.ZERO;
    }

    if ((previousStrap != carriesStrapPayload || !previousOffset.equals(payloadHardwareOffset))
        && world != null && world.isRemote) {
      SpanWireCableRenderer.cleanupDisplayList(pos);
    }
  }

  /**
   * Where this mount's hardware comes down to meet what it carries.
   *
   * <p>Measured from the block's own centre line, <b>not</b> from {@link #getAttachPoint()}.
   * That distinction is the whole of it: the attach point is already shifted sideways by the
   * span, and since the span now follows these same payload offsets by default, adding the
   * payload offset on top of it applied the setback twice and stood the mast off the back of the
   * housing.
   *
   * <p>Keeping the two separate is also what makes the geometry self-correcting. The cable sits
   * at the span's offset and the foot at the payload's; when they agree -- which is exactly what
   * the automatic side arranges -- the offset arm has zero length and the mast is plumb. When a
   * builder overrides the side, the arm grows by the difference and still reaches.
   */
  public Vec3d getHardwareFootPoint() {
    return SpanWireDefinition.attachPoint(pos).add(payloadHardwareOffset);
  }

  public SpanWireMountStyle getMountStyle() {
    return mountStyle;
  }

  public SpanWireCoilStyle getCoilStyle() {
    return coilStyle;
  }

  /** Advances the mount style and pushes the change to clients. */
  public void cycleMountStyle() {
    mountStyle = mountStyle.getNext();
    onSettingChanged();
  }

  /**
   * Advances the signal side for the <b>whole span</b>, not just this mount.
   *
   * <p>Handled by the manager rather than here because the change belongs to the span: every
   * attachment holds a copy of it, and all of them have to be rewritten together.
   */
  /** Adds or removes the span's lower tether. Span-wide, like the signal side. */
  public void toggleBoxSpan() {
    if (world != null && !world.isRemote) {
      SpanWireManager.toggleBoxSpan(world, pos);
    }
  }

  public void cycleSignalSide() {
    if (world != null && !world.isRemote) {
      SpanWireManager.cycleSignalSide(world, pos);
    }
  }

  /** Advances the coil style and pushes the change to clients. */
  public void cycleCoilStyle() {
    coilStyle = coilStyle.getNext();
    onSettingChanged();
  }

  /**
   * Both settings change what is drawn, so the cached geometry has to go with them -- the mount
   * style also moves the signal underneath, whose own cache picks the change up on its next
   * refresh.
   */
  private void onSettingChanged() {
    if (world != null) {
      if (world.isRemote) {
        SpanWireCableRenderer.cleanupDisplayList(pos);
      }
      markDirtySync(world, pos, true);
    }
  }
}

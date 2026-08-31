package com.micatechnologies.minecraft.csm.trafficaccessories.spanwire;

import net.minecraft.util.EnumFacing;
import net.minecraft.util.IStringSerializable;
import net.minecraft.util.math.Vec3d;

/**
 * Which side of the span the signal housings sit on, and therefore where the <b>whole span</b>
 * runs to line up with them: messenger, tether, masts, clamps and all.
 *
 * <p>A signal head's housing does not sit on its block's centre line, but a cable strung between
 * two block centres does. Everything hung off that cable therefore lands beside the housings
 * rather than on them -- the mast comes down off-centre, the tether misses the housing bottoms it
 * is meant to tie, and the assembly reads as two things that happen to be near each other.
 * Shifting the span's own centre line by the same amount puts all of it back in one plane.
 *
 * <p>This started life as a tether-only fix. It applies to the messenger too because the same
 * misalignment is there, one level up, and fixing only the lower wire just moved the problem.
 *
 * <p>Which way to shift cannot be worked out from the span: it depends on which way the heads
 * face, and the span wire package deliberately knows nothing about signal geometry (the same
 * one-way dependency {@code SpanWireHangOffset} is shaped around). So it is told.
 *
 * <p>Sides are named relative to the direction the span runs, not by compass, because a span may
 * run diagonally and no compass name would be right for it. The configuration screen resolves the
 * chosen side to the compass direction it actually points on <em>this</em> span, so a builder
 * sees "Left (north)" rather than having to work it out.
 */
public enum SpanWireSignalSide implements IStringSerializable {

  /** Down the middle of the blocks. What every span did before the setting existed. */
  CENTRED("centred", "Centred"),

  /** Offset to the left of the span, looking from its first anchor toward its second. */
  LEFT("left", "Left of Span"),

  /** Offset to the right of the span, looking from its first anchor toward its second. */
  RIGHT("right", "Right of Span"),

  /**
   * Follow whatever hangs on the span, by however much that thing is off centre in its block.
   *
   * <p>The default, and the reason it exists: a signal's body sits behind the middle of its
   * block, so a span running down the block centre line puts the messenger, the tether and every
   * tie in front of the housings -- through the visors, which is where they are most visible.
   * The span is asked to sit where the payloads actually are instead, which lines all of it up
   * and leaves the mast drops vertical.
   *
   * <p>Unlike the fixed sides this is not a constant: the amount comes from the payloads
   * themselves ({@code ISpanWireHangable.getSpanHardwareOffset}) and is worked out when the span
   * is strung, then stored on it. Restring a span after changing what hangs on it, or pick a
   * fixed side, if the automatic answer is not the one you want.
   *
   * <p><b>Deliberately last.</b> The ordinal is what goes to NBT, so appending keeps every span
   * saved before this existed reading back as the side it was actually set to.
   */
  AUTO("auto", "Auto (follow signals)");

  /**
   * How far off the block centre line the span runs when a side is chosen, in blocks.
   *
   * <p>Five pixels. The first attempt was three, set against the housing alone, and it was not
   * enough: a head with a backplate has more depth behind it than the housing does, and the wires
   * ended up in front of the backplate. Five clears it.
   *
   * <p>A single number cannot be right for every head in the mod -- a backplated head and a bare
   * one genuinely want different amounts -- so this is the one to change if a particular family
   * reads badly, and a per-mount override is noted against the Phase 4B options.
   */
  public static final double OFFSET = 0.3125;

  private final String name;
  private final String friendlyName;

  SpanWireSignalSide(String name, String friendlyName) {
    this.name = name;
    this.friendlyName = friendlyName;
  }

  public String getFriendlyName() {
    return friendlyName;
  }

  /**
   * The displacement to apply, given the direction the span runs.
   *
   * @param along the span's direction, normalised and flattened to the horizontal
   *
   * @return the offset to add to a point on the centre line, zero when centred
   */
  public Vec3d offsetFor(Vec3d along) {
    if (this == CENTRED || this == AUTO) {
      // AUTO carries no offset of its own -- its amount lives on the span, which is the only
      // thing that knows what hangs from it. SpanWireDefinition.spanOffset applies it.
      return Vec3d.ZERO;
    }
    return leftwardOf(along).scale(this == LEFT ? OFFSET : -OFFSET);
  }

  /**
   * The left-hand normal of a heading, flattened to the horizontal.
   *
   * <p>Minecraft's horizontal axes are X east and Z south, so this is {@code (z, -x)}, not
   * {@code (-z, x)}. Facing east, left is north. The sign is easy to get backwards -- it was, once
   * -- so it lives here alone and a test pins it.
   */
  public static Vec3d leftwardOf(Vec3d along) {
    return new Vec3d(along.z, 0.0, -along.x);
  }

  /**
   * The compass direction this side points on a span running the given way, for display. Null
   * when centred, which points nowhere.
   */
  public EnumFacing compassFor(Vec3d along) {
    return compassFor(along, offsetFor(along));
  }

  /**
   * The compass direction an actual displacement points, for display. Null when it is nowhere,
   * which is what a centred span and an automatic span with nothing to follow both are.
   */
  public static EnumFacing compassFor(Vec3d along, Vec3d offset) {
    if (offset.x * offset.x + offset.z * offset.z < 1.0e-9) {
      return null;
    }
    return Math.abs(offset.x) > Math.abs(offset.z)
        ? (offset.x > 0 ? EnumFacing.EAST : EnumFacing.WEST)
        : (offset.z > 0 ? EnumFacing.SOUTH : EnumFacing.NORTH);
  }

  public int toNBT() {
    return ordinal();
  }

  public static SpanWireSignalSide fromNBT(int ordinal) {
    if (ordinal < 0 || ordinal >= values().length) {
      return CENTRED;
    }
    return values()[ordinal];
  }

  /** Cycles to the next side, wrapping. */
  public SpanWireSignalSide getNext() {
    return values()[(ordinal() + 1) % values().length];
  }

  @Override
  public String getName() {
    return name;
  }
}

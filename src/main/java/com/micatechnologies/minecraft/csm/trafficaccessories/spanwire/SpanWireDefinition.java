package com.micatechnologies.minecraft.csm.trafficaccessories.spanwire;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import javax.annotation.Nullable;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

/**
 * One span of messenger cable: the two anchors it runs between, the hanger mounts along it, and
 * how much slack it was strung with.
 *
 * <p>Every block taking part in a span stores a complete copy of this, not a pointer to a single
 * owner. That is deliberate. Each attachment draws its own piece of the cable (the plan's D6),
 * and to place that piece correctly it has to solve the <em>whole</em> curve -- the shape at any
 * point depends on both far ends. A copy on every participant means each one can do that alone,
 * without reaching into chunks that may not be loaded, and a span whose middle has unloaded
 * still draws correctly at the ends. The cost is a handful of block positions per tile entity.
 *
 * <p>Immutable. Editing a span means building a new definition and writing it to every
 * participant, which is what keeps the copies from drifting.
 */
public final class SpanWireDefinition {

  /**
   * Height within a block, in blocks, at which cable attaches. Both the anchors and the hanger
   * mounts present their attachment near their top -- the existing vertical wire mount's own
   * hitbox occupies the top half of its block.
   *
   * <p>Provisional: Phase 6 replaces it with a per-block value once real anchor and clamp
   * hardware exists and there is a modelled eyebolt to attach to.
   */
  public static final double CABLE_ATTACH_HEIGHT = 0.75;

  private static final String ANCHOR_A_KEY = "swA";
  private static final String ANCHOR_B_KEY = "swB";
  private static final String HANGERS_KEY = "swH";
  private static final String SLACK_KEY = "swS";
  private static final String BOX_SPAN_KEY = "swBox";
  private static final String SIGNAL_SIDE_KEY = "swSS";
  private static final String AUTO_OFFSET_KEY = "swAO";
  private static final String AUTO_OFFSET_X_KEY = "swOX";
  private static final String AUTO_OFFSET_Z_KEY = "swOZ";
  private static final String TETHER_CLEARANCE_KEY = "swTC";
  private static final String TETHER_ANCHOR_A_KEY = "swTA";
  private static final String TETHER_ANCHOR_B_KEY = "swTB";

  /**
   * The <em>smallest</em> gap the tether keeps below the messenger anywhere along a span, in
   * blocks. Sized against a hanging three-section head, whose model reaches about 2.25 blocks
   * from a top that sits just under the cable, plus margin so the tether passes under it rather
   * than through its bottom lamp.
   *
   * <p><b>This is a minimum clearance, not a fixed drop, and that distinction is the whole
   * problem.</b> The tether is strung hard and the messenger is not, so the two are not parallel:
   * the messenger falls away from its anchors far faster and the gap between them <em>closes</em>
   * toward midspan. A fixed drop is therefore only correct at the anchors. On a 20-block span the
   * messenger sags 1.0 blocks and the tether 0.2, so a 2.75 drop leaves just 1.95 at midspan —
   * straight through the signals — and a 40-block span is twice as bad. Hanging the tether by
   * {@link #tetherDropAtAnchors()} instead makes the clearance at the tightest point exactly this
   * value at every span length.
   *
   * <p>One number still cannot suit every head height on a mixed span; a per-mount tether height
   * is noted against the Phase 4B mount options.
   */
  public static final double TETHER_MIN_CLEARANCE = 2.6;

  /**
   * The tether is strung far tighter than the messenger it hangs beneath. That is not decoration:
   * the messenger carries the weight and is allowed to sag, while the tether only has to stop the
   * heads turning, so it is pulled up hard. A real lower tether sags roughly 75-85% less than the
   * span wire above it.
   *
   * <p>This value is not guessed — it is solved. At the messenger default of {@code 1.0066}
   * (a 5% sag) this gives a tether sagging <b>80% less</b>, the middle of that range, and because
   * both hold their sag ratio at any span length the relationship holds for every span.
   * {@code SpanWireDefinitionTest} pins the ratio so it cannot drift back toward the messenger
   * unnoticed.
   */
  public static final double TETHER_SLACK = 1.000265;

  private final BlockPos anchorA;
  private final BlockPos anchorB;
  private final List<BlockPos> hangers;
  private final double slack;
  private final boolean boxSpan;
  private final SpanWireSignalSide signalSide;

  /**
   * How far off the block centre line this span runs when its side is
   * {@link SpanWireSignalSide#AUTO}, signed, positive to the left of the run.
   *
   * <p>Measured from the payloads when the span is strung rather than chosen, which is what makes
   * the messenger and the tether line up with the signal housings without anyone configuring
   * anything. Kept even while a fixed side is selected, so switching back to automatic does not
   * throw the measurement away.
   */
  private final double autoOffsetX;
  private final double autoOffsetZ;

  /**
   * How far below the messenger the tether hangs at its tightest point, in blocks.
   *
   * <p>Measured from the payloads when the span is strung, for the reason
   * {@link #TETHER_MIN_CLEARANCE} could not be: how far a head reaches below the cable depends on
   * how many sections it has and how big they are, so one constant clears a three-section head
   * and is cut straight through a taller one. The measurement takes the <b>deepest</b> payload on
   * the span, so the tether passes under all of them and every tie points upward.
   */
  private final double tetherClearance;

  /**
   * The anchors a box span's tether dead-ends on, or null for a tether hung at a derived height.
   *
   * <p>These are ordinary {@code spanwireanchor} blocks placed lower on the same poles -- the same
   * block as the messenger's own anchors, which is the point: the lower end of a box span then
   * looks exactly like the upper one, has a hitbox, and takes part in the traffic poles'
   * auto-connection like any other block. Deriving the height from the payloads was only ever the
   * fallback for a builder who has not placed them.
   *
   * <p>Both or neither. One end anchored and the other floating would be a tether at two different
   * heights depending on which end you looked at, which is worse than either answer alone.
   */
  @Nullable
  private final BlockPos tetherAnchorA;

  @Nullable
  private final BlockPos tetherAnchorB;

  /**
   * Lazily computed {@link #hashCode()}. Not final because it is filled on first use; not volatile
   * because the computation is pure and idempotent, so two threads racing on it can only arrive at
   * the same value (the same reasoning {@link String#hashCode()} uses).
   */
  private transient int cachedHash;

  public SpanWireDefinition(BlockPos anchorA, BlockPos anchorB, List<BlockPos> hangers,
      double slack) {
    this(anchorA, anchorB, hangers, slack, false);
  }

  public SpanWireDefinition(BlockPos anchorA, BlockPos anchorB, List<BlockPos> hangers,
      double slack, boolean boxSpan) {
    this(anchorA, anchorB, hangers, slack, boxSpan, SpanWireSignalSide.AUTO, 0.0);
  }

  public SpanWireDefinition(BlockPos anchorA, BlockPos anchorB, List<BlockPos> hangers,
      double slack, boolean boxSpan, SpanWireSignalSide signalSide) {
    this(anchorA, anchorB, hangers, slack, boxSpan, signalSide, 0.0);
  }

  public SpanWireDefinition(BlockPos anchorA, BlockPos anchorB, List<BlockPos> hangers,
      double slack, boolean boxSpan, SpanWireSignalSide signalSide, double autoOffset) {
    this(anchorA, anchorB, hangers, slack, boxSpan, signalSide, autoOffset,
        TETHER_MIN_CLEARANCE);
  }

  public SpanWireDefinition(BlockPos anchorA, BlockPos anchorB, List<BlockPos> hangers,
      double slack, boolean boxSpan, SpanWireSignalSide signalSide, double autoOffset,
      double tetherClearance) {
    this(anchorA, anchorB, hangers, slack, boxSpan, signalSide, autoOffset, 0.0, tetherClearance,
        null, null);
  }

  public SpanWireDefinition(BlockPos anchorA, BlockPos anchorB, List<BlockPos> hangers,
      double slack, boolean boxSpan, SpanWireSignalSide signalSide, double autoOffsetX,
      double autoOffsetZ, double tetherClearance, @Nullable BlockPos tetherAnchorA,
      @Nullable BlockPos tetherAnchorB) {
    this.tetherClearance = tetherClearance;
    this.tetherAnchorA = tetherAnchorA;
    this.tetherAnchorB = tetherAnchorB;
    this.anchorA = anchorA;
    this.anchorB = anchorB;
    this.hangers = Collections.unmodifiableList(new ArrayList<>(hangers));
    this.slack = slack;
    this.boxSpan = boxSpan;
    this.signalSide = signalSide;
    this.autoOffsetX = autoOffsetX;
    this.autoOffsetZ = autoOffsetZ;
  }

  /**
   * Which side of the span the signal housings sit on, which is where the tether runs.
   *
   * <p>A property of the <b>span</b>, not of one mount, even though it is set from a mount's
   * configuration screen. The tether is a single wire from anchor to anchor; if each mount held
   * its own answer the wire would zigzag between them.
   */
  public SpanWireSignalSide getSignalSide() {
    return signalSide;
  }

  /** A copy of this span with a different signal side. */
  public SpanWireDefinition withSignalSide(SpanWireSignalSide side) {
    return new SpanWireDefinition(anchorA, anchorB, hangers, slack, boxSpan, side, autoOffsetX,
        autoOffsetZ, tetherClearance, tetherAnchorA, tetherAnchorB);
  }

  /** The same span with a different measured tether clearance. */
  public SpanWireDefinition withTetherClearance(double clearance) {
    return new SpanWireDefinition(anchorA, anchorB, hangers, slack, boxSpan, signalSide,
        autoOffsetX, autoOffsetZ, clearance, tetherAnchorA, tetherAnchorB);
  }

  /** The same span with the tether dead-ended on a pair of lower anchors, or on neither. */
  public SpanWireDefinition withTetherAnchors(@Nullable BlockPos lowerA,
      @Nullable BlockPos lowerB) {
    final boolean both = lowerA != null && lowerB != null;
    return new SpanWireDefinition(anchorA, anchorB, hangers, slack, boxSpan, signalSide,
        autoOffsetX, autoOffsetZ, tetherClearance, both ? lowerA : null, both ? lowerB : null);
  }

  /** Whether this span's tether dead-ends on placed anchors rather than hanging at a derived drop. */
  public boolean hasTetherAnchors() {
    return tetherAnchorA != null && tetherAnchorB != null;
  }

  /**
   * The lower anchor belonging to the end of this span nearest the given anchor, or null.
   *
   * <p>Matched by which end was asked for rather than by distance, so the two ends cannot swap on
   * a short span where both lower anchors are within reach of both ends.
   */
  @Nullable
  public BlockPos getTetherAnchorFor(BlockPos upperAnchor) {
    if (!hasTetherAnchors()) {
      return null;
    }
    return upperAnchor.equals(anchorA) ? tetherAnchorA
        : upperAnchor.equals(anchorB) ? tetherAnchorB : null;
  }

  /** How far below the messenger this span's tether hangs at its tightest point. */
  public double getTetherClearance() {
    return tetherClearance;
  }

  /** The same span with the lower tether added or taken away. */
  public SpanWireDefinition withBoxSpan(boolean box) {
    return new SpanWireDefinition(anchorA, anchorB, hangers, slack, box, signalSide, autoOffsetX,
        autoOffsetZ, tetherClearance, tetherAnchorA, tetherAnchorB);
  }

  /**
   * The same span with a different automatic sideways amount, as measured from its payloads.
   *
   * <p>Only has any effect while the side is {@link SpanWireSignalSide#AUTO}; a span set to a
   * fixed side keeps the value but ignores it, so switching back to automatic does not lose it.
   */
  public SpanWireDefinition withAutoOffset(Vec3d offset) {
    return new SpanWireDefinition(anchorA, anchorB, hangers, slack, boxSpan, signalSide, offset.x,
        offset.z, tetherClearance, tetherAnchorA, tetherAnchorB);
  }

  /**
   * Where an automatic span sits relative to the block centre line: a full horizontal
   * displacement, not just a sideways one.
   *
   * <p>It used to be a signed distance along the span's left-hand normal, which was wrong for any
   * span that does not run along an axis. A head's body is set back along the way it <em>faces</em>,
   * and on a diagonal that direction is not perpendicular to the wire -- so a purely perpendicular
   * shift could never put the wire over the housings, and everything hung off it leaned to make up
   * the difference.
   *
   * <p>Translating the whole span by the payload's own offset puts the wire directly over what it
   * carries at any angle. It moves the ends slightly along the span as well as across it, which is
   * harmless: an anchor's shackle already takes up the difference between its fixed eyebolt and
   * where the wire actually lands.
   */
  public Vec3d getAutoOffset() {
    return new Vec3d(autoOffsetX, 0.0, autoOffsetZ);
  }

  /**
   * The span's horizontal direction, normalised -- the axis the tether is offset across.
   */
  public Vec3d horizontalDirection() {
    final Vec3d delta = attachPoint(anchorB).subtract(attachPoint(anchorA));
    final double length = Math.sqrt(delta.x * delta.x + delta.z * delta.z);
    if (length < 1.0e-9) {
      return new Vec3d(1.0, 0.0, 0.0);
    }
    return new Vec3d(delta.x / length, 0.0, delta.z / length);
  }

  /**
   * The displacement the whole span is shifted by across its own direction, or zero when centred.
   *
   * <p>Applied to every attach point, so the messenger, the tether, the masts and the clamps all
   * move together and the span stays one assembly. Being perpendicular, it does not change where
   * along the span anything sits -- a position still projects onto the chord at the same
   * fraction, so nothing that depends on that has to know this exists.
   */
  public Vec3d spanOffset() {
    if (signalSide == SpanWireSignalSide.AUTO) {
      return getAutoOffset();
    }
    return signalSide.offsetFor(horizontalDirection());
  }

  /**
   * Where cable meets hardware at an attachment on <em>this</em> span -- the block's own attach
   * point, shifted by {@link #spanOffset()}.
   *
   * <p>Prefer this to the static {@link #attachPoint(BlockPos)} anywhere the result is drawn.
   * The static one is the raw block centre and is still what the geometry is defined against;
   * this is where the span actually is.
   */
  public Vec3d attachPointOn(BlockPos pos) {
    return attachPoint(pos).add(spanOffset());
  }

  /**
   * Whether this span carries a second, lower tether tying the bottoms of its signals together.
   * Real installations use one to stop the heads turning in wind; a free-swinging span does not.
   */
  public boolean isBoxSpan() {
    return boxSpan;
  }

  public BlockPos getAnchorA() {
    return anchorA;
  }

  public BlockPos getAnchorB() {
    return anchorB;
  }

  /** The hanger mounts along this span, ordered from {@link #getAnchorA()} toward B. */
  public List<BlockPos> getHangers() {
    return hangers;
  }

  public double getSlack() {
    return slack;
  }

  /**
   * Every block taking part in this span, ordered along the cable: anchor A, the hangers, anchor
   * B. This ordering is what makes a segment's owner well defined -- each attachment owns the
   * cable running to the next entry.
   */
  public List<BlockPos> getAttachments() {
    final List<BlockPos> all = new ArrayList<>(hangers.size() + 2);
    all.add(anchorA);
    all.addAll(hangers);
    all.add(anchorB);
    return all;
  }

  public boolean involves(BlockPos pos) {
    return anchorA.equals(pos) || anchorB.equals(pos) || hangers.contains(pos);
  }

  public boolean isAnchor(BlockPos pos) {
    return anchorA.equals(pos) || anchorB.equals(pos);
  }

  /**
   * The attachment the cable runs to next after the given one, or null if the given position is
   * the far end (which owns no segment) or is not part of this span.
   */
  @Nullable
  public BlockPos nextAfter(BlockPos pos) {
    final List<BlockPos> all = getAttachments();
    final int index = all.indexOf(pos);
    if (index < 0 || index >= all.size() - 1) {
      return null;
    }
    return all.get(index + 1);
  }

  /** The point in world space where cable meets hardware at the given attachment. */
  public static Vec3d attachPoint(BlockPos pos) {
    return new Vec3d(pos.getX() + 0.5, pos.getY() + CABLE_ATTACH_HEIGHT, pos.getZ() + 0.5);
  }

  /** Solves the messenger for this whole span. */
  public SpanWireCatenary solve() {
    return SpanWireCatenary.between(attachPointOn(anchorA), attachPointOn(anchorB), slack);
  }

  /**
   * Solves the lower tether of a box span, or returns null when this span has none.
   *
   * <p>Strung between the same two anchors, lower down and tighter. Because it spans the same
   * horizontal chord as the messenger, a position along one is the same fraction along the
   * other, so every attachment can draw its piece of both from the one parameter it already has.
   */
  @Nullable
  public SpanWireCatenary solveTether() {
    if (!boxSpan) {
      return null;
    }
    // Dead-ended on placed anchors where there are any: the tether then runs anchor to anchor
    // exactly as the messenger does, at whatever height the builder put them, and needs no
    // derived drop at all. Both ends inherit the span's sideways offset through attachPointOn.
    if (hasTetherAnchors()) {
      return SpanWireCatenary.between(attachPointOn(tetherAnchorA), attachPointOn(tetherAnchorB),
          TETHER_SLACK);
    }
    final double drop = tetherDropAtAnchors();
    // Inherits the span's own offset through attachPointOn, so the tether stays directly under
    // the messenger rather than being shifted twice or not at all.
    final Vec3d a = attachPointOn(anchorA).subtract(0.0, drop, 0.0);
    final Vec3d b = attachPointOn(anchorB).subtract(0.0, drop, 0.0);
    return SpanWireCatenary.between(a, b, TETHER_SLACK);
  }

  /**
   * How far below the messenger's own anchor points the tether is dead ended, in blocks.
   *
   * <p>Derived rather than fixed: it is {@link #TETHER_MIN_CLEARANCE} plus however much further
   * the messenger sags than the tether does. That extra is exactly the amount the two converge by
   * at midspan, so adding it at the anchors leaves the clearance at midspan equal to the minimum
   * and the gap widest at the ends — which is both what keeps the tether clear of the signals and
   * what a real box span looks like.
   *
   * <p>A tether's sag depends only on the chord it crosses and how hard it is strung, not on how
   * high it hangs, so the probe below measures the right number without knowing its own answer
   * first.
   */
  public double tetherDropAtAnchors() {
    final double messengerSag = solve().sag();
    final SpanWireCatenary probe =
        SpanWireCatenary.between(attachPoint(anchorA), attachPoint(anchorB), TETHER_SLACK);
    return tetherClearance + Math.max(0.0, messengerSag - probe.sag());
  }

  /**
   * How far the given attachment hangs below the cable, in blocks -- the length of hardware
   * needed to reach from the cable down to it.
   *
   * <p>Sign matters, and it is the whole rule for placing mounts. <b>Positive means the mount
   * sits below the cable, which is the only arrangement that can be built:</b> a clamp grips the
   * messenger and everything else hangs off it. Negative means the mount is above the cable,
   * where no hardware could reach it — that is a placement error, not a tight fit.
   *
   * <p>It is also why a mount level with its anchors is wrong rather than ideal. The cable sags
   * away from the chord between the anchors, so a mount left at anchor height ends up above the
   * cable everywhere except at the anchors themselves. On a 20-block span at the default slack a
   * builder wants their mounts a block lower.
   */
  public double cableDropAt(BlockPos pos) {
    final SpanWireCatenary cable = solve();
    final double t = cable.parameterAt(pos.getX() + 0.5, pos.getZ() + 0.5);
    return cable.heightAt(t) - (pos.getY() + CABLE_ATTACH_HEIGHT);
  }

  public NBTTagCompound writeToNBT(NBTTagCompound compound) {
    compound.setLong(ANCHOR_A_KEY, anchorA.toLong());
    compound.setLong(ANCHOR_B_KEY, anchorB.toLong());
    final long[] packed = new long[hangers.size()];
    for (int i = 0; i < hangers.size(); i++) {
      packed[i] = hangers.get(i).toLong();
    }
    // NBT has no long-array tag in 1.12, so hanger positions ride in an int array as
    // high/low word pairs. BlockPos.toLong is lossless for any position a world can hold.
    final int[] words = new int[packed.length * 2];
    for (int i = 0; i < packed.length; i++) {
      words[i * 2] = (int) (packed[i] >> 32);
      words[i * 2 + 1] = (int) packed[i];
    }
    compound.setIntArray(HANGERS_KEY, words);
    compound.setDouble(SLACK_KEY, slack);
    // Written only for a box span, so an ordinary span's tag is byte for byte what it was before
    // tethers existed and a world that never builds one never grows the key.
    if (boxSpan) {
      compound.setBoolean(BOX_SPAN_KEY, true);
      // Only a box span has a tether, so only a box span carries the measurement for one.
      compound.setDouble(TETHER_CLEARANCE_KEY, tetherClearance);
      if (hasTetherAnchors()) {
        compound.setLong(TETHER_ANCHOR_A_KEY, tetherAnchorA.toLong());
        compound.setLong(TETHER_ANCHOR_B_KEY, tetherAnchorB.toLong());
      }
    }
    if (signalSide != SpanWireSignalSide.CENTRED) {
      compound.setInteger(SIGNAL_SIDE_KEY, signalSide.toNBT());
      compound.setDouble(AUTO_OFFSET_X_KEY, autoOffsetX);
      compound.setDouble(AUTO_OFFSET_Z_KEY, autoOffsetZ);
    }
    return compound;
  }

  /**
   * Reads a span from NBT, or returns null if the tag holds none. A tag written by an older
   * build, or a truncated one, yields null rather than a half-built span.
   */
  @Nullable
  public static SpanWireDefinition readFromNBT(NBTTagCompound compound) {
    if (!compound.hasKey(ANCHOR_A_KEY) || !compound.hasKey(ANCHOR_B_KEY)) {
      return null;
    }
    final BlockPos anchorA = BlockPos.fromLong(compound.getLong(ANCHOR_A_KEY));
    final BlockPos anchorB = BlockPos.fromLong(compound.getLong(ANCHOR_B_KEY));
    final int[] words = compound.getIntArray(HANGERS_KEY);
    if (words.length % 2 != 0) {
      return null;
    }
    final List<BlockPos> hangers = new ArrayList<>(words.length / 2);
    for (int i = 0; i < words.length; i += 2) {
      final long packed = ((long) words[i] << 32) | (words[i + 1] & 0xFFFFFFFFL);
      hangers.add(BlockPos.fromLong(packed));
    }
    double slack = compound.hasKey(SLACK_KEY) ? compound.getDouble(SLACK_KEY)
        : SpanWireCatenary.DEFAULT_SLACK;
    if (Double.isNaN(slack) || slack < 1.0) {
      slack = SpanWireCatenary.DEFAULT_SLACK;
    }
    return new SpanWireDefinition(anchorA, anchorB, hangers, slack,
        compound.getBoolean(BOX_SPAN_KEY),
        SpanWireSignalSide.fromNBT(compound.getInteger(SIGNAL_SIDE_KEY)),
        compound.hasKey(AUTO_OFFSET_X_KEY) ? compound.getDouble(AUTO_OFFSET_X_KEY) : 0.0,
        compound.hasKey(AUTO_OFFSET_Z_KEY) ? compound.getDouble(AUTO_OFFSET_Z_KEY) : 0.0,
        compound.hasKey(TETHER_CLEARANCE_KEY)
            ? compound.getDouble(TETHER_CLEARANCE_KEY)
            : TETHER_MIN_CLEARANCE,
        compound.hasKey(TETHER_ANCHOR_A_KEY)
            ? BlockPos.fromLong(compound.getLong(TETHER_ANCHOR_A_KEY)) : null,
        compound.hasKey(TETHER_ANCHOR_B_KEY)
            ? BlockPos.fromLong(compound.getLong(TETHER_ANCHOR_B_KEY)) : null);
  }

  /** Clears every key this class writes, so a torn-down span leaves nothing behind in NBT. */
  public static void clearNBT(NBTTagCompound compound) {
    compound.removeTag(ANCHOR_A_KEY);
    compound.removeTag(ANCHOR_B_KEY);
    compound.removeTag(HANGERS_KEY);
    compound.removeTag(SLACK_KEY);
    compound.removeTag(BOX_SPAN_KEY);
    compound.removeTag(SIGNAL_SIDE_KEY);
    compound.removeTag(AUTO_OFFSET_KEY);
    compound.removeTag(AUTO_OFFSET_X_KEY);
    compound.removeTag(AUTO_OFFSET_Z_KEY);
    compound.removeTag(TETHER_CLEARANCE_KEY);
    compound.removeTag(TETHER_ANCHOR_A_KEY);
    compound.removeTag(TETHER_ANCHOR_B_KEY);
  }

  /** A copy of this span with the given hanger removed; used when one is broken. */
  public SpanWireDefinition withoutHanger(BlockPos pos) {
    final List<BlockPos> remaining = new ArrayList<>(hangers);
    remaining.remove(pos);
    return new SpanWireDefinition(anchorA, anchorB, remaining, slack, boxSpan, signalSide,
        autoOffsetX, autoOffsetZ, tetherClearance, tetherAnchorA, tetherAnchorB);
  }

  @Override
  public boolean equals(Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof SpanWireDefinition)) {
      return false;
    }
    final SpanWireDefinition that = (SpanWireDefinition) other;
    return anchorA.equals(that.anchorA)
        && anchorB.equals(that.anchorB)
        && hangers.equals(that.hangers)
        && Double.compare(slack, that.slack) == 0
        && boxSpan == that.boxSpan
        && Double.compare(autoOffsetX, that.autoOffsetX) == 0
        && Double.compare(autoOffsetZ, that.autoOffsetZ) == 0
        && Double.compare(tetherClearance, that.tetherClearance) == 0
        && Objects.equals(tetherAnchorA, that.tetherAnchorA)
        && Objects.equals(tetherAnchorB, that.tetherAnchorB)
        && signalSide == that.signalSide;
  }

  @Override
  public int hashCode() {
    // Computed once and kept. Every attachment on the span asks for this once a frame, purely to
    // decide whether its cached display list is still valid, and the un-cached form allocated an
    // eleven-element array, boxed five primitives and walked the whole hanger list to answer --
    // per attachment, per frame, so the cost grew with the square of the span. Safe because every
    // field here is final and the hanger list is unmodifiable: this object cannot become a
    // different object, which is the only thing that could make a remembered hash wrong.
    if (cachedHash == 0) {
      final int computed = Arrays.hashCode(
          new Object[]{anchorA, anchorB, hangers, slack, boxSpan, signalSide, autoOffsetX,
              autoOffsetZ, tetherClearance, tetherAnchorA, tetherAnchorB});
      // Zero is the "not computed yet" marker, so a span that genuinely hashes to zero would be
      // recomputed every time. Harmless, and far cheaper than a second field to track validity.
      cachedHash = computed;
      return computed;
    }
    return cachedHash;
  }

  @Override
  public String toString() {
    return "SpanWire[" + anchorA + " -> " + anchorB + ", " + hangers.size() + " hangers, slack "
        + slack + (boxSpan ? ", box span" : "") + "]";
  }
}

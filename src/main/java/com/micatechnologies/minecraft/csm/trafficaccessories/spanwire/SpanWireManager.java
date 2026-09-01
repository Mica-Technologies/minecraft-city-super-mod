package com.micatechnologies.minecraft.csm.trafficaccessories.spanwire;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nullable;
import net.minecraft.block.state.IBlockState;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

/**
 * Creating, updating and tearing down spans of messenger cable.
 *
 * <p>All of it is server-side bookkeeping over tile entity data. A span carries nothing and
 * collides with nothing (the plan's D9), so there is no network to walk, no per-tick work, and
 * no world save data -- a span exists only as the copies of its {@link SpanWireDefinition} held
 * by the blocks taking part in it.
 */
public final class SpanWireManager {

  /**
   * Longest span that may be strung, in blocks. Well past any real intersection; it exists so a
   * mis-click across a continent cannot start a scan of a hundred thousand block positions.
   */
  public static final int MAX_SPAN_LENGTH = 64;

  /** Shortest span. Two adjacent anchors leave no room for cable or for anything to hang. */
  public static final int MIN_SPAN_LENGTH = 2;

  /**
   * Longest run of hanger hardware, in blocks, between the cable and the mount it carries.
   *
   * <p>Sized against reality rather than convenience: at the default 5% sag a 20-block span
   * hangs a full block low at midspan, so a mount that sits right under one part of a span is a
   * block off elsewhere, and the hardware has to absorb that. Past this the mount is so far from
   * the cable that the builder should move it rather than have a long rod drawn to reach it.
   */
  public static final double MAX_HANGER_DROP = 1.5;

  /**
   * How far below the lower anchor to look for hanger mounts, beyond the computed sag. Mounts
   * are placed to follow the cable, so they are found below the anchors, not level with them.
   */
  private static final int HANGER_SEARCH_MARGIN = 2;

  /** How far above the higher anchor to look. Small: a mount above both anchors is a mistake. */
  private static final int HANGER_SEARCH_ABOVE = 1;

  /**
   * How far the hanger walk advances along the chord per step, in blocks. Below half a block so
   * that a diagonal cannot skip a column it only clips the corner of.
   */
  private static final double COLUMN_WALK_STEP = 0.25;

  private SpanWireManager() {
  }

  /** Outcome of a link attempt, with a message fit to show the player either way. */
  public static final class LinkResult {

    private final boolean success;
    private final String message;
    @Nullable
    private final SpanWireDefinition span;

    private LinkResult(boolean success, String message, @Nullable SpanWireDefinition span) {
      this.success = success;
      this.message = message;
      this.span = span;
    }

    static LinkResult failure(String message) {
      return new LinkResult(false, message, null);
    }

    static LinkResult success(String message, SpanWireDefinition span) {
      return new LinkResult(true, message, span);
    }

    public boolean isSuccess() {
      return success;
    }

    public String getMessage() {
      return message;
    }

    @Nullable
    public SpanWireDefinition getSpan() {
      return span;
    }
  }

  /**
   * Strings a span between two anchors, discovering the hanger mounts along the way and writing
   * the result to every block taking part.
   *
   * <p>Any span either anchor already belonged to is torn down first, so re-linking an anchor
   * replaces its span rather than leaving an orphan behind.
   */
  public static LinkResult link(World world, BlockPos anchorA, BlockPos anchorB, double slack) {
    return link(world, anchorA, anchorB, slack, false);
  }

  /**
   * Strings a span, optionally as a box span carrying a lower tether.
   */
  public static LinkResult link(World world, BlockPos anchorA, BlockPos anchorB, double slack,
      boolean boxSpan) {
    if (anchorA.equals(anchorB)) {
      return LinkResult.failure("Both ends are the same anchor.");
    }
    if (!isAnchor(world, anchorA) || !isAnchor(world, anchorB)) {
      return LinkResult.failure("Both ends of a span must be span wire anchors.");
    }

    // Straight-line distance, not Manhattan. A diagonal span's Manhattan length overstates it by
    // up to 41%, which would have rejected perfectly ordinary skewed intersections.
    final double horizontalLength = Math.hypot(anchorB.getX() - anchorA.getX(),
        anchorB.getZ() - anchorA.getZ());
    if (horizontalLength < MIN_SPAN_LENGTH) {
      return LinkResult.failure("Anchors are too close together to string a span.");
    }
    if (horizontalLength > MAX_SPAN_LENGTH) {
      return LinkResult.failure(
          "Span is " + Math.round(horizontalLength) + " blocks; the longest supported is "
              + MAX_SPAN_LENGTH + ".");
    }

    detachExistingSpan(world, anchorA);
    detachExistingSpan(world, anchorB);

    final SpanWireDefinition bare =
        new SpanWireDefinition(anchorA, anchorB, new ArrayList<>(), slack, boxSpan);
    final double expectedSag = bare.solve().sag();

    final List<BlockPos> hangers = discoverHangers(world, anchorA, anchorB, expectedSag);
    final SpanWireDefinition span =
        new SpanWireDefinition(anchorA, anchorB, hangers, slack, boxSpan);

    apply(world, span);

    // Measured after the span is on the blocks, not before, and the order matters. A sign only
    // reports where its panel is once it is in setback, and it only goes into setback once it
    // knows it is on a span -- so asking first would get the answer for a sign that is still
    // sitting at the front of its block. Applying twice at link time is cheap; this runs when a
    // player clicks two anchors, not per tick.
    SpanWireDefinition aligned = span.withAutoOffset(measureAutoOffset(world, span));
    if (aligned.isBoxSpan()) {
      aligned = attachTetherAnchors(world, aligned);
      aligned = aligned.withTetherClearance(measureTetherClearance(world, aligned));
    }
    if (!aligned.equals(span)) {
      apply(world, aligned);
    }

    return LinkResult.success(buildSummary(world, aligned, expectedSag), aligned);
  }

  /**
   * How far below the messenger this span's tether has to hang to pass under everything on it.
   *
   * <p>The <b>deepest</b> payload wins, not the median: a tether that clears most of the heads and
   * cuts through one is worse than one hanging slightly low, and every tie has to point upward for
   * the ties to be drawn at all. Payloads that take no tie -- signs -- are not consulted, since
   * nothing ties to them and there is no reason to drop the wire to clear one.
   *
   * <p>Falls back to {@link SpanWireDefinition#TETHER_MIN_CLEARANCE} when nothing on the span
   * answers, which is the same figure spans used before this was measured.
   */
  /**
   * How far below an anchor to look for the tether's own anchor on the same pole.
   *
   * <p>Generous enough for a tall signal assembly to hang between the two wires, and bounded so
   * a lone anchor at the foot of a pole is not adopted by a span several blocks above it.
   */
  private static final int MAX_TETHER_ANCHOR_DROP = 8;

  /**
   * Finds the pair of lower anchors a box span's tether should dead-end on, if the builder has
   * placed them.
   *
   * <p><b>Both ends or neither.</b> One end dead-ended and the other hanging at a derived drop
   * would be a tether at two different heights depending on which end you looked at -- worse than
   * either answer on its own -- so a single lower anchor is ignored until its partner exists.
   */
  private static SpanWireDefinition attachTetherAnchors(World world, SpanWireDefinition span) {
    final BlockPos lowerA = findTetherAnchorBelow(world, span.getAnchorA());
    final BlockPos lowerB = findTetherAnchorBelow(world, span.getAnchorB());
    return span.withTetherAnchors(lowerA, lowerB);
  }

  /**
   * Re-derives the tether ends of any span that was dead-ended on an anchor at this position.
   *
   * <p>The inverse of {@link #findTetherAnchorBelow}: a span that could be using this block is a
   * span whose anchor is directly above it within the same reach, so one definition of range
   * serves both directions and they cannot drift apart.
   *
   * <p>Losing one end drops <em>both</em>, by the both-or-neither rule, and the tether falls back
   * to a derived drop rather than being left hanging off a block that is gone.
   */
  public static void onTetherAnchorRemoved(World world, BlockPos removed) {
    if (world.isRemote) {
      return;
    }
    for (int rise = 1; rise <= MAX_TETHER_ANCHOR_DROP; rise++) {
      final BlockPos above = removed.up(rise);
      if (!world.isBlockLoaded(above)) {
        return;
      }
      final SpanWireDefinition span = getSpanAt(world, above);
      if (span == null || !span.hasTetherAnchors()) {
        continue;
      }
      if (removed.equals(span.getTetherAnchorFor(above))) {
        apply(world, span.withTetherAnchors(null, null));
      }
    }
  }

  /**
   * Dead-ends a box span's tether on an anchor placed below one of its own anchors.
   *
   * <p>The mirror of {@link #onTetherAnchorRemoved}, and it exists because the two were not
   * symmetric: breaking a lower anchor dropped the tether back to its derived height immediately,
   * but placing one did nothing at all until the span was re-strung. Anchors are how a builder
   * says where the tether goes, so a span that ignores them until it is torn down and rebuilt is
   * a span that looks broken.
   *
   * <p>Shares the both-or-neither rule with {@link #attachTetherAnchors}: this re-reads both ends
   * rather than attaching the one just placed, so the first of a pair still changes nothing and
   * the second brings both in at once.
   */
  public static void onTetherAnchorPlaced(World world, BlockPos placed) {
    if (world == null || world.isRemote) {
      return;
    }
    for (int rise = 1; rise <= MAX_TETHER_ANCHOR_DROP; rise++) {
      final BlockPos above = placed.up(rise);
      if (!world.isBlockLoaded(above)) {
        return;
      }
      final SpanWireDefinition span = getSpanAt(world, above);
      // Only a box span has a tether to dead-end, and only one not already dead-ended on this
      // very block has anything to learn from it.
      if (span == null || !span.isBoxSpan()) {
        continue;
      }
      final SpanWireDefinition attached = attachTetherAnchors(world, span);
      if (!attached.equals(span)) {
        apply(world, attached);
      }
      return;
    }
  }

  /** The nearest span wire anchor directly below this one, within reach, or null. */
  @Nullable
  private static BlockPos findTetherAnchorBelow(World world, BlockPos anchor) {
    for (int drop = 1; drop <= MAX_TETHER_ANCHOR_DROP; drop++) {
      final BlockPos candidate = anchor.down(drop);
      if (!world.isBlockLoaded(candidate)) {
        return null;
      }
      if (world.getBlockState(candidate).getBlock() instanceof BlockSpanWireAnchor) {
        return candidate;
      }
    }
    return null;
  }

  private static double measureTetherClearance(World world, SpanWireDefinition span) {
    if (span.hasTetherAnchors()) {
      // The anchors decide the height; the derived clearance is the fallback for when they do not
      // exist, and re-measuring it here would only bake a number nothing reads.
      return span.getTetherClearance();
    }
    final SpanWireCatenary cable = span.solve();
    double deepest = 0.0;
    boolean found = false;

    for (BlockPos hanger : span.getHangers()) {
      // Every column this mount carries, not just the one under its own block. A cluster holds up
      // to four heads spread along its bracket and only one of them is under the mast, so reading
      // `hanger.down()` alone measured one head in four and let the other three decide nothing --
      // a deep head on an outer column had the tether strung straight through it.
      for (BlockPos payloadPos : payloadPositions(world, hanger)) {
        if (!world.isBlockLoaded(payloadPos)) {
          continue;
        }
        final IBlockState below = world.getBlockState(payloadPos);
        if (!(below.getBlock() instanceof ISpanWireHangable)) {
          continue;
        }
        final double tieY = ((ISpanWireHangable) below.getBlock())
            .getSpanTetherTieY(world, payloadPos, below);
        if (Double.isNaN(tieY)) {
          continue;
        }
        // Sampled under the payload rather than under the mast, for the same reason the tie
        // itself is: on a diagonal cluster a head sits a column away from the block the mount
        // occupies, where the cable is a different height.
        final double cableY = cable.heightAt(
            cable.parameterAt(payloadPos.getX() + 0.5, payloadPos.getZ() + 0.5));
        deepest = Math.max(deepest, cableY - tieY);
        found = true;
      }
    }

    return found ? deepest + TETHER_TIE_GAP : SpanWireDefinition.TETHER_MIN_CLEARANCE;
  }

  /**
   * How far under the deepest payload the tether is strung, so the ties up to it read as ties
   * rather than as the wire grazing the housings.
   */
  private static final double TETHER_TIE_GAP = 0.35;

  /**
   * Every block position a mount carries something at.
   *
   * <p>One for an ordinary mount, up to four for a cluster. Asked of the cluster rather than
   * re-derived from its width here, so the measurement can never disagree with the bracket that
   * is actually drawn.
   */
  private static List<BlockPos> payloadPositions(World world, BlockPos hanger) {
    final TileEntity mount = world.getTileEntity(hanger);
    if (!(mount instanceof TileEntitySpanWireClusterMount)) {
      return Collections.singletonList(hanger.down());
    }
    final List<BlockPos> columns = ((TileEntitySpanWireClusterMount) mount).getCoveredColumns();
    if (columns.isEmpty()) {
      return Collections.singletonList(hanger.down());
    }
    final List<BlockPos> positions = new ArrayList<>(columns.size());
    for (BlockPos column : columns) {
      positions.add(new BlockPos(column.getX(), hanger.getY() - 1, column.getZ()));
    }
    return positions;
  }

  /**
   * Where this span should sit relative to the block centre line, measured from what hangs on it.
   *
   * <p>Each payload is asked where its own hardware wants to be met
   * ({@link ISpanWireHangable#getSpanHardwareOffset}) and the <b>whole</b> displacement is taken,
   * not just the part across the span. That distinction is the difference between a wire that
   * passes over its housings and one that only manages it where the span happens to run along an
   * axis: a head is set back along the way it <em>faces</em>, which on a diagonal is not
   * perpendicular to the wire. Taking only the perpendicular part left the wire beside the
   * housings on every diagonal, and everything hanging off it leaning to make up the difference.
   *
   * <p>The <b>most common</b> answer wins rather than an average. These are a handful of discrete
   * directions, so averaging two of them yields a direction no payload actually asked for --
   * halfway between and right for neither. A span carrying four signals and one sign runs over
   * the signals.
   */
  private static Vec3d measureAutoOffset(World world, SpanWireDefinition span) {
    final Map<String, Vec3d> byKey = new LinkedHashMap<>();
    final Map<String, Integer> counts = new LinkedHashMap<>();

    for (BlockPos hanger : span.getHangers()) {
      final BlockPos payloadPos = hanger.down();
      if (!world.isBlockLoaded(payloadPos)) {
        continue;
      }
      final IBlockState below = world.getBlockState(payloadPos);
      if (!(below.getBlock() instanceof ISpanWireHangable)) {
        continue;
      }
      final Vec3d offset = ((ISpanWireHangable) below.getBlock())
          .getSpanHardwareOffset(world, payloadPos, below);
      // Quantised into a key so near-identical answers group together instead of each counting as
      // its own direction.
      final String key = Math.round(offset.x * 64.0) + ":" + Math.round(offset.z * 64.0);
      byKey.putIfAbsent(key, offset);
      counts.merge(key, 1, Integer::sum);
    }

    String best = null;
    int bestCount = 0;
    for (Map.Entry<String, Integer> entry : counts.entrySet()) {
      if (entry.getValue() > bestCount) {
        bestCount = entry.getValue();
        best = entry.getKey();
      }
    }
    return best == null ? Vec3d.ZERO : byKey.get(best);
  }

  /**
   * Finds the hanger mounts standing under the line between two anchors.
   *
   * <p>Walks the chord itself rather than stepping along an axis, so a skewed span works the same
   * as a straight one. The alternative -- testing every column in the bounding rectangle against
   * the line -- would be up to four thousand columns on a long diagonal before the height scan
   * even starts, against roughly two hundred and fifty here, and would not be any more correct:
   * a mount is on the wire if the wire passes through its column, which is exactly what walking
   * the line tells you.
   *
   * <p>Mounts are looked for over a height window running from just above the anchors down past
   * the cable's expected sag. The window matters: a builder following the cable places midspan
   * mounts a block or more below the anchors, and a search level with the anchors would miss
   * exactly the mounts a wide span most needs.
   *
   * <p>Where a column holds more than one mount the highest wins. Two mounts stacked in one
   * column is a build error, not an arrangement worth guessing at.
   */
  public static List<BlockPos> discoverHangers(World world, BlockPos anchorA, BlockPos anchorB,
      double expectedSag) {
    final List<BlockPos> found = new ArrayList<>();

    final double ax = anchorA.getX() + 0.5;
    final double az = anchorA.getZ() + 0.5;
    final double dx = (anchorB.getX() + 0.5) - ax;
    final double dz = (anchorB.getZ() + 0.5) - az;
    final double length = Math.sqrt(dx * dx + dz * dz);
    if (length < 1.0e-6) {
      return found;
    }

    final int searchTop = Math.max(anchorA.getY(), anchorB.getY()) + HANGER_SEARCH_ABOVE;
    final int searchBottom = Math.min(anchorA.getY(), anchorB.getY())
        - (int) Math.ceil(expectedSag) - HANGER_SEARCH_MARGIN;

    // Sub-block steps so no column the line clips through is stepped over. A diagonal enters some
    // columns for only a fraction of a block.
    final int steps = (int) Math.ceil(length / COLUMN_WALK_STEP);
    final Set<Long> visited = new HashSet<>();

    for (int i = 0; i <= steps; i++) {
      final double t = i / (double) steps;
      final int x = (int) Math.floor(ax + dx * t);
      final int z = (int) Math.floor(az + dz * t);

      // One column can be entered more than once on a diagonal; scan it once.
      if (!visited.add((((long) x) << 32) ^ (z & 0xFFFFFFFFL))) {
        continue;
      }
      // The anchors are attachments in their own right, not hangers.
      if ((x == anchorA.getX() && z == anchorA.getZ())
          || (x == anchorB.getX() && z == anchorB.getZ())) {
        continue;
      }

      for (int y = searchTop; y >= searchBottom; y--) {
        final BlockPos candidate = new BlockPos(x, y, z);
        if (!world.isBlockLoaded(candidate)) {
          continue;
        }
        if (world.getTileEntity(candidate) instanceof TileEntitySpanWireHanger) {
          found.add(candidate);
          break;
        }
      }
    }

    // Ordered by where each mount projects onto the chord, not by the order the walk happened to
    // reach them. On a diagonal those differ, and the order is what decides which piece of cable
    // each attachment draws.
    found.sort(Comparator.comparingDouble(
        pos -> ((pos.getX() + 0.5 - ax) * dx + (pos.getZ() + 0.5 - az) * dz) / (length * length)));
    return found;
  }

  /** Writes a span to every block taking part in it. Positions that no longer hold an attachment are skipped. */
  public static void apply(World world, SpanWireDefinition span) {
    for (BlockPos pos : span.getAttachments()) {
      final ISpanWireAttachment attachment = getAttachment(world, pos);
      if (attachment != null) {
        attachment.setSpan(span);
      }
    }
  }

  /** Clears a span from every block taking part in it. */
  public static void teardown(World world, SpanWireDefinition span) {
    for (BlockPos pos : span.getAttachments()) {
      final ISpanWireAttachment attachment = getAttachment(world, pos);
      if (attachment != null) {
        attachment.setSpan(null);
      }
    }
  }

  /**
   * Handles an anchor or hanger being broken.
   *
   * <p>Losing an anchor dissolves the span -- cable cannot terminate on nothing. Losing a hanger
   * only removes that hanger, and the cable closes over the gap, because a builder rearranging
   * one mount on a long span should not have to re-string the whole thing.
   *
   * <p>Call this before the tile entity is removed, while it can still report which span it
   * belonged to.
   */
  public static void onAttachmentRemoved(World world, BlockPos pos) {
    if (world.isRemote) {
      return;
    }
    final ISpanWireAttachment attachment = getAttachment(world, pos);
    if (attachment == null) {
      return;
    }
    final SpanWireDefinition span = attachment.getSpan();
    if (span == null) {
      return;
    }

    if (span.isAnchor(pos)) {
      teardown(world, span);
      return;
    }

    // A hanger: drop it and re-apply. The removed block's own copy goes with the block, so it
    // is cleared first and deliberately left out of the re-application below.
    attachment.setSpan(null);
    final SpanWireDefinition reduced = span.withoutHanger(pos);
    apply(world, reduced);
  }

  /**
   * Changes which side of a span its signal housings are on, and writes the new span to every
   * block taking part.
   *
   * <p>Set from one mount but applied to the whole span, because the tether is one wire: a
   * per-mount answer would let it zigzag between them.
   */
  public static void cycleSignalSide(World world, BlockPos anyAttachment) {
    final SpanWireDefinition span = getSpanAt(world, anyAttachment);
    if (span == null) {
      return;
    }
    apply(world, span.withSignalSide(span.getSignalSide().getNext()));
  }

  /**
   * Adds or removes the lower tether on the whole span this attachment belongs to.
   *
   * <p>Span-wide for the same reason the signal side is: the tether is a single run, so a
   * per-mount answer would be a wire that exists along part of its own length.
   */
  public static void toggleBoxSpan(World world, BlockPos anyAttachment) {
    final SpanWireDefinition span = getSpanAt(world, anyAttachment);
    if (span == null) {
      return;
    }
    SpanWireDefinition toggled = span.withBoxSpan(!span.isBoxSpan());
    // Measured here as well as at link time, because a span strung without a tether never ran the
    // measurement and would otherwise get the fallback clearance the moment one is switched on.
    if (toggled.isBoxSpan()) {
      toggled = attachTetherAnchors(world, toggled);
      toggled = toggled.withTetherClearance(measureTetherClearance(world, toggled));
    }
    apply(world, toggled);
  }

  /**
   * How far the measured clearance must move before the span is rewritten, in blocks.
   *
   * <p>Only there to stop floating point noise from republishing an identical span; any real
   * change is a section of signal, which is a great deal more than this.
   */
  private static final double TETHER_REMEASURE_EPSILON = 1.0e-3;

  /**
   * Re-measures a box span's tether after something changed how far a payload reaches down.
   *
   * <p>Called by the payload, never by the span -- a head knows when a section is added or taken
   * off it, and a mount two blocks above never hears about it, because the block below a block
   * below is not a neighbour. That gap is what made adding an add-on to a finished span require
   * re-stringing it: the clearance had been measured once, against a shorter assembly, and nothing
   * ever asked again.
   *
   * <p>Does nothing where there is nothing to do -- off a span, off a box span, or on a span whose
   * tether dead-ends on its own anchors and so takes its height from them rather than from what
   * hangs on it.
   */
  public static void onPayloadDepthChanged(World world, BlockPos payloadPos) {
    if (world == null || world.isRemote) {
      return;
    }
    final TileEntitySpanWireHanger mount = SpanWireHangOffset.findMount(world, payloadPos);
    if (mount == null) {
      return;
    }
    final SpanWireDefinition span = mount.getSpan();
    if (span == null || !span.isBoxSpan() || span.hasTetherAnchors()) {
      return;
    }
    final double measured = measureTetherClearance(world, span);
    if (Math.abs(measured - span.getTetherClearance()) < TETHER_REMEASURE_EPSILON) {
      return;
    }
    apply(world, span.withTetherClearance(measured));
  }

  /** The span the block at this position belongs to, or null. */
  @Nullable
  public static SpanWireDefinition getSpanAt(World world, BlockPos pos) {
    final ISpanWireAttachment attachment = getAttachment(world, pos);
    return attachment != null ? attachment.getSpan() : null;
  }

  /** Detaches whatever span this position already belonged to, if any. */
  public static void detachExistingSpan(World world, BlockPos pos) {
    final SpanWireDefinition existing = getSpanAt(world, pos);
    if (existing != null) {
      teardown(world, existing);
    }
  }

  @Nullable
  private static ISpanWireAttachment getAttachment(World world, BlockPos pos) {
    if (!world.isBlockLoaded(pos)) {
      return null;
    }
    final TileEntity tileEntity = world.getTileEntity(pos);
    return tileEntity instanceof ISpanWireAttachment ? (ISpanWireAttachment) tileEntity : null;
  }

  private static boolean isAnchor(World world, BlockPos pos) {
    return world.isBlockLoaded(pos) && world.getTileEntity(pos) instanceof TileEntitySpanWireAnchor;
  }

  /**
   * The chat summary shown after a successful link. Reports the sag in blocks, because that is
   * the number that decides where the builder has to put their mounts, and calls out any mount
   * the cable misses by more than {@link #MAX_DROOP}.
   */
  /**
   * The longest hardware run allowed at one mount, which depends on how that mount is set up: an
   * extending mast is chosen precisely because it is going to be long, so it is allowed further
   * than a flush mount, which drags its signal with it.
   */
  private static double maximumDropFor(World world, BlockPos hanger) {
    final TileEntity te = world.isBlockLoaded(hanger) ? world.getTileEntity(hanger) : null;
    if (te instanceof TileEntitySpanWireHanger) {
      return ((TileEntitySpanWireHanger) te).getMountStyle().getMaximumDrop();
    }
    return MAX_HANGER_DROP;
  }

  private static String buildSummary(World world, SpanWireDefinition span, double expectedSag) {
    // Two different mistakes needing opposite corrections, so they are counted apart.
    final List<BlockPos> aboveCable = new ArrayList<>();
    final List<BlockPos> tooFarBelow = new ArrayList<>();
    for (BlockPos hanger : span.getHangers()) {
      final double drop = span.cableDropAt(hanger);
      if (drop < 0.0) {
        aboveCable.add(hanger);
      } else if (drop > maximumDropFor(world, hanger)) {
        tooFarBelow.add(hanger);
      }
    }

    final StringBuilder message = new StringBuilder();
    message.append(span.isBoxSpan() ? "Box span strung: " : "Span strung: ")
        .append(span.getHangers().size())
        .append(span.getHangers().size() == 1 ? " hanger, " : " hangers, ")
        .append(String.format("%.2f", expectedSag))
        .append(" block sag at midspan.");

    if (!aboveCable.isEmpty()) {
      message.append(" Cable passes below ")
          .append(describeCount(aboveCable, "mount"))
          .append(" — nothing can hang upward, so lower ")
          .append(aboveCable.size() == 1 ? "it" : "them")
          .append(": ")
          .append(listPositions(aboveCable))
          .append(".");
    }
    if (!tooFarBelow.isEmpty()) {
      message.append(" ")
          .append(describeCount(tooFarBelow, "mount"))
          .append(" further below the cable than their mount style reaches; raise ")
          .append(tooFarBelow.size() == 1 ? "it" : "them")
          .append(": ")
          .append(listPositions(tooFarBelow))
          .append(".");
    }
    return message.toString();
  }

  private static String describeCount(List<BlockPos> positions, String noun) {
    return positions.size() + " " + noun + (positions.size() == 1 ? "" : "s");
  }

  /** Names up to three positions, so a badly built span does not print a wall of coordinates. */
  private static String listPositions(List<BlockPos> positions) {
    final StringBuilder text = new StringBuilder();
    for (int i = 0; i < positions.size() && i < 3; i++) {
      final BlockPos pos = positions.get(i);
      if (i > 0) {
        text.append(", ");
      }
      text.append("(").append(pos.getX()).append(",").append(pos.getY()).append(",")
          .append(pos.getZ()).append(")");
    }
    if (positions.size() > 3) {
      text.append(" and ").append(positions.size() - 3).append(" more");
    }
    return text.toString();
  }
}

package com.micatechnologies.minecraft.csm.trafficsignals.logic;

import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;
import com.micatechnologies.minecraft.csm.codeutils.DirectionSixteen;
import com.micatechnologies.minecraft.csm.codeutils.ICsmTileEntityProvider;
import com.micatechnologies.minecraft.csm.trafficaccessories.spanwire.ISpanWireHangable;
import com.micatechnologies.minecraft.csm.trafficsignals.TileEntityTrafficSignalHead;
import com.micatechnologies.minecraft.csm.trafficsignals.TileEntityTrafficSignalHeadRenderer;
import javax.annotation.Nullable;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import org.jetbrains.annotations.NotNull;

public abstract class AbstractBlockControllableSignalHead extends AbstractBlockControllableSignal
    implements ICsmTileEntityProvider, ISpanWireHangable {

  /**
   * Brings a span's drop back onto the roof of the housing instead of onto a visor.
   *
   * <p>A signal is not centred in its block: the body sits at the back by
   * {@link TrafficSignalBoundingBoxHelper#BODY_CENTRE_SETBACK} and the visors hang off the front
   * of it. So hardware coming straight down the block's centre line meets the top visor, which
   * is both wrong and conspicuously wrong. Backwards is the opposite of the way the head faces.
   */
  @Override
  public net.minecraft.util.math.Vec3d getSpanHardwareOffset(
      net.minecraft.world.IBlockAccess world, BlockPos pos, IBlockState state) {
    final EnumFacing back = state.getValue(FACING).getOpposite();
    return new net.minecraft.util.math.Vec3d(back.getXOffset(), 0.0, back.getZOffset())
        .scale(TrafficSignalBoundingBoxHelper.BODY_CENTRE_SETBACK);
  }

  /**
   * Shared "no pivot offset" result for {@link #getTiltPivotOffset(IBlockAccess, BlockPos)}.
   *
   * <p><b>Read-only.</b> The section-layout accessors on this class have always been free to
   * return an internally held array -- {@code BlockControllableSignal} returns its own configured
   * arrays directly -- so callers already must not write into what they get back. These caches
   * lean on that same contract to stop re-deriving constant layouts on every frame: the renderer,
   * the traffic light cover and mount kit, and the bounding box helper all call these per frame,
   * per visible signal.</p>
   */
  private static final int[] NO_TILT_PIVOT = new int[]{0, 0, 0};

  /** Default vertical stack layouts, memoised by section count. Read-only; see NO_TILT_PIVOT. */
  private static final Map<Integer, float[]> DEFAULT_Y_POSITIONS = new ConcurrentHashMap<>();

  /** Default (all-zero) X offsets, memoised by section count. Read-only; see NO_TILT_PIVOT. */
  private static final Map<Integer, float[]> DEFAULT_X_POSITIONS = new ConcurrentHashMap<>();

  /** Default (all 12-inch) section sizes, memoised by section count. Read-only. */
  private static final Map<Integer, int[]> DEFAULT_SIZES = new ConcurrentHashMap<>();

  public AbstractBlockControllableSignalHead(Material p_i45394_1_) {
    super(p_i45394_1_);
  }

  /**
   * Where a box span's tether ties to this head: the bottom of the housing as actually drawn.
   *
   * <p>Taken from the bounding box rather than from a constant, so it already accounts for the
   * section count, the section sizes and the rise the span has given this head. A tether tie of
   * fixed length cannot do that -- the heads on a sagging span sit at different heights relative
   * to a taut tether, so a fixed stub overshoots into the lenses at one end of the span and falls
   * short at the other.
   */
  /** Drawn by its own renderer, so it can and does take the span's sub-block rise. */
  @Override
  public boolean takesSpanRise() {
    return true;
  }

  @Override
  public double getSpanTetherTieY(IBlockAccess world, BlockPos pos, IBlockState state) {
    return pos.getY() + unrisenBoundingBox(world, pos).minY;
  }

  /**
   * This head's shape <b>before</b> any span wire rise is applied.
   *
   * <p>Both span hooks report against this rather than against the live box, and the mount adds
   * the rise it is giving. That is not a tidiness choice, it fixes an ordering bug: the live box
   * asks the head's tile entity for its span offset, which is cached behind a refresh interval and
   * is still zero at the moment a span is strung -- so hardware measured against it was built
   * against an un-risen head and then never corrected, leaving the tether ties visibly short.
   *
   * <p>Taking the base and adding the rise makes the answer depend only on things known
   * synchronously, so there is no moment at which it can be read too early.
   */
  private AxisAlignedBB unrisenBoundingBox(IBlockAccess world, BlockPos pos) {
    return TrafficSignalBoundingBoxHelper.computeBoundingBox(this, world, pos,
        getBaseSignalYOffset(world, pos));
  }

  /**
   * Where a span's drop should come down to: the roof of the housing as actually drawn.
   *
   * <p>Matters for the extending-mast mount style, and only for that one. A flush mount lifts the
   * head until it meets the hardware, so the drop has nothing to reach for; an extending mast
   * deliberately leaves the head on its own block, and the drop then stopped at the mount's attach
   * height three quarters of the way up the block above -- a quarter of a block clear of the roof
   * it was supposed to be holding.
   *
   * <p>Read from the bounding box, so it follows the section count, the section sizes and whatever
   * rise the head has taken, rather than assuming any of them.
   */
  @Override
  public double getSpanHangerTopY(IBlockAccess world, BlockPos pos, IBlockState state) {
    return pos.getY() + unrisenBoundingBox(world, pos).maxY;
  }

  @Override
  public AxisAlignedBB getBlockBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
    // Recomputed per-call: signals can flip between vertical and horizontal layout via
    // their TE, and a single per-block-class cache cannot reflect per-position state.
    final AxisAlignedBB box = TrafficSignalBoundingBoxHelper.computeBoundingBox(this, source, pos);
    if (source == null || pos == null || !state.getProperties().containsKey(FACING)) {
      return box;
    }
    // The box follows the model sideways as well as up. A head slid under a span's clamp that kept
    // its hitbox on the block would be clickable where it is not drawn and solid where it is.
    final Vec3d slide = getBoundingBoxSlide(source, pos, state.getValue(FACING));
    return slide == Vec3d.ZERO ? box : box.offset(slide.x, 0.0, slide.z);
  }

  /**
   * Uses a clamped AABB (0-1 per axis) for raytrace/click targeting so that the oversized
   * visual selection box doesn't steal clicks from adjacent blocks (e.g., placing add-on
   * signals below a 3-section signal). The full AABB from {@link #getBoundingBox} is still
   * used for the visual selection outline.
   */
  @Nullable
  @Override
  public RayTraceResult collisionRayTrace(IBlockState state, World worldIn, BlockPos pos,
      Vec3d start, Vec3d end) {
    AxisAlignedBB bb = getBoundingBox(state, worldIn, pos);
    AxisAlignedBB clamped = new AxisAlignedBB(
        Math.max(0.0, bb.minX), Math.max(0.0, bb.minY), Math.max(0.0, bb.minZ),
        Math.min(1.0, bb.maxX), Math.min(1.0, bb.maxY), Math.min(1.0, bb.maxZ));
    return rayTrace(pos, start, end, clamped);
  }

  public DirectionSixteen getTiltedFacing(
      @NotNull IBlockAccess worldIn,
      @NotNull BlockPos pos, EnumFacing facing4) {
    TileEntity tileEntity = worldIn.getTileEntity(pos);
    if (tileEntity instanceof TileEntityTrafficSignalHead trafficSignalHead) {
      TrafficSignalBodyTilt stateBodyTilt = trafficSignalHead.getBodyTilt();
      return getTiltedFacing(stateBodyTilt, facing4);
    }
    return null;
  }

  public static DirectionSixteen getTiltedFacing(TrafficSignalBodyTilt stateBodyTilt, EnumFacing facing4) {
    DirectionSixteen stateTiltedFacing;
    if (facing4 == EnumFacing.NORTH) {
      // Rotate the signal head based on the body tilt
      if (stateBodyTilt == TrafficSignalBodyTilt.LEFT_ANGLE) {
        stateTiltedFacing = DirectionSixteen.NE;
      } else if (stateBodyTilt == TrafficSignalBodyTilt.RIGHT_ANGLE) {
        stateTiltedFacing = DirectionSixteen.NW;
      } else if (stateBodyTilt == TrafficSignalBodyTilt.LEFT_TILT) {
        stateTiltedFacing = DirectionSixteen.NNE;
      } else if (stateBodyTilt == TrafficSignalBodyTilt.RIGHT_TILT) {
        stateTiltedFacing = DirectionSixteen.NNW;
      } else {
        stateTiltedFacing = DirectionSixteen.N;
      }
    } else if (facing4 == EnumFacing.EAST) {
      // Rotate the signal head based on the body tilt
      if (stateBodyTilt == TrafficSignalBodyTilt.LEFT_ANGLE) {
        stateTiltedFacing = DirectionSixteen.SE;
      } else if (stateBodyTilt == TrafficSignalBodyTilt.RIGHT_ANGLE) {
        stateTiltedFacing = DirectionSixteen.NE;
      } else if (stateBodyTilt == TrafficSignalBodyTilt.LEFT_TILT) {
        stateTiltedFacing = DirectionSixteen.ESE;
      } else if (stateBodyTilt == TrafficSignalBodyTilt.RIGHT_TILT) {
        stateTiltedFacing = DirectionSixteen.ENE;
      } else {
        stateTiltedFacing = DirectionSixteen.E;
      }
    } else if (facing4 == EnumFacing.SOUTH) {
      // Rotate the signal head based on the body tilt
      if (stateBodyTilt == TrafficSignalBodyTilt.LEFT_ANGLE) {
        stateTiltedFacing = DirectionSixteen.SW;
      } else if (stateBodyTilt == TrafficSignalBodyTilt.RIGHT_ANGLE) {
        stateTiltedFacing = DirectionSixteen.SE;
      } else if (stateBodyTilt == TrafficSignalBodyTilt.LEFT_TILT) {
        stateTiltedFacing = DirectionSixteen.SSW;
      } else if (stateBodyTilt == TrafficSignalBodyTilt.RIGHT_TILT) {
        stateTiltedFacing = DirectionSixteen.SSE;
      } else {
        stateTiltedFacing = DirectionSixteen.S;
      }
    } else {
      // Rotate the signal head based on the body tilt
      if (stateBodyTilt == TrafficSignalBodyTilt.LEFT_ANGLE) {
        stateTiltedFacing = DirectionSixteen.NW;
      } else if (stateBodyTilt == TrafficSignalBodyTilt.RIGHT_ANGLE) {
        stateTiltedFacing = DirectionSixteen.SW;
      } else if (stateBodyTilt == TrafficSignalBodyTilt.LEFT_TILT) {
        stateTiltedFacing = DirectionSixteen.WNW;
      } else if (stateBodyTilt == TrafficSignalBodyTilt.RIGHT_TILT) {
        stateTiltedFacing = DirectionSixteen.WSW;
      } else {
        stateTiltedFacing = DirectionSixteen.W;
      }
    }

    return stateTiltedFacing;
  }

  /**
   * Gets the tile entity class for the block.
   *
   * @return the tile entity class for the block
   *
   * @since 1.0
   */
  @Override
  public Class<? extends TileEntity> getTileEntityClass() {
    return TileEntityTrafficSignalHead.class;
  }

  /**
   * Gets the tile entity name for the block.
   *
   * @return the tile entity name for the block
   *
   * @since 1.0
   */
  @Override
  public String getTileEntityName() {
    return "tileentitytrafficsignalhead";
  }


  /**
   * Gets a new tile entity for the block.
   *
   * @param worldIn the world
   * @param meta    the block metadata
   *
   * @return the new tile entity for the block
   *
   * @since 1.1
   */
  @Nullable
  @Override
  public TileEntity createNewTileEntity(World worldIn, int meta) {
    return new TileEntityTrafficSignalHead(getDefaultTrafficSignalSectionInfo());
  }

  public abstract TrafficSignalSectionInfo[] getDefaultTrafficSignalSectionInfo();

  /**
   * Returns whether the given blockstate color value (0=red, 1=yellow, 2=green, 3=off)
   * should light up sections with the given bulb color. The default maps color=0 to RED,
   * color=1 to YELLOW, color=2 to GREEN, and color=3 to nothing.
   *
   * Override for signals that respond to multiple color states, such as single-section
   * flasher signals that light on both color=0 (red phase) and color=1 (yellow phase)
   * so they work with the controller's flash mode.
   */
  public boolean shouldLightBulb(int colorState, TrafficSignalBulbColor bulbColor) {
    if (colorState == 0 && bulbColor == TrafficSignalBulbColor.RED) return true;
    if (colorState == 1 && bulbColor == TrafficSignalBulbColor.YELLOW) return true;
    if (colorState == 2 && bulbColor == TrafficSignalBulbColor.GREEN) return true;
    return false;
  }

  /**
   * Returns whether the given blockstate color value (0=red, 1=yellow, 2=green, 3=off)
   * should light up ALL sections regardless of their bulb color. Default is false.
   * Override to true for single-section flasher signals that should light on multiple
   * controller color states.
   */
  public boolean shouldLightAllSections(int colorState) {
    return false;
  }

  /**
   * Returns the Y offset (in model units) to shift the entire signal rendering.
   * Override in subclasses whose JSON model positions the signal body at a different
   * Y origin than the standard 3-section vertical (which uses Y=0 as baseline).
   * For example, single-section signals need +2 to match their model's Y=2-14 range.
   */
  public float getSignalYOffset() {
    return 0.0f;
  }

  /**
   * Returns per-section Y offsets for the renderer. Default uses the standard vertical
   * stack formula (evenly spaced 12 units apart, centered). Override for non-standard
   * layouts such as add-on signals where multiple sections overlap at the same position.
   */
  public float[] getSectionYPositions(int sectionCount) {
    float[] cached = DEFAULT_Y_POSITIONS.get(sectionCount);
    if (cached != null) {
      return cached;
    }
    float[] positions = new float[sectionCount];
    for (int i = 0; i < sectionCount; i++) {
      positions[i] = ((sectionCount - 1 - i) - (sectionCount - 1) / 2.0f) * 12.0f;
    }
    DEFAULT_Y_POSITIONS.put(sectionCount, positions);
    return positions;
  }

  /**
   * Returns whether this signal uses horizontal body orientation. When true, the renderer
   * uses body/door vertex data rotated 90° so the back taper goes top-to-bottom instead
   * of left-to-right. Visors and bulb textures are NOT rotated.
   */
  public boolean isHorizontal() {
    return false;
  }

  /**
   * Returns whether this signal's orientation can be toggled between vertical and horizontal
   * via the signal-head config GUI. Default is {@code true}; specialty signals (doghouse,
   * hawk, and anything whose geometry doesn't round-trip through a 90° rotation cleanly)
   * should override to {@code false}. Static-horizontal blocks also return {@code false}
   * since there's nothing to toggle — they're already horizontal.
   */
  public boolean allowsHorizontalFlip() {
    return !isHorizontal();
  }

  /**
   * Returns per-section sizes (12 or 8) for the renderer. Default is 12 for all sections.
   * Override for 8-inch signals or mixed-size (8-8-12, 12-8-8) signals.
   */
  public int[] getSectionSizes(int sectionCount) {
    int[] cached = DEFAULT_SIZES.get(sectionCount);
    if (cached != null) {
      return cached;
    }
    int[] sizes = new int[sectionCount];
    Arrays.fill(sizes, 12);
    DEFAULT_SIZES.put(sectionCount, sizes);
    return sizes;
  }

  /**
   * Returns per-section X offsets for the renderer. Default is 0 for all sections
   * (straight vertical stack). Override for doghouse signals where lower sections
   * are shifted left or right relative to the top section.
   */
  public float[] getSectionXPositions(int sectionCount) {
    float[] cached = DEFAULT_X_POSITIONS.get(sectionCount);
    if (cached != null) {
      return cached;
    }
    float[] positions = new float[sectionCount]; // all zeros
    DEFAULT_X_POSITIONS.put(sectionCount, positions);
    return positions;
  }

  // --- World-aware layout overloads ---
  // These allow add-on signals to detect adjacent horizontal signals and adapt their
  // layout at render time. Default implementations delegate to the static methods.
  // The renderer calls these instead of the static versions.

  /**
   * World-aware version of {@link #isHorizontal()}. Override to detect adjacent signal
   * orientation dynamically.
   */
  public boolean isHorizontal(IBlockAccess world, BlockPos pos) {
    return isHorizontal();
  }

  /**
   * World-aware version of {@link #getSectionYPositions(int)}.
   */
  public float[] getSectionYPositions(int sectionCount, IBlockAccess world, BlockPos pos) {
    return getSectionYPositions(sectionCount);
  }

  /**
   * World-aware version of {@link #getSectionXPositions(int)}.
   */
  public float[] getSectionXPositions(int sectionCount, IBlockAccess world, BlockPos pos) {
    return getSectionXPositions(sectionCount);
  }

  /**
   * World-aware version of {@link #getSignalYOffset()}: everything that shifts this signal
   * vertically, added together.
   *
   * <p>Final on purpose. The span wire term has to reach every caller — the renderer, the
   * bounding box helper, and the cover and mount-kit geometry that fit themselves around a
   * signal — and a subclass that overrode this and forgot to add it would silently draw a
   * hanging signal in the wrong place, with a hitbox somewhere else again. Subclasses adjust
   * their own contribution through {@link #getBaseSignalYOffset} instead.
   */
  public final float getSignalYOffset(IBlockAccess world, BlockPos pos) {
    return (float) getSignalOffset(world, pos).y;
  }

  /**
   * Everything that shifts this signal away from its block, on all three axes, in model units.
   *
   * <p>The head's own contribution is vertical only -- a signal sits where its block is unless
   * something moved it -- so the horizontal terms come entirely from what it hangs from. A span
   * wire is the one thing that supplies them today, and it uses them to slide a head under its
   * clamp so the mast comes straight down instead of reaching across.
   *
   * <p>Final for the same reason the vertical form is: this has to reach the renderer, the hitbox,
   * and the cover and mount-kit geometry that fit themselves around a signal, and anything that
   * quietly dropped a term would draw a head in one place with a hitbox in another.
   *
   * @param world the block access.
   * @param pos   this block's position.
   *
   * @return the offset in model units, never null.
   */
  public final Vec3d getSignalOffset(IBlockAccess world, BlockPos pos) {
    final Vec3d moved = getHeadDisplacement(world, pos);
    return new Vec3d(moved.x, getBaseSignalYOffset(world, pos) + moved.y, moved.z);
  }

  /**
   * How far this head has moved off its own block: what a span did to it, plus any hand-placed
   * nudge. In model units.
   *
   * <p>Deliberately excludes {@link #getBaseSignalYOffset}, which is not movement at all -- it is
   * where this kind of head sits on its block in the first place, and anything drawn to match a
   * head already accounts for it. A backplate wants this one: it is authored against the head's
   * resting place and needs to follow only the part that moved.
   *
   * @param world the block access.
   * @param pos   this block's position.
   *
   * @return the displacement in model units and world axes, never null.
   */
  public final Vec3d getHeadDisplacement(IBlockAccess world, BlockPos pos) {
    final Vec3d span = getSpanWireOffset(world, pos);
    final Vec3d nudge = getNudgeOffset(world, pos);
    return new Vec3d(span.x + nudge.x, span.y, span.z + nudge.z);
  }

  /**
   * A head's hand-placed nudge, turned from its own forward/right terms into world axes.
   *
   * <p>Stored against the facing so that turning a head takes its nudge with it, which is what
   * anyone who placed one would expect; the renderer and the hitbox both want world axes, so the
   * conversion happens once, here.
   *
   * @param world the block access.
   * @param pos   this block's position.
   *
   * @return the nudge in world axes and model units, with a zero Y.
   */
  private Vec3d getNudgeOffset(IBlockAccess world, BlockPos pos) {
    if (world == null || pos == null) {
      return Vec3d.ZERO;
    }
    final TileEntity tileEntity = world.getTileEntity(pos);
    if (!(tileEntity instanceof TileEntityTrafficSignalHead)) {
      return Vec3d.ZERO;
    }
    final TileEntityTrafficSignalHead head = (TileEntityTrafficSignalHead) tileEntity;
    final int forward = head.getNudgeForward();
    final int side = head.getNudgeSide();
    if (forward == 0 && side == 0) {
      return Vec3d.ZERO;
    }
    final IBlockState state = world.getBlockState(pos);
    if (!state.getProperties().containsKey(FACING)) {
      return Vec3d.ZERO;
    }
    final EnumFacing facing = state.getValue(FACING);
    final EnumFacing right = facing.rotateY();
    return new Vec3d(
        forward * facing.getXOffset() + side * right.getXOffset(),
        0.0,
        forward * facing.getZOffset() + side * right.getZOffset());
  }

  /**
   * The horizontal part of {@link #getSignalOffset}, turned from world axes into the north-facing
   * frame the bounding box is built in.
   *
   * <p>The box is authored facing north and rotated by {@code RotationUtils} afterwards, so a shift
   * added to it in world axes would be rotated a second time and land on the wrong side of the
   * block. This inverts that rotation so the box ends up moved the way the model is.
   *
   * @param world  the block access.
   * @param pos    this block's position.
   * @param facing the block's facing.
   *
   * @return the shift to apply to a north-facing box, in blocks.
   */
  public final Vec3d getBoundingBoxSlide(IBlockAccess world, BlockPos pos, EnumFacing facing) {
    final Vec3d offset = getSignalOffset(world, pos);
    final double x = offset.x / 16.0;
    final double z = offset.z / 16.0;
    if (x == 0.0 && z == 0.0) {
      return Vec3d.ZERO;
    }
    switch (facing) {
      case SOUTH:
        return new Vec3d(-x, 0.0, -z);
      case EAST:
        return new Vec3d(z, 0.0, -x);
      case WEST:
        return new Vec3d(-z, 0.0, x);
      case NORTH:
      default:
        return new Vec3d(x, 0.0, z);
    }
  }

  /**
   * This signal's own vertical offset, before anything it hangs from is taken into account.
   * Override this rather than {@link #getSignalYOffset(IBlockAccess, BlockPos)}.
   */
  protected float getBaseSignalYOffset(IBlockAccess world, BlockPos pos) {
    return getSignalYOffset();
  }

  /**
   * How far this signal is shifted by hanging from a span wire, in model units, or zero when it
   * does not. Read through the tile entity, which caches it — the renderer asks for the total
   * offset every frame, and the underlying lookup walks blocks.
   */
  private Vec3d getSpanWireOffset(IBlockAccess world, BlockPos pos) {
    if (world == null || pos == null) {
      return Vec3d.ZERO;
    }
    final TileEntity tileEntity = world.getTileEntity(pos);
    if (tileEntity instanceof TileEntityTrafficSignalHead) {
      return ((TileEntityTrafficSignalHead) tileEntity).getSpanWireOffset();
    }
    return Vec3d.ZERO;
  }

  /**
   * Returns the block-space offset from this signal to the main signal that this add-on
   * should align its tilt rotation with. Default is (0,0,0) — no pivot compensation needed.
   * Horizontal add-on signals override this to return the offset to the main signal so
   * the renderer can apply a post-rotation correction to keep them aligned when tilted.
   *
   * @return int[3] array {dx, dy, dz} in block units from this block to the main signal
   */
  public int[] getTiltPivotOffset(IBlockAccess world, BlockPos pos) {
    return NO_TILT_PIVOT;
  }

  /**
   * Returns an enforced bulb style for this signal head, or null if the user may freely
   * choose any style. Override in subclasses that only work correctly with a specific
   * bulb style (e.g., bi-modal/hybrid arrows that require LED_DOTTED for a proper
   * color-independent off-state texture).
   */
  public TrafficSignalBulbStyle getEnforcedBulbStyle() {
    return null;
  }

  @Override
  public int getLightValue(IBlockState state, IBlockAccess world, BlockPos pos) {
    return 0;
  }

  /**
   * Ensures a tile entity exists for this block. Handles migration of blocks that existed
   * in the world before being converted to custom rendering (they were saved without a TE).
   * Called from neighborChanged and onBlockActivated so the TE is created automatically
   * when the signal controller cycles colors or the player interacts.
   */
  private void ensureTileEntity(World worldIn, BlockPos pos) {
    if (!worldIn.isRemote && worldIn.getTileEntity(pos) == null) {
      worldIn.setTileEntity(pos, createNewTileEntity(worldIn, 0));
    }
  }

  @Override
  public void neighborChanged(IBlockState state, World worldIn, BlockPos pos,
      net.minecraft.block.Block blockIn, BlockPos fromPos) {
    ensureTileEntity(worldIn, pos);
    // A head appearing or disappearing beside this one changes whether the mount bracket keeps
    // its end cap, and the renderer caches that answer rather than re-deriving it every frame.
    net.minecraft.tileentity.TileEntity tileEntity = worldIn.getTileEntity(pos);
    if (tileEntity instanceof TileEntityTrafficSignalHead) {
      ((TileEntityTrafficSignalHead) tileEntity).invalidateMountSuppression();
    }
    super.neighborChanged(state, worldIn, pos, blockIn, fromPos);
  }

  @Override
  public boolean onBlockActivated(World p_180639_1_, BlockPos p_180639_2_, IBlockState p_180639_3_,
      EntityPlayer p_180639_4_, EnumHand p_180639_5_, EnumFacing p_180639_6_, float p_180639_7_,
      float p_180639_8_, float p_180639_9_) {
    ensureTileEntity(p_180639_1_, p_180639_2_);
    return super.onBlockActivated(p_180639_1_, p_180639_2_, p_180639_3_, p_180639_4_, p_180639_5_,
        p_180639_6_, p_180639_7_, p_180639_8_, p_180639_9_);
  }

}
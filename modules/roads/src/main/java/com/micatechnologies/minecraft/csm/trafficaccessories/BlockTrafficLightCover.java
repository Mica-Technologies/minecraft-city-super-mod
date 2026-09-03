package com.micatechnologies.minecraft.csm.trafficaccessories;

import com.micatechnologies.minecraft.csm.codeutils.AbstractBlockRotatableNSEWUD;
import com.micatechnologies.minecraft.csm.codeutils.ICsmNoSnowAccumulation;
import com.micatechnologies.minecraft.csm.codeutils.ICsmTileEntityProvider;
import com.micatechnologies.minecraft.csm.trafficsignals.TileEntityBlankoutBox;
import com.micatechnologies.minecraft.csm.trafficsignals.TileEntityTrafficSignalHead;
import com.micatechnologies.minecraft.csm.trafficsignals.logic.AbstractBlockControllableSignalHead;
import com.micatechnologies.minecraft.csm.trafficsignals.logic.BlankoutBoxVertexData;
import java.util.Random;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import org.jetbrains.annotations.NotNull;

/**
 * Dynamic traffic signal cover block (registry name {@code tlvcover}). Uses a TESR to detect
 * the adjacent traffic signal head and render a rain hood/cover that automatically adapts to
 * the signal's orientation (vertical or horizontal), section count, section sizes, body tilt,
 * and any attached add-on signals. Replaces the old static vertical/horizontal cover pair —
 * the {@code tlhcover} block retires into this one.
 *
 * <p>The visual bounding box is computed dynamically to match the cover shell extent.
 * Raytrace/click targeting is clamped to the 0-1 block range so the selection box doesn't
 * steal clicks from adjacent blocks (same approach as the dynamic signal mount kit).
 */
public class BlockTrafficLightCover extends AbstractBlockRotatableNSEWUD
    implements ICsmTileEntityProvider, ICsmNoSnowAccumulation {

  // --- Cover shell constants (model units, 16 units = 1 block, NORTH-facing frame) ---
  /** Thickness of every cover panel. */
  static final float PANEL_THICKNESS = 1.0f;
  /** Front face of the cover's face plate (inside the cover's own block). */
  static final float FRONT_Z = 15.0f;
  /** Back edge of the wrap panels (extends into the signal's block). */
  static final float BACK_Z = 28.0f;

  // --- Default signal envelope when no signal is detected ---
  // Matches a classic 3-section 12-inch vertical signal (and the legacy static tlvcover model).
  static final float DEFAULT_ENV_MIN_X = 2.0f;
  static final float DEFAULT_ENV_MAX_X = 14.0f;
  static final float DEFAULT_ENV_MIN_Y = -12.0f;
  static final float DEFAULT_ENV_MAX_Y = 24.0f;

  // Model-space constants matching the signal head renderer
  private static final float CENTER_X = 8.0f;
  private static final float CENTER_Y = 6.0f;
  private static final int MAX_SCAN_DISTANCE = 3;

  public BlockTrafficLightCover() {
    super(Material.ROCK, SoundType.STONE, "pickaxe", 1, 2.0F, 10.0F, 0F, 0);
    // Random ticks drive one-time tile entity creation/sync for legacy (pre-TESR) placements
    setTickRandomly(true);
  }

  @Override
  public String getBlockRegistryName() {
    return "tlvcover";
  }

  /**
   * Computes a NORTH-facing bounding box that matches the actual cover shell extent based on
   * the adjacent signal. Caches the result on the tile entity for performance; the cache
   * is invalidated when a neighbor block changes.
   */
  @Override
  public AxisAlignedBB getBlockBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
    // Check cache first
    TileEntity te = source.getTileEntity(pos);
    if (te instanceof TileEntityTrafficLightCover) {
      AxisAlignedBB cached = ((TileEntityTrafficLightCover) te).getCachedBoundingBox();
      if (cached != null) return cached;
    }

    EnumFacing facing = EnumFacing.NORTH;
    if (state.getProperties().containsKey(FACING)) {
      facing = state.getValue(FACING);
    }

    CoverSignalScan scan = scanForSignal(source, pos, facing);

    float minX = scan.minX - PANEL_THICKNESS;
    float maxX = scan.maxX + PANEL_THICKNESS;
    float minY = scan.minY - PANEL_THICKNESS;
    float maxY = scan.maxY + PANEL_THICKNESS;
    float minZ = FRONT_Z;
    float maxZ = BACK_Z;

    if (scan.flipped) {
      // Signal sits on the cover's facing side, so the shell wraps the opposite direction.
      // Mirror X and Z around the block center (equivalent to the renderer's 180° rotation).
      float oldMinX = minX;
      minX = 16.0f - maxX;
      maxX = 16.0f - oldMinX;
      minZ = 16.0f - BACK_Z;
      maxZ = 16.0f - FRONT_Z;
    }

    AxisAlignedBB result = new AxisAlignedBB(
        minX / 16.0, minY / 16.0, minZ / 16.0,
        maxX / 16.0, maxY / 16.0, maxZ / 16.0);

    // Cache on the tile entity
    if (te instanceof TileEntityTrafficLightCover) {
      ((TileEntityTrafficLightCover) te).setCachedBoundingBox(result);
    }

    return result;
  }

  @Override
  public void neighborChanged(IBlockState state, World worldIn, BlockPos pos, Block blockIn,
      BlockPos fromPos) {
    super.neighborChanged(state, worldIn, pos, blockIn, fromPos);
    TileEntity te = worldIn.getTileEntity(pos);
    if (te instanceof TileEntityTrafficLightCover) {
      ((TileEntityTrafficLightCover) te).invalidateCachedBB();
    }
  }

  /**
   * Random ticks perform one-time legacy migration: covers placed before the TESR conversion
   * were saved without a tile entity, which would leave them invisible on clients. Touching
   * the tile entity here lazily creates it (server side), and a single update packet pushes
   * it to tracking clients so the TESR starts rendering without requiring a relog.
   */
  @Override
  public void randomTick(@NotNull World worldIn, @NotNull BlockPos pos, @NotNull IBlockState state,
      @NotNull Random random) {
    super.randomTick(worldIn, pos, state, random);
    if (!worldIn.isRemote) {
      TileEntity te = worldIn.getTileEntity(pos); // lazily creates for legacy placements
      if (te instanceof TileEntityTrafficLightCover
          && !((TileEntityTrafficLightCover) te).isLegacyClientSyncSent()) {
        ((TileEntityTrafficLightCover) te).setLegacyClientSyncSent();
        ((TileEntityTrafficLightCover) te).syncServerToClient(worldIn);
      }
    }
  }

  /**
   * Sneak + right-click cycles the cover's color scheme. Plain right-click is a no-op so
   * players can still open other blocks or place items near the cover without accidentally
   * repainting it. Handled server-side only; the TE syncs the new state down to the client
   * for re-rendering.
   */
  @Override
  public boolean onBlockActivated(World world, BlockPos pos, IBlockState state,
      EntityPlayer player, EnumHand hand, EnumFacing side,
      float hitX, float hitY, float hitZ) {
    if (!player.isSneaking()) {
      return false;
    }
    if (world.isRemote) {
      // Let the server authoritatively cycle; still return true so the client doesn't also
      // trigger an item-use action in the same tick.
      return true;
    }
    TileEntity te = world.getTileEntity(pos);
    if (!(te instanceof TileEntityTrafficLightCover)) {
      return false;
    }
    TileEntityTrafficLightCover cover = (TileEntityTrafficLightCover) te;
    MountKitColorScheme next = cover.cycleColorScheme();
    cover.markDirtySync(world, pos, true);
    player.sendMessage(new TextComponentString(
        TextFormatting.GRAY + "Cover color: " + TextFormatting.WHITE + next.getFriendlyName()));
    return true;
  }

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

  @Override
  public boolean getBlockIsOpaqueCube(IBlockState state) {
    return false;
  }

  @Override
  public boolean getBlockIsFullCube(IBlockState state) {
    return false;
  }

  @Override
  public boolean getBlockConnectsRedstone(IBlockState state, IBlockAccess access, BlockPos pos,
      @Nullable EnumFacing facing) {
    return false;
  }

  @Nonnull
  @Override
  public BlockRenderLayer getBlockRenderLayer() {
    return BlockRenderLayer.CUTOUT_MIPPED;
  }

  // --- ICsmTileEntityProvider ---

  @Override
  public Class<? extends TileEntity> getTileEntityClass() {
    return TileEntityTrafficLightCover.class;
  }

  @Override
  public String getTileEntityName() {
    return "tileentitytrafficlightcover";
  }

  @Override
  public TileEntity createNewTileEntity(World worldIn, int meta) {
    return new TileEntityTrafficLightCover();
  }

  // --- Signal detection (shared between bounding box and TESR) ---

  /**
   * Result of scanning for the signal a cover should wrap: the primary signal position (or
   * null), the facing the cover geometry should be rendered with, whether that facing is the
   * reverse of the blockstate facing, and the merged signal envelope in model units
   * (including add-on signals above/below the primary signal).
   */
  public static final class CoverSignalScan {

    /** Position of the primary signal head, or {@code null} if none was found. */
    @Nullable
    public final BlockPos signalPos;

    /** The facing the cover geometry should be rendered with (wrap extends opposite). */
    public final EnumFacing renderFacing;

    /** True when {@link #renderFacing} is the reverse of the cover's blockstate facing. */
    public final boolean flipped;

    /** Merged signal envelope in model units (signal-local frame). */
    public final float minX, maxX, minY, maxY;

    CoverSignalScan(@Nullable BlockPos signalPos, EnumFacing renderFacing, boolean flipped,
        float minX, float maxX, float minY, float maxY) {
      this.signalPos = signalPos;
      this.renderFacing = renderFacing;
      this.flipped = flipped;
      this.minX = minX;
      this.maxX = maxX;
      this.minY = minY;
      this.maxY = maxY;
    }
  }

  /**
   * Finds the signal head adjacent to a cover and merges its envelope with any add-on signals
   * stacked above or below it. Checks behind the cover first (one step opposite the facing,
   * matching normal placement in front of a signal), then in front as a fallback. Returns the
   * default 3-section vertical envelope when no signal is found or the cover faces up/down.
   */
  public static CoverSignalScan scanForSignal(IBlockAccess world, BlockPos pos,
      EnumFacing facing) {
    if (facing.getAxis() == EnumFacing.Axis.Y) {
      return defaultScan(facing);
    }

    BlockPos signalPos = null;
    EnumFacing renderFacing = facing;
    boolean flipped = false;
    if (isSignalHead(world, pos.offset(facing.getOpposite()))) {
      signalPos = pos.offset(facing.getOpposite());
    } else if (isSignalHead(world, pos.offset(facing))) {
      signalPos = pos.offset(facing);
      renderFacing = facing.getOpposite();
      flipped = true;
    }

    if (signalPos == null) {
      return defaultScan(facing);
    }

    float[] envelope = readSignalEnvelope(world, signalPos);
    if (envelope == null) {
      return defaultScan(facing);
    }
    float minX = envelope[0], maxX = envelope[1], minY = envelope[2], maxY = envelope[3];

    // Scan vertically for add-on signals. Don't break on gaps — double add-on signals may be
    // 2 blocks away with an empty block in between (legacy placement for world compatibility).
    for (int dy = 1; dy <= MAX_SCAN_DISTANCE; dy++) {
      float[] addon = readSignalEnvelope(world, signalPos.down(dy));
      if (addon != null) {
        minX = Math.min(minX, addon[0]);
        maxX = Math.max(maxX, addon[1]);
        minY = Math.min(minY, addon[2] - dy * 16f);
        maxY = Math.max(maxY, addon[3] - dy * 16f);
      }
      addon = readSignalEnvelope(world, signalPos.up(dy));
      if (addon != null) {
        minX = Math.min(minX, addon[0]);
        maxX = Math.max(maxX, addon[1]);
        minY = Math.min(minY, addon[2] + dy * 16f);
        maxY = Math.max(maxY, addon[3] + dy * 16f);
      }
    }

    return new CoverSignalScan(signalPos, renderFacing, flipped, minX, maxX, minY, maxY);
  }

  private static CoverSignalScan defaultScan(EnumFacing facing) {
    return new CoverSignalScan(null, facing, false,
        DEFAULT_ENV_MIN_X, DEFAULT_ENV_MAX_X, DEFAULT_ENV_MIN_Y, DEFAULT_ENV_MAX_Y);
  }

  private static boolean isSignalHead(IBlockAccess world, BlockPos pos) {
    TileEntity te = world.getTileEntity(pos);
    return te instanceof TileEntityTrafficSignalHead
        || te instanceof TileEntityBlankoutBox
        || te instanceof TileEntityLaneControlSignal;
  }

  /**
   * Reads a signal envelope from a block position. Returns {minX, maxX, minY, maxY}
   * in model units, or null if not a valid signal.
   */
  @Nullable
  private static float[] readSignalEnvelope(IBlockAccess world, BlockPos pos) {
    TileEntity te = world.getTileEntity(pos);

    if (te instanceof TileEntityBlankoutBox || te instanceof TileEntityLaneControlSignal) {
      return new float[]{
          BlankoutBoxVertexData.BODY_X_MIN, BlankoutBoxVertexData.BODY_X_MAX,
          BlankoutBoxVertexData.BODY_Y_MIN, BlankoutBoxVertexData.BODY_Y_MAX};
    }

    if (!(te instanceof TileEntityTrafficSignalHead)) return null;

    TileEntityTrafficSignalHead signalHead = (TileEntityTrafficSignalHead) te;
    IBlockState signalState = world.getBlockState(pos);
    if (!(signalState.getBlock() instanceof AbstractBlockControllableSignalHead)) return null;

    AbstractBlockControllableSignalHead signalBlock =
        (AbstractBlockControllableSignalHead) signalState.getBlock();

    int sectionCount = signalHead.getSectionCount();
    if (sectionCount == 0) return null;

    int[] sizes = signalBlock.getSectionSizes(sectionCount);
    float[] yPos = signalBlock.getSectionYPositions(sectionCount, world, pos);
    float[] xPos = signalBlock.getSectionXPositions(sectionCount, world, pos);
    float yOffset = signalBlock.getSignalYOffset(world, pos);

    float minX = Float.MAX_VALUE, maxX = -Float.MAX_VALUE;
    float minY = Float.MAX_VALUE, maxY = -Float.MAX_VALUE;

    for (int i = 0; i < sectionCount && i < sizes.length && i < yPos.length && i < xPos.length;
        i++) {
      float scale = sizes[i] / 12.0f;
      float lMinX = CENTER_X + (2.0f - CENTER_X) * scale + xPos[i];
      float lMaxX = CENTER_X + (14.0f - CENTER_X) * scale + xPos[i];
      float lMinY = CENTER_Y + (0.0f - CENTER_Y) * scale + yPos[i];
      float lMaxY = CENTER_Y + (12.0f - CENTER_Y) * scale + yPos[i];
      minX = Math.min(minX, lMinX);
      maxX = Math.max(maxX, lMaxX);
      minY = Math.min(minY, lMinY);
      maxY = Math.max(maxY, lMaxY);
    }
    if (minX > maxX) return null;
    minY += yOffset;
    maxY += yOffset;

    return new float[]{minX, maxX, minY, maxY};
  }
}

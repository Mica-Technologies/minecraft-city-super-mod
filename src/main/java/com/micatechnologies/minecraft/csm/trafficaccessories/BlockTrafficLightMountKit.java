package com.micatechnologies.minecraft.csm.trafficaccessories;

import com.micatechnologies.minecraft.csm.codeutils.AbstractBlockRotatableNSEWUD;
import com.micatechnologies.minecraft.csm.codeutils.ICsmNoSnowAccumulation;
import com.micatechnologies.minecraft.csm.codeutils.ICsmTileEntityProvider;
import com.micatechnologies.minecraft.csm.trafficsignals.TileEntityBlankoutBox;
import com.micatechnologies.minecraft.csm.trafficsignals.TileEntityTrafficSignalHead;
import com.micatechnologies.minecraft.csm.trafficsignals.logic.AbstractBlockControllableSignalHead;
import com.micatechnologies.minecraft.csm.trafficsignals.logic.BlankoutBoxVertexData;
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

/**
 * Dynamic signal mount kit block (Pelco Astro-brac style). Uses a TESR to detect the adjacent
 * traffic signal head and render a bracket that automatically adapts to the signal's orientation,
 * section count, and section sizes. Replaces the need for multiple static mount kit variants.
 *
 * <p>The visual bounding box is computed dynamically to match the bracket extent.
 * Raytrace/click targeting is clamped to the 0-1 block range so the selection box doesn't
 * steal clicks from adjacent blocks (same approach as signal head blocks).
 */
public class BlockTrafficLightMountKit extends AbstractBlockRotatableNSEWUD
    implements ICsmTileEntityProvider, ICsmNoSnowAccumulation {

  // Fallback bounding box when no signal is detected (covers default 3-section vertical bracket).
  // Computed from default signal envelope: Y=-12 to 24 model units, with arms/knuckles adding ~5 units.
  private static final AxisAlignedBB DEFAULT_BB =
      new AxisAlignedBB(0.25, -1.125, -0.25, 0.75, 1.875, 1.0);

  // Model-space constants matching the renderer
  private static final float CENTER_X = 8.0f;
  private static final float CENTER_Y = 6.0f;
  private static final float CHANNEL_OUTER = 2.5f;
  private static final float KNUCKLE_MAIN_WIDE = 4.0f;
  private static final float KNUCKLE_MAIN_THIN = 2.0f;
  private static final float PIVOT_SIZE = 3.5f;
  private static final float COLLAR_HEIGHT = 1.5f;
  private static final float ARM_FRONT_Z = -2.0f;
  private static final int MAX_SCAN_DISTANCE = 3;

  public BlockTrafficLightMountKit() {
    super(Material.ROCK, SoundType.STONE, "pickaxe", 1, 2.0F, 10.0F, 0F, 0);
  }

  @Override
  public String getBlockRegistryName() {
    return "trafficlightmountkit";
  }

  /**
   * Computes a NORTH-facing bounding box that matches the actual bracket extent based on
   * the adjacent signal. Caches the result on the tile entity for performance; the cache
   * is invalidated when a neighbor block changes.
   */
  @Override
  public AxisAlignedBB getBlockBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
    // Check cache first
    TileEntity te = source.getTileEntity(pos);
    if (te instanceof TileEntityTrafficLightMountKit) {
      AxisAlignedBB cached = ((TileEntityTrafficLightMountKit) te).getCachedBoundingBox();
      if (cached != null) return cached;
    }

    EnumFacing facing = EnumFacing.NORTH;
    if (state.getProperties().containsKey(FACING)) {
      facing = state.getValue(FACING);
    }

    // Find the primary signal (check forward then backward)
    BlockPos signalPos = null;
    if (isSignalHead(source, pos.offset(facing))) {
      signalPos = pos.offset(facing);
    } else if (isSignalHead(source, pos.offset(facing.getOpposite()))) {
      signalPos = pos.offset(facing.getOpposite());
    }

    if (signalPos == null) {
      return DEFAULT_BB;
    }

    // Read primary signal info
    float minX = Float.MAX_VALUE, maxX = -Float.MAX_VALUE;
    float minY = Float.MAX_VALUE, maxY = -Float.MAX_VALUE;
    boolean horizontal = false;

    float[] envelope = readSignalEnvelope(source, signalPos);
    if (envelope != null) {
      minX = envelope[0]; maxX = envelope[1]; minY = envelope[2]; maxY = envelope[3];
      horizontal = envelope[4] != 0;
    } else {
      return DEFAULT_BB;
    }

    // Scan vertically for add-on signals
    for (int dy = 1; dy <= MAX_SCAN_DISTANCE; dy++) {
      float[] addon = readSignalEnvelope(source, signalPos.down(dy));
      if (addon != null) {
        minX = Math.min(minX, addon[0]); maxX = Math.max(maxX, addon[1]);
        minY = Math.min(minY, addon[2] - dy * 16f); maxY = Math.max(maxY, addon[3] - dy * 16f);
      }
      addon = readSignalEnvelope(source, signalPos.up(dy));
      if (addon != null) {
        minX = Math.min(minX, addon[0]); maxX = Math.max(maxX, addon[1]);
        minY = Math.min(minY, addon[2] + dy * 16f); maxY = Math.max(maxY, addon[3] + dy * 16f);
      }
    }

    // Compute BB from bracket geometry (NORTH-facing, in model units)
    float bbMinX, bbMaxX, bbMinY, bbMaxY;
    float bbMinZ = ARM_FRONT_Z;
    float bbMaxZ = 16.0f;

    if (horizontal) {
      float armCenterY = CENTER_Y;
      bbMinY = armCenterY - CHANNEL_OUTER / 2f;
      bbMaxY = armCenterY + CHANNEL_OUTER / 2f;
      // Arms + knuckles extend outward from signal edges
      float leftPivot = minX - CHANNEL_OUTER / 2f;
      float rightPivot = maxX + CHANNEL_OUTER / 2f;
      bbMinX = minX - CHANNEL_OUTER - KNUCKLE_MAIN_THIN;
      bbMaxX = maxX + CHANNEL_OUTER + KNUCKLE_MAIN_THIN;
      // Collar at right end
      bbMaxX = Math.max(bbMaxX, rightPivot + PIVOT_SIZE / 2f + COLLAR_HEIGHT);
    } else {
      float armCenterX = CENTER_X;
      bbMinX = armCenterX - Math.max(CHANNEL_OUTER, KNUCKLE_MAIN_WIDE) / 2f;
      bbMaxX = armCenterX + Math.max(CHANNEL_OUTER, KNUCKLE_MAIN_WIDE) / 2f;
      // Arms + knuckles extend outward from signal edges
      bbMinY = minY - CHANNEL_OUTER - KNUCKLE_MAIN_THIN;
      bbMaxY = maxY + CHANNEL_OUTER + KNUCKLE_MAIN_THIN;
      // Collar at top
      float topPivot = maxY + CHANNEL_OUTER / 2f;
      bbMaxY = Math.max(bbMaxY, topPivot + PIVOT_SIZE / 2f + COLLAR_HEIGHT);
    }

    // Convert model units to block coordinates (16 model units = 1 block)
    AxisAlignedBB result = new AxisAlignedBB(
        bbMinX / 16.0, bbMinY / 16.0, bbMinZ / 16.0,
        bbMaxX / 16.0, bbMaxY / 16.0, bbMaxZ / 16.0);

    // Cache on the tile entity
    if (te instanceof TileEntityTrafficLightMountKit) {
      ((TileEntityTrafficLightMountKit) te).setCachedBoundingBox(result);
    }

    return result;
  }

  @Override
  public void neighborChanged(IBlockState state, World worldIn, BlockPos pos, Block blockIn,
      BlockPos fromPos) {
    super.neighborChanged(state, worldIn, pos, blockIn, fromPos);
    TileEntity te = worldIn.getTileEntity(pos);
    if (te instanceof TileEntityTrafficLightMountKit) {
      ((TileEntityTrafficLightMountKit) te).invalidateCachedBB();
    }
  }

  /**
   * Sneak + right-click cycles the bracket's color scheme. Plain right-click is a no-op so
   * players can still open other blocks or place items near the kit without accidentally
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
    if (!(te instanceof TileEntityTrafficLightMountKit)) {
      return false;
    }
    TileEntityTrafficLightMountKit mountKit = (TileEntityTrafficLightMountKit) te;
    MountKitColorScheme next = mountKit.cycleColorScheme();
    mountKit.markDirtySync(world, pos, true);
    player.sendMessage(new TextComponentString(
        TextFormatting.GRAY + "Mount kit color: " + TextFormatting.WHITE + next.getFriendlyName()));
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
    return TileEntityTrafficLightMountKit.class;
  }

  @Override
  public String getTileEntityName() {
    return "tileentitytrafficlightmountkit";
  }

  @Override
  public TileEntity createNewTileEntity(World worldIn, int meta) {
    return new TileEntityTrafficLightMountKit();
  }

  // --- Signal detection helpers (shared logic with renderer) ---

  private static boolean isSignalHead(IBlockAccess world, BlockPos pos) {
    TileEntity te = world.getTileEntity(pos);
    return te instanceof TileEntityTrafficSignalHead
        || te instanceof TileEntityBlankoutBox;
  }

  /**
   * Reads signal envelope from a block position. Returns {minX, maxX, minY, maxY, horizontal}
   * in model units, or null if not a valid signal.
   */
  @Nullable
  private static float[] readSignalEnvelope(IBlockAccess world, BlockPos pos) {
    TileEntity te = world.getTileEntity(pos);

    if (te instanceof TileEntityBlankoutBox) {
      return new float[]{
          BlankoutBoxVertexData.BODY_X_MIN, BlankoutBoxVertexData.BODY_X_MAX,
          BlankoutBoxVertexData.BODY_Y_MIN, BlankoutBoxVertexData.BODY_Y_MAX, 0f};
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

    for (int i = 0; i < sectionCount; i++) {
      float scale = sizes[i] / 12.0f;
      float lMinX = CENTER_X + (2.0f - CENTER_X) * scale + xPos[i];
      float lMaxX = CENTER_X + (14.0f - CENTER_X) * scale + xPos[i];
      float lMinY = CENTER_Y + (0.0f - CENTER_Y) * scale + yPos[i];
      float lMaxY = CENTER_Y + (12.0f - CENTER_Y) * scale + yPos[i];
      minX = Math.min(minX, lMinX); maxX = Math.max(maxX, lMaxX);
      minY = Math.min(minY, lMinY); maxY = Math.max(maxY, lMaxY);
    }
    minY += yOffset; maxY += yOffset;

    return new float[]{minX, maxX, minY, maxY, signalBlock.isHorizontal(world, pos) ? 1f : 0f};
  }
}

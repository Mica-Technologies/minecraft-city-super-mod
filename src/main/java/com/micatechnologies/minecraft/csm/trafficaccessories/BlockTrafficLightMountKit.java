package com.micatechnologies.minecraft.csm.trafficaccessories;

import com.micatechnologies.minecraft.csm.codeutils.AbstractBlockRotatableNSEWUD;
import com.micatechnologies.minecraft.csm.codeutils.ICsmTileEntityProvider;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

/**
 * Dynamic signal mount kit block (Pelco Astro-brac style). Uses a TESR to detect the adjacent
 * traffic signal head and render a bracket that automatically adapts to the signal's orientation,
 * section count, and section sizes. Replaces the need for multiple static mount kit variants.
 *
 * <p>The visual bounding box is generous to avoid render clipping, but raytrace/click
 * targeting is clamped to the 0-1 block range so the oversized selection box doesn't steal
 * clicks from adjacent blocks (same approach as signal head blocks).
 */
public class BlockTrafficLightMountKit extends AbstractBlockRotatableNSEWUD
    implements ICsmTileEntityProvider {

  // Generous static bounding box that covers the maximum bracket extent for the visual
  // selection outline. Covers both horizontal and vertical orientations.
  private static final AxisAlignedBB BOUNDING_BOX =
      new AxisAlignedBB(-0.6875, -0.8125, -0.25, 1.6875, 1.5625, 1.0);

  public BlockTrafficLightMountKit() {
    super(Material.ROCK, SoundType.STONE, "pickaxe", 1, 2.0F, 10.0F, 0F, 0);
  }

  @Override
  public String getBlockRegistryName() {
    return "trafficlightmountkit";
  }

  @Override
  public AxisAlignedBB getBlockBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
    return BOUNDING_BOX;
  }

  /**
   * Clamps the raytrace AABB to the 0-1 block range so the oversized visual selection box
   * doesn't steal clicks from adjacent blocks. Same approach used by signal head blocks.
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
}

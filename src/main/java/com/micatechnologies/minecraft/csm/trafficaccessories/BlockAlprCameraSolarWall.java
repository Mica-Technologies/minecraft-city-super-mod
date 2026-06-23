package com.micatechnologies.minecraft.csm.trafficaccessories;

import com.micatechnologies.minecraft.csm.codeutils.AbstractBlockRotatableNSEW;
import com.micatechnologies.minecraft.csm.codeutils.ICsmNoSnowAccumulation;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;

/**
 * Side/wall-mountable solar ALPR (automatic license-plate reader) camera. The same matte-black
 * "Flock"-style teardrop camera and tilted solar panel as {@link BlockAlprCameraSolar}, but on a
 * short back-plate bracket that seats flush against a wall or an existing pole instead of carrying
 * its own ground pole. The camera aims outward (toward the unrotated -Z / north face) and a
 * drooping power cable links the panel to the camera. Renders from the procedural OBJ model
 * {@code alpr_camera_solar_wall.obj} (see {@code dev-env-utils/scripts/gen_alpr_obj.py}).
 *
 * @author Mica Technologies
 * @since 1.0
 */
public class BlockAlprCameraSolarWall extends AbstractBlockRotatableNSEW
    implements ICsmNoSnowAccumulation {

  public BlockAlprCameraSolarWall() {
    super(Material.ROCK, SoundType.METAL, "pickaxe", 1, 2F, 10F, 0F, 0);
  }

  /**
   * Retrieves the registry name of the block.
   *
   * @return The registry name of the block.
   *
   * @since 1.0
   */
  @Override
  public String getBlockRegistryName() {
    return "alprcamerasolarwall";
  }

  /**
   * Retrieves the bounding box of the block. Covers the camera body and back plate; the solar panel
   * renders above the block (the overhead mast lifts it past the block's top face).
   *
   * @since 1.0
   */
  @Override
  public AxisAlignedBB getBlockBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
    return new AxisAlignedBB(0.26, 0.30, 0.18, 0.74, 1.0, 0.50);
  }

  /**
   * Retrieves whether the block is an opaque cube.
   *
   * @param state The block state.
   *
   * @return {@code true} if the block is an opaque cube, {@code false} otherwise.
   *
   * @since 1.0
   */
  @Override
  public boolean getBlockIsOpaqueCube(IBlockState state) {
    return false;
  }

  /**
   * Retrieves whether the block is a full cube.
   *
   * @param state The block state.
   *
   * @return {@code true} if the block is a full cube, {@code false} otherwise.
   *
   * @since 1.0
   */
  @Override
  public boolean getBlockIsFullCube(IBlockState state) {
    return false;
  }

  /**
   * Retrieves whether the block connects to redstone.
   *
   * @param state  the block state
   * @param access the block access
   * @param pos    the block position
   * @param facing the block facing direction
   *
   * @return {@code true} if the block connects to redstone, {@code false} otherwise.
   *
   * @since 1.0
   */
  @Override
  public boolean getBlockConnectsRedstone(IBlockState state,
      IBlockAccess access,
      BlockPos pos,
      @Nullable
      EnumFacing facing) {
    return false;
  }

  /**
   * Retrieves the block's render layer.
   *
   * @return The block's render layer.
   *
   * @since 1.0
   */
  @Nonnull
  @Override
  public BlockRenderLayer getBlockRenderLayer() {
    return BlockRenderLayer.CUTOUT_MIPPED;
  }
}

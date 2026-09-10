package com.micatechnologies.minecraft.csm.codeutils;

import javax.annotation.Nonnull;
import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

/**
 * Abstract block class which provides the same common methods and properties as
 * {@link AbstractBlockRotatableNSEW} and settles the block onto the surface below it.
 *
 * <p>The horizontal counterpart of {@link AbstractBlockRoadSurface}, for work zone devices that
 * face a direction — barricades, vertical panels, the arrow board. See {@link RoadSurfaceHeight}
 * for how the surface height is worked out.</p>
 *
 * <p>The offset is applied <em>after</em> the facing rotation the superclass performs, which is
 * why this overrides {@code getBoundingBox} rather than the rotated
 * {@code getBlockBoundingBox}: a vertical shift is unchanged by a horizontal rotation, but a
 * subclass box passed through the rotation twice is not.</p>
 *
 * @version 1.0
 * @see AbstractBlockRotatableNSEW
 * @see RoadSurfaceHeight
 * @since 2026.9
 */
public abstract class AbstractBlockRoadSurfaceRotatableNSEW extends AbstractBlockRotatableNSEW
    implements ICsmRoadSurfaceAware {

  /**
   * Constructs an {@link AbstractBlockRoadSurfaceRotatableNSEW} instance.
   *
   * @param material The material of the block.
   *
   * @since 1.0
   */
  public AbstractBlockRoadSurfaceRotatableNSEW(Material material) {
    super(material);
  }

  /**
   * Constructs an {@link AbstractBlockRoadSurfaceRotatableNSEW} instance.
   *
   * @param material         The material of the block.
   * @param soundType        The sound type of the block.
   * @param harvestToolClass The harvest tool class of the block.
   * @param harvestLevel     The harvest level of the block.
   * @param hardness         The block's hardness.
   * @param resistance       The block's resistance to explosions.
   * @param lightLevel       The block's light level.
   * @param lightOpacity     The block's light opacity.
   *
   * @since 1.0
   */
  public AbstractBlockRoadSurfaceRotatableNSEW(Material material,
      SoundType soundType,
      String harvestToolClass,
      int harvestLevel,
      float hardness,
      float resistance,
      float lightLevel,
      int lightOpacity) {
    super(material, soundType, harvestToolClass, harvestLevel, hardness, resistance, lightLevel,
        lightOpacity);
  }

  /**
   * Overridden method from {@link Block} which declares that this block renders at an offset from
   * its own cell.
   *
   * @return the offset type of the block
   *
   * @see AbstractBlockRoadSurface#getOffsetType()
   * @since 1.0
   */
  @Override
  @Nonnull
  public Block.EnumOffsetType getOffsetType() {
    return Block.EnumOffsetType.XYZ;
  }

  /**
   * Overridden method from {@link Block} which offsets the block's rendering downward onto the
   * surface below it.
   *
   * @param state the block state
   * @param world the block access
   * @param pos   the block position
   *
   * @return the render offset of the block
   *
   * @since 1.0
   */
  @Override
  @Nonnull
  public Vec3d getOffset(IBlockState state, IBlockAccess world, BlockPos pos) {
    return getRoadSurfaceOffsetVector(world, pos);
  }

  /**
   * Overridden method from {@link AbstractBlockRotatableNSEW} which applies the same downward
   * offset to the rotated bounding box that {@link #getOffset} applies to the rendering, so that
   * the block is selected and collided with where it is drawn.
   *
   * @param state  the block state
   * @param source the block access
   * @param pos    the block position
   *
   * @return the bounding box of the block, rotated and settled onto the surface below it
   *
   * @since 1.0
   */
  @Override
  @Nonnull
  public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
    AxisAlignedBB box = super.getBoundingBox(state, source, pos);
    double offset = getRoadSurfaceOffset(source, pos);
    return offset == 0.0 ? box : box.offset(0.0, offset, 0.0);
  }

  /**
   * Overridden method from {@link Block} which redraws the block when the surface below it
   * changes.
   *
   * @param state   the block state
   * @param world   the world the block is in
   * @param pos     the block position
   * @param blockIn the neighbor block
   * @param fromPos the neighbor block position
   *
   * @see AbstractBlockRoadSurface#neighborChanged(IBlockState, World, BlockPos, Block, BlockPos)
   * @since 1.0
   */
  @Override
  public void neighborChanged(IBlockState state, World world, BlockPos pos, Block blockIn,
      BlockPos fromPos) {
    super.neighborChanged(state, world, pos, blockIn, fromPos);
    RoadSurfaceHeight.markSurfaceRenderUpdate(world, pos, fromPos);
  }
}

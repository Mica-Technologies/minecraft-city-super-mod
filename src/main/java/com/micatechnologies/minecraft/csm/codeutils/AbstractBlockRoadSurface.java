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
 * {@link AbstractBlock} and settles the block onto the surface below it, for blocks with no
 * rotation.
 *
 * <p>A block extending this is placed in the cell above a road and drawn pulled down onto it by
 * however much the road did not fill, so a work zone device stands on a road that climbs in
 * sixteenths exactly as it stands on flat ground. See {@link RoadSurfaceHeight}.</p>
 *
 * @version 1.0
 * @see AbstractBlock
 * @see RoadSurfaceHeight
 * @since 2026.9
 */
public abstract class AbstractBlockRoadSurface extends AbstractBlock
    implements ICsmRoadSurfaceAware {

  /**
   * Constructs an {@link AbstractBlockRoadSurface} instance.
   *
   * @param material The material of the block.
   *
   * @since 1.0
   */
  public AbstractBlockRoadSurface(Material material) {
    super(material);
  }

  /**
   * Constructs an {@link AbstractBlockRoadSurface} instance.
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
  public AbstractBlockRoadSurface(Material material,
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
   * <p>The renderer in this version reads the offset unconditionally, so this is not what makes
   * the offset work. It is declared because it is the documented way to say a block does this,
   * and because it is what the default {@link Block#getOffset} body consults — which matters for
   * anything that reaches the default body instead of the override below.</p>
   *
   * @return the offset type of the block
   *
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
   * Overridden method from {@link AbstractBlock} which applies the same downward offset to the
   * bounding box that {@link #getOffset} applies to the rendering, so that the block is selected
   * and collided with where it is drawn.
   *
   * @param state  the block state
   * @param source the block access
   * @param pos    the block position
   *
   * @return the bounding box of the block, settled onto the surface below it
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
   * <p>The offset is read from a neighbour, so nothing marks this block's own chunk section for
   * a rebuild when that neighbour changes, and the two can be in different sections. Without
   * this, raising the road under a placed device leaves the device drawn at its old height until
   * something else happens to rebuild the section.</p>
   *
   * @param state   the block state
   * @param world   the world the block is in
   * @param pos     the block position
   * @param blockIn the neighbor block
   * @param fromPos the neighbor block position
   *
   * @since 1.0
   */
  @Override
  public void neighborChanged(IBlockState state, World world, BlockPos pos, Block blockIn,
      BlockPos fromPos) {
    super.neighborChanged(state, world, pos, blockIn, fromPos);
    RoadSurfaceHeight.markSurfaceRenderUpdate(world, pos, fromPos);
  }
}

package com.micatechnologies.minecraft.csm.hvac;

import com.micatechnologies.minecraft.csm.codeutils.AbstractBlockRotatableNSEW;
import com.micatechnologies.minecraft.csm.codeutils.ICsmTileEntityProvider;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.state.IBlockState;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

/**
 * HVAC Zone Boundary (#305): a slim strip hung under a ceiling that splits a big open floor into
 * separate HVAC rooms without a wall. To the room finder its whole column, from the floor up to
 * the ceiling, is the edge of a room that heat crosses as freely as open air
 * ({@link HvacZoneBoundaries}); to a player it is nothing to walk into. A line of them across a
 * floor, from wall to wall, makes two rooms, each under the room size limit and each rescanned on
 * its own when a block in it changes.
 *
 * <p>Stored: the facing, which only turns the strip (it runs along the way the placer looked).
 * The tile entity holds no data; it only puts the marker in its world's index while its chunk is
 * loaded, as an HVAC device's anchor does.</p>
 *
 * @author Mica Technologies
 * @since 2026.10
 */
public class BlockHvacZoneBoundary extends AbstractBlockRotatableNSEW
    implements ICsmTileEntityProvider {

  /** The strip running north and south, flush under the ceiling. */
  private static final AxisAlignedBB BOX = new AxisAlignedBB(0.4375, 0.9375, 0.0, 0.5625, 1.0,
      1.0);

  public BlockHvacZoneBoundary() {
    super(Material.IRON, SoundType.METAL, "pickaxe", 0, 1F, 5F, 0F, 0);
  }

  @Override
  public String getBlockRegistryName() {
    return "hvac_zone_boundary";
  }

  @Override
  public AxisAlignedBB getBlockBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
    return BOX;
  }

  /** Nothing to walk into, and air passes it (it is open air to everything but the room finder). */
  @Nullable
  @Override
  @SuppressWarnings("deprecation")
  public AxisAlignedBB getCollisionBoundingBox(IBlockState state, IBlockAccess source,
      BlockPos pos) {
    return NULL_AABB;
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
  public BlockFaceShape getBlockFaceShape(IBlockAccess worldIn, IBlockState state, BlockPos pos,
      EnumFacing face) {
    return BlockFaceShape.UNDEFINED;
  }

  @Nonnull
  @Override
  public BlockRenderLayer getBlockRenderLayer() {
    return BlockRenderLayer.CUTOUT_MIPPED;
  }

  @Override
  public Class<? extends TileEntity> getTileEntityClass() {
    return TileEntityHvacZoneBoundary.class;
  }

  @Override
  public String getTileEntityName() {
    return "tileentityhvaczoneboundary";
  }

  @Override
  public TileEntity createNewTileEntity(World worldIn, int meta) {
    return new TileEntityHvacZoneBoundary();
  }
}

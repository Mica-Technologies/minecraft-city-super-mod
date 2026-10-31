package com.micatechnologies.minecraft.csm.venues.av;

import com.micatechnologies.minecraft.csm.codeutils.AbstractBlock;
import com.micatechnologies.minecraft.csm.codeutils.CsmBlockStateContainer;
import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

/**
 * A venue catwalk ({@code venue_catwalk}): a floor of steel bar grating in the top of the roof
 * and over the bowl, for reaching the scoreboard's rigging, the speakers and the banners. Cells
 * side by side are one walkway; every side with no catwalk beside it gets a rail (toe board, mid
 * rail and top rail), so a run closes itself. Nothing is stored: the four sides are actual state.
 * Venues may not need Roads, so this is its own and not Roads' truss catwalk.
 *
 * @since 2026.10
 */
public class BlockVenueCatwalk extends AbstractBlock {

  public static final PropertyBool NORTH = PropertyBool.create("north");
  public static final PropertyBool EAST = PropertyBool.create("east");
  public static final PropertyBool SOUTH = PropertyBool.create("south");
  public static final PropertyBool WEST = PropertyBool.create("west");

  private static final double FLOOR = 1.5 / 16.0;
  private static final double RAIL_H = 1.5;
  private static final AxisAlignedBB FLOOR_BOX = new AxisAlignedBB(0, 0, 0, 1, FLOOR, 1);
  private static final AxisAlignedBB RAIL_N = new AxisAlignedBB(0, 0, 0, 1, RAIL_H, 1 / 16.0);
  private static final AxisAlignedBB RAIL_S = new AxisAlignedBB(0, 0, 15 / 16.0, 1, RAIL_H, 1);
  private static final AxisAlignedBB RAIL_W = new AxisAlignedBB(0, 0, 0, 1 / 16.0, RAIL_H, 1);
  private static final AxisAlignedBB RAIL_E = new AxisAlignedBB(15 / 16.0, 0, 0, 1, RAIL_H, 1);
  private static final AxisAlignedBB SELECT = new AxisAlignedBB(0, 0, 0, 1, 1, 1);

  /** Constructs the catwalk. */
  public BlockVenueCatwalk() {
    super(Material.IRON, SoundType.METAL, "pickaxe", 0, 2F, 6F, 0F, 0);
    setDefaultState(blockState.getBaseState().withProperty(NORTH, true).withProperty(EAST, true)
        .withProperty(SOUTH, true).withProperty(WEST, true));
  }

  @Override
  public String getBlockRegistryName() {
    return "venue_catwalk";
  }

  @Override
  @Nonnull
  protected BlockStateContainer createBlockState() {
    return new CsmBlockStateContainer(this, NORTH, EAST, SOUTH, WEST);
  }

  @Override
  @Nonnull
  @SuppressWarnings("deprecation")
  public IBlockState getStateFromMeta(int meta) {
    return getDefaultState();
  }

  @Override
  public int getMetaFromState(IBlockState state) {
    return 0;
  }

  /** A side is railed (true) where the cell beside it holds no catwalk. */
  @Override
  @Nonnull
  @SuppressWarnings("deprecation")
  public IBlockState getActualState(@Nonnull IBlockState state, IBlockAccess world,
      BlockPos pos) {
    return state.withProperty(NORTH, open(world, pos, EnumFacing.NORTH))
        .withProperty(EAST, open(world, pos, EnumFacing.EAST))
        .withProperty(SOUTH, open(world, pos, EnumFacing.SOUTH))
        .withProperty(WEST, open(world, pos, EnumFacing.WEST));
  }

  private boolean open(IBlockAccess world, BlockPos pos, EnumFacing side) {
    return !(world.getBlockState(pos.offset(side)).getBlock() instanceof BlockVenueCatwalk);
  }

  @Override
  public AxisAlignedBB getBlockBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
    return SELECT;
  }

  @Override
  @Nullable
  @SuppressWarnings("deprecation")
  public AxisAlignedBB getCollisionBoundingBox(IBlockState state, IBlockAccess source,
      BlockPos pos) {
    return FLOOR_BOX;
  }

  @Override
  @SuppressWarnings("deprecation")
  public void addCollisionBoxToList(IBlockState state, World world, BlockPos pos,
      AxisAlignedBB entityBox, List<AxisAlignedBB> boxes, @Nullable Entity entity,
      boolean isActualState) {
    addCollisionBoxToList(pos, entityBox, boxes, FLOOR_BOX);
    if (open(world, pos, EnumFacing.NORTH)) {
      addCollisionBoxToList(pos, entityBox, boxes, RAIL_N);
    }
    if (open(world, pos, EnumFacing.SOUTH)) {
      addCollisionBoxToList(pos, entityBox, boxes, RAIL_S);
    }
    if (open(world, pos, EnumFacing.WEST)) {
      addCollisionBoxToList(pos, entityBox, boxes, RAIL_W);
    }
    if (open(world, pos, EnumFacing.EAST)) {
      addCollisionBoxToList(pos, entityBox, boxes, RAIL_E);
    }
  }

  @Override
  @Nonnull
  @SuppressWarnings("deprecation")
  public BlockFaceShape getBlockFaceShape(@Nonnull IBlockAccess world, @Nonnull IBlockState state,
      @Nonnull BlockPos pos, @Nonnull EnumFacing face) {
    return face == EnumFacing.DOWN ? BlockFaceShape.SOLID : BlockFaceShape.UNDEFINED;
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

  @Override
  @Nonnull
  public BlockRenderLayer getBlockRenderLayer() {
    return BlockRenderLayer.CUTOUT;
  }
}

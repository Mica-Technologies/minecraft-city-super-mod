package com.micatechnologies.minecraft.csm.venues.outdoor;

import com.micatechnologies.minecraft.csm.codeutils.CsmBlockStateContainer;
import com.micatechnologies.minecraft.csm.codeutils.RotationUtils;
import com.micatechnologies.minecraft.csm.venues.BlockVenueUnit;
import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

/**
 * A press box wall module ({@code press_box_module}): one bay of a press box's outside wall, two
 * blocks tall, on the front edge of its cells: a {@link BlockVenueUnit} whose master is the lower
 * block, the upper one an invisible part. The window module is a siding panel to desk height, a writing
 * ledge inside, tinted glass leaning out over the field (as a press box's does, against glare)
 * and a fascia; the wall module is siding to the fascia. Modules side by side facing the same way
 * are one wall, mullions between the bays and end cheeks where the wall stops ({@code end_l},
 * {@code end_r}, actual state). A corner module also walls its left side (looking the way it
 * faces), turning the wall round an outside corner: build a box clockwise, each corner facing the
 * run that leaves it. A slab roof on the modules closes the box.
 *
 * <p><b>Metadata</b>: bits 0-1 the facing (S 0, W 1, N 2, E 3), outwards (the window looks that
 * way); bit 2 a solid wall rather than a window; bit 3 a corner. Placed by hand it faces the
 * placer, a window; sneaking places a wall.</p>
 *
 * @since 2026.10
 */
public class BlockPressBox extends BlockVenueUnit {

  /** Siding all the way up rather than a window. Stored, bit 2. */
  public static final PropertyBool WALL = PropertyBool.create("wall");
  /** Also walls its left side: an outside corner. Stored, bit 3. */
  public static final PropertyBool CORNER = PropertyBool.create("corner");
  /** The wall stops on the left (looking the way it faces). Actual. */
  public static final PropertyBool END_L = PropertyBool.create("end_l");
  /** The wall stops on the right. Actual. */
  public static final PropertyBool END_R = PropertyBool.create("end_r");

  private static final AxisAlignedBB FRONT = box(0, 0, 0, 16, 16, 3);
  private static final AxisAlignedBB SIDE = box(0, 0, 0, 3, 16, 16);

  /**
   * Constructs the module.
   *
   * @param registryName its registry name
   * @param cells        its footprint after the master
   * @param boxes        its boxes
   */
  public BlockPressBox(String registryName, int[] cells, int[] boxes) {
    super(registryName, cells, boxes);
    setDefaultState(blockState.getBaseState().withProperty(FACING, EnumFacing.NORTH)
        .withProperty(WALL, false).withProperty(CORNER, false).withProperty(END_L, true)
        .withProperty(END_R, true));
  }

  static AxisAlignedBB box(double x0, double y0, double z0, double x1, double y1, double z1) {
    return new AxisAlignedBB(x0 / 16.0, y0 / 16.0, z0 / 16.0, x1 / 16.0, y1 / 16.0, z1 / 16.0);
  }

  @Override
  @Nonnull
  protected BlockStateContainer createBlockState() {
    return new CsmBlockStateContainer(this, FACING, WALL, CORNER, END_L, END_R);
  }

  @Override
  @Nonnull
  @SuppressWarnings("deprecation")
  public IBlockState getStateFromMeta(int meta) {
    return getDefaultState().withProperty(FACING, EnumFacing.byHorizontalIndex(meta & 3))
        .withProperty(WALL, (meta & 4) != 0).withProperty(CORNER, (meta & 8) != 0);
  }

  @Override
  public int getMetaFromState(IBlockState state) {
    return state.getValue(FACING).getHorizontalIndex() | (state.getValue(WALL) ? 4 : 0)
        | (state.getValue(CORNER) ? 8 : 0);
  }

  @Override
  @Nonnull
  public IBlockState getStateForPlacement(World world, BlockPos pos, EnumFacing facing,
      float hitX, float hitY, float hitZ, int meta, EntityLivingBase placer) {
    return getDefaultState().withProperty(FACING, placer.getHorizontalFacing().getOpposite())
        .withProperty(WALL, placer.isSneaking());
  }

  @Override
  public int damageDropped(IBlockState state) {
    return 0;
  }

  @Override
  @Nonnull
  @SuppressWarnings("deprecation")
  public IBlockState getActualState(@Nonnull IBlockState state, IBlockAccess world,
      BlockPos pos) {
    EnumFacing f = state.getValue(FACING);
    return state.withProperty(END_L, !same(world, pos.offset(f.rotateYCCW()), f))
        .withProperty(END_R, !same(world, pos.offset(f.rotateY()), f));
  }

  private boolean same(IBlockAccess world, BlockPos pos, EnumFacing facing) {
    IBlockState s = world.getBlockState(pos);
    return s.getBlock() == this && s.getValue(FACING) == facing;
  }

  @Override
  public AxisAlignedBB getBlockBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
    return state.getValue(CORNER) ? new AxisAlignedBB(0, 0, 0, 1, 1, 1) : FRONT;
  }

  @Override
  @SuppressWarnings("deprecation")
  public void addCollisionBoxToList(IBlockState state, World world, BlockPos pos,
      AxisAlignedBB entityBox, List<AxisAlignedBB> boxes, @Nullable Entity entity,
      boolean isActualState) {
    EnumFacing f = state.getValue(FACING);
    addCollisionBoxToList(pos, entityBox, boxes,
        RotationUtils.rotateBoundingBoxByFacing(FRONT, f));
    if (state.getValue(CORNER)) {
      addCollisionBoxToList(pos, entityBox, boxes,
          RotationUtils.rotateBoundingBoxByFacing(SIDE, f));
    }
  }

  @Override
  @Nonnull
  public BlockRenderLayer getBlockRenderLayer() {
    return BlockRenderLayer.TRANSLUCENT;
  }
}

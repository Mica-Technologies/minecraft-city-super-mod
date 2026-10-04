package com.micatechnologies.minecraft.csm.trafficaccessories.truss;

import com.micatechnologies.minecraft.csm.codeutils.AbstractBlockRotatableNSEW;
import com.micatechnologies.minecraft.csm.codeutils.CsmBlockStateContainer;
import com.micatechnologies.minecraft.csm.codeutils.RotationUtils;
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
 * The maintenance catwalk along an overhead sign truss: bar grating, a toe board and a railing on
 * its outer edge, and brackets under the floor reaching back into the truss behind it
 * (gen_sign_truss.py). Real sign bridges carry one on the sign side, below the signs, so a crew can
 * reach the signs' lights; place it in front of the truss a block below the sign.
 *
 * <p>Its facing is the outer side, towards whoever placed it, with the truss behind. Blocks of the
 * same facing side by side join into one walkway; a railing closes each end that nothing carries
 * on from ({@link #END_WEST}, {@link #END_EAST}, in the model's frame: west and east of a catwalk
 * facing north). It collides as its floor and its railings, the railings at a fence's height so it
 * cannot be climbed over.</p>
 *
 * @since 2026.10
 */
public class BlockTrussCatwalk extends AbstractBlockRotatableNSEW {

  /** The model's west end (to the left, facing out) does not carry on: a railing across it. */
  public static final PropertyBool END_WEST = PropertyBool.create("end_west");
  /** The model's east end does not carry on. */
  public static final PropertyBool END_EAST = PropertyBool.create("end_east");

  private static final ThreadLocal<String> PENDING = new ThreadLocal<>();

  // facing north, in sixteenths (gen_sign_truss.py's CW_* numbers); railings to a fence's height
  private static final AxisAlignedBB FLOOR = box(0, 0, 0, 16, 1, 16);
  private static final AxisAlignedBB OUTER = box(0, 1, 0.5, 16, 24, 1.5);
  private static final AxisAlignedBB WEST = box(0.5, 1, 1.5, 1.5, 24, 16);
  private static final AxisAlignedBB EAST = box(14.5, 1, 1.5, 15.5, 24, 16);

  private final String registryName;

  public BlockTrussCatwalk(String registryName) {
    super(stash(registryName), SoundType.METAL, "pickaxe", 1, 3.0F, 12.0F, 0F, 0);
    this.registryName = registryName;
    PENDING.remove();
    setDefaultState(getDefaultState().withProperty(END_WEST, false).withProperty(END_EAST, false));
  }

  private static Material stash(String registryName) {
    PENDING.set(registryName);
    return Material.IRON;
  }

  private static AxisAlignedBB box(double x0, double y0, double z0, double x1, double y1,
      double z1) {
    return new AxisAlignedBB(x0 / 16, y0 / 16, z0 / 16, x1 / 16, y1 / 16, z1 / 16);
  }

  @Override
  public String getBlockRegistryName() {
    return registryName != null ? registryName : PENDING.get();
  }

  @Override
  @Nonnull
  protected BlockStateContainer createBlockState() {
    return new CsmBlockStateContainer(this, FACING, END_WEST, END_EAST);
  }

  @Override
  @Nonnull
  @SuppressWarnings("deprecation")
  public IBlockState getActualState(@Nonnull IBlockState state, IBlockAccess world,
      BlockPos pos) {
    EnumFacing facing = state.getValue(FACING);
    // the model is drawn facing north and turned by y: its west is the facing turned
    // counter-clockwise, its east the facing turned clockwise
    return state.withProperty(END_WEST, !continues(world, pos.offset(facing.rotateYCCW()), facing))
        .withProperty(END_EAST, !continues(world, pos.offset(facing.rotateY()), facing));
  }

  private boolean continues(IBlockAccess world, BlockPos pos, EnumFacing facing) {
    IBlockState s = world.getBlockState(pos);
    return s.getBlock() == this && s.getValue(FACING) == facing;
  }

  @Override
  public AxisAlignedBB getBlockBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
    return RotationUtils.rotateBoundingBoxByFacing(box(0, 0, 0, 16, 16, 16),
        state.getValue(FACING));
  }

  @Override
  @SuppressWarnings("deprecation")
  public void addCollisionBoxToList(@Nonnull IBlockState state, @Nonnull World world,
      @Nonnull BlockPos pos, @Nonnull AxisAlignedBB entityBox,
      @Nonnull List<AxisAlignedBB> collidingBoxes, @Nullable Entity entity,
      boolean isActualState) {
    IBlockState actual = isActualState ? state : getActualState(state, world, pos);
    EnumFacing facing = actual.getValue(FACING);
    addCollisionBoxToList(pos, entityBox, collidingBoxes,
        RotationUtils.rotateBoundingBoxByFacing(FLOOR, facing));
    addCollisionBoxToList(pos, entityBox, collidingBoxes,
        RotationUtils.rotateBoundingBoxByFacing(OUTER, facing));
    if (actual.getValue(END_WEST)) {
      addCollisionBoxToList(pos, entityBox, collidingBoxes,
          RotationUtils.rotateBoundingBoxByFacing(WEST, facing));
    }
    if (actual.getValue(END_EAST)) {
      addCollisionBoxToList(pos, entityBox, collidingBoxes,
          RotationUtils.rotateBoundingBoxByFacing(EAST, facing));
    }
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

  @Override
  @Nonnull
  @SuppressWarnings("deprecation")
  public BlockFaceShape getBlockFaceShape(IBlockAccess world, IBlockState state, BlockPos pos,
      EnumFacing face) {
    return BlockFaceShape.UNDEFINED;
  }
}

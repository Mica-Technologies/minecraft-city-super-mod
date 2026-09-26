package com.micatechnologies.minecraft.csm.powergrid.water;

import com.micatechnologies.minecraft.csm.codeutils.AbstractBlock;
import com.micatechnologies.minecraft.csm.codeutils.CsmBlockStateContainer;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.properties.PropertyEnum;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

/**
 * A water tower's cross bracing, a block of it at a time: a tie rod corner to corner of the
 * block in the plane through the legs' centres ({@link #AXIS}, stored, the axis the plane runs
 * along, square to the placing player's view).
 *
 * <p>A rod goes up to the right ({@link #RISE}) where another brace is diagonally up-right or
 * down-left of it in its plane, and down ({@link #FALL}) where one is up-left or down-right, both
 * where both are (the middle of an X), and rises when it stands alone. So a square panel between
 * two legs is braced by placing a brace in each block along its two diagonals. Where the block
 * beside it in the plane is a leg ({@link #LEGL}, {@link #LEGR}), the rod reaches on into it.
 * All four are actual state.</p>
 *
 * @since 2026.9
 */
public class BlockTowerBrace extends AbstractBlock {

  public static final PropertyEnum<EnumFacing.Axis> AXIS = PropertyEnum.create("axis",
      EnumFacing.Axis.class, EnumFacing.Axis.X, EnumFacing.Axis.Z);
  public static final PropertyBool RISE = PropertyBool.create("rise");
  public static final PropertyBool FALL = PropertyBool.create("fall");
  public static final PropertyBool LEGL = PropertyBool.create("legl");
  public static final PropertyBool LEGR = PropertyBool.create("legr");

  private static final AxisAlignedBB BOX_X = new AxisAlignedBB(0, 0, 0.4, 1, 1, 0.6);
  private static final AxisAlignedBB BOX_Z = new AxisAlignedBB(0.4, 0, 0, 0.6, 1, 1);
  private static final ThreadLocal<String> PENDING = new ThreadLocal<>();

  private final String registryName;

  public BlockTowerBrace(String registryName) {
    super(stash(registryName), SoundType.METAL, "pickaxe", 0, 1.5F, 6F, 0F, 0);
    this.registryName = registryName;
    PENDING.remove();
  }

  private static Material stash(String registryName) {
    PENDING.set(registryName);
    return Material.IRON;
  }

  @Override
  public String getBlockRegistryName() {
    return registryName != null ? registryName : PENDING.get();
  }

  @Override
  @Nonnull
  protected BlockStateContainer createBlockState() {
    return new CsmBlockStateContainer(this, AXIS, RISE, FALL, LEGL, LEGR);
  }

  /** The direction the plane's "right" is along: east along x, south along z. */
  static EnumFacing right(EnumFacing.Axis axis) {
    return axis == EnumFacing.Axis.X ? EnumFacing.EAST : EnumFacing.SOUTH;
  }

  private boolean brace(IBlockAccess world, BlockPos at, EnumFacing.Axis axis) {
    IBlockState s = world.getBlockState(at);
    return s.getBlock() == this && s.getValue(AXIS) == axis;
  }

  /** Whether a tower leg is at {@code at}: what a rod or a strut reaches into. */
  static boolean leg(IBlockAccess world, BlockPos at) {
    Block b = world.getBlockState(at).getBlock();
    return b instanceof BlockTowerColumn
        && "water_tower_leg".equals(((BlockTowerColumn) b).getBlockRegistryName());
  }

  @Override
  @Nonnull
  @SuppressWarnings("deprecation")
  public IBlockState getActualState(@Nonnull IBlockState state, IBlockAccess world,
      BlockPos pos) {
    EnumFacing.Axis axis = state.getValue(AXIS);
    EnumFacing r = right(axis);
    EnumFacing l = r.getOpposite();
    boolean rise = brace(world, pos.offset(r).up(), axis) || brace(world, pos.offset(l).down(),
        axis);
    boolean fall = brace(world, pos.offset(l).up(), axis) || brace(world, pos.offset(r).down(),
        axis);
    if (!rise && !fall) {
      rise = true;
    }
    return state.withProperty(RISE, rise).withProperty(FALL, fall)
        .withProperty(LEGL, leg(world, pos.offset(l))).withProperty(LEGR, leg(world,
            pos.offset(r)));
  }

  @Override
  @Nonnull
  @SuppressWarnings("deprecation")
  public IBlockState getStateFromMeta(int meta) {
    return getDefaultState().withProperty(AXIS, (meta & 1) == 0 ? EnumFacing.Axis.X
        : EnumFacing.Axis.Z);
  }

  @Override
  public int getMetaFromState(IBlockState state) {
    return state.getValue(AXIS) == EnumFacing.Axis.X ? 0 : 1;
  }

  /** The plane runs square to the way the player is looking. */
  @Override
  @Nonnull
  public IBlockState getStateForPlacement(World world, BlockPos pos, EnumFacing facing,
      float hitX, float hitY, float hitZ, int meta, EntityLivingBase placer) {
    return getDefaultState().withProperty(AXIS, placer.getHorizontalFacing().rotateY().getAxis());
  }

  @Override
  public AxisAlignedBB getBlockBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
    return state.getValue(AXIS) == EnumFacing.Axis.X ? BOX_X : BOX_Z;
  }

  /** A tie rod is thin enough to pass through. */
  @Nullable
  @Override
  public AxisAlignedBB getCollisionBoundingBox(IBlockState state, IBlockAccess source,
      BlockPos pos) {
    return NULL_AABB;
  }

  @Override
  public boolean isPassable(IBlockAccess world, BlockPos pos) {
    return true;
  }

  @Override
  @Nonnull
  @SuppressWarnings("deprecation")
  public BlockFaceShape getBlockFaceShape(IBlockAccess world, IBlockState state, BlockPos pos,
      EnumFacing face) {
    return BlockFaceShape.UNDEFINED;
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
    return BlockRenderLayer.SOLID;
  }
}

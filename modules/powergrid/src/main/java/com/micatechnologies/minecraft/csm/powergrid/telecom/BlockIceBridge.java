package com.micatechnologies.minecraft.csm.powergrid.telecom;

import com.micatechnologies.minecraft.csm.codeutils.AbstractBlock;
import com.micatechnologies.minecraft.csm.codeutils.CsmBlockStateContainer;
import com.micatechnologies.minecraft.csm.powergrid.water.BlockTowerColumn;
import com.micatechnologies.minecraft.csm.powergrid.water.IColumnJoint;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
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
 * A length of a cell site's cable ice bridge: the grating canopy that keeps falling ice off the
 * coax and fibre running from the equipment cabinets to the tower, over the cable tray that
 * carries them. It runs along {@link #AXIS} (stored: the way the placing player was looking) and
 * joins the ice bridge ahead and behind it on that axis into one run, the canopy's side rails
 * closed off only where the run stops. Where an ice bridge stanchion (a {@link BlockTowerColumn})
 * is below, {@link #POST} draws the stanchion on up to the cross beam under the tray.
 *
 * <p>Axis, ends and post: sixteen states, one model location. No tile entity, nothing ticks.</p>
 *
 * @since 2026.9
 */
public class BlockIceBridge extends AbstractBlock implements IColumnJoint {

  public static final PropertyEnum<EnumFacing.Axis> AXIS = PropertyEnum.create("axis",
      EnumFacing.Axis.class, EnumFacing.Axis.X, EnumFacing.Axis.Z);
  /** The run carries on at the model's north end (north for a z bridge, east for an x one). */
  public static final PropertyBool AHEAD = PropertyBool.create("ahead");
  /** The run carries on at the model's south end. */
  public static final PropertyBool BEHIND = PropertyBool.create("behind");
  /** A stanchion stands under this block. */
  public static final PropertyBool POST = PropertyBool.create("post");

  /** The canopy and the tray, facing along z: walked under, not into. */
  private static final AxisAlignedBB BOX_Z = new AxisAlignedBB(0.0625, 0.6875, 0, 0.9375, 1, 1);
  private static final AxisAlignedBB BOX_X = new AxisAlignedBB(0, 0.6875, 0.0625, 1, 1, 0.9375);

  private static final ThreadLocal<String> PENDING = new ThreadLocal<>();

  private final String registryName;

  public BlockIceBridge(String registryName) {
    super(stash(registryName), SoundType.METAL, "pickaxe", 1, 2F, 6F, 0F, 0);
    this.registryName = registryName;
    setDefaultState(getDefaultState().withProperty(AXIS, EnumFacing.Axis.Z)
        .withProperty(AHEAD, false).withProperty(BEHIND, false).withProperty(POST, false));
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
    return new CsmBlockStateContainer(this, AXIS, AHEAD, BEHIND, POST);
  }

  @Override
  @Nonnull
  public IBlockState getStateForPlacement(World world, BlockPos pos, EnumFacing facing,
      float hitX, float hitY, float hitZ, int meta, EntityLivingBase placer) {
    return getDefaultState().withProperty(AXIS, placer.getHorizontalFacing().getAxis());
  }

  @Override
  @Nonnull
  @SuppressWarnings("deprecation")
  public IBlockState getActualState(@Nonnull IBlockState state, IBlockAccess world,
      BlockPos pos) {
    EnumFacing.Axis axis = state.getValue(AXIS);
    EnumFacing ahead = axis == EnumFacing.Axis.Z ? EnumFacing.NORTH : EnumFacing.EAST;
    return state.withProperty(AHEAD, runs(world, pos.offset(ahead), axis))
        .withProperty(BEHIND, runs(world, pos.offset(ahead.getOpposite()), axis))
        .withProperty(POST, world.getBlockState(pos.down()).getBlock() instanceof BlockTowerColumn);
  }

  private boolean runs(IBlockAccess world, BlockPos at, EnumFacing.Axis axis) {
    IBlockState other = world.getBlockState(at);
    return other.getBlock() == this && other.getValue(AXIS) == axis;
  }

  @Override
  @Nonnull
  @SuppressWarnings("deprecation")
  public IBlockState getStateFromMeta(int meta) {
    return getDefaultState().withProperty(AXIS,
        (meta & 1) == 0 ? EnumFacing.Axis.Z : EnumFacing.Axis.X);
  }

  @Override
  public int getMetaFromState(IBlockState state) {
    return state.getValue(AXIS) == EnumFacing.Axis.Z ? 0 : 1;
  }

  @Override
  public AxisAlignedBB getBlockBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
    return state.getValue(AXIS) == EnumFacing.Axis.Z ? BOX_Z : BOX_X;
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

  /** Cutout: the canopy is bar grating. */
  @Nonnull
  @Override
  public BlockRenderLayer getBlockRenderLayer() {
    return BlockRenderLayer.CUTOUT;
  }
}

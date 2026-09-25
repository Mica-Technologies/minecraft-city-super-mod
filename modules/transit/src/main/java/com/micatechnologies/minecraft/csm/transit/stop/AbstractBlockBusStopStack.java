package com.micatechnologies.minecraft.csm.transit.stop;

import com.micatechnologies.minecraft.csm.codeutils.AbstractBlockRotatableNSEW;
import com.micatechnologies.minecraft.csm.codeutils.ICsmRoadSurfaceAware;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

/**
 * A block of a bus stop: a length of pole, or a length of pole with a fitting clamped to it. A
 * stop is a stack of these, and each reads the stack around it ({@link BusStopStack}): the cap
 * ({@link #CAP}) where nothing of the stack is above, the base ({@link #BASE}) where nothing is
 * below, and the settling of the whole stack onto the surface under its bottom block. All three
 * are actual state or render offsets; the metadata holds only the facing.
 *
 * <p>The box is given facing north, in sixteenths, like every CSM rotatable block, and moved down
 * by the settling offset so the block is picked and collided with where it is drawn.</p>
 *
 * @since 2026.9
 */
public abstract class AbstractBlockBusStopStack extends AbstractBlockRotatableNSEW
    implements ICsmRoadSurfaceAware {

  /** True where nothing of the stack is above: the pole wears its cap. */
  public static final PropertyBool CAP = PropertyBool.create("cap");

  /** True where nothing of the stack is below: the pole stands on its base. */
  public static final PropertyBool BASE = PropertyBool.create("base");

  private static final ThreadLocal<String> PENDING = new ThreadLocal<>();

  private final String registryName;
  private final AxisAlignedBB box;

  /**
   * Constructs a block of a stop.
   *
   * @param registryName its registry name
   * @param box          its box facing north, in sixteenths: {x0, y0, z0, x1, y1, z1}
   * @param lightLevel   the light it gives, 0 to 1
   */
  protected AbstractBlockBusStopStack(String registryName, double[] box, float lightLevel) {
    super(stash(registryName), SoundType.METAL, "pickaxe", 1, 2.0F, 6.0F, lightLevel, 0);
    this.registryName = registryName;
    this.box = new AxisAlignedBB(box[0] / 16.0, box[1] / 16.0, box[2] / 16.0, box[3] / 16.0,
        box[4] / 16.0, box[5] / 16.0);
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
  @SuppressWarnings("deprecation")
  public IBlockState getActualState(@Nonnull IBlockState state, IBlockAccess world,
      BlockPos pos) {
    return super.getActualState(state, world, pos)
        .withProperty(CAP, !BusStopStack.isPart(world, pos.up()))
        .withProperty(BASE, !BusStopStack.isPart(world, pos.down()));
  }

  @Override
  public AxisAlignedBB getBlockBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
    return box;
  }

  @Override
  public double getRoadSurfaceOffset(IBlockAccess world, BlockPos pos) {
    return BusStopStack.offsetAt(world, pos);
  }

  @Override
  @Nonnull
  public Block.EnumOffsetType getOffsetType() {
    return Block.EnumOffsetType.XYZ;
  }

  @Override
  @Nonnull
  @SuppressWarnings("deprecation")
  public Vec3d getOffset(IBlockState state, IBlockAccess world, BlockPos pos) {
    return getRoadSurfaceOffsetVector(world, pos);
  }

  @Override
  @Nonnull
  @SuppressWarnings("deprecation")
  public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
    AxisAlignedBB rotated = super.getBoundingBox(state, source, pos);
    double offset = getRoadSurfaceOffset(source, pos);
    return offset == 0.0 ? rotated : rotated.offset(0.0, offset, 0.0);
  }

  /**
   * Redraws the stack when a block of it, or the surface under it, changes: the cap, the base,
   * a fitting's pole style and the settling are all read from other blocks of the column.
   */
  @Override
  @SuppressWarnings("deprecation")
  public void neighborChanged(IBlockState state, World world, BlockPos pos, Block blockIn,
      BlockPos fromPos) {
    super.neighborChanged(state, world, pos, blockIn, fromPos);
    if (fromPos.getX() == pos.getX() && fromPos.getZ() == pos.getZ()) {
      world.markBlockRangeForRenderUpdate(pos.down(BusStopStack.REACH),
          pos.up(BusStopStack.REACH));
    }
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

  @Override
  @Nonnull
  public BlockRenderLayer getBlockRenderLayer() {
    return BlockRenderLayer.CUTOUT;
  }
}

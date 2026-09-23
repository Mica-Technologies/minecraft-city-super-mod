package com.micatechnologies.minecraft.csm.parks.amenities;

import com.micatechnologies.minecraft.csm.codeutils.AbstractBlockRotatableNSEW;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;

/**
 * A park bench or picnic table: one block of seat, placed side by side into a run. The frame
 * (legs and arms) is drawn only at the two ends of a run -- {@link #LEFT} and {@link #RIGHT} say
 * whether the same block, facing the same way, continues on that side -- so three placed in a
 * row are one long bench, not three benches pushed together.
 *
 * <p>Left and right are the sitter's, facing {@link #FACING}. Both are actual state, never
 * stored; the multipart blockstate picks the frame from them ({@code gen_park_amenities.py}).</p>
 *
 * @since 2026.9
 */
public class BlockParkBench extends AbstractBlockRotatableNSEW {

  /** The run continues to the sitter's left. */
  public static final PropertyBool LEFT = PropertyBool.create("left");
  /** The run continues to the sitter's right. */
  public static final PropertyBool RIGHT = PropertyBool.create("right");

  private static final ThreadLocal<String> PENDING = new ThreadLocal<>();

  private final String registryName;
  private final AxisAlignedBB box;

  /**
   * Constructs a bench.
   *
   * @param registryName its registry name
   * @param box          its box facing north, in sixteenths: {x0, y0, z0, x1, y1, z1}
   */
  public BlockParkBench(String registryName, int[] box) {
    super(stash(registryName), SoundType.WOOD, "axe", 0, 1.5F, 3.0F, 0.0F, 0);
    this.registryName = registryName;
    this.box = new AxisAlignedBB(box[0] / 16.0, box[1] / 16.0, box[2] / 16.0, box[3] / 16.0,
        box[4] / 16.0, box[5] / 16.0);
    PENDING.remove();
  }

  private static Material stash(String registryName) {
    PENDING.set(registryName);
    return Material.WOOD;
  }

  @Override
  public String getBlockRegistryName() {
    return registryName != null ? registryName : PENDING.get();
  }

  @Override
  @Nonnull
  protected BlockStateContainer createBlockState() {
    return new BlockStateContainer(this, FACING, LEFT, RIGHT);
  }

  @Override
  @Nonnull
  public IBlockState getActualState(@Nonnull IBlockState state, IBlockAccess world, BlockPos pos) {
    IBlockState s = super.getActualState(state, world, pos);
    EnumFacing facing = s.getValue(FACING);
    return s.withProperty(LEFT, continues(world, pos, facing, facing.rotateYCCW()))
        .withProperty(RIGHT, continues(world, pos, facing, facing.rotateY()));
  }

  private boolean continues(IBlockAccess world, BlockPos pos, EnumFacing facing,
      EnumFacing side) {
    IBlockState other = world.getBlockState(pos.offset(side));
    return other.getBlock() == this && other.getValue(FACING) == facing;
  }

  @Override
  public AxisAlignedBB getBlockBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
    return box;
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
  @Nonnull
  @SuppressWarnings("deprecation")
  public BlockFaceShape getBlockFaceShape(@Nonnull IBlockAccess world, @Nonnull IBlockState state,
      @Nonnull BlockPos pos, @Nonnull EnumFacing face) {
    return BlockFaceShape.UNDEFINED;
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

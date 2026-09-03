package com.micatechnologies.minecraft.csm.trafficaccessories.spanwire;

import com.micatechnologies.minecraft.csm.codeutils.AbstractBlockRotatableNSEW;
import com.micatechnologies.minecraft.csm.codeutils.ICsmNoSnowAccumulation;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

/**
 * The ground anchor a span wire's back-guy is dead ended on.
 *
 * <p>A span pulls hard and sideways on the pole it terminates at, so a real installation runs a
 * guy from just under the span's own anchor down to a rod driven into the ground behind the pole.
 * Without one a span reads as though it is holding itself up.
 *
 * <p>Deliberately <b>not</b> linked with the span wire tool. A guy is not part of a span's
 * topology — it does not change the cable, carry anything, or appear in the span's data — and
 * making the builder link one would be a second linking mode to learn for something with exactly
 * one sensible answer. Instead the span anchor finds the nearest guy anchor below it, and this
 * block tells nearby anchors to look again whenever one is placed or broken, so the guy appears
 * and disappears as the builder works.
 */
public class BlockSpanWireGuyAnchor extends AbstractBlockRotatableNSEW
    implements ICsmNoSnowAccumulation {

  /** A short stub of rod and its plate, standing proud of the ground. */
  private static final AxisAlignedBB BOUNDING_BOX =
      new AxisAlignedBB(0.375D, 0.0D, 0.375D, 0.625D, 0.5D, 0.625D);

  public BlockSpanWireGuyAnchor() {
    super(Material.IRON, SoundType.METAL, "pickaxe", 1, 2F, 10F, 0F, 0);
  }

  @Override
  public String getBlockRegistryName() {
    return "spanwireguyanchor";
  }

  @Override
  public AxisAlignedBB getBlockBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
    return BOUNDING_BOX;
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
    return BlockRenderLayer.CUTOUT;
  }

  /**
   * Breaking a ground anchor drops any guy running to it, so none is left drawn to a block that
   * has gone.
   *
   * <p>There is deliberately no {@code onBlockAdded} counterpart. Placing one of these does not
   * create a guy -- a guy exists only where a builder has paired it with an anchor using the
   * span wire tool.
   */
  @Override
  public void breakBlock(World worldIn, BlockPos pos, IBlockState state) {
    super.breakBlock(worldIn, pos, state);
    SpanWireGuyFinder.clearGuysTo(worldIn, pos);
  }
}

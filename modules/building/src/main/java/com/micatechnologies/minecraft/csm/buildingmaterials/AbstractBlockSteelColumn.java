package com.micatechnologies.minecraft.csm.buildingmaterials;

import com.micatechnologies.minecraft.csm.codeutils.CsmBlockStateContainer;
import javax.annotation.Nonnull;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;

/**
 * A wide-flange steel column: two flanges either side of a web, standing the height of
 * its block.
 *
 * <p>The shape nearly every column in a steel building actually is. Its axis says which
 * way the flanges face rather than which way it spans, which is the one thing a column
 * means differently from a beam.</p>
 *
 * <p>Shipped in two finishes, which differ only in their texture: red oxide shop primer, and
 * galvanized.</p>
 *
 * <p>A beam's flanges sit three sixteenths in from the top and bottom of its block, where a column
 * fills its own, so a column under (or on) a beam reaches into the beam's block to its flange:
 * {@link #TOP} and {@link #BOTTOM}, from the neighbours. Without them a column stopped short of the
 * beam it carries by a visible gap.</p>
 *
 * @version 1.0
 * @since 2026.9
 */
public abstract class AbstractBlockSteelColumn extends BlockFramingSpan {

  /** The member's own extent, as drawn: spanning north-south. */
  private static final AxisAlignedBB BOX = new AxisAlignedBB(4 / 16.0D, 0 / 16.0D, 5 / 16.0D,
          12 / 16.0D, 16 / 16.0D, 11 / 16.0D);

  /**
   * Constructs an {@link AbstractBlockSteelColumn}.
   *
   * @since 1.0
   */
  protected AbstractBlockSteelColumn() {
    super(Material.IRON, SoundType.METAL, "pickaxe", 1, 2.0F, 12F);
  }

  /** A steel beam is on top: the column reaches up to its bottom flange. */
  public static final PropertyBool TOP = PropertyBool.create("top");
  /** A steel beam is below: the column reaches down to its top flange. */
  public static final PropertyBool BOTTOM = PropertyBool.create("bottom");

  @Override
  protected AxisAlignedBB getSpanBoundingBox() {
    return BOX;
  }

  @Override
  @Nonnull
  protected BlockStateContainer createBlockState() {
    return new CsmBlockStateContainer(this, AXIS, TOP, BOTTOM);
  }

  @Override
  @Nonnull
  public IBlockState getActualState(@Nonnull IBlockState state, IBlockAccess world,
      BlockPos pos) {
    return state
        .withProperty(TOP, world.getBlockState(pos.up()).getBlock()
            instanceof AbstractBlockSteelBeam)
        .withProperty(BOTTOM, world.getBlockState(pos.down()).getBlock()
            instanceof AbstractBlockSteelBeam);
  }
}

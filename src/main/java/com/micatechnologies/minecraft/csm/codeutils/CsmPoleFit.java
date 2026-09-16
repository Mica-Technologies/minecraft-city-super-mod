package com.micatechnologies.minecraft.csm.codeutils;

import java.util.Arrays;
import net.minecraft.block.Block;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.properties.PropertyEnum;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.IStringSerializable;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;

/**
 * Which width of pole an accessory is meeting, for accessories whose geometry has to reach the
 * pole's skin.
 *
 * <p>CSM has three vertical traffic pole styles: the 12-across pole family, the 8-across thin
 * pole and the 6-across pedestal pole. A side-mounted arm -- a light mount, a tapered mast, a
 * mast arm curve -- is drawn to meet the pole behind it, and geometry cut for the widest pole
 * stops short of the other two by two and three sixteenths respectively, which reads as the arm
 * floating beside the pole. The arm adapts to the pole rather than the pole growing to meet the
 * arm: a block that implements {@link ICsmPoleFitted} carries {@link #PROPERTY} in its actual
 * state, resolved from {@link AbstractBlockTrafficPole#getPoleRadius()} of the block behind it,
 * and its blockstate swaps in a model whose pole end reaches that far. The models are generated
 * by {@code dev-env-utils/scripts/gen_pole_fit_models.py} from the large-pole model, and the
 * mast arm curve's generator emits its own three roots.
 *
 * <p>Anything that is not a pole -- a wall, a full block, air -- gets {@link #LARGE}: the model
 * cut for the widest pole is the one that reaches least far, so it is the safe one to draw
 * against a block whose surface is at the block face.
 *
 * <p>The radii here are the geometry the fitted models were generated for; the thresholds in
 * {@link #forPoleRadius} are the midpoints between them, so a pole of some other width gets
 * the nearest joint rather than none.
 *
 * @since 2026.9
 */
public enum CsmPoleFit implements IStringSerializable {
  /** The 12-across pole family ({@code trafficpolevertical*}). */
  LARGE(6.0D),
  /** The 8-across thin pole ({@code trafficpolehorizontal*} stood on end). */
  THIN(4.0D),
  /** The 6-across pedestal pole ({@code trafficpolepedestal*}). */
  PEDESTAL(3.0D);

  /**
   * The actual-state property a fitted block carries. Never stored in metadata.
   */
  public static final PropertyEnum<CsmPoleFit> PROPERTY = PropertyEnum.create("polefit",
      CsmPoleFit.class);

  private final double radius;

  CsmPoleFit(double radius) {
    this.radius = radius;
  }

  /**
   * The pole tube radius this fit's geometry was generated for, in sixteenths of a block.
   *
   * @return the radius
   */
  public double getRadius() {
    return radius;
  }

  @Override
  public String getName() {
    return name().toLowerCase();
  }

  /**
   * The fit for a pole of the given tube radius.
   *
   * @param radius the pole's tube radius in sixteenths of a block, as
   *               {@link AbstractBlockTrafficPole#getPoleRadius()} reports it
   *
   * @return the fit whose generated geometry is nearest that radius
   */
  public static CsmPoleFit forPoleRadius(double radius) {
    if (radius >= (LARGE.radius + THIN.radius) / 2.0D) {
      return LARGE;
    }
    if (radius >= (THIN.radius + PEDESTAL.radius) / 2.0D) {
      return THIN;
    }
    return PEDESTAL;
  }

  /**
   * The fit for whatever stands behind an accessory.
   *
   * <p>"Behind" is the opposite of the accessory's facing: the pole family's placement rule is
   * that an accessory reaches away from the face it was placed on, so the pole is at
   * {@code pos.offset(facing.getOpposite())}.
   *
   * @param world  the world
   * @param pos    the accessory's position
   * @param facing the direction the accessory reaches
   *
   * @return the fit for the block behind it; {@link #LARGE} for anything that is not a pole
   */
  public static CsmPoleFit behind(IBlockAccess world, BlockPos pos, EnumFacing facing) {
    Block behind = world.getBlockState(pos.offset(facing.getOpposite())).getBlock();
    if (behind instanceof AbstractBlockTrafficPole) {
      return forPoleRadius(((AbstractBlockTrafficPole) behind).getPoleRadius());
    }
    return LARGE;
  }

  /**
   * The properties a block's state container should be built with: the ones given, plus
   * {@link #PROPERTY} when the block is {@link ICsmPoleFitted}.
   *
   * <p>Called from {@code createBlockState}, which runs inside the {@link Block} constructor,
   * before any subclass field exists. The block's <em>class</em> is known by then, which is why
   * opting in is a marker interface and not a field.
   *
   * @param block the block being constructed
   * @param base  the properties it always has
   *
   * @return the properties for its state container
   */
  public static IProperty<?>[] properties(Block block, IProperty<?>... base) {
    if (!(block instanceof ICsmPoleFitted)) {
      return base;
    }
    IProperty<?>[] out = Arrays.copyOf(base, base.length + 1);
    out[base.length] = PROPERTY;
    return out;
  }

  /**
   * Resolves {@link #PROPERTY} on an actual state, for a block that carries it.
   *
   * @param state  the actual state so far
   * @param world  the world
   * @param pos    the block's position
   * @param facing the direction the block reaches
   *
   * @return the state with the fit set, or the state unchanged for a block that is not fitted
   */
  public static IBlockState apply(IBlockState state, IBlockAccess world, BlockPos pos,
      EnumFacing facing) {
    if (!(state.getBlock() instanceof ICsmPoleFitted)
        || !state.getPropertyKeys().contains(PROPERTY)) {
      return state;
    }
    return state.withProperty(PROPERTY, behind(world, pos, facing));
  }
}

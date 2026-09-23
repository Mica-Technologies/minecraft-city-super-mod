package com.micatechnologies.minecraft.csm.trafficaccessories;

import com.micatechnologies.minecraft.csm.codeutils.AbstractBlockTrafficPole;
import com.micatechnologies.minecraft.csm.codeutils.ICsmPostTopFixture;
import com.micatechnologies.minecraft.csm.codeutils.ICsmTrafficPoleIgnored;
import javax.annotation.Nonnull;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyEnum;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.IStringSerializable;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import org.jetbrains.annotations.NotNull;

/**
 * A concrete traffic pole: a spun round pole or a precast octagonal one, in the 12-across thick
 * width or the 8-across thin width.
 *
 * <p>A member of the {@link AbstractBlockTrafficPole} family like the painted thin and thick
 * poles: one block type stacked to any height, growing a mount stub toward anything mountable on
 * its four flanks, and sized for the pole-fitted arms through {@link #getPoleRadius()}. What it
 * adds is the pedestal pole's idea (see {@link BlockTrafficPolePedestal}): each block decides what
 * the two ends of its shaft show, from its neighbours alone, so there is no separate base, middle
 * or top block to place:
 *
 * <ul>
 *   <li>nothing, where another concrete pole continues the shaft, or where it runs into some
 *       other solid block;</li>
 *   <li>a cap, wherever the shaft ends in the open;</li>
 *   <li>a collar and tenon, under a post-top fixture ({@link ICsmPostTopFixture});</li>
 *   <li>a stepped plinth, at the bottom of a vertical pole standing on the ground.</li>
 * </ul>
 *
 * <p>Those four pieces replace the hand-placed base, middle and top blocks the concrete poles
 * used to be ({@code rcpb}, {@code rcpb2}, {@code rcpm}, {@code rcpt}, {@code ocpb},
 * {@code ocpm}, {@code ocpt}), which now retire into this block.
 *
 * <p>The end properties are named in model space, exactly as the pedestal pole's are, and for
 * the same reason: see {@link BlockTrafficPolePedestal#endDirection}. Unlike the pedestal pole,
 * each end model carries its own half of the shaft, because the cap and the octagon's tapered
 * tenon are narrower than the shaft and need it to stop short. Geometry, blockstates and lang come
 * from {@code dev-env-utils/scripts/gen_concrete_poles.py}.
 *
 * @since 2026.9
 */
public class BlockTrafficPoleConcrete extends AbstractBlockTrafficPole {

  /**
   * What one end of the shaft shows. Values are lower-cased for the blockstate.
   */
  public enum EndStyle implements IStringSerializable {
    /** Plain shaft end: another concrete pole continues it, or it runs into a solid block. */
    NONE,
    /** The cap: the shaft ends in the open. */
    CAP,
    /** The collar and tenon: a post-top fixture sits on this end. */
    TENON,
    /** The stepped plinth: the bottom of a vertical pole standing on the ground. */
    BASE;

    @Override
    public String getName() {
      return name().toLowerCase();
    }
  }

  /** What the model-north end of the shaft shows. */
  public static final PropertyEnum<EndStyle> END_NORTH = PropertyEnum.create("endn",
      EndStyle.class);

  /** What the model-south end of the shaft shows. */
  public static final PropertyEnum<EndStyle> END_SOUTH = PropertyEnum.create("ends",
      EndStyle.class);

  /**
   * Carries the constructor arguments past {@code super()}: {@link #getBlockRegistryName()} is
   * called from the superclass constructor, before this class's fields exist. Same trick as
   * {@link BlockTrafficPolePedestal}.
   */
  private static final ThreadLocal<Object[]> PENDING = new ThreadLocal<>();

  private final String registryName;
  private final double poleRadius;
  private final AxisAlignedBB shaftBoundingBox;
  private final AxisAlignedBB baseBoundingBox;

  /**
   * Constructs a concrete pole block.
   *
   * @param registryName the block's registry name
   * @param poleRadius   the shaft's radius in sixteenths of a block: 6 for the thick pole, 4
   *                     for the thin one ({@code d / 2} in {@code gen_concrete_poles.py})
   * @param baseRadius   the plinth's widest radius, likewise ({@code 8} and {@code 6})
   */
  public BlockTrafficPoleConcrete(String registryName, double poleRadius, double baseRadius) {
    super(stash(registryName));
    this.registryName = registryName;
    this.poleRadius = poleRadius;
    this.shaftBoundingBox = centredBox(poleRadius);
    this.baseBoundingBox = centredBox(baseRadius);
    PENDING.remove();
  }

  private static Material stash(String registryName) {
    PENDING.set(new Object[]{registryName});
    return Material.ROCK;
  }

  private static AxisAlignedBB centredBox(double radius) {
    double lo = (8.0 - radius) / 16.0;
    double hi = (8.0 + radius) / 16.0;
    return new AxisAlignedBB(lo, lo, 0.0, hi, hi, 1.0);
  }

  @Override
  public String getBlockRegistryName() {
    if (registryName != null) {
      return registryName;
    }
    return (String) PENDING.get()[0];
  }

  @Override
  public Class<?>[] getIgnoreBlock() {
    return null;
  }

  /**
   * Silver: the finish a post light takes on a concrete pole. A concrete pole has no paint to
   * lend its fixture, and the fixture on one is bare metal.
   */
  @Override
  public TRAFFIC_POLE_COLOR getTrafficPoleColor() {
    return TRAFFIC_POLE_COLOR.SILVER;
  }

  @Override
  public double getPoleRadius() {
    return poleRadius;
  }

  @Override
  public AxisAlignedBB getBlockBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
    if (state.getPropertyKeys().contains(END_NORTH)
        && (state.getValue(END_NORTH) == EndStyle.BASE
        || state.getValue(END_SOUTH) == EndStyle.BASE)) {
      return baseBoundingBox;
    }
    return shaftBoundingBox;
  }

  @Override
  @Nonnull
  protected BlockStateContainer createBlockState() {
    return new BlockStateContainer(this, FACING, MOUNT_EAST, MOUNT_WEST, MOUNT_UP, MOUNT_DOWN,
        END_NORTH, END_SOUTH);
  }

  /**
   * The flank mounts come from the superclass; this adds what each end of the shaft shows.
   */
  @Override
  @SuppressWarnings("deprecation")
  public @NotNull IBlockState getActualState(@NotNull IBlockState state,
      @NotNull IBlockAccess worldIn, @NotNull BlockPos pos) {
    IBlockState actual = super.getActualState(state, worldIn, pos);
    EnumFacing facing = actual.getValue(FACING);
    return actual
        .withProperty(END_NORTH, endStyle(worldIn, pos, facing,
            BlockTrafficPolePedestal.endDirection(facing, true)))
        .withProperty(END_SOUTH, endStyle(worldIn, pos, facing,
            BlockTrafficPolePedestal.endDirection(facing, false)));
  }

  /**
   * Decides what one end of the shaft shows from the block beyond it. The pedestal pole's rules,
   * in the same order, plus the tenon.
   *
   * @param worldIn the world
   * @param pos     the pole's position
   * @param facing  the pole's facing
   * @param end     the world direction of the end being decided
   *
   * @return what that end shows
   */
  private static EndStyle endStyle(IBlockAccess worldIn, BlockPos pos, EnumFacing facing,
      EnumFacing end) {
    BlockPos beyond = pos.offset(end);
    Block beyondBlock = worldIn.getBlockState(beyond).getBlock();

    // Another concrete pole continues the shaft: seamless, whatever its profile or facing.
    if (beyondBlock instanceof BlockTrafficPoleConcrete) {
      return EndStyle.NONE;
    }

    // A post light slips over the tenon.
    if (beyondBlock instanceof ICsmPostTopFixture) {
      return EndStyle.TENON;
    }

    // Nothing worth abutting: air, plants, snow layers, water. The shaft ends in the open.
    if (worldIn.isAirBlock(beyond) || beyondBlock.isReplaceable(worldIn, beyond)
        || isNaturalClutter(beyondBlock)) {
      return EndStyle.CAP;
    }

    // Standing vertically on something that is not itself pole hardware: the plinth. Another
    // pole type below, or a CSM block that draws its own mounting hardware, meets it flush.
    if (end == EnumFacing.DOWN && facing.getAxis() == EnumFacing.Axis.Y
        && !(beyondBlock instanceof AbstractBlockTrafficPole)
        && !(beyondBlock instanceof ICsmTrafficPoleIgnored)) {
      return EndStyle.BASE;
    }
    return EndStyle.NONE;
  }

  /**
   * Whether a block is one of the vanilla natural/decorative blocks the pole family ignores for
   * mounting. An end against them still wears its cap. The CSM opt-out marker is left out on
   * purpose, as in {@link BlockTrafficPolePedestal}: a signal head on the pole top is solid
   * hardware the shaft should run straight into.
   *
   * @param block the block beyond the shaft end
   *
   * @return {@code true} if the block is vanilla clutter the end should ignore
   */
  private static boolean isNaturalClutter(Block block) {
    Class<?> blockClass = block.getClass();
    for (Class<?> ignored : IGNORE_BLOCK) {
      if (ignored != ICsmTrafficPoleIgnored.class && ignored.isAssignableFrom(blockClass)) {
        return true;
      }
    }
    return false;
  }
}

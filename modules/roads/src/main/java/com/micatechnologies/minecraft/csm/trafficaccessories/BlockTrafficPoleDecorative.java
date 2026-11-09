package com.micatechnologies.minecraft.csm.trafficaccessories;

import com.micatechnologies.minecraft.csm.codeutils.AbstractBlockTrafficPole;
import com.micatechnologies.minecraft.csm.codeutils.CsmBlockStateContainer;
import com.micatechnologies.minecraft.csm.codeutils.ICsmPostTopFixture;
import com.micatechnologies.minecraft.csm.codeutils.ICsmTrafficPoleBystander;
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
 * A decorative or historic street light pole: a cast, fluted or tiled column such as the Los
 * Angeles electroliers, the Anaheim Resort Area pole or a reproduction cast-iron post.
 *
 * <p>Built exactly as the concrete pole is ({@link BlockTrafficPoleConcrete}): one block type per
 * design, stacked to any height, each block deciding from its neighbours alone what the two ends
 * of its shaft show, so there is no separate base, middle or top block to place:
 *
 * <ul>
 *   <li>nothing, where another block of the same design continues the shaft, or where it runs
 *       into some other solid block;</li>
 *   <li>the design's cap (a finial, a ball, a cast ring), wherever the shaft ends in the
 *       open;</li>
 *   <li>the design's capital, under a post-top fixture ({@link ICsmPostTopFixture}): the
 *       lanterns and globes in the Lighting module sit on it;</li>
 *   <li>the design's cast base, at the bottom of a vertical pole standing on the ground.</li>
 * </ul>
 *
 * <p>The shaft is drawn by every block; the ends are submodels named in model space, for the
 * reason {@link BlockTrafficPolePedestal#endDirection} gives. The flanks behave like the rest of
 * the pole family: anything mountable beside the shaft grows a clamp toward it, and
 * {@link #getPoleRadius()} lets the pole-fitted arms and the luminaire arms meet the shaft. Every
 * asset comes from {@code dev-env-utils/scripts/gen_decorative_poles.py}.
 *
 * @since 2026.11
 */
public class BlockTrafficPoleDecorative extends AbstractBlockTrafficPole {

  /**
   * What one end of the shaft shows. Values are lower-cased for the blockstate.
   */
  public enum EndStyle implements IStringSerializable {
    /** Plain shaft end: the same design continues it, or it runs into a solid block. */
    NONE,
    /** The design's cap: the shaft ends in the open. */
    CAP,
    /** The design's capital: a post-top fixture sits on this end. */
    TENON,
    /** The design's cast base: the bottom of a vertical pole standing on the ground. */
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
   * Carries the registry name past {@code super()}, which calls {@link #getBlockRegistryName()}
   * before this class's fields exist. Same trick as {@link BlockTrafficPoleConcrete}.
   */
  private static final ThreadLocal<String> PENDING = new ThreadLocal<>();

  private final String registryName;
  private final double poleRadius;
  private final TRAFFIC_POLE_COLOR color;
  private final AxisAlignedBB shaftBoundingBox;
  private final AxisAlignedBB baseBoundingBox;

  /**
   * Constructs a decorative pole block.
   *
   * @param registryName the block's registry name
   * @param poleRadius   the radius the arms fit to, in sixteenths of a block (the generator's
   *                     {@code fit_radius}: 3 for a slim post, 4 for an LA electrolier)
   * @param shaftRadius  the drawn shaft's radius, for the bounding box
   * @param baseRadius   the cast base's widest half-width, likewise
   * @param color        the finish a pole-coloured post light takes on this pole
   */
  public BlockTrafficPoleDecorative(String registryName, double poleRadius, double shaftRadius,
      double baseRadius, TRAFFIC_POLE_COLOR color) {
    super(stash(registryName));
    this.registryName = registryName;
    this.poleRadius = poleRadius;
    this.color = color;
    this.shaftBoundingBox = centredBox(shaftRadius);
    this.baseBoundingBox = centredBox(baseRadius);
    PENDING.remove();
  }

  private static Material stash(String registryName) {
    PENDING.set(registryName);
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
    return PENDING.get();
  }

  @Override
  public Class<?>[] getIgnoreBlock() {
    return null;
  }

  @Override
  public TRAFFIC_POLE_COLOR getTrafficPoleColor() {
    return color;
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
    return new CsmBlockStateContainer(this, FACING, MOUNT_EAST, MOUNT_WEST, MOUNT_UP, MOUNT_DOWN,
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
   * Decides what one end of the shaft shows from the block beyond it: the concrete pole's rules,
   * in the same order.
   *
   * @param worldIn the world
   * @param pos     the pole's position
   * @param facing  the pole's facing
   * @param end     the world direction of the end being decided
   *
   * @return what that end shows
   */
  private EndStyle endStyle(IBlockAccess worldIn, BlockPos pos, EnumFacing facing,
      EnumFacing end) {
    BlockPos beyond = pos.offset(end);
    Block beyondBlock = worldIn.getBlockState(beyond).getBlock();

    // The same design continues the shaft: seamless. Another design meets it with a plain end
    // too (the rule for any solid block), so the two read as stacked castings.
    if (beyondBlock instanceof BlockTrafficPoleDecorative) {
      return EndStyle.NONE;
    }

    // A lantern or globe sits on the capital.
    if (beyondBlock instanceof ICsmPostTopFixture) {
      return EndStyle.TENON;
    }

    // Nothing worth abutting: air, plants, snow layers, water. The shaft ends in the open.
    if (worldIn.isAirBlock(beyond) || beyondBlock.isReplaceable(worldIn, beyond)
        || isNaturalClutter(beyondBlock)) {
      return EndStyle.CAP;
    }

    // Standing vertically on something that is not itself pole hardware: the cast base.
    if (end == EnumFacing.DOWN && facing.getAxis() == EnumFacing.Axis.Y
        && !(beyondBlock instanceof AbstractBlockTrafficPole)
        && !(beyondBlock instanceof ICsmTrafficPoleIgnored)) {
      return EndStyle.BASE;
    }
    return EndStyle.NONE;
  }

  /**
   * Whether a block is clutter an end should ignore: the vanilla natural blocks the pole family
   * ignores for mounting, and CSM's bystanders, as for {@link BlockTrafficPoleConcrete}.
   *
   * @param block the block beyond the shaft end
   *
   * @return {@code true} if the end should wear its cap against it
   */
  private static boolean isNaturalClutter(Block block) {
    if (block instanceof ICsmTrafficPoleBystander) {
      return true;
    }
    Class<?> blockClass = block.getClass();
    for (Class<?> ignored : IGNORE_BLOCK) {
      if (ignored != ICsmTrafficPoleIgnored.class && ignored.isAssignableFrom(blockClass)) {
        return true;
      }
    }
    return false;
  }
}

package com.micatechnologies.minecraft.csm.trafficaccessories;

import com.micatechnologies.minecraft.csm.CsmRegistry;
import com.micatechnologies.minecraft.csm.codeutils.AbstractBlockTrafficPole;
import com.micatechnologies.minecraft.csm.codeutils.CsmBlockStateContainer;
import com.micatechnologies.minecraft.csm.codeutils.ICsmModelIgnoredProperties;
import com.micatechnologies.minecraft.csm.codeutils.ICsmPostTopFixture;
import com.micatechnologies.minecraft.csm.codeutils.ICsmRetiringBlock;
import com.micatechnologies.minecraft.csm.codeutils.ICsmTrafficPoleBystander;
import com.micatechnologies.minecraft.csm.codeutils.ICsmTrafficPoleIgnored;
import javax.annotation.Nonnull;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.IProperty;
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
 *   <li>a cap, wherever the shaft ends in the open, or the finial if its owner chose one
 *       ({@link #TOP});</li>
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
public class BlockTrafficPoleConcrete extends AbstractBlockTrafficPole
    implements ICsmModelIgnoredProperties {

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
   * What the owner picked for the top of the pole. Stored, and only an input to
   * {@link #getActualState}: it never reaches a model location
   * ({@link ICsmModelIgnoredProperties}).
   */
  public enum TopStyle implements IStringSerializable {
    /** The cap in the open, the tenon under a post-top fixture. */
    AUTO("Flat cap"),
    /**
     * The finial in the open as well: the collar and spigot of the round pole, the stepped
     * taper of the octagon. What the legacy top pieces ({@code rcpt}, {@code ocpt}) always
     * showed, and what they keep when they retire into this block.
     */
    FINIAL("Finial");

    private final String friendlyName;

    TopStyle(String friendlyName) {
      this.friendlyName = friendlyName;
    }

    @Override
    public String getName() {
      return name().toLowerCase();
    }

    /**
     * The name the Street Light Configuration Tool reports.
     *
     * @return the display name
     */
    public String getFriendlyName() {
      return friendlyName;
    }
  }

  /**
   * The owner's choice of top. It applies to the end a finial can stand on: the upper end of an
   * upright pole, and the end a pole lying on its side faces.
   */
  public static final PropertyEnum<TopStyle> TOP = PropertyEnum.create("top", TopStyle.class);

  /**
   * How many values of the combined choice number each top takes. Three, not one, so the
   * metadata an upright pole with a finial is stored under leaves room below it.
   */
  private static final int CHOICES_PER_TOP = 3;

  /** First metadata value of an upright pole with a choice other than the defaults. */
  private static final int META_UPRIGHT_CHOICES = 6;

  /** First metadata value of a pole lying on its side with a finial. */
  private static final int META_SIDEWAYS_FINIAL = 11;

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
    return new CsmBlockStateContainer(this, FACING, MOUNT_EAST, MOUNT_WEST, MOUNT_UP, MOUNT_DOWN,
        END_NORTH, END_SOUTH, TOP);
  }

  @Override
  public IProperty<?>[] getModelIgnoredProperties() {
    return new IProperty<?>[]{TOP};
  }

  /**
   * The facing in the six values the pole always stored, so every pole already in a world reads
   * back as it was, and the owner's choices in the ten values that were spare:
   *
   * <ul>
   *   <li>0-5: the facing, every choice at its default;</li>
   *   <li>6-10: upright, with one of the five combinations of choices other than the
   *       defaults;</li>
   *   <li>11-14: lying on its side (north, south, west, east), with a finial.</li>
   * </ul>
   *
   * <p>An upright pole with a choice is always stored facing {@code UP}. A pole facing
   * {@code DOWN} draws the same shaft, and the choices are about the world's up and down, not
   * the model's ends, so nothing is lost by standing it the usual way round.
   */
  @Override
  public int getMetaFromState(IBlockState state) {
    EnumFacing facing = state.getValue(FACING);
    int choice = choiceIndex(state);
    if (choice == 0) {
      return facing.getIndex();
    }
    if (facing.getAxis() == EnumFacing.Axis.Y) {
      return META_UPRIGHT_CHOICES + choice - 1;
    }
    return state.getValue(TOP) == TopStyle.FINIAL
        ? META_SIDEWAYS_FINIAL + facing.getIndex() - 2 : facing.getIndex();
  }

  @Override
  @Nonnull
  public IBlockState getStateFromMeta(int meta) {
    if (meta >= META_UPRIGHT_CHOICES && meta < META_SIDEWAYS_FINIAL) {
      return withChoiceIndex(getDefaultState().withProperty(FACING, EnumFacing.UP),
          meta - META_UPRIGHT_CHOICES + 1);
    }
    if (meta >= META_SIDEWAYS_FINIAL && meta < META_SIDEWAYS_FINIAL + 4) {
      return getDefaultState()
          .withProperty(FACING, EnumFacing.byIndex(meta - META_SIDEWAYS_FINIAL + 2))
          .withProperty(TOP, TopStyle.FINIAL);
    }
    return super.getStateFromMeta(meta);
  }

  /** The owner's choices as one number, 0 for every default. */
  private static int choiceIndex(IBlockState state) {
    return state.getValue(TOP).ordinal() * CHOICES_PER_TOP;
  }

  private static IBlockState withChoiceIndex(IBlockState state, int choice) {
    TopStyle[] tops = TopStyle.values();
    return state.withProperty(TOP, tops[Math.min(choice / CHOICES_PER_TOP, tops.length - 1)]);
  }

  /**
   * The world direction of the end the {@link #TOP} choice is about: up for an upright pole,
   * whichever way round it was placed, and the facing for one on its side.
   *
   * @param facing the pole's facing
   *
   * @return the end a finial can stand on
   */
  public static EnumFacing topEnd(EnumFacing facing) {
    return facing.getAxis() == EnumFacing.Axis.Y ? EnumFacing.UP : facing;
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
    TopStyle top = actual.getValue(TOP);
    return actual
        .withProperty(END_NORTH, endStyle(worldIn, pos, facing,
            BlockTrafficPolePedestal.endDirection(facing, true), top))
        .withProperty(END_SOUTH, endStyle(worldIn, pos, facing,
            BlockTrafficPolePedestal.endDirection(facing, false), top));
  }

  /**
   * Decides what one end of the shaft shows from the block beyond it. The pedestal pole's rules,
   * in the same order, plus the tenon.
   *
   * @param worldIn the world
   * @param pos     the pole's position
   * @param facing  the pole's facing
   * @param end     the world direction of the end being decided
   * @param top     the owner's choice of top
   *
   * @return what that end shows
   */
  private static EndStyle endStyle(IBlockAccess worldIn, BlockPos pos, EnumFacing facing,
      EnumFacing end, TopStyle top) {
    BlockPos beyond = pos.offset(end);
    Block beyondBlock = worldIn.getBlockState(beyond).getBlock();

    // Another concrete pole continues the shaft: seamless, whatever its profile or facing.
    if (isConcretePole(beyondBlock)) {
      return EndStyle.NONE;
    }

    // A post light slips over the tenon.
    if (beyondBlock instanceof ICsmPostTopFixture) {
      return EndStyle.TENON;
    }

    // Nothing worth abutting: air, plants, snow layers, water. The shaft ends in the open.
    if (worldIn.isAirBlock(beyond) || beyondBlock.isReplaceable(worldIn, beyond)
        || isNaturalClutter(beyondBlock)) {
      // The finial is the tenon's model: a legacy top piece was one casting whether or not a
      // light sat on it.
      return top == TopStyle.FINIAL && end == topEnd(facing) ? EndStyle.TENON : EndStyle.CAP;
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
   * Whether a block continues the shaft: a concrete pole, or a legacy piece that has not yet
   * retired into one. Without the second, a pole stacked on a legacy base drew a plinth of its
   * own on top of it, and one under a legacy top a cap inside it, until the random tick that
   * converts the piece came round.
   *
   * @param block the block beyond the shaft end
   *
   * @return {@code true} if the shaft runs on into it
   */
  private static boolean isConcretePole(Block block) {
    if (block instanceof BlockTrafficPoleConcrete) {
      return true;
    }
    if (block instanceof ICsmRetiringBlock) {
      String replacement = ((ICsmRetiringBlock) block).getReplacementBlockId();
      return replacement != null
          && CsmRegistry.getBlock(replacement) instanceof BlockTrafficPoleConcrete;
    }
    return false;
  }

  /**
   * Whether a block is one of the vanilla natural/decorative blocks the pole family ignores for
   * mounting. An end against them still wears its cap. The CSM opt-out marker is left out on
   * purpose, as in {@link BlockTrafficPolePedestal}: a signal head on the pole top is solid
   * hardware the shaft should run straight into. A {@link ICsmTrafficPoleBystander} is not
   * hardware, so it counts as clutter, as vanilla leaves and carpet do.
   *
   * @param block the block beyond the shaft end
   *
   * @return {@code true} if the block is clutter the end should ignore
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

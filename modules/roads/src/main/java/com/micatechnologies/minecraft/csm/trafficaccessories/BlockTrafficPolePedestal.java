package com.micatechnologies.minecraft.csm.trafficaccessories;

import com.micatechnologies.minecraft.csm.codeutils.AbstractBlockTrafficPole;
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
 * The pedestal traffic pole: the slim aluminium post that carries a pedestrian signal, a push
 * button or a small sign at the kerb. A 4.5-inch tube threaded into a cast, tapered pedestal
 * base with an access door, and a domed cap on top.
 *
 * <p>It is a member of the {@link AbstractBlockTrafficPole} family and behaves like the thin
 * and thick poles: one block type, stacked to whatever height is wanted, growing a mount
 * bracket toward anything mountable on its four flanks. What it adds is that each block also
 * decides what the two <em>ends</em> of its tube show, from its neighbours alone:
 *
 * <ul>
 *   <li>nothing, where another pedestal pole continues the tube (so a stack reads as one
 *       seamless post, with no coupling at every block joint);</li>
 *   <li>the pedestal base, at the bottom of a vertical pole standing on the ground;</li>
 *   <li>the domed cap, wherever the tube ends in the open;</li>
 *   <li>nothing again where the tube runs into some other solid block, such as a signal head
 *       sitting on the post top.</li>
 * </ul>
 *
 * <p>So adding a block on top of a pole moves the cap up with it, and breaking the bottom block
 * takes the base away: the pole is as dynamic as the rest of the family, and there is no
 * separate base or cap block to place.
 *
 * <p><b>The end properties are named in model space.</b> {@link #END_NORTH} and
 * {@link #END_SOUTH} are the two ends of the tube before the {@code facing} rotation stands it
 * up; the blockstate attaches a fixed submodel to each value and the facing rotation carries it
 * to the right world end. That is what keeps the blockstate a plain per-property Forge
 * blockstate. Which model end is on the ground depends on the facing: the vanilla {@code x}
 * rotation puts model north at the facing direction, so a pole facing {@code UP} (the usual
 * result of placing on the ground) stands on its model-south end and a pole facing {@code DOWN}
 * on its model-north end. {@link #endDirection} is the one place that knowledge lives.
 *
 * <p>Geometry, blockstates and the tab and lang fragments come from
 * {@code dev-env-utils/scripts/gen_pedestal_pole.py}; nothing under
 * {@code shared_models/pedestalpole_*} is hand edited.
 */
public class BlockTrafficPolePedestal extends AbstractBlockTrafficPole {

  /**
   * What one end of the tube shows. Values are lower-cased for the blockstate.
   */
  public enum EndStyle implements IStringSerializable {
    /** Plain tube end: another pedestal pole continues it, or it runs into a solid block. */
    NONE,
    /** The domed cap: the tube ends in the open. */
    CAP,
    /** The pedestal base: the bottom of a vertical pole standing on the ground. */
    BASE;

    @Override
    public String getName() {
      return name().toLowerCase();
    }
  }

  /** What the model-north end of the tube shows. */
  public static final PropertyEnum<EndStyle> END_NORTH = PropertyEnum.create("endn",
      EndStyle.class);

  /** What the model-south end of the tube shows. */
  public static final PropertyEnum<EndStyle> END_SOUTH = PropertyEnum.create("ends",
      EndStyle.class);

  /**
   * Carries the constructor arguments past {@code super()}: {@link #getBlockRegistryName()} is
   * called from the superclass constructor, before this class's fields exist. Same trick, and
   * same reason, as {@link BlockTrafficPoleMastArmCurve}.
   */
  private static final ThreadLocal<Object[]> PENDING = new ThreadLocal<>();

  /** The tube: 6 across, centred, full block length along model Z. */
  private static final AxisAlignedBB SHAFT_BOUNDING_BOX =
      new AxisAlignedBB(5 / 16.0, 5 / 16.0, 0.0, 11 / 16.0, 11 / 16.0, 1.0);

  /** The tube plus the pedestal base's footprint, for the block that carries the base. */
  private static final AxisAlignedBB BASE_BOUNDING_BOX =
      new AxisAlignedBB(1.8 / 16.0, 1.8 / 16.0, 0.0, 14.2 / 16.0, 14.2 / 16.0, 1.0);

  private final String registryName;
  private final TRAFFIC_POLE_COLOR color;

  /**
   * Constructs a pedestal pole block.
   *
   * @param registryName the block's registry name
   * @param color        the pole's finish, reported to accessories that inherit a pole colour
   */
  public BlockTrafficPolePedestal(String registryName, TRAFFIC_POLE_COLOR color) {
    super(stash(registryName, color));
    this.registryName = registryName;
    this.color = color;
    PENDING.remove();
  }

  private static Material stash(String registryName, TRAFFIC_POLE_COLOR color) {
    PENDING.set(new Object[]{registryName, color});
    return Material.ROCK;
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

  @Override
  public TRAFFIC_POLE_COLOR getTrafficPoleColor() {
    return color;
  }

  @Override
  public AxisAlignedBB getBlockBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
    if (state.getPropertyKeys().contains(END_NORTH)
        && (state.getValue(END_NORTH) == EndStyle.BASE
        || state.getValue(END_SOUTH) == EndStyle.BASE)) {
      return BASE_BOUNDING_BOX;
    }
    return SHAFT_BOUNDING_BOX;
  }

  @Override
  @Nonnull
  protected BlockStateContainer createBlockState() {
    return new BlockStateContainer(this, FACING, MOUNT_EAST, MOUNT_WEST, MOUNT_UP, MOUNT_DOWN,
        END_NORTH, END_SOUTH);
  }

  /**
   * The flank mounts come from the superclass; this adds what each end of the tube shows.
   */
  @Override
  @SuppressWarnings("deprecation")
  public @NotNull IBlockState getActualState(@NotNull IBlockState state,
      @NotNull IBlockAccess worldIn, @NotNull BlockPos pos) {
    IBlockState actual = super.getActualState(state, worldIn, pos);
    EnumFacing facing = actual.getValue(FACING);
    return actual
        .withProperty(END_NORTH, endStyle(worldIn, pos, facing, endDirection(facing, true)))
        .withProperty(END_SOUTH, endStyle(worldIn, pos, facing, endDirection(facing, false)));
  }

  /**
   * The world direction one end of the tube points in.
   *
   * <p>The blockstate's {@code facing} variants are the vanilla {@code x}/{@code y} rotations
   * that carry a north-facing model to each facing, so the model-north end of the tube always
   * points in the facing direction and the model-south end the opposite way.
   *
   * @param facing the block's facing
   * @param north  {@code true} for the model-north end, {@code false} for the model-south end
   *
   * @return the world direction that end points in
   */
  static EnumFacing endDirection(EnumFacing facing, boolean north) {
    return north ? facing : facing.getOpposite();
  }

  /**
   * Decides what one end of the tube shows from the block beyond it.
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
    IBlockState beyondState = worldIn.getBlockState(beyond);
    Block beyondBlock = beyondState.getBlock();

    // Another pedestal pole continues the tube: seamless, whatever its facing.
    if (beyondBlock instanceof BlockTrafficPolePedestal) {
      return EndStyle.NONE;
    }

    // Nothing worth abutting: air, plants, snow layers, water. The tube ends in the open.
    if (worldIn.isAirBlock(beyond) || beyondBlock.isReplaceable(worldIn, beyond)
        || isNaturalClutter(beyondBlock)) {
      return EndStyle.CAP;
    }

    // Standing vertically on something that is not itself pole hardware: the pedestal base.
    // Another pole type below (a thick pole, a pole base block) or a CSM block that draws its
    // own mounting hardware gets a plain end so the two meet flush.
    if (end == EnumFacing.DOWN && facing.getAxis() == EnumFacing.Axis.Y
        && !(beyondBlock instanceof AbstractBlockTrafficPole)
        && !(beyondBlock instanceof ICsmTrafficPoleIgnored)) {
      return EndStyle.BASE;
    }
    return EndStyle.NONE;
  }

  /**
   * Whether a block is one of the vanilla natural/decorative blocks the pole family ignores
   * for mounting (leaves, vines, torches, rails, carpets, ...). Those do not stop a tube
   * ending in the open either, so an end against them still wears its cap. CSM's own opt-out
   * marker, {@link ICsmTrafficPoleIgnored}, is deliberately not included: a signal head sat on
   * the post top is solid hardware the tube should run straight into.
   *
   * @param block the block beyond the tube end
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

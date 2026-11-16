package com.micatechnologies.minecraft.csm.trafficsignals;

import com.micatechnologies.minecraft.csm.codeutils.AbstractBlockTrafficPole;
import com.micatechnologies.minecraft.csm.codeutils.CsmBlockStateContainer;
import com.micatechnologies.minecraft.csm.codeutils.CsmPoleFit;
import com.micatechnologies.minecraft.csm.codeutils.ICsmNoSnowAccumulation;
import com.micatechnologies.minecraft.csm.codeutils.ICsmTrafficPoleIgnored;
import com.micatechnologies.minecraft.csm.codeutils.RotationUtils;
import com.micatechnologies.minecraft.csm.trafficaccessories.IPreemptPoleMounted;
import java.util.function.Predicate;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyDirection;
import net.minecraft.block.properties.PropertyEnum;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.IStringSerializable;
import net.minecraft.util.Mirror;
import net.minecraft.util.Rotation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

/**
 * The traffic signal controller in a slim pole-mounted cabinet (#360), for a tight sidewalk with
 * no room for a ground cabinet: an NYC-style upright box with a door, louvres and a police panel,
 * clamped to the side of a traffic pole or bolted flat to a wall, about a block or two above the
 * walk.
 * <p>
 * It is {@link BlockTrafficSignalController} in everything but its model, box and placement: the
 * same tile entity, so the Signal Link Tool, every mode, the ADVANCED programming GUI, the NBT,
 * redstone power and the Fabricator cost are the controller's own.
 * <p>
 * Click it onto the side of a pole and it stands in the cell in front of the pole, facing away
 * from it ({@link IPreemptPoleMounted#placementFacing}), on two arms that clamp to the pole with
 * a band each. Click it onto a wall and it sits flat against the wall on two rails. Which one it
 * draws is {@link #MOUNT}, an actual-state property resolved from the block behind it (the pole
 * or wall it was clicked onto) and never stored: a traffic pole of any family gives the band
 * sized for that pole's width ({@link CsmPoleFit}), anything else (a wall, a full block, air)
 * gives the wall rails. The cabinet body, and so the box, is the same in every case.
 * <p>
 * Power: redstone into its own cell, as every controller; and, on a pole, redstone at the foot
 * of that pole ({@link #isControllerPowered}), the way a real cabinet's feed comes up the pole
 * from a service under the sidewalk. A redstone block or a lever under the walk below the pole's
 * base runs every cabinet on that pole.
 * <p>
 * Metadata: the horizontal facing's index in bits 0-1 (south 0, west 1, north 2, east 3, as
 * every NSEW block), {@link #POWERED} in bit 2.
 *
 * @since 2026.11
 */
public class BlockTrafficSignalControllerPole extends BlockTrafficSignalController
    implements ICsmTrafficPoleIgnored, ICsmNoSnowAccumulation {

  /** The way the cabinet's door faces; the pole or wall is behind it. */
  public static final PropertyDirection FACING =
      PropertyDirection.create("facing", EnumFacing.Plane.HORIZONTAL);

  /** What the cabinet is mounted on, from the block behind it (actual state only). */
  public static final PropertyEnum<Mount> MOUNT = PropertyEnum.create("mount", Mount.class);

  /**
   * The cabinet, its rain cap and the rails or arms up to the back of its cell, facing north
   * (from {@code gen_pole_controller.py}).
   */
  /** How far down a pole the power lookup walks to find its foot. */
  static final int MAX_POLE_WALK = 32;

  private static final AxisAlignedBB NORTH_BOUNDING_BOX =
      new AxisAlignedBB(2.6 / 16.0, 0.5 / 16.0, 7.4 / 16.0, 13.4 / 16.0, 1.0, 1.0);

  /**
   * Carries the registry name past {@code super()}: {@link #getBlockRegistryName()} is called
   * from the superclass constructor, before this class's fields exist. Same trick as
   * {@code BlockTrafficPolePedestal}.
   */
  private static final ThreadLocal<String> PENDING = new ThreadLocal<>();

  private final String registryName;

  /**
   * A pole-mounted controller cabinet.
   *
   * @param registryName the block's registry name ({@code signalcontroller_pole_<colour>})
   */
  public BlockTrafficSignalControllerPole(String registryName) {
    super(stash(registryName), SoundType.METAL, 0);
    this.registryName = registryName;
    PENDING.remove();
    setDefaultState(blockState.getBaseState().withProperty(FACING, EnumFacing.NORTH)
        .withProperty(POWERED, false).withProperty(MOUNT, Mount.WALL));
  }

  private static Material stash(String registryName) {
    PENDING.set(registryName);
    return Material.IRON;
  }

  @Override
  public String getBlockRegistryName() {
    if (registryName != null) {
      return registryName;
    }
    return PENDING.get();
  }

  // --- state ---------------------------------------------------------------------------------

  @Override
  protected BlockStateContainer createBlockState() {
    return new CsmBlockStateContainer(this, FACING, POWERED, MOUNT);
  }

  @Override
  @Nonnull
  public IBlockState getStateFromMeta(int meta) {
    return getDefaultState().withProperty(FACING, EnumFacing.byHorizontalIndex(meta & 3))
        .withProperty(POWERED, (meta & 4) != 0);
  }

  @Override
  public int getMetaFromState(IBlockState state) {
    return state.getValue(FACING).getHorizontalIndex() | (state.getValue(POWERED) ? 4 : 0);
  }

  @Override
  @Nonnull
  @SuppressWarnings("deprecation")
  public IBlockState getActualState(IBlockState state, IBlockAccess worldIn, BlockPos pos) {
    return state.withProperty(MOUNT, mountBehind(worldIn, pos, state.getValue(FACING)));
  }

  /**
   * What the cabinet facing {@code facing} at {@code pos} is mounted on. Reads only the block
   * behind it, never its box or actual state (a pole's actual state asks its neighbours in turn).
   *
   * @param world  the world
   * @param pos    the cabinet's position
   * @param facing the way its door faces
   *
   * @return the band for the pole behind it, or {@link Mount#WALL} for anything else
   */
  public static Mount mountBehind(IBlockAccess world, BlockPos pos, EnumFacing facing) {
    Block behind = world.getBlockState(pos.offset(facing.getOpposite())).getBlock();
    if (!(behind instanceof AbstractBlockTrafficPole)) {
      return Mount.WALL;
    }
    switch (CsmPoleFit.forPoleRadius(((AbstractBlockTrafficPole) behind).getPoleRadius())) {
      case PEDESTAL:
        return Mount.PEDESTAL;
      case THIN:
        return Mount.THIN;
      default:
        return Mount.LARGE;
    }
  }

  @Override
  @Nonnull
  public IBlockState getStateForPlacement(World worldIn, BlockPos pos, EnumFacing facing,
      float hitX, float hitY, float hitZ, int meta, EntityLivingBase placer) {
    return getDefaultState().withProperty(FACING,
        IPreemptPoleMounted.placementFacing(facing, placer));
  }

  @Override
  @Nonnull
  @SuppressWarnings("deprecation")
  public IBlockState withRotation(IBlockState state, Rotation rot) {
    return state.withProperty(FACING, rot.rotate(state.getValue(FACING)));
  }

  @Override
  @Nonnull
  @SuppressWarnings("deprecation")
  public IBlockState withMirror(IBlockState state, Mirror mirrorIn) {
    return state.withRotation(mirrorIn.toRotation(state.getValue(FACING)));
  }

  // --- power ---------------------------------------------------------------------------------

  /**
   * Powered by redstone into the cabinet's own cell, or, when it is mounted on a traffic pole, by
   * redstone into the block under that pole's foot. Asked by the tile entity about once a second
   * and on a neighbour change, never per frame.
   */
  @Override
  public boolean isControllerPowered(World world, BlockPos pos, IBlockState state) {
    if (world.isBlockPowered(pos)) {
      return true;
    }
    EnumFacing facing = state.getBlock() == this ? state.getValue(FACING) : EnumFacing.NORTH;
    BlockPos foot = poleFoot(pos.offset(facing.getOpposite()),
        p -> world.getBlockState(p).getBlock() instanceof AbstractBlockTrafficPole,
        MAX_POLE_WALK);
    return foot != null && world.isBlockPowered(foot.down());
  }

  /**
   * The foot of the pole column that {@code start} is part of: the lowest pole block reached by
   * walking down from it, at most {@code maxWalk} blocks, never below y 1.
   *
   * @param start   the block behind the cabinet
   * @param isPole  whether a position holds a pole block
   * @param maxWalk how many blocks down to look at most
   *
   * @return the foot, or {@code null} when {@code start} is not a pole
   */
  @Nullable
  static BlockPos poleFoot(BlockPos start, Predicate<BlockPos> isPole, int maxWalk) {
    if (!isPole.test(start)) {
      return null;
    }
    BlockPos foot = start;
    for (int i = 0; i < maxWalk && foot.getY() > 1 && isPole.test(foot.down()); i++) {
      foot = foot.down();
    }
    return foot;
  }

  // --- shape ---------------------------------------------------------------------------------

  /**
   * No face is solid: it is a slim cabinet standing off its pole or wall, so fences and panes do
   * not join it and nothing hangs off its sides (and no snow settles on its cap).
   */
  @Override
  @Nonnull
  @SuppressWarnings("deprecation")
  public BlockFaceShape getBlockFaceShape(IBlockAccess worldIn, IBlockState state, BlockPos pos,
      EnumFacing face) {
    return BlockFaceShape.UNDEFINED;
  }

  @Override
  public AxisAlignedBB getBlockBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
    EnumFacing facing = state.getPropertyKeys().contains(FACING) ? state.getValue(FACING)
        : EnumFacing.NORTH;
    return RotationUtils.rotateBoundingBoxByFacing(NORTH_BOUNDING_BOX, facing);
  }

  @Override
  public boolean getBlockIsOpaqueCube(IBlockState state) {
    return false;
  }

  @Override
  public boolean getBlockIsFullCube(IBlockState state) {
    return false;
  }

  @Nonnull
  @Override
  public BlockRenderLayer getBlockRenderLayer() {
    return BlockRenderLayer.CUTOUT_MIPPED;
  }

  /**
   * What a pole or wall cabinet is mounted on: a wall (or anything that is not a traffic pole),
   * or a pole of one of the three widths, each with its own band.
   *
   * @since 2026.11
   */
  public enum Mount implements IStringSerializable {
    /** Flat against a wall, a full block or nothing: two rails. */
    WALL,
    /** The 12-across pole family (and anything wider): arms to a band hidden in the pole. */
    LARGE,
    /** The 8-across thin pole: arms to a band round it. */
    THIN,
    /** The 6-across pedestal pole: arms to a band round it. */
    PEDESTAL;

    @Override
    public String getName() {
      return name().toLowerCase(java.util.Locale.ROOT);
    }
  }
}

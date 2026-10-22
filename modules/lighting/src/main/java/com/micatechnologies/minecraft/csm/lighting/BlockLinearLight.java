package com.micatechnologies.minecraft.csm.lighting;

import com.micatechnologies.minecraft.csm.codeutils.CsmBlockStateContainer;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nonnull;
import net.minecraft.block.Block;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

/**
 * A segment of a linear LED light, the long slim fixture of a modern open office: one block a
 * segment, joined into one fixture along a run, flush to the ceiling or hung below it on aircraft
 * cables.
 *
 * <p>Stored: the facing and the light state, as every bright light (metadata is full). Everything
 * else is actual state:</p>
 * <ul>
 *   <li>{@code front} / {@code back}: whether the run goes on past this segment towards its facing
 *   and away from it, to a segment on the same axis at the same height; an end cap is drawn where
 *   it does not.</li>
 *   <li>{@code hung}: flush against the ceiling when the block above has a solid underside;
 *   otherwise the fixture hangs lower in its block on cables.</li>
 *   <li>{@code cable}: a hung segment's cable up out of the top of its block, at a run's ends,
 *   every fourth block along it, and under a {@link BlockLinearLightHanger}, which carries the
 *   cable up to the ceiling a block at a time.</li>
 * </ul>
 *
 * <p>A run is one fixture: a click switches every segment of it, and redstone powering any
 * segment powers the whole run. The models come from
 * {@code dev-env-utils/scripts/gen_linear_lights.py}.</p>
 *
 * @since 2026.10
 */
public class BlockLinearLight extends AbstractBrightLight {

  public static final PropertyBool FRONT = PropertyBool.create("front");
  public static final PropertyBool BACK = PropertyBool.create("back");
  public static final PropertyBool HUNG = PropertyBool.create("hung");
  public static final PropertyBool CABLE = PropertyBool.create("cable");

  /** The most segments a click or a redstone change walks along a run, each way. */
  private static final int MAX_RUN = 64;

  private static final AxisAlignedBB BOX_SURFACE = new AxisAlignedBB(6.5 / 16, 13 / 16.0, 0,
      9.5 / 16, 1, 1);
  private static final AxisAlignedBB BOX_HUNG = new AxisAlignedBB(6.5 / 16, 10 / 16.0, 0,
      9.5 / 16, 13 / 16.0, 1);

  private static final ThreadLocal<String> PENDING_REGISTRY_NAME = new ThreadLocal<>();

  private final String registryName;

  public BlockLinearLight(String registryName) {
    this(initRegistryName(registryName), registryName);
  }

  private BlockLinearLight(Void ignored, String registryName) {
    this.registryName = registryName;
    setDefaultState(getDefaultState().withProperty(FRONT, false).withProperty(BACK, false)
        .withProperty(HUNG, false).withProperty(CABLE, false));
  }

  private static Void initRegistryName(String name) {
    PENDING_REGISTRY_NAME.set(name);
    return null;
  }

  @Override
  public String getBlockRegistryName() {
    return registryName != null ? registryName : PENDING_REGISTRY_NAME.get();
  }

  @Override
  public int getBrightLightXOffset() {
    return 0;
  }

  @Nonnull
  @Override
  protected BlockStateContainer createBlockState() {
    return new CsmBlockStateContainer(this, FACING, STATE, FRONT, BACK, HUNG, CABLE);
  }

  /** Whether a segment at {@code pos} hangs: the block above gives it no solid underside. */
  private static boolean hung(IBlockAccess world, BlockPos pos) {
    BlockPos up = pos.up();
    IBlockState above = world.getBlockState(up);
    return above.getBlockFaceShape(world, up, EnumFacing.DOWN) != BlockFaceShape.SOLID;
  }

  /** Whether the segment beside {@code pos} towards {@code side} continues this one's run. */
  private static boolean joins(IBlockAccess world, BlockPos pos, IBlockState state,
      EnumFacing side, boolean hung) {
    BlockPos next = pos.offset(side);
    IBlockState other = world.getBlockState(next);
    return other.getBlock() instanceof BlockLinearLight
        && other.getValue(FACING).getAxis() == state.getValue(FACING).getAxis()
        && hung(world, next) == hung;
  }

  @Nonnull
  @Override
  @SuppressWarnings("deprecation")
  public IBlockState getActualState(@Nonnull IBlockState state, @Nonnull IBlockAccess world,
      @Nonnull BlockPos pos) {
    EnumFacing facing = state.getValue(FACING);
    boolean hung = hung(world, pos);
    boolean front = joins(world, pos, state, facing, hung);
    boolean back = joins(world, pos, state, facing.getOpposite(), hung);
    int along = facing.getAxis() == EnumFacing.Axis.X ? pos.getX() : pos.getZ();
    boolean cable = hung && (!front || !back || Math.floorMod(along, 4) == 0
        || world.getBlockState(pos.up()).getBlock() instanceof BlockLinearLightHanger);
    return state.withProperty(FRONT, front).withProperty(BACK, back).withProperty(HUNG, hung)
        .withProperty(CABLE, cable);
  }

  @Override
  public AxisAlignedBB getBlockBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
    return hung(source, pos) ? BOX_HUNG : BOX_SURFACE;
  }

  @Nonnull
  @Override
  public BlockFaceShape getBlockFaceShape(IBlockAccess worldIn, IBlockState state, BlockPos pos,
      EnumFacing face) {
    return BlockFaceShape.UNDEFINED;
  }

  /** Every segment of the run through {@code pos}, this one first, at most MAX_RUN each way. */
  private static List<BlockPos> run(World world, BlockPos pos, IBlockState state) {
    List<BlockPos> out = new ArrayList<>();
    out.add(pos);
    boolean hung = hung(world, pos);
    EnumFacing facing = state.getValue(FACING);
    for (EnumFacing side : new EnumFacing[]{facing, facing.getOpposite()}) {
      BlockPos at = pos;
      for (int i = 0; i < MAX_RUN && joins(world, at, state, side, hung); i++) {
        at = at.offset(side);
        out.add(at);
      }
    }
    return out;
  }

  /** Puts {@code stateValue} on every other segment of the run, with its light-up air. */
  private void spread(World world, List<BlockPos> run, int stateValue) {
    boolean lit = stateValue == 1 || stateValue == 3;
    for (int i = 1; i < run.size(); i++) {
      BlockPos at = run.get(i);
      IBlockState other = world.getBlockState(at);
      if (other.getBlock() instanceof BlockLinearLight && other.getValue(STATE) != stateValue) {
        world.setBlockState(at, other.withProperty(STATE, stateValue), 3);
        ((BlockLinearLight) other.getBlock()).handleAirLightBlock(lit, world, at);
      }
    }
  }

  @Override
  public boolean onBlockActivated(World world, BlockPos pos, IBlockState state,
      EntityPlayer player, EnumHand hand, EnumFacing facing, float hitX, float hitY,
      float hitZ) {
    boolean result = super.onBlockActivated(world, pos, state, player, hand, facing, hitX, hitY,
        hitZ);
    if (!world.isRemote) {
      spread(world, run(world, pos, state), world.getBlockState(pos).getValue(STATE));
    }
    return result;
  }

  @Override
  public void neighborChanged(IBlockState state, World world, BlockPos pos, Block blockIn,
      BlockPos fromPos) {
    if (world.isRemote) {
      return;
    }
    int current = state.getValue(STATE);
    // redstone matters only in the automatic states (0 off, 1 on)
    if (current > 1) {
      return;
    }
    List<BlockPos> run = run(world, pos, state);
    boolean powered = false;
    for (BlockPos at : run) {
      if (world.isBlockPowered(at)) {
        powered = true;
        break;
      }
    }
    int wanted = powered ? 1 : 0;
    if (current != wanted) {
      world.setBlockState(pos, state.withProperty(STATE, wanted), 3);
      handleAirLightBlock(powered, world, pos);
    }
    for (int i = 1; i < run.size(); i++) {
      BlockPos at = run.get(i);
      IBlockState other = world.getBlockState(at);
      if (other.getBlock() instanceof BlockLinearLight && other.getValue(STATE) <= 1
          && other.getValue(STATE) != wanted) {
        world.setBlockState(at, other.withProperty(STATE, wanted), 3);
        ((BlockLinearLight) other.getBlock()).handleAirLightBlock(powered, world, at);
      }
    }
  }
}

package com.micatechnologies.minecraft.csm.venues.ballpark;

import com.micatechnologies.minecraft.csm.codeutils.CsmBlockStateContainer;
import com.micatechnologies.minecraft.csm.venues.BlockVenue;
import javax.annotation.Nonnull;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

/**
 * A net panel a block square: the backstop behind home plate, the netting over the dugouts and
 * along the stands, the walls of a batting cage ({@code ballpark_net}, a panel down the middle of
 * its cell; {@code ballpark_net_diagonal}, across it corner to corner, joining straight panels
 * diagonally beside it as a dasher corner does). Panels side by side are one net, stacked as
 * high as wanted; the top panel draws the head cable ({@code top}, actual state: nothing of the
 * net above it). A straight run stands a galvanised pole at each end ({@code end_l},
 * {@code end_r}) and, with the {@code pole} bit, one at a panel's left edge: put one every three
 * or four blocks along a long run. The batting cage's roof is {@code ballpark_net_roof}.
 *
 * <p><b>Metadata</b>: bits 0-1 the facing (S 0, W 1, N 2, E 3): a straight panel runs across it;
 * a diagonal one faces between it and the next facing clockwise (the dasher corner's
 * convention: facing north looks north-east); bit 2 a pole (a straight panel's left edge looking
 * the way it faces, a diagonal one's middle). Placed by hand a panel faces the placer, with a
 * pole if placed while sneaking.</p>
 *
 * @since 2026.10
 */
public class BlockBallparkNet extends BlockVenue {

  /** A pole at the left edge (straight) or middle (diagonal). Stored, bit 2. */
  public static final PropertyBool POLE = PropertyBool.create("pole");
  /** Nothing of the net above: the head cable. Actual. */
  public static final PropertyBool TOP = PropertyBool.create("top");
  /** No panel to the left (straight). Actual. */
  public static final PropertyBool END_L = PropertyBool.create("end_l");
  /** No panel to the right (straight). Actual. */
  public static final PropertyBool END_R = PropertyBool.create("end_r");

  private static final ThreadLocal<Boolean> PENDING = new ThreadLocal<>();
  private static final AxisAlignedBB STRAIGHT = new AxisAlignedBB(0, 0, 7.5 / 16.0, 1, 1,
      8.5 / 16.0);
  private static final AxisAlignedBB DIAGONAL = new AxisAlignedBB(0.25, 0, 0.25, 0.75, 1, 0.75);

  private final boolean diagonal;

  /**
   * Constructs a net panel.
   *
   * @param registryName its registry name
   * @param diagonal     across its cell corner to corner rather than down the middle
   */
  public BlockBallparkNet(String registryName, boolean diagonal) {
    super(stash(registryName, diagonal), Material.CLOTH, SoundType.CLOTH, 0.8F);
    this.diagonal = diagonal;
    PENDING.remove();
    IBlockState s = blockState.getBaseState().withProperty(FACING, EnumFacing.NORTH)
        .withProperty(POLE, false).withProperty(TOP, true);
    if (!diagonal) {
      s = s.withProperty(END_L, true).withProperty(END_R, true);
    }
    setDefaultState(s);
  }

  private static String stash(String registryName, boolean diagonal) {
    PENDING.set(diagonal);
    return registryName;
  }

  @Override
  @Nonnull
  protected BlockStateContainer createBlockState() {
    if (Boolean.TRUE.equals(PENDING.get())) {
      return new CsmBlockStateContainer(this, FACING, POLE, TOP);
    }
    return new CsmBlockStateContainer(this, FACING, POLE, TOP, END_L, END_R);
  }

  @Override
  @Nonnull
  public IBlockState getStateFromMeta(int meta) {
    return getDefaultState().withProperty(FACING, EnumFacing.byHorizontalIndex(meta & 3))
        .withProperty(POLE, (meta & 4) != 0);
  }

  @Override
  public int getMetaFromState(IBlockState state) {
    return state.getValue(FACING).getHorizontalIndex() | (state.getValue(POLE) ? 4 : 0);
  }

  @Override
  @Nonnull
  public IBlockState getStateForPlacement(World world, BlockPos pos, EnumFacing facing,
      float hitX, float hitY, float hitZ, int meta, EntityLivingBase placer) {
    return super.getStateForPlacement(world, pos, facing, hitX, hitY, hitZ, meta, placer)
        .withProperty(POLE, placer.isSneaking());
  }

  @Override
  public int damageDropped(IBlockState state) {
    return 0;
  }

  /** Whether the block at {@code pos} is a straight panel running the same way. */
  private boolean panel(IBlockAccess world, BlockPos pos, EnumFacing facing) {
    IBlockState s = world.getBlockState(pos);
    return s.getBlock() instanceof BlockBallparkNet
        && (((BlockBallparkNet) s.getBlock()).diagonal
        || s.getValue(FACING).getAxis() == facing.getAxis());
  }

  @Override
  @Nonnull
  @SuppressWarnings("deprecation")
  public IBlockState getActualState(@Nonnull IBlockState state, IBlockAccess world,
      BlockPos pos) {
    boolean top = !(world.getBlockState(pos.up()).getBlock() instanceof BlockBallparkNet);
    IBlockState s = state.withProperty(TOP, top);
    if (diagonal) {
      return s;
    }
    EnumFacing f = state.getValue(FACING);
    return s.withProperty(END_L, !panel(world, pos.offset(f.rotateYCCW()), f))
        .withProperty(END_R, !panel(world, pos.offset(f.rotateY()), f));
  }

  @Override
  public AxisAlignedBB getBlockBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
    return diagonal ? DIAGONAL : STRAIGHT;
  }
}

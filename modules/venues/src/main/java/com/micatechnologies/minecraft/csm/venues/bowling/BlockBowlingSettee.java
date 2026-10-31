package com.micatechnologies.minecraft.csm.venues.bowling;

import com.micatechnologies.minecraft.csm.codeutils.CsmBlockStateContainer;
import com.micatechnologies.minecraft.csm.venues.BlockVenue;
import com.micatechnologies.minecraft.csm.venues.seating.StadiumSeating;
import javax.annotation.Nonnull;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyBool;
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
 * The bowlers' settee: a padded vinyl bench with a high back on a chrome plinth. Straight pieces
 * join side by side into a run and draw an arm where it stops ({@code end_l}, {@code end_r},
 * actual state); a corner piece turns the run a quarter round a curve, so straights and corners
 * make the horseshoe a pair of lanes sits in. Any settee piece beside a straight counts as joined.
 * A click sits on it (Core's seat entity).
 *
 * <p><b>Metadata</b>: the facing (S 0, W 1, N 2, E 3), the way a sitter faces. A corner facing
 * north joins a straight to its east and one to its north (facing east): the curve's inside is the
 * block's north-east corner.</p>
 *
 * @since 2026.10
 */
public class BlockBowlingSettee extends BlockVenue {

  /** The run stops on the sitter's left. Actual. */
  public static final PropertyBool END_L = PropertyBool.create("end_l");
  /** The run stops on the sitter's right. Actual. */
  public static final PropertyBool END_R = PropertyBool.create("end_r");

  private static final ThreadLocal<Boolean> PENDING = new ThreadLocal<>();
  private static final double SEAT_TOP = 7.5 / 16.0;
  private static final AxisAlignedBB STRAIGHT = new AxisAlignedBB(0, 0, 3 / 16.0, 1, 16 / 16.0,
      1);
  private static final AxisAlignedBB CORNER = new AxisAlignedBB(0, 0, 0, 1, 16 / 16.0, 1);

  private final boolean corner;

  /**
   * Constructs a settee piece.
   *
   * @param registryName its registry name
   * @param corner       the corner rather than a straight
   */
  public BlockBowlingSettee(String registryName, boolean corner) {
    super(stash(registryName, corner), Material.CLOTH, SoundType.CLOTH, 1.0F);
    this.corner = corner;
    PENDING.remove();
    IBlockState s = blockState.getBaseState().withProperty(FACING, EnumFacing.NORTH);
    if (!corner) {
      s = s.withProperty(END_L, true).withProperty(END_R, true);
    }
    setDefaultState(s);
  }

  private static String stash(String registryName, boolean corner) {
    PENDING.set(corner);
    return registryName;
  }

  @Override
  @Nonnull
  protected BlockStateContainer createBlockState() {
    if (Boolean.TRUE.equals(PENDING.get())) {
      return new CsmBlockStateContainer(this, FACING);
    }
    return new CsmBlockStateContainer(this, FACING, END_L, END_R);
  }

  @Override
  @Nonnull
  public IBlockState getStateFromMeta(int meta) {
    return getDefaultState().withProperty(FACING, EnumFacing.byHorizontalIndex(meta & 3));
  }

  @Override
  public int getMetaFromState(IBlockState state) {
    return state.getValue(FACING).getHorizontalIndex();
  }

  @Override
  @Nonnull
  @SuppressWarnings("deprecation")
  public IBlockState getActualState(@Nonnull IBlockState state, IBlockAccess world,
      BlockPos pos) {
    if (corner) {
      return state;
    }
    EnumFacing f = state.getValue(FACING);
    return state.withProperty(END_L, !settee(world, pos.offset(f.rotateYCCW())))
        .withProperty(END_R, !settee(world, pos.offset(f.rotateY())));
  }

  private static boolean settee(IBlockAccess world, BlockPos pos) {
    return world.getBlockState(pos).getBlock() instanceof BlockBowlingSettee;
  }

  @Override
  public AxisAlignedBB getBlockBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
    return corner ? CORNER : STRAIGHT;
  }

  @Override
  public boolean onBlockActivated(World world, BlockPos pos, IBlockState state,
      EntityPlayer player, EnumHand hand, EnumFacing side, float hitX, float hitY, float hitZ) {
    if (player.isSneaking()) {
      return false;
    }
    if (!world.isRemote) {
      EnumFacing f = state.getValue(FACING);
      StadiumSeating.sit(world, pos, f, SEAT_TOP, 0.0, player);
    }
    return true;
  }
}

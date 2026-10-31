package com.micatechnologies.minecraft.csm.venues.arena;

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
 * An event floor piece that joins side by side into a run: the player bench (a padded seat on
 * a steel frame, no back: the boards behind the players are the back), the penalty box bench (a
 * padded seat and back) and the scorer's table (a counter whose front is an LED panel, an
 * animated texture, so nothing ticks); and a ballpark's dugout bench, dugout rail and dugout
 * canopy; a football team bench and a soccer substitutes' shelter. Each block draws its own
 * length; where the run stops it
 * draws an end frame or end panel ({@code end_l}, {@code end_r}, actual state).
 *
 * <p><b>Metadata</b>: the facing (S 0, W 1, N 2, E 3), the way a sitter or scorer faces: the
 * ice or court. A bench is sat on with a click (Core's seat entity, one sitter a block).</p>
 *
 * @since 2026.10
 */
public class BlockArenaRun extends BlockVenue {

  /** Which piece. */
  public enum Kind {
    /** The player bench: a seat, no back. */
    PLAYER_BENCH(BlockVenueBox.of(0, 0, 3, 16, 9, 13), 9.0 / 16.0),
    /** The penalty box bench: a seat and back. */
    PENALTY_BENCH(BlockVenueBox.of(0, 0, 3, 16, 17.5, 15.5), 9.0 / 16.0),
    /** The scorer's table. */
    SCORERS_TABLE(BlockVenueBox.of(0, 0, 0.5, 16, 14, 15), -1),
    /** A ballpark's dugout bench: aluminium slats on a green frame, with a back. */
    DUGOUT_BENCH(BlockVenueBox.of(0, 0, 3, 16, 18, 15.5), 9.25 / 16.0),
    /** The dugout's front rail: a padded top rail on posts, to lean on. */
    DUGOUT_RAIL(BlockVenueBox.of(0, 0, 6, 16, 18, 10), -1),
    /** The dugout's canopy: a roof deck at the top of its cell with a padded front fascia. */
    DUGOUT_CANOPY(BlockVenueBox.of(0, 11, 0, 16, 16, 16), -1),
    /** A football sideline's team bench: aluminium planks and a backrest. */
    TEAM_BENCH(BlockVenueBox.of(0, 0, 4, 16, 17, 14.75), 8.5 / 16.0),
    /** A soccer substitutes' shelter: seats against a back panel under a clear roof, two blocks
     * tall (the roof has no collision). */
    SUB_SHELTER(BlockVenueBox.of(0, 0, 6, 16, 16, 16), 9.0 / 16.0);

    final AxisAlignedBB box;
    final double seatTop;

    Kind(AxisAlignedBB box, double seatTop) {
      this.box = box;
      this.seatTop = seatTop;
    }
  }

  /** The run stops on the left (the sitter's). Actual. */
  public static final PropertyBool END_L = PropertyBool.create("end_l");
  /** The run stops on the right. Actual. */
  public static final PropertyBool END_R = PropertyBool.create("end_r");

  private final Kind kind;

  /**
   * Constructs a run piece.
   *
   * @param registryName its registry name
   * @param kind         which piece
   */
  public BlockArenaRun(String registryName, Kind kind) {
    super(registryName, kind == Kind.SCORERS_TABLE ? Material.WOOD : Material.IRON,
        kind == Kind.SCORERS_TABLE ? SoundType.WOOD : SoundType.METAL, 1.5F);
    this.kind = kind;
    setDefaultState(blockState.getBaseState().withProperty(FACING, EnumFacing.NORTH)
        .withProperty(END_L, true).withProperty(END_R, true));
  }

  @Override
  @Nonnull
  protected BlockStateContainer createBlockState() {
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
    EnumFacing f = state.getValue(FACING);
    return state.withProperty(END_L, !same(world, pos.offset(f.rotateYCCW()), f))
        .withProperty(END_R, !same(world, pos.offset(f.rotateY()), f));
  }

  private boolean same(IBlockAccess world, BlockPos pos, EnumFacing facing) {
    IBlockState s = world.getBlockState(pos);
    return s.getBlock() == this && s.getValue(FACING) == facing;
  }

  @Override
  public AxisAlignedBB getBlockBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
    return kind.box;
  }

  @Override
  public boolean onBlockActivated(World world, BlockPos pos, IBlockState state,
      EntityPlayer player, EnumHand hand, EnumFacing side, float hitX, float hitY, float hitZ) {
    if (kind.seatTop < 0 || player.isSneaking()) {
      return false;
    }
    if (!world.isRemote) {
      StadiumSeating.sit(world, pos, state.getValue(FACING), kind.seatTop, 0.0, player);
    }
    return true;
  }

  /** Box helpers in pixels. */
  static final class BlockVenueBox {

    private BlockVenueBox() {
    }

    static AxisAlignedBB of(double x0, double y0, double z0, double x1, double y1, double z1) {
      return new AxisAlignedBB(x0 / 16.0, y0 / 16.0, z0 / 16.0, x1 / 16.0, y1 / 16.0, z1 / 16.0);
    }
  }
}

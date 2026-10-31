package com.micatechnologies.minecraft.csm.venues.seating;

import com.micatechnologies.minecraft.csm.codeutils.CsmBlockStateContainer;
import com.micatechnologies.minecraft.csm.codeutils.RotationUtils;
import com.micatechnologies.minecraft.csm.venues.BlockVenue;
import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

/**
 * A stadium seat ({@code stadium_seat_<colour>}): a folding theatre-style seat with a moulded
 * shell, a cast standard with an armrest and a cupholder on its left, one block per colour. Plain
 * baked models with no tile entity, no renderer and no entity, since an arena bowl holds about
 * eighteen thousand of them.
 *
 * <p>Seats facing the same way side by side are a row: each draws the standard on its left
 * boundary, which is the right-hand armrest of the seat beside it, and an end standard with a
 * blank aluminium plate wherever the cell to its left or right holds no seat of the row
 * ({@link #END_L}, {@link #END_R}, actual state). An aisle-end block
 * ({@link BlockStadiumAisleEnd}) counts as a seat of the row, so the row's end shows its row
 * letter instead.</p>
 *
 * <p><b>Metadata</b> (so a generator can place any state with {@code /setblock}): bits 0-1 the
 * facing (S 0, W 1, N 2, E 3), the way the seat faces, towards the event floor; bit 2 the seat
 * folded up; bit 3 the 1-deep shape. The two shapes:</p>
 * <ul>
 *   <li><b>2-deep</b> (bit 3 clear): the seat at the back of a row of 1 up x 2 deep, its
 *   standards on the floor, the legroom the cell in front of it;</li>
 *   <li><b>1-deep</b> (bit 3 set): a row of 1 up x 1 deep, the standards hung off the riser
 *   behind the seat (as a steep upper bowl's are), the seat shallower, the legroom the front of
 *   its own cell.</li>
 * </ul>
 * <p>Placed by hand, a seat faces whoever placed it and takes the 1-deep shape when the cell in
 * front of it has no floor at its own level (the row below is a block lower straight away).</p>
 *
 * <p>A click sits in a seat that is down, or puts a folded seat down; a sneak-click folds the
 * seat up or down. A sitter gets up into the cell in front.</p>
 *
 * @since 2026.10
 */
public class BlockStadiumSeat extends BlockVenue implements IStadiumSeat {

  /** The seat is folded up. Stored, bit 2. */
  public static final PropertyBool UP = PropertyBool.create("up");
  /** The 1-deep, riser-mounted shape. Stored, bit 3. */
  public static final PropertyBool RISER = PropertyBool.create("riser");
  /** No seat of the row to the left (as the sitter sees it): an end standard there. Actual. */
  public static final PropertyBool END_L = PropertyBool.create("end_l");
  /** No seat of the row to the right: an end standard there. Actual. */
  public static final PropertyBool END_R = PropertyBool.create("end_r");

  /** The pan's top above the floor, in blocks. */
  static final double SEAT_TOP = 8.5 / 16.0;
  /** How far forward of the cell's middle the pan's middle is, 2-deep and 1-deep, in blocks. */
  static final double FORWARD_FLOOR = 1.0 / 16.0;
  static final double FORWARD_RISER = -1.5 / 16.0;

  private static final AxisAlignedBB SELECT = new AxisAlignedBB(0, 0, 1 / 16.0, 1, 1, 1);
  private static final AxisAlignedBB[] FLOOR_DOWN = {
      box(1.5, 6, 3, 14.5, 8.5, 11), box(1.5, 8.5, 11, 14.5, 16, 15)};
  private static final AxisAlignedBB[] FLOOR_UP = {box(1.5, 6, 8.5, 14.5, 16, 15)};
  private static final AxisAlignedBB[] RISER_DOWN = {
      box(1.5, 6, 5.5, 14.5, 8.5, 13.5), box(1.5, 8.5, 13.5, 14.5, 16, 15.5)};
  private static final AxisAlignedBB[] RISER_UP = {box(1.5, 6, 11.5, 14.5, 16, 15.5)};

  private final int colour;

  /**
   * Constructs a seat.
   *
   * @param registryName its registry name, {@code stadium_seat_<colour>}
   * @param colour       its shell's colour, 0xRRGGBB
   */
  public BlockStadiumSeat(String registryName, int colour) {
    super(registryName, Material.IRON, SoundType.METAL, 1.5F);
    this.colour = colour;
    setDefaultState(blockState.getBaseState().withProperty(FACING, EnumFacing.NORTH)
        .withProperty(UP, false).withProperty(RISER, false).withProperty(END_L, true)
        .withProperty(END_R, true));
  }

  static AxisAlignedBB box(double x0, double y0, double z0, double x1, double y1, double z1) {
    return new AxisAlignedBB(x0 / 16.0, y0 / 16.0, z0 / 16.0, x1 / 16.0, y1 / 16.0, z1 / 16.0);
  }

  /** The shell's colour. */
  public int getColour() {
    return colour;
  }

  @Override
  @Nonnull
  protected BlockStateContainer createBlockState() {
    return new CsmBlockStateContainer(this, FACING, UP, RISER, END_L, END_R);
  }

  @Override
  @Nonnull
  public IBlockState getStateFromMeta(int meta) {
    return getDefaultState().withProperty(FACING, EnumFacing.byHorizontalIndex(meta & 3))
        .withProperty(UP, (meta & 4) != 0).withProperty(RISER, (meta & 8) != 0);
  }

  @Override
  public int getMetaFromState(IBlockState state) {
    return state.getValue(FACING).getHorizontalIndex() | (state.getValue(UP) ? 4 : 0)
        | (state.getValue(RISER) ? 8 : 0);
  }

  @Override
  @Nonnull
  @SuppressWarnings("deprecation")
  public IBlockState getActualState(@Nonnull IBlockState state, IBlockAccess world,
      BlockPos pos) {
    EnumFacing f = state.getValue(FACING);
    return state.withProperty(END_L, !inRow(world, pos.offset(f.rotateYCCW()), f))
        .withProperty(END_R, !inRow(world, pos.offset(f.rotateY()), f));
  }

  /**
   * Whether the cell at {@code pos} holds a seat of a row facing {@code facing}.
   *
   * @param world  the world
   * @param pos    the cell
   * @param facing the row's facing
   *
   * @return true for a stadium seat or aisle-end block facing the same way
   */
  static boolean inRow(IBlockAccess world, BlockPos pos, EnumFacing facing) {
    IBlockState s = world.getBlockState(pos);
    return s.getBlock() instanceof IStadiumSeat
        && ((IStadiumSeat) s.getBlock()).seatFacing(world, pos, s) == facing;
  }

  @Override
  public EnumFacing seatFacing(IBlockAccess world, BlockPos pos, IBlockState state) {
    return state.getValue(FACING);
  }

  @Override
  public int seatColour(IBlockAccess world, BlockPos pos, IBlockState state) {
    return colour;
  }

  @Override
  @Nonnull
  public IBlockState getStateForPlacement(World world, BlockPos pos, EnumFacing facing,
      float hitX, float hitY, float hitZ, int meta, EntityLivingBase placer) {
    EnumFacing front = placer.getHorizontalFacing().getOpposite();
    BlockPos ahead = pos.offset(front).down();
    boolean riser = !world.getBlockState(ahead).isSideSolid(world, ahead, EnumFacing.UP);
    return getDefaultState().withProperty(FACING, front).withProperty(RISER, riser);
  }

  @Override
  public AxisAlignedBB getBlockBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
    return SELECT;
  }

  @Override
  @SuppressWarnings("deprecation")
  public void addCollisionBoxToList(IBlockState state, World world, BlockPos pos,
      AxisAlignedBB entityBox, List<AxisAlignedBB> boxes, @Nullable Entity entity,
      boolean isActualState) {
    addSeatBoxes(pos, entityBox, boxes, state.getValue(FACING), state.getValue(RISER),
        state.getValue(UP));
  }

  /**
   * Adds a seat's collision: the pan and back, or the folded pan against the back. The legroom
   * and the standards are left free, so a player walks along a 2-deep row in front of the seats,
   * and along a 1-deep row once its seats are folded.
   */
  static void addSeatBoxes(BlockPos pos, AxisAlignedBB entityBox, List<AxisAlignedBB> boxes,
      EnumFacing facing, boolean riser, boolean up) {
    AxisAlignedBB[] parts = riser ? (up ? RISER_UP : RISER_DOWN) : (up ? FLOOR_UP : FLOOR_DOWN);
    for (AxisAlignedBB part : parts) {
      addCollisionBoxToList(pos, entityBox, boxes,
          RotationUtils.rotateBoundingBoxByFacing(part, facing));
    }
  }

  @Override
  public boolean onBlockActivated(World world, BlockPos pos, IBlockState state,
      EntityPlayer player, EnumHand hand, EnumFacing side, float hitX, float hitY, float hitZ) {
    boolean up = state.getValue(UP);
    if (player.isSneaking() || up) {
      if (!world.isRemote) {
        world.setBlockState(pos, state.withProperty(UP, !up), 3);
      }
      return true;
    }
    if (!world.isRemote) {
      EnumFacing front = state.getValue(FACING);
      StadiumSeating.sit(world, pos, front, SEAT_TOP,
          state.getValue(RISER) ? FORWARD_RISER : FORWARD_FLOOR, player);
    }
    return true;
  }
}

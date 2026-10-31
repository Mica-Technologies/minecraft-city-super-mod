package com.micatechnologies.minecraft.csm.venues.outdoor;

import com.micatechnologies.minecraft.csm.codeutils.CsmBlockStateContainer;
import com.micatechnologies.minecraft.csm.codeutils.EntityCsmSeat;
import com.micatechnologies.minecraft.csm.codeutils.RotationUtils;
import com.micatechnologies.minecraft.csm.venues.BlockVenue;
import com.micatechnologies.minecraft.csm.venues.seating.StadiumSeating;
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
 * An aluminium bleacher cell: the row's deck (three footboard planks over a closed riser) filling
 * its cell, and in the cell above it the seat plank ({@code aluminium_bleacher}) or the aisle's
 * half step ({@code aluminium_bleacher_aisle}). Rows rise a block at a time, as the bowl's do,
 * one or two deep: a 1-deep row is one cell of seat blocks a row, each row a block up and a block
 * back from the one before; a 2-deep row is a cell of plain deck (seat bit clear) in front of a
 * cell of seat blocks, on the same level.
 *
 * <p>A player stands on the deck (its top is the row's floor) and sits on the plank: a click on a
 * seat block sits there (Core's seat entity, only while someone sits). The plank's top is half a
 * block over the deck and half a block under the next row's, so the planks are the steps a
 * bleacher is climbed by; the aisle's half step is the same height, with no plank to sit on. A
 * click with an empty hand on a seat block's deck sits on its plank.</p>
 *
 * <p>Nothing about the run is stored. A row closes itself: where the cell to the side holds no
 * bleacher of the same facing it draws an end panel under the deck and an end guard rail above it
 * ({@code end_l}, {@code end_r}); the top row draws a back panel and a back guard rail where
 * nothing of the bleacher is behind it or behind and above it ({@code back}). An aisle cell
 * continues a row, so rows beside an aisle stay open to it. Underneath, fill the cells under the
 * rows with {@link BlockBleacherFrame}.</p>
 *
 * <p><b>Metadata</b>: bits 0-1 the facing (S 0, W 1, N 2, E 3), the way the spectators look; bit
 * 2 (seat blocks only) the seat plank. Placed by hand a seat block has its plank.</p>
 *
 * @since 2026.10
 */
public class BlockBleacher extends BlockVenue {

  /** The seat plank in the cell above (seat blocks). Stored, bit 2. */
  public static final PropertyBool SEAT = PropertyBool.create("seat");
  /** No bleacher to the left (looking the way the row faces). Actual. */
  public static final PropertyBool END_L = PropertyBool.create("end_l");
  /** No bleacher to the right. Actual. */
  public static final PropertyBool END_R = PropertyBool.create("end_r");
  /** Nothing of the bleacher behind, or behind and a block up: the top row's back. Actual. */
  public static final PropertyBool BACK = PropertyBool.create("back");

  /** The plank's top over the deck's floor (the block's bottom), in blocks. */
  static final double PLANK_TOP = 24 / 16.0;
  /** How far forward of the cell's middle the plank's middle is, in blocks. */
  static final double PLANK_FORWARD = -4.25 / 16.0;

  private static final AxisAlignedBB DECK = box(0, 0, 0, 16, 16, 16);
  private static final AxisAlignedBB PLANK = box(0, 16, 9, 16, 24, 15.5);
  private static final AxisAlignedBB STEP = box(0, 16, 8, 16, 24, 16);
  private static final AxisAlignedBB RAIL_L = box(0, 16, 0, 1.5, 31, 16);
  private static final AxisAlignedBB RAIL_R = box(14.5, 16, 0, 16, 31, 16);
  private static final AxisAlignedBB RAIL_BACK = box(0, 16, 14.5, 16, 31.5, 16);

  private final boolean aisle;

  /**
   * Constructs a bleacher cell.
   *
   * @param registryName its registry name
   * @param aisle        the aisle cell (a half step, no seat) rather than a seat cell
   */
  public BlockBleacher(String registryName, boolean aisle) {
    super(registryName, Material.IRON, SoundType.METAL, 2.0F);
    this.aisle = aisle;
    setDefaultState(blockState.getBaseState().withProperty(FACING, EnumFacing.NORTH)
        .withProperty(SEAT, !aisle).withProperty(END_L, true).withProperty(END_R, true)
        .withProperty(BACK, true));
  }

  static AxisAlignedBB box(double x0, double y0, double z0, double x1, double y1, double z1) {
    return new AxisAlignedBB(x0 / 16.0, y0 / 16.0, z0 / 16.0, x1 / 16.0, y1 / 16.0, z1 / 16.0);
  }

  /** Whether this is the aisle cell. */
  public boolean isAisle() {
    return aisle;
  }

  @Override
  @Nonnull
  protected BlockStateContainer createBlockState() {
    return new CsmBlockStateContainer(this, FACING, SEAT, END_L, END_R, BACK);
  }

  @Override
  @Nonnull
  @SuppressWarnings("deprecation")
  public IBlockState getStateFromMeta(int meta) {
    return getDefaultState().withProperty(FACING, EnumFacing.byHorizontalIndex(meta & 3))
        .withProperty(SEAT, !aisle && (meta & 4) != 0);
  }

  @Override
  public int getMetaFromState(IBlockState state) {
    return state.getValue(FACING).getHorizontalIndex() | (state.getValue(SEAT) ? 4 : 0);
  }

  @Override
  @Nonnull
  public IBlockState getStateForPlacement(World world, BlockPos pos, EnumFacing facing,
      float hitX, float hitY, float hitZ, int meta, EntityLivingBase placer) {
    return getDefaultState().withProperty(FACING, placer.getHorizontalFacing().getOpposite())
        .withProperty(SEAT, !aisle);
  }

  /** Whether the cell at {@code pos} is a bleacher cell (seat, deck or aisle) facing so. */
  static boolean bleacher(IBlockAccess world, BlockPos pos, EnumFacing facing) {
    IBlockState s = world.getBlockState(pos);
    return s.getBlock() instanceof BlockBleacher && s.getValue(FACING) == facing;
  }

  @Override
  @Nonnull
  @SuppressWarnings("deprecation")
  public IBlockState getActualState(@Nonnull IBlockState state, IBlockAccess world,
      BlockPos pos) {
    EnumFacing f = state.getValue(FACING);
    BlockPos behind = pos.offset(f.getOpposite());
    return state.withProperty(END_L, !bleacher(world, pos.offset(f.rotateYCCW()), f))
        .withProperty(END_R, !bleacher(world, pos.offset(f.rotateY()), f))
        .withProperty(BACK, !bleacher(world, behind, f) && !bleacher(world, behind.up(), f));
  }

  @Override
  public AxisAlignedBB getBlockBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
    return DECK;
  }

  @Override
  @SuppressWarnings("deprecation")
  public void addCollisionBoxToList(IBlockState state, World world, BlockPos pos,
      AxisAlignedBB entityBox, List<AxisAlignedBB> boxes, @Nullable Entity entity,
      boolean isActualState) {
    IBlockState s = isActualState ? state : state.getActualState(world, pos);
    EnumFacing f = s.getValue(FACING);
    add(pos, entityBox, boxes, DECK, f);
    if (aisle) {
      add(pos, entityBox, boxes, STEP, f);
    } else if (s.getValue(SEAT)) {
      add(pos, entityBox, boxes, PLANK, f);
    }
    if (s.getValue(END_L)) {
      add(pos, entityBox, boxes, RAIL_L, f);
    }
    if (s.getValue(END_R)) {
      add(pos, entityBox, boxes, RAIL_R, f);
    }
    if (s.getValue(BACK)) {
      add(pos, entityBox, boxes, RAIL_BACK, f);
    }
  }

  private static void add(BlockPos pos, AxisAlignedBB entityBox, List<AxisAlignedBB> boxes,
      AxisAlignedBB part, EnumFacing facing) {
    addCollisionBoxToList(pos, entityBox, boxes,
        RotationUtils.rotateBoundingBoxByFacing(part, facing));
  }

  @Override
  public boolean onBlockActivated(World world, BlockPos pos, IBlockState state,
      EntityPlayer player, EnumHand hand, EnumFacing side, float hitX, float hitY, float hitZ) {
    if (aisle || !state.getValue(SEAT) || hand != EnumHand.MAIN_HAND || player.isSneaking()
        || !player.getHeldItem(hand).isEmpty()) {
      return false;
    }
    if (!world.isRemote) {
      EnumFacing front = state.getValue(FACING);
      double sx = pos.getX() + 0.5 + front.getXOffset() * PLANK_FORWARD;
      double sz = pos.getZ() + 0.5 + front.getZOffset() * PLANK_FORWARD;
      BlockPos out = pos.up().offset(front);
      EntityCsmSeat.sit(world, pos, new AxisAlignedBB(pos).expand(0, 1, 0),
          StadiumSeating.SEAT_TAKEN, sx, pos.getY() + 1.1, sz, PLANK_TOP - 1.1 - 0.12, front,
          out.getX() + 0.5, out.getY(), out.getZ() + 0.5, player);
    }
    return true;
  }
}

package com.micatechnologies.minecraft.csm.healthcare.patient;

import com.micatechnologies.minecraft.csm.codeutils.EntityCsmSeat;
import com.micatechnologies.minecraft.csm.codeutils.RotationUtils;
import com.micatechnologies.minecraft.csm.healthcare.BlockHealthcareRun;
import com.micatechnologies.minecraft.csm.healthcare.HealthcareMaterial;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

/**
 * A seat in a row of tiered lecture hall seating ({@code lecture_hall_seat}): a seat and back
 * with a tablet arm beside it, joining left and right into a row like the beam seating, ends
 * only where the row stops.
 *
 * <p>The tiers step up half a block a row. A row is one block deep: the seat at the back of the
 * cell, the legroom and the tablet in front of it, which a player can stand in. The front row
 * stands on the floor ({@link #RAISED} clear); the next row back, in the cell behind it, stands
 * on its own half-block riser ({@code raised}, bit 2 of the metadata), drawn as the cell's lower
 * half with a nosing on its front edge; the row after that stands on the floor one block up (a
 * full block laid behind the riser), and so on: the rows of a hall alternate floor and riser, a
 * block up every two rows.</p>
 *
 * <p><b>Metadata</b>: bits 0-1 the facing (S 0, W 1, N 2, E 3), the way the seats face; bit 2
 * the riser. A click sits; a sitter leaves into the legroom in front of the seat.</p>
 *
 * @since 2026.10
 */
public class BlockLectureSeating extends BlockHealthcareRun {

  /** The row stands on a half-block riser. */
  public static final PropertyBool RAISED = PropertyBool.create("raised");

  private static final PropertyBool[] FLAGS = {RAISED};

  /** The riser's height, in blocks. */
  public static final double RISER = 0.5;

  /** The seat's top above the row's floor, and how far forward of the middle it is, in px. */
  private static final double SEAT_TOP = 7.5;
  private static final double SEAT_FORWARD = -2.5;

  private static final AxisAlignedBB LOW = new AxisAlignedBB(0, 0, 0, 1, 15 / 16.0, 1);
  private static final AxisAlignedBB HIGH = new AxisAlignedBB(0, 0, 0, 1, 1, 1);
  /**
   * The riser, the seat and the back (facing north, back at +z), in blocks. They leave the front
   * of the cell (the legroom) and the air over the seat free, so a player stands in front of the
   * seat or steps onto it.
   */
  private static final AxisAlignedBB RISER_BOX = new AxisAlignedBB(0, 0, 0, 1, RISER, 1);
  private static final AxisAlignedBB SEAT_BOX = new AxisAlignedBB(0, 0, 5.5 / 16.0, 1,
      7.6 / 16.0, 13 / 16.0);
  private static final AxisAlignedBB BACK_BOX = new AxisAlignedBB(0, 7.6 / 16.0, 12.2 / 16.0, 1,
      15.5 / 16.0, 14.2 / 16.0);

  /** Constructs the seat. */
  public BlockLectureSeating() {
    super("lecture_hall_seat", new int[]{0, 0, 0, 16, 15, 16}, HealthcareMaterial.CLOTH, true,
        "lecture_hall_seat");
    withSeat(SEAT_TOP, SEAT_FORWARD);
  }

  @Override
  protected PropertyBool[] storedFlags() {
    return FLAGS;
  }

  @Override
  protected double seatTop(IBlockState state) {
    return super.seatTop(state) + (state.getValue(RAISED) ? RISER : 0);
  }

  @Override
  public AxisAlignedBB getBlockBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
    return state.getValue(RAISED) ? HIGH : LOW;
  }

  @Override
  @SuppressWarnings("deprecation")
  public void addCollisionBoxToList(IBlockState state, World world, BlockPos pos,
      AxisAlignedBB entityBox, List<AxisAlignedBB> boxes, @Nullable Entity entity,
      boolean isActualState) {
    EnumFacing facing = state.getValue(FACING);
    double lift = state.getValue(RAISED) ? RISER : 0;
    if (lift > 0) {
      addCollisionBoxToList(pos, entityBox, boxes, RISER_BOX);
    }
    addCollisionBoxToList(pos, entityBox, boxes,
        RotationUtils.rotateBoundingBoxByFacing(SEAT_BOX, facing).offset(0, lift, 0));
    addCollisionBoxToList(pos, entityBox, boxes,
        RotationUtils.rotateBoundingBoxByFacing(BACK_BOX, facing).offset(0, lift, 0));
  }

  @Override
  public boolean onBlockActivated(World world, BlockPos pos, IBlockState state,
      EntityPlayer player, EnumHand hand, EnumFacing side, float hitX, float hitY, float hitZ) {
    if (player.isSneaking()) {
      return false;
    }
    if (!world.isRemote) {
      EnumFacing front = state.getValue(FACING);
      double lift = state.getValue(RAISED) ? RISER : 0;
      double top = seatTop(state);
      double fwd = SEAT_FORWARD / 16.0;
      double sx = pos.getX() + 0.5 + front.getXOffset() * fwd;
      double sz = pos.getZ() + 0.5 + front.getZOffset() * fwd;
      // out into the row's own legroom, in front of the seat
      double ox = pos.getX() + 0.5 + front.getXOffset() * 0.2;
      double oz = pos.getZ() + 0.5 + front.getZOffset() * 0.2;
      EntityCsmSeat.sit(world, pos, new AxisAlignedBB(pos), "csm.healthcare.seat.taken", sx,
          pos.getY() + 0.1, sz, top - 0.12 - 0.1, front, ox, pos.getY() + lift, oz, player);
    }
    return true;
  }
}

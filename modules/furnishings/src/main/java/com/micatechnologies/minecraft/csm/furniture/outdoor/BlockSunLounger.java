package com.micatechnologies.minecraft.csm.furniture.outdoor;

import com.micatechnologies.minecraft.csm.codeutils.EntityCsmSeat;
import com.micatechnologies.minecraft.csm.furniture.residential.BlockResidentialWide;
import com.micatechnologies.minecraft.csm.furniture.residential.WidePieces;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * A sun lounger, two blocks long, its backrest raised at the end it was placed from (part 0) and
 * its cushion running on to the foot ({@link WidePieces}: the other block is to the right of
 * someone facing its side). A right-click on either block sits the player on it, just past the
 * backrest's hinge, looking along it to the foot; sneak to get up, onto the ground beside the
 * backrest.
 *
 * @since 2026.9
 */
public class BlockSunLounger extends BlockResidentialWide {

  /** The top of the cushion, in blocks. */
  private static final double SEAT_TOP = 7.0 / 16.0;
  /** Where the sitter's middle is, past the middle of part 0 toward the foot, in blocks. */
  private static final double SEAT_ALONG = 6.0 / 16.0;
  /** The seat entity's height and how far below the seat's top a rider sits, in blocks. */
  private static final double SEAT_Y = 0.1;
  private static final double RIDER_BELOW_SEAT = 0.12;

  /**
   * Constructs a sun lounger.
   *
   * @param registryName its registry name, ending in its frame's finish
   * @param box          its box facing north along both blocks, in sixteenths, x 0 to 32
   */
  public BlockSunLounger(String registryName, int[] box) {
    super(registryName, box);
  }

  @Override
  public boolean onBlockActivated(World world, BlockPos pos, IBlockState state,
      EntityPlayer player, EnumHand hand, EnumFacing side, float hitX, float hitY, float hitZ) {
    if (player.isSneaking()) {
      return false;
    }
    if (world.isRemote) {
      return true;
    }
    BlockPos head = state.getValue(WidePieces.PART) == 0 ? pos : WidePieces.otherCell(pos, state);
    BlockPos foot = WidePieces.otherCell(head, state.withProperty(WidePieces.PART, 0));
    EnumFacing front = state.getValue(FACING);
    EnumFacing along = front.rotateY();
    double x = head.getX() + 0.5 + along.getXOffset() * SEAT_ALONG;
    double z = head.getZ() + 0.5 + along.getZOffset() * SEAT_ALONG;
    BlockPos out = head.offset(front);
    EntityCsmSeat.sit(world, head, new AxisAlignedBB(head).union(new AxisAlignedBB(foot)),
        "csm.furnishings.seat.taken", x, head.getY() + SEAT_Y, z,
        SEAT_TOP - RIDER_BELOW_SEAT - SEAT_Y, along, out.getX() + 0.5, out.getY(),
        out.getZ() + 0.5, player);
    return true;
  }
}

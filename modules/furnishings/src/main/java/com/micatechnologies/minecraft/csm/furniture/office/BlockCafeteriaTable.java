package com.micatechnologies.minecraft.csm.furniture.office;

import com.micatechnologies.minecraft.csm.codeutils.EntityCsmSeat;
import com.micatechnologies.minecraft.csm.furniture.residential.BlockResidentialWide;
import com.micatechnologies.minecraft.csm.furniture.residential.FixtureMaterial;
import com.micatechnologies.minecraft.csm.furniture.residential.WidePieces;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * The school cafeteria's mobile fold-up table: a laminate top two blocks long with four round
 * stools on each side, fixed to its frame ({@link WidePieces}: placed and broken as one, the
 * other block to the right of someone facing its front). Its top is a table's height, so the
 * things a table takes stand on it ({@code SurfaceRest}).
 *
 * <p>Each block has two stools a side. A right-click sits the player on the stool nearest the
 * point clicked, on the side of the table they stand on, facing across the table; so up to
 * eight people sit at one. Sneak to get up, back onto that side.</p>
 *
 * @since 2026.10
 */
public class BlockCafeteriaTable extends BlockResidentialWide {

  /** The top of a stool, in blocks. */
  private static final double SEAT_TOP = 7.5 / 16.0;
  /** How far out from the block's middle a stool is, to the front or the back, in blocks. */
  private static final double STOOL_OUT = 5.5 / 16.0;
  /** How far along from the block's middle a stool is, either way, in blocks. */
  private static final double STOOL_ALONG = 4.0 / 16.0;
  /** The seat entity's height and how far below the seat's top a rider sits, in blocks. */
  private static final double SEAT_Y = 0.1;
  private static final double RIDER_BELOW_SEAT = 0.12;
  /** How far round a stool another sitter makes it taken, in blocks. */
  private static final double STOOL_REACH = 0.2;

  /**
   * Constructs a cafeteria table.
   *
   * @param registryName its registry name, ending in its stools' colour
   * @param box          its box facing north along both blocks, in sixteenths, x 0 to 32
   */
  public BlockCafeteriaTable(String registryName, int[] box) {
    super(registryName, box, FixtureMaterial.METAL.getMaterial(),
        FixtureMaterial.METAL.getSound(), FixtureMaterial.METAL.getHardness());
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
    EnumFacing front = state.getValue(FACING);
    double dx = player.posX - (pos.getX() + 0.5);
    double dz = player.posZ - (pos.getZ() + 0.5);
    EnumFacing out = dx * front.getXOffset() + dz * front.getZOffset() >= 0 ? front
        : front.getOpposite();
    EnumFacing along = front.rotateY();
    double a = (hitX - 0.5) * along.getXOffset() + (hitZ - 0.5) * along.getZOffset();
    double alongOffset = a < 0 ? -STOOL_ALONG : STOOL_ALONG;
    double x = pos.getX() + 0.5 + out.getXOffset() * STOOL_OUT + along.getXOffset() * alongOffset;
    double z = pos.getZ() + 0.5 + out.getZOffset() * STOOL_OUT + along.getZOffset() * alongOffset;
    AxisAlignedBB stool = new AxisAlignedBB(x - STOOL_REACH, pos.getY(), z - STOOL_REACH,
        x + STOOL_REACH, pos.getY() + 1, z + STOOL_REACH);
    BlockPos exit = pos.offset(out);
    EntityCsmSeat.sit(world, pos, stool, "csm.furnishings.seat.taken", x, pos.getY() + SEAT_Y, z,
        SEAT_TOP - RIDER_BELOW_SEAT - SEAT_Y, out.getOpposite(), exit.getX() + 0.5, exit.getY(),
        exit.getZ() + 0.5, player);
    return true;
  }
}

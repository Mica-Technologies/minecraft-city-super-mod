package com.micatechnologies.minecraft.csm.venues.seating;

import com.micatechnologies.minecraft.csm.venues.BlockVenue;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

/**
 * The companion seat beside a wheelchair space ({@code stadium_companion_seat}): a loose padded
 * chair on a steel frame, moved out of the way when the space is used. Its cushion takes the
 * colour of the stadium seat beside it (any of the four sides), so one block suits every
 * section; charcoal with none.
 *
 * <p><b>Metadata</b>: the facing (S 0, W 1, N 2, E 3). A click sits.</p>
 *
 * @since 2026.10
 */
public class BlockStadiumCompanionSeat extends BlockVenue {

  private static final AxisAlignedBB BOX = BlockStadiumSeat.box(2, 0, 2, 14, 15.5, 14.5);
  private static final double SEAT_TOP = 9.0 / 16.0;

  /**
   * Constructs the companion seat.
   *
   * @param registryName its registry name
   */
  public BlockStadiumCompanionSeat(String registryName) {
    super(registryName, Material.IRON, SoundType.METAL, 1.0F);
    setDefaultState(blockState.getBaseState().withProperty(FACING, EnumFacing.NORTH));
  }

  /**
   * The colour of the first stadium seat beside {@code pos}, or charcoal.
   *
   * @param world the world
   * @param pos   the companion seat
   *
   * @return the colour, 0xRRGGBB
   */
  public static int colourBeside(IBlockAccess world, BlockPos pos) {
    for (EnumFacing d : EnumFacing.HORIZONTALS) {
      IBlockState s = world.getBlockState(pos.offset(d));
      if (s.getBlock() instanceof BlockStadiumSeat) {
        return ((BlockStadiumSeat) s.getBlock()).getColour();
      }
    }
    return IStadiumSeat.DEFAULT_COLOUR;
  }

  @Override
  public AxisAlignedBB getBlockBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
    return BOX;
  }

  @Override
  public boolean onBlockActivated(World world, BlockPos pos, IBlockState state,
      EntityPlayer player, EnumHand hand, EnumFacing side, float hitX, float hitY, float hitZ) {
    if (player.isSneaking()) {
      return false;
    }
    if (!world.isRemote) {
      StadiumSeating.sit(world, pos, state.getValue(FACING), SEAT_TOP, 0.0, player);
    }
    return true;
  }
}

package com.micatechnologies.minecraft.csm.transit.airport;

import javax.annotation.Nonnull;
import net.minecraft.block.properties.PropertyInteger;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;

/**
 * The check-in desk: a counter piece of the {@code "checkin"} family (it joins with the bag drop
 * scales and the other desks beside it) with a lit panel on a post behind it carrying one of the
 * four invented airlines. A click with an empty hand steps the airline, a sneaking click steps it
 * back; {@link #AIRLINE} is kept in the two bits of metadata above the facing, so the desk needs
 * no tile entity.
 *
 * @since 2026.9
 */
public class BlockCheckinDesk extends BlockAirportCounter {

  /** The airline on the panel, an index into {@link FlightSchedule#AIRLINES}. */
  public static final PropertyInteger AIRLINE = PropertyInteger.create("airline", 0,
      FlightSchedule.AIRLINES.length - 1);

  /**
   * Constructs a check-in desk.
   *
   * @param registryName its registry name
   * @param box          its box facing north, in sixteenths
   */
  public BlockCheckinDesk(String registryName, double[] box) {
    super(registryName, "checkin", box);
    setDefaultState(getDefaultState().withProperty(AIRLINE, 0));
  }

  @Override
  @Nonnull
  protected BlockStateContainer createBlockState() {
    return new BlockStateContainer(this, FACING, LEFT, RIGHT, AIRLINE);
  }

  @Override
  @Nonnull
  public IBlockState getStateFromMeta(int meta) {
    return getDefaultState().withProperty(FACING, EnumFacing.byHorizontalIndex(meta & 3))
        .withProperty(AIRLINE, (meta >> 2) & 3);
  }

  @Override
  public int getMetaFromState(IBlockState state) {
    return state.getValue(FACING).getHorizontalIndex() | (state.getValue(AIRLINE) << 2);
  }

  @Override
  public boolean onBlockActivated(World world, BlockPos pos, IBlockState state,
      EntityPlayer player, EnumHand hand, EnumFacing side, float hitX, float hitY, float hitZ) {
    if (hand != EnumHand.MAIN_HAND || !player.getHeldItemMainhand().isEmpty()) {
      return false;
    }
    if (!world.isRemote) {
      int count = FlightSchedule.AIRLINES.length;
      int next = (state.getValue(AIRLINE) + (player.isSneaking() ? count - 1 : 1)) % count;
      world.setBlockState(pos, state.withProperty(AIRLINE, next), 3);
      world.playSound(null, pos, SoundEvents.BLOCK_STONE_BUTTON_CLICK_ON, SoundCategory.BLOCKS,
          0.3F, 0.8F);
      player.sendStatusMessage(new TextComponentTranslation("csm.transit.airline",
          FlightSchedule.AIRLINES[next]), true);
    }
    return true;
  }
}

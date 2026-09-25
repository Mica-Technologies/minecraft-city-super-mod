package com.micatechnologies.minecraft.csm.transit.airport;

import com.micatechnologies.minecraft.csm.codeutils.CsmBlockStateContainer;
import com.micatechnologies.minecraft.csm.transit.TransitSounds;
import com.micatechnologies.minecraft.csm.transit.platform.BlockPlatformFixture;
import java.util.Random;
import javax.annotation.Nonnull;
import net.minecraft.block.properties.PropertyInteger;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

/**
 * The boarding pass scanner at a gate, the ticket validator's idea for a plane: hold a
 * {@link ItemBoardingPass} to it and, if the pass has not been used and its flight has neither
 * left nor been cancelled, the pass is marked boarded, the screen shows a green tick and the
 * validator's accept tone sounds; otherwise a red cross, the refusal tone, and the action bar says
 * why. It does not check the gate: which gate a scanner stands at is the builder's business.
 * {@link #LIGHT} is stored in the two bits above the facing (0 idle, 1 accepted, 2 refused) and
 * goes back to idle after {@link #HOLD_TICKS}.
 *
 * @since 2026.9
 */
public class BlockBoardingPassScanner extends BlockPlatformFixture {

  /** The screen: 0 idle, 1 accepted, 2 refused. */
  public static final PropertyInteger LIGHT = PropertyInteger.create("light", 0, 2);

  private static final int HOLD_TICKS = 30;

  /**
   * Constructs a scanner.
   *
   * @param registryName its registry name
   * @param box          its box facing north, in sixteenths
   */
  public BlockBoardingPassScanner(String registryName, double[] box) {
    super(registryName, box);
    setDefaultState(getDefaultState().withProperty(LIGHT, 0));
  }

  @Override
  @Nonnull
  protected BlockStateContainer createBlockState() {
    return new CsmBlockStateContainer(this, FACING, LIGHT);
  }

  @Override
  @Nonnull
  public IBlockState getStateFromMeta(int meta) {
    return getDefaultState().withProperty(FACING, EnumFacing.byHorizontalIndex(meta & 3))
        .withProperty(LIGHT, Math.min(2, (meta >> 2) & 3));
  }

  @Override
  public int getMetaFromState(IBlockState state) {
    return state.getValue(FACING).getHorizontalIndex() | (state.getValue(LIGHT) << 2);
  }

  @Override
  @SuppressWarnings("deprecation")
  public int getLightValue(@Nonnull IBlockState state) {
    return state.getValue(LIGHT) != 0 ? 5 : 0;
  }

  @Override
  public int getLightValue(@Nonnull IBlockState state, IBlockAccess world,
      @Nonnull BlockPos pos) {
    return getLightValue(state);
  }

  @Override
  public boolean onBlockActivated(World world, BlockPos pos, IBlockState state,
      EntityPlayer player, EnumHand hand, EnumFacing side, float hitX, float hitY, float hitZ) {
    if (hand != EnumHand.MAIN_HAND) {
      return true;
    }
    if (world.isRemote) {
      return true;
    }
    ItemStack held = player.getHeldItemMainhand();
    boolean accepted = false;
    TextComponentTranslation message;
    if (held.getItem() instanceof ItemBoardingPass && ItemBoardingPass.hasFlight(held)) {
      long slot = ItemBoardingPass.getSlot(held);
      String flight = FlightSchedule.flight(slot, false);
      FlightSchedule.Status status = FlightSchedule.status(slot, false,
          FlightSchedule.minuteOf(world.getWorldTime()));
      if (ItemBoardingPass.isBoarded(held)) {
        message = new TextComponentTranslation("csm.transit.scanner.boarded");
      } else if (status == FlightSchedule.Status.CANCELLED) {
        message = new TextComponentTranslation("csm.transit.scanner.cancelled", flight);
      } else if (status == FlightSchedule.Status.DEPARTED) {
        message = new TextComponentTranslation("csm.transit.scanner.departed", flight);
      } else {
        ItemBoardingPass.setBoarded(held);
        accepted = true;
        message = new TextComponentTranslation("csm.transit.scanner.ok", flight,
            FlightSchedule.city(slot, false), ItemBoardingPass.getSeat(held));
      }
    } else {
      message = new TextComponentTranslation("csm.transit.scanner.none");
    }
    player.sendStatusMessage(message, true);
    SoundEvent sound = (accepted ? TransitSounds.VALIDATOR_ACCEPT : TransitSounds.VALIDATOR_DENY)
        .getSoundEvent();
    if (sound != null) {
      world.playSound(null, pos, sound, SoundCategory.BLOCKS, 0.7F, 1.0F);
    }
    world.setBlockState(pos, state.withProperty(LIGHT, accepted ? 1 : 2), 3);
    world.scheduleUpdate(pos, this, HOLD_TICKS);
    return true;
  }

  @Override
  public void updateTick(World world, BlockPos pos, IBlockState state, Random rand) {
    if (!world.isRemote && state.getValue(LIGHT) != 0) {
      world.setBlockState(pos, state.withProperty(LIGHT, 0), 3);
    }
  }
}
